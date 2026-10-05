package umons.ac.be.ObservationTable.VStarObservationTables;

import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.word.Word;
import umons.ac.be.ObservationTable.AbstractObservationTable;
import umons.ac.be.ObservationTable.Row.RegularRow;
import umons.ac.be.ObservationTable.Row.Row;
import umons.ac.be.learner.VStar.AbstractVStarLearner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractInitialObservationTable<I, L extends AbstractVStarLearner<I>>
    extends AbstractObservationTable<I, SEVPA<?, I>, L> {
    protected VPAlphabet<I> alphabet;
    protected List<Word<I>> SigmaM;

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
        checkInconsistency(row);
    }

    public Word<I> getWellMatchedPrefix(Word<I> r) {
        int depth = 0;
        for (int idx = 0; idx < r.size(); idx++) {
            if (alphabet.isReturnSymbol(r.getSymbol(r.size()-1-idx))) {
                depth ++;
            } else if (alphabet.isCallSymbol(r.getSymbol(r.size()-1-idx))) {
                depth --;
            }
            if (depth == 0) {
                return r.prefix(r.size()-1-idx);
            }
        }
        return Word.epsilon();
    }

    public Word<I> getWellMatchedSuffix(Word<I> r) {
        int depth = 0;
        for (int idx = 0; idx < r.size(); idx++) {
            if (alphabet.isCallSymbol(r.getSymbol(idx))) {
                depth ++;
            } else if (alphabet.isReturnSymbol(r.getSymbol(idx))) {
                depth --;
            }
            if (depth == 0) {
                return r.prefix(idx+1);
            }
        }
        assert false;
        return null;
    }

    @Override
    public void initialize() {
        addRepresentative(Word.epsilon());
        addSeparator(Word.epsilon());
    }

    @Override
    public SEVPA<?, I> constructHypothesis() {
        assert false;
        return null;
    }

    @Override
    public void addSymbol(I symbol) {
        assert false;
    }

    public void addSymbol(Word<I> symbol) {
        SigmaM.add(symbol);
        for (Word<I> r: representatives){
            allRows.get(r).setSuccessorRow(SigmaM.size() -1,
                    createNewRow(Word.fromWords(r, symbol)));
        }
        close();
        checkAllRowsPrimeAndInconsistence();
    }

    @Override
    public Row<I> createNewEmptyRow(Word<I> r) {
        return new RegularRow<>(r);
    }

    public List<Word<I>> getQ() {
        return representatives;
    }

    public boolean isFinal(Word<I> q) {
        return allRows.get(q).isAccepting();
    }

    public Map<Word<I>,Word<I>> getSuccessorList(Word<I> q) {
        Map<Word<I>, Word<I>> symbolToSuccessor = new HashMap<>();
        for (int i = 0; i < SigmaM.size(); i++) {
            Row<I> successor = allRows.get(q).getSuccessor(i);
            //System.out.println(q + " " + allRows.get(q));
            //System.out.println(i + " " + SigmaM.get(i));
            //System.out.println(successor);
            // System.out.println(SigmaM);
            symbolToSuccessor.put(
                    SigmaM.get(i),
                    successor.isPrime() ? successor.getPrefix() : successor.getParent().getPrefix()
            );
        }
        return symbolToSuccessor;
    }

    public Word<I> getSuccessor(Word<I> state, Word<I> symbol) {
        assert representatives.contains(state);
        int idx = SigmaM.indexOf(symbol);
        assert idx >= 0;
        Row<I> successor = allRows.get(state).getSuccessor(idx);
        return successor.isPrime() ? successor.getPrefix() : successor.getParent().getPrefix();
    }
}
