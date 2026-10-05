package umons.ac.be.ObservationTable.VStarObservationTables;

import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import umons.ac.be.ObservationTable.Row.RegularRow;
import umons.ac.be.ObservationTable.Row.Row;
import umons.ac.be.learner.VStar.AbstractVStarLearner;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCallObservationTable<I, L extends AbstractVStarLearner<I>>
        extends AbstractInitialObservationTable<I, L> {
    protected I callSymbol;

    protected List<Pair<Word<I>,Word<I>>> C = new ArrayList<>();
    protected Pair<Word<I>,Word<I>> inconsistentC;

    @Override
    public void addRepresentative(Word<I> r) {
        if (representatives.contains(r))
            return;
        if (!r.isEmpty()) {
            addRepresentative(getWellMatchedPrefix(r));
        }

        System.out.println("Adding representative " + r);
        representatives.add(r);
        Row<I> row = createNewRow(r);
        for(int idx = 0; idx < SigmaM.size(); idx++) {
            row.setSuccessorRow(
                    idx,
                    createNewRow(Word.fromWords(r, SigmaM.get(idx)))
            );
        }
        for (I ret: alphabet.getReturnAlphabet()) {
            learner.addSymbol(Word.fromWords(
                    Word.fromLetter(callSymbol),
                    r,
                    Word.fromLetter(ret)
            ));
        }
        checkInconsistency(row);
    }

    @Override
    public Row<I> createNewRow(Word<I> r){
        if (!allRows.containsKey(r)) {
            allRows.put(r, createNewEmptyRow(r));
            for (int idx = 0; idx < C.size(); idx++) {
                fetchMembership(allRows.get(r), idx);
            }
            checkRowPrime(allRows.get(r));
        }
        return allRows.get(r);
    }

    @Override
    public void addSeparator(Word<I> s) {
        assert false;
    }

    public void addSeparator(Word<I> s, Word<I> p) {
        Pair<Word<I>, Word<I>> newPair = Pair.of(s, p);
        if (!C.contains(newPair)) {
            System.out.println("Adding separator " + s + " - " + p);
            C.add(newPair);
            createNewColumn(C.size()-1);
        }
    }

    @Override
    public boolean consistent(){
        if (inconsistentC == null) {
            return false;
        }
        System.out.print("Non-consistent. ");
        Word<I> first = inconsistentC.getFirst();
        Word<I> second = inconsistentC.getSecond();
        inconsistentC = null;
        addSeparator(first, second);
        return true;
    }

    @Override
    public void initialize() {
        addRepresentative(Word.epsilon());
        for (I r: alphabet.getReturnAlphabet()) {
            addSeparator(Word.fromLetter(callSymbol), Word.fromLetter(r));
        }
    }

    @Override
    public void fetchMembership(Row<I> row, int idx) {
        //System.out.println("Fetching membership for " + row.getPrefix());
        if(learner.askMembershipQuery(Word.fromWords(
                C.get(idx).getFirst(),
                row.getPrefix(),
                C.get(idx).getSecond()
        ))) {
            ((RegularRow<I>) row).fetchContent(idx);
        }
    }

    @Override
    public void checkInconsistency(Row<I> row) {
        if (row.isPrime()) {
            return;
        }
        Pair<Word<I>, Word<I>> separator = getInconsistentSeparatorC(row, row.getParent());
        if (separator != null) {
            inconsistentC = separator;
        }
    }

    @Override
    public Word<I> getInconsistentSeparator(Row<I> row1, Row<I> row2) {
        assert false;
        return null;
    }

    public Pair<Word<I>, Word<I>> getInconsistentSeparatorC(Row<I> row1, Row<I> row2) {
        int symbolIdx = row1.consistentTo(row2);
        if (symbolIdx == -1)
            return null;
        int separatorIdx = row1.getSuccessor(symbolIdx).getDistinctionSeparator(row2.getSuccessor(symbolIdx));
        assert separatorIdx != -1;
        return Pair.of(
                C.get(separatorIdx).getFirst(),
                Word.fromWords(
                        SigmaM.get(symbolIdx),
                        C.get(separatorIdx).getSecond()
                ));
    }

    @Override
    public String toString() {
        return "\t" + C + "\n" + super.toString();
    }
}
