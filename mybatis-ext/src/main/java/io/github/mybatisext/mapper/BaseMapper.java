package io.github.mybatisext.mapper;

import java.util.List;

public interface BaseMapper<T> extends ExtMapper<T> {

    default int save(T entity) {
        return 0;
    }

    default int saveBatch(List<T> list) {
        return 0;
    }

    default int update(T entity) {
        return 0;
    }

    default int updateFull(T entity) {
        return 0;
    }

    default int delete(T query) {
        return 0;
    }

    default int deleteBatch(T query) {
        return 0;
    }

    default T get(T query) {
        return null;
    }

    default List<T> list(T query) {
        return java.util.Collections.emptyList();
    }

    default long count(T query) {
        return 0;
    }

    default boolean exists(T query) {
        return false;
    }
}
