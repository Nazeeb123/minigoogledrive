package com.minidrive.minigoogledrive.controller;

import com.minidrive.minigoogledrive.model.Folder;
import com.minidrive.minigoogledrive.service.FolderService;
import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.service.FileDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/folders")
public class FolderController {

    @Autowired
    private FolderService folderService;
    @Autowired
    private FileDataService fileDataService;

    // Create folder
    @PostMapping("/create")
    public ResponseEntity<Folder> createFolder(
            @RequestParam String folderName) {

        return ResponseEntity.ok(
                folderService.createFolder(folderName));
    }

    // Get all folders
    @GetMapping("/my")
    public ResponseEntity<List<Folder>> getFolders() {
        return ResponseEntity.ok(
                folderService.getMyFolders());
    }

    // Get all files inside a folder
    @GetMapping("/{id}/files")
    public ResponseEntity<List<FileData>> getFilesInFolder(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                fileDataService.getFilesInFolder(id));
    }

    // Delete folder
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFolder(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                folderService.deleteFolder(id));
    }

}