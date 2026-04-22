package umons.ac.be.vra;

import net.automatalib.automaton.UniversalDeterministicAutomaton;
import net.automatalib.automaton.fsa.DFA;
import org.checkerframework.checker.nullness.qual.Nullable;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.Map;

public class DefaultVRA<S, I> extends AbstractVRA<S, I> {
    public DefaultVRA(VRAlphabet<I> alphabet, Map<I, DFA<S, I>> procedures,  DFA<S, I> startingProcedure){
        super(alphabet, procedures, startingProcedure);
    }
}
