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
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.text.DateFormat;
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
    @ApiOperation(value = "Get all carts", notes = "Retrieve all carts from the Database")
    public ResponseEntity<Map<String, Object>> getCarts(@RequestParam(defaultValue = "", required = false) String productId,
                                                        @RequestParam(defaultValue = "", required = false) String inventoryId,
                                                        @RequestParam(defaultValue = "1970-01-01T00:00:0.000+00:00", required = false) Date minCreationDate,
                                                        @RequestParam(defaultValue = "2024-01-01T00:00:0.000+00:00", required = false) Date maxCreationDate,
                                                        @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                                        @RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                        @RequestParam(value = "sort", defaultValue = "creationDate,asc", required = false) String sort) {
        incrementCounter();
        PageRequest request = PageRequest.of(page, size, Sort.by(new Sort.Order(sort.split(",")[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(",")[0])));
        Page<Cart> pagedCarts = cartRepository.findByCreationDateBetween(minCreationDate, maxCreationDate, request);
        List<Cart> list = new ArrayList<>();

        //seleccionar solo los carritos con algun productId como el especificado
        if (!productId.equals("")) {
            //solo las que tengan el productId si se ha especificado
            for (int i = 0; i < pagedCarts.getContent().size(); ++i) {
                boolean found = false;
                for (int j = 0; !found && j < pagedCarts.getContent().get(i).getCartItems().size(); ++j) {
                    if (pagedCarts.getContent().get(i).getCartItems().get(j).getProductId().equals(productId))
                        found = true;
                }
                if (found) {
                    //seleccionar solo los carritos con algun catalogId como el especificado
                    if (!inventoryId.equals("")) {
                        //solo las que tengan el productId si se ha especificado
                        found = false;
                        for (int j = 0; !found && j < pagedCarts.getContent().get(i).getCartItems().size(); ++j) {
                            if (pagedCarts.getContent().get(i).getCartItems().get(j).getInventoryId().equals(inventoryId))
                                found = true;
                        }
                        if (found)
                            list.add(pagedCarts.getContent().get(i));
                    }
                    else
                        list.add(pagedCarts.getContent().get(i));
                    }
                }
            pagedCarts = new PageImpl<>(list, PageRequest.of(page, size), list.size());
        }
        else if (!inventoryId.equals("")) {
            //solo las que tengan el productId si se ha especificado
            for (int i1 = 0; i1 < pagedCarts.getContent().size(); ++i1) {
                boolean found = false;
                for (int j = 0; !found && j < pagedCarts.getContent().get(i1).getCartItems().size(); ++j) {
                    if (pagedCarts.getContent().get(i1).getCartItems().get(j).getInventoryId().equals(inventoryId))
                        found = true;
                }
                if (found) {
                    list.add(pagedCarts.getContent().get(i1));
                }
            }
            pagedCarts = new PageImpl<>(list, PageRequest.of(page, size), list.size());
        }

        List<CartDTO> result = new ArrayList<>();

        List<Cart> carts = pagedCarts.getContent();
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

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("currentPage", pagedCarts.getNumber());
        response.put("totalItems", pagedCarts.getTotalElements());
        response.put("totalPages", pagedCarts.getTotalPages());
        response.put("carts", result);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/{cartId}")
    @ApiOperation(value = "Get a cart", notes = "Provide an Id to retrieve a specific cart from the Database and all its cart items")
    public CartDTO getCart(@ApiParam(value = "Id of the cart to get", required = true) @PathVariable final String cartId) {
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

        // set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity;
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
        incrementCounter();
        Optional<Cart> cart = cartRepository.findById(cartId);
        double originalPrice = 0.0;
        String dC = "";
        double discountedAmount = 0.0;
        double deliveryFreeMinimumPrice = 30.0;
        double deliveryPrice = 2.9;
        double finalPrice = 0.0;

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

            finalPrice += inventoryItemDTO.getProduct().getCurrentPrice() * (double) cartItem.getQuantity();

        }
        //formatear el precio total a dos decimales
        JSONObject obj = new JSONObject();
        DecimalFormat df = new DecimalFormat("#.##");
        finalPrice = Double.valueOf(df.format(finalPrice));
        originalPrice = finalPrice;
        //comprovar que el codigo de descuento, si hay, es valido, y aplicarlo
        if (discountCodeBody != null) {
            String discountCode = discountCodeBody.get("discountCode");
            dC = discountCode;
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
            if (discountDTO.getMinimumAmount() > finalPrice)
                //no ha llegado al precio minimo para usar el cupon
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "The minimum amount of the purchase to use the discount code is not reached"
                );
            if (discountDTO.getCurrentUses() >= discountDTO.getMaxUses())
                //se ha llegado al limite de usos del cupon
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "The maximum number of uses of the discount code has been reached"
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
                finalPrice -= discountDTO.getValue();
                discountedAmount = discountDTO.getValue();
            }
            else {
                double percentage = 100 - discountDTO.getValue();
                percentage /= 100;
                //ver si hay limite de descuento y aplicarlo si es necesario
                if ((discountDTO.getMaxDiscount() > 0.0) && ((finalPrice * (1 - percentage)) > discountDTO.getMaxDiscount())) {
                    finalPrice -= discountDTO.getMaxDiscount();
                    discountedAmount = discountDTO.getMaxDiscount();
                }
                else {
                    finalPrice *= percentage;
                    finalPrice = Double.valueOf(df.format(finalPrice));
                    discountedAmount = originalPrice - finalPrice;
                }
            }
            finalPrice = Double.valueOf(df.format(finalPrice));
        }

        //sumar gastos de envio si el el precio del pedido es menor a minimo establecido para el envio gratuito
        if (finalPrice < deliveryFreeMinimumPrice)
            finalPrice += deliveryPrice;

        //realizar el pago
        obj.put("originalPrice", Double.toString(originalPrice));
        obj.put("discountCode", dC);
        obj.put("discountedAmount", Double.toString(discountedAmount));
        if (finalPrice < deliveryFreeMinimumPrice)
            obj.put("deliveryPrice", Double.toString(deliveryPrice));
        else
            obj.put("deliveryPrice", "0.0");
        obj.put("finalPrice", Double.toString(finalPrice));
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

        HttpHeaders headers;

        //aumetar el numero de ventas de cada producto del carrito
        for (CartItem cartItem: cart.get().getCartItems()) {
            JSONObject obj = new JSONObject();
            obj.put("quantity", cartItem.getQuantity());
            // set headers
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);
            restTemplate.exchange("http://product-service:8080/" + cartItem.getProductId() + "/sales",
                    HttpMethod.PUT, entity, new ParameterizedTypeReference<ProductDTO>() {
                    });
        }

        //eliminar items del inventario
        for (CartItem cartItem: cart.get().getCartItems()) {
            JSONObject obj = new JSONObject();
            obj.put("quantity", cartItem.getQuantity());
            // set headers
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);
            // reducir el numero de items de un producto en el inventario
            restTemplate.exchange("http://inventory-service:8080/" + cartItem.getInventoryId() +
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

        //vaciar carrito
        cart = cartRepository.findById(cartId);
        cart.get().setCartItems(new ArrayList<>());
        cartRepository.save(cart.get());

        //llamar al micro de transporte y pasarle la deliveryAddress del usuario
        // set headers
        JSONObject obj = new JSONObject();
        obj.put("orderId", res1.getBody().getId());
        //obtener direccion de entrega del usuario
        final ResponseEntity<String> res4 = restTemplate.exchange("http://account-service:8080/" + cartId,
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        Gson gson = new Gson();
        AccountDTO accountDTO = gson.fromJson(res4.getBody(), AccountDTO.class);
        obj.put("deliveryAddress", accountDTO.getDeliveryAddress());

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

        //enviar email de confirmacion del pedido realizado
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
