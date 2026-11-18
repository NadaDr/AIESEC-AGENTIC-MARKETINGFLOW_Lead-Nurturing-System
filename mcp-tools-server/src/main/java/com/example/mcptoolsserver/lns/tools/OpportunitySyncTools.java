package com.example.mcptoolsserver.lns.tools;

import com.example.mcptoolsserver.lns.dto.OpportunitySyncResult;
import com.example.mcptoolsserver.lns.entity.SyncedRecord;
import com.example.mcptoolsserver.lns.service.AiesecOpportunitySyncService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpportunitySyncTools {

    private final AiesecOpportunitySyncService syncService;

    public OpportunitySyncTools(AiesecOpportunitySyncService syncService) {
        this.syncService = syncService;
    }

    @McpTool(
            name = "sync_aiesec_opportunities",
            description = "Synchronize eligible AIESEC.org OGV, OGTa and OGTe opportunities into MySQL. Existing records are updated by external opportunity ID."
    )
    public OpportunitySyncResult synchronizeOpportunities() {

        return syncService.synchronize();
    }

    @McpTool(
            name = "get_synchronized_opportunities",
            description = "Retrieve synchronized OGV, OGTa and OGTe opportunities. Optionally filter by programme code OGV, OGTa or OGTe."
    )
    public List<SyncedRecord> getSynchronizedOpportunities(
            @McpToolParam(description = "Optional programme code: OGV, OGTa or OGTe", required = false)
            String programme) {

        if (programme == null || programme.isBlank()) {
            return syncService.getAllOpportunities();
        }

        return syncService.getByProgramme(programme);
    }
}
