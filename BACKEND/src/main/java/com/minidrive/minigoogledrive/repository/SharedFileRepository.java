package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.SharedFile;
import com.minidrive.minigoogledrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharedFileRepository extends JpaRepository<SharedFile, Long> {

    List<SharedFile> findBySharedWith(User user);
}