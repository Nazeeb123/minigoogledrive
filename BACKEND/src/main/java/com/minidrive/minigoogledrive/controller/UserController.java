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

    @PostMapping("/forgot-password")
    public java.util.Map<String, String> forgotPassword(@RequestBody java.util.Map<String, String> request) {
        userService.requestPasswordReset(request.get("email"));
        return java.util.Map.of("message", "If an account exists, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public java.util.Map<String, String> resetPassword(@RequestBody java.util.Map<String, String> request) {
        userService.resetPassword(request.get("token"), request.get("password"));
        return java.util.Map.of("message", "Your password has been updated. Please sign in.");
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
