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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.api.Procedure;

import com.oltpbenchmark.benchmarks.seats.SEATSConstants;
import com.oltpbenchmark.benchmarks.seats.util.CustomerId;
import com.oltpbenchmark.benchmarks.seats.util.ErrorType;
import com.oltpbenchmark.benchmarks.seats.util.RestQuery;

public class NewReservation extends Procedure {
    private static final Logger LOG = Logger.getLogger(NewReservation.class);
    
    public final SQLStmt GetFlight = new SQLStmt(
            "SELECT F_AL_ID, F_SEATS_LEFT, " +
                    SEATSConstants.TABLENAME_AIRLINE + ".* " +
            "  FROM " + SEATSConstants.TABLENAME_FLIGHT + ", " +
                        SEATSConstants.TABLENAME_AIRLINE +
            " WHERE F_ID = ? AND F_AL_ID = AL_ID");
    
    public final SQLStmt GetCustomer = new SQLStmt(
            "SELECT C_BASE_AP_ID, C_BALANCE, C_SATTR00 " +
            "  FROM " + SEATSConstants.TABLENAME_CUSTOMER +
            " WHERE C_ID = ? ");
    
    public final SQLStmt CheckSeat = new SQLStmt(
            "SELECT R_ID " +
            "  FROM " + SEATSConstants.TABLENAME_RESERVATION +
            " WHERE R_F_ID = ? and R_SEAT = ?");
    
    public final SQLStmt CheckCustomer = new SQLStmt(
            "SELECT R_ID " + 
            "  FROM " + SEATSConstants.TABLENAME_RESERVATION +
            " WHERE R_F_ID = ? AND R_C_ID = ?");
    
    public final SQLStmt UpdateFlight = new SQLStmt(
            "UPDATE " + SEATSConstants.TABLENAME_FLIGHT +
            "   SET F_SEATS_LEFT = F_SEATS_LEFT - 1 " + 
            " WHERE F_ID = ? ");
    
    public final SQLStmt UpdateCustomer = new SQLStmt(
            "UPDATE " + SEATSConstants.TABLENAME_CUSTOMER +
            "   SET C_IATTR10 = C_IATTR10 + 1, " + 
            "       C_IATTR11 = C_IATTR11 + 1, " +
            "       C_IATTR12 = ?, " +
            "       C_IATTR13 = ?, " +
            "       C_IATTR14 = ?, " +
            "       C_IATTR15 = ? " +
            " WHERE C_ID = ? ");
    
    public final SQLStmt UpdateFrequentFlyer = new SQLStmt(
            "UPDATE " + SEATSConstants.TABLENAME_FREQUENT_FLYER +
            "   SET FF_IATTR10 = FF_IATTR10 + 1, " + 
            "       FF_IATTR11 = ?, " +
            "       FF_IATTR12 = ?, " +
            "       FF_IATTR13 = ?, " +
            "       FF_IATTR14 = ? " +
            " WHERE FF_C_ID = ? " +
            "   AND FF_AL_ID = ?");
    
    public final SQLStmt InsertReservation = new SQLStmt(
            "INSERT INTO " + SEATSConstants.TABLENAME_RESERVATION + " (" +
            "   R_ID, " +
            "   R_C_ID, " +
            "   R_F_ID, " +
            "   R_SEAT, " +
            "   R_PRICE, " +
            "   R_IATTR00, " +
            "   R_IATTR01, " +
            "   R_IATTR02, " +
            "   R_IATTR03, " +
            "   R_IATTR04, " +
            "   R_IATTR05, " +
            "   R_IATTR06, " +
            "   R_IATTR07, " +
            "   R_IATTR08 " +
            ") VALUES (" +
            "   ?, " +  // R_ID
            "   ?, " +  // R_C_ID
            "   ?, " +  // R_F_ID
            "   ?, " +  // R_SEAT
            "   ?, " +  // R_PRICE
            "   ?, " +  // R_ATTR00
            "   ?, " +  // R_ATTR01
            "   ?, " +  // R_ATTR02
            "   ?, " +  // R_ATTR03
            "   ?, " +  // R_ATTR04
            "   ?, " +  // R_ATTR05
            "   ?, " +  // R_ATTR06
            "   ?, " +  // R_ATTR07
            "   ? " +   // R_ATTR08
            ")");
    
    public void run(Connection conn, long r_id, long c_id, long f_id, long seatnum, double price, long attrs[], int id ) throws SQLException {
        final boolean debug = LOG.isDebugEnabled();
        boolean found;
        
        // Flight Information
        StringBuilder sb = new StringBuilder();
        sb.append( "SELECT f_id, f_al_id, f_seats_left FROM " );
        sb.append( SEATSConstants.TABLENAME_FLIGHT );
        sb.append( ", " );
        sb.append( SEATSConstants.TABLENAME_AIRLINE );
        sb.append( " WHERE f_id = " );
        sb.append( f_id );
        sb.append( " AND f_al_id = al_id" );

        List<Map<String,Object>> results = RestQuery.restReadQuery( sb.toString(), id );
        if( results.isEmpty() ) {
            throw new UserAbortException(ErrorType.INVALID_FLIGHT_ID +
                                         String.format(" Invalid flight #%d", f_id));
        }
        Map<String,Object> row = results.get( 0 );
        long airline_id = (long) Double.parseDouble(row.get( "f_al_id" ).toString() );
        long seats_left = (long) Double.parseDouble(row.get( "f_seats_left" ).toString() );
        if (seats_left <= 0) {
            throw new UserAbortException(ErrorType.NO_MORE_SEATS +
                                         String.format(" No more seats available for flight #%d", f_id));
        }
        // Check if Seat is Available
        
        sb = new StringBuilder();
        sb.append( "SELECT r_id FROM " );
        sb.append( SEATSConstants.TABLENAME_RESERVATION );
        sb.append(" WHERE r_f_id = " );
        sb.append( f_id );
        sb.append( " AND r_seat = ");
        sb.append( seatnum );

        results = RestQuery.restReadQuery( sb.toString(), id );
        if( !results.isEmpty() ) {
            throw new UserAbortException(ErrorType.SEAT_ALREADY_RESERVED +
                                         String.format(" Seat %d is already reserved on flight #%d", seatnum, f_id));
        }

        sb = new StringBuilder();
        sb.append( "SELECT r_id, r_c_id FROM "); 
        sb.append( SEATSConstants.TABLENAME_RESERVATION );
        sb.append( " WHERE r_f_id = " );
        sb.append( f_id );
        sb.append( " AND r_c_id = " );
        sb.append( c_id );
        results = RestQuery.restReadQuery( sb.toString(), id );

        if( !results.isEmpty() ) {
            throw new UserAbortException(ErrorType.CUSTOMER_ALREADY_HAS_SEAT +
                                         String.format(" Customer %d already owns on a reservations on flight #%d", c_id, f_id));
        }

        // Get Customer Information
        sb = new StringBuilder();
        sb.append( "SELECT c_base_ap_id, c_balance, c_sattr00 fROM " );
        sb.append( SEATSConstants.TABLENAME_CUSTOMER );
        sb.append( " WHERE c_id = " );
        sb.append( c_id );
        results = RestQuery.restReadQuery( sb.toString(), id );

        if( results.isEmpty() ) 
            throw new UserAbortException(ErrorType.INVALID_CUSTOMER_ID + 
                                         String.format(" Invalid customer id: %d / %s", c_id, new CustomerId(c_id)));
        
        sb = new StringBuilder();
        sb.append( "INSERT INTO " );
        sb.append( SEATSConstants.TABLENAME_RESERVATION );
        sb.append( " (r_id, r_c_id, r_f_id, r_seat, r_price, r_iattr00, r_iattr01, " );
        sb.append( "r_iattr02, r_iattr03, r_iattr04, r_iattr05, r_iattr06, r_iattr07, " );
        sb.append( "r_iattr08) VALUES ( " );
        sb.append( r_id );
        sb.append( ", " );
        sb.append( c_id );
        sb.append( ", " );
        sb.append( f_id );
        sb.append( ", " );
        sb.append( seatnum );
        sb.append( ", " );
        sb.append( price );
        sb.append( ", " );
        sb.append( attrs[0] );
        sb.append( ", " );
        sb.append( attrs[1] );
        sb.append( ", " );
        sb.append( attrs[2] );
        sb.append( ", " );
        sb.append( attrs[3] );
        sb.append( ", " );
        sb.append( attrs[4] );
        sb.append( ", " );
        sb.append( attrs[5] );
        sb.append( ", " );
        sb.append( attrs[6] );
        sb.append( ", " );
        sb.append( attrs[7] );
        sb.append( ", " );
        sb.append( attrs[8] );
        sb.append( ") " );

        int updated = RestQuery.restOtherQuery( sb.toString(), id );

        if (updated != 1) {
            String msg = String.format("Failed to add reservation for flight #%d - Inserted %d records for InsertReservation", f_id, updated);
            if (debug) LOG.warn(msg);
            throw new UserAbortException(ErrorType.VALIDITY_ERROR + " " + msg);
        }
        
        sb = new StringBuilder();
        sb.append( "UPDATE " );
        sb.append( SEATSConstants.TABLENAME_FLIGHT );
        sb.append( " SET f_seats_left = f_seats_left - 1" );
        sb.append( " WHERE f_id = ");
        sb.append( f_id );

        updated = RestQuery.restOtherQuery( sb.toString(), id );
        if (updated != 1) {
            String msg = String.format("Failed to add reservation for flight #%d - Updated %d records for UpdateFlight", f_id, updated);
            if (debug) LOG.warn(msg);
            throw new UserAbortException(ErrorType.VALIDITY_ERROR + " " + msg);
        }
        
        sb = new StringBuilder();
        sb.append( "UPDATE " );
        sb.append( SEATSConstants.TABLENAME_CUSTOMER );
        sb.append( " SET c_iattr10 = c_iattr10 + 1, " );
        sb.append( " c_iattr11 = c_iattr11 + 1, "  );
        sb.append( " c_iattr12 = ");
        sb.append( attrs[0] ); 
        sb.append( ", c_iattr13 = ");
        sb.append( attrs[1] );
        sb.append( ", c_iattr14 = " );
        sb.append( attrs[2] );
        sb.append( ", c_iattr15 = ");
        sb.append( attrs[3] );
        sb.append( "WHERE c_id = ");
        sb.append( c_id );

        updated = RestQuery.restOtherQuery( sb.toString(), id );
        if( updated != 1 ) {
            String msg = String.format("Failed to add reservation for flight #%d - Updated %d records for UpdateCustomer", f_id, updated);
            if (debug) LOG.warn(msg);
            throw new UserAbortException(ErrorType.VALIDITY_ERROR + " " + msg);
        }
        
        // We don't care if we updated FrequentFlyer 
        sb = new StringBuilder();
        sb.append( "UPDATE " );
        sb.append( SEATSConstants.TABLENAME_FREQUENT_FLYER );
        sb.append( " SET ff_iattr10 = ff_iattr10 + 1, " );
        sb.append( " ff_iattr11 = " );
        sb.append( attrs[4] );
        sb.append( ", ff_iattr12 = " );
        sb.append( attrs[5] );
        sb.append( ", ff_iattr13 = " );
        sb.append( attrs[6] );
        sb.append( ", ff_iattr14 = " );
        sb.append( attrs[7] );
        sb.append( " WHERE ff_c_id = " );
        sb.append( c_id );
        sb.append( " AND ff_al_id = " );
        sb.append( airline_id );

        updated = RestQuery.restOtherQuery( sb.toString(), id );

        if (debug) 
            LOG.debug(String.format("Reserved new seat on flight %d for customer %d [seatsLeft=%d]",
                                    f_id, c_id, seats_left-1));
        
        return;
    }
}
