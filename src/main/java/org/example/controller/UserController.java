package org.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import java.util.Map;

//@RequestMapping("/api/users")
@RestController
@Slf4j
public class UserController {

    @GetMapping("/verify")
    public Map<String, Object> verifyUserData(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-FirstName", required = false) String firstName,
            @RequestHeader(value = "X-User-LastName", required = false) String lastName,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader Map<String, String> allHeaders
    ) {
        log.info("=== User Verification Request ===");
        log.info("User ID: {}", userId);
        log.info("Username: {}", username);
        log.info("Email: {}", email);
        log.info("First Name: {}", firstName);
        log.info("Last Name: {}", lastName);

        Map<String, Object> response = new HashMap<>();

        // User data extracted from JWT
        Map<String, String> userData = new HashMap<>();
        userData.put("userId", userId != null ? userId : "NOT PROVIDED");
        userData.put("username", username != null ? username : "NOT PROVIDED");
        userData.put("email", email != null ? email : "NOT PROVIDED");
        userData.put("firstName", firstName != null ? firstName : "NOT PROVIDED");
        userData.put("lastName", lastName != null ? lastName : "NOT PROVIDED");
        userData.put("fullName", (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : ""));

        response.put("service", "user-service");
        response.put("message", "User data verification");
        response.put("userData", userData);

        // Show all headers that start with X-
        Map<String, String> customHeaders = new HashMap<>();
        allHeaders.forEach((key, value) -> {
            if (key.toLowerCase().startsWith("x-")) {
                customHeaders.put(key, value);
            }
        });
        response.put("customHeaders", customHeaders);

        // Check if data is present
        boolean allDataPresent = userId != null && username != null && email != null;
        response.put("isComplete", allDataPresent);
        response.put("status", allDataPresent ? "✅ All user data received" : "❌ Missing user data");

        return response;
    }

    /**
     * Simple hello endpoint
     */
    @GetMapping("/hello")
    public Map<String, Object> hello(
            @RequestHeader(value = "X-User-Name", required = false) String username
    ) {
        return Map.of(
                "service", "user-service",
                "message", username != null
                        ? "Hello " + username + " from User Service!"
                        : "Hello from User Service! (No user identified)"
        );
    }
}
