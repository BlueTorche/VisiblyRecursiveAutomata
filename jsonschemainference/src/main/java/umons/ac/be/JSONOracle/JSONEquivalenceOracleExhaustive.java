package umons.ac.be.JSONOracle;

import be.ac.umons.jsonschematools.JSONSchema;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.word.Word;
import umons.ac.be.JSONutils.JSONSymbol;

import java.util.Random;
import java.util.Set;

public class JSONEquivalenceOracleExhaustive
        extends AbstractJSONConformanceExhaustive {

    public JSONEquivalenceOracleExhaustive(int numberTests, boolean canGenerateInvalid, int maxProperties, int maxItems,
                                           JSONSchema schema, Random random, boolean shuffleKeys, VPAlphabet<JSONSymbol> alphabet,
                                           int maxDocumentDepth, Set<Word<JSONSymbol>> documentsToTest) {
        super(numberTests, canGenerateInvalid, maxProperties, maxItems,
                schema, random, shuffleKeys, alphabet,
                maxDocumentDepth, documentsToTest);
    }
}