package com.example;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;

@Mapper
public interface DemoMapper0 extends BaseMapper<Demo> {

    @Override
    Long selectCount(@Param(Constants.WRAPPER) Wrapper<Demo> queryWrapper);

}
