package com.gateway.apigatewayservice;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableDiscoveryClient
@EnableZuulProxy

public class ApiGatewayServiceApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(ApiGatewayServiceApplication.class, args);
		MyComponent myComponent = context.getBean(MyComponent.class);
		CustomInterceptor customInterceptor = context.getBean(CustomInterceptor.class);
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
