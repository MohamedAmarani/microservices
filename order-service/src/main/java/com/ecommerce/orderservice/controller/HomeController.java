package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.model.*;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.text.DateFormat;
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

    @InitBinder
    public void initBinder(WebDataBinder binder) throws Exception {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        final CustomDateEditor dateEditor = new CustomDateEditor(df, true) {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if ("today".equals(text)) {
                    setValue(new Date());
                } else {
                    super.setAsText(text);
                }
            }
        };
        binder.registerCustomEditor(Date.class, dateEditor);
    }

    @GetMapping("/hello")
    public ResponseEntity<String> getHello() {
        incrementCounter();
        System.out.println(message);
        System.out.println(env.getProperty("message"));
        return new ResponseEntity<String>( env.getProperty("message"), HttpStatus.OK);
    }

    @GetMapping("/info")
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
    public ResponseEntity<Map<String, Object>> getOrders(@RequestParam(defaultValue = "", required = false) String deliveryId,
                                                         @RequestParam(defaultValue = "", required = false) String cartId,
                                                         @RequestParam(defaultValue = "1970-01-01T00:00:0.000+00:00", required = false) Date minCreationDate,
                                                         @RequestParam(defaultValue = "2024-01-01T00:00:0.000+00:00", required = false) Date maxCreationDate,
                                                         @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                                         @RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                         @RequestParam(value = "sort", defaultValue = "creationDate,asc", required = false) String sort) {
        incrementCounter();
        PageRequest request = PageRequest.of(page, size, Sort.by(new Sort.Order(sort.split(",")[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(",")[0])));
        Page<Order> pagedOrders = orderRepository.findByDeliveryIdContainingIgnoreCaseAndCreationDateBetween(deliveryId, minCreationDate, maxCreationDate, request);
        List<Order> list = new ArrayList<>();

        //seleccionar solo los pedidos cuyo cartId coincida con el especificado
        if (!cartId.equals("")) {
            //solo las que tengan el cartId si se ha especificado
            for (int i = 0; i < pagedOrders.getContent().size(); ++i) {
                if (pagedOrders.getContent().get(i).getCart().getId().equals(cartId))
                        list.add(pagedOrders.getContent().get(i));
            }
            pagedOrders = new PageImpl<>(list, PageRequest.of(page, size), list.size());
        }

        List<OrderDTO> result = new ArrayList<>();
        List<Order> orders = pagedOrders.getContent();
        for (Order order: orders) {
            OrderDTO orderDTO = new OrderDTO(order.getId(), order.getDeliveryId(), order.getCreationDate());
            Cart cart = order.getCart();
            CartDTO cartDTO = new CartDTO(cart.getId(), cart.getCreationDate());
            List<CartItemDTO> cartItemDTOS = new ArrayList<>();
            for (CartItem cartItem : cart.getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://product-service:8080/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.getInventoryId(),
                        cartItem.isAvailable(), cartItem.getCreationDate());
                cartDTO.addItems(cartItemDTO);
            }
            orderDTO.setCart(cartDTO);
            result.add(orderDTO);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("currentPage", pagedOrders.getNumber());
        response.put("totalItems", pagedOrders.getTotalElements());
        response.put("totalPages", pagedOrders.getTotalPages());
        response.put("carts", result);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    //@HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/{id}")
    @ApiOperation(value = "Get an order", notes = "Provide an Id to retrieve a specific order from the Database")
    public OrderDTO getOrder(@ApiParam(value = "Id of the order to get", required = true) @PathVariable final String id) {
        incrementCounter();
        Order order = orderRepository.findById(id).get();
        OrderDTO orderDTO = new OrderDTO(order.getId(), order.getDeliveryId(), order.getCreationDate());
        try {
            CartDTO cartDTO = new CartDTO(order.getCart().getId(), order.getCart().getCreationDate());
            List<CartItemDTO> cartItemDTOS = new ArrayList<>();
            for (CartItem cartItem : order.getCart().getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://product-service:8080/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });
                Gson gson = new Gson();
                ProductDTO productDTO = gson.fromJson(res.getBody(), ProductDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(productDTO, cartItem.getQuantity(), cartItem.getInventoryId(), cartItem.isAvailable(),
                        cartItem.getCreationDate());
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

    @PostMapping("")
    @ApiOperation(value = "Create an order", notes = "Provide information to create an order")
    public OrderDTO createOrder(@ApiParam(value = "Information of the order to create", required = true) @RequestBody Cart cart) {
        incrementCounter();
        Order order = orderRepository.save(new Order(cart, new Date()));
        OrderDTO orderDTO = new OrderDTO(order.getId(), order.getDeliveryId(), order.getCreationDate());
        try {
            CartDTO cartDTO = new CartDTO(order.getCart().getId(), order.getCart().getCreationDate());
            for (CartItem cartItem : order.getCart().getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId()+
                                "/products/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.getInventoryId(),
                        cartItem.isAvailable(), cart.getCreationDate());
                cartDTO.addItems(cartItemDTO);
            }
            orderDTO.setCart(cartDTO);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Order not found"
            );

        }
        System.out.println(order.getId() + " POST");
        return orderDTO;
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete an order", notes = "Provide an Id to delete a specific order from the Database")
    public OrderDTO deleteOrder(@ApiParam(value = "Id of the order to delete", required = true) @PathVariable final String id) {
        incrementCounter();
        Order order = orderRepository.findById(id).get();
        OrderDTO orderDTO = new OrderDTO(order.getId(), order.getDeliveryId(), order.getCreationDate());
        try {
            CartDTO cartDTO = new CartDTO(order.getCart().getId(), order.getCart().getCreationDate());
            List<CartItemDTO> cartItemDTOS = new ArrayList<>();
            for (CartItem cartItem : order.getCart().getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId() +
                                "/products/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.getInventoryId(),
                        cartItem.isAvailable(), cartItem.getCreationDate());
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

    @DeleteMapping("")
    @ApiOperation(value = "Delete all order", notes = "Delete all orders from the Database")
    public void deleteOrders() {
        orderRepository.deleteAll();
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
        OrderDTO orderDTO = new OrderDTO(order.getId(), order.getDeliveryId(), order.getCreationDate());
        try {
            CartDTO cartDTO = new CartDTO(order.getCart().getId(), order.getCart().getCreationDate());
            List<CartItemDTO> cartItemDTOS = new ArrayList<>();
            for (CartItem cartItem : order.getCart().getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId() +
                                "/products/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.getInventoryId(),
                        cartItem.isAvailable(), cartItem.getCreationDate());
                cartDTO.addItems(cartItemDTO);
            }
            orderDTO.setCart(cartDTO);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Order not found"
            );
        }
        System.out.println(order.getId() + " PUT");
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
