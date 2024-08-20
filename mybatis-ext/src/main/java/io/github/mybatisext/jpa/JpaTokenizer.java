package io.github.mybatisext.jpa;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;

public class JpaTokenizer implements Tokenizer {

    private final TableInfo tableInfo;
    private final String text;
    private final Configuration configuration;
    private final Parameter[] parameters;
    private final List<Variable> variables;
    private final ExpectedTokens expectedTokens;
    private final TokenMarker tokenMarker;
    private int cursor = 0;

    public JpaTokenizer(TableInfo tableInfo, String text, Configuration configuration) {
        this(tableInfo, text, configuration, new Parameter[0]);
    }

    public JpaTokenizer(TableInfo tableInfo, String text, Configuration configuration, Parameter[] parameters) {
        this.tableInfo = tableInfo;
        this.text = text;
        this.configuration = configuration;
        this.parameters = parameters;
        this.variables = buildVariables(configuration, parameters);
        this.expectedTokens = new ExpectedTokens(text);
        this.tokenMarker = new TokenMarker(text);
    }

    private List<Variable> buildVariables(Configuration configuration, Parameter[] parameters) {
        List<Variable> variables = new ArrayList<>();
        if (parameters.length == 0) {
            return variables;
        }
        if (parameters.length == 1 && Variable.hasSubVariable(configuration, parameters[0].getType())) {
            Param param = parameters[0].getAnnotation(Param.class);
            if (param != null) {
                variables.add(new Variable(param.value(), parameters[0].getType()));
                variables.addAll(new Variable("", parameters[0].getType()).getSubVariable(configuration));
            } else {
                variables.addAll(new Variable("", parameters[0].getType()).getSubVariable(configuration));
            }
            return variables;
        }
        for (Parameter parameter : parameters) {
            Param param = parameter.getAnnotation(Param.class);
            if (param != null) {
                variables.add(new Variable(param.value(), parameter.getType()));
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
        for (PropertyInfo propertyInfo : prevPropertyInfo.getSubPropertyInfos()) {
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
        for (Variable variable : prevVariable.getSubVariable(configuration)) {
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

    public Configuration getConfiguration() {
        return configuration;
    }

    public Parameter[] getParameters() {
        return parameters;
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
