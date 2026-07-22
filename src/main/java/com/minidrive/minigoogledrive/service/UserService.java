package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.config.JwtService;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }


    // Register
    public User registerUser(User user) {

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        return userRepository.save(user);
    }


    // Login + Generate JWT
    public String loginUser(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));


        if (!passwordEncoder.matches(password, user.getPassword())) {

            throw new RuntimeException("Invalid password");
        }


        return jwtService.generateToken(email);
    }
}