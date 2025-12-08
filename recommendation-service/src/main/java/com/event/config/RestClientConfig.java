package com.event.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean("userClient")
    public RestClient userRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 3 Seconds to connect
        factory.setReadTimeout(60000);
        return builder
                .baseUrl("http://user-service:8083")
                .requestFactory(factory)
                .build();
    }

    @Bean("eventClient")
    public RestClient eventRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 3 Seconds to connect
        factory.setReadTimeout(60000);
        return builder
                .baseUrl("http://event-service:8084")
                .requestFactory(factory)
                .build();
    }
}
