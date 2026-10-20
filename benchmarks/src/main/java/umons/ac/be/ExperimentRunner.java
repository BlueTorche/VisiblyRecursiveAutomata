package umons.ac.be;

import be.ac.umons.jsonschematools.JSONSchema;
import be.ac.umons.jsonschematools.JSONSchemaStore;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.MembershipOracle;
import net.automatalib.alphabet.GrowingAlphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.alphabet.impl.DefaultVPAlphabet;
import net.automatalib.alphabet.impl.GrowingMapAlphabet;
import net.automatalib.automaton.concept.FiniteRepresentation;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.FastDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.automaton.vpa.OneSEVPA;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.automaton.vpa.impl.DefaultNSEVPA;
import net.automatalib.automaton.vpa.impl.DefaultOneSEVPA;
import net.automatalib.automaton.vpa.impl.Location;
import net.automatalib.common.util.Pair;
import net.automatalib.graph.concept.GraphViewable;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.visualization.Visualization;
import net.automatalib.word.Word;
import org.json.JSONObject;
import umons.ac.be.JSONOracle.*;
import umons.ac.be.JSONutils.JSONSymbol;
import umons.ac.be.JSONutils.Utils;
import umons.ac.be.JSONutils.WordConversion;
import umons.ac.be.learner.Learner;
import umons.ac.be.learner.VPATTT.VPATTTLearner;
import umons.ac.be.learner.VRALearner.VRAIsomorphicLearner;
import umons.ac.be.learner.VRALearner.VRASeparateLearner;
import umons.ac.be.learner.VRALearner.VRASeparateLearnerOptimized;
import umons.ac.be.learner.VStar.AbstractVStarLearner;
import umons.ac.be.learner.VStar.VStarLearner;
import umons.ac.be.oracle.CounterMembershipOracle;
import umons.ac.be.oracle.vpl.BestCaseForVRAConformance;
import umons.ac.be.oracle.vpl.BestCaseForVRAMembership;
import umons.ac.be.vra.VRA;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static umons.ac.be.JSONutils.Utils.extractSymbolsFromSchema;

public class ExperimentRunner {
    ExperimentConfig config;

    public ExperimentRunner(String datasetFile, String algorithmFile) throws IOException {
        config = ConfigLoader.load(datasetFile, algorithmFile);
    }

    public void run() throws Exception {
        for (int i = 0; i < config.execution.warmupRuns; i++) {
            switch (config.datasets.type) {
                case JSON:
                    runJSONExperiment(new Random(69420+i), null);
                    break;
                case BEST_CASE:
                    runBestCaseExperiment(new Random(69420+i), null);
                    break;
            }
        }

        // Récupération de "schema" depuis "schema.json"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String dateTime = LocalDateTime.now().format(formatter);

        Path filePath =  switch (config.datasets.type) {
            case JSON ->
                Path.of(config.datasets.directory + "results/" +
                        dateTime + "_" + config.datasets.file + "_" + config.algorithm.automatonType + "_results.csv");
            case BEST_CASE ->
                Path.of(config.datasets.directory + "results/" +
                        dateTime + "_BestCase" + config.datasets.file + "_" + config.algorithm.automatonType + "_results.csv");
        };
        ExperimentCSVLogger logger =  new ExperimentCSVLogger(filePath);

        try {
            for (int i = 0; i < config.execution.repetitions; i++) {
                System.gc();
                TimeUnit.SECONDS.sleep(1);

                switch (config.datasets.type) {
                    case JSON:
                        runJSONExperiment(new Random(config.execution.seed + i), logger);
                        break;
                    case BEST_CASE:
                        runBestCaseExperiment(new Random(config.execution.seed + i), logger);
                        break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.close();
    }

    public void runJSONExperiment(Random random, ExperimentCSVLogger csvLogger) throws Exception {
        final JSONSchema schema = getSchema(
                Paths.get(config.datasets.directory + config.datasets.file)
        );
        final VPAlphabet<JSONSymbol> alphabet = extractSymbolsFromSchema(schema);
        final CounterMembershipOracle<JSONSymbol> membershipOracle = new JSONMembershipOracle(schema);
        final int numberTest = config.execution.testConformance;

        Set<Word<JSONSymbol>> documentsToTest = new HashSet<>();
        for (String s: config.datasets.getProblematicWords()) {
            documentsToTest.add(WordConversion.fromJSONDocumentToJSONSymbolWord(
                    new JSONObject(Utils.escapeSymbolsForJSON(s))
            ));
        }

        final AbstractJSONConformance<?> equivalenceOracle =
                switch (config.execution.equivalenceOracleType) {
                    case EXHAUSTIVE ->
                        switch (config.algorithm.automatonType) {
                            case VRA -> new VRAJSONEquivalenceOracleExhaustive(
                                    numberTest, true, 10, 2,
                                    schema, random, false, alphabet,
                                    10, documentsToTest
                            );
                            case VPA_TTT, VPA_VSTAR -> new VPAJSONEquivalenceOracleExhaustive(
                                    numberTest, true, 10, 2,
                                    schema, random, false, alphabet,
                                    10, documentsToTest
                            );
                        };
                    case RANDOM ->
                        switch (config.algorithm.automatonType) {
                                case VRA -> new VRAJSONEquivalenceOracleRandom(
                                        numberTest, true, 10, 2,
                                        schema, random, false, alphabet,
                                        10, documentsToTest
                                );
                                case VPA_TTT, VPA_VSTAR -> new VPAJSONEquivalenceOracleRandom(
                                        numberTest, true, 10, 2,
                                        schema, random, false, alphabet,
                                        10, documentsToTest
                                );
                            };
                };

        final Learner<JSONSymbol, ? extends DeterministicAcceptorTS<?, JSONSymbol>> learner =
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
                            membershipOracle,
                            (EquivalenceOracle<SEVPA<?, JSONSymbol>, JSONSymbol, Boolean>) equivalenceOracle
                    );
                    case VPA_VSTAR -> new VStarLearner<>(
                            alphabet,
                            membershipOracle,
                            (EquivalenceOracle<SEVPA<?, JSONSymbol>, JSONSymbol, Boolean> ) equivalenceOracle
                    );
                };

        long timeMeasure = -1;

        if (config.measurement.time) {
            timeMeasure = System.nanoTime();
        }

        DeterministicAcceptorTS<?, JSONSymbol> hypo = learner.learn();

        if (config.measurement.time) {
            timeMeasure = (System.nanoTime() - timeMeasure) / 1_000_000;
        }

        if (csvLogger != null) {
            int numberAutomaton = -1;
            int numberStates = -1;
            int numberTrans = -1;
            switch (config.algorithm.automatonType) {
                case VRA:
                    VRA<?, JSONSymbol, ?> vraWithoutBin = ((VRA<?, JSONSymbol, ?>) hypo).removeBinStatesAndAutomata();
                    numberAutomaton = vraWithoutBin.getInputAlphabet().getNumProcedurals() + 1;
                    numberStates = vraWithoutBin.size();
                    numberTrans = vraWithoutBin.numberOfTrans();
//                    Visualization.visualize(vraWithoutBin);
                    break;
                case VPA_VSTAR:
//                    Pair<Integer, Integer> numberStateTrans  = getNumberStateTrans((SEVPA<?, JSONSymbol>) hypo);
//                    numberStates = numberStateTrans.getFirst();
//                    numberTrans = numberStateTrans.getSecond();
                    numberStates = ((SEVPA<?, JSONSymbol>) hypo).size() - alphabet.getNumCalls() - 1;
                    numberAutomaton = alphabet.getNumCalls() + 1;
                    break;
                case VPA_TTT:
//                    OneSEVPA<?, JSONSymbol> oneSevpaWithoutBin = removeBinState((OneSEVPA<?, JSONSymbol>) hypo);
//                    numberStates = oneSevpaWithoutBin.size();
//                    numberTrans = getNumberOfTransitions(oneSevpaWithoutBin);
                    numberStates = ((OneSEVPA<?, JSONSymbol>) hypo).size() - 1;
                    numberAutomaton = 1;
                    break;
            }

            csvLogger.log(
                    learner.getNumberOfEQ(),            // "#EQ",
                    membershipOracle.getNumberOfMQ(),           // "#MQ"
                    timeMeasure,                                // "Time (ms)"
                    equivalenceOracle.getNumberOfWordTried(),   // "#w Conformance"
                    equivalenceOracle.getLastEQTime(),          // "Time Last EQ (ms)"
                    equivalenceOracle.getTotalTime(),           // "Total EQ time (ms)"
                    numberAutomaton,                            // "#module"
                    numberStates,                               // "#states"
                    numberTrans                                 // "#trans"
            );

            System.out.println("Number of Equivalence Queries: " + learner.getNumberOfEQ());
            System.out.println("Number of Membership Queries: " + membershipOracle.getNumberOfMQ());
            System.out.println("Total learning time : " + timeMeasure + " ms");
            System.out.println("Number of words test by conformance: " + equivalenceOracle.getNumberOfWordTried());
            System.out.println("Time last EQ : " + equivalenceOracle.getLastEQTime() + " ms");
            System.out.println("Total Time EQ : " + equivalenceOracle.getTotalTime() + " ms");
            System.out.println("Number of automaton in hypothesis: " + numberAutomaton);
            System.out.println("Number of states in hypothesis: " + numberStates);
            System.out.println("Number of transition in hypothesis: " + numberTrans);

//            switch (config.algorithm.automatonType) {
//                case VRA:
//                    VRA<?, JSONSymbol, ?> vraWithoutBin = ((VRA<?, JSONSymbol, ?>) hypo).removeBinStatesAndAutomata();
//                    Visualization.visualize(vraWithoutBin);
//                    break;
//                case VPA_VSTAR:
//                    ((AbstractVStarLearner<JSONSymbol>) learner).printTables();
//                    Visualization.visualize(convertToDFA((SEVPA<?, JSONSymbol>) hypo));
//                    break;
//                case VPA_TTT:
//                    OneSEVPA<?, JSONSymbol> oneSevpaWithoutBin = removeBinState((OneSEVPA<?, JSONSymbol>) hypo);
//                    Visualization.visualize(oneSevpaWithoutBin);
//                    break;
//            }

        }
    }

    private void runBestCaseExperiment(Random random, ExperimentCSVLogger csvLogger) throws Exception {
        List<String> intAlpha = new ArrayList<>();
        for (int i = 0; i < config.datasets.k; i++) {
            intAlpha.add("a" + i);
        }
        VPAlphabet<String> alphabet = new DefaultVPAlphabet<>(
                Alphabets.fromCollection(intAlpha),
                Alphabets.singleton("c"),
                Alphabets.singleton("r")
        );

        BestCaseForVRAMembership membershipOracle = new BestCaseForVRAMembership(alphabet, config.datasets.n);

        BestCaseForVRAConformance<?> equivalenceOracle = switch (config.algorithm.automatonType) {
            case VRA -> new BestCaseForVRAConformance<VRA<?, String, ?>>(
                    membershipOracle, config.datasets.n, config.execution.testConformance, random
            );
            case VPA_TTT, VPA_VSTAR -> new BestCaseForVRAConformance<SEVPA<?, String>>(
                    membershipOracle, config.datasets.n, config.execution.testConformance, random
            );
        };

        final Learner<String, ? extends DeterministicAcceptorTS<?, String>> learner =
                switch (config.algorithm.automatonType) {
                    case VRA -> switch (config.algorithm.mode) {
                        case ISO -> new VRAIsomorphicLearner<>(
                                VRAlphabet.fromVPAlphabet(alphabet),
                                membershipOracle,
                                (BestCaseForVRAConformance<VRA<FastDFAState, String, DFA<FastDFAState, String>>>) equivalenceOracle);
                        case SEP -> new VRASeparateLearner<>(
                                VRAlphabet.fromVPAlphabet(alphabet),
                                membershipOracle,
                                (BestCaseForVRAConformance<VRA<FastDFAState, String, DFA<FastDFAState, String>>>) equivalenceOracle);
                        case SEP_OPT -> new VRASeparateLearnerOptimized<>(
                                VRAlphabet.fromVPAlphabet(alphabet),
                                membershipOracle,
                                (BestCaseForVRAConformance<VRA<FastDFAState, String, DFA<FastDFAState, String>>>) equivalenceOracle);
                    };
                    case VPA_TTT -> new VPATTTLearner<>(
                            alphabet,
                            membershipOracle,
                            (BestCaseForVRAConformance<SEVPA<?, String>>) equivalenceOracle
                    );
                    case VPA_VSTAR -> new VStarLearner<>(
                            alphabet,
                            membershipOracle,
                            (BestCaseForVRAConformance<SEVPA<?, String>>) equivalenceOracle
                    );
                };

        long timeMeasure = -1;

        if (config.measurement.time) {
            timeMeasure = System.nanoTime();
        }

        DeterministicAcceptorTS<?, String> hypo = learner.learn();

        if (config.measurement.time) {
            timeMeasure = (System.nanoTime() - timeMeasure) / 1_000_000;
        }


        if (csvLogger != null) {
            int numberAutomaton = -1;
            int numberStates = -1;
            int numberTrans = -1;
            switch (config.algorithm.automatonType) {
                case VRA:
                    VRA<?, String, ?> vraWithoutBin = ((VRA<FastDFAState, String, DFA<FastDFAState, String>>) hypo).removeBinStatesAndAutomata();
                    numberAutomaton = vraWithoutBin.getInputAlphabet().getNumProcedurals() + 1;
                    numberStates = vraWithoutBin.size();
                    numberTrans = vraWithoutBin.numberOfTrans();
//                    Visualization.visualize(vraWithoutBin);
                    break;
                case VPA_VSTAR:
//                    Pair<Integer, Integer> numberStateTrans  = getNumberStateTrans((SEVPA<?, JSONSymbol>) hypo);
//                    numberStates = numberStateTrans.getFirst();
//                    numberTrans = numberStateTrans.getSecond();
                    numberStates = ((SEVPA<?, String>) hypo).size() - alphabet.getNumCalls() - 1;
                    numberAutomaton = alphabet.getNumCalls() + 1;
                    break;
                case VPA_TTT:
//                    OneSEVPA<?, JSONSymbol> oneSevpaWithoutBin = removeBinState((OneSEVPA<?, JSONSymbol>) hypo);
//                    numberStates = oneSevpaWithoutBin.size();
//                    numberTrans = getNumberOfTransitions(oneSevpaWithoutBin);
                    numberStates = ((OneSEVPA<?, String>) hypo).size() - 1;
                    numberAutomaton = 1;
                    break;
            }

            csvLogger.log(
                    learner.getNumberOfEQ(),            // "#EQ",
                    membershipOracle.getNumberOfMQ(),           // "#MQ"
                    timeMeasure,                                // "Time (ms)"
                    equivalenceOracle.getNumberOfWordTried(),   // "#w Conformance"
                    equivalenceOracle.getLastEQTime(),          // "Time Last EQ (ms)"
                    equivalenceOracle.getTotalTime(),           // "Total EQ time (ms)"
                    numberAutomaton,                            // "#module"
                    numberStates,                               // "#states"
                    numberTrans                                 // "#trans"
            );

            System.out.println("Number of Equivalence Queries: " + learner.getNumberOfEQ());
            System.out.println("Number of Membership Queries: " + membershipOracle.getNumberOfMQ());
            System.out.println("Total learning time : " + timeMeasure + " ms");
            System.out.println("Number of words test by conformance: " + equivalenceOracle.getNumberOfWordTried());
            System.out.println("Time last EQ : " + equivalenceOracle.getLastEQTime() + " ms");
            System.out.println("Total Time EQ : " + equivalenceOracle.getTotalTime() + " ms");
            System.out.println("Number of automaton in hypothesis: " + numberAutomaton);
            System.out.println("Number of states in hypothesis: " + numberStates);
            System.out.println("Number of transition in hypothesis: " + numberTrans);

//            switch (config.algorithm.automatonType) {
//                case VRA:
//                    VRA<?, JSONSymbol, ?> vraWithoutBin = ((VRA<?, JSONSymbol, ?>) hypo).removeBinStatesAndAutomata();
//                    Visualization.visualize(vraWithoutBin);
//                    break;
//                case VPA_VSTAR:
//                    ((AbstractVStarLearner<JSONSymbol>) learner).printTables();
//                    Visualization.visualize(convertToDFA((SEVPA<?, JSONSymbol>) hypo));
//                    break;
//                case VPA_TTT:
//                    OneSEVPA<?, JSONSymbol> oneSevpaWithoutBin = removeBinState((OneSEVPA<?, JSONSymbol>) hypo);
//                    Visualization.visualize(oneSevpaWithoutBin);
//                    break;
//            }

        }
    }

    public static JSONSchema getSchema(Path filePath) {
        final JSONSchemaStore schemaStore = new JSONSchemaStore(false);
        try {
            return schemaStore.load(filePath.toUri().toURL().toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <L> FastDFA<String> convertToDFA(SEVPA<L, JSONSymbol> sevpa) {
        VPAlphabet<JSONSymbol> vpalphabet = sevpa.getInputAlphabet();
        Set<L> notBinStates = getNotBinStates(sevpa);

        GrowingAlphabet<String> alphabet = new GrowingMapAlphabet<>();
        for (JSONSymbol symbol : vpalphabet.getInternalAlphabet()) {
            alphabet.add(symbol.toString());
        }
        for (JSONSymbol callSymbol : vpalphabet.getCallAlphabet()) {
            alphabet.add(callSymbol.toString());
            for (JSONSymbol returnSymbol : vpalphabet.getReturnAlphabet()) {
                for (L loc: sevpa.getLocations()) {
                    alphabet.add(returnSymbol.toString() + " [" + callSymbol.toString() + " , " + loc.toString() + "]");
                }
            }
        }

        FastDFA<String> fastDFA = new FastDFA<>(alphabet);
        final Map<L, FastDFAState> oldToNewLocations = new HashMap<>();
        for (final L loc : notBinStates) {
            FastDFAState state = fastDFA.addState(sevpa.isAcceptingLocation(loc));
            oldToNewLocations.put(loc, state);
        }

        for(final L source : notBinStates) {
            for (JSONSymbol symbol : vpalphabet.getInternalAlphabet()) {
                final L target = sevpa.getInternalSuccessor(source, symbol);
                if (notBinStates.contains(target)) {
                    fastDFA.addTransition(
                            oldToNewLocations.get(source),
                            symbol.toString(),
                            oldToNewLocations.get(target)
                    );
                }
            }
            for (JSONSymbol callSymbol : vpalphabet.getCallAlphabet()) {
                final L target = sevpa.getModuleEntry(callSymbol);
                if (notBinStates.contains(target)) {
//                    fastDFA.addTransition(
//                            oldToNewLocations.get(source),
//                            callSymbol.toString(),
//                            oldToNewLocations.get(target)
//                    );
                }
                for (JSONSymbol returnSymbol : vpalphabet.getReturnAlphabet()) {
                    for (L locationBeforeCall: sevpa.getLocations()) {
                        String symbol = returnSymbol.toString() +
                                " [" + callSymbol.toString() + " , " + locationBeforeCall.toString() + "]";
                        final L targetReturn = sevpa.getReturnSuccessor(
                                source, returnSymbol, sevpa.encodeStackSym(locationBeforeCall, callSymbol));
                        if (notBinStates.contains(target)) {
                            fastDFA.addTransition(
                                    oldToNewLocations.get(source),
                                    symbol,
                                    oldToNewLocations.get(targetReturn)
                            );
                        }
                    }
                }
            }
        }
        return fastDFA;
    }

    private <L> Set<L> getNotBinStates(SEVPA<L, JSONSymbol> learned) {
        VPAlphabet<JSONSymbol> alphabet = learned.getInputAlphabet();
        Map<L, Set<L>> predecessors = new HashMap<>();
        Set<L> notBinStates = new HashSet<>();
        for (L loc: learned.getLocations()) {
            predecessors.put(loc, new HashSet<>());
            if (learned.isAcceptingLocation(loc)) {
                notBinStates.add(loc);
            }
            if (learned.getInitialLocation().equals(loc)) {
                notBinStates.add(loc);
            }
        }

        for (L loc: learned.getLocations()) {
            for (JSONSymbol internalSymbol : alphabet.getInternalAlphabet()) {
                predecessors.get(
                        learned.getInternalSuccessor(loc, internalSymbol)
                ).add(loc);
            }
            for (JSONSymbol returnSymbol : alphabet.getReturnAlphabet()) {
                for (JSONSymbol callSymbol : alphabet.getCallAlphabet()) {
                    for (L locationBeforeCall : learned.getLocations()) {
                        final int stackSymLearned = learned.encodeStackSym(locationBeforeCall, callSymbol);
                        if (learned.getReturnSuccessor(loc, returnSymbol, stackSymLearned) != null) {
                            predecessors.get(
                                    learned.getReturnSuccessor(loc, returnSymbol, stackSymLearned)
                            ).add(loc);
                        }
                    }
                }
            }
        }

        List<L> queue = new ArrayList<>(notBinStates.stream().toList());
        while (!queue.isEmpty()) {
            L loc = queue.removeFirst();
            for (L pred: predecessors.get(loc)) {
                if (notBinStates.contains(pred)) continue;
                notBinStates.add(pred);
                queue.add(pred);
            }
        }
        return notBinStates;
    }

    private <L> OneSEVPA<?, JSONSymbol> removeBinState(OneSEVPA<L, JSONSymbol> learned) {
        VPAlphabet<JSONSymbol> alphabet = learned.getInputAlphabet();
        Set<L> notBinStates = getNotBinStates(learned);

        final DefaultOneSEVPA<JSONSymbol> withoutBin = new DefaultOneSEVPA<>(alphabet);
        final Map<L, Location> oldToNewLocations = new HashMap<>();
        for (final L location : notBinStates) {
            final Location newLocation;
            final boolean accepting = learned.isAcceptingLocation(location);
            if (learned.getInitialLocation().equals(location)) {
                newLocation = withoutBin.addInitialLocation(accepting);
            } else {
                newLocation = withoutBin.addLocation(accepting);
            }
            oldToNewLocations.put(location, newLocation);
        }

        for (final L source : notBinStates) {
            for (final JSONSymbol internalSymbol : alphabet.getInternalAlphabet()) {
                final L target = learned.getInternalSuccessor(source, internalSymbol);
                assert target != null;
                if (notBinStates.contains(target)) {
                    withoutBin.setInternalSuccessor(
                            oldToNewLocations.get(source),
                            internalSymbol,
                            oldToNewLocations.get(target));
                }
            }

            for (final JSONSymbol returnSymbol : alphabet.getReturnAlphabet()) {
                for (final JSONSymbol callSymbol : alphabet.getCallAlphabet()) {
                    for (final L locationBeforeCall : notBinStates) {
                        final L target = learned.getReturnSuccessor(
                                source, returnSymbol, learned.encodeStackSym(locationBeforeCall, callSymbol));
                        assert target != null;

                        if (notBinStates.contains(target) && notBinStates.contains(locationBeforeCall)) {
                            final int stackSymNew = withoutBin.encodeStackSym(
                                    oldToNewLocations.get(locationBeforeCall), callSymbol);
                            withoutBin.setReturnSuccessor(
                                    oldToNewLocations.get(source),
                                    returnSymbol,
                                    stackSymNew,
                                    oldToNewLocations.get(target));
                        }
                    }
                }
            }
        }

        return withoutBin;
    }

    private <L> Pair<Integer, Integer> getNumberStateTrans(SEVPA<L, JSONSymbol> learned) {
        VPAlphabet<JSONSymbol> alphabet = learned.getInputAlphabet();
        Set<L> notBinStates = getNotBinStates(learned);


        int numberTrans = 0;

        for (final L source : learned.getLocations()) {
            for (final JSONSymbol internalSymbol : alphabet.getInternalAlphabet()) {
                final L target = learned.getInternalSuccessor(source, internalSymbol);
                assert target != null;
                if (notBinStates.contains(target)) {
                    numberTrans++;
                }
            }

            for (final JSONSymbol returnSymbol : alphabet.getReturnAlphabet()) {
                for (final JSONSymbol callSymbol : alphabet.getCallAlphabet()) {
                    for (final L locationBeforeCall : learned.getLocations()) {
                        final int stackSymLearned = learned.encodeStackSym(locationBeforeCall, callSymbol);
                        final L target = learned.getReturnSuccessor(source, returnSymbol, stackSymLearned);
//                        assert target != null;
                        if (notBinStates.contains(target)) {
                            numberTrans++;
                        }
                    }
                }
            }
        }

        int numberStates = notBinStates.size();

        return Pair.of(numberStates,
                numberTrans + numberStates * alphabet.getNumCalls());
    }

    private int getNumberOfTransitions(SEVPA<?, JSONSymbol> vpa) {
        return numberOfReturnTransitions(vpa) +
                numberOfCallTransitions(vpa) +
                numberOfInternalTransitions(vpa);
    }

    private <L> int numberOfCallTransitions(SEVPA<L, JSONSymbol> vpa) {
        return vpa.size() * vpa.getInputAlphabet().getNumCalls();
    }

    private <L> int numberOfReturnTransitions(SEVPA<L, JSONSymbol> vpa) {
        int number = 0;
        for (L location : vpa.getLocations()) {
            for (JSONSymbol symbol : vpa.getInputAlphabet().getReturnAlphabet()) {
                for (L source : vpa.getLocations()) {
                    for (JSONSymbol call : vpa.getInputAlphabet().getCallAlphabet()) {
                        if (vpa.getReturnSuccessor(location, symbol, vpa.encodeStackSym(source, call)) != null) {
                            number++;
                        }
                    }
                }
            }
        }
        return number;
    }

    private <L> int numberOfInternalTransitions(SEVPA<L, JSONSymbol> vpa) {
        int number = 0;
        for (L location : vpa.getLocations()) {
            for (JSONSymbol symbol : vpa.getInputAlphabet().getInternalAlphabet()) {
                if (vpa.getInternalSuccessor(location, symbol) != null) {
                    number++;
                }
            }
        }
        return number;
    }
}
