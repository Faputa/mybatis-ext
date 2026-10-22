package io.github.mybatisext.analysis;

/** 目标为 PropertyInfo、Variable 或实际绑定的 Java 声明；源码适配器据此定位源码元素。 */
public final class SymbolReference {

    private final int start;
    private final int end;
    private final Object target;

    public SymbolReference(int start, int end, Object target) {
        this.start = start;
        this.end = end;
        this.target = target;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public Object getTarget() {
        return target;
    }
}
