package umons.ac.be;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class ExperimentCSVLogger implements AutoCloseable {
    private final BufferedWriter writer;

    public final String[] headers = {
            "#EQ",
            "#MQ",
            "Time (ms)",
            "#w Conformance",
            "Last EQ time (ms)",
            "Total EQ time (ms)",
            "#module",
            "#states",
            "#trans"
    };

    /**
     * Crée un nouveau fichier CSV et écrit les en-têtes.
     *
     * @param filePath chemin du fichier CSV
     */
    public ExperimentCSVLogger(Path filePath) throws IOException {
        // Crée les dossiers parents s'ils n'existent pas
        Path parent = filePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        // CREATE + TRUNCATE_EXISTING permet de repartir d'un fichier vide
        writer = Files.newBufferedWriter(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        // Écrit les en-têtes
        writer.write(toCsvLine(List.of(headers)));
        writer.newLine();
        writer.flush();
    }

    /**
     * Ajoute une expérience dans le fichier CSV.
     * Les valeurs sont automatiquement échappées selon les règles CSV.
     */
    public synchronized void log(Object... values) throws IOException {
        writer.write(
                toCsvLine(
                        List.of(values)
                                .stream()
                                .map(String::valueOf)
                                .collect(Collectors.toList())
                )
        );

        writer.newLine();

        // Important : le résultat est immédiatement écrit sur le disque.
        writer.flush();
    }

    /**
     * Échappe une valeur selon les règles CSV :
     * - guillemets doublés
     * - valeur entourée de guillemets si elle contient
     *   une virgule, un guillemet ou un retour à la ligne.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        boolean needsQuotes =
                value.contains(",") ||
                        value.contains("\"") ||
                        value.contains("\n") ||
                        value.contains("\r");

        String escaped = value.replace("\"", "\"\"");

        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private String toCsvLine(List<String> values) {
        return values.stream()
                .map(this::escapeCsv)
                .collect(Collectors.joining(","));
    }

    /**
     * Ferme le fichier CSV.
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }
}