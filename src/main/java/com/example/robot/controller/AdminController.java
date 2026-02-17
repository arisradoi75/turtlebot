package com.example.robot.controller;
import com.example.robot.dto.MoveRequestDTO;
import com.example.robot.service.CommandService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    private final CommandService commandService;

    public AdminController(CommandService commandService) {
        this.commandService = commandService;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/dock")
    public ResponseEntity<String> sendToDock() {
        commandService.sendCommand("DOCK");
        return ResponseEntity.ok("Comanda [DOCK] a fost trimisă!");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/start")
    public ResponseEntity<String> startRobot() {
        commandService.sendCommand("START");
        return ResponseEntity.ok("Comanda [START] a fost trimisă!");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/stop")
    public ResponseEntity<String> stopRobot() {
        commandService.sendCommand("STOP");
        return ResponseEntity.ok("Robotul a fost OPRIT de urgență!");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/move")
    public ResponseEntity<String> moveToCoordinates(@RequestBody MoveRequestDTO moveRequest) {
        // Creăm un Map cu datele suplimentare
        Map<String, Object> data = new HashMap<>();
        data.put("x", moveRequest.getX());
        data.put("y", moveRequest.getY());

        // Apelăm metoda de serviciu cu comanda "MOVE" și datele
        commandService.sendCommand("MOVE", data);

        return ResponseEntity.ok("Comanda [MOVE] a fost trimisă cu coordonatele: " + moveRequest);
    }
}
