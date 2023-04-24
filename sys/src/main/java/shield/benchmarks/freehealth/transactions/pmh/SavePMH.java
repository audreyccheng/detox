package shield.benchmarks.freehealth.transactions.pmh;
import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.PMH;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.ArrayList;
import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents saving the PMH for a patient indexed by PMH ID
 * If the PMH data already exists in the database, data is updated else data is inserted
 */
public class SavePMH extends BenchmarkTransaction {

    private PMH pmhToSave;
    private FreeHealthExperimentConfiguration config;

    public SavePMH(FreeHealthGenerator generator, PMH PMHData) {
        // plugins/pmhplugin/pmhbase.cpp : 471
        // also requires    updatePmhData : 520 and savePmhEpisodeData : 576
        config = generator.getConfig();
        pmhToSave = PMHData;
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
            List<PMH> priorMedicalHistory = new ArrayList<>();

            Table patientsTable= client.getTable(FreeHealthConstants.patientsIdentityTable);
            Table pmhByPatientTable = client.getTable(FreeHealthConstants.pmhByPatientTable);
            Table pmhByUserTable = client.getTable(FreeHealthConstants.pmhByUserTable);
            Table pmhTable = client.getTable(FreeHealthConstants.pmhTable);

            client.startTransaction();

            // TODO @sitar should this be able to create new PMHs if no ID is provided?

            results = client.readAndExecute(FreeHealthConstants.patientsIdentityTable, pmhToSave.getPatientUuid().toString());
            if (isEmptyRow(results.get(0))) {
                client.abortTransaction();
                return false; //patient doesn't exist
            }
            results = client.readAndExecute(FreeHealthConstants.pmhTable, pmhToSave.getId().toString()); // check if PMH already exists

            byte[] newPMHRow = results.get(0);
            if (isEmptyRow(newPMHRow)) { // create new PMH
                newPMHRow = pmhTable.createNewRow(config.PAD_COLUMNS);
            }

            pmhTable.updateColumn("ID", pmhToSave.getId(), newPMHRow);
            pmhTable.updateColumn("PATIENT_UUID", pmhToSave.getPatientUuid(), newPMHRow);
            pmhTable.updateColumn("USER_UUID", pmhToSave.getUserUuid(), newPMHRow);
            pmhTable.updateColumn("CATEGORY_ID", pmhToSave.getCategoryId(), newPMHRow);
            pmhTable.updateColumn("LABEL", pmhToSave.getLabel(), newPMHRow);
            pmhTable.updateColumn("VALID", pmhToSave.getValid(), newPMHRow);
            pmhTable.updateColumn("COMMENT", pmhToSave.getComment(), newPMHRow);
            pmhTable.updateColumn("CREATIONDATETIME", pmhToSave.getCreationdatetime(), newPMHRow);
            client.write(FreeHealthConstants.pmhTable, pmhToSave.getId().toString(), newPMHRow);

            client.read(FreeHealthConstants.pmhByPatientTable, pmhToSave.getPatientUuid().toString()); // look up PMH by patient list
            results = client.readAndExecute(FreeHealthConstants.pmhByUserTable, pmhToSave.getUserUuid().toString()); // look up PMH by user list

            // update patient ID to PMH ids mapping
            byte[] pmhPatientIDsRow = results.get(0);
            if (isEmptyRow(pmhPatientIDsRow)) {
                pmhPatientIDsRow = pmhByPatientTable.createNewRow(config.PAD_COLUMNS);
            }
            SerializableIDSet pmhPatientIDs = new SerializableIDSet(config, (String) pmhByPatientTable.getColumn("ID_LIST", pmhPatientIDsRow));
            pmhPatientIDs.add(pmhToSave.getId());
            pmhByPatientTable.updateColumn("ID_LIST", pmhPatientIDs.serialize(), pmhPatientIDsRow);
            client.write(FreeHealthConstants.pmhByPatientTable, pmhToSave.getPatientUuid().toString(), pmhPatientIDsRow);

            // update user ID to PMH ids mapping
            byte[] pmhUserIDsRow = results.get(1);
            if (isEmptyRow(pmhUserIDsRow)) {
                pmhUserIDsRow = pmhByUserTable.createNewRow(config.PAD_COLUMNS);
            }
            SerializableIDSet pmhUserIDs = new SerializableIDSet(config, (String) pmhByUserTable.getColumn("ID_LIST", pmhUserIDsRow));
            pmhUserIDs.add(pmhToSave.getId());
            pmhByUserTable.updateColumn("ID_LIST", pmhUserIDs.serialize(), pmhUserIDsRow);
            client.write(FreeHealthConstants.pmhByUserTable, pmhToSave.getUserUuid().toString(), pmhUserIDsRow);

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }


    }
}
