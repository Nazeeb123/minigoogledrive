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

            // 1. Download the file from Cloudinary
            byte[] fileBytes = cloudinaryService.downloadFile(
                    fileUrl,
                    publicId,
                    resourceType,
                    fileName
            );

            // 2. Convert the file to Base64
            String base64File = Base64.getEncoder()
                    .encodeToString(fileBytes);

            // 3. Create email attachment
            Attachment attachment = Attachment.builder()
                    .fileName(fileName)
                    .content(base64File)
                    .build();

            // 4. Create the email
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

            // 5. Send email using Resend HTTPS API
            CreateEmailResponse response = resend.emails().send(email);

            System.out.println(
                    "Email sent successfully. Resend ID: "
                            + response.getId()
            );

        } catch (Exception e) {

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