package io.github.mybatisext.condition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.ognl.Ognl;

public class ConditionComp implements Condition {

    private final Set<Condition> conditions = new HashSet<>();
    private final ConditionCompRel rel;
    private Variable variable;
    private IfTest test = IfTest.None;

    public ConditionComp(ConditionCompRel rel) {
        this.rel = rel;
    }

    public Set<Condition> getConditions() {
        return conditions;
    }

    public ConditionCompRel getRel() {
        return rel;
    }

    @Override
    public Variable getVariable() {
        return variable;
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    @Override
    public IfTest getTest() {
        return test;
    }

    public void setTest(@Nonnull IfTest test) {
        this.test = test;
    }

    public boolean hasTest() {
        for (Condition condition : conditions) {
            if (condition.getTest() != null && condition.getTest() != IfTest.None) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConditionComp that = (ConditionComp) o;
        return Objects.equals(conditions, that.conditions) && rel == that.rel && test == that.test;
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditions, rel, test);
    }

    @Override
    public void collectDirectTableAliases(Set<String> tableAliases) {
        for (Condition condition : conditions) {
            condition.collectDirectTableAliases(tableAliases);
        }
    }

    @Override
    public String toScriptlet(Dialect dialect) {
        List<String> ss = new ArrayList<>();
        if (!hasTest()) {
            for (Condition condition : conditions) {
                ss.add(condition.toScriptlet(dialect));
            }
            return "(" + String.join(" " + rel + " ", ss) + ")";
        }
        ss.add("<trim prefix=\"(\" suffix=\")\" prefixOverrides=\"" + rel + "\" >");
        for (Condition condition : conditions) {
            if (condition.getTest() == IfTest.NotNull) {
                ss.add("<if test=\"" + condition.getVariable() + " != null\">");
                ss.add(rel.toString());
                ss.add(condition.toScriptlet(dialect));
                ss.add("</if>");
            } else if (condition.getTest() == IfTest.NotEmpty) {
                ss.add("<if test=\"" + Ognl.IsNotEmpty + "(" + condition.getVariable() + ")\">");
                ss.add(rel.toString());
                ss.add(condition.toScriptlet(dialect));
                ss.add("</if>");
            } else {
                ss.add(rel.toString());
                ss.add(condition.toScriptlet(dialect));
            }
        }
        ss.add("</trim>");
        return String.join(" ", ss);
    }
}
