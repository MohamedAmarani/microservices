package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.model.*;
import com.ecommerce.cartservice.repository.CartRepository;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
    private CartRepository cartRepository;

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

    @GetMapping("/info")
    @ApiOperation(value = "Get information from the cart-service instance", notes = "Retrieve information from a cart-service instance")
    public String home() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Cart Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @GetMapping("/do")
    public String getPr() {
        incrementCounter();
        RestTemplate restTemplate = new RestTemplate();
        String resourceUrl = "http://inventory-service:8080/do";
        ResponseEntity<String> response = restTemplate.getForEntity(resourceUrl, String.class);

        return response.getBody().toString();
    }

    @GetMapping("")
    @ApiOperation(value = "Get all carts", notes = "Retrieve all carts from the Database and all their cart items")
    public List<CartDTO> getCarts() {
        incrementCounter();
        List<CartDTO> result = new ArrayList<>();
        List<Cart> carts = cartRepository.findAll();
        for (Cart cart: carts) {
            CartDTO cartDTO = new CartDTO(cart.getId(), cart.getCreationDate());
            List<CartItemDTO> cartItemDTOS = new ArrayList<>();
            for (CartItem cartItem : cart.getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId() +
                                "/products/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                CartItemDTO cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.getInventoryId(), cartItem.isAvailable(),
                        cartItem.getCreationDate());
                cartDTO.addItems(cartItemDTO);
            }
            result.add(cartDTO);
        }
        return result;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/{cartId}")
    @ApiOperation(value = "Get a cart", notes = "Provide an Id to retrieve a specific cart from the Database and all its cart items")
    public CartDTO getCart(@ApiParam(value = "Id of the cart to get", required = true) @PathVariable final String cartId) {
        incrementCounter();
        System.out.println("Starting " + env.getProperty("local.server.port"));
        Optional<Cart> cart = cartRepository.findById(cartId);
        List<CartItem> cartItems = null;
        try {
            cartItems = cart.get().getCartItems();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cart not found"
            );
        }
        CartDTO cartDTO = new CartDTO(cart.get().getId(), cart.get().getCreationDate());
        List<CartItemDTO> cartItemDTOs = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        for (CartItem cartItem: cartItems) {
            CartItemDTO cartItemDTO = new CartItemDTO();

            final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId() +
                            "/products/" + cartItem.getProductId(),
                    HttpMethod.GET, entity, new ParameterizedTypeReference<String>() {
                    });

            Gson gson = new Gson();
            InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);

            cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.getInventoryId(), cartItem.isAvailable(),
                    cartItem.getCreationDate());

            cartDTO.addItems(cartItemDTO);
        }
        return cartDTO;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @DeleteMapping("/{cartId}")
    @ApiOperation(value = "Delete a cart", notes = "Provide an Id to delete a specific cart from the Database")
    public CartDTO deleteCart(@ApiParam(value = "Id of the cart to delete", required = true) @PathVariable final String cartId) {
        incrementCounter();

        Optional<Cart> cart = cartRepository.findById(cartId);
        List<CartItem> cartItems = null;
        try {
            cartItems = cart.get().getCartItems();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cart not found"
            );
        }
        CartDTO cartDTO = new CartDTO(cart.get().getId(), cart.get().getCreationDate());
        List<CartItemDTO> cartItemDTOs = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        for (CartItem cartItem: cartItems) {
            CartItemDTO cartItemDTO = new CartItemDTO();

            final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId() +
                            "/products/" + cartItem.getProductId(),
                    HttpMethod.GET, entity, new ParameterizedTypeReference<String>() {
                    });

            Gson gson = new Gson();
            InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);

            cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.getInventoryId(), cartItem.isAvailable(),
                    cartItem.getCreationDate());

            cartDTO.addItems(cartItemDTO);
        }
        cartRepository.deleteById(cartId);
        return cartDTO;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @DeleteMapping("/{cartId}/items/{cartItemId}")
    @ApiOperation(value = "Delete a cart item from a cart", notes = "Provide an Id to delete a specific cart item of a cart from the Database")
    public CartDTO deleteCartItem(@ApiParam(value = "Id of the cart that contains the cart item to delete", required = true) @PathVariable final String cartId,
                              @ApiParam(value = "Id of the cart item to delete", required = true) @PathVariable final String cartItemId) {
        incrementCounter();
        Cart cart = null;
        try {
            cart = cartRepository.findById(cartId).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cart not found"
            );
        }
        try {
            cart.deleteFromCartItems(cartItemId);
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cart item not found"
            );
        }
        cart = cartRepository.save(cart);
        CartDTO cartDTO = new CartDTO(cart.getId(), cart.getCreationDate());
        List<CartItemDTO> cartItemDTOs = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);

        for (CartItem cartItem: cart.getCartItems()) {
            CartItemDTO cartItemDTO = new CartItemDTO();

            final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId() +
                            "/products/" + cartItem.getProductId(),
                    HttpMethod.GET, entity, new ParameterizedTypeReference<String>() {
                    });

            Gson gson = new Gson();
            InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);

            cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.getInventoryId(), cartItem.isAvailable(),
                    cartItem.getCreationDate());

            cartDTO.addItems(cartItemDTO);
        }
        cartRepository.deleteById(cartId);
        return cartDTO;
    }

    /*@PatchMapping("/{cartId}/inventoryId")
    @ApiOperation(value = "Change the inventoryId of a cart", notes = "Provide the new inventory id")
    public CartDTO changeInventoryId(@ApiParam(value = "Id of the inventory for which the inventoryId has to be changed", required = true) @PathVariable final String cartId,
                                         @ApiParam(value = "New inventoryId", required = true) @RequestBody Map<String, String> myJsonRequest) {
        incrementCounter();
        Optional<Cart> cart = cartRepository.findById(cartId);
        cart.get().setInventoryId(myJsonRequest.get("inventoryId").toString());
        List<CartItem> cartItems = null;
        try {
            cartItems = cart.get().getCartItems();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Cart not found"
            );
        }
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

            cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem.getQuantity(), cartItem.isAvailable());

            cartDTO.addItems(cartItemDTO);
        }
        cartRepository.save(cart.get());
        return cartDTO;
    }*/


    @PostMapping("")
    @ApiOperation(value = "Create a cart", notes = "Provide information to create a cart")
    public Cart createCart(@ApiParam(value = "Information of the cart to create", required = true) @RequestBody Cart cart) {
        incrementCounter();
        cart.setCreationDate(new Date());
        return cartRepository.save(cart);
    }

    // a fallback method to be called if failure happened
    public List<ProductDTO> fallback(String catalogId, Throwable hystrixCommand) {
        return new ArrayList<>();
    }

    //añadir producto a cart
    @PutMapping("/{cartId}")
    @ApiOperation(value = "Add certain quantity of an inventory product to cart", notes = "Add a product available in the inventory to a cart")
    public CartDTO addInventoryProductToCart(@ApiParam(value = "Id of the cart on which an inventory product has to be added", required = true) @PathVariable final String cartId,
                                      @ApiParam(value = "Product Id and quantity of items available in the inventory to be added to the given cart", required = true) @RequestBody CartItem cartItem) {
        incrementCounter();
        Optional<Cart> cart = cartRepository.findById(cartId);
        JSONObject obj = new JSONObject();
        obj.put("numItems", cartItem.getQuantity());
        // set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);
        // send request and parse result
        //añadir al carrito si hay numero suficiente de items del producto en el inventario y no existe ya en el carrito (si ya existe se suman items)
        final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId() +
                        "/products/" + cartItem.getProductId(),
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });

        Gson gson = new Gson();
        InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);

        //comprovar si hay stock disponible antes de añadir
        if (inventoryItemDTO.getQuantity() >= cartItem.getQuantity()) {
            //comprovar si ya existe y hay stock disponible antes de añadir
            boolean alreadyExists = false;
            for (CartItem cartItem1: cart.get().getCartItems()) {
                if (cartItem1.getProductId().equals(cartItem.getProductId())) {
                    alreadyExists = true;
                    if (inventoryItemDTO.getQuantity() >= (cartItem.getQuantity() + cartItem1.getQuantity())) {
                        cartItem1.setQuantity(cartItem.getQuantity() + cartItem1.getQuantity());
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
        //pasar cart a cartDTO
        Cart cart1 = cartRepository.save(cart.get());

        CartDTO cartDTO = new CartDTO(cart1.getId(), cart1.getCreationDate());
        List<CartItemDTO> cartItemDTOs = new ArrayList<>();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        entity = new HttpEntity<String>(null, headers);

        for (CartItem cartItem1: cart1.getCartItems()) {
            CartItemDTO cartItemDTO = new CartItemDTO();

            final ResponseEntity<String> res1 = restTemplate.exchange("http://inventory-service:8080/" + cartItem1.getInventoryId() +
                            "/products/" + cartItem1.getProductId(),
                    HttpMethod.GET, entity, new ParameterizedTypeReference<String>() {
                    });

            gson = new Gson();
            inventoryItemDTO = gson.fromJson(res1.getBody(), InventoryItemDTO.class);

            cartItemDTO = new CartItemDTO(inventoryItemDTO.getProduct(), cartItem1.getQuantity(), cartItem1.getInventoryId(), cartItem1.isAvailable(),
                    cartItem1.getCreationDate());

            cartDTO.addItems(cartItemDTO);
        }
        return cartDTO;
    }

    @RequestMapping(value = "/{cartId}/checkout", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Checkout a cart", notes = "Proceed to do the checkout of a given cart, paying with Paypal")
    public Object doCheckoutPart1(@ApiParam(value = "Id of the cart for which the checkout has to be done", required = true) @PathVariable final String cartId,
                                  @ApiParam(value = "Discount code, if any", required = false) @RequestBody (required=false) Map<String, String> discountCodeBody) {
        System.out.println(discountCodeBody);
        incrementCounter();
        Optional<Cart> cart = cartRepository.findById(cartId);
        double totalPrice = 0.0;

        //obtener precio total
        for (CartItem cartItem: cart.get().getCartItems()) {
            //provocar excepcion si alguno no esta disponible
            if (!cartItem.isAvailable())
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Some of the cart items are not available"
                );

            // obtener el precio de un producto * num items del producto
            final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId()  +
                            "/products/" + cartItem.getProductId(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                    });

            Gson gson = new Gson();
            InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);

            totalPrice += inventoryItemDTO.getProduct().getCurrentPrice() * (double) cartItem.getQuantity();

        }
        //formatear el precio total a dos decimales
        JSONObject obj = new JSONObject();
        DecimalFormat df = new DecimalFormat("#.##");
        totalPrice = Double.valueOf(df.format(totalPrice));
        System.out.println("discountCodeBody");
        //comprovar que el codigo de descuento, si hay, es valido, y aplicarlo
        if (discountCodeBody != null) {
            System.out.println("discountCodeBody");
            String discountCode = discountCodeBody.get("discountCode");
            final ResponseEntity<String> res;
            //consultar informacion del descuento Y COMPROVAR QUE EXISTE
            try {
                res = restTemplate.exchange("http://discount-service:8080/" + discountCode,
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });
            } catch (Exception e) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "The discount code does not exist"
                );
            }
            Gson gson = new Gson();
            DiscountDTO discountDTO = gson.fromJson(res.getBody(), DiscountDTO.class);
            boolean cont = true;
            if (discountDTO.getUsers() != null) {
                for (int i = 0; i < discountDTO.getUsers().size() && cont; ++i) {
                    if (discountDTO.getUsers().get(i).getAccountId().equals(cartId))
                        cont = false;
                }
                if (cont) {
                    //el usuario no esta entre los que pueden usar el cupon
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "User is not allowed to use the discount code"
                    );
                }
            }
            if (!discountDTO.getStartDate().before(new Date()) || !discountDTO.getEndDate().after(new Date()))
                //el cupon no esta activo en esta fecha
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "The discount code is not in its active date interval"
                );
            if (discountDTO.getMinimumAmount() > totalPrice)
                //no ha llegado al precio minimo para usar el cupon
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "The minimum amount of the purchase to use the discount code is not reached"
                );
            if (discountDTO.getCurrentUses() >= discountDTO.getMaxUses())
                //se ha llegado al limite de usos del cupon
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "The maximum uses of the discount code has been reached"
                );
            if (!discountDTO.isEnabled())
                //el cupon esta desactivado
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "The discount code is disabled due to technical issues"
                );

            //incrementar current uses
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            //headers.add("X-HTTP-Method-Override", "PATCH");
            HttpEntity<String> entity = new HttpEntity<String>(null, headers);
            final ResponseEntity<String> res1 = restTemplate.exchange("http://discount-service:8080/" + discountDTO.getId() + "/useDiscount",
                    HttpMethod.PUT, entity, new ParameterizedTypeReference<String>() {
                    });

            //reducir el precio total aplicando el descuento
            if (!discountDTO.getPercentage()) {
                totalPrice -= discountDTO.getValue();
            }
            else {
                double percentage = 100 - discountDTO.getValue();
                percentage /= 100;
                //ver si hay limite de descuento y aplicarlo si es necesario
                if ((discountDTO.getMaxDiscount() > 0.0) && ((totalPrice * (1 - percentage)) > discountDTO.getMaxDiscount()))
                    totalPrice -= discountDTO.getMaxDiscount();
                else
                    totalPrice *= percentage;
            }
            totalPrice = Double.valueOf(df.format(totalPrice));
        }

        //realizar el pago
        obj.put("totalPrice", totalPrice);
        System.out.println(obj.get("totalPrice"));
        // set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);
        final ResponseEntity<String> res = restTemplate.exchange("http://paypal-gateway-service:8080/make/payment/" + cartId,
                HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                });
        return res.getBody().toString();
    }

    @RequestMapping(value = "/{cartId}/checkout2", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Checkout a cart, 2nd part", notes = "Checkout logic to be executed after the payment has been made")
    public Object doCheckoutPart2(@ApiParam(value = "Id of the cart for which the second part of the checkout has to be done", required = true) @PathVariable final String cartId) {
        incrementCounter();
        Optional<Cart> cart = cartRepository.findById(cartId);
        /*final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://account-service/" + accountId + "/buy",
                HttpMethod.PUT, entity, new ParameterizedTypeReference<ProductDTO>() {
                });*/

        HttpHeaders headers = new HttpHeaders();
        //eliminar items del inventario
        for (CartItem cartItem: cart.get().getCartItems()) {
            JSONObject obj = new JSONObject();
            obj.put("quantity", cartItem.getQuantity());
            // set headers
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);
            // reducir el numero de items de un producto en el inventario
            final ResponseEntity<ProductDTO> res1 = restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId() +
                            "/products/" + cartItem.getProductId() + "/reduceStock",
                    HttpMethod.PUT, entity, new ParameterizedTypeReference<ProductDTO>() {
                    });
        }

        //crear pedido (order)
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Cart> orderEntity = new HttpEntity<Cart>(cart.get(), headers);
        final ResponseEntity<OrderDTO> res1 = restTemplate.exchange("http://order-service:8080",
                HttpMethod.POST, orderEntity, new ParameterizedTypeReference<OrderDTO>() {
                });
        System.out.println(res1.getBody().getId());

        //vaciar carrito
        cart = cartRepository.findById(cartId);
        cart.get().setCartItems(new ArrayList<>());
        cartRepository.save(cart.get());

        //llamar al micro de transporte
        // set headers
        JSONObject obj = new JSONObject();
        obj.put("orderId", res1.getBody().getId());
        //obtener direccion de entrega del usuario
        final ResponseEntity<String> res4 = restTemplate.exchange("http://account-service:8080/" + cartId,
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        System.out.println(res4.toString());
        Gson gson = new Gson();
        AccountDTO accountDTO = gson.fromJson(res4.getBody(), AccountDTO.class);
        obj.put("deliveryAddress", accountDTO.getDeliveryAddress());
        System.out.println(res1.getBody().getId());
        System.out.println(accountDTO.getDeliveryAddress());
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
        final ResponseEntity<OrderDTO> res3 = restTemplate.exchange("http://order-service:8080/" + res1.getBody().getId() + "/deliveryId",
                HttpMethod.PUT, entity, new ParameterizedTypeReference<OrderDTO>() {
                });

        //enviar email de confirmacion de pedido
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DeliveryDTO> deliveryEntity = new HttpEntity<DeliveryDTO>(res2.getBody(), headers);
        //enviar mail de update
        final ResponseEntity<String> res6 = restTemplate.exchange("http://account-service:8080/" + cartId + "/orderSuccessEmail",
                HttpMethod.POST, deliveryEntity, new ParameterizedTypeReference<String>() {
                });

        //retornar envio
        //return res2.getBody();
        JSONObject jo = new JSONObject();
        jo.put("paymentStatus", "success");
        jo.put("orderURI", "/orders/" + res1.getBody().getId());
        return jo.toString();
    }

    @PutMapping("/update")
    @ApiOperation(value = "Update the availability of all carts", notes = "Update all inventory products of the carts in function of " +
            "the available items on the inventory")
    public void updateCartsAvailability() {
        incrementCounter();
        updateAvailability();
    }

    //retorna falso si hay algun producto no disponible
    private void updateAvailability() {
        for (Cart cart: cartRepository.findAll()) {
            for (CartItem cartItem : cart.getCartItems()) {
                final ResponseEntity<String> res = restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId() +
                                "/products/" + cartItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                        });

                Gson gson = new Gson();
                InventoryItemDTO inventoryItemDTO = gson.fromJson(res.getBody(), InventoryItemDTO.class);
                if (inventoryItemDTO.getQuantity() >= cartItem.getQuantity())
                    cartItem.setAvailable(true);
                else {
                    cartItem.setAvailable(false);
                }
            }
            cartRepository.save(cart);
        }
    }

    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of Cart service running at port: " + env.getProperty("local.server.port");
    }
}
