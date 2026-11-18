package com.example.mcptoolsserver.lns.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class AiesecGraphQlClient {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    public AiesecGraphQlClient(
            @Qualifier("aiesecGraphQlRestClient") RestClient restClient,

            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode execute(String operationName, String query, Map<String, Object> variables) {

        Map<String, Object> request = Map.of(
                "operationName", operationName,
                "query", query,
                "variables", variables
        );

        try {
            String response = restClient.post()

                    .contentType(MediaType.APPLICATION_JSON)

                    .accept(MediaType.APPLICATION_JSON)

                    .body(request)

                    .retrieve()

                    .body(String.class);

            if (response == null || response.isBlank()) {

                throw new IllegalStateException("AIESEC.org returned an empty GraphQL response.");
            }

            JsonNode root = objectMapper.readTree(response);

            JsonNode errors = root.path("errors");

            if (errors.isArray() && !errors.isEmpty()) {

                throw new IllegalStateException("AIESEC.org GraphQL returned errors: " + errors);

            }
            return root;

        } catch (RestClientResponseException ex) {

            throw new IllegalStateException(
                    "AIESEC.org GraphQL HTTP request failed with status " + ex.getStatusCode().value() + ".",
                    ex
            );
        } catch (IllegalStateException ex) {

            throw ex;
        } catch (Exception ex) {

            throw new IllegalStateException("Could not execute the AIESEC.org GraphQL request.", ex);
        }
    }
}
