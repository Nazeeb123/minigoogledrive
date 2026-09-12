package com.minidrive.minigoogledrive.service;

import jakarta.mail.MessagingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Service
public class EmailService {

    private final CloudinaryService cloudinaryService;
    private final HttpClient httpClient;

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${mail.from}")
    private String senderEmail;

    public EmailService(CloudinaryService cloudinaryService) {

        this.cloudinaryService = cloudinaryService;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
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

            // =====================================================
            // 1. DOWNLOAD FILE FROM CLOUDINARY
            // =====================================================

            System.out.println("Downloading file from Cloudinary...");

            byte[] fileBytes = cloudinaryService.downloadFile(
                    fileUrl,
                    publicId,
                    resourceType,
                    fileName
            );

            System.out.println(
                    "Cloudinary download successful. File size: "
                            + fileBytes.length
                            + " bytes"
            );

            // =====================================================
            // 2. CONVERT FILE TO BASE64
            // =====================================================

            String base64File = Base64.getEncoder()
                    .encodeToString(fileBytes);

            System.out.println(
                    "Base64 conversion successful. Base64 size: "
                            + base64File.length()
            );

            // =====================================================
            // 3. ESCAPE JSON VALUES
            // =====================================================

            String safeSender = escapeJson(senderEmail);
            String safeRecipient = escapeJson(recipientEmail);
            String safeFileName = escapeJson(fileName);

            // =====================================================
            // 4. CREATE EMAIL TEXT
            // =====================================================

            String emailText =
                    "Hello,\n\n"
                            + "A file has been shared with you through Mini Google Drive.\n\n"
                            + "File: " + fileName + "\n\n"
                            + "Regards,\n"
                            + "Mini Google Drive";

            String safeEmailText = escapeJson(emailText);

            // =====================================================
            // 5. CREATE RESEND JSON
            // =====================================================

            String jsonBody =
                    "{"
                            + "\"from\":\"" + safeSender + "\","
                            + "\"to\":[\"" + safeRecipient + "\"],"
                            + "\"subject\":\"File shared with you - Mini Google Drive\","
                            + "\"text\":\"" + safeEmailText + "\","
                            + "\"attachments\":["
                            + "{"
                            + "\"filename\":\"" + safeFileName + "\","
                            + "\"content\":\"" + base64File + "\""
                            + "}"
                            + "]"
                            + "}";

            System.out.println("Resend request created.");

            // =====================================================
            // 6. CREATE HTTPS REQUEST
            // =====================================================

            System.out.println("Connecting to Resend...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(60))
                    .header(
                            "Authorization",
                            "Bearer " + resendApiKey
                    )
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(jsonBody)
                    )
                    .build();

            // =====================================================
            // 7. SEND EMAIL
            // =====================================================

            System.out.println("Sending email through Resend...");

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // =====================================================
            // 8. PRINT RESEND RESPONSE
            // =====================================================

            System.out.println(
                    "Resend HTTP Status: "
                            + response.statusCode()
            );

            System.out.println(
                    "Resend Response: "
                            + response.body()
            );

            // =====================================================
            // 9. CHECK SUCCESS
            // =====================================================

            if (response.statusCode() >= 200
                    && response.statusCode() < 300) {

                System.out.println(
                        "========================================"
                );

                System.out.println(
                        "EMAIL SENT SUCCESSFULLY!"
                );

                System.out.println(
                        "Recipient: " + recipientEmail
                );

                System.out.println(
                        "File: " + fileName
                );

                System.out.println(
                        "========================================"
                );

                return;
            }

            // =====================================================
            // 10. RESEND ERROR
            // =====================================================

            throw new RuntimeException(
                    "Resend rejected the email. HTTP "
                            + response.statusCode()
                            + " - "
                            + response.body()
            );

        } catch (Exception e) {

            System.err.println(
                    "========== SEND EMAIL FAILED =========="
            );

            System.err.println(
                    "Error type: "
                            + e.getClass().getName()
            );

            System.err.println(
                    "Error message: "
                            + e.getMessage()
            );

            e.printStackTrace();

            System.err.println(
                    "========== SEND EMAIL FAILED =========="
            );

            throw new RuntimeException(
                    "Failed to send email: "
                            + (e.getMessage() == null
                            ? "Unknown email error"
                            : e.getMessage()),
                    e
            );
        }
    }

    // =============================================================
    // JSON ESCAPE METHOD
    // =============================================================

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}