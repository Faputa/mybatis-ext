package com.example;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("camera")
public class Demo {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
