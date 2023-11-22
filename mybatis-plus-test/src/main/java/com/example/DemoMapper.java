package com.example;

import org.apache.ibatis.annotations.Mapper;

@Mapper
@Mapping(Demo.class)
public interface DemoMapper extends DemoMapper0 {

    int countDidaTask();

    int countDidaTask2();

    int countDidaTask3();
}
