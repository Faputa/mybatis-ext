package io.github.mybatisext.idea.xml;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ibatis.type.JdbcType;
import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;

import io.github.mybatisext.idea.model.SourceText;

public final class MybatisCompletion extends CompletionContributor {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        PsiElement position = parameters.getPosition();
        if (DumbService.isDumb(position.getProject()) || MybatisProject.root(position) == null) {
            return;
        }
        PsiElement host = PsiTreeUtil.getParentOfType(position, XmlAttributeValue.class, XmlText.class);
        SourceText source = host == null ? null : SourceText.of(host);
        if (source != null) {
            int caret = parameters.getOffset() - host.getTextOffset();
            int decoded = source.decodedOffset(caret);
            String before = source.text.substring(0, decoded);
            Matcher placeholder = Pattern.compile("[#$]\\{\\s*([\\w.$]*)$").matcher(before);
            if (placeholder.find()) {
                String path = placeholder.group(1);
                int dot = path.lastIndexOf('.');
                String parent = dot < 0 ? "" : path.substring(0, dot);
                String prefix = path.substring(dot + 1);
                XmlBindings bindings = new XmlBindings(host);
                bindings.candidates(parent).forEach((name, binding) -> result.withPrefixMatcher(prefix).addElement(LookupElementBuilder.create(name).withTypeText("MyBatis parameter")));
                return;
            }
            for (PsiReference ref : MybatisReferences.references(host)) {
                if (ref.getRangeInElement().containsOffset(caret)) {
                    String rawPrefix = host.getText().substring(ref.getRangeInElement().getStartOffset(), Math.min(caret, ref.getRangeInElement().getEndOffset()));
                    CompletionResultSet variants = result.withPrefixMatcher(rawPrefix);
                    for (Object variant : ref.getVariants()) {
                        if (variant instanceof LookupElement lookup) {
                            variants.addElement(lookup);
                        } else {
                            variants.addElement(LookupElementBuilder.create(variant.toString()));
                        }
                    }
                }
            }
            if (host instanceof XmlAttributeValue value && value.getParent() instanceof XmlAttribute attr) {
                String name = attr.getName();
                Collection<String> constants = switch (name) {
                    case "jdbcType" -> Arrays.stream(JdbcType.values()).map(Enum::name).toList();
                    case "statementType" -> List.of("STATEMENT", "PREPARED", "CALLABLE");
                    case "fetchType" -> List.of("lazy", "eager");
                    case "useCache", "flushCache", "useGeneratedKeys", "resultOrdered" -> List.of("true", "false");
                    default -> List.of();
                };
                constants.forEach(v -> result.withPrefixMatcher(before).addElement(LookupElementBuilder.create(v)));
            }
            return;
        }
        // 具有 DTD 的文件使用原生 XML 补全，同时支持尚未声明 DOCTYPE 的文件。
        XmlTag tag = PsiTreeUtil.getParentOfType(position, XmlTag.class);
        if (tag == null) {
            return;
        }
        if (position instanceof XmlToken token && token.getTokenType() == XmlTokenType.XML_NAME && position.getParent() instanceof XmlAttribute) {
            for (String attribute : attributes(tag.getName())) {
                if (tag.getAttribute(attribute) == null) {
                    result.addElement(LookupElementBuilder.create(attribute));
                }
            }
        } else if (position instanceof XmlToken token && token.getTokenType() == XmlTokenType.XML_NAME) {
            XmlTag parent = tag.getParentTag();
            String parentName = parent == null ? tag.getName() : parent.getName();
            Collection<String> tags = switch (parentName) {
                case "mapper" -> List.of("select", "insert", "update", "delete", "resultMap", "sql", "cache", "cache-ref");
                case "resultMap", "association", "collection" -> List.of("id", "result", "association", "collection", "constructor", "discriminator");
                case "configuration" -> List.of("properties", "settings", "typeAliases", "typeHandlers", "plugins", "environments", "databaseIdProvider", "mappers");
                case "mappers" -> List.of("mapper", "package");
                case "typeAliases" -> List.of("typeAlias", "package");
                case "choose" -> List.of("when", "otherwise");
                default -> List.of("if", "where", "set", "trim", "foreach", "choose", "include", "bind");
            };
            tags.forEach(n -> result.addElement(LookupElementBuilder.create(n)));
        }
    }

    private static Collection<String> attributes(String tag) {
        return switch (tag) {
            case "mapper" -> List.of("namespace");
            case "select" -> List.of("id", "parameterType", "resultType", "resultMap", "timeout", "fetchSize", "statementType", "useCache", "flushCache", "databaseId");
            case "insert", "update", "delete" -> List.of("id", "parameterType", "timeout", "statementType", "flushCache", "databaseId", "useGeneratedKeys", "keyProperty", "keyColumn");
            case "resultMap" -> List.of("id", "type", "extends", "autoMapping");
            case "id", "result" -> List.of("property", "column", "javaType", "jdbcType", "typeHandler");
            case "association", "collection" -> List.of("property", "column", "javaType", "ofType", "select", "resultMap", "fetchType", "notNullColumn", "columnPrefix");
            case "foreach" -> List.of("collection", "item", "index", "open", "close", "separator", "nullable");
            case "if", "when" -> List.of("test");
            case "bind" -> List.of("name", "value");
            case "include" -> List.of("refid");
            case "sql" -> List.of("id", "databaseId");
            case "trim" -> List.of("prefix", "suffix", "prefixOverrides", "suffixOverrides");
            default -> List.of();
        };
    }
}
