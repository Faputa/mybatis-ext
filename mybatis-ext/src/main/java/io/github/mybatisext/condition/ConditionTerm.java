package io.github.mybatisext.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import io.github.mybatisext.annotation.IfTest;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.ognl.Ognl;
import io.github.mybatisext.util.SimpleStringTemplate;

public class ConditionTerm implements Condition {

    private PropertyInfo propertyInfo;
    private boolean ignorecase;
    private boolean not;
    private ConditionRel rel;
    private Variable variable;
    private Variable secondVariable;
    private IfTest test = IfTest.None;

    public PropertyInfo getPropertyInfo() {
        return propertyInfo;
    }

    public void setPropertyInfo(PropertyInfo propertyInfo) {
        this.propertyInfo = propertyInfo;
    }

    public boolean isIgnorecase() {
        return ignorecase;
    }

    public void setIgnorecase(boolean ignorecase) {
        this.ignorecase = ignorecase;
    }

    public boolean isNot() {
        return not;
    }

    public void setNot(boolean not) {
        this.not = not;
    }

    public ConditionRel getRel() {
        return rel;
    }

    public void setRel(ConditionRel rel) {
        this.rel = rel;
    }

    @Override
    public Variable getVariable() {
        return variable;
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    public Variable getSecondVariable() {
        return secondVariable;
    }

    public void setSecondVariable(Variable secondVariable) {
        this.secondVariable = secondVariable;
    }

    @Override
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
        ConditionTerm that = (ConditionTerm) o;
        return ignorecase == that.ignorecase && not == that.not && Objects.equals(propertyInfo, that.propertyInfo) && rel == that.rel && Objects.equals(variable, that.variable) && Objects.equals(secondVariable, that.secondVariable) && test == that.test;
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyInfo, ignorecase, not, rel, variable, secondVariable, test);
    }

    @Override
    public void collectDirectTableAliases(Set<String> tableAliases) {
        tableAliases.add(propertyInfo.getJoinTableInfo().getAlias());
    }

    @Override
    public String toScriptlet(Dialect dialect) {
        List<String> ss = new ArrayList<>();
        if (not) {
            ss.add("NOT");
        }
        switch (rel) {
            case Equals: {
                if (ignorecase) {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                    ss.add(dialect.upper("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName}") + " = #{__{variable}__bind}");
                } else {
                    ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} = #{{variable}}");
                }
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case LessThan: {
                if (ignorecase) {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                    ss.add(dialect.upper("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName}") + " &lt; #{__{variable}__bind}");
                } else {
                    ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} &lt; #{{variable}}");
                }
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case LessThanEqual: {
                if (ignorecase) {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                    ss.add(dialect.upper("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName}") + " &lt;= #{__{variable}__bind}");
                } else {
                    ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} &lt;= #{{variable}}");
                }
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case GreaterThan: {
                if (ignorecase) {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                    ss.add(dialect.upper("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName}") + " &gt; #{__{variable}__bind}");
                } else {
                    ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} &gt; #{{variable}}");
                }
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case GreaterThanEqual: {
                if (ignorecase) {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                    ss.add(dialect.upper("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName}") + " &gt;= #{__{variable}__bind}");
                } else {
                    ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} &gt;= #{{variable}}");
                }
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case Like: {
                if (ignorecase) {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"'%' + " + Ognl.ToUpperCase + "({variable}) + '%'\"/>");
                    ss.add(dialect.upper("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName}") + " &gt;= #{__{variable}__bind}");
                } else {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"'%' + ${{variable}} + '%'\"/>");
                    ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} &gt;= #{__{variable}__bind}");
                }
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case StartWith: {
                if (ignorecase) {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable}) + '%'\"/>");
                    ss.add(dialect.upper("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName}") + " &gt;= #{__{variable}__bind}");
                } else {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"${{variable}} + '%'\"/>");
                    ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} &gt;= #{__{variable}__bind}");
                }
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case EndWith: {
                if (ignorecase) {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"'%' + " + Ognl.ToUpperCase + "({variable})\"/>");
                    ss.add(dialect.upper("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName}") + " &gt;= #{__{variable}__bind}");
                } else {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"'%' + ${{variable}}\"/>");
                    ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} &gt;= #{__{variable}__bind}");
                }
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case Between: {
                if (ignorecase) {
                    ss.add("<bind name=\"__{variable}__bind\" value=\"" + Ognl.ToUpperCase + "({variable})\"/>");
                    ss.add("<bind name=\"__{secondVariable}__bind\" value=\"" + Ognl.ToUpperCase + "({secondVariable})\"/>");
                    ss.add(dialect.upper("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName}") + " BETWEEN #{__{variable}__bind} AND #{__{secondVariable}__bind}");
                } else {
                    ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} BETWEEN #{{variable}} AND #{{secondVariable}}");
                }
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case In: {
                if (ignorecase) {
                    ss.add(dialect.upper("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName}") + " IN <foreach collection=\"{variable}\" item=\"__{variable}__item\" separator=\",\" open=\"(\" close=\")\">");
                    ss.add("<bind name=\"__{variable}__item\" value=\"" + Ognl.ToUpperCase + "(__{variable}__item)\"/>");
                } else {
                    ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} IN <foreach collection=\"{variable}\" item=\"__{variable}__item\" separator=\",\" open=\"(\" close=\")\">");
                }
                ss.add("#{__{variable}__item}");
                ss.add("</foreach>");
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case IsNull: {
                ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} IS NULL");
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case IsNotNull: {
                ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} IS NOT NULL");
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case IsTrue: {
                ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} " + dialect.isTrue());
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
            case IsFalse: {
                ss.add("{propertyInfo.joinTableInfo.alias}.{propertyInfo.columnName} " + dialect.isFalse());
                return SimpleStringTemplate.build(String.join(" ", ss), this);
            }
        }
        throw new MybatisExtException("Unsupported condition type:" + rel);
    }
}
