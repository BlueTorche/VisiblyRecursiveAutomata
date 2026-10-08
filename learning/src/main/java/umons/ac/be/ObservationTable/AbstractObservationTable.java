package umons.ac.be.ObservationTable;

import net.automatalib.common.util.Pair;
import umons.ac.be.ObservationTable.Row.RegularRow;
import umons.ac.be.ObservationTable.Row.Row;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.word.Word;
import umons.ac.be.learner.Learner;

import java.util.*;

public abstract class AbstractObservationTable<I, M, L extends Learner<I, ?>>
        implements ObservationTable<I, M> {
    protected List<Word<I>> representatives = new ArrayList<>();
    protected List<Word<I>> separators = new ArrayList<>();

    protected Map<Word<I>, Row<I>> allRows = new HashMap<>();

    protected Set<Word<I>> primeRepresentatives = new HashSet<>();
    protected Word<I> inconsistence = null;

    protected GrowingAlphabet<I> inputAlphabet;
    protected L learner;

    // OK heritance
    @Override
    public void addRepresentative(Word<I> r) {
        if (representatives.contains(r))
            return;
        if (!r.isEmpty()) {
            addRepresentative(r.prefix(r.size()-1));
        }
        // System.out.println("Adding representative " + r);
        representatives.add(r);
        Row<I> row = createNewRow(r);
        for(int idx = 0; idx < inputAlphabet.size(); idx++) {
            row.setSuccessorRow(idx, createNewRow(Word.fromWords(r, Word.fromLetter(inputAlphabet.getSymbol(idx)))));
        }
        checkInconsistency(row);
    }

    // OK heritance
    @Override
    public void addSeparator(Word<I> s) {
        if (!separators.contains(s)) {
            // System.out.println("Adding separator " + s);
            separators.add(s);
            createNewColumn(separators.size()-1);
        }
    }

    // OK heritance
    @Override
    public Row<I> createNewRow(Word<I> r){
        if (!allRows.containsKey(r)) {
            allRows.put(r, createNewEmptyRow(r));
            for (int idx = 0; idx < separators.size(); idx++) {
                fetchMembership(allRows.get(r), idx);
            }
            checkRowPrime(allRows.get(r));
        }
        return allRows.get(r);
    }

    // OK heritance
    @Override
    public void createNewColumn(int idx) {
        for(Row<I> row: allRows.values()) {
            row.addSeparator();
            fetchMembership(row, idx);
        }
        checkAllRowsPrimeAndInconsistence();
    }

    // Override by AbstractRecObsTab
    @Override
    public void fetchMembership(Row<I> row, int idx) {
        if (learner.askMembershipQuery(
                Word.fromWords(row.getPrefix(), separators.get(idx))
        )) {
            ((RegularRow<I>) row).fetchContent(idx);
        }
    }

    // Override by AbstractRecObsTab
    @Override
    public void enforce() {
        while(close() || consistent()) { }
    }

    // OK heritance
    @Override
    public boolean close() {
        boolean toRet = false;
        for (Word<I> r: new HashSet<>(primeRepresentatives)) {
            if (!representatives.contains(r)) {
                // System.out.print("Non closed. ");
                addRepresentative(r);
                toRet = true;
            }
        }
        return toRet;
    }

    // OK heritance
    @Override
    public boolean consistent() {
        if (inconsistence == null) {
            return false;
        }
        // System.out.print("Non-consistent. ");
        Word<I> s = Word.fromWords(inconsistence);
        inconsistence = null;
        addSeparator(s);
        return true;
    }

    // Ok heritance jusque Isomorphic
    @Override
    public void checkRowPrime(Row<I> row) {
        for (Word<I> prime: primeRepresentatives) {
            if (areEquivalent(allRows.get(prime), row)){
                return;
            }
        }
        row.setParent(null);
        primeRepresentatives.add(row.getPrefix());
    }

    @Override
    public void checkInconsistency(Row<I> row) {
        if (row.isPrime()) {
            return;
        }
        Word<I> separator = getInconsistentSeparator(row, row.getParent());
        if (separator != null) {
            inconsistence = separator;
        }
    }

    @Override
    public void checkAllRowsPrimeAndInconsistence() {
        for (Word<I> r: representatives) {
            Row<I> row = allRows.get(r);
            if (!row.isPrime() && !areEquivalent(row.getParent(), row)) {
                checkRowPrime(row);
            }
            checkInconsistency(row);
        }
        for (Row<I> row: allRows.values()) {
            if (!representatives.contains(row.getPrefix())) {
                if (!row.isPrime() && !areEquivalent(row.getParent(), row)) {
                    checkRowPrime(row);
                }
            }
        }
    }

    @Override
    public Word<I> getInconsistentSeparator(Row<I> row1, Row<I> row2) {
        int symbolIdx = row1.consistentTo(row2);
        if (symbolIdx == -1)
            return null;
        int separatorIdx = row1.getSuccessor(symbolIdx).getDistinctionSeparator(row2.getSuccessor(symbolIdx));
        assert separatorIdx != -1;
        return Word.fromWords(Word.fromLetter(inputAlphabet.getSymbol(symbolIdx)), separators.get(separatorIdx));
    }

    private boolean areEquivalent(Row<I> prime, Row<I> row){
        if(row.equivalentTo(prime)) {
            row.setParent(prime);
            return true;
        }
        return false;
    }

    @Override
    public void initialize() {
        addRepresentative(Word.epsilon());
        addSeparator(Word.epsilon());
    }

    @Override
    public void addSymbol(I symbol) {
        inputAlphabet.add(symbol);
        for (Word<I> r: representatives){
            allRows.get(r).setSuccessorRow(inputAlphabet.size() -1,
                    createNewRow(Word.fromWords(r, Word.fromLetter(symbol))));
        }
        close();
        checkAllRowsPrimeAndInconsistence();
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("\t  " + separators.toString());
        for(Word<I> r: representatives) {
            output.append("\n").append(allRows.get(r));
        }
        output.append("\n----------------------");
        for(Word<I> r: allRows.keySet()){
            if (!representatives.contains(r)
                    // && allRows.get(r).notEmpty()
            ) {
                output.append("\n").append(allRows.get(r));
            }
        }
        return output.toString();
    }
}
