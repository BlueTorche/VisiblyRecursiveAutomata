package umons.ac.be.oracle;

import de.learnlib.oracle.SingleQueryOracle;

public interface CounterMembershipOracle<I> extends SingleQueryOracle.SingleQueryOracleDFA<I>{
    int getNumberOfMQ();
}
