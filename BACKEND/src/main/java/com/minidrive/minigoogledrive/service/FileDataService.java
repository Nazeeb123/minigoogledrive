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
import org.springframework.transaction.annotation.Transactional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.FileOutputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

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
        @Autowired
        private CloudinaryService cloudinaryService;

        @Autowired
        private EmbeddingService embeddingService;

        @Autowired
        private EmailService emailService;

        private final String uploadDir = System.getenv().getOrDefault("UPLOAD_DIR", "/app/uploads/");

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
        // Upload File
        public FileData uploadFile(
                        MultipartFile file,
                        String email,
                        Long folderId,
                        String fileName) {

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

                        // =========================
                        // UPLOAD TO CLOUDINARY
                        // =========================

                        Map uploadResult = cloudinaryService.uploadFile(file);

                        String secureUrl = (String) uploadResult.get("secure_url");

                        String publicId = (String) uploadResult.get("public_id");

                        String resourceType = (String) uploadResult.get("resource_type");

                        if (secureUrl == null || secureUrl.isBlank()) {
                                throw new RuntimeException(
                                                "Cloudinary did not return a file URL");
                        }

                        // =========================
                        // GET USER
                        // =========================

                        User user = userRepository
                                        .findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        // =========================
                        // GET FOLDER
                        // =========================

                        Folder folder = null;

                        if (folderId != null) {

                                folder = folderRepository
                                                .findById(folderId)
                                                .orElseThrow(() -> new RuntimeException("Folder not found"));

                                if (!folder.getUser().getId().equals(user.getId())) {
                                        throw new RuntimeException(
                                                        "You cannot upload to this folder");
                                }
                        }

                        // =========================
                        // ORIGINAL FILE NAME
                        // =========================

                        String originalFileName = file.getOriginalFilename();

                        if (originalFileName == null ||
                                        originalFileName.trim().isEmpty()) {

                                originalFileName = "uploaded-file";
                        }

                        // =========================
                        // CREATE DATABASE RECORD
                        // =========================

                        FileData fileData = new FileData();

                        fileData.setFileName(
                                        fileName != null &&
                                                        !fileName.trim().isEmpty()
                                                                        ? fileName
                                                                        : originalFileName);

                        // IMPORTANT:
                        // Store Cloudinary URL instead of local path
                        fileData.setFilePath(secureUrl);

                        fileData.setCloudinaryPublicId(publicId);

                        fileData.setCloudinaryResourceType(resourceType);

                        fileData.setFileType(contentType);

                        fileData.setFileSize(file.getSize());

                        fileData.setUploadDate(LocalDateTime.now());

                        fileData.setUser(user);

                        fileData.setFolder(folder);

                        fileData.setDeleted(false);

                        fileData.setStarred(false);

                        fileData.setHiddenFromRecent(false);

                        FileData savedFile = fileDataRepository.save(fileData);

                        // =========================
                        // EMBEDDING
                        // =========================

                        embeddingService.generateEmbedding(savedFile);

                        return savedFile;

                } catch (IOException e) {

                        throw new RuntimeException(
                                        "Cloudinary upload failed",
                                        e);
                }
        }

        // Get all files
        public List<FileData> getAllFiles() {
                return fileDataRepository.findAll();
        }

        // Download file (secured)
        public Resource downloadFile(Long id, String email) {

                try {

                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        FileData fileData = fileDataRepository.findById(id)
                                        .orElseThrow(() -> new RuntimeException("File not found"));

                        boolean isOwner = fileData.getUser() != null
                                        && fileData.getUser().getId().equals(user.getId());

                        boolean isShared = fileData.getSharedUsers() != null
                                        && fileData.getSharedUsers().contains(user);

                        if (!isOwner && !isShared) {
                                throw new RuntimeException(
                                                "You cannot access this file");
                        }

                        if (fileData.getFilePath() == null ||
                                        fileData.getFilePath().isBlank()) {

                                throw new RuntimeException(
                                                "File URL is empty");
                        }

                        fileData.setLastAccessed(LocalDateTime.now());
                        fileDataRepository.save(fileData);

                        return new UrlResource(fileData.getFilePath());

                } catch (MalformedURLException e) {

                        throw new RuntimeException(
                                        "Invalid Cloudinary URL",
                                        e);
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

                String email = SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                FileData fileData = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                if (!fileData.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException(
                                        "You cannot rename this file");
                }

                if (newName == null ||
                                newName.trim().isEmpty()) {

                        throw new RuntimeException(
                                        "File name cannot be empty");
                }

                String oldFileName = fileData.getFileName();

                int dotIndex = oldFileName.lastIndexOf(".");

                String finalName;

                if (dotIndex != -1) {

                        String extension = oldFileName.substring(dotIndex);

                        finalName = newName.trim() + extension;

                } else {

                        finalName = newName.trim();
                }

                fileData.setFileName(finalName);

                fileDataRepository.save(fileData);

                return "File renamed successfully";
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

                return fileDataRepository.findBySharedUsersAndDeletedFalse(user);
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

                User user = getLoggedInUser();

                FileData fileData = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                if (!fileData.getUser().getId().equals(user.getId())) {

                        throw new RuntimeException(
                                        "You cannot permanently delete this file");
                }

                String publicId = fileData.getCloudinaryPublicId();

                String resourceType = fileData.getCloudinaryResourceType();

                try {

                        if (publicId != null &&
                                        !publicId.isBlank()) {

                                cloudinaryService.deleteFile(
                                                publicId,
                                                resourceType != null
                                                                ? resourceType
                                                                : "image");
                        }

                } catch (Exception e) {

                        throw new RuntimeException(
                                        "Failed to delete file from Cloudinary",
                                        e);
                }

                fileDataRepository.delete(fileData);

                return "File permanently deleted";
        }

        // Share file with another user
        // Send file directly to an external email address
        public String sendFileByEmail(
                        Long fileId,
                        String recipientEmail) {

                // Get logged-in user
                User sender = getLoggedInUser();

                // Find file
                FileData fileData = fileDataRepository.findById(fileId)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                // Only owner can send the file
                if (!fileData.getUser().getId().equals(sender.getId())) {

                        throw new RuntimeException(
                                        "You can only send your own file by email");
                }

                // Don't allow files from Trash
                if (fileData.isDeleted()) {

                        throw new RuntimeException(
                                        "You cannot send a file that is in Trash");
                }

                // Validate recipient email
                if (recipientEmail == null ||
                                recipientEmail.trim().isEmpty()) {

                        throw new RuntimeException(
                                        "Recipient email is required");
                }

                String email = recipientEmail.trim();

                // Validate Cloudinary URL
                String fileUrl = fileData.getFilePath();

                if (fileUrl == null ||
                                fileUrl.isBlank()) {

                        throw new RuntimeException(
                                        "File URL is empty");
                }

                try {

                        emailService.sendFile(
                                        email,
                                        fileUrl,
                                        fileData.getFileName());

                        return "File sent successfully to " + email;

                } catch (Exception e) {

                        e.printStackTrace();

                        throw new RuntimeException(
                                        "Failed to send file by email: "
                                                        + e.getMessage(),
                                        e);
                }
        }

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

                return "https://minigoogledrive-1.onrender.com/files/shared/" + token;
        }

        public Resource downloadSharedFile(String token) {

                try {

                        FileData fileData = fileDataRepository
                                        .findByShareToken(token)
                                        .orElseThrow(() -> new RuntimeException("Invalid share link"));

                        if (!fileData.isLinkSharing()) {
                                throw new RuntimeException(
                                                "Sharing is disabled");
                        }

                        if (fileData.getFilePath() == null ||
                                        fileData.getFilePath().isBlank()) {

                                throw new RuntimeException(
                                                "File URL is empty");
                        }

                        return new UrlResource(fileData.getFilePath());

                } catch (MalformedURLException e) {

                        throw new RuntimeException(
                                        "Invalid Cloudinary URL",
                                        e);
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

        public FileData uploadNewVersion(
                        Long fileId,
                        MultipartFile file,
                        User user) {

                try {

                        FileData oldFile = fileDataRepository.findById(fileId)
                                        .orElseThrow(() -> new RuntimeException("File not found"));

                        // Security
                        if (!oldFile.getUser().getId().equals(user.getId())) {
                                throw new RuntimeException(
                                                "You are not the owner");
                        }

                        if (file == null || file.isEmpty()) {
                                throw new RuntimeException(
                                                "Uploaded file is empty");
                        }

                        // Save old file as version history
                        FileVersion version = new FileVersion();

                        version.setFileName(oldFile.getFileName());
                        version.setFilePath(oldFile.getFilePath());
                        version.setFileSize(oldFile.getFileSize());
                        version.setCreatedAt(LocalDateTime.now());

                        List<FileVersion> versions = fileVersionRepository.findByFileDataId(fileId);

                        version.setVersionNumber(
                                        versions.size() + 1);

                        version.setFileData(oldFile);

                        fileVersionRepository.save(version);

                        // Upload new version to Cloudinary
                        Map uploadResult = cloudinaryService.uploadFile(file);

                        String secureUrl = (String) uploadResult.get("secure_url");

                        String publicId = (String) uploadResult.get("public_id");

                        String resourceType = (String) uploadResult.get("resource_type");

                        if (secureUrl == null ||
                                        secureUrl.isBlank()) {

                                throw new RuntimeException(
                                                "Cloudinary did not return a file URL");
                        }

                        // Update current file
                        oldFile.setFilePath(secureUrl);
                        oldFile.setCloudinaryPublicId(publicId);
                        oldFile.setCloudinaryResourceType(resourceType);
                        oldFile.setFileSize(file.getSize());
                        oldFile.setFileType(file.getContentType());
                        oldFile.setUploadDate(LocalDateTime.now());

                        return fileDataRepository.save(oldFile);

                } catch (IOException e) {

                        throw new RuntimeException(
                                        "Version upload failed",
                                        e);
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

                try {

                        FileData file = fileDataRepository
                                        .findByShareToken(token)
                                        .orElseThrow(() -> new RuntimeException("Invalid link"));

                        if (file.getFilePath() == null ||
                                        file.getFilePath().isBlank()) {

                                throw new RuntimeException(
                                                "File URL is empty");
                        }

                        Resource resource = new UrlResource(file.getFilePath());

                        return ResponseEntity.ok()
                                        .contentType(
                                                        MediaType.parseMediaType(
                                                                        file.getFileType()))
                                        .body(resource);

                } catch (MalformedURLException e) {

                        throw new RuntimeException(
                                        "Invalid Cloudinary URL",
                                        e);
                }
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

                // Original shared file
                FileData original = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                // Check if this exact shared file is already in My Drive
                boolean alreadyAdded = fileDataRepository
                                .existsByOriginalFileIdAndUser(id, user);

                if (alreadyAdded) {
                        return "Already in My Drive";
                }

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

                // Remember the original shared file
                copy.setOriginalFileId(original.getId());

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

        public String getFileLocation(Long fileId) {

                String email = SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getName();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                FileData file = fileDataRepository.findById(fileId)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                // Security check
                if (!file.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("You cannot access this file");
                }

                Folder folder = file.getFolder();

                // File is directly in My Drive
                if (folder == null) {
                        return "My Drive";
                }

                return "My Drive / " + folder.getFolderName();
        }

        @Transactional
        public String removeFromShared(Long id) {

                FileData original = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                User user = getLoggedInUser();

                if (!original.getSharedUsers().contains(user)) {
                        throw new RuntimeException("This file is not shared with you");
                }

                // Create a personal Trash record
                FileData trashFile = new FileData();

                trashFile.setFileName(original.getFileName());
                trashFile.setFilePath(original.getFilePath());
                trashFile.setFileType(original.getFileType());
                trashFile.setFileSize(original.getFileSize());

                trashFile.setUploadDate(LocalDateTime.now());

                trashFile.setDeleted(true);
                trashFile.setStarred(false);
                trashFile.setHiddenFromRecent(false);

                // This Trash record belongs to the logged-in user
                trashFile.setUser(user);

                // Remember the original shared file
                trashFile.setOriginalFileId(original.getId());

                fileDataRepository.save(trashFile);

                // Remove the user from Shared With Me
                original.getSharedUsers().removeIf(
                                sharedUser -> sharedUser.getId().equals(user.getId()));

                fileDataRepository.save(original);

                return "File moved to Trash";
        }

        public FileData convertFile(
                        Long fileId,
                        String format,
                        String email) {

                Path inputPath = null;
                Path outputPath = null;

                try {

                        // =========================
                        // GET USER
                        // =========================

                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        // =========================
                        // GET ORIGINAL FILE
                        // =========================

                        FileData originalFile = fileDataRepository.findById(fileId)
                                        .orElseThrow(() -> new RuntimeException("File not found"));

                        // =========================
                        // SECURITY
                        // =========================

                        if (!originalFile.getUser().getId().equals(user.getId())) {

                                throw new RuntimeException(
                                                "You can only convert your own files");
                        }

                        if (originalFile.isDeleted()) {

                                throw new RuntimeException(
                                                "You cannot convert a file in Trash");
                        }

                        // =========================
                        // VALIDATE URL
                        // =========================

                        String fileUrl = originalFile.getFilePath();

                        if (fileUrl == null ||
                                        fileUrl.isBlank()) {

                                throw new RuntimeException(
                                                "File URL is empty");
                        }

                        // =========================
                        // TEMP DIRECTORY
                        // =========================

                        Path tempDirectory = Files.createTempDirectory("minidrive-convert-");

                        // =========================
                        // DOWNLOAD CLOUDINARY FILE
                        // =========================

                        try (var inputStream = new UrlResource(fileUrl)
                                        .getInputStream()) {

                                inputPath = tempDirectory.resolve(
                                                originalFile.getFileName());

                                Files.copy(
                                                inputStream,
                                                inputPath,
                                                StandardCopyOption.REPLACE_EXISTING);
                        }

                        // =========================
                        // FILE INFORMATION
                        // =========================

                        String originalName = originalFile.getFileName();

                        String extension = getExtension(originalName);

                        int dotIndex = originalName.lastIndexOf(".");

                        String baseName = dotIndex > 0
                                        ? originalName.substring(0, dotIndex)
                                        : originalName;

                        format = format.toLowerCase().trim();

                        // =========================
                        // IMAGE -> PDF
                        // =========================

                        if (format.equals("pdf")) {

                                if (!(extension.equals("jpg") ||
                                                extension.equals("jpeg") ||
                                                extension.equals("png") ||
                                                extension.equals("webp"))) {

                                        throw new RuntimeException(
                                                        "Only JPG, JPEG, PNG and WEBP files can be converted to PDF");
                                }

                                BufferedImage image = ImageIO.read(inputPath.toFile());

                                if (image == null) {

                                        throw new RuntimeException(
                                                        "Unable to read image");
                                }

                                outputPath = tempDirectory.resolve(
                                                baseName + "_converted.pdf");

                                createPdfFromImage(
                                                image,
                                                outputPath);
                        }

                        // =========================
                        // PDF -> JPG / PNG
                        // =========================

                        else if (format.equals("jpg") ||
                                        format.equals("png")) {

                                if (!extension.equals("pdf")) {

                                        throw new RuntimeException(
                                                        "Only PDF files can be converted to images");
                                }

                                outputPath = tempDirectory.resolve(
                                                baseName +
                                                                "_converted." +
                                                                format);

                                try (PDDocument document = Loader.loadPDF(inputPath.toFile())) {

                                        PDFRenderer renderer = new PDFRenderer(document);

                                        BufferedImage image = renderer.renderImageWithDPI(
                                                        0,
                                                        150);

                                        boolean success = ImageIO.write(
                                                        image,
                                                        format,
                                                        outputPath.toFile());

                                        if (!success) {

                                                throw new RuntimeException(
                                                                "Unable to create image");
                                        }
                                }
                        }

                        // =========================
                        // IMAGE -> DOCX
                        // =========================

                        else if (format.equals("docx")) {

                                if (!(extension.equals("jpg") ||
                                                extension.equals("jpeg") ||
                                                extension.equals("png"))) {

                                        throw new RuntimeException(
                                                        "Only JPG, JPEG and PNG files can be converted to DOCX");
                                }

                                outputPath = tempDirectory.resolve(
                                                baseName + "_converted.docx");

                                createDocxFromImage(
                                                inputPath,
                                                outputPath,
                                                extension);
                        }

                        else {

                                throw new RuntimeException(
                                                "Unsupported conversion format: " +
                                                                format);
                        }

                        // =========================
                        // CHECK OUTPUT
                        // =========================

                        if (!Files.exists(outputPath)) {

                                throw new RuntimeException(
                                                "Converted file was not created");
                        }

                        // =========================
                        // UPLOAD RESULT TO CLOUDINARY
                        // =========================

                        Map uploadResult = cloudinaryService.uploadBytes(
                                        Files.readAllBytes(outputPath),
                                        outputPath.getFileName().toString());
                        String secureUrl = (String) uploadResult.get("secure_url");

                        String publicId = (String) uploadResult.get("public_id");

                        String resourceType = (String) uploadResult.get("resource_type");

                        if (secureUrl == null ||
                                        secureUrl.isBlank()) {

                                throw new RuntimeException(
                                                "Cloudinary did not return converted file URL");
                        }

                        // =========================
                        // CREATE DATABASE RECORD
                        // =========================

                        FileData convertedFile = new FileData();

                        convertedFile.setFileName(
                                        outputPath.getFileName().toString());

                        convertedFile.setFilePath(
                                        secureUrl);

                        convertedFile.setCloudinaryPublicId(
                                        publicId);

                        convertedFile.setCloudinaryResourceType(
                                        resourceType);

                        convertedFile.setFileSize(
                                        Files.size(outputPath));

                        convertedFile.setUploadDate(
                                        LocalDateTime.now());

                        convertedFile.setUser(user);

                        convertedFile.setDeleted(false);

                        convertedFile.setStarred(false);

                        convertedFile.setHiddenFromRecent(false);

                        convertedFile.setFileType(
                                        getContentTypeForFormat(format));

                        return fileDataRepository.save(
                                        convertedFile);

                } catch (Exception e) {

                        e.printStackTrace();

                        throw new RuntimeException(
                                        "File conversion failed: " +
                                                        e.getMessage(),
                                        e);

                } finally {

                        // =========================
                        // DELETE TEMP FILES
                        // =========================

                        try {

                                if (inputPath != null) {
                                        Files.deleteIfExists(inputPath);
                                }

                                if (outputPath != null) {
                                        Files.deleteIfExists(outputPath);
                                }

                                if (inputPath != null &&
                                                inputPath.getParent() != null) {

                                        Files.deleteIfExists(
                                                        inputPath.getParent());
                                }

                        } catch (Exception ignored) {
                        }
                }
        }

        private String getExtension(String fileName) {

                int dotIndex = fileName.lastIndexOf(".");

                if (dotIndex == -1) {
                        return "";
                }

                return fileName
                                .substring(dotIndex + 1)
                                .toLowerCase();
        }

        private void createPdfFromImage(
                        BufferedImage image,
                        Path outputPath) throws IOException {

                int width = image.getWidth();
                int height = image.getHeight();

                float pageWidth = 595;
                float pageHeight = (float) height / width * pageWidth;

                try (PDDocument document = new PDDocument()) {

                        org.apache.pdfbox.pdmodel.common.PDRectangle pageSize = new org.apache.pdfbox.pdmodel.common.PDRectangle(
                                        pageWidth,
                                        pageHeight);

                        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(
                                        pageSize);

                        document.addPage(page);

                        Path temporaryImage = Paths.get(
                                        outputPath.toString()
                                                        + ".png");

                        ImageIO.write(
                                        image,
                                        "png",
                                        temporaryImage.toFile());

                        org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage = org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
                                        .createFromFile(
                                                        temporaryImage.toString(),
                                                        document);

                        try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(
                                        document,
                                        page)) {

                                contentStream.drawImage(
                                                pdImage,
                                                0,
                                                0,
                                                pageWidth,
                                                pageHeight);
                        }

                        document.save(outputPath.toFile());

                        Files.deleteIfExists(temporaryImage);
                }
        }

        private void createDocxFromImage(
                        Path imagePath,
                        Path outputPath,
                        String extension) throws Exception {

                BufferedImage image = ImageIO.read(imagePath.toFile());

                if (image == null) {
                        throw new RuntimeException(
                                        "Unable to read image");
                }

                String imageType;

                if (extension.equals("jpg") ||
                                extension.equals("jpeg")) {

                        imageType = "jpg";

                } else if (extension.equals("png")) {

                        imageType = "png";

                } else {

                        // WEBP may not be supported by ImageIO
                        // depending on installed plugins.
                        throw new RuntimeException(
                                        "WEBP to DOCX is not supported. Use JPG or PNG.");
                }

                int pictureType;

                if (imageType.equals("jpg")) {

                        pictureType = org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_JPEG;

                } else {

                        pictureType = org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG;
                }

                int width = image.getWidth();
                int height = image.getHeight();

                int maxWidth = 600;

                int finalWidth = maxWidth;

                int finalHeight = (int) ((double) height /
                                width *
                                maxWidth);

                try (XWPFDocument document = new XWPFDocument()) {

                        XWPFParagraph paragraph = document.createParagraph();

                        XWPFRun run = paragraph.createRun();

                        try (var inputStream = Files.newInputStream(imagePath)) {

                                run.addPicture(
                                                inputStream,
                                                pictureType,
                                                imagePath.getFileName().toString(),
                                                Units.toEMU(finalWidth),
                                                Units.toEMU(finalHeight));
                        }

                        try (FileOutputStream outputStream = new FileOutputStream(
                                        outputPath.toFile())) {

                                document.write(outputStream);
                        }
                }
        }

        public FileData compressFile(
                        Long fileId,
                        long targetSize,
                        String email) {

                Path inputPath = null;
                Path outputPath = null;

                try {

                        // =========================
                        // GET USER
                        // =========================

                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "User not found"));

                        // =========================
                        // GET FILE
                        // =========================

                        FileData originalFile = fileDataRepository.findById(fileId)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "File not found"));

                        // =========================
                        // SECURITY
                        // =========================

                        if (!originalFile.getUser()
                                        .getId()
                                        .equals(user.getId())) {

                                throw new RuntimeException(
                                                "You can only compress your own files");
                        }

                        if (originalFile.isDeleted()) {

                                throw new RuntimeException(
                                                "You cannot compress a file in Trash");
                        }

                        // =========================
                        // CLOUDINARY URL
                        // =========================

                        String fileUrl = originalFile.getFilePath();

                        if (fileUrl == null ||
                                        fileUrl.isBlank()) {

                                throw new RuntimeException(
                                                "File URL is empty");
                        }

                        // =========================
                        // TEMP DIRECTORY
                        // =========================

                        Path tempDirectory = Files.createTempDirectory(
                                        "minidrive-compress-");

                        // =========================
                        // DOWNLOAD FROM CLOUDINARY
                        // =========================

                        try (var inputStream = new UrlResource(fileUrl)
                                        .getInputStream()) {

                                inputPath = tempDirectory.resolve(
                                                originalFile.getFileName());

                                Files.copy(
                                                inputStream,
                                                inputPath,
                                                StandardCopyOption.REPLACE_EXISTING);
                        }

                        long originalSize = Files.size(inputPath);

                        if (targetSize >= originalSize) {

                                throw new RuntimeException(
                                                "Target size must be smaller than the original file");
                        }

                        // =========================
                        // EXTENSION
                        // =========================

                        String extension = getExtension(
                                        originalFile.getFileName());

                        if (!(extension.equals("jpg") ||
                                        extension.equals("jpeg") ||
                                        extension.equals("png") ||
                                        extension.equals("webp"))) {

                                throw new RuntimeException(
                                                "Currently only JPG, JPEG, PNG and WEBP files can be compressed");
                        }

                        // =========================
                        // READ IMAGE
                        // =========================

                        BufferedImage image = ImageIO.read(inputPath.toFile());

                        if (image == null) {

                                throw new RuntimeException(
                                                "Unable to read image");
                        }

                        String originalName = originalFile.getFileName();

                        int dotIndex = originalName.lastIndexOf(".");

                        String baseName = dotIndex > 0
                                        ? originalName.substring(
                                                        0,
                                                        dotIndex)
                                        : originalName;

                        outputPath = tempDirectory.resolve(
                                        baseName +
                                                        "_compressed." +
                                                        extension);

                        // =========================
                        // COMPRESS
                        // =========================

                        compressImageToTarget(
                                        image,
                                        outputPath,
                                        extension,
                                        targetSize);

                        if (!Files.exists(outputPath)) {

                                throw new RuntimeException(
                                                "Compressed file was not created");
                        }

                        // =========================
                        // UPLOAD TO CLOUDINARY
                        // =========================

                        Map uploadResult = cloudinaryService.uploadBytes(
                                        Files.readAllBytes(outputPath),
                                        outputPath.getFileName().toString());

                        String secureUrl = (String) uploadResult.get("secure_url");

                        String publicId = (String) uploadResult.get("public_id");

                        String resourceType = (String) uploadResult.get("resource_type");

                        if (secureUrl == null ||
                                        secureUrl.isBlank()) {

                                throw new RuntimeException(
                                                "Cloudinary did not return compressed file URL");
                        }

                        // =========================
                        // DATABASE RECORD
                        // =========================

                        FileData compressedFile = new FileData();

                        compressedFile.setFileName(
                                        outputPath.getFileName().toString());

                        compressedFile.setFilePath(
                                        secureUrl);

                        compressedFile.setCloudinaryPublicId(
                                        publicId);

                        compressedFile.setCloudinaryResourceType(
                                        resourceType);

                        compressedFile.setFileSize(
                                        Files.size(outputPath));

                        compressedFile.setUploadDate(
                                        LocalDateTime.now());

                        compressedFile.setUser(user);

                        compressedFile.setDeleted(false);

                        compressedFile.setStarred(false);

                        compressedFile.setHiddenFromRecent(false);

                        compressedFile.setFileType(
                                        getContentTypeForFormat(extension));

                        return fileDataRepository.save(
                                        compressedFile);

                } catch (Exception e) {

                        e.printStackTrace();

                        throw new RuntimeException(
                                        "File compression failed: " +
                                                        e.getMessage(),
                                        e);

                } finally {

                        // =========================
                        // DELETE TEMP FILES
                        // =========================

                        try {

                                if (inputPath != null) {
                                        Files.deleteIfExists(inputPath);
                                }

                                if (outputPath != null) {
                                        Files.deleteIfExists(outputPath);
                                }

                                if (inputPath != null &&
                                                inputPath.getParent() != null) {

                                        Files.deleteIfExists(
                                                        inputPath.getParent());
                                }

                        } catch (Exception ignored) {
                        }
                }
        }

        private String getContentTypeForFormat(String format) {

                switch (format.toLowerCase()) {

                        case "pdf":
                                return "application/pdf";

                        case "docx":
                                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

                        case "jpg":
                        case "jpeg":
                                return "image/jpeg";

                        case "png":
                                return "image/png";

                        case "webp":
                                return "image/webp";

                        default:
                                return "application/octet-stream";
                }
        }

        private void writeJpeg(
                        BufferedImage image,
                        Path outputPath,
                        float quality) throws IOException {

                javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg")
                                .next();

                javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();

                param.setCompressionMode(
                                javax.imageio.ImageWriteParam.MODE_EXPLICIT);

                param.setCompressionQuality(
                                quality);

                try (
                                FileOutputStream output = new FileOutputStream(
                                                outputPath.toFile());

                                javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {

                        writer.setOutput(ios);

                        writer.write(
                                        null,
                                        new javax.imageio.IIOImage(
                                                        image,
                                                        null,
                                                        null),
                                        param);
                }

                writer.dispose();
        }

        private Path resolveFilePath(String storedPath) {

                if (storedPath == null || storedPath.isBlank()) {
                        throw new RuntimeException("File path is empty");
                }

                storedPath = storedPath.replace("\\", "/");

                Path path = Paths.get(storedPath);

                if (!path.isAbsolute()) {
                        path = Paths.get(uploadDir)
                                        .resolve(path.getFileName());
                }

                return path.normalize();
        }

        public Resource viewFile(Long id, String email) {

                FileData fileData = fileDataRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("File not found"));

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                boolean isOwner = fileData.getUser() != null
                                && fileData.getUser().getId().equals(user.getId());

                boolean isShared = fileData.getSharedUsers() != null
                                && fileData.getSharedUsers().contains(user);

                if (!isOwner && !isShared) {
                        throw new RuntimeException(
                                        "You are not allowed to view this file");
                }

                String filePath = fileData.getFilePath();

                if (filePath == null || filePath.isBlank()) {
                        throw new RuntimeException("File URL is empty");
                }

                System.out.println("========== VIEW FILE ==========");
                System.out.println("ID: " + id);
                System.out.println("NAME: " + fileData.getFileName());
                System.out.println("PATH: " + filePath);
                System.out.println("===============================");

                fileData.setLastAccessed(LocalDateTime.now());
                fileDataRepository.save(fileData);

                try {

                        // ================================
                        // CLOUDINARY FILE
                        // ================================
                        if (filePath.startsWith("http://")
                                        || filePath.startsWith("https://")) {

                                System.out.println("OPENING CLOUDINARY URL");

                                return new UrlResource(filePath);
                        }

                        // ================================
                        // OLD LOCAL FILE
                        // ================================
                        Path path = Paths.get(filePath);

                        if (!Files.exists(path)) {
                                throw new RuntimeException(
                                                "Local file does not exist: " + filePath);
                        }

                        System.out.println("OPENING LOCAL FILE");

                        return new FileSystemResource(path);

                } catch (Exception e) {

                        e.printStackTrace();

                        throw new RuntimeException(
                                        "Could not open file: " + fileData.getFileName(),
                                        e);
                }
        }

        private void compressImageToTarget(
                        BufferedImage image,
                        Path outputPath,
                        String extension,
                        long targetSize) throws IOException {

                extension = extension.toLowerCase();

                // =========================
                // PNG
                // =========================

                if (extension.equals("png")) {

                        BufferedImage current = image;

                        for (int i = 0; i < 8; i++) {

                                ImageIO.write(
                                                current,
                                                "png",
                                                outputPath.toFile());

                                if (Files.size(outputPath) <= targetSize) {
                                        return;
                                }

                                int newWidth = Math.max(
                                                1,
                                                (int) (current.getWidth() * 0.8));

                                int newHeight = Math.max(
                                                1,
                                                (int) (current.getHeight() * 0.8));

                                BufferedImage resized = new BufferedImage(
                                                newWidth,
                                                newHeight,
                                                BufferedImage.TYPE_INT_RGB);

                                var graphics = resized.createGraphics();

                                graphics.drawImage(
                                                current,
                                                0,
                                                0,
                                                newWidth,
                                                newHeight,
                                                null);

                                graphics.dispose();

                                current = resized;
                        }

                        throw new RuntimeException(
                                        "Unable to compress PNG to requested size");
                }

                // =========================
                // JPEG / JPG
                // =========================

                if (extension.equals("jpg") ||
                                extension.equals("jpeg")) {

                        float quality = 0.90f;

                        for (int i = 0; i < 15; i++) {

                                writeJpeg(
                                                image,
                                                outputPath,
                                                quality);

                                if (Files.size(outputPath) <= targetSize) {
                                        return;
                                }

                                quality -= 0.05f;

                                if (quality < 0.10f) {
                                        break;
                                }
                        }

                        // Resize if quality alone is not enough
                        BufferedImage current = image;

                        for (int i = 0; i < 8; i++) {

                                int newWidth = Math.max(
                                                1,
                                                (int) (current.getWidth() * 0.8));

                                int newHeight = Math.max(
                                                1,
                                                (int) (current.getHeight() * 0.8));

                                BufferedImage resized = new BufferedImage(
                                                newWidth,
                                                newHeight,
                                                BufferedImage.TYPE_INT_RGB);

                                var graphics = resized.createGraphics();

                                graphics.drawImage(
                                                current,
                                                0,
                                                0,
                                                newWidth,
                                                newHeight,
                                                null);

                                graphics.dispose();

                                current = resized;

                                writeJpeg(
                                                current,
                                                outputPath,
                                                0.60f);

                                if (Files.size(outputPath) <= targetSize) {
                                        return;
                                }
                        }

                        throw new RuntimeException(
                                        "Unable to compress image to requested size");
                }

                // =========================
                // WEBP
                // =========================

                if (extension.equals("webp")) {

                        throw new RuntimeException(
                                        "WEBP compression requires a WebP ImageIO plugin. " +
                                                        "Please convert the WEBP to JPG or PNG first.");
                }

                throw new RuntimeException(
                                "Unsupported image format: " + extension);
        }
}