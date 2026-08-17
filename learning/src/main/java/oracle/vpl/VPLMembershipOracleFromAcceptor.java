package oracle.vpl;

import de.learnlib.oracle.SingleQueryOracle;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;

public class VPLMembershipOracleFromAcceptor<I> implements SingleQueryOracle<I, Boolean> {
    private final DeterministicAcceptorTS<?, I> teacherAutomata;
    private int counterNumberMQ = 0;

    public VPLMembershipOracleFromAcceptor(DeterministicAcceptorTS<?, I> automaton) {
        this.teacherAutomata = automaton;
    }

    @Override
    public Boolean answerQuery(Word<I> word) {
        counterNumberMQ++;
        return teacherAutomata.accepts(word);
    }

    @Override
    public Boolean answerQuery(Word<I> word, Word<I> word1) {
        return teacherAutomata.accepts(Word.fromWords(word, word1));
    }

    public void displayStats(){
        System.out.println("Number of MQ: " + counterNumberMQ);
    }
}
