package com.futuremedia.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "job_applications")
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vacancy_id", nullable = false)
    private Vacancy vacancy;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String highestQualification;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String workExperience;

    @Column(nullable = false)
    private boolean namibian;

    /** CV document URL in MinIO — PDF/DOC/DOCX/CSV only, never stored in PostgreSQL */
    @Column(nullable = false)
    private String cvPath;

    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedDate;

    @PrePersist
    protected void onCreate() {
        appliedDate = LocalDateTime.now();
    }
}
