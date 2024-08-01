package io.github.mybatisext.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

public class State {

    private final @Nullable State parent;
    private final @Nullable Scope scope;
    private final Tokenizer tokenizer;
    private final Map<Scope, Map<String, MatchResult>> scopeToNameToMatchResult = new HashMap<>();
    private final List<MatchResult> matchResults = new ArrayList<>();

    public State(Tokenizer tokenizer) {
        this(null, null, tokenizer);
    }

    public State(@Nullable State parent, @Nullable Scope scope, Tokenizer tokenizer) {
        this.parent = parent;
        this.scope = scope;
        this.tokenizer = tokenizer;
    }

    public @Nullable State getParent() {
        return parent;
    }

    public @Nullable Scope getScope() {
        return scope;
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    public MatchResult getMatch(Symbol symbol, Scope scope, int index) {
        List<MatchResult> foundMatchResults = new ArrayList<>();
        for (State state = this; state != null; state = state.parent) {
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

    public void addMatch(Symbol symbol, Scope scope, String text, Object value) {
        MatchResult matchResult = new MatchResult(symbol, scope, text, value);
        matchResults.add(matchResult);
    }

    public void addMatch(Symbol symbol, String text, Object value) {
        addMatch(symbol, scope, text, value);
    }

    public MatchResult getMatch(String name, Scope scope) {
        Map<String, MatchResult> nameToMatchResult = scopeToNameToMatchResult.get(scope);
        if (nameToMatchResult != null && nameToMatchResult.containsKey(name)) {
            return nameToMatchResult.get(name);
        }
        if (parent != null) {
            return parent.getMatch(name, scope);
        }
        return null;
    }

    public MatchResult getMatch(String name) {
        return getMatch(name, scope);
    }

    public void addMatch(String name, Symbol symbol, Scope scope, String text, Object value) {
        Map<String, MatchResult> nameToMatchResult = scopeToNameToMatchResult.computeIfAbsent(scope, k -> new HashMap<>());
        MatchResult matchResult = new MatchResult(symbol, scope, text, value);
        nameToMatchResult.put(name, matchResult);
    }

    public void addMatch(String name, Symbol symbol, String text, Object value) {
        addMatch(name, symbol, scope, text, value);
    }

    public void setReturn(Object value) {
        Objects.requireNonNull(this.scope).setReturnValue(value);
    }
}
