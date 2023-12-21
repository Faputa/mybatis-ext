package io.github.mybatisext.mapper;

import java.util.List;

public interface BaseMapper<T> extends ExtMapper<T> {

    int save(T entity);

    int saveBatch(List<T> list);

    int update(T entity);

    int updateFull(T entity);

    int delete(T query);

    int deleteBatch(T query);

    T get(T query);

    List<T> list(T query);

    long count(T query);

    boolean exists(T query);
}
