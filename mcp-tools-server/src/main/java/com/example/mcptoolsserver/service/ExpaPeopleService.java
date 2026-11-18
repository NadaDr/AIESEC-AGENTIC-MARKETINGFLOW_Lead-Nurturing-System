
package com.example.mcptoolsserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpaPeopleService {

    private final RestClient expaRestClient;

    private final ObjectMapper objectMapper;

    private final int pageSize;

    private static final DateTimeFormatter EXPA_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ExpaPeopleService(
            RestClient expaRestClient,
            ObjectMapper objectMapper,
            @Value("${expa.api.page-size:100}") int pageSize) {

        this.expaRestClient = expaRestClient;

        this.objectMapper = objectMapper;

        this.pageSize = Math.min(Math.max(pageSize, 1), 100);
    }

    public Map<String, Object> getPeoplePage(int page) {

        if (page < 1) {

            throw new IllegalArgumentException(
                    "Page number must be at least 1.");
        }

        String query = """
                query ExpaPeoplePage($page: Int, $perPage: Int) {
                    people: allPeople(
                        page: $page,
                        per_page: $perPage
                    ) {
                        data {
                            id
                            status
                            created_at
                            home_lc {
                                id
                                name
                            }
                            home_mc {
                                id
                                name
                            }
                        }
                        paging {
                            total_items
                            current_page
                            total_pages
                        }
                    }
                }
                """;

        Map<String, Object> requestBody = Map.of(
                "query", query,
                "variables", Map.of(
                        "page", page,
                        "perPage", pageSize
                )
        );

        try {

            JsonNode root = sendRequest(requestBody);

            JsonNode peopleNode = root.path("data").path("people");

            JsonNode records = peopleNode.path("data");

            JsonNode paging = peopleNode.path("paging");

            if (!records.isArray() || paging.isMissingNode()) {

                throw new IllegalStateException(
                        "Unexpected EXPA response structure.");
            }

            List<Map<String, Object>> people = new ArrayList<>();

            for (JsonNode record : records) {

                Map<String, Object> person =
                        objectMapper.convertValue(record, Map.class);

                people.add(person);
            }

            return Map.of(
                    "page", paging.path("current_page").asInt(),
                    "pageSize", pageSize,
                    "totalRecords", paging.path("total_items").asInt(),
                    "totalPages", paging.path("total_pages").asInt(),
                    "recordsReturned", people.size(),
                    "people", people
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Could not retrieve EXPA People page: "
                            + exception.getMessage(),
                    exception);
        }
    }

    public List<Map<String, Object>> searchPeopleByName(String name) {

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException(
                    "A student name must be provided.");
        }

        String query = """
                query ExpaPeopleSearchByName($q: String, $perPage: Int) {
                    people: allPeople(q: $q, per_page: $perPage) {
                        data {
                            id
                            full_name
                            email
                            status
                            created_at
                            contact_detail {
                                phone
                            }
                            lc_alignment {
                                keywords
                            }
                            home_lc {
                                id
                                name
                            }
                            home_mc {
                                id
                                name
                            }
                            person_profile {
                                selected_programmes
                                backgrounds {
                                    name
                                }
                                skills {
                                    constant_name
                                }
                                languages {
                                    constant_name
                                }
                            }
                        }
                    }
                }
                """;

        Map<String, Object> requestBody = Map.of(
                "query", query,
                "variables", Map.of(
                        "q", name,
                        "perPage", 20
                )
        );

        try {

            JsonNode root = sendRequest(requestBody);

            JsonNode records = root.path("data").path("people").path("data");

            if (!records.isArray()) {

                throw new IllegalStateException(
                        "Unexpected EXPA response structure.");
            }

            List<Map<String, Object>> people = new ArrayList<>();

            for (JsonNode record : records) {

                people.add(objectMapper.convertValue(record, Map.class));
            }

            return people;

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Could not search EXPA people by name \"" + name
                            + "\": " + exception.getMessage(),
                    exception);
        }
    }

    public Map<String, Object> getPersonById(String personId) {

        if (personId == null || personId.isBlank()) {

            throw new IllegalArgumentException(
                    "A student id must be provided.");
        }

        String query = """
                query ExpaPersonById($filters: PeopleFilter, $perPage: Int) {
                    people: allPeople(filters: $filters, per_page: $perPage) {
                        data {
                            id
                            full_name
                            email
                            status
                            created_at
                            contact_detail {
                                phone
                            }
                            lc_alignment {
                                keywords
                            }
                            home_lc {
                                id
                                name
                            }
                            home_mc {
                                id
                                name
                            }
                            person_profile {
                                selected_programmes
                                backgrounds {
                                    name
                                }
                                skills {
                                    constant_name
                                }
                                languages {
                                    constant_name
                                }
                            }
                        }
                    }
                }
                """;

        Map<String, Object> requestBody = Map.of(
                "query", query,
                "variables", Map.of(
                        "filters", Map.of("ids", List.of(personId)),
                        "perPage", 1
                )
        );

        try {

            JsonNode root = sendRequest(requestBody);

            JsonNode records = root.path("data").path("people").path("data");

            if (!records.isArray() || records.isEmpty()) {

                throw new IllegalStateException(
                        "EXPA person " + personId + " was not found.");
            }

            return objectMapper.convertValue(records.get(0), Map.class);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Could not retrieve EXPA person " + personId + ": "
                            + exception.getMessage(),
                    exception);
        }
    }

    public Map<String, Object> getPeopleByYear(int year) {

        Year selectedYear = Year.of(year);

        String from = selectedYear.atDay(1)
                .atStartOfDay()
                .format(EXPA_DATE_FORMAT);

        String to = selectedYear.atMonth(12)
                .atEndOfMonth()
                .atTime(23, 59, 59)
                .format(EXPA_DATE_FORMAT);

        String query = """
                query ExpaPeopleByYear(
                    $page: Int,
                    $perPage: Int,
                    $filters: PeopleFilter
                ) {
                    people: allPeople(
                        page: $page,
                        per_page: $perPage,
                        filters: $filters
                    ) {
                        data {
                            status
                        }
                        paging {
                            total_items
                            current_page
                            total_pages
                        }
                    }
                }
                """;

        Map<String, Object> filters = Map.of(
                "registered", Map.of(
                        "from", from,
                        "to", to
                )
        );

        Map<String, Integer> statusCounts = new LinkedHashMap<>();

        int totalRecords = 0;
        int totalPages = 0;

        int currentPage = 1;

        try {
            do {

                Map<String, Object> requestBody = Map.of(
                        "query", query,
                        "variables", Map.of(
                                "page", currentPage,
                                "perPage", pageSize,
                                "filters", filters
                        )
                );

                JsonNode root = sendRequest(requestBody);

                JsonNode peopleNode =
                        root.path("data").path("people");

                JsonNode records = peopleNode.path("data");

                JsonNode paging = peopleNode.path("paging");

                if (!records.isArray() || paging.isMissingNode()) {

                    throw new IllegalStateException(
                            "Unexpected EXPA response structure.");
                }

                totalRecords = paging.path("total_items").asInt();
                totalPages = paging.path("total_pages").asInt();

                for (JsonNode person : records) {

                    String status = person.path("status").asText("unknown");

                    if (status.isBlank()) {

                        status = "unknown";
                    }

                    statusCounts.merge(status, 1, Integer::sum);
                }

                currentPage++;

            } while (currentPage <= totalPages);

            return Map.of(
                    "year", year,
                    "dateFrom", from,
                    "dateTo", to,
                    "totalRegistrations", totalRecords,
                    "totalPages", totalPages,
                    "pagesProcessed", totalPages,
                    "statusCounts", statusCounts
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Could not retrieve EXPA People for year "
                            + year + ": " + exception.getMessage(),
                    exception);
        }
    }

    private JsonNode sendRequest(Map<String, Object> requestBody)
            throws Exception {

        String response = expaRestClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (response == null) {

            throw new IllegalStateException(
                    "EXPA returned an empty response.");
        }

        JsonNode root = objectMapper.readTree(response);

        if (root.has("errors")) {

            throw new IllegalStateException(
                    "EXPA GraphQL errors: " + root.path("errors"));
        }

        return root;
    }
}
