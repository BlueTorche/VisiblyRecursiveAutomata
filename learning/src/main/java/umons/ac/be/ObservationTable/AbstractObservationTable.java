package umons.ac.be.ObservationTable;

import umons.ac.be.learner.Learner;
import umons.ac.be.ObservationTable.Row.Row;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.word.Word;

import java.util.*;

public abstract class AbstractObservationTable<I, M> implements ObservationTable<I, M> {
    protected List<Word<I>> representatives = new ArrayList<>();
    protected Set<Word<I>> primeRepresentatives = new HashSet<>();

    protected List<Word<I>> separators = new ArrayList<>();

    protected Map<Word<I>, Row<I>> allRows = new HashMap<>();

    protected GrowingAlphabet<I> inputAlphabet;
    protected Learner<I, ?> learner;

    @Override
    public void addRepresentative(Word<I> r) {
        if (representatives.contains(r))
            return;
        if (!r.isEmpty()) {
            addRepresentative(r.prefix(r.size()-1));
        }
        representatives.add(r);
        Row<I> row = createNewRow(r);
        for(int idx = 0; idx < inputAlphabet.size(); idx++) {
            row.setSuccessorRow(idx, createNewRow(Word.fromWords(r, Word.fromLetter(inputAlphabet.getSymbol(idx)))));
        }
    }

    @Override
    public void addSeparator(Word<I> s) {
        if (!separators.contains(s)) {
            if (!s.isEmpty()) {
                addSeparator(s.suffix(s.size()-1));
            }
            separators.add(s);
            createNewColumn(separators.size()-1);
        }
    }

    @Override
    public boolean close() {
        boolean toRet = false;
        for (Word<I> r: new HashSet<>(primeRepresentatives)) {
            if (!representatives.contains(r)) {
                System.out.println("Non closed. Adding representatives " + r);
                addRepresentative(r);
                toRet = true;
            }
        }
        return toRet;
    }

    @Override
    public boolean consistent() {
        boolean toRet = false;
        for (Word<I> r: representatives) {
            Row<I> row = allRows.get(r);
            if (!row.isPrime()) {
                Word<I> separator = getInconsistentSeparator(row, row.getParent());
                if (separator != null) {
//                    System.out.println(r + " -- " + row.getParent().getPrefix() + " -- " + separator);
                    addSeparator(separator);
                    toRet = true;
                }
            }
        }
        return toRet;
    }

    @Override
    public void initialize() {
        addRepresentative(Word.epsilon());
        addSeparator(Word.epsilon());
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder("\t  " + separators.toString());
        for(Word<I> r: representatives) {
            output.append("\n").append(allRows.get(r));
        }
        output.append("\n----------------------");
        for(Word<I> r: allRows.keySet()){
            if (!representatives.contains(r) && allRows.get(r).notEmpty()) {
                output.append("\n").append(allRows.get(r));
            }
        }
        return output.toString();
    }


    @Override
    public void createNewColumn(int idx) {
        for(Row<I> row: allRows.values()) {
            row.addSeparator();
            fetchMembership(row, idx);
        }
        checkNewPrimes();
    }

    @Override
    public void addSymbol(I symbol) {
        inputAlphabet.add(symbol);
        checkNewPrimes();
        for (Word<I> r: representatives){
            allRows.get(r).setSuccessorRow(inputAlphabet.size() -1,
                    createNewRow(Word.fromWords(r, Word.fromLetter(symbol))));
        }
    }

    @Override
    public Word<I> getParent(Word<I> word) {
        if (allRows.get(word).isPrime()) {
            return word;
        }
        return allRows.get(word).getParent().getPrefix();
    }
}
