package io.github.mybatisext.idea.xml;

import javax.swing.Icon;

import org.apache.ibatis.annotations.Mapper;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlFile;

import io.github.mybatisext.annotation.MapTable;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.mapper.ExtMapper;

public final class MapperIconProvider extends IconProvider {

    @Override
    public Icon getIcon(PsiElement element, int flags) {
        if (element instanceof XmlFile xml) {
            var root = xml.getRootTag();
            return root != null && "mapper".equals(root.getName()) ? MybatisIcons.XML : null;
        }
        if (DumbService.isDumb(element.getProject())) {
            return null;
        }
        if (element instanceof PsiClass cls) {
            return typeIcon(cls);
        }
        if (element instanceof PsiJavaFile file) {
            for (PsiClass cls : file.getClasses()) {
                if ((cls.getName() + ".java").equals(file.getName())) {
                    return typeIcon(cls);
                }
            }
        }
        return null;
    }

    private Icon typeIcon(PsiClass cls) {
        if (cls.hasAnnotation(Table.class.getName())) {
            return MybatisIcons.TABLE;
        }
        if (cls.hasAnnotation(TableRef.class.getName())) {
            return MybatisIcons.TABLE_REF;
        }
        return isMapper(cls) ? MybatisIcons.MAPPER : null;
    }

    private boolean isMapper(PsiClass cls) {
        if (!cls.isInterface()) {
            return false;
        }
        if (cls.hasAnnotation(Mapper.class.getName()) || cls.hasAnnotation(MapTable.class.getName())
                || InheritanceUtil.isInheritor(cls, ExtMapper.class.getName())) {
            return true;
        }
        for (PsiMethod method : cls.getMethods()) {
            if (MybatisProject.hasSqlAnnotation(method)) {
                return true;
            }
        }
        return !new MybatisProject(cls).mappers(cls).isEmpty();
    }
}
