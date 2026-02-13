package com.example.robot.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CommandService {
    private final String ROBOT_API_URL = "http://172.20.10.12:5000/api/command";
    private final RestTemplate restTemplate = new RestTemplate();

    // O clasă internă simplă pentru a structura JSON-ul
    @Data
    @AllArgsConstructor
    private static class CommandPayload {
        private String command;
    }

    public void sendCommand(String command) {
        try {
            // 1. Creăm payload-ul
            CommandPayload payload = new CommandPayload(command);

            // 2. Setăm headerele pentru a specifica faptul că trimitem JSON
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 3. Împachetăm payload-ul și headerele într-o entitate HTTP
            HttpEntity<CommandPayload> request = new HttpEntity<>(payload, headers);

            // 4. Trimitem cererea POST
            restTemplate.postForObject(ROBOT_API_URL, request, String.class);
            
            System.out.println("Comandă JSON trimisă către Robot: " + payload);

        } catch (Exception e) {
            System.err.println("Eroare: Robotul nu răspunde! " + e.getMessage());
            throw new RuntimeException("Robot offline sau IP greșit!");
        }
    }
}
