package io.github.mybatisext.mapper;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.ibatis.session.RowBounds;

import io.github.mybatisext.annotation.OnlyById;

public interface BaseMapper<T> extends ExtMapper<T> {

    int save(T entity);

    int saveIgnoreNull(T entity);

    int saveBatch(List<T> list);

    int saveBatchIgnoreNull(List<T> list);

    int update(@OnlyById T entity);

    int updateIgnoreNull(@OnlyById T entity);

    int updateBatch(@OnlyById List<T> list);

    int updateBatchIgnoreNull(@OnlyById List<T> list);

    int delete(@OnlyById T query);

    int deleteBatch(@OnlyById List<T> query);

    @Nullable
    T get(@OnlyById T query);

    List<T> list(T query);

    List<T> list(T query, RowBounds rowBounds);

    long count(T query);

    boolean exists(T query);
}
