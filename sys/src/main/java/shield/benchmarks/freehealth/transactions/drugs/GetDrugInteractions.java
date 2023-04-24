package shield.benchmarks.freehealth.transactions.drugs;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.Drug;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.ArrayList;
import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents looking up drugs that interact with the input drug
 * Returns a Drug list representing the drugs that interact poorly with the input drug
 */
public class GetDrugInteractions extends BenchmarkTransaction {
    private FreeHealthExperimentConfiguration config;
    private Integer drugID;

    public GetDrugInteractions(FreeHealthGenerator generator, Integer drugID) {
        config = generator.getConfig();
        this.drugID = drugID;
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
            List<Drug> interactingDrugs;

            Table drugsTable = client.getTable(FreeHealthConstants.drugsTable);
            Table drugInteractionsTable = client.getTable(FreeHealthConstants.drugInteractionsTable);

            client.startTransaction();

            results = client.readAndExecute(FreeHealthConstants.drugsTable, drugID.toString());

            if (isEmptyRow(results.get(0))) {
                // drug doesn't exist so can't check for interactions
                // TODO: what should we do here?
                client.abortTransaction();
                return true;
            }

            results = client.readAndExecute(FreeHealthConstants.drugInteractionsTable, drugID.toString());

            if (isEmptyRow(results.get(0))) {
                // drug exists but has no interactions so return empty list
                interactingDrugs = new ArrayList<>();
                client.commitTransaction();
                return true;
            }


            String interactionIDsStr = (String) drugInteractionsTable.getColumn("INTERACTIONS", results.get(0));
            List<Integer> interactingDrugIDs = (new SerializableIDSet(config, interactionIDsStr)).toList();

            for (int i = 0; i < interactingDrugIDs.size() - 1; i++) {
                client.read(FreeHealthConstants.drugsTable, interactingDrugIDs.get(i).toString());
            }
            results = client.readAndExecute(FreeHealthConstants.drugsTable, interactingDrugIDs.get(interactingDrugIDs.size() - 1).toString());

            interactingDrugs = new ArrayList<>();
            for (byte[] drugRow : results) {
                if (!isEmptyRow(drugRow)) { // just add drugs that have an entry
                    Drug drug = new Drug(
                            (Integer) drugsTable.getColumn("ID", drugRow),
                            (String) drugsTable.getColumn("NAME", drugRow),
                            (Integer) drugsTable.getColumn("STRENGTH", drugRow),
                            (Integer) drugsTable.getColumn("ATC_ID", drugRow));
                    interactingDrugs.add(drug);
                }
            }



            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}