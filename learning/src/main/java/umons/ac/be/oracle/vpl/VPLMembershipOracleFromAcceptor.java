package umons.ac.be.oracle.vpl;

import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import umons.ac.be.oracle.AbstractCounterMembershipOracle;
import umons.ac.be.oracle.CounterMembershipOracle;

public class VPLMembershipOracleFromAcceptor<I> extends AbstractCounterMembershipOracle<I> {
    private final DeterministicAcceptorTS<?, I> teacherAutomata;

    public VPLMembershipOracleFromAcceptor(DeterministicAcceptorTS<?, I> automaton) {
        this.teacherAutomata = automaton;
    }

    @Override
    public Boolean getAnswer(Word<I> word) {
        return  teacherAutomata.accepts(word);
    }
}
