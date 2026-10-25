package io.github.mybatisext.idea.xml;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.util.Processor;

/** getter 的属性引用使用属性名，Java 字符串和 XML 中的用法需以此名称搜索。 */
public final class PropertyMethodReferencesSearch extends QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters> {

    public PropertyMethodReferencesSearch() {
        super(true);
    }

    @Override
    public void processQuery(@NotNull MethodReferencesSearch.SearchParameters parameters, @NotNull Processor<? super PsiReference> consumer) {
        PsiMethod method = parameters.getMethod();
        if (!PropertyUtilBase.isSimplePropertyGetter(method)) {
            return;
        }
        String property = PropertyUtilBase.getPropertyName(method);
        parameters.getOptimizer().searchWord(property, parameters.getEffectiveSearchScope(),
                (short) (UsageSearchContext.IN_STRINGS | UsageSearchContext.IN_FOREIGN_LANGUAGES), true, method);

        // flagsDotActive 这样的语法名称不是独立单词，通过注解索引补充嵌套属性引用。
        PsiClass param = JavaPsiFacade.getInstance(method.getProject()).findClass(MybatisProject.PARAM, method.getResolveScope());
        if (param == null) {
            return;
        }
        String nestedProperty = "Dot" + StringUtil.capitalize(property);
        for (PsiParameter parameter : AnnotatedElementsSearch.searchPsiParameters(param, parameters.getEffectiveSearchScope())) {
            ProgressManager.checkCanceled();
            PsiAnnotation annotation = parameter.getAnnotation(MybatisProject.PARAM);
            if (annotation == null || !(annotation.findAttributeValue("value") instanceof PsiLiteralExpression literal)
                    || !(literal.getValue() instanceof String text) || !(text.startsWith(property + "Dot") || text.contains(nestedProperty))) {
                continue;
            }
            for (PsiReference reference : literal.getReferences()) {
                if (reference.isReferenceTo(method) && !consumer.process(reference)) {
                    return;
                }
            }
        }
    }
}
