package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.model.FileVersion;
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
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileDataController {

    @Autowired
    private FileDataService fileDataService;

    @Autowired
    private UserRepository userRepository;


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
    @PutMapping("/{id}/star")
    public ResponseEntity<FileData> toggleStar(@PathVariable Long id,
                                           Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        FileData updatedFile = fileDataService.toggleStar(id, user);

        return ResponseEntity.ok(updatedFile);
    }
    @GetMapping("/starred")
    public ResponseEntity<List<FileData>> getStarredFiles(Authentication authentication) {

     User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<FileData> starredFiles = fileDataService.getStarredFiles(user);

        return ResponseEntity.ok(starredFiles);
    }
    @GetMapping("/recent")
    public ResponseEntity<List<FileData>> getRecentFiles(Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<FileData> recentFiles = fileDataService.getRecentFiles(user);

        return ResponseEntity.ok(recentFiles);
    }
    @GetMapping("/storage")
    public ResponseEntity<Map<String, Object>> getStorageUsage(Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> storage = fileDataService.getStorageUsage(user);

            return ResponseEntity.ok(storage);
    }
    @PostMapping("/{id}/share-link")
    public ResponseEntity<String> generateShareLink(
        @PathVariable Long id,
        Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        String shareLink = fileDataService.generateShareLink(id, user);

        return ResponseEntity.ok(shareLink);
    }
    @GetMapping("/shared/{token}")
    public ResponseEntity<Resource> downloadSharedFile(
        @PathVariable String token) {

        Resource resource = fileDataService.downloadSharedFile(token);

        return ResponseEntity.ok()
            .body(resource);
    }
    @PutMapping("/{id}/disable-share")
    public ResponseEntity<FileData> disableShareLink(
        @PathVariable Long id,
        Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        FileData fileData = fileDataService.disableShareLink(id, user);

        return ResponseEntity.ok(fileData);
    }
    @PostMapping("/{id}/versions")
    public ResponseEntity<FileData> uploadNewVersion(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        Authentication authentication) {


        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));


        FileData updatedFile =
            fileDataService.uploadNewVersion(id, file, user);


        return ResponseEntity.ok(updatedFile);
    }
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<FileVersion>> getFileVersions(
        @PathVariable Long id) {

        List<FileVersion> versions =
            fileDataService.getFileVersions(id);

        return ResponseEntity.ok(versions);
    }
    @PutMapping("/versions/{versionId}/restore")
    public ResponseEntity<FileData> restoreVersion(
        @PathVariable Long versionId,
        Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        FileData restoredFile = fileDataService.restoreVersion(versionId, user);

        return ResponseEntity.ok(restoredFile);
    }
    
}