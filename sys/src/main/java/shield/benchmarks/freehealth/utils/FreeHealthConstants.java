package shield.benchmarks.freehealth.utils;

public class FreeHealthConstants {




    /**
     * List of all possible transactions
     */
    public enum Transactions {
        ADD_DRUG_INTERACTION,
        GET_DRUG_INTERACTIONS,
        GET_PRESCRIPTIONS,
        PRESCRIBE_DRUG,
        REMOVE_DRUG_INTERACTION,
        ADD_EPISODE_CONTENT,
        CREATE_ENCOUNTER,
        GET_EPISODE_CONTENT,
        GET_EPISODES,
        INVALIDATE_EPISODE,
        UPDATE_EPISODE_CONTENT,
        UPDATE_EPISODES,
        CREATE_PATIENT,
        GET_PATIENT_DATA,
        GET_PATIENT_NAMES,
        LOOKUP_PATIENT,
        UPDATE_PATIENT_DATA,
        GET_PMH,
        SAVE_PMH,
        CHECK_LOGIN,
        GET_USER
    }

    public static String patientsIdentityTable = "PATIENT_IDENTITY";
    public static String patientsIdentityByNameTable = "PATIENT_IDENTITY_BY_NAME";
    public static String patientsIdentityByDOBTable = "PATIENT_IDENTITY_BY_DOB";
    public static String patientsIdentityByUserTable = "PATIENT_IDENTITY_BY_USER";
    public static String episodesTable = "EPISODES";
    public static String episodesByPatientTable = "EPISODES_BY_PATIENT";
    public static String episodesByUserTable = "EPISODES_BY_USER";
    public static String episodesContentTable = "EPISODES_CONTENT";
    public static String episodesContentByEpisodeTable = "EPISODES_CONTENT_BY_EPISODE";
    public static String pmhTable = "PMH_MASTER";
    public static String pmhByPatientTable = "PMH_MASTER_BY_PATIENT";
    public static String pmhByUserTable = "PMH_MASTER_BY_USER";
    public static String drugsTable = "DRUGS";
    public static String drugInteractionsTable = "DRUG_INTERACTIONS";
    public static String prescriptionsTable = "PRESCRIPTIONS";
    public static String prescriptionsByPatientTable = "PRESCRIPTIONS_BY_PATIENT";
    public static String usersTable = "USERS";
    public static String usersByLoginTable = "USERS_BY_LOGIN";
    public static String nextSharedIDTable = "NEXT_SHARED_ID";


    // keys for looking up the next shared ID in nextSharedIDTable
    public static String nextEpisodeContentKeyPrefix = "EC";
    public static String nextPatientKeyPrefix = "PA";
    public static String nextPrescriptionKeyPrefix = "PR";
    public static String nextEpisodeKeyPrefix = "EP";

    // size of the GENDER field - in the spec this is a single character
    public static int GENDER_SIZE=1;
    // size of the COUNTRY field - in the spec this is two character
    public static int COUNTRY_SIZE=2;
    // size of the DATE field
    public static int DATE_SIZE=16;

    public static int PRESCRIPTION_CONTENT_TYPE = 1; // content type id for a prescription
}
