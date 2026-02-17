package com.example.robot.service;

import com.example.robot.dto.RobotStatusDTO;
import com.example.robot.model.Robot;
import com.example.robot.model.RobotStatus;
import com.example.robot.repository.RobotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RobotService {
    private final RobotRepository robotRepository;
    private final AlertService alertService; // Injectăm AlertService pentru a semnala probleme
    private final SimpMessagingTemplate messagingTemplate;

    public void processTelemetry(RobotStatusDTO dto) {
        Robot robot = new Robot();

        robot.setX(dto.getX());
        robot.setY(dto.getY());
        robot.setBatteryLevel(dto.getBatteryLevel());

        try {
            if (dto.getStatus() != null) {
                robot.setStatus(RobotStatus.valueOf(dto.getStatus().toUpperCase()));
            } else {
                robot.setStatus(RobotStatus.ALERT);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Status necunoscut primit: " + dto.getStatus());
            robot.setStatus(RobotStatus.ALERT);
        }

        if (dto.getTimestamp() != null && !dto.getTimestamp().isEmpty()) {
            try {
                robot.setTimestamp(LocalDateTime.parse(dto.getTimestamp()));
            } catch (Exception e) {
                robot.setTimestamp(LocalDateTime.now());
            }
        } else {
            robot.setTimestamp(LocalDateTime.now());
        }

        if (robot.getBatteryLevel() < 15.0 && robot.getStatus() != RobotStatus.CHARGING) {
            alertService.createInternalAlert("LOW_BATTERY",
                    "Bateria robotului este critică (" + robot.getBatteryLevel() + "%) și nu încarcă!");
        }

        robotRepository.save(robot);
        messagingTemplate.convertAndSend("/topic/telemetry", robot);
    }
    public Robot getLatestTelemetry() {
        return robotRepository.findTopByOrderByTimestampDesc().orElse(null);
    }



}
