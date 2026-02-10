package com.example.robot.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CommandService {
    // AICI TREBUIE PUS IP-ul LAPTOPULUI CU LINUX (unde e Robotul)
    // Robotul trebuie să aibă un server Flask pe portul 5000 (ți-am dat codul python anterior)
    private final String ROBOT_API_URL = "http://192.168.1.XXX:5000/api/command";

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendCommand(String command) {
        try {
            restTemplate.postForObject(ROBOT_API_URL, command, String.class);
            System.out.println("Comandă trimisă către Robot: " + command);
        } catch (Exception e) {
            System.err.println("Eroare: Robotul nu răspunde! " + e.getMessage());
            throw new RuntimeException("Robot offline sau IP greșit!");
        }
    }
}
