package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.service.FileDataService;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

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
        @RequestParam(value = "folderId", required = false) Long folderId) {

        String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

        FileData fileData = fileDataService.uploadFile(file, email, folderId);

        return ResponseEntity.ok(fileData);
    }


    // Get all files
    @GetMapping("/all")
    public ResponseEntity<List<FileData>> getAllFiles() {

        List<FileData> files = fileDataService.getAllFiles();

        return ResponseEntity.ok(files);
    }
    // Get logged-in user's files
    @GetMapping("/my")
    public ResponseEntity<List<FileData>> getMyFiles() {

        List<FileData> files = fileDataService.getMyFiles();

        return ResponseEntity.ok(files);
    }
    


    @GetMapping("/download/{id}")
public ResponseEntity<Resource> downloadFile(
        @PathVariable Long id,
        Authentication authentication) {

    String email = authentication.getName();

    Resource resource = fileDataService.downloadFile(id, email);

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
    // Move file to another folder
        @PutMapping("/{id}/move")
    public ResponseEntity<String> moveFile(
        @PathVariable Long id,
        @RequestParam Long folderId) {

        return ResponseEntity.ok(
            fileDataService.moveFile(id, folderId)
        );
    }
    // View Trash
    @GetMapping("/trash")
    public ResponseEntity<List<FileData>> getTrashFiles() {

        return ResponseEntity.ok(
                fileDataService.getTrashFiles()
        );
    }
    // Restore file
    @PutMapping("/restore/{id}")
    public ResponseEntity<String> restoreFile(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                fileDataService.restoreFile(id)
        );
    }
    // Permanently delete file
    @DeleteMapping("/permanent/{id}")
    public ResponseEntity<String> permanentDelete(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            fileDataService.permanentDelete(id)
        );
    }
    // Share file
    @PostMapping("/{id}/share")
    public ResponseEntity<String> shareFile(
        @PathVariable Long id,
        @RequestParam String email) {

        return ResponseEntity.ok(
            fileDataService.shareFile(id, email)
        );
    }
    @GetMapping("/shared")
    public ResponseEntity<List<FileData>> getSharedFiles(
        Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
            fileDataService.getSharedFiles(email)
        );
    }
}