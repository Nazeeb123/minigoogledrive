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

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map uploadFile(MultipartFile file) throws IOException {

                String resourceType = "auto";

                if ("application/pdf".equalsIgnoreCase(file.getContentType())) {
                        resourceType = "raw";
                }

        return cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                                                "resource_type", resourceType,
                        "folder", "minigoogledrive"));
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
                HttpURLConnection connection =
                                (HttpURLConnection) URI.create(fileUrl)
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

        return cloudinary.uploader().upload(
                fileBytes,
                ObjectUtils.asMap(
                        "resource_type", "auto",
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