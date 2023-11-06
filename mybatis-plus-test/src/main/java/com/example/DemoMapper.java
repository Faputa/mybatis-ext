package com.example;

import org.apache.ibatis.annotations.Mapper;

@Mapper
@ExtMapper(Demo.class)
public interface DemoMapper extends DemoMapper0 {

    int countDidaTask();

    int countDidaTask2();
}
