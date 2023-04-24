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
 * A parameterized transaction that represents adding an encounter with a patient.
 * Episode contents like XML and prescriptions should be added later
 */
public class CreateEncounter extends BenchmarkTransaction {

    private FreeHealthExperimentConfiguration config;
    private Episode encounter;
    private FreeHealthGenerator generator;
    private int hospital;

    public CreateEncounter(FreeHealthGenerator generator, int hospital, Episode encounter) {
        this.config = generator.getConfig();
        this.encounter = encounter;
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
            Table nextSharedID = client.getTable(FreeHealthConstants.nextSharedIDTable);

            client.startTransaction();

            // get next episode ID
            String episodeKey = FreeHealthConstants.nextEpisodeKeyPrefix + hospital;
            results = client.readForUpdateAndExecute(FreeHealthConstants.nextSharedIDTable, episodeKey);
            byte[] episodeIDrow = results.get(0);
            if (isEmptyRow(episodeIDrow)) {
                client.abortTransaction();
                return false; // something is wrong with the next shared ID table
            }
            Integer episodeID = (Integer) nextSharedID.getColumn("NEXT_ID", episodeIDrow);

            Integer nextID = (episodeID + 1) % (config.MAX_EPISODES / config.NB_HOSPITALS);
            nextSharedID.updateColumn("NEXT_ID", nextID, episodeIDrow);
            client.writeAndExecute(FreeHealthConstants.nextSharedIDTable, episodeKey, episodeIDrow); // increment next episode ID
            encounter.setEpisodeId(generator.idFromHospitalID(hospital, episodeID));

            // create new row for this encounter episode and write its value to DB
            byte[] episodeRow = episodesTable.createNewRow(config.PAD_COLUMNS);
            episodesTable.updateColumn("EPISODE_ID", encounter.getEpisodeId(), episodeRow);
            episodesTable.updateColumn("PATIENT_UID", encounter.getPatientUid(), episodeRow);
            episodesTable.updateColumn("ISVALID", encounter.getIsvalid(), episodeRow);
            episodesTable.updateColumn("LABEL", encounter.getLabel(), episodeRow);
            episodesTable.updateColumn("USERDATE", encounter.getUserdate(), episodeRow);
            episodesTable.updateColumn("DATECREATION", encounter.getDatecreation(), episodeRow);
            episodesTable.updateColumn("CREATOR", encounter.getCreator(), episodeRow);
            episodesTable.updateColumn("PRIOR", encounter.getPrior(), episodeRow);
            client.write(FreeHealthConstants.episodesContentByEpisodeTable, encounter.getEpisodeId().toString(), episodeRow);

            client.commitTransaction();
            generator.setMaxKnownEpisodeID(hospital, episodeID); // write committed so update knowledge of max known ID
            System.out.println("SUCCESSFULLY COMMITTED NEW EP WITH ID " + encounter.getEpisodeId());
            return true;
        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
