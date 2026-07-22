package com.minidrive.minigoogledrive.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minidrive.minigoogledrive.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

}
