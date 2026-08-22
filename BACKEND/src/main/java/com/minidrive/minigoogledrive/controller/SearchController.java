package com.minidrive.minigoogledrive.controller;


import com.minidrive.minigoogledrive.model.SearchResult;
import com.minidrive.minigoogledrive.service.SearchService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/search")
public class SearchController {


    @Autowired
    private SearchService searchService;



    @GetMapping
    public List<SearchResult> search(
            @RequestParam String keyword
    ){

        return searchService.search(keyword);

    }

}