package com.fpms.fpms_backend.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_shares")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_id", nullable = false)
    private Faculty sharedWith;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SharePermission permission;

    @Column(nullable = false)
    private LocalDateTime sharedAt;

    @PrePersist
    protected void onCreate() {
        sharedAt = LocalDateTime.now();
    }

    public enum SharePermission {
        VIEW, EDIT
    }
}