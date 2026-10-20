package umons.ac.be;

import java.util.ArrayList;
import java.util.List;

public class ExperimentConfig {
    public DatasetConfig datasets;
    public AlgorithmConfig algorithm;
    public ExecutionConfig execution;
    public MeasurementConfig measurement;

    public static class DatasetConfig {
        public enum DatasetType {
            JSON,
            BEST_CASE
        }

        public DatasetType type;
        public String file;
        public String directory;
        private List<String> problematicWords = new ArrayList<>();
        public int n;
        public int k;

        public List<String> getProblematicWords() {
            return problematicWords;
        }

        public void setProblematicWords(List<String> problematicWords) {
            this.problematicWords = problematicWords;
        }
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
        public int seed = 0;
    }

    public static class MeasurementConfig {
        public boolean time;
    }
}