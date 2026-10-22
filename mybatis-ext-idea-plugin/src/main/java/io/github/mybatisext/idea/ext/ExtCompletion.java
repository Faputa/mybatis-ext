package io.github.mybatisext.idea.ext;

import java.lang.annotation.Annotation;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

import io.github.mybatisext.idea.model.SourceText;
import io.github.mybatisext.model.JavaAnnotation;
import io.github.mybatisext.model.JavaMethod;
import io.github.mybatisext.model.JavaParameter;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.UnresolvedTypeException;

public final class ExtCompletion extends CompletionContributor {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        PsiElement position = parameters.getPosition();
        if (DumbService.isDumb(position.getProject())) {
            return;
        }
        PsiElement originalPosition = parameters.getOriginalFile().findElementAt(Math.max(0, parameters.getOffset() - 1));
        try {
            PsiLiteralExpression literal = PsiTreeUtil.getParentOfType(position, PsiLiteralExpression.class);
            if (literal != null) {
                PsiLiteralExpression original = PsiTreeUtil.getParentOfType(originalPosition, PsiLiteralExpression.class);
                completePath(parameters, result, original == null ? literal : original);
                return;
            }
            PsiMethod originalMethod = PsiTreeUtil.getParentOfType(originalPosition, PsiMethod.class);
            PsiMethod method = originalMethod == null ? PsiTreeUtil.getParentOfType(position, PsiMethod.class) : originalMethod;
            if (method == null || method.getNameIdentifier() == null || !method.getNameIdentifier().getTextRange().containsOffset(parameters.getOffset())) {
                return;
            }
            PsiClass owner = method.getContainingClass();
            if (owner == null) {
                return;
            }
            // 优先使用真实源码，因为 Java 补全副本可能移除标识符后缀。
            String name = method.getName().replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "");
            int caret = parameters.getOffset() - method.getNameIdentifier().getTextOffset();
            if (caret < 0 || caret > name.length()) {
                return;
            }
            ExtSession session = new ExtSession(method);
            if (!session.derived(owner, method)) {
                return;
            }
            var table = session.table(owner);
            if (!table.isSuccess()) {
                return;
            }
            // 脱离原语法树的方法会丢失所属类型，因此使用原始声明创建方法模型，仅覆盖方法名。
            var original = session.model.method(method, session.model.type(owner));
            JavaMethod completionMethod = new NamedMethod(original, name);
            var completed = session.analyzer.completeMethod(table.getValue(), completionMethod, caret);
            if (completed.getValue() == null) {
                return;
            }
            String prefix = name.substring(0, caret);
            String suffix = name.substring(caret);
            for (var item : completed.getValue()) {
                String replacement = name.substring(0, item.getReplaceStart()) + item.getText() + name.substring(item.getReplaceEnd());
                result.withPrefixMatcher(prefix).addElement(LookupElementBuilder.create(replacement).withPresentableText(item.getText()).withTypeText("mybatis-ext " + item.getKind().name().toLowerCase())
                        .withInsertHandler((context, lookup) -> {
                            int start = context.getStartOffset();
                            int end = context.getTailOffset();
                            if (!suffix.isEmpty() && end + suffix.length() <= context.getDocument().getTextLength()
                                    && context.getDocument().getCharsSequence().subSequence(end, end + suffix.length()).toString().equals(suffix)) {
                                end += suffix.length();
                            }
                            context.getDocument().replaceString(start, end, replacement);
                            int newCaret = start + item.getReplaceStart() + item.getText().length();
                            context.getEditor().getCaretModel().moveToOffset(newCaret);
                            context.setTailOffset(start + replacement.length());
                        }));
            }
        } catch (UnresolvedTypeException ignored) {
            // 类型信息不完整时，不提供猜测性的补全。
        }
    }

    private void completePath(CompletionParameters parameters, CompletionResultSet result, PsiLiteralExpression literal) {
        ExtPaths.Context context = ExtPaths.context(literal);
        SourceText source = SourceText.of(literal);
        if (context == null || source == null) {
            return;
        }
        int offset = source.decodedOffset(parameters.getOffset() - literal.getTextOffset());
        String text = source.text.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "");
        if (offset > text.length()) {
            return;
        }
        if (context.parameterProperty()) {
            var content = source.range(0, source.text.length());
            if (content == null) {
                return;
            }
            int contentStart = literal.getTextOffset() + content.getStartOffset();
            int contentEnd = literal.getTextOffset() + content.getEndOffset();
            for (var item : context.session().analyzer.completeParameterProperty(context.table(), text, offset)) {
                String replacement = text.substring(0, item.getReplaceStart()) + item.getText() + text.substring(item.getReplaceEnd());
                String escaped = StringUtil.escapeStringCharacters(replacement);
                result.withPrefixMatcher(text.substring(0, offset)).addElement(LookupElementBuilder.create(replacement).withTypeText("mybatis-ext property")
                        .withInsertHandler((insertion, lookup) -> {
                            int end = insertion.getTailOffset() + contentEnd - parameters.getOffset();
                            insertion.getDocument().replaceString(contentStart, end, escaped);
                            int caret = contentStart + StringUtil.escapeStringCharacters(replacement.substring(0, item.getReplaceStart() + item.getText().length())).length();
                            insertion.getEditor().getCaretModel().moveToOffset(caret);
                            insertion.setTailOffset(contentStart + escaped.length());
                        }));
            }
            return;
        }
        int start = text.lastIndexOf('.', offset - 1) + 1;
        for (var item : context.session().analyzer.completePropertyPath(context.table(), text, offset)) {
            // IntelliJ 的字面量修改器会保留 Java 转义及路径前缀。
            result.withPrefixMatcher(text.substring(start, offset)).addElement(LookupElementBuilder.create(item.getText()).withTypeText("mybatis-ext property"));
        }
    }

    private record NamedMethod(JavaMethod delegate, String name) implements JavaMethod {

        public String getName() {
            return name;
        }

        public JavaType getDeclaringType() {
            return delegate.getDeclaringType();
        }

        public JavaType getGenericReturnType() {
            return delegate.getGenericReturnType();
        }

        public JavaParameter[] getParameters() {
            return delegate.getParameters();
        }

        public boolean isBridge() {
            return delegate.isBridge();
        }

        public boolean isDefault() {
            return delegate.isDefault();
        }

        public JavaAnnotation annotation(Class<? extends Annotation> type) {
            return delegate.annotation(type);
        }

        public JavaAnnotation[] annotations(Class<? extends Annotation> type) {
            return delegate.annotations(type);
        }
    }
}
