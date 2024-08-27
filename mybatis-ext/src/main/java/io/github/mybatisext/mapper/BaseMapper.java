package io.github.mybatisext.mapper;

import java.util.List;

import io.github.mybatisext.annotation.OnlyById;

public interface BaseMapper<T> extends ExtMapper<T> {

    int save(T entity);

    int saveBatch(List<T> list);

    int update(@OnlyById T entity);

    int updateIgnoreNull(@OnlyById T entity);

    int delete(@OnlyById T query);

    int deleteBatch(@OnlyById T query);

    T get(@OnlyById T query);

    List<T> list(T query);

    long count(T query);

    boolean exists(T query);
}
