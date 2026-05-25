package com.rodin.SsuBench.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private Bid bid;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(name = "processed_at")
    private ZonedDateTime processedAt;

    @PrePersist
    public void validate() {
        if (task == null) {
            throw new IllegalArgumentException("Задача должна быть указана");
        }
        if (bid == null) {
            throw new IllegalArgumentException("Отклик должен быть указан");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма платежа должна быть положительной");
        }
    }
}
