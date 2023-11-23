package com.mybatisext.test.plus;

import org.apache.ibatis.annotations.Mapper;

import com.mybatisext.annotation.MapTable;

@Mapper
@MapTable(Demo.class)
public interface DemoMapper extends DemoMapper0 {

    int countDidaTask();

    int countDidaTask2();

    // int countDidaTask3();
}
