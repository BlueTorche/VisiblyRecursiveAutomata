package oracle.vpl;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import org.checkerframework.checker.nullness.qual.Nullable;
import umons.ac.be.vra.VRA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VPLConformanceOracleEnumeratingAcceptor<I> implements EquivalenceOracle<DeterministicAcceptorTS<?, I>, I, Boolean> {
    private final DeterministicAcceptorTS<?, I> teacherAutomata;
    private int counterNumberEQ = 0;
    private final int maxLength = 10;
    private final VPAlphabet<I> alphabet;

    public VPLConformanceOracleEnumeratingAcceptor(DeterministicAcceptorTS<?, I> teacherAutomata, VPAlphabet<I> alphabet) {
        this.teacherAutomata = teacherAutomata;
        this.alphabet = alphabet;
    }

    @Override
    public @Nullable DefaultQuery<I, Boolean> findCounterExample(
            DeterministicAcceptorTS<?, I> hypothesis, Collection<? extends I> collection) {
        this.counterNumberEQ++;

        System.out.println("New Equivalence Query #" + counterNumberEQ);

        for(int length = 1;  length < maxLength; length++) {
            List<Word<I>> toTest = generateWellMatchedWords(length, 0);
            System.out.println("Testing word of size " + length);
            for (Word<I> word : toTest) {
                boolean accepted = teacherAutomata.accepts(word);
                if (hypothesis.accepts(word)
                        != accepted) {
                    return new DefaultQuery<>(word, accepted);
                }
                // if (word.equals(Word.fromSymbols("c2", "c1", "c1", "r2", "r1", "r2"))) { throw  new RuntimeException("Error"); }
            }
        }

        return null;
    }


    /**
     * Génère tous les mots bien parenthésés jusqu’à une certaine taille.
     */
    private List<Word<I>> generateWellMatchedWords(int size, int currentDepth) {
        final List<Word<I>> words = new ArrayList<>();
        if (size == 0) {
            words.add(Word.epsilon());
            return words;
        }
        if (currentDepth == size) {
            final List<Word<I>> suffixes = generateWellMatchedWords(size-1, currentDepth-1);
            for(I r: alphabet.getReturnAlphabet()){
                for (Word<I> suffix : suffixes) {
                    words.add(Word.fromWords(Word.fromLetter(r), suffix));
                }
            }
            return words;
        }

        for(I i: alphabet){
            List<Word<I>> suffixes = new ArrayList<>();
            if (alphabet.isCallSymbol(i) && size - 1 > currentDepth) {
                suffixes = generateWellMatchedWords(size-1, currentDepth+1);
            } else if (alphabet.isReturnSymbol(i) && currentDepth > 0) {
                suffixes = generateWellMatchedWords(size-1, currentDepth-1);
            } else if (alphabet.isInternalSymbol(i)) {
                suffixes = generateWellMatchedWords(size-1, currentDepth);
            }
            for (Word<I> suffix : suffixes) {
                words.add(Word.fromWords(Word.fromLetter(i), suffix));
            }
        }
        return words;
    }

    public void displayStats() {
        System.out.println("Number of EQ: " + counterNumberEQ);
    }

    public Alphabet<I> getInputAlphabet() {
        return alphabet;
    }
}
