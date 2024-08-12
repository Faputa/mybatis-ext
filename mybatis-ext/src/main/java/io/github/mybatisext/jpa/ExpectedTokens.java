package io.github.mybatisext.jpa;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

public class ExpectedTokens {

    private final String text;
    private final Set<String> expects = new HashSet<>();
    private int cursor = 0;

    public ExpectedTokens(String text) {
        this.text = text;
    }

    public void record(int cur, String expect) {
        if (cur == cursor) {
            expects.add(expect);
            return;
        }
        if (cur > cursor) {
            expects.clear();
            expects.add(expect);
            cursor = cur;
        }
    }

    public String getText() {
        return text;
    }

    public Set<String> getExpects() {
        return expects;
    }

    public int getCursor() {
        return cursor;
    }

    @Override
    public String toString() {
        return "Expected " + String.join(" or ", expects) + ", but found '" + text.substring(cursor) + "'.";
    }

    public void printMessage(PrintStream printStream) {
        printStream.println(text);
        for (int i = 0; i < cursor; i++) {
            printStream.print(' ');
        }
        for (int i = cursor; i < text.length(); i++) {
            printStream.print('~');
        }
        printStream.println();
        printStream.println(this);
    }
}
