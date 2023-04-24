package chronocache.db;

import chronocache.core.Engine;

public class DBFactory {

	private Engine engine;

	public enum DBType {
		REAL_DB,
		TEST_DB
	}

	public DBFactory( Engine e ) {
		engine = e;
	}

	public DBFactory(){
	}

	public DB getDBInstance( DBType dbType ) {
		if( dbType == DBType.REAL_DB ) {
			return DBImpl.getInstance();
		}
		return new UnitTestDB( engine );
	}
}
