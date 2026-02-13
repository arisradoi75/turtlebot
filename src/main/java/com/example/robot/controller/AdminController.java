package com.example.robot.controller;

import com.example.robot.service.CommandService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> startRobot() {
        commandService.sendCommand("START");
        return ResponseEntity.ok("Comanda [START] a fost trimisă!");
    }

    @PostMapping("/stop")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> stopRobot() {
        commandService.sendCommand("STOP");
        return ResponseEntity.ok("Robotul a fost OPRIT de urgență!");
    }
}
