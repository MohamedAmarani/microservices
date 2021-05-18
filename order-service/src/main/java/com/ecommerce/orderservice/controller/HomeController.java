package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.model.*;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
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

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

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
    @ApiOperation(value = "Get information from the order-service instance", notes = "Retrieve information from a order-service instance")
    public String home() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Order Service running at port: " + env.getProperty("local.server.port");
    }

    @GetMapping("")
    @ApiOperation(value = "Get all orders", notes = "Retrieve all orders from the Database")
    public List<OrderDTO> getDeliveries()    {
        incrementCounter();
        List<OrderDTO> result = new ArrayList<>();
        List<Order> orders = orderRepository.findAll();
        for (Order order: orders) {
            OrderDTO orderDTO = new OrderDTO(order.getId(), order.getDeliveryId());
            Cart cart = order.getCart();
            CartDTO cartDTO = new CartDTO(cart.getId(), cart.getInventoryId());
            List<CartItemDTO> cartItemDTOS = new ArrayList<>();
            for (CartItem cartItem : cart.getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cart.getInventoryId() +
                                "/products/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.isAvailable());
                cartDTO.addItems(cartItemDTO);
            }
            orderDTO.setCart(cartDTO);
            result.add(orderDTO);
        }
        return result;
    }

    //@HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/{id}")
    @ApiOperation(value = "Get an order", notes = "Provide an Id to retrieve a specific order from the Database")
    public OrderDTO getOrder(@ApiParam(value = "Id of the order to get", required = true) @PathVariable final String id) {
        incrementCounter();
        Order order = orderRepository.findById(id).get();
        OrderDTO orderDTO = new OrderDTO(order.getId(), order.getDeliveryId());
        try {
            CartDTO cartDTO = new CartDTO(order.getCart().getId(), order.getCart().getInventoryId());
            List<CartItemDTO> cartItemDTOS = new ArrayList<>();
            for (CartItem cartItem : order.getCart().getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://product-service:8080/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });
                Gson gson = new Gson();
                ProductDTO productDTO = gson.fromJson(res.getBody(), ProductDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(productDTO, cartItem.getQuantity(), cartItem.isAvailable());
                cartDTO.addItems(cartItemDTO);
                System.out.println(cartItem.getProductId());
            }
            orderDTO.setCart(cartDTO);
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Order not found"
            );
        }
        return orderDTO;
    }

    @PostMapping("")
    @ApiOperation(value = "Create an order", notes = "Provide information to create an order")
    public OrderDTO createInventory(@ApiParam(value = "Information of the order to create", required = true) @RequestBody Cart cart) {
        incrementCounter();
        Order order = orderRepository.save(new Order(cart));
        OrderDTO orderDTO = new OrderDTO(order.getId(), order.getDeliveryId());
        try {
            CartDTO cartDTO = new CartDTO(order.getCart().getId(), order.getCart().getInventoryId());
            List<CartItemDTO> cartItemDTOS = new ArrayList<>();
            for (CartItem cartItem : order.getCart().getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + order.getCart().getInventoryId() +
                                "/products/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.isAvailable());
                cartDTO.addItems(cartItemDTO);
                System.out.println(cartItem.getProductId());
            }
            orderDTO.setCart(cartDTO);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Order not found"
            );

        }
        return orderDTO;
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete an order", notes = "Provide an Id to delete a specific order from the Database")
    public OrderDTO deleteOrder(@ApiParam(value = "Id of the order to delete", required = true) @PathVariable final String id) {
        incrementCounter();
        Order order = orderRepository.findById(id).get();
        OrderDTO orderDTO = new OrderDTO(order.getId(), order.getDeliveryId());
        try {
            CartDTO cartDTO = new CartDTO(order.getCart().getId(), order.getCart().getInventoryId());
            List<CartItemDTO> cartItemDTOS = new ArrayList<>();
            for (CartItem cartItem : order.getCart().getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + order.getCart().getInventoryId() +
                                "/products/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.isAvailable());
                cartDTO.addItems(cartItemDTO);
            }
            orderDTO.setCart(cartDTO);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Order not found"
            );
        }
        orderRepository.deleteById(id);
        return orderDTO;
    }

    //set delivery id
    @PutMapping("/{orderId}/deliveryId")
    @ApiOperation(value = "Add deliveryId to the order", notes = "Link the order to a delivery by giving a deliveryId")
    public OrderDTO setDeliveryId(@ApiParam(value = "Id of the order for which the delivery has to be linked", required = true) @PathVariable final String orderId,
                                  @ApiParam(value = "Id of the delivery that has to be linked to the order", required = true) @RequestBody Map<String, String> myJsonRequest) {
        incrementCounter();
        Order order = orderRepository.findById(orderId).get();
        order.setDeliveryId(myJsonRequest.get("deliveryId").toString());
        order = orderRepository.save(order);
        OrderDTO orderDTO = new OrderDTO(order.getId(), order.getDeliveryId());
        try {
            CartDTO cartDTO = new CartDTO(order.getCart().getId(), order.getCart().getInventoryId());
            List<CartItemDTO> cartItemDTOS = new ArrayList<>();
            for (CartItem cartItem : order.getCart().getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + order.getCart().getInventoryId() +
                                "/products/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.isAvailable());
                cartDTO.addItems(cartItemDTO);
            }
            orderDTO.setCart(cartDTO);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Order not found"
            );
        }
        return orderDTO;
    }


    // a fallback method to be called if failure happened
    public List<OrderDTO> fallback(String catalogId, Throwable hystrixCommand) {
        return new ArrayList<>();
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    // We'll add the logic of role based auth later
    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of Order service running at port: " + env.getProperty("local.server.port");
    }
}
