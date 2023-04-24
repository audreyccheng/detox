package chronocache.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

	private static Logger logger = LoggerFactory.getLogger(Configuration.class);

	private static final String CONFIG_FILE_NAME = "/chronocache.properties";
	static Properties properties = new Properties();

	static {
		InputStream inputStream = Configuration.class.getResourceAsStream(CONFIG_FILE_NAME);
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			logger.error("Properties file from:{} could NOT be read", CONFIG_FILE_NAME);
		}
	}

	public static String getDatabaseType() {
		return properties.getProperty("database.type");
	}

	public static String getDatabaseServer() {
		return properties.getProperty("database.server");
	}

	public static int getDatabasePort() {
		String port = properties.getProperty("database.port");
		int portNumber = -1;

		try {
			portNumber = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			// TODO Exception
			logger.warn("{} could NOT be parsed to an Integer", portNumber);
		}

		return portNumber;
	}

	public static String getDatabaseName() {
		return properties.getProperty("database.name");
	}

	public static String getDatabaseUsername() {
		return properties.getProperty("database.username");
	}

	public static String getDatabasePassword() {
		return properties.getProperty("database.password");
	}

	public static String getDatabaseDriver() {
		return properties.getProperty("database.driver");
	}

	public static int getDatabaseThreadCount() {
		String threadCount = properties.getProperty("database.threadCount");
		int threadCountNumber = -1;

		try {
			threadCountNumber = Integer.parseInt(threadCount);
		} catch (NumberFormatException e) {
			// TODO Exception
			logger.warn("{} could NOT be parsed to an Integer", threadCount);
		}

		return threadCountNumber;
	}

	public static String getMemcachedAddress() {
		return properties.getProperty("memcached.address");
	}

	public static String getMemcachedPort() {
		return properties.getProperty("memcached.port");
	}

	public static String getRedisAddress() {
		return properties.getProperty("redis.address");
	}

	public static int getRedisPort() {
		String port = properties.getProperty("redis.port");
		int portNumber = -1;
		try {
			portNumber = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			logger.warn("{} could NOT be parsed to an Integer", portNumber);
		}
		return portNumber;
	}

	public static String[] getMemcachedServers() {
		String num = properties.getProperty("memcached.servernum");
		String[] serverAddr=new String[Integer.parseInt(num)];
		serverAddr[0] = properties.getProperty("memcached.address1")+":"+properties.getProperty("memcached.port");
		serverAddr[1] = properties.getProperty("memcached.address2")+":"+properties.getProperty("memcached.port");
		serverAddr[2] = properties.getProperty("memcached.address3")+":"+properties.getProperty("memcached.port");
		serverAddr[3] = properties.getProperty("memcached.address4")+":"+properties.getProperty("memcached.port");
		return serverAddr;
	}

	public static int getSpeculativeExecutionThreadCount() {
		String threadCount = properties.getProperty("speculative.threadCount");
		int threadCountNumber = -1;

		try {
			threadCountNumber = Integer.parseInt(threadCount);
		} catch (NumberFormatException e) {
			// TODO Exception
			logger.warn("{} could NOT be parsed to an Integer", threadCount);
		}

		return threadCountNumber;
	}

	public static String getCacheDbName() {
		return properties.getProperty("cachedb.name");
	}

	public static String getCacheDbAddress() {
		return properties.getProperty("cachedb.address");
	}

    public static String getEngineType() {
        return properties.getProperty("engine.type");
    }

}
