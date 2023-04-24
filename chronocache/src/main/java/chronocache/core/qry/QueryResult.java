package chronocache.core.qry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import chronocache.core.VersionVector;

public class QueryResult {

	public static enum Type {
		UPDATE, SELECT
	};

	private List<Map<String, Object>> selectResult;
	private int updateResult;
	private Type resultType;
	private VersionVector resultVersion;
	private Integer numColumns;
	private Map<QueryIdentifier, Integer> lastMappingPos;
	private boolean cached;


	public QueryResult( List<Map<String, Object>> result, VersionVector resultVersion ) {
		resultType = Type.SELECT;
		selectResult = result;
		this.resultVersion = resultVersion;
		this.numColumns = null;
		lastMappingPos = new HashMap<>();
	}

	public QueryResult( List<Map<String, Object>> result, VersionVector resultVersion, Integer numColumns ) {
		resultType = Type.SELECT;
		selectResult = result;
		this.resultVersion = resultVersion;
		this.numColumns = numColumns;
		lastMappingPos = new HashMap<>();
	}


	public QueryResult( Integer result, VersionVector resultVersion ) {
		resultType = Type.UPDATE;
		updateResult = result;
		this.resultVersion = resultVersion;
		lastMappingPos = new HashMap<>();
	}


	public static QueryResult surfaceCopyQueryResult( QueryResult qr ) {
		if( qr.resultType == Type.UPDATE ) {
			return new QueryResult( qr.updateResult, qr.resultVersion );
		}
		return new QueryResult( qr.selectResult, qr.resultVersion, qr.numColumns );
	}

	public void announceCached() {
		this.cached = true;
	}

	public boolean isSelect() {
		return resultType.equals( Type.SELECT );
	}

	public List<Map<String, Object>> getSelectResult() {
		return selectResult;
	}

	public List<Map<String, Object>> getSelectResultWithCachingInfo() {
		if (cached) {
			if (selectResult.isEmpty()) {
				selectResult.add(new HashMap<>());
			}
			selectResult.get(0).put("CACHE_HIT", true);
		}
		return selectResult;
	}

	public int getUpdateResult() {
		return updateResult;
	}

	public VersionVector getResultVersion() {
		return resultVersion;
	}

	public VersionVector setResultVersion( VersionVector newVersion ) {
		resultVersion = newVersion;
		return resultVersion;
	}

	public int getLastMappingPos( QueryIdentifier dstQueryId ) {
		if( lastMappingPos.containsKey( dstQueryId ) ) {
			return lastMappingPos.get( dstQueryId );
		}
		return -1;
	}

	public void setMappedPos( QueryIdentifier dstQueryId, int pos ) {
		lastMappingPos.put( dstQueryId, pos );
	}

	public Integer getNumColumns() {
		return numColumns;
	}

	@Override
	public String toString() {
		return selectResult.toString();
	}
}
