package io.github.mybatisext.jpa;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.annotation.Criteria;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.util.StringUtils;
import io.github.mybatisext.util.TypeArgumentResolver;

public class JpaParser extends BaseParser {

    Symbol grammar = new Symbol("grammar");
    Symbol conditionList = new Symbol("conditionList");
    Symbol condition = new Symbol("condition");
    Symbol modifierList = new Symbol("modifierList");
    Symbol modifier = new Symbol("modifier");
    Symbol propertyList = new Symbol("propertyList");
    Symbol property = new Symbol("property");
    Symbol variable = new Symbol("variable");

    Symbol propertyName = new Symbol("propertyName").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        int cursor = jpaTokenizer.getCursor();
        Object result = state.getResult();
        List<PropertyInfo> propertyInfos;
        if (result instanceof PropertyInfo) {
            propertyInfos = jpaTokenizer.property((PropertyInfo) result);
        } else {
            propertyInfos = jpaTokenizer.property();
        }
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
        Object result = state.getResult();
        List<Variable> variables;
        if (result instanceof Variable) {
            variables = jpaTokenizer.variable((Variable) result);
        } else {
            variables = jpaTokenizer.variable();
        }
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

    public JpaParser() {
        grammar.set(choice(
                join(choice(keyword("find"), keyword("select"), keyword("list"), keyword("get")), optional(keyword("Distinct")), optional(choice(keyword("All"), keyword("One"), join(keyword("Top"), choice(integer, variable)))), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), optional(modifierList), end, action(state -> {
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
                        semantic.setConditionList(_conditionList.val());
                    } else {
                        semantic.setConditionList(getParamConditionList(state, false, IfTest.NotNull));
                    }
                    MatchResult _modifierList = state.getMatch(modifierList);
                    if (_modifierList != null) {
                        semantic.setModifierList(_modifierList.val());
                    }
                    state.setReturn(semantic);
                })),
                join(keyword("exists"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), optional(modifierList), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.EXISTS);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setConditionList(_conditionList.val());
                    } else {
                        semantic.setConditionList(getParamConditionList(state, false, IfTest.NotNull));
                    }
                    MatchResult _modifierList = state.getMatch(modifierList);
                    if (_modifierList != null) {
                        semantic.setModifierList(_modifierList.val());
                    }
                    state.setReturn(semantic);
                })),
                join(keyword("count"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), optional(modifierList), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.COUNT);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setConditionList(_conditionList.val());
                    } else {
                        semantic.setConditionList(getParamConditionList(state, false, IfTest.NotNull));
                    }
                    MatchResult _modifierList = state.getMatch(modifierList);
                    if (_modifierList != null) {
                        semantic.setModifierList(_modifierList.val());
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("update"), keyword("modify")), optional(keyword("Batch")), optional(keyword("IgnoreNull")), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.UPDATE);
                    if (state.getMatch("IgnoreNull") != null) {
                        semantic.setIgnoreNull(true);
                    }
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setConditionList(_conditionList.val());
                    } else {
                        semantic.setConditionList(getParamConditionList(state, true, IfTest.None));
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("delete"), keyword("remove")), optional(keyword("Batch")), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.DELETE);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setConditionList(_conditionList.val());
                    } else {
                        semantic.setConditionList(getParamConditionList(state, true, IfTest.None));
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("save"), keyword("insert")), optional(keyword("Batch")), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.INSERT);
                    state.setReturn(semantic);
                }))));

        conditionList.set(
                join(condition, optional(join(choice(keyword("And"), keyword("Or")), conditionList)), action(state -> {
                    Condition c = state.getMatch(condition).val();
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        if (state.getMatch("And") != null) {
                            state.setReturn(new ConditionList(c, _conditionList.val(), ConditionListRel.And));
                        } else {
                            state.setReturn(new ConditionList(c, _conditionList.val(), ConditionListRel.Or));
                        }
                    } else {
                        state.setReturn(new ConditionList(c));
                    }
                })));

        Symbol integerB = new Symbol("integerB").set(integer);
        Symbol variableB = new Symbol("variableB").set(variable);
        Function<ConditionRel, Symbol> conditionAction = rel -> action(state -> {
            Condition condition = new Condition();
            condition.setPropertyInfo(state.getMatch(property).val());
            condition.setRel(rel);
            if (state.getMatch("Ignorecase") != null) {
                condition.setIgnorecase(true);
            }
            if (state.getMatch("Not") != null) {
                condition.setNot(true);
            }
            MatchResult _variable = state.getMatch(variable);
            if (_variable == null) {
                if (ConditionRel.Between != condition.getRel()) {
                    JpaTokenizer jpaTokenizer = state.getTokenizer();
                    jpaTokenizer.getVariables().stream().filter(v -> condition.getPropertyInfo().getName().equals(v.getName())).findFirst().ifPresent(v -> condition.setVariable(v.getFullName()));
                }
            } else {
                condition.setVariable(_variable.val());
                if (ConditionRel.Between == condition.getRel()) {
                    condition.setSecondVariable(state.getMatch(variableB).val());
                }
            }
            state.setReturn(condition);
        });
        condition.set(join(property, choice(
                join(optional(keyword("Ignorecase")), optional(keyword("Not")), choice(
                        join(keyword("Is"), optional(variable), conditionAction.apply(ConditionRel.Equals)),
                        join(keyword("Equals"), optional(variable), conditionAction.apply(ConditionRel.Equals)),
                        join(keyword("LessThan"), optional(variable), conditionAction.apply(ConditionRel.LessThan)),
                        join(keyword("LessThanEqual"), optional(variable), conditionAction.apply(ConditionRel.LessThanEqual)),
                        join(keyword("GreaterThan"), optional(variable), conditionAction.apply(ConditionRel.GreaterThan)),
                        join(keyword("GreaterThanEqual"), optional(variable), conditionAction.apply(ConditionRel.GreaterThanEqual)),
                        join(keyword("Like"), optional(variable), conditionAction.apply(ConditionRel.Like)),
                        join(keyword("StartWith"), optional(variable), conditionAction.apply(ConditionRel.StartWith)),
                        join(keyword("EndWith"), optional(variable), conditionAction.apply(ConditionRel.EndWith)),
                        join(keyword("Between"), optional(join(variable, keyword("To"), variableB)), conditionAction.apply(ConditionRel.Between)),
                        join(keyword("In"), optional(variable), conditionAction.apply(ConditionRel.In)),
                        conditionAction.apply(ConditionRel.Equals))),
                join(optional(keyword("Not")), choice(
                        join(keyword("IsNull"), conditionAction.apply(ConditionRel.IsNull)),
                        join(keyword("IsNotNull"), conditionAction.apply(ConditionRel.IsNotNull)),
                        join(keyword("IsTrue"), conditionAction.apply(ConditionRel.IsTrue)),
                        join(keyword("IsFalse"), conditionAction.apply(ConditionRel.IsFalse)))))));

        modifierList.set(
                join(modifier, optional(modifierList), action(state -> {
                    List<Modifier> list = new ArrayList<>();
                    Modifier _modifier = state.getMatch(modifier).val();
                    list.add(_modifier);
                    MatchResult _modifierList = state.getMatch(modifierList);
                    if (_modifierList != null) {
                        list.addAll(_modifierList.val());
                    }
                    state.setReturn(list);
                })));

        modifier.set(choice(
                join(keyword("OrderBy"), propertyList, optional(choice(keyword("Asc"), keyword("Desc"))), action(state -> {
                    OrderBy orderBy = new OrderBy();
                    orderBy.setPropertyInfos(state.getMatch(propertyList).val());
                    if (state.getMatch("Asc") != null) {
                        orderBy.setType(OrderByType.ASC);
                    } else if (state.getMatch("Desc") != null) {
                        orderBy.setType(OrderByType.DESC);
                    }
                    state.setReturn(orderBy);
                })),
                join(keyword("GroupBy"), propertyList, action(state -> {
                    GroupBy groupBy = new GroupBy();
                    groupBy.setPropertyInfos(state.getMatch(propertyList).val());
                    state.setReturn(groupBy);
                })),
                join(keyword("Limit"), choice(integer, variable), keyword("To"), choice(integerB, join(variableB)), action(state -> {
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
                })),
                join(keyword("Limit"), choice(integer, variable), action(state -> {
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
                    List<PropertyInfo> list = new ArrayList<>();
                    PropertyInfo propertyInfo = state.getMatch(property).val();
                    list.add(propertyInfo);
                    MatchResult _propertyList = state.getMatch(propertyList);
                    if (_propertyList != null) {
                        list.addAll(_propertyList.val());
                    }
                    state.setReturn(list);
                })));

        property.set(join(propertyName, star(join(keyword("Dot"), propertyName))));
        variable.set(join(variableName, star(join(keyword("Dot"), variableName)), action(state -> {
            Variable _variable = (Variable) state.getResult();
            state.setResult(_variable.getFullName());
        })));
    }

    private ConditionList getParamConditionList(State state, boolean onlyId, IfTest test) {
        ConditionList conditionList = null;
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        Configuration configuration = jpaTokenizer.getConfiguration();
        Parameter[] parameters = jpaTokenizer.getParameters();
        TableInfo tableInfo = jpaTokenizer.getTableInfo();
        if (parameters.length == 1) {
            if (tableInfo.getTableClass().isAssignableFrom(parameters[0].getType())) {
                Param param = parameters[0].getAnnotation(Param.class);
                return ConditionFactory.fromTableInfo(tableInfo, onlyId, test, param != null ? param.value() : "");
            }
            if (Collection.class.isAssignableFrom(parameters[0].getType()) && tableInfo.getTableClass().isAssignableFrom(TypeArgumentResolver.resolveTypeArgument(parameters[0].getParameterizedType(), Collection.class, 0))) {
                return ConditionFactory.fromTableInfo(tableInfo, onlyId, test, "item");
            }
            if (parameters[0].getType().isAnnotationPresent(Criteria.class)) {
                Param param = parameters[0].getAnnotation(Param.class);
                return ConditionFactory.fromCriteria(configuration, parameters[0].getType(), param != null ? param.value() : "");
            }
        }
        for (Parameter parameter : parameters) {
            Param param = parameter.getAnnotation(Param.class);
            if (param == null || !configuration.getTypeHandlerRegistry().hasTypeHandler(parameter.getType())) {
                throw new MybatisExtException("Unsupported parameter type: " + parameter.getType().getName() + ". Method: " + jpaTokenizer.getText());
            }
            PropertyInfo propertyInfo = parseProperty(configuration, jpaTokenizer.getTableInfo(), param.value());
            Condition condition = new Condition();
            condition.setPropertyInfo(propertyInfo);
            condition.setRel(ConditionRel.Equals);
            condition.setVariable(param.value());
            if (conditionList == null) {
                conditionList = new ConditionList(condition);
            } else {
                conditionList = new ConditionList(condition, conditionList, ConditionListRel.And);
            }
        }
        return conditionList;
    }

    private void ensureConditionVariable(Configuration configuration, ConditionList conditionList, Parameter[] parameters, TableInfo tableInfo, String methodName) {
        int i = 0;
        Set<String> set = new HashSet<>();
        for (; conditionList != null; conditionList = conditionList.getTailList()) {
            while (i < parameters.length && !configuration.getTypeHandlerRegistry().hasTypeHandler(parameters[i].getType())) {
                i++;
            }
            Condition condition = conditionList.getCondition();
            if (condition.getConditionList() != null) {
                continue;
            }
            if (StringUtils.isBlank(condition.getVariable())) {
                if (i >= parameters.length) {
                    throw new MybatisExtException("Insufficient parameters for method: " + methodName);
                }
                Param param = parameters[i].getAnnotation(Param.class);
                condition.setVariable(param != null ? param.value() : "param" + i + 1);
                i++;
            } else {
                String firstName = condition.getVariable().split("\\.")[0];
                if (!set.contains(firstName)) {
                    set.add(firstName);
                    i++;
                }
            }
            if (condition.getRel() == ConditionRel.Between) {
                while (i < parameters.length && !configuration.getTypeHandlerRegistry().hasTypeHandler(parameters[i].getType())) {
                    i++;
                }
                if (StringUtils.isBlank(condition.getSecondVariable())) {
                    if (i >= parameters.length) {
                        throw new MybatisExtException("Insufficient parameters for method: " + methodName);
                    }
                    Param param = parameters[i].getAnnotation(Param.class);
                    condition.setSecondVariable(param != null ? param.value() : "param" + i + 1);
                    i++;
                } else {
                    String firstName = condition.getSecondVariable().split("\\.")[0];
                    if (!set.contains(firstName)) {
                        set.add(firstName);
                        i++;
                    }
                }
            }
        }
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
            throw new ParserException(jpaTokenizer.getExpectedTokens().toString());
        }
        if (tokenMarkers.size() > 1) {
            tokenMarkers.get(0).printDiff(tokenMarkers.get(tokenMarkers.size() - 1), System.err);
            throw new ParserException("Conflict detected at column " + tokenMarkers.get(0).getDiffBegin(tokenMarkers.get(tokenMarkers.size() - 1)));
        }
        return reference.get();
    }

    public Semantic parse(Configuration configuration, TableInfo tableInfo, String methodName, Parameter[] parameters) {
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
            throw new ParserException(jpaTokenizer.getExpectedTokens().toString());
        }
        if (tokenMarkers.size() > 1) {
            tokenMarkers.get(0).printDiff(tokenMarkers.get(tokenMarkers.size() - 1), System.err);
            throw new ParserException("Conflict detected at column " + tokenMarkers.get(0).getDiffBegin(tokenMarkers.get(tokenMarkers.size() - 1)));
        }
        ensureConditionVariable(configuration, reference.get().getConditionList(), parameters, tableInfo, methodName);
        return reference.get();
    }
}
