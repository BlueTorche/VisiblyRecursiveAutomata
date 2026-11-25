package umons.ac.be.oracle;

import de.learnlib.oracle.SingleQueryOracle;
import net.automatalib.word.Word;

public interface CounterMembershipOracle<I> extends SingleQueryOracle.SingleQueryOracleDFA<I>{
    int getNumberOfMQ();

    long getTotalTime();

    int getMeanMQSize();
}
