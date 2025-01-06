package io.github.mybatisext.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.Semantic;
import io.github.mybatisext.jpa.SemanticType;
import io.github.mybatisext.ognl.Ognl;

public class SemanticScriptHelper {

    public static String buildScript(Map<String, Semantic> signatureToSemantic, Dialect dialect) {
        if (signatureToSemantic.size() == 1) {
            for (Semantic semantic : signatureToSemantic.values()) {
                return "<script>" + buildScriptContent(semantic, dialect) + "</script>";
            }
        }
        List<String> ss = new ArrayList<>();
        for (Map.Entry<String, Semantic> entry : signatureToSemantic.entrySet()) {
            ss.add("<if test=\"" + Ognl.IsParameterSignatureMatch + "(_parameter, '" + entry.getKey() + "')\">");
            ss.add(buildScriptContent(entry.getValue(), dialect));
            ss.add("</if>");
        }
        return "<script>" + String.join(" ", ss) + "</script>";
    }

    public static String buildScriptContent(Semantic semantic, Dialect dialect) {
        if (semantic.getType() == SemanticType.COUNT) {
            return dialect.count(semantic.getTableInfo(), semantic.getWhere());
        }
        if (semantic.getType() == SemanticType.EXISTS) {
            return dialect.exists(semantic.getTableInfo(), semantic.getWhere());
        }
        if (semantic.getType() == SemanticType.SELECT) {
            return dialect.select(semantic.getTableInfo(), semantic.getWhere(), semantic.getSelectItems(), semantic.isDistinct(), semantic.getOrderBy(), semantic.getGroupBy(), semantic.getHaving(), semantic.getLimit());
        }
        if (semantic.getType() == SemanticType.DELETE) {
            return dialect.delete(semantic.getTableInfo(), semantic.getParameter(), semantic.getWhere());
        }
        if (semantic.getType() == SemanticType.INSERT) {
            return dialect.insert(semantic.getTableInfo(), semantic.getParameter(), semantic.isIgnoreNull());
        }
        if (semantic.getType() == SemanticType.UPDATE) {
            return dialect.update(semantic.getTableInfo(), semantic.getParameter(), semantic.getWhere(), semantic.isIgnoreNull());
        }
        throw new MybatisExtException("Unsupported semantic type: " + semantic.getType());
    }
}
