package io.github.mybatisext.jpa;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TokenMarker {

    private final String text;
    private final Stack<Integer> endMarks = new Stack<>();

    public TokenMarker(String text) {
        this.text = text;
    }

    public TokenMarker(TokenMarker tokenMarker) {
        this.text = tokenMarker.text;
        this.endMarks.addAll(tokenMarker.endMarks);
    }

    public void record(int end) {
        while (!endMarks.isEmpty() && endMarks.peek() > end) {
            endMarks.pop();
        }
        endMarks.push(end);
    }

    public List<String> getTokens() {
        List<String> tokens = new ArrayList<>();
        int begin = 0;
        for (int end : endMarks) {
            tokens.add(text.substring(begin, end));
            begin = end;
        }
        return tokens;
    }

    public void printDiff(TokenMarker tokenMarker, PrintStream printStream) {
        int begin = 0;
        int end1;
        int end2;
        for (int i = 0; i < endMarks.size() && i < tokenMarker.endMarks.size(); i++) {
            end1 = endMarks.get(i);
            end2 = tokenMarker.endMarks.get(i);
            if (end1 != end2) {
                printStream.println(text);
                for (int j = 0; j < begin; j++) {
                    printStream.print(' ');
                }
                printStream.println(text.substring(begin, end1));
                for (int j = 0; j < begin; j++) {
                    printStream.print(' ');
                }
                printStream.println(text.substring(begin, end2));
                return;
            }
            begin = end1;
        }
    }

    public int getDiffBegin(TokenMarker tokenMarker) {
        int begin = 0;
        int end1;
        int end2;
        for (int i = 0; i < endMarks.size() && i < tokenMarker.endMarks.size(); i++) {
            end1 = endMarks.get(i);
            end2 = tokenMarker.endMarks.get(i);
            if (end1 != end2) {
                return begin;
            }
            begin = end1;
        }
        return 0;
    }

    @Override
    public String toString() {
        String s = "";
        int begin = 0;
        for (int end : endMarks) {
            if (begin > 0) {
                s += " ";
            }
            s += text.substring(begin, end);
            begin = end;
        }
        return s;
    }
}
