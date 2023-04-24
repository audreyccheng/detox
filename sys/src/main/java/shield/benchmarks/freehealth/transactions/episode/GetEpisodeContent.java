package shield.benchmarks.freehealth.transactions.episode;
import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.EpisodeContent;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.ArrayList;
import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents looking up the XML or prescription content of a single episode
 * Returns an EpisodeContent list representing the contents of some episode
 */
public class GetEpisodeContent extends BenchmarkTransaction {
    private FreeHealthExperimentConfiguration config;
    private Integer episodeID;

    public GetEpisodeContent(FreeHealthGenerator generator, Integer episodeID) {
        // plugins/formmanagerplugin/episodebase.cpp : 1307
        config = generator.getConfig();
        this.episodeID = episodeID;
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
            List<EpisodeContent> contents;

            Table episodesTable = client.getTable(FreeHealthConstants.episodesTable);
            Table episodesContentByEpisodeTable = client.getTable(FreeHealthConstants.episodesContentByEpisodeTable);
            Table episodesContentTable = client.getTable(FreeHealthConstants.episodesContentTable);

            client.startTransaction();

            results = client.readAndExecute(FreeHealthConstants.episodesTable, episodeID.toString());

            if (isEmptyRow(results.get(0))) {
                // episode doesn't exist so can't get content
                client.commitTransaction();
                return true; //TODO: should we return true or false here? should we abort?
            }

            results = client.readAndExecute(FreeHealthConstants.episodesContentByEpisodeTable, episodeID.toString());

            if (isEmptyRow(results.get(0))) {
                // episode exists but has no content so return empty list
                contents = new ArrayList<>();
                client.commitTransaction();
                return true;
            }

            String contentIDstr = (String) episodesContentByEpisodeTable.getColumn("ID_LIST", results.get(0));
            List<Integer> contentIDs;

            try {
                contentIDs = (new SerializableIDSet(config, contentIDstr)).toList();
            } catch (DatabaseAbortException e) {
                client.abortTransaction();
                return false;
            }

            if (contentIDs.size() == 0) {
                // episode exists but has no content so return empty list
                contents = new ArrayList<>();
                client.commitTransaction();
                return true;
            }

            for (int i = 0; i < contentIDs.size() - 1; i++) {
                client.read(FreeHealthConstants.episodesContentTable, contentIDs.get(i).toString());
            }
            results = client.readAndExecute(FreeHealthConstants.episodesContentTable, contentIDs.get(contentIDs.size()-1).toString());

            contents = new ArrayList<>();
            for (byte[] contentRow : results) {
                if (!isEmptyRow(contentRow)) {
                    EpisodeContent content = new EpisodeContent(
                            (Integer) episodesContentTable.getColumn("CONTENT_ID", contentRow),
                            (Integer) episodesContentTable.getColumn("EPISODE_ID", contentRow),
                            (Integer) episodesContentTable.getColumn("CONTENT_TYPE", contentRow),
                            ((String) episodesContentTable.getColumn("CONTENT", contentRow)).replace("\0", ""),
                            null); // set prescription ID to null and update next
                    if (content.getContentType().equals(FreeHealthConstants.PRESCRIPTION_CONTENT_TYPE))
                        content.setPrescription(Integer.parseInt(content.getXmlContent()));
                    contents.add(content);
                }
            }

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
