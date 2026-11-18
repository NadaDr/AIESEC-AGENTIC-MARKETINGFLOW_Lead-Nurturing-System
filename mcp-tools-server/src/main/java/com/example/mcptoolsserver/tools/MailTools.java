
package com.example.mcptoolsserver.tools;

import com.example.mcptoolsserver.service.MailService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MailTools {

    private final MailService mailService;

    public MailTools(MailService mailService) {

        this.mailService = mailService;
    }

    @McpTool(
            name = "send_email",
            description = """
                    Send a real email via SMTP to notify or confirm something
                    to a recipient (for example a campaign alert, an interview
                    invitation, or a confirmation message). Only call this
                    tool when the user has explicitly asked for an email to
                    be sent and has provided (or clearly confirmed) a
                    recipient address, subject, and message content.
                    """
    )
    public Map<String, Object> sendEmail(

            @McpToolParam(description = "Recipient email address", required = true)
            String to,

            @McpToolParam(description = "Email subject line", required = true)
            String subject,

            @McpToolParam(description = "Email body text", required = true)
            String body
    ) {

        return mailService.sendEmail(to, subject, body);
    }
}
