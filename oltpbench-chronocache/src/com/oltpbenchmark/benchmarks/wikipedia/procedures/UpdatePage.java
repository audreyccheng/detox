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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

//import ch.ethz.ssh2.log.Logger;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.benchmarks.wikipedia.WikipediaConstants;
import com.oltpbenchmark.benchmarks.wikipedia.util.RestQuery;
import com.oltpbenchmark.util.TimeUtil;
import org.apache.log4j.Logger;

public class UpdatePage extends Procedure {
	private static final Logger LOG = Logger.getLogger(Procedure.class);
    // -----------------------------------------------------------------
    // STATEMENTS
    // -----------------------------------------------------------------
    
	public SQLStmt insertText = new SQLStmt(
        "INSERT INTO " + WikipediaConstants.TABLENAME_TEXT + " (" +
        "old_page,old_text,old_flags" + 
        ") VALUES (" +
        "?,?,?" +
        ")"
    ); 
	public SQLStmt insertRevision = new SQLStmt(
        "INSERT INTO " + WikipediaConstants.TABLENAME_REVISION + " (" +
		"rev_page, " +
		"rev_text_id, " +
		"rev_comment, " +
		"rev_minor_edit, " +
		"rev_user, " +
        "rev_user_text, " +
        "rev_timestamp, " +
        "rev_deleted, " +
        "rev_len, " +
        "rev_parent_id" +
		") VALUES (" +
        "?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
		")"
	);
	public SQLStmt updatePage = new SQLStmt(
        "UPDATE " + WikipediaConstants.TABLENAME_PAGE +
        "   SET page_latest = ?, page_touched = ?, page_is_new = 0, page_is_redirect = 0, page_len = ? " +
        " WHERE page_id = ?"
    );
	public SQLStmt insertRecentChanges = new SQLStmt(
        "INSERT INTO " + WikipediaConstants.TABLENAME_RECENTCHANGES + " (" + 
	    "rc_timestamp, " +
	    "rc_cur_time, " +
	    "rc_namespace, " +
	    "rc_title, " +
	    "rc_type, " +
        "rc_minor, " +
        "rc_cur_id, " +
        "rc_user, " +
        "rc_user_text, " +
        "rc_comment, " +
        "rc_this_oldid, " +
	    "rc_last_oldid, " +
	    "rc_bot, " +
	    "rc_moved_to_ns, " +
	    "rc_moved_to_title, " +
	    "rc_ip, " +
        "rc_old_len, " +
        "rc_new_len " +
        ") VALUES (" +
        "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
        ")"
    );
	public SQLStmt selectWatchList = new SQLStmt(
        "SELECT wl_user FROM " + WikipediaConstants.TABLENAME_WATCHLIST +
        " WHERE wl_title = ?" +
        "   AND wl_namespace = ?" +
		"   AND wl_user != ?" +
		"   AND wl_notificationtimestamp IS NULL"
    );
	public SQLStmt updateWatchList = new SQLStmt(
        "UPDATE " + WikipediaConstants.TABLENAME_WATCHLIST +
        "   SET wl_notificationtimestamp = ? " +
	    " WHERE wl_title = ?" +
	    "   AND wl_namespace = ?" +
	    "   AND wl_user = ?"
    );
	public SQLStmt selectUser = new SQLStmt(
        "SELECT * FROM " + WikipediaConstants.TABLENAME_USER + " WHERE user_id = ?"
    );
	public SQLStmt insertLogging = new SQLStmt(
        "INSERT INTO " + WikipediaConstants.TABLENAME_LOGGING + " (" +
		"log_type, log_action, log_timestamp, log_user, log_user_text, " +
        "log_namespace, log_title, log_page, log_comment, log_params" +
        ") VALUES (" +
        "'patrol','patrol',?,?,?,?,?,?,'',?" +
        ")"
    );
	public SQLStmt updateUserEdit = new SQLStmt(
        "UPDATE " + WikipediaConstants.TABLENAME_USER +
        "   SET user_editcount=user_editcount+1" +
        " WHERE user_id = ?"
    );
	public SQLStmt updateUserTouched = new SQLStmt(
        "UPDATE " + WikipediaConstants.TABLENAME_USER + 
        "   SET user_touched = ?" +
        " WHERE user_id = ?"
    );
	
    // -----------------------------------------------------------------
    // RUN
    // -----------------------------------------------------------------
	
	public void run(Connection conn, int textId, int pageId,
	                                 String pageTitle, String pageText, int pageNamespace,
	                                 int userId, String userIp, String userText,
	                                 int revisionId, String revComment, int revMinorEdit, int id) throws SQLException {

	    boolean adv;
	    PreparedStatement ps = null;
	    ResultSet rs = null;
	    int param;
	    final String timestamp = TimeUtil.getCurrentTimeString14();
	    
	    // INSERT NEW TEXT

		StringBuilder sb = new StringBuilder();
		sb.append( "INSERT INTO " );
		sb.append( WikipediaConstants.TABLENAME_TEXT );
		sb.append( " ( old_page, old_text, old_flags ) VALUES ( " );
		sb.append( pageId );
		sb.append( ", " );
		sb.append( RestQuery.quoteAndSanitize( pageText ) );
		sb.append( ", 'utf-8' )" );

		RestQuery.restOtherQuery( sb.toString(), id );
		
		// FIXME
		// We need to get the latest ID. Because we don't really respect transactions,
		// the latest ID might not actually be us. This isn't ideal because we will try
		// to insert a new revision for the same page.

		sb = new StringBuilder();
		sb.append(  "SELECT MAX(old_id) AS mid FROM " );
		sb.append( WikipediaConstants.TABLENAME_TEXT );
		sb.append( " WHERE old_page = " );
		sb.append( pageId );

		List<Map<String,Object>> resultSet = RestQuery.restReadQuery( sb.toString(), id );
		int nextTextId = (Integer) resultSet.get( 0 ).get( "mid" );
		
		sb = new StringBuilder();
		sb.append( "INSERT INTO " );
		sb.append( WikipediaConstants.TABLENAME_REVISION );
		sb.append( " ( rev_page, " );
		sb.append( "rev_text_id, " );
		sb.append( "rev_comment, " );
		sb.append( "rev_minor_edit, " );
		sb.append( "rev_user, " );
        sb.append( "rev_user_text, " );
        sb.append( "rev_timestamp, " );
        sb.append( "rev_deleted, " );
        sb.append( "rev_len, " );
        sb.append( "rev_parent_id " );
		sb.append( ") VALUES ( " );
        sb.append( pageId );
		sb.append( ", " );
		sb.append( nextTextId );
		sb.append( ", " );
		sb.append( RestQuery.quoteAndSanitize( revComment ) );
		sb.append( ", " );
		sb.append( revMinorEdit );
		sb.append( ", " );
		sb.append( userId );
		sb.append( ", " );
		sb.append( RestQuery.quoteAndSanitize( userText ) );
		sb.append( ", " );
		sb.append( RestQuery.quoteAndSanitize( timestamp ) );
		sb.append( ", 0, " );
		sb.append( pageText.length() );
		sb.append( ", " );
		sb.append( revisionId );
		sb.append( ") " );

		RestQuery.restOtherQuery( sb.toString(), id );
		
		//FIXME: see notes above
		sb = new StringBuilder( "SELECT MAX(rev_id) AS mid FROM " );
		sb.append( WikipediaConstants.TABLENAME_REVISION );
		sb.append( " WHERE rev_page = " );
		sb.append( pageId );

		resultSet = RestQuery.restReadQuery( sb.toString(), id );
		int nextRevId = (Integer) resultSet.get( 0 ).get( "mid" );

		sb = new StringBuilder();
		sb.append( "UPDATE " );
		sb.append( WikipediaConstants.TABLENAME_PAGE );
		sb.append( " SET page_latest = " );
		sb.append( nextRevId );
		sb.append( ", page_touched =  " );
		sb.append( RestQuery.quoteAndSanitize( timestamp ) );
		sb.append( ", page_is_new = 0, page_is_redirect = 0, page_len = " );
		sb.append( pageText.length() );
		sb.append( " WHERE page_id = " );
		sb.append( pageId );

		RestQuery.restOtherQuery( sb.toString(), id );

		sb = new StringBuilder();
		sb.append( "INSERT INTO "  );
		sb.append( WikipediaConstants.TABLENAME_RECENTCHANGES );
	    sb.append( " ( rc_timestamp, " );
	    sb.append( "rc_cur_time, " );
	    sb.append( "rc_namespace, " );
	    sb.append( "rc_title, " );
	    sb.append( "rc_type, " );
        sb.append( "rc_minor, " );
        sb.append( "rc_cur_id, " );
        sb.append( "rc_user, " );
        sb.append( "rc_user_text, " );
        sb.append( "rc_comment, " );
        sb.append( "rc_this_oldid, " );
	    sb.append( "rc_last_oldid, " );
	    sb.append( "rc_bot, " );
	    sb.append( "rc_moved_to_ns, " );
	    sb.append( "rc_moved_to_title, " );
	    sb.append( "rc_ip, " );
        sb.append( "rc_old_len, " );
        sb.append( "rc_new_len " );
        sb.append( ") VALUES (" );
		sb.append( RestQuery.quoteAndSanitize( timestamp ) );
		sb.append( ", " );
		sb.append( RestQuery.quoteAndSanitize( timestamp ) );
		sb.append( ", " );
		sb.append( pageNamespace );
		sb.append( ", " );
		sb.append( RestQuery.quoteAndSanitize( pageTitle ) );
		sb.append( ", 0, 0, " );
		sb.append( pageId );
		sb.append( ", " );
		sb.append( userId );
		sb.append( ", " );
		sb.append( RestQuery.quoteAndSanitize( userText ) );
		sb.append( ", " );
		sb.append( RestQuery.quoteAndSanitize( revComment ) );
		sb.append( ", " );
		sb.append( nextTextId );
		sb.append( ", " );
		sb.append( textId );
		sb.append( ", 0, 0, '', " );
		sb.append( RestQuery.quoteAndSanitize( userIp ) );
		sb.append( ", " );
		sb.append( pageText.length() );
		sb.append( ", " );
		sb.append( pageText.length() );
		sb.append( " ) " );
		RestQuery.restOtherQuery( sb.toString(), id );

		// sql="INSERT INTO `cu_changes` () VALUES ();";
		// st.addBatch(sql);

		// SELECT WATCHING USERS

		// sb = new StringBuilder();
		// sb.append( "SELECT wl_user FROM " );
		// sb.append( WikipediaConstants.TABLENAME_WATCHLIST );
		// sb.append( " WHERE wl_title = " );
		// sb.append( RestQuery.quoteAndSanitize( pageTitle ) );
		// sb.append( " AND wl_namespace = " );
		// sb.append( pageNamespace );
		// sb.append( " AND wl_user != " );
		// sb.append( userId );
		// sb.append( " AND wl_notificationtimestamp IS NULL" );

		// resultSet = RestQuery.restReadQuery( sb.toString(), id );
		// Iterator<Map<String,Object>> rowIter = resultSet.iterator();

		// ArrayList<Integer> wlUser = new ArrayList<Integer>();
		// while( rowIter.hasNext() ) {
		// 	wlUser.add( (Integer) rowIter.next().get( "wl_user" ) );
		// }

		// =====================================================================
		// UPDATING WATCHLIST: txn3 (not always, only if someone is watching the
		// page, might be part of txn2)
		// =====================================================================
		//if (wlUser.isEmpty() == false) {
			sb = new StringBuilder();
			sb.append( "UPDATE " );
			sb.append( WikipediaConstants.TABLENAME_WATCHLIST );
			sb.append( " SET wl_notificationtimestamp = " );
			sb.append( RestQuery.quoteAndSanitize( timestamp ) );
			sb.append( " WHERE wl_title = " );
			sb.append( RestQuery.quoteAndSanitize( pageTitle ) );
			sb.append( " AND wl_namespace = " );
			sb.append( pageNamespace );	
			sb.append( " AND wl_user IN ( " );
			sb.append( "SELECT wl_user FROM " );
			sb.append( WikipediaConstants.TABLENAME_WATCHLIST );
			sb.append( " WHERE wl_title = " );
			sb.append( RestQuery.quoteAndSanitize( pageTitle ) );
			sb.append( " AND wl_namespace = " );
			sb.append( pageNamespace );
			sb.append( " AND wl_user != " );
			sb.append( userId );
			sb.append( " AND wl_notificationtimestamp IS NULL )" );

			RestQuery.restOtherQuery( sb.toString(), id );
        	
			// ===================================================================== 
			// UPDATING USER AND LOGGING STUFF: txn4 (might still be part of
			// txn2)
			// =====================================================================
			sb = new StringBuilder();
			sb.append( "SELECT user_name FROM " );
			sb.append( WikipediaConstants.TABLENAME_USER );
			sb.append( " WHERE user_id IN ( "  );
			sb.append( "SELECT wl_user FROM " );
			sb.append( WikipediaConstants.TABLENAME_WATCHLIST );
			sb.append( " WHERE wl_title = " );
			sb.append( RestQuery.quoteAndSanitize( pageTitle ) );
			sb.append( " AND wl_namespace = " );
			sb.append( pageNamespace );
			sb.append( " AND wl_user != " );
			sb.append( userId );
			sb.append( " AND wl_notificationtimestamp IS NULL )" );

			RestQuery.restReadQuery( sb.toString(), id );
		//}

		// This is always executed, sometimes as a separate transaction,
		// sometimes together with the previous one
		

		sb = new StringBuilder();
		sb.append( "INSERT INTO " );
		sb.append( WikipediaConstants.TABLENAME_LOGGING );
		sb.append( " ( " );
		sb.append( "log_type, log_action, log_timestamp, log_user, log_user_text, " );
		sb.append( "log_namespace, log_title, log_page, log_comment, log_params ) VALUES ( " );
		sb.append( "'patrol', 'patrol', " );
		sb.append( RestQuery.quoteAndSanitize( timestamp ) );
		sb.append( ", " );
		sb.append( userId );
		sb.append( ", " );
		sb.append( RestQuery.quoteAndSanitize( pageTitle ) );
		sb.append( ", " );
		sb.append( pageNamespace );
		sb.append( ", " );
		sb.append( RestQuery.quoteAndSanitize( userText ) );
		sb.append( ", " );
		sb.append( pageId );
		sb.append( ", '', " );
		sb.append( RestQuery.quoteAndSanitize( String.format("%d\\n%d\\n%d", nextRevId, revisionId, 1)) );
		sb.append( " )" );
		RestQuery.restOtherQuery( sb.toString(), id );

		sb = new StringBuilder();
		sb.append( "UPDATE " );
		sb.append( WikipediaConstants.TABLENAME_USER );
		sb.append( " SET user_editcount=user_editcount+1 WHERE user_id = " );
		sb.append( userId );
		RestQuery.restOtherQuery( sb.toString(), id );
		
		sb = new StringBuilder();
		sb.append( "UPDATE " );
		sb.append( WikipediaConstants.TABLENAME_USER );
		sb.append( " SET user_touched = "  );
		sb.append( RestQuery.quoteAndSanitize( timestamp ) );
		sb.append( " WHERE user_id = " );
		sb.append( userId );
		RestQuery.restOtherQuery( sb.toString(), id );
	}	
	
	public void execute(Connection conn, PreparedStatement p) throws SQLException{
	      boolean successful = false;
			while (!successful) {
				try {
					p.execute();
					successful = true;
				} catch (SQLException esql) {
					int errorCode = esql.getErrorCode();
					if (errorCode == 8177)
						conn.rollback();
					else
						throw esql;
				}
			}
		}
	public void executeBatch(Connection conn, PreparedStatement p) throws SQLException{
	      boolean successful = false;
			while (!successful) {
				try {
					p.executeBatch();
					successful = true;
				} catch (SQLException esql) {
					int errorCode = esql.getErrorCode();
					if (errorCode == 8177)
						conn.rollback();
					else
						throw esql;
				}
			}
		}
}
