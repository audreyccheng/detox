package shield.benchmarks.freehealth.transactions.patient;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents updating data about the patient (like zipcode and phone number)
 */
public class UpdatePatientData extends BenchmarkTransaction {

    private final Integer uuid;
    private final String field;
    private final Object value;
    private final FreeHealthGenerator generator;


    public UpdatePatientData(FreeHealthGenerator generator, Integer uuid, String field, Object value) {
        // plugins/patientbaseplugin/patientmodel.cpp : 636
        this.uuid = uuid;
        this.field = field;
        this.value = value;
        this.generator = generator;
        this.client = generator.getClient();
    }

    private boolean updateVal(Table tab, byte[] row) {
        tab.updateColumn(field, value, row);
        try {
            client.write(FreeHealthConstants.patientsIdentityTable, uuid.toString(), row);
        } catch (DatabaseAbortException e) {
            return false;
        }
        return true;
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
            Table patientsDOBTable= client.getTable(FreeHealthConstants.patientsIdentityByDOBTable);
            client.startTransaction();

            // check if patient is valid
            results = client.readAndExecute(FreeHealthConstants.patientsIdentityTable, uuid.toString());
            byte[] patientRow = results.get(0);
            if (isEmptyRow(patientRow)) {
                // Invalid patient uuid
                client.abortTransaction();
                return false;
            }

            // TODO @sitar these shouldn't throw database abotrt exceptions.
            switch (field) {
                case "IDENT_ISACTIVE":
                    if (!(value instanceof Integer) || !updateVal(patientsTable, patientRow))
                        throw new DatabaseAbortException();
                    break;
                case "NAME":
                    if (!(value instanceof String) || !updateVal(patientsTable, patientRow))
                        throw new DatabaseAbortException();
                    // update the index mapping in the name table
                    String newName = (String) value;
                    String oldName = ((String) patientsTable.getColumn("NAME", patientRow)).replace("\0", ""); // remove padding from read string
                    if (oldName.equals(newName)) {
                        // no need to update if the old and new names are the same
                        client.commitTransaction();
                        return true;
                    }
                    client.read(FreeHealthConstants.patientsIdentityByNameTable, oldName);
                    results = client.readAndExecute(FreeHealthConstants.patientsIdentityByNameTable, newName);
                    byte[] oldNameRow = results.get(0);
                    byte[] newNameRow = results.get(1);
                    if (isEmptyRow(oldNameRow)) {
                        // Something is wrong with creating patients
                        client.abortTransaction();
                        return false;
                    }

                    // remove id from old name mapping
                    SerializableIDSet oldNameIDs = new SerializableIDSet(
                            generator.getConfig(),
                            (String) patientsNameTable.getColumn("ID_LIST", oldNameRow)
                    );
                    oldNameIDs.remove(uuid);

                    // add id to new name mapping
                    SerializableIDSet newNameIDs;
                    if (isEmptyRow(newNameRow)) {
                        newNameIDs = new SerializableIDSet(generator.getConfig());
                        newNameRow = patientsNameTable.createNewRow(generator.getConfig().PAD_COLUMNS);
                    } else {
                        newNameIDs = new SerializableIDSet(
                                generator.getConfig(),
                                (String) patientsNameTable.getColumn("ID_LIST", newNameRow)
                        );
                    }
                    newNameIDs.add(uuid);

                    // write updates to table
                    patientsNameTable.updateColumn("ID_LIST", oldNameIDs.serialize(), oldNameRow);
                    patientsNameTable.updateColumn("ID_LIST", newNameIDs.serialize(), newNameRow);
                    client.write(FreeHealthConstants.patientsIdentityByNameTable, oldName, oldNameRow);
                    client.write(FreeHealthConstants.patientsIdentityByNameTable, newName, newNameRow);
                    break;
                case "GENDER":
                    if (!(value instanceof String))
                        throw new DatabaseAbortException();
                    String toSave = (String) value;
                    if (toSave != "M" && toSave != "F" && toSave != "H" && toSave != "K"){
                        System.err.println("Unknown gender " + toSave);
                        client.commitTransaction();
                        return true;
                    }
                    String prev = (String) patientsTable.getColumn("GENDER", patientRow);
                    if (prev.equals(toSave)) break;
                    patientsTable.updateColumn("GENDER", toSave, patientRow);
                    client.write(FreeHealthConstants.patientsIdentityTable, uuid.toString(), patientRow);
                    break;
                case "DOB":
                    if (!(value instanceof String) || !updateVal(patientsTable, patientRow))
                        throw new DatabaseAbortException();

                    // update the index mapping in the DOB table
                    String newDOB = (String) value;
                    String oldDOB = ((String) patientsTable.getColumn("DOB", patientRow)).replace("\0", ""); // remove padding from read string
                    if (oldDOB.equals(newDOB)) {
                        // no need to update if the old and new DOBs are the same
                        client.commitTransaction();
                        return true;
                    }
                    client.read(FreeHealthConstants.patientsIdentityByDOBTable, oldDOB);
                    results = client.readAndExecute(FreeHealthConstants.patientsIdentityByDOBTable, newDOB);
                    byte[] oldDOBRow = results.get(0);
                    byte[] newDOBRow = results.get(1);
                    if (isEmptyRow(oldDOBRow)) {
                        // Something is wrong with creating patients
                        client.abortTransaction();
                        return false;
                    }
                    // remove id from old Dob mapping
                    SerializableIDSet oldDOBIDs = new SerializableIDSet(
                            generator.getConfig(),
                            (String) patientsDOBTable.getColumn("ID_LIST", oldDOBRow)
                    );
                    oldDOBIDs.remove(uuid);

                    // add id to new DOB mapping
                    SerializableIDSet newDOBIDs;
                    if (isEmptyRow(newDOBRow)) {
                        newDOBIDs = new SerializableIDSet(generator.getConfig());
                        newDOBRow = patientsDOBTable.createNewRow(generator.getConfig().PAD_COLUMNS);
                    } else {
                        newDOBIDs = new SerializableIDSet(
                                generator.getConfig(),
                                (String) patientsDOBTable.getColumn("ID_LIST", newDOBRow)
                        );
                    }
                    newDOBIDs.add(uuid);

                    // write updates to table
                    patientsDOBTable.updateColumn("ID_LIST", oldDOBIDs.serialize(), oldDOBRow);
                    patientsDOBTable.updateColumn("ID_LIST", newDOBIDs.serialize(), newDOBRow);
                    client.write(FreeHealthConstants.patientsIdentityByDOBTable, oldDOB, oldDOBRow);
                    client.write(FreeHealthConstants.patientsIdentityByDOBTable, newDOB, newDOBRow);
                    break;
                case "USER_UUID":
                    // don't update the USER_UUID
                    break;
                case "ZIP":
                    if (!(value instanceof Integer) || !updateVal(patientsTable, patientRow))
                        throw new DatabaseAbortException();
                    break;
                case "COUNTRY":
                    if (!(value instanceof String) || !updateVal(patientsTable, patientRow))
                        throw new DatabaseAbortException();
                    break;
                case "MAILS":
                    if (!(value instanceof String) || !updateVal(patientsTable, patientRow))
                        throw new DatabaseAbortException();
                    break;
                case "TELS":
                    if (!(value instanceof String) || !updateVal(patientsTable, patientRow))
                        throw new DatabaseAbortException();
                    break;
                case "NOTE":
                    if (!(value instanceof String) || !updateVal(patientsTable, patientRow))
                        throw new DatabaseAbortException();
                    break;
            }

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
