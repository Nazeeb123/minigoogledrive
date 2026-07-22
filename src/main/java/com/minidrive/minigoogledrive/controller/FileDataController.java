package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.service.FileDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileDataController {

    @Autowired
    private FileDataService fileDataService;

    // Test API
    @GetMapping("/test")
    public String test() {
        return "Controller Working";
    }

    // Upload File
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileData uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return fileDataService.uploadFile(file);
    }

    // Get All Files
    @GetMapping
    public List<FileData> getAllFiles() {
        return fileDataService.getAllFiles();
    }

    // Download File
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) throws IOException {

        Resource resource = fileDataService.downloadFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // Delete File
    @DeleteMapping("/{id}")
    public String deleteFile(@PathVariable Long id) {
        return fileDataService.deleteFile(id);
    }
}