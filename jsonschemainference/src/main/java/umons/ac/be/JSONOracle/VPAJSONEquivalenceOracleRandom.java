package umons.ac.be.JSONOracle;

import be.ac.umons.jsonschematools.JSONSchema;
import de.learnlib.oracle.EquivalenceOracle;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.vpa.OneSEVPA;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.word.Word;
import umons.ac.be.JSONutils.JSONSymbol;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class VPAJSONEquivalenceOracleRandom
        extends AbstractJSONConformanceRandom<SEVPA<?, JSONSymbol>>
        implements EquivalenceOracle<SEVPA<?, JSONSymbol>, JSONSymbol, Boolean> {

    public VPAJSONEquivalenceOracleRandom(int numberTests, boolean canGenerateInvalid, int maxProperties, int maxItems,
                                              JSONSchema schema, Random random, boolean shuffleKeys, VPAlphabet<JSONSymbol> alphabet,
                                              int maxDocumentDepth, Set<Word<JSONSymbol>> documentsToTest) {
        super(numberTests, canGenerateInvalid, maxProperties, maxItems,
                schema, random, shuffleKeys, alphabet,
                maxDocumentDepth, documentsToTest);
    }
}