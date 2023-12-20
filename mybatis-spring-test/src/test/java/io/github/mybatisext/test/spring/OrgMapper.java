package io.github.mybatisext.test.spring;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrgMapper {

    List<Org> selectOrg();

    List<Org> selectOrgLazy();

    List<Org> selectOrgJoin();
}
