package io.github.mybatisext.jpa;

public class MathParser extends BaseParser<MathTokenizer> {

    // <end>=$
    protected Symbol end = new Symbol("end").setMatch(state -> tokenizer.next().isEmpty());
    // <digit>:="0"|"1"|"2"|"3"|"4"|"5"|"6"|"7"|"8"|"9"
    protected Symbol digit = new Symbol("digit").set(choice(
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
    // <expr>:=<term>{("+"|"-")<expr>}<end>
    protected Symbol expr = new Symbol("expr");
    // <term>:=<factor>{("*"|"/")<term>}
    protected Symbol term = new Symbol("term");
    // <factor>:=<integer>|"("<expr>")"
    protected Symbol factor = new Symbol("factor");
    // <integer>:=<digit><integer>|<digit>
    protected Symbol integer = new Symbol("integer");
    // <all>:=<expr><end>
    protected Symbol all = new Symbol("all").set(join(expr, end));

    {
        expr.set(join(term, optional(join(choice(keyword("+"), keyword("-")), expr))));
        term.set(join(factor, optional(join(choice(keyword("*"), keyword("/")), term))));
        factor.set(choice(integer, join(keyword("("), expr, keyword(")"))));
        integer.set(choice(join(digit, integer), digit));
    }

    public MathParser(MathTokenizer tokenizer) {
        super(tokenizer);
    }

    protected Symbol keyword(String s) {
        tokenizer.getKeywords().add(s);
        return new Symbol("keyword").setMatch(state -> tokenizer.next().equals(s));
    }

    public boolean perse() {
        return all.match(new State(all));
        // return expr.match(new State(expr));
        // return integer.match(new State(integer));
    }

    public static void main(String[] args) {
        // TODO 左递归文法
        // TODO 选择结构顺序
        // TODO 状态和值的传递
        MathTokenizer mathTokenizer = new MathTokenizer("1+2*34-(100+3) ");
        MathParser mathParser = new MathParser(mathTokenizer);
        System.out.println(mathParser.perse());
        System.out.println(mathTokenizer.substring(0, mathTokenizer.getCursor()));
    }
}
