package com.rodin.SsuBench.Repository;

import com.rodin.SsuBench.Entity.Task;
import com.rodin.SsuBench.Entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByCustomerId(Long customerId, Pageable pageable);

    Page<Task> findByStatusAndCustomerIdNot(TaskStatus status, Long excludeCustomerId, Pageable pageable);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.acceptedBid WHERE t.id = :id")
    Optional<Task> findByIdWithBid(@Param("id") Long id);

    List<Task> findByCustomerIdAndStatus(Long customerId, TaskStatus status);
}
