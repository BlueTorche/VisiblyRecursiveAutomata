package umons.ac.be;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;

import java.util.HashMap;
import java.util.Map;

public class utils {
    public static <S, I> FastDFA<I> removeBinState(DFA<S, I> dfa, Alphabet<I> alphabet) {
        FastDFA<I> toReturn = new FastDFA<>(alphabet);
        Map<S, FastDFAState> stateToNewState = new HashMap<>();
        for (S s: dfa.getStates()) {
            boolean to_remove = true;
            for (I i : alphabet) {
                S nextState = dfa.getSuccessor(s, i);
                if (nextState != null && !nextState.equals(s)) {
                    to_remove = false;
                    break;
                }
            }
            if (dfa.isAccepting(s) || !to_remove) {
                FastDFAState newState = toReturn.addState(dfa.isAccepting(s));
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
