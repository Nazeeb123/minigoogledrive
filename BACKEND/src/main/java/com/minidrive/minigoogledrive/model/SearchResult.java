package com.minidrive.minigoogledrive.model;

public class SearchResult {

    private String type;
    private Long id;
    private String name;

    public SearchResult(String type, Long id, String name) {
        this.type = type;
        this.id = id;
        this.name = name;
    }


    public String getType() {
        return type;
    }


    public Long getId() {
        return id;
    }


    public String getName() {
        return name;
    }
}
