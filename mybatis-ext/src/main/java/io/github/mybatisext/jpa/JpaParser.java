package io.github.mybatisext.jpa;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

public class JpaParser extends BaseParser {

    Symbol grammar = new Symbol("grammar");
    Symbol conditionList = new Symbol("conditionList");
    Symbol condition = new Symbol("condition");
    Symbol modifierList = new Symbol("modifierList");
    Symbol modifier = new Symbol("modifier");
    Symbol propertyList = new Symbol("propertyList");
    Symbol property = new Symbol("property");

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
            if (state.setResult(propertyInfo) && continuation.test(state)) {
                return true;
            }
            jpaTokenizer.setCursor(cursor);
        }
        return false;
    });

    Symbol integer = new Symbol("integer").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        int i = jpaTokenizer.integer();
        return i > -1 && state.setResult(i) && continuation.test(state);
    });

    Symbol variable = new Symbol("variable").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        int cursor = jpaTokenizer.getCursor();
        for (String s : jpaTokenizer.variable()) {
            jpaTokenizer.setCursor(cursor + s.length());
            if (state.setResult(s) && continuation.test(state)) {
                return true;
            }
            jpaTokenizer.setCursor(cursor);
        }
        return false;
    });

    Symbol end = new Symbol("end").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        return jpaTokenizer.getCursor() == jpaTokenizer.getText().length() && continuation.test(state);
    });

    Symbol keyword(String s) {
        return assign(s, new Symbol("keyword(" + s + ")").set((state, continuation) -> {
            JpaTokenizer jpaTokenizer = state.getTokenizer();
            return !jpaTokenizer.keyword(s).isEmpty() && continuation.test(state);
        }));
    }

    public JpaParser() {
        grammar.set(choice(
                join(choice(keyword("find"), keyword("select"), keyword("list"), keyword("get")), optional(keyword("Distinct")), optional(choice(keyword("All"), keyword("One"), join(keyword("Top"), choice(integer, variable)))), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), optional(modifierList), end, action(state -> {
                    Semantic semantic = new Semantic();
                    semantic.setType(SemanticType.SELECT);
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
                    }
                    MatchResult _modifierList = state.getMatch(modifierList);
                    if (_modifierList != null) {
                        semantic.setModifierList(_modifierList.val());
                    }
                    state.setReturn(semantic);
                })),
                join(keyword("exists"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), optional(modifierList), end, action(state -> {
                    Semantic semantic = new Semantic();
                    semantic.setType(SemanticType.EXISTS);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setConditionList(_conditionList.val());
                    }
                    MatchResult _modifierList = state.getMatch(modifierList);
                    if (_modifierList != null) {
                        semantic.setModifierList(_modifierList.val());
                    }
                    state.setReturn(semantic);
                })),
                join(keyword("count"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), optional(modifierList), end, action(state -> {
                    Semantic semantic = new Semantic();
                    semantic.setType(SemanticType.COUNT);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setConditionList(_conditionList.val());
                    }
                    MatchResult _modifierList = state.getMatch(modifierList);
                    if (_modifierList != null) {
                        semantic.setModifierList(_modifierList.val());
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("update"), keyword("modify")), optional(keyword("Batch")), optional(keyword("IgnoreNull")), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic();
                    semantic.setType(SemanticType.UPDATE);
                    if (state.getMatch("IgnoreNull") != null) {
                        semantic.setIgnoreNull(true);
                    }
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setConditionList(_conditionList.val());
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("delete"), keyword("remove")), optional(keyword("Batch")), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), end, action(state -> {
                    Semantic semantic = new Semantic();
                    semantic.setType(SemanticType.DELETE);
                    MatchResult _conditionList = state.getMatch(conditionList);
                    if (_conditionList != null) {
                        semantic.setConditionList(_conditionList.val());
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("save"), keyword("insert")), optional(keyword("Batch")), end, action(state -> {
                    Semantic semantic = new Semantic();
                    semantic.setType(SemanticType.INSERT);
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
        condition.set(join(property, choice(
                join(optional(keyword("Ignorecase")), optional(keyword("Not")), optional(choice(
                        join(keyword("Is"), optional(variable)),
                        join(keyword("Equals"), optional(variable)),
                        join(keyword("LessThan"), optional(variable)),
                        join(keyword("LessThanEqual"), optional(variable)),
                        join(keyword("GreaterThan"), optional(variable)),
                        join(keyword("GreaterThanEqual"), optional(variable)),
                        join(keyword("Like"), optional(variable)),
                        join(keyword("StartWith"), optional(variable)),
                        join(keyword("EndWith"), optional(variable)),
                        join(keyword("Between"), optional(join(variable, keyword("To"), variableB))),
                        join(keyword("In"), optional(variable))))),
                join(optional(keyword("Not")), choice(
                        keyword("IsNull"),
                        keyword("IsNotNull"),
                        keyword("IsTrue"),
                        keyword("IsFalse")))),
                action(state -> {
                    Condition condition = new Condition();
                    condition.setPropertyInfo(state.getMatch(property).val());
                    for (ConditionRel rel : ConditionRel.values()) {
                        if (state.getMatch(rel.name()) != null) {
                            condition.setRel(rel);
                            break;
                        }
                    }
                    if (condition.getRel() == null) {
                        condition.setRel(ConditionRel.Equals);
                    }
                    if (state.getMatch("Ignorecase") != null) {
                        condition.setIgnorecase(true);
                    }
                    if (state.getMatch("Not") != null) {
                        condition.setNot(true);
                    }
                    MatchResult _variable = state.getMatch(variable);
                    if (_variable != null) {
                        condition.setVariableA(_variable.val());
                        if (ConditionRel.Between == condition.getRel()) {
                            condition.setVariableB(state.getMatch(variableB).val());
                        }
                    }
                    state.setReturn(condition);
                })));

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
    }

    public Semantic parse(TableInfo tableInfo, String methodName, Parameter[] parameters) {
        AtomicReference<Semantic> reference = new AtomicReference<>();
        JpaTokenizer jpaTokenizer = new JpaTokenizer(tableInfo, methodName, parameters);
        grammar.match(jpaTokenizer, state -> {
            Semantic semantic = (Semantic) state.getResult();
            reference.set(semantic);
            return true;
        });
        return reference.get();
    }
}
