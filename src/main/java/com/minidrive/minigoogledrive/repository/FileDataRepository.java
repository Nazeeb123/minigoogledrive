package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileDataRepository extends JpaRepository<FileData, Long> {

    List<FileData> findByUser(User user);

}