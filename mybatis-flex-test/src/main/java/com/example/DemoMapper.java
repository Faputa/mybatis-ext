package com.example;

import org.apache.ibatis.annotations.Mapper;

import com.mybatisflex.core.BaseMapper;

@Mapper
@Mapping(Demo.class)
public interface DemoMapper extends BaseMapper<Demo> {

    int countDidaTask();

    int countDidaTask2();

    int countDidaTask3();
}
