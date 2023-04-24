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

package com.oltpbenchmark.benchmarks.seats.procedures;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.api.Procedure;

import com.oltpbenchmark.benchmarks.seats.SEATSConstants;
import com.oltpbenchmark.benchmarks.seats.util.RestQuery;

public class LoadConfig extends Procedure {
// -----------------------------------------------------------------
    // STATEMENTS
    // -----------------------------------------------------------------
    
    public final SQLStmt getConfigProfile = new SQLStmt(
        "SELECT * FROM " + SEATSConstants.TABLENAME_CONFIG_PROFILE
    );
    
    public final SQLStmt getConfigHistogram = new SQLStmt(
        "SELECT * FROM " + SEATSConstants.TABLENAME_CONFIG_HISTOGRAMS
    );
    
    public final SQLStmt getCountryCodes = new SQLStmt(
        "SELECT CO_ID, CO_CODE_3 FROM " + SEATSConstants.TABLENAME_COUNTRY
    );
    
    public final SQLStmt getAirportCodes = new SQLStmt(
        "SELECT AP_ID, AP_CODE FROM " + SEATSConstants.TABLENAME_AIRPORT
    );
    
    public final SQLStmt getAirlineCodes = new SQLStmt(
        "SELECT AL_ID, AL_IATA_CODE FROM " + SEATSConstants.TABLENAME_AIRLINE +
        " WHERE AL_IATA_CODE != ''"
    );
    
    public final SQLStmt getFlights = new SQLStmt(
        "SELECT f_id FROM " + SEATSConstants.TABLENAME_FLIGHT +
        " ORDER BY F_DEPART_TIME DESC " + 
        " LIMIT " + SEATSConstants.CACHE_LIMIT_FLIGHT_IDS
    );
    
    public List<List<Map<String,Object>>> run( Connection conn, int id ) throws SQLException {
        List<List<Map<String,Object>>> resultSets = new LinkedList<>();
        
        StringBuilder sb = new StringBuilder();
        sb.append( "SELECT  " );
        sb.append( "CFP_SCALE_FACTOR, CFP_AIPORT_MAX_CUSTOMER, ");
        sb.append( "CFP_FLIGHT_START, CFP_FLIGHT_UPCOMING, ");
        sb.append( "CFP_FLIGHT_PAST_DAYS, CFP_FLIGHT_FUTURE_DAYS, " );
        sb.append( "CFP_FLIGHT_OFFSET, CFP_RESERVATION_OFFSET, " );
        sb.append( "CFP_NUM_RESERVATIONS, CFP_CODE_IDS_XREFS FROM " );
        sb.append( SEATSConstants.TABLENAME_CONFIG_PROFILE );
        resultSets.add( RestQuery.restReadQuery( sb.toString(), id ) );


        sb = new StringBuilder();
        sb.append( "SELECT " );
        sb.append( "CFH_NAME, CFH_DATA, CFH_IS_AIRPORT FROM ");
        sb.append( SEATSConstants.TABLENAME_CONFIG_HISTOGRAMS );
        resultSets.add( RestQuery.restReadQuery( sb.toString(), id ) );

        sb = new StringBuilder();
        sb.append( "SELECT CO_ID, CO_CODE_3 FROM " );
        sb.append( SEATSConstants.TABLENAME_COUNTRY );
        resultSets.add( RestQuery.restReadQuery( sb.toString(), id ) );

        sb = new StringBuilder();
        sb.append( "SELECT AP_ID, AP_CODE FROM " );
        sb.append( SEATSConstants.TABLENAME_AIRPORT );
        resultSets.add( RestQuery.restReadQuery( sb.toString(), id ) );

        sb = new StringBuilder();
        sb.append( "SELECT AL_ID, AL_IATA_CODE FROM " );
        sb.append( SEATSConstants.TABLENAME_AIRLINE );
        sb.append( " WHERE AL_IATA_CODE != ''" );
        resultSets.add( RestQuery.restReadQuery( sb.toString(), id ) );

        sb = new StringBuilder();
        sb.append( "SELECT f_id FROM " );
        sb.append( SEATSConstants.TABLENAME_FLIGHT );
        sb.append( " ORDER BY F_DEPART_TIME DESC LIMIT " );
        sb.append( SEATSConstants.CACHE_LIMIT_FLIGHT_IDS );
        resultSets.add( RestQuery.restReadQuery( sb.toString(), id ) );

        return resultSets;
    }
}
