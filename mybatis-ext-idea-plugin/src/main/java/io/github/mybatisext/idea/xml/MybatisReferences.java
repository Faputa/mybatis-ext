package io.github.mybatisext.idea.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;
import com.intellij.util.ProcessingContext;

import io.github.mybatisext.idea.model.SourceText;

public final class MybatisReferences extends PsiReferenceContributor {

    private static final Set<String> TYPE_ATTRIBUTES = Set.of("resultType", "parameterType", "javaType", "ofType", "typeHandler");
    private static final Pattern PLACEHOLDER = Pattern.compile("[#$]\\{\\s*(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(?:\\.\\p{javaJavaIdentifierPart}+)*)");

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        PsiReferenceProvider provider = new PsiReferenceProvider() {

            @Override
            public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                return references(element).toArray(PsiReference[]::new);
            }
        };
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlAttributeValue.class), provider);
        // IDEA 在文本 token 上查找光标引用，CDATA 的 token 与 XmlText 之间还隔着一层节点。
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlToken.class).inside(XmlText.class), provider);
    }

    public static List<PsiReference> references(PsiElement element) {
        if (DumbService.isDumb(element.getProject()) || MybatisProject.root(element) == null) {
            return List.of();
        }
        SourceText source = SourceText.of(element);
        if (source == null) {
            return List.of();
        }
        List<PsiReference> refs = new ArrayList<>();
        MybatisProject project = new MybatisProject(element);
        if (element instanceof XmlAttributeValue value && value.getParent() instanceof XmlAttribute attr) {
            XmlTag tag = attr.getParent();
            String name = attr.getName();
            String text = source.text;
            if ("namespace".equals(name) && "mapper".equals(tag.getName())) {
                // MyBatis 允许仅在 XML 中定义命名空间，不要求对应的 Java 接口。
                add(refs, source, 0, text.length(), true, "Mapper interface not found", () -> nullable(project.findClass(text)), () -> project.classVariants(true));
            } else if ("id".equals(name) && MybatisProject.STATEMENTS.contains(tag.getName())) {
                PsiClass mapper = project.mapper(MybatisProject.root(tag));
                add(refs, source, 0, text.length(), mapper == null, "Mapper method not found", () -> mapper == null ? List.of() : Arrays.asList(mapper.findMethodsByName(text, true)),
                        () -> mapper == null ? new Object[0] : Arrays.stream(mapper.getAllMethods()).filter(m -> !m.isConstructor() && !m.hasModifierProperty(PsiModifier.STATIC)).map(m -> LookupElementBuilder.create(m, m.getName())).toArray());
            } else if (TYPE_ATTRIBUTES.contains(name) || "type".equals(name) && Set.of("resultMap", "typeAlias", "typeHandler", "objectFactory", "objectWrapperFactory", "reflectorFactory").contains(tag.getName()) || "class".equals(name) && Set.of("mapper", "cache").contains(tag.getName()) || "interceptor".equals(name) && "plugin".equals(tag.getName())) {
                boolean primitive = text.startsWith("_") || text.endsWith("[]");
                add(refs, source, 0, text.length(), primitive, "Java type or MyBatis alias not found", () -> nullable(project.resolveType(text)), () -> project.classVariants(false));
            } else if ("refid".equals(name) && "include".equals(tag.getName()) || "resultMap".equals(name) || "extends".equals(name) && "resultMap".equals(tag.getName()) || "select".equals(name) && Set.of("association", "collection").contains(tag.getName())) {
                String kind = "refid".equals(name) ? "sql" : "select".equals(name) ? "select" : "resultMap";
                Matcher tokens = Pattern.compile("[^,\\s]+").matcher(text);
                while (tokens.find()) {
                    String id = tokens.group();
                    add(refs, source, tokens.start(), tokens.end(), id.contains("${"), "MyBatis " + kind + " reference not found", () -> new ArrayList<>(project.declarations(tag, id, kind)), () -> declarationVariants(project, tag, kind, id));
                }
            } else if ("property".equals(name) && Set.of("id", "result", "association", "collection").contains(tag.getName())) {
                propertyReferences(refs, source, resultOwner(tag, project), 0, text, false);
            } else if ("resource".equals(name) && "mapper".equals(tag.getName())) {
                TextRange range = source.range(0, text.length());
                if (range != null) {
                    refs.addAll(Arrays.asList(new FileReferenceSet(text, element, range.getStartOffset(), null, true).getAllReferences()));
                }
            } else if ("collection".equals(name) && "foreach".equals(tag.getName())) {
                // foreach 声明本身不在其 item 变量的词法作用域内。
                bindingReferences(refs, source, 0, text, true);
            } else if ("keyProperty".equals(name)) {
                Matcher paths = Pattern.compile("[^,\\s]+").matcher(text);
                while (paths.find()) {
                    bindingReferences(refs, source, paths.start(), paths.group(), false);
                }
            } else if ("test".equals(name) || "value".equals(name) && "bind".equals(tag.getName())) {
                Matcher paths = Pattern.compile("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*").matcher(text);
                while (paths.find()) {
                    if (!Set.of("null", "true", "false", "and", "or", "not", "eq", "ne", "gt", "lt", "ge", "le", "empty", "instanceof").contains(paths.group())
                            && (paths.end() == text.length() || text.charAt(paths.end()) != '(')) {
                        bindingReferences(refs, source, paths.start(), paths.group(), true);
                    }
                }
            }
        }
        Matcher placeholders = PLACEHOLDER.matcher(source.text);
        while (placeholders.find()) {
            bindingReferences(refs, source, placeholders.start(1), placeholders.group(1), false);
        }
        return refs;
    }

    private static Object[] declarationVariants(MybatisProject project, XmlTag tag, String kind, String typed) {
        XmlTag root = MybatisProject.root(tag);
        if (root == null) {
            return new Object[0];
        }
        List<Object> variants = new ArrayList<>();
        int dot = typed.lastIndexOf('.');
        String prefix = dot < 0 ? "" : typed.substring(0, dot + 1);
        String namespace = dot < 0 ? root.getAttributeValue("namespace") : typed.substring(0, dot);
        for (XmlTag mapper : project.roots("mapper:" + namespace)) {
            for (XmlTag child : mapper.getSubTags()) {
                String id = child.getAttributeValue("id");
                if (kind.equals(child.getName()) && id != null) {
                    variants.add(LookupElementBuilder.create(child, prefix + id));
                }
            }
        }
        return variants.toArray();
    }

    private static List<PsiElement> nullable(PsiElement element) {
        return element == null ? List.of() : List.of(element);
    }

    private static void add(List<PsiReference> refs, SourceText source, int start, int end, boolean soft, String problem,
            Supplier<? extends Collection<? extends PsiElement>> targets, Supplier<Object[]> variants) {
        TextRange range = source.range(start, end);
        if (range != null) {
            refs.add(new MappingReference(source.element, range, soft, problem, targets, variants));
        }
    }

    public static PsiType resultOwner(XmlTag tag, MybatisProject project) {
        XmlTag parent = tag.getParentTag();
        if (parent == null) {
            return null;
        }
        if ("resultMap".equals(parent.getName())) {
            return MybatisProject.classType(project.resolveType(parent.getAttributeValue("type")));
        }
        if (Set.of("association", "collection").contains(parent.getName())) {
            String explicit = parent.getAttributeValue("ofType");
            if (explicit == null) {
                explicit = parent.getAttributeValue("javaType");
            }
            if (explicit != null) {
                return MybatisProject.classType(project.resolveType(explicit));
            }
            PsiType owner = resultOwner(parent, project);
            PsiElement property = MybatisProject.properties(owner).get(parent.getAttributeValue("property"));
            return MybatisProject.unwrap(MybatisProject.propertyType(owner, property));
        }
        return resultOwner(parent, project);
    }

    private static void propertyReferences(List<PsiReference> refs, SourceText source, PsiType owner, int base, String path, boolean soft) {
        int offset = 0;
        PsiType current = owner;
        for (String segment : path.split("\\.", -1)) {
            PsiType type = current;
            Map<String, PsiElement> properties = MybatisProject.properties(type);
            PsiElement target = properties.get(segment);
            add(refs, source, base + offset, base + offset + segment.length(), soft || type == null || XmlBindings.map(type), "Mapped property not found", () -> nullable(MybatisProject.propertyTarget(segment, target)),
                    () -> properties.entrySet().stream().map(e -> LookupElementBuilder.create(MybatisProject.propertyTarget(e.getKey(), e.getValue()), e.getKey())).toArray());
            current = target == null ? null : MybatisProject.propertyType(type, target);
            offset += segment.length() + 1;
        }
    }

    private static void bindingReferences(List<PsiReference> refs, SourceText source, int base, String path, boolean soft) {
        XmlBindings bindings = new XmlBindings(source.element);
        String[] segments = path.split("\\.", -1);
        int offset = 0;
        for (int i = 0; i < segments.length; i++) {
            String prefix = String.join(".", Arrays.copyOfRange(segments, 0, i + 1));
            String parent = String.join(".", Arrays.copyOfRange(segments, 0, i));
            XmlBindings.Binding binding = bindings.resolve(prefix);
            XmlBindings.Binding parentBinding = parent.isEmpty() ? null : bindings.resolve(parent);
            boolean unknown = bindings.dynamic || parentBinding != null && parentBinding.dynamic() || binding != null && binding.dynamic();
            add(refs, source, base + offset, base + offset + segments[i].length(), soft || unknown, "MyBatis parameter or property not found",
                    () -> binding == null ? List.of() : nullable(binding.source()),
                    () -> bindings.candidates(parent).entrySet().stream().map(e -> LookupElementBuilder.create(e.getKey())).toArray());
            offset += segments[i].length() + 1;
        }
    }
}
