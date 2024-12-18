package io.github.mybatisext.statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.condition.Condition;
import io.github.mybatisext.condition.ConditionComp;
import io.github.mybatisext.condition.ConditionTerm;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.OrderByElement;
import io.github.mybatisext.jpa.OrderByType;
import io.github.mybatisext.jpa.Semantic;
import io.github.mybatisext.jpa.SemanticType;
import io.github.mybatisext.metadata.JoinTableInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.ognl.Ognl;

public class SemanticScriptBuilder {

    private final Dialect dialect;

    public SemanticScriptBuilder(Dialect dialect) {
        this.dialect = dialect;
    }

    public String buildScript(Semantic semantic) {
        switch (semantic.getType()) {
            case COUNT:
                return buildCount(semantic);
            case EXISTS:
                return buildExists(semantic);
            case SELECT:
                return buildSelect(semantic);
            case DELETE:
                return buildDelete(semantic);
            case INSERT:
                return buildInsert(semantic);
            case UPDATE:
                return buildUpdate(semantic);
        }
        throw new MybatisExtException("Unsupported semantic type: " + semantic.getType());
    }

    private String buildUpdate(Semantic semantic) {
        String tableAndJoin = buildTableAndJoin(semantic);
        String where = semantic.getWhere() != null ? buildWhere(semantic.getWhere()) : null;
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(semantic);
        return "<script>" + dialect.update(
                semantic.getTableInfo(),
                semantic.getParameter(),
                Collection.class.isAssignableFrom(semantic.getParameter().getJavaType().getType()),
                semantic.isIgnoreNull(),
                joinTableInfos.size() > 1,
                tableAndJoin,
                where) + "</script>";
    }

    private String buildInsert(Semantic semantic) {
        return "<script>" + dialect.insert(
                semantic.getTableInfo(),
                semantic.getParameter(),
                Collection.class.isAssignableFrom(semantic.getParameter().getJavaType().getType()),
                semantic.isIgnoreNull()) + "</script>";
    }

    private String buildDelete(Semantic semantic) {
        String tableAndJoin = buildTableAndJoin(semantic);
        String where = semantic.getWhere() != null ? buildWhere(semantic.getWhere()) : null;
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(semantic);
        return "<script>" + dialect.delete(
                semantic.getTableInfo(),
                semantic.getParameter(),
                Collection.class.isAssignableFrom(semantic.getParameter().getJavaType().getType()),
                joinTableInfos.size() > 1,
                tableAndJoin,
                where) + "</script>";
    }

    private String buildSelect(Semantic semantic) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT");
        if (semantic.getGroupBy() != null) {
            ss.add(buildSelectItems(semantic.getGroupBy()));
        } else {
            ss.add(buildSelectItems(semantic.getTableInfo()));
        }
        ss.add("FROM");
        ss.add(buildTableAndJoin(semantic));
        if (semantic.getWhere() != null) {
            ss.add(buildWhere(semantic.getWhere()));
        }
        if (semantic.getGroupBy() != null) {
            ss.add(buildGroupBy(semantic.getGroupBy()));
            if (semantic.getHaving() != null) {
                ss.add(buildHaving(semantic.getHaving()));
            }
        }
        if (semantic.getOrderBy() != null) {
            ss.add(buildOrderBy(semantic.getOrderBy()));
        }
        if (semantic.getLimit() != null) {
            return "<script>" + dialect.limit(semantic.getLimit(), String.join(" ", ss)) + "</script>";
        }
        return "<script>" + String.join(" ", ss) + "</script>";
    }

    private String buildExists(Semantic semantic) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT 1 FROM");
        ss.add(buildTableAndJoin(semantic));
        if (semantic.getWhere() != null) {
            ss.add(buildWhere(semantic.getWhere()));
        }
        return "<script>" + dialect.exists(String.join(" ", ss)) + "</script>";
    }

    private String buildCount(Semantic semantic) {
        List<String> ss = new ArrayList<>();
        ss.add("SELECT COUNT(1) FROM");
        ss.add(buildTableAndJoin(semantic));
        if (semantic.getWhere() != null) {
            ss.add(buildWhere(semantic.getWhere()));
        }
        return "<script>" + String.join(" ", ss) + "</script>";
    }

    private String buildWhere(Condition condition) {
        List<String> ss = new ArrayList<>();
        if (condition instanceof ConditionTerm) {
            if (condition.getTest() == IfTest.NotNull) {
                ss.add("<if test=\"" + condition.getVariable() + " != null\">");
                ss.add("WHERE");
                ss.add(condition.toScriptlet(dialect));
                ss.add("</if>");
            } else if (condition.getTest() == IfTest.NotEmpty) {
                ss.add("<if test=\"" + Ognl.IsNotEmpty + "(" + condition.getVariable() + ")\">");
                ss.add("WHERE");
                ss.add(condition.toScriptlet(dialect));
                ss.add("</if>");
            } else {
                ss.add("WHERE");
                ss.add(condition.toScriptlet(dialect));
            }
            return String.join(" ", ss);
        }
        if (condition instanceof ConditionComp) {
            ConditionComp conditionComp = (ConditionComp) condition;
            if (!conditionComp.hasTest()) {
                for (Condition c : conditionComp.getConditions()) {
                    ss.add(c.toScriptlet(dialect));
                }
                return "WHERE " + String.join(" " + conditionComp.getRel() + " ", ss);
            }
            ss.add("<where>");
            for (Condition c : conditionComp.getConditions()) {
                if (c.getTest() == IfTest.NotNull) {
                    ss.add("<if test=\"" + c.getVariable() + " != null\">");
                    ss.add(conditionComp.getRel().toString());
                    ss.add(c.toScriptlet(dialect));
                    ss.add("</if>");
                } else if (c.getTest() == IfTest.NotEmpty) {
                    ss.add("<if test=\"" + Ognl.IsNotEmpty + "(" + c.getVariable() + ")\">");
                    ss.add(conditionComp.getRel().toString());
                    ss.add(c.toScriptlet(dialect));
                    ss.add("</if>");
                } else {
                    ss.add(conditionComp.getRel().toString());
                    ss.add(c.toScriptlet(dialect));
                }
            }
            ss.add("</where>");
            return String.join(" ", ss);
        }
        return null;
    }

    private String buildHaving(Condition condition) {
        List<String> ss = new ArrayList<>();
        if (condition instanceof ConditionTerm) {
            if (condition.getTest() == IfTest.NotNull) {
                ss.add("<if test=\"" + condition.getVariable() + " != null\">");
                ss.add("HAVING");
                ss.add(condition.toScriptlet(dialect));
                ss.add("</if>");
            } else if (condition.getTest() == IfTest.NotEmpty) {
                ss.add("<if test=\"" + Ognl.IsNotEmpty + "(" + condition.getVariable() + ")\">");
                ss.add("HAVING");
                ss.add(condition.toScriptlet(dialect));
                ss.add("</if>");
            } else {
                ss.add("HAVING");
                ss.add(condition.toScriptlet(dialect));
            }
            return String.join(" ", ss);
        }
        if (condition instanceof ConditionComp) {
            ConditionComp conditionComp = (ConditionComp) condition;
            if (!conditionComp.hasTest()) {
                for (Condition c : conditionComp.getConditions()) {
                    ss.add(c.toScriptlet(dialect));
                }
                return "HAVING " + String.join(" " + conditionComp.getRel() + " ", ss);
            }
            ss.add("<trim prefix=\"HAVING\" prefixOverrides=\"" + conditionComp.getRel() + "\" >");
            for (Condition c : conditionComp.getConditions()) {
                if (c.getTest() == IfTest.NotNull) {
                    ss.add("<if test=\"" + c.getVariable() + " != null\">");
                    ss.add(conditionComp.getRel().toString());
                    ss.add(c.toScriptlet(dialect));
                    ss.add("</if>");
                } else if (c.getTest() == IfTest.NotEmpty) {
                    ss.add("<if test=\"" + Ognl.IsNotEmpty + "(" + condition.getVariable() + ")\">");
                    ss.add(conditionComp.getRel().toString());
                    ss.add(c.toScriptlet(dialect));
                    ss.add("</if>");
                } else {
                    ss.add(conditionComp.getRel().toString());
                    ss.add(c.toScriptlet(dialect));
                }
            }
            ss.add("</trim>");
            return String.join(" ", ss);
        }
        return null;
    }

    private String buildTableAndJoin(Semantic semantic) {
        List<String> ss = new ArrayList<>();
        List<JoinTableInfo> joinTableInfos = collectJoinTableInfo(semantic);
        ss.add(joinTableInfos.get(0).getTableInfo().getName());
        ss.add("AS");
        ss.add(joinTableInfos.get(0).getAlias());
        for (int i = 1; i < joinTableInfos.size(); i++) {
            JoinTableInfo joinTableInfo = joinTableInfos.get(i);
            ss.add("LEFT JOIN");
            ss.add(joinTableInfo.getTableInfo().getName());
            ss.add("AS");
            ss.add(joinTableInfo.getAlias());
            ss.add("ON");
            List<String> conditions = new ArrayList<>();
            joinTableInfo.getLeftJoinTableInfos().forEach(((joinColumnInfo, leftJoinTableInfo) -> {
                conditions.add(leftJoinTableInfo.getAlias() + "." + joinColumnInfo.getLeftColumn() + " = " + joinTableInfo.getAlias() + "." + joinColumnInfo.getRightColumn());
            }));
            ss.add(String.join(" AND ", conditions));
        }
        return String.join(" ", ss);
    }

    private String buildSelectItems(TableInfo tableInfo) {
        List<String> selectItems = new ArrayList<>();
        tableInfo.getAliasToJoinTableInfo().forEach((alias, joinTableInfo) -> {
            joinTableInfo.getTableInfo().getNameToColumnInfo().forEach((name, columnInfo) -> {
                if (!columnInfo.isReadonly()) {
                    selectItems.add(alias + "." + name + " AS " + alias + "_" + name);
                }
            });
        });
        return String.join(", ", selectItems);
    }

    private String buildSelectItems(List<PropertyInfo> propertyInfos) {
        List<String> selectItems = new ArrayList<>();
        for (PropertyInfo propertyInfo : propertyInfos) {
            if (propertyInfo.getColumnName() != null) {
                if (!propertyInfo.getJoinTableInfo().getTableInfo().getNameToColumnInfo().get(propertyInfo.getColumnName()).isReadonly()) {
                    return propertyInfo.getJoinTableInfo().getAlias() + "." + propertyInfo.getColumnName() + " " + propertyInfo.getJoinTableInfo().getAlias() + "_" + propertyInfo.getColumnName();
                }
            } else {
                selectItems.add(buildSelectItems(propertyInfo.getSubPropertyInfos()));
            }
        }
        return String.join(", ", selectItems);
    }

    private String buildGroupBy(List<PropertyInfo> groupBy) {
        List<String> columns = new ArrayList<>();
        for (PropertyInfo propertyInfo : groupBy) {
            columns.add(propertyInfo.getJoinTableInfo().getAlias() + "." + propertyInfo.getColumnName());
        }
        return "GROUP BY " + String.join(", ", columns);
    }

    private String buildOrderBy(List<OrderByElement> orderBy) {
        List<String> ss = new ArrayList<>();
        for (OrderByElement orderByElement : orderBy) {
            String s = orderByElement.getPropertyInfo().getJoinTableInfo().getAlias() + "." + orderByElement.getPropertyInfo().getColumnName();
            if (OrderByType.ASC == orderByElement.getType()) {
                s += " ASC";
            } else if (OrderByType.DESC == orderByElement.getType()) {
                s += " DESC";
            }
            ss.add(s);
        }
        return "ORDER BY " + String.join(", ", ss);
    }

    private List<JoinTableInfo> collectJoinTableInfo(Semantic semantic) {
        Set<String> directAliases = new HashSet<>();
        if (semantic.getType() == SemanticType.SELECT) {
            directAliases.addAll(semantic.getTableInfo().getAliasToJoinTableInfo().keySet());
        } else {
            directAliases.add(semantic.getTableInfo().getJoinTableInfo().getAlias());
            if (semantic.getWhere() != null) {
                semantic.getWhere().collectDirectTableAliases(directAliases);
            }
            if (semantic.getGroupBy() != null) {
                for (PropertyInfo propertyInfo : semantic.getGroupBy()) {
                    directAliases.add(propertyInfo.getJoinTableInfo().getAlias());
                }
            }
            if (semantic.getOrderBy() != null) {
                for (OrderByElement orderByElement : semantic.getOrderBy()) {
                    directAliases.add(orderByElement.getPropertyInfo().getJoinTableInfo().getAlias());
                }
            }
        }
        LinkedHashSet<String> orderAliases = new LinkedHashSet<>();
        for (String alias : directAliases) {
            semantic.getTableInfo().getAliasToJoinTableInfo().get(alias).collectTableAliases(orderAliases);
        }
        return orderAliases.stream().map(v -> semantic.getTableInfo().getAliasToJoinTableInfo().get(v)).collect(Collectors.toList());
    }
}
