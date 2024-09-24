package io.github.mybatisext.condition;

import java.util.Set;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.jpa.Variable;

public interface Condition {

    void collectDirectTableAliases(Set<String> tableAliases);

    String toScriptlet(Dialect dialect);

    Variable getVariable();

    IfTest getTest();
}
