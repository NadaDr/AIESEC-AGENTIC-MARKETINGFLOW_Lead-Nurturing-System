package com.example.mcptoolsserver.lns.dto;

import java.util.List;

public record LeadMatchResult(
        String leadId,
        List<String> preferredProgrammes,
        List<OpportunityMatch> recommendedOpportunities
) {

    public record OpportunityMatch(
            String opportunityId,
            String title,
            String programme,
            String country,
            double matchingScore,
            List<String> reasons
    ) {}
}
