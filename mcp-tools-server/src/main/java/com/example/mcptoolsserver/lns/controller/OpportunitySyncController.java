package com.example.mcptoolsserver.lns.controller;

import com.example.mcptoolsserver.lns.dto.OpportunitySyncResult;
import com.example.mcptoolsserver.lns.entity.SyncedRecord;
import com.example.mcptoolsserver.lns.service.AiesecOpportunitySyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lns/opportunities")

public class OpportunitySyncController {

    private final AiesecOpportunitySyncService syncService;

    public OpportunitySyncController(AiesecOpportunitySyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/sync")
    public OpportunitySyncResult synchronize(
            @RequestParam(required = false)

            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)

            LocalDate fromDate) {
        return fromDate == null
                ? syncService.synchronize()

                : syncService.synchronize(fromDate);

    }

    @GetMapping
    public List<SyncedRecord> getOpportunities(
            @RequestParam(required = false) String programme) {

        return programme == null || programme.isBlank()
                ? syncService.getAllOpportunities()

                : syncService.getByProgramme(programme);

    }

    @GetMapping("/search")

    public List<SyncedRecord> searchByTitle(@RequestParam String keyword) {

        return syncService.searchByTitle(keyword);
    }
}
