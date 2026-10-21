package com.sledenje.teren.common.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String API_TOKEN_SCHEME = "apiToken";

    @Bean
    public OpenAPI terenOpenApi(TerenApiProperties terenApiProperties) {
        return new OpenAPI()
                .info(new Info()
                        .title("Teren API")
                        .description("REST API za aplikacijo Teren (stranke, vozila, delovni nalogi, plan).")
                        .version("v1"))
                .servers(List.of(new Server()
                        .url(terenApiProperties.publicUrl())
                        .description("Strežnik API-ja")))
                // Gumb "Authorize" v Swagger UI: vnese se vrednost API_TOKEN.
                .components(new Components().addSecuritySchemes(API_TOKEN_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description("Vrednost API_TOKEN iz .env")))
                .addSecurityItem(new SecurityRequirement().addList(API_TOKEN_SCHEME));
    }

}
