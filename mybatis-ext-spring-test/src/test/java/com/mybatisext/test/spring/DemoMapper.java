package com.mybatisext.test.spring;

import org.apache.ibatis.annotations.Mapper;

import com.mybatisext.mapper.BaseMapper;

@Mapper
public interface DemoMapper extends BaseMapper<Demo> {

    int countDidaTask();

    int countDidaTask2();
}
