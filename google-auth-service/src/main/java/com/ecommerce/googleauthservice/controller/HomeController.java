package com.ecommerce.googleauthservice.controller;

import com.ecommerce.googleauthservice.model.AccountDTO;
import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
    private RestTemplate restTemplate1;

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

    @GetMapping("/instanceInfo")
    public ResponseEntity<String> getHello() {
        incrementCounter();
        System.out.println(message);
        System.out.println(env.getProperty("message"));
        return new ResponseEntity<String>( env.getProperty("message"), HttpStatus.OK);
    }

    @RequestMapping("/info")
    @ApiOperation(value = "Get information from the google-auth-service instance", notes = "Retrieve information from a google-auth-service instance")
    public String home() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Google Auth Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId;
    }

    @GetMapping("/fun")
    public ResponseEntity<String> getHellod() {
        incrementCounter();
        final ResponseEntity<String> res2 = restTemplate.exchange("http://account-service:8080/" + "602a72269ae0650a89271487",
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });
        return new ResponseEntity<String>(res2.getBody().toString(), HttpStatus.OK);
    }

    @GetMapping("/return")
    @ApiOperation(value = "Retrieve a Google access token out of the authorization code and generate own JWT access token for the user",
            notes = "If the user does not exist in the system, they will be added. At the end, a JWT access token will be returned for the user for which" +
                    "the authorization code was created.")
    public String hello(@ApiParam(value = "Authorization code of the user requesting access to the system", required = true) @RequestParam("code") final String code) {
        incrementCounter();
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
        System.out.println(jo.getString("access_token"));
        url = "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + jo.getString("access_token");
        ResponseEntity<String> res1 = restTemplate1.exchange(url,
                HttpMethod.GET, null, new ParameterizedTypeReference<String>() {
                });

        jo = new JSONObject(res1.getBody());

        try {
        //obtener usuario
        final ResponseEntity<AccountDTO> res2 = restTemplate.exchange("http://account-service:8080/" + jo.getString("id"),
                HttpMethod.GET, null, new ParameterizedTypeReference<AccountDTO>() {
                });
        } catch (Exception e) {
        //si no existe el usuario, crearlo
        JSONObject obj = new JSONObject();
        obj.put("id", jo.getString("id"));
        obj.put("username", jo.getString("email").split("@")[0]); //username tiene que ser null mejor
        obj.put("email", jo.getString("email"));
        obj.put("password", jo.getString("id"));
        obj.put("deliveryAddress", "Barcelona, Calle Cardedeu, 27, 2-1");
        obj.put("role", "ADMIN");
        // set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(obj.toString(), headers);

        //crear cuenta
        restTemplate.exchange("http://account-service:8080/",
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

        //get JWT token for the logged in or registered user
        final ResponseEntity<String> res4 = restTemplate.exchange("http://auth-service:8080/auth",
                HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                });

        HttpHeaders hdrs = res4.getHeaders();
        return "Authorization: " + hdrs.get("Authorization").toString();
    }

    @PreAuthorize("hasRole('GOOGLE')")
    @RequestMapping("/info1")
    public String homes() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of gallery service running at different ports.
        // We load balance among them, and display which instance received the request.
        return "Hello from Google Auth Service running at port: " + env.getProperty("local.server.port");
    }
}
