package com.example.robot.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "robot_telemetry")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double x;
    private double y;
    private double batteryLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private RobotStatus status;

    private LocalDateTime timestamp;

}
