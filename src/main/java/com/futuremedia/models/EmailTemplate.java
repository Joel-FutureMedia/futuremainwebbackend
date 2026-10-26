package com.futuremedia.models;

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
@Entity(name = "email_templates")
@Table(name = "email_templates")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Short inbox preview text */
    private String preheader;

    private String headerHeadline;

    @Column(columnDefinition = "TEXT")
    private String headerSubheadline;

    @Column(nullable = false)
    private String headerColor = "#0b1f3a";

    @Column(nullable = false)
    private String headerTextColor = "#ffffff";

    private String bodyHeadline;

    @Column(columnDefinition = "TEXT")
    private String bodyContent;

    @Column(nullable = false)
    private String bodyColor = "#ffffff";

    @Column(nullable = false)
    private String bodyTextColor = "#334155";

    private String ctaText;

    private String ctaUrl;

    @Column(nullable = false)
    private String ctaButtonColor = "#1a73e8";

    @Column(nullable = false)
    private String ctaTextColor = "#ffffff";

    @Column(columnDefinition = "TEXT")
    private String footerText;

    @Column(nullable = false)
    private String footerColor = "#0b1f3a";

    @Column(nullable = false)
    private String footerTextColor = "#a8c0d8";

    private boolean useCompanyLogo = true;

    private boolean active = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC, id ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<EmailTemplateImage> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
