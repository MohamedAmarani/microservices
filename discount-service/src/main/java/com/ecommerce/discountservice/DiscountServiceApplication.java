package com.ecommerce.discountservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableDiscoveryClient
@EnableSwagger2

public class DiscountServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscountServiceApplication.class, args);
	}

}

@Configuration
class RestTemplateConfig {

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}