package umons.ac.be.test;

import be.ac.umons.jsonschematools.JSONSchema;
import be.ac.umons.jsonschematools.JSONSchemaException;
import be.ac.umons.jsonschematools.JSONSchemaStore;
import de.learnlib.acex.AcexAnalyzers;
import de.learnlib.algorithm.ttt.vpa.TTTLearnerVPA;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.DefaultQuery;
import net.automatalib.automaton.vpa.OneSEVPA;
import umons.ac.be.JSONOracle.*;
import umons.ac.be.learner.VRALearner.AbstractVRALearner;
import umons.ac.be.learner.VRALearner.VRAIsomorphicLearner;
import umons.ac.be.learner.VRALearner.VRASeparateLearner;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.visualization.Visualization;
import umons.ac.be.JSONutils.JSONSymbol;
import umons.ac.be.learner.VRALearner.VRASeparateLearnerOptimized;
import umons.ac.be.vra.VRA;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static umons.ac.be.JSONutils.Utils.extractSymbolsFromSchema;

public class testJSONLearning {
    enum EquivalenceOracleType {
        EXPLORATION,
        RANDOM
    }


    private static final boolean VISUALIZATION = true;
    private static final TestLearningVRA.LearnerType LEARNER_TYPE =
            TestLearningVRA.LearnerType.SEPARATE_LEARNER_OPTIMIZED;
    private static final int numberExperiment = 1;
    private static final int schemaIndex = 2;
    private static final boolean VPA = false;

    private static float totalTime = 0;
    private static int totalMQ = 0;
    private static int totalEQ = 0;

    public static void main(String[] args) throws JSONSchemaException, InterruptedException {
        String schema = new String[]{"recursiveList", "basicTypes", "vscode", "vim", "proxies", "codecov"}[schemaIndex];
        Path filePath = Paths.get(
//                "C:\\Users\\dubru\\IdeaProjects\\ValidatingJSONDocumentsWithLearnedVPA-main\\schemas\\benchmarks\\"
                "D:\\TFE2-code-gaetan\\ValidatingJSONDocumentsWithLearnedVPA\\schemas\\benchmarks\\"
                        + schema + "\\" + schema + ".json"
        );
        EquivalenceOracleType equivalenceOracleType = schemaIndex < 3 ? EquivalenceOracleType.EXPLORATION : EquivalenceOracleType.RANDOM;
//        testJSONLearning(filePath, ISOMORPHIC_LEARNER);
//        testJSONLearning(filePath, SEPARATE_LEARNER);
        for (int i = 0; i < numberExperiment; i++) {
            System.out.println("Starting Experiment " + i);
            System.gc();
            TimeUnit.SECONDS.sleep(1);
            if (VPA) {
                testJSONVPALearning(filePath, equivalenceOracleType, new Random(i));
            } else {
                testJSONVRALearning(filePath, equivalenceOracleType, new Random(i));
            }
        }


        System.out.println("Mean time: " + totalTime/numberExperiment);
        System.out.println("Mean MQ: " + ((float) totalMQ)/numberExperiment);
        System.out.println("Mean EQ: " + ((float) totalEQ)/numberExperiment);
    }

    private static void testJSONVRALearning(Path filePath,
                                            EquivalenceOracleType equivalenceOracleType,
                                            Random random) throws JSONSchemaException {
        final JSONSchema schema = getSchema(filePath);
        final VPAlphabet<JSONSymbol> alphabet = extractSymbolsFromSchema(schema);
        final MembershipOracle<JSONSymbol, Boolean> membershipOracle = new JSONMembershipOracle(schema);
        final int numberTest = schema.toString().length() + 500;
        System.out.println("Number of tests: " + numberTest);
        final EquivalenceOracle<VRA<FastDFAState, JSONSymbol, DFA<FastDFAState, JSONSymbol>>, JSONSymbol, Boolean> equivalenceOracle =
                switch (equivalenceOracleType) {
                    case EXPLORATION -> new VRAJSONEquivalenceOracleExhaustive(
                            numberTest, true, 10, 2,
                            schema, random, false, alphabet,
                            10
                    );
                    case RANDOM -> new VRAJSONEquivalenceOracleRandom(
                            numberTest, true, 10, 2,
                            schema, random, false, alphabet,
                            10
                    );
                };


        final AbstractVRALearner<JSONSymbol> learner = switch (LEARNER_TYPE) {
            case ISOMORPHIC_LEARNER -> new VRAIsomorphicLearner<>(
                    VRAlphabet.fromVPAlphabet(alphabet), membershipOracle, equivalenceOracle);
            case SEPARATE_LEARNER -> new VRASeparateLearner<>(
                    VRAlphabet.fromVPAlphabet(alphabet), membershipOracle, equivalenceOracle);
            case SEPARATE_LEARNER_OPTIMIZED -> new VRASeparateLearnerOptimized<>(
                    VRAlphabet.fromVPAlphabet(alphabet), membershipOracle, equivalenceOracle);
        };


        float startTime = System.nanoTime();
        VRA<FastDFAState, JSONSymbol, DFA<FastDFAState, JSONSymbol>> hypo = learner.learn();
        float runtime = (System.nanoTime() - startTime) / 1_000_000_000;
        System.out.println("Total learning time : " + runtime+ " s");
        learner.displayStats();

        if (testJSONLearning.VISUALIZATION) {
//            Visualization.visualize(hypo.removeBinStatesAndAutomata());
            hypo.removeBinStatesAndAutomata().visualizeIndividually();
        }
        totalTime += runtime;
        totalMQ += learner.getNumberMQ();
        totalEQ += learner.getNumberEQ();

        Path parentFolder = filePath.getParent();

        // Format : jourheure_schema_vra_learning_results.txt
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String dateTime = LocalDateTime.now().format(formatter);

        // Récupération de "schema" depuis "schema.json"
        String schemaName = filePath.getFileName().toString()
                .replaceFirst("\\.json$", "");

        Path resultsFolder = parentFolder.resolve("VRA_learning_results");

        Path resultsFile = resultsFolder.resolve(
                dateTime + "_" + schemaName + "_vra_learning_results.txt"
        );

        saveContent(resultsFile, "Total learning time : " + runtime+ " s\n" + learner);
        System.out.println("Saved results to: " + resultsFile);
    }


    private static void testJSONVPALearning(Path filePath,
                                            EquivalenceOracleType equivalenceOracleType,
                                            Random random)
            throws JSONSchemaException {
        final JSONSchema schema = getSchema(filePath);
        final VPAlphabet<JSONSymbol> alphabet = extractSymbolsFromSchema(schema);

        final JSONMembershipOracle membershipOracle = new JSONMembershipOracle(schema);
//        final CounterOracle<JSONSymbol, Boolean> membershipOracle = new CounterOracle<>(sul, "membership queries");

        final int numberTest = schema.toString().length() + 500;
        final EquivalenceOracle<OneSEVPA<?, JSONSymbol>, JSONSymbol, Boolean> equivalenceOracle =
                switch (equivalenceOracleType) {
                    case EXPLORATION -> new VPAJSONEquivalenceOracleExhaustive(
                            numberTest, true, 10, 2,
                            schema, random, false, alphabet,
                            10
                    );
                    case RANDOM -> new VPAJSONEquivalenceOracleRandom(
                            numberTest, true, 10, 2,
                            schema, random, false, alphabet,
                            10
                    );
                };
//        final CounterEQOracle<OneSEVPA<?, JSONSymbol>, JSONSymbol, Boolean> equivalenceOracle = new CounterEQOracle<>(
//                eqOracle, "equivalence queries");

        float startTime = System.nanoTime();

        final TTTLearnerVPA<JSONSymbol> learner = new TTTLearnerVPA<>(alphabet,
                membershipOracle,
                AcexAnalyzers.LINEAR_FWD);

        learner.startLearning();
        OneSEVPA<?, JSONSymbol> hypo;
        while (true) {
            hypo = learner.getHypothesisModel();
            totalEQ ++;

            System.out.println("Searching for counterexample " + totalEQ);

            DefaultQuery<JSONSymbol, Boolean> ce = equivalenceOracle.findCounterExample(hypo, new HashSet<>());

            System.out.println("Found counterexample, processing...");

            if (ce == null) {
                break;
            }
            final boolean refined = learner.refineHypothesis(ce);
            assert refined;
        }

        float runtime = (System.nanoTime() - startTime) / 1_000_000_000;
        System.out.println("Total learning time : " + runtime+ " s");
        totalTime += runtime;
        totalMQ += membershipOracle.getNumberOfMQ();

        if (testJSONLearning.VISUALIZATION) {
            Visualization.visualize(hypo);
        }
//        Path parentFolder = filePath.getParent();
//
//        // Format : jourheure_schema_vra_learning_results.txt
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
//        String dateTime = LocalDateTime.now().format(formatter);
//
//        // Récupération de "schema" depuis "schema.json"
//        String schemaName = filePath.getFileName().toString()
//                .replaceFirst("\\.json$", "");
//
//        Path resultsFolder = parentFolder.resolve("VRA_learning_results");
//
//        Path resultsFile = resultsFolder.resolve(
//                dateTime + "_" + schemaName + "_vpa_learning_results.txt"
//        );
//
//        saveContent(resultsFile, "Total learning time : " + runtime+ " s\n" + learner.toString());
//        System.out.println("Saved results to: " + resultsFile);
    }

    public static JSONSchema getSchema(Path filePath) {
        final JSONSchemaStore schemaStore = new JSONSchemaStore(false);
        try {
            return schemaStore.load(filePath.toUri().toURL().toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveContent(Path filepath, String content) {
        try {
            Path parent = filepath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(filepath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'écrire dans le fichier : " + filepath, e);
        }
    }
}
