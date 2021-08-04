package com.ecommerce.deliveryservice.controller;

import com.ecommerce.deliveryservice.model.Delivery;
import com.ecommerce.deliveryservice.model.OrderDTO;
import com.ecommerce.deliveryservice.repository.DeliveryRepository;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
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
    private DeliveryRepository deliveryRepository;

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
    @ApiOperation(value = "Get information from the delivery-service instance", notes = "Retrieve information from a delivery-service instance")
    public String home() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Delivery Service running at port: " + env.getProperty("local.server.port");
    }

    @GetMapping("")
    @ApiOperation(value = "Get all deliveries", notes = "Retrieve all deliveries from the Database")
    public ResponseEntity<Map<String, Object>> getDeliveries(@RequestParam(defaultValue = "", required = false) String orderId,
                                                             @RequestParam(defaultValue = "", required = false) String deliveryAddress,
                                                             @RequestParam(defaultValue = "", required = false) String deliveryState,
                                                             @RequestParam(defaultValue = "", required = false) String deliveryCompany,
                                                             @RequestParam(defaultValue = "1970-01-01T00:00:0.000+00:00", required = false) Date minCreationDate,
                                                             @RequestParam(defaultValue = "2024-01-01T00:00:0.000+00:00", required = false) Date maxCreationDate,
                                                             @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                                             @RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                             @RequestParam(value = "sort", defaultValue = "creationDate,asc", required = false) String sort) {
        incrementCounter();
        PageRequest request = PageRequest.of(page, size, Sort.by(new Sort.Order(sort.split(",")[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(",")[0])));
        Page<Delivery> pagedDeliveries = deliveryRepository.findByOrderIdContainingIgnoreCaseAnDeliveryAddressContainingIgnoreCaseAndDeliveryStateContainingIgnoreCaseAndDeliveryCompanyContainingIgnoreCaseAndCreationDateBetween(orderId, deliveryAddress, deliveryState, deliveryCompany, minCreationDate, maxCreationDate, request);

        Map<String, Object> response = new HashMap<>();
        response.put("currentPage", pagedDeliveries.getNumber());
        response.put("totalItems", pagedDeliveries.getTotalElements());
        response.put("totalPages", pagedDeliveries.getTotalPages());
        response.put("carts", pagedDeliveries.getContent());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    //@HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/{id}")
    @ApiOperation(value = "Get a delivery", notes = "Provide an Id to retrieve a specific delivery from the Database")
    public Delivery getDelivery(@ApiParam(value = "Id of the delivery to get", required = true) @PathVariable final String id) {
        incrementCounter();
        try {
            return deliveryRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Delivery not found"
            );
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete a delivery", notes = "Provide an Id to delete a specific delivery from the Database")
    public Delivery deleteDelivery(@ApiParam(value = "Id of the delivery to delete", required = true) @PathVariable final String id) throws Exception {
        incrementCounter();
        Delivery delivery = null;
        try {
            delivery = deliveryRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Delivery not found"
            );
        }
        deliveryRepository.deleteById(id);
        return delivery;
    }

    @PostMapping("")
    @ApiOperation(value = "Create a delivery", notes = "Provide information to create a delivery")
    public Delivery createDelivery(@ApiParam(value = "Delivery to create", required = true) @RequestBody Map<String, String> myJsonRequest) {
        incrementCounter();
        return deliveryRepository.save(new Delivery(myJsonRequest.get("orderId").toString(), myJsonRequest.get("deliveryAddress").toString(),
                new Date()));
    }

    @PatchMapping("/{id}/nextEvent")
    @ApiOperation(value = "Update the delivery state", notes = "Proceed to get to the next stage of the delivery")
    public Delivery continueProcess(@ApiParam(value = "Id of the delivery for which the state has to be updated", required = true) @PathVariable final String id) {
        incrementCounter();
        Delivery delivery = deliveryRepository.findById(id).get();
        delivery.setNextDeliveryEvent();
        delivery = deliveryRepository.save(delivery);
        //obtener accountId de la cuenta que ha hecho el pedido
        final ResponseEntity<String> res4 = restTemplate.exchange("http://order-service:8080/" + delivery.getOrderId(),
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        Gson gson = new Gson();
        OrderDTO orderDTO = gson.fromJson(res4.getBody(), OrderDTO.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Delivery> deliveryEntity = new HttpEntity<Delivery>(delivery, headers);
        //enviar mail de update
        final ResponseEntity<String> res1 = restTemplate.exchange("http://account-service:8080/" + orderDTO.getCart().getId() + "/deliveryUpdateEmail",
                HttpMethod.POST, deliveryEntity, new ParameterizedTypeReference<String>() {
                });
        return delivery;
    }

    @PatchMapping("/{id}/estimatedDateOfArrival")
    @ApiOperation(value = "Update the delivery estimated date of arrival", notes = "Proceed to update the date of the delivery")
    public Delivery updateEstimatedDateOfArrival(@ApiParam(value = "Id of the delivery for which the date has to be updated", required = true) @PathVariable final String id,
                                                 @ApiParam(value = "New date based out of offset days of the estimated date of arrival", required = true) @RequestBody Map<String, Integer> myJsonRequest) {
        incrementCounter();
        Delivery delivery = deliveryRepository.findById(id).get();
        int offsetDays = myJsonRequest.get("offsetDays");
        delivery.updateEstimatedDateOfArrival(offsetDays);
        delivery = deliveryRepository.save(delivery);
        //obtener accountId de la cuenta que ha hecho el pedido
        final ResponseEntity<String> res4 = restTemplate.exchange("http://order-service:8080/" + delivery.getOrderId(),
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        Gson gson = new Gson();
        OrderDTO orderDTO = gson.fromJson(res4.getBody(), OrderDTO.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Delivery> deliveryEntity = new HttpEntity<Delivery>(delivery, headers);
        //enviar mail de update
        final ResponseEntity<String> res1 = restTemplate.exchange("http://account-service:8080/" + orderDTO.getCart().getId() + "/deliveryDateUpdateEmail",
                HttpMethod.POST, deliveryEntity, new ParameterizedTypeReference<String>() {
                });
        return delivery;
    }

    // a fallback method to be called if failure happened
    public List<Delivery> fallback(String catalogId, Throwable hystrixCommand) {
        return new ArrayList<>();
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    // We'll add the logic of role based auth later
    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of Delivery service running at port: " + env.getProperty("local.server.port");
    }
}
