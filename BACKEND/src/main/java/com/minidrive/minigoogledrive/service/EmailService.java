package com.minidrive.minigoogledrive.service;

import com.resend.Resend;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

import jakarta.mail.MessagingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class EmailService {

    private final CloudinaryService cloudinaryService;
    private final Resend resend;

    @Value("${mail.from}")
    private String senderEmail;

    public EmailService(
            CloudinaryService cloudinaryService,
            @Value("${resend.api.key}") String resendApiKey) {

        this.cloudinaryService = cloudinaryService;
        this.resend = new Resend(resendApiKey);
    }

    public void sendFile(
            String recipientEmail,
            String fileUrl,
            String publicId,
            String resourceType,
            String fileName) throws MessagingException {

        try {

            System.out.println("========== SEND EMAIL START ==========");
            System.out.println("Recipient: " + recipientEmail);
            System.out.println("File name: " + fileName);
            System.out.println("Public ID: " + publicId);
            System.out.println("Resource type: " + resourceType);
            System.out.println("Sender: " + senderEmail);

            // 1. Download file from Cloudinary
            System.out.println("Downloading file from Cloudinary...");

            byte[] fileBytes = cloudinaryService.downloadFile(
                    fileUrl,
                    publicId,
                    resourceType,
                    fileName
            );

            System.out.println(
                    "Cloudinary download successful. File size: "
                            + fileBytes.length + " bytes"
            );

            // 2. Convert file to Base64
            String base64File = Base64.getEncoder()
                    .encodeToString(fileBytes);

            System.out.println(
                    "Base64 conversion successful. Base64 size: "
                            + base64File.length()
            );

            // 3. Create attachment
            Attachment attachment = Attachment.builder()
                    .fileName(fileName)
                    .content(base64File)
                    .build();

            System.out.println("Email attachment created.");

            // 4. Create email
            CreateEmailOptions email = CreateEmailOptions.builder()
                    .from(senderEmail)
                    .to(recipientEmail)
                    .subject("File shared with you - Mini Google Drive")
                    .text(
                            "Hello,\n\n" +
                            "A file has been shared with you through Mini Google Drive.\n\n" +
                            "File: " + fileName + "\n\n" +
                            "Regards,\n" +
                            "Mini Google Drive"
                    )
                    .attachments(attachment)
                    .build();

            System.out.println("Resend email object created.");
            System.out.println("Sending email through Resend...");

            // 5. Send through Resend
            CreateEmailResponse response = resend.emails().send(email);

            System.out.println(
                    "EMAIL SENT SUCCESSFULLY!"
            );

            System.out.println(
                    "Resend ID: " + response.getId()
            );

            System.out.println("========== SEND EMAIL END ==========");

        } catch (Exception e) {

            System.err.println("========== SEND EMAIL FAILED ==========");
            System.err.println("Error type: " + e.getClass().getName());
            System.err.println("Error message: " + e.getMessage());

            if (e.getCause() != null) {
                System.err.println(
                        "Cause type: " + e.getCause().getClass().getName()
                );

                System.err.println(
                        "Cause message: " + e.getCause().getMessage()
                );
            }

            e.printStackTrace();

            System.err.println("========== SEND EMAIL FAILED ==========");

            throw new RuntimeException(
                    "Failed to send email: "
                            + (e.getMessage() == null
                            ? "unknown email or file error"
                            : e.getMessage()),
                    e
            );
        }
    }
}