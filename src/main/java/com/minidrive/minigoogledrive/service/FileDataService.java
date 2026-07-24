package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.repository.FileDataRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class FileDataService {

    @Autowired
    private FileDataRepository fileDataRepository;

    private final String uploadDir = "uploads/";

    public FileData uploadFile(MultipartFile file) {

        try {

            // Create uploads folder if not exists
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // File path
            Path filePath = Paths.get(
                    uploadDir + file.getOriginalFilename()
            );

            // Save file into folder
            Files.write(
                    filePath,
                    file.getBytes()
            );

            // Save file details in database
            FileData fileData = new FileData();

            fileData.setFileName(file.getOriginalFilename());
            fileData.setFileType(file.getContentType());
            fileData.setFilePath(filePath.toString());
            fileData.setFileSize(file.getSize());
            fileData.setUploadDate(LocalDateTime.now());

            return fileDataRepository.save(fileData);

        } catch (IOException e) {

            throw new RuntimeException("File upload failed", e);

        }
    }
}