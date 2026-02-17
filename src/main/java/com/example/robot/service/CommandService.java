package com.example.robot.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class CommandService {
    private final String ROBOT_API_URL = "http://172.20.10.12:5000/api/command";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Trimite o comandă simplă, fără parametri.
     * @param command Comanda de trimis (ex: "START", "STOP").
     */
    public void sendCommand(String command) {
        // Refolosim metoda mai complexă, fără date suplimentare.
        sendCommand(command, null);
    }

    /**
     * Trimite o comandă cu un set de date suplimentare.
     * @param command Comanda principală (ex: "MOVE").
     * @param data Un Map cu datele suplimentare (ex: {"x": 10.5, "y": 20.0}).
     */
    public void sendCommand(String command, Map<String, Object> data) {
        try {
            // 1. Creăm payload-ul, care va fi mereu un Map.
            Map<String, Object> payload = new HashMap<>();
            payload.put("command", command);

            // Dacă există date suplimentare, le adăugăm în payload.
            if (data != null && !data.isEmpty()) {
                payload.put("data", data);
            }

            // 2. Setăm headerele pentru a specifica JSON.
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 3. Împachetăm payload-ul și headerele.
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // 4. Trimitem cererea POST.
            restTemplate.postForObject(ROBOT_API_URL, request, String.class);
            
            System.out.println("Comandă JSON trimisă către Robot: " + payload);

        } catch (Exception e) {
            System.err.println("Eroare: Robotul nu răspunde! " + e.getMessage());
            throw new RuntimeException("Robot offline sau IP greșit!");
        }
    }
}
