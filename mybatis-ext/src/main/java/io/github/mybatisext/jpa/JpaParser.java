package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.annotation.OnlyById;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.reflect.GenericParameter;
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
                join(choice(keyword("find"), keyword("select"), keyword("list"), keyword("get")), optional(keyword("Distinct")), optional(choice(keyword("All"), keyword("One"), join(keyword("Top"), choice(integer, variable)))), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), optional(join(groupBy, optional(having))), optional(orderBy), optional(limit), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.SELECT);
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
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setWhere(ensureConditionVariable(state, _conditionList.val()));
                    } else {
                        semantic.setWhere(getParamConditionList(state));
                    }
                    MatchResult _groupBy = state.getMatch(groupBy);
                    if (_groupBy != null) {
                        semantic.setGroupBy(_groupBy.val());
                        MatchResult _having = state.getMatch(having);
                        if (_having != null) {
                            semantic.setHaving(ensureConditionVariable(state, _having.val()));
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
                    state.setReturn(semantic);
                })),
                join(keyword("exists"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.EXISTS);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setWhere(ensureConditionVariable(state, _conditionList.val()));
                    } else {
                        semantic.setWhere(getParamConditionList(state));
                    }
                    state.setReturn(semantic);
                })),
                join(keyword("count"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.COUNT);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setWhere(ensureConditionVariable(state, _conditionList.val()));
                    } else {
                        semantic.setWhere(getParamConditionList(state));
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("update"), keyword("modify")), optional(keyword("Batch")), optional(keyword("IgnoreNull")), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.UPDATE);
                    semantic.setParameter(getParamTargetVariable(state));
                    if (state.getMatch("IgnoreNull") != null) {
                        semantic.setIgnoreNull(true);
                    }
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setWhere(ensureConditionVariable(state, _conditionList.val()));
                    } else {
                        semantic.setWhere(getParamConditionList(state));
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("delete"), keyword("remove")), optional(keyword("Batch")), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.DELETE);
                    semantic.setParameter(getParamTargetVariable(state));
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setWhere(ensureConditionVariable(state, _conditionList.val()));
                    } else {
                        semantic.setWhere(getParamConditionList(state));
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("save"), keyword("insert")), optional(keyword("Batch")), optional(keyword("IgnoreNull")), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.INSERT);
                    semantic.setParameter(getParamTargetVariable(state));
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

    private Variable getParamTargetVariable(State state) {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        GenericParameter[] parameters = jpaTokenizer.getParameters();
        TableInfo tableInfo = jpaTokenizer.getTableInfo();
        if (parameters.length == 0) {
            throw new MybatisExtException("No parameters provided in the query.");
        }
        if (tableInfo.getTableClass().isAssignableFrom(parameters[0].getType())) {
            Param param = parameters[0].getAnnotation(Param.class);
            if (tableInfo.getTableClass().getType().isArray()) {
                return new Variable(param != null ? param.value() : "array", parameters[0].getGenericType());
            }
            return new Variable(param != null ? param.value() : "", parameters[0].getGenericType());
        }
        if (Collection.class.isAssignableFrom(parameters[0].getType()) && tableInfo.getTableClass().isAssignableFrom(TypeArgumentResolver.resolveTypeArgument(parameters[0].getGenericType(), Collection.class, 0))) {
            Param param = parameters[0].getAnnotation(Param.class);
            if (List.class.isAssignableFrom(parameters[0].getType())) {
                return new Variable(param != null ? param.value() : "list", parameters[0].getGenericType());
            }
            return new Variable(param != null ? param.value() : "collection", parameters[0].getGenericType());
        }
        throw new MybatisExtException("Invalid parameter type. Expected: " + tableInfo.getTableClass() + ", but was: " + parameters[0].getType());
    }

    private Condition getParamConditionList(State state) {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        Configuration configuration = jpaTokenizer.getConfiguration();
        GenericParameter[] parameters = jpaTokenizer.getParameters();
        TableInfo tableInfo = jpaTokenizer.getTableInfo();
        if (parameters.length == 1) {
            if (tableInfo.getTableClass().isAssignableFrom(parameters[0].getType())) {
                Param param = parameters[0].getAnnotation(Param.class);
                OnlyById onlyById = parameters[0].getAnnotation(OnlyById.class);
                if (tableInfo.getTableClass().getType().isArray()) {
                    return ConditionHelper.fromTableInfo(tableInfo, onlyById != null, onlyById != null ? IfTest.None : IfTest.NotNull, param != null ? param.value() : "__array__item");
                }
                return ConditionHelper.fromTableInfo(tableInfo, onlyById != null, onlyById != null ? IfTest.None : IfTest.NotNull, param != null ? param.value() : "");
            }
            if (Collection.class.isAssignableFrom(parameters[0].getType()) && tableInfo.getTableClass().isAssignableFrom(TypeArgumentResolver.resolveTypeArgument(parameters[0].getGenericType(), Collection.class, 0))) {
                OnlyById onlyById = parameters[0].getAnnotation(OnlyById.class);
                if (List.class.isAssignableFrom(parameters[0].getType())) {
                    return ConditionHelper.fromTableInfo(tableInfo, onlyById != null, onlyById != null ? IfTest.None : IfTest.NotNull, "__list__item");
                }
                return ConditionHelper.fromTableInfo(tableInfo, onlyById != null, onlyById != null ? IfTest.None : IfTest.NotNull, "__collection__item");
            }
        }
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.setLogicalOperator(LogicalOperator.AND);
        for (GenericParameter parameter : parameters) {
            Param param = parameter.getAnnotation(Param.class);
            if (param == null || !configuration.getTypeHandlerRegistry().hasTypeHandler(parameter.getType())) {
                throw new MybatisExtException("Unsupported parameter type: " + parameter.getType().getName() + ". Method: " + jpaTokenizer.getText());
            }
            PropertyInfo propertyInfo = parseProperty(configuration, jpaTokenizer.getTableInfo(), param.value());
            Condition subCondition = new Condition(ConditionType.BASIC);
            subCondition.setPropertyInfo(propertyInfo);
            subCondition.setCompareOperator(CompareOperator.Equals);
            subCondition.setVariable(new Variable(param.value(), parameter.getGenericType()));
            condition.getSubConditions().add(subCondition);
        }
        return condition;
    }

    private Condition ensureConditionVariable(State state, ConditionList conditionList) {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        GenericParameter[] parameters = jpaTokenizer.getParameters();
        int i = 0;
        Set<String> set = new HashSet<>();
        for (ConditionList list = conditionList; list != null; list = list.getTailList()) {
            Condition condition = list.getCondition();
            if (condition.getVariable() == null) {
                if (i >= parameters.length) {
                    throw new MybatisExtException("Insufficient parameters for method: " + jpaTokenizer.getText());
                }
                Param param = parameters[i].getAnnotation(Param.class);
                condition.setVariable(new Variable(param != null ? param.value() : "param" + i + 1, parameters[i].getGenericType()));
                i++;
            } else {
                String firstName = condition.getVariable().getFullName().split("\\.")[0];
                if (!set.contains(firstName)) {
                    set.add(firstName);
                    i++;
                }
            }
            if (condition.getCompareOperator() == CompareOperator.Between) {
                if (condition.getSecondVariable() == null) {
                    if (i >= parameters.length) {
                        throw new MybatisExtException("Insufficient parameters for method: " + jpaTokenizer.getText());
                    }
                    Param param = parameters[i].getAnnotation(Param.class);
                    condition.setSecondVariable(new Variable(param != null ? param.value() : "param" + i + 1, parameters[i].getGenericType()));
                    i++;
                } else {
                    String firstName = condition.getSecondVariable().getFullName().split("\\.")[0];
                    if (!set.contains(firstName)) {
                        set.add(firstName);
                        i++;
                    }
                }
            }
        }
        return ConditionHelper.fromConditionList(conditionList);
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

    public Semantic parse(Configuration configuration, TableInfo tableInfo, String methodName, GenericParameter[] parameters) {
        AtomicReference<Semantic> reference = new AtomicReference<>();
        List<TokenMarker> tokenMarkers = new ArrayList<>();
        JpaTokenizer jpaTokenizer = new JpaTokenizer(tableInfo, methodName, configuration, parameters);
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
