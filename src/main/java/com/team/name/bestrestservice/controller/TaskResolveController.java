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

    @GetMapping(value = "/healthz", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> checkHealth() {
        return ResponseEntity.ok(Collections.singletonMap("status", "OK"));
    }

    @PostMapping(value = "/move", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> move(@RequestBody GameStatus gameStatus) {
        return ResponseEntity.ok(Collections.singletonMap("move", SpaceshipAI.determineMove(gameStatus)));
    }


    public static class GameStatus implements Serializable {

        public GameStatus(String[][] field, int narrowingIn, int gameId) {
            this.field = field;
            this.narrowingIn = narrowingIn;
            this.gameId = gameId;
        }

        public String [][] field = new String [13][13];
        public int narrowingIn;
        public int gameId;
    }
}
