package com.mybatisext.test.flex;

import org.apache.ibatis.annotations.Mapper;

import com.mybatisext.annotation.MapTable;
import com.mybatisflex.core.BaseMapper;

@Mapper
@MapTable(Demo.class)
public interface DemoMapper extends BaseMapper<Demo> {

    int countDidaTask();

    int countDidaTask2();

    // int countDidaTask3();
}
