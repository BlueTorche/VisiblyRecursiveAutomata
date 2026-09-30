package umons.ac.be.oracle.vpl;

import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.vpa.OneSEVPA;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;

public class VPAConformanceOracleEnumerating<I>
        extends AbstractVPLConformanceOracleEnumerating<I, OneSEVPA<?, I>> {
    public VPAConformanceOracleEnumerating(DeterministicAcceptorTS<?, I> teacherAutomata, VPAlphabet<I> alphabet) {
        super(teacherAutomata, alphabet);
    }

    public VPAConformanceOracleEnumerating(
            DeterministicAcceptorTS<?, I> teacherAutomata,
            VPAlphabet<I> alphabet,
            int maxLength) {
        super(teacherAutomata, alphabet, maxLength);
    }
}
