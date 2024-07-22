package io.github.mybatisext.jpa;

import java.util.HashSet;
import java.util.Set;

public class MathTokenizer implements Tokenizer {

    private final String text;
    private int cursor = 0;
    private final Set<String> keywords = new HashSet<>();

    public MathTokenizer(String text) {
        this.text = text;
    }

    public String next() {
        for (; cursor < text.length(); cursor++) {
            String c = String.valueOf(text.charAt(cursor));
            if (keywords.contains(c)) {
                return c;
            }
        }
        return "";
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
    public int getCursor() {
        return cursor;
    }

    @Override
    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    @Override
    public String substring(int begin, int end) {
        return text.substring(begin, end).trim();
    }
}
