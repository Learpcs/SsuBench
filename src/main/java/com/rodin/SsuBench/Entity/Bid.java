package com.rodin.SsuBench.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "bids")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executor_id", nullable = false)
    private User executor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BidStatus status = BidStatus.PENDING;

    @CreationTimestamp
    @Column(updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Описание отклика не может быть пустым");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Исполнитель должен быть указан");
        }
        if (task == null) {
            throw new IllegalArgumentException("Задача должна быть указана");
        }
    }
}
