package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class State<T extends Tokenizer> {

    private final State<T> prevState;
    private final Scope<T> scope;
    private final T tokenizer;
    private Object result;
    private Object returnValue;
    private final Map<Scope<T>, Map<String, MatchResult>> scopeToNameToMatchResult = new HashMap<>();
    private final List<MatchResult> matchResults = new ArrayList<>();

    public State(T tokenizer) {
        this.prevState = null;
        this.scope = new Scope<>(null, null);
        this.tokenizer = tokenizer;
    }

    public State(State<T> prevState, Scope<T> scope) {
        this.prevState = prevState;
        this.scope = scope;
        this.tokenizer = prevState.tokenizer;
        this.result = prevState.result;
    }

    public State<T> getPrevState() {
        return prevState;
    }

    public Scope<T> getScope() {
        return scope;
    }

    public T getTokenizer() {
        return tokenizer;
    }

    public Object getResult() {
        return result;
    }

    public boolean setResult(Object result) {
        this.result = result;
        return true;
    }

    public Object getReturn() {
        for (State<T> state = this; state != scope.getOutside() && state != null; state = state.prevState) {
            if (state.getScope() == scope && state.returnValue != null) {
                return state.returnValue;
            }
        }
        return null;
    }

    public boolean setReturn(Object value) {
        this.returnValue = value;
        return true;
    }

    public List<MatchResult> getMatches(Symbol<T> symbol) {
        List<MatchResult> matches = new ArrayList<>();
        for (State<T> state = this; state != scope.getOutside() && state != null; state = state.prevState) {
            int i = 0;
            for (MatchResult matchResult : state.matchResults) {
                if (matchResult.getScope() == scope && matchResult.getSymbol() == symbol) {
                    matches.add(i++, matchResult);
                }
            }
        }
        return matches;
    }

    public MatchResult getMatch(Symbol<T> symbol) {
        List<MatchResult> matches = getMatches(symbol);
        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(0);
    }

    public boolean addMatch(Symbol<T> symbol, Scope<T> scope, String text, Object value) {
        MatchResult matchResult = new MatchResult(symbol, scope, text, value);
        return matchResults.add(matchResult);
    }

    public MatchResult getMatch(String name) {
        for (State<T> state = this; state != scope.getOutside() && state != null; state = state.prevState) {
            Map<String, MatchResult> nameToMatchResult = state.scopeToNameToMatchResult.get(scope);
            if (nameToMatchResult != null) {
                MatchResult matchResult = nameToMatchResult.get(name);
                if (matchResult != null) {
                    return matchResult;
                }
            }
        }
        return null;
    }

    public boolean addMatch(String name, Symbol<T> symbol, Scope<T> scope, String text, Object value) {
        Map<String, MatchResult> nameToMatchResult = scopeToNameToMatchResult.computeIfAbsent(scope, k -> new HashMap<>());
        MatchResult matchResult = new MatchResult(symbol, scope, text, value);
        nameToMatchResult.put(name, matchResult);
        return true;
    }

}
