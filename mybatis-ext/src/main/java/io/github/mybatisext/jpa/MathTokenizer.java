package io.github.mybatisext.jpa;

public class MathTokenizer implements Tokenizer {

    private final String text;
    private int cursor = 0;

    public MathTokenizer(String text) {
        this.text = text;
    }

    public String next() {
        while (cursor < text.length()) {
            char c = text.charAt(cursor++);
            if (!Character.isWhitespace(c)) {
                return String.valueOf(c);
            }
        }
        return "";
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
