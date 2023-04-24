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
 * A parameterized transaction that represents retreiving the prior medical history of an individual patient, indexed by patientID
 * Returns a string encoding the PMH matching the query
 */
public class GetPMH extends BenchmarkTransaction {

    private FreeHealthExperimentConfiguration config;
    private Integer patientID;

    public GetPMH(FreeHealthGenerator generator, Integer patientID) {
        // plugins/pmhplugin/pmhbase.cpp : 357
        config = generator.getConfig();
        this.patientID = patientID;
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
            Table pmhTable = client.getTable(FreeHealthConstants.pmhTable);

            client.startTransaction();

            results = client.readAndExecute(FreeHealthConstants.patientsIdentityTable, patientID.toString());
            if (isEmptyRow(results.get(0))) {
                client.abortTransaction();
                return true; //patient doesn't exist
            }

            results = client.readAndExecute(FreeHealthConstants.pmhByPatientTable, patientID.toString()); // look up PMH list
            if (!(isEmptyRow(results.get(0)))) { // make sure patient has some PMH
                byte[] pmhIDrow = results.get(0);
                SerializableIDSet pmhIDs = new SerializableIDSet(config, (String) pmhByPatientTable.getColumn("ID_LIST", pmhIDrow));
                if (pmhIDs.size() != 0) { // only add to pmh result if there are actually PMHs
                    List<Integer> pmhIDlist = pmhIDs.toList();
                    for (int i = 0; i < pmhIDlist.size() - 1; i++) {
                        client.read(FreeHealthConstants.pmhTable, pmhIDlist.get(i).toString());
                    }
                    results = client.readAndExecute(FreeHealthConstants.pmhTable, pmhIDlist.get(pmhIDlist.size() - 1).toString());
                    for (byte[] result : results) {
                        if (!isEmptyRow(result))
                            priorMedicalHistory.add(new PMH(
                                    (Integer) pmhTable.getColumn("ID", result),
                                    (Integer) pmhTable.getColumn("PATIENT_UUID", result),
                                    (Integer) pmhTable.getColumn("USER_UUID", result),
                                    (Integer) pmhTable.getColumn("CATEGORY_ID", result),
                                    (String) pmhTable.getColumn("LABEL", result),
                                    (Integer) pmhTable.getColumn("VALID", result),
                                    (String) pmhTable.getColumn("COMMENT", result),
                                    (String) pmhTable.getColumn("CREATIONDATETIME", result)
                            ));
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
