package com.typenglish.controller;

import com.typenglish.common.Result;
import com.typenglish.dto.AuthDTO;
import com.typenglish.entity.User;
import com.typenglish.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody AuthDTO.RegisterReq req) {
        var result = authService.register(req.getUsername(), req.getEmail(), req.getPassword());
        return Result.ok(userMap(result));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody AuthDTO.LoginReq req) {
        var result = authService.login(req.getAccount(), req.getPassword());
        return Result.ok(userMap(result));
    }

    private Map<String, Object> userMap(AuthService.AuthResult result) {
        User u = result.user();
        return Map.of(
                "token", result.token(),
                "user", Map.of("id", u.getId(), "username", u.getUsername(), "email", u.getEmail(),
                        "level", u.getLevel() != null ? u.getLevel() : 1,
                        "xp", u.getXp() != null ? u.getXp() : 0,
                        "totalXp", u.getTotalXp() != null ? u.getTotalXp() : 0)
        );
    }
}
