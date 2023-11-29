package io.github.mybatisext;

public class ExtContext {

    /** 是否启用级联 */
    private boolean cascadeEnabled = false;

    public boolean isCascadeEnabled() {
        return cascadeEnabled;
    }

    public void setCascadeEnabled(boolean cascadeEnabled) {
        this.cascadeEnabled = cascadeEnabled;
    }
}
