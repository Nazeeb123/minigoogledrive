package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.dto.LoginRequest;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {

        return userService.registerUser(user);
    }

    @PostMapping("/login")
    public String loginUser(@RequestBody LoginRequest loginRequest) {

        System.out.println("========== LOGIN API HIT ==========");

        return userService.loginUser(
                loginRequest.getEmail(),
                loginRequest.getPassword());
    }

    @PostMapping("/google-login")
    public String googleLogin(@RequestBody java.util.Map<String, String> request) {

        String credential = request.get("credential");

        if (credential == null || credential.isEmpty()) {
            throw new RuntimeException("Google credential missing");
        }

        return userService.googleLogin(credential);
    }

}