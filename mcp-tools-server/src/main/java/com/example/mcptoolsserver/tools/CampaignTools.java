
package com.example.mcptoolsserver.tools;

import com.example.mcptoolsserver.entities.Campaign;

import com.example.mcptoolsserver.service.CampaignService;

import org.springframework.ai.mcp.annotation.McpTool;

import org.springframework.ai.mcp.annotation.McpToolParam;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CampaignTools {

    private final CampaignService campaignService;

    public CampaignTools(CampaignService campaignService) {

        this.campaignService = campaignService;
    }

    @McpTool(

            name = "get_all_campaigns",

            description = "Retrieve all marketing campaigns stored in MySQL"
    )
    public List<Campaign> getAllCampaigns() {

        return campaignService.getAllCampaigns();
    }

    @McpTool(
            name = "get_campaigns_by_status",
            description = "Find campaigns by status such as PLANNED, ACTIVE or COMPLETED"
    )
    public List<Campaign> getCampaignsByStatus(

            @McpToolParam(description = "Campaign status", required = true)
            String status
    ) {

        return campaignService.getCampaignsByStatus(status);
    }

    @McpTool(
            name = "get_campaigns_by_platform",
            description = "Find campaigns by social media platform"
    )
    public List<Campaign> getCampaignsByPlatform(

            @McpToolParam(description = "Platform name, for example Instagram")
            String platform
    ) {

        return campaignService.getCampaignsByPlatform(platform);
    }

    @McpTool(
            name = "search_campaigns_by_name",
            description = "Search campaigns by part of their name"
    )
    public List<Campaign> searchCampaignsByName(

            @McpToolParam(description = "Campaign name or keyword")
            String name
    ) {

        return campaignService.searchCampaignsByName(name);
    }

    @McpTool(
            name = "create_campaign",
            description = "Create and save a marketing campaign in MySQL"
    )
    public Campaign createCampaign(

            @McpToolParam(description = "Campaign name")
            String name,

            @McpToolParam(description = "Campaign description")
            String description,

            @McpToolParam(description = "Target audience")
            String targetAudience,

            @McpToolParam(description = "Social media platform")
            String platform,

            @McpToolParam(description = "Campaign status")
            String status
    ) {

        Campaign campaign = Campaign.builder()
                .name(name)
                .description(description)
                .targetAudience(targetAudience)
                .platform(platform)
                .status(status)
                .build();

        return campaignService.saveCampaign(campaign);
    }
}
