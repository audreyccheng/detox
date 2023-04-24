package chronocache.db;

import javax.sql.DataSource;
import java.util.LinkedList;
import java.util.List;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.util.ObjectPool;


public class DBConnPool {

	private Logger logger = LoggerFactory.getLogger(DBConnPool.class);
	private ObjectPool<Connection> objPool;

	public DBConnPool(DataSource source, int poolSize){
		List<Connection> connPool = new LinkedList<>();
		for(int i = 0; i < poolSize; i++){
			try {
				connPool.add(source.getConnection());
			} catch( Exception e) {
				logger.error("Could not create appropriate number of connections at connection {}, error: {}", i, e.getMessage());
			}
		}
		objPool = new ObjectPool<>( connPool );

	}

	public Connection getConn(){
		logger.trace("Trying to get a connection from conn pool...");
		Connection conn = objPool.borrow();
		logger.trace("conn acquired.");
		return conn;
	 }

	public void returnConn( Connection conn ){
		logger.trace("Returning conn");
		objPool.returnObj( conn );
	}
}
