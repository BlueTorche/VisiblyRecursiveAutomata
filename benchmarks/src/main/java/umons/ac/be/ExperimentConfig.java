package umons.ac.be;

public class ExperimentConfig {
    public DatasetConfig datasets;
    public AlgorithmConfig algorithm;
    public ExecutionConfig execution;
    public MeasurementConfig measurement;

    public static class DatasetConfig {
        public enum DatasetType {
            JSON
        }

        public DatasetType type;
        public String file;
    }

    public static class AlgorithmConfig {
        public enum AutomatonType {
            VRA,
            VPA_TTT,
            VPA_VSTAR,
        }
        public enum AlgorithmMode {
            ISO,
            SEP,
            SEP_OPT,
        }

        public AutomatonType automatonType;
        public AlgorithmMode mode;
    }

    public static class ExecutionConfig {
        public enum EquivalenceOracleType {
            EXHAUSTIVE,
            RANDOM
        }

        public int repetitions;
        public int warmupRuns;
        public int testConformance;
        public EquivalenceOracleType equivalenceOracleType;
    }

    public static class MeasurementConfig {
        public boolean time;
        public boolean membership;
        public boolean equivalence;
        public boolean word;
    }
}