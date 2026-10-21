package com.sledenje.teren.common.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI terenOpenApi(TerenApiProperties terenApiProperties) {
        return new OpenAPI()
                .info(new Info()
                        .title("Teren API")
                        .description("REST API za aplikacijo Teren (stranke, vozila, delovni nalogi, plan).")
                        .version("v1"))
                .servers(List.of(new Server()
                        .url(terenApiProperties.publicUrl())
                        .description("Strežnik API-ja")));
    }

}
