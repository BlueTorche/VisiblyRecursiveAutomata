package umons.ac.be.ObservationTable.RecursiveObservationTable;

import umons.ac.be.ObservationTable.Row.IsomorphicRecursiveRow;
import umons.ac.be.ObservationTable.Row.RegularRow;
import umons.ac.be.ObservationTable.Row.Row;
import umons.ac.be.learner.VRALearner.AbstractVRALearner;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.word.Word;

import java.util.HashMap;

public class IsomorphicRecursiveObservationTable<I> extends AbstractRecursiveObservationTable<I> {
    public IsomorphicRecursiveObservationTable(GrowingAlphabet<I> inputAlphabet,
                                             AbstractVRALearner<I> learner,
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
            FastDFAState s = hypothesis.addState(areRecursiveEquivalent(recEquivClass, eqClass, 0));
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
    public boolean procComplete() {
        for (Word<I> r: representatives) {
            if (recursiveEquivalenceClasses.contains(r)) {
                continue;
            }
            if (isRecursivePrime(r, 0)) {
                recursiveEquivalenceClasses.add(r);
                ((AbstractVRALearner<I>) learner).addProceduralSymbol(r, callSymbol, returnSymbol);
                return true;
            }
        }
        return false;
    }

    @Override
    public Row<I> createNewEmptyRow(Word<I> prefix) {
        return new IsomorphicRecursiveRow<>(prefix, separators.size());
    }

    public Word<I> getInconsistentSeparator(Row<I> row1, Row<I> row2) {
        int symbolIdx = row1.consistentTo(row2);
        if (symbolIdx == -1)
            return null;
        int separatorIdx = row1.getSuccessor(symbolIdx).getDistinctionSeparator(row2.getSuccessor(symbolIdx));
        assert separatorIdx != -1;
        return Word.fromWords(Word.fromLetter(inputAlphabet.getSymbol(symbolIdx)), separators.get(separatorIdx));
    }

    public void checkRowPrime(Row<I> row) {
        for (Word<I> prime: primeRepresentatives) {
            if (areEquivalent(allRows.get(prime), row)){
                return;
            }
        }
        row.setParent(null);
        primeRepresentatives.add(row.getPrefix());
    }

    public void checkNewPrimes() {
        for (Word<I> r: representatives) {
            Row<I> row = allRows.get(r);
            if (!row.isPrime() && !areEquivalent(row.getParent(), row)) {
                checkRowPrime(row);
            }
        }
        for (Row<I> row: allRows.values()) {
            if (!representatives.contains(row.getPrefix())) {
                if (!row.isPrime() && !areEquivalent(row.getParent(), row)) {
                    checkRowPrime(row);
                }
            }
        }
    }

    public boolean areEquivalent(Row<I> prime, Row<I> row){
        if(row.equivalentTo(prime)) {
            row.setParent(prime);
            return true;
        }
        return false;
    }
}
