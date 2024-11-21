package com.team.name.bestrestservice.controller;

import com.team.name.bestrestservice.SpaceshipAI;
import com.team.name.bestrestservice.SpaceshipAiV0;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class TaskResolveController {

    private int currentVersion = 1;

    @GetMapping(value = "/healthz", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> checkHealth() {
        return ResponseEntity.ok(Collections.singletonMap("status", "OK"));
    }


    @PostMapping(value = "/move", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> move(@RequestBody SpaceshipAI.GameStatus gameStatus) {
        SpaceshipAI spaceshipAI = new SpaceshipAI();
        if (currentVersion == 0) {
            SpaceshipAiV0 spaceshipAI0 = new SpaceshipAiV0();
            return ResponseEntity.ok(Collections.singletonMap("move", spaceshipAI0.decideMove(gameStatus)));
        } else {
            return ResponseEntity.ok(Collections.singletonMap("move", spaceshipAI.decideMove(gameStatus)));
        }
    }

    @PostMapping(value = "/changeVersion", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> move(@RequestBody Integer version) {
        this.currentVersion = version;
        return ResponseEntity.ok(Collections.singletonMap("version", currentVersion));
    }

}
