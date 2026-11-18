
package com.example.mcptoolsserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExpaConfig {

    @Bean
    public ObjectMapper objectMapper() {

        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public RestClient expaRestClient(
            @Value("${expa.api.url}") String apiUrl,

            @Value("${expa.api.token}") String token) {

        return RestClient.builder()

                .baseUrl(apiUrl)

                .defaultHeader("Authorization", token)

                .defaultHeader("Content-Type", "application/json")

                .build();

    }
}
