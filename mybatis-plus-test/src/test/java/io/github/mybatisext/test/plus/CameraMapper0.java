package io.github.mybatisext.test.plus;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import io.github.mybatisext.mapper.ExtMapper;

@Mapper
public interface CameraMapper0 extends BaseMapper<Camera>, ExtMapper<Camera> {

    @Override
    Long selectCount(@Param(Constants.WRAPPER) Wrapper<Camera> queryWrapper);

}
