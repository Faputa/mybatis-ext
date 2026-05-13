package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Filterable;
import io.github.mybatisext.annotation.OnlyById;
import io.github.mybatisext.annotation.TestMode;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.FilterableInfo;
import io.github.mybatisext.metadata.JoinColumnInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericParameter;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.statement.NestedSelectHelper;
import io.github.mybatisext.util.StringUtils;
import io.github.mybatisext.util.TypeUtils;

public class JpaParser extends BaseParser<JpaTokenizer> {

    private final Configuration configuration;
    private final ExtContext extContext;

    private final Symbol grammar = new Symbol("grammar");
    private final Symbol conditionList = new Symbol("conditionList");
    private final Symbol condition = new Symbol("condition");
    private final Symbol propertyList = new Symbol("propertyList");
    private final Symbol orderByList = new Symbol("orderByList");
    private final Symbol limit = new Symbol("limit");

    private final Symbol propertyName = new Symbol("propertyName").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        int cursor = jpaTokenizer.getCursor();
        List<PropertyInfo> propertyInfos = jpaTokenizer.property();
        for (PropertyInfo propertyInfo : propertyInfos) {
            jpaTokenizer.setCursor(cursor + propertyInfo.getName().length());
            jpaTokenizer.getTokenMarker().record(jpaTokenizer.getCursor());
            if (state.setResult(propertyInfo) && continuation.test(state)) {
                return true;
            }
            jpaTokenizer.setCursor(cursor);
        }
        jpaTokenizer.getExpectedTokens().record(cursor, "propertyName");
        return false;
    });

    private final Symbol subPropertyName = new Symbol("subPropertyName").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        int cursor = jpaTokenizer.getCursor();
        List<PropertyInfo> propertyInfos = jpaTokenizer.property((PropertyInfo) state.getResult());
        for (PropertyInfo propertyInfo : propertyInfos) {
            jpaTokenizer.setCursor(cursor + propertyInfo.getName().length());
            jpaTokenizer.getTokenMarker().record(jpaTokenizer.getCursor());
            if (state.setResult(propertyInfo) && continuation.test(state)) {
                return true;
            }
            jpaTokenizer.setCursor(cursor);
        }
        jpaTokenizer.getExpectedTokens().record(cursor, "subPropertyName");
        return false;
    });

    private final Symbol integer = new Symbol("integer").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        int cursor = jpaTokenizer.getCursor();
        int i = jpaTokenizer.integer();
        if (i < 0) {
            jpaTokenizer.getExpectedTokens().record(cursor, "integer");
            return false;
        }
        jpaTokenizer.getTokenMarker().record(jpaTokenizer.getCursor());
        return state.setResult(i) && continuation.test(state);
    });

    private final Symbol variableName = new Symbol("variableName").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        int cursor = jpaTokenizer.getCursor();
        List<Variable> variables = jpaTokenizer.variable();
        for (Variable v : variables) {
            jpaTokenizer.setCursor(cursor + v.getName().length());
            jpaTokenizer.getTokenMarker().record(jpaTokenizer.getCursor());
            if (state.setResult(v) && continuation.test(state)) {
                return true;
            }
            jpaTokenizer.setCursor(cursor);
        }
        jpaTokenizer.getExpectedTokens().record(cursor, "variableName");
        return false;
    });

    private final Symbol subVariableName = new Symbol("subVariableName").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        int cursor = jpaTokenizer.getCursor();
        List<Variable> variables = jpaTokenizer.variable((Variable) state.getResult());
        for (Variable v : variables) {
            jpaTokenizer.setCursor(cursor + v.getName().length());
            jpaTokenizer.getTokenMarker().record(jpaTokenizer.getCursor());
            if (state.setResult(v) && continuation.test(state)) {
                return true;
            }
            jpaTokenizer.setCursor(cursor);
        }
        jpaTokenizer.getExpectedTokens().record(cursor, "subVariableName");
        return false;
    });

    private final Symbol end = new Symbol("end").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        if (jpaTokenizer.getCursor() != jpaTokenizer.getText().length()) {
            jpaTokenizer.getExpectedTokens().record(jpaTokenizer.getCursor(), "end");
            return false;
        }
        return continuation.test(state);
    });

    private Symbol keyword(String s) {
        return assign(s, new Symbol("keyword(" + s + ")").set((state, continuation) -> {
            JpaTokenizer jpaTokenizer = state.getTokenizer();
            int cursor = jpaTokenizer.getCursor();
            if (jpaTokenizer.keyword(s).isEmpty()) {
                jpaTokenizer.getExpectedTokens().record(cursor, "'" + s + "'");
                return false;
            }
            jpaTokenizer.getTokenMarker().record(jpaTokenizer.getCursor());
            return continuation.test(state);
        }));
    }

    private final Symbol returnTypeName = new Symbol("returnTypeName").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        GenericType returnType = TypeUtils.unwrapToGenericType(jpaTokenizer.getReturnType());
        int cursor = jpaTokenizer.getCursor();
        if (jpaTokenizer.keyword(returnType.getSimpleName()).isEmpty()) {
            jpaTokenizer.getExpectedTokens().record(cursor, "'" + returnType.getSimpleName() + "'");
            return false;
        }
        jpaTokenizer.getTokenMarker().record(jpaTokenizer.getCursor());
        return continuation.test(state);
    });

    private final Symbol property = new Symbol("property").set(join(propertyName, star(join(keyword("Dot"), subPropertyName))));
    private final Symbol variable = new Symbol("variable").set(join(variableName, star(join(keyword("Dot"), subVariableName))));
    private final Symbol integerB = new Symbol("integerB").set(integer);
    private final Symbol variableB = new Symbol("variableB").set(variable);

    private Symbol conditionAction(CompareOperator compareOperator) {
        return action(state -> {
            Condition condition = new Condition(ConditionType.BASIC);
            condition.setPropertyInfos(state.getTokenizer().getTableInfo().getNameToPropertyInfo());
            condition.setPropertyInfo(state.getMatch(property).val());
            condition.setCompareOperator(compareOperator);
            if (state.getMatch("Ignorecase") != null) {
                condition.setIgnorecase(true);
            }
            if (state.getMatch("Not") != null) {
                condition.setNot(true);
            }
            if (compareOperator.isRequiredSecondVariable()) {
                MatchResult _variable = state.getMatch(variable);
                if (_variable != null) {
                    condition.setVariable(_variable.val());
                    condition.setSecondVariable(state.getMatch(variableB).val());
                }
            } else if (compareOperator.isRequiredVariable()) {
                MatchResult _variable = state.getMatch(variable);
                if (_variable != null) {
                    condition.setVariable(_variable.val());
                } else {
                    for (Variable variable : state.getTokenizer().getVariables()) {
                        if (condition.getPropertyInfo().getName().equals(variable.getName())) {
                            condition.setVariable(variable);
                            break;
                        }
                    }
                }
            }
            state.setReturn(condition);
        });
    }

    private final Symbol groupBy = new Symbol("groupBy").set(join(keyword("GroupBy"), propertyList));
    private final Symbol having = new Symbol("having").set(join(keyword("Having"), conditionList));
    private final Symbol orderBy = new Symbol("orderBy").set(join(keyword("OrderBy"), orderByList));

    public JpaParser(Configuration configuration, ExtContext extContext) {
        this.configuration = configuration;
        this.extContext = extContext;

        grammar.set(choice(
                join(choice(keyword("find"), keyword("select"), keyword("list"), keyword("get")), optional(keyword("Distinct")), optional(choice(keyword("All"), keyword("One"), join(keyword("Top"), choice(integer, variable)))), optional(choice(propertyList, returnTypeName)), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), optional(join(groupBy, optional(having))), optional(orderBy), optional(limit), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.SELECT);
                    MatchResult _propertyList = state.getMatch(propertyList);
                    if (_propertyList != null) {
                        semantic.setSelectItems(ensureJoinRelationColumns(state.getTokenizer(), _propertyList.val()));
                    } else {
                        semantic.setSelectItems(buildDefaultSelectItems(state.getTokenizer()));
                    }
                    if (state.getMatch("Distinct") != null) {
                        semantic.setDistinct(true);
                    }
                    if (state.getMatch("Top") != null) {
                        Limit limit = new Limit();
                        MatchResult _integer = state.getMatch(integer);
                        if (_integer != null) {
                            limit.setRowCount(_integer.val());
                        } else {
                            limit.setRowCountVariable(state.getMatch(variable).val());
                        }
                        semantic.setLimit(limit);
                    } else if (state.getMatch("One") != null) {
                        Limit limit = new Limit();
                        limit.setRowCount(1);
                        semantic.setLimit(limit);
                    }
                    Set<String> usedParamNames = new HashSet<>();
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        usedParamNames.addAll(collectUsedParamNames(_conditionList.<ConditionList>val()));
                        semantic.setWhere(ensureConditionVariable(state.getTokenizer(), usedParamNames, _conditionList.val()));
                    }
                    MatchResult _groupBy = state.getMatch(groupBy);
                    if (_groupBy != null) {
                        semantic.setGroupBy(_groupBy.val());
                        MatchResult _having = state.getMatch(having);
                        if (_having != null) {
                            usedParamNames.addAll(collectUsedParamNames(_having.<ConditionList>val()));
                            semantic.setHaving(ensureConditionVariable(state.getTokenizer(), usedParamNames, _having.val()));
                        }
                    }
                    MatchResult _orderBy = state.getMatch(orderBy);
                    if (_orderBy != null) {
                        semantic.setOrderBy(_orderBy.val());
                    }
                    MatchResult _limit = state.getMatch(limit);
                    if (_limit != null) {
                        semantic.setLimit(_limit.val());
                    }
                    if (semantic.getLimit() != null) {
                        usedParamNames.addAll(collectUsedParamNames(semantic.getLimit()));
                    }
                    if (hasUnusedParam(state.getTokenizer(), usedParamNames)) {
                        if (_groupBy != null) {
                            if (semantic.getHaving() == null) {
                                semantic.setHaving(buildDefaultCondition(state.getTokenizer(), usedParamNames));
                            }
                        } else {
                            if (semantic.getWhere() == null) {
                                semantic.setWhere(buildDefaultCondition(state.getTokenizer(), usedParamNames));
                            }
                        }
                    }
                    state.setReturn(semantic);
                })),
                join(keyword("exists"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.EXISTS);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setWhere(ensureConditionVariable(state.getTokenizer(), collectUsedParamNames(_conditionList.<ConditionList>val()), _conditionList.val()));
                    } else {
                        semantic.setWhere(buildDefaultCondition(state.getTokenizer(), new HashSet<>()));
                    }
                    state.setReturn(semantic);
                })),
                join(keyword("count"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.COUNT);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setWhere(ensureConditionVariable(state.getTokenizer(), collectUsedParamNames(_conditionList.<ConditionList>val()), _conditionList.val()));
                    } else {
                        semantic.setWhere(buildDefaultCondition(state.getTokenizer(), new HashSet<>()));
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("update"), keyword("modify")), optional(keyword("Batch")), optional(propertyList), optional(keyword("IgnoreNull")), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.UPDATE);
                    // TODO 考虑支持updateSet
                    MatchResult _propertyList = state.getMatch(propertyList);
                    if (_propertyList != null) {
                        semantic.setSelectItems(ensureJoinRelationColumns(state.getTokenizer(), _propertyList.val()));
                    } else {
                        semantic.setSelectItems(state.getTokenizer().getTableInfo().getNameToPropertyInfo().values().stream().filter(PropertyInfo::isOwnColumn).collect(Collectors.toList()));
                    }
                    semantic.setParameter(buildSemanticParameter(state.getTokenizer(), true));
                    if (state.getMatch("IgnoreNull") != null) {
                        semantic.setIgnoreNull(true);
                    }
                    Condition condition;
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        condition = ensureConditionVariable(state.getTokenizer(), collectUsedParamNames(_conditionList.<ConditionList>val()), _conditionList.val());
                    } else {
                        condition = buildDefaultCondition(state.getTokenizer(), new HashSet<>());
                    }
                    if (condition == null) {
                        throw new MybatisExtException("Condition cannot be null for update: " + state.getTokenizer().getText());
                    }
                    semantic.setWhere(condition);
                    state.setReturn(semantic);
                })),
                join(choice(keyword("delete"), keyword("remove")), optional(keyword("Batch")), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.DELETE);
                    semantic.setParameter(buildSemanticParameter(state.getTokenizer(), false));
                    Condition condition;
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        condition = ensureConditionVariable(state.getTokenizer(), collectUsedParamNames(_conditionList.<ConditionList>val()), _conditionList.val());
                    } else {
                        condition = buildDefaultCondition(state.getTokenizer(), new HashSet<>());
                    }
                    if (condition == null) {
                        throw new MybatisExtException("Condition cannot be null for delete: " + state.getTokenizer().getText());
                    }
                    semantic.setWhere(condition);
                    state.setReturn(semantic);
                })),
                join(choice(keyword("save"), keyword("insert")), optional(keyword("Batch")), optional(keyword("IgnoreNull")), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.INSERT);
                    semantic.setParameter(buildSemanticParameter(state.getTokenizer(), true));
                    if (state.getMatch("IgnoreNull") != null) {
                        semantic.setIgnoreNull(true);
                    }
                    state.setReturn(semantic);
                }))));

        conditionList.set(
                join(condition, optional(join(choice(keyword("And"), keyword("Or")), conditionList)), action(state -> {
                    Condition c = state.getMatch(condition).val();
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        if (state.getMatch("And") != null) {
                            state.setReturn(new ConditionList(c, _conditionList.val(), LogicalOperator.AND));
                        } else {
                            state.setReturn(new ConditionList(c, _conditionList.val(), LogicalOperator.OR));
                        }
                    } else {
                        state.setReturn(new ConditionList(c));
                    }
                })));

        condition.set(join(property, choice(
                join(optional(keyword("Ignorecase")), optional(keyword("Not")), choice(
                        join(keyword("Is"), optional(variable), conditionAction(CompareOperator.Equals)),
                        join(keyword("Equals"), optional(variable), conditionAction(CompareOperator.Equals)),
                        join(keyword("LessThan"), optional(variable), conditionAction(CompareOperator.LessThan)),
                        join(keyword("LessThanEqual"), optional(variable), conditionAction(CompareOperator.LessThanEqual)),
                        join(keyword("GreaterThan"), optional(variable), conditionAction(CompareOperator.GreaterThan)),
                        join(keyword("GreaterThanEqual"), optional(variable), conditionAction(CompareOperator.GreaterThanEqual)),
                        join(keyword("Like"), optional(variable), conditionAction(CompareOperator.Like)),
                        join(keyword("StartWith"), optional(variable), conditionAction(CompareOperator.StartWith)),
                        join(keyword("EndWith"), optional(variable), conditionAction(CompareOperator.EndWith)),
                        join(keyword("Between"), optional(join(variable, keyword("To"), variableB)), conditionAction(CompareOperator.Between)),
                        join(keyword("In"), optional(variable), conditionAction(CompareOperator.In)),
                        conditionAction(CompareOperator.Equals))),
                join(optional(keyword("Not")), choice(
                        join(keyword("IsNull"), conditionAction(CompareOperator.IsNull)),
                        join(keyword("IsNotNull"), conditionAction(CompareOperator.IsNotNull)),
                        join(keyword("IsTrue"), conditionAction(CompareOperator.IsTrue)),
                        join(keyword("IsFalse"), conditionAction(CompareOperator.IsFalse)))))));

        orderByList.set(
                join(property, optional(choice(keyword("Asc"), keyword("Desc"))), optional(join(keyword("And"), orderByList)), action(state -> {
                    OrderByElement orderByElement = new OrderByElement();
                    orderByElement.setPropertyInfo(state.getMatch(property).val());
                    if (state.getMatch("Asc") != null) {
                        orderByElement.setType(OrderByType.ASC);
                    } else if (state.getMatch("Desc") != null) {
                        orderByElement.setType(OrderByType.DESC);
                    }
                    List<OrderByElement> orderByElements = new ArrayList<>();
                    orderByElements.add(orderByElement);
                    MatchResult _orderByList = state.getMatch(orderByList);
                    if (_orderByList != null) {
                        orderByElements.addAll(_orderByList.val());
                    }
                    state.setReturn(orderByElements);
                })));

        limit.set(join(keyword("Limit"), choice(
                choice(integer, variable), keyword("To"), choice(integerB, variableB), action(state -> {
                    Limit limit = new Limit();
                    MatchResult _integer = state.getMatch(integer);
                    if (_integer != null) {
                        limit.setOffset(_integer.val());
                    } else {
                        limit.setOffsetVariable(state.getMatch(variable).val());
                    }
                    MatchResult _integerB = state.getMatch(integerB);
                    if (_integerB != null) {
                        limit.setRowCount(_integerB.val());
                    } else {
                        limit.setRowCountVariable(state.getMatch(variableB).val());
                    }
                    state.setReturn(limit);
                }),
                choice(integer, variable), action(state -> {
                    Limit limit = new Limit();
                    state.setReturn(limit);
                    MatchResult _integer = state.getMatch(integer);
                    if (_integer != null) {
                        limit.setRowCount(_integer.val());
                    } else {
                        limit.setRowCountVariable(state.getMatch(variable).val());
                    }
                }))));

        propertyList.set(
                join(property, star(join(keyword("And"), property)), action(state -> {
                    List<PropertyInfo> propertyInfos = new ArrayList<>();
                    List<MatchResult> matches = state.getMatches(property);
                    for (MatchResult match : matches) {
                        propertyInfos.add(match.val());
                    }
                    state.setReturn(propertyInfos);
                })));
    }

    private List<PropertyInfo> buildDefaultSelectItems(JpaTokenizer jpaTokenizer) {
        GenericType returnType = TypeUtils.unwrapToGenericType(jpaTokenizer.getReturnType());
        if (!TableInfoFactory.isSameTableType(returnType, jpaTokenizer.getTableInfo().getClassType())) {
            throw new MybatisExtException("Incompatible return type: " + returnType.getTypeName() + ", expected: " + jpaTokenizer.getTableInfo().getClassType().getName());
        }
        TableInfo tableInfo = TableInfoFactory.buildTableInfo(returnType, configuration, extContext);
        List<PropertyInfo> propertyInfos = tableInfo.getNameToPropertyInfo().values().stream().filter(v -> v.getFetchType() == null).collect(Collectors.toList());
        return ensureJoinRelationColumns(jpaTokenizer, propertyInfos);
    }

    private List<PropertyInfo> ensureJoinRelationColumns(JpaTokenizer jpaTokenizer, List<PropertyInfo> propertyInfos) {
        TableInfo tableInfo = jpaTokenizer.getTableInfo();
        Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();
        for (PropertyInfo propertyInfo : propertyInfos) {
            nameToPropertyInfo.put(propertyInfo.getFullName(), propertyInfo);
            if (propertyInfo.getFetchType() != null) {
                List<JoinColumnInfo> joinColumnInfos = NestedSelectHelper.buildLeftmostJoinColumns(propertyInfo);
                for (JoinColumnInfo joinColumnInfo : joinColumnInfos) {
                    PropertyInfo leftPropertyInfo = TableInfoFactory.getPropertyInfo(tableInfo, joinColumnInfo.getLeftFullName());
                    nameToPropertyInfo.put(leftPropertyInfo.getFullName(), leftPropertyInfo);
                }
            }
        }
        return new ArrayList<>(nameToPropertyInfo.values());
    }

    private Set<String> collectUsedParamNames(Limit limit) {
        Set<String> set = new HashSet<>();
        if (limit.getOffsetVariable() != null) {
            set.add(limit.getOffsetVariable().getFullName().split("\\.")[0]);
        }
        if (limit.getRowCountVariable() != null) {
            set.add(limit.getRowCountVariable().getFullName().split("\\.")[0]);
        }
        return set;
    }

    private Set<String> collectUsedParamNames(ConditionList conditionList) {
        Set<String> set = new HashSet<>();
        for (ConditionList list = conditionList; list != null; list = list.getTailList()) {
            Condition condition = list.getCondition();
            if (condition.getVariable() != null) {
                set.add(condition.getVariable().getFullName().split("\\.")[0]);
            }
            if (condition.getSecondVariable() != null) {
                set.add(condition.getSecondVariable().getFullName().split("\\.")[0]);
            }
        }
        return set;
    }

    private boolean hasUnusedParam(JpaTokenizer jpaTokenizer, Set<String> usedParamNames) {
        if (jpaTokenizer.getParameters().length == 1 && usedParamNames.isEmpty()) {
            return true;
        }
        for (GenericParameter parameter : jpaTokenizer.getParameters()) {
            Param param = parameter.getAnnotation(Param.class);
            if (param != null && !usedParamNames.contains(param.value())) {
                return true;
            }
        }
        return false;
    }

    private Variable buildSemanticParameter(JpaTokenizer jpaTokenizer, boolean requiredTableParameter) {
        GenericParameter[] parameters = jpaTokenizer.getParameters();
        if (parameters.length == 0) {
            throw new MybatisExtException("No parameters provided in the query.");
        }
        GenericType tableClass = jpaTokenizer.getTableInfo().getClassType();
        GenericType parameterType = parameters[0].getGenericType();
        Param param = parameters[0].getAnnotation(Param.class);
        if (parameterType.isArray() && TableInfoFactory.isSameTableType(tableClass, parameterType.getComponentType())) {
            return new Variable(param != null ? param.value() : (parameters.length == 1 ? "array" : "param1"), parameterType);
        }
        if (Collection.class.isAssignableFrom(parameterType.getType()) && TableInfoFactory.isSameTableType(tableClass, TypeUtils.unwrapToGenericType(parameterType))) {
            if (List.class.isAssignableFrom(parameterType.getType())) {
                return new Variable(param != null ? param.value() : (parameters.length == 1 ? "list" : "param1"), parameterType);
            }
            return new Variable(param != null ? param.value() : (parameters.length == 1 ? "collection" : "param1"), parameterType);
        }
        if (TableInfoFactory.isSameTableType(tableClass, parameterType)) {
            return new Variable(param != null ? param.value() : (parameters.length == 1 ? "" : "param1"), parameterType);
        }
        if (requiredTableParameter) {
            throw new MybatisExtException("Invalid parameter type. Expected: " + tableClass + ", but was: " + parameterType);
        }
        if (param != null && parameters.length == 1) {
            if (parameterType.isArray() && configuration.getTypeHandlerRegistry().hasTypeHandler(parameterType.getComponentType().getType())) {
                return new Variable(param.value(), parameterType);
            }
            if (Collection.class.isAssignableFrom(parameterType.getType()) && configuration.getTypeHandlerRegistry().hasTypeHandler(TypeUtils.unwrapToClass(parameterType))) {
                return new Variable(param.value(), parameterType);
            }
        }
        return null;
    }

    private Condition buildDefaultCondition(JpaTokenizer jpaTokenizer, Set<String> usedParamNames) {
        GenericParameter[] parameters = jpaTokenizer.getParameters();
        if (parameters.length == 1) {
            return buildSingleParamCondition(jpaTokenizer, usedParamNames, parameters[0]);
        }
        return buildMultiParamCondition(jpaTokenizer, usedParamNames, parameters);
    }

    private Condition buildSingleParamCondition(JpaTokenizer jpaTokenizer, Set<String> usedParamNames, GenericParameter parameter) {
        TableInfo tableInfo = jpaTokenizer.getTableInfo();
        List<Variable> variables = jpaTokenizer.getVariables();
        GenericType tableClass = tableInfo.getClassType();
        GenericType parameterType = parameter.getGenericType();
        Param param = parameter.getAnnotation(Param.class);
        String paramName;
        if (parameterType.isArray() && TableInfoFactory.isSameTableType(tableClass, parameterType.getComponentType())) {
            paramName = param != null ? "__" + param.value() + "__item" : "__array__item";
        } else if (Collection.class.isAssignableFrom(parameterType.getType()) && TableInfoFactory.isSameTableType(tableClass, TypeUtils.unwrapToGenericType(parameterType))) {
            paramName = param != null ? "__" + param.value() + "__item" : "__collection__item";
            if (List.class.isAssignableFrom(parameterType.getType())) {
                paramName = param != null ? "__" + param.value() + "__item" : "__list__item";
            }
        } else if (TableInfoFactory.isSameTableType(tableClass, parameterType)) {
            paramName = param != null ? param.value() : "";
        } else if (param != null) {
            Variable variable;
            if (parameterType.isArray() && configuration.getTypeHandlerRegistry().hasTypeHandler(parameterType.getComponentType().getType())) {
                variable = new Variable("__" + param.value() + "__item", parameterType.getComponentType());
            } else if (Collection.class.isAssignableFrom(parameterType.getType()) && configuration.getTypeHandlerRegistry().hasTypeHandler(TypeUtils.unwrapToClass(parameterType))) {
                variable = new Variable("__" + param.value() + "__item", TypeUtils.unwrapToGenericType(parameterType));
            } else {
                return buildMultiParamCondition(jpaTokenizer, usedParamNames, parameter);
            }
            Condition condition = new Condition(ConditionType.BASIC);
            condition.setPropertyInfos(tableInfo.getNameToPropertyInfo());
            condition.setPropertyInfo(parseProperty(tableInfo, param.value()));
            condition.setVariable(variable);
            applyFilterableInfo(condition, buildFilterableInfo(parameter.getAnnotation(Filterable.class)), variables, usedParamNames);
            return ConditionHelper.simplifyCondition(condition);
        } else {
            return buildMultiParamCondition(jpaTokenizer, usedParamNames, parameter);
        }
        OnlyById onlyById = parameter.getAnnotation(OnlyById.class);
        Condition condition = ConditionHelper.buildForTableInfo(tableInfo, onlyById != null, paramName);
        Filterable filterable = parameter.getAnnotation(Filterable.class);
        if (onlyById == null && filterable != null) {
            FilterableInfo filterableInfo = buildFilterableInfo(filterable);
            applyFilterableInfo(condition, filterableInfo, variables, usedParamNames);
            condition.setVariable(new Variable(StringUtils.isNotBlank(paramName) ? paramName : "param1", tableClass));
        }
        return ConditionHelper.simplifyCondition(condition);
    }

    private Condition buildMultiParamCondition(JpaTokenizer jpaTokenizer, Set<String> usedParamNames, GenericParameter... parameters) {
        TableInfo tableInfo = jpaTokenizer.getTableInfo();
        List<Variable> variables = jpaTokenizer.getVariables();
        List<Condition> conditions = new ArrayList<>();
        for (GenericParameter parameter : parameters) {
            Param param = parameter.getAnnotation(Param.class);
            if (param != null && usedParamNames.contains(param.value())) {
                continue;
            }
            if (TableInfoFactory.isSameTableType(TypeUtils.unwrapToGenericType(parameter.getGenericType()), tableInfo.getClassType())) {
                continue;
            }
            if (param == null || !configuration.getTypeHandlerRegistry().hasTypeHandler(parameter.getType())) {
                throw new MybatisExtException("Unsupported parameter type: " + parameter.getType().getName() + ". Method: " + jpaTokenizer.getText());
            }
            Condition condition = new Condition(ConditionType.BASIC);
            condition.setPropertyInfos(tableInfo.getNameToPropertyInfo());
            condition.setPropertyInfo(parseProperty(tableInfo, param.value()));
            condition.setVariable(new Variable(param.value(), parameter.getGenericType()));
            Filterable filterable = parameter.getAnnotation(Filterable.class);
            FilterableInfo filterableInfo = buildFilterableInfo(filterable);
            applyFilterableInfo(condition, filterableInfo, variables, usedParamNames);
            conditions.add(condition);
        }
        if (conditions.isEmpty()) {
            return null;
        }
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.getSubConditions().addAll(conditions);
        condition.setLogicalOperator(LogicalOperator.AND);
        condition.setPropertyInfos(jpaTokenizer.getTableInfo().getNameToPropertyInfo());
        return ConditionHelper.simplifyCondition(condition);
    }

    private FilterableInfo buildFilterableInfo(@Nullable Filterable filterable) {
        if (filterable == null) {
            FilterableInfo filterableInfo = new FilterableInfo();
            filterableInfo.setTestMode(TestMode.None);
            filterableInfo.setOperator(CompareOperator.Equals);
            filterableInfo.setLogicalOperator(LogicalOperator.AND);
            return filterableInfo;
        }
        return new FilterableInfo(filterable);
    }

    private void applyFilterableInfo(Condition condition, FilterableInfo filterableInfo, List<Variable> variables, Set<String> usedParamNames) {
        condition.setTestMode(filterableInfo.getTestMode());
        condition.setCompareOperator(filterableInfo.getOperator());
        condition.setLogicalOperator(filterableInfo.getLogicalOperator());
        condition.setIgnorecase(filterableInfo.isIgnorecase());
        condition.setNot(filterableInfo.isNot());
        condition.setTestTemplate(filterableInfo.getTestTemplate());
        condition.setExprTemplate(filterableInfo.getExprTemplate());
        if (condition.getCompareOperator().isRequiredSecondVariable()) {
            Variable secondVariable = deepGet(variables, filterableInfo.getSecondVariable());
            if (secondVariable == null) {
                throw new MybatisExtException("Second variable '" + filterableInfo.getSecondVariable() + "' not found in variables.");
            }
            usedParamNames.add(secondVariable.getFullName().split("\\.")[0]);
            condition.setSecondVariable(secondVariable);
        }
    }

    private Variable deepGet(List<Variable> variables, String path) {
        Map<String, Variable> map = new HashMap<>();
        for (Variable variable : variables) {
            map.put(variable.getName(), variable);
        }
        Variable variable = null;
        for (String key : path.split("\\.")) {
            if ((variable = map.get(key)) == null) {
                return null;
            }
            map = variable.getNameToVariable();
        }
        return variable;
    }

    private Condition ensureConditionVariable(JpaTokenizer jpaTokenizer, Set<String> usedParamNames, ConditionList conditionList) {
        GenericParameter[] parameters = jpaTokenizer.getParameters();
        AtomicInteger paramIndex = new AtomicInteger(0);
        for (ConditionList list = conditionList; list != null; list = list.getTailList()) {
            Condition condition = list.getCondition();
            if (condition.getCompareOperator().isRequiredVariable() && condition.getVariable() == null) {
                condition.setVariable(buildNextUnusedVariable(usedParamNames, paramIndex, parameters, jpaTokenizer));
            }
            if (condition.getCompareOperator().isRequiredSecondVariable() && condition.getSecondVariable() == null) {
                condition.setSecondVariable(buildNextUnusedVariable(usedParamNames, paramIndex, parameters, jpaTokenizer));
            }
        }
        return ConditionHelper.simplifyCondition(ConditionHelper.buildForConditionList(conditionList));
    }

    private Variable buildNextUnusedVariable(Set<String> usedParamNames, AtomicInteger paramIndex, GenericParameter[] parameters, JpaTokenizer jpaTokenizer) {
        for (int i = paramIndex.get(); i < parameters.length; i++) {
            GenericParameter parameter = parameters[i];
            Param param = parameter.getAnnotation(Param.class);
            if (param == null || !usedParamNames.contains(param.value())) {
                if (param != null) {
                    usedParamNames.add(param.value());
                }
                paramIndex.set(i + 1);
                // TODO paramName应该提前计算好
                String paramName;
                if (param == null) {
                    paramName = "param" + (i + 1);
                    if (parameters.length == 1) {
                        if (List.class.isAssignableFrom(parameter.getType())) {
                            paramName = "list";
                        } else if (Collection.class.isAssignableFrom(parameter.getType())) {
                            paramName = "collection";
                        } else if (parameter.getType().isArray()) {
                            paramName = "array";
                        }
                    }
                } else {
                    paramName = param.value();
                }
                return new Variable(paramName, parameter.getGenericType());
            }
        }
        throw new MybatisExtException("Insufficient parameters for method: " + jpaTokenizer.getText());
    }

    private final Symbol propertyEnd = join(property, end);

    private PropertyInfo parseProperty(TableInfo tableInfo, String param) {
        AtomicReference<PropertyInfo> reference = new AtomicReference<>();
        List<TokenMarker> tokenMarkers = new ArrayList<>();
        JpaTokenizer jpaTokenizer = new JpaTokenizer(tableInfo, param.substring(0, 1).toUpperCase() + param.substring(1), configuration);
        propertyEnd.match(jpaTokenizer, state -> {
            PropertyInfo propertyInfo = (PropertyInfo) state.getResult();
            reference.set(propertyInfo);
            tokenMarkers.add(new TokenMarker(jpaTokenizer.getTokenMarker()));
            return false;
        });
        if (reference.get() == null) {
            jpaTokenizer.getExpectedTokens().printMessage(System.err);
            throw new ParserException(jpaTokenizer.getExpectedTokens().getMessage() + " Parameter: " + jpaTokenizer.getText());
        }
        if (tokenMarkers.size() > 1) {
            tokenMarkers.get(0).printDiff(tokenMarkers.get(tokenMarkers.size() - 1), System.err);
            throw new ParserException("Conflict detected at column " + tokenMarkers.get(0).getDiffBegin(tokenMarkers.get(tokenMarkers.size() - 1)));
        }
        return reference.get();
    }

    public Semantic parse(TableInfo tableInfo, String methodName, GenericParameter[] parameters, GenericType returnType) {
        GenericType unwrappedReturnType = TypeUtils.unwrapToGenericType(returnType);
        if (TableInfoFactory.isSameTableType(tableInfo.getClassType(), unwrappedReturnType)) {
            tableInfo = TableInfoFactory.buildTableInfo(unwrappedReturnType, configuration, extContext);
        }
        for (GenericParameter parameter : parameters) {
            if (TypeUtils.isSpecialParameter(parameter.getType())) {
                continue;
            }
            GenericType parameterType = TypeUtils.unwrapToGenericType(parameter.getGenericType());
            if (TableInfoFactory.isSameTableType(tableInfo.getClassType(), parameterType)) {
                tableInfo = TableInfoFactory.buildTableInfo(parameterType, configuration, extContext);
            }
            break;
        }

        AtomicReference<Semantic> reference = new AtomicReference<>();
        List<TokenMarker> tokenMarkers = new ArrayList<>();
        JpaTokenizer jpaTokenizer = new JpaTokenizer(tableInfo, methodName, configuration, parameters, returnType);
        grammar.match(jpaTokenizer, state -> {
            Semantic semantic = (Semantic) state.getResult();
            reference.set(semantic);
            tokenMarkers.add(new TokenMarker(jpaTokenizer.getTokenMarker()));
            return false;
        });
        if (reference.get() == null) {
            jpaTokenizer.getExpectedTokens().printMessage(System.err);
            throw new ParserException(jpaTokenizer.getExpectedTokens().getMessage() + " Method: " + jpaTokenizer.getText());
        }
        if (tokenMarkers.size() > 1) {
            tokenMarkers.get(0).printDiff(tokenMarkers.get(tokenMarkers.size() - 1), System.err);
            throw new ParserException("Conflict detected at column " + tokenMarkers.get(0).getDiffBegin(tokenMarkers.get(tokenMarkers.size() - 1)));
        }
        reference.get().setTableInfo(tableInfo);
        return reference.get();
    }
}
