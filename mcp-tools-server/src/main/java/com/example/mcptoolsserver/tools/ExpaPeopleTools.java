
package com.example.mcptoolsserver.tools;

import com.example.mcptoolsserver.service.ExpaPeopleService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExpaPeopleTools {

    private final ExpaPeopleService expaPeopleService;

    public ExpaPeopleTools(ExpaPeopleService expaPeopleService) {

        this.expaPeopleService = expaPeopleService;
    }

    @McpTool(
            name = "get_expa_people_by_year",
            description = """
                    Retrieve aggregated EXPA People registration statistics
                    for a given year. Returns total registrations, date range,
                    pages processed, and counts grouped by current status.
                    """
    )
    public Map<String, Object> getPeopleByYear(
            @McpToolParam(
                    description = "Registration year, for example 2026",
                    required = true
            )
            int year
    ) {

        return expaPeopleService.getPeopleByYear(year);
    }
}
