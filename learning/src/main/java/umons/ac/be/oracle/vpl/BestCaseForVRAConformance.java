package umons.ac.be.oracle.vpl;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import org.checkerframework.checker.nullness.qual.Nullable;
import umons.ac.be.vra.AbstractVRAwithDFA;
import umons.ac.be.vraalphabet.VRAlphabet;

import javax.swing.plaf.synth.SynthUI;
import java.util.Collection;
import java.util.Random;

public class BestCaseForVRAConformance<M  extends DeterministicAcceptorTS<?, String>>
        implements EquivalenceOracle<M, String, Boolean> {
    AbstractVRAwithDFA<FastDFAState, String> vra;
    VRAlphabet<String> alphabet;
    int n;
    int numberOfEQ;
    int numberOfIteration;
    Random random;
    int numberOfWordTried = 0;
    long totalTime = 0;
    long timeLastEQ;

    public BestCaseForVRAConformance(BestCaseForVRAMembership membershipOracle,
                                     int n,
                                     int numberOfIteration,
                                     Random random) {
        this.vra = membershipOracle.getVRA();
        this.alphabet = vra.getInputAlphabet();
        this.n=n;
        this.numberOfIteration=numberOfIteration;
        this.random = random;
    }

    @Override
    public @Nullable DefaultQuery<String, Boolean> findCounterExample(M m, Collection<? extends String> collection) {
        timeLastEQ = System.nanoTime();
        DefaultQuery<String, Boolean> query = findCounterExample(m);
        timeLastEQ = System.nanoTime() - timeLastEQ;
        totalTime += timeLastEQ;
        return query;
    }

    public @Nullable DefaultQuery<String, Boolean> findCounterExample(M m) {
        numberOfEQ++;

        String call = alphabet.getCallSymbol(0);
        String ret = alphabet.getReturnSymbol(0);

        for (int i = 0; i < numberOfIteration; i++) {
            if (i > 0 && i%1000 == 0) {
                System.out.println("Generating cx " + i + "...");
            }

            Word<String> word = generateRandomWord();
            for (String intSymbol: alphabet.getInternalAlphabet()) {
                Word<String> w = Word.fromWords(
                        Word.fromLetter(intSymbol), Word.fromLetter(call), word, Word.fromLetter(ret)
                );
                boolean accepted = vra.accepts(w);
                numberOfWordTried++;
                if (accepted != m.accepts(w)) {
                    return new DefaultQuery<>(w, accepted);
                }

                w = Word.fromWords(
                        Word.fromLetter(intSymbol), Word.fromLetter(call), word, Word.fromLetter(ret)
                );
                accepted = vra.accepts(w);
                numberOfWordTried++;
                if (accepted != m.accepts(w)) {
                    return new DefaultQuery<>(w, accepted);
                }
            }

            word = generateGibberish();
//            System.out.print(word);
            boolean accepted = vra.accepts(word);
            numberOfWordTried++;
            if (accepted != m.accepts(word)) {
                return new DefaultQuery<>(word, accepted);
            }
//            System.out.println("\t\t" + accepted + "\t\t" + m.accepts(word));
        }

        // generates word
        // test membershipOracle.getVRA().accepts(word);

        return null;
    }

    public int getNumberOfEQ() {
        return numberOfEQ;
    }

    private Word<String> generateRandomWord() {
        int k = random.nextInt(alphabet.getNumInternals());
        boolean equalToN = random.nextBoolean();
        int numberOfN = random.nextInt(0, 10);

        Word<String> word = Word.epsilon();

        for (int i = 0; i < n*numberOfN; i++) {
            while (random.nextBoolean()) {
                int l = random.nextInt(alphabet.getNumInternals());
                if (l == k) {
                    continue;
                }
                word = word.append(alphabet.getInternalSymbol(l));
            }
            word = word.append(alphabet.getInternalSymbol(k));
        }
        if (!equalToN) {
            for (int i = 0; i < random.nextInt(n-1); i++){
                while (random.nextBoolean()) {
                    int l = random.nextInt(alphabet.getNumInternals());
                    if (l == k) {
                        continue;
                    }
                    word = word.append(alphabet.getInternalSymbol(l));
                }
                word = word.append(alphabet.getInternalSymbol(k));
            }
        }

        return word;
    }

    private Word<String> generateGibberish() {
        int depth = 0;
        Word<String> word = Word.epsilon();
        for (int i = 0; i < random.nextInt(Math.max(50,2*n)); i++) {
            int k = random.nextInt(
                    alphabet.getNumInternals() +
                            alphabet.getNumCalls() +
                            alphabet.getNumReturns());
            String s = alphabet.getSymbol(k);
            if (alphabet.isReturnSymbol(s) && depth == 0) {
                word = word.append(alphabet.getCallSymbol(0));
                depth += 1;
            }
            word = word.append(s);
            if (alphabet.isReturnSymbol(s)) {
                depth -= 1;
            }
            if (alphabet.isCallSymbol(s)) {
                depth += 1;
            }
        }
        for (int i = 0; i < depth; i++) {
            word = word.append(alphabet.getReturnSymbol(0));
        }
        return word;
    }

    public long getLastEQTime() {
        return timeLastEQ / 1_000_000;
    }

    public long getTotalTime() {
        return totalTime / 1_000_000;
    }

    public int getNumberOfWordTried() {
        return numberOfWordTried;
    }
}
