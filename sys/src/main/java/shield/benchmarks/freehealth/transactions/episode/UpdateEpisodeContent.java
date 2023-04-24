package shield.benchmarks.freehealth.transactions.episode;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.EpisodeContent;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents updating the content of a single episode
 */
public class UpdateEpisodeContent extends BenchmarkTransaction {

    private FreeHealthExperimentConfiguration config;
    private EpisodeContent content;

    public UpdateEpisodeContent(FreeHealthGenerator generator, EpisodeContent content) {
        // plugins/formmanagerplugin/episodebase.cpp : 983
        this.config = generator.getConfig();
        this.content = content;
        this.client = generator.getClient();
    }

    /**
     * Attempts to execute one instance of the
     * transaction
     */
    @Override
    public boolean tryRun() {
        try {

            List<byte[]> results;

            Table episodesContentByEpisodeTable = client.getTable(FreeHealthConstants.episodesContentByEpisodeTable);
            Table episodesContentTable = client.getTable(FreeHealthConstants.episodesContentTable);

            client.startTransaction();

            results = client.readAndExecute(FreeHealthConstants.episodesContentTable, content.getContentId().toString());
            if (isEmptyRow(results.get(0))) {
                // episode content with this ID doesnt exist
                client.abortTransaction();
                return false;
            }

            // create new row for this episode content and write its value to DB
            byte[] contentRow = results.get(0);
            episodesContentTable.updateColumn("CONTENT_TYPE", content.getContentType(), contentRow);
            episodesContentTable.updateColumn("CONTENT", content.getXmlContent(), contentRow);
            client.write(FreeHealthConstants.episodesContentTable, content.getContentId().toString(), contentRow);

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
