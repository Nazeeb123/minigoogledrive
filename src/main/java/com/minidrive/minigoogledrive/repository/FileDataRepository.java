package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.model.Folder;
import com.minidrive.minigoogledrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileDataRepository extends JpaRepository<FileData, Long> {

    List<FileData> findByFileNameContainingIgnoreCase(String fileName);

    List<FileData> findByUser(User user);
    List<FileData> findByFolder(Folder folder);
    List<FileData> findByUserAndDeletedFalse(User user);

    List<FileData> findByUserAndDeletedTrue(User user);
    List<FileData> findBySharedUsers(User user);

}