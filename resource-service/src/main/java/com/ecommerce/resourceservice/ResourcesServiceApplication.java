package com.ecommerce.resourceservice;

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

public class ResourcesServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourcesServiceApplication.class, args);
	}

}

@Configuration
class RestTemplateConfig {

	// Create a bean for restTemplate to call services
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}

/*@Configuration
class Config implements WebMvcConfigurer
{
	//@Autowired
	//MinimalInterceptor minimalInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry)
	{
		registry.addInterceptor(new CustomInterceptor());
	}
}*/