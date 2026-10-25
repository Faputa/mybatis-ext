package io.github.mybatisext.idea.ext;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.analysis.AnalysisResult;
import io.github.mybatisext.analysis.MapperResolver;
import io.github.mybatisext.analysis.MybatisExtAnalyzer;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.idea.model.PsiModel;
import io.github.mybatisext.idea.model.PsiTypePolicy;
import io.github.mybatisext.idea.xml.MybatisProject;
import io.github.mybatisext.jpa.Semantic;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.UnresolvedTypeException;

/** 每次请求使用独立的模型和核心缓存，PSI 读访问由 IDE 调用方负责。 */
public final class ExtSession {

    public final PsiModel model;
    public final MybatisExtAnalyzer analyzer;

    public ExtSession(PsiElement context) {
        model = new PsiModel(context.getProject());
        AnalysisSettings.State settings = context.getProject().getService(AnalysisSettings.class).getState();
        ExtContext extContext = new ExtContext();
        extContext.setDefaultFilterable(settings.defaultFilterable);
        analyzer = new MybatisExtAnalyzer(new PsiTypePolicy(settings.scalarTypes), extContext);
    }

    public boolean enhanced(PsiClass cls) {
        return cls != null && cls.getTypeParameters().length == 0 && !DumbService.isDumb(cls.getProject()) && MapperResolver.isEnhancedMapper(model.type(cls));
    }

    public JavaType entity(PsiClass mapper) {
        return MapperResolver.entityType(model.type(mapper));
    }

    public AnalysisResult<TableInfo> table(PsiClass mapper) {
        return analyzer.analyzeMetadata(entity(mapper));
    }

    public boolean derived(PsiClass mapper, PsiMethod method) {
        return !method.hasModifierProperty(PsiModifier.STATIC) && !method.hasModifierProperty(PsiModifier.PRIVATE)
                && enhanced(mapper) && MapperResolver.isDerivedMethod(model.type(mapper), model.method(method, model.type(mapper)), new MybatisProject(mapper)::statementExists);
    }

    /** 非增强方法返回 null；增强方法保留核心分析结果及诊断。 */
    public @Nullable AnalysisResult<Semantic> analyzeMethod(PsiMethod method) {
        try {
            PsiClass mapper = method.getContainingClass();
            if (mapper == null || !derived(mapper, method)) {
                return null;
            }

            var table = table(mapper);
            if (!table.isSuccess()) {
                return new AnalysisResult<>(null, table.getDiagnostics(), List.of());
            }
            return analyzer.analyzeMethod(table.getValue(), model.method(method, model.type(mapper)));
        } catch (UnresolvedTypeException e) {
            return new AnalysisResult<>(null, List.of(e.getDiagnostic()), List.of());
        }
    }

    public TableInfo pathTable(PsiClass owner) {
        JavaType type = model.type(owner);
        if (type.annotation(TableRef.class) != null) {
            type = type.annotation(TableRef.class).value("value");
        }
        return analyzer.analyzeMetadata(type).getValue();
    }
}
