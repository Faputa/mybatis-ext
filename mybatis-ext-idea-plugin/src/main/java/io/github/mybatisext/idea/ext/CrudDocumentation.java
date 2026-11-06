package io.github.mybatisext.idea.ext;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.intellij.model.Pointer;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.backend.documentation.DocumentationResult;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.documentation.DocumentationTargetProvider;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.SmartPointerManager;

import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.statement.SemanticScriptHelper;

/** 文档预览与运行时 CRUD 共用语义模型和脚本渲染器。 */
public final class CrudDocumentation implements DocumentationTargetProvider {

    @Override
    public @NotNull List<? extends DocumentationTarget> documentationTargets(PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (!(element instanceof PsiIdentifier) || DumbService.isDumb(file.getProject())) {
            return List.of();
        }
        PsiElement parent = element.getParent();
        PsiMethod method = parent instanceof PsiMethod m && element == m.getNameIdentifier()
                ? m
                : parent instanceof PsiReferenceExpression reference && element == reference.getReferenceNameElement()
                        && reference.resolve() instanceof PsiMethod m ? m : null;
        String html = render(method);
        return html == null ? List.of() : List.of(new Target(method, html));
    }

    public static String render(PsiMethod method) {
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

    private record Target(PsiMethod method, String html) implements DocumentationTarget {

        @Override
        public Pointer<? extends DocumentationTarget> createPointer() {
            var pointer = SmartPointerManager.createPointer(method);
            return () -> {
                PsiMethod restored = pointer.getElement();
                String currentHtml = render(restored);
                return currentHtml == null ? null : new Target(restored, currentHtml);
            };
        }

        @Override
        public TargetPresentation computePresentation() {
            return TargetPresentation.builder(method.getName()).icon(method.getIcon(0))
                    .containerText(method.getContainingClass().getQualifiedName()).presentation();
        }

        @Override
        public Navigatable getNavigatable() {
            return method;
        }

        @Override
        public DocumentationResult computeDocumentation() {
            return DocumentationResult.documentation(html);
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
