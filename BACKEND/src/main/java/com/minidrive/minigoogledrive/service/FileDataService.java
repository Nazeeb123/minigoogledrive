package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.dto.StorageResponse;
import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.model.FileVersion;
import com.minidrive.minigoogledrive.model.User;
import com.minidrive.minigoogledrive.repository.FileDataRepository;
import com.minidrive.minigoogledrive.repository.FileVersionRepository;
import com.minidrive.minigoogledrive.repository.FolderRepository;
import com.minidrive.minigoogledrive.repository.UserRepository;
import com.minidrive.minigoogledrive.model.Folder;
import com.minidrive.minigoogledrive.model.SharedFile;
import com.minidrive.minigoogledrive.repository.SharedFileRepository;

import com.minidrive.minigoogledrive.model.SearchResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.Map;
import java.util.UUID;

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

        @Autowired
        private FileVersionRepository fileVersionRepository;
        @Autowired
        private NotificationService notificationService;

        private final String uploadDir = "uploads/";

        private User getLoggedInUser() {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null ||
                                !authentication.isAuthenticated() ||
                                "anonymousUser".equals(authentication.getName())) {

                        throw new RuntimeException("User is not authenticated");
                }

                String email = authentication.getName();

                return userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));
        }

        // Upload File
        public FileData uploadFile(MultipartFile file, String email, Long folderId, String fileName) {

                if (file.getSize() > 10 * 1024 * 1024) {
                        throw new RuntimeException("File size exceeds 10 MB");
                }
                String contentType = file.getContentType();

                if (contentType == null ||
                                !(contentType.equals("application/pdf") ||
                                                contentType.equals("image/jpeg") ||
                                                contentType.equals("image/png") ||
                                                contentType.equals("application/msword") ||
                                                contentType.equals(
                                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                                ||
                                                contentType.equals("text/plain"))) {

                        throw new RuntimeException("File type is not allowed");
                }
                try {

                        File directory = new File(uploadDir);

                        if (!directory.exists()) {
                                directory.mkdirs();
                        }

                        Path filePath = Paths.get(
                                        uploadDir,
                                        file.getOriginalFilename());

                        Files.copy(
                                        file.getInputStream(),
                                        filePath,
                                        StandardCopyOption.REPLACE_EXISTING);

                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        Folder folder = null;

                        if (folderId != null) {
                                folder = folderRepository.findById(folderId)
                                                .orElseThrow(() -> new RuntimeException("Folder not found"));
                        }

                        FileData fileData = new FileData();

                        fileData.setFileName(fileName);
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

                var authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                System.out.println("AUTH OBJECT: " + authentication);

                System.out.println(
                                "AUTH NAME: " + authentication.getName());

                System.out.println(
                                "AUTH CLASS: " + authentication.getClass());

                String email = authentication.getName();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

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

                        // Update recent access time
                        fileData.setLastAccessed(LocalDateTime.now());
                        fileDataRepository.save(fileData);

                        return new UrlResource(path.toUri());

                } catch (MalformedURLException e) {

                        throw new RuntimeException("Download failed", e);
                }
        }

        // Delete file (secured)
        public String deleteFile(Long id) {

                FileData fileData = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                User user = getLoggedInUser();

                if (!fileData.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("You cannot delete this file");
                }

                fileData.setDeleted(true);

                fileDataRepository.save(fileData);

                return "File moved to trash successfully";
        }

        // Search files

        public List<FileData> searchFiles(String fileName) {

                String email = SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return fileDataRepository
                                .findByUserAndFileNameContainingIgnoreCase(user, fileName)
                                .stream()
                                .filter(file -> !file.isDeleted())
                                .toList();
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
                        // Old file path
                        Path oldPath = Paths.get(fileData.getFilePath());

                        // Get original file name
                        String oldFileName = fileData.getFileName();

                        // Preserve extension
                        String finalName = newName;

                        int dotIndex = oldFileName.lastIndexOf(".");

                        if (dotIndex != -1) {

                                String extension = oldFileName.substring(dotIndex);

                                finalName = newName + extension;
                        }

                        // New file path
                        Path newPath = oldPath.resolveSibling(finalName);

                        // Rename physical file
                        Files.move(
                                        oldPath,
                                        newPath,
                                        StandardCopyOption.REPLACE_EXISTING);

                        // Update database
                        fileData.setFileName(finalName);
                        fileData.setFilePath(newPath.toString());

                        fileDataRepository.save(fileData);

                        return "File renamed successfully";

                } catch (Exception e) {

                        throw new RuntimeException(
                                        "Rename failed",
                                        e);
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
                return fileDataRepository.findByFolderAndDeletedFalse(folder);
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

        // Share file with another user
        public String shareFile(Long id, String email) {

                // Get file
                FileData fileData = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                // Get logged-in sender
                User sender = getLoggedInUser();

                // Check that sender owns the file
                if (!fileData.getUser().getId().equals(sender.getId())) {
                        throw new RuntimeException("You can only share your own file");
                }

                // Find receiver
                User receiver = userRepository.findByEmail(email.trim())
                                .orElseThrow(() -> new RuntimeException(
                                                "User not found with email: " + email));

                // Don't allow sharing with yourself
                if (receiver.getId().equals(sender.getId())) {
                        throw new RuntimeException("You cannot share a file with yourself");
                }

                // Initialize shared users if necessary
                if (fileData.getSharedUsers() == null) {
                        fileData.setSharedUsers(new ArrayList<>());
                }

                // Don't share twice
                if (fileData.getSharedUsers().contains(receiver)) {
                        throw new RuntimeException("File is already shared with this user");
                }

                // Add receiver
                fileData.getSharedUsers().add(receiver);

                // Mark notification as unseen
                fileData.setSharedSeen(false);

                // Save
                fileDataRepository.save(fileData);

                // Create notification
                notificationService.createNotification(
                                receiver,
                                sender.getUsername()
                                                + " shared "
                                                + fileData.getFileName()
                                                + " with you");

                return "File shared successfully";
        }

        public FileData toggleStar(Long fileId, User user) {

                FileData file = fileDataRepository.findById(fileId)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                if (!file.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("Unauthorized");
                }

                file.setStarred(!file.isStarred());

                return fileDataRepository.save(file);
        }

        public List<FileData> getStarredFiles(User user) {
                return fileDataRepository.findByUserAndStarred(user, true);
        }

        public List<FileData> getRecentFiles(User user) {

                return fileDataRepository
                                .findByUserAndDeletedFalseOrderByLastAccessedDesc(user)
                                .stream()
                                .filter(file -> !file.isHiddenFromRecent())
                                .toList();

        }

        public String generateShareLink(Long fileId, User user) {

                FileData fileData = fileDataRepository.findById(fileId)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                if (!fileData.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("You are not the owner of this file");
                }

                String token = UUID.randomUUID().toString();

                fileData.setShareToken(token);
                fileData.setLinkSharing(true);

                fileDataRepository.save(fileData);

                return "http://localhost:8080/files/shared/" + token;
        }

        public Resource downloadSharedFile(String token) {

                try {

                        FileData fileData = fileDataRepository.findByShareToken(token)
                                        .orElseThrow(() -> new RuntimeException("Invalid share link"));

                        if (!fileData.isLinkSharing()) {
                                throw new RuntimeException("Sharing is disabled");
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

        public FileData disableShareLink(Long fileId, User user) {

                FileData fileData = fileDataRepository.findById(fileId)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                if (!fileData.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("You are not the owner");
                }

                fileData.setLinkSharing(false);
                fileData.setShareToken(null);

                return fileDataRepository.save(fileData);
        }

        public FileData uploadNewVersion(Long fileId, MultipartFile file, User user) {

                try {

                        FileData oldFile = fileDataRepository.findById(fileId)
                                        .orElseThrow(() -> new RuntimeException("File not found"));

                        if (!oldFile.getUser().getId().equals(user.getId())) {
                                throw new RuntimeException("You are not the owner");
                        }

                        // Save old file as version history
                        FileVersion version = new FileVersion();

                        version.setFileName(oldFile.getFileName());
                        version.setFilePath(oldFile.getFilePath());
                        version.setFileSize(oldFile.getFileSize());
                        version.setCreatedAt(LocalDateTime.now());

                        List<FileVersion> versions = fileVersionRepository.findByFileDataId(fileId);

                        version.setVersionNumber(versions.size() + 1);

                        version.setFileData(oldFile);

                        fileVersionRepository.save(version);

                        // Save new uploaded file
                        String path = uploadDir + file.getOriginalFilename();

                        Files.write(
                                        Paths.get(path),
                                        file.getBytes());

                        oldFile.setFilePath(path);
                        oldFile.setFileSize(file.getSize());
                        oldFile.setFileType(file.getContentType());
                        oldFile.setUploadDate(LocalDateTime.now());

                        return fileDataRepository.save(oldFile);

                } catch (IOException e) {

                        throw new RuntimeException("Version upload failed");
                }
        }

        public List<FileVersion> getFileVersions(Long fileId) {

                return fileVersionRepository.findByFileDataId(fileId);

        }

        public FileData restoreVersion(Long versionId, User user) {

                FileVersion version = fileVersionRepository.findById(versionId)
                                .orElseThrow(() -> new RuntimeException("Version not found"));

                FileData fileData = version.getFileData();

                if (!fileData.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("You are not the owner");
                }

                fileData.setFilePath(version.getFilePath());
                fileData.setFileSize(version.getFileSize());
                fileData.setFileName(version.getFileName());

                fileData.setUploadDate(LocalDateTime.now());

                return fileDataRepository.save(fileData);
        }

        // Remove file from Recent
        public String removeFromRecent(Long id, User user) {

                FileData fileData = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                if (!fileData.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("You cannot modify this file");
                }

                fileData.setHiddenFromRecent(true);

                fileDataRepository.save(fileData);

                return "File removed from recent successfully";
        }

        public long getUnreadSharedCount(User user) {

                List<FileData> files = fileDataRepository
                                .findBySharedUsersContains(user);

                return files.stream()
                                .filter(file -> !file.getViewedBy().contains(user))
                                .count();
        }

        public String generateShareLink(Long id) {

                FileData fileData = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                String token = UUID.randomUUID().toString();

                fileData.setShareToken(token);

                fileDataRepository.save(fileData);

                return token;

        }

        public ResponseEntity<Resource> getSharedFile(String token) {

                FileData file = fileDataRepository.findByShareToken(token)
                                .orElseThrow(
                                                () -> new RuntimeException("Invalid link"));

                Path path = Paths.get(file.getFilePath());

                Resource resource = new FileSystemResource(path);

                return ResponseEntity.ok()
                                .contentType(
                                                MediaType.parseMediaType(file.getFileType()))
                                .body(resource);

        }

        public List<SearchResult> globalSearch(String query) {

                String email = SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<SearchResult> results = new ArrayList<>();

                // Search only current user's files
                List<FileData> files = fileDataRepository.findByUserAndFileNameContainingIgnoreCase(
                                user,
                                query);
                System.out.println("Current user: " + user.getEmail());

                System.out.println("Files found: " + files.size());

                for (FileData file : files) {

                        results.add(
                                        new SearchResult(
                                                        "FILE",
                                                        file.getId(),
                                                        file.getFileName()));

                }

                // Search only current user's folders
                List<Folder> folders = folderRepository
                                .findByUserAndFolderNameContainingIgnoreCase(user, query);

                for (Folder folder : folders) {

                        results.add(
                                        new SearchResult(
                                                        "FOLDER",
                                                        folder.getId(),
                                                        folder.getFolderName()));

                }

                return results;
        }

        public StorageResponse getStorage() {

                String email = SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<FileData> files = fileDataRepository.findByUser(user);

                long used = files.stream()
                                .filter(file -> !file.isDeleted())
                                .mapToLong(FileData::getFileSize)
                                .sum();

                long limit = 1024L * 1024 * 1024; // 1GB

                return new StorageResponse(
                                used,
                                limit);

        }

        public Map<String, Object> getStorageUsage(User user) {

                List<FileData> files = fileDataRepository.findByUserAndDeletedFalse(user);

                long usedStorage = 0;

                for (FileData file : files) {
                        usedStorage += file.getFileSize();
                }

                long storageLimit = 1024L * 1024 * 1024; // 1 GB

                Map<String, Object> result = new HashMap<>();

                result.put("used", usedStorage);
                result.put("limit", storageLimit);

                return result;
        }

        public String addToMyDrive(Long id) {

                // Logged-in user
                String email = SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Shared file
                FileData original = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                // Create copy
                FileData copy = new FileData();

                copy.setFileName(original.getFileName());
                copy.setFilePath(original.getFilePath());
                copy.setFileType(original.getFileType());
                copy.setFileSize(original.getFileSize());

                copy.setUploadDate(LocalDateTime.now());

                copy.setDeleted(false);
                copy.setStarred(false);
                copy.setHiddenFromRecent(false);

                copy.setUser(user);

                fileDataRepository.save(copy);

                return "Added to My Drive";

        }

        public FileData getFileForAI(Long fileId) {

                String email = SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                FileData fileData = fileDataRepository.findById(fileId)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                // Only allow the owner for now
                if (!fileData.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException(
                                        "You cannot use this file with AI");
                }

                if (fileData.isDeleted()) {
                        throw new RuntimeException(
                                        "File is in Trash");
                }

                return fileData;
        }

}