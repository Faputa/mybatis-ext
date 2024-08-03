package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class State {

    private final @Nullable State prev;
    private final @Nullable Scope scope;
    private Object result;
    private final Map<Scope, Map<String, MatchResult>> scopeToNameToMatchResult = new HashMap<>();
    private final List<MatchResult> matchResults = new ArrayList<>();

    public State() {
        this(null, null);
    }

    public State(@Nullable State prev, @Nullable Scope scope) {
        this.prev = prev;
        this.scope = scope;
        this.result = prev != null ? prev.result : null;
    }

    public @Nullable State getPrev() {
        return prev;
    }

    public @Nullable Scope getScope() {
        return scope;
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
        for (State state = this; state != scope.getGuard() && state != null; state = state.prev) {
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
        if (prev != scope.getGuard() && prev != null) {
            return prev.getMatch(name, scope);
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
}
