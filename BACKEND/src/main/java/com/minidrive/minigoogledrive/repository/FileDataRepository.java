package com.minidrive.minigoogledrive.repository;

import com.minidrive.minigoogledrive.model.FileData;
import com.minidrive.minigoogledrive.model.Folder;
import com.minidrive.minigoogledrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileDataRepository extends JpaRepository<FileData, Long> {
    boolean existsByFileNameAndUser(String fileName, User user);

    boolean existsByOriginalFileIdAndUser(Long originalFileId, User user);

    List<FileData> findByFileNameContainingIgnoreCase(String fileName);

    List<FileData> findByUser(User user);

    List<FileData> findByFolder(Folder folder);

    List<FileData> findByUserAndDeletedFalse(User user);

    List<FileData> findByUserAndDeletedTrue(User user);

    List<FileData> findBySharedUsers(User user);

    List<FileData> findByUserAndStarred(User user, boolean starred);

    List<FileData> findByUserAndDeletedFalseOrderByLastAccessedDesc(User user);

    List<FileData> findByFolderAndDeletedFalse(Folder folder);

    List<FileData> findBySharedUsersContains(User user);

    List<FileData> findBySharedUsersAndDeletedFalse(User user);

    List<FileData> findByUserAndFileNameContainingIgnoreCase(
            User user,
            String fileName);

    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileData f WHERE f.user = :user AND f.deleted = false")
    long sumFileSizeByUserAndDeletedFalse(@Param("user") User user);

    long countBySharedUsersContainsAndSharedSeenFalse(User user);

    Optional<FileData> findByShareToken(String shareToken);

    @Modifying
    @Query("DELETE FROM FileData f WHERE f.id = :fileId AND :user MEMBER OF f.sharedUsers")
    void removeSharedFile(
            @Param("fileId") Long fileId,
            @Param("user") User user);

}