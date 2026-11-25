package umons.ac.be.vra;

import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import org.checkerframework.checker.nullness.qual.Nullable;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.*;

public class DefaultVRAwithDFA<S, I> extends AbstractVRAwithDFA<S, I> {
    @SuppressWarnings("unchecked")
    public DefaultVRAwithDFA(VRAlphabet<I> alphabet,
                             Map<I, ? extends DFA<? extends S, I>> procedures,
                             DFA<? extends S, I> startingProcedure) {
        super(alphabet, (Map<I, DFA<S, I>>) procedures, (DFA<S, I>) startingProcedure);
    }
}
