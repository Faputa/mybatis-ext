package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public class State {

    private final State prevState;
    private final Scope scope;
    private final Tokenizer tokenizer;
    private Object result;
    private Object returnValue;
    private final Map<Scope, Map<String, MatchResult>> scopeToNameToMatchResult = new HashMap<>();
    private final List<MatchResult> matchResults = new ArrayList<>();

    public State(Tokenizer tokenizer) {
        this.prevState = null;
        this.scope = null;
        this.tokenizer = tokenizer;
    }

    public State(@Nonnull State prevState, Scope scope) {
        this.prevState = prevState;
        this.scope = scope;
        this.tokenizer = prevState.tokenizer;
        this.result = prevState.result;
    }

    public State getPrevState() {
        return prevState;
    }

    public Scope getScope() {
        return scope;
    }

    @SuppressWarnings("unchecked")
    public <T extends Tokenizer> T getTokenizer() {
        return (T) tokenizer;
    }

    public Object getResult() {
        return result;
    }

    public boolean setResult(Object result) {
        this.result = result;
        return true;
    }

    public Object getReturn() {
        assert scope != null;
        for (State state = this; state != scope.getOutside() && state != null; state = state.prevState) {
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

    public MatchResult getMatch(Symbol symbol, Scope scope, int index) {
        List<MatchResult> foundMatchResults = new ArrayList<>();
        for (State state = this; state != scope.getOutside() && state != null; state = state.prevState) {
            int i = 0;
            for (MatchResult matchResult : state.matchResults) {
                if (matchResult.getScope() == scope && matchResult.getSymbol() == symbol) {
                    foundMatchResults.add(i++, matchResult);
                }
            }
        }
        if (index < 0) {
            index += foundMatchResults.size();
        }
        if (foundMatchResults.size() > index) {
            return foundMatchResults.get(index);
        }
        return null;
    }

    public MatchResult getMatch(Symbol symbol, int index) {
        assert scope != null;
        return getMatch(symbol, scope, index);
    }

    public MatchResult getMatch(Symbol symbol) {
        return getMatch(symbol, 0);
    }

    public boolean addMatch(Symbol symbol, Scope scope, String text, Object value) {
        MatchResult matchResult = new MatchResult(symbol, scope, text, value);
        return matchResults.add(matchResult);
    }

    public boolean addMatch(Symbol symbol, String text, Object value) {
        return addMatch(symbol, scope, text, value);
    }

    public MatchResult getMatch(String name, Scope scope) {
        for (State state = this; state != scope.getOutside() && state != null; state = state.prevState) {
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

    public MatchResult getMatch(String name) {
        assert scope != null;
        return getMatch(name, scope);
    }

    public boolean addMatch(String name, Symbol symbol, Scope scope, String text, Object value) {
        Map<String, MatchResult> nameToMatchResult = scopeToNameToMatchResult.computeIfAbsent(scope, k -> new HashMap<>());
        MatchResult matchResult = new MatchResult(symbol, scope, text, value);
        nameToMatchResult.put(name, matchResult);
        return true;
    }

    public boolean addMatch(String name, Symbol symbol, String text, Object value) {
        assert scope != null;
        return addMatch(name, symbol, scope, text, value);
    }
}
