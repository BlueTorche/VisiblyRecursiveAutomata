package umons.ac.be.oracle.vpl;

import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import umons.ac.be.vra.VRA;

public class VRAConformanceOracleEnumerating<I>
        extends AbstractVPLConformanceOracleEnumerating<I, VRA<FastDFAState, I, DFA<FastDFAState, I>>> {
    public VRAConformanceOracleEnumerating(
            DeterministicAcceptorTS<?, I> teacherAutomata,
            VPAlphabet<I> alphabet) {
        super(teacherAutomata, alphabet);
    }

    public VRAConformanceOracleEnumerating(
            DeterministicAcceptorTS<?, I> teacherAutomata,
            VPAlphabet<I> alphabet,
            int maxLength) {
        super(teacherAutomata, alphabet, maxLength);
    }
}
