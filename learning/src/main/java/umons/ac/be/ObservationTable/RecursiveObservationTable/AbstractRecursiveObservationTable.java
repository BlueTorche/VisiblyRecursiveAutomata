package umons.ac.be.ObservationTable.RecursiveObservationTable;

import umons.ac.be.ObservationTable.AbstractObservationTable;
import umons.ac.be.ObservationTable.Row.Content.RecursiveContent;
import umons.ac.be.ObservationTable.Row.Row;
import umons.ac.be.learner.Learner;
import umons.ac.be.learner.VRALearner.AbstractVRALearner;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;

import java.util.*;
import java.util.stream.Stream;

public abstract class AbstractRecursiveObservationTable<I, L extends AbstractVRALearner<I>>
        extends AbstractObservationTable<I, Map<Word<I>, FastDFA<I>>, L>
        implements RecursiveObservationTable<I> {
    List<Pair<Word<I>, Word<I>>> contextPairs = new ArrayList<>();

    I callSymbol;
    I returnSymbol;

//    Set<Word<I>> recursiveEquivalenceClasses = new HashSet<>();
    Map<Word<I>, BitSet> recursiveEquivalenceClasses = new HashMap<>();
    Map<Word<I>, BitSet> newRecursivePrime = new HashMap<>();

    @Override
    public void fetchMembership(Row<I> row, int idx) {
        for (int contIdx = 0; contIdx < contextPairs.size(); contIdx++) {
            fetchMembershipContext(row, idx, contIdx);
        }
        if (isRecursivePrime(row.getPrefix(), idx)) {
            newRecursivePrime.put(
                    Word.fromWords(row.getPrefix(), separators.get(idx)),
                    ((RecursiveContent) allRows.get(row.getPrefix()).getContent()).getContentVal(idx)
            );
        }
    }

    @Override
    public void fetchMembershipContext(Row<I> row, int sepIdx, int contIdx) {
        if(learner.recursiveMembershipQuery(
                contextPairs.get(contIdx).getFirst(),
                callSymbol,
                Word.fromWords(row.getPrefix(), separators.get(sepIdx)),
                returnSymbol,
                contextPairs.get(contIdx).getSecond()
        )) {
            ((RecursiveContent) row.getContent()).set(Pair.of(sepIdx, contIdx));
        }
    }

    @Override
    public void enforce() {
        while(procComplete() || close() || consistent()) { }
    }

    @Override
    public Map<Word<I>, FastDFA<I>> constructHypothesis() {
        Map<Word<I>, FastDFA<I>> allDFAs = new HashMap<>();
//        System.out.println(recursiveEquivalenceClasses);
        for (Word<I> recEquivClass: recursiveEquivalenceClasses.keySet()) {
            allDFAs.put(
                    Word.fromWords(Word.fromLetter(callSymbol), recEquivClass, Word.fromLetter(returnSymbol)),
                    constructIndividualHypothesis(recEquivClass)
            );
        }
        return allDFAs;
    }

    @Override
    public void addContext(Word<I> prefix, Word<I> suffix){
        Pair<Word<I>, Word<I>> pair = Pair.of(prefix, suffix);
        contextPairs.add(pair);

        for (Row<I> row : allRows.values()) {
            for(int idx = 0; idx < separators.size(); idx++) {
                fetchMembershipContext(row, idx, contextPairs.size()-1);
            }
        }

        // We loop again to ensure all primes values have been updated
        for (Row<I> row : allRows.values()) {
            for(int idx = 0; idx < separators.size(); idx++) {
                if (isRecursivePrime(row.getPrefix(), idx)) {
                    newRecursivePrime.put(
                            Word.fromWords(row.getPrefix(), separators.get(idx)),
                            ((RecursiveContent) allRows.get(row.getPrefix()).getContent()).getContentVal(idx)
                    );
                }
            }
        }
        checkAllRowsPrimeAndInconsistence();
    }

    @Override
    public Word<I> getRecursiveEquivalent(Word<I> word) {
        return getRecursiveEquivalent(word, 0);
    }

    @Override
    public Word<I> getRecursiveEquivalent(Word<I> word, int separatorIdx) {
        for (Map.Entry<Word<I>, BitSet> recEquivClass :
                Stream.concat(
                        recursiveEquivalenceClasses.entrySet().stream(),
                        newRecursivePrime.entrySet().stream()
                ).toList()) {
            if (areRecursiveEquivalent(recEquivClass.getValue(), word, separatorIdx)) {
                return recEquivClass.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean areRecursiveEquivalent(BitSet recEquivClass, Word<I> representative, int separatorIdx) {
        return recEquivClass.equals(
                ((RecursiveContent) allRows.get(representative).getContent()).getContentVal(separatorIdx)
        );
    }

    @Override
    public boolean isRecursivePrime(Word<I> rowPrefix, int separatorIdx) {
        return getRecursiveEquivalent(rowPrefix, separatorIdx) == null;
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

    @Override
    public Map<Word<I>, BitSet> getRecursiveEquivalentClass() {
        return recursiveEquivalenceClasses;
    }
}
