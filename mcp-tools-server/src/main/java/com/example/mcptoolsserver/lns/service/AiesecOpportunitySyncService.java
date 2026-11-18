
package com.example.mcptoolsserver.lns.service;

import com.example.mcptoolsserver.lns.client.AiesecGraphQlClient;
import com.example.mcptoolsserver.lns.dto.OpportunitySyncResult;
import com.example.mcptoolsserver.lns.entity.SyncedRecord;
import com.example.mcptoolsserver.lns.repository.SyncedRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class AiesecOpportunitySyncService {

    private static final String SEARCH_QUERY = """
            query SearchAllOpportunitiesQuery(
              $page: Int,
              $per_page: Int,
              $filters: OpportunityFilter,
              $gpPage: Int,
              $gpPerPage: Int,
              $gpFilters: OpportunityFilter,
              $skipGpOpportunities: Boolean!
            ) {
              allOpportunity: opportunities(
                filters: $filters
                pagination: {page: $page, per_page: $per_page}
              ) {
                data {
                  id
                  title
                  programme {
                    id
                    short_name
                    short_name_display
                  }
                }
                paging {
                  total_items
                  total_pages
                  current_page
                }
              }
              globalPartnersOpportunities: opportunities(
                filters: $gpFilters
                pagination: {page: $gpPage, per_page: $gpPerPage}
              ) @skip(if: $skipGpOpportunities) {
                data {
                  id
                  title
                  programme {
                    id
                    short_name
                    short_name_display
                  }
                }
                paging {
                  total_items
                  total_pages
                  current_page
                }
              }
            }
            """;

    private static final String DETAIL_QUERY = """
            query GetOpportunityQuery($opportunityId: ID) {
              opportunity(id: $opportunityId) {
                id
                title
                description
                project_name
                project_description
                location
                company_description
                cover_photo(size: "1306x808")
                duration
                earliest_start_date
                backgrounds {
                  id
                  constant_id
                  constant_name
                  option
                }
                skills {
                  id
                  constant_id
                  constant_name
                  option
                }
                skills_developments {
                  id
                  constant_id
                  constant_name
                  option
                }
                measure_of_impacts {
                  id
                  value
                  description
                }
                branch {
                  id
                  address_detail {
                    id
                    country
                  }
                  company {
                    id
                    name
                    profile_photo(size: "160x160")
                  }
                }
                languages {
                  id
                  constant_id
                  constant_name
                  option
                }
                nationalities {
                  id
                  constant_id
                  constant_name
                  option
                }
                study_levels {
                  id
                  name
                  option
                }
                programme {
                  id
                  short_name
                  short_name_display
                }
                partner_type
                experience_type
                remote_experience_duration
                remote_experience_salary
                specifics_info {
                  salary
                  salary_currency {
                    id
                    alphabetic_code
                  }
                  salary_periodicity
                }
                sdg_info {
                  id
                  sdg_target {
                    description
                    goal_index
                    id
                    parent {
                      id
                      sdg_goal_hex_code
                    }
                    target
                    target_id
                  }
                }
                sub_product {
                  id
                  name
                }
                organisation {
                  id
                  name
                }
                weekly_activities(public_facing_only: true) {
                  id
                  activity
                  week
                  is_active
                }
                role_info {
                  selection_process
                  learning_points
                  learning_points_list
                }
                work_hours
                available_slots {
                  id
                  applications_close_date
                  available_openings
                  end_date
                  start_date
                  status
                }
                fee_and_health_insurance
                application_processing_time
                selection_processes(first: 50) {
                  edges {
                    node {
                      id
                      title
                      no_of_days
                    }
                  }
                }
                logistics_info {
                  accommodation_covered
                  accommodation_provided
                  computer_provided
                  food_covered
                  food_provided
                  no_of_meals
                  transportation_covered
                  transportation_provided
                }
              }
            }
            """;

    private final AiesecGraphQlClient graphQlClient;

    private final SyncedRecordRepository repository;

    private final ObjectMapper objectMapper;

    private final int pageSize;

    private final int globalPartnerPageSize;

    private final int maxPages;

    private final String configuredStartDate;

    private final String excludedHomeMcs;

    public AiesecOpportunitySyncService(
            AiesecGraphQlClient graphQlClient,
            SyncedRecordRepository repository,
            ObjectMapper objectMapper,
            @Value("${aiesec.opportunities.page-size:50}") int pageSize,

            @Value("${aiesec.opportunities.global-partner-page-size:50}") int globalPartnerPageSize,

            @Value("${aiesec.opportunities.max-pages:500}") int maxPages,

            @Value("${aiesec.opportunities.earliest-start-date:}") String configuredStartDate,

            @Value("${aiesec.opportunities.exclude-home-mcs:}") String excludedHomeMcs) {

        this.graphQlClient = graphQlClient;
        this.repository = repository;
        this.objectMapper = objectMapper;

        this.pageSize = Math.max(1, Math.min(pageSize, 100));
        this.globalPartnerPageSize = Math.max(1, Math.min(globalPartnerPageSize, 100));

        this.maxPages = Math.max(1, maxPages);

        this.configuredStartDate = configuredStartDate;
        this.excludedHomeMcs = excludedHomeMcs;
    }

    public OpportunitySyncResult synchronize() {

        LocalDate fromDate = parseStartDate();
        return synchronize(fromDate);
    }

    public OpportunitySyncResult synchronize(LocalDate fromDate) {

        LocalDateTime startedAt = LocalDateTime.now();

        Map<String, JsonNode> candidates = new LinkedHashMap<>();

        int regularPages = collectSearchPages(false, fromDate, candidates);

        int globalPages = collectSearchPages(true, fromDate, candidates);

        int detailsFetched = 0;
        int inserted = 0;
        int updated = 0;
        int ignored = 0;

        Map<String, Integer> savedByProgramme = new LinkedHashMap<>();

        for (String opportunityId : candidates.keySet()) {

            JsonNode detail = fetchDetails(opportunityId);

            JsonNode opportunity = detail.path("data").path("opportunity");

            if (opportunity.isMissingNode() || opportunity.isNull()) {
                continue;
            }

            detailsFetched++;

            Optional<OpportunityProgramme> programme =
                    resolveProgramme(opportunity.path("programme"));

            if (programme.isEmpty()) {
                ignored++;
                continue;
            }

            Optional<SyncedRecord> existing =
                    repository.findByExternalId(opportunityId);

            SyncedRecord record = existing.orElseGet(SyncedRecord::new);

            boolean isNew = existing.isEmpty();

            mapDetails(record, opportunity, opportunityId, programme.get());

            repository.save(record);

            if (isNew) {
                inserted++;
            } else {
                updated++;
            }

            savedByProgramme.merge(
                    programme.get().code(),
                    1,
                    Integer::sum
            );
        }

        return new OpportunitySyncResult(
                startedAt,
                LocalDateTime.now(),
                regularPages,
                globalPages,
                candidates.size(),
                detailsFetched,
                inserted,
                updated,
                ignored,
                Collections.unmodifiableMap(savedByProgramme)

        );
    }

    public List<SyncedRecord> getAllOpportunities() {

        return repository.findAllByOrderBySyncedAtDesc();
    }

    public Optional<SyncedRecord> findByExternalId(String externalId) {

        return repository.findByExternalId(externalId);
    }

    public List<SyncedRecord> getByProgramme(String programmeCode) {

        OpportunityProgramme programme =
                parseProgrammeParameter(programmeCode);

        return repository.findByProgrammeCodeOrderByEarliestStartDateAsc(
                programme.code()
        );
    }

    public List<SyncedRecord> searchByTitle(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        return repository.findByTitleContainingIgnoreCase(keyword.trim());
    }

    private int collectSearchPages(
            boolean globalPartners,
            LocalDate fromDate,
            Map<String, JsonNode> candidates) {

        int page = 1;
        int totalPages;

        do {

            Map<String, Object> filters =
                    buildFilters(fromDate, globalPartners);

            Map<String, Object> variables = new LinkedHashMap<>();

            variables.put("page", page);
            variables.put("per_page", pageSize);
            variables.put("filters", filters);

            variables.put("gpPage", page);
            variables.put("gpPerPage", globalPartnerPageSize);
            variables.put("gpFilters", buildFilters(fromDate, true));

            variables.put("skipGpOpportunities", !globalPartners);

            JsonNode root = graphQlClient.execute(
                    "SearchAllOpportunitiesQuery",
                    SEARCH_QUERY,
                    variables
            );

            String resultKey = globalPartners
                    ? "globalPartnersOpportunities"
                    : "allOpportunity";

            JsonNode result = root.path("data").path(resultKey);
            JsonNode data = result.path("data");
            JsonNode paging = result.path("paging");

            if (!data.isArray() || paging.isMissingNode()) {
                throw new IllegalStateException(
                        "AIESEC.org search response has an unexpected structure ("
                                + resultKey + ")."
                );
            }

            totalPages = paging.path("total_pages").asInt(0);

            if (totalPages > maxPages) {
                throw new IllegalStateException(
                        "Search returned " + totalPages
                                + " pages, exceeding configured safety limit "
                                + maxPages + "."
                );
            }

            for (JsonNode item : data) {

                String id = item.path("id").asText("");

                if (id.isBlank()) {
                    continue;
                }

                if (resolveProgramme(item.path("programme")).isPresent()) {

                    candidates.putIfAbsent(id, item);
                }
            }

            page++;

        } while (page <= totalPages && page <= maxPages);

        return Math.min(
                Math.max(totalPagesOrOne(page), 0),
                maxPages
        );
    }

    private int totalPagesOrOne(int nextPage) {

        return Math.max(0, nextPage - 1);
    }

    private Map<String, Object> buildFilters(
            LocalDate fromDate,
            boolean globalPartners) {

        Map<String, Object> filters = new LinkedHashMap<>();

        filters.put("q", null);

        filters.put("sort", "created_at");
        filters.put("sort_direction", "desc");

        filters.put(
                "earliest_start_date",
                Map.of("from", fromDate.toString())
        );

        List<Integer> homeMcIds = parseIds(excludedHomeMcs);

        if (!homeMcIds.isEmpty()) {
            filters.put("exclude_home_mcs", homeMcIds);
        }

        if (globalPartners) {
            filters.put("partner_type", List.of("global"));
        }

        return filters;
    }

    private JsonNode fetchDetails(String opportunityId) {

        return graphQlClient.execute(
                "GetOpportunityQuery",
                DETAIL_QUERY,
                Map.of("opportunityId", opportunityId)
        );
    }

    private Optional<OpportunityProgramme> resolveProgramme(
            JsonNode programmeNode) {

        return OpportunityProgramme.fromApi(
                programmeNode.path("short_name").asText(null),
                programmeNode.path("short_name_display").asText(null)
        );
    }

    private void mapDetails(
            SyncedRecord record,
            JsonNode opportunity,
            String opportunityId,
            OpportunityProgramme programme) {

        record.setExternalId(opportunityId);

        record.setProgrammeCode(programme.code());
        record.setProgrammeName(programme.displayName());

        record.setTitle(text(opportunity, "title", "Untitled opportunity"));
        record.setDescription(text(opportunity, "description", null));

        record.setProjectName(text(opportunity, "project_name", null));
        record.setProjectDescription(
                text(opportunity, "project_description", null)
        );

        record.setLocation(text(opportunity, "location", null));
        record.setPartnerType(text(opportunity, "partner_type", null));
        record.setExperienceType(text(opportunity, "experience_type", null));
        record.setDuration(text(opportunity, "duration", null));

        JsonNode branch = opportunity.path("branch");
        JsonNode address = branch.path("address_detail");
        JsonNode company = branch.path("company");

        String country = address.path("country").asText(null);

        if (country == null || country.isBlank()) {
            country = opportunity.path("host_lc")
                    .path("address_detail")
                    .path("country")
                    .asText(null);
        }

        record.setCountry(country);
        record.setCompanyName(company.path("name").asText(null));

        record.setEarliestStartDate(
                parseDate(opportunity.path("earliest_start_date").asText(null))
        );

        record.setBackgroundsJson(toJson(opportunity.path("backgrounds")));
        record.setSkillsJson(toJson(opportunity.path("skills")));

        record.setSkillsDevelopmentsJson(
                toJson(opportunity.path("skills_developments"))
        );

        record.setLanguagesJson(toJson(opportunity.path("languages")));
        record.setNationalitiesJson(toJson(opportunity.path("nationalities")));
        record.setStudyLevelsJson(toJson(opportunity.path("study_levels")));

        record.setAvailableSlotsJson(
                toJson(opportunity.path("available_slots"))
        );

        record.setRawDetailsJson(toJson(opportunity));

        record.setSourceUrl(
                "https://aiesec.org/opportunity/"
                        + slug(programme)
                        + "/"
                        + opportunityId
        );
    }

    private String slug(OpportunityProgramme programme) {

        return switch (programme) {
            case OGV -> "global-volunteer";
            case OGTA -> "global-talent";
            case OGTE -> "global-teacher";
        };
    }

    private String text(
            JsonNode node,
            String field,
            String defaultValue) {

        JsonNode value = node.path(field);

        return value.isMissingNode()
                || value.isNull()
                || value.asText().isBlank()
                ? defaultValue
                : value.asText();
    }

    private String toJson(JsonNode node) {

        if (node == null || node.isMissingNode() || node.isNull()) {
            return "[]";
        }

        try {

            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Could not serialize an opportunity field.",
                    ex
            );
        }
    }

    private LocalDate parseDate(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {

            return LocalDate.parse(
                    value.substring(0, Math.min(10, value.length()))
            );
        } catch (DateTimeParseException ex) {

            return null;
        }
    }

    private LocalDate parseStartDate() {

        if (configuredStartDate == null || configuredStartDate.isBlank()) {
            return LocalDate.now();
        }

        return LocalDate.parse(configuredStartDate.trim());
    }

    private List<Integer> parseIds(String csv) {

        if (csv == null || csv.isBlank()) {
            return List.of();
        }

        List<Integer> ids = new ArrayList<>();

        for (String value : csv.split(",")) {

            if (!value.isBlank()) {
                ids.add(Integer.parseInt(value.trim()));
            }
        }

        return ids;
    }

    private OpportunityProgramme parseProgrammeParameter(String value) {

        return OpportunityProgramme.fromApi(value, value)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported programme. Use OGV, OGTa or OGTe."
                ));
    }
}
