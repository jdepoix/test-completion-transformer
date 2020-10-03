package org.jdepoix.dataset.creator.gwt;

import org.jdepoix.dataset.ast.serialization.ASTSequentializer;
import org.jdepoix.dataset.ast.serialization.ASTSerializer;
import org.jdepoix.dataset.config.ResultDirConfig;
import org.jdepoix.dataset.creator.DatasetCreator;
import org.jdepoix.dataset.creator.DatasetStore;
import org.jdepoix.dataset.sqlite.ConnectionHandler;
import org.jdepoix.dataset.testrelationfinder.gwt.GWTTestRelation;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class App {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");

        final ResultDirConfig config = new ResultDirConfig(Path.of(args[0]));

        final DatasetStore<GWTDatapoint> completeDatasetStore = DatasetStore.create(
            config,
            "gwt"
        );
        final DatasetStore<GWTDatapoint> whenVisibleDatasetStore = DatasetStore.create(
            config,
            "gwt_when_in_both",
            datapoint ->
                datapoint.getWhenLocation().equals(GWTTestRelation.WhenLocation.GIVEN)
                    || datapoint.getWhenLocation().equals(GWTTestRelation.WhenLocation.BOTH)
        );
        final DatasetStore<GWTDatapoint> whenInGivenDatasetStore = DatasetStore.create(
            config,
            "gwt_when_in_then",
            datapoint -> datapoint.getWhenLocation().equals(GWTTestRelation.WhenLocation.GIVEN)
        );

        Logger logger = Logger.getLogger("DatasetCreator");
        FileHandler fileHandler = new FileHandler(
            config.getDatasetDir().resolve("all.logs").toString()
        );
        logger.addHandler(fileHandler);
        fileHandler.setFormatter(new SimpleFormatter());

        new DatasetCreator(
            config,
            new GWTReportRetriever(new ConnectionHandler(config.getDbFile())),
            new GWTDatapointResolver(config, new ASTSerializer(), new ASTSequentializer()),
            List.of(completeDatasetStore, whenVisibleDatasetStore, whenInGivenDatasetStore),
            logger
        ).create();
    }
}
