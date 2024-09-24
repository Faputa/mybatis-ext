package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.util.StringUtils;

public abstract class BaseDialect implements Dialect {

    protected String buildSimpleInsert(TableInfo tableInfo, Variable variable, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        ss.add("INSERT INTO " + tableInfo.getName());
        ss.add(buildInsertItems(tableInfo, variable, ignoreNull));
        ss.add("VALUES");
        ss.add(buildInsertValues(tableInfo, variable, ignoreNull));
        return String.join(" ", ss);
    }

    protected String buildSimpleUpdate(TableInfo tableInfo, Variable variable, boolean ignoreNull, String tableAndJoin, String where) {
        List<String> ss = new ArrayList<>();
        ss.add("UPDATE");
        ss.add(tableAndJoin);
        ss.add(buildUpdateSet(tableInfo.getJoinTableInfo().getAlias(), tableInfo, variable, ignoreNull));
        if (StringUtils.isNotBlank(where)) {
            ss.add(where);
        }
        return String.join(" ", ss);
    }

    protected String buildSimpleDelete(String tableAndJoin, String where) {
        List<String> ss = new ArrayList<>();
        ss.add("DELETE");
        ss.add(tableAndJoin);
        if (StringUtils.isNotBlank(where)) {
            ss.add(where);
        }
        return String.join(" ", ss);
    }

    protected String buildInsertValues(TableInfo tableInfo, Variable variable, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (ignoreNull) {
            ss.add("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\" >");
            ss.add(buildInsertValues(tableInfo.getNameToPropertyInfo().values(), variable, true, true));
            ss.add("</trim>");
        } else {
            ss.add("(");
            ss.add(buildInsertValues(tableInfo.getNameToPropertyInfo().values(), variable, false, false));
            ss.add(")");
        }
        return String.join(" ", ss);
    }

    private String buildInsertValues(Collection<PropertyInfo> propertyInfos, Variable variable, boolean ignoreNull, boolean hasNext) {
        List<String> ss = new ArrayList<>();
        Iterator<PropertyInfo> iterator = propertyInfos.stream().filter(PropertyInfo::isOwnColumn).iterator();
        while (iterator.hasNext()) {
            PropertyInfo propertyInfo = iterator.next();
            Variable subVariable = new Variable(variable.getFullName(), propertyInfo.getName(), propertyInfo.getJavaType());
            if (propertyInfo.getColumnName() != null) {
                String value = "#{" + subVariable + "}";
                if (hasNext || iterator.hasNext()) {
                    value += ",";
                }
                if (ignoreNull) {
                    value = "<if test=\"" + subVariable + " != null\">" + value + "</if>";
                }
                ss.add(value);
            } else {
                ss.add(buildInsertValues(propertyInfo.getSubPropertyInfos(), subVariable, ignoreNull, hasNext || iterator.hasNext()));
            }
        }
        return String.join(" ", ss);
    }

    protected String buildInsertItems(TableInfo tableInfo, Variable variable, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (ignoreNull) {
            ss.add("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\" >");
            ss.add(buildInsertItems(tableInfo.getNameToPropertyInfo().values(), variable, true, true));
            ss.add("</trim>");
        } else {
            ss.add("(");
            ss.add(buildInsertItems(tableInfo.getNameToPropertyInfo().values(), variable, false, false));
            ss.add(")");
        }
        return String.join(" ", ss);
    }

    private String buildInsertItems(Collection<PropertyInfo> propertyInfos, Variable variable, boolean ignoreNull, boolean hasNext) {
        List<String> ss = new ArrayList<>();
        Iterator<PropertyInfo> iterator = propertyInfos.stream().filter(PropertyInfo::isOwnColumn).iterator();
        while (iterator.hasNext()) {
            PropertyInfo propertyInfo = iterator.next();
            Variable subVariable = new Variable(variable.getFullName(), propertyInfo.getName(), propertyInfo.getJavaType());
            if (propertyInfo.getColumnName() != null) {
                String columnName = propertyInfo.getColumnName();
                if (hasNext || iterator.hasNext()) {
                    columnName += ",";
                }
                if (ignoreNull) {
                    columnName = "<if test=\"" + subVariable + " != null\">" + columnName + "</if>";
                }
                ss.add(columnName);
            } else {
                ss.add(buildInsertItems(propertyInfo.getSubPropertyInfos(), subVariable, ignoreNull, hasNext || iterator.hasNext()));
            }
        }
        return String.join(" ", ss);
    }

    protected String buildUpdateSet(String tableAlias, TableInfo tableInfo, Variable variable, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (ignoreNull) {
            ss.add("<set>");
            ss.add(buildUpdateSet(tableAlias, tableInfo.getNameToPropertyInfo().values(), variable, true, true));
            ss.add("</set>");
        } else {
            ss.add("SET");
            ss.add(buildUpdateSet(tableAlias, tableInfo.getNameToPropertyInfo().values(), variable, false, false));
        }
        return String.join(" ", ss);
    }

    private String buildUpdateSet(String tableAlias, Collection<PropertyInfo> propertyInfos, Variable variable, boolean ignoreNull, boolean hasNext) {
        List<String> ss = new ArrayList<>();
        Iterator<PropertyInfo> iterator = propertyInfos.stream().filter(PropertyInfo::isOwnColumn).iterator();
        while (iterator.hasNext()) {
            PropertyInfo propertyInfo = iterator.next();
            Variable subVariable = new Variable(variable.getFullName(), propertyInfo.getName(), propertyInfo.getJavaType());
            if (propertyInfo.getColumnName() != null) {
                String updateItem = tableAlias + "." + propertyInfo.getColumnName() + " = #{" + subVariable + "}";
                if (hasNext || iterator.hasNext()) {
                    updateItem += ",";
                }
                if (ignoreNull) {
                    updateItem = "<if test=\"" + subVariable + " != null\">" + updateItem + "</if>";
                }
                ss.add(updateItem);
            } else {
                ss.add(buildUpdateSet(tableAlias, propertyInfo.getSubPropertyInfos(), subVariable, ignoreNull, hasNext || iterator.hasNext()));
            }
        }
        return String.join(" ", ss);
    }
}
