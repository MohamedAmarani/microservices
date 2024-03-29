package com.ecommerce.paypalgatewayservice;

import com.google.common.util.concurrent.AtomicDouble;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.PaymentExecution;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/")
public class HomeController {
    @Autowired
    private Environment env;

    private PayPalClient payPalClient;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @Value("${message:Hello default}")
    private String message;

    private AtomicDouble ref;

    private Map<String, Integer> requestsLastMinute = new HashMap<>();

    @Autowired
    public HomeController(PayPalClient payPalClient, MeterRegistry registry) {
        this.payPalClient = payPalClient;
        ref = registry.gauge("requests_per_second", new AtomicDouble(0.0f));
    }

    @PostConstruct
    public void getSetup() {
        //inicializar las 60 posiciones con 0s
        for (int i = 0; i < 60; ++i) {
            String key = Integer.toString(i);
            requestsLastMinute.put(key.length() < 2 ? "0" + key : key, 0);
        }
        //actualizando campos de los segundos cada segundo
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                //borrar el segundo que viene
                String timeStamp = new SimpleDateFormat("ss").format(Calendar.getInstance().getTime());
                String timeStampIncremented = Integer.toString(Integer.parseInt(timeStamp) + 1);
                requestsLastMinute.put(Integer.parseInt(timeStamp) + 1 < 60 ?
                        (timeStampIncremented.length() < 2 ? "0" + timeStampIncremented : timeStampIncremented) : "00", 0);
            }
        }, 0, 1000);

        //actualizando el valor de micrometer cada 3 segundos
        Timer t1 = new Timer();
        t1.schedule(new TimerTask() {
            @Override
            public void run() {
                //calcular las requests por segundo en el ultimo minuto
                int counter = 0;
                for (String key: requestsLastMinute.keySet()) {
                    counter += requestsLastMinute.get(key);
                    //System.out.println(key);
                }
                ref.set(counter / (double) 60);
            }
        }, 0, 3000);
    };

    private synchronized void incrementCounter() {
        String timeStamp = new SimpleDateFormat("ss").format(Calendar.getInstance().getTime());
        requestsLastMinute.put(timeStamp, requestsLastMinute.get(timeStamp) + 1);
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/hello")
    public ResponseEntity<String> getHello() {
        incrementCounter();
        System.out.println(message);
        System.out.println(env.getProperty("message"));
        return new ResponseEntity<String>( env.getProperty("message"), HttpStatus.OK);
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/info")
    @ApiOperation(value = "Get information from the paypal-gateway-service instance", notes = "Retrieve information from a paypal-gateway-service instance")
    public String home() {
        incrementCounter();
        return "Hello from PayPal Gateway Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping(value = "/{accountId}", params = {
            "PayerID"
    })
    @ApiOperation(value = "Confirm a payment", notes = "Provide the Id of the cart to checkout and the paymentId and PayerID")
    public Object confirmPayment(@ApiParam(value = "Id of the cart for which a payment has to be done", required = true) @PathVariable final String accountId,
                                 @ApiParam(value = "Id of the payment", required = true) @RequestParam("paymentId") String paymentId,
                                 @ApiParam(value = "Id of the payer", required = true) @RequestParam("PayerID") String PayerID) throws PayPalRESTException {
        incrementCounter();
        Object obj;
        try {
            //comprobar que no va a info o cancel
            obj = completePayment(accountId, paymentId, PayerID);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cart not found"
            );
        }
        return obj;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/success/{accountId}")
    @ApiOperation(value = "Manage success in payment", notes = "If the payment goes well the user will be redirected here.")
    public Object successfulPayment(@ApiParam(value = "Id of the cart that tried to be checked out", required = true) @PathVariable final String accountId,
                                    @ApiParam(value = "Id of the payment", required = true) @RequestParam("paymentId") String paymentId,
                                    @ApiParam(value = "Id of the payer", required = true) @RequestParam("PayerID") String PayerID) throws PayPalRESTException {
        incrementCounter();
        return completePayment(accountId, paymentId, PayerID);
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/cancel/{accountId}")
    @ApiOperation(value = "Manage error in payment", notes = "If the payment goes wrong the user will be redirected here.")
    public Map<String, Object> cancelPayment(@ApiParam(value = "Id of the cart that tried to be checked out", required = true) @PathVariable final String accountId) throws PayPalRESTException {
        incrementCounter();
        Map<String, Object> response = new HashMap();
        response.put("status", "canceled");
        return response;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @PostMapping(value = "/make/payment/{accountId}")
    @ApiOperation(value = "Create a payment", notes = "A payment will be created and its amount of money will be passed as a parameter.")
    public Map<String, String> makePayment(@ApiParam(value = "Id of the client that wants to check out", required = true) @PathVariable final String accountId,
                                           @ApiParam(value = "Amount of money to be payed, in Euros.", required = true) @RequestBody Map<String, String> priceInformation) throws PayPalRESTException {
        incrementCounter();
        return payPalClient.createPayment(accountId, priceInformation.get("originalPrice"), priceInformation.get("discountCode"),
                priceInformation.get("discountedAmount"), priceInformation.get("deliveryPrice"), priceInformation.get("finalPrice"));
    }

    //complete the payment
    public Object completePayment(String accountId, String paymentId, String PayerID){
        incrementCounter();
        payPalClient.completePayment(paymentId, PayerID);
        ResponseEntity<Object> res = restTemplate.exchange("http://cart-service:8080/" + accountId + "/checkout2",
                HttpMethod.PUT, null, new ParameterizedTypeReference<Object>() {
                });
        return res.getBody();
    }

    // el metodo fallback a llamar si ocurre algun fallo
    public List<String> fallback(String accountId, Throwable hystrixCommand) {
        return new ArrayList<>();
    }

    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of PayPal Gateway service running at port: " + env.getProperty("local.server.port");
    }
}

