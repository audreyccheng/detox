package shield.benchmarks.freehealth.transactions.patient;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.Patient;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;


/**
 * A parameterized transaction that represents creating a new patient with inputted description fields
 * It returns the UUID of the newly created patient.
 */
public class CreatePatient extends BenchmarkTransaction {

    private Patient patient;
    private FreeHealthGenerator generator;
    private final FreeHealthExperimentConfiguration config;
    private final int hospital;

    // plugins/patientbaseplugin/patientbase.cpp : 241
    public CreatePatient(FreeHealthGenerator generator, int hospital, Patient patient) {
        this.patient = patient;
        this.config = generator.getConfig();
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

            Table patientsTable = client.getTable(FreeHealthConstants.patientsIdentityTable);
            Table patientsNameTable = client.getTable(FreeHealthConstants.patientsIdentityByNameTable);
            Table patientsDOBTable = client.getTable(FreeHealthConstants.patientsIdentityByDOBTable);
            Table patientsUserTable = client.getTable(FreeHealthConstants.patientsIdentityByUserTable);
            Table nextSharedID = client.getTable(FreeHealthConstants.nextSharedIDTable);

            client.startTransaction();

            // get next patient ID
            String nextPatientKey = FreeHealthConstants.nextPatientKeyPrefix + hospital;
            results = client.readForUpdateAndExecute(FreeHealthConstants.nextSharedIDTable, nextPatientKey);
            byte[] patientIDrow = results.get(0);
            if (isEmptyRow(patientIDrow)) {
                // something is really wrong and the nextSharedIDTable was not initialized correctly
                client.abortTransaction();
                return false;
            }
            Integer patientID = (Integer) nextSharedID.getColumn("NEXT_ID", patientIDrow);
            Integer nextID = (patientID + 1) % (config.MAX_PATIENTS / config.NB_HOSPITALS);
            nextSharedID.updateColumn("NEXT_ID", nextID, patientIDrow);
            client.writeAndExecute(FreeHealthConstants.nextSharedIDTable, nextPatientKey, patientIDrow); // increment next presc ID

            patient.setID(generator.idFromHospitalID(hospital, patientID));
            // set aspects of the patient that are deterministically generated from patient ID
            patient.setName("name" + patient.getID() % (config.NB_PATIENTS/2));
            patient.setDateOfBirth(generator.IDtoDate(patient.getID()));
            patient.setUserUuid(patient.getID() % config.NB_USERS);

            byte[] newPatientRow = patientsTable.createNewRow(config.PAD_COLUMNS);
            patientsTable.updateColumn("IDENT_ID", patient.getID(), newPatientRow);
            patientsTable.updateColumn("NAME", patient.getName(), newPatientRow);
            patientsTable.updateColumn("DOB", patient.getDateOfBirth(), newPatientRow);
            patientsTable.updateColumn("USER_UUID", patient.getUserUuid(), newPatientRow);
            patientsTable.updateColumn("GENDER", patient.getGender(), newPatientRow);
            patientsTable.updateColumn("ZIP", patient.getZip(), newPatientRow);
            patientsTable.updateColumn("COUNTRY", patient.getCountry(), newPatientRow);
            patientsTable.updateColumn("MAILS", patient.getMails(), newPatientRow);
            patientsTable.updateColumn("TELS", patient.getTels(), newPatientRow);
            patientsTable.updateColumn("NOTE", patient.getNote(), newPatientRow);
            patientsTable.updateColumn("IDENT_ISACTIVE", patient.getIsActive(), newPatientRow);
            client.write(FreeHealthConstants.patientsIdentityTable, patient.getID().toString(), newPatientRow);

//            If patientNameRow is empty, create a new row otherwise update list of UUIDs, same for DOB and
            addToIndex(patientsNameTable, patient.getName(), patient.getID());
            addToIndex(patientsDOBTable, patient.getDateOfBirth(), patient.getID());
            addToIndex(patientsUserTable, patient.getUserUuid().toString(), patient.getID());

            client.commitTransaction();
            generator.setMaxKnownPatientID(hospital, patientID);
            System.out.println("SUCCESSFULLY COMMITTED NEW PA WITH ID " + patient.getID());
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }

    }

    /* Adds to key to value mapping or updates key's list of values in table */
    private void addToIndex(Table table, String key, Integer value) throws DatabaseAbortException {
        List<byte[]> results = client.readAndExecute(table.getTableName(), key);
        byte[] valueRow = results.get(0);

        // check if key is already in the table
        if (isEmptyRow(valueRow)) {
            valueRow = table.createNewRow(config.PAD_COLUMNS);
        }
        String strValList = (String) table.getColumn("ID_LIST", valueRow);
        SerializableIDSet values = new SerializableIDSet(config, strValList);
        values.add(value);
        table.updateColumn("ID_LIST", values.serialize(), valueRow);
        client.write(table.getTableName(), key, valueRow);
    }

}
