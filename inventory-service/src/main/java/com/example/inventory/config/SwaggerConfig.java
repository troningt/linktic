package com.example.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de Swagger/OpenAPI
 */
@Configuration
public class SwaggerConfig {

  @Value("${app.name}")
  private String appName;

  @Value("${app.version}")
  private String appVersion;

  @Value("${app.description}")
  private String appDescription;

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title(appName + " API")
            .version(appVersion)
            .description(appDescription)
            .contact(new Contact()
                .name("Inventory Team")
                .email("inventory-team@company.com")
                .url("https://company.com/inventory-team"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT")))
        .servers(List.of(
            new Server().url("http://localhost:8082").description("Development server"),
            new Server().url("https://api.company.com").description("Production server")))
        .addSecurityItem(new SecurityRequirement().addList("apiKey"))
        .components(new io.swagger.v3.oas.models.Components()
            .addSecuritySchemes("apiKey",
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("API Key for authentication")));
  }
}