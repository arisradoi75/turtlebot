package com.example.robot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Table(name = "security_alert")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String alertType;

    @Column(length = 500)
    private String message;

    private LocalDateTime timestamp;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String snapshotBase64;

    private String status;

}
