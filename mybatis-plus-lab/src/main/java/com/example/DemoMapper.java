package com.example;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface DemoMapper extends BaseMapper<Demo> {

    int countDidaTask();

    int countDidaTask2();
}
