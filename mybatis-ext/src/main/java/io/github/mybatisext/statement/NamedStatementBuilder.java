package io.github.mybatisext.statement;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.ibatis.mapping.MappedStatement;

/**
 * 根据方法名生成statement
 */
public class NamedStatementBuilder {

    public MappedStatement build(String id, String methodName, List<Method> methods, Class<?> returnType, Class<?> tableType) {
        // TODO
        return null;
    }
}
