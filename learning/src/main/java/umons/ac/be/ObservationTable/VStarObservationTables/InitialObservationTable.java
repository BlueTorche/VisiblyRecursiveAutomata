package umons.ac.be.ObservationTable.VStarObservationTables;

import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.word.Word;
import umons.ac.be.ObservationTable.AbstractObservationTable;
import umons.ac.be.learner.VStar.AbstractVStarLearner;

import java.util.ArrayList;

public class InitialObservationTable<I, L extends AbstractVStarLearner<I>>
        extends AbstractInitialObservationTable<I, L> {
    public InitialObservationTable(VPAlphabet<I> alphabet, L learner) {
        this.alphabet = alphabet;
        this.learner = learner;
        this.SigmaM = new ArrayList<>();
        for (I i: alphabet.getInternalAlphabet()) {
            SigmaM.add(Word.fromLetter(i));
        }
    }
}
