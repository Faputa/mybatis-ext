package io.github.mybatisext.dialect;

import java.util.List;

import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.OrderByElement;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

public interface Dialect {

    String count(TableInfo tableInfo, Condition where);

    String exists(TableInfo tableInfo, Condition where);

    String select(TableInfo tableInfo, Condition where, boolean distinct, List<OrderByElement> orderBy, List<PropertyInfo> groupBy, Condition having, Limit limit);

    String delete(TableInfo tableInfo, Variable parameter, Condition where);

    String insert(TableInfo tableInfo, Variable parameter, boolean ignoreNull);

    String update(TableInfo tableInfo, Variable parameter, Condition where, boolean ignoreNull);

    String upper(String expr);

    String isTrue();

    String isFalse();
}
