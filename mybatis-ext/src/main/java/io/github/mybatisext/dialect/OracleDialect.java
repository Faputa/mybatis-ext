package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.List;

import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.util.StringUtils;

public class OracleDialect extends BaseDialect {

    @Override
    public String insert(TableInfo tableInfo, Variable variable, boolean batch, boolean ignoreNull) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + variable.getName() + "__item", variable.getJavaType().getTypeParameters()[0]);
            ss.add("<foreach Iterable=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"begin\" close=\"; end;\" separator=\";\">");
            ss.add(insert(tableInfo, itemVariable, false, ignoreNull));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        return buildSimpleInsert(tableInfo, variable, ignoreNull);
    }

    @Override
    public String update(TableInfo tableInfo, Variable variable, boolean batch, boolean ignoreNull, boolean join, String tableAndJoin, String where) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + variable.getName() + "__item", variable.getJavaType().getTypeParameters()[0]);
            ss.add("<foreach Iterable=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"begin\" close=\"; end;\" separator=\";\">");
            ss.add(update(tableInfo, itemVariable, false, ignoreNull, join, tableAndJoin, where));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        if (join) {
            ss.add("UPDATE (");
            ss.add("SELECT");
            ss.add(tableInfo.getJoinTableInfo().getAlias() + ".* FROM");
            ss.add(tableAndJoin);
            if (StringUtils.isNotBlank(where)) {
                ss.add(where);
            }
            ss.add(") x");
            ss.add(buildUpdateSet("x", tableInfo, variable, ignoreNull));
            return String.join(" ", ss);
        }
        return buildSimpleUpdate(tableInfo, variable, ignoreNull, tableAndJoin, where);
    }

    @Override
    public String delete(TableInfo tableInfo, Variable variable, boolean batch, boolean join, String tableAndJoin, String where) {
        List<String> ss = new ArrayList<>();
        if (batch) {
            Variable itemVariable = new Variable("__" + variable.getName() + "__item", variable.getJavaType().getTypeParameters()[0]);
            ss.add("<foreach Iterable=\"" + variable + "\" item=\"" + itemVariable + "\" open=\"begin\" close=\"; end;\" separator=\";\">");
            ss.add(delete(tableInfo, itemVariable, false, join, tableAndJoin, where));
            ss.add("</foreach>");
            return String.join(" ", ss);
        }
        if (join) {
            ss.add("DELETE FROM (");
            ss.add("SELECT");
            ss.add(tableInfo.getJoinTableInfo().getAlias() + ".* FROM");
            ss.add(tableAndJoin);
            if (StringUtils.isNotBlank(where)) {
                ss.add(where);
            }
            ss.add(") x");
            return String.join(" ", ss);
        }
        return buildSimpleDelete(tableAndJoin, where);
    }

    @Override
    public String limit(Limit limit, String select) {
        String startRow;
        String endRow;
        if (limit.getOffset() != null || limit.getOffsetVariable() != null) {
            startRow = limit.getOffset() != null ? limit.getOffset().toString() : "#{" + limit.getOffsetVariable() + "}";
            String offset = limit.getOffset() != null ? limit.getOffset().toString() : limit.getOffsetVariable().toString();
            String rowCount = limit.getRowCount() != null ? limit.getRowCount().toString() : limit.getRowCountVariable().toString();
            endRow = "<bind name=\"__endRow\" value=\"" + offset + " + " + rowCount + "\"/> #{__endRow}";
        } else {
            startRow = null;
            endRow = limit.getRowCount() != null ? limit.getRowCount().toString() : "#{" + limit.getRowCountVariable() + "}";
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
    public String exists(String select) {
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
}
