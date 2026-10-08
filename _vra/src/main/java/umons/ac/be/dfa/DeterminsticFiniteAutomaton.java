package umons.ac.be.dfa;

public class DeterminsticFiniteAutomaton<T extends State<T>> {
    private T initialState;

    public DeterminsticFiniteAutomaton() {};
    public DeterminsticFiniteAutomaton(T initialState) {
        this.initialState = initialState;
    }

    public void setInitialState(T state) {
        initialState = state;
    }

    public void addTransition(T from, String symbol, T to) {
        from.addTransition(symbol, to);
    }

    public boolean accepts(Word input, boolean DEBUG) {
        T currentState = initialState;
        if (DEBUG) {
            System.out.print(currentState);
        }

        for (String symbol : input.getSymbols()) {
            currentState = currentState.getTransitions(symbol);

            if (DEBUG) {
                System.out.print(" - " + symbol + " -> " + currentState);
            }
            if (currentState == null)
                return false;
        }

        if (DEBUG) {
            System.out.println();
        }
        return currentState.isFinal();
    }

    public T getInitalState() {
        return this.initialState;
    }
}
