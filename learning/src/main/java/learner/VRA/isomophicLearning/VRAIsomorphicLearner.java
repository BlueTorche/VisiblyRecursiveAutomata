package learner.VRA.isomophicLearning;

import learner.VRA.VRALearner;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import oracle.Oracle;
import umons.ac.be.vra.AbstractVRAwithDFA;
import umons.ac.be.vraalphabet.VRAlphabet;

public class VRAIsomorphicLearner<I> extends VRALearner<I> {
    public VRAIsomorphicLearner(VRAlphabet<I> alphabet, Oracle<I, AbstractVRAwithDFA<?, I>> oracle) {
        super(alphabet, oracle);
    }


    @Override
    public void processCounterExample(Word<I> cx) {
        Word<I> regularCX = getRegularWord(cx, Word.epsilon(), Word.epsilon());
        System.out.println("Processing counterexample: " + cx + " with regular proj " + regularCX);
        startingObservationTable.addRepresentative(regularCX);
        startingObservationTable.enforce();
    }

    private I processRecursiveCounterExample(Word<I> prefix, I callSymbol, Word<I> cx, I returnSymbol, Word<I> suffix) {
        RecursiveObservationTable<I> recObsTab = recursiveObservationTables.get(Pair.of(callSymbol, returnSymbol));
        Word<I> regularWord = getRegularWord(cx,
                Word.fromWords(prefix, Word.fromLetter(callSymbol)),
                Word.fromWords(suffix, Word.fromLetter(returnSymbol)));
        recObsTab.addContext(prefix, suffix);
        recObsTab.addRepresentative(regularWord);
        for (RecursiveObservationTable<I> rot:  recursiveObservationTables.values()) {
            rot.enforce();
        }
        return wordToProceduralSymbol.get(recObsTab.getRecursiveEquivalent(regularWord));
    }


    private Word<I> getRegularWord(Word<I> cx, Word<I> prefix, Word<I> suffix) {
        Word<I> regularWord = Word.epsilon();
        for (int i = 0; i < cx.size(); i++) {
            if (alphabet.isCallSymbol(cx.getSymbol(i))) {
                int indexReturn = getMatchingReturnIndex(cx, i);
                System.out.println(i + "\t\t" +
                        Word.fromWords(prefix, cx.prefix(i)) + "\t\t" + cx.getSymbol(i) + "\t\t" +
                        cx.subWord(i+1, indexReturn) + "\t\t" + cx.getSymbol(indexReturn) + "\t\t" +
                        Word.fromWords(cx.suffix(cx.size()-indexReturn-1), suffix)
                );
                regularWord = regularWord.append(processRecursiveCounterExample(
                        Word.fromWords(prefix, cx.prefix(i)),
                        cx.getSymbol(i),
                        cx.subWord(i+1, indexReturn),
                        cx.getSymbol(indexReturn),
                        Word.fromWords(cx.suffix(cx.size()-indexReturn-1), suffix)
                ));
                i = indexReturn;
            }
            else {
                regularWord = regularWord.append(cx.getSymbol(i));
            }
        }
        return regularWord;
    }

}
