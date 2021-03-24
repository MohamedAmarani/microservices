package com.ecommerce.paypalgatewayservice.paypalgatewayservice;

import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping(value = "/")
public class HomeController {

    private final PayPalClient payPalClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    public HomeController(PayPalClient payPalClient){
        this.payPalClient = payPalClient;
    }

    @GetMapping("/{accountId}")
    public Object confirmPayment(@PathVariable final String accountId, @RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String PayerID) throws PayPalRESTException {
        return completePayment(accountId, paymentId, PayerID);
    }

    @GetMapping("/cancel")
    public String cancelPayment(@PathVariable final String accountId, @RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String PayerID) throws PayPalRESTException {
        completePayment(accountId, paymentId, PayerID);
        return "Algo no ha ido como debía.";
    }

    @PostMapping(value = "/paypal/make/payment/{accountId}")
    public Map<String, Object> makePayment(@PathVariable final String accountId, @RequestBody Map<String, Double> myJsonRequest) throws PayPalRESTException {
        return payPalClient.createPayment(accountId, myJsonRequest.get("totalPrice").toString());
    }

    public Object completePayment(String accountId, String paymentId, String PayerID){
        payPalClient.completePayment(paymentId, PayerID);
        ResponseEntity<Object> res = restTemplate.exchange("http://cart-service/" + accountId + "/checkout2",
                HttpMethod.PUT, null, new ParameterizedTypeReference<Object>() {
                });
        return res.getBody();
    }
}
