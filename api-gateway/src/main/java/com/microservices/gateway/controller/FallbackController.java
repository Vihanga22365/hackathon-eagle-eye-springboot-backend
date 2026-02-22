package com.microservices.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Controller
 * 
 * Provides fallback responses when circuit breakers are triggered.
 * This ensures graceful degradation when downstream services are unavailable.
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @RequestMapping(value = "/auth", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Map<String, String>> authServiceFallback() {
        log.warn("Auth service is unavailable, returning fallback response");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Auth service is temporarily unavailable. Please try again later.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @RequestMapping(value = "/user", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Map<String, String>> userServiceFallback() {
        log.warn("User service is unavailable, returning fallback response");
        Map<String, String> response = new HashMap<>();
        response.put("message", "User service is temporarily unavailable. Please try again later.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @RequestMapping(value = "/loan", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<Map<String, String>> loanServiceFallback() {
        log.warn("Loan service is unavailable, returning fallback response");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Loan service is temporarily unavailable. Please try again later.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
