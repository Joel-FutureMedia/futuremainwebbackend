package com.futuremedia.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity(name = "shopbeat_demo_requests")
@Table(name = "shopbeat_demo_requests")
public class ShopbeatDemoRequest {

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "feeling_id", nullable = false)
    private ShopbeatFeeling feeling;

    @Column(nullable = false)
    private boolean hasSoundInspiration;

    @Column(columnDefinition = "TEXT")
    private String soundInspiration;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedDate;

    @PrePersist
    protected void onCreate() {
        submittedDate = LocalDateTime.now();
    }
}
