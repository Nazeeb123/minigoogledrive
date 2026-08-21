package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.FileData;
import net.sourceforge.tess4j.Tesseract;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Service
public class FileTextService {

        public String extractText(FileData fileData) {

                String path = fileData.getFilePath();
                String fileType = fileData.getFileType();

                File file = new File(path);

                // Handle relative paths
                if (!file.isAbsolute()) {
                        file = new File(System.getProperty("user.dir"), path);
                }

                System.out.println("=================================");
                System.out.println("AI TEXT EXTRACTION");
                System.out.println("FILE: " + fileData.getFileName());
                System.out.println("TYPE: " + fileType);
                System.out.println("PATH: " + file.getAbsolutePath());
                System.out.println("EXISTS: " + file.exists());
                System.out.println("SIZE: " + file.length());
                System.out.println("=================================");

                if (!file.exists()) {
                        throw new RuntimeException(
                                        "File does not exist: " +
                                                        file.getAbsolutePath());
                }

                // =====================================================
                // PDF
                // =====================================================

                if ("application/pdf".equalsIgnoreCase(fileType)) {

                        return extractPdfText(file);
                }

                // =====================================================
                // DOCX
                // =====================================================

                if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                .equalsIgnoreCase(fileType)) {

                        return extractDocxText(file);
                }

                // =====================================================
                // DOC
                // =====================================================

                if ("application/msword".equalsIgnoreCase(fileType)) {

                        return extractDocText(file);
                }

                // =====================================================
                // TXT
                // =====================================================

                if ("text/plain".equalsIgnoreCase(fileType)) {

                        return extractTxtText(file);
                }

                // =====================================================
                // JPG / JPEG / PNG
                // =====================================================

                if ("image/jpeg".equalsIgnoreCase(fileType)
                                || "image/jpg".equalsIgnoreCase(fileType)
                                || "image/png".equalsIgnoreCase(fileType)) {

                        return extractImageText(file);
                }

                System.out.println(
                                "UNSUPPORTED FILE TYPE: " + fileType);

                return "";
        }

        // =========================================================
        // PDF TEXT EXTRACTION
        // =========================================================

        private String extractPdfText(File file) {

                try (PDDocument document = Loader.loadPDF(file)) {

                        System.out.println("PDF PAGES: " + document.getNumberOfPages());

                        // First try normal PDF text extraction
                        PDFTextStripper stripper = new PDFTextStripper();

                        String text = stripper.getText(document);

                        System.out.println(
                                        "PDF NORMAL TEXT LENGTH: " +
                                                        (text == null ? 0 : text.length()));

                        // -------------------------------------------------
                        // If PDF contains actual text, use it
                        // -------------------------------------------------

                        if (text != null && !text.trim().isEmpty()) {

                                return cleanText(text);
                        }

                        // -------------------------------------------------
                        // Otherwise PDF is probably scanned/image PDF
                        // -------------------------------------------------

                        System.out.println(
                                        "PDF HAS NO READABLE TEXT.");
                        System.out.println(
                                        "STARTING OCR ON PDF PAGES...");

                        return extractPdfUsingOCR(document);
                }

                catch (Exception e) {

                        e.printStackTrace();

                        throw new RuntimeException(
                                        "Could not read PDF file: " +
                                                        e.getMessage(),
                                        e);
                }
        }

        // =========================================================
        // PDF OCR
        // =========================================================

        private String extractPdfUsingOCR(
                        PDDocument document) {

                StringBuilder result = new StringBuilder();

                try {

                        PDFRenderer renderer = new PDFRenderer(document);

                        Tesseract tesseract = createTesseract();

                        int totalPages = document.getNumberOfPages();

                        for (int page = 0; page < totalPages; page++) {

                                System.out.println(
                                                "OCR PDF PAGE " +
                                                                (page + 1) +
                                                                "/" +
                                                                totalPages);

                                // 200 DPI gives good OCR quality
                                BufferedImage image = renderer.renderImageWithDPI(
                                                page,
                                                200);

                                String pageText = tesseract.doOCR(image);

                                if (pageText != null
                                                && !pageText.trim().isEmpty()) {

                                        result.append(
                                                        "\n\n===== PAGE ")
                                                        .append(page + 1)
                                                        .append(" =====\n\n");

                                        result.append(pageText);
                                }
                        }

                        String finalText = cleanText(result.toString());

                        System.out.println(
                                        "PDF OCR TEXT LENGTH: " +
                                                        finalText.length());

                        return finalText;

                } catch (Exception e) {

                        e.printStackTrace();

                        throw new RuntimeException(
                                        "Could not OCR PDF: " +
                                                        e.getMessage(),
                                        e);
                }
        }

        // =========================================================
        // DOCX
        // =========================================================

        private String extractDocxText(File file) {

                try (
                                FileInputStream input = new FileInputStream(file);

                                XWPFDocument document = new XWPFDocument(input)) {

                        StringBuilder text = new StringBuilder();

                        document.getParagraphs()
                                        .forEach(paragraph -> text.append(
                                                        paragraph.getText())
                                                        .append("\n"));

                        return cleanText(
                                        text.toString());

                } catch (IOException e) {

                        throw new RuntimeException(
                                        "Could not read DOCX file",
                                        e);
                }
        }

        // =========================================================
        // DOC
        // =========================================================

        private String extractDocText(File file) {

                try (
                                FileInputStream input = new FileInputStream(file);

                                HWPFDocument document = new HWPFDocument(input);

                                WordExtractor extractor = new WordExtractor(document)) {

                        return cleanText(
                                        extractor.getText());

                } catch (IOException e) {

                        throw new RuntimeException(
                                        "Could not read DOC file",
                                        e);
                }
        }

        // =========================================================
        // TXT
        // =========================================================

        private String extractTxtText(File file) {

                try {

                        byte[] bytes = Files.readAllBytes(
                                        file.toPath());

                        if (bytes.length == 0) {
                                return "";
                        }

                        // UTF-8 BOM
                        if (bytes.length >= 3
                                        && (bytes[0] & 0xFF) == 0xEF
                                        && (bytes[1] & 0xFF) == 0xBB
                                        && (bytes[2] & 0xFF) == 0xBF) {

                                return new String(
                                                bytes,
                                                3,
                                                bytes.length - 3,
                                                StandardCharsets.UTF_8);
                        }

                        String text = new String(
                                        bytes,
                                        StandardCharsets.UTF_8);

                        if (!text.contains("\uFFFD")) {
                                return cleanText(text);
                        }

                        return cleanText(
                                        new String(
                                                        bytes,
                                                        Charset.forName(
                                                                        "Windows-1252")));

                } catch (Exception e) {

                        throw new RuntimeException(
                                        "Could not read TXT file: " +
                                                        e.getMessage(),
                                        e);
                }
        }

        // =========================================================
        // IMAGE OCR
        // =========================================================

        private String extractImageText(File file) {

                try {

                        System.out.println(
                                        "STARTING IMAGE OCR...");

                        BufferedImage image = ImageIO.read(file);

                        if (image == null) {

                                throw new RuntimeException(
                                                "Could not load image.");
                        }

                        System.out.println(
                                        "IMAGE SIZE: " +
                                                        image.getWidth() +
                                                        " x " +
                                                        image.getHeight());

                        Tesseract tesseract = createTesseract();

                        String text = tesseract.doOCR(image);

                        System.out.println(
                                        "IMAGE OCR TEXT LENGTH: " +
                                                        (text == null
                                                                        ? 0
                                                                        : text.length()));

                        return cleanText(text);

                } catch (Exception e) {

                        e.printStackTrace();

                        throw new RuntimeException(
                                        "Could not read image using OCR: " +
                                                        e.getMessage(),
                                        e);
                }
        }

        // =========================================================
        // CREATE TESSERACT
        // =========================================================

        private Tesseract createTesseract() {

                String tessdataPath = "C:/Program Files/Tesseract-OCR/tessdata";

                File tessdataFolder = new File(tessdataPath);

                if (!tessdataFolder.exists()) {

                        throw new RuntimeException(
                                        "Tesseract tessdata folder not found: "
                                                        + tessdataPath);
                }

                File englishData = new File(
                                tessdataFolder,
                                "eng.traineddata");

                if (!englishData.exists()) {

                        throw new RuntimeException(
                                        "Tesseract English language file not found: "
                                                        + englishData.getAbsolutePath());
                }

                Tesseract tesseract = new Tesseract();

                tesseract.setDatapath(
                                tessdataPath);

                tesseract.setLanguage(
                                "eng");

                // Better page segmentation
                tesseract.setPageSegMode(6);

                return tesseract;
        }

        // =========================================================
        // CLEAN TEXT
        // =========================================================

        private String cleanText(String text) {

                if (text == null) {
                        return "";
                }

                return text
                                .replace("\r\n", "\n")
                                .replace("\r", "\n")
                                .replaceAll(
                                                "\n{3,}",
                                                "\n\n")
                                .trim();
        }
}