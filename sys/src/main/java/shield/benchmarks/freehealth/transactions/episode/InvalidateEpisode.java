package shield.benchmarks.freehealth.transactions.episode;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents removing an episode from the table of episodes via invalidation
 */
public class InvalidateEpisode extends BenchmarkTransaction {

    FreeHealthExperimentConfiguration config;
    private Integer episodeID;

    public InvalidateEpisode(FreeHealthGenerator generator, Integer episodeID) {
        // plugins/formmanagerplugin/episodebase.cpp : 1091
        this.episodeID = episodeID;
        config = generator.getConfig();
        this.client = generator.getClient();
    }

    /**
     * Attempts to execute one instance of the
     * transaction
     */
    @Override
    public boolean tryRun() {
        try {

            List<byte[]> result;

            Table episodesTable = client.getTable(FreeHealthConstants.episodesTable);

            client.startTransaction();

            result = client.readAndExecute(FreeHealthConstants.episodesTable, episodeID.toString());

            if (isEmptyRow(result.get(0))) {
                // episode doesn't exists so can't invalidate
                client.commitTransaction();
                return true;
            }

            // invalidate the episode
            byte[] episodeRow = result.get(0);
            episodesTable.updateColumn("ISVALID", 0, episodeRow);
            client.write(FreeHealthConstants.episodesTable, episodeID.toString(), episodeRow);

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
