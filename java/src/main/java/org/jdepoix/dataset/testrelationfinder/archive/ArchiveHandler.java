package org.jdepoix.dataset.testrelationfinder.archive;

import org.jdepoix.dataset.config.ResultDirConfig;
import org.rauschig.jarchivelib.ArchiveStream;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.stream.Stream;

public class ArchiveHandler {
    private final Path tempUnpackDirectoryRoot;

    public ArchiveHandler(ResultDirConfig config) {
        this.tempUnpackDirectoryRoot = config.getTempDir();
    }

    public Path unpackArchive(Path archivePath) throws IOException {
        File archive = new File(archivePath.toUri());
        Archiver archiver = ArchiverFactory.createArchiver("tar", "gz");
        ArchiveStream stream = archiver.stream(archive);
        Path tempUnpackedArchivePath = this.tempUnpackDirectoryRoot.resolve(stream.getNextEntry().getName());
        stream.close();
        archiver.extract(archive, new File(this.tempUnpackDirectoryRoot.toUri()));
        return tempUnpackedArchivePath;
    }

    public void deleteTempUnpackedArchive(Path tempUnpackedArchivePath) throws IOException {
        try (Stream<Path> walk = Files.walk(tempUnpackedArchivePath)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}
