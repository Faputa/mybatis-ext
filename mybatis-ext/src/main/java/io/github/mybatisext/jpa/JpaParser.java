package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.analysis.AnalysisResult;
import io.github.mybatisext.analysis.CompletionCollector;
import io.github.mybatisext.analysis.CompletionItem;
import io.github.mybatisext.analysis.SymbolReference;
import io.github.mybatisext.annotation.Filterable;
import io.github.mybatisext.annotation.OnlyById;
import io.github.mybatisext.annotation.TestMode;
import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.diagnostic.DiagnosticLocation;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.FilterableInfo;
import io.github.mybatisext.metadata.JoinColumnInfo;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.model.JavaAnnotation;
import io.github.mybatisext.model.JavaMethod;
import io.github.mybatisext.model.JavaParameter;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.JavaTypes;
import io.github.mybatisext.model.TypePolicy;
import io.github.mybatisext.model.UnresolvedTypeException;
import io.github.mybatisext.reflect.ReflectionTypePolicy;
import io.github.mybatisext.statement.NestedSelectHelper;
import io.github.mybatisext.util.StringUtils;

public class JpaParser extends BaseParser<JpaTokenizer> {

    private final TypePolicy typePolicy;
    private final TableInfoFactory tableInfoFactory;

    private final Symbol grammar = new Symbol("grammar");
    private final Symbol conditionExpression = new Symbol("conditionExpression");
    private final Symbol andExpression = new Symbol("andExpression");
    private final Symbol condition = new Symbol("condition");
    private final Symbol propertyList = new Symbol("propertyList");
    private final Symbol orderByList = new Symbol("orderByList");
    private final Symbol limit = new Symbol("limit");

    private final Symbol propertyName = new Symbol("propertyName").set((state, continuation) -> {
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

    private final Symbol subPropertyName = new Symbol("subPropertyName").set((state, continuation) -> {
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

    private final Symbol integer = new Symbol("integer").set((state, continuation) -> {
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

    private final Symbol variableName = new Symbol("variableName").set((state, continuation) -> {
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

    private final Symbol subVariableName = new Symbol("subVariableName").set((state, continuation) -> {
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

    private final Symbol end = new Symbol("end").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        // 补全会探索各个语法分支，但不执行最终的语义绑定操作。
        if (jpaTokenizer.isCompleting()) {
            return false;
        }
        if (jpaTokenizer.getCursor() != jpaTokenizer.getText().length()) {
            jpaTokenizer.getExpectedTokens().record(jpaTokenizer.getCursor(), "end");
            return false;
        }
        return continuation.test(state);
    });

    private Symbol keyword(String s) {
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

    private final Symbol returnTypeName = new Symbol("returnTypeName").set((state, continuation) -> {
        JpaTokenizer jpaTokenizer = state.getTokenizer();
        JavaType returnType = jpaTokenizer.getReturnType();
        if (returnType == null || !returnType.isResolved()) {
            return false;
        }
        returnType = JavaTypes.unwrap(returnType);
        int cursor = jpaTokenizer.getCursor();
        if (jpaTokenizer.keyword(returnType.getSimpleName()).isEmpty()) {
            jpaTokenizer.getExpectedTokens().record(cursor, "'" + returnType.getSimpleName() + "'");
            return false;
        }
        jpaTokenizer.getTokenMarker().record(jpaTokenizer.getCursor());
        return state.setResult(returnType) && continuation.test(state);
    });

    private final Symbol property = new Symbol("property").set(join(propertyName, star(join(keyword("Dot"), subPropertyName))));
    private final Symbol variable = new Symbol("variable").set(join(variableName, star(join(keyword("Dot"), subVariableName))));
    private final Symbol integerB = new Symbol("integerB").set(integer);
    private final Symbol variableB = new Symbol("variableB").set(variable);

    private Symbol conditionAction(CompareOperator compareOperator) {
        return action(state -> {
            Condition condition = new Condition(ConditionType.BASIC);
            condition.setPropertyInfos(state.getTokenizer().getTableInfo().getNameToPropertyInfo());
            condition.setPropertyInfo(state.getMatch(property).val());
            condition.setCompareOperator(compareOperator);
            if (state.getMatch("Ignorecase") != null) {
                condition.setIgnorecase(true);
            }
            if (state.getMatch("Not") != null) {
                condition.setNot(true);
            }
            if (compareOperator.isRequiredSecondVariable()) {
                MatchResult _variable = state.getMatch(variable);
                if (_variable != null) {
                    condition.setVariable(_variable.val());
                    condition.setSecondVariable(state.getMatch(variableB).val());
                }
            } else if (compareOperator.isRequiredVariable()) {
                MatchResult _variable = state.getMatch(variable);
                if (_variable != null) {
                    condition.setVariable(_variable.val());
                } else {
                    for (Variable variable : state.getTokenizer().getVariables()) {
                        if (condition.getPropertyInfo().getName().equals(variable.getName())) {
                            condition.setVariable(variable);
                            break;
                        }
                    }
                }
            }
            state.setReturn(condition);
        });
    }

    private final Symbol groupBy = new Symbol("groupBy").set(join(keyword("GroupBy"), propertyList));
    private final Symbol having = new Symbol("having").set(join(keyword("Having"), conditionExpression));
    private final Symbol orderBy = new Symbol("orderBy").set(join(keyword("OrderBy"), orderByList));

    public JpaParser(Configuration configuration, TableInfoFactory tableInfoFactory) {
        this(new ReflectionTypePolicy(configuration), tableInfoFactory);
    }

    public JpaParser(TypePolicy typePolicy, TableInfoFactory tableInfoFactory) {
        this.typePolicy = typePolicy;
        this.tableInfoFactory = tableInfoFactory;

        grammar.set(choice(
                join(choice(keyword("find"), keyword("select"), keyword("list"), keyword("get")), optional(keyword("Distinct")), optional(choice(keyword("All"), keyword("One"), join(keyword("Top"), choice(integer, variable)))), optional(choice(propertyList, returnTypeName)), optional(join(choice(keyword("By"), keyword("Where")), conditionExpression)), optional(join(groupBy, optional(having))), optional(orderBy), optional(limit), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.SELECT);
                    MatchResult _propertyList = state.getMatch(propertyList);
                    if (_propertyList != null) {
                        semantic.setSelectItems(ensureJoinRelationColumns(state.getTokenizer(), _propertyList.val()));
                    } else {
                        semantic.setSelectItems(buildDefaultSelectItems(state.getTokenizer()));
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

                    MatchResult whereMatch = state.getMatch(conditionExpression);
                    MatchResult groupByMatch = state.getMatch(groupBy);
                    MatchResult havingMatch = null;
                    MatchResult orderByMatch = state.getMatch(orderBy);
                    MatchResult limitMatch = state.getMatch(limit);
                    if (groupByMatch != null) {
                        semantic.setGroupBy(groupByMatch.val());
                        havingMatch = state.getMatch(having);
                    }
                    if (orderByMatch != null) {
                        semantic.setOrderBy(orderByMatch.val());
                    }
                    if (limitMatch != null) {
                        semantic.setLimit(limitMatch.val());
                    }

                    // 先保留所有显式引用，再为 WHERE、HAVING 中缺失的变量按参数顺序补位。
                    Set<String> usedParamNames = new HashSet<>();
                    if (whereMatch != null) {
                        collectUsedParamNames(whereMatch.<Condition>val(), usedParamNames);
                    }
                    if (havingMatch != null) {
                        collectUsedParamNames(havingMatch.<Condition>val(), usedParamNames);
                    }
                    if (semantic.getLimit() != null) {
                        collectUsedParamNames(semantic.getLimit(), usedParamNames);
                    }

                    JpaTokenizer tokenizer = state.getTokenizer();
                    if (whereMatch != null) {
                        semantic.setWhere(ensureConditionVariable(tokenizer, usedParamNames, whereMatch.val()));
                    }
                    if (havingMatch != null) {
                        semantic.setHaving(ensureConditionVariable(tokenizer, usedParamNames, havingMatch.val()));
                    }
                    if (groupByMatch != null) {
                        if (semantic.getHaving() == null && hasDefaultConditionCandidate(tokenizer, usedParamNames)) {
                            semantic.setHaving(buildDefaultCondition(tokenizer, usedParamNames));
                        }
                    } else if (semantic.getWhere() == null && hasDefaultConditionCandidate(tokenizer, usedParamNames)) {
                        semantic.setWhere(buildDefaultCondition(tokenizer, usedParamNames));
                    }

                    state.setReturn(semantic);
                })),
                join(keyword("exists"), optional(join(choice(keyword("By"), keyword("Where")), conditionExpression)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.EXISTS);
                    MatchResult _conditionExpression = state.getMatch(conditionExpression);
                    if (_conditionExpression != null) {
                        semantic.setWhere(ensureConditionVariable(state.getTokenizer(), collectUsedParamNames(_conditionExpression.val()), _conditionExpression.val()));
                    } else {
                        semantic.setWhere(buildDefaultCondition(state.getTokenizer(), new HashSet<>()));
                    }
                    state.setReturn(semantic);
                })),
                join(keyword("count"), optional(join(choice(keyword("By"), keyword("Where")), conditionExpression)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.COUNT);
                    MatchResult _conditionExpression = state.getMatch(conditionExpression);
                    if (_conditionExpression != null) {
                        semantic.setWhere(ensureConditionVariable(state.getTokenizer(), collectUsedParamNames(_conditionExpression.val()), _conditionExpression.val()));
                    } else {
                        semantic.setWhere(buildDefaultCondition(state.getTokenizer(), new HashSet<>()));
                    }
                    state.setReturn(semantic);
                })),
                join(choice(keyword("update"), keyword("modify")), optional(keyword("Batch")), optional(propertyList), optional(keyword("IgnoreNull")), optional(join(choice(keyword("By"), keyword("Where")), conditionExpression)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.UPDATE);
                    // TODO 考虑支持updateSet
                    MatchResult _propertyList = state.getMatch(propertyList);
                    if (_propertyList != null) {
                        semantic.setSelectItems(ensureJoinRelationColumns(state.getTokenizer(), _propertyList.val()));
                    } else {
                        semantic.setSelectItems(state.getTokenizer().getTableInfo().getNameToPropertyInfo().values().stream().filter(PropertyInfo::isOwnColumn).collect(Collectors.toList()));
                    }
                    semantic.setParameter(buildSemanticParameter(state.getTokenizer(), true));
                    if (state.getMatch("IgnoreNull") != null) {
                        semantic.setIgnoreNull(true);
                    }
                    Condition condition;
                    MatchResult _conditionExpression = state.getMatch(conditionExpression);
                    if (_conditionExpression != null) {
                        condition = ensureConditionVariable(state.getTokenizer(), collectUsedParamNames(_conditionExpression.val()), _conditionExpression.val());
                    } else {
                        condition = buildDefaultCondition(state.getTokenizer(), new HashSet<>());
                    }
                    if (condition == null) {
                        throw state.getTokenizer().semanticError("Condition cannot be null for update: " + state.getTokenizer().getText());
                    }
                    semantic.setWhere(condition);
                    state.setReturn(semantic);
                })),
                join(choice(keyword("delete"), keyword("remove")), optional(keyword("Batch")), optional(join(choice(keyword("By"), keyword("Where")), conditionExpression)), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.DELETE);
                    semantic.setParameter(buildSemanticParameter(state.getTokenizer(), false));
                    Condition condition;
                    MatchResult _conditionExpression = state.getMatch(conditionExpression);
                    if (_conditionExpression != null) {
                        condition = ensureConditionVariable(state.getTokenizer(), collectUsedParamNames(_conditionExpression.val()), _conditionExpression.val());
                    } else {
                        condition = buildDefaultCondition(state.getTokenizer(), new HashSet<>());
                    }
                    if (condition == null) {
                        throw state.getTokenizer().semanticError("Condition cannot be null for delete: " + state.getTokenizer().getText());
                    }
                    semantic.setWhere(condition);
                    state.setReturn(semantic);
                })),
                join(choice(keyword("save"), keyword("insert")), optional(keyword("Batch")), optional(keyword("IgnoreNull")), end, action(state -> {
                    Semantic semantic = new Semantic(SemanticType.INSERT);
                    semantic.setParameter(buildSemanticParameter(state.getTokenizer(), true));
                    if (state.getMatch("IgnoreNull") != null) {
                        semantic.setIgnoreNull(true);
                    }
                    state.setReturn(semantic);
                }))));

        conditionExpression.set(join(andExpression, optional(join(keyword("Or"), conditionExpression)), action(state -> {
            state.setReturn(combineConditions(state.getMatch(andExpression).val(), state.getMatch(conditionExpression), LogicalOperator.OR));
        })));
        andExpression.set(join(condition, optional(join(keyword("And"), andExpression)), action(state -> {
            state.setReturn(combineConditions(state.getMatch(condition).val(), state.getMatch(andExpression), LogicalOperator.AND));
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

        orderByList.set(
                join(property, optional(choice(keyword("Asc"), keyword("Desc"))), optional(join(keyword("And"), orderByList)), action(state -> {
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
                join(choice(integer, variable), keyword("To"), choice(integerB, variableB), action(state -> {
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
                join(choice(integer, variable), action(state -> {
                    Limit limit = new Limit();
                    state.setReturn(limit);
                    MatchResult _integer = state.getMatch(integer);
                    if (_integer != null) {
                        limit.setRowCount(_integer.val());
                    } else {
                        limit.setRowCountVariable(state.getMatch(variable).val());
                    }
                })))));

        propertyList.set(
                join(property, star(join(keyword("And"), property)), action(state -> {
                    List<PropertyInfo> propertyInfos = new ArrayList<>();
                    List<MatchResult> matches = state.getMatches(property);
                    for (MatchResult match : matches) {
                        propertyInfos.add(match.val());
                    }
                    state.setReturn(propertyInfos);
                })));
    }

    private Condition combineConditions(Condition first, MatchResult tailMatch, LogicalOperator operator) {
        if (tailMatch == null) {
            return first;
        }
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.setLogicalOperator(operator);
        condition.setPropertyInfos(first.getPropertyInfos());
        condition.getSubConditions().add(first);
        Condition tail = tailMatch.val();
        if (tail.getType() == ConditionType.COMPLEX && tail.getLogicalOperator() == operator) {
            condition.getSubConditions().addAll(tail.getSubConditions());
        } else {
            condition.getSubConditions().add(tail);
        }
        return condition;
    }

    private List<PropertyInfo> buildDefaultSelectItems(JpaTokenizer jpaTokenizer) {
        JavaType returnType = JavaTypes.unwrap(jpaTokenizer.getReturnType());
        if (!TableInfoFactory.isSameTableType(returnType, jpaTokenizer.getTableInfo().getClassType())) {
            throw jpaTokenizer.semanticError("Incompatible return type: " + returnType.getName() + ", expected: " + jpaTokenizer.getTableInfo().getClassType().getName());
        }
        TableInfo tableInfo = tableInfoFactory.buildTableInfo(returnType);
        List<PropertyInfo> propertyInfos = tableInfo.getNameToPropertyInfo().values().stream().filter(v -> v.getFetchType() == null).collect(Collectors.toList());
        return ensureJoinRelationColumns(jpaTokenizer, propertyInfos);
    }

    private List<PropertyInfo> ensureJoinRelationColumns(JpaTokenizer jpaTokenizer, List<PropertyInfo> propertyInfos) {
        TableInfo tableInfo = jpaTokenizer.getTableInfo();
        Map<String, PropertyInfo> nameToPropertyInfo = new HashMap<>();
        for (PropertyInfo propertyInfo : propertyInfos) {
            nameToPropertyInfo.put(propertyInfo.getFullName(), propertyInfo);
            if (propertyInfo.getFetchType() != null) {
                List<JoinColumnInfo> joinColumnInfos = NestedSelectHelper.buildLeftmostJoinColumns(propertyInfo);
                for (JoinColumnInfo joinColumnInfo : joinColumnInfos) {
                    PropertyInfo leftPropertyInfo = TableInfoFactory.getPropertyInfo(tableInfo, joinColumnInfo.getLeftFullName());
                    nameToPropertyInfo.put(leftPropertyInfo.getFullName(), leftPropertyInfo);
                }
            }
        }
        return new ArrayList<>(nameToPropertyInfo.values());
    }

    private void collectUsedParamNames(Limit limit, Set<String> usedParamNames) {
        if (limit.getOffsetVariable() != null) {
            usedParamNames.add(rootName(limit.getOffsetVariable()));
        }
        if (limit.getRowCountVariable() != null) {
            usedParamNames.add(rootName(limit.getRowCountVariable()));
        }
    }

    private Set<String> collectUsedParamNames(Condition condition) {
        Set<String> set = new HashSet<>();
        collectUsedParamNames(condition, set);
        return set;
    }

    private void collectUsedParamNames(Condition condition, Set<String> usedParamNames) {
        if (condition.getVariable() != null) {
            usedParamNames.add(rootName(condition.getVariable()));
        }
        if (condition.getSecondVariable() != null) {
            usedParamNames.add(rootName(condition.getSecondVariable()));
        }
        for (Condition child : condition.getSubConditions()) {
            collectUsedParamNames(child, usedParamNames);
        }
    }

    private static String rootName(Variable variable) {
        return rootName(variable.getFullName());
    }

    private static String rootName(String fullName) {
        int separator = fullName.indexOf('.');
        return separator < 0 ? fullName : fullName.substring(0, separator);
    }

    private boolean hasDefaultConditionCandidate(JpaTokenizer tokenizer, Set<String> usedParamNames) {
        if (tokenizer.getParameterBindings().size() == 1 && usedParamNames.isEmpty()) {
            return true;
        }
        for (JpaParameter parameter : tokenizer.getParameterBindings()) {
            if (parameter.isNamed() && !usedParamNames.contains(parameter.getName())) {
                return true;
            }
        }
        return false;
    }

    private Variable buildSemanticParameter(JpaTokenizer tokenizer, boolean requiredTableParameter) {
        List<JpaParameter> parameters = tokenizer.getParameterBindings();
        if (parameters.isEmpty()) {
            throw tokenizer.semanticError("No parameters provided in the query.");
        }
        JpaParameter parameter = parameters.get(0);
        JavaType tableType = tokenizer.getTableInfo().getClassType();
        if (parameter.isTable(tableType)) {
            return parameter.isCollection() ? parameter.getVariable() : parameter.getValueVariable();
        }
        if (requiredTableParameter) {
            throw tokenizer.semanticError("Invalid parameter type. Expected: " + tableType + ", but was: " + parameter.getType());
        }
        if (parameters.size() == 1 && parameter.isNamed() && parameter.isCollection() && typePolicy.isScalar(parameter.getElementType())) {
            return parameter.getVariable();
        }
        return null;
    }

    private Condition buildDefaultCondition(JpaTokenizer tokenizer, Set<String> usedParamNames) {
        List<JpaParameter> parameters = tokenizer.getParameterBindings();
        if (parameters.size() == 1) {
            JpaParameter parameter = parameters.get(0);
            if (parameter.isTable(tokenizer.getTableInfo().getClassType())) {
                return buildEntityCondition(tokenizer, usedParamNames, parameter);
            }
            if (parameter.isNamed() && parameter.isCollection() && typePolicy.isScalar(parameter.getElementType())) {
                tokenizer.recordParameterProperty(parameter.getDeclaration());
                return ConditionHelper.simplifyCondition(buildParameterCondition(tokenizer, usedParamNames, parameter, parameter.getValueVariable()));
            }
        }
        return buildScalarConditions(tokenizer, usedParamNames);
    }

    private Condition buildEntityCondition(JpaTokenizer tokenizer, Set<String> usedParamNames, JpaParameter parameter) {
        TableInfo tableInfo = tokenizer.getTableInfo();
        String prefix = parameter.getValueVariable().getFullName();
        JavaAnnotation onlyById = parameter.getDeclaration().annotation(OnlyById.class);
        Condition condition = ConditionHelper.buildForTableInfo(tableInfo, onlyById != null, prefix);
        JavaAnnotation filterable = parameter.getDeclaration().annotation(Filterable.class);
        if (onlyById == null && filterable != null) {
            applyFilterableInfo(condition, buildFilterableInfo(filterable), usedParamNames, tokenizer);
            condition.setVariable(new Variable(StringUtils.isNotBlank(prefix) ? prefix : "param1", tableInfo.getClassType()));
        }
        return ConditionHelper.simplifyCondition(condition);
    }

    private Condition buildScalarConditions(JpaTokenizer tokenizer, Set<String> usedParamNames) {
        TableInfo tableInfo = tokenizer.getTableInfo();

        // 先确定主参数及其第二参数的归属，尚未选为主参数的引用不消费其他参数。
        Set<String> pendingParamNames = new LinkedHashSet<>();
        Map<String, String> nameToSecondParamName = new HashMap<>();
        for (JpaParameter parameter : tokenizer.getParameterBindings()) {
            if (usedParamNames.contains(parameter.getName()) || parameter.isTable(tableInfo.getClassType())) {
                continue;
            }
            pendingParamNames.add(parameter.getName());
            JavaAnnotation annotation = parameter.getDeclaration().annotation(Filterable.class);
            if (annotation != null && parameter.isNamed() && typePolicy.isScalar(parameter.getType())) {
                FilterableInfo filterable = buildFilterableInfo(annotation);
                if (filterable.getOperator().isRequiredSecondVariable()) {
                    nameToSecondParamName.put(parameter.getName(), rootName(filterable.getSecondVariable()));
                }
            }
        }

        Set<String> primaryParamNames = new HashSet<>();
        while (!pendingParamNames.isEmpty()) {
            Set<String> referencedParamNames = new HashSet<>();
            for (String name : pendingParamNames) {
                String secondName = nameToSecondParamName.get(name);
                if (secondName != null && !secondName.equals(name)) {
                    referencedParamNames.add(secondName);
                }
            }
            Set<String> readyParamNames = new LinkedHashSet<>(pendingParamNames);
            readyParamNames.removeAll(referencedParamNames);
            if (readyParamNames.isEmpty()) {
                throw tokenizer.semanticError("Cyclic second-variable references among parameters: " + String.join(", ", pendingParamNames));
            }

            // 已被主参数消费的第二参数不再传播自身的 Filterable 引用。
            for (String name : readyParamNames) {
                primaryParamNames.add(name);
                pendingParamNames.remove(name);
                String secondName = nameToSecondParamName.get(name);
                if (secondName != null) {
                    pendingParamNames.remove(secondName);
                }
            }
        }

        tokenizer.recordParameterProperties(primaryParamNames);

        List<Condition> conditions = new ArrayList<>();
        for (JpaParameter parameter : tokenizer.getParameterBindings()) {
            if (!primaryParamNames.contains(parameter.getName())) {
                continue;
            }
            if (!parameter.isNamed() || !typePolicy.isScalar(parameter.getType())) {
                throw tokenizer.semanticError("Unsupported parameter type: " + parameter.getType().getName() + ". Method: " + tokenizer.getText());
            }
            conditions.add(buildParameterCondition(tokenizer, usedParamNames, parameter, parameter.getVariable()));
        }
        if (conditions.isEmpty()) {
            return null;
        }
        Condition condition = new Condition(ConditionType.COMPLEX);
        condition.getSubConditions().addAll(conditions);
        condition.setLogicalOperator(LogicalOperator.AND);
        condition.setPropertyInfos(tableInfo.getNameToPropertyInfo());
        return ConditionHelper.simplifyCondition(condition);
    }

    private Condition buildParameterCondition(JpaTokenizer tokenizer, Set<String> usedParamNames, JpaParameter parameter, Variable variable) {
        Condition condition = new Condition(ConditionType.BASIC);
        condition.setPropertyInfos(tokenizer.getTableInfo().getNameToPropertyInfo());
        DiagnosticLocation location = new DiagnosticLocation(tokenizer.getLocation().getOwner(), parameter.getDeclaration(), Param.class, "value", null)
                .inText(parameter.getName(), 0, parameter.getName().length());
        condition.setPropertyInfo(parseProperty(tokenizer.getTableInfo(), parameter.getName(), location, null));
        condition.setVariable(variable);
        FilterableInfo filterable = buildFilterableInfo(parameter.getDeclaration().annotation(Filterable.class));
        applyFilterableInfo(condition, filterable, usedParamNames, tokenizer);
        return condition;
    }

    private FilterableInfo buildFilterableInfo(@Nullable JavaAnnotation filterable) {
        if (filterable == null) {
            FilterableInfo filterableInfo = new FilterableInfo();
            filterableInfo.setTestMode(TestMode.None);
            filterableInfo.setOperator(CompareOperator.Equals);
            filterableInfo.setLogicalOperator(LogicalOperator.AND);
            return filterableInfo;
        }
        return new FilterableInfo(filterable);
    }

    private void applyFilterableInfo(Condition condition, FilterableInfo filterableInfo, Set<String> usedParamNames, JpaTokenizer jpaTokenizer) {
        ConditionHelper.applyFilterableSettings(condition, filterableInfo);
        if (condition.getCompareOperator().isRequiredSecondVariable()) {
            Variable secondVariable = resolveSecondVariable(jpaTokenizer, filterableInfo);
            usedParamNames.add(rootName(secondVariable));
            condition.setSecondVariable(secondVariable);
        }
    }

    private Variable resolveSecondVariable(JpaTokenizer tokenizer, FilterableInfo filterableInfo) {
        Variable variable = resolveVariablePath(tokenizer.getVariables(), filterableInfo.getSecondVariable());
        if (variable == null) {
            throw tokenizer.semanticError("Second variable '" + filterableInfo.getSecondVariable() + "' not found in variables.");
        }
        return variable;
    }

    private Variable resolveVariablePath(List<Variable> variables, String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        String[] segments = path.split("\\.", -1);
        Variable variable = null;
        for (Variable candidate : variables) {
            if (candidate.getName().equals(segments[0])) {
                variable = candidate;
                break;
            }
        }
        for (int i = 1; variable != null && i < segments.length; i++) {
            if (segments[i].isEmpty()) {
                return null;
            }
            VariableFactory.addChildren(typePolicy, variable);
            variable = variable.getNameToVariable().get(segments[i]);
        }
        return variable;
    }

    private Condition ensureConditionVariable(JpaTokenizer tokenizer, Set<String> usedParamNames, Condition condition) {
        bindConditionVariables(tokenizer, usedParamNames, tokenizer.getParameterBindings().iterator(), condition);
        return ConditionHelper.simplifyCondition(condition);
    }

    private void bindConditionVariables(JpaTokenizer tokenizer, Set<String> usedParamNames, Iterator<JpaParameter> parameters, Condition condition) {
        if (condition.getType() == ConditionType.COMPLEX) {
            for (Condition child : condition.getSubConditions()) {
                bindConditionVariables(tokenizer, usedParamNames, parameters, child);
            }
        } else {
            if (condition.getCompareOperator().isRequiredVariable() && condition.getVariable() == null) {
                condition.setVariable(buildNextUnusedVariable(tokenizer, parameters, usedParamNames, condition.getPropertyInfo()));
            }
            if (condition.getCompareOperator().isRequiredSecondVariable() && condition.getSecondVariable() == null) {
                condition.setSecondVariable(buildNextUnusedVariable(tokenizer, parameters, usedParamNames, condition.getPropertyInfo()));
            }
        }
    }

    private Variable buildNextUnusedVariable(JpaTokenizer tokenizer, Iterator<JpaParameter> parameters, Set<String> usedParamNames, PropertyInfo property) {
        while (parameters.hasNext()) {
            JpaParameter parameter = parameters.next();
            if (usedParamNames.add(parameter.getName())) {
                return parameter.getVariable();
            }
        }
        throw tokenizer.semanticError("Insufficient parameters for method: " + tokenizer.getText()
                + ", condition: " + property.getFullName() + ", supplied parameters: " + tokenizer.getParameterBindings().size());
    }

    private final Symbol propertyEnd = join(property, end);

    private PropertyInfo parseProperty(TableInfo tableInfo, String param, DiagnosticLocation location, List<SymbolReference> references) {
        AtomicReference<PropertyInfo> reference = new AtomicReference<>();
        List<TokenMarker> tokenMarkers = new ArrayList<>();
        JpaTokenizer jpaTokenizer = new JpaTokenizer(tableInfo, param.isEmpty() ? param : param.substring(0, 1).toUpperCase() + param.substring(1), typePolicy);
        propertyEnd.match(jpaTokenizer, state -> {
            PropertyInfo propertyInfo = (PropertyInfo) state.getResult();
            reference.set(propertyInfo);
            tokenMarkers.add(new TokenMarker(jpaTokenizer.getTokenMarker()));
            if (references != null) {
                collectReferences(state, references);
            }
            return false;
        });
        if (reference.get() == null) {
            throw new ParserException(new Diagnostic(Diagnostic.Code.SYNTAX_ERROR, jpaTokenizer.getExpectedTokens().getMessage() + " Parameter: " + jpaTokenizer.getText(), location));
        }
        if (tokenMarkers.size() > 1) {
            throw new ParserException(new Diagnostic(Diagnostic.Code.AMBIGUOUS, "Conflict detected at column " + tokenMarkers.get(0).getDiffBegin(tokenMarkers.get(tokenMarkers.size() - 1)), location));
        }
        return reference.get();
    }

    /** Returns the effective table only when the parameter is parsed as a default condition property. */
    public TableInfo parameterPropertyTable(TableInfo table, JavaMethod method, int parameterIndex) {
        JavaParameter[] parameters = method.getParameters();
        JavaParameter target = parameters[parameterIndex];
        AtomicReference<TableInfo> result = new AtomicReference<>();
        try {
            analyzeSyntax(table, method.getName(), parameters, method.getGenericReturnType(), null, methodLocation(method), (parameter, effectiveTable) -> {
                if (parameter == target) {
                    result.set(effectiveTable);
                }
            });
        } catch (MybatisExtException | ParserException ignored) {
            // Keep the confirmed property context even when the name is incomplete.
        }
        return result.get();
    }

    public AnalysisResult<PropertyInfo> resolveParameterProperty(TableInfo table, String name) {
        List<SymbolReference> references = new ArrayList<>();
        DiagnosticLocation location = new DiagnosticLocation(table.getClassType(), null, Param.class, "value", null).inText(name, 0, name.length());
        try {
            return new AnalysisResult<>(parseProperty(table, name, location, references), Collections.emptyList(), references);
        } catch (ParserException e) {
            return new AnalysisResult<>(null, Collections.singletonList(e.getDiagnostic()), Collections.emptyList());
        }
    }

    public List<CompletionItem> completeParameterProperty(TableInfo table, String name, int offset) {
        String text = name.isEmpty() ? name : name.substring(0, 1).toUpperCase() + name.substring(1);
        CompletionCollector collector = new CompletionCollector(text, offset);
        JpaTokenizer tokenizer = new JpaTokenizer(table, text.substring(0, offset), typePolicy, new JavaParameter[0], null, collector);
        propertyEnd.match(tokenizer, state -> false);
        List<CompletionItem> result = new ArrayList<>();
        for (CompletionItem item : collector.getItems()) {
            String token = item.getText();
            if (item.getReplaceStart() == 0) {
                token = token.substring(0, 1).toLowerCase() + token.substring(1);
            }
            result.add(new CompletionItem(token, item.getKind(), item.getReplaceStart(), item.getReplaceEnd(), item.getTarget()));
        }
        return result;
    }

    public Semantic parse(TableInfo tableInfo, String methodName, JavaParameter[] parameters, JavaType returnType) {
        return parseChecked(tableInfo, methodName, parameters, returnType, null,
                new DiagnosticLocation(tableInfo.getClassType(), null, null, null, null).inText(methodName, 0, methodName.length()));
    }

    /** 运行时入口，保留 Mapper 方法声明及其归一化后的返回类型。 */
    public Semantic parse(TableInfo tableInfo, JavaMethod method, JavaType returnType) {
        return parseChecked(tableInfo, method.getName(), method.getParameters(), returnType, null, methodLocation(method));
    }

    public AnalysisResult<Semantic> analyze(TableInfo tableInfo, JavaMethod method) {
        return analyze(tableInfo, method.getName(), method.getParameters(), method.getGenericReturnType(), methodLocation(method));
    }

    private AnalysisResult<Semantic> analyze(TableInfo tableInfo, String methodName, JavaParameter[] parameters, JavaType returnType, DiagnosticLocation location) {
        List<SymbolReference> references = new ArrayList<>();
        try {
            Semantic semantic = parseChecked(tableInfo, methodName, parameters, returnType, references, location);
            return new AnalysisResult<>(semantic, Collections.emptyList(), references);
        } catch (MybatisExtException e) {
            if (e.getDiagnostic() == null) {
                throw e;
            }
            return new AnalysisResult<>(null, Collections.singletonList(e.getDiagnostic()), Collections.emptyList());
        } catch (ParserException e) {
            if (e.getDiagnostic() == null) {
                throw e;
            }
            return new AnalysisResult<>(null, Collections.singletonList(e.getDiagnostic()), Collections.emptyList());
        }
    }

    private Semantic parseChecked(TableInfo tableInfo, String methodName, JavaParameter[] parameters, JavaType returnType,
            List<SymbolReference> references, DiagnosticLocation location) {
        try {
            return analyzeSyntax(tableInfo, methodName, parameters, returnType, references, location);
        } catch (UnresolvedTypeException e) {
            throw e.at(location);
        }
    }

    public AnalysisResult<List<CompletionItem>> complete(TableInfo tableInfo, JavaMethod method, int offset) {
        String methodName = method.getName();
        JavaParameter[] parameters = method.getParameters();
        JavaType returnType = method.getGenericReturnType();
        DiagnosticLocation location = methodLocation(method);
        CompletionCollector collector = new CompletionCollector(methodName, offset);
        List<Diagnostic> diagnostics = new ArrayList<>();
        try {
            TableInfo effectiveTable = effectiveTable(tableInfo, parameters, returnType, true);
            JpaTokenizer tokenizer = new JpaTokenizer(effectiveTable, methodName.substring(0, offset), typePolicy, parameters, returnType, collector, location.inText(methodName, 0, offset));
            grammar.match(tokenizer, state -> false);
        } catch (UnresolvedTypeException e) {
            diagnostics.add(e.at(location.inText(methodName, 0, offset)).getDiagnostic());
        } catch (MybatisExtException e) {
            if (e.getDiagnostic() == null) {
                throw e;
            }
            diagnostics.add(e.getDiagnostic());
        } catch (ParserException e) {
            if (e.getDiagnostic() == null) {
                throw e;
            }
            diagnostics.add(e.getDiagnostic());
        }
        return new AnalysisResult<>(collector.getItems(), diagnostics, Collections.emptyList());
    }

    private static DiagnosticLocation methodLocation(JavaMethod method) {
        return new DiagnosticLocation(method.getDeclaringType(), method, null, null, null).inText(method.getName(), 0, method.getName().length());
    }

    private TableInfo effectiveTable(TableInfo tableInfo, JavaParameter[] parameters, JavaType returnType, boolean editing) {
        if (!editing && (returnType == null || !returnType.isResolved())) {
            throw new UnresolvedTypeException(returnType == null ? "<return type>" : returnType.getName());
        }
        JavaType unwrappedReturnType = returnType == null || !returnType.isResolved() ? null : JavaTypes.unwrap(returnType);
        if (TableInfoFactory.isSameTableType(tableInfo.getClassType(), unwrappedReturnType)) {
            tableInfo = tableInfoFactory.buildTableInfo(unwrappedReturnType);
        }
        for (JavaParameter parameter : parameters) {
            if (editing && !parameter.getGenericType().isResolved()) {
                break;
            }
            if (typePolicy.isSpecialParameter(parameter.getGenericType())) {
                continue;
            }
            JavaType parameterType = JavaTypes.unwrap(parameter.getGenericType());
            if (TableInfoFactory.isSameTableType(tableInfo.getClassType(), parameterType)) {
                tableInfo = tableInfoFactory.buildTableInfo(parameterType);
            }
            break;
        }
        return tableInfo;
    }

    private Semantic analyzeSyntax(TableInfo tableInfo, String methodName, JavaParameter[] parameters, JavaType returnType, List<SymbolReference> references, DiagnosticLocation location) {
        return analyzeSyntax(tableInfo, methodName, parameters, returnType, references, location, null);
    }

    private Semantic analyzeSyntax(TableInfo tableInfo, String methodName, JavaParameter[] parameters, JavaType returnType, List<SymbolReference> references,
            DiagnosticLocation location, BiConsumer<JavaParameter, TableInfo> parameterPropertyConsumer) {
        tableInfo = effectiveTable(tableInfo, parameters, returnType, false);
        AtomicReference<Semantic> reference = new AtomicReference<>();
        List<TokenMarker> tokenMarkers = new ArrayList<>();
        JpaTokenizer tokenizer = new JpaTokenizer(tableInfo, methodName, typePolicy, parameters, returnType, null, location);
        tokenizer.setParameterPropertyConsumer(parameterPropertyConsumer);
        grammar.match(tokenizer, state -> {
            reference.set((Semantic) state.getResult());
            tokenMarkers.add(new TokenMarker(tokenizer.getTokenMarker()));
            if (references != null) {
                collectReferences(state, references);
            }
            return false;
        });
        if (reference.get() == null) {
            ExpectedTokens expected = tokenizer.getExpectedTokens();
            throw new ParserException(new Diagnostic(Diagnostic.Code.SYNTAX_ERROR, expected.getMessage() + " Method: " + methodName, location.inText(methodName, expected.getCursor(), methodName.length())));
        }
        if (tokenMarkers.size() > 1) {
            int begin = tokenMarkers.get(0).getDiffBegin(tokenMarkers.get(tokenMarkers.size() - 1));
            throw new ParserException(new Diagnostic(Diagnostic.Code.AMBIGUOUS, "Conflict detected at column " + begin, location.inText(methodName, begin, methodName.length())));
        }
        reference.get().setTableInfo(tableInfo);
        return reference.get();
    }

    private void collectReferences(State<JpaTokenizer> state, List<SymbolReference> references) {
        references.clear();
        List<MatchResult> matches = state.getAllMatches();
        Map<Integer, MatchResult> propertyStartToMatch = new HashMap<>();
        for (MatchResult match : matches) {
            if (match.getSymbol() == property) {
                propertyStartToMatch.put(match.getStart(), match);
            }
        }

        // 绑定已完成，将每个条件关联到属性路径的末段。
        Map<Integer, Condition> propertyEndToCondition = new HashMap<>();
        for (MatchResult match : matches) {
            if (match.getSymbol() == condition) {
                MatchResult propertyMatch = propertyStartToMatch.get(match.getStart());
                propertyEndToCondition.put(propertyMatch.getEnd(), match.val());
            }
        }

        for (MatchResult match : matches) {
            if (match.getSymbol() == propertyName || match.getSymbol() == subPropertyName) {
                Condition boundCondition = propertyEndToCondition.get(match.getEnd());
                if (boundCondition != null) {
                    boolean firstBound = addBindingReference(references, boundCondition.getVariable(), match.getStart(), match.getEnd());
                    boolean secondBound = addBindingReference(references, boundCondition.getSecondVariable(), match.getStart(), match.getEnd());
                    if (firstBound || secondBound) {
                        continue;
                    }
                }
                references.add(new SymbolReference(match.getStart(), match.getEnd(), match.val()));
            } else if (match.getSymbol() == variableName || match.getSymbol() == subVariableName || match.getSymbol() == returnTypeName) {
                references.add(new SymbolReference(match.getStart(), match.getEnd(), match.val()));
            }
        }
        references.sort(Comparator.comparingInt(SymbolReference::getStart));
    }

    private boolean addBindingReference(List<SymbolReference> references, Variable variable, int start, int end) {
        if (variable != null && variable.getSource() != null) {
            references.add(new SymbolReference(start, end, variable.getSource()));
            return true;
        }
        return false;
    }
}
