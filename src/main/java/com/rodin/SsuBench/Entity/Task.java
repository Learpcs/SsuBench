package com.rodin.SsuBench.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.OPEN;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_bid_id")
    private Bid acceptedBid;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal reward;

    @CreationTimestamp
    @Column(updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    private ZonedDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Описание задачи не может быть пустым");
        }
        if (reward == null || reward.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Награда должна быть положительной");
        }
        if (customer == null) {
            throw new IllegalArgumentException("Заказчик должен быть указан");
        }
    }
}
