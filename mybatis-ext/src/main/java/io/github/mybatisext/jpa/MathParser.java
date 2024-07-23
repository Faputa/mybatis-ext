package io.github.mybatisext.jpa;

public class MathParser extends BaseParser<MathTokenizer> {

    // <end>=$
    protected Nonterminal end = new Nonterminal("end").set(state -> tokenizer.next().isEmpty());
    // <digit>:="0"|"1"|"2"|"3"|"4"|"5"|"6"|"7"|"8"|"9"
    protected Nonterminal digit = new Nonterminal("digit").set(choice(
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
    protected Nonterminal expr = new Nonterminal("expr");
    // <term>:=<factor>{("*"|"/")<term>}
    protected Nonterminal term = new Nonterminal("term");
    // <factor>:=<integer>|"("<expr>")"
    protected Nonterminal factor = new Nonterminal("factor");
    // <integer>:=<digit><integer>|<digit>
    protected Nonterminal integer = new Nonterminal("integer");
    // <all>:=<expr><end>
    protected Nonterminal all = new Nonterminal("all").set(join(expr, end));

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
        return state -> tokenizer.next().equals(s);
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
