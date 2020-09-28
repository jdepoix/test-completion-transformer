package org.jdepoix.datasetcreator;

import org.jdepoix.config.ResultDirConfig;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatasetStore {
    private final Path datasetPath;

    private DatasetStore(Path datasetPath) {
        this.datasetPath = datasetPath;
    }

    public static DatasetStore create(ResultDirConfig config, String datasetName) throws IOException {
        final Path datasetPath = config
            .getDatasetDir()
            .resolve(datasetName)
            .resolve(String.format("%s.jsonl", datasetName));
        Files.createDirectories(datasetPath.getParent());
        Files.createFile(datasetPath);
        return new DatasetStore(datasetPath);
    }

    StoreWriter getStoreWriter() throws FileNotFoundException {
        return StoreWriter.open(this.datasetPath);
    }
}

class StoreWriter implements Closeable {
    private final FileOutputStream fileOutputStream;

    private StoreWriter(FileOutputStream fileOutputStream) {
        this.fileOutputStream = fileOutputStream;
    }

    void store(Datapoint datapoint) throws IOException {
        this.fileOutputStream.write(String.format("%s\n", datapoint.toJSON()).getBytes());
    }

    @Override
    public void close() throws IOException {
        this.fileOutputStream.close();
    }

    static StoreWriter open(Path datasetPath) throws FileNotFoundException {
        return new StoreWriter(new FileOutputStream(datasetPath.toFile()));
    }
}
