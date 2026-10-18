package io.github.mybatisext.analysis;

import java.util.ArrayList;
import java.util.List;

/** 单次请求内的补全状态，各语法分支独立收集候选项。 */
public final class CompletionCollector {

    private final String text;
    private final int offset;
    private final List<CompletionItem> items = new ArrayList<>();

    public CompletionCollector(String text, int offset) {
        if (offset < 0 || offset > text.length()) {
            throw new IllegalArgumentException("Invalid completion offset: " + offset);
        }
        this.text = text;
        this.offset = offset;
    }

    public void offer(int start, String token, CompletionItem.Kind kind, Object target) {
        if (start > offset) {
            return;
        }
        String prefix = text.substring(start, offset);
        if (!token.startsWith(prefix) || token.equals(prefix)) {
            return;
        }
        int end = offset;
        if (text.startsWith(token, start)) {
            end = start + token.length();
        } else {
            while (end < text.length() && Character.isLowerCase(text.charAt(end))) {
                end++;
            }
        }
        for (CompletionItem item : items) {
            if (item.getReplaceStart() == start && item.getText().equals(token) && item.getTarget() == target) {
                return;
            }
        }
        items.add(new CompletionItem(token, kind, start, end, target));
    }

    public List<CompletionItem> getItems() {
        return new ArrayList<>(items);
    }
}
