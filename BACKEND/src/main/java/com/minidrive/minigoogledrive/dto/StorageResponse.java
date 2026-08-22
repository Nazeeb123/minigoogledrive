package com.minidrive.minigoogledrive.dto;

public class StorageResponse {

    private long used;
    private long limit;

    public StorageResponse(long used, long limit) {
        this.used = used;
        this.limit = limit;
    }


    public long getUsed() {
        return used;
    }


    public long getLimit() {
        return limit;
    }

}