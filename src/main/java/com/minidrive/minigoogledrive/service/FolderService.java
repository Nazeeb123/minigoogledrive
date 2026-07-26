package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.Folder;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.FolderRepository;
import com.minidrive.minigoogledrive.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FolderService {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private UserRepository userRepository;


    // Create Folder
    public Folder createFolder(String folderName) {


        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();


        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));


        Folder folder = new Folder();

        folder.setFolderName(folderName);
        folder.setCreatedDate(LocalDateTime.now());
        folder.setUser(user);


        return folderRepository.save(folder);
    }



    // Get My Folders
    public List<Folder> getMyFolders() {


        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();


        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));


        return folderRepository.findByUser(user);
    }
    // Delete Folder
    public String deleteFolder(Long id) {

        Folder folder = folderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Folder not found"));

        folderRepository.delete(folder);

     return "Folder deleted successfully";
    }
}