package io.github.mybatisext.statement;

import java.util.HashMap;
import java.util.Map;

public class ParameterSignature {

    private Class<?> type;
    private final Map<String, Class<?>> nameToType = new HashMap<>();

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public Map<String, Class<?>> getNameToType() {
        return nameToType;
    }
}
