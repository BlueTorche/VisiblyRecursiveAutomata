package umons.ac.be.ObservationTable.RecursiveObservationTable;

import umons.ac.be.ObservationTable.Row.Content.RecursiveContent;
import umons.ac.be.ObservationTable.Row.IsomorphicRecursiveRow;
import umons.ac.be.ObservationTable.Row.RegularRow;
import umons.ac.be.ObservationTable.Row.Row;
import umons.ac.be.learner.VRALearner.AbstractVRALearner;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.word.Word;
import umons.ac.be.learner.VRALearner.VRAIsomorphicLearner;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class IsomorphicRecursiveObservationTable<I, L extends VRAIsomorphicLearner<I>>
        extends AbstractRecursiveObservationTable<I, L> {
    public IsomorphicRecursiveObservationTable(GrowingAlphabet<I> inputAlphabet,
                                               L learner,
                                               I callSymbol,
                                               I returnSymbol) {
        this.inputAlphabet = inputAlphabet;
        this.learner = learner;
        this.callSymbol = callSymbol;
        this.returnSymbol = returnSymbol;
    }

    @Override
    public FastDFA<I> constructIndividualHypothesis(Word<I> recEquivClass) {
        FastDFA<I> hypothesis = new FastDFA<>(inputAlphabet);
        HashMap<Row<I>, Integer> rowToStateID = new HashMap<>();
        for(Word<I> eqClass : primeRepresentatives) {
            FastDFAState s = hypothesis.addState(
                    areRecursiveEquivalent(recursiveEquivalenceClasses.get(recEquivClass), eqClass, 0)
            );
            rowToStateID.put(allRows.get(eqClass), s.getId());
            if (eqClass.equals(Word.epsilon())) {
                hypothesis.setInitial(s, true);
            }
        }
        for(Word<I> eqClass : primeRepresentatives) {
            for (int idx = 0; idx < inputAlphabet.size(); idx++) {
                Row<I> ingoingRow = allRows.get(eqClass).getSuccessor(idx);
                Row<I> primeIngoingRow = ingoingRow.isPrime() ? ingoingRow : ingoingRow.getParent();
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
    public boolean procComplete() {
//        System.out.println(newRecursivePrime);
        if(!newRecursivePrime.isEmpty()) {
            Map.Entry<Word<I>, BitSet> newPrime = newRecursivePrime.entrySet().iterator().next();
            newRecursivePrime.remove(newPrime.getKey());
            recursiveEquivalenceClasses.put(newPrime.getKey(), newPrime.getValue());
            learner.addProceduralSymbol(newPrime.getKey(), callSymbol, returnSymbol);
            return true;
        }
        return false;
    }

    @Override
    public Row<I> createNewEmptyRow(Word<I> prefix) {
        return new IsomorphicRecursiveRow<>(prefix, separators.size());
    }
}
