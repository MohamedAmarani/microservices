package com.ecommerce.discountservice.controller;

import com.ecommerce.discountservice.model.Discount;
import com.ecommerce.discountservice.repository.DiscountRepository;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/")
public class HomeController {
    @Autowired
    private Environment env;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscountRepository discountRepository;

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

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
        return "Hello from Discount Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @GetMapping("")
    public List<Discount> getDiscounts() {
        incrementCounter();
        return discountRepository.findAll();
    }

    @PostMapping("")
    @ApiOperation(value = "Create a discount", notes = "Provide information to create a discount")
    public Discount postDiscount(@ApiParam(value = "Information of the discount to create", required = true) @RequestBody Discount discount) {
        incrementCounter();
        try {
            discount = discountRepository.save(discount);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A discount with the same code already exists"
            );
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Discount> discountEntity = new HttpEntity<Discount>(discount, headers);
        //enviar mail de update
        final ResponseEntity<String> res1 = restTemplate.exchange("http://account-service:8080/newDiscountEmail",
                HttpMethod.POST, discountEntity, new ParameterizedTypeReference<String>() {
                });
        return discount;
    }

    @GetMapping("/{discountId}")
    public Discount getDiscount(@ApiParam(value = "Id of the discount that wants to be used", required = true) @PathVariable final String discountId) {
        incrementCounter();
        return discountRepository.findById(discountId).get();
    }

    @DeleteMapping("/{discountId}")
    @ApiOperation(value = "Delete a discount", notes = "Provide information to delete a discount")
    public Discount deleteDiscount(@ApiParam(value = "Id of the discount that wants to be deleted", required = true) @PathVariable final String discountId) {
        incrementCounter();
        Discount discount = discountRepository.findById(discountId).get();
        discountRepository.delete(discount);
        return discount;
    }

    @DeleteMapping("")
    @ApiOperation(value = "Delete all discounts", notes = "Provide information to delete all discounts")
    public List<Discount> deleteDiscounts() {
        incrementCounter();
        discountRepository.deleteAll();
        return discountRepository.findAll();
    }

    @PatchMapping("/{discountId}/useDiscount")
    public Discount useDiscount(@ApiParam(value = "Id of the discount that wants to be used", required = true) @PathVariable final String discountId) {
        incrementCounter();
        Discount discount = discountRepository.findById(discountId).get();
        discount.incrementCurrentUses();
        return discountRepository.save(discount);
    }

    @PatchMapping("/{discountId}/enable")
    public Discount enableDiscount(@ApiParam(value = "Id of the discount that has to be enabled", required = true) @PathVariable final String discountId) {
        incrementCounter();
        Discount discount = discountRepository.findById(discountId).get();
        if (!discount.isEnabled()) {
            discount.setEnabled(true);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Discount> discountEntity = new HttpEntity<Discount>(discount, headers);
            //enviar mail de update
            final ResponseEntity<String> res1 = restTemplate.exchange("http://account-service:8080/enabledDiscountEmail",
                    HttpMethod.POST, discountEntity, new ParameterizedTypeReference<String>() {
                    });
        }
        return discountRepository.save(discount);
    }

    @PatchMapping("/{discountId}/disable")
    public Discount disableDiscount(@ApiParam(value = "Id of the discount that has to be disabled", required = true) @PathVariable final String discountId) {
        incrementCounter();
        Discount discount = discountRepository.findById(discountId).get();
        if (discount.isEnabled()) {
            discount.setEnabled(false);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Discount> discountEntity = new HttpEntity<Discount>(discount, headers);
            //enviar mail de update
            final ResponseEntity<String> res1 = restTemplate.exchange("http://account-service:8080/disabledDiscountEmail",
                    HttpMethod.POST, discountEntity, new ParameterizedTypeReference<String>() {
                    });
        }
        return discountRepository.save(discount);
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    // We'll add the logic of role based auth later
    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of Discount service running at port: " + env.getProperty("local.server.port");
    }
}

