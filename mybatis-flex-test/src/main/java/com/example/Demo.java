package com.example;

import com.mybatisflex.annotation.Table;

@Table("camera")
public class Demo {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
