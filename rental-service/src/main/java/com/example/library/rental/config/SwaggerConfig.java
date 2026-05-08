package com.example.library.rental.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 대여 서비스 OpenAPI 문서 설정입니다.
 * http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {
    @Value("${swagger.title:Rental Service API}")
    private String title;

    @Value("${swagger.description:Library rental service API}")
    private String description;

    @Value("${swagger.version:v1.0.0}")
    private String version;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${swagger.server-url:}")
    private String serverUrl;

    @Value("${swagger.contact.name:Library Team}")
    private String contactName;

    @Value("${swagger.contact.email:support@example.com}")
    private String contactEmail;

    /**
     * Swagger UI에서 사용할 OpenAPI 메타데이터와 인증 스키마를 구성합니다.
     *
     * @return 서비스별 OpenAPI 설정 객체를 반환합니다.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(title)
                .description(description)
                .version(version)
                .contact(new Contact()
                    .name(contactName)
                    .email(contactEmail)))
            .servers(List.of(new Server()
                .url(resolveServerUrl())
                .description("API Server")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT 토큰을 입력하세요")));
    }

    private String resolveServerUrl() {
        if (StringUtils.hasText(serverUrl)) {
            return serverUrl;
        }
        return "http://localhost:" + serverPort;
    }
}
