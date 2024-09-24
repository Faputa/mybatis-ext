package io.github.mybatisext.dialect;

import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.TableInfo;

public interface Dialect {

    String insert(TableInfo tableInfo, Variable variable, boolean batch, boolean ignoreNull);

    String update(TableInfo tableInfo, Variable variable, boolean batch, boolean ignoreNull, boolean join, String tableAndJoin, String where);

    String delete(TableInfo tableInfo, Variable variable, boolean batch, boolean join, String tableAndJoin, String where);

    String limit(Limit limit, String select);

    String exists(String select);

    String upper(String expr);

    String isTrue();

    String isFalse();
}
