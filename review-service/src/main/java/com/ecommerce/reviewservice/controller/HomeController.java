package com.ecommerce.reviewservice.controller;

import com.ecommerce.reviewservice.model.Review;
import com.ecommerce.reviewservice.repository.ReviewRepository;
import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.minidev.json.JSONObject;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private ReviewRepository reviewRepository;

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
                for (String key : requestsLastMinute.keySet()) {
                    counter += requestsLastMinute.get(key);
                    //System.out.println(key);
                }
                ref.set(counter / (double) 60);
            }
        }, 0, 3000);
    }

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
    @ApiOperation(value = "Get information from the review-service instance", notes = "Retrieve information from a review-service instance")
    public String home() {
        incrementCounter();

        return "Hello from Inventory Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @GetMapping("")
    @ApiOperation(value = "Get reviews", notes = "Retrieve reviews from the Database")
    public ResponseEntity<Map<String, Object>> getReviews(@RequestParam(defaultValue = "", required = false) String accountId,
                                                           @RequestParam(defaultValue = "", required = false) String productId,
                                                           @RequestParam(defaultValue = "-1", required = false) int stars,
                                                           @RequestParam(defaultValue = "", required = false) String comment,
                                                           @RequestParam(defaultValue = "1970-01-01T00:00:0.000+00:00", required = false) Date minCreationDate,
                                                           @RequestParam(defaultValue = "2025-01-01T00:00:0.000+00:00", required = false) Date maxCreationDate,
                                                           @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                                           @RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                           @RequestParam(value = "sort", defaultValue = "creationDate,asc", required = false) String sort) {
        incrementCounter();
        List<Review> reviews;
        PageRequest request = PageRequest.of(page, size, Sort.by(new Sort.Order(sort.split(",")[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(",")[0])));
        Page<Review> pagedReviews = reviewRepository.findByAccountIdContainingIgnoreCaseAndProductIdContainingIgnoreCaseAndCommentContainingIgnoreCaseAndCreationDateBetween(accountId, productId, comment, minCreationDate, maxCreationDate, request);
        List<Review> list = new ArrayList<>();

        //seleccionar solo las reviews con el valor stars igual al especificado, si es que se ha especificado alguno
        if (stars != -1) {
            //solo las que tengan el productId si se ha especificado
            for (int i = 0; i < pagedReviews.getContent().size(); ++i) {
                if (pagedReviews.getContent().get(i).getStars() == stars)
                    list.add(pagedReviews.getContent().get(i));
            }
            pagedReviews = new PageImpl<>(list, PageRequest.of(page, size), list.size());
        }

        reviews = pagedReviews.getContent();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("currentPage", pagedReviews.getNumber());
        response.put("totalItems", pagedReviews.getTotalElements());
        response.put("totalPages", pagedReviews.getTotalPages());
        response.put("reviews", reviews);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Get an account", notes = "Provide an Id to retrieve a specific review from the Database")
    public Review getReview(@ApiParam(value = "Id of the review to get", required = true) @PathVariable final String id) {
        incrementCounter();
        Review review;
        try {
            review = reviewRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Review not found"
            );
        }
        return review;
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete a review", notes = "Provide an Id to delete a specific review from the Database")
    public Review deleteReview(@ApiParam(value = "Id of the review to delete", required = true) @PathVariable final String id) {
        incrementCounter();
        Optional<Review> review = reviewRepository.findById(id);
        Review review1;
        try {
            //borrar cuenta
            review1 = review.get();
            reviewRepository.deleteById(id);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Review not found"
            );
        }
        return review1;
    }

    @PostMapping("")
    @ApiOperation(value = "Create a review", notes = "Provide information to create a review")
    public Review postReview(@ApiParam(value = "Information of the review to create", required = true) @RequestBody Review review) {
        incrementCounter();
        review.setCreationDate(new Date());
        return reviewRepository.save(review);
    }

    @PatchMapping("/{id}/comment")
    @ApiOperation(value = "Edit the comment of a review", notes = "Provide information to edit the comment of a review")
    public Review editComment(@ApiParam(value = "Id of the review for which the comment has to be changed", required = true) @PathVariable final String id,
                              @ApiParam(value = "New comment of the review", required = true) @RequestBody Review review) {
        incrementCounter();
        Review review1;
        try {
            review1 = reviewRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Review not found"
            );
        }
        review1.setComment(review.getComment());
        return reviewRepository.save(review1);
    }

    @PatchMapping("/{id}/stars")
    @ApiOperation(value = "Eit the stars value of a review", notes = "Provide information to edit the stars value of a review")
    public Review editStars(@ApiParam(value = "Id of the review for which the stars value has to be changed", required = true) @PathVariable final String id,
                            @ApiParam(value = "New stars value for the review", required = true) @RequestBody Review review) {
        incrementCounter();
        Review review1;
        try {
            review1 = reviewRepository.findById(id).get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Review not found"
            );
        }
        review1.setStars(review.getStars());
        return reviewRepository.save(review1);
    }

    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of Review service running at port: " + env.getProperty("local.server.port");
    }
}
