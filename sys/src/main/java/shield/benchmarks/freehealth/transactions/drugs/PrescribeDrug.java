package shield.benchmarks.freehealth.transactions.drugs;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.EpisodeContent;
import shield.benchmarks.freehealth.models.Prescription;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents prescribing drug with id [prescription.getDrug()] to patient in an episode
 * denoted by [episodeID]. Prescribes the drug to the patient and associates the prescription with the given episode.
 * Checks if drug interacts with any drugs already perscribed and if not, creates a new prescription for the drug
 *
 * This code is distributed around in freehealth so some judgements were made in consolidating it into one transaction.
 */
public class PrescribeDrug extends BenchmarkTransaction {
    private FreeHealthExperimentConfiguration config;
    private Integer episodeID;
    private Prescription prescription;
    private FreeHealthGenerator generator;
    private int hospital;

    public PrescribeDrug(FreeHealthGenerator generator, int hospital, Integer episodeID, Prescription prescription) {
        config = generator.getConfig();
        this.episodeID = episodeID;
        this.prescription = prescription;
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
            Table episodesContentTable = client.getTable(FreeHealthConstants.episodesContentTable);
            Table episodesContentByEpisodeTable = client.getTable(FreeHealthConstants.episodesContentByEpisodeTable);
            Table nextSharedID = client.getTable(FreeHealthConstants.nextSharedIDTable);
            Table patientsIdentityTable = client.getTable(FreeHealthConstants.patientsIdentityTable);
            Table prescriptionsByPatientTable = client.getTable(FreeHealthConstants.prescriptionsByPatientTable);
            Table prescriptionsTable = client.getTable(FreeHealthConstants.prescriptionsTable);
            Table drugInteractionsTable = client.getTable(FreeHealthConstants.drugInteractionsTable);

            client.startTransaction();

            results = client.readAndExecute(FreeHealthConstants.episodesTable, episodeID.toString());
            if (isEmptyRow(results.get(0))) {
                // episode doesn't exist so can't associate a prescription
                client.commitTransaction(); //TODO @sitar should this be an abort?
                return true;
            }

            // get patient to prescribe drug to from episode
            byte[] episodeRow = results.get(0);
            Integer patientID = (Integer) episodesTable.getColumn("PATIENT_UID", episodeRow);

            client.read(FreeHealthConstants.prescriptionsByPatientTable, patientID.toString());
            client.read(FreeHealthConstants.patientsIdentityTable, patientID.toString());
            results = client.readAndExecute(FreeHealthConstants.drugsTable, prescription.getDrug().toString());

            // make sure drug to prescribe exists
            if (isEmptyRow(results.get(2))) {
                // drug doesn't exist so can't add it as a prescription
                client.abortTransaction();
                return true;
            }

            // sanity check that patient exists
            if (isEmptyRow(results.get(1))) {
                // patient doesn't exist so can't add presctiptions
                client.abortTransaction();
                return true;
            }

            // sanity check that patient exists
            if (isEmptyRow(results.get(0))) {
                // patient doesn't exist so can't add presctiptions
                client.abortTransaction();
                return true;
            }

            // check for drug interactions with already prescribed drugs
            byte[] patientPrescriptionsRow = results.get(0);
            SerializableIDSet patientPrescriptions;
            if (!isEmptyRow(patientPrescriptionsRow)) { // if patient currently has no prescriptions, we don't need to check for drug interactions
                String prescIDstr = (String) prescriptionsByPatientTable.getColumn("ID_LIST", patientPrescriptionsRow);
                patientPrescriptions = new SerializableIDSet(config, prescIDstr);

                results = client.readAndExecute(FreeHealthConstants.drugInteractionsTable, prescription.getDrug().toString());

                byte[] drugInteractionsRow = results.get(0);
                if (!isEmptyRow(drugInteractionsRow)) { // drug has no interactions so nothing to check
                    String interactionIDsStr = (String) drugInteractionsTable.getColumn("INTERACTIONS", drugInteractionsRow);
                    SerializableIDSet interactingDrugIDs = new SerializableIDSet(config, interactionIDsStr);
                    interactingDrugIDs.intersect(patientPrescriptions);
                    if (interactingDrugIDs.size() > 0) {
                        // there exists some drug that interacts with [drugID] and is perscribed to the patient - so raise error
                        client.commitTransaction();
                        return true;
                    }
                }
            } else {
                patientPrescriptions = new SerializableIDSet(config);
                patientPrescriptionsRow = prescriptionsByPatientTable.createNewRow(config.PAD_COLUMNS);
            }

            /////// create the prescription and necessary mappings ///////

            // add patient to prescription mapping
            prescriptionsByPatientTable.updateColumn("ID_LIST", patientPrescriptions.serialize(), patientPrescriptionsRow);
            patientPrescriptions.add(prescription.getDrug());
            client.write(FreeHealthConstants.prescriptionsByPatientTable, patientID.toString(), patientPrescriptionsRow);

            String nextPrescriptionKey = FreeHealthConstants.nextPrescriptionKeyPrefix + hospital;
            String nextEpisodeContentKey = FreeHealthConstants.nextEpisodeContentKeyPrefix + hospital;

            client.readForUpdate(FreeHealthConstants.nextSharedIDTable, nextPrescriptionKey);
            results = client.readForUpdateAndExecute(FreeHealthConstants.nextSharedIDTable, nextEpisodeContentKey);
            if (isEmptyRow(results.get(0)) || isEmptyRow(results.get(1))) {
                // something is really wrong and the nextSharedIDTable was not initialized correctly
                client.abortTransaction();
                return false;
            }

            // get the next prescription id from DB
            byte[] prescIDrow = results.get(0);
            Integer prescriptionID = (Integer) nextSharedID.getColumn("NEXT_ID", prescIDrow);
            int nextID = (prescriptionID + 1) % (config.MAX_PRESCRIPTIONS / config.NB_HOSPITALS);
            nextSharedID.updateColumn("NEXT_ID", nextID, prescIDrow);
            client.write(FreeHealthConstants.nextSharedIDTable, nextPrescriptionKey, prescIDrow); // increment next presc ID
            prescription.setId(generator.idFromHospitalID(hospital, prescriptionID));

            // get the next episode content id from DB
            byte[] contentIDrow = results.get(1);
            Integer epContentID = (Integer) nextSharedID.getColumn("NEXT_ID", contentIDrow);
            nextID = (epContentID + 1) % (config.MAX_EPISODE_CONTENTS / config.NB_HOSPITALS);
            nextSharedID.updateColumn("NEXT_ID", nextID, contentIDrow);
            client.writeAndExecute(FreeHealthConstants.nextSharedIDTable, nextEpisodeContentKey, contentIDrow); // increment next content ID

            // create prescription
            byte[] prescriptionRow = prescriptionsTable.createNewRow(config.PAD_COLUMNS);
            prescriptionsTable.updateColumn("ID", prescription.getId(), prescriptionRow);
            prescriptionsTable.updateColumn("PATIENT_UUID", prescription.getPatientUuid(), prescriptionRow);
            prescriptionsTable.updateColumn("LABEL", prescription.getLabel(), prescriptionRow);
            prescriptionsTable.updateColumn("DRUG", prescription.getDrug(), prescriptionRow);
            prescriptionsTable.updateColumn("DOSAGE", prescription.getDosage(), prescriptionRow);
            prescriptionsTable.updateColumn("STARTDATE", prescription.getStartdate(), prescriptionRow);
            prescriptionsTable.updateColumn("ENDDATE", prescription.getEnddate(), prescriptionRow);
            prescriptionsTable.updateColumn("DATECREATION", prescription.getDatecreation(), prescriptionRow);
            prescriptionsTable.updateColumn("VALID", prescription.getValid(), prescriptionRow);
            prescriptionsTable.updateColumn("COMMENT", prescription.getComment(), prescriptionRow);
            client.write(FreeHealthConstants.prescriptionsTable, prescription.getId().toString(), prescriptionRow);

            /////// create episode content for the prescription ///////

            EpisodeContent prescriptionEpContent = new EpisodeContent( //create episode content
                    generator.idFromHospitalID(hospital, epContentID), episodeID, 1,
                    prescription.getId().toString(), prescription.getId()
            );

            // create new row for this episode content and write its value to DB
            byte[] contentRow = episodesContentTable.createNewRow(config.PAD_COLUMNS);
            episodesContentTable.updateColumn("CONTENT_ID", prescriptionEpContent.getContentId(), contentRow);
            episodesContentTable.updateColumn("EPISODE_ID", prescriptionEpContent.getEpisodeId(), contentRow);
            episodesContentTable.updateColumn("CONTENT_TYPE", prescriptionEpContent.getContentType(), contentRow);
            episodesContentTable.updateColumn("CONTENT", prescriptionEpContent.getXmlContent(), contentRow);
            client.write(FreeHealthConstants.episodesContentTable, prescriptionEpContent.getContentId().toString(), contentRow);

            // update mapping of episode ID to episode contents
            results = client.readAndExecute(FreeHealthConstants.episodesContentByEpisodeTable, episodeID.toString());
            byte[] contentIDMapRow = results.get(0);
            if (isEmptyRow(contentIDMapRow)) {
                contentIDMapRow = episodesContentByEpisodeTable.createNewRow(config.PAD_COLUMNS);
            }
            String contentIDstr = (String) episodesContentByEpisodeTable.getColumn("ID_LIST", contentIDMapRow);
            SerializableIDSet episodeContentMapIDs = new SerializableIDSet(config, contentIDstr);

            episodeContentMapIDs.add(prescriptionEpContent.getContentId());
            episodesContentByEpisodeTable.updateColumn("ID_LIST", episodeContentMapIDs.serialize(), contentIDMapRow);
            client.write(FreeHealthConstants.episodesContentByEpisodeTable, episodeID.toString(), contentIDMapRow);

            client.commitTransaction();
            generator.setMaxKnownPrescriptionID(hospital, prescriptionID);
            generator.setMaxKnownEpisodeContentID(hospital, epContentID);
            System.out.println("SUCCESSFULLY COMMITTED NEW PR WITH ID " + prescription.getId());
            System.out.println("SUCCESSFULLY COMMITTED NEW EC WITH ID " + prescriptionEpContent.getContentId());
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}