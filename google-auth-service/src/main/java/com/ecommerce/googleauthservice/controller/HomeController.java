package com.ecommerce.googleauthservice.controller;

import com.ecommerce.googleauthservice.model.AccountDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/")
public class HomeController {
    @Autowired
    private Environment env;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RestTemplate restTemplate1;

    @Value("${message:Hello default}")
    private String message;

    @GetMapping("/hellom")
    public ResponseEntity<String> getHello() {
        System.out.println(message);
        System.out.println(env.getProperty("message"));
        return new ResponseEntity<String>(env.getProperty("message"), HttpStatus.OK);
    }

    @GetMapping("/fun")
    public ResponseEntity<String> getHellod() {
        final ResponseEntity<String> res2 = restTemplate.exchange("http://account-service/" + "602a72269ae0650a89271487",
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        return new ResponseEntity<String>(res2.getBody().toString(), HttpStatus.OK);
    }

    @GetMapping("/hello")
    public String hello(@RequestParam("code") final String code) {
        //obtener token a partir de authorization code
        String url = "https://www.googleapis.com/oauth2/v4/token?code=" + code +
                "&client_id=18414052942-t2sumb9e4q6otlc1gvrcblgu9r1p2mdg.apps.googleusercontent.com&" +
                "client_secret=szRV9z8x3WPUkJmpZe0JfKiA&" +
                "redirect_uri=http://localhost:8080/hello&" +
                "grant_type=authorization_code";

        ResponseEntity<String> res = restTemplate1.exchange(url,
                HttpMethod.POST, null, new ParameterizedTypeReference<String>() {
                });

        //obtener info del usuario loggeado con token
        JSONObject jo = new JSONObject(res.getBody());
        url = "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + jo.getString("access_token");
        ResponseEntity<String> res1 = restTemplate1.exchange(url,
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });

        jo = new JSONObject(res1.getBody());

        try {
        //obtener usuario
        final ResponseEntity<AccountDTO> res2 = restTemplate.exchange("http://account-service/" + jo.getString("id"),
                HttpMethod.GET, null, new ParameterizedTypeReference<AccountDTO>() {
                });

        } catch (Exception e) {
        //si no existe el usuario, crearlo
        JSONObject obj = new JSONObject();
        obj.put("id", jo.getString("id"));
        obj.put("username", jo.getString("email").split("@")[0]); //username tiene que ser null mejor
        obj.put("email", jo.getString("email"));
        obj.put("password", jo.getString("id"));
        obj.put("role", "GOOGLE");
        // set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);

        restTemplate.exchange("http://account-service/",
                HttpMethod.POST, entity, new ParameterizedTypeReference<AccountDTO>() {
                });
        }

        //llamar al micro de auth para obtener token JWT (contraseña null?) que pasa si te registras con username igual que el de google despues?
        JSONObject obj = new JSONObject();
        obj.put("username", jo.getString("email").split("@")[0]); //username tiene que ser null mejor
        obj.put("password", jo.getString("id"));
        // set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);

        final ResponseEntity<String> res4 = restTemplate.exchange("http://auth-service/auth",
                HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                });

        HttpHeaders hdrs = res4.getHeaders();
        return "Authorization: " + hdrs.get("Authorization").toString();
    }

    @PreAuthorize("hasRole('GOOGLE')")
    @RequestMapping("/info")
    public String home() {
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Cart Service running at port: " + env.getProperty("local.server.port");
    }
}