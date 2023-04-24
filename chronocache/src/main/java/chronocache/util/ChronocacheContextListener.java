package chronocache.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.db.DB;
import chronocache.db.DBFactory;
import chronocache.db.DBException;
public class ChronocacheContextListener implements ServletContextListener {


	private static final Logger logger =
        LoggerFactory.getLogger(ChronocacheContextListener.class);
	private static final DB db = new DBFactory().getDBInstance( DBFactory.DBType.REAL_DB );

	@Override
	public void contextInitialized(ServletContextEvent sce) {

		try {
			logger.info("Beginning ChronoCache initialization");
			db.query(1, "select 1");
			logger.info("ChronoCache is succesfully initialized");
		} catch (DBException e) {
			logger.error("ChronoCache could NOT be initialized");
		}

	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		logger.info("Cache hits: {} - Cache miss: {}", db.getCacheHits(),
				db.getCacheMiss());
		logger.info("Closing down...");
	}
}
