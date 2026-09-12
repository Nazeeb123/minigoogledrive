package com.minidrive.minigoogledrive.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final CloudinaryService cloudinaryService;

    public EmailService(
            JavaMailSender mailSender,
            CloudinaryService cloudinaryService) {
        this.mailSender = mailSender;
        this.cloudinaryService = cloudinaryService;
    }

    public void sendFile(
            String recipientEmail,
            String fileUrl,
            String publicId,
            String resourceType,
            String fileName) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom("demo@gmail.com");

        helper.setTo(recipientEmail);

        helper.setSubject(
                "File shared with you - Mini Google Drive");

        helper.setText(
                "Hello,\n\n" +
                        "A file has been shared with you through Mini Google Drive.\n\n" +
                        "File: " + fileName + "\n\n" +
                        "Regards,\n" +
                        "Mini Google Drive");

        try {

                byte[] fileBytes = cloudinaryService.downloadFile(
                    fileUrl,
                    publicId,
                    resourceType,
                    fileName);

            helper.addAttachment(
                    fileName,
                    new org.springframework.core.io.ByteArrayResource(fileBytes));

            mailSender.send(message);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to attach Cloudinary file",
                    e);
        }
    }
}