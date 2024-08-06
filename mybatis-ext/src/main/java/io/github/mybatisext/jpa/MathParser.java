package io.github.mybatisext.jpa;

import java.util.HashSet;
import java.util.Set;

public class MathParser extends BaseParser {

    private final Set<String> keywords = new HashSet<>();

    Symbol end = new Symbol("end").setMatch((state, continuation) -> {
        MathTokenizer mathTokenizer = state.getTokenizer();
        return mathTokenizer.next().isEmpty() && continuation.test(state);
    });

    Symbol digit = new Symbol("digit").set(choice(keyword("0"), keyword("1"), keyword("2"), keyword("3"), keyword("4"), keyword("5"), keyword("6"), keyword("7"), keyword("8"), keyword("9")));

    Symbol keyword(String s) {
        keywords.add(s);
        return new Symbol("keyword(" + s + ")").setMatch((state, continuation) -> {
            MathTokenizer mathTokenizer = state.getTokenizer();
            return mathTokenizer.next().equals(s) && state.setResult(s) && continuation.test(state);
        });
    }

    public boolean parse(MathTokenizer tokenizer) {
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

        return all.match(tokenizer, state -> {
            System.out.println(state.getResult());
            return true;
        });
    }

    public boolean parse2(MathTokenizer tokenizer) {
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

        return all.match(tokenizer, state -> {
            System.out.println(state.getResult());
            return true;
        });
    }

    public boolean parseStar(MathTokenizer tokenizer) {
        Symbol integer = new Symbol("integer").set(join(star(keyword("1")), star(keyword("2"))));
        return integer.match(tokenizer, state -> {
            System.out.println(state.getResult());
            return true;
        });
    }

    public boolean parsePlus(MathTokenizer tokenizer) {
        Symbol integer = new Symbol("integer").set(plus(digit));
        return integer.match(tokenizer, state -> {
            System.out.println(state.getResult());
            return true;
        });
    }

    public boolean parseCount(MathTokenizer tokenizer) {
        Symbol integer = new Symbol("integer").set(join(count(keyword("1"), 2), count(keyword("2"), 2)));
        return integer.match(tokenizer, state -> {
            System.out.println(state.getResult());
            return true;
        });
    }

    public static void main(String[] args) {
        MathParser mathParser = new MathParser();
        System.out.println(mathParser.parse(new MathTokenizer("1+2*34-(100+3) ")));
        System.out.println(mathParser.parse2(new MathTokenizer("1+2*34-(100+3) ")));
        System.out.println(mathParser.parseStar(new MathTokenizer("1111 ")));
        System.out.println(mathParser.parsePlus(new MathTokenizer("1122 ")));
        System.out.println(mathParser.parseCount(new MathTokenizer("1122 ")));
    }
}
