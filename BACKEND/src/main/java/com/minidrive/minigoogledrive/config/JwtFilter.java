package com.minidrive.minigoogledrive.config;

import com.minidrive.minigoogledrive.service.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

        private final JwtService jwtService;
        private final CustomUserDetailsService userDetailsService;

        public JwtFilter(
                        JwtService jwtService,
                        CustomUserDetailsService userDetailsService) {

                this.jwtService = jwtService;
                this.userDetailsService = userDetailsService;
        }

        @Override
        protected boolean shouldNotFilter(
                        HttpServletRequest request) {

                String path = request.getRequestURI();

                return path.equals("/register")
                                || path.equals("/login")
                                || path.equals("/google-login");
        }

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain)
                        throws ServletException, IOException {

                System.out.println(
                                "JWT FILTER RUNNING: "
                                                + request.getRequestURI());

                String authHeader = request.getHeader("Authorization");

                System.out.println(
                                "AUTH HEADER: " + authHeader);

                String token = null;
                String email = null;

                try {

                        // Check Authorization header
                        if (authHeader != null
                                        && authHeader.startsWith("Bearer ")) {

                                token = authHeader.substring(7);

                                email = jwtService.extractUsername(token);

                                System.out.println(
                                                "TOKEN EMAIL: " + email);
                        }

                        // Authenticate user
                        if (email != null
                                        && SecurityContextHolder
                                                        .getContext()
                                                        .getAuthentication() == null) {

                                UserDetails userDetails = userDetailsService
                                                .loadUserByUsername(email);

                                if (jwtService.validateToken(
                                                token,
                                                email)) {

                                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                                        userDetails,
                                                        null,
                                                        userDetails.getAuthorities());

                                        authentication.setDetails(
                                                        new WebAuthenticationDetailsSource()
                                                                        .buildDetails(request));

                                        SecurityContextHolder
                                                        .getContext()
                                                        .setAuthentication(
                                                                        authentication);

                                        System.out.println(
                                                        "AUTHENTICATED USER: "
                                                                        + authentication.getName());
                                }
                        }

                } catch (Exception e) {

                        System.out.println(
                                        "JWT ERROR: "
                                                        + e.getClass().getSimpleName());

                        System.out.println(
                                        "JWT ERROR MESSAGE: "
                                                        + e.getMessage());

                        // Clear invalid authentication
                        SecurityContextHolder
                                        .clearContext();
                }

                filterChain.doFilter(
                                request,
                                response);
        }
}