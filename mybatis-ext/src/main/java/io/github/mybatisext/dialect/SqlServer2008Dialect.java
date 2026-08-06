package io.github.mybatisext.dialect;

import java.util.ArrayList;
import java.util.List;

import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.jpa.Limit;
import io.github.mybatisext.jpa.OrderByElement;
import io.github.mybatisext.jpa.OrderByType;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

public class SqlServer2008Dialect extends SqlServerDialect {

    @Override
    public String select(TableInfo tableInfo, List<PropertyInfo> selectItems, Condition where, boolean distinct, List<OrderByElement> orderBy, List<PropertyInfo> groupBy, Condition having, Limit limit) {
        if (limit == null || limit.getOffset() == null && limit.getOffsetVariable() == null) {
            return super.select(tableInfo, selectItems, where, distinct, orderBy, groupBy, having, limit);
        }

        List<PropertyInfo> innerSelectItems = selectItems;
        if (!distinct && groupBy == null && orderBy != null) {
            innerSelectItems = new ArrayList<>(selectItems);
            List<String> selectedAliases = new ArrayList<>();
            collectColumnAliases(selectItems, selectedAliases);
            for (OrderByElement orderByElement : orderBy) {
                PropertyInfo propertyInfo = orderByElement.getPropertyInfo();
                String alias = quote(propertyInfo.getFullName());
                if (!selectedAliases.contains(alias)) {
                    innerSelectItems.add(propertyInfo);
                    selectedAliases.add(alias);
                }
            }
        }
        String innerSelect = super.select(tableInfo, innerSelectItems, where, distinct, null, groupBy, having, null);
        List<PropertyInfo> resultItems = groupBy != null ? groupBy : selectItems;
        List<String> resultAliases = new ArrayList<>();
        collectColumnAliases(resultItems, resultAliases);

        List<String> ss = new ArrayList<>();
        ss.add("SELECT");
        ss.add(String.join(", ", resultAliases));
        ss.add("FROM (SELECT *, ROW_NUMBER() OVER (");
        ss.add(buildRowNumberOrderBy(orderBy));
        ss.add(") AS __row_number FROM (");
        ss.add(innerSelect);
        ss.add(") x) x WHERE __row_number &gt;");
        ss.add(limitValue(limit.getOffset(), limit.getOffsetVariable()));
        ss.add("AND __row_number &lt;=");
        ss.add(buildEndRow(limit));
        ss.add("ORDER BY __row_number");
        return String.join(" ", ss);
    }

    private String buildRowNumberOrderBy(List<OrderByElement> orderBy) {
        if (orderBy == null) {
            return "ORDER BY (SELECT NULL)";
        }
        List<String> ss = new ArrayList<>();
        for (OrderByElement orderByElement : orderBy) {
            String item = quote(orderByElement.getPropertyInfo().getFullName());
            if (orderByElement.getType() == OrderByType.ASC) {
                item += " ASC";
            } else if (orderByElement.getType() == OrderByType.DESC) {
                item += " DESC";
            }
            ss.add(item);
        }
        return "ORDER BY " + String.join(", ", ss);
    }

    private void collectColumnAliases(Iterable<PropertyInfo> propertyInfos, List<String> columnAliases) {
        for (PropertyInfo propertyInfo : propertyInfos) {
            if (propertyInfo.isReadonly()) {
                continue;
            }
            if (propertyInfo.getColumnName() != null) {
                columnAliases.add(quote(propertyInfo.getFullName()));
            } else {
                collectColumnAliases(propertyInfo.getNameToPropertyInfo().values(), columnAliases);
            }
        }
    }

    private String buildEndRow(Limit limit) {
        if (limit.getOffset() != null && limit.getRowCount() != null) {
            return Integer.toString(limit.getOffset() + limit.getRowCount());
        }
        String offset = limit.getOffset() != null ? limit.getOffset().toString() : limit.getOffsetVariable().toString();
        String rowCount = limit.getRowCount() != null ? limit.getRowCount().toString() : limit.getRowCountVariable().toString();
        return "<bind name=\"__endRow\" value=\"" + offset + " + " + rowCount + "\"/> #{__endRow}";
    }

}
