package shield.benchmarks.freehealth.transactions.episode;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.EpisodeContent;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents attaching a new EpisodeContent to an episode with ID episodeID
 */
public class AddEpisodeContent extends BenchmarkTransaction {
    private FreeHealthExperimentConfiguration config;
    private Integer episodeID;
    private EpisodeContent content;
    private FreeHealthGenerator generator;
    private int hospital;

    public AddEpisodeContent(FreeHealthGenerator generator, int hospital, Integer episodeID, EpisodeContent content) {
        // plugins/formmanagerplugin/episodebase.cpp : 1307
        config = generator.getConfig();
        this.episodeID = episodeID;
        this.content = content;
        this.generator = generator;
        this.client = generator.getClient();
        this.hospital = hospital;
    }

    /**
     * Attempts to execute one instance of the
     * transaction
     */
    @Override
    public boolean tryRun() {
        try {

            List<byte[]> results;

            Table episodesTable = client.getTable(FreeHealthConstants.episodesTable);
            Table episodesContentByEpisodeTable = client.getTable(FreeHealthConstants.episodesContentByEpisodeTable);
            Table episodesContentTable = client.getTable(FreeHealthConstants.episodesContentTable);
            Table nextSharedID = client.getTable(FreeHealthConstants.nextSharedIDTable);

            client.startTransaction();

            String nextEpisodeContentKey = FreeHealthConstants.nextEpisodeContentKeyPrefix + hospital;

            results = client.readAndExecute(FreeHealthConstants.episodesTable, episodeID.toString());
            if (isEmptyRow(results.get(0))) {
                // episode doesn't exist so can't add content
                client.commitTransaction();
                return true;
            }

            results = client.readForUpdateAndExecute(FreeHealthConstants.nextSharedIDTable, nextEpisodeContentKey);
            if (isEmptyRow(results.get(0))) {
                // something is really wrong and the nextSharedIDTable was not initialized correctly
                client.abortTransaction();
                System.out.println("Big problem!!");
                return false;
            }

            byte[] contentIDrow = results.get(0);

            Integer epContentID = (Integer) nextSharedID.getColumn("NEXT_ID", contentIDrow);
            Integer nextID = (epContentID + 1) % (config.MAX_EPISODE_CONTENTS / config.NB_HOSPITALS);
            nextSharedID.updateColumn("NEXT_ID", nextID, contentIDrow);
            client.writeAndExecute(FreeHealthConstants.nextSharedIDTable, nextEpisodeContentKey, contentIDrow); // increment next content ID
            content.setContentId(generator.idFromHospitalID(hospital, epContentID));

            // create new row for this episode content and write its value to DB
            byte[] contentRow = episodesContentTable.createNewRow(config.PAD_COLUMNS);
            episodesContentTable.updateColumn("CONTENT_ID", content.getContentId(), contentRow);
            episodesContentTable.updateColumn("EPISODE_ID", content.getEpisodeId(), contentRow);
            episodesContentTable.updateColumn("CONTENT_TYPE", content.getContentType(), contentRow);
            episodesContentTable.updateColumn("CONTENT", content.getXmlContent(), contentRow);
            client.write(FreeHealthConstants.episodesContentTable, content.getContentId().toString(), contentRow);

            // update mapping of episode ID to episode contents
            results = client.readAndExecute(FreeHealthConstants.episodesContentByEpisodeTable, episodeID.toString());
            byte[] contentIDMapRow = results.get(0); // if this is empty, will just create a new one
            if (isEmptyRow(contentIDMapRow)) {
                contentIDMapRow = episodesContentByEpisodeTable.createNewRow(config.PAD_COLUMNS);
            }
            String contentIDstr = (String) episodesContentByEpisodeTable.getColumn("ID_LIST", contentIDMapRow);
            SerializableIDSet episodeContentMapIDs = new SerializableIDSet(config, contentIDstr);
            episodeContentMapIDs.add(content.getContentId());
            episodesContentByEpisodeTable.updateColumn("ID_LIST", episodeContentMapIDs.serialize(), contentIDMapRow);
            client.write(FreeHealthConstants.episodesContentByEpisodeTable, episodeID.toString(), contentIDMapRow);

            client.commitTransaction();
            generator.setMaxKnownEpisodeContentID(hospital, epContentID);
            System.out.println("SUCCESSFULLY COMMITTED NEW EC WITH ID " + content.getContentId());
            return true;
        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
