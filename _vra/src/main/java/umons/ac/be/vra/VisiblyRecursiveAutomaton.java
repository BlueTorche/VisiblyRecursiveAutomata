package umons.ac.be.vra;

import java.util.*;

public class VisiblyRecursiveAutomaton {
    protected final Set<ProceduralAutomaton> proceduralAutomata = new HashSet<>();
    protected VRAAlphabet alphabet;
    protected ProceduralAutomaton startingAutomaton;

    public VisiblyRecursiveAutomaton(VRAAlphabet alphabet) {
        this.alphabet = alphabet;
    }

    public void setVSPAAlphabet(VRAAlphabet alphabet) {
        this.alphabet = alphabet;
    }

    public void addProceduralAutomaton(ProceduralAutomaton dfa, String callSymbol, String returnSymbol) {
        proceduralAutomata.add(dfa);
        alphabet.addProceduralSymbol(dfa.getProceduralSymbol(), callSymbol, returnSymbol);
    }

    public void setStartingAutomaton(ProceduralAutomaton nfa) {
        startingAutomaton = nfa;
    }


    public boolean accepts(String word) {
        Set<VRAState> currentStates = Set.of(startingAutomaton.getInitalState());
        List<Set<VRAState>> stack = new ArrayList<>();

        for (int i = 0; i < word.length() ; i++) {
            String symbol = String.valueOf(word.charAt(i));
            Set<VRAState> nextStates = new HashSet<>();
            if (alphabet.getInternalSymbols().contains(symbol)) {
                for (VRAState state : currentStates) {
                    final VRAState next_state = state.getTransitions(symbol);
                    if (next_state == null)
                        continue;
                    nextStates.add(next_state);
                }
            } else if (alphabet.getCallSymbols().contains(symbol)) {
                stack.add(currentStates);
                for (VRAState state : currentStates) {
                    for (ProceduralAutomaton pa : proceduralAutomata) {
                        String procSymbol = pa.getProceduralSymbol();
                        if (alphabet.getCallFromProcedural(pa.getProceduralSymbol()).equals(symbol) &&
                                state.getTransitions(procSymbol) != null) {
                            nextStates.add(pa.getInitalState());
                        }
                    }
                }
            } else if (alphabet.getReturnSymbols().contains(symbol)) {
                if (stack.isEmpty()) return false;
                Set<VRAState> calledStates = stack.removeLast();
                for (VRAState state : currentStates) {
                    final String proceduralSymbol = state.getProceduralAutomaton().getProceduralSymbol();
                    if (state.isFinal() &&
                            alphabet.getReturnFromProcedural(proceduralSymbol).equals(symbol)) {
                        for (VRAState state2 : calledStates) {
                            final VRAState next_state = state2.getTransitions(proceduralSymbol);
                            if (next_state == null)
                                continue;
                            nextStates.add(next_state);
                        }
                    }
                }
            }
            currentStates = nextStates;
        }

        if (stack.isEmpty()) {
            for (VRAState state : currentStates) {
                if (state.isFinal())
                    return true;
            }
        }
        return false;
    }
}

