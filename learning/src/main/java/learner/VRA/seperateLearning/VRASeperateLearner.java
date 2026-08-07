package learner.VRA.seperateLearning;

import learner.VRA.VRALearner;
import learner.VRA.isomophicLearning.RecursiveObservationTable;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import oracle.Oracle;
import umons.ac.be.vra.AbstractVRAwithDFA;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.HashMap;

public class VRASeperateLearner<I> extends VRALearner<I> {
    protected HashMap<Pair<I, I>, SeperateRecursiveObservationTable<I>> recursiveObservationTables = new HashMap<>();

    public VRASeperateLearner(VRAlphabet<I> alphabet, Oracle<I, AbstractVRAwithDFA<?, I>> oracle) {
        super(alphabet, oracle);
    }


    @Override
    public void processCounterExample(Word<I> cx) {
        // todo
    }
}
