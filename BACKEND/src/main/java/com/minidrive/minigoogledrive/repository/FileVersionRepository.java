package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    List<FileVersion> findByFileDataId(Long fileId);

}
