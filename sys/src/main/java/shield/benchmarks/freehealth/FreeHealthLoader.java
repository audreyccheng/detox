package shield.benchmarks.freehealth;

import java.sql.SQLException;
import org.json.simple.parser.ParseException;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.freehealth.utils.SerializableIDSet;
import shield.benchmarks.utils.ClientUtils;
import shield.benchmarks.utils.Generator;
import shield.client.ClientBase;
import shield.client.DatabaseAbortException;
import shield.client.schema.Table;

import java.io.IOException;
import java.util.*;

import static shield.benchmarks.freehealth.utils.RowUtils.isEmptyRow;
import static shield.benchmarks.utils.Generator.generatePortNumber;

public class FreeHealthLoader {


    public static void main(String[] args) throws IOException, ParseException,
            DatabaseAbortException, InterruptedException {

        String expConfigFile;
        FreeHealthExperimentConfiguration config;

        if (args.length != 1) {
            System.out.println(args.length);
            System.out.println(args[0]);
            System.out.println(args[1]);
            System.err.println(
                    "Incorrect number of arguments: expected <clientConfigFile.json expConfigFile.json>");
        }

        // Contains the experiment parameters
        expConfigFile = args[0];
        System.err.println(expConfigFile);
        config = new FreeHealthExperimentConfiguration(expConfigFile);

        if (config.MUST_LOAD_KEYS) {
            System.out.println("Begin loading data");
            loadData(config, expConfigFile);
        }

        System.out.println("Data loaded");
        System.exit(0);
    }

    private static void loadData(FreeHealthExperimentConfiguration config, String expConfigFile)
            throws InterruptedException, IOException, ParseException {

        // First load data
        int ranges = config.NB_USERS / config.NB_LOADER_THREADS + 1;
        int usersToLoad = ranges > 0 ? ranges : 1;
        List<Thread> threads = new LinkedList<Thread>();

        // Pre initialise set of ports to avoid risk of duplicates
        Set<Integer> ports = new HashSet<>();
        while (ports.size() < config.NB_LOADER_THREADS * 10) {
            ports.add(generatePortNumber());
        }
        Iterator<Integer> it = ports.iterator();

        System.out.println("Loading [USERS]");

        for (int i = 0; i < config.NB_USERS; i += usersToLoad) {
            final int j = i;
            int port = it.next();
            Thread t = new Thread() {
                public void run() {
                    try {
                        int lastUser = ((j + usersToLoad) > config.NB_USERS) ? config.NB_USERS : (j + usersToLoad);
                        loadUsers(j, lastUser, port, expConfigFile);
                        System.out.println("Finished loading [Users]");
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(e);
                        System.err.println("Loading users failed");
                        System.exit(-1);
                    }
                }
            };
            threads.add(t);
            t.start();
        }

        System.out.println("Loading [PATIENTS]");

        ranges = config.NB_PATIENTS / config.NB_LOADER_THREADS + 1;
        int patientsToLoad = ranges > 0 ? ranges : 1;
        for (int i = 0; i < config.NB_PATIENTS; i += patientsToLoad) {
            final int j = i;
            int port = it.next();
            Thread t = new Thread() {
                public void run() {
                    try {
                        int lastPatient = (j + patientsToLoad) > config.NB_PATIENTS ? config.NB_PATIENTS : (j + patientsToLoad);
                        loadPatients(j, lastPatient, port, expConfigFile);
                        System.out.println("Finished loading [Patients]");
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(e);
                        System.err.println("Loading patients failed");
                        System.exit(-1);
                    }
                }
            };
            threads.add(t);
            t.start();
        }

        System.out.println("Loading [PMH]");

        int NB_PMH = config.NB_PMH_PER_PATIENT * config.NB_PATIENTS;
        ranges = NB_PMH / config.NB_LOADER_THREADS + 1;
        int pmhToLoad = ranges > 0 ? ranges : 1;
        for (int i = 0; i < NB_PMH; i += pmhToLoad) {
            final int j = i;
            int port = it.next();
            Thread t = new Thread() {
                public void run() {
                    try {
                        int lastPMH = (j + pmhToLoad) > NB_PMH ? NB_PMH : (j + pmhToLoad);
                        loadPMH(j, lastPMH, port, expConfigFile);
                        System.out.println("Finished loading [PMH]");
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(e);
                        System.err.println("Loading PMH failed");
                        System.exit(-1);
                    }
                }
            };
            threads.add(t);
            t.start();
        }

        System.out.println("Loading [DRUGS]");

        ranges = config.NB_DRUGS / config.NB_LOADER_THREADS + 1;
        int drugsToLoad = ranges > 0 ? ranges : 1;
        for (int i = 0; i < config.NB_DRUGS; i += drugsToLoad) {
            final int j = i;
            int port = it.next();
            Thread t = new Thread() {
                public void run() {
                    try {
                        int lastDrug = (j + drugsToLoad) > config.NB_DRUGS ? config.NB_DRUGS : (j + drugsToLoad);
                        loadDrugs(j, lastDrug, port, expConfigFile);
                        System.out.println("Finished loading [Drugs]");
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(e);
                        System.err.println("Loading drugs failed");
                        System.exit(-1);
                    }
                }
            };
            threads.add(t);
            t.start();
        }

        System.out.println("Loading [EPISODES]");

        int NB_EPISODES = config.NB_EPISODES_PER_PATIENT * config.NB_PATIENTS;
        ranges = NB_EPISODES / config.NB_LOADER_THREADS + 1;
        int episodesToLoad = ranges > 0 ? ranges : 1;
        for (int i = 0; i < NB_EPISODES; i += episodesToLoad) {
            final int j = i;
            int port = it.next();
            Thread t = new Thread() {
                public void run() {
                    try {
                        int lastEpisode = (j + episodesToLoad) > NB_EPISODES ? NB_EPISODES : (j + episodesToLoad);
                        loadEpisodes(j, lastEpisode, port, expConfigFile);
                        System.out.println("Finished loading [Episodes]");
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(e);
                        System.err.println("Loading episodes from " + j + " failed");
                        System.exit(-1);
                    }
                }
            };
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        // Load default values into nextSharedIDTable table
        try {
            loadSharedIDs(it.next(), expConfigFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
            System.err.println("Loading shared keys failed");
            System.exit(-1);
        }


        try {
            testDB(it.next(), expConfigFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
            System.err.println("DB test failed");
            System.exit(-1);
        }
    }

    private static void testDB(Integer uid, String expConfigFile)
        throws InterruptedException, IOException, DatabaseAbortException, ParseException, SQLException {

        FreeHealthExperimentConfiguration config = new FreeHealthExperimentConfiguration(expConfigFile);
        ClientBase client = ClientUtils
                .createClient(config.CLIENT_TYPE, expConfigFile, uid, uid);

        System.out.println("Testing DB");

        client.registerClient();

        FreeHealthGenerator generator = new FreeHealthGenerator(client, config);

        System.out.println("Client registered");

        Table patientsIdentityTable = client.getTable(FreeHealthConstants.patientsIdentityTable);

        boolean success = false;
        while (!success) {
            try {
                client.startTransaction();

                List<byte[]> results;
                results = client.readAndExecute(FreeHealthConstants.patientsIdentityTable, "1");
                if(isEmptyRow(results.get(0))) {
                    client.abortTransaction();
                    success = false;
                    continue;
                }

                String dob = (String) patientsIdentityTable.getColumn("DOB", results.get(0));
                String name = (String) patientsIdentityTable.getColumn("NAME", results.get(0));
                System.out.println("dob: " + dob);
                System.out.println("name: " + name);

                client.commitTransaction();
                success = true;
            } catch (DatabaseAbortException e) {
                success = false;
            }
        }

    }

    private static void loadSharedIDs(Integer uid, String expConfigFile)
        throws InterruptedException, IOException, DatabaseAbortException, ParseException, SQLException {

        FreeHealthExperimentConfiguration config = new FreeHealthExperimentConfiguration(expConfigFile);
        ClientBase client = ClientUtils
                .createClient(config.CLIENT_TYPE, expConfigFile, uid, uid);

        System.out.println("Loading Shared IDs");

        client.registerClient();

        FreeHealthGenerator generator = new FreeHealthGenerator(client, config);

        System.out.println("Client registered");

        Table nextSharedID = client.getTable(FreeHealthConstants.nextSharedIDTable);

        Integer nextECID = (config.NB_EPISODES_PER_PATIENT * config.NB_PATIENTS) / config.NB_HOSPITALS;
        Integer nextPRID = (config.NB_PRESCRIPTIONS_PER_PATIENT * config.NB_PATIENTS) / config.NB_HOSPITALS;
        Integer nextEPID = (config.NB_EPISODES_PER_PATIENT * config.NB_PATIENTS) / config.NB_HOSPITALS;
        Integer nextPID = config.NB_PATIENTS / config.NB_HOSPITALS;

        byte[] ECIDrow = nextSharedID.createNewRow(config.PAD_COLUMNS);
        nextSharedID.updateColumn("NEXT_ID", nextECID, ECIDrow);
        byte[] PRIDrow = nextSharedID.createNewRow(config.PAD_COLUMNS);
        nextSharedID.updateColumn("NEXT_ID", nextPRID, PRIDrow);
        byte[] EPIDrow = nextSharedID.createNewRow(config.PAD_COLUMNS);
        nextSharedID.updateColumn("NEXT_ID", nextEPID, EPIDrow);
        byte[] PIDrow = nextSharedID.createNewRow(config.PAD_COLUMNS);
        nextSharedID.updateColumn("NEXT_ID", nextPID, PIDrow);

        for (int hospital = 0; hospital < config.NB_HOSPITALS; hospital++){
            boolean success = false;
            while (!success) {
                try {
                    client.startTransaction();

                    // only need to do this once for each hospital - set next shared IDs
                    String episodeContentKey = FreeHealthConstants.nextEpisodeContentKeyPrefix + hospital;
                    client.write(FreeHealthConstants.nextSharedIDTable, episodeContentKey, ECIDrow); // set next content ID

                    String prescriptionKey = FreeHealthConstants.nextPrescriptionKeyPrefix + hospital;
                    client.write(FreeHealthConstants.nextSharedIDTable, prescriptionKey, PRIDrow); // set next prescription ID

                    String episodeKey = FreeHealthConstants.nextEpisodeKeyPrefix + hospital;
                    client.write(FreeHealthConstants.nextSharedIDTable, episodeKey, EPIDrow); // set next episode ID

                    String patientKey = FreeHealthConstants.nextPatientKeyPrefix + hospital;
                    client.write(FreeHealthConstants.nextSharedIDTable, patientKey, PIDrow); // set next patient ID

                    client.commitTransaction();
                    success = true;
                } catch (DatabaseAbortException e) {
                    success = false;
                }
            }
        }
    }

    private static void loadUsers(int start, int end, Integer uid, String expConfigFile)
        throws InterruptedException, IOException, DatabaseAbortException, ParseException, SQLException {

        FreeHealthExperimentConfiguration config = new FreeHealthExperimentConfiguration(expConfigFile);
        ClientBase client = ClientUtils
                .createClient(config.CLIENT_TYPE, expConfigFile, uid, uid);

        System.out.println("Loading Users: " + start + " to " + end);

        assert (end <= config.NB_USERS);

        client.registerClient();

        FreeHealthGenerator generator = new FreeHealthGenerator(client, config);

        System.out.println("Client registered");

        boolean success = false;
        byte[] row;

        Table usersTable = client.getTable(FreeHealthConstants.usersTable);
        Table usersByLoginTable = client.getTable(FreeHealthConstants.usersByLoginTable);

        while (start < end) {
            success = false;
            while (!success) {
                try {
                    client.startTransaction();

                    // create user in user table
                    row = usersTable.createNewRow(config.PAD_COLUMNS);
                    Integer userId = start;
                    System.out.println("[User] " + userId);
                    usersTable.updateColumn("USER_ID", userId, row);
                    Integer validity = Generator.generateInt(0, 1);
                    usersTable.updateColumn("VALIDITY", validity, row);
                    String login = "username" + userId.toString();
                    usersTable.updateColumn("LOGIN", login, row);
                    String password = "password" + userId.toString();
                    usersTable.updateColumn("PASSWORD", password, row);
                    String lastlogin = generator.getTime();
                    usersTable.updateColumn("LASTLOGIN", lastlogin, row);
                    String name = generator.RandString(14, 24, false);
                    usersTable.updateColumn("NAME", name, row);
                    String title = generator.RandString(0, 2, false);
                    usersTable.updateColumn("TITLE", title, row);
                    Integer gender = Generator.generateInt(0, 3);
                    usersTable.updateColumn("GENDER", gender.toString(), row);
                    String mail = generator.RandString(14, 24, false);
                    usersTable.updateColumn("MAIL", mail, row);
                    String language = generator.RandString(14, 24, false);
                    usersTable.updateColumn("LANGUAGE", language, row);
                    client.write(FreeHealthConstants.usersTable, userId.toString(), row);

                    // update login to userID mapping
                    row = usersByLoginTable.createNewRow(config.PAD_COLUMNS);
                    usersByLoginTable.updateColumn("USER_ID", userId, row);
                    client.write(FreeHealthConstants.usersByLoginTable, login, row);

                    client.commitTransaction();
                    start += 1;
                    success = true;
                } catch (DatabaseAbortException e) {
                    success = false;
                }
            }
        }
    }

    private static void loadPatients(int start, int end, Integer uid, String expConfigFile)
        throws InterruptedException, IOException, DatabaseAbortException, ParseException, SQLException {

        FreeHealthExperimentConfiguration config = new FreeHealthExperimentConfiguration(expConfigFile);
        ClientBase client = ClientUtils
                .createClient(config.CLIENT_TYPE, expConfigFile, uid, uid);

        System.out.println("Loading Patients: " + start + " to " + end);

        assert (end <= config.NB_PATIENTS);

        client.registerClient();

        FreeHealthGenerator generator = new FreeHealthGenerator(client, config);

        System.out.println("Client registered");

        boolean success = false;
        byte[] row;

        Table patientsTable = client.getTable(FreeHealthConstants.patientsIdentityTable);
        Table patientsNameTable = client.getTable(FreeHealthConstants.patientsIdentityByNameTable);
        Table patientsDOBTable = client.getTable(FreeHealthConstants.patientsIdentityByDOBTable);
        Table patientsUserTable = client.getTable(FreeHealthConstants.patientsIdentityByUserTable);

        while (start < end) {
            success = false;
            while (!success) {
                try {
                    client.startTransaction();

                    // create patient in patients table
                    row = patientsTable.createNewRow(config.PAD_COLUMNS);
                    Integer pid = start;
                    System.out.println("[Patient] " + pid);
                    patientsTable.updateColumn("IDENT_ID", pid, row);
                    String name = "name" + pid % (config.NB_PATIENTS/2);
                    patientsTable.updateColumn("NAME", name, row);
                    String dateOfBirth = generator.IDtoDate(pid);
                    patientsTable.updateColumn("DOB", dateOfBirth, row);
                    Integer userUuid = pid % config.NB_USERS;
                    patientsTable.updateColumn("USER_UUID", userUuid, row);
                    String gender = generator.RandString(14, 24, false);
                    patientsTable.updateColumn("GENDER", gender, row);
                    Integer zip = generator.RandZipCode();
                    patientsTable.updateColumn("ZIP", zip, row);
                    String country = generator.RandString(14, 24, false);
                    patientsTable.updateColumn("COUNTRY", country, row);
                    String mails = generator.RandString(14, 24, false);
                    patientsTable.updateColumn("MAILS", mails, row);
                    String tels = generator.RandString(9, 10, false);
                    patientsTable.updateColumn("TELS", tels, row);
                    String note = generator.RandString(0, 100, false);
                    patientsTable.updateColumn("NOTE", note, row);
                    Integer isActive = 1;
                    patientsTable.updateColumn("IDENT_ISACTIVE", isActive, row);
                    client.write(FreeHealthConstants.patientsIdentityTable, pid.toString(), row);
                    // TODO: might want to multi-thread this since they are deterministic
                    addToIndexList(client, config, patientsNameTable, name, pid);
                    addToIndexList(client, config, patientsDOBTable, dateOfBirth, pid);
                    addToIndexList(client, config, patientsUserTable, userUuid.toString(), pid);

                    client.commitTransaction();
                    start += 1;
                    success = true;
                } catch (DatabaseAbortException e) {
                    success = false;
                }
            }
        }
    }

    /* Adds to key to value mapping or updates key's list of values in table */
    private static void addToIndexList(ClientBase client, FreeHealthExperimentConfiguration config,
                                       Table table, String key, Integer value) throws DatabaseAbortException {
        List<byte[]> results = client.readAndExecute(table.getTableName(), key);
        byte[] valueRow = results.get(0);

        SerializableIDSet values;
        // check if key is already in the table
        if (isRowEmpty(valueRow)) {
            valueRow = table.createNewRow(config.PAD_COLUMNS);
            values = new SerializableIDSet(config);
        } else {
            String strValList = (String) table.getColumn("ID_LIST", valueRow);
            values = new SerializableIDSet(config, strValList);
        }
        values.add(value);
        table.updateColumn("ID_LIST", values.serialize(), valueRow);
        client.write(table.getTableName(), key, valueRow);
    }

    private static void loadPMH(int start, int end, Integer uid, String expConfigFile)
        throws InterruptedException, IOException, DatabaseAbortException, ParseException, SQLException {

        FreeHealthExperimentConfiguration config = new FreeHealthExperimentConfiguration(expConfigFile);
        ClientBase client = ClientUtils
                .createClient(config.CLIENT_TYPE, expConfigFile, uid, uid);

        System.out.println("Loading PMH: " + start + " to " + end);

        assert (end <= config.NB_PMH_PER_PATIENT * config.NB_PATIENTS);

        client.registerClient();

        FreeHealthGenerator generator = new FreeHealthGenerator(client, config);

        System.out.println("Client registered");

        boolean success = false;
        byte[] row;

        Table pmhTable = client.getTable(FreeHealthConstants.pmhTable);
        Table pmhByPatientTable = client.getTable(FreeHealthConstants.pmhByPatientTable);
        Table pmhByUserTable = client.getTable(FreeHealthConstants.pmhByUserTable);

        while (start < end) {
            success = false;
            while (!success) {
                try {
                    client.startTransaction();

                    row = pmhTable.createNewRow(config.PAD_COLUMNS);
                    Integer pmhid = start;
                    System.out.println("[PMH] " + pmhid);
                    pmhTable.updateColumn("ID", pmhid, row);
                    Integer patientUuid = pmhid / config.NB_PMH_PER_PATIENT;
                    pmhTable.updateColumn("PATIENT_UUID", patientUuid, row);
                    Integer userUuid = patientUuid % config.NB_USERS;
                    pmhTable.updateColumn("USER_UUID", userUuid, row);
                    Integer categoryId = Generator.generateInt(0, 10);
                    pmhTable.updateColumn("CATEGORY_ID", categoryId, row);
                    String label = generator.RandString(14, 24, false);
                    pmhTable.updateColumn("LABEL", label, row);
                    Integer valid = Generator.generateInt(0, 1);
                    pmhTable.updateColumn("VALID", valid, row);
                    String comment = generator.RandString(14, 100, false);
                    pmhTable.updateColumn("COMMENT", comment, row);
                    String creationdatetime = generator.RandDate(2000, 2018);
                    pmhTable.updateColumn("CREATIONDATETIME", creationdatetime, row);
                    client.write(FreeHealthConstants.pmhTable, pmhid.toString(), row);

                    // TODO: might want to multi-thread this since they are deterministic
                    addToIndexList(client, config, pmhByPatientTable, patientUuid.toString(), pmhid);
                    addToIndexList(client, config, pmhByUserTable, userUuid.toString(), pmhid);

                    client.commitTransaction();
                    start += 1;
                    success = true;
                } catch (DatabaseAbortException e) {
                    success = false;
                }
            }
        }
    }

    private static void loadDrugs(int start, int end, Integer uid, String expConfigFile)
        throws InterruptedException, IOException, DatabaseAbortException, ParseException, SQLException {

        FreeHealthExperimentConfiguration config = new FreeHealthExperimentConfiguration(expConfigFile);
        ClientBase client = ClientUtils
                .createClient(config.CLIENT_TYPE, expConfigFile, uid, uid);

        System.out.println("Loading Drugs: " + start + " to " + end);

        assert (end <= config.NB_DRUGS);

        client.registerClient();

        FreeHealthGenerator generator = new FreeHealthGenerator(client, config);

        System.out.println("Client registered");

        boolean success = false;
        byte[] row;

        Table drugsTable = client.getTable(FreeHealthConstants.drugsTable);
        Table drugInteractionsTable = client.getTable(FreeHealthConstants.drugInteractionsTable);

        while (start < end) {
            // load drugs into drugs table
            Integer drugid = start;
            success = false;
            while (!success) {
                try {
                    client.startTransaction();
                    // generate drug with random name
                    row = drugsTable.createNewRow(config.PAD_COLUMNS);
                    System.out.println("[Drug] " + drugid);
                    drugsTable.updateColumn("ID", drugid, row);
                    String name = generator.RandString(14, 24, false);
                    drugsTable.updateColumn("NAME", name, row);
                    Integer strength = Generator.generateInt(0, 10);
                    drugsTable.updateColumn("STRENGTH", strength, row);
                    Integer atcID = drugid * 2;
                    drugsTable.updateColumn("ATC_ID", atcID, row);
                    client.write(FreeHealthConstants.drugsTable, drugid.toString(), row);

                    client.commitTransaction();
                    success = true;
                } catch (DatabaseAbortException e) {
                    success = false;
                }
            }

            // load drug interactions
            Set<Integer> interactingIDsToAdd = new HashSet<>();
            for (int i = 0; i < config.NB_DRUG_INTERACTIONS; i++) {
                Integer interactingDrugID = Generator.generateInt(0, config.NB_DRUGS - 1);
                if (interactingDrugID.equals(drugid)) continue;
                interactingIDsToAdd.add(interactingDrugID);
            }
            for (Integer interactingDrugID : interactingIDsToAdd) {
                success = false;
                while (!success) {
                    try {
                        client.startTransaction();

                        // TODO: multi thread this? all deterministic
                        // Generate random drug interactions and update both mapping tables
                        List<byte[]> results;

                        client.read(FreeHealthConstants.drugInteractionsTable, drugid.toString());

                            results = client.readAndExecute(FreeHealthConstants.drugInteractionsTable, interactingDrugID.toString());

                            byte[] interactions1Row = results.get(0);
                            byte[] interactions2Row = results.get(1);

                            SerializableIDSet interactingIDs;
                            if (isRowEmpty(interactions1Row)){
                                interactions1Row = drugInteractionsTable.createNewRow(config.PAD_COLUMNS);
                                interactingIDs = new SerializableIDSet(config);
                            } else {
                                String interactionIDsStr = (String) drugInteractionsTable.getColumn("INTERACTIONS", interactions1Row);
                                interactingIDs = new SerializableIDSet(config, interactionIDsStr);
                            }
                            interactingIDs.add(interactingDrugID); // add drug 2 as an interacting drug
                            drugInteractionsTable.updateColumn("INTERACTIONS", interactingIDs.serialize(), interactions1Row);
                            client.write(FreeHealthConstants.drugInteractionsTable, drugid.toString(), interactions1Row);

                            if (isRowEmpty(interactions2Row)){
                                interactions2Row = drugInteractionsTable.createNewRow(config.PAD_COLUMNS);
                                interactingIDs = new SerializableIDSet(config);
                            } else {
                                String interactionIDsStr = (String) drugInteractionsTable.getColumn("INTERACTIONS", interactions2Row);
                                interactingIDs = new SerializableIDSet(config, interactionIDsStr);
                            }
                            interactingIDs.add(drugid); // add drug 1 as an interacting drug
                            drugInteractionsTable.updateColumn("INTERACTIONS", interactingIDs.serialize(), interactions2Row);
                            client.write(FreeHealthConstants.drugInteractionsTable, interactingDrugID.toString(), interactions2Row);


                        client.commitTransaction();
                        success = true;
                    } catch (DatabaseAbortException e) {
                        success = false;
                    }
                }
            }
            start += 1;
        }
    }


    private static void loadEpisodes(int start, int end, Integer uid, String expConfigFile)
        throws InterruptedException, IOException, DatabaseAbortException, ParseException, SQLException {

        FreeHealthExperimentConfiguration config = new FreeHealthExperimentConfiguration(expConfigFile);
        ClientBase client = ClientUtils
                .createClient(config.CLIENT_TYPE, expConfigFile, uid, uid);

        System.out.println("Loading Episodes: " + start + " to " + end);

        assert (end <= config.NB_PATIENTS * config.NB_EPISODES_PER_PATIENT);

        client.registerClient();

        FreeHealthGenerator generator = new FreeHealthGenerator(client, config);

        System.out.println("Client registered");

        boolean success = false;
        byte[] row;

        Table episodesTable = client.getTable(FreeHealthConstants.episodesTable);
        Table episodesByPatientTable = client.getTable(FreeHealthConstants.episodesByPatientTable);
        Table episodesByUserTable = client.getTable(FreeHealthConstants.episodesByUserTable);
        Table episodesContentTable = client.getTable(FreeHealthConstants.episodesContentTable);
        Table episodesContentByEpisodeTable = client.getTable(FreeHealthConstants.episodesContentByEpisodeTable);
        Table prescriptionsTable = client.getTable(FreeHealthConstants.prescriptionsTable);
        Table prescriptionsByPatientTable = client.getTable(FreeHealthConstants.prescriptionsByPatientTable);

        while (start < end) {
            success = false;
            while (!success) {
                try {
                    client.startTransaction();

                    // create episode in episodes table
                    row = episodesTable.createNewRow(config.PAD_COLUMNS);
                    Integer episodeId = start;
                    episodesTable.updateColumn("EPISODE_ID", episodeId, row);
                    Integer patientUid = episodeId / config.NB_EPISODES_PER_PATIENT;
                    episodesTable.updateColumn("PATIENT_UID", patientUid, row);
                    Integer isvalid = 1;
                    episodesTable.updateColumn("ISVALID", isvalid, row);
                    String label = generator.RandString(14, 24, false);
                    episodesTable.updateColumn("LABEL", label, row);
                    String userdate = generator.RandDate(2000, 2018);
                    episodesTable.updateColumn("USERDATE", userdate, row);
                    String datecreation = userdate;
                    episodesTable.updateColumn("DATECREATION", datecreation, row);
                    Integer creator = patientUid % config.NB_USERS;
                    episodesTable.updateColumn("CREATOR", creator, row);
                    Integer prior = 1;
                    episodesTable.updateColumn("PRIOR", prior, row);

                    client.write(FreeHealthConstants.episodesTable, episodeId.toString(), row);

                    // TODO: might want to multi-thread this since they are deterministic
                    addToIndexList(client, config, episodesByPatientTable, patientUid.toString(), episodeId);
                    addToIndexList(client, config, episodesByUserTable, creator.toString(), episodeId);


                    // if this episode should contain a prescription, create one
                    boolean isPrescription = episodeId % config.NB_EPISODES_PER_PATIENT <= config.NB_PRESCRIPTIONS_PER_PATIENT;
                    // TODO: idea for handling creating new prescriptions: have a range of prescription IDs per patient and check if there's one available when prescribint
                    String episodeContent = generator.RandString(14, 100, false);
                    if (isPrescription) {

                        row = prescriptionsTable.createNewRow(config.PAD_COLUMNS);
                        Integer prescriptionID = episodeId;
                        prescriptionsTable.updateColumn("ID", prescriptionID, row);
                        prescriptionsTable.updateColumn("PATIENT_UUID", patientUid, row);
                        String plabel = generator.RandString(14, 24, false);
                        prescriptionsTable.updateColumn("LABEL",plabel, row);
                        Integer drug = Generator.generateInt(0, config.NB_DRUGS - 1); //TODO: should this be -1 here?
                        prescriptionsTable.updateColumn("DRUG", drug, row);
                        String dosage = generator.RandString(14, 24, false);
                        prescriptionsTable.updateColumn("DOSAGE", dosage, row);
                        String startDate = generator.RandDate(2010, 2018);
                        prescriptionsTable.updateColumn("STARTDATE", startDate, row);
                        String endDate = generator.RandDate(2019, 2021);
                        prescriptionsTable.updateColumn("ENDDATE", endDate, row);
                        prescriptionsTable.updateColumn("DATECREATION", startDate, row);
                        prescriptionsTable.updateColumn("VALID", 1, row);
                        String prescComment = generator.RandString(14, 100, false);
                        prescriptionsTable.updateColumn("COMMENT", prescComment, row);
                        client.write(FreeHealthConstants.prescriptionsTable, prescriptionID.toString(), row);

                        addToIndexList(client, config, prescriptionsByPatientTable, patientUid.toString(), prescriptionID);

                        episodeContent = prescriptionID.toString();
                    }

                    // create new row for this episode content
                    byte[] contentRow = episodesContentTable.createNewRow(config.PAD_COLUMNS);
                    episodesContentTable.updateColumn("CONTENT_ID", episodeId, contentRow);
                    episodesContentTable.updateColumn("EPISODE_ID", episodeId, contentRow);
                    Integer contentType = isPrescription ? FreeHealthConstants.PRESCRIPTION_CONTENT_TYPE : 0;
                    episodesContentTable.updateColumn("CONTENT_TYPE", contentType, contentRow);
                    episodesContentTable.updateColumn("CONTENT", episodeContent, contentRow);
                    client.write(FreeHealthConstants.episodesContentTable, episodeId.toString(), contentRow);

                    addToIndexList(client, config, episodesContentByEpisodeTable, episodeId.toString(), episodeId);

                    client.commitTransaction();
                    start += 1;
                    success = true;
                } catch (DatabaseAbortException e) {
                    success = false;
                }
            }
        }
    }

    private static boolean isRowEmpty(byte[] row) {
        return row.length == 0;
    }

}
