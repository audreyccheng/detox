package shield.benchmarks.freehealth.transactions.patient;
import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.ArrayList;
import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents looking up the names of every patient in list of UUIDs
 * It returns a list of the names of every patient in the UUID list
 */
public class GetPatientNames extends BenchmarkTransaction {

    private FreeHealthExperimentConfiguration config;
    private List<Integer> uuidList;

    public GetPatientNames(FreeHealthGenerator generator, List<Integer> uuidList) {
        // plugins/patientbaseplugin/patientbase.cpp : 355
        this.uuidList = uuidList;
        config = generator.getConfig();
        this.client = generator.getClient();
    }

    /**
     * Attempts to execute one instance of the
     * transaction
     */
    @Override
    public boolean tryRun() {
        if (uuidList.size() == 0) return true;
        try {

            List<byte[]> results;

            Table patientsTable = client.getTable(FreeHealthConstants.patientsIdentityTable);

            client.startTransaction();

            for (int i = 0; i < uuidList.size() - 1; i++) {
                client.read(FreeHealthConstants.patientsIdentityTable, uuidList.get(i).toString());
            }
            results = client.readAndExecute(FreeHealthConstants.patientsIdentityTable, uuidList.get(uuidList.size()-1).toString());

            List<String> names = new ArrayList<>();
            for (byte[] patientRow : results) {
                if (!isEmptyRow(patientRow)) {
                    names.add((String) patientsTable.getColumn("NAME", patientRow));
                }
            }

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
