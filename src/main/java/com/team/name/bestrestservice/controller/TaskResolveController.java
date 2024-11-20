package com.team.name.bestrestservice.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class TaskResolveController {

    @GetMapping(value = "/healthz", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> checkHealth() {
        return ResponseEntity.ok(Collections.singletonMap("status", "OK"));
    }

    @GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> testHeroku() {
        return ResponseEntity.ok(Collections.singletonMap("testNumber", "1"));
    }
}
