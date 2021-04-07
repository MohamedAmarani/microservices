package com.ecommerce.deliveryservice.controller;

import com.ecommerce.deliveryservice.model.Cart;
import com.ecommerce.deliveryservice.model.Delivery;
import com.ecommerce.deliveryservice.repository.DeliveryRepository;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("")
public class HomeController {
    @Autowired
    private Environment env;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Value("${message:Hello default}")
    private String message;

    @GetMapping("/hello")
    public ResponseEntity<String> getHello() {
        System.out.println(message);
        System.out.println(env.getProperty("message"));
        return new ResponseEntity<String>( env.getProperty("message"), HttpStatus.OK);
    }

    @RequestMapping("/info")
    public String home() {
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Delivery Service running at port: " + env.getProperty("local.server.port");
    }

    @GetMapping("")
    public List<Delivery> getDeliveries()    {
        return deliveryRepository.findAll();
    }

    //@HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/{id}")
    public Delivery getDelivery(@PathVariable final String id) {
        return deliveryRepository.findById(id).get();
    }

    @PostMapping("")
    public Delivery createInventory(@RequestBody Map<String, String> myJsonRequest) {
        return deliveryRepository.save(new Delivery(myJsonRequest.get("orderId").toString()));
    }

    @PutMapping("/{id}/nextEvent")
    public Delivery continueProcess(@PathVariable final String id) {
        Delivery delivery = deliveryRepository.findById(id).get();
        delivery.setNextDeliveryEvent();
        return deliveryRepository.save(delivery);
    }

    // a fallback method to be called if failure happened
    public List<Delivery> fallback(String catalogId, Throwable hystrixCommand) {
        return new ArrayList<>();
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    // We'll add the logic of role based auth later
    @RequestMapping("/admin")
    public String homeAdmin() {
        return "This is the admin area of Delivery service running at port: " + env.getProperty("local.server.port");
    }
}

