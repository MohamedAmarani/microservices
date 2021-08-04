package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import javax.annotation.PostConstruct;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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
    @ApiOperation(value = "Get all products", notes = "Retrieve all products from the Database")
    public ResponseEntity<Map<String, Object>> getProducts(@RequestParam(defaultValue = "", required = false) String name,
                                                           @RequestParam(defaultValue = "", required = false) String description,
                                                           @RequestParam(defaultValue = "", required = false) String color,
                                                           @RequestParam(defaultValue = "0", required = false) double minOriginalPrice,
                                                           @RequestParam(defaultValue = "999999", required = false) double maxOriginalPrice,
                                                           @RequestParam(defaultValue = "0", required = false) double minCurrentPrice,
                                                           @RequestParam(defaultValue = "999999", required = false) double maxCurrentPrice,
                                                           @RequestParam(defaultValue = "", required = false) String productSize,
                                                           @RequestParam(defaultValue = "", required = false) String type,
                                                           @RequestParam(defaultValue = "", required = false) String sex,
                                                           @RequestParam(defaultValue = "1970-01-01T00:00:0.000+00:00", required = false) Date minCreationDate,
                                                           @RequestParam(defaultValue = "today", required = false) Date maxCreationDate,
                                                           @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                                           @RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                           @RequestParam(value = "sort", defaultValue = "creationDate,asc", required = false) String sort) {
        incrementCounter();
        List<Product> products;
        PageRequest request = PageRequest.of(page, size, Sort.by(new Sort.Order(sort.split(",")[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(",")[0])));
        Page<Product> pagedProducts = productRepository.findByNameContainingIgnoreCaseAndDescriptionContainingIgnoreCaseAndColorContainingIgnoreCaseAndSizeContainingIgnoreCaseAndTypeContainingIgnoreCaseAndSexContainingIgnoreCaseAndOriginalPriceBetweenAndCurrentPriceBetweenAndCreationDateBetween(name, description, color, productSize, type, sex, minOriginalPrice, maxOriginalPrice, minCurrentPrice, maxCurrentPrice, minCreationDate, maxCreationDate, request);

        products = pagedProducts.getContent();
        Map<String, Object> response = new HashMap<>();
        response.put("currentPage", pagedProducts.getNumber());
        response.put("totalItems", pagedProducts.getTotalElements());
        response.put("totalPages", pagedProducts.getTotalPages());
        response.put("products", products);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("")
    @ApiOperation(value = "Create a product", notes = "Provide information to create a product")
    public Product postProduct(@ApiParam(value = "Product to create", required = true) @RequestBody Product product) {
        incrementCounter();
        try{
            product.setCurrentPrice(product.getOriginalPrice());
            product.setCreationDate(new Date());
            return productRepository.save(product);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Product already exists"
            );
        }
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Get a product", notes = "Provide the Id of the specific product to retrieve from the Database")
    public Product getProduct(@ApiParam(value = "Id of the product to get", required = true) @PathVariable final String id) throws Exception {
        Product product = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            product = productRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"
            );
        }
        incrementCounter();
        return product;
    }

    @PatchMapping("/{id}/description")
    @ApiOperation(value = "Change a product description", notes = "Provide the Id of the product for which the description has to be changed")
    public Product patchProductDescription(@ApiParam(value = "Id of the product for which the description has to be changed", required = true) @PathVariable final String id,
                                            @ApiParam(value = "New product description", required = true) @RequestBody Map<String, String> newProductDescription) throws Exception {
        incrementCounter();
        Product product = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            product = productRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"
            );
        }
        product.setDescription(newProductDescription.get("newDescription"));
        product = productRepository.save(product);
        return product;
    }

    @PatchMapping("/{id}/color")
    @ApiOperation(value = "Change a product color", notes = "Provide the Id of the product for which the color has to be changed")
    public Product patchProductColor(@ApiParam(value = "Id of the product for which the color has to be changed", required = true) @PathVariable final String id,
                                            @ApiParam(value = "New product color", required = true) @RequestBody Map<String, String> newProductColor) throws Exception {
        incrementCounter();
        Product product = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            product = productRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"
            );
        }
        product.setColor(newProductColor.get("newColor"));
        product = productRepository.save(product);
        return product;
    }

    @PatchMapping("/{id}/size")
    @ApiOperation(value = "Change a product size", notes = "Provide the Id of the product for which the size has to be changed")
    public Product patchProductSize(@ApiParam(value = "Id of the product for which the size has to be changed", required = true) @PathVariable final String id,
                                            @ApiParam(value = "New product size", required = true) @RequestBody Map<String, String> newProductSize) throws Exception {
        incrementCounter();
        Product product = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            product = productRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"
            );
        }
        try {
            product.setSizeFromString(newProductSize.get("newSize"));
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Not a valid size"
            );
        }
        product = productRepository.save(product);
        return product;
    }

    @PatchMapping("/{id}/type")
    @ApiOperation(value = "Change a product current price", notes = "Provide the Id of the product for which the type has to be changed")
    public Product patchProductType(@ApiParam(value = "Id of the product for which the type has to be changed", required = true) @PathVariable final String id,
                                            @ApiParam(value = "New product type", required = true) @RequestBody Map<String, String> newProductType) throws Exception {
        incrementCounter();
        Product product = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            product = productRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"
            );
        }
        try {
            product.setTypeFromString(newProductType.get("newType"));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Not a valid type"
            );
        }
        product = productRepository.save(product);
        return product;
    }

    @PatchMapping("/{id}/sex")
    @ApiOperation(value = "Change a product sex", notes = "Provide the Id of the product for which the sex has to be changed")
    public Product patchProductSex(@ApiParam(value = "Id of the product for which the type has to be changed", required = true) @PathVariable final String id,
                                            @ApiParam(value = "New product sex", required = true) @RequestBody Map<String, String> newProductSex) throws Exception {
        incrementCounter();
        Product product = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            product = productRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"
            );
        }
        try {
            product.setSexFromString(newProductSex.get("newSex"));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Not a valid sex"
            );
        }
        product = productRepository.save(product);
        return product;
    }

    @PatchMapping("/{id}/currentPrice")
    @ApiOperation(value = "Change a product current price", notes = "Provide the Id of the product for which the current price has to be changed")
    public Product patchProductRegularPrice(@ApiParam(value = "Id of the product to get", required = true) @PathVariable final String id,
                                     @ApiParam(value = "New product price", required = true) @RequestBody Map<String, Double> newProductPrice) throws Exception {
        incrementCounter();
        Map<String, String> updatedProductInfo = new HashMap<>();
        Product product = null;
        //si no existe ningun producto con ese id retornamos null
        try {
            product = productRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"
            );
        }
        //meter precio antiguo en el map
        DecimalFormat df = new DecimalFormat("#.##");
        //update solo 2 decimales
        double oldPrice = product.getCurrentPrice();
        updatedProductInfo.put("oldPrice", df.format(product.getCurrentPrice()));
        product.setCurrentPrice(newProductPrice.get("newPrice"));
        product = productRepository.save(product);
        //avisar a las wishlist que lo tengan si el precio nuevo es menor al precio anterior
        if (oldPrice > newProductPrice.get("newPrice")) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            updatedProductInfo.put("productId", id);
            //update solo 2 decimales
            updatedProductInfo.put("newPrice", df.format(product.getCurrentPrice()));
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

    @GetMapping("/admin")
    public String getAdmin() {
        incrementCounter();
        return "This is the admin area of product service running at port: " + env.getProperty("local.server.port");
    }
}
