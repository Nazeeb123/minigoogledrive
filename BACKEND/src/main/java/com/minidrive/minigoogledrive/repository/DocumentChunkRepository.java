package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.DocumentChunk;
import com.minidrive.minigoogledrive.model.FileData;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByFile(FileData file);

    void deleteByFile(FileData file);
}