package com.gateway.apigatewayservice;

import com.gateway.apigatewayservice.requestsinterceptor.CustomInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableDiscoveryClient
@EnableZuulProxy

public class ApiGatewayServiceApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(ApiGatewayServiceApplication.class, args);
	}

}

@Configuration
class Config implements WebMvcConfigurer
{
	//@Autowired
	//MinimalInterceptor minimalInterceptor;
	@Autowired
	CustomInterceptor customInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry)
	{
		registry.addInterceptor(customInterceptor);
	}
}
