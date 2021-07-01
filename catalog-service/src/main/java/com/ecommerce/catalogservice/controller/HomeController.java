package com.ecommerce.catalogservice.controller;

import com.ecommerce.catalogservice.model.Catalog;
import com.ecommerce.catalogservice.model.CatalogDTO;
import com.ecommerce.catalogservice.model.ProductDTO;
import com.ecommerce.catalogservice.model.CatalogItem;
import com.ecommerce.catalogservice.repository.CatalogRepository;
import com.google.common.util.concurrent.AtomicDouble;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
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
import org.springframework.util.CollectionUtils;
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
    private CatalogRepository catalogRepository;

    @Value("${message:Hello default}")
    private String message;

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

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

    /*@GetMapping("/pr/r")
    public String getPrr() {
        RestTemplate restTemplate = new RestTemplate();
        String resourceUrl = "http://product-service/na";
        ResponseEntity<String> response = restTemplate.getForEntity(resourceUrl, String.class);

        return response.getBody().toString();
    }

    @GetMapping("/pr/p")
    public String getPr() {
        RestTemplate restTemplate = new RestTemplate();
        String resourceUrl = "http://product-service:8080/na";
        ResponseEntity<String> response = restTemplate.getForEntity(resourceUrl, String.class);

        return response.getBody().toString();
    }*/

    @GetMapping("/info")
    @ApiOperation(value = "Get information from the catalog-service instance", notes = "Retrieve information from a catalog-service instance")
    public String home() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Catalog Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @GetMapping("")
    @ApiOperation(value = "Get all catalogs", notes = "Retrieve all catalogs from the Database")
    public List<CatalogDTO> getCatalog() {
        incrementCounter();
        List<CatalogDTO> catalogDTOs = new ArrayList<>();
        for (Catalog catalog: catalogRepository.findAll()) {
            List<ProductDTO> productDTOs = new ArrayList<>();
            CatalogDTO catalogDTO = new CatalogDTO(catalog.getId(), catalog.getCreationDate());
            List<CatalogItem> ids = catalog.getProductIdentifiers();
            List<ProductDTO> products = new ArrayList<ProductDTO>();
            for (CatalogItem productIdentifier : ids) {
                ResponseEntity<ProductDTO> res = restTemplate.exchange("http://product-service:8080/" + productIdentifier.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                        });
                ProductDTO product = res.getBody();
                productDTOs.add(product);
            }
            catalogDTO.setProducts(productDTOs);
            catalogDTOs.add(catalogDTO);
        }
        return catalogDTOs;
    }

    //@HystrixCommand(fallbackMethod = "fallback")
    //poner escalera de llamadas a todos los servicios
    @GetMapping("/{id}")
    @ApiOperation(value = "Get a catalog", notes = "Provide an Id to retrieve a specific catalog from the Database")
    public CatalogDTO getCatalog(@ApiParam(value = "Id of the catalog to get", required = true) @PathVariable final String id) {
        incrementCounter();
        //si no existe ningun catalogo con ese id retornamos null
        Optional<Catalog> catalog = null;
        List<ProductDTO> productDTOs = new ArrayList<>();
        CatalogDTO result = null;
        try {
            catalog = catalogRepository.findById(id);
            result = new CatalogDTO(catalog.get().getId(), catalog.get().getCreationDate());
        } catch (Exception e){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Catalog not found"
            );
        }
        List<CatalogItem> ids = catalog.get().getProductIdentifiers();
        List<ProductDTO> products = new ArrayList<ProductDTO>();
        for (CatalogItem productIdentifier: ids) {
            ResponseEntity<ProductDTO> res = restTemplate.exchange("http://product-service:8080/" + productIdentifier.getProductId(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                    });
            ProductDTO product = res.getBody();
            productDTOs.add(product);
        }
        result.setProducts(productDTOs);
        return result;
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete a catalog", notes = "Provide an Id to delete a specific catalog from the Database")
    public CatalogDTO deleteCatalog(@ApiParam(value = "Id of the catalog to delete", required = true) @PathVariable final String id) throws Exception {
        incrementCounter();
        //si no existe ningun catalogo con ese id retornamos null
        Optional<Catalog> catalog = null;
        List<ProductDTO> productDTOs = new ArrayList<>();
        CatalogDTO result = null;
        try {
            catalog = catalogRepository.findById(id);
            result = new CatalogDTO(catalog.get().getId(), catalog.get().getCreationDate());
        } catch (Exception e){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Catalog not found"
            );
        }
        List<CatalogItem> ids = catalog.get().getProductIdentifiers();
        List<ProductDTO> products = new ArrayList<ProductDTO>();
        for (CatalogItem productIdentifier: ids) {
            ResponseEntity<ProductDTO> res = restTemplate.exchange("http://product-service:8080/" + productIdentifier.getProductId(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                    });
            ProductDTO product = res.getBody();
            productDTOs.add(product);
        }
        catalogRepository.deleteById(id);
        result.setProducts(productDTOs);
        return result;
    }

    @PostMapping("")
    @ApiOperation(value = "Create a catalog", notes = "Provide information to create a catalog")
    public CatalogDTO createCatalog() {
        incrementCounter();
        Catalog catalog = new Catalog(new Date());
        catalogRepository.save(catalog);
        return new CatalogDTO(catalog.getId(), new ArrayList<>(), catalog.getCreationDate());
    }

    // a fallback method to be called if failure happened
    public CatalogDTO fallback(String catalogId, Throwable hystrixCommand) {
        return new CatalogDTO();
    }

    @PutMapping("/{catalogId}")
    @ApiOperation(value = "Add product to a catalog", notes = "Add a product to a catalog")
    public ProductDTO addProductToCatalog(@ApiParam(value = "Id of the catalog for which a product has to be added", required = true) @PathVariable final String catalogId,
                                          @ApiParam(value = "Id of the product to add", required = true) @RequestBody CatalogItem productIdentifier) {
        incrementCounter();
        Optional<Catalog> catalog = catalogRepository.findById(catalogId);
        catalog.get().addProductIdentifier(new CatalogItem(productIdentifier.getProductId(), new Date()));
        ResponseEntity<ProductDTO> res = restTemplate.exchange("http://product-service:8080/" + productIdentifier.getProductId(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                });
        catalogRepository.save(catalog.get());
        return  res.getBody();
    }

    @GetMapping("/na")
    public String getNa() {
        incrementCounter();
        ResponseEntity<String> res = restTemplate.exchange("http://product-service:8080/na",
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        return  res.getBody();
    }

    @GetMapping("/{catalogId}/products/{productId}")
    @ApiOperation(value = "Get a product from a given catalog", notes = "Retrieve a product from a given catalog")
    public ProductDTO getCatalogProduct(@ApiParam(value = "Id of the catalog for which a product has to be retrieved", required = true) @PathVariable final String catalogId,
                                        @ApiParam(value = "Id of the product to retrieve from the catalog", required = true) @PathVariable final String productId) {
        incrementCounter();
        Optional<Catalog> catalog = catalogRepository.findById(catalogId);
        List<CatalogItem> ids = catalog.get().getProductIdentifiers();
        System.out.println(catalogId);
        List<ProductDTO> products = new ArrayList<ProductDTO>();
        for (CatalogItem productIdentifier: ids) {
            if (productIdentifier.getProductId().equals(productId)) {
                ResponseEntity<ProductDTO> res = restTemplate.exchange("http://product-service:8080/" + productIdentifier.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                        });
                return res.getBody();
            }
        }
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Product not found in catalog"
        );
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of Catalog service running at port: " + env.getProperty("local.server.port");
    }
}
