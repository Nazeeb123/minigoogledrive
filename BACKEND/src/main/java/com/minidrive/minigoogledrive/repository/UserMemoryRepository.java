package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMemoryRepository
        extends JpaRepository<UserMemory, Long> {

    List<UserMemory> findByUserEmailOrderByUpdatedAtDesc(
            String userEmail
    );

    void deleteByUserEmail(String userEmail);
}
