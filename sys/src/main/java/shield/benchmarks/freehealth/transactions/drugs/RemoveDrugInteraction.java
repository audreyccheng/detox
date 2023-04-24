package shield.benchmarks.freehealth.transactions.drugs;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents marking that two drugs DON'T interact with each other
 */
public class RemoveDrugInteraction extends BenchmarkTransaction {
    private FreeHealthExperimentConfiguration config;
    private Integer drug1ID;
    private Integer drug2ID;

    public RemoveDrugInteraction(FreeHealthGenerator generator, Integer drug1ID, Integer drug2ID) {
        config = generator.getConfig();
        this.drug1ID = drug1ID;
        this.drug2ID = drug2ID;
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

            Table drugsTable = client.getTable(FreeHealthConstants.drugsTable);
            Table drugInteractionsTable = client.getTable(FreeHealthConstants.drugInteractionsTable);

            if (drug1ID.equals(drug2ID)) return true;

            client.startTransaction();

            client.read(FreeHealthConstants.drugsTable, drug1ID.toString());
            results = client.readAndExecute(FreeHealthConstants.drugsTable, drug2ID.toString());

            if (isEmptyRow(results.get(0)) || isEmptyRow(results.get(1))) {
                // one of the two drugs doesn't exist so can't delete an interaction
                client.abortTransaction();
                return false;
            }

            client.read(FreeHealthConstants.drugInteractionsTable, drug1ID.toString());
            results = client.readAndExecute(FreeHealthConstants.drugInteractionsTable, drug2ID.toString());

            byte[] interactions1Row = results.get(0);
            byte[] interactions2Row = results.get(1);
            if (isEmptyRow(results.get(0)) || isEmptyRow(results.get(1))) {
                // one of the two drugs doesn't have any interactions so there's nothing to remove
                client.commitTransaction();
                return true;
            }

            String interactionIDsStr = (String) drugInteractionsTable.getColumn("INTERACTIONS", interactions1Row);
            SerializableIDSet interactingIDs = new SerializableIDSet(config, interactionIDsStr);
            interactingIDs.remove(drug2ID); // remove drug 2 from list of interacting drugs (if no interaction, this does nothing)
            drugInteractionsTable.updateColumn("INTERACTIONS", interactingIDs.serialize(), interactions1Row);
            client.write(FreeHealthConstants.drugInteractionsTable, drug1ID.toString(), interactions1Row);

            interactionIDsStr = (String) drugInteractionsTable.getColumn("INTERACTIONS", interactions2Row);
            interactingIDs = new SerializableIDSet(config, interactionIDsStr);
            interactingIDs.remove(drug1ID); // remove drug 1 from list of interacting drugs (if no interaction, this does nothing)
            drugInteractionsTable.updateColumn("INTERACTIONS", interactingIDs.serialize(), interactions2Row);
            client.write(FreeHealthConstants.drugInteractionsTable, drug2ID.toString(), interactions2Row);

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}