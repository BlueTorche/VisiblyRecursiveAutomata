package umons.ac.be.dfa;
import java.util.*;

public class State<T extends State<T>> {
    private final String name;
    private final boolean isFinal;
    private final Map<String, T> transitions = new HashMap<>();

    public State(String name, boolean isFinal) {
        this.name = name;
        this.isFinal = isFinal;
    }

    public String getName() {
        return name;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void addTransition(String symbol, T target) {
        transitions.put(symbol, target);
    }

    public T getTransitions(String symbol) {
        return transitions.getOrDefault(symbol, null);
    }

    public Set<String> getTransitionsSymbols() { return transitions.keySet(); }

    public Map<String, T> getAllTransitions() {
        return transitions;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof State)) return false;
        T other = (T) obj;
        return name.equals(other.getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name + (isFinal ? " (final)" : "");
    }
}
