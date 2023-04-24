package shield.benchmarks.freehealth.transactions.episode;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.Episode;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents looking up all episodes that match some arbitrary query string
 * Returns a list of all episodes that match the inputted query
 */
public class GetEpisodes extends BenchmarkTransaction {

    private FreeHealthExperimentConfiguration config;
    private Optional<Integer> patientID;
    private Optional<Integer> userID;
    private Optional<Boolean> isValid;

    // must contain at least 1 of patientID or userID. the rest are optional filters
    public GetEpisodes(FreeHealthGenerator generator, Optional<Integer> patientID, Optional<Integer> userID, Optional<Boolean> isValid) {
        // plugins/formmanagerplugin/episodebase.cpp : 1144
        if (!(patientID.isPresent() || userID.isPresent()))
            throw new RuntimeException("Get episodes query requires at least one of patient ID or user ID");
        config = generator.getConfig();
        this.patientID = patientID;
        this.userID = userID;
        this.isValid = isValid;
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
            List<Episode> episodes;

            Table episodesTable = client.getTable(FreeHealthConstants.episodesTable);
            Table episodesByPatientTable = client.getTable(FreeHealthConstants.episodesByPatientTable);
            Table episodesByUserTable = client.getTable(FreeHealthConstants.episodesByUserTable);

            client.startTransaction();

            SerializableIDSet episodeIDs = new SerializableIDSet(config);
            if (patientID.isPresent()) {
                results = client.readAndExecute(FreeHealthConstants.episodesByPatientTable, patientID.get().toString());
                byte[] episodesByPatientRow = results.get(0);
                if (isEmptyRow(episodesByPatientRow)) {
                    episodesByPatientRow = episodesByUserTable.createNewRow(config.PAD_COLUMNS);
                }
                String epByPatientStr = (String) episodesByPatientTable.getColumn("ID_LIST", episodesByPatientRow);
                SerializableIDSet episodeIDsByPatient = new SerializableIDSet(config, epByPatientStr);
                episodeIDs.union(episodeIDsByPatient);
            }

            if (userID.isPresent()) {
                results = client.readAndExecute(FreeHealthConstants.episodesByUserTable, userID.get().toString());
                byte[] episodesByUserRow = results.get(0);
                if (isEmptyRow(episodesByUserRow)) {
                    episodesByUserRow = episodesByUserTable.createNewRow(config.PAD_COLUMNS);
                }
                String epByUserStr = (String) episodesByUserTable.getColumn("ID_LIST", episodesByUserRow);
                SerializableIDSet episodeIDsByUser = new SerializableIDSet(config, epByUserStr);
                if (patientID.isPresent()) {
                    episodeIDs.intersect(episodeIDsByUser);
                } else {
                    episodeIDs.union(episodeIDsByUser);
                }
            }

            if (episodeIDs.size() == 0) {
                // query returns no episodes so return empty list
                episodes = new ArrayList<>();
                client.commitTransaction();
                return true;
            }

            List<Integer> episodeIDList = episodeIDs.toList();
            for (int i = 0; i < episodeIDList.size() - 1; i++) {
                client.read(FreeHealthConstants.episodesTable, episodeIDList.get(i).toString());
            }
            results = client.readAndExecute(FreeHealthConstants.episodesTable, episodeIDList.get(episodeIDList.size()-1).toString());

            episodes = new ArrayList<>();
            for (byte[] episodeRow : results) {
                if (!isEmptyRow(episodeRow)) { // only add readable episodes
                    Episode episode = new Episode(
                            (Integer) episodesTable.getColumn("EPISODE_ID", episodeRow),
                            (Integer) episodesTable.getColumn("PATIENT_UID", episodeRow),
                            (Integer) episodesTable.getColumn("ISVALID", episodeRow),
                            (String) episodesTable.getColumn("LABEL", episodeRow),
                            (String) episodesTable.getColumn("USERDATE", episodeRow),
                            (String) episodesTable.getColumn("DATECREATION", episodeRow),
                            (Integer) episodesTable.getColumn("CREATOR", episodeRow),
                            (Integer) episodesTable.getColumn("PRIOR", episodeRow)
                    );
                    // filter by validity if necessary
                    if (isValid.isPresent()) {
                        if ((isValid.get() && episode.getIsvalid().equals(1)) || (!isValid.get() && episode.getIsvalid().equals(0))) {
                            episodes.add(episode);
                        }
                    } else {
                        episodes.add(episode);
                    }
                }
            }

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
