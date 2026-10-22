package io.github.mybatisext.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.diagnostic.DiagnosticLocation;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.jpa.JpaParser;
import io.github.mybatisext.jpa.Semantic;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.PropertyPath;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.model.JavaMethod;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.TypePolicy;

/** 每个稳定的源码与配置快照使用一个实例；传入配置在实例使用期间应保持不变，源码、泛型上下文或配置变化时应丢弃该实例。 */
public final class MybatisExtAnalyzer {

    private final TableInfoFactory tables;
    private final JpaParser methods;

    public MybatisExtAnalyzer(TypePolicy typePolicy, ExtContext context) {
        tables = new TableInfoFactory(typePolicy, context);
        methods = new JpaParser(typePolicy, tables);
    }

    public AnalysisResult<TableInfo> analyzeMetadata(JavaType type) {
        try {
            return new AnalysisResult<>(tables.buildTableInfo(type), Collections.emptyList(), Collections.emptyList());
        } catch (MybatisExtException e) {
            if (e.getDiagnostic() == null) {
                throw e;
            }
            return new AnalysisResult<>(null, Collections.singletonList(e.getDiagnostic()), Collections.emptyList());
        }
    }

    public AnalysisResult<Semantic> analyzeMethod(TableInfo table, JavaMethod method) {
        return methods.analyze(table, method);
    }

    public AnalysisResult<List<CompletionItem>> completeMethod(TableInfo table, JavaMethod method, int offset) {
        return methods.complete(table, method, offset);
    }

    public TableInfo parameterPropertyTable(TableInfo table, JavaMethod method, int parameterIndex) {
        return methods.parameterPropertyTable(table, method, parameterIndex);
    }

    public AnalysisResult<PropertyInfo> resolveParameterProperty(TableInfo table, String name) {
        return methods.resolveParameterProperty(table, name);
    }

    public List<CompletionItem> completeParameterProperty(TableInfo table, String name, int offset) {
        return methods.completeParameterProperty(table, name, offset);
    }

    public AnalysisResult<PropertyInfo> resolvePropertyPath(TableInfo table, String path) {
        List<SymbolReference> references = new ArrayList<>();
        try {
            PropertyInfo property = PropertyPath.resolve(table.getNameToPropertyInfo(), path, table.getClassType(), PropertyInfo::getNameToPropertyInfo,
                    new DiagnosticLocation(table.getClassType(), table.getClassType(), null, null, path).forPath(path, path),
                    (propertyValue, start, end) -> references.add(new SymbolReference(start, end, propertyValue)));
            return new AnalysisResult<>(property, Collections.emptyList(), references);
        } catch (MybatisExtException e) {
            if (e.getDiagnostic() == null) {
                throw e;
            }
            return new AnalysisResult<>(null, Collections.singletonList(e.getDiagnostic()), references);
        }
    }

    public List<CompletionItem> completePropertyPath(TableInfo table, String path, int offset) {
        if (offset < 0 || offset > path.length()) {
            throw new IllegalArgumentException("Invalid completion offset: " + offset);
        }
        int start = path.lastIndexOf('.', offset - 1) + 1;
        Map<String, PropertyInfo> properties = table.getNameToPropertyInfo();
        if (start > 0) {
            String parentPath = path.substring(0, start - 1);
            try {
                properties = PropertyPath.resolve(properties, parentPath, table.getClassType(), PropertyInfo::getNameToPropertyInfo,
                        new DiagnosticLocation(table.getClassType(), table.getClassType(), null, null, parentPath).forPath(parentPath, parentPath), null).getNameToPropertyInfo();
            } catch (MybatisExtException e) {
                if (e.getDiagnostic() == null) {
                    throw e;
                }
                return Collections.emptyList();
            }
        }
        String prefix = path.substring(start, offset);
        int end = path.indexOf('.', offset);
        if (end < 0) {
            end = path.length();
        }
        List<CompletionItem> items = new ArrayList<>();
        for (PropertyInfo property : properties.values()) {
            if (property.getName().startsWith(prefix)) {
                items.add(new CompletionItem(property.getName(), CompletionItem.Kind.PROPERTY, start, end, property));
            }
        }
        items.sort(Comparator.comparing(CompletionItem::getText));
        return items;
    }
}
