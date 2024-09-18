package io.github.mybatisext.condition;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import io.github.mybatisext.annotation.IfTest;

public class ConditionComp implements Condition {

    private final Set<Condition> conditions = new HashSet<>();
    private final ConditionCompRel rel;
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

    public IfTest getTest() {
        return test;
    }

    public void setTest(@Nonnull IfTest test) {
        this.test = test;
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
}
