package com.example.robot.dto;

import lombok.Data;

@Data
public class AlertDTO {
    private String alertType;
    private String message;
    private String imageBase64;
    private String timestamp;
    private String status;
}
