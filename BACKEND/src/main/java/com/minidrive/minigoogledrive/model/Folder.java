package com.minidrive.minigoogledrive.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Data
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String folderName;


    private LocalDateTime createdDate;


    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


   @OneToMany(mappedBy = "folder")
    @JsonManagedReference
    private List<FileData> files;
}