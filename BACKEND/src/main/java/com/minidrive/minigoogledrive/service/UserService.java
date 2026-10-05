
package com.minidrive.minigoogledrive.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import com.minidrive.minigoogledrive.config.JwtService;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }


    // ==============================
    // REGISTER
    // ==============================

    public User registerUser(User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        validateGmail(user.getEmail());
        validatePassword(user.getPassword());
        if (userRepository.findByEmail(user.getEmail().trim().toLowerCase()).isPresent()) {
            throw new RuntimeException("An account already exists for this email");
        }
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public void requestPasswordReset(String email) {
        validateGmail(email);
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), frontendUrl + "/reset-password?token=" + token);
        });
    }

    public void resetPassword(String token, String password) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("This reset link is invalid or has expired");
        }
        validatePassword(password);
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("This reset link is invalid or has expired"));
        if (user.getPasswordResetTokenExpiresAt() == null
                || user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("This reset link is invalid or has expired");
        }
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
    }

    private void validateGmail(String email) {
        if (email == null || !email.trim().matches("^[A-Za-z0-9._%+-]+@gmail\\.com$")) {
            throw new RuntimeException("Please use a valid @gmail.com address");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*\\d.*")) {
            throw new RuntimeException("Password must be 8+ characters and include uppercase, lowercase, and a number");
        }
    }


    // ==============================
    // NORMAL LOGIN
    // ==============================

    public String loginUser(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        if (!passwordEncoder.matches(
                password,
                user.getPassword()
        )) {

            throw new RuntimeException("Invalid password");
        }

        return jwtService.generateToken(email);
    }


    // ==============================
    // GOOGLE LOGIN
    // ==============================

    public String googleLogin(String credential) {

        try {

            GoogleIdTokenVerifier verifier =
                    new GoogleIdTokenVerifier.Builder(
                            new NetHttpTransport(),
                            GsonFactory.getDefaultInstance()
                    )
                    .setAudience(
                            Collections.singletonList(
                                    "97919262881-qf278cjpj709lodidkp7q1e5jea5ctb4.apps.googleusercontent.com"
                            )
                    )
                    .build();


            GoogleIdToken idToken =
                    verifier.verify(credential);


            if (idToken == null) {

                throw new RuntimeException(
                        "Invalid Google credential"
                );
            }


            GoogleIdToken.Payload payload =
                    idToken.getPayload();


            String email =
                    payload.getEmail();

            String name =
                    (String) payload.get("name");


            // ==============================
            // FIND EXISTING USER
            // ==============================

            User user =
                    userRepository.findByEmail(email)
                            .orElse(null);


            // ==============================
            // CREATE USER IF NOT EXISTS
            // ==============================

            if (user == null) {

                user = new User();

                user.setEmail(email);

                user.setUsername(
                        name != null
                                ? name
                                : email.split("@")[0]
                );

                /*
                 * Google users don't need a real password
                 * for Google login.
                 *
                 * We store a random encoded value so that
                 * password field is never NULL.
                 */

                user.setPassword(
                        passwordEncoder.encode(
                                java.util.UUID.randomUUID().toString()
                        )
                );

                userRepository.save(user);
            }


            // ==============================
            // GENERATE YOUR JWT
            // ==============================

            return jwtService.generateToken(email);


        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Google login failed"
            );
        }
    }
}

