package io.github.mybatisext.jpa;

public class JpaParser extends BaseParser {

    Symbol grammar = new Symbol("grammar");
    Symbol conditionList = new Symbol("conditionList");
    Symbol condition = new Symbol("condition");
    Symbol modifier = new Symbol("modifier");
    Symbol propertyList = new Symbol("propertyList");

    Symbol property = new Symbol("property").setMatch((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        int cursor = jpaTokenizer.getCursor();
        for (String s : jpaTokenizer.property()) {
            if (!s.isEmpty() && continuation.test(state)) {
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
        return !jpaTokenizer.variable().isEmpty() && continuation.test(state);
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
        grammar.set(choice(
                join(choice(keyword("find"), keyword("select"), keyword("list"), keyword("get")), optional(keyword("Distinct")), optional(choice(keyword("All"), keyword("One"), join(keyword("Top"), choice(integer, variable)))), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), star(modifier)),
                join(keyword("exists"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), star(modifier)),
                join(keyword("count"), optional(join(choice(keyword("By"), keyword("Where")), conditionList)), star(modifier)),
                join(choice(keyword("update"), keyword("modify")), optional(keyword("Batch")), optional(keyword("IgnoreNull")), optional(join(choice(keyword("By"), keyword("Where")), conditionList))),
                join(choice(keyword("delete"), keyword("remove")), optional(keyword("Batch")), optional(join(choice(keyword("By"), keyword("Where")), conditionList))),
                join(choice(keyword("save"), keyword("remove")), optional(keyword("Batch")))));

        conditionList.set(choice(
                join(condition, keyword("And"), conditionList),
                join(condition, keyword("Or"), conditionList),
                join(condition)));

        condition.set(choice(
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not")), keyword("Is"), optional(variable)),
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not")), keyword("Equals"), optional(variable)),
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not")), keyword("LessThan"), optional(variable)),
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not")), keyword("LessThanEqual"), optional(variable)),
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not")), keyword("GreaterThan"), optional(variable)),
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not")), keyword("GreaterThanEqual"), optional(variable)),
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not")), keyword("Like"), optional(variable)),
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not")), keyword("StartWith"), optional(variable)),
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not")), keyword("EndWith"), optional(variable)),
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not")), keyword("Between"), optional(join(variable, keyword("To"), variable))),
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not")), keyword("In"), optional(variable)),
                join(property, optional(keyword("Ignorecase")), optional(keyword("Not"))),
                join(property, optional(keyword("Not")), keyword("IsNull")),
                join(property, optional(keyword("Not")), keyword("IsNotNull")),
                join(property, optional(keyword("Not")), keyword("IsTrue")),
                join(property, optional(keyword("Not")), keyword("IsFalse"))));

        modifier.set(choice(
                join(keyword("OrderBy"), propertyList, keyword("Asc")),
                join(keyword("OrderBy"), propertyList, keyword("Desc")),
                join(keyword("OrderBy"), propertyList),
                join(keyword("GroupBy"), propertyList),
                join(keyword("Limit"), choice(integer, variable), keyword("To"), choice(integer, variable)),
                join(keyword("Limit"), choice(integer, variable))));

        propertyList.set(choice(
                join(property, keyword("And"), propertyList),
                join(property)));
    }
}
