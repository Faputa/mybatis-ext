package io.github.mybatisext.idea.ext;

import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.util.PsiTreeUtil;

import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.diagnostic.DiagnosticLocation;
import io.github.mybatisext.idea.model.PsiModel;
import io.github.mybatisext.idea.model.SourceText;

/** 将核心位置信息映射到实际源码；注解实例无法唯一确定时使用回退位置。 */
public final class ExtLocation {

    public record Highlight(PsiElement element, TextRange range) {
    }

    public static Highlight locate(Diagnostic diagnostic, PsiElement fallback) {
        DiagnosticLocation location = diagnostic.getLocation();
        PsiElement source = location == null ? null : PsiModel.source(location.getSource());
        if (source == null || source.getContainingFile() != fallback.getContainingFile()) {
            source = fallback;
        }
        if (location != null && location.getAnnotationType() != null && source instanceof PsiModifierListOwner owner && owner.getModifierList() != null) {
            List<PsiElement> candidates = new ArrayList<>();
            for (PsiAnnotation annotation : PsiTreeUtil.findChildrenOfType(owner.getModifierList(), PsiAnnotation.class)) {
                if (!location.getAnnotationType().getName().equals(annotation.getQualifiedName())) {
                    continue;
                }
                String member = location.getAnnotationMember();
                if (member == null) {
                    candidates.add(annotation);
                    continue;
                }
                String leaf = member.substring(member.lastIndexOf('.') + 1);
                for (PsiNameValuePair pair : PsiTreeUtil.findChildrenOfType(annotation, PsiNameValuePair.class)) {
                    if (!leaf.equals(pair.getName() == null ? "value" : pair.getName()) || pair.getValue() == null) {
                        continue;
                    }
                    SourceText text = SourceText.of(pair.getValue());
                    if (location.getText() == null || text != null && location.getText().equals(text.text)) {
                        candidates.add(pair.getValue());
                    }
                }
            }
            if (candidates.size() == 1) {
                source = candidates.get(0);
                SourceText text = SourceText.of(source);
                TextRange range = diagnostic.hasRange() && text != null && text.text.equals(location.getText()) ? text.range(diagnostic.getStart(), diagnostic.getEnd()) : null;
                return new Highlight(source, range);
            }
        }
        if (source instanceof PsiNameIdentifierOwner named && named.getNameIdentifier() != null) {
            source = named.getNameIdentifier();
        }
        TextRange range = null;
        if (location != null && diagnostic.hasRange() && source.getText().equals(location.getText()) && diagnostic.getEnd() <= source.getTextLength()) {
            range = new TextRange(diagnostic.getStart(), diagnostic.getEnd());
        }
        return new Highlight(source, range);
    }
}
