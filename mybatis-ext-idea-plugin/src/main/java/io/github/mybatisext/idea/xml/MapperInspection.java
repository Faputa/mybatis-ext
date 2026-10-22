package io.github.mybatisext.idea.xml;

import org.apache.ibatis.annotations.Mapper;
import org.jetbrains.annotations.NotNull;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.InheritanceUtil;

import io.github.mybatisext.idea.ext.ExtSession;
import io.github.mybatisext.model.UnresolvedTypeException;

/** 检查普通 MyBatis Mapper；增强 Mapper 由共享的 CRUD 解析器校验。 */
public final class MapperInspection extends LocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                PsiClass mapper = method.getContainingClass();
                if (mapper == null || !mapper.isInterface() || mapper.getTypeParameters().length != 0 || method.getNameIdentifier() == null
                        || !method.hasModifierProperty(PsiModifier.ABSTRACT) || DumbService.isDumb(method.getProject())) {
                    return;
                }
                MybatisProject project = new MybatisProject(mapper);
                if (!mapper.hasAnnotation(Mapper.class.getName()) && project.mappers(mapper).isEmpty()) {
                    return;
                }
                // MyBatis-Plus 和 MyBatis-Flex 通过各自的提供器注入语句。
                if (InheritanceUtil.isInheritor(mapper, "com.baomidou.mybatisplus.core.mapper.BaseMapper") || InheritanceUtil.isInheritor(mapper, "com.mybatisflex.core.BaseMapper")) {
                    return;
                }
                try {
                    if (new ExtSession(mapper).enhanced(mapper)) {
                        return;
                    }
                } catch (UnresolvedTypeException e) {
                    return;
                }
                if (!project.statements(mapper, method.getName(), true).isEmpty() || MybatisProject.hasSqlAnnotation(method)) {
                    return;
                }
                for (PsiMethod parent : method.findSuperMethods()) {
                    if (MybatisProject.hasSqlAnnotation(parent)) {
                        return;
                    }
                }
                holder.registerProblem(method.getNameIdentifier(), "No MyBatis XML or annotation statement for method: " + method.getName(), ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            }
        };
    }
}
