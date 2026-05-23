package com.nexoracommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Global Configuration for SpringDoc OpenAPI 3.
 * Configures package filtering and JWT security schemes globally.
 * 
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access OpenAPI JSON at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("Nexora-Public-API")
                .pathsToMatch("/api/v1/**")
                .packagesToScan("com.nexoracommerce")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nexora-Commerce REST API")
                        .version("1.0.0")
                        .description("E-Commerce high-performance RESTful API documentation for frontend integration.")
                        .contact(new Contact()
                                .name("Nexora Core Dev Team")
                                .email("dev@nexoracommerce.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                // Integrate standard "Authorize" lock button at top-right of Swagger UI
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Please paste your JWT Access Token in the box below to authorize.")));
    }
}



