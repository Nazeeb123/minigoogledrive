package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.Folder;
import com.minidrive.minigoogledrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolderRepository 
        extends JpaRepository<Folder,Long> {

    List<Folder> findByUser(User user);
    List<Folder> findByUserAndFolderNameContainingIgnoreCase(
            User user,
            String folderName
    );
    List<Folder> findByFolderNameContainingIgnoreCase( String query);
}
