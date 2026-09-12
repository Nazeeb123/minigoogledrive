package com.minidrive.minigoogledrive.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map uploadFile(MultipartFile file) throws IOException {

        return cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "auto",
                        "folder", "minigoogledrive"));
    }

        public String generateSignedUrl(String publicId, String resourceType) {
                if (publicId == null || publicId.isBlank()) {
                        throw new IllegalArgumentException("Cloudinary public ID is missing");
                }

                String type = resourceType == null || resourceType.isBlank()
                                ? "image"
                                : resourceType;

                return cloudinary.url()
                                .secure(true)
                                .resourceType(type)
                                .signed(true)
                                .generate(publicId);
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