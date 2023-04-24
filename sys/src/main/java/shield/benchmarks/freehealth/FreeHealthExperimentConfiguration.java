package shield.benchmarks.freehealth;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import shield.benchmarks.utils.ClientUtils;
import shield.config.Configuration;

import java.io.FileReader;
import java.io.IOException;

public class FreeHealthExperimentConfiguration extends Configuration {

    public int ID_SIZE=8; // max number of digits in an ID (determines padding for index serialization)

    public int NAME_SIZE=64;
    public int SHORT_TEXT_SIZE=32;
    public int VAR_TEXT_SIZE=500;
    public int TEXT_BLOB_SIZE=1000; // size of xml form content
    public int UUID_LIST_SIZE=ID_SIZE*100; // size of ID Lists
    public int DATE_SIZE=32;

    public int MAX_COLUMN_SIZE = 2000;

    /**
     * Warm-up period before which results start being collected
     */
    public int RAMP_UP = 15;

    /**
     * Ramp-down period during which results are no longer collected
     */
    public int RAMP_DOWN = 15;

    /**
     * Total experiment duration (including ramp up, ramp down)
     */
    public int EXP_LENGTH = 90;

    /**
     * Name of this run (used to determine where collected data will be outputted)
     */
    public String RUN_NAME = "";

    /**
     * Experiment dir
     */
    public String EXP_DIR = "";

    /**
     * Name of the file in which the keys are stored. If the file name remains "" after load,
     */
    public String KEY_FILE_NAME = "";

    /**
     * Number of loader threads
     */
    public int NB_LOADER_THREADS = 16;

    public ClientUtils.ClientType CLIENT_TYPE = ClientUtils.ClientType.SHIELD;

    /**
     * True if must pad columns
     */

    public boolean PAD_COLUMNS = true;

    /**
     * True if must load keys
     */
    public boolean MUST_LOAD_KEYS = true;

    /**
     * Number of users
     */
    public int NB_USERS = 50;

    /**
     * Number of unique drugs total
     */
    public int NB_DRUGS = 300;

    /**
     * Number of patients
     */
    public int NB_PATIENTS = 100;

    /**
     * Number of PMH associated with each patient
     */
    public int NB_PMH_PER_PATIENT = 5;

    /**
     * Number of episodes associated with each patient to begin with
     */
    public int NB_EPISODES_PER_PATIENT = 10;

    /**
     * Number of prescriptions associated with each patient to begin with
     * some subset of initial episodes so should be <= NB_EPISODES_PER_PATIENT
     */
    public int NB_PRESCRIPTIONS_PER_PATIENT = 5;

    /**
     * Number of interactions to generate per drug each drug with have >= this value interactions to begin with
     */
    public int NB_DRUG_INTERACTIONS = 5;

    /**
     * Maximum episode contents to create. When a transaction attempts to create more than this value,
     * it will set the next episode content pointer back to NB_EPISODES_PER_PATIENT * NB_PATIENTS
     */
    public int MAX_EPISODE_CONTENTS = 20000;

    /**
     * Maximum prescriptions to create. When a transaction attempts to create more than this value,
     * it will set the next prescription pointer back to NB_PRESCRIPTIONS_PER_PATIENT * NB_PATIENTS
     */
    public int MAX_PRESCRIPTIONS = 15000;

    /**
     * Maximum patients to create. When a transaction attempts to create more than this value,
     * it will set the next patient pointer back to NB_PATIENTS
     */
    public int MAX_PATIENTS = 2000;

    /**
     * Maximum episodes to create. When a transaction attempts to create more than this value,
     * it will set the next episode pointer back to NB_EPISODES_PER_PATIENT * NB_PATIENTS
     */
    public int MAX_EPISODES = 15000;

    /**
     * Number of hospitals to distribute patients over
     */
    public int NB_HOSPITALS = 10;

    public int EPISODES_PER_HOSPITAL = MAX_EPISODES / NB_HOSPITALS;
    public int EPISODE_CONTENTS_PER_HOSPITAL = MAX_EPISODE_CONTENTS / NB_HOSPITALS;
    public int PATIENTS_PER_HOSPITAL = MAX_PATIENTS / NB_HOSPITALS;
    public int PRESCRIPTIONS_PER_HOSPITAL = MAX_PRESCRIPTIONS / NB_HOSPITALS;

    // these are individual percentage likelihoods that should sum to 100 (different from other benchmarks)
    public int PROB_TRX_ADD_DRUG_INTERACTION = 1;
    public int PROB_TRX_REMOVE_DRUG_INTERACTION = 1;
    public int PROB_TRX_GET_DRUG_INTERACTIONS = 3;
    public int PROB_TRX_GET_PRESCRIPTIONS = 6;
    public int PROB_TRX_PRESCRIBE_DRUG = 10;
    public int PROB_TRX_ADD_EPISODE_CONTENT = 10;
    public int PROB_TRX_CREATE_ENCOUNTER = 8;
    public int PROB_TRX_GET_EPISODE_CONTENT = 7;
    public int PROB_TRX_GET_EPISODES = 6;
    public int PROB_TRX_INVALIDATE_EPISODE = 1;
    public int PROB_TRX_UPDATE_EPISODE_CONTENT = 7;
    public int PROB_TRX_UPDATE_EPISODES = 2;
    public int PROB_TRX_CREATE_PATIENT = 1;
    public int PROB_TRX_GET_PATIENT_DATA = 8;
    public int PROB_TRX_GET_PATIENT_NAMES = 5;
    public int PROB_TRX_LOOKUP_PATIENT = 6;
    public int PROB_TRX_UPDATE_PATIENT_DATA = 3;
    public int PROB_TRX_GET_PMH = 4;
    public int PROB_TRX_SAVE_PMH = 2;
    public int PROB_TRX_CHECK_LOGIN = 6;
    public int PROB_TRX_GET_USER = 3;

    public boolean USE_THINK_TIME = true;
    public int THINK_TIME = 10;

    public FreeHealthExperimentConfiguration(String configFileName)
            throws IOException, ParseException {
        loadProperties(configFileName);
    }

    public FreeHealthExperimentConfiguration() {
        loadProperties();
    }


    /**
     * Loads the constant values from JSON file
     */
    public void loadProperties(String fileName)
            throws IOException, ParseException {

        // TODO: finish implementing me

        isInitialised = true;

        FileReader reader = new FileReader(fileName);
        if (fileName == "") {
            System.err.println("Empty Property File, Intentional?");
        }
        JSONParser jsonParser = new JSONParser();
        JSONObject prop = (JSONObject) jsonParser.parse(reader);
        CLIENT_TYPE = ClientUtils.fromStringToClientType(getPropString(prop, "client_type", ""));
        RAMP_UP = getPropInt(prop, "ramp_up", RAMP_UP);
        RAMP_DOWN = getPropInt(prop, "ramp_down", RAMP_DOWN);
        EXP_LENGTH = getPropInt(prop, "exp_length", EXP_LENGTH);
        RUN_NAME = getPropString(prop, "run_name", RUN_NAME);
        EXP_DIR = getPropString(prop, "exp_dir", EXP_DIR);
        KEY_FILE_NAME = getPropString(prop, "key_file_name", KEY_FILE_NAME);
        NB_LOADER_THREADS = getPropInt(prop, "nb_loader_threads", NB_LOADER_THREADS);
        PAD_COLUMNS = getPropBool(prop, "pad_columns", PAD_COLUMNS);
        MUST_LOAD_KEYS = getPropBool(prop, "must_load_keys", MUST_LOAD_KEYS);

        NB_HOSPITALS = getPropInt(prop, "nb_hospitals", NB_HOSPITALS);
        NB_USERS = getPropInt(prop, "nb_users", NB_USERS);
        NB_DRUGS = getPropInt(prop, "nb_drugs", NB_DRUGS);
        NB_PATIENTS = getPropInt(prop, "nb_patients", NB_PATIENTS);
        NB_PMH_PER_PATIENT = getPropInt(prop, "nb_pmh_per_patient", NB_PMH_PER_PATIENT);
        NB_EPISODES_PER_PATIENT = getPropInt(prop, "nb_episodes_per_patient", NB_EPISODES_PER_PATIENT);
        NB_PRESCRIPTIONS_PER_PATIENT = getPropInt(prop, "nb_prescriptions_per_patient", NB_PRESCRIPTIONS_PER_PATIENT);
        NB_DRUG_INTERACTIONS = getPropInt(prop, "nb_drug_interactions", NB_DRUG_INTERACTIONS);
        MAX_EPISODE_CONTENTS = getPropInt(prop, "max_episode_contents", MAX_EPISODE_CONTENTS);
        MAX_PRESCRIPTIONS = getPropInt(prop, "max_prescriptions", MAX_PRESCRIPTIONS);
        MAX_PATIENTS = getPropInt(prop, "max_patients", MAX_PATIENTS);
        MAX_EPISODES = getPropInt(prop, "max_episodes", MAX_EPISODES);
        EPISODES_PER_HOSPITAL = MAX_EPISODES / NB_HOSPITALS;
        EPISODE_CONTENTS_PER_HOSPITAL = MAX_EPISODE_CONTENTS / NB_HOSPITALS;
        PATIENTS_PER_HOSPITAL = MAX_PATIENTS / NB_HOSPITALS;
        PRESCRIPTIONS_PER_HOSPITAL = MAX_PRESCRIPTIONS / NB_HOSPITALS;
        PROB_TRX_ADD_DRUG_INTERACTION = getPropInt(prop, "prob_trx_add_drug_interaction", PROB_TRX_ADD_DRUG_INTERACTION);
        PROB_TRX_REMOVE_DRUG_INTERACTION = getPropInt(prop, "prob_trx_remove_drug_interaction", PROB_TRX_REMOVE_DRUG_INTERACTION);
        PROB_TRX_GET_DRUG_INTERACTIONS = getPropInt(prop, "prob_trx_get_drug_interactions", PROB_TRX_GET_DRUG_INTERACTIONS);
        PROB_TRX_GET_PRESCRIPTIONS = getPropInt(prop, "prob_trx_get_prescriptions", PROB_TRX_GET_PRESCRIPTIONS);
        PROB_TRX_PRESCRIBE_DRUG = getPropInt(prop, "prob_trx_prescribe_drug", PROB_TRX_PRESCRIBE_DRUG);
        PROB_TRX_ADD_EPISODE_CONTENT = getPropInt(prop, "prob_trx_add_episode_content", PROB_TRX_ADD_EPISODE_CONTENT);
        PROB_TRX_CREATE_ENCOUNTER = getPropInt(prop, "prob_trx_create_encounter", PROB_TRX_CREATE_ENCOUNTER);
        PROB_TRX_GET_EPISODE_CONTENT = getPropInt(prop, "prob_trx_get_episode_content", PROB_TRX_GET_EPISODE_CONTENT);
        PROB_TRX_GET_EPISODES = getPropInt(prop, "prob_trx_get_episodes", PROB_TRX_GET_EPISODES);
        PROB_TRX_INVALIDATE_EPISODE = getPropInt(prop, "prob_trx_invalidate_episode", PROB_TRX_INVALIDATE_EPISODE);
        PROB_TRX_UPDATE_EPISODE_CONTENT = getPropInt(prop, "prob_trx_update_episode_content", PROB_TRX_UPDATE_EPISODE_CONTENT);
        PROB_TRX_UPDATE_EPISODES = getPropInt(prop, "prob_trx_update_episodes", PROB_TRX_UPDATE_EPISODES);
        PROB_TRX_CREATE_PATIENT = getPropInt(prop, "prob_trx_create_patient", PROB_TRX_CREATE_PATIENT);
        PROB_TRX_GET_PATIENT_DATA = getPropInt(prop, "prob_trx_get_patient_data", PROB_TRX_GET_PATIENT_DATA);
        PROB_TRX_GET_PATIENT_NAMES = getPropInt(prop, "prob_trx_get_patient_names", PROB_TRX_GET_PATIENT_NAMES);
        PROB_TRX_LOOKUP_PATIENT = getPropInt(prop, "prob_trx_lookup_patient", PROB_TRX_LOOKUP_PATIENT);
        PROB_TRX_UPDATE_PATIENT_DATA = getPropInt(prop, "prob_trx_update_patient_data", PROB_TRX_UPDATE_PATIENT_DATA);
        PROB_TRX_GET_PMH = getPropInt(prop, "prob_trx_get_pmh", PROB_TRX_GET_PMH);
        PROB_TRX_SAVE_PMH = getPropInt(prop, "prob_trx_save_pmh", PROB_TRX_SAVE_PMH);
        PROB_TRX_CHECK_LOGIN = getPropInt(prop, "prob_trx_check_login", PROB_TRX_CHECK_LOGIN);
        PROB_TRX_GET_USER = getPropInt(prop, "prob_trx_get_user", PROB_TRX_GET_USER);
        USE_THINK_TIME = getPropBool(prop, "use_think_time", USE_THINK_TIME);
        THINK_TIME = getPropInt(prop, "think_time", THINK_TIME);
    }


    /**
     * This is a test method which initializes constants to default values without the need to pass in
     * a configuration file
     *
     * @return true if initialization successful
     */
    @Override
    public void loadProperties() {
        isInitialised = true;
    }
}
