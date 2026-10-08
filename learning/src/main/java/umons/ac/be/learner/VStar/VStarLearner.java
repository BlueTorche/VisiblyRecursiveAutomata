package umons.ac.be.learner.VStar;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.common.util.Pair;
import net.automatalib.common.util.Triple;
import net.automatalib.word.Word;
import umons.ac.be.ObservationTable.VStarObservationTables.AbstractInitialObservationTable;
import umons.ac.be.ObservationTable.VStarObservationTables.CallObservationTable;
import umons.ac.be.ObservationTable.VStarObservationTables.InitialObservationTable;

import java.util.ArrayList;
import java.util.List;

public class VStarLearner<I> extends AbstractVStarLearner<I> {
    public VStarLearner(VPAlphabet<I> alphabet,
                        MembershipOracle<I, Boolean> membershipOracle,
                        EquivalenceOracle<SEVPA<?, I>, I, Boolean> equivalenceOracle) {
        this.alphabet = alphabet;
        this.membershipOracle = membershipOracle;
        this.equivalenceOracle = equivalenceOracle;
        initialTable = new InitialObservationTable<>(alphabet, this);
        for (I c: alphabet.getCallAlphabet()) {
            callTables.put(c, new CallObservationTable<>(alphabet, c, this));
        }
    }

    @Override
    public void processCounterExample(Word<I> cx) {
        boolean positiveCX = askMembershipQuery(cx);

        while (true) {
            Triple<I, Word<I>, Word<I>> separator = getSeparator(cx, positiveCX);
            if (separator == null) {
                return;
            }
            if (separator.getFirst() == null) {
                initialTable.addSeparator(separator.getThird());
            }
            else {
                callTables.get(separator.getFirst()).addSeparator(
                        separator.getSecond(), separator.getThird());
            }
            enforceTables();
        }
/*
        Word<I> state = Word.epsilon();
        List<Pair<I, Word<I>>> stack = new ArrayList<>();
        I callSymbol = null;
        for (int i = 0; i < cx.size(); i++) {
            if (alphabet.isCallSymbol(
                    cx.getSymbol(i)
            )) {
                stack.add(Pair.of(callSymbol, state));
                state = Word.epsilon();
                callSymbol = cx.getSymbol(i);
            } else if (alphabet.isReturnSymbol(
                    cx.getSymbol(i)
            )) {
                Pair<I, Word<I>> stackSybmol = stack.removeLast();
                AbstractInitialObservationTable<I, ?> table =
                        stackSybmol.getFirst() == null ? initialTable : callTables.get(stackSybmol.getFirst());
                state = table.getSuccessor(
                            stackSybmol.getSecond(),
                            Word.fromWords(
                                    Word.fromLetter(callSymbol),
                                    state,
                                    Word.fromLetter(cx.getSymbol(i))
                            )
                    );
                callSymbol = stackSybmol.getFirst();
            } else {
                AbstractInitialObservationTable<I, ?> table =
                        callSymbol == null ? initialTable : callTables.get(callSymbol);
                state = table.getSuccessor(
                        state,
                        Word.fromLetter(cx.getSymbol(i))
                );
            }
            Word<I> stackWord = Word.epsilon();
            for (Pair<I, Word<I>> stackElement: stack) {
                if (stackElement.getFirst() != null) {
                    stackWord = stackWord.append(stackElement.getFirst());
                }
                stackWord = Word.fromWords(stackWord, stackElement.getSecond());
            }
            if (callSymbol != null) {
                stackWord = stackWord.append(callSymbol);
            }
            Word<I> wordToTest = Word.fromWords(
                    stackWord,
                    state,
                    cx.suffix(cx.size()-1-i));
            // System.out.println(callSymbol + " -- " + state + " -- " + wordToTest + " --- " + stack);
            if (positiveCX != askMembershipQuery(wordToTest)) {
                if (callSymbol != null) {
                    callTables.get(callSymbol).addSeparator(
                            stackWord,
                            cx.suffix(cx.size()-1-i)
                    );
                } else {
                    initialTable.addSeparator(cx.suffix(cx.size()-1-i));
                }
                enforceTables();
                processCounterExample(cx);
                return;
            }
        } */
    }

    public Triple<I, Word<I>, Word<I>> getSeparator(Word<I> cx, boolean positiveCX) {
        int low = 1;
        int high = cx.size();
        while (low < high) {
            int mid = (low + high) / 2;
            if (getSeparatorFromIdx(cx, mid, positiveCX) == null) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return getSeparatorFromIdx(cx, low, positiveCX);
    }

    private Triple<I, Word<I>, Word<I>> getSeparatorFromIdx(Word<I> cx, int maxIdx, boolean positiveCX) {
        Word<I> state = Word.epsilon();
        List<Pair<I, Word<I>>> stack = new ArrayList<>();
        I callSymbol = null;
        for (int i = 0; i < maxIdx; i++) {
            if (alphabet.isCallSymbol(
                    cx.getSymbol(i)
            )) {
                stack.add(Pair.of(callSymbol, state));
                state = Word.epsilon();
                callSymbol = cx.getSymbol(i);
            } else if (alphabet.isReturnSymbol(
                    cx.getSymbol(i)
            )) {
                Pair<I, Word<I>> stackSymbol = stack.removeLast();
                AbstractInitialObservationTable<I, ?> table =
                        stackSymbol.getFirst() == null ? initialTable : callTables.get(stackSymbol.getFirst());
                state = table.getSuccessor(
                        stackSymbol.getSecond(),
                        Word.fromWords(
                                Word.fromLetter(callSymbol),
                                state,
                                Word.fromLetter(cx.getSymbol(i))
                        )
                );
                callSymbol = stackSymbol.getFirst();
            } else {
                AbstractInitialObservationTable<I, ?> table =
                        callSymbol == null ? initialTable : callTables.get(callSymbol);
                state = table.getSuccessor(
                        state,
                        Word.fromLetter(cx.getSymbol(i))
                );
            }
        }
        Word<I> stackWord = Word.epsilon();
        for (Pair<I, Word<I>> stackElement: stack) {
            if (stackElement.getFirst() != null) {
                stackWord = stackWord.append(stackElement.getFirst());
            }
            stackWord = Word.fromWords(stackWord, stackElement.getSecond());
        }
        if (callSymbol != null) {
            stackWord = stackWord.append(callSymbol);
        }
        Word<I> wordToTest = Word.fromWords(
                stackWord,
                state,
                cx.suffix(cx.size()-maxIdx));
        if (positiveCX != askMembershipQuery(wordToTest)) {
            return Triple.of(callSymbol, stackWord, cx.suffix(cx.size()-maxIdx));
        }
        return null;
    }
}
