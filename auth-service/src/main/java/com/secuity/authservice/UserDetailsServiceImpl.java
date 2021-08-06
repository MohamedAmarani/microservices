package com.secuity.authservice;

import java.text.SimpleDateFormat;
import java.util.*;

import com.google.common.util.concurrent.AtomicDouble;
import com.secuity.authservice.model.AccountDTO;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@RestController
@RequestMapping("/")
@Service
public class UserDetailsServiceImpl implements UserDetailsService  {

    @Autowired
    private Environment env;

    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private RestTemplate restTemplate;

    private AtomicDouble ref;

    private Map<String, Integer> requestsLastMinute = new HashMap<>();

    public UserDetailsServiceImpl(MeterRegistry registry) {
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

    @GetMapping("/auth/info")
    public String getInfo() {
        incrementCounter();
        // This is useful for debugging
        // When having multiple instance of product service running at different ports.
        // We load balance among them, and display which instance received the request.
        int counter = 0;
        for (String key: requestsLastMinute.keySet()) {
            counter += requestsLastMinute.get(key);
        }
        return "Hello from Auth Service running at port: " + env.getProperty("local.server.port") +
                " InstanceId " + instanceId + " " + counter;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        incrementCounter();
        final ResponseEntity<ResponseEntity<Map<String, List<AccountDTO>>>> res = restTemplate.exchange("http://account-service:8080/",
                        HttpMethod.GET, null, new ParameterizedTypeReference<ResponseEntity<Map<String, List<AccountDTO>>>>() {});
        ResponseEntity<Map<String, List<AccountDTO>>> response = res.getBody();
        List<AccountDTO> accounts = response.getBody().get("accounts");

        for (AccountDTO accountDTO: accounts) {
            if (accountDTO.getUsername().equals(username) && accountDTO.getRole().equals("ADMIN")) {
                List<GrantedAuthority> grantedAuthorities = AuthorityUtils
                        .commaSeparatedStringToAuthorityList("ROLE_" + accountDTO.getRole());

                return new User(accountDTO.getUsername(), accountDTO.getPassword(), grantedAuthorities);
            }
        }
        throw new UsernameNotFoundException("Username: " + username + " not found");
    }

}