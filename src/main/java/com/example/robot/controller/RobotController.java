package com.example.robot.controller;

import com.example.robot.dto.AlertDTO;
import com.example.robot.dto.RobotStatusDTO;
import com.example.robot.model.Alert;
import com.example.robot.model.Robot;
import com.example.robot.service.AlertService;
import com.example.robot.service.RobotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/robot")
@CrossOrigin(origins = "*")
public class RobotController {

    private final RobotService robotService;
    private final AlertService alertService;

    public RobotController(RobotService robotService, AlertService alertService) {
        this.robotService = robotService;
        this.alertService = alertService;
    }

    @PostMapping("/telemetry")
    public ResponseEntity<String> reciveTelemetry(@RequestBody RobotStatusDTO dto){
        robotService.processTelemetry(dto);
        return ResponseEntity.ok("UPDATE!");
    }
    @PostMapping("/alert")
    public ResponseEntity<String> reciveAlert(@RequestBody AlertDTO dto) {
        alertService.processAlertFromRobot(dto);
        return ResponseEntity.ok("Alert saved!");
    }

    @GetMapping("/latest-telemetry")
    public ResponseEntity<Robot> getLatestTelemetry() {
        return ResponseEntity.ok(robotService.getLatestTelemetry());
    }

    @GetMapping("/latest-alert")
    public ResponseEntity<Alert> getLatestAlert() {
        return ResponseEntity.ok(alertService.getLatestAlert());
    }
}
