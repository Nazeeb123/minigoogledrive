package com.minidrive.minigoogledrive.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CloudinaryService {

        private final Cloudinary cloudinary;

        public CloudinaryService(Cloudinary cloudinary) {
                this.cloudinary = cloudinary;
        }

        public Map uploadFile(MultipartFile file) throws IOException {

                if (file == null || file.isEmpty()) {
                        throw new IllegalArgumentException("File cannot be empty");
                }

                String contentType = file.getContentType();

                String resourceType;

                // Images → image
                if (contentType != null
                                && contentType.toLowerCase().startsWith("image/")) {

                        resourceType = "image";

                        // Videos and audio → video
                } else if (contentType != null
                                && (contentType.toLowerCase().startsWith("video/")
                                                || contentType.toLowerCase().startsWith("audio/"))) {

                        resourceType = "video";

                        // PDF / Office / text / other files → raw
                } else {

                        resourceType = "raw";
                }

                Path tempFile = null;

                try {

                        String originalName = file.getOriginalFilename();

                        String suffix = ".tmp";

                        if (originalName != null) {
                                int dotIndex = originalName.lastIndexOf(".");
                                if (dotIndex >= 0) {
                                        suffix = originalName.substring(dotIndex);
                                }
                        }

                        tempFile = Files.createTempFile(
                                        "minigoogledrive-upload-",
                                        suffix);

                        // Write upload to disk instead of loading the entire file into RAM
                        file.transferTo(tempFile.toFile());

                        Map options = ObjectUtils.asMap(
                                        "resource_type", resourceType,
                                        "folder", "minigoogledrive");

                        // Chunked upload.
                        // 10 MB chunks keep memory usage much lower.
                        return cloudinary.uploader().uploadLarge(
                                        tempFile.toFile(),
                                        options,
                                        20 * 1024 * 1024);

                } finally {

                        if (tempFile != null) {
                                try {
                                        Files.deleteIfExists(tempFile);
                                } catch (IOException ignored) {
                                }
                        }
                }
        }

        public String generateSignedUrl(
                        String publicId,
                        String resourceType,
                        String fileName) {

                if (publicId == null || publicId.isBlank()) {
                        throw new IllegalArgumentException("Cloudinary public ID is missing");
                }

                String type = resourceType == null || resourceType.isBlank()
                                ? "image"
                                : resourceType;

                String format = "";

                if (fileName != null && fileName.lastIndexOf('.') >= 0) {
                        format = fileName.substring(fileName.lastIndexOf('.') + 1);
                }

                return cloudinary.url()
                                .secure(true)
                                .resourceType(type)
                                .signed(true)
                                .format(format)
                                .generate(publicId);
        }

        public byte[] downloadFile(
                        String fileUrl,
                        String publicId,
                        String resourceType,
                        String fileName) throws IOException {

                try {
                        return fetch(fileUrl);
                } catch (IOException originalError) {
                        if (publicId == null || publicId.isBlank()) {
                                throw originalError;
                        }

                        try {
                                String format = "";
                                if (fileName != null && fileName.lastIndexOf('.') >= 0) {
                                        format = fileName.substring(fileName.lastIndexOf('.') + 1);
                                }

                                String resolvedResourceType = resourceType == null || resourceType.isBlank()
                                                ? "image"
                                                : resourceType;

                                try {
                                        String privateDownloadUrl = cloudinary.privateDownload(
                                                        publicId,
                                                        format,
                                                        ObjectUtils.asMap(
                                                                        "resource_type", resolvedResourceType,
                                                                        "type", "upload",
                                                                        "attachment", false));

                                        return fetch(privateDownloadUrl);
                                } catch (Exception uploadDownloadError) {
                                        String authenticatedDownloadUrl = cloudinary.privateDownload(
                                                        publicId,
                                                        format,
                                                        ObjectUtils.asMap(
                                                                        "resource_type", resolvedResourceType,
                                                                        "type", "authenticated",
                                                                        "attachment", false));

                                        try {
                                                return fetch(authenticatedDownloadUrl);
                                        } catch (Exception authenticatedDownloadError) {
                                                String privateDownloadUrl = cloudinary.privateDownload(
                                                                publicId,
                                                                format,
                                                                ObjectUtils.asMap(
                                                                                "resource_type", resolvedResourceType,
                                                                                "type", "private",
                                                                                "attachment", false));

                                                return fetch(privateDownloadUrl);
                                        }
                                }
                        } catch (Exception privateDownloadError) {
                                throw new IOException(
                                                "Cloudinary private download failed: "
                                                                + privateDownloadError.getMessage(),
                                                privateDownloadError);
                        }
                }
        }

        private byte[] fetch(String fileUrl) throws IOException {
                HttpURLConnection connection = (HttpURLConnection) URI.create(fileUrl)
                                .toURL()
                                .openConnection();

                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                connection.setRequestMethod("GET");

                int status = connection.getResponseCode();

                if (status < 200 || status >= 300) {
                        connection.disconnect();
                        throw new IOException(
                                        "Remote storage returned HTTP " + status);
                }

                try (InputStream input = connection.getInputStream()) {
                        return input.readAllBytes();
                } finally {
                        connection.disconnect();
                }
        }

        // Upload byte[] directly
        public Map uploadBytes(
                        byte[] fileBytes,
                        String fileName) throws IOException {

                String resourceType = "auto";

                if (fileName != null) {

                        String name = fileName.toLowerCase();

                        if (name.endsWith(".pdf")) {
                                resourceType = "raw";
                        }
                }

                return cloudinary.uploader().upload(
                                fileBytes,
                                ObjectUtils.asMap(
                                                "resource_type", resourceType,
                                                "folder", "minigoogledrive",
                                                "use_filename", true,
                                                "unique_filename", true));
        }

        public void deleteFile(
                        String publicId,
                        String resourceType) throws IOException {

                cloudinary.uploader().destroy(
                                publicId,
                                ObjectUtils.asMap(
                                                "resource_type",
                                                resourceType));
        }
}