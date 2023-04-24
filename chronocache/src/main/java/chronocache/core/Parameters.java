package chronocache.core;

import org.joda.time.Duration;

public class Parameters {
	public static double CHANCE_TO_SPEC_EXEC = 1;
	public static double SPECULATIVE_EXECUTION_THRESHOLD = 0.95;
	public static double PRUNE_THRESHOLD = 0.905;

	public static double EXECUTIONS_REQUIRED_BEFORE_TRACKING = 1;
	public static double TRACKING_PERIOD = 1;
	public static Duration DELTA = new Duration(100);
	public static Duration CLIENT_SHARE_SLEEP_TIME = new Duration(10000000);
	public static int MIN_WIDTH = 1;
	public static double WIDTH_FACTOR = 0;
	public static int MAX_WIDTH_EXP = 1;
	public static double SHELL_SKEW_THRESHOLD_TO_PREDICT = 0.8;
	public static int SHELL_SKEW_REQUIRED_TIMES_SEEN = 3;
	public static int NUM_QUERIES_TO_RELOAD = 0;
	public static double COMPUTED_RELOAD_THRESHOLD = 0;
	public static boolean ENABLE_COMPUTED_RELOAD = false;
	public static boolean ENABLE_SPECULATIVE_EXECUTION = true;
	public static boolean IS_UNIT_TEST = false;
	public static boolean SCALPEL_MODE = false;

	public static double FIDO_DISTANCE_THRESHOLD = 0.6;
	public static int FIDO_MAX_NUM_PREDICTIONS = 10;

	public static String FIDO_TRAIN_FILE = "/home/ec2-user/fido-training.sql";

	public static int FIDO_PREFIX_LEN = 3;
	public static int FIDO_SUFFIX_LEN = 2;
	public static int FIDO_OVERLAP = 4;

	public static int PARSER_POOL_SIZE = 10;
	public static WorkloadType WORKLOAD_TYPE = WorkloadType.EPINIONS;
	public static int LOCAL_DB_CONNS = 100;
	public static int REMOTE_DB_CONNS = 100;
}

