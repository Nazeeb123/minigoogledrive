package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.FileDataRepository;
import com.minidrive.minigoogledrive.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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


    private final String uploadDir = "uploads/";


    // Upload File
    public FileData uploadFile(MultipartFile file, String email) {

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


            FileData fileData = new FileData();

            fileData.setFileName(file.getOriginalFilename());
            fileData.setFileType(file.getContentType());
            fileData.setFilePath(filePath.toString());
            fileData.setFileSize(file.getSize());
            fileData.setUploadDate(LocalDateTime.now());

            fileData.setUser(user);


            return fileDataRepository.save(fileData);


        } catch (IOException e) {

            throw new RuntimeException("File upload failed", e);
        }
    }



    // Get all files
    public List<FileData> getAllFiles() {

        return fileDataRepository.findAll();
    }



    // Download file
    public Resource downloadFile(Long id) {

        try {

            FileData fileData = fileDataRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));


            Path path = Paths.get(fileData.getFilePath());


            if (!Files.exists(path)) {
                throw new RuntimeException("File does not exist");
            }


            return new UrlResource(path.toUri());


        } catch (MalformedURLException e) {

            throw new RuntimeException("Download failed", e);
        }
    }



    // Delete file
    public String deleteFile(Long id) {

        FileData fileData = fileDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));


        File file = new File(fileData.getFilePath());


        if (file.exists()) {
            file.delete();
        }


        fileDataRepository.delete(fileData);


        return "File deleted successfully";
    }



    // Search files
    public List<FileData> searchFiles(String fileName) {

        return fileDataRepository
                .findByFileNameContainingIgnoreCase(fileName);
    }



    // Rename file
    public String renameFile(Long id, String newName) {


        try {

            FileData fileData = fileDataRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));


            Path oldPath = Paths.get(fileData.getFilePath());


            Path newPath = oldPath.resolveSibling(newName);


            Files.move(
                    oldPath,
                    newPath,
                    StandardCopyOption.REPLACE_EXISTING
            );


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
}