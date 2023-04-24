package shield.benchmarks.freehealth;

import shield.benchmarks.freehealth.models.*;
import shield.benchmarks.freehealth.transactions.drugs.*;
import shield.benchmarks.freehealth.transactions.episode.*;
import shield.benchmarks.freehealth.transactions.patient.*;
import shield.benchmarks.freehealth.transactions.pmh.GetPMH;
import shield.benchmarks.freehealth.transactions.pmh.SavePMH;
import shield.benchmarks.freehealth.transactions.users.CheckLogin;
import shield.benchmarks.freehealth.transactions.users.GetUser;
import shield.benchmarks.freehealth.utils.FreeHealthConstants;
import shield.benchmarks.utils.BenchmarkTransaction;
import shield.benchmarks.utils.Generator;
import shield.benchmarks.utils.TrxStats;
import shield.client.ClientBase;
import shield.client.schema.ColumnInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static shield.benchmarks.freehealth.utils.FreeHealthConstants.*;

public class FreeHealthGenerator {

    private final char[] ALPHANUM =  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private final FreeHealthExperimentConfiguration config;
    private final ClientBase client;
    private final HashMap<Transactions, TrxStats> trxStats;


    // increment as new structures are created - allows use of newly created objects
    private List<Integer> maxKnownEpisodeIDPerHospital;

    private List<Integer> maxKnownEpisodeContentIDPerHospital;
    private List<Integer> maxKnownPrescriptionIDPerHospital;
    private List<Integer> maxKnownPatientIDPerHospital;

    public FreeHealthGenerator(ClientBase client, FreeHealthExperimentConfiguration config) {
        this.config = config;
        this.client = client;
        this.trxStats = new HashMap<>();
        for (FreeHealthConstants.Transactions tType: FreeHealthConstants.Transactions.values()) {
            trxStats.put(tType, new TrxStats());
        }
        this.maxKnownEpisodeIDPerHospital = new ArrayList<>(
                Collections.nCopies(config.NB_HOSPITALS,
                        (config.NB_EPISODES_PER_PATIENT * config.NB_PATIENTS - 1) / config.NB_HOSPITALS));
        this.maxKnownEpisodeContentIDPerHospital = new ArrayList<>(
                Collections.nCopies(config.NB_HOSPITALS,
                        (config.NB_EPISODES_PER_PATIENT * config.NB_PATIENTS - 1) / config.NB_HOSPITALS));
        this.maxKnownPrescriptionIDPerHospital = new ArrayList<>(
                Collections.nCopies(config.NB_HOSPITALS,
                        (config.NB_PRESCRIPTIONS_PER_PATIENT * config.NB_PATIENTS - 1) / config.NB_HOSPITALS));
        this.maxKnownPatientIDPerHospital = new ArrayList<>(
                Collections.nCopies(config.NB_HOSPITALS,
                        (config.NB_PATIENTS - 1) / config.NB_HOSPITALS));
        createFreeHealthTables(client);
    }

    public int idFromHospitalID(int hospitalID, int id) {
        return id * config.NB_HOSPITALS + hospitalID;
    }

    private int getPatientID() {
        int hospital = Generator.generateInt(0, config.NB_HOSPITALS - 1);
        return idFromHospitalID(hospital, Generator.generateInt(0, maxKnownPatientIDPerHospital.get(hospital) - 1));
    }

    private int getEpisodeID() {
        int hospital = Generator.generateInt(0, config.NB_HOSPITALS - 1);
        return idFromHospitalID(hospital, Generator.generateInt(0, maxKnownEpisodeIDPerHospital.get(hospital) - 1));
    }

    private int getEpisodeContentID() {
        int hospital = Generator.generateInt(0, config.NB_HOSPITALS - 1);
        System.out.println("DELETEME: producing epcont from hospital: " + hospital + ", max is " + maxKnownEpisodeContentIDPerHospital.get(hospital));
        return idFromHospitalID(hospital, Generator.generateInt(0, maxKnownEpisodeContentIDPerHospital.get(hospital) - 1));
    }

    private int getPMHID() {
        return Generator.generateInt(0, config.NB_PMH_PER_PATIENT * config.NB_PATIENTS - 1);
    }

    private int getUserID() {
        return Generator.generateInt(0, config.NB_USERS - 1);
    }

    private int getDrugID() {
        return Generator.generateInt(0, config.NB_DRUGS - 1);
    }

    private int getHospital() {
        return Generator.generateInt(0, config.NB_HOSPITALS - 1);
    }

    private AddDrugInteraction GenerateAddDrugInteraction() {
        int drug1id = getDrugID();
        int drug2id = getDrugID();
        while (drug1id == drug2id) {
            drug1id = getDrugID();
            drug2id = getDrugID();
        }
        return new AddDrugInteraction(this, drug1id, drug2id);
    }

    private GetDrugInteractions GenerateGetDrugInteractions() {
        int drugID = getDrugID();
        return new GetDrugInteractions(this, drugID);
    }

    private GetPrescriptions GenerateGetPrescriptions() {
        int patientID = getPatientID();
        return new GetPrescriptions(this, patientID);
    }

    private PrescribeDrug GeneratePrescribeDrug() {
        Integer patientID = getPatientID();
        Integer episodeID = getEpisodeID();
        Integer drugID = getDrugID();
        String label = RandString(14, 24, false);
        String dosage = RandString(14, 24, false);
        String startDate = RandDate(2010, 2018);
        String endDate = RandDate(2019, 2021);
        String creationDate = getTime();
        String comment = RandString(10, 100, false);
        Prescription p = new Prescription(null, patientID, label, drugID, dosage,
                startDate, endDate, creationDate, 1, comment);
        return new PrescribeDrug(this, getHospital(), episodeID, p);
    }

    private RemoveDrugInteraction GenerateRemoveDrugInteraction() {
        int drug1id = getDrugID();
        int drug2id = getDrugID();
        return new RemoveDrugInteraction(this, drug1id, drug2id);
    }

    private AddEpisodeContent GenerateAddEpisodeContent() {
        int episodeID = getEpisodeID();
        String xmlContent = RandString(10, 100, false);
        int type = FreeHealthConstants.PRESCRIPTION_CONTENT_TYPE + Generator.generateInt(1, 10);
        EpisodeContent content = new EpisodeContent(episodeID, type, xmlContent, null);
        return new AddEpisodeContent(this, getHospital(), episodeID, content);
    }

    private CreateEncounter GenerateCreateEncounter() {
        int patientID = getPatientID();
        String label = RandString(14, 24, false);
        String userDate = RandDate(2010, 2018);
        String creationDate = getTime();
        int creator = getUserID();
        int prior = getEpisodeID();
        Episode episode = new Episode(patientID, 1, label, userDate, creationDate, creator, prior);
        return new CreateEncounter(this, getHospital(), episode);
    }

    private GetEpisodeContent GenerateGetEpisodeContent() {
        int episodeID = getEpisodeID();
        return new GetEpisodeContent(this, episodeID);
    }

    private GetEpisodes GenerateGetEpisodes() {
        boolean usePatientID = Generator.generateBoolean();
        boolean useIsValid = Generator.generateBoolean();
        Optional<Integer> patientID = Optional.empty();
        Optional<Integer> userID = Optional.empty();
        Optional<Boolean> isValid = Optional.empty();
        if (usePatientID) {
            Integer patientVal = getPatientID();
            patientID = Optional.of(patientVal);
        } else {
            Integer userVal = Generator.generateInt(0, config.NB_USERS - 1);
            userID = Optional.of(userVal);
        }
        if (useIsValid) {
            boolean validVal = Generator.generateBoolean();
            isValid = Optional.of(validVal);
        }
        return new GetEpisodes(this, patientID, userID, isValid);
    }

    private InvalidateEpisode GenerateInvalidateEpisode() {
        int minEpisodes = config.NB_EPISODES_PER_PATIENT * config.NB_PATIENTS;
        // TODO: @sitar decide on which of these to use.
//        int episodeID = Generator.generateInt(minEpisodes - 1, maxKnownEpisodeID); // may invalidate the last concrete episode
        int episodeID = getEpisodeID(); // may invalidate the last concrete episode
        return new InvalidateEpisode(this, episodeID);
    }

    private UpdateEpisodeContent GenerateUpdateEpisodeContent() {
        int episodeID = getEpisodeID();
        int epContID = getEpisodeContentID();
        System.out.println("DELETEME: produced epcontid: " + epContID);
        String xmlContent = RandString(10, 100, false);
        int type = FreeHealthConstants.PRESCRIPTION_CONTENT_TYPE + Generator.generateInt(1, 10);
        EpisodeContent episodeContent = new EpisodeContent(episodeID, epContID, type, xmlContent, null);
        return new UpdateEpisodeContent(this, episodeContent);
    }

    private UpdateEpisodes GenerateUpdateEpisodes() {
        List<Episode> episodes = new ArrayList<>();
        Set<Integer> IDs = new HashSet<>();
        int numEpisodes = Generator.generateInt(1, 5);
        for (int i = 0; i < numEpisodes; i++) {
            Integer episodeId = getEpisodeID();
            while (IDs.contains(episodeId))
                episodeId = getEpisodeID();
            IDs.add(episodeId);
            Integer patientUid = Generator.generateBoolean() ? null : getPatientID();
            Integer isvalid = Generator.generateBoolean() ? null : Generator.generateInt(0, 1);
            String label = Generator.generateBoolean() ? null : RandString(14, 24, false);
            String userdate = Generator.generateBoolean() ? null : RandDate(2000, 2018);
            String datecreation = Generator.generateBoolean() ? null : getTime();
            Integer creator = Generator.generateBoolean() ? null : getUserID();
            Integer prior = Generator.generateBoolean() ? null : getEpisodeID();
            Episode ep = new Episode(episodeId, patientUid, isvalid, label, userdate, datecreation, creator, prior);
            episodes.add(ep);
        }
        return new UpdateEpisodes(this, episodes);
    }

    private CreatePatient GenerateCreatePatient() {
        String name = "name"; // these will be updated to mach the patient ID in the transaction
        String dateOfBirth = "blank";
        Integer userUuid = 0;
        String gender = RandString(14, 24, false);
        Integer zip = RandZipCode();
        String country = RandString(14, 24, false);
        String mails = RandString(14, 24, false);
        String tels = RandString(9, 10, false);
        String note = RandString(0, 100, false);
        Patient patient = new Patient(name, dateOfBirth, userUuid, gender, zip, country, mails, tels, note, 1);
        return new CreatePatient(this, getHospital(), patient);
    }

    private GetPatientData GenerateGetPatientData() {
        Integer patientID = getPatientID();
        return new GetPatientData(this, patientID);
    }

    private GetPatientNames GenerateGetPatientNames() {
        int numPatients = Generator.generateInt(5, 15);
        Set<Integer> patientIDs = new HashSet<>();
        for (int i = 0; i < numPatients; i++) {
            Integer patientID = getPatientID();
            while (patientIDs.contains(patientID))
                patientID = getPatientID();
            patientIDs.add(patientID);
        }
        return new GetPatientNames(this, new ArrayList<>(patientIDs));
    }

    private LookupPatient GenerateLookupPatient() {
        int patientID = getPatientID();
        String name = null;
        String userUuid = null;
        if (Generator.generateBoolean()) {
            name = "name" + patientID % (config.NB_PATIENTS / 2);
        } else if (Generator.generateBoolean()) {
            userUuid = ((Integer) (patientID % config.NB_USERS)).toString();
        } else {
            name = "name" + patientID % (config.NB_PATIENTS / 2);
            userUuid = ((Integer) (patientID % config.NB_USERS)).toString();
        }
        String dateOfBirth = Generator.generateBoolean() ? null : IDtoDate(patientID);
        return new LookupPatient(this, name, dateOfBirth, userUuid);
    }

    private UpdatePatientData GenerateUpdatePatientData() {
        int patientID = getPatientID();
        String[] fields = {"IDENT_ISACTIVE", "NAME", "GENDER", "DOB", "ZIP", "COUNTRY", "MAILS", "TELS", "NOTE"};
        int fieldNum = Generator.generateInt(0, fields.length - 1);
        String field = fields[fieldNum];
        Object val;
        switch (field) {
            case "IDENT_ISACTIVE":
                val = Generator.generateInt(0, 1);
                break;
            case "NAME":
                val = "name" + (patientID + Generator.generateInt(1, 100)) % (config.NB_PATIENTS/2);
                break;
            case "GENDER":
                String[] genders = {"M", "F", "H", "K"};
                int genNum = Generator.generateInt(0, genders.length - 1);
                val = genders[genNum];
                break;
            case "DOB":
                val = IDtoDate(patientID + Generator.generateInt(1, 100));
                break;
            case "ZIP":
                val = RandZipCode();
                break;
            case "COUNTRY":
                val = RandString(14, 24, false);
                break;
            case "MAILS":
                val = RandString(14, 24, false);
                break;
            case "TELS":
                val = RandString(14, 24, false);
                break;
            case "NOTE":
                val = RandString(14, 100, false);
                break;
            default:
                val = 0;
                break;
        }

        return new UpdatePatientData(this, patientID, field, val);
    }

    private GetPMH GenerateGetPMH() {
        Integer patientID = getPatientID();
        return new GetPMH(this, patientID);
    }

    private SavePMH GenerateSavePMH() {
        Integer pmhID = getPMHID();
        Integer patientUuid = getPatientID();
        Integer userUuid = getUserID();
        Integer categoryId = Generator.generateInt(0, 10);
        String label = RandString(14, 24, false);
        Integer valid = Generator.generateInt(0, 1);
        String comment = RandString(14, 100, false);
        String creationdatetime = RandDate(2000, 2018);
        PMH pmh = new PMH(pmhID, patientUuid, userUuid, categoryId, label, valid, comment, creationdatetime);
        return new SavePMH(this, pmh);
    }

    private CheckLogin GenerateCheckLogin() {
        boolean passwordCorrect = Generator.generateBoolean();
        Integer userID = Generator.generateInt(0,config.NB_USERS-1);
        String username = "username" + userID.toString();
        String password = "password" + userID.toString();
        if (!passwordCorrect) {
            password = "banana";
        }
        return new CheckLogin(this, username, password);
    }

    private GetUser GenerateGetUser() {
        Integer userID = Generator.generateInt(0,config.NB_USERS-1);
        return new GetUser(this, userID);
    }

    private void createFreeHealthTables(ClientBase client) {
        client.createTable(patientsIdentityTable,
                new ColumnInfo("IDENT_ID", Integer.class),
                new ColumnInfo("NAME", String.class, config.NAME_SIZE),
                new ColumnInfo("DOB", String.class, config.DATE_SIZE),
                new ColumnInfo("USER_UUID", Integer.class), // the user that created this patient
                new ColumnInfo("GENDER", String.class, config.SHORT_TEXT_SIZE),
                new ColumnInfo("ZIP", Integer.class),
                new ColumnInfo("COUNTRY", String.class, config.SHORT_TEXT_SIZE),
                new ColumnInfo("MAILS", String.class, config.VAR_TEXT_SIZE), // emails stored in a string as 'Context:Value;Context;Value..'
                new ColumnInfo("TELS", String.class, config.VAR_TEXT_SIZE), // telephone numbers stored in a string as 'Context:Value;Context;Value..'
                new ColumnInfo("NOTE", String.class, config.VAR_TEXT_SIZE),
                new ColumnInfo("IDENT_ISACTIVE", Integer.class));
        client.createTable(patientsIdentityByNameTable, // lookup table for ordering by name
                new ColumnInfo("ID_LIST", String.class, config.UUID_LIST_SIZE));
        client.createTable(patientsIdentityByDOBTable, // lookup table for ordering by DOB
                new ColumnInfo("ID_LIST", String.class, config.UUID_LIST_SIZE));
        client.createTable(patientsIdentityByUserTable, // lookup table for ordering by user_uuid
                new ColumnInfo("ID_LIST", String.class, config.UUID_LIST_SIZE));

        client.createTable(episodesTable, // this table is largely metadata about an episode, actual content is in episodesContentTable
                new ColumnInfo("EPISODE_ID", Integer.class),
                new ColumnInfo("PATIENT_UID", Integer.class),
                new ColumnInfo("ISVALID", Integer.class), // boolean value
                new ColumnInfo("LABEL", String.class, config.SHORT_TEXT_SIZE),
                new ColumnInfo("USERDATE", String.class, config.DATE_SIZE),
                new ColumnInfo("DATECREATION", String.class, config.DATE_SIZE),
                new ColumnInfo("CREATOR", Integer.class),
                new ColumnInfo("PRIOR", Integer.class)); // defaults to 1
        client.createTable(episodesByPatientTable, // lookup table for ordering by patient uuid
                new ColumnInfo("ID_LIST", String.class, config.UUID_LIST_SIZE));
        client.createTable(episodesByUserTable, // lookup table for ordering by user uuid
                new ColumnInfo("ID_LIST", String.class, config.UUID_LIST_SIZE));

        client.createTable(episodesContentTable,
                new ColumnInfo("CONTENT_ID", Integer.class),
                new ColumnInfo("EPISODE_ID", Integer.class),
                new ColumnInfo("CONTENT_TYPE", Integer.class), // 1: prescription, 2: consultation, 3: lab test
                new ColumnInfo("CONTENT", String.class, config.TEXT_BLOB_SIZE)); // if CONTENT_TYPE = 1, this contains just the ID of the prescription
        client.createTable(episodesContentByEpisodeTable, // lookup table for ordering by episode
                new ColumnInfo("ID_LIST", String.class, config.UUID_LIST_SIZE));

        client.createTable(pmhTable,
                new ColumnInfo("ID", Integer.class),
                new ColumnInfo("PATIENT_UUID", Integer.class),
                new ColumnInfo("USER_UUID", Integer.class),
                new ColumnInfo("CATEGORY_ID", Integer.class),
                new ColumnInfo("MH_EPISODE_ID", Integer.class),
                new ColumnInfo("MH_CONTACTS_ID", Integer.class),
                new ColumnInfo("LABEL", String.class, config.SHORT_TEXT_SIZE),
                new ColumnInfo("TYPE_ID", Integer.class),
                new ColumnInfo("STATE_ID", Integer.class),
                new ColumnInfo("GLOBAL_CONF_INDEX", Integer.class),
                new ColumnInfo("VALID", Integer.class), // boolean value
                new ColumnInfo("PRIV", Integer.class), // boolean value
                new ColumnInfo("COMMENT", String.class, config.VAR_TEXT_SIZE),
                new ColumnInfo("CREATIONDATETIME", String.class, config.VAR_TEXT_SIZE));
        client.createTable(pmhByPatientTable, // lookup table for ordering by patient ID
                new ColumnInfo("ID_LIST", String.class, config.UUID_LIST_SIZE));
        client.createTable(pmhByUserTable, // lookup table for ordering by user ID
                new ColumnInfo("ID_LIST", String.class, config.UUID_LIST_SIZE));

        client.createTable(drugsTable,
                new ColumnInfo("ID", Integer.class),
                new ColumnInfo("NAME", String.class, config.SHORT_TEXT_SIZE),
                new ColumnInfo("STRENGTH", Integer.class),
                new ColumnInfo("ATC_ID", Integer.class));
        client.createTable(drugInteractionsTable, // lookup table for drug interactions
                new ColumnInfo("INTERACTIONS", String.class, config.UUID_LIST_SIZE));

        client.createTable(prescriptionsTable,
                new ColumnInfo("ID", Integer.class),
                new ColumnInfo("PATIENT_UUID", Integer.class),
                new ColumnInfo("LABEL", String.class, config.SHORT_TEXT_SIZE),
                new ColumnInfo("DRUG", Integer.class), // ID of drug to prescribe
                new ColumnInfo("DOSAGE", String.class, config.UUID_LIST_SIZE),
                new ColumnInfo("STARTDATE", String.class, config.DATE_SIZE),
                new ColumnInfo("ENDDATE", String.class, config.DATE_SIZE),
                new ColumnInfo("DATECREATION", String.class, config.DATE_SIZE),
                new ColumnInfo("VALID", Integer.class), // boolean value
                new ColumnInfo("COMMENT", String.class, config.VAR_TEXT_SIZE));
        client.createTable(prescriptionsByPatientTable, // lookup table for ordering by patient ID
                new ColumnInfo("ID_LIST", String.class, config.UUID_LIST_SIZE));

        client.createTable(usersTable,
                new ColumnInfo("USER_ID", Integer.class),
                new ColumnInfo("VALIDITY", Integer.class), // boolean
                new ColumnInfo("LOGIN", String.class, config.SHORT_TEXT_SIZE),
                new ColumnInfo("PASSWORD", String.class, config.SHORT_TEXT_SIZE),
                new ColumnInfo("LASTLOGIN", String.class, config.DATE_SIZE),
                new ColumnInfo("NAME", String.class, config.NAME_SIZE),
                new ColumnInfo("TITLE", String.class, config.SHORT_TEXT_SIZE),
                new ColumnInfo("GENDER", String.class, config.SHORT_TEXT_SIZE),
                new ColumnInfo("MAIL", String.class, config.SHORT_TEXT_SIZE),
                new ColumnInfo("LANGUAGE", String.class, config.SHORT_TEXT_SIZE));
        client.createTable(usersByLoginTable, // lookup for ordering by user login
                new ColumnInfo("USER_ID", Integer.class));

        client.createTable(nextSharedIDTable, // table of incrementing counters that store the next IDs to use in object creation
                new ColumnInfo("NEXT_ID", Integer.class)); // access shared next with key defined in constants
    }

    // utility that allows us to select a random transaction with probability out of 100 described by PROB in config
    private int probIncrementer = 0;
    private void resetProbComp() {
        probIncrementer = 0;
    }

    private int probComp(int txProb) {
        probIncrementer += txProb;
        return probIncrementer;
    }

    public void runNextTransaction() {
        int x = Generator.generateInt(0,100);
        System.out.println("Next Ran Number " + x);
        int nbAborts;
        BenchmarkTransaction trx;
        long start = System.currentTimeMillis();
        long end = 0;
        resetProbComp(); // reset probability incrementer
        if (x < probComp(config.PROB_TRX_ADD_DRUG_INTERACTION)) {
            trx = GenerateAddDrugInteraction();
            System.out.println("[" + Transactions.ADD_DRUG_INTERACTION + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
            trxStats.get(Transactions.ADD_DRUG_INTERACTION).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.ADD_DRUG_INTERACTION + "] End");
        }
        else if (x < probComp(config.PROB_TRX_GET_DRUG_INTERACTIONS)) {
            trx = GenerateGetDrugInteractions();
            System.out.println("[" + Transactions.GET_DRUG_INTERACTIONS + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.GET_DRUG_INTERACTIONS).addTransaction(nbAborts,end-start);
            System.out.println("[" + Transactions.GET_DRUG_INTERACTIONS + "] End");
        }
        else if (x < probComp(config.PROB_TRX_GET_PRESCRIPTIONS)) {
            trx = GenerateGetPrescriptions();
            System.out.println("[" + Transactions.GET_PRESCRIPTIONS + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.GET_PRESCRIPTIONS).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.GET_PRESCRIPTIONS + "] End");
        }
        else if (x < probComp(config.PROB_TRX_PRESCRIBE_DRUG)) {
            trx = GeneratePrescribeDrug();
            System.out.println("[" + Transactions.PRESCRIBE_DRUG + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.PRESCRIBE_DRUG).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.PRESCRIBE_DRUG + "] End");
        }
        else if (x < probComp(config.PROB_TRX_REMOVE_DRUG_INTERACTION)) {
            trx = GenerateRemoveDrugInteraction();
            System.out.println("[" + Transactions.REMOVE_DRUG_INTERACTION + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.REMOVE_DRUG_INTERACTION).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.REMOVE_DRUG_INTERACTION + "] End");
        }
        else if (x < probComp(config.PROB_TRX_ADD_EPISODE_CONTENT)) {
            trx = GenerateAddEpisodeContent();
            System.out.println("[" + Transactions.ADD_EPISODE_CONTENT + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.ADD_EPISODE_CONTENT).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.ADD_EPISODE_CONTENT + "] End");
        }
        else if (x < probComp(config.PROB_TRX_CREATE_ENCOUNTER)) {
            trx = GenerateCreateEncounter();
            System.out.println("[" + Transactions.CREATE_ENCOUNTER + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.CREATE_ENCOUNTER).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.CREATE_ENCOUNTER + "] End");
        }
        else if (x < probComp(config.PROB_TRX_GET_EPISODE_CONTENT)) {
            trx = GenerateGetEpisodeContent();
            System.out.println("[" + Transactions.GET_EPISODE_CONTENT + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.GET_EPISODE_CONTENT).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.GET_EPISODE_CONTENT + "] End");
        }
        else if (x < probComp(config.PROB_TRX_GET_EPISODES)) {
            trx = GenerateGetEpisodes();
            System.out.println("[" + Transactions.GET_EPISODES + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.GET_EPISODES).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.GET_EPISODES + "] End");
        }
        else if (x < probComp(config.PROB_TRX_INVALIDATE_EPISODE)) {
            trx = GenerateInvalidateEpisode();
            System.out.println("[" + Transactions.INVALIDATE_EPISODE + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.INVALIDATE_EPISODE).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.INVALIDATE_EPISODE + "] End");
        }
        else if (x < probComp(config.PROB_TRX_UPDATE_EPISODE_CONTENT)) {
            trx = GenerateUpdateEpisodeContent();
            System.out.println("[" + Transactions.UPDATE_EPISODE_CONTENT + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.UPDATE_EPISODE_CONTENT).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.UPDATE_EPISODE_CONTENT + "] End");
        }
        else if (x < probComp(config.PROB_TRX_UPDATE_EPISODES)) {
            trx = GenerateUpdateEpisodes();
            System.out.println("[" + Transactions.UPDATE_EPISODES + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.UPDATE_EPISODES).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.UPDATE_EPISODES + "] End");
        }
        else if (x < probComp(config.PROB_TRX_CREATE_PATIENT)) {
            trx = GenerateCreatePatient();
            System.out.println("[" + Transactions.CREATE_PATIENT + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.CREATE_PATIENT).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.CREATE_PATIENT + "] End");
        }
        else if (x < probComp(config.PROB_TRX_GET_PATIENT_DATA)) {
            trx = GenerateGetPatientData();
            System.out.println("[" + Transactions.GET_PATIENT_DATA + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.GET_PATIENT_DATA).addTransaction(nbAborts,end-start);
            System.out.println("[" + Transactions.GET_PATIENT_DATA + "] End");
        }
        else if (x < probComp(config.PROB_TRX_GET_PATIENT_NAMES)) {
            trx = GenerateGetPatientNames();
            System.out.println("[" + Transactions.GET_PATIENT_NAMES + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.GET_PATIENT_NAMES).addTransaction(nbAborts, end-start);
            System.out.println("[" + Transactions.GET_PATIENT_NAMES + "] End");
        }
        else if (x < probComp(config.PROB_TRX_LOOKUP_PATIENT)) {
            trx = GenerateLookupPatient();
            System.out.println("[" + Transactions.LOOKUP_PATIENT + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.LOOKUP_PATIENT).addTransaction(nbAborts,end-start);
            System.out.println("[" + Transactions.LOOKUP_PATIENT + "] End");
        }
        else if (x < probComp(config.PROB_TRX_UPDATE_PATIENT_DATA)) {
            trx = GenerateUpdatePatientData();
            System.out.println("[" + Transactions.UPDATE_PATIENT_DATA + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.UPDATE_PATIENT_DATA).addTransaction(nbAborts,end-start);
            System.out.println("[" + Transactions.UPDATE_PATIENT_DATA + "] End");
        }
        else if (x < probComp(config.PROB_TRX_GET_PMH)) {
            trx = GenerateGetPMH();
            System.out.println("[" + Transactions.GET_PMH + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.GET_PMH).addTransaction(nbAborts,end-start);
            System.out.println("[" + Transactions.GET_PMH + "] End");
        }
        else if (x < probComp(config.PROB_TRX_SAVE_PMH)) {
            trx = GenerateSavePMH();
            System.out.println("[" + Transactions.SAVE_PMH + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.SAVE_PMH).addTransaction(nbAborts,end-start);
            System.out.println("[" + Transactions.SAVE_PMH + "] End");
        }
        else if (x < probComp(config.PROB_TRX_CHECK_LOGIN)) {
            trx = GenerateCheckLogin();
            System.out.println("[" + Transactions.CHECK_LOGIN + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.CHECK_LOGIN).addTransaction(nbAborts,end-start);
            System.out.println("[" + Transactions.CHECK_LOGIN + "] End");
        }
        else if (x < probComp(config.PROB_TRX_GET_USER)) {
            trx = GenerateGetUser();
            System.out.println("[" + Transactions.GET_USER + "] Begin");
            nbAborts = trx.run();
            end = System.currentTimeMillis();
             trxStats.get(Transactions.GET_USER).addTransaction(nbAborts,end-start);
            System.out.println("[" + Transactions.GET_USER + "] End");
        }
    }

    public FreeHealthExperimentConfiguration getConfig() {
        return config;
    }

    // called by transactions that create new objects
    public void setMaxKnownEpisodeID(int hospital, Integer newMax){
        maxKnownEpisodeIDPerHospital.set(hospital,
                Math.max(newMax, maxKnownEpisodeIDPerHospital.get(hospital)));
    }

    public void setMaxKnownEpisodeContentID(int hospital, Integer newMax){
        maxKnownEpisodeContentIDPerHospital.set(hospital,
                Math.max(newMax, maxKnownEpisodeContentIDPerHospital.get(hospital)));
    }

    public void setMaxKnownPrescriptionID(int hospital, Integer newMax){
        maxKnownPrescriptionIDPerHospital.set(hospital,
                Math.max(newMax, maxKnownPrescriptionIDPerHospital.get(hospital)));
    }

    public void setMaxKnownPatientID(int hospital, Integer newMax){
        maxKnownPatientIDPerHospital.set(hospital,
                Math.max(newMax, maxKnownPatientIDPerHospital.get(hospital)));
    }


    public char RandCharNum(boolean num_only) {
        int x = Generator.generateInt(0, num_only ? 9 : 61);
        return ALPHANUM[x];
    }

    /**
     * Generates a random string of size between min and max, and optinally consisting
     * of numbers only
     *
     * @param num_only
     * @return
     */
    public String RandString(int min, int max, boolean num_only) {
        StringBuffer bf = new StringBuffer();
        int len = Generator.generateInt(min, max);
        for (int i = 0; i < len; ++i) {
            bf.append(RandCharNum(num_only));
        }
        return bf.toString();
    }

    public Integer RandZipCode() {
        StringBuffer bf;
        String s = RandString(5, 5, true);
        return Integer.parseInt(s);
    }

    public String getTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * Generates a string representing a random date between
     * [startYear] and [endYear]
     */
    public String RandDate(int startYear, int endYear) {
        GregorianCalendar gc = new GregorianCalendar();
        int year = Generator.generateInt(startYear, endYear);
        gc.set(Calendar.YEAR, year);
        int day = Generator.generateInt(1, gc.getActualMaximum(Calendar.DAY_OF_YEAR));
        gc.set(Calendar.DAY_OF_YEAR, day);

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = gc.getTime();
        return dateFormat.format(date);
    }

    /**
     * Generates a date for the inputted ID, will always create the same date
     * for the same input but multiple IDs might collide
     */
    public String IDtoDate(int id) {
        GregorianCalendar gc = new GregorianCalendar();
        int year = 1918 + (id % 100);
        gc.set(Calendar.YEAR, year);
        int upper = id / 100;
        gc.set(Calendar.DAY_OF_YEAR, upper % 364 + 1);

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = gc.getTime();
        return dateFormat.format(date);
    }

    public void printStats() {
        trxStats.forEach((tType,stat) -> System.out.println("[STAT] " + tType + " " +  stat.getStats()));
    }

    public ClientBase getClient() {
        return client;
    }
}
