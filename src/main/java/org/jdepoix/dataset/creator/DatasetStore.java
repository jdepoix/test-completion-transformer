package org.jdepoix.dataset.creator;

import org.jdepoix.dataset.config.ResultDirConfig;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

public class DatasetStore<T extends Datapoint> {
    private final Path datasetPath;
    private final Optional<Predicate<T>> filter;

    private DatasetStore(Path datasetPath, Optional<Predicate<T>> filter) {
        this.datasetPath = datasetPath;
        this.filter = filter;
    }

    public static <D extends Datapoint>  DatasetStore<D> create(
        ResultDirConfig config,
        String datasetName
    ) throws IOException {
        return create(config, datasetName, null);
    }

    public static <D extends Datapoint>  DatasetStore<D> create(
        ResultDirConfig config,
        String datasetName,
        Predicate<D> filter
    ) throws IOException {
        final Path datasetPath = config
            .getDatasetDir()
            .resolve(String.format("%s.jsonl", datasetName));
        Files.createDirectories(datasetPath.getParent());
        if (!Files.exists(datasetPath)) {
            Files.createFile(datasetPath);
        }
        return new DatasetStore<>(datasetPath, Optional.ofNullable(filter));
    }

    StoreWriter<T> getStoreWriter() throws FileNotFoundException {
        return StoreWriter.open(this.datasetPath, this.filter);
    }
}

class StoreWriter<T extends Datapoint> implements Closeable {
    private final FileOutputStream fileOutputStream;
    private final Optional<Predicate<T>> filter;

    StoreWriter(FileOutputStream fileOutputStream, Optional<Predicate<T>> filter) {
        this.fileOutputStream = fileOutputStream;
        this.filter = filter;
    }

    void store(T datapoint) throws IOException {
        if (this.filter.isEmpty() || this.filter.get().test(datapoint)) {
            this.fileOutputStream.write(String.format("%s\n", datapoint.toJSON()).getBytes());
        }
    }

    @Override
    public void close() throws IOException {
        this.fileOutputStream.close();
    }

    static <D extends Datapoint> StoreWriter<D> open(
        Path datasetPath, Optional<Predicate<D>> filter
    ) throws FileNotFoundException {
        return new StoreWriter<>(new FileOutputStream(datasetPath.toFile()), filter);
    }
}
