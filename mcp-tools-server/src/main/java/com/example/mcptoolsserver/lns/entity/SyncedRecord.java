package com.example.mcptoolsserver.lns.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "lns_synced_opportunities",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lns_opportunity_external_id",
                columnNames = "external_id"
        ),
        indexes = {
                @Index(name = "idx_lns_opportunity_programme", columnList = "programme_code"),
                @Index(name = "idx_lns_opportunity_country", columnList = "country"),
                @Index(name = "idx_lns_opportunity_start_date", columnList = "earliest_start_date")
        }
)
public class SyncedRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, length = 40, unique = true)
    private String externalId;

    @Column(name = "programme_code", nullable = false, length = 10)
    private String programmeCode;

    @Column(name = "programme_name", nullable = false, length = 100)
    private String programmeName;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "project_name", length = 500)
    private String projectName;

    @Column(columnDefinition = "LONGTEXT", name = "project_description")
    private String projectDescription;

    @Column(length = 500)
    private String location;

    @Column(length = 150)
    private String country;

    @Column(name = "company_name", length = 300)
    private String companyName;

    @Column(name = "partner_type", length = 100)
    private String partnerType;

    @Column(name = "experience_type", length = 100)
    private String experienceType;

    @Column(length = 100)
    private String duration;

    @Column(name = "earliest_start_date")
    private LocalDate earliestStartDate;

    @Column(name = "backgrounds_json", columnDefinition = "LONGTEXT")
    private String backgroundsJson;

    @Column(name = "skills_json", columnDefinition = "LONGTEXT")
    private String skillsJson;

    @Column(name = "skills_developments_json", columnDefinition = "LONGTEXT")
    private String skillsDevelopmentsJson;

    @Column(name = "languages_json", columnDefinition = "LONGTEXT")
    private String languagesJson;

    @Column(name = "nationalities_json", columnDefinition = "LONGTEXT")
    private String nationalitiesJson;

    @Column(name = "study_levels_json", columnDefinition = "LONGTEXT")
    private String studyLevelsJson;

    @Column(name = "available_slots_json", columnDefinition = "LONGTEXT")
    private String availableSlotsJson;

    @Column(name = "raw_details_json", columnDefinition = "LONGTEXT")
    private String rawDetailsJson;

    @Column(name = "source_url", length = 700)
    private String sourceUrl;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {

        this.syncedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getProgrammeCode() { return programmeCode; }
    public void setProgrammeCode(String programmeCode) { this.programmeCode = programmeCode; }
    public String getProgrammeName() { return programmeName; }
    public void setProgrammeName(String programmeName) { this.programmeName = programmeName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) { this.projectDescription = projectDescription; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getPartnerType() { return partnerType; }
    public void setPartnerType(String partnerType) { this.partnerType = partnerType; }
    public String getExperienceType() { return experienceType; }
    public void setExperienceType(String experienceType) { this.experienceType = experienceType; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public LocalDate getEarliestStartDate() { return earliestStartDate; }
    public void setEarliestStartDate(LocalDate earliestStartDate) { this.earliestStartDate = earliestStartDate; }
    public String getBackgroundsJson() { return backgroundsJson; }
    public void setBackgroundsJson(String backgroundsJson) { this.backgroundsJson = backgroundsJson; }
    public String getSkillsJson() { return skillsJson; }
    public void setSkillsJson(String skillsJson) { this.skillsJson = skillsJson; }
    public String getSkillsDevelopmentsJson() { return skillsDevelopmentsJson; }
    public void setSkillsDevelopmentsJson(String skillsDevelopmentsJson) { this.skillsDevelopmentsJson = skillsDevelopmentsJson; }
    public String getLanguagesJson() { return languagesJson; }
    public void setLanguagesJson(String languagesJson) { this.languagesJson = languagesJson; }
    public String getNationalitiesJson() { return nationalitiesJson; }
    public void setNationalitiesJson(String nationalitiesJson) { this.nationalitiesJson = nationalitiesJson; }
    public String getStudyLevelsJson() { return studyLevelsJson; }
    public void setStudyLevelsJson(String studyLevelsJson) { this.studyLevelsJson = studyLevelsJson; }
    public String getAvailableSlotsJson() { return availableSlotsJson; }
    public void setAvailableSlotsJson(String availableSlotsJson) { this.availableSlotsJson = availableSlotsJson; }
    public String getRawDetailsJson() { return rawDetailsJson; }
    public void setRawDetailsJson(String rawDetailsJson) { this.rawDetailsJson = rawDetailsJson; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public LocalDateTime getSyncedAt() { return syncedAt; }
}
