package com.example.robot.dto;

import lombok.Data;

@Data
public class RobotStatusDTO {
    private double x;
    private double y;
    private double batteryLevel;
    private String status;
    private String timestamp;
}
