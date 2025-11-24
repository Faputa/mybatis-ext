package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.jpa.ConditionHelper;
import io.github.mybatisext.jpa.ConditionType;
import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.LogicalOperator;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.util.TypeArgumentResolver;

public class PostgreSqlDialect extends BaseTemplateDialect {

    @Override
    public String buildUpdate(TableInfo tableInfo, List<PropertyInfo> selectItems, Variable parameter, List<JoinTableInfo> joinTableInfos, Condition where, boolean batch, boolean join, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + parameter.getName() + "__item", TypeArgumentResolver.resolveGenericType(parameter.getJavaType(), Collection.class, 0));
            ss.add("<foreach collection=\"" + parameter + "\" item=\"" + "__" + parameter.getName() + "__item\" open=\"\" close=\"\" separator=\";\">");
            ss.add(buildUpdate(tableInfo, selectItems, itemVariable, joinTableInfos, where, false, join, ignoreNull));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        if (join) {
            ss.add("UPDATE");
            ss.add(tableInfo.getName());
            ss.add(tableInfo.getJoinTableInfo().getAlias());
            ss.add(buildUpdateSet(null, selectItems, parameter, ignoreNull));
            ss.add("FROM");
            ss.add(buildJoinAndWhere(tableInfo, joinTableInfos, where));
            return String.join(" ", ss);
        }
        ss.add("UPDATE");
        ss.add(tableInfo.getName());
        ss.add(tableInfo.getJoinTableInfo().getAlias());
        ss.add(buildUpdateSet(null, selectItems, parameter, ignoreNull));
        if (where != null) {
            ss.add(buildWhere(tableInfo, where));
        }
        return String.join(" ", ss);
    }

    @Override
    public String buildDelete(TableInfo tableInfo, Variable parameter, List<JoinTableInfo> joinTableInfos, Condition where, boolean batch, boolean join) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + parameter.getName() + "__item", TypeArgumentResolver.resolveGenericType(parameter.getJavaType(), Collection.class, 0));
            ss.add("<foreach collection=\"" + parameter + "\" item=\"" + "__" + parameter.getName() + "__item\" open=\"\" close=\"\" separator=\";\">");
            ss.add(buildDelete(tableInfo, itemVariable, joinTableInfos, where, false, join));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        if (join) {
            ss.add("DELETE FROM");
            ss.add(tableInfo.getName());
            ss.add(tableInfo.getJoinTableInfo().getAlias());
            ss.add("USING");
            ss.add(buildJoinAndWhere(tableInfo, joinTableInfos, where));
            return String.join(" ", ss);
        }
        return buildSimpleDelete(tableInfo, where);
    }

    @Override
    public String buildInsert(TableInfo tableInfo, Variable variable, boolean batch, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + variable.getName() + "__item", TypeArgumentResolver.resolveGenericType(variable.getJavaType(), Collection.class, 0));
            if (ignoreNull) {
                ss.add("<foreach collection=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"\" close=\"\" separator=\";\">");
                ss.add(buildSimpleInsert(tableInfo, itemVariable, true));
                ss.add("</foreach>");
                return String.join(" ", ss);
            }
            ss.add("INSERT INTO " + tableInfo.getName());
            ss.add(buildInsertItems(tableInfo, itemVariable, false));
            ss.add("VALUES");
            ss.add("<foreach collection=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"\" close=\"\" separator=\",\">");
            ss.add(buildInsertValues(tableInfo, itemVariable, false));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        return buildSimpleInsert(tableInfo, variable, ignoreNull);
    }

    @Override
    public String buildLimit(Limit limit, String select) {
        List<String> ss = new ArrayList<>();
        ss.add(select);
        if (limit.getOffset() == null && limit.getOffsetVariable() == null) {
            ss.add("LIMIT");
            ss.add(limit.getRowCount() != null ? limit.getRowCount().toString() : "#{" + limit.getRowCountVariable() + "}");
        } else {
            ss.add("LIMIT");
            ss.add(limit.getRowCount() != null ? limit.getRowCount().toString() : "#{" + limit.getRowCountVariable() + "}");
            ss.add("OFFSET");
            ss.add(limit.getOffset() != null ? limit.getOffset().toString() : "#{" + limit.getOffsetVariable() + "}");
        }
        return String.join(" ", ss);
    }

    private String buildJoinAndWhere(TableInfo tableInfo, List<JoinTableInfo> joinTableInfos, Condition where) {
        List<Condition> conditions = new ArrayList<>();
        List<String> tables = new ArrayList<>();
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            tables.add(joinTableInfo.getTableInfo() + " " + joinTableInfo.getAlias());
            joinTableInfo.getLeftJoinTableInfos().forEach((joinColumnInfo, leftJoinTableInfo) -> {
                Condition condition = new Condition(ConditionType.BASIC);
                condition.setExprTemplate(joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumn().getColumnName() + " = " + leftJoinTableInfo.getAlias() + "." + joinColumnInfo.getLeftColumn().getColumnName());
                conditions.add(condition);
            });
        }
        conditions.add(where);
        List<String> ss = new ArrayList<>();
        ss.add(String.join(", ", tables));
        ss.add(ConditionHelper.toWhere(tableInfo, conditions, LogicalOperator.AND, this));
        return String.join(" ", ss);
    }

    @Override
    public String buildExists(String select) {
        return "SELECT EXISTS (" + select + ")";
    }

    @Override
    public String upper(String expr) {
        return "UPPER(" + expr + ")";
    }

    @Override
    public String isTrue() {
        return "IS TRUE";
    }

    @Override
    public String isFalse() {
        return "IS NOT TRUE";
    }

    @Override
    public String quote(String name) {
        return "\"" + name + "\"";
    }
}
