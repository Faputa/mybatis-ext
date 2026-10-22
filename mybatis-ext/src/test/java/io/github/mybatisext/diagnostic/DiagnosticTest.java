package io.github.mybatisext.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.github.mybatisext.model.UnresolvedTypeException;

class DiagnosticTest {

    @Test
    void rangesCannotBeCreatedOutsideTheirSourceText() {
        DiagnosticLocation source = new DiagnosticLocation(null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> source.inText("name", -1, 2));
        assertThrows(IllegalArgumentException.class, () -> source.inText("name", 3, 2));
        assertThrows(IllegalArgumentException.class, () -> source.inText("name", 0, 5));
        assertFalse(source.hasRange());
        DiagnosticLocation located = source.inText("name", 1, 4);
        assertEquals("ame", located.getText().substring(located.getStart(), located.getEnd()));
        assertFalse(source.hasRange());
    }

    @Test
    void errorsInsideGeneratedPrefixesHaveNoInventedSourceRange() {
        DiagnosticLocation source = new DiagnosticLocation(null, null, null, null, null)
                .forPath("embedded.id", "id");
        assertFalse(source.pathRange(0, 8).hasRange());
        DiagnosticLocation property = source.pathRange(9, 11);
        assertEquals(0, property.getStart());
        assertEquals(2, property.getEnd());
        assertThrows(IllegalArgumentException.class, () -> source.forPath("embedded.id", "name"));
    }

    @Test
    void unresolvedTypeContextDoesNotMutateAnExistingException() {
        UnresolvedTypeException original = new UnresolvedTypeException("missing.Type");
        DiagnosticLocation source = new DiagnosticLocation(null, null, null, null, "value");
        UnresolvedTypeException located = original.at(source);
        assertNotSame(original, located);
        assertNull(original.getDiagnostic().getLocation());
        assertSame(source, located.getDiagnostic().getLocation());
        assertSame(original, located.getCause());
        assertSame(located, located.at(new DiagnosticLocation(null, null, null, null, "outer")));
    }
}
