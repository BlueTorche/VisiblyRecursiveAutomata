package umons.ac.be;

public class Main {
    private static final String DEFAULT_DATASET_FILE = "benchmarks/configs/datasets/vscode.yaml";
    private static final String DEFAULT_ALGORITHM_FILE = "benchmarks/configs/algorithms/VRA_SEP.yaml";

    public static void main(String[] args) throws Exception {
        String datasetConfigFile = null;
        String algorithmConfigFile = null;
        int seed = -1;
        int batchSize = 1000;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--dataset-config") && i + 1 < args.length) {
                datasetConfigFile = args[++i];
            }
            if (args[i].equals("--algorithm-config") && i + 1 < args.length) {
                algorithmConfigFile = args[++i];
            }
            if (args[i].equals("--seed") && i + 1 < args.length) {
                seed = Integer.parseInt(args[++i]);
            }
            if (args[i].equals("--batch-size") && i + 1 < args.length) {
                batchSize = Integer.parseInt(args[++i]);
            }
        }
        if (datasetConfigFile == null) {
            datasetConfigFile = DEFAULT_DATASET_FILE;
        }
        if (algorithmConfigFile == null) {
            algorithmConfigFile = DEFAULT_ALGORITHM_FILE;
        }

        if (datasetConfigFile == null) {
            throw new IllegalArgumentException(
                    "Missing required argument: --dataset-config <file>"
            );
        }
        if (algorithmConfigFile == null ) {
            throw new IllegalArgumentException(
                    "Missing required argument: --algorithm-config <file>"
            );
        }

        ExperimentRunner runner = new ExperimentRunner(datasetConfigFile, algorithmConfigFile, seed, batchSize);
        runner.run();
    }
}