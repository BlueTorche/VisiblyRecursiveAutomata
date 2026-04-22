package umons.ac.be.vra;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import org.checkerframework.checker.nullness.qual.Nullable;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.*;

public abstract class AbstractVRA<S, I>
        implements VRA<VRAState<S, I, DFA<S, I>>, I,  DFA<S, I>> {

    private final VRAlphabet<I> vrAlphabet;
    private final Map<I, DFA<S, I>> procedures;
    private final DFA<S, I> startingProcedure;

    public AbstractVRA(VRAlphabet<I> vrAlphabet, Map<I, DFA<S, I>> procedures, DFA<S, I> startingProcedure) {
        this.vrAlphabet = vrAlphabet;
        this.procedures = procedures;
        this.startingProcedure = startingProcedure;
    }

    @Override
    public VRAlphabet<I> getInputAlphabet() {
        return vrAlphabet;
    }

    @Override
    public Alphabet<I> getAutomatonAlphabet() {
        return vrAlphabet.getAutomatonAlphabet();
    }

    @Override
    public DFA<S, I> getStartingProcedure() {
        return this.startingProcedure;
    }

    @Override
    public Map<I, DFA<S, I>> getProcedures() {
        return procedures;
    }

    @Override
    public @Nullable VRAState<S, I, DFA<S, I>> getTransition(VRAState<S, I, DFA<S, I>> currentState, I symbolInput) {
        HashMap<DFA<S, I>, Set<S>> nextProceduresStates = new HashMap<>();
        switch (getInputAlphabet().getSymbolType(symbolInput)) {
            case VPAlphabet.SymbolType.INTERNAL -> {
                for(DFA<S, I> procedure: currentState.getCurrentProcedures()) {
                    Set<S> nextStates = new HashSet<>();
                    for(S state : currentState.getCurrentStatesFromProcedure(procedure)) {
                        nextStates.add(procedure.getTransition(state, symbolInput));
                    }
                    if (!nextStates.isEmpty()) {
                        nextProceduresStates.put(procedure, nextStates);
                    }
                }
                return currentState.updateProcedureStates(nextProceduresStates);
            }
            case VPAlphabet.SymbolType.CALL -> {
                Set<I> nextProcedures = new HashSet<>();
                for (I proceduralSymbol : vrAlphabet.getProceduralAlphabetFromCall(symbolInput)) {
                    for(DFA<S, I> procedure: currentState.getCurrentProcedures()) {
                        for(S state : currentState.getCurrentStatesFromProcedure(procedure)) {
                            if (procedure.getTransition(state, symbolInput) != null) {
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
                    DFA<S, I> procedure = procedures.get(proceduralSymbol);
                    nextStates.put(procedure, Set.of(Objects.requireNonNull(procedure.getInitialState())));
                }
                return currentState.push(nextStates);
            }
            case VPAlphabet.SymbolType.RETURN -> {
                Set<I> finalProcedures = new HashSet<>();
                for(I proceduralSymbol: vrAlphabet.getProceduralAlphabetFromReturn(symbolInput)) {
                    DFA<S, I> procedure = procedures.get(proceduralSymbol);
                    for(S state: currentState.getCurrentStatesFromProcedure(procedure)) {
                        if (procedure.isAccepting(state)) {
                            finalProcedures.add(proceduralSymbol);
                        }
                    }
                }

                VRAState<S, I, DFA<S, I>> prevState = currentState.pop();

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
            }
        }
    }

    @Override
    public boolean isAccepting(VRAState<S, I, DFA<S, I>> s) {
        return s.isAccepting();
    }

    @Override
    public VRAState<S, I, DFA<S, I>> getInitialState() {
        Map<DFA<S, I>, Set<S>> initialLocation = new HashMap<>();
        initialLocation.put(startingProcedure, Set.of(Objects.requireNonNull(startingProcedure.getInitialState())));
        return new VRAState<>(initialLocation);
    }
}
