package com.futuremedia.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "email_campaigns")
@Table(name = "email_campaigns")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EmailCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id", nullable = false)
    private EmailTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailAudienceType audience = EmailAudienceType.ALL_CAPTURED;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lead_source_id")
    private LeadSource leadSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.DRAFT;

    private int totalRecipients = 0;

    private int sentCount = 0;

    private int failedCount = 0;

    @JsonIgnore
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailCampaignRecipient> recipients = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<EmailCampaignImage> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
