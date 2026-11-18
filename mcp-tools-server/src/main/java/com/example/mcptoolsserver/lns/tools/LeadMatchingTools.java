
package com.example.mcptoolsserver.lns.tools;

import com.example.mcptoolsserver.lns.service.LeadMatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class LeadMatchingTools {

    private final LeadMatchingService matchingService;
    private final ObjectMapper objectMapper;

    public LeadMatchingTools(
            LeadMatchingService matchingService,
            ObjectMapper objectMapper) {

        this.matchingService = matchingService;
        this.objectMapper = objectMapper;
    }

    @McpTool(
            name = "match_expa_leads",
            description = """
                    Match EXPA leads from a specified page with synchronized
                    AIESEC opportunities. Return up to five recommendations
                    per lead, with matching scores and explanations.
                    Results are not stored in MySQL.
                    """
    )
    public String matchExpaLeads(
            @McpToolParam(
                    description = "EXPA page number, starting from 1"
            )
            int page) {

        try {
            return objectMapper.writeValueAsString(
                    matchingService.matchPage(page));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not match EXPA leads: "
                            + exception.getMessage(),
                    exception);
        }
    }

    @McpTool(
            name = "match_one_expa_lead",
            description = """
                    Find up to five matching AIESEC opportunities
                    for one EXPA lead on a specified EXPA page.
                    """
    )
    public String matchOneExpaLead(
            @McpToolParam(description = "EXPA lead ID")
            String leadId,

            @McpToolParam(
                    description = "EXPA page number containing the lead"
            )
            int page) {

        try {
            return objectMapper.writeValueAsString(
                    matchingService.matchLeadById(page, leadId));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not match EXPA lead: "
                            + exception.getMessage(),
                    exception);
        }
    }
}
