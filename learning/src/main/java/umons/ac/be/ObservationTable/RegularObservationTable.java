package umons.ac.be.ObservationTable;

import umons.ac.be.learner.Learner;
import umons.ac.be.ObservationTable.Row.RegularRow;
import umons.ac.be.ObservationTable.Row.Row;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.word.Word;

import java.util.HashMap;

public class RegularObservationTable<I, L extends Learner<I, ?>> extends AbstractObservationTable<I, FastDFA<I>, L> {
    public RegularObservationTable(GrowingAlphabet<I> inputAlphabet, L learner) {
        this.inputAlphabet = inputAlphabet;
        this.learner = learner;
    }

    @Override
    public FastDFA<I> constructHypothesis() {
        FastDFA<I> hypothesis = new FastDFA<>(inputAlphabet);
        HashMap<Row<I>, Integer> rowToStateID = new HashMap<>();
        for(Word<I> eqClass : primeRepresentatives) {
            FastDFAState s = hypothesis.addState(allRows.get(eqClass).isAccepting());
            rowToStateID.put(allRows.get(eqClass), s.getId());
            if (eqClass.equals(Word.epsilon())) {
                hypothesis.setInitial(s, true);
            }
        }
        for(Word<I> eqClass : primeRepresentatives) {
            for (int idx = 0; idx < inputAlphabet.size(); idx++) {
                RegularRow<I> ingoingRow = (RegularRow<I>) allRows.get(eqClass).getSuccessor(idx);
                RegularRow<I> primeIngoingRow = ingoingRow.isPrime() ? ingoingRow : (RegularRow<I>) ingoingRow.getParent();
                hypothesis.addTransition(
                        hypothesis.getState(rowToStateID.get(allRows.get(eqClass))),
                        inputAlphabet.getSymbol(idx),
                        hypothesis.getState((rowToStateID.get(primeIngoingRow)))
                );
            }
        }

        return hypothesis;
    }

    @Override
    public Row<I> createNewEmptyRow(Word<I> r) {
        return new RegularRow<>(r);
    }
}
