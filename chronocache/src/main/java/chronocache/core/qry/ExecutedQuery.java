package chronocache.core.qry;

import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

/**
 * An executed version of a query, with constants and timestamps extracted
 * @author bjglasbe
 *
 */
public class ExecutedQuery extends Query {

	private DateTime dt;
	private QueryResult queryResult;
	private long responseTime;
	
	public ExecutedQuery(Query q, DateTime dt, long responseTime, QueryResult queryResult ){
		super(q);
		this.dt = dt;
		this.queryResult = queryResult;
		this.responseTime = responseTime;
	}
	
	public DateTime getExecutionTime(){
		return dt;
	}
	
	public QueryResult getResults(){
		return queryResult;
	}

	public long getResponseTime(){
		return responseTime;
	}
}
