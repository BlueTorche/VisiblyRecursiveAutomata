package learner.VRA.isomophicLearning;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import learner.ObservationTable.RecursiveObservationTable.IsomorphicRecursiveObservationTable;
import learner.VRA.VRALearner;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.common.util.Pair;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import umons.ac.be.vra.AbstractVRAwithDFA;
import umons.ac.be.vraalphabet.VRAlphabet;

public class VRAIsomorphicLearner<I> extends VRALearner<I> {
//    public VRAIsomorphicLearner(VRAlphabet<I> alphabet, Oracle<I, AbstractVRAwithDFA<?, I>> oracle) {
//        super(alphabet, oracle);
//        for (I call: alphabet.getCallAlphabet()) {
//            for (I ret: alphabet.getReturnAlphabet()) {
//                recursiveObservationTables.put(
//                        Pair.of(call, ret), new IsomorphicRecursiveObservationTable<>(new GrowingMapAlphabet<>(alphabet.getInternalAlphabet()), this, call, ret)
//                );
//            }
//        }
//    }

    public VRAIsomorphicLearner(VRAlphabet<I> alphabet,
                                MembershipOracle<I, Boolean> membershipOracle,
                                EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> equivalenceOracle)  {
        super(alphabet, membershipOracle, equivalenceOracle);
        for (I call: alphabet.getCallAlphabet()) {
            for (I ret: alphabet.getReturnAlphabet()) {
                recursiveObservationTables.put(
                        Pair.of(call, ret), new IsomorphicRecursiveObservationTable<>(new GrowingMapAlphabet<>(alphabet.getInternalAlphabet()), this, call, ret)
                );
            }
        }
    }
}
