package io.github.mybatisext.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class CpsParserFrameworkTest extends BaseParser<MathTokenizer> {

    @Test
    void choiceBacktracksWhenContinuationRejectsFirstMatch() {
        Symbol first = token("a", "first");
        Symbol second = token("a", "second");
        MathTokenizer tokenizer = new MathTokenizer("a");

        boolean matched = choice(first, second).match(tokenizer, state -> "second".equals(state.getResult()));

        assertTrue(matched);
        assertEquals(1, tokenizer.getCursor());
    }

    @Test
    void optionalRestoresCursorAfterAConsumingFailure() {
        Symbol consumeThenFail = new Symbol("consumeThenFail").setMatch((state, continuation) -> {
            state.getTokenizer().next();
            return false;
        });
        MathTokenizer tokenizer = new MathTokenizer("a");

        boolean matched = optional(consumeThenFail).match(tokenizer, state -> true);

        assertTrue(matched);
        assertEquals(0, tokenizer.getCursor());
    }

    @Test
    void zeroWidthRepetitionStopsAndInvokesContinuationOnce() {
        Symbol zeroWidth = action((Predicate<State<MathTokenizer>>) state -> true);
        AtomicInteger continuationCalls = new AtomicInteger();

        boolean matched = star(zeroWidth).match(new MathTokenizer(""), state -> continuationCalls.incrementAndGet() == 1);

        assertTrue(matched);
        assertEquals(1, continuationCalls.get());
    }

    @Test
    void rejectsDirectAndIndirectLeftRecursion() {
        Symbol direct = new Symbol("direct");
        ParserException directException = assertThrows(ParserException.class, () -> direct.set(direct));
        assertTrue(directException.getMessage().contains("Left recursion detected in direct"));

        Symbol first = new Symbol("first");
        Symbol second = new Symbol("second");
        first.set(second);
        ParserException indirectException = assertThrows(ParserException.class, () -> second.set(first));
        assertTrue(indirectException.getMessage().contains("Left recursion detected in second"));
    }

    @Test
    void propagatesScopedReturnValuesAndMatchText() {
        Symbol inner = new Symbol("inner").set(join(token("a", "token"), action((Predicate<State<MathTokenizer>>) state -> state.setReturn("returned"))));
        Symbol outer = new Symbol("outer").set(inner);

        boolean matched = outer.match(new MathTokenizer("a"), state -> {
            assertEquals("returned", state.getResult());
            return true;
        });

        assertTrue(matched);
    }

    @Test
    void emptyJoinAndZeroCountMatchWithoutConsumingInput() {
        MathTokenizer tokenizer = new MathTokenizer("a");

        assertTrue(join().match(tokenizer, state -> true));
        assertTrue(count(token("a", "value"), 0).match(tokenizer, state -> true));
        assertEquals(0, tokenizer.getCursor());
    }

    private Symbol token(String expected, String value) {
        return new Symbol("token(" + expected + ")").set((state, continuation) -> {
            if (!state.getTokenizer().next().equals(expected)) {
                return false;
            }
            return state.setResult(value) && continuation.test(state);
        });
    }
}
