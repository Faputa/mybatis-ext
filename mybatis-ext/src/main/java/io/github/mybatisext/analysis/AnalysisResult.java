package io.github.mybatisext.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.mybatisext.diagnostic.Diagnostic;

public final class AnalysisResult<T> {

    private final T value;
    private final List<Diagnostic> diagnostics;
    private final List<SymbolReference> references;

    public AnalysisResult(T value, List<Diagnostic> diagnostics, List<SymbolReference> references) {
        this.value = value;
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
        this.references = Collections.unmodifiableList(new ArrayList<>(references));
    }

    public T getValue() {
        return value;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public List<SymbolReference> getReferences() {
        return references;
    }

    public boolean isSuccess() {
        return value != null && diagnostics.isEmpty();
    }
}
