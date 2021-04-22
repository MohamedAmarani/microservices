package com.ecommerce.catalogservice.controller;

import com.ecommerce.catalogservice.model.Catalog;
import com.ecommerce.catalogservice.model.CatalogDTO;
import com.ecommerce.catalogservice.model.ProductDTO;
import com.ecommerce.catalogservice.model.CatalogItem;
import com.ecommerce.catalogservice.repository.CatalogRepository;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
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

import java.util.ArrayList;
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
    private CatalogRepository catalogRepository;

    @Value("${message:Hello default}")
    private String message;

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @GetMapping("/hello")
    public ResponseEntity<String> getHello() {
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

    @RequestMapping("/info")
    @ApiOperation(value = "Get information from the catalog-service instance", notes = "Retrieve information from a catalog-service instance")
    public String home() {
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Catalog Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @GetMapping("")
    @ApiOperation(value = "Get all catalogs", notes = "Retrieve all catalogs from the Database")
    public List<Catalog> getCatalog() {
        return catalogRepository.findAll();
    }

    //@HystrixCommand(fallbackMethod = "fallback")
    //poner escalera de llamadas a todos los servicios
    @GetMapping("/{id}")
    @ApiOperation(value = "Get a catalog", notes = "Provide an Id to retrieve a specific catalog from the Database")
    public CatalogDTO getCatalog(@ApiParam(value = "Id of the catalog to get", required = true) @PathVariable final String id) {
        Optional<Catalog> catalog = catalogRepository.findById(id);
        CatalogDTO result = new CatalogDTO(catalog.get().getId());
        List<ProductDTO> productDTOs = new ArrayList<>();

        List<CatalogItem> ids = catalog.get().getProductIdentifiers();
        List<ProductDTO> products = new ArrayList<ProductDTO>();
        for (CatalogItem productIdentifier: ids) {
            ResponseEntity<ProductDTO> res = restTemplate.exchange("http://product-service:8080/" + productIdentifier.getProductId(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                    });
            ProductDTO product = res.getBody();
            productDTOs.add(product);
        }
        result.setProductDTOs(productDTOs);
        return result;
    }

    @PostMapping("")
    @ApiOperation(value = "Create a catalog", notes = "Provide information to create a catalog")
    public CatalogDTO createCatalog() {
        Catalog catalog = new Catalog();
        catalogRepository.save(catalog);
        return new CatalogDTO(catalog.getId(), new ArrayList<>());
    }

    // a fallback method to be called if failure happened
    public CatalogDTO fallback(String catalogId, Throwable hystrixCommand) {
        return new CatalogDTO();
    }

    @PutMapping("/{catalogId}")
    @ApiOperation(value = "Add product to catalog", notes = "Add a product to a catalog")
    public ProductDTO addProductToCatalog(@ApiParam(value = "Id of the catalog for which a product has to be added", required = true) @PathVariable final String catalogId,
                                          @ApiParam(value = "Id of the product to add", required = true) @RequestBody CatalogItem productIdentifier) {
        Optional<Catalog> catalog = catalogRepository.findById(catalogId);
        catalog.get().addProductIdentifier(new CatalogItem(productIdentifier.getProductId()));
        ResponseEntity<ProductDTO> res = restTemplate.exchange("http://product-service:8080/" + productIdentifier.getProductId(),
                HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                });
        catalogRepository.save(catalog.get());
        return  res.getBody();
    }

    @GetMapping("/na")
    public String getNa() {
        ResponseEntity<String> res = restTemplate.exchange("http://product-service:8080/na",
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        return  res.getBody();
    }

    @GetMapping("/{catalogId}/products/{productId}")
    @ApiOperation(value = "Get a product from a given catalog", notes = "Retrieve a product from a given catalog")
    public ProductDTO getCatalogProduct(@ApiParam(value = "Id of the catalog for which a product has to be retrieved", required = true) @PathVariable final String catalogId,
                                        @ApiParam(value = "Id of the product to retrieve from the catalog", required = true) @PathVariable final String productId) {
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
                HttpStatus.NOT_FOUND, "Product not found"
        );
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    @RequestMapping("/admin")
    public String homeAdmin() {
        return "This is the admin area of Catalog service running at port: " + env.getProperty("local.server.port");
    }
}
