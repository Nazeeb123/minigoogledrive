package com.minidrive.minigoogledrive.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendFile(
            String recipientEmail,
            String filePath,
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

        FileSystemResource file = new FileSystemResource(new File(filePath));

        if (!file.exists()) {
            throw new RuntimeException(
                    "File not found: " + filePath);
        }

        helper.addAttachment(fileName, file);

        mailSender.send(message);
    }
}