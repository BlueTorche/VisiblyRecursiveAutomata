package umons.ac.be.vra;

import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.*;

public class AbstractVRAwithDFA<S, I> extends AbstractVRA<S, I, DFA<S, I>> {

    public AbstractVRAwithDFA(VRAlphabet<I> vrAlphabet, Map<I, DFA<S, I>> procedures, DFA<S, I> startingProcedure) {
        super(vrAlphabet, procedures, startingProcedure);
    }

    @Override
    public VRAState<S, I, DFA<S, I>> getTransition(VRAState<S, I, DFA<S, I>> currentState, I symbolInput) {
        HashMap<DFA<S, I>, Set<S>> nextProceduresStates = new HashMap<>();
        switch (getInputAlphabet().getSymbolType(symbolInput)) {
            case VPAlphabet.SymbolType.INTERNAL -> {
                for(DFA<S, I> procedure: currentState.getCurrentProcedures()) {
                    Set<S> nextStates = new HashSet<>();
                    for(S state : currentState.getCurrentStatesFromProcedure(procedure)) {
                        S next = procedure.getTransition(state, symbolInput);
                        if (next != null) {
                            nextStates.add(next);
                        }
                    }
                    if (!nextStates.isEmpty()) {
                        nextProceduresStates.put(procedure, nextStates);
                    }
                }
                return currentState.updateProcedureStates(nextProceduresStates);
            }
            case VPAlphabet.SymbolType.CALL -> {
                Set<I> nextProcedures = new HashSet<>();
                for (I proceduralSymbol : getInputAlphabet().getProceduralAlphabetFromCall(symbolInput)) {
                    for(DFA<S, I> procedure: currentState.getCurrentProcedures()) {
                        for(S state : currentState.getCurrentStatesFromProcedure(procedure)) {
                            if (procedure.getTransition(state, proceduralSymbol) != null) {
                                nextProcedures.add(proceduralSymbol);
                                break;
                            }
                        }
                        if (nextProcedures.contains(proceduralSymbol)) {
                            break;
                        }
                    }
                }
                Map<DFA<S, I>, Set<S>> nextStates = new HashMap<>();
                for (I proceduralSymbol : nextProcedures) {
                    DFA<S, I> procedure = getProcedures().get(proceduralSymbol);
                    nextStates.put(procedure, Set.of(Objects.requireNonNull(procedure.getInitialState())));
                }
                return currentState.push(nextStates);
            }
            case VPAlphabet.SymbolType.RETURN -> {
                Set<I> finalProcedures = new HashSet<>();
                for(I proceduralSymbol: getInputAlphabet().getProceduralAlphabetFromReturn(symbolInput)) {
                    DFA<S, I> procedure = getProcedures().get(proceduralSymbol);
                    Set<S> currentStatesInProcedure = currentState.getCurrentStatesFromProcedure(procedure);
                    if (currentStatesInProcedure == null) {
                        continue;
                    }
                    for(S state: currentStatesInProcedure) {
                        if (procedure.isAccepting(state)) {
                            finalProcedures.add(proceduralSymbol);
                        }
                    }
                }
                System.out.println(finalProcedures);

                VRAState<S, I, DFA<S, I>> prevState = currentState.pop();
                if (prevState == null) {
                    return new VRAState<>(new HashMap<>());
                }

                for(DFA<S, I> procedure: prevState.getCurrentProcedures()) {
                    Set<S> nextStates = new HashSet<>();
                    for(S state : prevState.getCurrentStatesFromProcedure(procedure)) {
                        for (I proceduralSymbol : finalProcedures) {
                            nextStates.add(procedure.getTransition(state, proceduralSymbol));
                        }
                    }
                    if (!nextStates.isEmpty()) {
                        nextProceduresStates.put(procedure, nextStates);
                    }
                }
                return prevState.updateProcedureStates(nextProceduresStates);
            }
            default -> throw new UnsupportedOperationException("The input symbol " + symbolInput + " is not in the vra alphabet");
        }
    }



    @Override
    public boolean isAccepting(VRAState<S, I, DFA<S, I>> vraState) {
        if (vraState.pop() != null) {
            return false;
        }
        for (S state: vraState.getCurrentStatesFromProcedure(getStartingProcedure())) {
            if (getStartingProcedure().isAccepting(state)) {
                return true;
            }
        }
        return false; // TODO
    }
}
