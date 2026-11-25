package umons.ac.be.vra;

import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.UniversalDeterministicAutomaton;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public class VRAWithDFASemantics<S, I>
        implements DeterministicAcceptorTS<VRAState<S, I, DFA<S, I>>, I> {
    AbstractVRAwithDFA<S, I> vra;

    public VRAWithDFASemantics(AbstractVRAwithDFA<S, I> vra){
        this.vra = vra;
    }

    @Override
    public VRAState<S, I, DFA<S, I>> getInitialState() {
        Map<DFA<S, I>, Set<S>> initialLocation = new HashMap<>();
        initialLocation.put(
                vra.getStartingProcedure(),
                Set.of(Objects.requireNonNull(vra.getStartingProcedure().getInitialState()))
        );
        return new VRAState<>(initialLocation);
    }

    public VRAState<S, I, DFA<S, I>> getTransition(VRAState<S, I, DFA<S, I>> currentState, I symbolInput) {
        HashMap<DFA<S, I>, Set<S>> nextProceduresStates = new HashMap<>();
//        System.out.println(symbolInput);
        switch (vra.getInputAlphabet().getSymbolType(symbolInput)) {
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
                for (I proceduralSymbol : vra.getInputAlphabet().getProceduralAlphabetFromCall(symbolInput)) {
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
                    DFA<S, I> procedure = vra.getProcedures().get(proceduralSymbol);
                    nextStates.put(procedure, Set.of(Objects.requireNonNull(procedure.getInitialState())));
                }
                return currentState.push(nextStates);
            }
            case VPAlphabet.SymbolType.RETURN -> {
                Set<I> finalProcedures = new HashSet<>();
                for(I proceduralSymbol: vra.getInputAlphabet().getProceduralAlphabetFromReturn(symbolInput)) {
                    DFA<S, I> procedure = vra.getProcedures().get(proceduralSymbol);
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

                VRAState<S, I, DFA<S, I>> prevState = currentState.pop();
                if (prevState == null) {
                    return new VRAState<>(new HashMap<>());
                }

                for(DFA<S, I> procedure: prevState.getCurrentProcedures()) {
                    Set<S> nextStates = new HashSet<>();
                    for(S state : prevState.getCurrentStatesFromProcedure(procedure)) {
                        for (I proceduralSymbol : finalProcedures) {
                            S next = procedure.getTransition(state, proceduralSymbol);
                            if (next != null) {
                                nextStates.add(procedure.getTransition(state, proceduralSymbol));
                            }
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
        if (vraState.getCurrentStatesFromProcedure(vra.getStartingProcedure()) == null) {
            return false;
        }
        for (S state: vraState.getCurrentStatesFromProcedure(vra.getStartingProcedure())) {
            if (vra.getStartingProcedure().isAccepting(state)) {
                return true;
            }
        }
        return false;
    }

}
