package umons.ac.be.JSONOracle;

import be.ac.umons.jsonschematools.JSONSchema;
import de.learnlib.oracle.EquivalenceOracle;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import umons.ac.be.JSONutils.JSONSymbol;
import umons.ac.be.vra.VRA;

import java.util.HashSet;
import java.util.Random;

public class VRAJSONEquivalenceOracleRandom
        extends AbstractJSONConformanceRandom<VRA<FastDFAState, JSONSymbol, DFA<FastDFAState, JSONSymbol>>>
        implements EquivalenceOracle<VRA<FastDFAState, JSONSymbol, DFA<FastDFAState, JSONSymbol>>, JSONSymbol, Boolean> {
    public VRAJSONEquivalenceOracleRandom(int numberTests, boolean canGenerateInvalid, int maxProperties, int maxItems,
                                          JSONSchema schema, Random random, boolean shuffleKeys, VPAlphabet<JSONSymbol> alphabet,
                                          int maxDocumentDepth) {
        super(numberTests, canGenerateInvalid, maxProperties, maxItems, schema, random, shuffleKeys,
                alphabet, maxDocumentDepth, new HashSet<>());
    }
}