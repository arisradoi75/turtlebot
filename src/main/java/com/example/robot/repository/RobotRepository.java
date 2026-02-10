package com.example.robot.repository;

import com.example.robot.model.Robot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {
    Optional<Robot>findTopByOrderByTimestampDesc();
}
