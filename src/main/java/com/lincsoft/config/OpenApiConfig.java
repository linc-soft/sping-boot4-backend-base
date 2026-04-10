package com.lincsoft.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Knife4j) configuration class.
 *
 * <p>Configures the OpenAPI 3.0 specification metadata and JWT Bearer authentication scheme for the
 * API documentation UI (Knife4j / Swagger UI).
 *
 * @author 林创科技
 * @since 2026-04-10
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {

  private static final String SECURITY_SCHEME_NAME = "BearerAuth";

  /**
   * Configures the OpenAPI specification with project metadata and JWT security scheme.
   *
   * <p>The JWT Bearer authentication is configured as a global security requirement, allowing the
   * Knife4j UI "Authorize" button to set the token for all API requests.
   *
   * @return the configured OpenAPI instance
   */
  @Bean
  OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Backend Base API")
                .version("1.0.0")
                .description("Spring Boot enterprise-level backend framework API documentation")
                .contact(new Contact().name("林创科技")))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(
            new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
