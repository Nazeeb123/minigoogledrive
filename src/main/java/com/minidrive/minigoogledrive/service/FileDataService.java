package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.FileDataRepository;
import com.minidrive.minigoogledrive.repository.FolderRepository;
import com.minidrive.minigoogledrive.repository.UserRepository;
import com.minidrive.minigoogledrive.model.Folder;
import com.minidrive.minigoogledrive.model.SharedFile;
import com.minidrive.minigoogledrive.repository.SharedFileRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileDataService {

    @Autowired
    private FileDataRepository fileDataRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FolderRepository folderRepository;
    @Autowired
    private SharedFileRepository sharedFileRepository;


    private final String uploadDir = "uploads/";


    // Upload File
    public FileData uploadFile(MultipartFile file, String email, Long folderId) {

    try {

        File directory = new File(uploadDir);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        Path filePath = Paths.get(
                uploadDir,
                file.getOriginalFilename()
        );

        Files.copy(
                file.getInputStream(),
                filePath,
                StandardCopyOption.REPLACE_EXISTING
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Folder folder = null;

        if (folderId != null) {
            folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
        }

        FileData fileData = new FileData();

        fileData.setFileName(file.getOriginalFilename());
        fileData.setFileType(file.getContentType());
        fileData.setFilePath(filePath.toString());
        fileData.setFileSize(file.getSize());
        fileData.setUploadDate(LocalDateTime.now());

        fileData.setUser(user);
        fileData.setFolder(folder);

        return fileDataRepository.save(fileData);

    } catch (IOException e) {

        throw new RuntimeException("File upload failed", e);
    }
}

    // Get all files
    public List<FileData> getAllFiles() {

        return fileDataRepository.findAll();
    }
    public List<FileData> getMyFiles() {

    var authentication =
            SecurityContextHolder
                    .getContext()
                    .getAuthentication();


    System.out.println("AUTH OBJECT: " + authentication);

    System.out.println(
            "AUTH NAME: " + authentication.getName()
    );

    System.out.println(
            "AUTH CLASS: " + authentication.getClass()
    );


    String email = authentication.getName();


    User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                    new RuntimeException("User not found")
            );


   return fileDataRepository.findByUserAndDeletedFalse(user);
}

    // Download file (secured)
public Resource downloadFile(Long id, String email) {

    try {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));


        FileData fileData = fileDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));


        boolean isOwner = fileData.getUser().getId()
                .equals(user.getId());


        boolean isShared = fileData.getSharedUsers() != null
                && fileData.getSharedUsers().contains(user);


        if (!isOwner && !isShared) {
            throw new RuntimeException("You cannot access this file");
        }


        Path path = Paths.get(fileData.getFilePath()).normalize();


        if (!Files.exists(path)) {
            throw new RuntimeException("File does not exist");
        }


        return new UrlResource(path.toUri());


    } catch (MalformedURLException e) {

        throw new RuntimeException("Download failed", e);
    }
}
    


    // Delete file (secured)
public String deleteFile(Long id) {

    FileData fileData = fileDataRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("File not found"));


    // Get logged-in user email from JWT
    String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();


    // Find logged-in user
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));


    // Check ownership
    if (!fileData.getUser().getId().equals(user.getId())) {

        throw new RuntimeException("You cannot delete this file");
    }


    // Delete physical file
    File file = new File(fileData.getFilePath());

    if (file.exists()) {
        file.delete();
    }


    // Delete database record
    fileDataRepository.delete(fileData);


    return "File deleted successfully";
}



    // Search files
    public List<FileData> searchFiles(String fileName) {

        return fileDataRepository
                .findByFileNameContainingIgnoreCase(fileName);
    }



    // Rename file (secured)
    public String renameFile(Long id, String newName) {

        try {

        // Get logged-in user email from JWT
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();


        // Find logged-in user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));


        // Find file
        FileData fileData = fileDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));


        // Check ownership
        if (!fileData.getUser().getId().equals(user.getId())) {

            throw new RuntimeException("You cannot rename this file");
        }


        // Old file path
        Path oldPath = Paths.get(fileData.getFilePath());


        // New file path
        Path newPath = oldPath.resolveSibling(newName);


        // Rename physical file
        Files.move(
                oldPath,
                newPath,
                StandardCopyOption.REPLACE_EXISTING
        );


        // Update database
        fileData.setFileName(newName);
        fileData.setFilePath(newPath.toString());


        fileDataRepository.save(fileData);


        return "File renamed successfully";


        } catch (Exception e) {

        throw new RuntimeException(
                "Rename failed",
                e
        );
        }
    }
    // Get all files in a folder
    public List<FileData> getFilesInFolder(Long folderId) {

    // Get logged-in user's email
        String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

        // Find logged-in user
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Find folder
        Folder folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new RuntimeException("Folder not found"));

        // Check folder ownership
        if (!folder.getUser().getId().equals(user.getId())) {
        throw new RuntimeException("You cannot access this folder");
        }

        // Return files in folder
        return fileDataRepository.findByFolder(folder);
    }
    // Move file to another folder
    public String moveFile(Long fileId, Long folderId) {

    // Logged-in user
        String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // File
        FileData fileData = fileDataRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File not found"));

        // Ownership check
        if (!fileData.getUser().getId().equals(user.getId())) {
        throw new RuntimeException("You cannot move this file");
        }

        // Folder
        Folder folder = folderRepository.findById(folderId)
            .orElseThrow(() -> new RuntimeException("Folder not found"));

        // Folder ownership check
        if (!folder.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You cannot use this folder");
        }

        // Move
        fileData.setFolder(folder);

        fileDataRepository.save(fileData);

        return "File moved successfully";
    }
    // Get files in Trash
    public List<FileData> getTrashFiles() {

        String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return fileDataRepository.findByUserAndDeletedTrue(user);
    }
    public List<FileData> getSharedFiles(String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return fileDataRepository.findBySharedUsers(user);
    }
    // Restore file from Trash
    public String restoreFile(Long id) {

        String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        FileData fileData = fileDataRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("File not found"));

        if (!fileData.getUser().getId().equals(user.getId())) {
        throw new RuntimeException("You cannot restore this file");
        }

        fileData.setDeleted(false);

        fileDataRepository.save(fileData);

        return "File restored successfully";
    }
    // Permanently delete file
    public String permanentDelete(Long id) {

        String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        FileData fileData = fileDataRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("File not found"));

        if (!fileData.getUser().getId().equals(user.getId())) {
        throw new RuntimeException("You cannot delete this file");
        }

        // Delete physical file
        File file = new File(fileData.getFilePath());

        if (file.exists()) {
        file.delete();
        }

        // Delete database record
        fileDataRepository.delete(fileData);

            return "File permanently deleted";
    }
    // Share file with another user
    public String shareFile(Long id, String email) {

    FileData fileData = fileDataRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("File not found"));


    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));


    fileData.getSharedUsers().add(user);


    fileDataRepository.save(fileData);


    return "File shared successfully";
}
}