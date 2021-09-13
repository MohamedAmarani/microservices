package com.ecommerce.discountservice.controller;

import com.ecommerce.discountservice.model.Discount;
import com.ecommerce.discountservice.repository.DiscountRepository;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
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
    private DiscountRepository discountRepository;

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
    @ApiOperation(value = "Get information from the discount-service instance", notes = "Retrieve information from a cart-service instance")
    public String home() {
        incrementCounter();
        return "Hello from Discount Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("")
    @ApiOperation(value = "Get all discounts", notes = "Retrieve all discounts from the Database")
    public ResponseEntity<Map<String, Object>> getDiscounts(@RequestParam(defaultValue = "", required = false) String code,
                                                            @RequestParam(defaultValue = "1970-01-01T00:00:0.000+00:00", required = false) Date minCreationDate,
                                                            @RequestParam(defaultValue = "2024-01-01T00:00:0.000+00:00", required = false) Date maxCreationDate,
                                                            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                                            @RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                            @RequestParam(value = "sort", defaultValue = "creationDate,asc", required = false) String sort) {
        incrementCounter();
        List<Discount> discounts;
        PageRequest request = PageRequest.of(page, size, Sort.by(new Sort.Order(sort.split(",")[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(",")[0])));
        Page<Discount> pagedDiscounts = discountRepository.findByCodeContainingIgnoreCaseAndCreationDateBetween(code, minCreationDate, maxCreationDate, request);

        discounts = pagedDiscounts.getContent();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("currentPage", pagedDiscounts.getNumber());
        response.put("totalItems", pagedDiscounts.getTotalElements());
        response.put("totalPages", pagedDiscounts.getTotalPages());
        response.put("discounts", discounts);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/{discountId}")
    @ApiOperation(value = "Get a specific discount", notes = "Provide the id of the discount to retrieve")
    public Discount getDiscount(@ApiParam(value = "Id of the discount that has to be retrieved", required = true) @PathVariable final String discountId) {
        incrementCounter();
        Discount discount;
        try {
            try {
                discount = discountRepository.findById(discountId).get();
            } catch (Exception e) {
                discount = discountRepository.findByCode(discountId).get();
            }
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Discount not found"
            );
        }
        return discount;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @PostMapping("")
    @ApiOperation(value = "Create a discount", notes = "Provide information to create a discount")
    public Discount postDiscount(@ApiParam(value = "Information of the discount to create", required = true) @RequestBody Discount discount) {
        incrementCounter();
        discount.setCreationDate(new Date());
        try {
            discount = discountRepository.save(discount);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A discount with the same code already exists"
            );
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Discount> discountEntity = new HttpEntity<Discount>(discount, headers);
        //enviar mail de update
        final ResponseEntity<String> res1 = restTemplate.exchange("http://account-service:8080/newDiscountEmail",
                HttpMethod.POST, discountEntity, new ParameterizedTypeReference<String>() {
                });
        return discount;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @DeleteMapping("/{discountId}")
    @ApiOperation(value = "Delete a specific discount", notes = "Provide the id of the discount to delete")
    public Discount deleteDiscount(@ApiParam(value = "Id of the discount that has to be deleted", required = true) @PathVariable final String discountId) {
        incrementCounter();
        Discount discount = discountRepository.findById(discountId).get();
        discountRepository.delete(discount);
        return discount;
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @DeleteMapping("")
    @ApiOperation(value = "Delete all discounts", notes = "Delete all discounts of the database")
    public List<Discount> deleteDiscounts() {
        incrementCounter();
        discountRepository.deleteAll();
        return discountRepository.findAll();
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @PutMapping("/{discountId}/useDiscount")
    @ApiOperation(value = "Use a discount", notes = "Increment by one the number of uses of the given discount")
    public Discount useDiscount(@ApiParam(value = "Id of the discount that has to be used", required = true) @PathVariable final String discountId) {
        incrementCounter();
        Discount discount = discountRepository.findById(discountId).get();
        discount.incrementCurrentUses();
        return discountRepository.save(discount);
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @PatchMapping("/{discountId}/enable")
    @ApiOperation(value = "Enable a specific discount", notes = "Enables the discount to be used")
    public Discount enableDiscount(@ApiParam(value = "Id of the discount that has to be enabled", required = true) @PathVariable final String discountId) {
        incrementCounter();
        Discount discount = discountRepository.findById(discountId).get();
        if (!discount.isEnabled()) {
            discount.setEnabled(true);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Discount> discountEntity = new HttpEntity<Discount>(discount, headers);
            //enviar mail de update
            final ResponseEntity<String> res1 = restTemplate.exchange("http://account-service:8080/enabledDiscountEmail",
                    HttpMethod.POST, discountEntity, new ParameterizedTypeReference<String>() {
                    });
        }
        return discountRepository.save(discount);
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @PatchMapping("/{discountId}/disable")
    @ApiOperation(value = "Disable a specific discount", notes = "Disables the discount in order to make it not redeemable")
    public Discount disableDiscount(@ApiParam(value = "Id of the discount that has to be disabled", required = true) @PathVariable final String discountId) {
        incrementCounter();
        Discount discount = discountRepository.findById(discountId).get();
        if (discount.isEnabled()) {
            discount.setEnabled(false);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Discount> discountEntity = new HttpEntity<Discount>(discount, headers);
            //enviar mail de update
            final ResponseEntity<String> res1 = restTemplate.exchange("http://account-service:8080/disabledDiscountEmail",
                    HttpMethod.POST, discountEntity, new ParameterizedTypeReference<String>() {
                    });
        }
        return discountRepository.save(discount);
    }

    // metodo fallback a llamar si falla alguna peticion
    public List<Discount> fallback(String discountId, Throwable hystrixCommand) {
        return new ArrayList<>();
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of Discount service running at port: " + env.getProperty("local.server.port");
    }
}

