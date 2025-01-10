package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.List;

import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.TableInfo;

public class MySqlDialect extends BaseSimpleDialect {

    @Override
    public String buildInsert(TableInfo tableInfo, Variable variable, boolean batch, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + variable.getName() + "__item", variable.getJavaType().getTypeParameters()[0]);
            if (ignoreNull) {
                ss.add("<foreach Iterable=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"begin\" close=\"; end;\" separator=\";\">");
                ss.add(buildInsert(tableInfo, itemVariable, false, true));
                ss.add("</foreach>");
                return String.join(" ", ss);
            }
            ss.add("INSERT INTO " + tableInfo.getName());
            ss.add(buildInsertItems(tableInfo, itemVariable, false));
            ss.add("VALUES");
            ss.add("<foreach Iterable=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"begin\" close=\"; end;\" separator=\";\">");
            ss.add(buildInsertValues(tableInfo, itemVariable, false));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        return buildSimpleInsert(tableInfo, variable, ignoreNull);
    }

    @Override
    public String buildUpdate(TableInfo tableInfo, Variable variable, boolean batch, boolean ignoreNull, boolean join, String tableAndJoin, String where) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + variable.getName() + "__item", variable.getJavaType().getTypeParameters()[0]);
            ss.add("<foreach Iterable=\"" + variable + "\" item=\"" + "__" + variable.getName() + "__item\" open=\"begin\" close=\"; end;\" separator=\";\">");
            ss.add(buildUpdate(tableInfo, itemVariable, false, ignoreNull, join, tableAndJoin, where));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        return buildSimpleUpdate(tableInfo, variable, ignoreNull, where);
    }

    @Override
    public String buildDelete(TableInfo tableInfo, Variable variable, boolean batch, boolean join, String tableAndJoin, String where) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + variable.getName() + "__item", variable.getJavaType().getTypeParameters()[0]);
            ss.add("<foreach collection=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"begin\" close=\"; end;\" separator=\";\">");
            ss.add(buildDelete(tableInfo, itemVariable, false, join, tableAndJoin, where));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        return buildSimpleDelete(tableInfo, where);
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
            ss.add(limit.getOffset() != null ? limit.getOffset().toString() : "#{" + limit.getOffsetVariable() + "}");
            ss.add(",");
            ss.add(limit.getRowCount() != null ? limit.getRowCount().toString() : "#{" + limit.getRowCountVariable() + "}");
        }
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
        return "`" + name + "`";
    }
}
