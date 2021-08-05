package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.model.*;
import com.ecommerce.inventoryservice.repository.InventoryRepository;
import com.google.common.util.concurrent.AtomicDouble;
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
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.text.DateFormat;
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
    private InventoryRepository inventoryRepository;

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
    @ApiOperation(value = "Get information from the inventory-service instance", notes = "Retrieve information from a inventory-service instance")
    public String home() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Inventory Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @GetMapping("/do")
    public String getPr() {
        incrementCounter();
        RestTemplate restTemplate = new RestTemplate();
        String resourceUrl = "http://catalog-service:8080/pr/p";
        ResponseEntity<String> response = restTemplate.getForEntity(resourceUrl, String.class);

        return response.getBody().toString();
    }

    @GetMapping("")
    @ApiOperation(value = "Get all inventories", notes = "Retrieve all inventories from the Database")
    public ResponseEntity<Map<String, Object>> getInventories(@RequestParam(defaultValue = "", required = false) String productId,
                                                              @RequestParam(defaultValue = "", required = false) String catalogId,
                                                              @RequestParam(defaultValue = "1970-01-01T00:00:0.000+00:00", required = false) Date minCreationDate,
                                                              @RequestParam(defaultValue = "2024-01-01T00:00:0.000+00:00", required = false) Date maxCreationDate,
                                                              @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                                              @RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                              @RequestParam(value = "sort", defaultValue = "creationDate,asc", required = false) String sort) {
        incrementCounter();
        PageRequest request = PageRequest.of(page, size, Sort.by(new Sort.Order(sort.split(",")[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(",")[0])));
        Page<Inventory> pagedInventories = inventoryRepository.findByCreationDateBetween(minCreationDate, maxCreationDate, request);
        List<Inventory> list = new ArrayList<>();

        //seleccionar solo los inventarios con algun productId como el especificado
        if (!productId.equals("")) {
            //solo las que tengan el productId si se ha especificado
            for (int i = 0; i < pagedInventories.getContent().size(); ++i) {
                boolean found = false;
                for (int j = 0; !found && j < pagedInventories.getContent().get(i).getInventoryItems().size(); ++j) {
                    if (pagedInventories.getContent().get(i).getInventoryItems().get(j).getProductId().equals(productId))
                        found = true;
                }
                if (found) {
                    //seleccionar solo los inventarios con algun catalogId como el especificado
                    if (!catalogId.equals("")) {
                        //solo las que tengan el productId si se ha especificado
                        for (int i1 = 0; i1 < pagedInventories.getContent().size(); ++i1) {
                            found = false;
                            for (int j = 0; !found && j < pagedInventories.getContent().get(i1).getInventoryItems().size(); ++j) {
                                if (pagedInventories.getContent().get(i1).getInventoryItems().get(j).getCatalogId().equals(catalogId))
                                    found = true;
                            }
                            if (found)
                                list.add(pagedInventories.getContent().get(i));
                        }
                    }
                }
            }
            pagedInventories = new PageImpl<>(list, PageRequest.of(page, size), list.size());
        }
        else if (!catalogId.equals("")) {
                //solo las que tengan el productId si se ha especificado
            for (int i = 0; i < pagedInventories.getContent().size(); ++i) {
                boolean found = false;
                for (int j = 0; !found && j < pagedInventories.getContent().get(i).getInventoryItems().size(); ++j) {
                    if (pagedInventories.getContent().get(i).getInventoryItems().get(j).getCatalogId().equals(catalogId))
                        found = true;
                }
                if (found)
                    list.add(pagedInventories.getContent().get(i));
            }
            pagedInventories = new PageImpl<>(list, PageRequest.of(page, size), list.size());
        }

        List<InventoryDTO> inventoryDTOs = new ArrayList<>();
        for (Inventory inventory: pagedInventories.getContent()) {
            //la respuesta inicialiada con dos paramatros
            InventoryDTO inventoryDTO = null;
            inventoryDTO = new InventoryDTO(inventory.getId(), inventory.getCreationDate());
            List<InventoryItemDTO> inventoryItemDTOs = new ArrayList<>();
            List<InventoryItem> inventoryItems = inventory.getInventoryItems();

            //pasamos todos los InventoryItem a InventoryItemDTO
            for (InventoryItem inventoryItem: inventoryItems) {
                final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://catalog-service:8080/" + inventoryItem.getCatalogId()
                                + "/products/" + inventoryItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                        });
                ProductDTO product = res.getBody();
                inventoryItemDTOs.add(new InventoryItemDTO(product, inventoryItem.getCatalogId(), inventoryItem.getQuantity(),
                        inventoryItem.getCreationDate()));
            }
            inventoryDTO.setItems(inventoryItemDTOs);
            inventoryDTOs.add(inventoryDTO);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("currentPage", pagedInventories.getNumber());
        response.put("totalItems", pagedInventories.getTotalElements());
        response.put("totalPages", pagedInventories.getTotalPages());
        response.put("inventories", inventoryDTOs);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @ApiOperation(value = "Get an inventory", notes = "Provide an Id to retrieve a specific inventory from the Database")
    @GetMapping("/{id}")
    public InventoryDTO getInventory(@ApiParam(value = "Id of the inventory to get", required = true) @PathVariable final String id) {
        incrementCounter();
        Optional<Inventory> inventory = inventoryRepository.findById(id);
        //la respuesta inicialiada con dos paramatros
        InventoryDTO response = null;
        try {
            response = new InventoryDTO(inventory.get().getId(), inventory.get().getCreationDate());
        }
        catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Inventory not found"
            );
        }
        List<InventoryItemDTO> inventoryItemDTOs = new ArrayList<>();
        List<InventoryItem> inventoryItems = inventory.get().getInventoryItems();

        //pasamos todos los InventoryItem a InventoryItemDTO
        for (InventoryItem inventoryItem: inventoryItems) {
            final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://catalog-service:8080/" + inventoryItem.getCatalogId()
                            + "/products/" + inventoryItem.getProductId(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                    });
            ProductDTO product = res.getBody();
            inventoryItemDTOs.add(new InventoryItemDTO(product, inventoryItem.getCatalogId(), inventoryItem.getQuantity(), inventoryItem.getCreationDate()));
        }
        response.setItems(inventoryItemDTOs);
        return response;
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete an inventory", notes = "Provide an Id to delete a specific inventory from the Database")
    public InventoryDTO deleteInventory(@ApiParam(value = "Id of the inventory to delete", required = true) @PathVariable final String id) throws Exception {
        Inventory inventory = null;
        //si no existe ningun producto con ese id retornamos null
        InventoryDTO response = null;
        try {
            //throw new Exception("Images can't be fetched");
            inventory = inventoryRepository.findById(id).get();
            //la respuesta inicialiada con dos paramatros (pasar inventory a inventorydto)
            response = new InventoryDTO(inventory.getId(), inventory.getCreationDate());
            List<InventoryItemDTO> inventoryItemDTOs = new ArrayList<>();
            List<InventoryItem> inventoryItems = inventory.getInventoryItems();

            //pasamos todos los InventoryItem a InventoryItemDTO
            for (InventoryItem inventoryItem: inventoryItems) {
                final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://catalog-service:8080/" + inventoryItem.getCatalogId()
                                + "/products/" + inventoryItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                        });
                ProductDTO product = res.getBody();
                inventoryItemDTOs.add(new InventoryItemDTO(product, inventoryItem.getCatalogId(), inventoryItem.getQuantity(), inventoryItem.getCreationDate()));
            }
            response.setItems(inventoryItemDTOs);
            inventoryRepository.deleteById(id);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Inventory not found"
            );
        }
        incrementCounter();
        return response;
    }

    @DeleteMapping("")
    @ApiOperation(value = "Delete all inventories", notes = "Delete all inventories and their respective items from the Database")
    public void deleteInventories() throws Exception {
        incrementCounter();
        inventoryRepository.deleteAll();
    }


    @PostMapping("")
    @ApiOperation(value = "Create an inventory", notes = "Provide information to create an inventory")
    public Inventory createInventory(@ApiParam(value = "Information of the inventory to create", required = true) @RequestBody Inventory inventory) {
        incrementCounter();
        inventory.setCreationDate(new Date());
        return inventoryRepository.save(inventory);
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
        incrementCounter();
        Optional<Inventory> inventory = inventoryRepository.findById(inventoryId);
        List<InventoryItem> inventoryItems = inventory.get().getInventoryItems();

        try {
            for (InventoryItem inventoryItem : inventoryItems) {
                //si es el item que buscamos
                if (inventoryItem.getProductId().equals(productId)) {
                    //obtener los datos del product a partir del productId
                    final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://catalog-service:8080/" + inventoryItem.getCatalogId()
                                    + "/products/" + inventoryItem.getProductId(),
                            HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                            });
                    return new InventoryItemDTO(res.getBody(), inventoryItem.getCatalogId(), inventoryItem.getQuantity(), inventoryItem.getCreationDate());
                }
            }
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found in inventory"
            );
        }
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Other error"
        );
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Add an inventory item to an inventory", notes = "Insert a product to an existing inventory")
    //añadir un producto a un inventario
    public InventoryItemDTO addProductToInventory(@ApiParam(value = "Id of the inventory for which a product has to be inserted", required = true) @PathVariable final String id,
                                                  @ApiParam(value = "Information of the product to insert into the inventory", required = true) @RequestBody InventoryItem inventoryItem) {
        Optional<Inventory> inventory = null;
        try {
        incrementCounter();
        inventory = inventoryRepository.findById(id);
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Inventory not found"
            );
        }
        ResponseEntity<ProductDTO> res = null;
        inventoryItem.setCreationDate(new Date());
        try {
            res = restTemplate.exchange("http://catalog-service:8080/" + inventoryItem.getCatalogId()
                            + "/products/" + inventoryItem.getProductId(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                    });
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found in the given catalog"
            );
        }
        //si ya esta en el inventario sumar cantidades, si no crear de nuevo
        boolean alreadyExists = false;
        for (InventoryItem inventoryItem1: inventory.get().getInventoryItems()) {
            if (inventoryItem1.getProductId().equals(inventoryItem.getProductId())) {
                alreadyExists = true;
                inventoryItem1.setQuantity(inventoryItem1.getQuantity() + inventoryItem.getQuantity());
                //para el return solamente
                inventoryItem.setQuantity(inventoryItem1.getQuantity() + inventoryItem.getQuantity());
            }
        }
        if (!alreadyExists)
            inventory.get().addInventoryItems(inventoryItem);
        Inventory inventory1 = inventoryRepository.save(inventory.get());
        for (InventoryItem inventoryItem1: inventory.get().getInventoryItems()) {
            if (inventoryItem1.getProductId().equals(inventoryItem.getProductId())) {
                return new InventoryItemDTO(res.getBody(), inventoryItem1.getCatalogId(), inventoryItem1.getQuantity(), inventoryItem.getCreationDate());
            }
        }
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Bad Request"
        );
    }

    //decrement items
    //HAY QUE LLAMAR A UPDATEAVAILABILITY
    @PutMapping("/{id}/products/{productId}/reduceStock")
    @ApiOperation(value = "Reduce stock of a product of an inventory", notes = "Reduce product stock from an inventory")
    public InventoryItemDTO addProductToCart(@ApiParam(value = "Id of the inventory for which a product stock has to be decremented", required = true) @PathVariable final String id,
                                             @ApiParam(value = "Id of the inventory product for which the stock has to be decremented", required = true) @PathVariable final String productId,
                                             @ApiParam(value = "Quantity of items to reduce from the inventory stock", required = true) @RequestBody Map<String, Integer> myJsonRequest) {
        incrementCounter();
        Optional<Inventory> inventory = inventoryRepository.findById(id);
        int num = myJsonRequest.get("quantity");
        for (InventoryItem inventoryItem : inventory.get().getInventoryItems())
            //si es el inventory item que buscamos
            if  (inventoryItem.getProductId().equals(productId)) {
                final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://catalog-service:8080/" + inventoryItem.getCatalogId()
                                + "/products/" + inventoryItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                        });
                try {
                    inventoryItem.decrementQuantity(num);
                    //guardamos los cambios
                    inventoryRepository.save(inventory.get());
                    //update carts
                    restTemplate.exchange("http://cart-service:8080/update",
                            HttpMethod.PUT, null, new ParameterizedTypeReference<Object>() {
                            });
                    return new InventoryItemDTO(res.getBody(), inventoryItem.getCatalogId(), inventoryItem.getQuantity(), inventoryItem.getCreationDate());

                } catch (Exception e) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "Not enough stock"
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
    @ApiOperation(value = "Increment stock of a product of an inventory", notes = "Increase product stock from an inventory")
    public InventoryItemDTO addStock(@ApiParam(value = "Id of the inventory for which a product stock has to be incremented", required = true) @PathVariable final String id,
                                     @ApiParam(value = "Id of the inventory product for which the stock has to be incremented", required = true) @PathVariable final String productId,
                                     @ApiParam(value = "Quantity of items to increment to the inventory stock", required = true) @RequestBody Map<String, Integer> myJsonRequest) {
        incrementCounter();
        Optional<Inventory> inventory = inventoryRepository.findById(id);
        int num = myJsonRequest.get("quantity");
        for (InventoryItem inventoryItem : inventory.get().getInventoryItems())
            //si es el inventory item que buscamos
            if  (inventoryItem.getProductId().equals(productId)) {
                final ResponseEntity<ProductDTO> res = restTemplate.exchange("http://catalog-service:8080/" + inventoryItem.getCatalogId()
                                + "/products/" + inventoryItem.getProductId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                        });
                try {
                    inventoryItem.incrementQuantity(num);
                    //guardamos los cambios
                    inventoryRepository.save(inventory.get());
                    //update carts
                    restTemplate.exchange("http://cart-service:8080/update",
                            HttpMethod.PUT, null, new ParameterizedTypeReference<Object>() {
                            });
                    return new InventoryItemDTO(res.getBody(), inventoryItem.getCatalogId(), inventoryItem.getQuantity(), inventoryItem.getCreationDate());

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

    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of Inventory service running at port: " + env.getProperty("local.server.port");
    }
}

