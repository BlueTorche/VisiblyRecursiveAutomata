package umons.ac.be.learner.VRALearner;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import umons.ac.be.ObservationTable.RecursiveObservationTable.SeparateRecursiveObservationTable;
//import learner.VRA.isomophicLearning.RecursiveObservationTable;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.common.util.Pair;
import umons.ac.be.vra.VRA;
import umons.ac.be.vraalphabet.VRAlphabet;

public class VRASeparateLearner<I> extends AbstractVRALearner<I> {
    public VRASeparateLearner(VRAlphabet<I> alphabet,
                              MembershipOracle<I, Boolean> membershipOracle,
                              EquivalenceOracle<VRA<FastDFAState, I, DFA<FastDFAState, I>>, I, Boolean> equivalenceOracle)  {
        super(alphabet, membershipOracle, equivalenceOracle);
        for (I call : alphabet.getCallAlphabet()) {
            for (I ret : alphabet.getReturnAlphabet()) {
                recursiveObservationTables.put(
                        Pair.of(call, ret), new SeparateRecursiveObservationTable<>(new GrowingMapAlphabet<>(alphabet.getInternalAlphabet()), this, call, ret)
                );
            }
        }
    }

    @Override
    public String toString() {
        return "-- VRASeparateLearner --\n" + super.toString();
    }
}
