package com.team.name.bestrestservice.controller;

import com.team.name.bestrestservice.SpaceshipAI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

@RestController
public class TaskResolveController {

    @PostMapping(value = "/move", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> move(@RequestBody SpaceshipAI.GameStatus gameStatus) {
        SpaceshipAI spaceshipAI = new SpaceshipAI();
        return ResponseEntity.ok(Collections.singletonMap("move", spaceshipAI.decideMove(gameStatus)));
    }

}
