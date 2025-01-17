package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.reflect.GenericParameter;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.util.CommonUtils;

public class JpaTokenizer implements Tokenizer {

    private final TableInfo tableInfo;
    private final String text;
    private final GenericParameter[] parameters;
    private final GenericType returnType;
    private final List<Variable> variables;
    private final ExpectedTokens expectedTokens;
    private final TokenMarker tokenMarker;
    private int cursor = 0;

    public JpaTokenizer(TableInfo tableInfo, String text, Configuration configuration) {
        this(tableInfo, text, configuration, new GenericParameter[0], null);
    }

    public JpaTokenizer(TableInfo tableInfo, String text, Configuration configuration, GenericParameter[] parameters, GenericType returnType) {
        this.tableInfo = tableInfo;
        this.text = text;
        this.returnType = returnType;
        this.parameters = Arrays.stream(parameters).filter(v -> !CommonUtils.isSpecialParameter(v.getType())).toArray(GenericParameter[]::new);
        this.variables = buildVariables(configuration, parameters);
        this.expectedTokens = new ExpectedTokens(text);
        this.tokenMarker = new TokenMarker(text);
    }

    private List<Variable> buildVariables(Configuration configuration, GenericParameter[] parameters) {
        List<Variable> variables = new ArrayList<>();
        if (parameters.length == 0) {
            return variables;
        }
        if (parameters.length == 1 && VariableFactory.hasSubVariable(configuration, parameters[0].getType())) {
            Param param = parameters[0].getAnnotation(Param.class);
            if (param != null) {
                Variable variable = VariableFactory.build(configuration, param.value(), parameters[0].getGenericType());
                variables.add(variable);
                variables.addAll(variable.values());
            } else {
                Variable variable = VariableFactory.build(configuration, "", parameters[0].getGenericType());
                variables.addAll(variable.values());
            }
            return variables;
        }
        for (GenericParameter parameter : parameters) {
            Param param = parameter.getAnnotation(Param.class);
            if (param != null) {
                variables.add(new Variable(param.value(), parameter.getGenericType()));
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
        for (PropertyInfo propertyInfo : prevPropertyInfo.values()) {
            String expect = propertyInfo.getName().substring(0, 1).toUpperCase() + propertyInfo.getName().substring(1);
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
        List<Variable> ss = new ArrayList<>();
        int _cursor = cursor;
        for (Variable variable : prevVariable.values()) {
            String expect = variable.getName().substring(0, 1).toUpperCase() + variable.getName().substring(1);
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
        return Integer.parseInt(s);
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public String getText() {
        return text;
    }

    public GenericParameter[] getParameters() {
        return parameters;
    }

    public GenericType getReturnType() {
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
