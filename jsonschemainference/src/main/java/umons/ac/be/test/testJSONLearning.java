package umons.ac.be.test;

import be.ac.umons.jsonschematools.JSONSchema;
import be.ac.umons.jsonschematools.JSONSchemaException;
import be.ac.umons.jsonschematools.JSONSchemaStore;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import umons.ac.be.JSONOracle.JSONMembershipOracle;
import umons.ac.be.JSONOracle.VRAJSONEquivalenceOracle;
import umons.ac.be.JSONutils.JSONSymbol;
import umons.ac.be.vra.VRA;

import java.nio.file.Path;
import java.util.Random;

import static umons.ac.be.JSONutils.Utils.extractSymbolsFromSchema;

public class testJSONLearning {
    public static void main(String[] args) {

    }

    private static void testJSONLearning(Path filePath) throws JSONSchemaException {
        final JSONSchema schema = getSchema(filePath);
        final VPAlphabet<JSONSymbol> alphabet = extractSymbolsFromSchema(schema);
        final MembershipOracle<JSONSymbol, Boolean> membershipOracle = new JSONMembershipOracle(schema);
        final EquivalenceOracle<?, JSONSymbol, Boolean> equivalenceOracle = new VRAJSONEquivalenceOracle(
                10000, true, 10, 10, 2,
                schema, new Random(42), false, alphabet);
        // todo define learner and learn
    }

    private static JSONSchema getSchema(Path filePath) {
        final JSONSchemaStore schemaStore = new JSONSchemaStore(false);
        try {
            return schemaStore.load(filePath.toUri().toURL().toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
