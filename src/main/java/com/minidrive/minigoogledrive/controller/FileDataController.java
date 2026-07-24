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

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileDataController {

    @Autowired
    private FileDataService fileDataService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileData uploadFile(@RequestParam("file") MultipartFile file) {
        return fileDataService.uploadFile(file);
    }

    @GetMapping
    public List<FileData> getAllFiles() {
        return fileDataService.getAllFiles();
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {

        Resource resource = fileDataService.downloadFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public String deleteFile(@PathVariable Long id) {
        return fileDataService.deleteFile(id);
    }

    @GetMapping("/search")
    public List<FileData> searchFiles(@RequestParam("name") String fileName) {
        return fileDataService.searchFiles(fileName);
    }

    @PutMapping("/rename/{id}")
    public String renameFile(@PathVariable Long id,
                             @RequestParam("newName") String newName) {
        return fileDataService.renameFile(id, newName);
    }
}