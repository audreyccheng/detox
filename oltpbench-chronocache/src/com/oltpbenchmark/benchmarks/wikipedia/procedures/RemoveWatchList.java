/******************************************************************************
 *  Copyright 2015 by OLTPBenchmark Project                                   *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *    http://www.apache.org/licenses/LICENSE-2.0                              *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 ******************************************************************************/


package com.oltpbenchmark.benchmarks.wikipedia.procedures;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.benchmarks.wikipedia.WikipediaConstants;
import com.oltpbenchmark.benchmarks.wikipedia.util.RestQuery;
import com.oltpbenchmark.util.TimeUtil;

import org.apache.log4j.Logger;

public class RemoveWatchList extends Procedure {
	
    private static final Logger LOG = Logger.getLogger(RemoveWatchList.class);

	public SQLStmt removeWatchList = new SQLStmt(
        "DELETE FROM " + WikipediaConstants.TABLENAME_WATCHLIST +
        " WHERE wl_user = ? AND wl_namespace = ? AND wl_title = ?"
    );
    public SQLStmt setUserTouched = new SQLStmt(
        "UPDATE " + WikipediaConstants.TABLENAME_USER +
        "   SET user_touched = ? " +
        " WHERE user_id =  ? "
    ); 

    public void run(Connection conn, int userId, int nameSpace, String pageTitle, int id) throws SQLException {
        if (userId > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("DELETE FROM ");
            sb.append(WikipediaConstants.TABLENAME_WATCHLIST);
            sb.append(" WHERE wl_user = ");
            sb.append(userId);
            sb.append(" AND wl_namespace = ");
            sb.append(nameSpace);
            sb.append(" AND wl_title = ");
            sb.append(RestQuery.quoteAndSanitize(pageTitle));
            RestQuery.restOtherQuery(sb.toString(), id);

            if (nameSpace == 0) {
                // if regular page, also remove a line of
                // watchlist for the corresponding talk page
                sb = new StringBuilder();
                sb.append("DELETE FROM ");
                sb.append(WikipediaConstants.TABLENAME_WATCHLIST);
                sb.append(" WHERE wl_user = ");
                sb.append(userId);
                sb.append(" AND wl_namespace = ");
                sb.append(1);
                sb.append(" AND wl_title = ");
                sb.append(RestQuery.quoteAndSanitize(pageTitle));
                RestQuery.restOtherQuery(sb.toString(), id);
            }

            sb = new StringBuilder();
            sb.append("UPDATE ");
            sb.append(WikipediaConstants.TABLENAME_USER);
            sb.append(" SET user_touched = ");
            sb.append(TimeUtil.getCurrentTimeString14());
            sb.append(" WHERE user_id = ");
            sb.append(userId);
            RestQuery.restOtherQuery(sb.toString(), id);
        }
    }
}
