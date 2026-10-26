package com.futuremedia.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "freelancer_applications")
@Table(name = "freelancer_applications")
public class FreelancerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private FreelancerRole role;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telephone;

    private String location;

    @Column(nullable = false)
    private String skill;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String experience;

    @Column(nullable = false)
    private boolean namibian;

    @Column(nullable = false)
    private String cvPath;

    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedDate;

    @PrePersist
    protected void onCreate() {
        appliedDate = LocalDateTime.now();
    }
}
