package io.github.mybatisext.idea.xml;

import java.util.Arrays;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;

public final class MybatisInspection extends LocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new XmlElementVisitor() {

            @Override
            public void visitXmlAttributeValue(@NotNull XmlAttributeValue value) {
                inspect(value);
            }

            @Override
            public void visitXmlText(@NotNull XmlText text) {
                inspect(text);
            }

            private void inspect(PsiElement element) {
                for (PsiReference ref : MybatisReferences.references(element)) {
                    if (ref instanceof MappingReference mapping && !ref.isSoft() && mapping.multiResolve(false).length == 0 && !ref.getCanonicalText().isBlank()) {
                        holder.registerProblem(element, mapping.problem + ": " + ref.getCanonicalText(), ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, ref.getRangeInElement());
                    }
                }
            }

            @Override
            public void visitXmlTag(@NotNull XmlTag tag) {
                if (DumbService.isDumb(tag.getProject()) || MybatisProject.root(tag) == null || tag.getParentTag() == null || !"mapper".equals(tag.getParentTag().getName())) {
                    return;
                }
                String id = tag.getAttributeValue("id");
                if (id == null) {
                    return;
                }
                String kind = MybatisProject.STATEMENTS.contains(tag.getName()) ? "statement" : tag.getName();
                MybatisProject project = new MybatisProject(tag);
                String namespace = tag.getParentTag().getAttributeValue("namespace");
                long count = project.roots("mapper:" + namespace).stream().flatMap(root -> Arrays.stream(root.getSubTags()))
                        .filter(other -> kind.equals(MybatisProject.STATEMENTS.contains(other.getName()) ? "statement" : other.getName()))
                        .filter(other -> id.equals(other.getAttributeValue("id")) && Objects.equals(tag.getAttributeValue("databaseId"), other.getAttributeValue("databaseId"))).count();
                if (count > 1 && tag.getAttribute("id") != null) {
                    holder.registerProblem(tag.getAttribute("id"), "Duplicate MyBatis " + kind + " ID: " + id);
                }
            }
        };
    }
}
