package io.github.mybatisext.idea.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.openapi.project.DumbService;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;

public final class MapperNavigation extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (DumbService.isDumb(element.getProject())) {
            return;
        }
        List<? extends PsiElement> targets = List.of();
        MybatisProject project = new MybatisProject(element);
        boolean toJava = false;
        if (element instanceof PsiIdentifier && element.getParent() instanceof PsiMethod method && element == method.getNameIdentifier() && method.getContainingClass() != null) {
            targets = project.statements(method.getContainingClass(), method.getName(), true);
        } else if (element instanceof PsiIdentifier && element.getParent() instanceof PsiClass cls && element == cls.getNameIdentifier()) {
            targets = project.mappers(cls);
        } else if (element instanceof XmlToken token && token.getTokenType() == XmlTokenType.XML_NAME
                && token.getPrevSibling() instanceof XmlToken start && start.getTokenType() == XmlTokenType.XML_START_TAG_START
                && token.getParent() instanceof XmlTag tag) {
            XmlTag root = MybatisProject.root(tag);
            if (root == null || !"mapper".equals(root.getName())) {
                return;
            }
            String attribute = tag == root ? "namespace" : tag.getParentTag() == root && MybatisProject.STATEMENTS.contains(tag.getName()) ? "id" : null;
            XmlAttribute attr = attribute == null ? null : tag.getAttribute(attribute);
            if (attr == null || attr.getValueElement() == null) {
                return;
            }
            Set<PsiElement> resolved = new LinkedHashSet<>();
            for (PsiReference reference : MybatisReferences.references(attr.getValueElement())) {
                if (reference instanceof PsiPolyVariantReference poly) {
                    for (ResolveResult r : poly.multiResolve(false)) {
                        if (r.getElement() != null) {
                            resolved.add(r.getElement());
                        }
                    }
                }
            }
            targets = new ArrayList<>(resolved);
            toJava = true;
        }
        String title = toJava ? "Navigate to MyBatis Mapper" : "Navigate to MyBatis XML";
        if (!targets.isEmpty()) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(toJava ? MybatisIcons.MAPPER : MybatisIcons.XML)
                    .setTargets(targets).setTooltipText(title).setPopupTitle(title);
            if (!toJava) {
                builder.setTargetRenderer(XmlTargetRenderer::new);
            }
            result.add(builder.createLineMarkerInfo(element));
        }
    }

    static final class XmlTargetRenderer extends PsiTargetPresentationRenderer<PsiElement> {

        @Override
        public String getElementText(PsiElement element) {
            if (element instanceof XmlTag tag && MybatisProject.STATEMENTS.contains(tag.getName())) {
                String databaseId = tag.getAttributeValue("databaseId");
                String id = tag.getAttributeValue("id");
                return databaseId == null || databaseId.isBlank() ? id : id + " [" + databaseId + "]";
            }
            return super.getElementText(element);
        }

        @Override
        public TargetPresentation getPresentation(PsiElement element) {
            return TargetPresentation.builder(getElementText(element))
                    .icon(getIcon(element))
                    .locationText(element.getContainingFile().getName())
                    .presentation();
        }
    }
}
