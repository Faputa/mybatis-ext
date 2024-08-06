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
    private final Map<Scope, Map<String, MatchResult>> scopeToNameToMatchResult = new HashMap<>();
    private final List<MatchResult> matchResults = new ArrayList<>();
    private final Map<String, Object> nameToGlobal = new HashMap<>();

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

    public MatchResult getMatch(Symbol symbol, Scope scope, int index) {
        List<MatchResult> foundMatchResults = new ArrayList<>();
        for (State state = this; state != scope.getGuard() && state != null; state = state.prevState) {
            int i = 0;
            for (MatchResult matchResult : state.matchResults) {
                if (matchResult.getScope() == scope && matchResult.getSymbol() == symbol) {
                    foundMatchResults.add(i++, matchResult);
                }
            }
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
        Map<String, MatchResult> nameToMatchResult = scopeToNameToMatchResult.get(scope);
        if (nameToMatchResult != null) {
            MatchResult matchResult = nameToMatchResult.get(name);
            if (matchResult != null) {
                return matchResult;
            }
        }
        if (prevState != scope.getGuard() && prevState != null) {
            return prevState.getMatch(name, scope);
        }
        return null;
    }

    public MatchResult getMatch(String name) {
        return getMatch(name, scope);
    }

    public boolean addMatch(String name, Symbol symbol, Scope scope, String text, Object value) {
        Map<String, MatchResult> nameToMatchResult = scopeToNameToMatchResult.computeIfAbsent(scope, k -> new HashMap<>());
        MatchResult matchResult = new MatchResult(symbol, scope, text, value);
        nameToMatchResult.put(name, matchResult);
        return true;
    }

    public boolean addMatch(String name, Symbol symbol, String text, Object value) {
        return addMatch(name, symbol, scope, text, value);
    }

    public boolean setReturn(Object value) {
        if (this.scope != null) {
            this.scope.setReturnValue(value);
        }
        return true;
    }

    public <T> T getGlobal(String name) {
        @SuppressWarnings("unchecked")
        T t = (T) nameToGlobal.get(name);
        if (t != null) {
            return t;
        }
        if (prevState != null) {
            return prevState.getGlobal(name);
        }
        return null;
    }

    public <T> boolean setGlobal(String name, T value) {
        nameToGlobal.put(name, value);
        return true;
    }
}
