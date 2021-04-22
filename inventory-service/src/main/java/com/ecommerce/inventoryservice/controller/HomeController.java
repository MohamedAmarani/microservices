package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.model.*;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/")
public class HomeController {
    @Autowired
    private Environment env;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private InventoryRepository catalogRepository;

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
    @ApiOperation(value = "Get information from the inventory-service instance", notes = "Retrieve information from a inventory-service instance")
    public String home() {
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Inventory Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @GetMapping("/do")
    public String getPr() {
        RestTemplate restTemplate = new RestTemplate();
        String resourceUrl = "http://catalog-service:8080/pr/p";
        ResponseEntity<String> response = restTemplate.getForEntity(resourceUrl, String.class);

        return response.getBody().toString();
    }

    @GetMapping("")
    @ApiOperation(value = "Get all inventories", notes = "Retrieve all inventory from the Database")
    public List<Inventory> getInventories() {
        return catalogRepository.findAll();
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @ApiOperation(value = "Get an inventory", notes = "Provide an Id to retrieve a specific inventory from the Database")
    @GetMapping("/{id}")
    public InventoryDTO getInventory(@ApiParam(value = "Id of the inventory to get", required = true) @PathVariable final String id) {
        Optional<Inventory> inventory = catalogRepository.findById(id);
        //la respuesta inicialiada con dos paramatros
        InventoryDTO response = new InventoryDTO(inventory.get().getId(), inventory.get().getCatalogId());

        List<InventoryItemDTO> inventoryItemDTOs = new ArrayList<>();
        List<InventoryItem> inventoryItems = inventory.get().getInventoryItems();

        //pasamos todos los InventoryItem a InventoryItemDTO
        for (InventoryItem inventoryItem: inventoryItems) {
            final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://catalog-service:8080/" + inventory.get().getCatalogId()
                            + "/products/" + inventoryItem.getProductId(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                    });
            ProductDTO product = res.getBody();
            inventoryItemDTOs.add(new InventoryItemDTO(product, inventoryItem.getItems()));
        }
        response.setInventoryItems(inventoryItemDTOs);
        return response;
    }

    @PostMapping("")
    @ApiOperation(value = "Create an inventory", notes = "Provide information to create an inventory")
    public Inventory createInventory(@ApiParam(value = "Information of the inventory to create", required = true) @RequestBody Inventory inventory) {
        return catalogRepository.save(inventory);
    }

    // a fallback method to be called if failure happened
    public List<ProductDTO> fallback(String catalogId, Throwable hystrixCommand) {
        return new ArrayList<>();
    }

    //@HystrixCommand(fallbackMethod = "fallback")
    //obtener un producto de un inventario
    @GetMapping("/{inventoryId}/products/{productId}")
    @ApiOperation(value = "Get a product from an inventory", notes = "Provide information of a product from an inventory")
    public InventoryItemDTO getInventory(@ApiParam(value = "Id of the inventory for which we a product has to be retrieved", required = true) @PathVariable final String inventoryId,
                                         @ApiParam(value = "Id of the product of the inventory to get", required = true) @PathVariable final String productId) {
        System.out.println("hola");
        Optional<Inventory> inventory = catalogRepository.findById(inventoryId);
        List<InventoryItem> inventoryItems = inventory.get().getInventoryItems();

        try {
            for (InventoryItem inventoryItem : inventoryItems) {
                //si es el item que buscamos
                if (inventoryItem.getProductId().equals(productId)) {
                    //obtener los datos del product a partir del productId
                    final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://catalog-service:8080/" + inventory.get().getCatalogId()
                                    + "/products/" + inventoryItem.getProductId(),
                            HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                            });
                    return new InventoryItemDTO(res.getBody(), inventoryItem.getItems());
                }
            }
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found in catalog"
            );
        }
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Other error"
        );
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Add a product to an inventory", notes = "Insert a product to an existing inventory")
    //añadir un producto a un inventario
    public InventoryItemDTO addProductToInventory(@ApiParam(value = "Id of the inventory for which a product has to be inserted", required = true) @PathVariable final String id,
                                                  @ApiParam(value = "Information of the product to insert into the inventory", required = true) @RequestBody InventoryItem inventoryItem) {
        Optional<Inventory> inventory = catalogRepository.findById(id);
        ResponseEntity<ProductDTO> res = null;
        try {
            res = restTemplate.exchange("http://catalog-service:8080/" + inventory.get().getCatalogId()
                            + "/products/" + inventoryItem.getProductId(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                    });
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found in catalog"
            );
        }
        boolean alreadyExists = false;
        for (InventoryItem inventoryItem1: inventory.get().getInventoryItems()) {
            if (inventoryItem1.getProductId().equals(inventoryItem.getProductId())) {
                alreadyExists = true;
                inventoryItem1.setItems(inventoryItem1.getItems() + inventoryItem.getItems());
                //para el return solamente
                inventoryItem.setItems(inventoryItem1.getItems() + inventoryItem.getItems());
            }
        }
        if (!alreadyExists)
            inventory.get().addInventoryItems(inventoryItem);
        catalogRepository.save(inventory.get());
        return new InventoryItemDTO(res.getBody(), inventoryItem.getItems());
    }

    //decrement items
    //HAY QUE LLAMAR A UPDATEAVAILABILITY
    @PutMapping("/{id}/products/{productId}/reduceStock")
    @ApiOperation(value = "Reduce stock of a product of an inventory", notes = "Reduce product stock from an inventory")
    public InventoryItemDTO addProductToCart(@ApiParam(value = "Id of the inventory for which a product stock has to be decremented", required = true) @PathVariable final String id,
                                             @ApiParam(value = "Id of the inventory product for which the stock has to be decremented", required = true) @PathVariable final String productId,
                                             @ApiParam(value = "Quantity of items to reduce from the inventory stock", required = true) @RequestBody Map<String, Integer> myJsonRequest) {
        Optional<Inventory> inventory = catalogRepository.findById(id);
        int num = myJsonRequest.get("numItems");
        for (InventoryItem inventoryItem : inventory.get().getInventoryItems())
            //si es el inventory item que buscamos
            if  (inventoryItem.getProductId().equals(productId)) {
                final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://catalog-service:8080/" + inventory.get().getCatalogId()
                                + "/products/" + inventoryItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                        });
                try {
                    inventoryItem.decrementItems(num);
                    //guardamos los cambios
                    catalogRepository.save(inventory.get());
                    //update carts
                    restTemplate.exchange("http://cart-service:8080/update",
                            HttpMethod.PUT, null, new ParameterizedTypeReference<Object>() {
                            });
                    return new InventoryItemDTO(res.getBody(), inventoryItem.getItems());

                } catch (Exception e) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Not enough stock"
                    );
                }
            }
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Product not found in inventory"
        );
    }

    //add stock
    //HAY QUE LLAMAR A UPDATEAVAILABILITY
    @PutMapping("/{id}/products/{productId}/addStock")
    @ApiOperation(value = "Increment stock of a product of an inventory", notes = "Reduce product stock from an inventory")
    public InventoryItemDTO addStock(@ApiParam(value = "Id of the inventory for which a product stock has to be incremented", required = true) @PathVariable final String id,
                                     @ApiParam(value = "Id of the inventory product for which the stock has to be incremented", required = true) @PathVariable final String productId,
                                     @ApiParam(value = "Quantity of items to increment to the inventory stock", required = true) @RequestBody Map<String, Integer> myJsonRequest) {
        Optional<Inventory> inventory = catalogRepository.findById(id);
        int num = myJsonRequest.get("numItems");
        for (InventoryItem inventoryItem : inventory.get().getInventoryItems())
            //si es el inventory item que buscamos
            if  (inventoryItem.getProductId().equals(productId)) {
                final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://catalog-service:8080/" + inventory.get().getCatalogId()
                                + "/products/" + inventoryItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                        });
                try {
                    inventoryItem.incrementItems(num);
                    //guardamos los cambios
                    catalogRepository.save(inventory.get());
                    //update carts
                    restTemplate.exchange("http://cart-service:8080/update",
                            HttpMethod.PUT, null, new ParameterizedTypeReference<Object>() {
                            });
                    return new InventoryItemDTO(res.getBody(), inventoryItem.getItems());

                } catch (Exception e) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Can not add negative stock"
                    );
                }
            }
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Product not found in inventory"
        );
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    // We'll add the logic of role based auth later
    @RequestMapping("/admin")
    public String homeAdmin() {
        return "This is the admin area of Inventory service running at port: " + env.getProperty("local.server.port");
    }
}

