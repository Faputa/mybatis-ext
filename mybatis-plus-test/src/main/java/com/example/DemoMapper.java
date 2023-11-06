package com.example;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DemoMapper extends DemoMapper0 {

    int countDidaTask();

    int countDidaTask2();
}
