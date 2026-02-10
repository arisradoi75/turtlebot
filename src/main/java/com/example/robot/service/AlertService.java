package com.example.robot.service;

import com.example.robot.dto.AlertDTO;
import com.example.robot.model.Alert;
import com.example.robot.repository.AlertRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AlertService {
    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public AlertService(AlertRepository alertRepository, SimpMessagingTemplate simpMessagingTemplate) {
        this.alertRepository = alertRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }
    public void processAlertFromRobot(AlertDTO dto) {

        Alert alert = new Alert();

        alert.setAlertType(dto.getAlertType());
        alert.setMessage(dto.getMessage());
        alert.setSnapshotBase64(dto.getImageBase64());

        // 2. Gestionare Status (Default: NEW)
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            alert.setStatus(dto.getStatus().toUpperCase());
        } else {
            alert.setStatus("NEW");
        }

        // 3. Gestionare Timp (De la Robot sau Acum)
        if (dto.getTimestamp() != null && !dto.getTimestamp().isEmpty()) {
            try {
                alert.setTimestamp(LocalDateTime.parse(dto.getTimestamp()));
            } catch (Exception e) {
                alert.setTimestamp(LocalDateTime.now()); // Fallback dacă formatul e greșit
            }
        } else {
            alert.setTimestamp(LocalDateTime.now());
        }

        saveAndBroadcast(alert);
    }

    /**
     * Metodă internă pentru a crea alerte generate de server (ex: Baterie Critică)
     */
    public void createInternalAlert(String type, String message) {
        Alert alert = Alert.builder()
                .alertType(type)
                .message(message)
                .status("NEW")
                .timestamp(LocalDateTime.now())
                .build();

        saveAndBroadcast(alert);
    }

    // Metodă ajutătoare pentru a nu repeta codul de salvare
    private void saveAndBroadcast(Alert alert) {
        // Salvare în MySQL
        Alert savedAlert = alertRepository.save(alert);

        // Trimitere Live pe WebSocket (către Dashboard)
        simpMessagingTemplate.convertAndSend("/topic/alerts", savedAlert);

        System.out.println("🚨 ALERTA SALVATĂ: " + alert.getAlertType() + " - " + alert.getMessage());
    }
}
