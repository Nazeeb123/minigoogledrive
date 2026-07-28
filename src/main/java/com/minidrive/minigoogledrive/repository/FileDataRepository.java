package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.model.Folder;
import com.minidrive.minigoogledrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileDataRepository extends JpaRepository<FileData, Long> {

    List<FileData> findByFileNameContainingIgnoreCase(String fileName);

    List<FileData> findByUser(User user);
    List<FileData> findByFolder(Folder folder);
    List<FileData> findByUserAndDeletedFalse(User user);

    List<FileData> findByUserAndDeletedTrue(User user);
    List<FileData> findBySharedUsers(User user);

    List<FileData> findByUserAndStarred(User user, boolean starred);

    List<FileData> findByUserAndDeletedFalseOrderByLastAccessedDesc(User user);

    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileData f WHERE f.user = :user AND f.deleted = false")
    long sumFileSizeByUserAndDeletedFalse(@Param("user") User user);

    Optional<FileData> findByShareToken(String shareToken);

}