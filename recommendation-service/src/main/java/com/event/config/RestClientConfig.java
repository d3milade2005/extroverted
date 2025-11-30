package com.event.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean("userClient")
    public RestClient userRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl("http://localhost:8082")
                .build();
    }

    @Bean("eventClient")
    public RestClient eventRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl("http://localhost:8083")
                .build();
    }
}
