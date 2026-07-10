package com.tahaberkamcadev.api_gateway;

import com.tahaberkamcadev.api_gateway.properties.JwtProperties;
import com.tahaberkamcadev.api_gateway.properties.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, RateLimitProperties.class})
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}
