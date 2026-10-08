package umons.ac.be;

import be.ac.umons.jsonschematools.JSONSchema;
import be.ac.umons.jsonschematools.JSONSchemaException;
import be.ac.umons.jsonschematools.JSONSchemaStore;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.automaton.vpa.SEVPA;
import umons.ac.be.JSONOracle.*;
import umons.ac.be.JSONutils.JSONSymbol;
import umons.ac.be.learner.Learner;
import umons.ac.be.learner.VPATTT.VPATTTLearner;
import umons.ac.be.learner.VRALearner.VRAIsomorphicLearner;
import umons.ac.be.learner.VRALearner.VRASeparateLearner;
import umons.ac.be.learner.VRALearner.VRASeparateLearnerOptimized;
import umons.ac.be.learner.VStar.VStarLearner;
import umons.ac.be.vra.VRA;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static umons.ac.be.JSONutils.Utils.extractSymbolsFromSchema;

public class ExperimentRunner {
    ExperimentConfig config;

    public ExperimentRunner(String datasetFile, String algorithmFile) throws IOException {
        config = ConfigLoader.load(datasetFile, algorithmFile);
    }

    public void run() throws Exception {
        switch (config.datasets.type) {
            case JSON:
                runJSONExperiment(new Random(42));
                break;
        }
    }

    public void runJSONExperiment(Random random) throws Exception {
        final JSONSchema schema = getSchema(
                Paths.get("benchmarks\\benchmarks\\" + config.datasets.file)
        );
        final VPAlphabet<JSONSymbol> alphabet = extractSymbolsFromSchema(schema);
        final MembershipOracle<JSONSymbol, Boolean> membershipOracle = new JSONMembershipOracle(schema);
        final int numberTest = config.execution.testConformance;

        final AbstractJSONConformance<?> equivalenceOracle =
                switch (config.execution.equivalenceOracleType) {
                    case EXHAUSTIVE ->
                        switch (config.algorithm.automatonType) {
                            case VRA -> new VRAJSONEquivalenceOracleExhaustive(
                                    numberTest, true, 10, 2,
                                    schema, random, false, alphabet,
                                    10
                            );
                            case VPA_TTT, VPA_VSTAR -> new VPAJSONEquivalenceOracleExhaustive(
                                    numberTest, true, 10, 2,
                                    schema, random, false, alphabet,
                                    10
                            );
                        };
                    case RANDOM ->
                        switch (config.algorithm.automatonType) {
                                case VRA -> new VRAJSONEquivalenceOracleRandom(
                                        numberTest, true, 10, 2,
                                        schema, random, false, alphabet,
                                        10
                                );
                                case VPA_TTT, VPA_VSTAR -> new VPAJSONEquivalenceOracleRandom(
                                        numberTest, true, 10, 2,
                                        schema, random, false, alphabet,
                                        10
                                );
                            };
                };

        final Learner<JSONSymbol, ?> learner =
                switch (config.algorithm.automatonType) {
                    case VRA -> switch (config.algorithm.mode) {
                        case ISO -> new VRAIsomorphicLearner<>(
                                VRAlphabet.fromVPAlphabet(alphabet),
                                membershipOracle,
                                (EquivalenceOracle<VRA<FastDFAState, JSONSymbol, DFA<FastDFAState, JSONSymbol>>, JSONSymbol, Boolean>) equivalenceOracle);
                        case SEP -> new VRASeparateLearner<>(
                                VRAlphabet.fromVPAlphabet(alphabet),
                                membershipOracle,
                                (EquivalenceOracle<VRA<FastDFAState, JSONSymbol, DFA<FastDFAState, JSONSymbol>>, JSONSymbol, Boolean>) equivalenceOracle);
                        case SEP_OPT -> new VRASeparateLearnerOptimized<>(
                                VRAlphabet.fromVPAlphabet(alphabet),
                                membershipOracle,
                                (EquivalenceOracle<VRA<FastDFAState, JSONSymbol, DFA<FastDFAState, JSONSymbol>>, JSONSymbol, Boolean>) equivalenceOracle);
                    };
                    case VPA_TTT -> new VPATTTLearner<>(
                            alphabet,
                            (MembershipOracle.DFAMembershipOracle<JSONSymbol>) membershipOracle,
                            (EquivalenceOracle<SEVPA<?, JSONSymbol>, JSONSymbol, Boolean>) equivalenceOracle
                    );
                    case VPA_VSTAR -> new VStarLearner<>(
                            alphabet,
                            membershipOracle,
                            (EquivalenceOracle<SEVPA<?, JSONSymbol>, JSONSymbol, Boolean> ) equivalenceOracle
                    );
                };

        float startTime = System.nanoTime();
        learner.learn();
        float runtime = (System.nanoTime() - startTime) / 1_000_000_000;

        System.out.println("Total learning time : " + runtime+ " s");
        System.out.println("Time last EQ : " + equivalenceOracle.getTime() + " s");
        learner.displayStats();
    }

    public static JSONSchema getSchema(Path filePath) {
        final JSONSchemaStore schemaStore = new JSONSchemaStore(false);
        try {
            return schemaStore.load(filePath.toUri().toURL().toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
