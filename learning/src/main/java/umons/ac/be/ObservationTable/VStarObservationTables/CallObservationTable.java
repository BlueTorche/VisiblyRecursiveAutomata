package umons.ac.be.ObservationTable.VStarObservationTables;

import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.word.Word;
import umons.ac.be.learner.VStar.AbstractVStarLearner;

import java.util.ArrayList;

public class CallObservationTable<I, L extends AbstractVStarLearner<I>>
        extends AbstractCallObservationTable<I, L> {
    public CallObservationTable(VPAlphabet<I> alphabet,
                                I callSymbol,
                                L learner) {
        this.alphabet = alphabet;
        this.callSymbol = callSymbol;
        this.learner = learner;
        this.SigmaM = new ArrayList<>();
        for (I i: alphabet.getInternalAlphabet()) {
            SigmaM.add(Word.fromLetter(i));
        }
    }
}
