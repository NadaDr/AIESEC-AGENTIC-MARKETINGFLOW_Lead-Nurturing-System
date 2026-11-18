
package com.example.mcptoolsserver.lns.service;

import com.example.mcptoolsserver.lns.dto.LeadMatchResult;
import com.example.mcptoolsserver.lns.entity.SyncedRecord;
import com.example.mcptoolsserver.service.ExpaPeopleService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeadMatchingService {

    private static final int MAX_RECOMMENDATIONS = 5;

    private final ExpaPeopleService expaPeopleService;

    private final AiesecOpportunitySyncService opportunitySyncService;

    private final ObjectMapper objectMapper;

    public LeadMatchingService(
            ExpaPeopleService expaPeopleService,
            AiesecOpportunitySyncService opportunitySyncService,
            ObjectMapper objectMapper) {

        this.expaPeopleService = expaPeopleService;
        this.opportunitySyncService = opportunitySyncService;
        this.objectMapper = objectMapper;
    }

    public List<LeadMatchResult> matchPage(int page) {

        Map<String, Object> expaResponse =
                expaPeopleService.getPeoplePage(page);

        Object peopleObject = expaResponse.get("people");

        if (!(peopleObject instanceof List<?> people)) {
            throw new IllegalStateException(
                    "EXPA response does not contain a valid people list.");
        }

        List<SyncedRecord> opportunities =
                opportunitySyncService.getAllOpportunities();

        if (opportunities == null || opportunities.isEmpty()) {
            throw new IllegalStateException(
                    "No synchronized opportunities found in MySQL.");
        }

        List<LeadMatchResult> results = new ArrayList<>();

        for (Object person : people) {

            JsonNode lead = objectMapper.valueToTree(person);
            results.add(matchLead(lead, opportunities));
        }

        return results;
    }

    public LeadMatchResult matchLeadById(int page, String leadId) {

        return matchPage(page).stream()
                .filter(result -> result.leadId().equals(leadId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Lead " + leadId + " was not found on EXPA page " + page));
    }

    public LeadMatchResult matchProfile(
            Object person,
            List<SyncedRecord> candidateOpportunities) {

        JsonNode lead = objectMapper.valueToTree(person);

        if (candidateOpportunities == null || candidateOpportunities.isEmpty()) {
            String leadId = lead.path("id").asText("unknown");
            return new LeadMatchResult(
                    leadId.isBlank() ? "unknown" : leadId,
                    new ArrayList<>(extractProgrammes(lead)),
                    List.of());
        }

        return matchLead(lead, candidateOpportunities);
    }

    private LeadMatchResult matchLead(
            JsonNode lead,
            List<SyncedRecord> opportunities) {

        String leadId = lead.path("id").asText("");

        if (leadId.isBlank()) {
            leadId = "unknown";
        }

        Set<String> preferredProgrammes =
                extractProgrammes(lead);

        JsonNode profile = lead.path("person_profile");

        Set<String> backgrounds =
                extractValues(profile.path("backgrounds"));

        Set<String> skills =
                extractValues(profile.path("skills"));

        Set<String> languages =
                extractValues(profile.path("languages"));

        Set<String> studyLevels =
                extractValues(lead.path("study_levels"));

        List<LeadMatchResult.OpportunityMatch> matches =
                new ArrayList<>();

        for (SyncedRecord opportunity : opportunities) {

            JsonNode opportunityJson =
                    objectMapper.valueToTree(opportunity);

            JsonNode rawDetails = parseJson(
                    readText(opportunityJson, "rawDetailsJson"));

            String programme = readText(
                    opportunityJson, "programmeCode");

            programme = normalizeProgramme(programme);

            if (!preferredProgrammes.isEmpty()
                    && !preferredProgrammes.contains(programme)) {
                continue;
            }

            Set<String> requiredBackgrounds =
                    extractOpportunityValues(
                            opportunityJson, rawDetails,
                            "backgroundsJson", "backgrounds");

            Set<String> requiredSkills =
                    extractOpportunityValues(
                            opportunityJson, rawDetails,
                            "skillsJson", "skills");

            Set<String> requiredLanguages =
                    extractOpportunityValues(
                            opportunityJson, rawDetails,
                            "languagesJson", "languages");

            Set<String> requiredStudyLevels =
                    extractOpportunityValues(
                            opportunityJson, rawDetails,
                            "studyLevelsJson", "study_levels");

            List<String> reasons = new ArrayList<>();

            double score = calculateScore(
                    backgrounds, requiredBackgrounds,
                    skills, requiredSkills,
                    languages, requiredLanguages,
                    studyLevels, requiredStudyLevels,
                    preferredProgrammes.contains(programme),
                    !preferredProgrammes.isEmpty(),
                    reasons
            );

            String title = readText(opportunityJson, "title");
            String country = readText(opportunityJson, "country");
            String opportunityId =
                    readText(opportunityJson, "externalId");

            if (opportunityId.isBlank()) {
                opportunityId =
                        readText(opportunityJson, "external_id");
            }

            if (title.isBlank() || opportunityId.isBlank()) {
                continue;
            }

            matches.add(new LeadMatchResult.OpportunityMatch(
                    opportunityId,
                    title,
                    programme,
                    country,
                    score,
                    reasons
            ));
        }

        List<LeadMatchResult.OpportunityMatch> topFive =
                matches.stream()
                        .sorted(Comparator.comparingDouble(
                                LeadMatchResult.OpportunityMatch::matchingScore
                        ).reversed())
                        .limit(MAX_RECOMMENDATIONS)
                        .toList();

        return new LeadMatchResult(
                leadId,
                new ArrayList<>(preferredProgrammes),
                topFive
        );
    }

    private double calculateScore(
            Set<String> leadBackgrounds,
            Set<String> opportunityBackgrounds,
            Set<String> leadSkills,
            Set<String> opportunitySkills,
            Set<String> leadLanguages,
            Set<String> opportunityLanguages,
            Set<String> leadStudyLevels,
            Set<String> opportunityStudyLevels,
            boolean programmeMatches,
            boolean hasProgrammePreference,
            List<String> reasons) {

        double totalWeight = 0;
        double earnedWeight = 0;

        if (hasProgrammePreference) {
            totalWeight += 20;

            if (programmeMatches) {
                earnedWeight += 20;
                reasons.add("Matches the lead's preferred programme");
            }
        }

        double[] backgroundResult = compare(
                leadBackgrounds, opportunityBackgrounds, 30);

        totalWeight += backgroundResult[1];
        earnedWeight += backgroundResult[0];

        if (backgroundResult[0] > 0) {
            reasons.add("Academic/professional background matches");
        }

        double[] skillResult = compare(
                leadSkills, opportunitySkills, 30);

        totalWeight += skillResult[1];
        earnedWeight += skillResult[0];

        if (skillResult[0] > 0) {
            reasons.add("Skills match opportunity requirements");
        }

        double[] languageResult = compare(
                leadLanguages, opportunityLanguages, 10);

        totalWeight += languageResult[1];
        earnedWeight += languageResult[0];

        if (languageResult[0] > 0) {
            reasons.add("Languages match");
        }

        double[] studyResult = compare(
                leadStudyLevels, opportunityStudyLevels, 10);

        totalWeight += studyResult[1];
        earnedWeight += studyResult[0];

        if (studyResult[0] > 0) {
            reasons.add("Study level matches");
        }

        if (reasons.isEmpty()) {
            reasons.add("No detailed profile match could be established");
        }

        return totalWeight == 0
                ? 0
                : Math.round((earnedWeight / totalWeight) * 100.0);
    }

    private double[] compare(
            Set<String> leadValues,
            Set<String> opportunityValues,
            double weight) {

        if (leadValues.isEmpty() || opportunityValues.isEmpty()) {
            return new double[]{0, 0};
        }

        long matchingValues = leadValues.stream()
                .filter(leadValue -> opportunityValues.stream()
                        .anyMatch(opportunityValue ->
                                isSimilar(leadValue, opportunityValue)))
                .count();

        double ratio = (double) matchingValues / leadValues.size();

        return new double[]{ratio * weight, weight};
    }

    private boolean isSimilar(String first, String second) {
        return first.equals(second)
                || first.contains(second)
                || second.contains(first);
    }

    private Set<String> extractProgrammes(JsonNode lead) {

        Set<String> programmes = new LinkedHashSet<>();

        addProgrammes(programmes, lead.path("selected_programmes"));
        addProgrammes(programmes, lead.path("programme_preferences"));
        addProgrammes(programmes, lead.path("programmes"));
        addProgrammes(programmes,
                lead.path("person_profile").path("selected_programmes"));

        return programmes;
    }

    private void addProgrammes(Set<String> result, JsonNode node) {

        if (node.isArray()) {
            node.forEach(item -> addProgrammes(result, item));
            return;
        }

        if (node.isObject()) {
            String value = firstText(
                    node, "short_name", "short_name_display",
                    "name", "code");

            addProgramme(result, value);
            return;
        }

        if (node.isTextual()) {
            addProgramme(result, node.asText());
        }
    }

    private void addProgramme(Set<String> result, String value) {
        String normalized = normalizeProgramme(value);

        if (Set.of("OGV", "OGTA", "OGTE").contains(normalized)) {
            result.add(normalized);
        }
    }

    private String normalizeProgramme(String value) {

        String normalized = normalize(value).replace(" ", "");

        if (normalized.contains("OUTGOINGGLOBALVOLUNTEER")
                || normalized.equals("GV")
                || normalized.equals("OGV")) {
            return "OGV";
        }

        if (normalized.contains("OUTGOINGGLOBALTALENT")
                || normalized.equals("GLOBALTALENT")
                || normalized.equals("GTA")
                || normalized.equals("OGTA")) {
            return "OGTA";
        }

        if (normalized.contains("OUTGOINGGLOBALTEACHER")
                || normalized.equals("GLOBALTEACHER")
                || normalized.equals("GTE")
                || normalized.equals("OGTE")) {
            return "OGTE";
        }

        return normalized;
    }

    private Set<String> extractOpportunityValues(
            JsonNode opportunity,
            JsonNode rawDetails,
            String jsonField,
            String rawField) {

        JsonNode storedJson = opportunity.path(jsonField);

        Set<String> result = extractValues(parseJson(
                storedJson.isTextual() ? storedJson.asText() : ""));

        if (result.isEmpty()) {
            result.addAll(extractValues(rawDetails.path(rawField)));
        }

        return result;
    }

    private Set<String> extractValues(JsonNode node) {

        Set<String> result = new LinkedHashSet<>();

        if (node == null || node.isMissingNode() || node.isNull()) {
            return result;
        }

        if (node.isArray()) {
            node.forEach(item -> result.addAll(extractValues(item)));
            return result;
        }

        if (node.isObject()) {
            String value = firstText(
                    node, "constant_name", "name", "title",
                    "short_name", "value");

            if (!value.isBlank()) {

                result.add(normalize(value));
            } else {

                node.fields().forEachRemaining(entry ->
                        result.addAll(extractValues(entry.getValue())));
            }

            return result;
        }

        if (node.isTextual()) {
            String value = normalize(node.asText());

            if (!value.isBlank()) {
                result.add(value);
            }
        }

        return result;
    }

    private JsonNode parseJson(String value) {

        if (value == null || value.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {

            return objectMapper.createObjectNode();
        }
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull()
                ? ""
                : value.asText("");
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = readText(node, field);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return Normalizer.normalize(

                        value.toLowerCase(Locale.ROOT),

                        Normalizer.Form.NFD)

                .replaceAll("\\p{M}", "")

                .replaceAll("[^a-z0-9]+", " ")

                .trim();
    }
}
