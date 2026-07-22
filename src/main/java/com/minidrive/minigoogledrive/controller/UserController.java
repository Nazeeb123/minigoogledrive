package com.minidrive.minigoogledrive.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.minidrive.minigoogledrive.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.dto.LoginRequest;

@RestController
public class UserController {

    @Autowired
    private UserService userService;
    @PostMapping("/register")
    public void registerUser(@RequestBody User user) {
        userService.registerUser(user);
    }
    @PostMapping("/login")
    public User loginUser(@RequestBody LoginRequest loginRequest) {
        return userService.loginUser(
            loginRequest.getEmail(),
            loginRequest.getPassword()
        );
    }
    
}
