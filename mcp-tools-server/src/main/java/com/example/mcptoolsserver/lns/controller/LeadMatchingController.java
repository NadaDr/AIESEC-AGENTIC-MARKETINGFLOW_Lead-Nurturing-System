package com.example.mcptoolsserver.lns.controller;

import com.example.mcptoolsserver.lns.service.OpportunityMatchingPipelineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lns/students")

public class LeadMatchingController {

    private final OpportunityMatchingPipelineService pipelineService;

    public LeadMatchingController(OpportunityMatchingPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @GetMapping("/search")
    public List<Map<String, Object>> searchStudents(@RequestParam String name) {

        return pipelineService.searchStudents(name);
    }

    @PostMapping("/{studentId}/suggest")

    public Map<String, Object> suggestOpportunities(@PathVariable String studentId) {

        return pipelineService.suggestOpportunities(studentId);
    }

    @GetMapping("/{studentId}/email/draft")

    public Map<String, Object> draftSuggestedOpportunitiesEmail(@PathVariable String studentId) {
        return pipelineService.draftSuggestedOpportunitiesEmail(studentId);
    }

    @PostMapping("/{studentId}/email/send")

    public Map<String, Object> sendSuggestedOpportunitiesEmail(@PathVariable String studentId) {
        return pipelineService.sendSuggestedOpportunitiesEmail(studentId);
    }
}
