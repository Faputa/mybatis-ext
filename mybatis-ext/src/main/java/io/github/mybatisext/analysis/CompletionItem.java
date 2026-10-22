package io.github.mybatisext.analysis;

public final class CompletionItem {

    public enum Kind {
        KEYWORD, PROPERTY, VARIABLE
    }

    private final String text;
    private final Kind kind;
    private final int replaceStart;
    private final int replaceEnd;
    private final Object target;

    public CompletionItem(String text, Kind kind, int replaceStart, int replaceEnd, Object target) {
        this.text = text;
        this.kind = kind;
        this.replaceStart = replaceStart;
        this.replaceEnd = replaceEnd;
        this.target = target;
    }

    public String getText() {
        return text;
    }

    public Kind getKind() {
        return kind;
    }

    public int getReplaceStart() {
        return replaceStart;
    }

    public int getReplaceEnd() {
        return replaceEnd;
    }

    public Object getTarget() {
        return target;
    }
}
