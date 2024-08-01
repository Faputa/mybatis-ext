package io.github.mybatisext.jpa;

public class MathParser extends BaseParser<MathTokenizer> {

    protected Symbol end = new Symbol("end").setMatch((state, continuation) -> {
        String next = tokenizer.next();
        return next.isEmpty() && continuation.test(state, next);
    });

    protected Symbol digit = new Symbol("digit").set(choice(keyword("0"), keyword("1"), keyword("2"), keyword("3"), keyword("4"), keyword("5"), keyword("6"), keyword("7"), keyword("8"), keyword("9")));

    public MathParser(MathTokenizer tokenizer) {
        super(tokenizer);
    }

    protected Symbol keyword(String s) {
        tokenizer.getKeywords().add(s);
        return new Symbol("keyword(" + s + ")").setMatch((state, continuation) -> {
            String next = tokenizer.next();
            if (!next.equals(s)) {
                // System.out.println(tokenizer.getCursor() + "，期望：" + s);
                return false;
            }
            return continuation.test(state, next);
        });
    }

    public boolean parse() {
        Symbol expr = new Symbol("expr");
        Symbol term = new Symbol("term");
        Symbol factor = new Symbol("factor");
        Symbol integer = new Symbol("integer");
        Symbol all = new Symbol("all").set(join(expr, end));
        expr.set(join(term, optional(choice(
                join(keyword("+"), expr, action(state -> {
                    int a = state.getMatch(term).val();
                    int b = state.getMatch(expr).val();
                    state.setReturn(a + b);
                })),
                join(keyword("-"), expr, action(state -> {
                    int a = state.getMatch(term).val();
                    int b = state.getMatch(expr).val();
                    state.setReturn(a - b);
                }))))));

        term.set(join(factor, optional(choice(
                join(keyword("*"), term, action(state -> {
                    int a = state.getMatch(factor).val();
                    int b = state.getMatch(term).val();
                    state.setReturn(a * b);
                })),
                join(keyword("/"), term, action(state -> {
                    int a = state.getMatch(factor).val();
                    int b = state.getMatch(term).val();
                    state.setReturn(a / b);
                }))))));

        factor.set(choice(integer, join(keyword("("), expr, keyword(")"), action(state -> {
            state.setReturn(state.getMatch(expr).val());
        }))));

        integer.set(join(assign("temp", join(choice(digit, join(digit, integer)))), action(state -> {
            String temp = state.getMatch("temp").text();
            state.setReturn(Integer.parseInt(temp));
        })));

        return all.match(new State(), (state, result) -> {
            System.out.println(result);
            return true;
        });
        // return expr.match(new State(), (state, result) -> true);
        // return integer.match(new State(), (state, result) -> true);
    }

    public boolean parse2() {
        Symbol expr = new Symbol("expr");
        Symbol term = new Symbol("term");
        Symbol factor = new Symbol("factor");
        Symbol integer = new Symbol("integer");
        Symbol all = new Symbol("all").set(join(expr, end));
        expr.set(choice(term, join(term, keyword("+"), expr), join(term, keyword("-"), expr)));
        term.set(choice(factor, join(factor, keyword("*"), term), join(factor, keyword("/"), term)));
        factor.set(choice(integer, join(keyword("("), expr, keyword(")"))));
        integer.set(choice(digit, join(digit, integer)));
        return all.match(new State(), (state, result) -> {
            System.out.println(result);
            return true;
        });
        // return expr.match(new State(), (state, result) -> true);
        // return integer.match(new State(), (state, result) -> true);
    }

    public static void main(String[] args) {
        {
            MathTokenizer mathTokenizer = new MathTokenizer("1+2*34-(100+3) ");
            MathParser mathParser = new MathParser(mathTokenizer);
            System.out.println(mathParser.parse());
            System.out.println(mathTokenizer.substring(0, mathTokenizer.getCursor()));
        }
        {
            MathTokenizer mathTokenizer = new MathTokenizer("1+2*34-(100+3) ");
            MathParser mathParser = new MathParser(mathTokenizer);
            System.out.println(mathParser.parse2());
            System.out.println(mathTokenizer.substring(0, mathTokenizer.getCursor()));
        }
    }
}
