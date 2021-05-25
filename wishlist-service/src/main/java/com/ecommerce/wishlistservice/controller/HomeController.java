package com.ecommerce.wishlistservice.controller;

import com.ecommerce.wishlistservice.model.Wishlist;
import com.ecommerce.wishlistservice.model.WishlistItem;
import com.ecommerce.wishlistservice.repository.WishlistItemRepository;
import com.ecommerce.wishlistservice.repository.WishlistRepository;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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

    @GetMapping("/hello")
    public ResponseEntity<String> getHello() {
        incrementCounter();
        System.out.println(message);
        System.out.println(env.getProperty("message"));
        return new ResponseEntity<String>( env.getProperty("message"), HttpStatus.OK);
    }

    @GetMapping("/info")
    @ApiOperation(value = "Get information from the wishlist-service instance", notes = "Retrieve information from a cart-service instance")
    public String home() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Wishlist Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @GetMapping("")
    public List<Wishlist> getWishlists() {
        incrementCounter();
        Wishlist wishlist;
        return wishlistRepository.findAll();
    }

    @GetMapping("/{wishlistId}")
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

    @PutMapping("/priceReduced")
    public void patchPriceReducedWishlist(@ApiParam(value = "Discount code, if any", required = true) @RequestBody Map<String, String> updatedProductInfo) {
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
                        updatedProductInfo.put("productId", wishlistItem.getProductId());
                        updatedProductInfo.put("oldPrice", updatedProductInfo.get("oldPrice"));
                        DecimalFormat df = new DecimalFormat("#.##");
                        //update solo 2 decimales
                        updatedProductInfo.put("targetPrice", df.format(wishlistItem.getTargetPrice()));
                        HttpEntity<Map<String, String>> entity = new HttpEntity<Map<String, String>>(updatedProductPriceInfo, headers);
                        final ResponseEntity<String> res1 = restTemplate.exchange("http://account-service:8080/" + wishlist.getId() + "/reachedTargetPriceEmail",
                            HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                                });
                    }
    }

    @PostMapping("")
    public Wishlist postWishlist(@ApiParam(value = "Information of the wishlist to create", required = true) @RequestBody (required = false) Wishlist wishlist) {
        incrementCounter();
        try {
            wishlist = wishlistRepository.save(wishlist);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Wishlist not created"
            );
        }
        return wishlist;
    }

    @DeleteMapping("/{wishlistId}")
    public Wishlist deleteWishlist(@ApiParam(value = "Id of the discount that wants to be used", required = true) @PathVariable final String wishlistId) {
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

    //@ApiParam(value = "Discount code, if any", required = true) @RequestBody Map<String, String> wishlistItemIdBody
    @DeleteMapping("/{wishlistId}/items/{wishlistItemId}")
    public Wishlist deleteWishlistItem(@ApiParam(value = "Id of the discount that wants to be used", required = true) @PathVariable final String wishlistId,
                                       @ApiParam(value = "Id of the discount that wants to be used", required = true) @PathVariable final String wishlistItemId) {
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
            //wishlist.deleteFromWishlistItems(wishlistItemId);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Wishlist item not found in the given wishlist"
            );
        }
        wishlist = wishlistRepository.save(wishlist);
        return wishlist;
    }

    @PutMapping("/{wishlistId}")
    public Wishlist addWishlistItemToWishlist(@ApiParam(value = "Id of the discount that wants to be used", required = true) @PathVariable final String wishlistId,
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
        wishlist.addToWishlistItems(wishlistItem);
        wishlist = wishlistRepository.save(wishlist);
        return wishlist;
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    // We'll add the logic of role based auth later
    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of Wishlist service running at port: " + env.getProperty("local.server.port");
    }
}
