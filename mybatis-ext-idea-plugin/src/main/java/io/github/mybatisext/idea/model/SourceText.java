package io.github.mybatisext.idea.model;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;

/** 创建 PSI 引用或高亮前，必须先映射解码后文本的偏移量。 */
public final class SourceText {

    public final PsiElement element;
    public final String text;
    private final LiteralTextEscaper<?> escaper;
    private final TextRange content;

    private SourceText(PsiElement element, String text, LiteralTextEscaper<?> escaper, TextRange content) {
        this.element = element;
        this.text = text;
        this.escaper = escaper;
        this.content = content;
    }

    public static SourceText of(PsiElement element) {
        if (element instanceof PsiLanguageInjectionHost host && host.isValidHost()) {
            LiteralTextEscaper<?> escaper = host.createLiteralTextEscaper();
            TextRange range = escaper.getRelevantTextRange();
            StringBuilder text = new StringBuilder();
            if (escaper.decode(range, text)) {
                return new SourceText(element, text.toString(), escaper, range);
            }
        }
        if (element instanceof XmlAttributeValue attr) {
            return new SourceText(element, attr.getValue(), null, attr.getValueTextRange().shiftLeft(attr.getTextOffset()));
        }
        if (element instanceof XmlText xml) {
            return new SourceText(element, xml.getValue(), null, new TextRange(0, element.getTextLength()));
        }
        if (element instanceof XmlToken token && token.getTokenType() == XmlTokenType.XML_DATA_CHARACTERS) {
            return new SourceText(element, token.getText(), null, new TextRange(0, element.getTextLength()));
        }
        return null;
    }

    public TextRange range(int start, int end) {
        if (start < 0 || end < start || end > text.length()) {
            return null;
        }
        int s = escaper == null ? offset(start) : escaper.getOffsetInHost(start, content);
        int e = escaper == null ? offset(end) : escaper.getOffsetInHost(end, content);
        return s < 0 || e < s || e > element.getTextLength() ? null : new TextRange(s, e);
    }

    private int offset(int index) {
        return element instanceof XmlText xml ? xml.displayToPhysical(index) : content.getStartOffset() + index;
    }

    public int decodedOffset(int physical) {
        for (int i = 0; i <= text.length(); i++) {
            TextRange range = range(i, i);
            if (range != null && range.getStartOffset() >= physical) {
                return i;
            }
        }
        return text.length();
    }
}
