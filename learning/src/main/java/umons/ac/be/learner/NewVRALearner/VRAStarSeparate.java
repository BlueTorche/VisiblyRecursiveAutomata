package umons.ac.be.learner.NewVRALearner;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;
import umons.ac.be.ObservationTable.Row.SeparateRecursiveRow;
import umons.ac.be.learnlibObservationTable.Row.RegularRow;
import umons.ac.be.learnlibObservationTable.VRA.RecursiveObservationTable;
import umons.ac.be.learnlibObservationTable.VRA.RecursiveObservationTableIsomorphic;
import umons.ac.be.learnlibObservationTable.VRA.RecursiveObservationTableSeperate;

import java.util.*;

public class VRAStarSeparate<I> extends AbstractVRAStar<I> {
    public VRAStarSeparate(VPAlphabet<I> alphabet,
                           MembershipOracle<I, Boolean> membershipOracle,
                           int query_batch_size) {
        super(alphabet, membershipOracle, query_batch_size);
        for (I c: alphabet.getCallAlphabet()) {
            for (I r: alphabet.getReturnAlphabet()) {
                recursiveTables.put(
                        Pair.of(c, r),
                        new RecursiveObservationTableSeperate<>(tableAlphabet, c, r, query_batch_size)
                );
            }
        }
    }

    @Override
    public boolean refineHypothesis(DefaultQuery<I, Boolean> ce) {
        int oldNumberSymbol = computeNumberSymbols();
        int oldNumberStates = computeNumberStates();
        super.refineHypothesis(ce);
        return computeNumberSymbols() > oldNumberSymbol || computeNumberStates() > oldNumberStates;
    }

    @Override
    public void enforceTables() {
        super.enforceTables();
        if (checkInconsistence()) {
            enforceTables();
        }
    }

    private boolean checkInconsistence() {
        boolean addedSeparator = false;
        for (I c: alphabet.getCallAlphabet()) {
            for (I r: alphabet.getReturnAlphabet()) {
                Word<LearningSymbol<I>> separator = ((RecursiveObservationTableSeperate<I>) recursiveTables.get(Pair.of(c,r))).getInconsistence();
                if (separator != null) {
                    addedSeparator = true;
                    recursiveUnclosed.put(Pair.of(c, r),
                            new ArrayList<>(recursiveTables.get(Pair.of(c,r)).addSeparator(
                                    separator, membershipOracle
                            )));
                }
            }
        }
        return addedSeparator;
    }

    private int computeNumberSymbols() {
        int numberSymbols = initialTable.getNumberStates();
        for (RecursiveObservationTable<I> recObsTab: recursiveTables.values()) {
            numberSymbols+= recObsTab.getNumberSymbols();
        }
        return numberSymbols;
    }

    private int computeNumberStates() {
        int numberSymbols = 0;
        for (RecursiveObservationTable<I> recObsTab: recursiveTables.values()) {
            numberSymbols+= ((RecursiveObservationTableSeperate<I>) recObsTab).getNumberStates();
        }
        return numberSymbols;
    }

    @Override
    LearningSymbol<I> processRecursiveCounterExample(Word<I> x, I c, Word<I> v, I r, Word<I> y, boolean isPositiveCX) {
        RecursiveObservationTableSeperate<I> recObsTab = (RecursiveObservationTableSeperate<I>) recursiveTables.get(Pair.of(c,r));

        // Récupérer le mot régulier z
        Word<LearningSymbol<I>> z = getRegularWord(v,
                Word.fromWords(x, Word.fromLetter(c)),
                Word.fromWords(Word.fromLetter(r), y),
                isPositiveCX
        );

        // calculer T(z)
        BitSet T_z = recObsTab.getBitsetValue(z, membershipOracle);

        // get J_z'^{c,r} tq T(z) = T(z')
        Word<LearningSymbol<I>> zprime = recObsTab.getRecursiveEquivalent(T_z);

        // S'il existe, cérifier que CX \in L ssi x c z' r y \in L
        if (zprime != null &&
                isPositiveCX != membershipOracle.answerQuery(Word.fromWords(
                        x, Word.fromLetter(c),
                        LearningSymbol.expand(zprime),
                        Word.fromLetter(r), y))) {
            // add (x,y) to C^{c,r}
            // System.out.println("Counterexample analysis. Adding context" + prefix + " ---- " + suffix);
            recursiveUnclosed.get(Pair.of(c,r)).addAll(
                    recObsTab.addContext(Pair.of(x,y), membershipOracle)
            );

            // Enforce the student
            enforceTables();

            // update T(z) and J_z^{c,r}
            T_z = recObsTab.getBitsetValue(z, membershipOracle);
            zprime = recObsTab.getRecursiveEquivalent(T_z);
        }


        // If J_w^{c,r} doesn't exist
        if (zprime == null) {
            // Create a temporary table O_z^{c,r}
            recursiveUnclosed.put(Pair.of(c,r),
                    new ArrayList<>(recObsTab.createNewProceduralSymbol(z, T_z)));

            if (!recursiveUnclosed.get(Pair.of(c,r)).isEmpty()) {
                enforceTables();
            }

            // System.out.println(z);
            // System.out.println(tableAlphabet);
            while(!recObsTab.isAccepted(z, T_z)) {
                recursiveUnclosed.put(Pair.of(c,r), new ArrayList<>(recObsTab.addSeparator(
                        getSeparator(recObsTab, z, T_z, true),
                        membershipOracle
                )));
//                assert !recursiveUnclosed.get(Pair.of(c,r)).isEmpty();
                enforceTables();
            }

            // update the value of J_z'^{c,r} so that z' is a representative
            zprime = recObsTab.removeProceduralSymbol(z, T_z);
            enforceTables();
        }

        // While z is a counterexample of a table
        boolean loop = true;
        while(loop) {
            loop = false;

            // loop over all projected table
            for (BitSet T: new ArrayList<>(recObsTab.getPrimeContexts())) {
                boolean accepted = T.equals(T_z);
                // Verify whether z is a counterexample of this table
                while(recObsTab.isAccepted(z, T) != accepted) {
                    // Apply Rivest-Schappire and enforce
                    // System.out.println(recObsTab);
                    //System.out.println(z + " " + T + " " + T_z);
                    // System.out.println(recObsTab.isAccepted(z, T));
                    // System.out.println(accepted);
                    recursiveUnclosed.put(Pair.of(c,r), new ArrayList<>(recObsTab.addSeparator(
                            getSeparator(recObsTab, z, T, accepted),
                            membershipOracle
                    )));
                    // System.out.println(recObsTab);
                    // assert !recursiveUnclosed.get(Pair.of(c,r)).isEmpty(); Not always true
                    enforceTables();
                    loop = true;
                }
            }
        }

        // Retourner J_z'^{c,r}
        return LearningSymbol.getLearningSymbol(c, zprime, r);
    }


    Word<LearningSymbol<I>> getSeparator(RecursiveObservationTableSeperate<I> recObsTab,
                                         Word<LearningSymbol<I>> z,
                                         BitSet T, boolean accepted) {
        int low = 1;
        int high = z.size();
        while (low < high) {
            int mid = (low + high) / 2;
            if (getSeparatorFromIdx(recObsTab, z, mid, T, accepted) == null) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return getSeparatorFromIdx(recObsTab, z, low, T, accepted);
    }

    Word<LearningSymbol<I>> getSeparatorFromIdx(RecursiveObservationTableSeperate<I> recObsTab,
                                                Word<LearningSymbol<I>> z,
                                                int idx, BitSet T, boolean accepted) {
        Word<LearningSymbol<I>> prefix = z.prefix(idx);
        Word<LearningSymbol<I>> suffix = z.suffix(z.length() - idx);
        Word<LearningSymbol<I>> prefEquiv = recObsTab.getEquivalentRep(prefix, T);

        // System.out.println(recObsTab.getBitsetValue(Word.fromWords(prefEquiv, suffix), membershipOracle));

        if(recObsTab.getBitsetValue(Word.fromWords(prefEquiv, suffix), membershipOracle).equals(T)
                != accepted) {
            return suffix;
        }
        return null;
    }


    @Override
    FastDFA<I> constructHypothesisDFA(Alphabet<I> automatonAlphabet, I c, I r, LearningSymbol<I> procSymbol) {
        FastDFA<I> automaton = new FastDFA<>(automatonAlphabet);
        RecursiveObservationTableSeperate<I> recObsTab = (RecursiveObservationTableSeperate<I>) recursiveTables.get(Pair.of(c,r));
        Map<RegularRow<LearningSymbol<I>>, FastDFAState> rowToState = new HashMap<>();

        BitSet context = proceduralSymbolToContext.get(procSymbol);
        int contextId = recObsTab.getContextId(context);

        List<RegularRow<LearningSymbol<I>>> states = recObsTab.getRepresentativeRows(contextId);

        for (RegularRow<LearningSymbol<I>> q: states) {
            if (q.getLabel().equals(Word.epsilon())) {
                rowToState.put(q, automaton.addInitialState(recObsTab.isFinal(q, context)));
            } else {
                rowToState.put(q, automaton.addState(recObsTab.isFinal(q, context)));
            }
        }

        for (RegularRow<LearningSymbol<I>> q: states) {
            for (LearningSymbol<I> s: tableAlphabet) {
                RegularRow<LearningSymbol<I>> succ = recObsTab.getSuccessor(q, s, contextId);
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
