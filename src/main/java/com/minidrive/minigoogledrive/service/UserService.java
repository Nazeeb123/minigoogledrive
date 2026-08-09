
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

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }


    // ==============================
    // REGISTER
    // ==============================

    public User registerUser(User user) {

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        String password = user.getPassword();

        if (password.length() < 8) {
            throw new RuntimeException(
                    "Password must be at least 8 characters long"
            );
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException(
                    "Password must contain at least one uppercase letter"
            );
        }

        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException(
                    "Password must contain at least one lowercase letter"
            );
        }

        if (!password.matches(".*\\d.*")) {
            throw new RuntimeException(
                    "Password must contain at least one number"
            );
        }

        return userRepository.save(user);
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

