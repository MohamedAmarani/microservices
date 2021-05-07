package com.ecommerce.paypalgatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableDiscoveryClient
@EnableSwagger2

public class PaypalGatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaypalGatewayServiceApplication.class, args);
	}

}

@Configuration
class RestTemplateConfig {

	// Create a bean for restTemplate to call services
	@Bean
	public PayPalClient restTemplate() {
		return new PayPalClient();
	}

	@Bean
	public RestTemplate restTemplate1() {
		return new RestTemplate();
	}
}