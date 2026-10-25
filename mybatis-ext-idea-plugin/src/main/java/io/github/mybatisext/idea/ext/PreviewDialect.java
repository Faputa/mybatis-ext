package io.github.mybatisext.idea.ext;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import io.github.mybatisext.dialect.DefaultDialectSelector;
import io.github.mybatisext.dialect.Dialect;

/** 仅使用静态配置线索，不建立数据库连接或执行应用配置逻辑。 */
public final class PreviewDialect {

    public record Selection(Dialect dialect, String description, String reason) {
    }

    private static final Key<CachedValue<Selection>> CACHE = Key.create("mybatisext.preview.dialect");
    private static final Key<CachedValue<Set<String>>> FILE_URLS = Key.create("mybatisext.preview.datasource.urls");
    private static final Pattern YAML_ENTRY = Pattern.compile("^(\\s*)([\\w.-]+)\\s*:\\s*(.*)$");
    private static final Pattern JDBC = Pattern.compile("(?i)^jdbc:([a-z0-9]+):");
    private static final Map<String, String> NAMES = Map.of("mysql", "MySQL", "oracle", "Oracle", "dm", "DM", "postgresql", "PostgreSQL", "h2", "H2", "sqlserver", "SQL Server");

    private PreviewDialect() {
    }

    public static Selection forMethod(PsiMethod method) {
        Project project = method.getProject();
        Module module = ModuleUtilCore.findModuleForPsiElement(method);
        UserDataHolder owner = module == null ? project : module;
        return CachedValuesManager.getManager(project).getCachedValue(owner, CACHE, () -> detect(project, module), false);
    }

    private static CachedValueProvider.Result<Selection> detect(Project project, Module module) {
        List<Object> dependencies = new ArrayList<>();
        dependencies.add(ProjectRootModificationTracker.getInstance(project));
        dependencies.add(VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS);
        Set<String> urls = new HashSet<>();
        if (module != null) {
            collect(GlobalSearchScope.moduleScope(module), project, urls, dependencies);
        }
        if (urls.isEmpty()) {
            collect(GlobalSearchScope.projectScope(project), project, urls, dependencies);
        }
        return CachedValueProvider.Result.create(select(urls), dependencies.toArray());
    }

    private static Selection select(Set<String> urls) {
        if (urls.isEmpty()) {
            return new Selection(DefaultDialectSelector.MYSQL_DIALECT, "MySQL（默认）", "未找到数据源配置");
        }
        Set<String> protocols = new HashSet<>();
        for (String url : urls) {
            var match = JDBC.matcher(url);
            if (url.contains("${") || !match.find() || !NAMES.containsKey(match.group(1).toLowerCase(Locale.ROOT))) {
                return fallback("数据源配置无法解析或方言不受支持");
            }
            protocols.add(match.group(1).toLowerCase(Locale.ROOT));
        }
        if (protocols.size() != 1) {
            return fallback("存在多种数据源方言");
        }
        String protocol = protocols.iterator().next();
        return new Selection(new DefaultDialectSelector().select("jdbc:" + protocol + ":"), NAMES.get(protocol), null);
    }

    private static Selection fallback(String reason) {
        return new Selection(DefaultDialectSelector.MYSQL_DIALECT, "MySQL（默认，方言未确定）", reason);
    }

    private static void collect(GlobalSearchScope scope, Project project, Set<String> urls, List<Object> dependencies) {
        for (String extension : List.of("properties", "yml", "yaml", "xml")) {
            for (var file : FilenameIndex.getAllFilesByExt(project, extension, scope)) {
                ProgressManager.checkCanceled();
                PsiFile psi = PsiManager.getInstance(project).findFile(file);
                if (psi == null) {
                    continue;
                }
                dependencies.add(psi);
                urls.addAll(CachedValuesManager.getCachedValue(psi, FILE_URLS, () -> CachedValueProvider.Result.create(collectFile(psi), psi)));
            }
        }
    }

    private static Set<String> collectFile(PsiFile psi) {
        Set<String> urls = new HashSet<>();
        String extension = psi.getVirtualFile().getExtension();
        if ("xml".equals(extension)) {
            if (psi instanceof XmlFile xml && xml.getRootTag() != null) {
                collectXml(xml.getRootTag(), urls);
            }
        } else if ("properties".equals(extension)) {
            Properties properties = new Properties();
            try {
                properties.load(new StringReader(psi.getText()));
            } catch (IOException | IllegalArgumentException e) {
                return urls;
            }
            for (String key : properties.stringPropertyNames()) {
                if (isUrlKey(key)) {
                    urls.add(properties.getProperty(key).trim());
                }
            }
        } else {
            collectYaml(psi.getText(), urls);
        }
        return urls;
    }

    private static boolean isUrlKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return (normalized.equals("url") || normalized.equals("jdbcurl") || normalized.contains("datasource") || normalized.startsWith("jdbc."))
                && (normalized.endsWith(".url") || normalized.endsWith("jdbcurl") || normalized.equals("url"));
    }

    private static void collectYaml(String text, Set<String> urls) {
        TreeMap<Integer, String> parents = new TreeMap<>();
        for (String line : text.split("\\R")) {
            var match = YAML_ENTRY.matcher(line);
            if (!match.matches()) {
                continue;
            }
            int indent = match.group(1).length();
            parents.tailMap(indent).clear();
            String key = (parents.isEmpty() ? "" : String.join(".", parents.values()) + ".") + match.group(2);
            String value = match.group(3).trim();
            if (value.isEmpty() || value.startsWith("#")) {
                if (isUrlKey(key)) {
                    urls.add("");
                }
                parents.put(indent, match.group(2));
                continue;
            }
            if (!isUrlKey(key)) {
                continue;
            }
            if (value.startsWith("\"") || value.startsWith("'")) {
                int end = value.indexOf(value.charAt(0), 1);
                value = end > 0 ? value.substring(1, end) : "";
            } else {
                value = value.replaceFirst("\\s+#.*$", "").trim();
            }
            urls.add(value);
        }
    }

    private static void collectXml(XmlTag tag, Set<String> urls) {
        ProgressManager.checkCanceled();
        if ("property".equals(tag.getLocalName()) && isUrlKey(Objects.toString(tag.getAttributeValue("name"), ""))) {
            String value = tag.getAttributeValue("value");
            XmlTag nested = tag.findFirstSubTag("value");
            if (value == null && nested != null) {
                value = nested.getValue().getTrimmedText();
            }
            if (value != null) {
                urls.add(value.trim());
            }
        }
        for (XmlTag child : tag.getSubTags()) {
            collectXml(child, urls);
        }
    }
}
