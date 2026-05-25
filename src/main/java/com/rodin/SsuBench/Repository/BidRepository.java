package com.rodin.SsuBench.Repository;

import com.rodin.SsuBench.Entity.Bid;
import com.rodin.SsuBench.Entity.BidStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    Page<Bid> findByTaskId(Long taskId, Pageable pageable);

    Page<Bid> findByExecutorId(Long executorId, Pageable pageable);

    List<Bid> findByTaskIdAndStatus(Long taskId, BidStatus status);

    Optional<Bid> findByTaskIdAndExecutorIdAndStatus(Long taskId, Long executorId, BidStatus status);

    @Query("SELECT b FROM Bid b LEFT JOIN FETCH b.executor WHERE b.id = :id")
    Optional<Bid> findByIdWithExecutor(@Param("id") Long id);

    boolean existsByTaskIdAndExecutorIdAndStatus(Long taskId, Long executorId, BidStatus status);

    long countByTaskIdAndStatus(Long taskId, BidStatus status);
}
