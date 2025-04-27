package io.github.mybatisext.jpa;

import org.junit.jupiter.api.Test;

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
    public void parse() {
        Symbol expr = new Symbol("expr");
        Symbol term = new Symbol("term");
        Symbol factor = new Symbol("factor");
        Symbol integer = new Symbol("integer");
        Symbol all = new Symbol("all").set(join(expr, end));

        expr.set(join(term, optional(choice(join(keyword("+"), expr, action(state -> {
            int a = state.getMatch(term).val();
            int b = state.getMatch(expr).val();
            state.setReturn(a + b);
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

        boolean match = all.match(new MathTokenizer("1+2*34-(100+3) "), state -> {
            System.out.println(state.getResult());
            return true;
        });
        System.out.println(match);
    }

    @Test
    public void parse2() {
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

        boolean match = all.match(new MathTokenizer("1+2*34-(100+3) "), state -> {
            System.out.println(state.getResult());
            return true;
        });
        System.out.println(match);
    }

    @Test
    public void parseStar() {
        Symbol integer = new Symbol("integer").set(join(star(keyword("1")), star(keyword("2"))));
        boolean match = integer.match(new MathTokenizer("1111 "), state -> {
            System.out.println(state.getResult());
            return true;
        });
        System.out.println(match);
    }

    @Test
    public void parsePlus() {
        Symbol integer = new Symbol("integer").set(plus(digit));
        boolean match = integer.match(new MathTokenizer("1122 "), state -> {
            System.out.println(state.getResult());
            return true;
        });
        System.out.println(match);
    }

    @Test
    public void parseCount() {
        Symbol integer = new Symbol("integer").set(join(count(keyword("1"), 2), count(keyword("2"), 2)));
        boolean match = integer.match(new MathTokenizer("1122 "), state -> {
            System.out.println(state.getResult());
            return true;
        });
        System.out.println(match);
    }
}
