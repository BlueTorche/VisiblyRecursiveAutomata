package umons.ac.be.test;

import umons.ac.be.vra.ProceduralAutomaton;
import umons.ac.be.vra.VRAAlphabet;
import umons.ac.be.vra.VRAState;
import umons.ac.be.vra.VisiblyRecursiveAutomaton;
import umons.ac.be.vra.*;

public class testVRA {

    public static void main(String[] args) {
        //long start_memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024;
        testVSPAWithGeneric();
        //System.out.println("Memory used by testVSPAWithGeneric: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 - start_memory) + " KB");
        // long start_memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024;
        testVSPAWithJSON();
        // System.out.println("Memory used by testVSPAWithJSON: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 - start_memory) + " KB");
    }

    private static void testVSPAWithGeneric() {
        // Alphabet
        VRAAlphabet alphabet = new VRAAlphabet();
        alphabet.addCallSymbol("a");
        alphabet.addReturnSymbol("z");
        alphabet.addInternalSymbol("b");

        // États
        VRAState qS0 = new VRAState("qS0", true);
        VRAState qR0 = new VRAState("qR0", false);
        VRAState qR1 = new VRAState("qR1", true);

        // Transitions
        qS0.addTransition("R", qS0);
        qS0.addTransition("S", qS0);
        qR0.addTransition("b", qR1);

        // Définition du Procedural Automaton
        ProceduralAutomaton S = new ProceduralAutomaton("S", qS0);
        ProceduralAutomaton R = new ProceduralAutomaton("R", qR0);

        // Definition VRA
        VisiblyRecursiveAutomaton vra = new VisiblyRecursiveAutomaton(alphabet);
        vra.addProceduralAutomaton(S, "a", "z");
        vra.addProceduralAutomaton(R, "a", "z");
        vra.setStartingAutomaton(S);

        // Test - L(VRA) == grammar : S -> (S'+R)* ; S' -> a(S'+R)*z ; R -> abz
        String[] words = {"a", "abz", "ab", "aabzz", "az", "aabzazz", "aabazzz", "aaz", "b", "aazz", "aabzazz", "aaaabzabzzabzzz", "aaabzabzzabzzz"};
        for (String w : words) {
            System.out.println("Input: " + w + " -> " + (vra.accepts(w) ? "ACCEPTED" : "REJECTED"));
        }
    }


    private static void testVSPAWithJSON() {
        // Alphabet
        VRAAlphabet alphabet = new VRAAlphabet();
        alphabet.addCallSymbol("[");
        alphabet.addReturnSymbol("]");
        alphabet.addCallSymbol("{");
        alphabet.addReturnSymbol("}");
        alphabet.addInternalSymbol("a"); // Keys of JSON document are replaced by "a", "b", "c", "d", "e"
        alphabet.addInternalSymbol("b");
        alphabet.addInternalSymbol("c");
        alphabet.addInternalSymbol("d");
        alphabet.addInternalSymbol("e");
        alphabet.addInternalSymbol("s");
        alphabet.addInternalSymbol("i");
        alphabet.addInternalSymbol("n");
        alphabet.addInternalSymbol("#");


        // Procedural Automaton A^S
        VRAState q0 = new VRAState("q0", false);
        VRAState q1 = new VRAState("q1", true);

        q0.addTransition("S0", q1);

        ProceduralAutomaton S = new ProceduralAutomaton("S", q0);

        // Procedural Automaton A^S0
        VRAState q00 = new VRAState("q00", false);
        VRAState q01 = new VRAState("q01", false);
        VRAState q02 = new VRAState("q02", false);
        VRAState q03 = new VRAState("q03", false);
        VRAState q04 = new VRAState("q04", false);
        VRAState q05 = new VRAState("q05", false);
        VRAState q06 = new VRAState("q06", false);
        VRAState q07 = new VRAState("q07", false);
        VRAState q08 = new VRAState("q08", true);

        q00.addTransition("a", q01);
        q01.addTransition("s", q02);
        q02.addTransition("#", q03);
        q03.addTransition("b", q04);
        q04.addTransition("S1", q05);
        q05.addTransition("#", q06);
        q06.addTransition("c", q07);
        q07.addTransition("S2", q08);

        ProceduralAutomaton S0 = new ProceduralAutomaton("S0", q00);


        // Procedural Automaton A^S1
        VRAState q10 = new VRAState("q10", true);
        VRAState q11 = new VRAState("q11", true);
        VRAState q12 = new VRAState("q12", false);

        q10.addTransition("s", q11);
        q11.addTransition("#", q12);
        q12.addTransition("s", q11);

        ProceduralAutomaton S1 = new ProceduralAutomaton("S1", q10);


        // Procedural Automaton A^S2
        VRAState q20 = new VRAState("q20", true);
        VRAState q21 = new VRAState("q21", false);
        VRAState q22 = new VRAState("q22", false);
        VRAState q23 = new VRAState("q23", false);
        VRAState q24 = new VRAState("q24", false);
        VRAState q25 = new VRAState("q25", true);
        VRAState q26 = new VRAState("q26", false);
        VRAState q27 = new VRAState("q27", true);
        VRAState q28 = new VRAState("q28", false);
        VRAState q29 = new VRAState("q29", true);

        q20.addTransition("d", q21);
        q21.addTransition("n", q22);
        q22.addTransition("#", q23);
        q23.addTransition("e", q24);
        q24.addTransition("i", q25);

        q20.addTransition("d", q26);
        q26.addTransition("n", q27);

        q20.addTransition("e", q28);
        q28.addTransition("i", q29);

        ProceduralAutomaton S2 = new ProceduralAutomaton("S2", q20);


        // Definition VSPA
        VisiblyRecursiveAutomaton vra = new VisiblyRecursiveAutomaton(alphabet);
        vra.addProceduralAutomaton(S0, "{", "}");
        vra.addProceduralAutomaton(S1, "[", "]");
        vra.addProceduralAutomaton(S2, "{", "}");
        vra.setStartingAutomaton(S);


        // Test - L(VPA) == grammar : S -> a(S+R)*z  ; R -> abz
        String[] words = {"{}", "{as#b[s#s#s]#c{dn#ei}}", "{b[s#s#s]#c{dn#ei}}",
                "{as#b[s#s#s]#c{}}", "{as#b[s#s#s#s#s#s#s]#c{}}", "{as#b[}#c{}}", "{as#b[]#c{}}",
                "{as#b[s#s#]#c{}}", "{as#b[s#s#s#s]#c{dn}}", "{as#b[s#s#s#s]#c{ei}}",
                "[s#s]", "{as#b[s]#c{}}"
        };
        for (String w : words) {
            System.out.println("Input: " + w + " -> " + (vra.accepts(w) ? "ACCEPTED" : "REJECTED"));
        }
    }
}
