package com.minidrive.minigoogledrive.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String fileName;

    private String filePath;

    private long fileSize;

    private int versionNumber;

    private LocalDateTime createdAt;

    private String fileType;


    @ManyToOne
    @JoinColumn(name = "file_data_id")
    private FileData fileData;


    public Long getId() {
        return id;
    }


    public String getFileName() {
        return fileName;
    }


    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    public String getFilePath() {
        return filePath;
    }


    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


    public long getFileSize() {
        return fileSize;
    }


    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }


    public int getVersionNumber() {
        return versionNumber;
    }


    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public FileData getFileData() {
        return fileData;
    }


    public void setFileData(FileData fileData) {
        this.fileData = fileData;
    }
    public String getFileType() {
     return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
