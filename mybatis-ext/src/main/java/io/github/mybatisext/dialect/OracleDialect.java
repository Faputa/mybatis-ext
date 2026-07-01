package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.List;

import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

public class OracleDialect extends BaseTemplateDialect {

    @Override
    public String buildInsert(TableInfo tableInfo, Variable variable, boolean batch, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = variable.getItemVariable();
            ss.add("<foreach collection=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"begin\" close=\"; end;\" separator=\";\">");
            ss.add(buildSimpleInsert(tableInfo, itemVariable, ignoreNull));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        return buildSimpleInsert(tableInfo, variable, ignoreNull);
    }

    @Override
    public String buildUpdate(TableInfo tableInfo, List<PropertyInfo> selectItems, Variable variable, List<JoinTableInfo> joinTableInfos, Condition where, boolean batch, boolean join, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = variable.getItemVariable();
            ss.add("<foreach collection=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"begin\" close=\"; end;\" separator=\";\">");
            ss.add(buildUpdate(tableInfo, selectItems, itemVariable, joinTableInfos, where, false, join, ignoreNull));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        if (join) {
            ss.add("UPDATE (");
            ss.add("SELECT");
            ss.add(tableInfo.getJoinTableInfo().getAlias() + ".* FROM");
            ss.add(buildTableAndJoin(joinTableInfos));
            if (where != null) {
                ss.add(buildWhere(where));
            }
            ss.add(") __x");
            ss.add(buildUpdateSet("__x", selectItems, variable, ignoreNull));
            return String.join(" ", ss);
        }
        return buildSimpleUpdate(tableInfo, selectItems, variable, ignoreNull, where);
    }

    @Override
    public String buildDelete(TableInfo tableInfo, Variable variable, List<JoinTableInfo> joinTableInfos, Condition where, boolean batch, boolean join) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = variable.getItemVariable();
            ss.add("<foreach collection=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"begin\" close=\"; end;\" separator=\";\">");
            ss.add(buildDelete(tableInfo, itemVariable, joinTableInfos, where, false, join));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        if (join) {
            ss.add("DELETE FROM (");
            ss.add("SELECT");
            ss.add(tableInfo.getJoinTableInfo().getAlias() + ".* FROM");
            ss.add(buildTableAndJoin(joinTableInfos));
            if (where != null) {
                ss.add(buildWhere(where));
            }
            ss.add(") __x");
            return String.join(" ", ss);
        }
        return buildSimpleDelete(tableInfo, where);
    }

    @Override
    public String buildLimit(Limit limit, String select) {
        String startRow;
        String endRow;
        if (limit.getOffset() == null && limit.getOffsetVariable() == null) {
            startRow = null;
            endRow = limit.getRowCount() != null ? limit.getRowCount().toString() : "#{" + limit.getRowCountVariable() + "}";
        } else {
            startRow = limit.getOffset() != null ? limit.getOffset().toString() : "#{" + limit.getOffsetVariable() + "}";
            String offset = limit.getOffset() != null ? limit.getOffset().toString() : limit.getOffsetVariable().toString();
            String rowCount = limit.getRowCount() != null ? limit.getRowCount().toString() : limit.getRowCountVariable().toString();
            endRow = "<bind name=\"__endRow\" value=\"" + offset + " + " + rowCount + "\"/> #{__endRow}";
        }
        List<String> ss = new ArrayList<>();
        if (startRow != null) {
            ss.add("SELECT * FROM (");
        }
        ss.add("SELECT TMP_PAGE.*, ROWNUM PAGEHELPER_ROW_ID FROM (");
        ss.add(select);
        ss.add(") TMP_PAGE WHERE ROWNUM &lt;=");
        ss.add(endRow);
        if (startRow != null) {
            ss.add(") WHERE PAGEHELPER_ROW_ID &gt;");
            ss.add(startRow);
        }
        return String.join(" ", ss);
    }

    @Override
    public String buildExists(String select) {
        return "SELECT CASE WHEN EXISTS (" + select + ") THEN 1 ELSE 0 END FROM DUAL";
    }

    @Override
    public String upper(String expr) {
        return "UPPER(" + expr + ")";
    }

    @Override
    public String isTrue() {
        return "!= 0";
    }

    @Override
    public String isFalse() {
        return "= 0";
    }

    @Override
    public String quote(String name) {
        return "\"" + name + "\"";
    }
}
