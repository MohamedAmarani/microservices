package com.users.accountservice.Controller;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.users.accountservice.model.*;
import com.users.accountservice.repository.UserRepository;
import io.fabric8.kubernetes.model.util.Helper;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@RestController
@RequestMapping("/")
@Service
public class HomeController {
    @Autowired
    private Environment env;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${message:Hello default}")
    private String message;

    @Value("${htmlWelcomeText:Hello default}")
    private String htmlWelcomeText;

    @Value("${htmlOrderCompletedText:Hello default}")
    private String htmlOrderCompletedText;

    @Value("${htmlDeliveryUpdateText:Hello default}")
    private String htmlDeliveryUpdateText;

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

    @RequestMapping("/info")
    @ApiOperation(value = "Get information from the account-service instance", notes = "Retrieve information from a account-service instance")
    public String home() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Account Service running at port: " + env.getProperty("local.server.port") +
        " InstanceId " + instanceId;
    }

    @GetMapping("")
    @ApiOperation(value = "Get all accounts", notes = "Retrieve a specific account from the Database")
    public List<Account> getAccounts() {
        incrementCounter();
        List<Account> accounts = userRepository.findAll();
        return accounts;
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Get an account", notes = "Provide an Id to retrieve a specific account from the Database")
    public Account getAccount(@ApiParam(value = "Id of the account to get", required = true) @PathVariable final String id) {
        incrementCounter();
        Optional<Account> account = userRepository.findById(id);
        try {
            return account.get();
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Account not found"
            );
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete an account", notes = "Provide an Id to delete a specific account from the Database")
    public Account deleteAccount(@ApiParam(value = "Id of the account to delete", required = true) @PathVariable final String id) {
        incrementCounter();
        Optional<Account> account = userRepository.findById(id);
        try {
            Account account1 = account.get();
            userRepository.deleteById(id);
            return account1;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Account not found"
            );
        }
    }

    @PostMapping("")
    @ApiOperation(value = "Create an account", notes = "Provide information to create an account")
    public Account postAccount(@ApiParam(value = "Information of the account to create", required = true) @RequestBody Account account) throws MessagingException {
        incrementCounter();
        //encriptar contraseña
        account.setPassword(new BCryptPasswordEncoder().encode(account.getPassword()));
        Account account1 = userRepository.save(account);
        //crear carrito
        JSONObject obj = new JSONObject();
        obj.put("id", account1.getId());
        //nventario por defecto
        obj.put("inventoryId", "609d71c0607c8c13aabf9e7b");
        // crear carrito
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);
        restTemplate.exchange("http://cart-service:8080/",
                HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                });
        //SI ES USER ENVIAR MAIL DE BIENVENIDA
        if (account1.getRole().equals("USER"))
            emailWelcome(account1);

        return account1;
    }

    @PatchMapping("/{accountId}/deliveryAddress")
    @ApiOperation(value = "Change the delivery address of an account", notes = "Provide the new delivery address")
    public Account changeDeliveryAddress(@ApiParam(value = "Id of the account for which the delivery address has to be changed", required = true) @PathVariable final String accountId,
                                         @ApiParam(value = "New delivery address", required = true) @RequestBody Map<String, String> myJsonRequest) {
        incrementCounter();
        Optional<Account> account = userRepository.findById(accountId);
        try {
            account.get().setDeliveryAddress(myJsonRequest.get("deliveryAddress"));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Could not change deliveryAddress"
            );
        }
        return userRepository.save(account.get());
    }

    @PatchMapping("/{accountId}/deposit")
    @ApiOperation(value = "Add credit to an account", notes = "Provide the quantity of credit to add")
    public Account depositCredit(@ApiParam(value = "Id of the account for which a deposit of credit has to be done", required = true) @PathVariable final String accountId,
                                 @ApiParam(value = "Quantity of credit to add to the given account", required = true) @RequestBody Map<String, Integer> myJsonRequest) {
        incrementCounter();
        Optional<Account> account = userRepository.findById(accountId);
        try {
            account.get().incrementCredit(myJsonRequest.get("credit"));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Could not add credit"
            );
        }
        return userRepository.save(account.get());
    }

    @PutMapping("/{accountId}/buy")
    @ApiOperation(value = "Decrease credit after a purchase", notes = "Provide the quantity of credits to subtract from the account")
    public Account makeBuy(@ApiParam(value = "Id of the account for which credits have to be subtracted", required = true) @PathVariable final String accountId,
                           @ApiParam(value = "Quantity of credits to subtract", required = true) @RequestBody Map<String, Integer> myJsonRequest) {
        incrementCounter();
        Optional<Account> account = userRepository.findById(accountId);
        try {
            account.get().decrementCredit(myJsonRequest.get("totalPrice"));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Not enough credits"
            );
        }
        return userRepository.save(account.get());
    }

    @PostMapping("/{accountId}/deliveryUpdateEmail")
    @ApiOperation(value = "Get an account", notes = "Provide an Id to retrieve a specific account from the Database")
    public void sendDeliveryUpdateEmail(@ApiParam(value = "Id of the account for which a delivery state update email has to be sent", required = true) @PathVariable final String accountId,
                                        @ApiParam(value = "Information of the updated delivery", required = true) @RequestBody DeliveryDTO deliveryDTO) throws MessagingException {
        incrementCounter();
        if (!deliveryDTO.isInLastState())
            emailDeliveryUpdate(userRepository.findById(accountId).get(), deliveryDTO);
        else
            emailDeliveryDelivered(userRepository.findById(accountId).get(), deliveryDTO);
    }

    @PostMapping("/{accountId}/deliveryDateUpdateEmail")
    @ApiOperation(value = "Get an account", notes = "Provide an Id to retrieve a specific account from the Database")
    public void sendDeliveryDateUpdateEmail(@ApiParam(value = "Id of the account for which a delivery date update email has to be sent", required = true) @PathVariable final String accountId,
                                        @ApiParam(value = "Information of the updated delivery", required = true) @RequestBody DeliveryDTO deliveryDTO) throws MessagingException {
        incrementCounter();
        emailDeliveryDateUpdate(userRepository.findById(accountId).get(), deliveryDTO);
    }

    @PostMapping("/{accountId}/orderSuccessEmail")
    @ApiOperation(value = "Get an account", notes = "Provide an Id to retrieve a specific account from the Database")
    public void sendOrderSuccessEmail(@ApiParam(value = "Id of the account for which a delivery date update email has to be sent", required = true) @PathVariable final String accountId,
                                            @ApiParam(value = "Information of the updated delivery", required = true) @RequestBody DeliveryDTO deliveryDTO) throws MessagingException {
        incrementCounter();
        emailOrderSuccess(userRepository.findById(accountId).get(), deliveryDTO);
    }

    @PostMapping("/newDiscountEmail")
    @ApiOperation(value = "Get an account", notes = "Provide an Id to retrieve a specific account from the Database")
    public void sendNewDiscountEmail(@ApiParam(value = "Information of the updated delivery", required = true) @RequestBody DiscountDTO discountDTO) throws MessagingException {
        incrementCounter();
        if (discountDTO.getUsers() != null) {
            for (Account account : userRepository.findAll())
                if (account.getRole().equals("USER")) {
                    emailNewDiscount(account, discountDTO);
                }
        }
        else {
            for (AccountIdDTO accountIdDTO : discountDTO.getUsers())
                if (userRepository.findById(accountIdDTO.getAccountId()).get().getRole().equals("USER"))
                    emailNewDiscount(userRepository.findById(accountIdDTO.getAccountId()).get(), discountDTO);
        }
    }

    public String emailNewDiscount(Account receiver, DiscountDTO discountDTO) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver.getEmail());
        helper.setBcc("saasecommerce@gmail.com");
        helper.setFrom("eCommerce SaaS <saasecommerce@gmail.com>");
        helper.setSubject("New discount code available for you");
        String text = "<h2>Hi " + receiver.getUsername() + ", a new discount code has been created for you!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">You can use the discount code <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + discountDTO.getCode() + "</strong> " +
                "to get a " + discountDTO.getValue() + (discountDTO.isPercentage() ? "%" : "") + " discount on every order of " + discountDTO.getMinimumAmount() + " EUR or more.</p>\n" +
                "You will be able to use it from "+ discountDTO.getStartDate() + " to " + discountDTO.getEndDate() + ", and it is only available for the "
                + discountDTO.getMaxUses() + " first uses. We will keep you updated of any new event.</p>\n" +
                "<p>Regards.</p>\n";

        helper.setText(text,true);
        //helper.addAttachment("my_photo.png", new ClassPathResource("android.png"));
        javaMailSender.send(msg);
        return msg.getSubject();
    }

    public String emailDeliveryUpdate(Account receiver, DeliveryDTO deliveryDTO) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver.getEmail());
        helper.setBcc("saasecommerce@gmail.com");
        helper.setFrom("eCommerce SaaS <saasecommerce@gmail.com>");
        helper.setSubject("News on the delivery " + deliveryDTO.getId());
        String text = "<h2>Hi " + receiver.getUsername() + ", a delivery status has been updated!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">The delivery " + deliveryDTO.getId() + " managed by " + deliveryDTO.getDeliveryCompany() + " is now in the <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + deliveryDTO.getDeliveryState() + "</strong> state, " +
                "and you will receive it at "+ deliveryDTO.getDeliveryAddress() + ", the " + deliveryDTO.getEstimatedDateOfArrival() + ". We will keep you updated of any new event.</p>\n" +
                "<p style=\"font-size: 1.5em;\">Below you can find the details of your order " + deliveryDTO.getOrderId() + ". " +
                "The <strong>visual editor</strong> on the right and the <strong>source editor</strong> on the left are linked together and the changes are reflected in the other one as you type! <img src=\"https://html5-editor.net/images/smiley.png\" alt=\"smiley\" /></p>\n" +
                "<table class=\"editorDemoTable\">\n" +
                "<tbody>\n" +
                "<tr>\n" +
                "<td><strong>Product name</strong></td>\n" +
                "<td><strong>Price</strong></td>\n" +
                "<td><strong>Quantity</strong></td>\n" +
                "</tr>\n";
        //obtener order
        final ResponseEntity<String> res4 = restTemplate.exchange("http://order-service:8080/" + deliveryDTO.getOrderId(),
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        Gson gson = new Gson();
        OrderDTO orderDTO = gson.fromJson(res4.getBody(), OrderDTO.class);
        //iterar sobre todos los elementos del cart del order
        for (CartItemDTO cartItemDTO : orderDTO.getCart().getItems()) {
            text += "<tr>\n" +
                    "<td>" + cartItemDTO.getProduct().getName() + "</td>\n" +
                    "<td>" + cartItemDTO.getProduct().getPrice() + "</td>\n" +
                    "<td>" + cartItemDTO.getQuantity() + "</td>\n" +
                    "</tr>\n";
        }
        text += "</tbody>\n" +
                "</table>\n" +
                "<p>Regards.</p>\n";

        helper.setText(text,true);
        //helper.addAttachment("my_photo.png", new ClassPathResource("android.png"));
        javaMailSender.send(msg);
        return msg.getSubject();
    }

    public String emailDeliveryDelivered(Account receiver, DeliveryDTO deliveryDTO) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver.getEmail());
        helper.setBcc("saasecommerce@gmail.com");
        helper.setFrom("eCommerce SaaS <saasecommerce@gmail.com>");
        helper.setSubject("News on the delivery " + deliveryDTO.getId());
        String text = "<h2>Hi " + receiver.getUsername() + ", a delivery has successfully arrived at its destination!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">" + "The delivery " + deliveryDTO.getId() + " managed by " + deliveryDTO.getDeliveryCompany() +
                " has now arrived at " + deliveryDTO.getDeliveryAddress() + ".\n" +
                "<p style=\"font-size: 1.5em;\">Below you can find the details of your order " + deliveryDTO.getOrderId() + ". " +
                "The <strong>visual editor</strong> on the right and the <strong>source editor</strong> on the left are linked together and the changes are reflected in the other one as you type! <img src=\"https://html5-editor.net/images/smiley.png\" alt=\"smiley\" /></p>\n" +
                "<table class=\"editorDemoTable\">\n" +
                "<tbody>\n" +
                "<tr>\n" +
                "<td><strong>Product name</strong></td>\n" +
                "<td><strong>Price</strong></td>\n" +
                "<td><strong>Quantity</strong></td>\n" +
                "</tr>\n";
        //obtener order
        final ResponseEntity<String> res4 = restTemplate.exchange("http://order-service:8080/" + deliveryDTO.getOrderId(),
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        Gson gson = new Gson();
        OrderDTO orderDTO = gson.fromJson(res4.getBody(), OrderDTO.class);
        //iterar sobre todos los elementos del cart del order
        for (CartItemDTO cartItemDTO : orderDTO.getCart().getItems()) {
            text += "<tr>\n" +
                    "<td>" + cartItemDTO.getProduct().getName() + "</td>\n" +
                    "<td>" + cartItemDTO.getProduct().getPrice() + "</td>\n" +
                    "<td>" + cartItemDTO.getQuantity() + "</td>\n" +
                    "</tr>\n";
        }
        text += "</tbody>\n" +
                "</table>\n" +
                "Thank you for the purchase, enjoy it!\n" +
                "<p>Regards.</p>\n";

        helper.setText(text,true);
        //helper.addAttachment("my_photo.png", new ClassPathResource("android.png"));
        javaMailSender.send(msg);
        return msg.getSubject();
    }

    public String emailDeliveryDateUpdate(Account receiver, DeliveryDTO deliveryDTO) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver.getEmail());
        helper.setBcc("saasecommerce@gmail.com");
        helper.setFrom("eCommerce SaaS <saasecommerce@gmail.com>");
        helper.setSubject("News on the delivery " + deliveryDTO.getId());
        String text = "<h2>Hi " + receiver.getUsername() + ", a delivery date has been updated!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">The delivery " + deliveryDTO.getId() + " managed by " + deliveryDTO.getDeliveryCompany() + " in the  " +
                "has changed its delivery date to " + deliveryDTO.getEstimatedDateOfArrival() + ". Remember that it is in the <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + deliveryDTO.getDeliveryState() + "</strong> state and you will receive it at " + deliveryDTO.getDeliveryAddress() + ". We will keep you updated of any new event.</p>\n" +
                "<p style=\"font-size: 1.5em;\">Below you can find the details of your order " + deliveryDTO.getOrderId() + ". " +
                "The <strong>visual editor</strong> on the right and the <strong>source editor</strong> on the left are linked together and the changes are reflected in the other one as you type! <img src=\"https://html5-editor.net/images/smiley.png\" alt=\"smiley\" /></p>\n" +
                "<table class=\"editorDemoTable\">\n" +
                "<tbody>\n" +
                "<tr>\n" +
                "<td><strong>Product name</strong></td>\n" +
                "<td><strong>Price</strong></td>\n" +
                "<td><strong>Quantity</strong></td>\n" +
                "</tr>\n";
        //obtener order
        final ResponseEntity<String> res4 = restTemplate.exchange("http://order-service:8080/" + deliveryDTO.getOrderId(),
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        Gson gson = new Gson();
        OrderDTO orderDTO = gson.fromJson(res4.getBody(), OrderDTO.class);
        //iterar sobre todos los elementos del cart del order
        for (CartItemDTO cartItemDTO : orderDTO.getCart().getItems()) {
            text += "<tr>\n" +
                    "<td>" + cartItemDTO.getProduct().getName() + "</td>\n" +
                    "<td>" + cartItemDTO.getProduct().getPrice() + "</td>\n" +
                    "<td>" + cartItemDTO.getQuantity() + "</td>\n" +
                    "</tr>\n";
        }
        text += "</tbody>\n" +
                "</table>\n" +
                "<p>Regards.</p>\n";

        helper.setText(text,true);
        //helper.addAttachment("my_photo.png", new ClassPathResource("android.png"));
        javaMailSender.send(msg);
        return msg.getSubject();
    }

    public String emailOrderSuccess(Account receiver, DeliveryDTO deliveryDTO) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver.getEmail());
        helper.setBcc("saasecommerce@gmail.com");
        helper.setFrom("eCommerce SaaS <saasecommerce@gmail.com>");
        helper.setSubject("Order done successfully " + deliveryDTO.getOrderId());
        String text = "<h2>Hi " + receiver.getUsername() + ", you have just paid and confirmed an order!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">The order "+ deliveryDTO.getOrderId() +", whose delivery  " + deliveryDTO.getId() + " is managed by " + deliveryDTO.getDeliveryCompany() + ", is now in the <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + deliveryDTO.getDeliveryState() + "</strong> state, " +
                "and you will receive it at "+ deliveryDTO.getDeliveryAddress() + ", the " + deliveryDTO.getEstimatedDateOfArrival() + ". We will notify you when " + deliveryDTO.getDeliveryCompany() + " ships your order.</p>\n" +
                "<p style=\"font-size: 1.5em;\">Below you can find the details of your order " + deliveryDTO.getOrderId() + ". " +
                "The <strong>visual editor</strong> on the right and the <strong>source editor</strong> on the left are linked together and the changes are reflected in the other one as you type! <img src=\"https://html5-editor.net/images/smiley.png\" alt=\"smiley\" /></p>\n" +
                "<table class=\"editorDemoTable\">\n" +
                "<tbody>\n" +
                "<tr>\n" +
                "<td><strong>Product name</strong></td>\n" +
                "<td><strong>Price</strong></td>\n" +
                "<td><strong>Quantity</strong></td>\n" +
                "</tr>\n";
        //obtener order
        final ResponseEntity<String> res4 = restTemplate.exchange("http://order-service:8080/" + deliveryDTO.getOrderId(),
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        Gson gson = new Gson();
        OrderDTO orderDTO = gson.fromJson(res4.getBody(), OrderDTO.class);
        double totalPrice = 0.0;
        //iterar sobre todos los elementos del cart del order
        for (CartItemDTO cartItemDTO : orderDTO.getCart().getItems()) {
            text += "<tr>\n" +
                    "<td>" + cartItemDTO.getProduct().getName() + "</td>\n" +
                    "<td>" + cartItemDTO.getProduct().getPrice() + "</td>\n" +
                    "<td>" + cartItemDTO.getQuantity() + "</td>\n" +
                    "</tr>\n";
            totalPrice += cartItemDTO.getProduct().getPrice() * (double) cartItemDTO.getQuantity();
        }
        //dos decimales a totalPrice
        DecimalFormat df = new DecimalFormat("#.##");
        totalPrice = Double.valueOf(df.format(totalPrice));
        text += "</tbody>\n" +
                "</table>\n" +
                "<p>The total price that you have paid is " + totalPrice + " euros.</p>\n" +
                "<p>Regards.</p>\n";

        helper.setText(text,true);
        //helper.addAttachment("my_photo.png", new ClassPathResource("android.png"));
        javaMailSender.send(msg);
        return msg.getSubject();
    }

    public String emailWelcome(Account receiver) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver.getEmail());
        helper.setBcc("saasecommerce@gmail.com");
        helper.setFrom("eCommerce SaaS <saasecommerce@gmail.com>");
        helper.setSubject("Welcome");
        String text = "<h2>Hi " + receiver.getUsername() + ", you have been signed up successfully!</h2>\n";
        text += "</tbody>\n" +
                "</table>\n" +
                "<p>Regards.</p>\n";
        helper.setText(text,true);
        //helper.addAttachment("my_photo.png", new ClassPathResource("android.png"));
        javaMailSender.send(msg);
        return msg.getSubject();
    }

    public String emailDeliveryUpdate(String receiver) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver);
        helper.setBcc("saasecommerce@gmail.com");
        helper.setFrom("eCommerce SaaS <saasecommerce@gmail.com>");
        helper.setSubject("Te has registrado correctamente");
        helper.setText( "<h2>Your delivery status has been updated!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">The delivery 'id' is now in the <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">type your text</strong> status. We will keep you updated of any new event.</p>\n" +
                "<p style=\"font-size: 1.5em;\">Below you can find the details of your order 'id'.The <strong>visual editor</strong> on the right and the <strong>source editor</strong> on the left are linked together and the changes are reflected in the other one as you type! <img src=\"https://html5-editor.net/images/smiley.png\" alt=\"smiley\" /></p>\n" +
                "<table class=\"editorDemoTable\" style=\"height: 88px;\" width=\"167\">\n" +
                "<tbody>\n" +
                "<tr>\n" +
                "<td style=\"width: 58px;\"><strong>Product name</strong></td>\n" +
                "<td style=\"width: 70px;\"><strong>Price</strong></td>\n" +
                "<td style=\"width: 30px;\"><strong>Quantity</strong></td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "<td style=\"width: 58px;\">Camisetita guapa</td>\n" +
                "<td style=\"width: 70px;\">Chicago</td>\n" +
                "<td style=\"width: 30px;\">23</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "<td style=\"width: 58px;\">Lucy</td>\n" +
                "<td style=\"width: 70px;\">Wisconsin</td>\n" +
                "<td style=\"width: 30px;\">19</td>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "<td style=\"width: 58px;\">Amanda</td>\n" +
                "<td style=\"width: 70px;\">Madison</td>\n" +
                "<td style=\"width: 30px;\">22</td>\n" +
                "</tr>\n" +
                "</tbody>\n" +
                "</table>\n" +
                "<p>Regards.</p>",true);
        //helper.addAttachment("my_photo.png", new ClassPathResource("android.png"));
        javaMailSender.send(msg);
        return msg.getSubject();
    }

    // -------- Admin Area --------
    // This method should only be accessed by users with role of 'admin'
    // We'll add the logic of role based auth later
    //@PreAuthorize("hasRole('GOOGLE')")
    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of Gallery service running at port: " + env.getProperty("local.server.port");
    }
}