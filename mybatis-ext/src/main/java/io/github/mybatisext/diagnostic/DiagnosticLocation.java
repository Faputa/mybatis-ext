package io.github.mybatisext.diagnostic;

import java.io.Serializable;
import java.lang.annotation.Annotation;

import io.github.mybatisext.model.JavaAnnotatedElement;
import io.github.mybatisext.model.JavaField;
import io.github.mybatisext.model.JavaMethod;
import io.github.mybatisext.model.JavaType;

/** 记录声明及其实际源码文本中的可选范围，推导出的路径单独保存。 */
public final class DiagnosticLocation implements Serializable {

    private static final long serialVersionUID = 1L;
    private final transient JavaType owner;
    private final transient JavaAnnotatedElement source;
    private final String ownerName;
    private final String sourceName;
    private final Class<? extends Annotation> annotationType;
    private final String annotationMember;
    private final String path;
    private final String text;
    private final int start;
    private final int end;

    public DiagnosticLocation(JavaType owner, JavaAnnotatedElement source, Class<? extends Annotation> annotationType, String annotationMember, String path) {
        this(owner, source, owner == null ? null : owner.getName(),
                source instanceof JavaField ? ((JavaField) source).getName() : source instanceof JavaMethod ? ((JavaMethod) source).getName() : null,
                annotationType, annotationMember, path, null, -1, -1);
    }

    private DiagnosticLocation(JavaType owner, JavaAnnotatedElement source, String ownerName, String sourceName,
            Class<? extends Annotation> annotationType, String annotationMember, String path, String text, int start, int end) {
        this.owner = owner;
        this.source = source;
        this.ownerName = ownerName;
        this.sourceName = sourceName;
        this.annotationType = annotationType;
        this.annotationMember = annotationMember;
        this.path = path;
        this.text = text;
        this.start = start;
        this.end = end;
    }

    public JavaType getOwner() {
        return owner;
    }

    public JavaAnnotatedElement getSource() {
        return source;
    }

    public Class<? extends Annotation> getAnnotationType() {
        return annotationType;
    }

    public String getAnnotationMember() {
        return annotationMember;
    }

    public String getPath() {
        return path;
    }

    public String getText() {
        return text;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public boolean hasRange() {
        return start >= 0;
    }

    /** 文本与其对应范围必须同时提供。 */
    public DiagnosticLocation inText(String text, int start, int end) {
        if (start < 0 || end < start || end > text.length()) {
            throw new IllegalArgumentException("Invalid diagnostic range: " + start + ".." + end);
        }
        return new DiagnosticLocation(owner, source, ownerName, sourceName, annotationType, annotationMember, path, text, start, end);
    }

    public DiagnosticLocation member(String member) {
        return new DiagnosticLocation(owner, source, ownerName, sourceName, annotationType, member, path, null, -1, -1);
    }

    /** 路径由推导得到而非显式书写时，sourceText 可以为空。 */
    public DiagnosticLocation forPath(String path, String sourceText) {
        if (sourceText != null && !path.endsWith(sourceText)) {
            throw new IllegalArgumentException("Source text must be a suffix of the resolved path");
        }
        return new DiagnosticLocation(owner, source, ownerName, sourceName, annotationType, annotationMember, path, sourceText, -1, -1);
    }

    /** 扣除内部添加的路径前缀，将已解析路径的范围映射回原始文本。 */
    public DiagnosticLocation pathRange(int start, int end) {
        if (text == null) {
            return this;
        }
        int prefixLength = path.length() - text.length();
        return start < prefixLength ? this : inText(text, start - prefixLength, end - prefixLength);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (ownerName != null) {
            result.append(ownerName);
        }
        if (sourceName != null) {
            result.append('#').append(sourceName);
        }
        if (path != null) {
            result.append(" path=").append(path);
        }
        if (annotationType != null) {
            result.append(" @").append(annotationType.getSimpleName());
            if (annotationMember != null) {
                result.append('.').append(annotationMember);
            }
        }
        if (hasRange()) {
            result.append(" [").append(start).append(',').append(end).append(")");
        }
        return result.toString();
    }
}
