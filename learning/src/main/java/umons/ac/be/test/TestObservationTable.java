package umons.ac.be.test;

import learner.DFA.AbstractLStar;
import learner.DFA.DefaultLStar;
import learner.ObservationTable.ObservationTable;
import learner.ObservationTable.RegularObservationTable;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.visualization.Visualization;
import net.automatalib.word.Word;
import oracle.rl.RLOracleWithRegex;

public class TestObservationTable {
    public static void main(String[] args) {
        testObservationTable();
        testLStar();
    }

    private static void testObservationTable() {
        GrowingAlphabet<String> alphabet = new GrowingMapAlphabet<>();
        alphabet.add("a");
        alphabet.add("b");
        String[] regexTotest = {"(a|b)*a(a|b){2}c(a|b)*",
                "(a|b)*(ab){3}(a|b|c)*",
                "(a|b|c)*a(a|b)*b(a|b)*a(a|b)*b(a|b|c)*"
        };

        for (String regex : regexTotest) {
            RLOracleWithRegex<String> oracle = new RLOracleWithRegex<>(alphabet, regex);
            AbstractLStar<String> learner = new DefaultLStar<>(alphabet, oracle);

            RegularObservationTable<String> table = new RegularObservationTable<>(alphabet, learner);
            table.initialize();
            System.out.println(table);
            table.addRepresentative(Word.fromWords(Word.fromLetter("a"), Word.fromLetter("b"), Word.fromLetter("a")));
            System.out.println(table);
            table.enforce();
            System.out.println(table);
            table.addSymbol("c");
            System.out.println(table);
            table.enforce();
            System.out.println(table);
            System.out.println("----------------");
        }
    }

    private static void testLStar() {
        GrowingAlphabet<String> alphabet = new GrowingMapAlphabet<>();
        alphabet.add("a");
        alphabet.add("b");
        String[] regexTotest = {"(a|b)*a(a|b){2}a(a|b)*",
                "(a|b)*(ab){3}(a|b)*",
                "(a|b)*a(a|b)*b(a|b)*a(a|b)*b(a|b)*"
        };

        for (String regex : regexTotest) {
            RLOracleWithRegex<String> oracle = new RLOracleWithRegex<>(alphabet, regex);
            AbstractLStar<String> learner = new DefaultLStar<>(alphabet, oracle);
            FastDFA<String> dfa = learner.learn();
            Visualization.visualize(dfa);
        }
    }
}
