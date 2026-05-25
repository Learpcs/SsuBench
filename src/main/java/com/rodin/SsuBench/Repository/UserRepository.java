package com.rodin.SsuBench.Repository;

import com.rodin.SsuBench.Entity.User;
import com.rodin.SsuBench.Entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByUsername(String username);


    boolean existsByUsername(String username);


    Page<User> findByRole(UserRole role, Pageable pageable);
}