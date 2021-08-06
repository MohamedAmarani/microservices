package com.ecommerce.paypalgatewayservice;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.*;

public class PayPalClient {
    String clientId = "AfoXWVmCgWponUqf5709_KA0gOOO_OKLN6CJXcMlXEglz_evSp_9HfZA-XTlnEI_lHSt5QbVOg2kTgqo";
    String clientSecret = "ELp07QJ7bNwOoGzPsotPAGRtwW50s-COMeki0NO8INJHb8gHS5o2vWL52zlT6ltoQm8ziszfqEtqluxz";

    @Autowired
    Environment environment;

    public Map<String, String> createPayment(String accountId, String originalPrice, String discountCode, String discountedAmount, String deliveryPrice, String finalPrice) throws PayPalRESTException {
        HashMap<String, String> response = new LinkedHashMap<>();
        Amount amount = new Amount();
        amount.setCurrency("EUR");
        amount.setTotal(finalPrice);
        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        List<Transaction> transactions = new ArrayList<Transaction>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        
        redirectUrls.setCancelUrl("http://ec2-35-180-204-28.eu-west-3.compute.amazonaws.com/api/latest/paypal/cancel/" + accountId);
        redirectUrls.setReturnUrl("http://ec2-35-180-204-28.eu-west-3.compute.amazonaws.com/api/latest/paypal/success/" + accountId);

        payment.setRedirectUrls(redirectUrls);

        Payment createdPayment;
        try {
            String redirectUrl = "";
            APIContext context = new APIContext(clientId, clientSecret, "sandbox");
            createdPayment = payment.create(context);
            if (createdPayment!=null) {
                List<Links> links = createdPayment.getLinks();
                for (Links link:links) {
                    if (link.getRel().equals("approval_url")){
                        redirectUrl = link.getHref();
                        break;
                    }
                }
                response.put("status", "success");
                response.put("paymentUrl", redirectUrl);
                response.put("originalPrice", originalPrice);
                response.put("discountCode", discountCode);
                response.put("discountedAmount", discountedAmount);
                response.put("deliveryPrice", deliveryPrice);
                response.put("finalPrice", finalPrice);
            }
        } catch (PayPalRESTException e) {
            System.out.println("An error happened during payment creation!");
        }
        return response;
    }

    public Map<String, Object> completePayment(String paymentId, String PayerID){
        Map<String, Object> response = new HashMap();
        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(PayerID);
        try {
            APIContext context = new APIContext(clientId, clientSecret, "sandbox");
            Payment createdPayment = payment.execute(context, paymentExecution);
            if (createdPayment!=null){
                response.put("status", "success");
                response.put("payment", createdPayment);
            }
        } catch (PayPalRESTException e) {
            System.err.println(e.getDetails());
        }
        return response;
    }
}

