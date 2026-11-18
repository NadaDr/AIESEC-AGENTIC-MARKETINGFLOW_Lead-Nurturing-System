package com.example.mcptoolsserver.lns.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "lns_explored_opportunities",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lns_explored_student_opportunity",
                columnNames = {"student_external_id", "opportunity_external_id"}
        ),
        indexes = {
                @Index(name = "idx_lns_explored_student", columnList = "student_external_id")
        }
)
public class ExploredOpportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_external_id", nullable = false, length = 40)
    private String studentExternalId;

    @Column(name = "student_name", length = 300)
    private String studentName;

    @Column(name = "student_email", length = 300)
    private String studentEmail;

    @Column(name = "opportunity_external_id", nullable = false, length = 40)
    private String opportunityExternalId;

    @Column(name = "opportunity_title", length = 500)
    private String opportunityTitle;

    @Column(name = "match_score")
    private Double matchScore;

    @Column(name = "suggested_at", nullable = false)
    private LocalDateTime suggestedAt;

    @Column(name = "emailed", nullable = false)
    private boolean emailed;

    @Column(name = "emailed_at")
    private LocalDateTime emailedAt;

    @PrePersist
    public void onCreate() {
        if (suggestedAt == null) {

            suggestedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }

    public String getStudentExternalId() { return studentExternalId; }
    public void setStudentExternalId(String studentExternalId) { this.studentExternalId = studentExternalId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getOpportunityExternalId() { return opportunityExternalId; }
    public void setOpportunityExternalId(String opportunityExternalId) { this.opportunityExternalId = opportunityExternalId; }

    public String getOpportunityTitle() { return opportunityTitle; }
    public void setOpportunityTitle(String opportunityTitle) { this.opportunityTitle = opportunityTitle; }

    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }

    public LocalDateTime getSuggestedAt() { return suggestedAt; }
    public void setSuggestedAt(LocalDateTime suggestedAt) { this.suggestedAt = suggestedAt; }

    public boolean isEmailed() { return emailed; }
    public void setEmailed(boolean emailed) { this.emailed = emailed; }

    public LocalDateTime getEmailedAt() { return emailedAt; }
    public void setEmailedAt(LocalDateTime emailedAt) { this.emailedAt = emailedAt; }
}
