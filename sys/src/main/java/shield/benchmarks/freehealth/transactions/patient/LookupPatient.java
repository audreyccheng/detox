package shield.benchmarks.freehealth.transactions.patient;
import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.ArrayList;
import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents looking up a patient in the patient identity database
 * It returns the UUID of the patient that matches the lookup fields
 */
public class LookupPatient extends BenchmarkTransaction {

    private FreeHealthExperimentConfiguration config;
    private String name;
    private String dob;
    private String user;


    public LookupPatient(FreeHealthGenerator generator, String name, String dob, String user) {
        // plugins/patientbaseplugin/patientbase.cpp : 355
        config = generator.getConfig();
        this.name = name;
        this.dob = dob;
        this.user = user;
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

            Table patientsTable= client.getTable(FreeHealthConstants.patientsIdentityTable);
            Table patientsNameTable= client.getTable(FreeHealthConstants.patientsIdentityByNameTable);
            Table patientsDOBTable = client.getTable(FreeHealthConstants.patientsIdentityByDOBTable);
            Table patientsUserTable= client.getTable(FreeHealthConstants.patientsIdentityByUserTable);

            client.startTransaction();

            // read all the keys that arent null
            if (name == null) {
                if (dob == null) {
                    if (user != null) {
                        results = client.readAndExecute(FreeHealthConstants.patientsIdentityByUserTable, user);
                    } else {
                        results = new ArrayList<>();
                    }
                } else {
                    if (user != null) client.read(FreeHealthConstants.patientsIdentityByUserTable, user);
                    results = client.readAndExecute(FreeHealthConstants.patientsIdentityByDOBTable, dob);
                }
            } else {
                if (user != null) client.read(FreeHealthConstants.patientsIdentityByUserTable, user);
                if (dob != null) client.read(FreeHealthConstants.patientsIdentityByDOBTable, dob);
                results = client.readAndExecute(FreeHealthConstants.patientsIdentityByNameTable, name);
            }

            // Narrow down to a list of ids that appear in every result
            SerializableIDSet ids;
            if (results.size() > 0) {
                // this works because all three tables are structured identically
                byte[] nameRow = results.get(0);
                if (isEmptyRow(results.get(0))) nameRow = patientsNameTable.createNewRow(config.PAD_COLUMNS);
                ids = new SerializableIDSet(config, (String) patientsNameTable.getColumn("ID_LIST", nameRow));
            } else {
                ids = new SerializableIDSet(config);
            }
            if (results.size() > 1) {
                for (int i = 1; i < results.size(); i++){
                    byte[] nameRow = results.get(i);
                    if (isEmptyRow(results.get(i))) nameRow = patientsNameTable.createNewRow(config.PAD_COLUMNS);
                    ids.intersect(new SerializableIDSet(config, (String) patientsNameTable.getColumn("ID_LIST", nameRow)));
                }
            }

            List<Integer> idList = ids.toList();

            // check that each id in the resulting list actually exists
            if (idList.size() > 0) {
                for (int i = 0; i < ids.size() - 1; i++) {
                    client.read(FreeHealthConstants.patientsIdentityTable, idList.get(i).toString());
                }
                results = client.readAndExecute(FreeHealthConstants.patientsIdentityTable, idList.get(idList.size()-1).toString());

                for (byte[] res : results) {
                    if (isEmptyRow(res)) {
                        client.abortTransaction();
                        return false; // user does not actually exist so error out.
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
