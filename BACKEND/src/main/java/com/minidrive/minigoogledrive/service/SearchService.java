package com.minidrive.minigoogledrive.service;


import com.minidrive.minigoogledrive.model.*;
import com.minidrive.minigoogledrive.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class SearchService {


    @Autowired
    private FileDataRepository fileRepository;


    @Autowired
    private FolderRepository folderRepository;


    @Autowired
    private UserRepository userRepository;



    public List<SearchResult> search(String keyword){


        String email =
        SecurityContextHolder
        .getContext()
        .getAuthentication()
        .getName();



        User user =
        userRepository.findByEmail(email)
        .orElseThrow();



        List<SearchResult> results =
        new ArrayList<>();


        folderRepository
        .findByUserAndFolderNameContainingIgnoreCase(user, keyword)
        .forEach(folder ->
            results.add(
                new SearchResult(
                    "folder",
                    folder.getId(),
                    folder.getFolderName()
                )
            )
        );



        fileRepository
        .findByUserAndFileNameContainingIgnoreCase(user, keyword)
        .stream()
        .filter(file -> !file.isDeleted())
        .forEach(file ->
            results.add(
                new SearchResult(
                    "file",
                    file.getId(),
                    file.getFileName()
                )
            )
        );


        return results;

    }

}
