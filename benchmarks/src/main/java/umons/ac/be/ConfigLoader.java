package umons.ac.be;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public class ConfigLoader {
    public static ExperimentConfig load(
            String datasetConfigFile,
            String algorithmConfigFile
    ) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        ExperimentConfig config = new ExperimentConfig();

        mapper.readerForUpdating(config)
                .readValue(new File(datasetConfigFile));

        mapper.readerForUpdating(config)
                .readValue(new File(algorithmConfigFile));

        return config;
    }
}