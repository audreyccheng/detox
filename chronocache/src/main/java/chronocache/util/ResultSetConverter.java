package chronocache.util;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anilpacaci
 *         <p>
 *         Utility class providing static methods for different ResultSet
 *         serializations. Currently it supports; <br>
 *         <ul>
 *         <li>JSON</li>
 *         <li>XML</li>
 *         </ul>
 *
 */

public class ResultSetConverter {
	private static ObjectMapper mapper = new ObjectMapper();

	private static Logger logger = LoggerFactory.getLogger(ResultSetConverter.class);

	public static List<Map<String, Object>> getEntitiesFromResultSet(ResultSet resultSet) throws SQLException {
		ArrayList<Map<String, Object>> entities = new ArrayList<Map<String, Object>>();
		while (resultSet.next()) {
			entities.add(getEntityFromResultSet(resultSet));
		}
		return entities;
	}

	private static Map<String, Object> getEntityFromResultSet(ResultSet resultSet) throws SQLException {
		ResultSetMetaData metaData = resultSet.getMetaData();
		int columnCount = metaData.getColumnCount();
		Map<String, Object> resultsMap = new HashMap<String, Object>();
		for (int i = 1; i <= columnCount; ++i) {
			String columnName = metaData.getColumnLabel(i);
			Object object = resultSet.getObject(i);
			resultsMap.put(columnName, object);
		}
		return resultsMap;
	}

	public static byte[] ResultSet2JSONByteArray(ResultSet rs) {
		byte[] resultArray = null;
		if (rs == null) {
			return null;
		}
		try {
			List<Map<String, Object>> resultMap = getEntitiesFromResultSet(rs);
			resultArray = mapper.writeValueAsBytes(resultMap);
		} catch (SQLException e) {
			logger.error("Error reading ResultSet", e);
		} catch (IOException e) {
			logger.error("Error writing ResultSet into byte[]", e);
		}

		return resultArray;
	}
}
