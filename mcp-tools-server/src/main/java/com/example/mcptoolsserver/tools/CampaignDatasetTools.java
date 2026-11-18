
package com.example.mcptoolsserver.tools;

import com.example.mcptoolsserver.service.CampaignDatasetService;

import org.springframework.ai.mcp.annotation.McpTool;

import org.springframework.ai.mcp.annotation.McpToolParam;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CampaignDatasetTools {

    private final CampaignDatasetService datasetService;

    public CampaignDatasetTools(CampaignDatasetService datasetService) {

        this.datasetService = datasetService;
    }

    @McpTool(
            name = "get_campaign_dataset_summary",

            description = """
                    Analyze the campaign_performance.csv dataset (a static file,
                    not the MySQL database) and return overall indicators:
                    total impressions, clicks, conversions, spend, click-through
                    rate and conversion rate, broken down per social media
                    platform.
                    """
    )
    public Map<String, Object> getDatasetSummary() {

        return datasetService.getSummary();
    }

    @McpTool(
            name = "get_campaign_platform_stats",
            description = """
                    Analyze the campaign_performance.csv dataset and return
                    aggregated indicators (impressions, clicks, conversions,
                    spend, CTR, conversion rate) for a single social media
                    platform.
                    """
    )
    public Map<String, Object> getPlatformStats(

            @McpToolParam(description = "Platform name, for example Instagram", required = true)
            String platform
    ) {

        return datasetService.getPlatformStats(platform);
    }

    @McpTool(
            name = "get_top_campaigns_from_dataset",
            description = """
                    Return the top performing campaigns from the
                    campaign_performance.csv dataset, ranked by number of
                    conversions, descending.
                    """
    )
    public List<Map<String, Object>> getTopCampaigns(

            @McpToolParam(description = "Maximum number of campaigns to return, for example 3", required = false)
            Integer limit
    ) {

        return datasetService.getTopCampaigns(limit == null ? 5 : limit);
    }
}
