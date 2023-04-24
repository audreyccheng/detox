package shield.benchmarks.freehealth.transactions.users;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.models.User;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents looking up the user by UUID
 * Returns user object
 */
public class GetUser extends BenchmarkTransaction {

    private FreeHealthExperimentConfiguration config;
    private Integer uuid;

    public GetUser(FreeHealthGenerator generator, Integer uuid) {
        // plugins/usermanagerplugin/database/userbase.cpp : 308, 385, 397
        config = generator.getConfig();
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

            Table usersTable = client.getTable(FreeHealthConstants.usersTable);

            client.startTransaction();

            results = client.readAndExecute(FreeHealthConstants.patientsIdentityTable, uuid.toString());

            byte[] userRow = results.get(0);
            if (isEmptyRow(userRow)) {
                // user doesn't exist
                client.commitTransaction();
                return true;
            }

            User user = new User(
                    (Integer) usersTable.getColumn("USER_ID", userRow),
                    (Integer) usersTable.getColumn("VALIDITY", userRow),
                    (String) usersTable.getColumn("LOGIN", userRow),
                    (String) usersTable.getColumn("PASSWORD", userRow),
                    (String) usersTable.getColumn("LASTLOGIN", userRow),
                    (String) usersTable.getColumn("NAME", userRow),
                    (String) usersTable.getColumn("TITLE", userRow),
                    (String) usersTable.getColumn("GENDER", userRow),
                    (String) usersTable.getColumn("MAIL", userRow),
                    (String) usersTable.getColumn("LANGUAGE", userRow));

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
