package org.example.controller;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

//@RequestMapping("/api/users")
@RestController
public class UserController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        return Map.of(
                "service", "user-service",
                "message", "Hello World from User Service!"
        );
    }
}
