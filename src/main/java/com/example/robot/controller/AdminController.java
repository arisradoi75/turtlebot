package com.example.robot.controller;

import com.example.robot.service.CommandService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
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
        // commandService.sendCommand("DOCK"); // Temporarily commented out for testing without robot IP
        return ResponseEntity.ok("Comanda [DOCK] a fost trimisă!");
    }

    @PostMapping("/patrol")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> startPatrol() {
        // commandService.sendCommand("PATROL"); // Temporarily commented out
        return ResponseEntity.ok("Comanda [PATROL] a fost trimisă!");
    }

    @PostMapping("/stop")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> stopRobot() {
        // commandService.sendCommand("STOP"); // Temporarily commented out
        return ResponseEntity.ok("Robotul a fost OPRIT de urgență!");
    }

    @GetMapping("/whoami")
    public ResponseEntity<String> whoAmI() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity.ok(
                "Nume: " + auth.getName() + "\n" +
                        "Roluri detectate de Java: " + auth.getAuthorities()
        );

    }
}
