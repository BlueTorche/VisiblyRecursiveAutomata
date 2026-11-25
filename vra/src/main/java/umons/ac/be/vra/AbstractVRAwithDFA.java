package umons.ac.be.vra;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import umons.ac.be.utils;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.*;

public abstract class AbstractVRAwithDFA<S, I> extends
        AbstractVRA<S, I, DFA<S, I>> {

    public AbstractVRAwithDFA(VRAlphabet<I> vrAlphabet,
                              Map<I, DFA<S, I>> procedures,
                              DFA<S, I> startingProcedure) {
        super(vrAlphabet, procedures, startingProcedure);
    }


    @Override
    public Collection<VRAState<S, I, DFA<S, I>>> getStates() {
        return List.of();
    }

    @Override
    public Boolean getStateProperty(VRAState<S, I, DFA<S, I>> vraState) {
        return getSemantics().isAccepting(vraState);
    }


    @Override
    public VRAState<S, I, DFA<S, I>> getSuccessor(VRAState<S, I, DFA<S, I>> vraState) {
        return null;
    }

    @Override
    public AbstractVRAwithDFA<S, I> removeBinStatesAndAutomata() {
        Map<I, DFA<S, I>> newProcedures = new HashMap<>();
        for (Map.Entry<I, DFA<S, I>> dfa: getProcedures().entrySet()) {
            newProcedures.put(dfa.getKey(), (DFA<S, I>) removeBinState(dfa.getValue(), getAutomatonAlphabet()));
        }
        AbstractVRAwithDFA<S, I> newVRA = new DefaultVRAwithDFA<>(
                getInputAlphabet(),
                newProcedures,
                (DFA<S, I>) removeBinState(getStartingProcedure(), getAutomatonAlphabet())
        );
        Alphabet<I> nonEmptyProcSymbol = newVRA.detectNonEmptyContext();
        for (I proceduralSymbol : getInputAlphabet().getProceduralAlphabet()) {
            if (!nonEmptyProcSymbol.contains(proceduralSymbol)) {
                newVRA.removeProcedure(proceduralSymbol);
            }
        }
        return newVRA;
    }

    public Alphabet<I> detectNonEmptyContext() {
        Alphabet<I> nonEmpty =  new GrowingMapAlphabet<>();
        HashSet<DFA<S, I>> stack = new HashSet<>();
        stack.add(getStartingProcedure());
        while (!stack.isEmpty()) {
            DFA<S, I> dfa = stack.iterator().next();
            stack.remove(dfa);
            for (I proceduralSymbol: getInputAlphabet().getProceduralAlphabet()) {
                if (!nonEmpty.contains(proceduralSymbol) &&
                    hasTransition(dfa, proceduralSymbol)) {
                    nonEmpty.add(proceduralSymbol);
                    stack.add(getProcedures().get(proceduralSymbol));
                }
            }
        }
        return nonEmpty;
    }

    @Override
    public int numberOfTrans() {
        int total = 0;

        for (DFA<S, I> p: getAllProcedures().values()) {
            for (S states: p.getStates()) {
                for (I symbol: getAutomatonAlphabet()) {
                    if (p.getTransition(states, symbol) != null) {
                        total++;
                    }
                }
            }
        }

        return total;
    }

    @Override
    public DeterministicAcceptorTS<VRAState<S, I, DFA<S, I>>, I> getSemantics(){
        return new VRAWithDFASemantics<>(this);
    }


    @Override
    public boolean accepts(Word<I> word) {
        VRAState<S, I, DFA<S, I>> currentState = getSemantics().getInitialState();

        for (I symbol: word) {
            currentState = getSemantics().getTransition(currentState, symbol);
            if (currentState.getCurrentProcedures().isEmpty()) {
                return false;
            }
        }
        return getSemantics().isAccepting(currentState);
    }


    public static <S, I> FastDFA<I> removeBinState(DFA<S, I> dfa, Alphabet<I> alphabet) {
        FastDFA<I> toReturn = new FastDFA<>(alphabet);
        Map<S, FastDFAState> stateToNewState = new HashMap<>();
        for (S s: dfa.getStates()) {
            boolean to_remove = true;
            for (I i : alphabet) {
                S nextState = dfa.getTransition(s, i);
                if (nextState != null && !nextState.equals(s)) {
                    to_remove = false;
                    break;
                }
            }
            if (dfa.isAccepting(s) || !to_remove) {
                FastDFAState newState = toReturn.addState(dfa.isAccepting(s));
                if (dfa.getInitialState().equals(s)) {
                    toReturn.setInitialState(newState);
                }
                stateToNewState.put(s, newState);
            }
        }
        for (S s: dfa.getStates()) {
            if (!stateToNewState.containsKey(s)) { continue; }
            for (I i : alphabet) {
                S nextState = dfa.getSuccessor(s, i);
                if (nextState != null && stateToNewState.containsKey(nextState)) {
                    toReturn.addTransition(
                            stateToNewState.get(s),
                            i,
                            stateToNewState.get(nextState)
                    );
                }
            }
        }

        return toReturn;
    }

    public static <S,I> boolean hasTransition(DFA<S, I> dfa, I symbol) {
        for (S s: dfa.getStates()) {
            if (dfa.getTransition(s, symbol) != null) {
                return true;
            }
        }
        return false;
    }
}
