package umons.ac.be.vra;

import net.automatalib.automaton.fsa.DFA;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.Map;

public class DefaultVRAwithDFA<S, I> extends AbstractVRAwithDFA<S, I> {
    @SuppressWarnings("unchecked")
    public DefaultVRAwithDFA(VRAlphabet<I> alphabet,
                             Map<I, ? extends DFA<? extends S, I>> procedures,
                             DFA<? extends S, I> startingProcedure) {
        super(alphabet, (Map<I, DFA<S, I>>) procedures, (DFA<S, I>) startingProcedure);
    }
}
