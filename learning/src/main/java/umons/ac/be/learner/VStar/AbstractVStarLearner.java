package umons.ac.be.learner.VStar;

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.Query;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.automaton.vpa.impl.DefaultNSEVPA;
import net.automatalib.automaton.vpa.impl.Location;
import net.automatalib.visualization.Visualization;
import net.automatalib.word.Word;
import umons.ac.be.ObservationTable.VStarObservationTables.AbstractCallObservationTable;
import umons.ac.be.ObservationTable.VStarObservationTables.AbstractInitialObservationTable;
import umons.ac.be.ObservationTable.VStarObservationTables.CallObservationTable;
import umons.ac.be.learner.Learner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class AbstractVStarLearner<I> implements Learner<I, SEVPA<?, I>> {
    protected VPAlphabet<I> alphabet;
    protected MembershipOracle<I, Boolean> membershipOracle;
    protected EquivalenceOracle<SEVPA<?, I>, I, Boolean> equivalenceOracle;

    protected Map<I, AbstractCallObservationTable<I,?>> callTables = new HashMap<>();
    protected AbstractInitialObservationTable<I,?> initialTable;

    private int numberOfEQ = 0;
    private int numberOfMQ = 0;

    @Override
    public SEVPA<?, I> learn() {
        initialize();
        enforceTables();
        while (true) {
            // printTables();
            SEVPA<?, I> hypothesis = constructHypothesis();
            // Visualization.visualize(hypothesis);
            numberOfEQ++;
            Query<I, Boolean> answer = equivalenceOracle.findCounterExample(hypothesis, new HashSet<>());
            if (answer == null) {
                return hypothesis;
            }
            System.out.println("Processing counterexample " + numberOfEQ + ": " + answer);
            processCounterExample(answer.getInput());
            enforceTables();
        }
    }

    @Override
    public SEVPA<?, I> constructHypothesis() {
        DefaultNSEVPA<I> sevpa = new DefaultNSEVPA<>(alphabet);

        Map<Word<I>, Location> initialLocations = new HashMap<>();
        for (Word<I> q: initialTable.getQ()) {
            if (q.equals(Word.epsilon())) {
                initialLocations.put(
                        q, sevpa.addInitialLocation(initialTable.isFinal(q)));
            } else {
                initialLocations.put(q, sevpa.addLocation(initialTable.isFinal(q)));
            }
        }

        Map<I, Map<Word<I>, Location>> callLocations = new HashMap<>();
        for (I c: alphabet.getCallAlphabet()) {
            callLocations.put(c, new HashMap<>());
            for (Word<I> q: callTables.get(c).getQ()) {
                if (q.equals(Word.epsilon())) {
                    callLocations.get(c).put(
                            q, sevpa.addModuleEntryLocation(c, false));
                } else {
                    callLocations.get(c).put(
                            q, sevpa.addLocation(c, false));
                }
            }
        }

        for (Word<I> q: initialTable.getQ()) {
            Map<Word<I>,Word<I>> symbToSucc = initialTable.getSuccessorList(q);
            for (Map.Entry<Word<I>,Word<I>> entry: symbToSucc.entrySet()) {
                Word<I> symbol = entry.getKey();
                Word<I> successor = entry.getValue();
                if (symbol.size() == 1) {
                    sevpa.setInternalSuccessor(
                                    initialLocations.get(q),
                                    symbol.getSymbol(0),
                                    initialLocations.get(successor)
                                );
                } else {
                    I callSymbol = symbol.getSymbol(0);
                    Word<I> locationReturn = symbol.subWord(1, symbol.size()-1);
                    I returnSymbol = symbol.getSymbol(symbol.size()-1);
                    // System.out.println(callSymbol + "-" +  locationReturn + "-" + returnSymbol);
                    // System.out.println(callLocations.get(callSymbol).get(locationReturn));
                    // System.out.println(callLocations.get(callSymbol));
                    // System.out.println(initialLocations.get(successor));

                    sevpa.setReturnSuccessor(
                            callLocations.get(callSymbol).get(locationReturn),
                            returnSymbol,
                            sevpa.encodeStackSym(initialLocations.get(q), callSymbol),
                            initialLocations.get(successor)
                    );
                }
            }
        }

        for (I c: alphabet.getCallAlphabet()) {
            for (Word<I> q: callTables.get(c).getQ()) {
                Map<Word<I>,Word<I>> symbToSucc = callTables.get(c).getSuccessorList(q);
                for (Map.Entry<Word<I>,Word<I>> entry: symbToSucc.entrySet()) {
                    Word<I> symbol = entry.getKey();
                    Word<I> successor = entry.getValue();
                    if (symbol.size() == 1) {
                        sevpa.setInternalSuccessor(
                                callLocations.get(c).get(q),
                                symbol.getSymbol(0),
                                callLocations.get(c).get(successor)
                        );
                    } else {
                        I callSymbol = symbol.getSymbol(0);
                        Word<I> locationReturn = symbol.subWord(1, symbol.size()-1);
                        I returnSymbol = symbol.getSymbol(symbol.size()-1);
                        // System.out.println(callSymbol + " - " +  locationReturn + " - " + returnSymbol);

                        sevpa.setReturnSuccessor(
                                callLocations.get(callSymbol).get(locationReturn),
                                returnSymbol,
                                sevpa.encodeStackSym(callLocations.get(c).get(q), callSymbol),
                                callLocations.get(c).get(successor)
                        );
                    }
                }
            }
        }

        return sevpa;
    }

    @Override
    public boolean askMembershipQuery(Word<I> input) {
        numberOfMQ++;
        boolean answer = membershipOracle.answerQuery(input);
        return answer;
    }

    @Override
    public void displayStats() {
        System.out.println("Number of membership queries: " + numberOfMQ);
        System.out.println("Number of equivalence queries: " + numberOfEQ);
    }

    public void initialize() {
        initialTable.initialize();
        for (AbstractCallObservationTable<I, ?> callTable: callTables.values()) {
            callTable.initialize();
        }
    }

    public void enforceTables() {
        initialTable.enforce();
        for (AbstractCallObservationTable<I, ?> callTable: callTables.values()) {
            callTable.enforce();
        }
    }

    public void addSymbol(Word<I> q){
        System.out.println("Adding symbol for " + q);
        initialTable.addSymbol(q);
        for (AbstractCallObservationTable<I, ?> callTable: callTables.values()) {
            callTable.addSymbol(q);
        }
    }

    public int getNumberOfEQ() {
        return numberOfEQ;
    }

    public int getNumberOfMQ() {
        return numberOfMQ;
    }

    private void printTables(){
        System.out.println("Initial Table:");
        System.out.println(initialTable);

        System.out.println("Call tables:");
        for (I c: callTables.keySet()) {
            System.out.println("\t Tables linked to " + c);
            System.out.println(callTables.get(c));
        }
    }
}
