package shield.benchmarks.freehealth.transactions.episode;
import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.Episode;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents updating a list of episodes (not content) in the episodes table.
 * Updates every episode in the list and returns true if all succeeds
 */
public class UpdateEpisodes extends BenchmarkTransaction {

    private FreeHealthExperimentConfiguration config;
    private List<Episode> episodeList;

    public UpdateEpisodes(FreeHealthGenerator generator, List<Episode> episodeList) {
        // plugins/formmanagerplugin/episodebase.cpp : 841
        config = generator.getConfig();
        this.episodeList = episodeList;
        this.client = generator.getClient();
    }

    /**
     * Attempts to execute one instance of the
     * transaction
     */
    @Override
    public boolean tryRun() {
        try {

            List<byte[]> episodeReadRows;

            Table episodesTable = client.getTable(FreeHealthConstants.episodesTable);

            if (episodeList.size() == 0) {
                // no episodes to update so exit
                return true;
            }

            client.startTransaction();

            for (int i = 0; i < episodeList.size() - 1; i++) {
                client.read(FreeHealthConstants.episodesTable, episodeList.get(i).getEpisodeId().toString());
            }
            episodeReadRows = client.readAndExecute(FreeHealthConstants.episodesTable, episodeList.get(episodeList.size() - 1).getEpisodeId().toString());

            for (int i = 0; i < episodeList.size(); i++) {
                byte[] episodeRow = episodeReadRows.get(i);
                if (isEmptyRow(episodeRow)) {
                    // episode doesn't exists so can't update
                    client.commitTransaction(); // TODO: @sitar should this be abort?
                    return true;
                }
                // update all the columns that aren't null in the episode to update
                Episode episode = episodeList.get(i);
                if (episode.getIsvalid() != null) episodesTable.updateColumn("ISVALID", episode.getIsvalid(), episodeRow);
                if (episode.getLabel() != null) episodesTable.updateColumn("LABEL", episode.getLabel(), episodeRow);
                if (episode.getUserdate() != null) episodesTable.updateColumn("USERDATE", episode.getUserdate(), episodeRow);
                if (episode.getDatecreation() != null) episodesTable.updateColumn("DATECREATION", episode.getDatecreation(), episodeRow);
                if (episode.getCreator() != null) episodesTable.updateColumn("CREATOR", episode.getCreator(), episodeRow);
                if (episode.getPrior() != null) episodesTable.updateColumn("PRIOR", episode.getPrior(), episodeRow);
                client.write(FreeHealthConstants.episodesTable, episode.getEpisodeId().toString(), episodeRow);
            }

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
