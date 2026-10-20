package umons.ac.be.oracle.vpl;

import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.impl.DefaultVPAlphabet;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.visualization.Visualization;
import net.automatalib.word.Word;
import umons.ac.be.oracle.CounterMembershipOracle;
import umons.ac.be.vra.AbstractVRAwithDFA;
import umons.ac.be.vra.DefaultVRAwithDFA;
import umons.ac.be.vra.VRAAlphabet;
import umons.ac.be.vraalphabet.AbstractVRAlphabet;
import umons.ac.be.vraalphabet.DefaultVRAlphabet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BestCaseForVRAMembership  implements CounterMembershipOracle<String> {
    AbstractVRAwithDFA<FastDFAState, String> vra;
    int numberOfMQ = 0;

    public BestCaseForVRAMembership(VPAlphabet<String> alphabet, int n) {
        assert n > 0;

        // Creation of the alphabets
        AbstractVRAlphabet<String> vraAlphabet = new DefaultVRAlphabet<>(alphabet);
        for (String intSymbol: alphabet.getInternalAlphabet()) {
            vraAlphabet.addProceduralSymbol(
                    "J" + intSymbol,
                    alphabet.getCallSymbol(0),
                    alphabet.getReturnSymbol(0)
            );
            vraAlphabet.addProceduralSymbol(
                    "J" + intSymbol + "prime",
                    alphabet.getCallSymbol(0),
                    alphabet.getReturnSymbol(0)
            );
        }
        // Creation of the procedures
        HashMap<String, FastDFA<String>> procedures = new HashMap<>();
        for (String intSymbol: alphabet.getInternalAlphabet()) {
            // -------- Li ----------------//
            {
                String procSymbol = "J" + intSymbol;
                procedures.put(procSymbol, new FastDFA<>(vraAlphabet.getAutomatonAlphabet()));

                // Define states
                List<FastDFAState> states = new ArrayList<>();
                for (int i = 0; i < n + 1; i++) {
                    states.add(procedures.get(procSymbol).addState(i == n));
                }
                procedures.get(procSymbol).setInitialState(states.get(0));

                // define transitions
                for (int i = 0; i < n; i++) {
                    for (String intSymbol2 : alphabet.getInternalAlphabet()) {
                        if (intSymbol2.equals(intSymbol)) {
                            procedures.get(procSymbol).addTransition(
                                    states.get(i),
                                    intSymbol,
                                    states.get((i + 1))
                            );
                            continue;
                        }
                        procedures.get(procSymbol).addTransition(
                                states.get(i),
                                intSymbol2,
                                states.get(i));
                    }
                }

                for (String intSymbol2 : alphabet.getInternalAlphabet()) {
                    if (intSymbol2.equals(intSymbol)) {
                        procedures.get(procSymbol).addTransition(
                                states.get(n),
                                intSymbol,
                                states.get((1))
                        );
                        continue;
                    }
                    procedures.get(procSymbol).addTransition(
                            states.get(n),
                            intSymbol2,
                            states.get(0));
                }
            }


            // -------- Li' ----------------//
            {
                String procSymbol = "J" + intSymbol + "prime";
                procedures.put(procSymbol, new FastDFA<>(vraAlphabet.getAutomatonAlphabet()));

                // Define states
                List<FastDFAState> states = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    states.add(procedures.get(procSymbol).addState(i != 0));
                    states.add(procedures.get(procSymbol).addState(false));
                }
                procedures.get(procSymbol).setInitialState(states.getFirst());

                for (int i = 0; i < n; i++) {
                    for (String intSymbol2 : alphabet.getInternalAlphabet()) {
                        if (intSymbol2.equals(intSymbol)) {
                            procedures.get(procSymbol).setTransition(
                                    states.get(2*i),
                                    intSymbol,
                                    states.get((2*i+2)%(n*2))
                            );
                            procedures.get(procSymbol).setTransition(
                                    states.get(2*i+1),
                                    intSymbol,
                                    states.get((2*i+2)%(n*2))
                            );
                            continue;
                        }
                        procedures.get(procSymbol).setTransition(
                                states.get(2*i),
                                intSymbol2,
                                states.get(2*i+1)
                        );
                        procedures.get(procSymbol).setTransition(
                                states.get(2*i+1),
                                intSymbol2,
                                states.get(2*i+1)
                        );
                    }
                }
            }
        }

        FastDFA<String> startingAutomaton = new FastDFA<>(vraAlphabet.getAutomatonAlphabet());
        FastDFAState initialState = startingAutomaton.addInitialState();
        FastDFAState finalState = startingAutomaton.addState(true);
        for (String intSymbol: alphabet.getInternalAlphabet()) {
            String procSymbol = "J" + intSymbol;
            String procSymbolPrime = "J" + intSymbol + "prime";

            FastDFAState s1 = startingAutomaton.addState(false);
            FastDFAState s2 = startingAutomaton.addState(false);

            startingAutomaton.addTransition(
                    initialState, intSymbol, s1
            );
            startingAutomaton.addTransition(
                    s1, procSymbol, finalState
            );
            startingAutomaton.addTransition(
                    initialState, procSymbolPrime, s2
            );
            startingAutomaton.addTransition(
                    s2, intSymbol, finalState
            );
        }

        vra = new DefaultVRAwithDFA<>(
                vraAlphabet,
                procedures,
                startingAutomaton
        );
    }

    public void visualize() {
        Visualization.visualize(vra);
    }

    @Override
    public int getNumberOfMQ() {
        return numberOfMQ;
    }

    @Override
    public Boolean answerQuery(Word<String> word, Word<String> word1) {
        numberOfMQ++;
        return vra.accepts(Word.fromWords(word, word1));
    }

    @Override
    public Boolean answerQuery(Word<String> word) {
        numberOfMQ++;
        return vra.accepts(word);
    }

    public AbstractVRAwithDFA<FastDFAState, String> getVRA() {
        return vra;
    }
}
