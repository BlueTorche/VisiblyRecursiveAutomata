package umons.ac.be.test;

import learner.DFA.DefaultLStar;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.visualization.Visualization;
import oracle.rl.RLOracleWithRegex;

public class TestLearningDFA {
    public static void main(String[] args) {
        testLearningWithRegex();
    }

    private static void testLearningWithRegex() {
        Alphabet<String> alphabet = Alphabets.fromArray("a", "b");
        String[] regexTotest = {"(a|b)*a(a|b){2}a(a|b)*",
                "(a|b)*(ab){3}(a|b)*",
                "(a|b)*a(a|b)*b(a|b)*a(a|b)*b(a|b)*"
        };

        for (String regex : regexTotest) {
            RLOracleWithRegex<String> oracle = new RLOracleWithRegex<>(alphabet, regex);
            DefaultLStar<String> learner = new DefaultLStar<>(alphabet, oracle);
            FastDFA<String> dfa = learner.learn();
            Visualization.visualize(dfa);
        }
    }
}
