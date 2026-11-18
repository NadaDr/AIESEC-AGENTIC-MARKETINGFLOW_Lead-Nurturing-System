
package com.example.mcptoolsserver.lns.tools;

import com.example.mcptoolsserver.lns.service.OpportunityMatchingPipelineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class OpportunityPipelineTools {

    private final OpportunityMatchingPipelineService pipelineService;
    private final ObjectMapper objectMapper;

    public OpportunityPipelineTools(
            OpportunityMatchingPipelineService pipelineService,
            ObjectMapper objectMapper) {

        this.pipelineService = pipelineService;
        this.objectMapper = objectMapper;
    }

    @McpTool(
            name = "find_student_by_name",
            description = """
                    Search EXPA for students/people matching a given name.
                    Returns a list of candidates (id, full name, email, status,
                    home LC/MC). If more than one candidate is returned, list
                    them and ask the user which one to use before calling
                    suggest_opportunities_for_student.
                    """
    )
    public String findStudentByName(
            @McpToolParam(description = "Full or partial student name to search for")
            String name) {

        try {
            return objectMapper.writeValueAsString(pipelineService.searchStudents(name));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not search for student \"" + name + "\": "
                            + exception.getMessage(),
                    exception);
        }
    }

    @McpTool(
            name = "suggest_opportunities_for_student",
            description = """
                    Fetch an EXPA student's profile by id, match it against the
                    synchronized AIESEC opportunity catalog, excluding anything
                    already suggested to them before, and return up to 3 new
                    ranked suggestions with matching scores and reasons. The
                    returned opportunities are recorded as explored for this
                    student, so a later call for the same student only returns
                    newer ones.
                    """
    )
    public String suggestOpportunitiesForStudent(
            @McpToolParam(description = "EXPA person id of the chosen student")
            String studentId) {

        try {
            return objectMapper.writeValueAsString(
                    pipelineService.suggestOpportunities(studentId));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not suggest opportunities for student " + studentId
                            + ": " + exception.getMessage(),
                    exception);
        }
    }

    @McpTool(
            name = "draft_suggested_opportunities_email",
            description = """
                    Compose, WITHOUT SENDING, the email for the student's most
                    recently suggested, not-yet-emailed opportunities. Returns
                    the exact "to", "subject" and "body" that would be sent.
                    Always call this and show the full draft to the user for
                    approval before ever calling
                    send_suggested_opportunities_email.
                    """
    )
    public String draftSuggestedOpportunitiesEmail(
            @McpToolParam(description = "EXPA person id of the student")
            String studentId) {

        try {
            return objectMapper.writeValueAsString(
                    pipelineService.draftSuggestedOpportunitiesEmail(studentId));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not draft the opportunities email for student " + studentId
                            + ": " + exception.getMessage(),
                    exception);
        }
    }

    @McpTool(
            name = "send_suggested_opportunities_email",
            description = """
                    Actually send the email drafted by
                    draft_suggested_opportunities_email for this student's
                    pending opportunity suggestions. Only call this after the
                    user has seen the exact draft and explicitly approved
                    sending it.
                    """
    )
    public String sendSuggestedOpportunitiesEmail(
            @McpToolParam(description = "EXPA person id of the student")
            String studentId) {

        try {
            return objectMapper.writeValueAsString(
                    pipelineService.sendSuggestedOpportunitiesEmail(studentId));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not send the opportunities email for student " + studentId
                            + ": " + exception.getMessage(),
                    exception);
        }
    }
}
