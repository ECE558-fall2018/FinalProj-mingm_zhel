package com.example.test1;

import java.util.Map;

/**
 * Model class for Firebase data entries
 */
public class ImageEntry {

    Long timestamp;
    String image;

    public ImageEntry() {
    }

    public ImageEntry(Long timestamp, String image) {
        this.timestamp = timestamp;
        this.image = image;
        //      this.annotations = annotations;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getImage() {
        return image;
    }
}

