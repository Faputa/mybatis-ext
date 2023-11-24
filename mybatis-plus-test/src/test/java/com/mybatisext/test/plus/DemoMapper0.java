package com.mybatisext.test.plus;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.mybatisext.mapper.ExtMapper;

@Mapper
public interface DemoMapper0 extends BaseMapper<Demo>, ExtMapper<Demo> {

    @Override
    Long selectCount(@Param(Constants.WRAPPER) Wrapper<Demo> queryWrapper);

}
