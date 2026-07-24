package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.service.FileDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
public class FileDataController {

    @Autowired
    private FileDataService fileDataService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileData uploadFile(@RequestParam("file") MultipartFile file) {

        return fileDataService.uploadFile(file);
    }
}