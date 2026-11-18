package com.example.mcptoolsserver.lns.service;

import com.example.mcptoolsserver.lns.dto.LeadMatchResult;
import com.example.mcptoolsserver.lns.entity.ExploredOpportunity;
import com.example.mcptoolsserver.lns.entity.SyncedRecord;
import com.example.mcptoolsserver.lns.repository.ExploredOpportunityRepository;
import com.example.mcptoolsserver.service.ExpaPeopleService;
import com.example.mcptoolsserver.service.MailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OpportunityMatchingPipelineService {

    private static final int MAX_SUGGESTIONS = 3;

    private static final String FALLBACK_EMAIL = "nada.dridi@polytechnicien.tn";

    private static final String EMAIL_SUBJECT = "Opportunities suggested for you";

    private static final String EMAIL_SIGNATURE = "AIESEC in Tunisia - Sousse";

    private final ExpaPeopleService expaPeopleService;
    private final AiesecOpportunitySyncService opportunitySyncService;
    private final LeadMatchingService leadMatchingService;
    private final ExploredOpportunityRepository exploredRepository;
    private final MailService mailService;
    private final ObjectMapper objectMapper;

    public OpportunityMatchingPipelineService(
            ExpaPeopleService expaPeopleService,
            AiesecOpportunitySyncService opportunitySyncService,
            LeadMatchingService leadMatchingService,
            ExploredOpportunityRepository exploredRepository,
            MailService mailService,
            ObjectMapper objectMapper) {

        this.expaPeopleService = expaPeopleService;
        this.opportunitySyncService = opportunitySyncService;
        this.leadMatchingService = leadMatchingService;
        this.exploredRepository = exploredRepository;
        this.mailService = mailService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> searchStudents(String name) {
        return expaPeopleService.searchPeopleByName(name);
    }

    public Map<String, Object> suggestOpportunities(String studentExternalId) {

        Map<String, Object> student = expaPeopleService.getPersonById(studentExternalId);
        JsonNode studentNode = objectMapper.valueToTree(student);

        List<SyncedRecord> allOpportunities = opportunitySyncService.getAllOpportunities();

        if (allOpportunities.isEmpty()) {
            throw new IllegalStateException(
                    "No synchronized opportunities found in MySQL. "
                            + "Run sync_aiesec_opportunities first.");
        }

        Set<String> alreadyExplored = exploredRepository
                .findByStudentExternalId(studentExternalId)
                .stream()
                .map(ExploredOpportunity::getOpportunityExternalId)
                .collect(Collectors.toSet());

        List<SyncedRecord> candidateOpportunities = allOpportunities.stream()
                .filter(opportunity -> !alreadyExplored.contains(opportunity.getExternalId()))
                .toList();

        LeadMatchResult matchResult =
                leadMatchingService.matchProfile(studentNode, candidateOpportunities);

        List<LeadMatchResult.OpportunityMatch> newSuggestions =
                matchResult.recommendedOpportunities().stream()
                        .limit(MAX_SUGGESTIONS)
                        .toList();

        String studentName = textOrNull(studentNode, "full_name");
        String studentEmail = textOrNull(studentNode, "email");

        LocalDateTime now = LocalDateTime.now();

        for (LeadMatchResult.OpportunityMatch match : newSuggestions) {

            ExploredOpportunity explored = new ExploredOpportunity();
            explored.setStudentExternalId(studentExternalId);
            explored.setStudentName(studentName);
            explored.setStudentEmail(studentEmail);
            explored.setOpportunityExternalId(match.opportunityId());
            explored.setOpportunityTitle(match.title());
            explored.setMatchScore(match.matchingScore());
            explored.setSuggestedAt(now);

            exploredRepository.save(explored);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentExternalId);
        result.put("studentName", studentName);
        result.put("studentEmail", studentEmail);
        result.put("suggestions", newSuggestions);
        result.put("suggestionCount", newSuggestions.size());
        result.put("unexploredCandidateCount", candidateOpportunities.size());

        if (newSuggestions.size() < MAX_SUGGESTIONS) {
            result.put("note", "Only " + newSuggestions.size()
                    + " new, not-yet-suggested matching opportunit"
                    + (newSuggestions.size() == 1 ? "y" : "ies")
                    + " could be found for this student right now.");
        }

        return result;
    }

    public Map<String, Object> draftSuggestedOpportunitiesEmail(String studentExternalId) {

        List<ExploredOpportunity> pending = pendingSuggestions(studentExternalId);

        String rawEmail = pending.get(0).getStudentEmail();
        String studentName = pending.get(0).getStudentName();

        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("to", resolveSendableEmail(rawEmail));
        draft.put("expaEmailOnFile", rawEmail);
        draft.put("subject", EMAIL_SUBJECT);
        draft.put("body", buildEmailBody(studentName, pending));

        return draft;
    }

    public Map<String, Object> sendSuggestedOpportunitiesEmail(String studentExternalId) {

        List<ExploredOpportunity> pending = pendingSuggestions(studentExternalId);

        String rawEmail = pending.get(0).getStudentEmail();
        String studentName = pending.get(0).getStudentName();
        String sendTo = resolveSendableEmail(rawEmail);

        String body = buildEmailBody(studentName, pending);

        Map<String, Object> sendResult =
                mailService.sendEmail(sendTo, EMAIL_SUBJECT, body);

        if (Boolean.TRUE.equals(sendResult.get("sent"))) {

            LocalDateTime now = LocalDateTime.now();

            for (ExploredOpportunity opportunity : pending) {
                opportunity.setEmailed(true);
                opportunity.setEmailedAt(now);
            }

            exploredRepository.saveAll(pending);
        }

        return sendResult;
    }

    private List<ExploredOpportunity> pendingSuggestions(String studentExternalId) {

        List<ExploredOpportunity> pending =
                exploredRepository.findByStudentExternalIdAndEmailedFalse(studentExternalId);

        if (pending.isEmpty()) {
            throw new IllegalStateException(
                    "There are no unsent opportunity suggestions for student "
                            + studentExternalId + ". Call suggestOpportunities first.");
        }

        LocalDateTime latestBatch = pending.stream()
                .map(ExploredOpportunity::getSuggestedAt)
                .max(LocalDateTime::compareTo)
                .orElseThrow();

        return pending.stream()
                .filter(opportunity -> opportunity.getSuggestedAt().equals(latestBatch))
                .toList();
    }

    private String resolveSendableEmail(String rawEmail) {
        return FALLBACK_EMAIL;
    }

    private String buildEmailBody(String studentName, List<ExploredOpportunity> pending) {

        StringBuilder body = new StringBuilder();

        body.append("Hello dear ").append(studentName == null ? "" : studentName).append(",\n\n");
        body.append("Based on your profile, here are some AIESEC opportunities you might like:\n\n");

        for (ExploredOpportunity opportunity : pending) {
            body.append(buildOpportunityBlock(opportunity)).append("\n");
        }

        body.append("If you need more information or guidance, feel free to contact us.\n\n");
        body.append("Cordialement,\n");
        body.append(EMAIL_SIGNATURE);

        return body.toString();
    }

    private String buildOpportunityBlock(ExploredOpportunity opportunity) {

        SyncedRecord record = opportunitySyncService
                .findByExternalId(opportunity.getOpportunityExternalId())
                .orElse(null);

        StringBuilder block = new StringBuilder();

        block.append("Opportunity: ").append(opportunity.getOpportunityTitle()).append("\n");

        if (record == null) {
            return block.toString();
        }

        block.append("Company: ").append(valueOrNotSpecified(record.getCompanyName())).append("\n");
        block.append("Destination: ").append(valueOrNotSpecified(record.getCountry())).append("\n");

        block.append("Duration: ").append(valueOrNotSpecified(record.getDuration()));

        if (record.getLocation() != null && !record.getLocation().isBlank()) {
            block.append(" (").append(record.getLocation()).append(")");
        }

        block.append("\n");
        block.append("Paid: ").append(resolvePaidStatus(record)).append("\n");

        if (record.getSourceUrl() != null && !record.getSourceUrl().isBlank()) {
            block.append("More info: ").append(record.getSourceUrl()).append("\n");
        }

        return block.toString();
    }

    private String resolvePaidStatus(SyncedRecord record) {

        JsonNode rawDetails = parseJsonSafely(record.getRawDetailsJson());
        JsonNode salary = rawDetails.path("specifics_info").path("salary");

        if (salary.isMissingNode() || salary.isNull()) {
            return "Not specified";
        }

        String salaryText = salary.asText("").trim();

        return salaryText.isBlank() || salaryText.equals("0")
                ? "Unpaid"
                : "Paid";
    }

    private JsonNode parseJsonSafely(String value) {

        if (value == null || value.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String valueOrNotSpecified(String value) {
        return value == null || value.isBlank() ? "Not specified" : value;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }
}
