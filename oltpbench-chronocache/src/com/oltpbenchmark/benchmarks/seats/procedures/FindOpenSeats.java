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


/* This file is part of VoltDB. 
 * Copyright (C) 2009 Vertica Systems Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR 
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.                       
 */

package com.oltpbenchmark.benchmarks.seats.procedures;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import com.oltpbenchmark.api.*;

import com.oltpbenchmark.benchmarks.seats.SEATSConstants;
import com.oltpbenchmark.benchmarks.seats.util.RestQuery;

public class FindOpenSeats extends Procedure {
    private static final Logger LOG = Logger.getLogger(FindOpenSeats.class);
    
//    private final VoltTable.ColumnInfo outputColumns[] = {
//        new VoltTable.ColumnInfo("F_ID", VoltType.BIGINT),
//        new VoltTable.ColumnInfo("SEAT", VoltType.INTEGER),
//        new VoltTable.ColumnInfo("PRICE", VoltType.FLOAT),
//    };
    
    public final SQLStmt GetFlight = new SQLStmt(
        "SELECT F_STATUS, F_BASE_PRICE, F_SEATS_TOTAL, F_SEATS_LEFT, " +
        "       (F_BASE_PRICE + (F_BASE_PRICE * (1 - (F_SEATS_LEFT / F_SEATS_TOTAL)))) AS F_PRICE " +
        "  FROM " + SEATSConstants.TABLENAME_FLIGHT +
        " WHERE F_ID = ?"
    );
    
    public final SQLStmt GetSeats = new SQLStmt(
        "SELECT R_ID, R_F_ID, R_SEAT " + 
        "  FROM " + SEATSConstants.TABLENAME_RESERVATION +
        " WHERE R_F_ID = ?"
    );
    
    public Object[][] run(Connection conn, long f_id, int id) throws SQLException {
        final boolean debug = LOG.isDebugEnabled();
        
        // 150 seats
        final long seatmap[] = new long[]
          {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,     
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        assert(seatmap.length == SEATSConstants.FLIGHTS_NUM_SEATS);
        
        // First calculate the seat price using the flight's base price
        // and the number of seats that remaining
        StringBuilder sb = new StringBuilder();
        sb.append( "SELECT f_id, f_status, f_base_price, f_seats_total, f_seats_left, " );
        sb.append( "(f_base_price + (f_base_price * (1 - (f_seats_left / f_seats_total)))) AS f_price " );
        sb.append( "FROM " );
        sb.append( SEATSConstants.TABLENAME_FLIGHT );
        sb.append( " WHERE f_id = " );
        sb.append( f_id );

        List<Map<String,Object>> flightRows = RestQuery.restReadQuery( sb.toString(), id );
        Map<String,Object> flightData = flightRows.get( 0 );

        // double base_price = (Double) flightData.get( "f_base_price" );
        // long seats_total = (long) Double.parseDouble(flightData.get("f_seats_total").toString());
        // long seats_left = (long) Double.parseDouble(flightData.get("f_seats_left").toString());
        double seat_price = (Double) flightData.get( "f_price" );
        /*
        double _seat_price = base_price + (base_price * (1.0 - (seats_left/(double)seats_total)));
        if (debug) 
            LOG.debug(String.format("Flight %d - SQL[%.2f] <-> JAVA[%.2f] [basePrice=%f, total=%d, left=%d]",
                                    f_id, seat_price, _seat_price, base_price, seats_total, seats_left));
        */
        // Then build the seat map of the remaining seats

        sb = new StringBuilder();
        sb.append( "SELECT r_id, r_seat " );
        sb.append( "FROM " );
        sb.append( SEATSConstants.TABLENAME_RESERVATION );
        sb.append( " WHERE r_f_id = " );
        sb.append( f_id );

        List<Map<String,Object>> seats = RestQuery.restReadQuery( sb.toString(), id );
        for( Map<String, Object> seatRow : seats ) {
		long r_id = (long) Double.parseDouble(seatRow.get("r_id").toString());

		int seatnum = (int) Double.parseDouble(seatRow.get( "r_seat" ).toString());
		if (debug) LOG.debug(String.format("Reserved Seat: fid %d / rid %d / seat %d", f_id, r_id, seatnum));
		assert(seatmap[seatnum] == -1) : "Duplicate seat reservation: R_ID=" + r_id;
		seatmap[seatnum] = 1;
        }

        int ctr = 0;
        Object[][] returnResults = new Object[SEATSConstants.FLIGHTS_NUM_SEATS][];
        for (int i = 0; i < seatmap.length; ++i) {
            if (seatmap[i] == -1) {
                // Charge more for the first seats
                double price = seat_price * (i < SEATSConstants.FLIGHTS_FIRST_CLASS_OFFSET ? 2.0 : 1.0);
                Object[] row = new Object[]{ f_id, i, price };
                returnResults[ctr++] = row;
                if (ctr == returnResults.length) break;
            }
        } // FOR
//        assert(seats_left == returnResults.getRowCount()) :
//            String.format("Flight %d - Expected[%d] != Actual[%d]", f_id, seats_left, returnResults.getRowCount());
       
        return returnResults;
    }
            
}
