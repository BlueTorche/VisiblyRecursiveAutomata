package umons.ac.be.ObservationTable;

import umons.ac.be.learner.Learner;
import umons.ac.be.ObservationTable.Row.RegularRow;
import umons.ac.be.ObservationTable.Row.Row;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.word.Word;

import java.util.HashMap;

public class RegularObservationTable<I> extends AbstractObservationTable<I, FastDFA<I>> {
    public RegularObservationTable(GrowingAlphabet<I> inputAlphabet, Learner<I, ?> learner) {
        this.inputAlphabet = inputAlphabet;
        this.learner = learner;
    }

    public Row<I> createNewRow(Word<I> r){
        if (!allRows.containsKey(r)) {
            allRows.put(r, new RegularRow<>(r));
            for (int idx = 0; idx < separators.size(); idx++) {
                fetchMembership(allRows.get(r), idx);
            }
            checkRowPrime(allRows.get(r));
        }
        return allRows.get(r);
    }

    public void createNewColumn(int idx) {
        for(Row<I> row: allRows.values()) {
            fetchMembership(row, idx);
        }
        checkNewPrimes();
    }

    public void fetchMembership(Row<I> row, int idx) {
        if (learner.askMembershipQuery(
                Word.fromWords(row.getPrefix(), separators.get(idx))
        )) {
            ((RegularRow<I>) row).fetchContent(idx);
        }
    }

    public void checkRowPrime(Row<I> row) {
        for (Word<I> prime: primeRepresentatives) {
            if (areEquivalent(allRows.get(prime), row)){
//                System.out.println("Equivalent: " + prime + "-" + row.getPrefix());
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

    private boolean areEquivalent(Row<I> prime, Row<I> row){
        if(row.equivalentTo(prime)) {
            row.setParent(prime);
            return true;
        }
        return false;
    }

    public Word<I> getInconsistentSeparator(Row<I> row1, Row<I> row2) {
        int symbolIdx = row1.consistentTo(row2);
        if (symbolIdx == -1)
            return null;
        int separatorIdx = row1.getSuccessor(symbolIdx).getDistinctionSeparator(row2.getSuccessor(symbolIdx));
        assert separatorIdx != -1;
        return Word.fromWords(Word.fromLetter(inputAlphabet.getSymbol(symbolIdx)), separators.get(separatorIdx));
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
    public void enforce() {
        while(close() || consistent()) { }
    }
}
