package umons.ac.be.learner.NewVRALearner;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.common.util.Pair;
import net.automatalib.common.util.Triple;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;
import umons.ac.be.learnlibObservationTable.VRA.RecursiveObservationTableIsomorphic;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class VRAStarIsomorphic<I> extends AbstractVRAStar<I> {
    public VRAStarIsomorphic(VPAlphabet<I> alphabet,
                             MembershipOracle<I, Boolean> membershipOracle,
                             int query_batch_size) {
        super(alphabet, membershipOracle, query_batch_size);
        for (I c: alphabet.getCallAlphabet()) {
            for (I r: alphabet.getReturnAlphabet()) {
                recursiveTables.put(
                        Pair.of(c, r),
                        new RecursiveObservationTableIsomorphic<>(tableAlphabet, c, r, query_batch_size)
                );
            }
        }
    }

    @Override
    LearningSymbol<I> processRecursiveCounterExample(Word<I> x, I c, Word<I> v, I r, Word<I> y, boolean isPositiveCX) {
        RecursiveObservationTableIsomorphic<I> recObsTab = (RecursiveObservationTableIsomorphic<I>) recursiveTables.get(Pair.of(c,r));

        // Récupérer le mot régulier z
        Word<LearningSymbol<I>> z = getRegularWord(v,
                Word.fromWords(x, Word.fromLetter(c)),
                Word.fromWords(Word.fromLetter(r), y),
                isPositiveCX
        );

        // we should get zprime s.t. T(z)=T(zprime)
        // Calculer T(z)
        BitSet T_z = recObsTab.getBitsetValue(z, membershipOracle);
        // récupérer zprime t.q. T(z)=T(zprime)
        Word<LearningSymbol<I>> zprime = recObsTab.getBitsetEquivalentWord(T_z);

        // we verify whether x c zprime r y \in L iff positiveCX
        if (zprime != null && membershipOracle.answerQuery(Word.fromWords(
                x, Word.fromLetter(c), LearningSymbol.expand(zprime), Word.fromLetter(r), y
        )) != isPositiveCX) {
            // if not, we add (x,y) to C and recompute T(z) and zprime
//            System.out.println("Adding (" + x + " " + y + ") to context set of table " + c + r);
            recursiveUnclosed.get(Pair.of(c, r)).addAll(recObsTab.addContext(Pair.of(x, y), membershipOracle));
            enforceTables();
            T_z = recObsTab.getBitsetValue(z, membershipOracle);
            zprime = recObsTab.getBitsetEquivalentWord(T_z);
        }

        // then we do RS while zprime =/= recObsTab.getEquivalentWord(z)
        while (zprime != recObsTab.getEquivalentWord(z)) { // getEquivalentRep is never null, hence if zprime is null this is always satisfied
            Word<LearningSymbol<I>> separator = getSeparator(recObsTab, z, T_z);
//            System.out.println("Adding " + separator + " to separators set of table " + c + r);
//            System.out.println(recObsTab);
//            System.out.println("Counterexample: " + z + " with T(z)=" + T_z + " but " + recObsTab.getEquivalentWord(z) + "  ~/~ "+ zprime +" and distinct sep = " + separator);
            assert separator != null;
            recursiveUnclosed.get(Pair.of(c,r))
                    .addAll(recObsTab.addSeparator(separator, membershipOracle));

            assert !recursiveUnclosed.get(Pair.of(c,r)).isEmpty();
            enforceTables();
            if (zprime == null) {
                zprime = recObsTab.getBitsetEquivalentWord(T_z);
            }
        }

////        System.out.println("Processing counterexample " + x + c + "--" + v + "--" + r + y + " with proj " + z);
//        // Récupérer le symbol J_z'^{c,r} équivalent à z
//        Word<LearningSymbol<I>> zprime = recObsTab.getEquivalentWord(z);
//
////        System.out.println("\n" + z);
////        System.out.println(zprime);
////
////        System.out.println(Word.fromWords(
////                x, Word.fromLetter(c), LearningSymbol.expand(zprime), Word.fromLetter(r), y
////        ) + " " + membershipOracle.answerQuery(Word.fromWords(
////                x, Word.fromLetter(c), LearningSymbol.expand(zprime), Word.fromLetter(r), y
////        )));
//
//        // Vérifier si prefix x c exp(z) r y est un CX, si oui, return J_z^{c,r}
//        if (membershipOracle.answerQuery(Word.fromWords(
//                x, Word.fromLetter(c), LearningSymbol.expand(zprime), Word.fromLetter(r), y
//                )) == isPositiveCX) {
//            return LearningSymbol.getLearningSymbol(c, zprime, r);
//        }
//
//
//        // Vérifier si T(z) = T(z')
//        if (T_z.equals(recObsTab.getBitsetValue(zprime, membershipOracle))) {
////            System.out.println("Adding (" + x + " " + y + ") to context set of table " + c + r);
//            // Si oui, ajouter (x,y) à C^{c,r} et et recompute J_z'^{c,r}
//            recursiveUnclosed.get(Pair.of(c, r)).addAll(recObsTab.addContext(Pair.of(x, y), membershipOracle));
//            enforceTables();
//            zprime = recObsTab.getEquivalentWord(z);
//            T_z = recObsTab.getBitsetValue(z, membershipOracle);
//        }
//
//
//        // Si T(z) =/= T(z') alors appliquer RS(z) et recompute J_z'^{c,r}
//        assert zprime != null;
//        while (!T_z.equals(recObsTab.getBitsetValue(zprime, membershipOracle))) {
//            Word<LearningSymbol<I>> separator = getSeparator(recObsTab, z, T_z);
////            System.out.println("Adding " + separator + " to separators set of table " + c + r);
////            System.out.println(recObsTab);
////            System.out.println("Counterxample: " + z + " with T(z)=" + T_z + "=/=T("+ zprime +") and distinct sep = " + separator);
//            assert separator != null;
//            recursiveUnclosed.get(Pair.of(c,r))
//                            .addAll(recObsTab.addSeparator(separator, membershipOracle));
//
//            assert !recursiveUnclosed.get(Pair.of(c,r)).isEmpty();
//            enforceTables();
//            zprime = recObsTab.getEquivalentWord(z);
//        }
//
//        // Vérifier si T(z) = T(z')
//        if (T_z.equals(recObsTab.getBitsetValue(zprime, membershipOracle)) &&
//                membershipOracle.answerQuery(Word.fromWords(
//                        x, Word.fromLetter(c), LearningSymbol.expand(zprime), Word.fromLetter(r), y
//                )) != isPositiveCX) {
////            System.out.println("Adding (" + x + " " + y + ") to context set of table " + c + r);
//            // Si oui, ajouter (x,y) à C^{c,r} et et recompute J_z'^{c,r}
//            recursiveUnclosed.get(Pair.of(c, r)).addAll(recObsTab.addContext(Pair.of(x, y), membershipOracle));
//            enforceTables();
//            zprime = recObsTab.getEquivalentWord(z);
//            T_z = recObsTab.getBitsetValue(z, membershipOracle);
//        }
//
//        System.out.println(zprime);
        // Retourner J_z'^{c,r}
        return LearningSymbol.getLearningSymbol(c, zprime, r);
    }

    Word<LearningSymbol<I>> getSeparator(RecursiveObservationTableIsomorphic<I> recObsTab,
                                                    Word<LearningSymbol<I>> z,
                                                    BitSet T_z) {
        int low = 1;
        int high = z.size();
        while (low < high) {
            int mid = (low + high) / 2;
            if (getSeparatorFromIdx(recObsTab, z, mid, T_z) == null) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return getSeparatorFromIdx(recObsTab, z, low, T_z);
    }

    Word<LearningSymbol<I>> getSeparatorFromIdx(RecursiveObservationTableIsomorphic<I> recObsTab,
                                         Word<LearningSymbol<I>> z,
                                         int idx,
                                         BitSet T_z) {
        Word<LearningSymbol<I>> prefix = z.prefix(idx);
        Word<LearningSymbol<I>> suffix = z.suffix(z.length() - idx);
        Word<LearningSymbol<I>> prefEquiv = recObsTab.getEquivalentRep(prefix);
        if(!recObsTab.getBitsetValue(Word.fromWords(prefEquiv, suffix), membershipOracle).equals(T_z)) {
            return suffix;
        }
        return null;
    }

    @Override
    FastDFA<I> constructHypothesisDFA(Alphabet<I> automatonAlphabet, I c, I r, LearningSymbol<I> procSymbol) {
        FastDFA<I> automaton = new FastDFA<>(automatonAlphabet);
        RecursiveObservationTableIsomorphic<I> recObsTab = (RecursiveObservationTableIsomorphic<I>)  recursiveTables.get(Pair.of(c,r));
        Map<RegularRow<LearningSymbol<I>>, FastDFAState> rowToState = new HashMap<>();

        for (RegularRow<LearningSymbol<I>> q: recObsTab.getRepresentativeRows()) {
            if (q.getLabel().equals(Word.epsilon())) {
                rowToState.put(q, automaton.addInitialState(recObsTab.isFinal(q, proceduralSymbolToContext.get(procSymbol))));
            } else {
                rowToState.put(q, automaton.addState(recObsTab.isFinal(q, proceduralSymbolToContext.get(procSymbol))));
            }
        }

        for (RegularRow<LearningSymbol<I>> q: recObsTab.getRepresentativeRows()) {
            for (LearningSymbol<I> s: tableAlphabet) {
                RegularRow<LearningSymbol<I>> succ = recObsTab.getSuccessor(q, s);
                I symbolTrans;
                if (learningSymbolToProceduralSymbol.containsKey(s)) {
                    symbolTrans = learningSymbolToProceduralSymbol.get(s);
                } else {
                    symbolTrans = s.expand().getSymbol(0);
                }
                automaton.addTransition(
                        rowToState.get(q),
                        symbolTrans,
                        rowToState.get(succ)
                );
            }
        }

        return automaton;
    }
}
