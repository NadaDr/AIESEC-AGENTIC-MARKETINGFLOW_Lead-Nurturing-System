
package com.example.mcptoolsserver.lns.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiesecGraphQlConfig {

    @Bean(name = "aiesecGraphQlRestClient")
    public RestClient aiesecGraphQlRestClient(
            @Value("${aiesec.api.url:https://gis-api.aiesec.org/graphql}")

            String apiUrl,

            @Value("${aiesec.api.token:}")

            String token,

            @Value("${aiesec.api.authorization-header:Authorization}")

            String authorizationHeader,

            @Value("${aiesec.api.authorization-prefix:}")

            String authorizationPrefix) {

        RestClient.Builder builder = RestClient.builder()

                .baseUrl(apiUrl)

                .defaultHeader("Content-Type", "application/json")

                .defaultHeader("Accept", "application/json");

        if (token != null && !token.isBlank()) {

            builder.defaultHeader(
                    authorizationHeader,
                    (authorizationPrefix == null ? "" : authorizationPrefix)
                            + token.trim()

            );
        }

        return builder.build();

    }
}
