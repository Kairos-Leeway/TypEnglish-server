package com.typenglish.controller;

import com.typenglish.common.Result;
import com.typenglish.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/xp/gain")
    public Result<Map<String, Object>> gainXp(@RequestBody Map<String, Object> body) {
        int amount = body.containsKey("amount") ? ((Number) body.get("amount")).intValue() : 10;
        var xp = userService.gainXp(amount);
        return Result.ok(Map.of("level", xp.level(), "xp", xp.xp(),
                "totalXp", xp.totalXp(), "gained", xp.gained()));
    }

    @GetMapping("/xp")
    public Result<Map<String, Object>> getXp() {
        var xp = userService.getXp();
        return Result.ok(Map.of("level", xp.level(), "xp", xp.xp(), "totalXp", xp.totalXp()));
    }
}
