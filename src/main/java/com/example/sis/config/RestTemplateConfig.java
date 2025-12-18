package com.example.sis.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate configuration for better HTTPS handling
 * Fixes Keycloak connectivity issues with UnknownHostException
 * Makes HttpComponentsClientHttpRequestFactory primary to fix OAuth2 client registration
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setConnectionRequestTimeout(Duration.ofSeconds(10));
        return factory;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .build();
    }
}
