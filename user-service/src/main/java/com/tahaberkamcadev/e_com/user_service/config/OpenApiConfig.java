package com.tahaberkamcadev.e_com.user_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "E-Commerce User Service API",
                description = "Production-ready user management microservice with JWT authentication, address management, and SAGA pattern support for e-commerce applications",
                version = "1.0.0",
                contact = @Contact(
                        name = "tahaberkamcadev",
                        url = "https://github.com/tahaberkamcadev"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8081", description = "Development Server"),
                @Server(url = "https://api.yourcompany.com", description = "Production Server")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Bearer token obtained from the login or register endpoint",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
