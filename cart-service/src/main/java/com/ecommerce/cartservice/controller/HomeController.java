package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.model.*;
import com.ecommerce.cartservice.repository.CartRepository;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/")
public class HomeController {
    @Autowired
    private Environment env;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CartRepository cartRepository;

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

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
        return "Hello from Cart Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @GetMapping("/do")
    public String getPr() {
        RestTemplate restTemplate = new RestTemplate();
        String resourceUrl = "http://inventory-service:8080/do";
        ResponseEntity<String> response = restTemplate.getForEntity(resourceUrl, String.class);

        return response.getBody().toString();
    }

    @GetMapping("")
    public List<CartDTO> getCarts() {
        List<CartDTO> result = new ArrayList<>();
        List<Cart> carts = cartRepository.findAll();
        for (Cart cart: carts) {
            CartDTO cartDTO = new CartDTO(cart.getId(), cart.getInventoryId());
            List<CartItemDTO> cartItemDTOS = new ArrayList<>();
            for (CartItem cartItem : cart.getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cart.getInventoryId() +
                                "/products/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(inventoryItemDTO.getProductDTO(), cartItem.getItems(), cartItem.isAvailable());
                cartDTO.addCartItemDTOs(cartItemDTO);
            }
            result.add(cartDTO);
        }
        return result;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/{accountId}")
    public CartDTO getCart(@PathVariable final String accountId) {
        System.out.println("Starting " + env.getProperty("local.server.port"));
        Optional<Cart> cart = cartRepository.findById(accountId);
        List<CartItem> cartItems = cart.get().getCartItems();

        CartDTO cartDTO = new CartDTO(cart.get().getId(), cart.get().getInventoryId());
        List<CartItemDTO> cartItemDTOs = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        for (CartItem cartItem: cartItems) {
            CartItemDTO cartItemDTO = new CartItemDTO();

            final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cart.get().getInventoryId() +
                            "/products/" + cartItem.getProductId(),
                    HttpMethod.GET, entity, new ParameterizedTypeReference<String>() {
                    });

            Gson gson = new Gson();
            InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);

            cartItemDTO = new CartItemDTO(inventoryItemDTO.getProductDTO(), cartItem.getItems(), cartItem.isAvailable());

            cartDTO.addCartItemDTOs(cartItemDTO);
        }
        return cartDTO;
    }

    @PostMapping("")
    public Cart createCart(@RequestBody Cart cart) {
        return cartRepository.save(cart);
    }

    // a fallback method to be called if failure happened
    public List<ProductDTO> fallback(String catalogId, Throwable hystrixCommand) {
        return new ArrayList<>();
    }

    //añadir producto a cart
    @PutMapping("/{accountId}")
    public Cart addProductToInventory(@PathVariable final String accountId, @RequestBody CartItem cartItem) {
        Optional<Cart> cart = cartRepository.findById(accountId);

        JSONObject obj = new JSONObject();
        obj.put("numItems", cartItem.getItems());
        // set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);
        // send request and parse result
        //añadir al carrito si hay numero suficiente de items del producto en el inventario y no existe ya en el carrito (si ya existe se suman items)
        final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cart.get().getInventoryId() +
                        "/products/" + cartItem.getProductId(),
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });

        Gson gson = new Gson();
        InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);

        //comprovar si hay stock disponible antes de añadir
        if (inventoryItemDTO.getItems() >= cartItem.getItems()) {
            //comprovar si ya existe y hay stock disponible antes de añadir
            boolean alreadyExists = false;
            for (CartItem cartItem1: cart.get().getCartItems()) {
                if (cartItem1.getProductId().equals(cartItem.getProductId())) {
                    alreadyExists = true;
                    if (inventoryItemDTO.getItems() >= (cartItem.getItems() + cartItem1.getItems())) {
                        cartItem1.setItems(cartItem.getItems() + cartItem1.getItems());
                    }
                    else
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT, "Not enough stock"
                        );
                }
            }
            cartItem.setAvailable(true);
            if (!alreadyExists)
                cart.get().addCartItem(cartItem);
        }
        else
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Not enough stock"
            );

        return cartRepository.save(cart.get());
    }

    @RequestMapping(value = "/{accountId}/checkout", method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Object doCheckoutPart1(@PathVariable final String accountId) {
        Optional<Cart> cart = cartRepository.findById(accountId);
        double totalPrice = 0.0;

        //obtener precio total
        for (CartItem cartItem: cart.get().getCartItems()) {
            //provocar excepcion si alguno no esta disponible
            if (!cartItem.isAvailable())
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Some of the products are not available"
                );

            // obtener el precio de un producto * num items del producto
            final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cart.get().getInventoryId()  +
                            "/products/" + cartItem.getProductId(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                    });

            Gson gson = new Gson();
            InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);

            totalPrice += inventoryItemDTO.getProductDTO().getPrice() * (double) cartItem.getItems();

        }
        //realizar el pago
        JSONObject obj = new JSONObject();
        obj.put("totalPrice", totalPrice);
        // set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);
        final ResponseEntity<String> res = restTemplate.exchange("http://paypal-gateway-service:8080/paypal/make/payment/" + accountId,
                HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                });
        return res.getBody().toString();
    }

    @RequestMapping(value = "/{accountId}/checkout2", method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Object doCheckoutPart2(@PathVariable final String accountId) {
        Optional<Cart> cart = cartRepository.findById(accountId);
                /*final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://account-service/" + accountId + "/buy",
                HttpMethod.PUT, entity, new ParameterizedTypeReference<ProductDTO>() {
                });*/
        //eliminar items del inventario
        /*for (CartItem cartItem: cart.get().getCartItems()) {
            obj = new JSONObject();
            obj.put("numItems", cartItem.getItems());
            // set headers
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            entity = new HttpEntity<String>(obj.toString(), headers);
            // reducir el numero de items de un producto en el inventario
            final ResponseEntity<ProductDTO> res1 = restTemplate.exchange("http://inventory-service/" + cart.get().getInventoryId() +
                            "/products/" + cartItem.getProductId() + "/reduceStock",
                    HttpMethod.PUT, entity, new ParameterizedTypeReference<ProductDTO>() {
                    });
        }*/

        //crear pedido (order)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Cart> orderEntity = new HttpEntity<Cart>(cart.get(), headers);
        final ResponseEntity<OrderDTO> res1 = restTemplate.exchange("http://order-service:8080/",
                HttpMethod.POST, orderEntity, new ParameterizedTypeReference<OrderDTO>() {
                });

        //vaciar carrito
        /*cart = cartRepository.findById(accountId);
        cart.get().setCartItems(new ArrayList<>());
        cartRepository.save(cart.get());*/

        //llamar al micro de transporte
        // set headers
        JSONObject obj = new JSONObject();
        obj.put("orderId", res1.getBody().getId());
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);
        final ResponseEntity<DeliveryDTO> res2 = restTemplate.exchange("http://delivery-service:8080",
                HttpMethod.POST, entity, new ParameterizedTypeReference<DeliveryDTO>() {
                });

        //setear deliveryId a order
        obj = new JSONObject();
        obj.put("deliveryId", res2.getBody().getId());
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        entity = new HttpEntity<String>(obj.toString(), headers);
        final ResponseEntity<DeliveryDTO> res3 = restTemplate.exchange("http://order-service:8080/" + res1.getBody().getId() + "/deliveryId",
                HttpMethod.PUT, entity, new ParameterizedTypeReference<DeliveryDTO>() {
                });

        //devolver envio
        //return res2.getBody();
        JSONObject jo = new JSONObject();
        jo.put("paymentStatus", "success");
        jo.put("orderURI", "/orders/" + res1.getBody().getId());
        return jo.toString();
    }

    @PutMapping("/update")
    public void updateCartsAvailability() {
        updateAvailability();
    }

    //retorna falso si hay algun producto no disponible
    private void updateAvailability() {
        for (Cart cart: cartRepository.findAll()) {
            for (CartItem cartItem : cart.getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cart.getInventoryId() +
                                "/products/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                if (inventoryItemDTO.getItems() >= cartItem.getItems())
                    cartItem.setAvailable(true);
                else {
                    cartItem.setAvailable(false);
                }
            }
            cartRepository.save(cart);
        }
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    // We'll add the logic of role based auth later
    @RequestMapping("/admin")
    public String homeAdmin() {
        return "This is the admin area of Cart service running at port: " + env.getProperty("local.server.port");
    }
}
