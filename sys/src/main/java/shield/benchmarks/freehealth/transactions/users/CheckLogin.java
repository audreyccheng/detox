package shield.benchmarks.freehealth.transactions.users;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;
import shield.benchmarks.freehealth.FreeHealthGenerator;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.util.List;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;

/**
 * A parameterized transaction that represents checking a username/password login pair
 * Returns \e true if a user can connect with these credentials, that is, if
 * credentials are correct for both RDBMS (MySQL user accounts) & app (USERS table)
 */
public class CheckLogin extends BenchmarkTransaction {

    private FreeHealthExperimentConfiguration config;
    private String login;
    private String password;
    private FreeHealthGenerator generator;

    public CheckLogin(FreeHealthGenerator generator, String username, String password) {
        // plugins/usermanagerplugin/database/userbase.cpp : 428
        config = generator.getConfig();
        login = username;
        this.password = password;
        this.generator = generator;
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
            boolean loginSuccessful; // will be set to true or false

            Table usersTable = client.getTable(FreeHealthConstants.usersTable);
            Table usersByLoginTable = client.getTable(FreeHealthConstants.usersByLoginTable);

            client.startTransaction();

            results = client.readAndExecute(FreeHealthConstants.usersByLoginTable, login);

            byte[] loginRow = results.get(0);
            if (isEmptyRow(loginRow)) {
                // login doesn't exist
                loginSuccessful = false;
                client.commitTransaction();
                return true;
            }

            Integer uuid = (Integer) usersByLoginTable.getColumn("USER_ID", loginRow);
            results = client.readAndExecute(FreeHealthConstants.usersTable, uuid.toString());
            byte[] userRow = results.get(0);
            if (isEmptyRow(userRow)) {
                // user doesn't exist
                loginSuccessful = false;
                client.commitTransaction();
                return true;
            }

            String savedPassword = (String) usersTable.getColumn("PASSWORD", userRow);
            loginSuccessful = password.equals(savedPassword);

            // update last login time
            usersTable.updateColumn("LASTLOGIN", generator.getTime(), userRow);
            client.write(FreeHealthConstants.usersTable, uuid.toString(), userRow);

            client.commitTransaction();
            return true;

        } catch (DatabaseAbortException e) {
            return false;
        }
    }
}
