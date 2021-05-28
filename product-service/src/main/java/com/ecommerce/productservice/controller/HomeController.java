package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RefreshScope
@RestController
@RequestMapping("/")
@Service
public class HomeController {
    @Autowired
    private Environment env;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Value("${message:Hello default}")
    private String message;

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @Autowired
    private DiscoveryClient discoveryClient;

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
        return new ResponseEntity<String>(env.getProperty("message"), HttpStatus.OK);
    }

    @GetMapping("/info")
    @ApiOperation(value = "Get information from the product-service instance", notes = "Retrieve information from a product-service instance")
    public String getInfo() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of product service running at different ports.
        // We load balance among them, and display which instance received the request.
        int counter = 0;
        for (String key: requestsLastMinute.keySet()) {
            counter += requestsLastMinute.get(key);
        }
        return "Hello from Product Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId + " " + counter;
    }

    @GetMapping("")
    @ApiOperation(value = "Get all products", notes = "Retrieve a specific product from the Database")
    public List<Product> getProducts() {
        incrementCounter();
        List<Product> products = productRepository.findAll();
        return products;
    }

    @PostMapping("")
    @ApiOperation(value = "Create a product", notes = "Provide information to create a product")
    public Product postProduct(@ApiParam(value = "Product to create", required = true) @RequestBody Product product) {
        incrementCounter();
        return productRepository.save(product);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Get a product", notes = "Provide the Id of the specific product to retrieve from the Database")
    public Product getProduct(@ApiParam(value = "Id of the product to get", required = true) @PathVariable final String id) throws Exception {
        Product product = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            //throw new Exception("Images can't be fetched");
            product = productRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"
            );
        }
        incrementCounter();
        return product;
    }

    @PatchMapping("/{id}/price")
    @ApiOperation(value = "Change a product price", notes = "Provide the Id of the product for which the price has to be changed")
    public Product patchProductPrice(@ApiParam(value = "Id of the product to get", required = true) @PathVariable final String id,
                                     @ApiParam(value = "New product price", required = true) @RequestBody Map<String, Double> newProductPrice) throws Exception {
        incrementCounter();
        Map<String, String> updatedProductInfo = new HashMap<>();
        Product product = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            //throw new Exception("Images can't be fetched");
            product = productRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"
            );
        }
        //meter precio antiguo en el map
        DecimalFormat df = new DecimalFormat("#.##");
        //update solo 2 decimales
        double oldPrice = product.getPrice();
        updatedProductInfo.put("oldPrice", df.format(product.getPrice()));
        product.setPrice(newProductPrice.get("newPrice"));
        product = productRepository.save(product);
        //avisar a las wishlist que lo tengan si el precio nuevo es menor al actual
        if (oldPrice > newProductPrice.get("newPrice")) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            updatedProductInfo.put("productId", id);
            //update solo 2 decimales
            updatedProductInfo.put("newPrice", df.format(product.getPrice()));
            HttpEntity<Map<String, String>> entity = new HttpEntity<Map<String, String>>(updatedProductInfo, headers);
            final ResponseEntity<Map<String, String>> res = restTemplate.exchange("http://wishlist-service:8080/priceReduced",
                    HttpMethod.PUT, entity, new ParameterizedTypeReference<Map<String, String>>() {
                    });
        }

        return product;
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Get a product", notes = "Provide an Id to delete a specific product from the Database")
    public Product deleteProduct(@ApiParam(value = "Id of the product to delete", required = true) @PathVariable final String id) throws Exception {
        Product product = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            //throw new Exception("Images can't be fetched");
            product = productRepository.findById(id).get();
            productRepository.deleteById(id);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"
            );
        }
        incrementCounter();
        return product;
    }

    @GetMapping("/na")
    public String getNa() {
        incrementCounter();
        return "holas";
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    // We'll add the logic of role based auth later
    @GetMapping("/admin")
    public String getAdmin() {
        incrementCounter();
        return "This is the admin area of Product service running at port: " + env.getProperty("local.server.port");
    }
}
