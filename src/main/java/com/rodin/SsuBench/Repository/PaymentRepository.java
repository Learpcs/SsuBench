package com.rodin.SsuBench.Repository;

import com.rodin.SsuBench.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTaskId(Long taskId);

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.task LEFT JOIN FETCH p.bid WHERE p.id = :id")
    Optional<Payment> findByIdWithDetails(@Param("id") Long id);

    boolean existsByTaskId(Long taskId);
}
