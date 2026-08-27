package umons.ac.be.ObservationTable.RecursiveObservationTable;

import umons.ac.be.ObservationTable.AbstractObservationTable;
import umons.ac.be.ObservationTable.Row.Content.RecursiveContent;
import umons.ac.be.ObservationTable.Row.Row;
import umons.ac.be.learner.VRALearner.AbstractVRALearner;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;

import java.util.*;

public abstract class AbstractRecursiveObservationTable<I>
        extends AbstractObservationTable<I, Map<Word<I>, FastDFA<I>>>
        implements RecursiveObservationTable<I> {
    List<Pair<Word<I>, Word<I>>> contextPairs = new ArrayList<>();

    I callSymbol;
    I returnSymbol;

    Set<Word<I>> recursiveEquivalenceClasses = new HashSet<>();

    @Override
    public Row<I> createNewRow(Word<I> r) {
        if (!allRows.containsKey(r)) {
            allRows.put(r, createNewEmptyRow(r));
            for (int idx = 0; idx < separators.size(); idx++) {
                fetchMembership(allRows.get(r), idx);
            }
            checkRowPrime(allRows.get(r));
        }
        return allRows.get(r);
    }

    @Override
    public void fetchMembership(Row<I> row, int idx) {
        for (int contIdx = 0; contIdx < contextPairs.size(); contIdx++) {
            fetchMembershipContext(row, idx, contIdx);
        }
    }

    @Override
    public Map<Word<I>, FastDFA<I>> constructHypothesis() {
        Map<Word<I>, FastDFA<I>> allDFAs = new HashMap<>();
//        System.out.println(recursiveEquivalenceClasses);
        for (Word<I> recEquivClass: recursiveEquivalenceClasses) {
            allDFAs.put(Word.fromWords(Word.fromLetter(callSymbol), recEquivClass, Word.fromLetter(returnSymbol)),
                    constructIndividualHypothesis(recEquivClass));
        }
        return allDFAs;
    }

    @Override
    public void enforce() {
        while(procComplete() || close() || consistent()) {
//            System.out.println(this);
        }
    }

    @Override
    public void fetchMembershipContext(Row<I> row, int sepIdx, int contIdx) {
        if(((AbstractVRALearner<I>) learner).recursiveMembershipQuery(
                contextPairs.get(contIdx).getFirst(),
                callSymbol,
                Word.fromWords(row.getPrefix(), separators.get(sepIdx)),
                returnSymbol,
                contextPairs.get(contIdx).getSecond()
        )) {
            row.getContent().set(Pair.of(sepIdx, contIdx));
        }
    }

    @Override
    public void addContext(Word<I> prefix, Word<I> suffix){
        Pair<Word<I>, Word<I>> pair = Pair.of(prefix, suffix);
        contextPairs.add(pair);

        for (Row<I> row : allRows.values()) {
            for(int i = 0; i < separators.size(); i++) {
                fetchMembershipContext(row, i, contextPairs.size()-1);
            }
        }
        checkNewPrimes();
    }

    @Override
    public Word<I> getRecursiveEquivalent(Word<I> word) {
        for (Word<I> recEquivClass: recursiveEquivalenceClasses) {
            if (areRecursiveEquivalent(recEquivClass, word, 0)) {
                return recEquivClass;
            }
        }
        return null;
    }

    @Override
    public boolean areRecursiveEquivalent(Word<I> recEquivClass, Word<I> representative, int separatorIdx) {
        return ((RecursiveContent) allRows.get(recEquivClass).getContent()).getContentVal(0).equals(
                ((RecursiveContent) allRows.get(representative).getContent()).getContentVal(separatorIdx)
        );
    }

    public boolean isRecursivePrime(Word<I> rowPrefix, int separatorIdx) {
        for (Word<I> recEquivClass: recursiveEquivalenceClasses) {
            if (areRecursiveEquivalent(recEquivClass, rowPrefix, separatorIdx)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("List of context pairs:\n");
        for (int i = 0; i < contextPairs.size() ; i++) {
            sb.append("\t").append(i).append(" :\t").append(contextPairs.get(i)).append("\n");
        }
        return sb.append("Table of dimension RxSxC = ")
                .append(representatives.size())
                .append("x")
                .append(separators.size())
                .append("x")
                .append(contextPairs.size())
                .append("\n")
                .append(callSymbol).append(",")
                .append(returnSymbol)
                .append(super.toString())
                .toString();
    }
}
