package oracle.rl;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.word.Word;
import oracle.Oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RLOracleWithRegex<I> implements Oracle<I, DFA<?, I>> {
    String regex;
    Alphabet<I> alphabet;
    int membershipCounter;
    int equivalenceCounter;
    int maxLength = 15;

    public RLOracleWithRegex(Alphabet<I> alphabet, String regex) {
        this.alphabet = alphabet;
        this.regex = regex;
        this.membershipCounter = 0;
        this.equivalenceCounter = 0;
    }

    public RLOracleWithRegex( Alphabet<I> alphabet, String regex, int maxLength) {
        this.alphabet = alphabet;
        this.regex = regex;
        this.maxLength = maxLength;
        this.membershipCounter = 0;
        this.equivalenceCounter = 0;
    }

    @Override
    public boolean MembershipQuery(Word<I> word) {
        this.membershipCounter++;
        return Pattern.matches(regex, word.toString().replaceAll("\\s+",""));
    }

    @Override
    public Word<I> EquivalenceQuery(DFA<?, I> hypothesis) {
        this.equivalenceCounter++;

        System.out.print("New Equivalence Query #");
        System.out.println(equivalenceCounter);

        for(int length = 1;  length < maxLength; length++) {
            List<Word<I>> toTest = generateWords(length);
            for (Word<I> word : toTest) {
                if (hypothesis.accepts(word) != Pattern.matches(regex, word.toString().replaceAll("\\s+",""))) {
                    return word;
                }
            }
        }

        return null;
    }

    private List<Word<I>> generateWords(int size){
        List<Word<I>> words = new ArrayList<>();
        if (size == 0) {
            words.add(Word.epsilon());
            return words;
        }
        List<Word<I>> prefixes = generateWords(size-1);
        for(I i: alphabet){
            for (Word<I> prefix : prefixes) {
                words.add(Word.fromWords(prefix, Word.fromLetter(i)));
            }
        }
        return words;
    }
}
