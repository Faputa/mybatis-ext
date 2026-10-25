package io.github.mybatisext.idea.ext;

import java.util.List;
import java.util.Objects;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;

import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.statement.SemanticScriptHelper;

/** 文档预览与运行时 CRUD 共用语义模型和脚本渲染器。 */
public final class CrudDocumentation extends AbstractDocumentationProvider {

    @Override
    public String generateDoc(PsiElement element, PsiElement originalElement) {
        PsiMethod method = element instanceof PsiMethod m
                ? m
                : element instanceof PsiIdentifier && element.getParent() instanceof PsiMethod m && element == m.getNameIdentifier() ? m : null;
        if (method == null || method.getContainingClass() == null || DumbService.isDumb(method.getProject())) {
            return null;
        }
        ExtSession session = new ExtSession(method);
        try {
            var analyzed = session.analyzeMethod(method);
            if (analyzed == null) {
                return null;
            }
            if (!analyzed.isSuccess()) {
                return failure(analyzed.getDiagnostics());
            }
            var selection = PreviewDialect.forMethod(method);
            String script = "<script>\n" + SemanticScriptHelper.buildScriptContent(analyzed.getValue(), selection.dialect()) + "\n</script>";
            String dialect = escape(selection.description());
            if (selection.reason() != null) {
                dialect = "<span title=\"" + escape(selection.reason()) + "\">" + dialect + "</span>";
            }
            return "<div class='definition'><pre>mybatis-ext CRUD script</pre></div><div class='content'>"
                    + dialect + "<pre style='white-space:pre-wrap'>"
                    + escape(script) + "</pre></div>";
        } catch (MybatisExtException e) {
            return failureText(e.getDiagnostic() == null ? e.getMessage() : e.getDiagnostic().getMessage());
        }
    }

    private static String failure(List<Diagnostic> diagnostics) {
        return failureText(String.join("\n", diagnostics.stream().map(Diagnostic::getMessage).toList()));
    }

    private static String failureText(String message) {
        return "<div class='definition'><pre>mybatis-ext CRUD script</pre></div><div class='content'>Preview unavailable:<pre>"
                + escape(Objects.toString(message, "Analysis failed")) + "</pre></div>";
    }

    private static String escape(String text) {
        return StringUtil.escapeXmlEntities(text);
    }
}
