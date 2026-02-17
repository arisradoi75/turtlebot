package com.example.robot.repository;

import com.example.robot.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByStatus(String status);

    Optional<Alert> findTopByOrderByTimestampDesc();
}
