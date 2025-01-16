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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.FilterSpec;
import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.annotation.OnlyById;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.FilterSpecInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.reflect.GenericParameter;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.statement.NestedSelectHelper;
import io.github.mybatisext.util.CommonUtils;
import io.github.mybatisext.util.ImmutablePair;
import io.github.mybatisext.util.StringUtils;
import io.github.mybatisext.util.TypeArgumentResolver;

public class JpaParser extends BaseParser {

    Symbol grammar = new Symbol("grammar");
    Symbol conditionList = new Symbol("conditionList");
    Symbol condition = new Symbol("condition");
    Symbol propertyList = new Symbol("propertyList");
    Symbol property = new Symbol("property");
    Symbol variable = new Symbol("variable");
    Symbol orderByList = new Symbol("orderByList");
    Symbol limit = new Symbol("limit");

    Symbol propertyName = new Symbol("propertyName").set((state, continuation) -> {
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

    Symbol subPropertyName = new Symbol("subPropertyName").set((state, continuation) -> {
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

    Symbol integer = new Symbol("integer").set((state, continuation) -> {
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

    Symbol variableName = new Symbol("variableName").set((state, continuation) -> {
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

    Symbol subVariableName = new Symbol("subVariableName").set((state, continuation) -> {
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

    Symbol end = new Symbol("end").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        if (jpaTokenizer.getCursor() != jpaTokenizer.getText().length()) {
            jpaTokenizer.getExpectedTokens().record(jpaTokenizer.getCursor(), "end");
            return false;
        }
        return continuation.test(state);
    });

    Symbol keyword(String s) {
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

    Symbol integerB = new Symbol("integerB").set(integer);
    Symbol variableB = new Symbol("variableB").set(variable);

    Symbol conditionAction(CompareOperator compareOperator) {
        return action(state -> {
            Condition condition = new Condition(ConditionType.BASIC);
            condition.setPropertyInfos(state.<JpaTokenizer>getTokenizer().getTableInfo().getNameToPropertyInfo());
            condition.setPropertyInfo(state.getMatch(property).val());
            condition.setCompareOperator(compareOperator);
            if (state.getMatch("Ignorecase") != null) {
                condition.setIgnorecase(true);
            }
            if (state.getMatch("Not") != null) {
                condition.setNot(true);
            }
            MatchResult _variable = state.getMatch(variable);
            if (_variable == null) {
                if (CompareOperator.Between != compareOperator) {
                    JpaTokenizer jpaTokenizer = state.getTokenizer();
                    jpaTokenizer.getVariables().stream().filter(v -> condition.getPropertyInfo().getName().equals(v.getName())).findFirst().ifPresent(condition::setVariable);
                }
            } else {
                condition.setVariable(_variable.val());
                if (CompareOperator.Between == compareOperator) {
                    condition.setSecondVariable(state.getMatch(variableB).val());
                }
            }
            state.setReturn(condition);
        });
    }

    Symbol groupBy = new Symbol("groupBy").set(join(keyword("GroupBy"), propertyList));
    Symbol having = new Symbol("having").set(join(keyword("Having"), conditionList));
    Symbol orderBy = new Symbol("orderBy").set(join(keyword("OrderBy"), orderByList));

    public JpaParser() {
        grammar.set(choice(
                join(choice(keyword("find"), keyword("select"), keyword("list"), keyword("get")), optional(keyword("Distinct")), optional(choice(keyword("All"), keyword("One"), join(keyword("Top"), choice(integer, variable)))), optional(propertyList), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), optional(join(groupBy, optional(having))), optional(orderBy), optional(limit), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.SELECT);
                    MatchResult _propertyList = state.getMatch(propertyList);
                    if (_propertyList != null) {
                        semantic.setSelectItems(ensureJoinRelationColumns(state, _propertyList.val()));
                    } else {
                        semantic.setSelectItems(buildDefaultSelectItems(state));
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
                        semantic.setWhere(ensureConditionVariable(state, usedParamNames, _conditionList.val()));
                    }
                    MatchResult _groupBy = state.getMatch(groupBy);
                    if (_groupBy != null) {
                        semantic.setGroupBy(_groupBy.val());
                        MatchResult _having = state.getMatch(having);
                        if (_having != null) {
                            usedParamNames.addAll(collectUsedParamNames(_having.<ConditionList>val()));
                            semantic.setHaving(ensureConditionVariable(state, usedParamNames, _having.val()));
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
                    if (hasUnusedParam(state, usedParamNames)) {
                        if (_groupBy != null) {
                            if (semantic.getHaving() == null) {
                                semantic.setHaving(buildDefaultCondition(state, usedParamNames));
                            }
                        } else {
                            if (semantic.getWhere() == null) {
                                semantic.setWhere(buildDefaultCondition(state, usedParamNames));
                            }
                        }
                    }
                    state.setReturn(semantic);
                })),
                join(keyword("exists"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.EXISTS);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setWhere(ensureConditionVariable(state, collectUsedParamNames(_conditionList.<ConditionList>val()), _conditionList.val()));
                    } else {
                        semantic.setWhere(buildDefaultCondition(state, new HashSet<>()));
                    }
                    state.setReturn(semantic);
                })),
                join(keyword("count"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.COUNT);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setWhere(ensureConditionVariable(state, collectUsedParamNames(_conditionList.<ConditionList>val()), _conditionList.val()));
                    } else {
                        semantic.setWhere(buildDefaultCondition(state, new HashSet<>()));
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("update"), keyword("modify")), optional(keyword("Batch")), optional(keyword("IgnoreNull")), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.UPDATE);
                    semantic.setParameter(buildSemanticParameter(state, true));
                    if (state.getMatch("IgnoreNull") != null) {
                        semantic.setIgnoreNull(true);
                    }
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setWhere(ensureConditionVariable(state, collectUsedParamNames(_conditionList.<ConditionList>val()), _conditionList.val()));
                    } else {
                        semantic.setWhere(buildDefaultCondition(state, new HashSet<>()));
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("delete"), keyword("remove")), optional(keyword("Batch")), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.DELETE);
                    semantic.setParameter(buildSemanticParameter(state, false));
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setWhere(ensureConditionVariable(state, collectUsedParamNames(_conditionList.<ConditionList>val()), _conditionList.val()));
                    } else {
                        semantic.setWhere(buildDefaultCondition(state, new HashSet<>()));
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("save"), keyword("insert")), optional(keyword("Batch")), optional(keyword("IgnoreNull")), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.INSERT);
                    semantic.setParameter(buildSemanticParameter(state, true));
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

        orderByList.set(join(property, optional(choice(keyword("Asc"), keyword("Desc"))), optional(join(keyword("And"), orderByList)), action(state -> {
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
                join(property, optional(join(keyword("And"), propertyList)), action(state -> {
                    List<PropertyInfo> propertyInfos = new ArrayList<>();
                    propertyInfos.add(state.getMatch(property).val());
                    MatchResult _propertyList = state.getMatch(propertyList);
                    if (_propertyList != null) {
                        propertyInfos.addAll(_propertyList.val());
                    }
                    state.setReturn(propertyInfos);
                })));

        property.set(join(propertyName, star(join(keyword("Dot"), subPropertyName))));
        variable.set(join(variableName, star(join(keyword("Dot"), subVariableName))));
    }

    private List<PropertyInfo> buildDefaultSelectItems(State state) {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        GenericType returnType = CommonUtils.unwrapType(jpaTokenizer.getReturnType());
        if (!TableInfoFactory.isAssignableEitherWithTable(returnType, jpaTokenizer.getTableInfo().getTableClass())) {
            throw new MybatisExtException("Incompatible return type: " + returnType.getTypeName() + ", expected: " + jpaTokenizer.getTableInfo().getTableClass().getName());
        }
        TableInfo tableInfo = TableInfoFactory.getTableInfo(jpaTokenizer.getConfiguration(), returnType);
        List<PropertyInfo> propertyInfos = tableInfo.getNameToPropertyInfo().values().stream().filter(v -> v.getLoadType() == null || v.getLoadType() == LoadType.JOIN).collect(Collectors.toList());
        return ensureJoinRelationColumns(state, propertyInfos);
    }

    private List<PropertyInfo> ensureJoinRelationColumns(State state, List<PropertyInfo> propertyInfos) {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        TableInfo tableInfo = jpaTokenizer.getTableInfo();
        Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();
        for (PropertyInfo propertyInfo : propertyInfos) {
            nameToPropertyInfo.put(propertyInfo.getName(), propertyInfo);
            if (propertyInfo.getLoadType() != null && propertyInfo.getLoadType() != LoadType.JOIN) {
                List<ImmutablePair<PropertyInfo, PropertyInfo>> immutablePairs = NestedSelectHelper.buildLeftmostJoinColumns(tableInfo, propertyInfo);
                for (ImmutablePair<PropertyInfo, PropertyInfo> immutablePair : immutablePairs) {
                    nameToPropertyInfo.put(immutablePair.getLeft().getName(), immutablePair.getLeft());
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

    private boolean hasUnusedParam(State state, Set<String> usedParamNames) {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
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

    private Variable buildSemanticParameter(State state, boolean required) {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        GenericParameter[] parameters = jpaTokenizer.getParameters();
        if (parameters.length == 0) {
            throw new MybatisExtException("No parameters provided in the query.");
        }
        GenericType tableClass = jpaTokenizer.getTableInfo().getTableClass();
        GenericType parameterType = parameters[0].getGenericType();
        Param param = parameters[0].getAnnotation(Param.class);
        if (parameterType.isArray() && TableInfoFactory.isAssignableEitherWithTable(tableClass, parameterType.getComponentType())) {
            return new Variable(param != null ? param.value() : (parameters.length == 1 ? "array" : "param1"), parameterType);
        }
        if (Collection.class.isAssignableFrom(parameterType.getType()) && TableInfoFactory.isAssignableEitherWithTable(tableClass, TypeArgumentResolver.resolveGenericType(parameterType, Collection.class, 0))) {
            if (List.class.isAssignableFrom(parameterType.getType())) {
                return new Variable(param != null ? param.value() : (parameters.length == 1 ? "list" : "param1"), parameterType);
            }
            return new Variable(param != null ? param.value() : (parameters.length == 1 ? "collection" : "param1"), parameterType);
        }
        if (TableInfoFactory.isAssignableEitherWithTable(tableClass, parameterType)) {
            return new Variable(param != null ? param.value() : (parameters.length == 1 ? "" : "param1"), parameterType);
        }
        if (required) {
            throw new MybatisExtException("Invalid parameter type. Expected: " + tableClass + ", but was: " + parameterType);
        }
        return null;
    }

    private Condition buildDefaultCondition(State state, Set<String> usedParamNames) {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        GenericParameter[] parameters = jpaTokenizer.getParameters();
        if (parameters.length == 1) {
            return buildSingleParamCondition(jpaTokenizer, usedParamNames, parameters[0]);
        }
        return buildMultiParamCondition(jpaTokenizer, usedParamNames, parameters);
    }

    private Condition buildSingleParamCondition(JpaTokenizer jpaTokenizer, Set<String> usedParamNames, GenericParameter parameter) {
        TableInfo tableInfo = jpaTokenizer.getTableInfo();
        List<Variable> variables = jpaTokenizer.getVariables();
        GenericType tableClass = tableInfo.getTableClass();
        GenericType parameterType = parameter.getGenericType();
        OnlyById onlyById = parameter.getAnnotation(OnlyById.class);
        Param param = parameter.getAnnotation(Param.class);
        String paramName;
        if (parameterType.isArray() && TableInfoFactory.isAssignableEitherWithTable(tableClass, parameterType.getComponentType())) {
            paramName = param != null ? "__" + param.value() + "__item" : "__array__item";
        } else if (Collection.class.isAssignableFrom(parameterType.getType()) && TableInfoFactory.isAssignableEitherWithTable(tableClass, TypeArgumentResolver.resolveGenericType(parameterType, Collection.class, 0))) {
            paramName = param != null ? "__" + param.value() + "__item" : "__collection__item";
            if (List.class.isAssignableFrom(parameterType.getType())) {
                paramName = param != null ? "__" + param.value() + "__item" : "__list__item";
            }
        } else if (TableInfoFactory.isAssignableEitherWithTable(tableClass, parameterType)) {
            paramName = param != null ? param.value() : "";
        } else {
            return buildMultiParamCondition(jpaTokenizer, usedParamNames, parameter);
        }
        Condition condition = ConditionHelper.fromTableInfo(tableInfo, onlyById != null, paramName);
        FilterSpec filterSpec = parameter.getAnnotation(FilterSpec.class);
        FilterSpecInfo filterSpecInfo = buildFilterSpecInfo(filterSpec);
        if (onlyById == null && filterSpecInfo != null) {
            applyFilterSpecInfo(condition, filterSpecInfo, variables, usedParamNames);
            condition.setVariable(new Variable(StringUtils.isNotBlank(paramName) ? paramName : "param1", tableClass));
        }
        return ConditionHelper.simplifyCondition(condition);
    }

    private Condition buildMultiParamCondition(JpaTokenizer jpaTokenizer, Set<String> usedParamNames, GenericParameter... parameters) {
        TableInfo tableInfo = jpaTokenizer.getTableInfo();
        List<Variable> variables = jpaTokenizer.getVariables();
        Configuration configuration = jpaTokenizer.getConfiguration();
        List<Condition> conditions = new ArrayList<>();
        for (GenericParameter parameter : parameters) {
            Param param = parameter.getAnnotation(Param.class);
            FilterSpec filterSpec = parameter.getAnnotation(FilterSpec.class);
            FilterSpecInfo filterSpecInfo = buildFilterSpecInfo(filterSpec);
            if (param != null && usedParamNames.contains(param.value())) {
                continue;
            }
            if (param == null || !configuration.getTypeHandlerRegistry().hasTypeHandler(parameter.getType())) {
                throw new MybatisExtException("Unsupported parameter type: " + parameter.getType().getName() + ". Method: " + jpaTokenizer.getText());
            }
            Condition condition = new Condition(ConditionType.BASIC);
            condition.setPropertyInfos(tableInfo.getNameToPropertyInfo());
            condition.setPropertyInfo(parseProperty(configuration, tableInfo, param.value()));
            condition.setVariable(new Variable(param.value(), parameter.getGenericType()));
            if (filterSpecInfo == null) {
                condition.setTest(IfTest.None);
                condition.setCompareOperator(CompareOperator.Equals);
            } else {
                applyFilterSpecInfo(condition, filterSpecInfo, variables, usedParamNames);
            }
            conditions.add(condition);
        }
        if (conditions.isEmpty()) {
            return null;
        }
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.setLogicalOperator(LogicalOperator.AND);
        condition.setPropertyInfos(jpaTokenizer.getTableInfo().getNameToPropertyInfo());
        return ConditionHelper.simplifyCondition(condition);
    }

    private static @Nullable FilterSpecInfo buildFilterSpecInfo(@Nullable FilterSpec filterSpec) {
        if (filterSpec == null) {
            return null;
        }
        FilterSpecInfo filterSpecInfo = new FilterSpecInfo();
        filterSpecInfo.setTest(filterSpec.test());
        filterSpecInfo.setOperator(filterSpec.operator());
        filterSpecInfo.setLogicalOperator(filterSpec.logicalOperator());
        filterSpecInfo.setTestTemplate(filterSpec.testTemplate());
        filterSpecInfo.setExprTemplate(filterSpec.exprTemplate());
        filterSpecInfo.setSecondVariable(filterSpec.secondVariable());
        return filterSpecInfo;
    }

    private void applyFilterSpecInfo(Condition condition, FilterSpecInfo filterSpecInfo, List<Variable> variables, Set<String> usedParamNames) {
        condition.setTest(filterSpecInfo.getTest());
        condition.setTestTemplate(filterSpecInfo.getTestTemplate());
        condition.setExprTemplate(filterSpecInfo.getExprTemplate());
        condition.setCompareOperator(filterSpecInfo.getOperator());
        condition.setLogicalOperator(filterSpecInfo.getLogicalOperator());
        if (condition.getCompareOperator() == CompareOperator.Between) {
            Variable secondVariable = deepGet(variables, filterSpecInfo.getSecondVariable());
            if (secondVariable == null) {
                throw new MybatisExtException("Second variable '" + filterSpecInfo.getSecondVariable() + "' not found in variables.");
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
            map = variable;
        }
        return variable;
    }

    private Condition ensureConditionVariable(State state, Set<String> usedParamNames, @Nonnull ConditionList conditionList) {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        GenericParameter[] parameters = jpaTokenizer.getParameters();
        AtomicInteger paramIndex = new AtomicInteger(0);
        for (ConditionList list = conditionList; list != null; list = list.getTailList()) {
            Condition condition = list.getCondition();
            if (condition.getVariable() == null) {
                setConditionVariable(usedParamNames, paramIndex, parameters, condition, jpaTokenizer);
            }
            if (condition.getCompareOperator() == CompareOperator.Between) {
                if (condition.getSecondVariable() == null) {
                    setConditionSecondVariable(usedParamNames, paramIndex, parameters, condition, jpaTokenizer);
                }
            }
        }
        return ConditionHelper.simplifyCondition(ConditionHelper.fromConditionList(conditionList));
    }

    private void setConditionVariable(Set<String> usedParamNames, AtomicInteger paramIndex, GenericParameter[] parameters, Condition condition, JpaTokenizer jpaTokenizer) {
        int i = paramIndex.get();
        for (; i < parameters.length; i++) {
            Param param = parameters[i].getAnnotation(Param.class);
            if (param == null || !usedParamNames.contains(param.value())) {
                condition.setVariable(new Variable(param != null ? param.value() : "param" + (i + 1), parameters[i].getGenericType()));
                if (param != null) {
                    usedParamNames.add(param.value());
                }
                break;
            }
        }
        if (i >= parameters.length) {
            throw new MybatisExtException("Insufficient parameters for method: " + jpaTokenizer.getText());
        }
        paramIndex.set(i + 1);
    }

    private void setConditionSecondVariable(Set<String> usedParamNames, AtomicInteger paramIndex, GenericParameter[] parameters, Condition condition, JpaTokenizer jpaTokenizer) {
        int i = paramIndex.get();
        for (; i < parameters.length; i++) {
            Param param = parameters[i].getAnnotation(Param.class);
            if (param == null || !usedParamNames.contains(param.value())) {
                condition.setSecondVariable(new Variable(param != null ? param.value() : "param" + (i + 1), parameters[i].getGenericType()));
                if (param != null) {
                    usedParamNames.add(param.value());
                }
                break;
            }
        }
        if (i >= parameters.length) {
            throw new MybatisExtException("Insufficient parameters for method: " + jpaTokenizer.getText());
        }
        paramIndex.set(i + 1);
    }

    private final Symbol propertyEnd = join(property, end);

    private PropertyInfo parseProperty(Configuration configuration, TableInfo tableInfo, String param) {
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
            throw new ParserException(jpaTokenizer.getExpectedTokens().toString() + " Parameter: " + jpaTokenizer.getText());
        }
        if (tokenMarkers.size() > 1) {
            tokenMarkers.get(0).printDiff(tokenMarkers.get(tokenMarkers.size() - 1), System.err);
            throw new ParserException("Conflict detected at column " + tokenMarkers.get(0).getDiffBegin(tokenMarkers.get(tokenMarkers.size() - 1)));
        }
        return reference.get();
    }

    public Semantic parse(Configuration configuration, TableInfo tableInfo, String methodName, GenericParameter[] parameters, GenericType returnType) {
        GenericType unwrappedReturnType = CommonUtils.unwrapType(returnType);
        if (TableInfoFactory.isAssignableFromWithTable(tableInfo.getTableClass(), unwrappedReturnType)) {
            tableInfo = TableInfoFactory.getTableInfo(configuration, unwrappedReturnType);
        }
        for (GenericParameter parameter : parameters) {
            if (CommonUtils.isSpecialParameter(parameter.getType())) {
                continue;
            }
            GenericType parameterType = CommonUtils.unwrapType(parameter.getGenericType());
            if (TableInfoFactory.isAssignableFromWithTable(tableInfo.getTableClass(), parameterType)) {
                tableInfo = TableInfoFactory.getTableInfo(configuration, parameterType);
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
            throw new ParserException(jpaTokenizer.getExpectedTokens().toString() + " Method: " + jpaTokenizer.getText());
        }
        if (tokenMarkers.size() > 1) {
            tokenMarkers.get(0).printDiff(tokenMarkers.get(tokenMarkers.size() - 1), System.err);
            throw new ParserException("Conflict detected at column " + tokenMarkers.get(0).getDiffBegin(tokenMarkers.get(tokenMarkers.size() - 1)));
        }
        reference.get().setTableInfo(tableInfo);
        return reference.get();
    }
}
