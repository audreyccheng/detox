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

public class GetPageAnonymous extends Procedure {
	
    private static final Logger LOG = Logger.getLogger(GetPageAnonymous.class);

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
	// XXX this is hard for translation
	public SQLStmt selectIpBlocks = new SQLStmt(
        "SELECT * FROM " + WikipediaConstants.TABLENAME_IPBLOCKS + 
        " WHERE ipb_address = ?"
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
        "SELECT old_text, old_flags FROM " + WikipediaConstants.TABLENAME_TEXT +
        " WHERE old_id = ? LIMIT 1"
    );

	// -----------------------------------------------------------------
    // RUN
    // -----------------------------------------------------------------
	
	public Article run(Connection conn, boolean forSelect, String userIp,
			                            int pageNamespace, String pageTitle, int id) throws UserAbortException, SQLException {
        int param = 1;

		StringBuilder sb = new StringBuilder();
		sb.append( "SELECT page_id FROM " );
		sb.append( WikipediaConstants.TABLENAME_PAGE );
		sb.append( " WHERE page_namespace = " );
		sb.append( pageNamespace );
		sb.append( " AND page_title = " );
		sb.append( RestQuery.quoteAndSanitize( pageTitle ) );
		sb.append( " LIMIT 1" );

		List<Map<String,Object>> resultSet = RestQuery.restReadQuery( sb.toString(), id );
		if( resultSet.isEmpty() ) {
            String msg = String.format("Invalid Page: Namespace:%d / Title:--%s--", pageNamespace, pageTitle);
            throw new UserAbortException(msg);
        }

		Map<String, Object> row = resultSet.get(0);
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
		sb.append( " WHERE ipb_address = " );
		sb.append( RestQuery.quoteAndSanitize( userIp ) );

		resultSet = RestQuery.restReadQuery( sb.toString(), id );
		rowIter = resultSet.iterator();


		sb = new StringBuilder();
		sb.append( "SELECT rev_id, rev_text_id FROM " );
		sb.append( WikipediaConstants.TABLENAME_PAGE );
		sb.append( ", " );
		sb.append( WikipediaConstants.TABLENAME_REVISION );
		sb.append( " WHERE page_id = rev_page " );
		sb.append( "AND rev_page = ");
		sb.append( pageId );
		sb.append( " AND page_id = ");
		sb.append( pageId );
        sb.append( " AND rev_id = page_latest LIMIT 1" );

		resultSet = RestQuery.restReadQuery( sb.toString(), id );
        if( resultSet.isEmpty() ) {
            String msg = String.format("Invalid Page: Namespace:%d / Title:--%s-- / PageId:%d",
                                       pageNamespace, pageTitle, pageId);
            throw new UserAbortException(msg);
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
		sb.append( "SELECT old_text, old_flags FROM " );
		sb.append( WikipediaConstants.TABLENAME_TEXT );
		sb.append( " WHERE old_id = " );
		sb.append( textId );
		sb.append( " LIMIT 1" );

		resultSet = RestQuery.restReadQuery( sb.toString(), id );
		if( resultSet.isEmpty() ) {
            String msg = "No such text: " + textId + " for page_id:" + pageId + " page_namespace: " + pageNamespace + " page_title:" + pageTitle;
            throw new UserAbortException(msg);
        }

		row = resultSet.get( 0 );
        Article a = null;
        if (!forSelect)
			a = new Article( userIp, pageId, (String) row.get("old_text"), textId, revisionId);
        return a;
    }
}
