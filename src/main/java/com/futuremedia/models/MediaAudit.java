package com.futuremedia.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "media_audits")
@Table(name = "media_audits")
public class MediaAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telephone;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String campaignName;

    @Column(nullable = false)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String additionalDetails;

    @Column(columnDefinition = "TEXT")
    private String businessDescription;

    private String campaignType;

    private String campaignTypeOther;

    private String locationCountry;

    private String locationTown;

    private String primaryAudience;

    @Column(columnDefinition = "TEXT")
    private String currentAdvertising;

    private String advertiseTimeline;

    private String advertisingBudget;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedDate;

    @PrePersist
    protected void onCreate() {
        submittedDate = LocalDateTime.now();
    }
}
