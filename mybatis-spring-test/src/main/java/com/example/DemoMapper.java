package com.example;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DemoMapper {

    int countDidaTask();

    int countDidaTask2();
}
