package learner.ObservationTable;

import learner.Learner;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.automaton.Automaton;

public class RegularObservationTable<I> extends AbstractObservationTable<I> {
    public RegularObservationTable(GrowingAlphabet<I> inputAlphabet, Learner<I, ?> learner) {
        this.inputAlphabet = inputAlphabet;
        this.learner = learner;
    }
}
