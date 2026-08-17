package learner.VRA.seperateLearning;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import learner.ObservationTable.RecursiveObservationTable.SeparateRecursiveObservationTable;
import learner.VRA.VRALearner;
//import learner.VRA.isomophicLearning.RecursiveObservationTable;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.common.util.Pair;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import umons.ac.be.vra.AbstractVRAwithDFA;
import umons.ac.be.vraalphabet.VRAlphabet;

public class VRASeparateLearner<I> extends VRALearner<I> {
//    public VRASeparateLearner(VRAlphabet<I> alphabet, Oracle<I, AbstractVRAwithDFA<?, I>> oracle) {
//        super(alphabet, oracle);
//        for (I call : alphabet.getCallAlphabet()) {
//            for (I ret : alphabet.getReturnAlphabet()) {
//                recursiveObservationTables.put(
//                        Pair.of(call, ret), new SeparateRecursiveObservationTable<>(new GrowingMapAlphabet<>(alphabet.getInternalAlphabet()), this, call, ret)
//                );
//            }
//        }
//    }

    public VRASeparateLearner(VRAlphabet<I> alphabet,
                              MembershipOracle<I, Boolean> membershipOracle,
                              EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> equivalenceOracle)  {
        super(alphabet, membershipOracle, equivalenceOracle);
        for (I call : alphabet.getCallAlphabet()) {
            for (I ret : alphabet.getReturnAlphabet()) {
                recursiveObservationTables.put(
                        Pair.of(call, ret), new SeparateRecursiveObservationTable<>(new GrowingMapAlphabet<>(alphabet.getInternalAlphabet()), this, call, ret)
                );
            }
        }
    }
}
