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
 * A parameterized transaction that represents marking that two drugs interact with each other
 */
public class AddDrugInteraction extends BenchmarkTransaction {
    private FreeHealthExperimentConfiguration config;
    private Integer drug1ID;
    private Integer drug2ID;

    public AddDrugInteraction(FreeHealthGenerator generator, Integer drug1ID, Integer drug2ID) {
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
                // one of the two drugs doesn't exist so can't create interactions
                client.abortTransaction();
                return false;
            }

            client.read(FreeHealthConstants.drugInteractionsTable, drug1ID.toString());
            results = client.readAndExecute(FreeHealthConstants.drugInteractionsTable, drug2ID.toString());

            byte[] interactions1Row = results.get(0);
            byte[] interactions2Row = results.get(1);
            if (isEmptyRow(results.get(0))) {
                interactions1Row = drugInteractionsTable.createNewRow(config.PAD_COLUMNS);
            }
            if (isEmptyRow(results.get(1))) {
                interactions2Row = drugInteractionsTable.createNewRow(config.PAD_COLUMNS);
            }
            String interactionIDsStr = (String) drugInteractionsTable.getColumn("INTERACTIONS", interactions1Row);
            SerializableIDSet interactingIDs = new SerializableIDSet(config, interactionIDsStr);
            interactingIDs.add(drug2ID); // add drug 2 as an interacting drug
            drugInteractionsTable.updateColumn("INTERACTIONS", interactingIDs.serialize(), interactions1Row);
            client.write(FreeHealthConstants.drugInteractionsTable, drug1ID.toString(), interactions1Row);

            interactionIDsStr = (String) drugInteractionsTable.getColumn("INTERACTIONS", interactions2Row);
            interactingIDs = new SerializableIDSet(config, interactionIDsStr);
            interactingIDs.add(drug1ID); // add drug 1 as an interacting drug
            drugInteractionsTable.updateColumn("INTERACTIONS", interactingIDs.serialize(), interactions2Row);
            client.write(FreeHealthConstants.drugInteractionsTable, drug2ID.toString(), interactions2Row);

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}