package com.xiaohelab.guard.server.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 3 配置。
 * 访问入口：
 *  - Swagger UI: http://localhost:8080/swagger-ui.html
 *  - OpenAPI JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI guardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Alzheimer Guard Server API")
                        .description("阿尔兹海默症患者协同寻回系统 - 后端 API 契约（对齐 API_V2.0 + LLD_V2.0）")
                        .version("v2.0")
                        .contact(new Contact().name("xiaohelab").email("dev@xiaohelab.com"))
                        .license(new License().name("Apache 2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("粘贴登录接口返回的 access_token")));
    }
}
