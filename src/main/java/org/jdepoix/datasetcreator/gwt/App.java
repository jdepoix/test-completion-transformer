package org.jdepoix.datasetcreator.gwt;

import org.jdepoix.ast.serialization.ASTSequentializer;
import org.jdepoix.ast.serialization.ASTSerializer;
import org.jdepoix.config.ResultDirConfig;
import org.jdepoix.datasetcreator.DatasetCreator;
import org.jdepoix.datasetcreator.DatasetStore;
import org.jdepoix.sqlite.ConnectionHandler;

import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class App {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");

        final ResultDirConfig config = new ResultDirConfig(Path.of(args[0]));

        final String datasetName = "gwt";
        final DatasetStore datasetStore = DatasetStore.create(config, datasetName);

        Logger logger = Logger.getLogger("DatasetCreator");
        FileHandler fileHandler = new FileHandler(
            config.getDatasetDir().resolve(datasetName).resolve("all.logs").toString()
        );
        logger.addHandler(fileHandler);
        fileHandler.setFormatter(new SimpleFormatter());

        new DatasetCreator(
            config,
            new GWTReportRetriever(new ConnectionHandler(config.getDbFile())),
            new GWTDatapointResolver(config, new ASTSerializer(), new ASTSequentializer()),
            datasetStore,
            logger
        ).create();
    }
}
