package io.github.mybatisext.statement;

import org.apache.ibatis.mapping.SqlCommandType;

import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.Semantic;

public class SqlCommandTypeResolver {

    public static SqlCommandType resolve(Semantic semantic) {
        switch (semantic.getType()) {
            case COUNT:
            case EXISTS:
            case SELECT:
                return SqlCommandType.SELECT;
            case DELETE:
                return SqlCommandType.DELETE;
            case INSERT:
                return SqlCommandType.INSERT;
            case UPDATE:
                return SqlCommandType.UPDATE;
        }
        throw new MybatisExtException("Unsupported semantic type: " + semantic.getType());
    }
}
