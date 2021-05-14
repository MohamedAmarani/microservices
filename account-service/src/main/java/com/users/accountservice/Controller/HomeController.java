package com.users.accountservice.Controller;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.users.accountservice.model.Account;
import com.users.accountservice.model.CartItemDTO;
import com.users.accountservice.model.DeliveryDTO;
import com.users.accountservice.model.OrderDTO;
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
        obj.put("inventoryId", "60991dba2ba67f7350df05c6");
        // set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);
        restTemplate.exchange("http://cart-service:8080/",
                HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                });

        emailWelcome(account.getEmail());

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

    @PostMapping("/deliveryUpdateEmail")
    @ApiOperation(value = "Get an account", notes = "Provide an Id to retrieve a specific account from the Database")
    public void sendDeliveryUpdateEmail(@ApiParam(value = "Information of the account to create", required = true) @RequestBody DeliveryDTO deliveryDTO) throws MessagingException {
        incrementCounter();
        emailOrderCompleted("e", deliveryDTO);
    }

    public String emailOrderCompleted(String receiver, DeliveryDTO deliveryDTO) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo("scndaccounx@gmail.com");
        helper.setSubject("New on the delivery " + deliveryDTO.getId());
        String text = "<h2>Your delivery status has been updated!</h2>\n" +
                "<p style=\"font-size: 1.5em;\">The delivery " + deliveryDTO.getId() + " is now in the <strong style=\"background-color: #317399; padding: 0 5px; color: #fff;\">" + deliveryDTO.getDeliveryState() + "</strong> state, " +
                "and you will receive it in the " + deliveryDTO.getEstimatedDateOfArrival() + ". We will keep you updated of any new event.</p>\n" +
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
        System.out.println(res4.toString());
        Gson gson = new Gson();
        OrderDTO orderDTO = gson.fromJson(res4.getBody(), OrderDTO.class);

        double totalPrice = 0.0;
        //iterar sobre todos los elementos del cart del order
        for (CartItemDTO cartItemDTO : orderDTO.getCart().getItems()) {
            text += "<tr>\n" +
                    "<td>" + cartItemDTO.getProductDTO().getName() + "</td>\n" +
                    "<td>" + cartItemDTO.getProductDTO().getPrice() + "</td>\n" +
                    "<td>" + cartItemDTO.getQuantity() + "</td>\n" +
                    "</tr>\n";
            totalPrice += cartItemDTO.getProductDTO().getPrice() * (double) cartItemDTO.getQuantity();
        }
        text += "</tbody>\n" +
                "</table>\n" +
                "<p>The total price is " + totalPrice + " euros.</p>\n" +
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

    public String emailWelcome(String receiver) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        helper.setTo(receiver);

        helper.setSubject("Te has registrado correctamente");
        helper.setText("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\">\n" +
                "\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta content=\"width=device-width, initial-scale=1\" name=\"viewport\">\n" +
                "    <meta name=\"x-apple-disable-message-reformatting\">\n" +
                "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                "    <meta content=\"telephone=no\" name=\"format-detection\">\n" +
                "    <title></title>\n" +
                "    <!--[if (mso 16)]>\n" +
                "    <style type=\"text/css\">\n" +
                "    a {text-decoration: none;}\n" +
                "    </style>\n" +
                "    <![endif]-->\n" +
                "    <!--[if gte mso 9]><style>sup { font-size: 100% !important; }</style><![endif]-->\n" +
                "    <!--[if gte mso 9]>\n" +
                "<xml>\n" +
                "    <o:OfficeDocumentSettings>\n" +
                "    <o:AllowPNG></o:AllowPNG>\n" +
                "    <o:PixelsPerInch>96</o:PixelsPerInch>\n" +
                "    </o:OfficeDocumentSettings>\n" +
                "</xml>\n" +
                "<![endif]-->\n" +
                "</head>\n" +
                "\n" +
                "<body>\n" +
                "    <div class=\"es-wrapper-color\">\n" +
                "        <!--[if gte mso 9]>\n" +
                "\t\t\t<v:background xmlns:v=\"urn:schemas-microsoft-com:vml\" fill=\"t\">\n" +
                "\t\t\t\t<v:fill type=\"tile\" color=\"#f8f9fd\"></v:fill>\n" +
                "\t\t\t</v:background>\n" +
                "\t\t<![endif]-->\n" +
                "        <table class=\"es-wrapper\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                "            <tbody>\n" +
                "                <tr>\n" +
                "                    <td class=\"esd-email-paddings\" valign=\"top\">\n" +
                "                        <table cellpadding=\"0\" cellspacing=\"0\" class=\"es-content esd-header-popover\" align=\"center\">\n" +
                "                            <tbody>\n" +
                "                                <tr>\n" +
                "                                    <td class=\"esd-stripe\" align=\"center\" bgcolor=\"#f8f9fd\" style=\"background-color: #f8f9fd;\">\n" +
                "                                        <table bgcolor=\"#ffffff\" class=\"es-content-body\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\">\n" +
                "                                            <tbody>\n" +
                "                                                <tr>\n" +
                "                                                    <td class=\"esd-structure es-p10t es-p15b es-p30r es-p30l\" align=\"left\">\n" +
                "                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                            <tbody>\n" +
                "                                                                <tr>\n" +
                "                                                                    <td width=\"540\" class=\"esd-container-frame\" align=\"center\" valign=\"top\">\n" +
                "                                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                                            <tbody>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"center\" class=\"esd-block-image\" style=\"font-size: 0px;\"><a target=\"_blank\"><img src=\"https://uxyja.stripocdn.email/content/guids/CABINET_1ce849b9d6fc2f13978e163ad3c663df/images/22451592470360730.gif\" alt style=\"display: block;\" width=\"130\"></a></td>\n" +
                "                                                                                </tr>\n" +
                "                                                                            </tbody>\n" +
                "                                                                        </table>\n" +
                "                                                                    </td>\n" +
                "                                                                </tr>\n" +
                "                                                            </tbody>\n" +
                "                                                        </table>\n" +
                "                                                    </td>\n" +
                "                                                </tr>\n" +
                "                                            </tbody>\n" +
                "                                        </table>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                            </tbody>\n" +
                "                        </table>\n" +
                "                        <table cellpadding=\"0\" cellspacing=\"0\" class=\"es-content\" align=\"center\">\n" +
                "                            <tbody>\n" +
                "                                <tr>\n" +
                "                                    <td class=\"esd-stripe\" align=\"center\" bgcolor=\"#f8f9fd\" style=\"background-color: #f8f9fd;\">\n" +
                "                                        <table bgcolor=\"transparent\" class=\"es-content-body\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"background-color: transparent;\">\n" +
                "                                            <tbody>\n" +
                "                                                <tr>\n" +
                "                                                    <td class=\"esd-structure es-p20t es-p10b es-p20r es-p20l\" align=\"left\">\n" +
                "                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                            <tbody>\n" +
                "                                                                <tr>\n" +
                "                                                                    <td width=\"560\" class=\"esd-container-frame\" align=\"center\" valign=\"top\">\n" +
                "                                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                                            <tbody>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"center\" class=\"esd-block-text es-p10b\">\n" +
                "                                                                                        <h1>¡Bienvenido a ejemplo tienda eCommerce</h1>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"center\" class=\"esd-block-text es-p10t es-p10b\">\n" +
                "                                                                                        <p>Usa Drag-n-drop y editores de email HTML:</p>\n" +
                "                                                                                        <p>dos creadores en uno.</p>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                            </tbody>\n" +
                "                                                                        </table>\n" +
                "                                                                    </td>\n" +
                "                                                                </tr>\n" +
                "                                                            </tbody>\n" +
                "                                                        </table>\n" +
                "                                                    </td>\n" +
                "                                                </tr>\n" +
                "                                                <tr>\n" +
                "                                                    <td class=\"esd-structure es-p15t es-m-p15t es-m-p0b es-m-p0r es-m-p0l\" align=\"left\">\n" +
                "                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                            <tbody>\n" +
                "                                                                <tr>\n" +
                "                                                                    <td width=\"600\" class=\"esd-container-frame\" align=\"center\" valign=\"top\">\n" +
                "                                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                                            <tbody>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"center\" class=\"esd-block-image\" style=\"font-size: 0px;\"><a target=\"_blank\"><img class=\"adapt-img\" src=\"https://uxyja.stripocdn.email/content/guids/CABINET_1ce849b9d6fc2f13978e163ad3c663df/images/3991592481152831.png\" alt style=\"display: block;\" width=\"600\"></a></td>\n" +
                "                                                                                </tr>\n" +
                "                                                                            </tbody>\n" +
                "                                                                        </table>\n" +
                "                                                                    </td>\n" +
                "                                                                </tr>\n" +
                "                                                            </tbody>\n" +
                "                                                        </table>\n" +
                "                                                    </td>\n" +
                "                                                </tr>\n" +
                "                                            </tbody>\n" +
                "                                        </table>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                            </tbody>\n" +
                "                        </table>\n" +
                "                        <table cellpadding=\"0\" cellspacing=\"0\" class=\"es-content\" align=\"center\">\n" +
                "                            <tbody>\n" +
                "                                <tr>\n" +
                "                                    <td class=\"esd-stripe\" align=\"center\" bgcolor=\"#071f4f\" style=\"background-color: #071f4f; background-image: url(https://uxyja.stripocdn.email/content/guids/CABINET_1ce849b9d6fc2f13978e163ad3c663df/images/10801592857268437.png); background-repeat: no-repeat; background-position: center top;\" background=\"https://uxyja.stripocdn.email/content/guids/CABINET_1ce849b9d6fc2f13978e163ad3c663df/images/10801592857268437.png\">\n" +
                "                                        <table bgcolor=\"#ffffff\" class=\"es-content-body\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\">\n" +
                "                                            <tbody>\n" +
                "                                                <tr>\n" +
                "                                                    <td class=\"esd-structure es-p40t es-p40b es-p30r es-p30l\" align=\"left\">\n" +
                "                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                            <tbody>\n" +
                "                                                                <tr>\n" +
                "                                                                    <td width=\"540\" class=\"esd-container-frame\" align=\"center\" valign=\"top\">\n" +
                "                                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                                            <tbody>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"center\" class=\"esd-block-spacer\" height=\"20\"></td>\n" +
                "                                                                                </tr>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"left\" class=\"esd-block-text es-p10b\">\n" +
                "                                                                                        <h1 style=\"text-align: center; color: #ffffff;\"><b>Columnas — Van más allá de los límites</b></h1>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"center\" class=\"esd-block-text es-p10t es-p10b\">\n" +
                "                                                                                        <p style=\"color: #ffffff;\">Si necesitas hacer un fondo de email, expandir un banner, o resaltar el contenido, utiliza el color de columna.</p>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                            </tbody>\n" +
                "                                                                        </table>\n" +
                "                                                                    </td>\n" +
                "                                                                </tr>\n" +
                "                                                            </tbody>\n" +
                "                                                        </table>\n" +
                "                                                    </td>\n" +
                "                                                </tr>\n" +
                "                                            </tbody>\n" +
                "                                        </table>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                            </tbody>\n" +
                "                        </table>\n" +
                "                        <table cellpadding=\"0\" cellspacing=\"0\" class=\"es-content esd-footer-popover\" align=\"center\">\n" +
                "                            <tbody>\n" +
                "                                <tr>\n" +
                "                                    <td class=\"esd-stripe\" align=\"center\" bgcolor=\"#ffffff\" style=\"background-color: #ffffff;\">\n" +
                "                                        <table bgcolor=\"transparent\" class=\"es-content-body\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"background-color: transparent;\">\n" +
                "                                            <tbody>\n" +
                "                                                <tr>\n" +
                "                                                    <td class=\"esd-structure es-p40t es-p20b es-p30r es-p30l\" align=\"left\">\n" +
                "                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                            <tbody>\n" +
                "                                                                <tr>\n" +
                "                                                                    <td width=\"540\" class=\"esd-container-frame\" align=\"center\" valign=\"top\">\n" +
                "                                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                                            <tbody>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"left\" class=\"esd-block-text\">\n" +
                "                                                                                        <h2 style=\"text-align: center;\">Cómo puedes usar los bloques de manera positiva</h2>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                            </tbody>\n" +
                "                                                                        </table>\n" +
                "                                                                    </td>\n" +
                "                                                                </tr>\n" +
                "                                                            </tbody>\n" +
                "                                                        </table>\n" +
                "                                                    </td>\n" +
                "                                                </tr>\n" +
                "                                                <tr>\n" +
                "                                                    <td class=\"esd-structure es-p20t es-p20b es-p20r es-p20l\" align=\"left\">\n" +
                "                                                        <!--[if mso]><table width=\"560\" cellpadding=\"0\" cellspacing=\"0\"><tr><td width=\"270\" valign=\"top\"><![endif]-->\n" +
                "                                                        <table cellpadding=\"0\" cellspacing=\"0\" class=\"es-left\" align=\"left\">\n" +
                "                                                            <tbody>\n" +
                "                                                                <tr>\n" +
                "                                                                    <td width=\"270\" align=\"left\" class=\"esd-container-frame\">\n" +
                "                                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" bgcolor=\"#ffffff\" style=\"background-color: #ffffff; border-radius: 3px; border-collapse: separate;\">\n" +
                "                                                                            <tbody>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"center\" class=\"esd-block-image\" style=\"font-size: 0px;\"><a target=\"_blank\" class=\"rollover\"><img class=\"adapt-img rollover-first\" src=\"https://uxyja.stripocdn.email/content/guids/CABINET_1ce849b9d6fc2f13978e163ad3c663df/images/65371592833510113.jpg\" alt style=\"display: block;\" width=\"270\">\n" +
                "                                                                                            <div style=\"mso-hide:all;\"><img width=\"270\" class=\"adapt-img rollover-second\" style=\"max-height: 0px; display: none;\" src=\"https://uxyja.stripocdn.email/content/guids/CABINET_1ce849b9d6fc2f13978e163ad3c663df/images/95001592833517910.jpg\" alt></div>\n" +
                "                                                                                        </a></td>\n" +
                "                                                                                </tr>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"left\" class=\"esd-block-text es-m-txt-c es-p10t es-p15r es-p15l\">\n" +
                "                                                                                        <h3 style=\"color: #071f4f; font-size: 22px;\">Añade el nombre del producto</h3>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"left\" class=\"esd-block-text es-m-txt-c es-p5t es-p15r es-p15l\">\n" +
                "                                                                                        <p style=\"color: #999999;\">Escribe una descripción atractiva.</p>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"left\" class=\"esd-block-text es-m-txt-c es-p5t es-p5b es-p15r es-p15l\">\n" +
                "                                                                                        <p style=\"color: #cc0000; font-size: 12px;\"><strong>Incluye el precio </strong></p>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"left\" class=\"esd-block-button es-p10t es-p15b es-p15r es-p15l\"><span class=\"es-button-border\"><a href=\"https://my.stripo.email/cabinet/\" class=\"es-button\" target=\"_blank\">Crea\n" +
                "                                                                                                <!--[if !mso]><!-- --><img src=\"https://uxyja.stripocdn.email/content/guids/CABINET_1ce849b9d6fc2f13978e163ad3c663df/images/44691592486896856.png\" alt=\"icon\" width=\"22\" class=\"esd-icon-right\" align=\"absmiddle\" style=\"margin-left: 9px;\">\n" +
                "                                                                                                <!--<![endif]--></a></span></td>\n" +
                "                                                                                </tr>\n" +
                "                                                                            </tbody>\n" +
                "                                                                        </table>\n" +
                "                                                                    </td>\n" +
                "                                                                </tr>\n" +
                "                                                            </tbody>\n" +
                "                                                        </table>\n" +
                "                                                        <!--[if mso]></td><td width=\"20\"></td><td width=\"270\" valign=\"top\"><![endif]-->\n" +
                "                                                        <table cellpadding=\"0\" cellspacing=\"0\" class=\"es-right\" align=\"right\">\n" +
                "                                                            <tbody>\n" +
                "                                                                <tr>\n" +
                "                                                                    <td width=\"270\" align=\"left\" class=\"esd-container-frame\">\n" +
                "                                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" bgcolor=\"#ffffff\" style=\"background-color: #ffffff; border-radius: 3px; border-collapse: separate;\">\n" +
                "                                                                            <tbody>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"center\" class=\"esd-block-image\" style=\"font-size: 0px;\"><a target=\"_blank\" class=\"rollover\"><img class=\"adapt-img rollover-first\" src=\"https://uxyja.stripocdn.email/content/guids/CABINET_1ce849b9d6fc2f13978e163ad3c663df/images/88591592901786286.jpg\" alt style=\"display: block;\" width=\"270\">\n" +
                "                                                                                            <div style=\"mso-hide:all;\"><img width=\"270\" class=\"adapt-img rollover-second\" style=\"max-height: 0px; display: none;\" src=\"https://uxyja.stripocdn.email/content/guids/CABINET_1ce849b9d6fc2f13978e163ad3c663df/images/76501592901794063.jpg\" alt></div>\n" +
                "                                                                                        </a></td>\n" +
                "                                                                                </tr>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"left\" class=\"esd-block-text es-m-txt-c es-p10t es-p15r es-p15l\">\n" +
                "                                                                                        <h3 style=\"color: #071f4f; font-size: 22px;\">Añade el nombre del producto</h3>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"left\" class=\"esd-block-text es-m-txt-c es-p5t es-p15r es-p15l\">\n" +
                "                                                                                        <p style=\"color: #999999;\">Escribe una descripción atractiva.</p>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"left\" class=\"esd-block-text es-m-txt-c es-p5t es-p5b es-p15r es-p15l\">\n" +
                "                                                                                        <p style=\"color: #cc0000; font-size: 12px;\"><strong>Incluye el precio </strong></p>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"left\" class=\"esd-block-button es-p10t es-p15b es-p15r es-p15l\"><span class=\"es-button-border\"><a href=\"https://my.stripo.email/cabinet/\" class=\"es-button\" target=\"_blank\">Crea\n" +
                "                                                                                                <!--[if !mso]><!-- --><img src=\"https://uxyja.stripocdn.email/content/guids/CABINET_1ce849b9d6fc2f13978e163ad3c663df/images/44691592486896856.png\" alt=\"icon\" width=\"22\" class=\"esd-icon-right\" align=\"absmiddle\" style=\"margin-left: 9px;\">\n" +
                "                                                                                                <!--<![endif]--></a></span></td>\n" +
                "                                                                                </tr>\n" +
                "                                                                            </tbody>\n" +
                "                                                                        </table>\n" +
                "                                                                    </td>\n" +
                "                                                                </tr>\n" +
                "                                                            </tbody>\n" +
                "                                                        </table>\n" +
                "                                                        <!--[if mso]></td></tr></table><![endif]-->\n" +
                "                                                    </td>\n" +
                "                                                </tr>\n" +
                "                                                <tr>\n" +
                "                                                    <td class=\"esd-structure es-p30t es-p30b es-p30r es-p30l\" align=\"left\">\n" +
                "                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                            <tbody>\n" +
                "                                                                <tr>\n" +
                "                                                                    <td width=\"540\" class=\"esd-container-frame\" align=\"center\" valign=\"top\">\n" +
                "                                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                                            <tbody>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"left\" class=\"esd-block-text es-m-txt-c\">\n" +
                "                                                                                        <h2 style=\"text-align: center;\">Muestra productos desde todos los ángulos utilizando el efecto rollover.</h2>\n" +
                "                                                                                    </td>\n" +
                "                                                                                </tr>\n" +
                "                                                                            </tbody>\n" +
                "                                                                        </table>\n" +
                "                                                                    </td>\n" +
                "                                                                </tr>\n" +
                "                                                            </tbody>\n" +
                "                                                        </table>\n" +
                "                                                    </td>\n" +
                "                                                </tr>\n" +
                "                                                <tr>\n" +
                "                                                    <td class=\"esd-structure es-p20b es-m-p0t es-m-p20b es-m-p15r es-m-p15l\" align=\"left\">\n" +
                "                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                            <tbody>\n" +
                "                                                                <tr>\n" +
                "                                                                    <td width=\"600\" class=\"esd-container-frame\" align=\"center\" valign=\"top\">\n" +
                "                                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                                                            <tbody>\n" +
                "                                                                                <tr>\n" +
                "                                                                                    <td align=\"center\" class=\"esd-block-image\" style=\"font-size: 0px;\"><a target=\"_blank\"><img class=\"adapt-img\" src=\"https://uxyja.stripocdn.email/content/guids/CABINET_1ce849b9d6fc2f13978e163ad3c663df/images/23051592903098544.gif\" alt style=\"display: block;\" width=\"600\"></a></td>\n" +
                "                                                                                </tr>\n" +
                "                                                                            </tbody>\n" +
                "                                                                        </table>\n" +
                "                                                                    </td>\n" +
                "                                                                </tr>\n" +
                "                                                            </tbody>\n" +
                "                                                        </table>\n" +
                "                                                    </td>\n" +
                "                                                </tr>\n" +
                "                                            </tbody>\n" +
                "                                        </table>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                            </tbody>\n" +
                "                        </table>\n" +
                "                    </td>\n" +
                "                </tr>\n" +
                "            </tbody>\n" +
                "        </table>\n" +
                "    </div>\n" +
                "</body>\n" +
                "\n" +
                "</html>", true);
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