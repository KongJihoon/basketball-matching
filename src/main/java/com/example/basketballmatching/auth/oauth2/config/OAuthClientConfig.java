package com.example.basketballmatching.auth.oauth2.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(
        {KakaoOAuthProperties.class,
        OAuthSecurityProperties.class}
)
public class OAuthClientConfig {


    @Bean
    @Qualifier("kakaoRestClient")
    public RestClient kakaoRestClient(
            RestClient.Builder builder,
            KakaoOAuthProperties properties
    ) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout()).build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .requestFactory(requestFactory)
                .build();


    }


}
