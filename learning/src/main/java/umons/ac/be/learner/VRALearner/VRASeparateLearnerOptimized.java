package umons.ac.be.learner.VRALearner;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import umons.ac.be.ObservationTable.RecursiveObservationTable.SeparateRecursiveObservationTableOptimized;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import umons.ac.be.vra.VRA;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.*;

public class VRASeparateLearnerOptimized<I> extends VRASeparateLearner<I> {
    public VRASeparateLearnerOptimized(VRAlphabet<I> alphabet,
                                       MembershipOracle<I, Boolean> membershipOracle,
                                       EquivalenceOracle<VRA<FastDFAState, I, DFA<FastDFAState, I>>, I, Boolean> equivalenceOracle) {
        super(alphabet, membershipOracle, equivalenceOracle);
        for (I call : alphabet.getCallAlphabet()) {
            for (I ret : alphabet.getReturnAlphabet()) {
                recursiveObservationTables.put(
                        Pair.of(call, ret), new SeparateRecursiveObservationTableOptimized<>(
                                new GrowingMapAlphabet<>(alphabet.getInternalAlphabet()), this, call, ret
                        )
                );
            }
        }
    }

    @Override
    protected I processRecursiveCounterExample(Word<I> prefix, I callSymbol, Word<I> cx, I returnSymbol, Word<I> suffix) {
        SeparateRecursiveObservationTableOptimized<I, VRASeparateLearnerOptimized<I>> recObsTab =
                (SeparateRecursiveObservationTableOptimized<I, VRASeparateLearnerOptimized<I>>) recursiveObservationTables.get(Pair.of(callSymbol, returnSymbol));
        Word<I> regularWord = getRegularWord(cx,
                Word.fromWords(prefix, Word.fromLetter(callSymbol)),
                Word.fromWords(Word.fromLetter(returnSymbol), suffix)
        );

        boolean flag = false;

        // calculer T(regularWord)
        BitSet T_regularWord = recObsTab.getBitsetValue(regularWord);

        // récupérer J_w^{c,r} tq T(w) = T(regularWord)
        Word<I> equivalentWord = recObsTab.getRecursiveEquivalent(T_regularWord);

        // S'il existe:
        if (equivalentWord != null) {
            // Vérifier que prefix callsymbol w returnsymbol suffix \in L ssi prefix callsymbol cx returnsymbol suffix \in L
            if (membershipOracle.answerQuery(Word.fromWords(
                    prefix, Word.fromLetter(callSymbol),
                    expand(regularWord),
                    Word.fromLetter(returnSymbol), suffix
                )) != membershipOracle.answerQuery(Word.fromWords(
                    prefix, Word.fromLetter(callSymbol),
                    expand(equivalentWord),
                    Word.fromLetter(returnSymbol), suffix
                ))
            ) {
                // ajouter (x,y) à C^{c,r}
                System.out.println("Counterexample analysis. Adding context" + prefix + " ---- " + suffix);
                recObsTab.addContext(prefix, suffix);
                // récupérer J_w^{c,r} tq T(w) = T(regularWord)
                enforce();
                T_regularWord = recObsTab.getBitsetValue(regularWord);
                equivalentWord = recObsTab.getRecursiveEquivalent(T_regularWord);
                flag = true;
            }
        }

        if (recObsTab.isInRows(regularWord)) {
            return wordToProceduralSymbol.get(
                    Word.fromWords(Word.fromLetter(callSymbol),
                            recObsTab.getRecursiveEquivalent(regularWord),
                            Word.fromLetter(returnSymbol)
                    ));
        }

        // Si J_w^{c,r} n'existe pas:
        if (equivalentWord == null) {
            System.out.print("Counterexample analysis. A new DFA will be created. ");
            // ajouter le plus petit prefix s de regularWord tq T(s)=T(regularWord)
            recObsTab.addRepresentative(regularWord);
            enforce();
            // récupérer J_w^{c,r} tq T(w) = T(regularWord)
            equivalentWord = recObsTab.getRecursiveEquivalent(T_regularWord);
            flag = true;
        }

        //  Vérifier pour tous H^{J_w^{c,r}} que regularWord n'est accepté que s'il est équivalent à w
        for (Word<I> equivClass: recObsTab.getRecursiveEquivalenceClasses()) {
            if(equivClass.equals(equivalentWord) && !recObsTab.isAccepted(regularWord, equivClass)
             || !equivClass.equals(equivalentWord) && recObsTab.isAccepted(regularWord, equivClass)) {
                System.out.print("Counterexample of the DFA " + equivClass + ". ");
                recObsTab.addSeparator(
                        findSeparator(regularWord, recObsTab, equivClass, equivClass.equals(equivalentWord))
                );
                enforce();
                flag = true;
            }
        }

        if (flag) {
//            System.out.println("Modification made to process " + regularWord);
        }

        // Retourner J_w^{c,r}
        return wordToProceduralSymbol.get(
                Word.fromWords(Word.fromLetter(callSymbol),
                        recObsTab.getRecursiveEquivalent(T_regularWord),
                        Word.fromLetter(returnSymbol)
                ));
    }

    private Word<I> findSeparator(Word<I> counterexample,
                                  SeparateRecursiveObservationTableOptimized<I, VRASeparateLearnerOptimized<I>> recObsTab,
                                  Word<I> equivalenceClass,
                                  boolean shouldBeAccepted) {
        BitSet T_equivClass = recObsTab.getBitsetValue(equivalenceClass);
        for (int i = 1; i < counterexample.length()+1; i++) { // TODO binary search
            Word<I> prefix = counterexample.prefix(counterexample.length() - i);
            Word<I> suffix = counterexample.suffix(i);
            Word<I> prefEquiv = recObsTab.getRegularEquivalent(prefix, equivalenceClass);
            if(shouldBeAccepted ==
                    recObsTab.getBitsetValue(Word.fromWords(prefEquiv, suffix)).equals(T_equivClass)) {
                return suffix.subWord(1);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "-- VRASeparateLearnerOptimized --\n" + super.toString();
    }
}
