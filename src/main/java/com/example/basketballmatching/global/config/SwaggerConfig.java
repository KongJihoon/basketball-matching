package com.example.basketballmatching.global.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;


@OpenAPIDefinition(
        info = @Info(
                title = "BasketBallMatching API Document",
                description = "API Document",
                version = "v0.1"

        ),
        tags = {
                @Tag(name = "USER", description = "회원 기능"),
                @Tag(name = "AUTH", description = "인증/인가"),
                @Tag(name = "REPORT", description = "유저 신고"),
                @Tag(name = "GAME", description = "경기 생성자 기능"),
                @Tag(name = "PARTICIPANT", description = "경기 참가 기능"),
                @Tag(name = "GAME_USER", description = "경기 참가자 기능"),
                @Tag(name = "NOTIFICATION", description = "알림 기능"),
                @Tag(name = "BLACK_LIST", description = "블랙리스트 기능"),
                @Tag(name = "OAUTH2", description = "소셜 로그인 (Kakao)")
        }
)
@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    private final ObjectMapper objectMapper;

    @PostConstruct
    public void initialize() {
        TypeNameResolver typeNameResolver = new TypeNameResolver() {
            @Override
            public String getNameOfClass(Class<?> cls) {

                String className = cls.getName();
                int lastIndex = className.lastIndexOf('.');
                return className.substring(lastIndex + 1).replace("$", ".");
            }
        };

        ModelConverters.getInstance().addConverter(
                new ModelResolver(objectMapper, typeNameResolver)
        );
    }

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("Bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER).name("Authorization");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                .components(
                        new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .security(Arrays.asList(securityRequirement));


    }

}
