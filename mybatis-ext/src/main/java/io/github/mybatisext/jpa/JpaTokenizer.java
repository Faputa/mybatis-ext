package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import io.github.mybatisext.analysis.CompletionCollector;
import io.github.mybatisext.analysis.CompletionItem;
import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.diagnostic.DiagnosticLocation;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.model.JavaParameter;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.TypePolicy;

public class JpaTokenizer implements Tokenizer {

    private final TableInfo tableInfo;
    private final String text;
    private final TypePolicy typePolicy;
    private final JavaParameter[] parameters;
    private final List<JpaParameter> parameterBindings;
    private final JavaType returnType;
    private final List<Variable> variables;
    private final ExpectedTokens expectedTokens;
    private final TokenMarker tokenMarker;
    private final CompletionCollector completion;
    private final DiagnosticLocation location;
    private int cursor = 0;
    private BiConsumer<JavaParameter, TableInfo> parameterPropertyConsumer;

    void setParameterPropertyConsumer(BiConsumer<JavaParameter, TableInfo> consumer) {
        parameterPropertyConsumer = consumer;
    }

    void recordParameterProperties(Set<String> primaryParamNames) {
        if (parameterPropertyConsumer == null) {
            return;
        }

        for (JpaParameter parameter : parameterBindings) {
            if (primaryParamNames.contains(parameter.getName()) && parameter.isNamed() && typePolicy.isScalar(parameter.getType())) {
                recordParameterProperty(parameter.getDeclaration());
            }
        }
    }

    void recordParameterProperty(JavaParameter parameter) {
        if (parameterPropertyConsumer != null) {
            parameterPropertyConsumer.accept(parameter, tableInfo);
        }
    }

    public JpaTokenizer(TableInfo tableInfo, String text, TypePolicy typePolicy) {
        this(tableInfo, text, typePolicy, new JavaParameter[0], null);
    }

    public JpaTokenizer(TableInfo tableInfo, String text, TypePolicy typePolicy, JavaParameter[] parameters, JavaType returnType) {
        this(tableInfo, text, typePolicy, parameters, returnType, null);
    }

    public JpaTokenizer(TableInfo tableInfo, String text, TypePolicy typePolicy, JavaParameter[] parameters, JavaType returnType,
            CompletionCollector completion) {
        this(tableInfo, text, typePolicy, parameters, returnType, completion,
                new DiagnosticLocation(tableInfo.getClassType(), null, null, null, null).inText(text, 0, text.length()));
    }

    public JpaTokenizer(TableInfo tableInfo, String text, TypePolicy typePolicy, JavaParameter[] parameters, JavaType returnType,
            CompletionCollector completion, DiagnosticLocation location) {
        this.location = location;
        this.tableInfo = tableInfo;
        this.text = text;
        this.typePolicy = typePolicy;
        this.returnType = returnType;
        this.completion = completion;
        List<JavaParameter> effectiveParameters = new ArrayList<>();
        for (JavaParameter parameter : parameters) {
            JavaType type = parameter.getGenericType();
            if (completion != null && !type.isResolved()) {
                continue;
            }
            if (typePolicy.isSpecialParameter(type)) {
                continue;
            }
            effectiveParameters.add(parameter);
        }
        this.parameters = effectiveParameters.toArray(new JavaParameter[0]);

        this.parameterBindings = new ArrayList<>();
        Set<String> parameterNames = new HashSet<>();
        for (int i = 0; i < this.parameters.length; i++) {
            JpaParameter parameter = new JpaParameter(this.parameters[i], i, this.parameters.length);
            if (!parameterNames.add(parameter.getName())) {
                throw semanticError("Conflicting parameter name '" + parameter.getName() + "'. Use distinct @Param names for these parameters.");
            }
            parameterBindings.add(parameter);
        }
        this.variables = buildVariables(typePolicy);
        this.expectedTokens = new ExpectedTokens(text);
        this.tokenMarker = new TokenMarker(text);
    }

    public DiagnosticLocation getLocation() {
        return location;
    }

    public MybatisExtException semanticError(String message) {
        return new MybatisExtException(new Diagnostic(Diagnostic.Code.SEMANTIC_ERROR, message, location));
    }

    public boolean isCompleting() {
        return completion != null;
    }

    public void offer(String token, CompletionItem.Kind kind, Object target) {
        if (completion != null) {
            completion.offer(cursor, token, kind, target);
        }
    }

    private List<Variable> buildVariables(TypePolicy typePolicy) {
        List<Variable> variables = new ArrayList<>();
        if (parameters.length == 0) {
            return variables;
        }
        if (parameters.length == 1 && VariableFactory.hasSubVariable(typePolicy, parameters[0].getGenericType())) {
            JpaParameter parameter = parameterBindings.get(0);
            Variable variable = parameter.getValueVariable();
            VariableFactory.addChildren(typePolicy, variable);
            if (parameter.isNamed()) {
                variables.add(variable);
            }
            variables.addAll(variable.getNameToVariable().values());
            return variables;
        }
        for (JpaParameter parameter : parameterBindings) {
            if (parameter.isNamed()) {
                variables.add(parameter.getVariable());
            }
        }
        return variables;
    }

    private String next() {
        StringBuilder sb = new StringBuilder();
        for (int i = cursor; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > cursor && (Character.isLowerCase(text.charAt(i - 1))
                        || (i < text.length() - 1 && Character.isLowerCase(text.charAt(i + 1))))) {
                    cursor += sb.length();
                    return sb.toString();
                }
            } else if (Character.isDigit(c)) {
                if (i > cursor && !Character.isDigit(text.charAt(i - 1))) {
                    cursor += sb.length();
                    return sb.toString();
                }
            } else if (c == '$') {
                if (i > cursor) {
                    cursor += sb.length();
                    return sb.toString();
                }
            }
            sb.append(c);
        }
        cursor += sb.length();
        return sb.toString();
    }

    public String keyword(String expect) {
        if (cursor < text.length() && text.charAt(cursor) == '$') {
            cursor++;
        }
        offer(expect, CompletionItem.Kind.KEYWORD, null);
        if (text.substring(cursor).startsWith(expect)) {
            String s = "";
            while (s.length() < expect.length()) {
                s += next();
                if (s.equals(expect)) {
                    return s;
                }
            }
        }
        return "";
    }

    public List<PropertyInfo> property() {
        List<PropertyInfo> propertyInfos = new ArrayList<>();
        int _cursor = cursor;
        for (PropertyInfo propertyInfo : tableInfo.getNameToPropertyInfo().values()) {
            String expect = propertyInfo.getName().substring(0, 1).toUpperCase() + propertyInfo.getName().substring(1);
            offer(expect, CompletionItem.Kind.PROPERTY, propertyInfo);
            if (text.substring(cursor).startsWith(expect)) {
                String s = "";
                while (s.length() < expect.length()) {
                    s += next();
                    if (s.equals(expect)) {
                        propertyInfos.add(propertyInfo);
                    }
                }
            }
            cursor = _cursor;
        }
        return propertyInfos;
    }

    public List<PropertyInfo> property(PropertyInfo prevPropertyInfo) {
        List<PropertyInfo> propertyInfos = new ArrayList<>();
        int _cursor = cursor;
        for (PropertyInfo propertyInfo : prevPropertyInfo.getNameToPropertyInfo().values()) {
            String expect = propertyInfo.getName().substring(0, 1).toUpperCase() + propertyInfo.getName().substring(1);
            offer(expect, CompletionItem.Kind.PROPERTY, propertyInfo);
            if (text.substring(cursor).startsWith(expect)) {
                String s = "";
                while (s.length() < expect.length()) {
                    s += next();
                    if (s.equals(expect)) {
                        propertyInfos.add(propertyInfo);
                    }
                }
            }
            cursor = _cursor;
        }
        return propertyInfos;
    }

    public List<Variable> variable() {
        List<Variable> ss = new ArrayList<>();
        int _cursor = cursor;
        for (Variable variable : variables) {
            String expect = variable.getName().substring(0, 1).toUpperCase() + variable.getName().substring(1);
            offer(expect, CompletionItem.Kind.VARIABLE, variable);
            if (text.substring(cursor).startsWith(expect)) {
                String s = "";
                while (s.length() < expect.length()) {
                    s += next();
                    if (s.equals(expect)) {
                        ss.add(variable);
                    }
                }
            }
            cursor = _cursor;
        }
        return ss;
    }

    public List<Variable> variable(Variable prevVariable) {
        VariableFactory.addChildren(typePolicy, prevVariable);
        List<Variable> ss = new ArrayList<>();
        int _cursor = cursor;
        for (Variable variable : prevVariable.getNameToVariable().values()) {
            String expect = variable.getName().substring(0, 1).toUpperCase() + variable.getName().substring(1);
            offer(expect, CompletionItem.Kind.VARIABLE, variable);
            if (text.substring(cursor).startsWith(expect)) {
                String s = "";
                while (s.length() < expect.length()) {
                    s += next();
                    if (s.equals(expect)) {
                        ss.add(variable);
                    }
                }
            }
            cursor = _cursor;
        }
        return ss;
    }

    public int integer() {
        String s = next();
        if (s.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return -1;
            }
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public String getText() {
        return text;
    }

    public JavaParameter[] getParameters() {
        return parameters;
    }

    List<JpaParameter> getParameterBindings() {
        return parameterBindings;
    }

    public JavaType getReturnType() {
        return returnType;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public ExpectedTokens getExpectedTokens() {
        return expectedTokens;
    }

    public TokenMarker getTokenMarker() {
        return tokenMarker;
    }

    @Override
    public int getCursor() {
        return cursor;
    }

    @Override
    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    @Override
    public String substring(int begin, int end) {
        return text.substring(begin, end);
    }
}
