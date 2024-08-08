package io.github.mybatisext.jpa;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

public class Checkpoint {

    private final String text;
    private final Set<String> expects = new HashSet<>();
    private int cursor = 0;

    public Checkpoint(String text) {
        this.text = text;
    }

    public void update(int cur, String expect) {
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cursor; i++) {
            sb.append(" ");
        }
        for (int i = cursor; i < text.length(); i++) {
            sb.append("~");
        }
        printStream.println(text);
        printStream.println(sb);
        printStream.println(this);
    }
}
