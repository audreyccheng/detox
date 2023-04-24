package shield.benchmarks.freehealth.transactions.drugs;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.Prescription;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.ArrayList;
import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents looking up all the prescriptions associated with some patient
 * Returns an Prescription list representing the drug prescriptions assigned to some patient
 */
public class GetPrescriptions extends BenchmarkTransaction {
    private FreeHealthExperimentConfiguration config;
    private Integer patientID;

    public GetPrescriptions(FreeHealthGenerator generator, Integer patientID) {
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
            List<Prescription> prescriptions;

            Table patientsIdentityTable = client.getTable(FreeHealthConstants.patientsIdentityTable);
            Table prescriptionsByPatientTable = client.getTable(FreeHealthConstants.prescriptionsByPatientTable);
            Table prescriptionsTable = client.getTable(FreeHealthConstants.prescriptionsTable);

            client.startTransaction();

            results = client.readAndExecute(FreeHealthConstants.patientsIdentityTable, patientID.toString());

            if (isEmptyRow(results.get(0))) {
                // patient doesn't exist so can't get presctiptions
                client.abortTransaction();
                return true;
            }

            results = client.readAndExecute(FreeHealthConstants.prescriptionsByPatientTable, patientID.toString());

            if (isEmptyRow(results.get(0))) {
                // patient exists but has no prescriptions so return empty list
                prescriptions = new ArrayList<>();
                client.commitTransaction();
                return true;
            }

            String prescIDstr = (String) prescriptionsByPatientTable.getColumn("ID_LIST", results.get(0));
            List<Integer> prescriptionIDs = (new SerializableIDSet(config, prescIDstr)).toList();

            for (int i = 0; i < prescriptionIDs.size() - 1; i++) {
                client.read(FreeHealthConstants.prescriptionsTable, prescriptionIDs.get(i).toString());
            }
            results = client.readAndExecute(FreeHealthConstants.prescriptionsTable, prescriptionIDs.get(prescriptionIDs.size()-1).toString());

            prescriptions = new ArrayList<>();
            for (byte[] prescriptionRow : results) {
                if (!isEmptyRow(prescriptionRow)) {
                    Prescription prescription = new Prescription(
                            (Integer) prescriptionsTable.getColumn("ID", prescriptionRow),
                            (Integer) prescriptionsTable.getColumn("PATIENT_UUID", prescriptionRow),
                            (String) prescriptionsTable.getColumn("LABEL", prescriptionRow),
                            (Integer) prescriptionsTable.getColumn("DRUG", prescriptionRow),
                            (String) prescriptionsTable.getColumn("DOSAGE", prescriptionRow),
                            (String) prescriptionsTable.getColumn("STARTDATE", prescriptionRow),
                            (String) prescriptionsTable.getColumn("ENDDATE", prescriptionRow),
                            (String) prescriptionsTable.getColumn("DATECREATION", prescriptionRow),
                            (Integer) prescriptionsTable.getColumn("VALID", prescriptionRow),
                            (String) prescriptionsTable.getColumn("COMMENT", prescriptionRow)); // set prescription ID to null and update next
                    prescriptions.add(prescription);
                }
            }

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}