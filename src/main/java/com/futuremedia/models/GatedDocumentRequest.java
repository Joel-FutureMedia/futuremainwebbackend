package com.futuremedia.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "gated_document_requests")
@Table(name = "gated_document_requests")
public class GatedDocumentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_id", nullable = false)
    private GatedDocument document;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telephone;

    @Column(nullable = false)
    private String company;

    @JsonIgnore
    @Column(nullable = false, unique = true)
    private String downloadToken;

    private boolean emailSent = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedDate;

    @PrePersist
    protected void onCreate() {
        submittedDate = LocalDateTime.now();
    }
}
