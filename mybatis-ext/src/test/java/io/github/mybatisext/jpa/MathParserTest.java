package io.github.mybatisext.jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MathParserTest extends BaseParser<MathTokenizer> {

    Symbol end = new Symbol("end").set((state, continuation) -> {
        MathTokenizer mathTokenizer = state.getTokenizer();
        return mathTokenizer.next().isEmpty() && continuation.test(state);
    });

    Symbol digit = new Symbol("digit").set(choice(keyword("0"), keyword("1"), keyword("2"), keyword("3"), keyword("4"), keyword("5"), keyword("6"), keyword("7"), keyword("8"), keyword("9")));

    Symbol keyword(String s) {
        return new Symbol("keyword(" + s + ")").set((state, continuation) -> {
            MathTokenizer mathTokenizer = state.getTokenizer();
            return mathTokenizer.next().equals(s) && state.setResult(s) && continuation.test(state);
        });
    }

    @Test
    public void parsesExpressionWithOptionalRecursiveOperators() {
        Symbol expr = new Symbol("expr");
        Symbol term = new Symbol("term");
        Symbol factor = new Symbol("factor");
        Symbol integer = new Symbol("integer");
        Symbol all = new Symbol("all").set(join(expr, end));

        expr.set(join(term, optional(choice(join(keyword("+"), expr, action(state -> {
            int a = state.getMatch(term).val();
            int b = state.getMatch(expr).val();
            return state.setReturn(a + b);
        })), join(keyword("-"), expr, action(state -> {
            int a = state.getMatch(term).val();
            int b = state.getMatch(expr).val();
            state.setReturn(a - b);
        }))))));

        term.set(join(factor, optional(choice(join(keyword("*"), term, action(state -> {
            int a = state.getMatch(factor).val();
            int b = state.getMatch(term).val();
            state.setReturn(a * b);
        })), join(keyword("/"), term, action(state -> {
            int a = state.getMatch(factor).val();
            int b = state.getMatch(term).val();
            state.setReturn(a / b);
        }))))));

        factor.set(choice(integer, join(keyword("("), expr, keyword(")"), action(state -> {
            state.setReturn(state.getMatch(expr).val());
        }))));

        integer.set(join(assign("temp", join(plus(digit))), action(state -> {
            String temp = state.getMatch("temp").text();
            state.setReturn(Integer.parseInt(temp));
        })));

        int[] result = new int[1];
        boolean match = all.match(new MathTokenizer("1+2*34-(100+3) "), state -> {
            result[0] = (Integer) state.getResult();
            return true;
        });
        assertTrue(match);
        assertEquals(-34, result[0]);
    }

    @Test
    public void parsesExpressionWithChoiceBasedRecursion() {
        Symbol expr = new Symbol("expr");
        Symbol term = new Symbol("term");
        Symbol factor = new Symbol("factor");
        Symbol integer = new Symbol("integer");
        Symbol all = new Symbol("all").set(join(expr, end));

        expr.set(choice(term, join(term, keyword("+"), expr, action(state -> {
            int a = state.getMatch(term).val();
            int b = state.getMatch(expr).val();
            state.setReturn(a + b);
        })), join(term, keyword("-"), expr, action(state -> {
            int a = state.getMatch(term).val();
            int b = state.getMatch(expr).val();
            state.setReturn(a - b);
        }))));

        term.set(choice(factor, join(factor, keyword("*"), term, action(state -> {
            int a = state.getMatch(factor).val();
            int b = state.getMatch(term).val();
            state.setReturn(a * b);
        })), join(factor, keyword("/"), term, action(state -> {
            int a = state.getMatch(factor).val();
            int b = state.getMatch(term).val();
            state.setReturn(a / b);
        }))));

        factor.set(choice(integer, join(keyword("("), expr, keyword(")"), action(state -> {
            state.setReturn(state.getMatch(expr).val());
        }))));

        integer.set(join(assign("temp", join(choice(digit, join(digit, integer)))), action(state -> {
            String temp = state.getMatch("temp").text();
            state.setReturn(Integer.parseInt(temp));
        })));

        int[] result = new int[1];
        boolean match = all.match(new MathTokenizer("1+2*34-(100+3) "), state -> {
            result[0] = (Integer) state.getResult();
            return true;
        });
        assertTrue(match);
        assertEquals(-34, result[0]);
    }

    @Test
    public void matchesZeroOrMoreSymbols() {
        Symbol integer = new Symbol("integer").set(join(star(keyword("1")), star(keyword("2"))));
        boolean match = integer.match(new MathTokenizer("1111 "), state -> true);
        assertTrue(match);
    }

    @Test
    public void matchesOneOrMoreSymbols() {
        Symbol integer = new Symbol("integer").set(plus(digit));
        boolean match = integer.match(new MathTokenizer("1122 "), state -> true);
        assertTrue(match);
    }

    @Test
    public void matchesExactSymbolCounts() {
        Symbol integer = new Symbol("integer").set(join(count(keyword("1"), 2), count(keyword("2"), 2)));
        boolean match = integer.match(new MathTokenizer("1122 "), state -> true);
        assertTrue(match);
    }

    @Test
    public void propagatesNestedAssignments() {
        Symbol x = new Symbol("X").set(assign("a", assign("b", assign("c", assign("d", assign("e", keyword("x")))))));
        boolean match = x.match(new MathTokenizer("x"), state -> {
            assertEquals("x", state.getMatch("a").val());
            assertEquals("x", state.getMatch("b").val());
            assertEquals("x", state.getMatch("c").val());
            assertEquals("x", state.getMatch("d").val());
            assertEquals("x", state.getMatch("e").val());
            return true;
        });
        assertTrue(match);
    }

}
