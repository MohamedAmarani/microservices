package com.users.accountservice.Controller;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.users.accountservice.model.*;
import com.users.accountservice.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.minidev.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
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
    @ApiOperation(value = "Get information from the account-service instance", notes = "Retrieve information from a account-service instance")
    public String home() {
        incrementCounter();
        return "Hello from Account Service running at port: " + env.getProperty("local.server.port") +
        " InstanceId " + instanceId;
    }

    @GetMapping("")
    @ApiOperation(value = "Get all accounts", notes = "Retrieve all accounts from the Database")
    public ResponseEntity<Map<String, Object>> getAccounts(@RequestParam(defaultValue = "", required = false) String username,
                                                            @RequestParam(defaultValue = "", required = false) String email,
                                                            @RequestParam(defaultValue = "", required = false) String role,
                                                            @RequestParam(defaultValue = "", required = false) String deliveryAddress,
                                                            @RequestParam(defaultValue = "1970-01-01T00:00:0.000+00:00", required = false) Date minCreationDate,
                                                            @RequestParam(defaultValue = "2025-01-01T00:00:0.000+00:00", required = false) Date maxCreationDate,
                                                            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
                                                            @RequestParam(value = "size", defaultValue = "5", required = false) int size,
                                                            @RequestParam(value = "sort", defaultValue = "creationDate,asc", required = false) String sort) {
        incrementCounter();
        List<Account> accounts;
        PageRequest request = PageRequest.of(page, size, Sort.by(new Sort.Order(sort.split(",")[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(",")[0])));
        Page<Account> pagedAccounts = userRepository.findByUsernameContainingIgnoreCaseAndEmailContainingIgnoreCaseAndRoleContainingIgnoreCaseAndDeliveryAddressContainingIgnoreCaseAndCreationDateBetween(username,email, role, deliveryAddress, minCreationDate, maxCreationDate, request);

        accounts = pagedAccounts.getContent();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("currentPage", pagedAccounts.getNumber());
        response.put("totalItems", pagedAccounts.getTotalElements());
        response.put("totalPages", pagedAccounts.getTotalPages());
        response.put("accounts", accounts);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Get an account", notes = "Provide an Id to retrieve a specific account from the Database")
    public Account getAccount(@ApiParam(value = "Id of the account to get", required = true) @PathVariable final String id) {
        incrementCounter();
        Optional<Account> account;
        try {
            account = userRepository.findById(id);
        } catch (Exception e) {
            try {
                account = userRepository.findByUsername(id);
            } catch (Exception e1) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"
                );
            }
        }
        return account.get();
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete an account", notes = "Provide an Id to delete a specific account from the Database")
    public Account deleteAccount(@ApiParam(value = "Id of the account to delete", required = true) @PathVariable final String id) {
        incrementCounter();
        Optional<Account> account = userRepository.findById(id);
        Account account1;
        try {
            //borrar cuenta
            account1 = account.get();
            userRepository.deleteById(id);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Account not found"
            );
        }
        //borrar carrito
        restTemplate.exchange("http://cart-service:8080/" + id,
                HttpMethod.DELETE, null, new ParameterizedTypeReference<String>() {
                });
        //borrar wishlist
        restTemplate.exchange("http://wishlist-service:8080/" + id,
                HttpMethod.DELETE, null, new ParameterizedTypeReference<String>() {
                });
        return account1;
    }

    @PostMapping("")
    @ApiOperation(value = "Create an account", notes = "Provide information to create an account")
    public Account postAccount(@ApiParam(value = "Information of the account to create", required = true) @RequestBody Account account) throws MessagingException {
        incrementCounter();
        //encriptar contraseña
        account.setPassword(new BCryptPasswordEncoder().encode(account.getPassword()));
        account.setCreationDate(new Date());
        if (userRepository.findByUsername(account.getUsername()).size() == 0) {
            Account account1 = userRepository.save(account);
            //crear carrito
            JSONObject obj = new JSONObject();
            obj.put("id", account1.getId());
            // crear carrito
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);
            restTemplate.exchange("http://cart-service:8080/",
                    HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                    });
            //crear wishlist
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            obj = new JSONObject();
            obj.put("id", account1.getId());
            entity = new HttpEntity<String>(obj.toString(), headers);
            restTemplate.exchange("http://wishlist-service:8080/",
                    HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                    });
            //SI ES USER ENVIAR MAIL DE BIENVENIDA
            if (account1.getRole().equals("USER"))
                emailWelcome(account1);

            return account1;
        }
        throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Account already exists"
        );
    }

    @PatchMapping("/{accountId}/password")
    @ApiOperation(value = "Change the password of an account", notes = "Provide the new password")
    public Account changePassword(@ApiParam(value = "Id of the account for which the password has to be changed", required = true) @PathVariable final String accountId,
                                         @ApiParam(value = "New password", required = true) @RequestBody Map<String, String> myJsonRequest) {
        incrementCounter();
        Optional<Account> account = userRepository.findById(accountId);
        try {
            account.get().setPassword(new BCryptPasswordEncoder().encode(myJsonRequest.get("password")));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Could not change password"
            );
        }
        return userRepository.save(account.get());
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
    @ApiOperation(value = "Send an email with the delivery state update information", notes = "Notify the required user when a delivery state is updated")
    public void sendDeliveryUpdateEmail(@ApiParam(value = "Id of the account for which a delivery state update email has to be sent", required = true) @PathVariable final String accountId,
                                        @ApiParam(value = "Information of the updated delivery", required = true) @RequestBody DeliveryDTO deliveryDTO) throws MessagingException {
        incrementCounter();
        if (!deliveryDTO.isInLastState())
            emailDeliveryUpdate(userRepository.findById(accountId).get(), deliveryDTO);
        else
            emailDeliveryDelivered(userRepository.findById(accountId).get(), deliveryDTO);
    }

    @PostMapping("/{accountId}/deliveryDateUpdateEmail")
    @ApiOperation(value = "Send an email with the delivery date update information", notes = "Notify the required user when a delivery estimated date of arrival is updated")
    public void sendDeliveryDateUpdateEmail(@ApiParam(value = "Id of the account for which a delivery date update email has to be sent", required = true) @PathVariable final String accountId,
                                        @ApiParam(value = "Information of the updated delivery", required = true) @RequestBody DeliveryDTO deliveryDTO) throws MessagingException {
        incrementCounter();
        emailDeliveryDateUpdate(userRepository.findById(accountId).get(), deliveryDTO);
    }

    @PostMapping("/{accountId}/orderSuccessEmail")
    @ApiOperation(value = "Send an email with the order information", notes = "Notify the required user when an order has been carried out successfully")
    public void sendOrderSuccessEmail(@ApiParam(value = "Id of the account that has made the order", required = true) @PathVariable final String accountId,
                                            @ApiParam(value = "Information of the delivery of the order", required = true) @RequestBody DeliveryDTO deliveryDTO) throws MessagingException {
        incrementCounter();
        emailOrderSuccess(userRepository.findById(accountId).get(), deliveryDTO);
    }

    @PostMapping("/newDiscountEmail")
    @ApiOperation(value = "Send an email with the new discount information", notes = "Notify the required users when a new discount is created")
    public void sendNewDiscountEmail(@ApiParam(value = "Information of the new discount", required = true) @RequestBody DiscountDTO discountDTO) throws MessagingException {
        incrementCounter();
        if (discountDTO.getUsers() == null) {
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

    @PostMapping("/enabledDiscountEmail")
    @ApiOperation(value = "Send an email notifying that a disabled discount is back enabled", notes = "Notify the required users when a new discount is back enabled")
    public void sendEnabledDiscountEmail(@ApiParam(value = "Information of the updated discount", required = true) @RequestBody DiscountDTO discountDTO) throws MessagingException {
        incrementCounter();
        if (discountDTO.getUsers() == null) {
            for (Account account : userRepository.findAll())
                if (account.getRole().equals("USER")) {
                    emailEnabledDiscount(account, discountDTO);
                }
        }
        else {
            for (AccountIdDTO accountIdDTO : discountDTO.getUsers())
                if (userRepository.findById(accountIdDTO.getAccountId()).get().getRole().equals("USER"))
                    emailEnabledDiscount(userRepository.findById(accountIdDTO.getAccountId()).get(), discountDTO);
        }
    }

    @PostMapping("/disabledDiscountEmail")
    @ApiOperation(value = "Send an email notifying that a specific discount has been disabled", notes = "Notify the required users when an enabled discounts gets disabled")
    public void sendDisabledDiscountEmail(@ApiParam(value = "Information of the updated discount", required = true) @RequestBody DiscountDTO discountDTO) throws MessagingException {
        incrementCounter();
        if (discountDTO.getUsers() == null) {
            for (Account account : userRepository.findAll())
                if (account.getRole().equals("USER")) {
                    emailDisabledDiscount(account, discountDTO);
                }
        }
        else {
            for (AccountIdDTO accountIdDTO : discountDTO.getUsers())
                if (userRepository.findById(accountIdDTO.getAccountId()).get().getRole().equals("USER"))
                    emailDisabledDiscount(userRepository.findById(accountIdDTO.getAccountId()).get(), discountDTO);
        }
    }

    @PostMapping("/{accountId}/reachedTargetPriceEmail")
    @ApiOperation(value = "Send an email notifying a user that a wishlist item has reached the specified target price", notes = "Notify the required user when one of their wishlist items has a price within the specified desired price")
    public void sendReachedTargetPriceEmail(@ApiParam(value = "Id of the account to notify", required = true) @PathVariable final String accountId,
                                            @ApiParam(value = "Information regarding the product whose price has been updated", required = true) @RequestBody Map<String, String> updatedProductInfo) throws MessagingException, IOException {
        incrementCounter();
        //obtener product
        ResponseEntity<ProductDTO> res = restTemplate.exchange("http://product-service:8080/" + updatedProductInfo.get("productId"),
                HttpMethod.GET, null, new ParameterizedTypeReference<ProductDTO>() {
                });
        ProductDTO productDTO = res.getBody();
        emailReachedTargetPrice(userRepository.findById(accountId).get(), productDTO, updatedProductInfo);
    }

    public String emailReachedTargetPrice(Account receiver, ProductDTO productDTO, Map<String, String> updatedProductInfo) throws MessagingException, IOException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver.getEmail());
        helper.setBcc("saasecommerce@gmail.com");
        helper.setFrom("eCommerce SaaS <saasecommerce@gmail.com>");
        helper.setSubject("A product of your wishlist has matched the target price");
        double reducedPercentage = 100 * ((productDTO.getCurrentPrice() - productDTO.getOriginalPrice()) / (double) productDTO.getOriginalPrice());
        DecimalFormat df = new DecimalFormat("#.##");
        reducedPercentage = Double.valueOf(df.format(reducedPercentage));
        String text = "<h2>Hi " + receiver.getUsername() + ", a product that you wish has now a price within your target price!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">The product " + productDTO.getName() + " of size " + productDTO.getSize() + " and id <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + productDTO.getId() + "</strong> " +
                "has changed its price from " + updatedProductInfo.get("oldPrice") + " EUR to " + productDTO.getCurrentPrice() + " EUR" + (Double.parseDouble(updatedProductInfo.get("oldPrice")) != productDTO.getOriginalPrice() ? " (" + reducedPercentage + "% from its original price of " + productDTO.getOriginalPrice() + " EUR)" : "") + ", matching the target price restriction of " +
                updatedProductInfo.get("targetPrice") + " EUR that you established on your wishlist.</p>\n" +
                "Below you can find attached the pics of the product. We will keep you updated of any new event.</p>\n" +
                "<p>Regards.</p></center>\n";

        helper.setText(text,true);
        /*//descargar y añadir fotos de product
        int cont = 0;
        for (Picture picture: productDTO.getPictures()) {
            ++cont;
            File file = new File("C:/Users/moha1/Pictures/eCommerceSaas/" + productDTO.getName() + "-picture" + cont + ".jpg");
            FileUtils.copyURLToFile(new URL(picture.getUrl()), file, 0, 0);
            helper.addAttachment(productDTO.getName() + "-picture" + cont + ".jpg", new File("C:/Users/moha1/Pictures/eCommerceSaas/" + productDTO.getName() + "-picture" + cont + ".jpg"));
        }
        javaMailSender.send(msg);

        //borrar fotos temporales del sistema
        for (Picture picture: productDTO.getPictures()) {
            ++cont;
            File file = new File("C:/Users/moha1/Pictures/eCommerceSaas/" + productDTO.getName() + "-picture" + cont + ".jpg");
            file.delete();
        }*/

        //adjuntar fotos de product
        for (Picture picture: productDTO.getPictures()) {
            //obtener resource
            final ResponseEntity<String> res4 = restTemplate.exchange("http://resource-service:8080/" + picture.getResourceId(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                    });
            Gson gson = new Gson();
            ResourceDTO resourceDTO = gson.fromJson(res4.getBody(), ResourceDTO.class);

            byte[] data = Base64.decodeBase64(resourceDTO.getData());

            try (OutputStream stream = new FileOutputStream(resourceDTO.getName())) {
                stream.write(data);
            }

            helper.addAttachment(resourceDTO.getName(), new File(resourceDTO.getName()));
        }
        javaMailSender.send(msg);

        //borrar fotos temporales del disco
        for (Picture picture: productDTO.getPictures()) {
            //obtener resource
            final ResponseEntity<String> res4 = restTemplate.exchange("http://resource-service:8080/" + picture.getResourceId(),
                    HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                    });
            Gson gson = new Gson();
            ResourceDTO resourceDTO = gson.fromJson(res4.getBody(), ResourceDTO.class);
            File file = new File(resourceDTO.getName());
            file.delete();
        }

        return msg.getSubject();
    }

    public String emailEnabledDiscount(Account receiver, DiscountDTO discountDTO) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true permite multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver.getEmail());
        helper.setBcc("saasecommerce@gmail.com");
        helper.setFrom("eCommerce SaaS <saasecommerce@gmail.com>");
        helper.setSubject("A discount code has been enabled again");
        String text = "<center><h2>Hi " + receiver.getUsername() + ", a discount is back enabled!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">You can now use the discount code <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + discountDTO.getCode() + "</strong> " +
                "again to get a " + discountDTO.getValue() + (discountDTO.isPercentage() ? "%" : " EUR") + " discount on every order of " + discountDTO.getMinimumAmount() + " EUR or more.</p>\n" +
                "You will be able to use it from "+ discountDTO.getStartDate() + " to " + discountDTO.getEndDate() + ", and it is only available for the first "
                + discountDTO.getMaxUses() + " uses. We will keep you updated of any new event.</p>\n" +
                "<p>Regards.</p></center>\n";
        helper.setText(text,true);
        //helper.addAttachment("my_photo.png", new ClassPathResource("android.png"));
        javaMailSender.send(msg);
        return msg.getSubject();
    }

    public String emailDisabledDiscount(Account receiver, DiscountDTO discountDTO) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver.getEmail());
        helper.setBcc("saasecommerce@gmail.com");
        helper.setFrom("eCommerce SaaS <saasecommerce@gmail.com>");
        helper.setSubject("A discount code has been disabled");
        String text = "<center><h2>Hi " + receiver.getUsername() + ", a discount code has been disabled!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">Due to technical issues, the discount code <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + discountDTO.getCode() + "</strong> " +
                "has been disabled, thus, it can not be redeemed at the moment. We will keep you updated of any new event.</p>\n" +
                "<p>Regards.</p></center>\n";

        helper.setText(text,true);
        //helper.addAttachment("my_photo.png", new ClassPathResource("android.png"));
        javaMailSender.send(msg);
        return msg.getSubject();
    }

    public String emailNewDiscount(Account receiver, DiscountDTO discountDTO) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver.getEmail());
        helper.setBcc("saasecommerce@gmail.com");
        helper.setFrom("eCommerce SaaS <saasecommerce@gmail.com>");
        helper.setSubject("New discount code available for you");
        String text = "<center><h2>Hi " + receiver.getUsername() + ", a new discount code has been created for you!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">You can use the discount code <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + discountDTO.getCode() + "</strong> " +
                "to get a " + discountDTO.getValue() + (discountDTO.isPercentage() ? "%" : " EUR") + " discount on every order of " + discountDTO.getMinimumAmount() + " EUR or more.</p>\n" +
                "You will be able to use it from "+ discountDTO.getStartDate() + " to " + discountDTO.getEndDate() + ", and it is only available for the "
                + discountDTO.getMaxUses() + " first uses. We will keep you updated of any new event.</p>\n" +
                "<p>Regards.</p></center>\n";

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
        String text = "<center><h2>Hi " + receiver.getUsername() + ", a delivery status has been updated!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">The delivery " + deliveryDTO.getId() + " managed by " + deliveryDTO.getDeliveryCompany() + " is now in the <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + deliveryDTO.getDeliveryState() + "</strong> state, " +
                "and you will receive it at "+ deliveryDTO.getDeliveryAddress() + ", the " + deliveryDTO.getEstimatedDateOfArrival() + ". We will keep you updated of any new event.</p>\n" +
                "<p style=\"font-size: 1.5em;\">Below you can find the details of your order " + deliveryDTO.getOrderId() + ".</p>\n" +
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
            double reducedPercentage = 100 * ((cartItemDTO.getProduct().getCurrentPrice() - cartItemDTO.getProduct().getOriginalPrice()) / (double) cartItemDTO.getProduct().getOriginalPrice());
            DecimalFormat df = new DecimalFormat("#.##");
            reducedPercentage = Double.valueOf(df.format(reducedPercentage));
            text += "<tr>\n" +
                    "<td>" + cartItemDTO.getProduct().getName() + "</td>\n" +
                    "<td>" + cartItemDTO.getProduct().getCurrentPrice() + " (" + reducedPercentage + "%)" + "</td>\n" +
                    "<td>" + cartItemDTO.getQuantity() + "</td>\n" +
                    "</tr>\n";
        }
        text += "</tbody>\n" +
                "</table>\n" +
                "<p>Regards.</p></center>\n";

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
        String text = "<center><h2>Hi " + receiver.getUsername() + ", a delivery has successfully arrived at its destination!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">" + "The delivery " + deliveryDTO.getId() + " managed by " + deliveryDTO.getDeliveryCompany() +
                " has now arrived at " + deliveryDTO.getDeliveryAddress() + ".\n" +
                "<p style=\"font-size: 1.5em;\">Below you can find the details of your order " + deliveryDTO.getOrderId() + ".</p>\n" +
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
            double reducedPercentage = 100 * ((cartItemDTO.getProduct().getCurrentPrice() - cartItemDTO.getProduct().getOriginalPrice()) / (double) cartItemDTO.getProduct().getOriginalPrice());
            DecimalFormat df = new DecimalFormat("#.##");
            reducedPercentage = Double.valueOf(df.format(reducedPercentage));
            text += "<tr>\n" +
                    "<td>" + cartItemDTO.getProduct().getName() + "</td>\n" +
                    "<td>" + cartItemDTO.getProduct().getCurrentPrice() + " (" + reducedPercentage + "%)" + "</td>\n" +
                    "<td>" + cartItemDTO.getQuantity() + "</td>\n" +
                    "</tr>\n";
        }
        text += "</tbody>\n" +
                "</table>\n\n" +
                "Thank you for the purchase, enjoy it!\n" +
                "<p>Regards.</p></center>\n";

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
        String text = "<center><h2>Hi " + receiver.getUsername() + ", a delivery date has been updated!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">The delivery " + deliveryDTO.getId() + " managed by " + deliveryDTO.getDeliveryCompany() +
                " has changed its delivery date to " + deliveryDTO.getEstimatedDateOfArrival() + ". Remember that it is in the <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + deliveryDTO.getDeliveryState() + "</strong> state and you will receive it at " + deliveryDTO.getDeliveryAddress() + ". We will keep you updated of any new event.</p>\n" +
                "<p style=\"font-size: 1.5em;\">Below you can find the details of your order " + deliveryDTO.getOrderId() + ".</p>\n" +
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
            double reducedPercentage = 100 * ((cartItemDTO.getProduct().getCurrentPrice() - cartItemDTO.getProduct().getOriginalPrice()) / (double) cartItemDTO.getProduct().getOriginalPrice());
            DecimalFormat df = new DecimalFormat("#.##");
            reducedPercentage = Double.valueOf(df.format(reducedPercentage));
            text += "<tr>\n" +
                    "<td>" + cartItemDTO.getProduct().getName() + "</td>\n" +
                    "<td>" + cartItemDTO.getProduct().getCurrentPrice() + " (" + reducedPercentage + "%)" + "</td>\n" +
                    "<td>" + cartItemDTO.getQuantity() + "</td>\n" +
                    "</tr>\n";
        }
        text += "</tbody>\n" +
                "</table>\n" +
                "<p>Regards.</p></center>\n";

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
        String text = "<center><h2>Hi " + receiver.getUsername() + ", you have just paid and confirmed an order!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">The order "+ deliveryDTO.getOrderId() +", whose delivery  " + deliveryDTO.getId() + " is managed by " + deliveryDTO.getDeliveryCompany() + ", is now in the <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + deliveryDTO.getDeliveryState() + "</strong> state, " +
                "and you will receive it at "+ deliveryDTO.getDeliveryAddress() + ", the " + deliveryDTO.getEstimatedDateOfArrival() + ". We will notify you when " + deliveryDTO.getDeliveryCompany() + " ships your order.</p>\n" +
                "<p style=\"font-size: 1.5em;\">Below you can find the details of your order " + deliveryDTO.getOrderId() + ".</p>\n" +
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
            double reducedPercentage = 100 * ((cartItemDTO.getProduct().getCurrentPrice() - cartItemDTO.getProduct().getOriginalPrice()) / (double) cartItemDTO.getProduct().getOriginalPrice());
            DecimalFormat df = new DecimalFormat("#.##");
            reducedPercentage = Double.valueOf(df.format(reducedPercentage));
            text += "<tr>\n" +
                    "<td>" + cartItemDTO.getProduct().getName() + "</td>\n" +
                    "<td>" + cartItemDTO.getProduct().getCurrentPrice() + " (" + reducedPercentage + "%)" + "</td>\n" +
                    "<td>" + cartItemDTO.getQuantity() + "</td>\n" +
                    "</tr>\n";
        }
        //dos decimales a totalPrice
        DecimalFormat df = new DecimalFormat("#.##");
        totalPrice = Double.valueOf(df.format(totalPrice));
        text += "</tbody>\n" +
                "</table>\n" +
                //"<p>The total price that you have paid is " + totalPrice + " euros.</p>\n" +
                "<p>Thank you and regards.</p></center>\n";

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
        String text = "<center><h2>Hi " + receiver.getUsername() + ", you have been signed up successfully!</h2>\n";
        text += "</tbody>\n" +
                "</table>\n" +
                "<p>Regards.</p></center>\n";
        helper.setText(text,true);
        //helper.addAttachment("my_photo.png", new ClassPathResource("android.png"));
        javaMailSender.send(msg);
        return msg.getSubject();
    }

    @GetMapping("/admin")
    public String homeAdmin() {
        incrementCounter();
        return "This is the admin area of account service running at port: " + env.getProperty("local.server.port");
    }
}