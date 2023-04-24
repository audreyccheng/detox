package shield.benchmarks.freehealth.transactions.patient;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.Patient;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents getting data about the patient (like zipcode and phone number)
 * It returns a structure containing the patient's data
 */
public class GetPatientData extends BenchmarkTransaction {

    private FreeHealthExperimentConfiguration config;
    private Integer uuid;

    public GetPatientData(FreeHealthGenerator generator, Integer uuid) {
        // plugins/patientbaseplugin/patientmodel.cpp : 445

        this.config = generator.getConfig();
        this.uuid = uuid;
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

            client.startTransaction();

            results = client.readAndExecute(FreeHealthConstants.patientsIdentityTable, uuid.toString());
            byte[] patientRow = results.get(0);

            if (isEmptyRow(patientRow)) {
                // patient doesn't exist
                client.commitTransaction();
                return true;
            }

            Patient resPatient = new Patient(uuid,
                    (String) patientsTable.getColumn("NAME", patientRow),
                    (String) patientsTable.getColumn("DOB", patientRow),
                    (Integer) patientsTable.getColumn("USER_UUID", patientRow),
                    (String) patientsTable.getColumn("GENDER", patientRow),
                    (Integer) patientsTable.getColumn("ZIP", patientRow),
                    (String) patientsTable.getColumn("COUNTRY", patientRow),
                    (String) patientsTable.getColumn("MAILS", patientRow),
                    (String) patientsTable.getColumn("TELS", patientRow),
                    (String) patientsTable.getColumn("NOTE", patientRow),
                    (Integer) patientsTable.getColumn("IDENT_ISACTIVE", patientRow)
            );

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
