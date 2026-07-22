package com.minidrive.minigoogledrive.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(User user) {

        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if(existingUser.isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }
    public User loginUser(String email, String password) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if(!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return user;
    }
}