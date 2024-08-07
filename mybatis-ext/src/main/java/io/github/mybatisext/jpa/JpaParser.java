package io.github.mybatisext.jpa;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

public class JpaParser extends BaseParser {

    private static final String SEMANTIC = "semantic";
    private static final String CONDITION = "condition";
    private static final String MODIFIER = "modifier";

    Symbol grammar = new Symbol("grammar");
    Symbol conditionList = new Symbol("conditionList");
    Symbol condition = new Symbol("condition");
    Symbol modifier = new Symbol("modifier");
    Symbol propertyList = new Symbol("propertyList");
    Symbol property = new Symbol("property");

    Symbol propertyName = new Symbol("propertyName").setMatch((state, continuation) -> {
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

    Symbol integer = new Symbol("integer").setMatch((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        return jpaTokenizer.integer() > -1 && continuation.test(state);
    });

    Symbol variable = new Symbol("variable").setMatch((state, continuation) -> {
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

    Symbol end = new Symbol("end").setMatch((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        return jpaTokenizer.getCursor() == jpaTokenizer.getText().length() && continuation.test(state);
    });

    Symbol keyword(String s) {
        return new Symbol("keyword(" + s + ")").setMatch((state, continuation) -> {
            JpaTokenizer jpaTokenizer = state.getTokenizer();
            return jpaTokenizer.keyword(s).equals(s) && continuation.test(state);
        });
    }

    public JpaParser() {
        grammar.set(join(choice(join(choice(keyword("find"), keyword("select"), keyword("list"), keyword("get")), action(state -> {
            Semantic semantic = new Semantic();
            semantic.setType(SemanticType.SELECT);
            state.setGlobal(SEMANTIC, semantic);
        }), optional(join(keyword("Distinct"), action(state -> {
            Semantic semantic = new Semantic(state.getGlobal(SEMANTIC));
            semantic.setDistinct(true);
            state.setGlobal(SEMANTIC, semantic);
        }))), optional(choice(keyword("All"), keyword("One"), join(keyword("Top"), choice(join(integer, action(state -> {
            Semantic semantic = new Semantic(state.getGlobal(SEMANTIC));
            Limit limit = new Limit();
            limit.setRowCount(state.getMatch(integer).val());
            semantic.setLimit(limit);
            state.setGlobal(SEMANTIC, semantic);
        })), join(variable, action(state -> {
            Semantic semantic = new Semantic(state.getGlobal(SEMANTIC));
            Limit limit = new Limit();
            limit.setRowCountVariable(state.getMatch(variable).val());
            semantic.setLimit(limit);
            state.setGlobal(SEMANTIC, semantic);
        })))))), optional(join(choice(keyword("By"), keyword("Where")), conditionList, action(state -> {
            Semantic semantic = new Semantic(state.getGlobal(SEMANTIC));
            semantic.setConditionList(state.getMatch(conditionList).val());
            state.setGlobal(SEMANTIC, semantic);
        }))), star(modifier)), join(keyword("exists"), action(state -> {
            Semantic semantic = new Semantic();
            semantic.setType(SemanticType.EXISTS);
            state.setGlobal(SEMANTIC, semantic);
        }), optional(join(choice(keyword("By"), keyword("Where")), conditionList, action(state -> {
            Semantic semantic = new Semantic(state.getGlobal(SEMANTIC));
            semantic.setConditionList(state.getMatch(conditionList).val());
            state.setGlobal(SEMANTIC, semantic);
        }))), star(modifier)), join(keyword("count"), action(state -> {
            Semantic semantic = new Semantic();
            semantic.setType(SemanticType.COUNT);
            state.setGlobal(SEMANTIC, semantic);
        }), optional(join(choice(keyword("By"), keyword("Where")), conditionList, action(state -> {
            Semantic semantic = new Semantic(state.getGlobal(SEMANTIC));
            semantic.setConditionList(state.getMatch(conditionList).val());
            state.setGlobal(SEMANTIC, semantic);
        }))), star(modifier)), join(choice(keyword("update"), keyword("modify")), action(state -> {
            Semantic semantic = new Semantic();
            semantic.setType(SemanticType.UPDATE);
            state.setGlobal(SEMANTIC, semantic);
        }), optional(keyword("Batch")), optional(join(keyword("IgnoreNull"), action(state -> {
            Semantic semantic = new Semantic(state.getGlobal(SEMANTIC));
            semantic.setIgnoreNull(true);
            state.setGlobal(SEMANTIC, semantic);
        }))), optional(join(choice(keyword("By"), keyword("Where")), conditionList, action(state -> {
            Semantic semantic = new Semantic(state.getGlobal(SEMANTIC));
            semantic.setConditionList(state.getMatch(conditionList).val());
            state.setGlobal(SEMANTIC, semantic);
        })))), join(choice(keyword("delete"), keyword("remove")), action(state -> {
            Semantic semantic = new Semantic();
            semantic.setType(SemanticType.DELETE);
            state.setGlobal(SEMANTIC, semantic);
        }), optional(keyword("Batch")), optional(join(choice(keyword("By"), keyword("Where")), conditionList, action(state -> {
            Semantic semantic = new Semantic(state.getGlobal(SEMANTIC));
            semantic.setConditionList(state.getMatch(conditionList).val());
            state.setGlobal(SEMANTIC, semantic);
        })))), join(choice(keyword("save"), keyword("remove")), action(state -> {
            Semantic semantic = new Semantic();
            semantic.setType(SemanticType.INSERT);
            state.setGlobal(SEMANTIC, semantic);
        }), optional(keyword("Batch")))), end));

        conditionList.set(choice(join(condition, action(state -> {
            Condition c = state.getMatch(condition).val();
            state.setReturn(new ConditionList(c));
        }), optional(choice(join(keyword("And"), conditionList, action(state -> {
            Condition c = state.getMatch(condition).val();
            ConditionList list = state.getMatch(conditionList).val();
            state.setReturn(new ConditionList(c, list, ConditionListRel.And));
        })), join(keyword("Or"), conditionList, action(state -> {
            Condition c = state.getMatch(condition).val();
            ConditionList list = state.getMatch(conditionList).val();
            state.setReturn(new ConditionList(c, list, ConditionListRel.Or));
        })))))));

        condition.set(choice(join(property, action(state -> {
            Condition c = new Condition();
            c.setPropertyInfo(state.getMatch(property).val());
            state.setGlobal(CONDITION, c);
        }), optional(join(keyword("Ignorecase"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setIgnorecase(true);
            state.setGlobal(CONDITION, c);
        }))), optional(join(keyword("Not"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setNot(true);
            state.setGlobal(CONDITION, c);
        }))), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.Equals);
            state.setGlobal(CONDITION, c);
        }), optional(choice(join(keyword("Is"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.Equals);
            state.setGlobal(CONDITION, c);
        }), optional(join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableA(state.getMatch(variable).val());
            state.setGlobal(CONDITION, c);
        })))), join(keyword("Equals"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.Equals);
            state.setGlobal(CONDITION, c);
        }), optional(join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableA(state.getMatch(variable).val());
            state.setGlobal(CONDITION, c);
        })))), join(keyword("LessThan"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.LessThan);
            state.setGlobal(CONDITION, c);
        }), optional(join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableA(state.getMatch(variable).val());
            state.setGlobal(CONDITION, c);
        })))), join(keyword("LessThanEqual"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.LessThanEqual);
            state.setGlobal(CONDITION, c);
        }), optional(join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableA(state.getMatch(variable).val());
            state.setGlobal(CONDITION, c);
        })))), join(keyword("GreaterThan"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.GreaterThan);
            state.setGlobal(CONDITION, c);
        }), optional(join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableA(state.getMatch(variable).val());
            state.setGlobal(CONDITION, c);
        })))), join(keyword("GreaterThanEqual"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.GreaterThanEqual);
            state.setGlobal(CONDITION, c);
        }), optional(join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableA(state.getMatch(variable).val());
            state.setGlobal(CONDITION, c);
        })))), join(keyword("Like"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.Like);
            state.setGlobal(CONDITION, c);
        }), optional(join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableA(state.getMatch(variable).val());
            state.setGlobal(CONDITION, c);
        })))), join(keyword("StartWith"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.StartWith);
            state.setGlobal(CONDITION, c);
        }), optional(join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableA(state.getMatch(variable).val());
            state.setGlobal(CONDITION, c);
        })))), join(keyword("EndWith"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.EndWith);
            state.setGlobal(CONDITION, c);
        }), optional(join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableA(state.getMatch(variable).val());
            state.setGlobal(CONDITION, c);
        })))), join(keyword("Between"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.Between);
            state.setGlobal(CONDITION, c);
        }), optional(join(join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableA(state.getMatch(variable).val());
            state.setGlobal(CONDITION, c);
        })), keyword("To"), join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableB(state.getMatch(variable, -1).text());
            state.setGlobal(CONDITION, c);
        }))))), join(keyword("In"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.In);
            state.setGlobal(CONDITION, c);
        }), optional(join(variable, action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setVariableA(state.getMatch(variable).val());
            state.setGlobal(CONDITION, c);
        })))))), action(state -> {
            Condition c = state.getGlobal(CONDITION);
            state.setReturn(c);
        })), join(property, action(state -> {
            Condition c = new Condition();
            c.setPropertyInfo(state.getMatch(property).val());
            state.setGlobal(CONDITION, c);
        }), optional(join(keyword("Not"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setNot(true);
            state.setGlobal(CONDITION, c);
        }))), optional(choice(join(keyword("IsNull"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.IsNull);
            state.setGlobal(CONDITION, c);
        })), join(keyword("IsNotNull"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.IsNotNull);
            state.setGlobal(CONDITION, c);
        })), join(keyword("IsTrue"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.IsTrue);
            state.setGlobal(CONDITION, c);
        })), join(keyword("IsFalse"), action(state -> {
            Condition c = new Condition(state.getGlobal(CONDITION));
            c.setRel(ConditionRel.IsFalse);
            state.setGlobal(CONDITION, c);
        })))), action(state -> {
            Condition c = state.getGlobal(CONDITION);
            state.setReturn(c);
        }))));

        modifier.set(choice(join(keyword("OrderBy"), propertyList, action(state -> {
            OrderBy by = new OrderBy(state.getMatch(propertyList).val());
            state.setGlobal(MODIFIER, by);
        }), optional(choice(join(keyword("Asc"), action(state -> {
            OrderBy by = state.getGlobal(MODIFIER);
            by.setType(OrderByType.ASC);
        })), join(keyword("Desc"), action(state -> {
            OrderBy by = state.getGlobal(MODIFIER);
            by.setType(OrderByType.ASC);
        })))), action(state -> {
            Semantic semantic = new Semantic(state.getGlobal(SEMANTIC));
            OrderBy by = state.getGlobal(MODIFIER);
            semantic.setOrderBy(by);
            state.setGlobal(SEMANTIC, semantic);
        })), join(keyword("GroupBy"), propertyList, action(state -> {
            GroupBy by = new GroupBy(state.getMatch(propertyList).val());
            state.setGlobal(MODIFIER, by);
        })), join(keyword("Limit"), action(state -> {
            Limit limit = new Limit();
            state.setGlobal(MODIFIER, limit);
        }), choice(join(integer, action(state -> {
            Limit limit = state.getGlobal(MODIFIER);
            limit.setOffset(state.getMatch(integer).val());
            state.setGlobal(MODIFIER, limit);
        })), join(variable, action(state -> {
            Limit limit = state.getGlobal(MODIFIER);
            limit.setOffsetVariable(state.getMatch(variable).val());
            state.setGlobal(MODIFIER, limit);
        }))), keyword("To"), choice(join(integer, action(state -> {
            Limit limit = state.getGlobal(MODIFIER);
            limit.setRowCount(state.getMatch(integer).val());
            state.setGlobal(MODIFIER, limit);
        })), join(variable, action(state -> {
            Limit limit = state.getGlobal(MODIFIER);
            limit.setRowCountVariable(state.getMatch(variable).val());
            state.setGlobal(MODIFIER, limit);
        })))), join(keyword("Limit"), action(state -> {
            Limit limit = new Limit();
            state.setGlobal(MODIFIER, limit);
        }), choice(join(integer, action(state -> {
            Limit limit = state.getGlobal(MODIFIER);
            limit.setRowCount(state.getMatch(integer).val());
            state.setGlobal(MODIFIER, limit);
        })), join(variable, action(state -> {
            Limit limit = state.getGlobal(MODIFIER);
            limit.setRowCountVariable(state.getMatch(variable).val());
            state.setGlobal(MODIFIER, limit);
        }))))));

        propertyList.set(join(property, action(state -> {
            PropertyInfo info = state.getMatch(property).val();
            state.setReturn(new PropertyList(info));
        }), optional(join(keyword("And"), propertyList, action(state -> {
            PropertyInfo info = state.getMatch(property).val();
            PropertyList list = state.getMatch(propertyList).val();
            state.setReturn(new PropertyList(info, list));
        })))));

        property.set(join(propertyName, star(join(keyword("Dot"), propertyName))));
    }

    public Semantic parse(TableInfo tableInfo, String methodName, Parameter[] parameters) {
        AtomicReference<Semantic> reference = new AtomicReference<>();
        JpaTokenizer jpaTokenizer = new JpaTokenizer(tableInfo, methodName, parameters);
        grammar.match(jpaTokenizer, state -> {
            Semantic semantic = state.getGlobal(SEMANTIC);
            reference.set(semantic);
            return true;
        });
        return reference.get();
    }
}
