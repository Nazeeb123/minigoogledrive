package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.service.FileDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileDataController {

    @Autowired
    private FileDataService fileDataService;


    // Upload file
    @PostMapping("/upload")
    public ResponseEntity<FileData> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("email") String email) {

        FileData fileData = fileDataService.uploadFile(file, email);

        return ResponseEntity.ok(fileData);
    }


    // Get all files
    @GetMapping("/all")
    public ResponseEntity<List<FileData>> getAllFiles() {

        List<FileData> files = fileDataService.getAllFiles();

        return ResponseEntity.ok(files);
    }


    // Download file
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id) {

        Resource resource = fileDataService.downloadFile(id);

        return ResponseEntity.ok(resource);
    }


    // Delete file
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFile(
            @PathVariable Long id) {

        String response = fileDataService.deleteFile(id);

        return ResponseEntity.ok(response);
    }


    // Search file
    @GetMapping("/search")
    public ResponseEntity<List<FileData>> searchFiles(
            @RequestParam String fileName) {

        return ResponseEntity.ok(
                fileDataService.searchFiles(fileName)
        );
    }


    // Rename file
    @PutMapping("/rename/{id}")
    public ResponseEntity<String> renameFile(
            @PathVariable Long id,
            @RequestParam String newName) {

        return ResponseEntity.ok(
                fileDataService.renameFile(id, newName)
        );
    }
}