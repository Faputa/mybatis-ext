package io.github.mybatisext.jpa;

public class MathParser extends BaseParser<MathTokenizer> {

    public MathParser(MathTokenizer tokenizer) {
        super(tokenizer);
    }

    protected Symbol keyword(String s) {
        tokenizer.getKeywords().add(s);
        return state -> tokenizer.next().equals(s);
    }

    public boolean perse() {
        Nonterminal expr = new Nonterminal();
        Nonterminal term = new Nonterminal();
        Nonterminal factor = new Nonterminal();
        Nonterminal integer = new Nonterminal();
        Nonterminal digit = new Nonterminal();

        expr.set(choice(
                term,
                join(expr, keyword("+"), term),
                join(expr, keyword("-"), term)));

        term.set(choice(
                factor,
                join(term, keyword("*"), factor),
                join(expr, keyword("/"), factor)));

        factor.set(choice(
                integer,
                join(keyword("("), expr, keyword(")"))));

        integer.set(choice(
                digit,
                join(digit, integer)));

        digit.set(choice(
                keyword("0"),
                keyword("1"),
                keyword("2"),
                keyword("3"),
                keyword("4"),
                keyword("5"),
                keyword("6"),
                keyword("7"),
                keyword("8"),
                keyword("9")));

        return expr.match(new State(expr));
    }

    public static void main(String[] args) {
        MathTokenizer mathTokenizer = new MathTokenizer("(1+1*1");
        MathParser mathParser = new MathParser(mathTokenizer);
        System.out.println(mathParser.perse());
    }
}
