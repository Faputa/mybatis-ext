package io.github.mybatisext.jpa;

import java.util.List;

public class JpaTokenizer implements Tokenizer {

    private final String text;
    private int cursor = 0;

    public JpaTokenizer(String text) {
        this.text = text;
    }

    public String keyword(String expect) {
        return "";
    }

    public List<String> property() {
        return null;
    }

    public String variable() {
        return "";
    }

    public int integer() {
        return -1;
    }

    public String getText() {
        return text;
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
