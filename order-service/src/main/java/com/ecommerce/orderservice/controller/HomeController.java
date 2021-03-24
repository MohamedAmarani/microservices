package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.model.Cart;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
public class HomeController {
    @Autowired
    private Environment env;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

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
        return "Hello from Order Service running at port: " + env.getProperty("local.server.port");
    }

    @GetMapping("")
    public List<Order> getDeliveries()    {
        return orderRepository.findAll();
    }

    //@HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/{id}")
    public Order getDelivery(@PathVariable final String id) {
        return orderRepository.findById(id).get();
    }

    @PostMapping("/")
    public Order createInventory(@RequestBody Cart cart) {
        return orderRepository.save(new Order(cart));
    }

    @PutMapping("/{id}/deliveryId")
    public Order setDeliveryId(@PathVariable final String id, @RequestBody Map<String, String> myJsonRequest) {
        Order order = orderRepository.findById(id).get();
        order.setDeliveryId(myJsonRequest.get("deliveryId").toString());
        return orderRepository.save(order);
    }


    // a fallback method to be called if failure happened
    public List<Order> fallback(String catalogId, Throwable hystrixCommand) {
        return new ArrayList<>();
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    // We'll add the logic of role based auth later
    @RequestMapping("/admin")
    public String homeAdmin() {
        return "This is the admin area of Order service running at port: " + env.getProperty("local.server.port");
    }
}
