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

package com.oltpbenchmark.benchmarks.tpcc.procedures;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.oltpbenchmark.api.RestQuery;
import com.sun.jersey.api.client.WebResource;
import org.apache.log4j.Logger;

import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.benchmarks.tpcc.TPCCConstants;
import com.oltpbenchmark.benchmarks.tpcc.TPCCUtil;
import com.oltpbenchmark.benchmarks.tpcc.TPCCWorker;

public class StockLevel extends TPCCProcedure {

    private static final Logger LOG = Logger.getLogger(StockLevel.class);

	 public boolean run(Connection conn, WebResource wr, Random gen,
                          int w_id, int numWarehouses,
                          int terminalDistrictLowerID, int terminalDistrictUpperID,
                          TPCCWorker w) throws SQLException {
         boolean txnHit = true;
	     boolean trace = LOG.isTraceEnabled(); 

	     int threshold = TPCCUtil.randomNumber(10, 20, gen);
	     int d_id = TPCCUtil.randomNumber(terminalDistrictLowerID,terminalDistrictUpperID, gen);

	     int o_id = 0;
	     // XXX int i_id = 0;
	     int stock_count = 0;

         String stockGetDistOrderId = "SELECT D_NEXT_O_ID " +
                 "  FROM " + TPCCConstants.TABLENAME_DISTRICT +
                 " WHERE D_W_ID = " + w_id +
                 "   AND D_ID = " + d_id;
         if (trace) LOG.trace(String.format("stockGetDistOrderId BEGIN [W_ID=%d, D_ID=%d]", w_id, d_id));
         List<Map<String, Object>> rs = RestQuery.restReadQuery(wr, stockGetDistOrderId, w.getId());
         if (trace) LOG.trace("stockGetDistOrderId END");

         if (rs.isEmpty()) {
             throw new RuntimeException("D_W_ID="+ w_id +" D_ID="+ d_id+" not found!");
         }
         if (rs.get(0).get("CACHE_HIT") == null) {
             txnHit = false;
         }
         o_id = (int) Double.parseDouble(rs.get(0).get("d_next_o_id").toString());

         String stockGetCountStock = "SELECT COUNT(DISTINCT (S_I_ID)) AS STOCK_COUNT " +
                 " FROM " + TPCCConstants.TABLENAME_ORDERLINE + ", " + TPCCConstants.TABLENAME_STOCK +
                 " WHERE OL_W_ID = " + w_id +
                 " AND OL_D_ID = " + d_id +
                 " AND OL_O_ID < " + o_id +
                 " AND OL_O_ID >= " + (o_id - 20) +
                 " AND S_W_ID = " + w_id +
                 " AND S_I_ID = OL_I_ID" +
                 " AND S_QUANTITY < " + threshold;
         if (trace) LOG.trace(String.format("stockGetCountStock BEGIN [W_ID=%d, D_ID=%d, O_ID=%d]", w_id, d_id, o_id));
         rs = RestQuery.restReadQuery(wr, stockGetCountStock, w.getId());
         if (trace) LOG.trace("stockGetCountStock END");

         if (rs.isEmpty()) {
             String msg = String.format("Failed to get StockLevel result for COUNT query " +
                                        "[W_ID=%d, D_ID=%d, O_ID=%d]", w_id, d_id, o_id);
             if (trace) LOG.warn(msg);
             throw new RuntimeException(msg);
         }
         if (rs.get(0).get("CACHE_HIT") == null) {
             txnHit = false;
         }
         stock_count = (int) Double.parseDouble(rs.get(0).get("stock_count").toString());
         if (trace) LOG.trace("stockGetCountStock RESULT=" + stock_count);

         conn.commit();

         if (trace) {
             StringBuilder terminalMessage = new StringBuilder();
             terminalMessage.append("\n+-------------------------- STOCK-LEVEL --------------------------+");
             terminalMessage.append("\n Warehouse: ");
             terminalMessage.append(w_id);
             terminalMessage.append("\n District:  ");
             terminalMessage.append(d_id);
             terminalMessage.append("\n\n Stock Level Threshold: ");
             terminalMessage.append(threshold);
             terminalMessage.append("\n Low Stock Count:       ");
             terminalMessage.append(stock_count);
             terminalMessage.append("\n+-----------------------------------------------------------------+\n\n");
             LOG.trace(terminalMessage.toString());
         }
         return txnHit;
	 }
}
