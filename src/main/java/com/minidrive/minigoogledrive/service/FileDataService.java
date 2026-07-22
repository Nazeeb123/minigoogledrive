package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.repository.FileDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileDataService {

    @Autowired
    private FileDataRepository fileDataRepository;

    // Upload File
    public FileData uploadFile(MultipartFile file) throws IOException {

        String uploadDir = System.getProperty("user.dir") + File.separator + "uploads";

        File folder = new File(uploadDir);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String filePath = uploadDir + File.separator + file.getOriginalFilename();

        file.transferTo(new File(filePath));

        FileData fileData = new FileData();

        fileData.setFileName(file.getOriginalFilename());
        fileData.setFileType(file.getContentType());
        fileData.setFileSize(file.getSize());
        fileData.setFilePath(filePath);
        fileData.setUploadDate(LocalDateTime.now());

        return fileDataRepository.save(fileData);
    }

    // Get All Files
    public List<FileData> getAllFiles() {
        return fileDataRepository.findAll();
    }

    // Download File
    public Resource downloadFile(Long id) throws MalformedURLException {

        FileData fileData = fileDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        Path path = Paths.get(fileData.getFilePath());

        return new UrlResource(path.toUri());
    }

    // Delete File
    public String deleteFile(Long id) {

        FileData fileData = fileDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        File file = new File(fileData.getFilePath());

        if (file.exists()) {
            file.delete();
        }

        fileDataRepository.delete(fileData);

        return "File deleted successfully.";
    }
}
