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
import java.util.Map;
import java.util.List;
import java.util.Iterator;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.benchmarks.wikipedia.WikipediaConstants;
import com.oltpbenchmark.benchmarks.wikipedia.util.RestQuery;
import com.oltpbenchmark.benchmarks.wikipedia.util.Article;

import org.apache.log4j.Logger;

public class GetPageAuthenticated extends Procedure {
	
    private static final Logger LOG = Logger.getLogger(GetPageAuthenticated.class);

    // -----------------------------------------------------------------
    // STATEMENTS
    // -----------------------------------------------------------------
    
    public SQLStmt selectPage = new SQLStmt(
        "SELECT * FROM " + WikipediaConstants.TABLENAME_PAGE + 
        " WHERE page_namespace = ? AND page_title = ? LIMIT 1"
    );
    public SQLStmt selectPageRestriction = new SQLStmt(
        "SELECT * FROM " + WikipediaConstants.TABLENAME_PAGE_RESTRICTIONS + 
        " WHERE pr_page = ?"
    );
    public SQLStmt selectIpBlocks = new SQLStmt(
        "SELECT * FROM " + WikipediaConstants.TABLENAME_IPBLOCKS +
        " WHERE ipb_user = ?"
    ); 
    public SQLStmt selectPageRevision = new SQLStmt(
        "SELECT * " +
        "  FROM " + WikipediaConstants.TABLENAME_PAGE + ", " +
                    WikipediaConstants.TABLENAME_REVISION +
        " WHERE page_id = rev_page " +
        "   AND rev_page = ? " +
        "   AND page_id = ? " +
        "   AND rev_id = page_latest LIMIT 1"
    );
    public SQLStmt selectText = new SQLStmt(
        "SELECT old_text,old_flags FROM " + WikipediaConstants.TABLENAME_TEXT +
        " WHERE old_id = ? LIMIT 1"
    );
	public SQLStmt selectUser = new SQLStmt(
        "SELECT * FROM " + WikipediaConstants.TABLENAME_USER + 
        " WHERE user_id = ? LIMIT 1"
    );
	public SQLStmt selectGroup = new SQLStmt(
        "SELECT ug_group FROM " + WikipediaConstants.TABLENAME_USER_GROUPS + 
        " WHERE ug_user = ?"
    );

    // -----------------------------------------------------------------
    // RUN
    // -----------------------------------------------------------------
	
    public Article run(Connection conn, boolean forSelect, String userIp, int userId, int nameSpace, String pageTitle, int id) throws SQLException {

        // =======================================================
        // LOADING BASIC DATA: txn1
        // =======================================================
        // Retrieve the user data, if the user is logged in

        // FIXME TOO FREQUENTLY SELECTING BY USER_ID
        String userText = RestQuery.quoteAndSanitize( userIp );

        PreparedStatement st = this.getPreparedStatement(conn, selectUser);
        if (userId > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append( "SELECT user_name FROM " );
			sb.append( WikipediaConstants.TABLENAME_USER );
			sb.append( " WHERE user_id = " );
			sb.append( userId );
			sb.append( " LIMIT 1" );

			List<Map<String,Object>> resultSet = RestQuery.restReadQuery( sb.toString(), id );
			if( !resultSet.isEmpty() ) {
				userText = (String) resultSet.get( 0 ).get( "user_name" );
			} else {
                throw new UserAbortException("Invalid UserId: " + userId);
            }

			sb = new StringBuilder();
			sb.append( "SELECT ug_group FROM " );
			sb.append( WikipediaConstants.TABLENAME_USER_GROUPS );
			sb.append( " WHERE ug_user = " );
			sb.append( userId );

            // Fetch all groups the user might belong to (access control
            // information)
			resultSet = RestQuery.restReadQuery( sb.toString(), id );
			Iterator<Map<String,Object>> rowIter = resultSet.iterator();
            while( rowIter.hasNext() ) {
                @SuppressWarnings("unused")
                String userGroupName = (String) rowIter.next().get("ug_group");
            }
        }

		StringBuilder sb = new StringBuilder();
		sb.append( "SELECT page_id FROM " );
		sb.append( WikipediaConstants.TABLENAME_PAGE );
		sb.append( " WHERE page_namespace = " );
		sb.append( nameSpace );
		sb.append( " AND page_title = " );
		sb.append( RestQuery.quoteAndSanitize( pageTitle ) );
		sb.append( " LIMIT 1" );

		List<Map<String,Object>> resultSet = RestQuery.restReadQuery( sb.toString(), id );

        if( resultSet.isEmpty() ) {
            throw new UserAbortException("INVALID page namespace/title:" + nameSpace + "/" + pageTitle);
        }

		Map<String,Object> row = resultSet.get( 0 );
        int pageId = (Integer) row.get( "page_id" );

		sb = new StringBuilder();
		sb.append( "SELECT pr_type FROM " );
        sb.append( WikipediaConstants.TABLENAME_PAGE_RESTRICTIONS );
        sb.append( " WHERE pr_page = " );
		sb.append( pageId );

		resultSet = RestQuery.restReadQuery( sb.toString(), id );
		Iterator<Map<String,Object>> rowIter = resultSet.iterator();
       

        // check using blocking of a user by either the IP address or the
        // user_name

		sb = new StringBuilder();
		sb.append( "SELECT ipb_expiry FROM " );
		sb.append( WikipediaConstants.TABLENAME_IPBLOCKS );
		sb.append( " WHERE ipb_user = " );
		sb.append( userId );

		resultSet = RestQuery.restReadQuery( sb.toString(), id );
		rowIter = resultSet.iterator();

		sb = new StringBuilder();
        sb.append( "SELECT rev_id, rev_text_id FROM " );
        sb.append( WikipediaConstants.TABLENAME_PAGE );
		sb.append( ", " );
        sb.append( WikipediaConstants.TABLENAME_REVISION );
        sb.append( " WHERE page_id = rev_page" );
        sb.append( " AND rev_page = " );
		sb.append( pageId );
		sb.append( " AND page_id = " );
		sb.append( pageId );
		sb.append( " AND rev_id = page_latest LIMIT 1" );

		resultSet = RestQuery.restReadQuery( sb.toString(), id );
		if( resultSet.isEmpty() ) {
            throw new UserAbortException("no such revision: page_id:" + pageId + " page_namespace: " + nameSpace + " page_title:" + pageTitle);
        }

		row = resultSet.get( 0 );
        int revisionId = (Integer) row.get("rev_id");
        int textId = (Integer) row.get("rev_text_id");

        // NOTE: the following is our variation of wikipedia... the original did
        // not contain old_page column!
        // sql =
        // "SELECT old_text,old_flags FROM `text` WHERE old_id = '"+textId+"' AND old_page = '"+pageId+"' LIMIT 1";
        // For now we run the original one, which works on the data we have
		sb = new StringBuilder();
		sb.append( "SELECT old_text,old_flags FROM " );
		sb.append( WikipediaConstants.TABLENAME_TEXT );
        sb.append( " WHERE old_id = " );
		sb.append( textId );
		sb.append( " LIMIT 1" );

		resultSet = RestQuery.restReadQuery( sb.toString(), id );
        if( resultSet.isEmpty() ) {
            throw new UserAbortException("no such text: " + textId + " for page_id:" + pageId + " page_namespace: " + nameSpace + " page_title:" + pageTitle);
        }
		row = resultSet.get( 0 );
        Article a = null;
        if (!forSelect)
            a = new Article(userText, pageId, (String) row.get("old_text"), textId, revisionId);

        return a;
    }
}
