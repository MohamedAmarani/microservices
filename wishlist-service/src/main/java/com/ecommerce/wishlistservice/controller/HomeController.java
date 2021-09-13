package com.ecommerce.wishlistservice.controller;

import com.ecommerce.wishlistservice.model.Wishlist;
import com.ecommerce.wishlistservice.model.WishlistItem;
import com.ecommerce.wishlistservice.repository.WishlistItemRepository;
import com.ecommerce.wishlistservice.repository.WishlistRepository;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
    private WishlistRepository wishlistRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

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
    @ApiOperation(value = "Get information from the wishlist-service instance", notes = "Retrieve information from a cart-service instance")
    public String home() {
        incrementCounter();
        return "Hello from Wishlist Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("")
    @ApiOperation(value = "Get all wishlists", notes = "Retrieve all wishlists from the Database")
    public ResponseEntity<Map<String, Object>> getWishlists(@RequestParam(defaultValue = "", required = false) String productId,
                                                            @RequestParam(defaultValue = "", required = false) String targetPrice,
                                                            @RequestParam(defaultValue = "1970-01-01T00:00:0.000+00:00", required = false) Date minCreationDate,
                                                            @RequestParam(defaultValue = "2024-01-01T00:00:0.000+00:00", required = false) Date maxCreationDate,
                                                            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                                            @RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                            @RequestParam(value = "sort", defaultValue = "creationDate,asc", required = false) String sort) {
        incrementCounter();
        List<Wishlist> wishlists;
        PageRequest request = PageRequest.of(page, 99999, Sort.by(new Sort.Order(sort.split(",")[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(",")[0])));
        Page<Wishlist> pagedWishlists = wishlistRepository.findByCreationDateBetween(minCreationDate, maxCreationDate, request);
        List<Wishlist> list = new ArrayList<>();

        //seleccionar solo los wishlist que contengan algun item con productId que coincida con el especificado
        if (!productId.equals("")) {
            //solo las que tengan el productId especificado
            for (int i = 0; i < pagedWishlists.getContent().size(); ++i) {
                boolean found = false;
                for (int j = 0; !found && j < pagedWishlists.getContent().get(i).getWishlistItems().size(); ++j) {
                    if (pagedWishlists.getContent().get(i).getWishlistItems().get(j).getProductId().equals(productId))
                        found = true;
                }
                if (found) {
                    //seleccionar solo los wishlist que contengan algun item con targetPrice que coincida con el especificado
                    if (!targetPrice.equals("")) {
                        //solo las que tengan el targetPrice especificado
                        found = false;
                        for (int j = 0; !found && j < pagedWishlists.getContent().get(i).getWishlistItems().size(); ++j) {
                            if (pagedWishlists.getContent().get(i).getWishlistItems().get(j).getTargetPrice() == Double.parseDouble(targetPrice))
                                found = true;
                        }
                        if (found)
                            list.add(pagedWishlists.getContent().get(i));
                    }
                    else
                        list.add(pagedWishlists.getContent().get(i));
                    }
                }
            pagedWishlists = new PageImpl<>(list, PageRequest.of(page, size), list.size());
        }
        else if (!targetPrice.equals("")) {
            //solo las que tengan el targetPrice especificado
            for (int i = 0; i < pagedWishlists.getContent().size(); ++i) {
                boolean found = false;
                for (int j = 0; !found && j < pagedWishlists.getContent().get(i).getWishlistItems().size(); ++j) {
                    if (pagedWishlists.getContent().get(i).getWishlistItems().get(j).getTargetPrice() == Double.parseDouble(targetPrice))
                        found = true;
                }
                if (found)
                    list.add(pagedWishlists.getContent().get(i));
            }
            pagedWishlists = new PageImpl<>(list, PageRequest.of(page, size), list.size());
        }
        else {
            request = PageRequest.of(page, size, Sort.by(new Sort.Order(sort.split(",")[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(",")[0])));
            pagedWishlists = wishlistRepository.findByCreationDateBetween(minCreationDate, maxCreationDate, request);
        }

        wishlists = pagedWishlists.getContent();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("currentPage", pagedWishlists.getNumber());
        response.put("totalItems", pagedWishlists.getTotalElements());
        response.put("totalPages", pagedWishlists.getTotalPages());
        response.put("wishlists", wishlists);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/{wishlistId}")
    @ApiOperation(value = "Get a cart", notes = "Provide an Id to retrieve a specific wishlist from the Database and all its wishlist items")
    public Wishlist getWishlist(@ApiParam(value = "Id of the discount that wants to be used", required = true) @PathVariable final String wishlistId) {
        incrementCounter();
        Wishlist wishlist;
        try {
        wishlist = wishlistRepository.findById(wishlistId).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Wishlist not found"
            );
        }
        return wishlist;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @PutMapping("/priceReduced")
    @ApiOperation(value = "Notify users when one of their wishlist items price is within their target price", notes = "When the price of a product gets reduced, call the account-service to notify all users" +
            "that have it in their wishlist and the new product price is withing their specified target price")
    public void patchPriceReducedWishlist(@ApiParam(value = "Information of the product whose price has been reduced", required = true) @RequestBody Map<String, String> updatedProductInfo) {
        incrementCounter();
        List<Wishlist> wishlistList;
        try {
            wishlistList = wishlistRepository.findAll();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Wishlist not found"
            );
        }
        //iterar sobre todos los wishlist
        for (Wishlist wishlist: wishlistList)
            //iterar sobre todos los items de cada wishlist
            for (WishlistItem wishlistItem: wishlist.getWishlistItems())
                if (wishlistItem.getProductId().equals(updatedProductInfo.get("productId")))
                    if (wishlistItem.getTargetPrice() >= Double.parseDouble(updatedProductInfo.get("newPrice"))) {
                    //enviar corro a propietario de la wishlist
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        Map<String, String> updatedProductPriceInfo = new HashMap<>();
                        updatedProductPriceInfo.put("productId", wishlistItem.getProductId());

                        updatedProductPriceInfo.put("oldPrice", updatedProductInfo.get("oldPrice"));
                        DecimalFormat df = new DecimalFormat("#.##");
                        //update solo 2 decimales
                        updatedProductPriceInfo.put("targetPrice", df.format(wishlistItem.getTargetPrice()));
                        HttpEntity<Map<String, String>> entity = new HttpEntity<Map<String, String>>(updatedProductPriceInfo, headers);
                        final ResponseEntity<String> res1 = restTemplate.exchange("http://account-service:8080/" + wishlist.getId() + "/reachedTargetPriceEmail",
                            HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                                });
                    }
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @PostMapping("")
    @ApiOperation(value = "Create a wishlist", notes = "Provide information to create a wishlist in the database")
    public Wishlist postWishlist(@ApiParam(value = "Information of the wishlist to create", required = true) @RequestBody (required = false) Wishlist wishlist) {
        incrementCounter();
        wishlist.setCreationDate(new Date());
        try {
            wishlist = wishlistRepository.save(wishlist);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Wishlist not created"
            );
        }
        return wishlist;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @DeleteMapping("/{wishlistId}")
    @ApiOperation(value = "Delete a wishlist", notes = "Provide the Id of the wishlist that has to be deleted from the database")
    public Wishlist deleteWishlist(@ApiParam(value = "Id of the wishlist that has to be deleted", required = true) @PathVariable final String wishlistId) {
        incrementCounter();
        Wishlist wishlist;
        try {
            wishlist = wishlistRepository.findById(wishlistId).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Wishlist not found"
            );
        }
        wishlistRepository.deleteById(wishlistId);
        return wishlist;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @DeleteMapping("/{wishlistId}/items/{wishlistItemProductId}")
    @ApiOperation(value = "Delete a wishlist item", notes = "Provide the Id of the wishlist that that contains the wishlist item that has to be deleted from the database")
    public Wishlist deleteWishlistItem(@ApiParam(value = "Id of the wishlist that contains the wishlist item to delete", required = true) @PathVariable final String wishlistId,
                                   @ApiParam(value = "Id of the wishlist item to delete", required = true) @PathVariable final String wishlistItemProductId) {
        incrementCounter();
        Wishlist wishlist;
        try {
            wishlist = wishlistRepository.findById(wishlistId).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Wishlist not found"
            );
        }
        try {
            wishlist.deleteFromWishlistItems(wishlistItemProductId);
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Wishlist item not found"
            );
        }
        wishlist = wishlistRepository.save(wishlist);
        return wishlist;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @PutMapping("/{wishlistId}")
    @ApiOperation(value = "Add a wishlist item to a wishlist", notes = "Provide the Id of the wishlist where a wishlist item has to be added and the information" +
            "of the wishlist item to add")
    public Wishlist addWishlistItemToWishlist(@ApiParam(value = "Id of the wishlist where a wishlist item has to be added", required = true) @PathVariable final String wishlistId,
                                              @ApiParam(value = "Information of the wishlist item to be added", required = true) @RequestBody WishlistItem wishlistItem) {
        incrementCounter();
        Wishlist wishlist;
        try {
            wishlist = wishlistRepository.findById(wishlistId).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Wishlist not found"
            );
        }
        //comprovar que no existe ya otro wishlistitem con este productId en esta wishlist
        for (WishlistItem wishlistItem1: wishlist.getWishlistItems()) {
            if (wishlistItem1.getProductId().equals(wishlistItem.getProductId()))
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "The product is already in the wishlist"
                );
        }
        //una vez hechas las comprobaciones, añado el wishlistitem a la wishlist
        wishlist.addToWishlistItems(wishlistItem);
        wishlist = wishlistRepository.save(wishlist);
        return wishlist;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @PatchMapping("/{wishlistId}/items/{wishlistItemProductId}/targetPrice")
    @ApiOperation(value = "Change the target price of a wishlist item", notes = "Provide the Id of the wishlist where a wishlist item target price has to be changed" +
            "of the wishlist item to add")
    public Wishlist changeTargetPriceOfWishlistItem(@ApiParam(value = "Id of the wishlist where a wishlist item target price has to be changed", required = true) @PathVariable final String wishlistId,
                                                    @ApiParam(value = "Id of the productId of the wishlist item for which the target price has to be changed", required = true) @PathVariable final String wishlistItemProductId,
                                                    @ApiParam(value = "New target price for the wishlist item", required = true) @RequestBody Map<String, Double> newTargetPrice) {
        incrementCounter();
        Wishlist wishlist;
        try {
            wishlist = wishlistRepository.findById(wishlistId).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Wishlist not found"
            );
        }
        //comprovar que no existe ya otro wishlistitem con este productId en esta wishlist
        for (WishlistItem wishlistItem1: wishlist.getWishlistItems()) {
            if (wishlistItem1.getProductId().equals(wishlistItemProductId)) {
                wishlistItem1.setTargetPrice(newTargetPrice.get("newTargetPrice"));
                wishlist = wishlistRepository.save(wishlist);
                return wishlist;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Wishlist item not found in the wishlist"
        );
    }

    // el metodo fallback a llamar si ocurre algun fallo
    public List<Wishlist> fallback(String wishlistId, Throwable hystrixCommand) {
        return new ArrayList<>();
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of Wishlist service running at port: " + env.getProperty("local.server.port");
    }
}
