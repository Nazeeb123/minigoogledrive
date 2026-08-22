package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.model.FileVersion;
import com.minidrive.minigoogledrive.service.EmbeddingService;
import com.minidrive.minigoogledrive.service.FileDataService;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.FileDataRepository;
import com.minidrive.minigoogledrive.repository.UserRepository;
import com.minidrive.minigoogledrive.model.SearchResult;
import com.minidrive.minigoogledrive.service.SemanticSearchService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;

@RestController
@RequestMapping("/files")
public class FileDataController {

        @Autowired
        private FileDataService fileDataService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private FileDataRepository fileDataRepository;

        @Autowired
        private EmbeddingService embeddingService;

        @Autowired
        private SemanticSearchService semanticSearchService;

        // Upload file
        @PostMapping("/upload")
        public ResponseEntity<FileData> uploadFile(
                        @RequestParam("file") MultipartFile file,
                        @RequestParam(value = "folderId", required = false) Long folderId,
                        @RequestParam(value = "fileName", required = false) String fileName) {

                String email = SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName();

                FileData fileData = fileDataService.uploadFile(
                                file,
                                email,
                                folderId,
                                fileName);

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
        public ResponseEntity<List<SearchResult>> searchFiles(
                        @RequestParam String query) {

                return ResponseEntity.ok(
                                fileDataService.globalSearch(query));

        }

        // Rename file
        @PutMapping("/rename/{id}")
        public ResponseEntity<String> renameFile(
                        @PathVariable Long id,
                        @RequestParam String newName) {

                return ResponseEntity.ok(
                                fileDataService.renameFile(id, newName));
        }

        // Move file to another folder
        @PutMapping("/{id}/move")
        public ResponseEntity<String> moveFile(
                        @PathVariable Long id,
                        @RequestParam Long folderId) {

                return ResponseEntity.ok(
                                fileDataService.moveFile(id, folderId));
        }

        // View Trash
        @GetMapping("/trash")
        public ResponseEntity<List<FileData>> getTrashFiles() {

                return ResponseEntity.ok(
                                fileDataService.getTrashFiles());
        }

        // Move file to Trash

        // Restore file
        @PutMapping("/restore/{id}")
        public ResponseEntity<String> restoreFile(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                fileDataService.restoreFile(id));
        }

        // Permanently delete file
        @DeleteMapping("/permanent/{id}")
        public ResponseEntity<String> permanentDelete(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                fileDataService.permanentDelete(id));
        }

        // Share file
        @PostMapping("/{id}/share")
        public ResponseEntity<String> shareFile(
                        @PathVariable Long id,
                        @RequestParam String email) {

                return ResponseEntity.ok(
                                fileDataService.shareFile(id, email));
        }

        // Send file directly to an external email address
        @PostMapping("/{id}/send-email")
        public ResponseEntity<String> sendFileByEmail(
                        @PathVariable Long id,
                        @RequestParam String email) {

                return ResponseEntity.ok(
                                fileDataService.sendFileByEmail(id, email));
        }

        @GetMapping("/shared")
        public ResponseEntity<List<FileData>> getSharedFiles(
                        Authentication authentication) {

                String email = authentication.getName();

                return ResponseEntity.ok(
                                fileDataService.getSharedFiles(email));
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

        @PutMapping("/{id}/remove-recent")
        public ResponseEntity<String> removeFromRecent(
                        @PathVariable Long id,
                        Authentication authentication) {

                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String response = fileDataService.removeFromRecent(id, user);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/{id}/add-to-drive")
        public ResponseEntity<String> addToMyDrive(@PathVariable Long id) {

                return ResponseEntity.ok(
                                fileDataService.addToMyDrive(id));

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

                FileData updatedFile = fileDataService.uploadNewVersion(id, file, user);

                return ResponseEntity.ok(updatedFile);
        }

        @GetMapping("/{id}/versions")
        public ResponseEntity<List<FileVersion>> getFileVersions(
                        @PathVariable Long id) {

                List<FileVersion> versions = fileDataService.getFileVersions(id);

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

        @GetMapping("/view/{id}")
        public ResponseEntity<Resource> viewFile(
                        @PathVariable Long id,
                        Authentication authentication) throws IOException {

                FileData fileData = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                String email = authentication.getName();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                boolean isOwner = fileData.getUser().getId().equals(user.getId());

                boolean isShared = fileData.getSharedUsers() != null &&
                                fileData.getSharedUsers().contains(user);

                if (!isOwner && !isShared) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }

                Path path = Paths.get(fileData.getFilePath()).normalize();

                if (!Files.exists(path)) {
                        throw new RuntimeException("Physical file not found: " + path);
                }

                Resource resource = new FileSystemResource(path);

                String contentType = fileData.getFileType();

                if (contentType == null || contentType.isBlank()) {
                        contentType = Files.probeContentType(path);

                        if (contentType == null) {
                                contentType = "application/octet-stream";
                        }
                }

                return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .body(resource);
        }

        @GetMapping("/shared/count")
        public long getUnreadSharedCount(Authentication authentication) {

                User user = userRepository
                                .findByEmail(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return fileDataService.getUnreadSharedCount(user);
        }

        @GetMapping("/mark-viewed/{id}")
        public ResponseEntity<?> markViewed(
                        @PathVariable Long id,
                        Authentication authentication) {

                FileData fileData = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                User user = userRepository.findByEmail(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                if (!fileData.getViewedBy().contains(user)) {
                        fileData.getViewedBy().add(user);
                        fileDataRepository.save(fileData);
                }

                return ResponseEntity.ok("Marked as viewed");
        }

        @PostMapping("/files/{id}/generate-link")
        public String generateLink(@PathVariable Long id) {

                return fileDataService.generateShareLink(id);

        }

        @DeleteMapping("/{id}/remove-from-shared")
        public ResponseEntity<String> removeFromShared(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                fileDataService.removeFromShared(id));
        }

        @PostMapping("/{id}/generate-embedding")
        public ResponseEntity<String> generateEmbedding(
                        @PathVariable Long id) {

                FileData fileData = fileDataRepository
                                .findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                embeddingService.generateEmbedding(fileData);

                return ResponseEntity.ok(
                                "Embedding generated successfully");
        }

        @GetMapping("/semantic-search")
        public ResponseEntity<List<SearchResult>> semanticSearch(
                        @RequestParam String query,
                        Authentication authentication) {

                User user = userRepository
                                .findByEmail(authentication.getName())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<FileData> files = semanticSearchService.search(query, user);

                List<SearchResult> results = files.stream()
                                .map(file -> new SearchResult(
                                                "FILE",
                                                file.getId(),
                                                file.getFileName()))
                                .toList();

                return ResponseEntity.ok(results);
        }

        @PostMapping("/{id}/convert")
        public ResponseEntity<?> convertFile(
                        @PathVariable Long id,
                        @RequestParam String format,
                        Authentication authentication) {

                try {

                        String email = authentication.getName();

                        FileData convertedFile = fileDataService.convertFile(id, format, email);

                        return ResponseEntity.ok(convertedFile);

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of(
                                                        "message",
                                                        e.getMessage() != null
                                                                        ? e.getMessage()
                                                                        : "File conversion failed"));
                }
        }

        @PostMapping("/{id}/compress")
        public ResponseEntity<?> compressFile(
                        @PathVariable Long id,
                        @RequestParam long targetSize) {

                try {

                        Authentication authentication = SecurityContextHolder
                                        .getContext()
                                        .getAuthentication();

                        String email = authentication.getName();

                        FileData compressed = fileDataService.compressFile(
                                        id,
                                        targetSize,
                                        email);

                        Map<String, Object> response = new HashMap<>();

                        response.put(
                                        "message",
                                        "File compressed successfully");

                        response.put(
                                        "file",
                                        compressed);

                        return ResponseEntity.ok(response);

                } catch (Exception e) {

                        Map<String, String> response = new HashMap<>();

                        response.put(
                                        "message",
                                        e.getMessage());

                        return ResponseEntity
                                        .badRequest()
                                        .body(response);
                }
        }

}