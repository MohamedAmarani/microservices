package com.ecommerce.paypalgatewayservice.paypalgatewayservice;

import com.google.common.util.concurrent.AtomicDouble;
import com.paypal.base.rest.PayPalRESTException;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
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
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping(value = "/")
public class HomeController {
    @Autowired
    private Environment env;

    private PayPalClient payPalClient;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${message:Hello default}")
    private String message;

    private AtomicDouble ref;

    private Map<String, Integer> requestsLastMinute = new HashMap<>();

    public HomeController(MeterRegistry registry) {
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

    @GetMapping("/hello")
    public ResponseEntity<String> getHello() {
        incrementCounter();
        System.out.println(message);
        System.out.println(env.getProperty("message"));
        return new ResponseEntity<String>( env.getProperty("message"), HttpStatus.OK);
    }

    @RequestMapping("/info")
    @ApiOperation(value = "Get information from the cart-service instance", notes = "Retrieve information from a cart-service instance")
    public String home() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Order Service running at port: " + env.getProperty("local.server.port");
    }

    @Autowired
    public HomeController(PayPalClient payPalClient){
        this.payPalClient = payPalClient;
    }

    @GetMapping("/{accountId}")
    public Object confirmPayment(@PathVariable final String accountId, @RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String PayerID) throws PayPalRESTException {
        incrementCounter();
        Object obj;
        try {
            obj = completePayment(accountId, paymentId, PayerID);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Account not found"
            );
        }
        return obj;
    }

    @GetMapping("/cancel")
    public String cancelPayment(@PathVariable final String accountId, @RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String PayerID) throws PayPalRESTException {
        incrementCounter();
        completePayment(accountId, paymentId, PayerID);
        return "Algo no ha ido como debía.";
    }

    @PostMapping(value = "/paypal/make/payment/{accountId}")
    public Map<String, Object> makePayment(@PathVariable final String accountId, @RequestBody Map<String, Double> myJsonRequest) throws PayPalRESTException {
        incrementCounter();
        return payPalClient.createPayment(accountId, myJsonRequest.get("totalPrice").toString());
    }

    public Object completePayment(String accountId, String paymentId, String PayerID){
        incrementCounter();
        payPalClient.completePayment(paymentId, PayerID);
        ResponseEntity<Object> res = restTemplate.exchange("http://cart-service:8080/" + accountId + "/checkout2",
                HttpMethod.PUT, null, new ParameterizedTypeReference<Object>() {
                });
        return res.getBody();
    }
}
