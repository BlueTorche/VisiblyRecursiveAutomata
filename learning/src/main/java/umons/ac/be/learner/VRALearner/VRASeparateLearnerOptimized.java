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

public class VRASeparateLearnerOptimized<I> extends AbstractVRALearner<I> {
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
        SeparateRecursiveObservationTableOptimized<I> recObsTab =
                (SeparateRecursiveObservationTableOptimized<I>) recursiveObservationTables.get(Pair.of(callSymbol, returnSymbol));
        Word<I> regularWord = getRegularWord(cx,
                Word.fromWords(prefix, Word.fromLetter(callSymbol)),
                Word.fromWords(Word.fromLetter(returnSymbol), suffix)
        );

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
                recObsTab.addContext(prefix, suffix);
                // récupérer J_w^{c,r} tq T(w) = T(regularWord)
                enforce();
                T_regularWord = recObsTab.getBitsetValue(regularWord);
                equivalentWord = recObsTab.getRecursiveEquivalent(T_regularWord);
            }
        }

        // Si J_w^{c,r} n'existe pas:
        if (equivalentWord == null) {
            // ajouter regularWord à S (intelligement) pour qu'il existe
            recObsTab.addRepresentative(regularWord); // TODO
            enforce();
            // récupérer J_w^{c,r} tq T(w) = T(regularWord)
            equivalentWord = recObsTab.getRecursiveEquivalent(T_regularWord);
        }

        //  Vérifier que regularWord est accepté par H^{J_w^{c,r}}
        if(!recObsTab.isAccepted(regularWord, equivalentWord)) {
            // récupérer le plus grand s tq regularword = u a s et us n'est pas accepté par H^{...} ?
//            Word<I> s = recObsTab.findSmallestSeparator(regularWord, proceduralSymbolToWord.get(Jwcr));
//            //  ajouter s à S
//            recObsTab.addSeparator(s);
            recObsTab.addRepresentative(regularWord); // TODO
            enforce();
        }

        // Retourner J_w^{c,r}
        return wordToProceduralSymbol.get(
                Word.fromWords(Word.fromLetter(callSymbol),
                        recObsTab.getRecursiveEquivalent(T_regularWord),
                        Word.fromLetter(returnSymbol)
                ));
    }

//    private I getEquivalentSymbol(BitSet T, SeparateRecursiveObservationTableOptimized<I> recObsTab) {
//        System.out.println("base set : " + T);
//        for (I symbol: this.proceduralSymbolToWord.keySet()) {
//            System.out.println(symbol + " : " + recObsTab.getContext(regularWordFromProcSymbol(symbol)));
//            if (T.equals(
//                    recObsTab.getContext(regularWordFromProcSymbol(symbol))
//            ))
//                return symbol;
//        }
//        return null;
//    }
//
//    private Word<I> regularWordFromProcSymbol(I procSymbol) {
//        return proceduralSymbolToWord.get(procSymbol).subWord(1, proceduralSymbolToWord.get(procSymbol).size()-1);
//    }

//     @Override
//    protected I processRecursiveCounterExample(Word<I> prefix, I callSymbol, Word<I> cx, I returnSymbol, Word<I> suffix) {
//        SeparateRecursiveObservationTableOptimized<I> recObsTab =
//                (SeparateRecursiveObservationTableOptimized<I>) recursiveObservationTables.get(Pair.of(callSymbol, returnSymbol));
//        Word<I> regularWord = getRegularWord(cx,
//                Word.fromWords(prefix, Word.fromLetter(callSymbol)),
//                Word.fromWords(Word.fromLetter(returnSymbol), suffix)
//        );
//
//        // Récupérer tous les symboles acceptant CX
//        Set<Word<I>> allAccepting = recObsTab.getAllEquivalent(regularWord);
//        // Si il n'y en a pas 1, ajouter v au "représentatif"
//        Word<I> uniqueAccepting;
//        if (allAccepting.size() != 1) {
//            recObsTab.addRepresentative(regularWord);
//            enforce();
//            uniqueAccepting = recObsTab.getRecursiveEquivalent(regularWord);
//        } else {
//            uniqueAccepting = allAccepting.iterator().next();
//        }
//        // Vérifier si remplacer v par son rec equiv produit un CX
//        BitSet acceptingCX = null;
//        while (
//                recursiveMembershipQuery(prefix, callSymbol, regularWord, returnSymbol, suffix) !=
//                askMembershipQuery(Word.fromWords(
//                        prefix, Word.fromLetter(callSymbol), uniqueAccepting, Word.fromLetter(returnSymbol), suffix))
//        ) {
////            System.out.println(
////                    "Testing " + regularWord + ":\n" +
////                    "MQ of " + prefix + callSymbol + regularWord+ returnSymbol + suffix + "=" +
////                            recursiveMembershipQuery(prefix, callSymbol, regularWord, returnSymbol, suffix) +
////                            "!= MQ of "  + prefix + callSymbol + uniqueAccepting+ returnSymbol + suffix + " = " +
////                            askMembershipQuery(Word.fromWords(
////                                prefix, Word.fromLetter(callSymbol), uniqueAccepting, Word.fromLetter(returnSymbol), suffix)
////                            )
////            );
//            if (acceptingCX == null) {
//                acceptingCX = new BitSet();
//                List<Pair<Word<I>, Word<I>>> contextPairs = recObsTab.getContextPairs();
//                for(int i = 0; i < contextPairs.size(); ++i) {
//                    if(recursiveMembershipQuery(contextPairs.get(i).getFirst(),
//                            callSymbol, regularWord, returnSymbol,
//                            contextPairs.get(i).getSecond())) {
//                        acceptingCX.set(i);
//                    }
//                }
//            }
//            if (acceptingCX.equals(recObsTab.getContext(uniqueAccepting))) { // T(w) = T(v)
////                System.out.println(acceptingCX + " is equal to " + recObsTab.getContext(uniqueAccepting));
////                System.out.println(recObsTab.getContextPairs());
//                recObsTab.addContext(prefix, suffix);
//                enforce();
//                // Récupérer tous les symboles acceptant CX
//                allAccepting = recObsTab.getAllEquivalent(regularWord);
//                if (allAccepting.size() != 1) {
//                    recObsTab.addRepresentative(regularWord);
//                    enforce();
//                    uniqueAccepting = recObsTab.getRecursiveEquivalent(regularWord);
//                } else {
//                    Word<I> newUniqueAccepting = allAccepting.iterator().next();
//                    if (newUniqueAccepting.equals(uniqueAccepting)) {
//                        recObsTab.addRepresentative(regularWord);
//                        enforce();
//                        uniqueAccepting = recObsTab.getRecursiveEquivalent(regularWord);
//                    } else {
//                        uniqueAccepting = newUniqueAccepting;
//                    }
//                }
//            }
//            else { // T(w) =/= T(v)
//                recObsTab.addRepresentative(regularWord);
//                enforce();
//                uniqueAccepting = recObsTab.getRecursiveEquivalent(regularWord);
//            }
//        }
//        System.out.println(recObsTab);
//        System.out.println("Processed counterexample: " + cx  +
//                "\n\twith regular proj " + regularWord +
//                "\n\twith recursive equivalent " + uniqueAccepting +
//                " : \n\tMQ of " + prefix + " " + callSymbol + " " + regularWord + " " + returnSymbol + " " + suffix + "=" +
//                recursiveMembershipQuery(prefix, callSymbol, regularWord, returnSymbol, suffix) +
//                        "== MQ of "  + prefix + callSymbol + uniqueAccepting+ returnSymbol + suffix + " = " +
//                        askMembershipQuery(Word.fromWords(
//                                prefix, Word.fromLetter(callSymbol), uniqueAccepting, Word.fromLetter(returnSymbol), suffix)
//                        )
//        );
//
//        return wordToProceduralSymbol.get(
//                Word.fromWords(Word.fromLetter(callSymbol),
//                        uniqueAccepting,
//                        Word.fromLetter(returnSymbol)
//                ));
//    }

    @Override
    public String toString() {
        return "-- VRASeparateLearnerOptimized --\n" + super.toString();
    }
}
