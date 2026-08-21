package com.minidrive.minigoogledrive.service;

import com.minidrive.minigoogledrive.model.FileData;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
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

                if (!file.isAbsolute()) {
                        file = new File(System.getProperty("user.dir"), path);
                }

                // If DB contains a relative path like "uploads\file.pdf",
                // resolve it from the project working directory.
                

                if (!file.exists()) {
                        throw new RuntimeException(
                                        "File does not exist: " + file.getAbsolutePath());
                }

                // PDF
                if ("application/pdf".equalsIgnoreCase(fileType)) {

                        try (PDDocument document = Loader.loadPDF(file)) {

                                PDFTextStripper stripper = new PDFTextStripper();

                                return stripper.getText(document);

                        } catch (IOException e) {

                                throw new RuntimeException(
                                                "Could not read PDF file", e);
                        }
                }

                // DOCX
                if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                .equalsIgnoreCase(fileType)) {

                        try (
                                        FileInputStream input = new FileInputStream(file);

                                        XWPFDocument document = new XWPFDocument(input)) {

                                StringBuilder text = new StringBuilder();

                                document.getParagraphs()
                                                .forEach(paragraph -> text.append(
                                                                paragraph.getText()).append("\n"));

                                return text.toString();

                        } catch (IOException e) {

                                throw new RuntimeException(
                                                "Could not read DOCX file", e);
                        }
                }

                // DOC
                if ("application/msword"
                                .equalsIgnoreCase(fileType)) {

                        try (
                                        FileInputStream input = new FileInputStream(file);

                                        HWPFDocument document = new HWPFDocument(input);

                                        WordExtractor extractor = new WordExtractor(document)) {

                                return extractor.getText();

                        } catch (IOException e) {

                                throw new RuntimeException(
                                                "Could not read DOC file", e);
                        }
                }

                // TXT
                if ("text/plain".equalsIgnoreCase(fileType)) {

                        try {

                                byte[] bytes = Files.readAllBytes(file.toPath());

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
                                        return text;
                                }

                                return new String(
                                                bytes,
                                                Charset.forName("Windows-1252"));

                        } catch (Exception e) {

                                throw new RuntimeException(
                                                "Could not read TXT file: "
                                                                + e.getMessage(),
                                                e);
                        }
                }

                // JPG / JPEG / PNG
                if ("image/jpeg".equalsIgnoreCase(fileType)
                                || "image/jpg".equalsIgnoreCase(fileType)
                                || "image/png".equalsIgnoreCase(fileType)) {

                        return extractImageText(file);
                }

                return "";
        }

        private String extractImageText(File file) {

                try {

                        BufferedImage image = ImageIO.read(file);

                        if (image == null) {
                                throw new RuntimeException(
                                                "Could not load image.");
                        }

                        Tesseract tesseract = new Tesseract();

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

                        tesseract.setDatapath(tessdataPath);
                        tesseract.setLanguage("eng");

                        return tesseract.doOCR(image);

                } catch (Exception e) {

                        throw new RuntimeException(
                                        "Could not read image using OCR: "
                                                        + e.getMessage(),
                                        e);
                }
        }
}