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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;


import org.apache.log4j.Logger;

import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.benchmarks.seats.SEATSConstants;
import com.oltpbenchmark.benchmarks.seats.util.RestQuery;

public class DeleteReservation extends Procedure {
    private static final Logger LOG = Logger.getLogger(DeleteReservation.class);
    
    public final SQLStmt GetCustomerByIdStr = new SQLStmt(
            "SELECT C_ID " + 
            "  FROM " + SEATSConstants.TABLENAME_CUSTOMER + 
            " WHERE C_ID_STR = ?");
    
    public final SQLStmt GetCustomerByFFNumber = new SQLStmt(
            "SELECT C_ID, FF_AL_ID " +
            "  FROM " + SEATSConstants.TABLENAME_CUSTOMER + ", " + 
                        SEATSConstants.TABLENAME_FREQUENT_FLYER + 
            " WHERE FF_C_ID_STR = ? AND FF_C_ID = C_ID");
    
    public final SQLStmt GetCustomerReservation = new SQLStmt(
            "SELECT C_SATTR00, C_SATTR02, C_SATTR04, " +
            "       C_IATTR00, C_IATTR02, C_IATTR04, C_IATTR06, " +
            "       F_SEATS_LEFT, " +
            "       R_ID, R_SEAT, R_PRICE, R_IATTR00 " +
            "  FROM " + SEATSConstants.TABLENAME_CUSTOMER + ", " +
                        SEATSConstants.TABLENAME_FLIGHT + ", " +
                        SEATSConstants.TABLENAME_RESERVATION +
            " WHERE C_ID = ? AND C_ID = R_C_ID " +
            "   AND F_ID = ? AND F_ID = R_F_ID "
    );
    
    public final SQLStmt DeleteReservation = new SQLStmt(
            "DELETE FROM " + SEATSConstants.TABLENAME_RESERVATION +
            " WHERE R_ID = ? AND R_C_ID = ? AND R_F_ID = ?");

    public final SQLStmt UpdateFlight = new SQLStmt(
            "UPDATE " + SEATSConstants.TABLENAME_FLIGHT +
            "   SET F_SEATS_LEFT = F_SEATS_LEFT + 1 " + 
            " WHERE F_ID = ? ");
    
    public final SQLStmt UpdateCustomer = new SQLStmt(
            "UPDATE " + SEATSConstants.TABLENAME_CUSTOMER +
            "   SET C_BALANCE = C_BALANCE + ?, " +
            "       C_IATTR00 = ?, " +
            "       C_IATTR10 = C_IATTR10 - 1, " + 
            "       C_IATTR11 = C_IATTR10 - 1 " +
            " WHERE C_ID = ? ");
    
    public final SQLStmt UpdateFrequentFlyer = new SQLStmt(
            "UPDATE " + SEATSConstants.TABLENAME_FREQUENT_FLYER +
            "   SET FF_IATTR10 = FF_IATTR10 - 1 " + 
            " WHERE FF_C_ID = ? " +
            "   AND FF_AL_ID = ?");
    
    public void run(Connection conn, long f_id, Long c_id, String c_id_str, String ff_c_id_str, Long ff_al_id, int id) throws SQLException {
        final boolean debug = LOG.isDebugEnabled();
        PreparedStatement stmt = null; 
        
        // If we weren't given the customer id, then look it up
        if( c_id == null ) {
            boolean has_al_id = false;
            
            String queryText = null;
            // Use the customer's id as a string
            if (c_id_str != null && c_id_str.length() > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append( "SELECT c_id ");
                sb.append( "FROM " );
                sb.append( SEATSConstants.TABLENAME_CUSTOMER );
                sb.append( " WHERE c_id_str = ");
                sb.append( RestQuery.quoteAndSanitize( c_id_str ) );
                queryText = sb.toString();
            }
            // Otherwise use their FrequentFlyer information
            else {
                assert(ff_c_id_str.isEmpty() == false);
                assert(ff_al_id != null);
                StringBuilder sb = new StringBuilder();
                sb.append( "SELECT c_id, ff_al_id " );
                sb.append( "FROM " );
                sb.append( SEATSConstants.TABLENAME_CUSTOMER );
                sb.append( ", " );
                sb.append( SEATSConstants.TABLENAME_FREQUENT_FLYER );
                sb.append( " WHERE ff_c_id_str = ");
                sb.append( RestQuery.quoteAndSanitize( ff_c_id_str ) );
                sb.append( " AND ff_c_id = c_id" );
                queryText = sb.toString();

                has_al_id = true;
            }
            List<Map<String,Object>> resultSet = RestQuery.restReadQuery( queryText, id );
            if( resultSet.isEmpty() ) {
                throw new UserAbortException(String.format("No Customer record was found [c_id_str=%s, ff_c_id_str=%s, ff_al_id=%s]", c_id, ff_c_id_str, ff_al_id ) );
            }
            Map<String, Object> row = resultSet.get(0);
            c_id = (long) Double.parseDouble(row.get( "c_id" ).toString());
            if( has_al_id ) {
                ff_al_id = (long) Double.parseDouble(row.get( "ff_al_id" ).toString() );
            }
        }

        // Now get the result of the information that we need
        // If there is no valid customer record, then throw an abort
        // This should happen 5% of the time
        StringBuilder sb = new StringBuilder();
        sb.append( "SELECT c_sattr00, c_sattr02, c_sattr04, " );
        sb.append( "c_iattr00, c_iattr02, c_iattr04, c_iattr06, " );
        sb.append( "f_seats_left, r_id, r_seat, r_price, r_iattr00 " );
        sb.append( "FROM " );
        sb.append( SEATSConstants.TABLENAME_CUSTOMER );
        sb.append( ", " );
        sb.append( SEATSConstants.TABLENAME_FLIGHT );
        sb.append( ", " );
        sb.append( SEATSConstants.TABLENAME_RESERVATION );
        sb.append( " WHERE c_id = " );
        sb.append( c_id );
        sb.append( " AND c_id = r_c_id AND f_id = " );
        sb.append( f_id );
        sb.append( " AND f_id = r_f_id ");


        List<Map<String,Object>> resultSet = RestQuery.restReadQuery( sb.toString(), id );
        if( resultSet.isEmpty() ) {
            throw new UserAbortException(String.format("No Customer information record found for id '%d'", c_id));
        }
        Map<String,Object> row = resultSet.get( 0 );
    	long c_iattr00 = (long) Double.parseDouble(row.get( "c_iattr00" ).toString()) + 1;
        long seats_left = (long) Double.parseDouble(row.get( "f_seats_left" ).toString());
        long r_id = (long) Double.parseDouble(row.get( "r_id" ).toString());
        double r_price = (Double) row.get( "r_price" );

        int updated = 0;
        
        // Now delete all of the flights that they have on this flight
        sb = new StringBuilder();
        sb.append( "DELETE FROM " );
        sb.append( SEATSConstants.TABLENAME_RESERVATION );
        sb.append( " WHERE r_id = " );
        sb.append( r_id );
        sb.append( " AND r_c_id = " );
        sb.append( c_id );
        sb.append( " AND r_f_id = ");
        sb.append( f_id );

        RestQuery.restOtherQuery( sb.toString(), id );

        // Update Available Seats on Flight
        sb = new StringBuilder();
        sb.append( "UPDATE " );
        sb.append( SEATSConstants.TABLENAME_FLIGHT );
        sb.append( " SET f_seats_left = f_seats_left + 1" );
        sb.append( " WHERE f_id = " );
        sb.append( f_id );

        RestQuery.restOtherQuery( sb.toString(), id );
        
        // Update Customer's Balance
        sb = new StringBuilder();
        sb.append( "UPDATE " );
        sb.append( SEATSConstants.TABLENAME_CUSTOMER );
        sb.append( " SET c_balance = c_balance + " );
        double val = -1 * r_price;
        sb.append( val );
        sb.append( ", c_iattr00 = " );
        sb.append( c_iattr00 );
        sb.append( ", c_iattr10 = c_iattr10 - 1," );
        sb.append( " c_iattr11 = c_iattr10 - 1" );
        sb.append( " WHERE c_id = ");
        sb.append( c_id );
        RestQuery.restOtherQuery( sb.toString(), id );

        // Update Customer's Frequent Flyer Information (Optional)
        if (ff_al_id != null) {

            sb = new StringBuilder();
            sb.append( "UPDATE " );
            sb.append( SEATSConstants.TABLENAME_FREQUENT_FLYER );
            sb.append( " SET ff_iattr10 = ff_iattr10 - 1 " );
            sb.append( " WHERE ff_c_id = " );
            sb.append( c_id );
            sb.append( " AND ff_al_id = " );
            sb.append( ff_al_id );
            RestQuery.restOtherQuery( sb.toString(), id );

        }
        if (debug)
            LOG.debug(String.format("Deleted reservation on flight %d for customer %d [seatsLeft=%d]", f_id, c_id, seats_left+1));        
    }

}
