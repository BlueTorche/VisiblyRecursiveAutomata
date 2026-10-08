package umons.ac.be.test;

import de.learnlib.acex.AcexAnalyzers;
import de.learnlib.algorithm.ttt.vpa.TTTLearnerVPA;
import de.learnlib.query.DefaultQuery;
import net.automatalib.automaton.vpa.OneSEVPA;
import net.automatalib.automaton.vpa.SEVPA;
import net.automatalib.visualization.Visualization;
import umons.ac.be.learner.VRALearner.AbstractVRALearner;
import umons.ac.be.learner.VRALearner.VRAIsomorphicLearner;
import umons.ac.be.learner.VRALearner.VRASeparateLearner;
import umons.ac.be.learner.VRALearner.VRASeparateLearnerOptimized;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.VPAlphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.alphabet.impl.DefaultVPAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.automaton.fsa.impl.FastDFAState;
import net.automatalib.automaton.vpa.impl.DefaultOneSEVPA;
import net.automatalib.automaton.vpa.impl.Location;
import net.automatalib.common.util.Pair;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.util.automaton.builder.AutomatonBuilders;
import umons.ac.be.learner.VStar.AbstractVStarLearner;
import umons.ac.be.learner.VStar.VStarLearner;
import umons.ac.be.oracle.vpl.VPAConformanceOracleEnumerating;
import umons.ac.be.oracle.vpl.VRAConformanceOracleEnumerating;
import umons.ac.be.oracle.vpl.VPLMembershipOracleFromAcceptor;
import umons.ac.be.vra.DefaultVRAwithDFA;
import umons.ac.be.vra.VRA;
import umons.ac.be.vraalphabet.DefaultVRAlphabet;
import umons.ac.be.vraalphabet.VRAlphabet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;


public class TestLearningVRA {
    private final static int maxLength = 10;

    enum LearnerType {
        ISOMORPHIC_LEARNER,
        SEPARATE_LEARNER,
        SEPARATE_LEARNER_OPTIMIZED
    }
    enum BenchmarkType {
        ONE_SEVPA,
        VRA_BENCHMARK_1,
        VRA_BENCHMARK_2,
        VRA_BENCHMARK_POSTER_1,
        VRA_BENCHMARK_POSTER_2, // c^n c^m r^m c^p r^p r^n
        VRA_BENCHMARK_BIGPAPER,
        VRA_BENCHMARK_LEARNINGPAPER
    }
    enum AutomatonType {
        VRA,
        oneSEVPA,
        kSEVPA
    }

    static LearnerType VRA_LEARNER =  LearnerType.SEPARATE_LEARNER_OPTIMIZED;
    static BenchmarkType BENCHMARK = BenchmarkType.VRA_BENCHMARK_LEARNINGPAPER;
    static AutomatonType AUTOMATON = AutomatonType.kSEVPA;
    static boolean VISUALIZATION = false;
    static int NUMBER_EXPERIMENT = 10;

    static float total_time = 0;
    static int total_EQ = 0;
    static int total_MQ = 0;

    public static void main(String[] args) throws InterruptedException {
        testLearningVRA(BENCHMARK, VRA_LEARNER, VISUALIZATION);
        testLearningVPATTT(BENCHMARK, VISUALIZATION);
        testLearningVPAVstar(BENCHMARK, VISUALIZATION);

        total_time = 0;
        total_EQ = 0;
        total_MQ = 0;

        for (int i = 0; i < NUMBER_EXPERIMENT; i++) {
            System.out.println("Starting Experiment " + i);
            System.gc();
            TimeUnit.SECONDS.sleep(1);

            switch (AUTOMATON) {
                case VRA:
                    testLearningVRA(BENCHMARK, VRA_LEARNER, false);
                    break;
                case oneSEVPA:
                    testLearningVPATTT(BENCHMARK, false);
                    break;
                case kSEVPA:
                    testLearningVPAVstar(BENCHMARK, false);
                    break;
            }
        }
        if (NUMBER_EXPERIMENT > 0) {
            System.out.println("Mean time is " + total_time / NUMBER_EXPERIMENT);
            System.out.println("Mean MQ is " + ((float) total_MQ) / NUMBER_EXPERIMENT);
            System.out.println("Mean EQ is " + ((float) total_EQ) / NUMBER_EXPERIMENT);
        }
    }

    public static void testLearningVPATTT(BenchmarkType benchmarkType,
                                          boolean visualization) {
        Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>> benchmark = getBenchmark(benchmarkType);

        DeterministicAcceptorTS<?, String> teacherBlackbox = benchmark.getFirst();
        VPAlphabet<String> alphabet = benchmark.getSecond();

        VPLMembershipOracleFromAcceptor<String> membershipOracle = new VPLMembershipOracleFromAcceptor<>(teacherBlackbox);
        VPAConformanceOracleEnumerating<String> equivalenceOracle =
                new VPAConformanceOracleEnumerating<>(teacherBlackbox, alphabet, maxLength);

        final TTTLearnerVPA<String> learner = new TTTLearnerVPA<>(alphabet,
                    membershipOracle,
                    AcexAnalyzers.LINEAR_FWD);


        int totalEQ = 0;
        float time = System.nanoTime();

        learner.startLearning();
        OneSEVPA<?, String> hypo;
        while (true) {
            hypo = learner.getHypothesisModel();
            totalEQ ++;

            System.out.println("Searching for counterexample " + totalEQ);

            DefaultQuery<String, Boolean> ce = equivalenceOracle.findCounterExample(hypo, new HashSet<>());

            System.out.println("Found counterexample, processing...");

            if (ce == null) {
                break;
            }
            final boolean refined = learner.refineHypothesis(ce);
            assert refined;
        }

        float runtime = (System.nanoTime() - time)/1000_000_000;
        System.out.println("Total learning time : " + runtime+ " s");
        System.out.println("Total number if MQ : " + membershipOracle.getNumberOfMQ());
        System.out.println("Total number if EQ : " + totalEQ);

        if (visualization) {
            Visualization.visualize(hypo);
        }
        total_time += runtime;
        total_MQ += membershipOracle.getNumberOfMQ();
        total_EQ += totalEQ;
    }

    public static void testLearningVRA(BenchmarkType benchmarkType,
                                       LearnerType learnerType,
                                       boolean visualization) {
        Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>> benchmark = getBenchmark(benchmarkType);

        DeterministicAcceptorTS<?, String> teacherBlackbox = benchmark.getFirst();
        VPAlphabet<String> alphabet = benchmark.getSecond();

        VPLMembershipOracleFromAcceptor<String> membershipOracle = new VPLMembershipOracleFromAcceptor<>(teacherBlackbox);
        VRAConformanceOracleEnumerating<String> equivalenceOracle =
                new VRAConformanceOracleEnumerating<>(teacherBlackbox, alphabet, maxLength);

        final AbstractVRALearner<String> learner = switch (learnerType) {
            case ISOMORPHIC_LEARNER -> new VRAIsomorphicLearner<>(
                    VRAlphabet.fromVPAlphabet(alphabet), membershipOracle, equivalenceOracle);
            case SEPARATE_LEARNER -> new VRASeparateLearner<>(
                    VRAlphabet.fromVPAlphabet(alphabet), membershipOracle, equivalenceOracle);
            case SEPARATE_LEARNER_OPTIMIZED -> new VRASeparateLearnerOptimized<>(
                    VRAlphabet.fromVPAlphabet(alphabet), membershipOracle, equivalenceOracle);
        };


        float startTime = System.nanoTime();
        VRA<?, String, DFA<FastDFAState, String>> learnedVRA = learner.learn();
        float runtime = (System.nanoTime() - startTime) / 1_000_000_000;
        System.out.println("Total learning time : " + runtime+ " s");
        learner.displayStats();

        if (visualization) {
            learner.printTables();
            Visualization.visualize(learnedVRA);
            Visualization.visualize(learnedVRA.removeBinStatesAndAutomata());
        }
        total_time += runtime;
        total_MQ += learner.getNumberOfMQ();
        total_EQ += learner.getNumberOfEQ();
    }

    public static void testLearningVPAVstar(BenchmarkType benchmarkType,
                                            boolean visualization) {
        Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>> benchmark = getBenchmark(benchmarkType);

        DeterministicAcceptorTS<?, String> teacherBlackbox = benchmark.getFirst();
        VPAlphabet<String> alphabet = benchmark.getSecond();

        VPLMembershipOracleFromAcceptor<String> membershipOracle = new VPLMembershipOracleFromAcceptor<>(teacherBlackbox);
        VRAConformanceOracleEnumerating<String> equivalenceOracle =
                new VRAConformanceOracleEnumerating<>(teacherBlackbox, alphabet, maxLength);


        float startTime = System.nanoTime();
        final AbstractVStarLearner<String> learner = new VStarLearner(alphabet, membershipOracle, equivalenceOracle);
        SEVPA<?, String> learnedVPA = learner.learn();
        float runtime = (System.nanoTime() - startTime) / 1_000_000_000;
        System.out.println("Total learning time : " + runtime+ " s");
        learner.displayStats();

        if (visualization) {
            Visualization.visualize(learnedVPA);
        }
        total_time += runtime;
        total_MQ += learner.getNumberOfMQ();
        total_EQ += learner.getNumberOfEQ();
    }

    private static Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>> getOneSEVPAOracle() {
        VPAlphabet<String> alphabet = new DefaultVPAlphabet<String>(
                Alphabets.fromArray("i1", "i2"), Alphabets.fromArray("c"), Alphabets.fromArray("r")
        );
        final DefaultOneSEVPA<String> oneSEVPA = new DefaultOneSEVPA<>(alphabet);

        final Location l0 = oneSEVPA.addInitialLocation(false);
        final Location l1 = oneSEVPA.addLocation(true);

        oneSEVPA.setInternalSuccessor(l0, "i1", l1);
        oneSEVPA.setInternalSuccessor(l1, "i2", l0);

        oneSEVPA.setReturnSuccessor(l0, "r", oneSEVPA.encodeStackSym(l1, "c"), l1);
        oneSEVPA.setReturnSuccessor(l1, "r", oneSEVPA.encodeStackSym(l0, "c"), l1);

//        Visualization.visualize(oneSEVPA);

        return Pair.of(oneSEVPA, alphabet);
    }

    private static Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>> getVRA_Benchmark1_Oracle() {
        Alphabet<String> internalAlphabet = Alphabets.fromArray("i1", "i2");
        Alphabet<String> callAlphabet = Alphabets.fromArray("c1", "c2");
        Alphabet<String> returnAlphabet = Alphabets.fromArray("r1", "r2");

        VRAlphabet<String> alphabet = new DefaultVRAlphabet<>(internalAlphabet, callAlphabet, returnAlphabet);

        alphabet.addProceduralSymbol("J1", "c1", "r1");
        alphabet.addProceduralSymbol("J2", "c2", "r2");
        alphabet.addProceduralSymbol("J3", "c1", "r1");


        DFA<?, String> startingProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("s0")
                .from("s0").on("J3").to("s0")
                .from("s0").on("J1").to("s0")
                .from("s0").on("i1").to("s1")
                .from("s0").on("J2").to("s3")
                .from("s1").on("J1").to("s2")
                .from("s2").on("i2").to("s3")
                .withAccepting("s3")
                .create();


        DFA<?, String> J1Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j10")
                .from("j10").on("J2").to("j11")
                .from("j10").on("i2").to("j11")
                .from("j11").on("J1").to("j11")
                .from("j11").on("i1").to("j11")
                .withAccepting("j11")
                .create();


        DFA<?, String> J2Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j20")
                .from("j20").on("i1").to("j21")
                .from("j20").on("i2").to("j21")
                .from("j20").on("J3").to("j21")
                .withAccepting("j21")
                .create();


        DFA<?, String> J3Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j30")
                .from("j30").on("J3").to("j30")
                .from("j30").on("i1").to("j30")
                .from("j30").on("i2").to("j30")
                .withAccepting("j30")
                .create();

        HashMap<String, DFA<?, String>> procedures = new HashMap<>();
        procedures.put("J1", J1Procedure);
        procedures.put("J2", J2Procedure);
        procedures.put("J3", J3Procedure);

        return Pair.of(new DefaultVRAwithDFA<>(alphabet, procedures, startingProcedure), alphabet);
    }

    private static Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>> getVRA_Benchmark2_Oracle() {
        Alphabet<String> internalAlphabet = Alphabets.fromArray("a");
        Alphabet<String> callAlphabet = Alphabets.fromArray("c");
        Alphabet<String> returnAlphabet = Alphabets.fromArray("r");

        VRAlphabet<String> alphabet = new DefaultVRAlphabet<>(internalAlphabet, callAlphabet, returnAlphabet);

        alphabet.addProceduralSymbol("J1", "c", "r");
        alphabet.addProceduralSymbol("J2", "c", "r");
        alphabet.addProceduralSymbol("J3", "c", "r");


        DFA<?, String> startingProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("s0")
                .from("s0").on("J3").to("s0")
                .from("s0").on("J2").to("s1")
                .from("s0").on("J1").to("s2")
                .from("s1").on("J1").to("s0")
                .from("s1").on("J2").to("s0")
                .from("s2").on("J1").to("s2")
                .from("s2").on("J2").to("s2")
                .from("s2").on("J3").to("s0")
                .withAccepting("s0")
                .withAccepting("s2")
                .create();


        DFA<?, String> J1Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j10")
                .withAccepting("j10")
                .create();


        DFA<?, String> J2Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j20")
                .from("j20").on("a").to("j21")
                .from("j20").on("J1").to("j21")
                .from("j20").on("J2").to("j21")
                .from("j20").on("J3").to("j21")
                .from("j21").on("a").to("j20")
                .from("j21").on("J1").to("j20")
                .from("j21").on("J2").to("j20")
                .from("j21").on("J3").to("j20")
                .withAccepting("j21")
                .create();


        DFA<?, String> J3Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j30")
                .from("j30").on("a").to("j31")
                .from("j30").on("J1").to("j31")
                .from("j30").on("J2").to("j31")
                .from("j30").on("J3").to("j31")
                .from("j31").on("a").to("j32")
                .from("j31").on("J1").to("j32")
                .from("j31").on("J2").to("j32")
                .from("j31").on("J3").to("j32")
                .from("j32").on("a").to("j31")
                .from("j32").on("J1").to("j31")
                .from("j32").on("J2").to("j31")
                .from("j32").on("J3").to("j31")
                .withAccepting("j32")
                .create();

        HashMap<String, DFA<?, String>> procedures = new HashMap<>();
        procedures.put("J1", J1Procedure);
        procedures.put("J2", J2Procedure);
        procedures.put("J3", J3Procedure);

        return Pair.of(new DefaultVRAwithDFA<>(alphabet, procedures, startingProcedure), alphabet);
    }

    private static Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>> getVRABenchmarkPoster1() {
        Alphabet<String> internalAlphabet = Alphabets.fromArray("a");
        Alphabet<String> callAlphabet = Alphabets.fromArray("c");
        Alphabet<String> returnAlphabet = Alphabets.fromArray("r");

        VRAlphabet<String> alphabet = new DefaultVRAlphabet<>(internalAlphabet, callAlphabet, returnAlphabet);

        alphabet.addProceduralSymbol("R", "c", "r");
        alphabet.addProceduralSymbol("T", "c", "r");


        DFA<?, String> startingProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("s0")
                .from("s0").on("R").to("s1")
                .withAccepting("s1")
                .create();


        DFA<?, String> RProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("r0")
                .from("r0").on("T").to("r0")
                .from("r0").on("R").to("r1")
                .from("r0").on("a").to("r1")
                .withAccepting("r1")
                .create();


        DFA<?, String> TProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("t0")
                .from("t0").on("a").to("t0")
                .from("t0").on("R").to("t0")
                .withAccepting("t0")
                .create();


        HashMap<String, DFA<?, String>> procedures = new HashMap<>();
        procedures.put("R", RProcedure);
        procedures.put("T", TProcedure);

        return Pair.of(new DefaultVRAwithDFA<>(alphabet, procedures, startingProcedure), alphabet);
    }

    private static Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>> getVRABenchmarkPoster2() {
        Alphabet<String> internalAlphabet = Alphabets.fromArray();
        Alphabet<String> callAlphabet = Alphabets.fromArray("c");
        Alphabet<String> returnAlphabet = Alphabets.fromArray("r");

        VRAlphabet<String> alphabet = new DefaultVRAlphabet<>(internalAlphabet, callAlphabet, returnAlphabet);

        alphabet.addProceduralSymbol("R", "c", "r");
        alphabet.addProceduralSymbol("T", "c", "r");


        DFA<?, String> startingProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("s0")
                .from("s0").on("R").to("s1")
                .withAccepting("s1")
                .create();


        DFA<?, String> RProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("r0")
                .from("r0").on("T").to("r1")
                .from("r1").on("T").to("r2")
                .from("r0").on("R").to("r2")
                .withAccepting("r2")
                .create();


        DFA<?, String> TProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("t0")
                .from("t0").on("T").to("t1")
                .withAccepting("t0")
                .withAccepting("t1")
                .create();


        HashMap<String, DFA<?, String>> procedures = new HashMap<>();
        procedures.put("R", RProcedure);
        procedures.put("T", TProcedure);

        return Pair.of(new DefaultVRAwithDFA<>(alphabet, procedures, startingProcedure), alphabet);
    }

    private static Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>>  getVRA_Benchmark_LearningPaper() {
        Alphabet<String> internalAlphabet = Alphabets.fromArray("a");
        Alphabet<String> callAlphabet = Alphabets.fromArray("c");
        Alphabet<String> returnAlphabet = Alphabets.fromArray("r");

        VRAlphabet<String> alphabet = new DefaultVRAlphabet<>(internalAlphabet, callAlphabet, returnAlphabet);

        alphabet.addProceduralSymbol("R", "c", "r");
        alphabet.addProceduralSymbol("T", "c", "r");


        DFA<?, String> startingProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("s0")
                .from("s0").on("R").to("s1")
                .withAccepting("s1")
                .create();


        DFA<?, String> RProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("r0")
                .from("r0").on("a").to("r1")
                .from("r0").on("T").to("r1")
                .from("r1").on("a").to("r1")
                .withAccepting("r0")
                .withAccepting("r1")
                .create();


        DFA<?, String> TProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("t0")
                .from("t0").on("a").to("t1")
                .from("t0").on("T").to("t1")
                .from("t1").on("T").to("t1")
                .withAccepting("t1")
                .create();

        HashMap<String, DFA<?, String>> procedures = new HashMap<>();
        procedures.put("R", RProcedure);
        procedures.put("T", TProcedure);

        return Pair.of(new DefaultVRAwithDFA<>(alphabet, procedures, startingProcedure), alphabet);
    }


    private static Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>>  getVRA_Benchmark3_Oracle() {
        Alphabet<String> internalAlphabet = Alphabets.fromArray("a");
        Alphabet<String> callAlphabet = Alphabets.fromArray("c");
        Alphabet<String> returnAlphabet = Alphabets.fromArray("r");

        VRAlphabet<String> alphabet = new DefaultVRAlphabet<>(internalAlphabet, callAlphabet, returnAlphabet);

        alphabet.addProceduralSymbol("J1", "c", "r");
        alphabet.addProceduralSymbol("J2", "c", "r");


        DFA<?, String> startingProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("s0")
                .from("s0").on("J1").to("s1")
                .from("s0").on("J2").to("s2")
                .from("s1").on("a").to("s2")
                .withAccepting("s2")
                .create();


        DFA<?, String> J1Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j10")
                .from("j10").on("a").to("j11")
                .from("j11").on("a").to("j10")
                .withAccepting("j10")
                .create();


        DFA<?, String> J2Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
                .withInitial("j20")
                .from("j20").on("a").to("j21")
                .from("j21").on("a").to("j22")
                .from("j22").on("a").to("j21")
                .withAccepting("j20")
                .withAccepting("j21")
                .create();

        HashMap<String, DFA<?, String>> procedures = new HashMap<>();
        procedures.put("J1", J1Procedure);
        procedures.put("J2", J2Procedure);

        return Pair.of(new DefaultVRAwithDFA<>(alphabet, procedures, startingProcedure), alphabet);

    }

//    private static Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>>  getVRABenchmarkBestCase() {
//        Alphabet<String> internalAlphabet = Alphabets.fromArray();
//        Alphabet<String> callAlphabet = Alphabets.fromArray("c");
//        Alphabet<String> returnAlphabet = Alphabets.fromArray("r");
//
//        VRAlphabet<String> alphabet = new DefaultVRAlphabet<>(internalAlphabet, callAlphabet, returnAlphabet);
//
//        alphabet.addProceduralSymbol("J1", "c", "r");
//        alphabet.addProceduralSymbol("J2", "c", "r");
//
//        DFA<?, String> startingProcedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
//                .withInitial("s0")
//                .from("s0").on("J1").to("s1")
//                .from("s1").on("J1").to("s2")
//                .from("s2").on("J1").to("s3")
//                .from("s3").on("J1").to("s0")
//                .from("s0").on("J2").to("s0")
//                .from("s1").on("J2").to("s1")
//                .from("s2").on("J2").to("s2")
//                .from("s3").on("J2").to("s3")
//                .withAccepting("s0")
//                .create();
//
//
//        DFA<?, String> j1Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
//                .withInitial("s0")
//                .from("s0").on("J2").to("s1")
//                .from("s1").on("J2").to("s2")
//                .from("s2").on("J2").to("s3")
//                .from("s3").on("J2").to("s0")
//                .from("s0").on("J1").to("s0")
//                .from("s1").on("J1").to("s1")
//                .from("s2").on("J1").to("s2")
//                .from("s3").on("J1").to("s4")
//                .from("s4").on("J1").to("s4")
//                .from("s4").on("J2").to("s0")
//                .withAccepting("s3")
//                .create();
//
//        DFA<?, String> j2Procedure = AutomatonBuilders.forDFA(new CompactDFA<>(alphabet.getAutomatonAlphabet()))
//                .withInitial("s0")
//                .from("s0").on("J1").to("s1")
//                .from("s1").on("J1").to("s2")
//                .from("s2").on("J1").to("s3")
//                .from("s3").on("J1").to("s0")
//                .from("s0").on("J2").to("s4")
//                .from("s1").on("J2").to("s1")
//                .from("s2").on("J2").to("s2")
//                .from("s3").on("J2").to("s3")
//                .from("s4").on("J1").to("s1")
//                .withAccepting("s0")
//                .create();
//
//        HashMap<String, DFA<?, String>> procedures = new HashMap<>();
//        procedures.put("J1", j1Procedure);
//        procedures.put("J2", j2Procedure);
//
//        return Pair.of(new DefaultVRAwithDFA<>(alphabet, procedures, startingProcedure), alphabet);
//    }

    private static Pair<DeterministicAcceptorTS<?, String>, VPAlphabet<String>> getBenchmark(BenchmarkType benchmarkType) {
        return switch(benchmarkType) {
            case ONE_SEVPA -> getOneSEVPAOracle();
            case VRA_BENCHMARK_1 -> getVRA_Benchmark1_Oracle();
            case VRA_BENCHMARK_2 ->  getVRA_Benchmark2_Oracle();
            case VRA_BENCHMARK_POSTER_1 ->  getVRABenchmarkPoster1();
            case VRA_BENCHMARK_POSTER_2 ->  getVRABenchmarkPoster2();
            case VRA_BENCHMARK_BIGPAPER ->  getVRA_Benchmark3_Oracle();
            case VRA_BENCHMARK_LEARNINGPAPER ->  getVRA_Benchmark_LearningPaper();
        };
    }
}
