package umons.ac.be.test;


import umons.ac.be.dfa.*;
import umons.ac.be.dfa.DFAState;
import umons.ac.be.dfa.DeterminsticFiniteAutomaton;

public class testFiniteAutomaton {
    static boolean DEBUG = true;

    public static void main(String[] args) {
        System.out.println("DFA :");
        testDFA();
    }

    private static void testDFA() {
        // États
        DFAState q0 = new DFAState("q0", false);
        DFAState q1 = new DFAState("q1", true);
        DFAState q2 = new DFAState("q2", false);
        DFAState q3 = new DFAState("q3", true);

        // Transitions
        q0.addTransition("a", q2);
        q0.addTransition("b", q1);
        q1.addTransition("a", q2);
        q1.addTransition("b", q1);
        q2.addTransition("a", q2);
        q2.addTransition("b", q3);
        q3.addTransition("a", q2);
        q3.addTransition("b", q3);


        // Définition du NFA
        DeterminsticFiniteAutomaton<DFAState> dfa = new DeterminsticFiniteAutomaton<>();
        dfa.setInitialState(q0);

        // Test - L(dfa) = Suff(w) = "b"
        Word[] acceptedWords = {
                Word.of("a", "b"), Word.of("a", "a", "a" , "b"),
                Word.of("b"), Word.of("b", "b"),
                Word.of("a", "a", "b"), Word.of("a", "a", "b", "b")
        };
        Word[] rejectedWords = {
                Word.of(""), Word.of("a"),
                Word.of("b", "a", "a"), Word.of("b", "a", "b", "a")};

        System.out.println("L(dfa) = {w | \"b\" in Suff(w) }");
        for (Word w : acceptedWords) {
            System.out.println("Input accepted: " + w + " -> " + (dfa.accepts(w, testFiniteAutomaton.DEBUG) ? "ACCEPTED" : "REJECTED"));
        }
        for (Word w : rejectedWords) {
            System.out.println("Input rejected: " + w + " -> " + (dfa.accepts(w, testFiniteAutomaton.DEBUG) ? "ACCEPTED" : "REJECTED"));
        }
    }
}
