package io.github.mybatisext.jpa;

public interface Tokenizer {

    int getCursor();

    void setCursor(int cursor);

    String substring(int begin, int end);
}
