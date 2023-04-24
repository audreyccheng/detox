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
import java.sql.Timestamp;
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
import com.oltpbenchmark.benchmarks.tpcc.TPCCConfig;

public class Delivery extends TPCCProcedure {

    private static final Logger LOG = Logger.getLogger(Delivery.class);

    public boolean run(Connection conn, WebResource wr, Random gen,
                         int w_id, int numWarehouses,
                         int terminalDistrictLowerID, int terminalDistrictUpperID,
                         TPCCWorker w) throws SQLException {
		boolean txnHit = true;
        boolean trace = LOG.isDebugEnabled();
        int o_carrier_id = TPCCUtil.randomNumber(1, 10, gen);
        Timestamp timestamp = w.getBenchmarkModule().getTimestamp(System.currentTimeMillis());

		int d_id, c_id;
        float ol_total = 0;
        int[] orderIDs;

        orderIDs = new int[10];
        for (d_id = 1; d_id <= terminalDistrictUpperID; d_id++) {
            String delivGetOrderId =
            "SELECT NO_O_ID FROM " + TPCCConstants.TABLENAME_NEWORDER +
                    " WHERE NO_D_ID = " + d_id +
                    "   AND NO_W_ID = " + w_id +
                    " ORDER BY NO_O_ID ASC " +
                    " LIMIT 1";
            if (trace) LOG.trace("delivGetOrderId START");
            List<Map<String, Object>> rs = RestQuery.restReadQuery(wr, delivGetOrderId, w.getId());
            if (trace) LOG.trace("delivGetOrderId END");
            if (rs.isEmpty()) {
                // This district has no new orders
                // This can happen but should be rare
                if (trace) LOG.warn(String.format("District has no new orders [W_ID=%d, D_ID=%d]", w_id, d_id));
                continue;
            }
            if (rs.get(0).get("CACHE_HIT") == null) {
                txnHit = false;
            }

            int no_o_id = (int) Double.parseDouble(rs.get(0).get("no_o_id").toString());
            orderIDs[d_id - 1] = no_o_id;

            String delivDeleteNewOrder = "DELETE FROM " + TPCCConstants.TABLENAME_NEWORDER +
                    " WHERE NO_O_ID = " + no_o_id +
                    "   AND NO_D_ID = " + d_id +
                    "   AND NO_W_ID = " + w_id;
            if (trace) LOG.trace("delivDeleteNewOrder START");
            int result = RestQuery.restOtherQuery(wr, delivDeleteNewOrder, w.getId());
            if (trace) LOG.trace("delivDeleteNewOrder END");
            if (result != 1) {
                // This code used to run in a loop in an attempt to make this work
                // with MySQL's default weird consistency level. We just always run
                // this as SERIALIZABLE instead. I don't *think* that fixing this one
                // error makes this work with MySQL's default consistency. 
                // Careful auditing would be required.
                String msg = String.format("NewOrder delete failed. Not running with SERIALIZABLE isolation? " +
                                           "[w_id=%d, d_id=%d, no_o_id=%d]", w_id, d_id, no_o_id);
                throw new UserAbortException(msg);
            }

            String delivGetCustId = "SELECT O_C_ID FROM " + TPCCConstants.TABLENAME_OPENORDER +
                    " WHERE O_ID = " + no_o_id +
                    "   AND O_D_ID = " + d_id +
                    "   AND O_W_ID = " + w_id;
            if (trace) LOG.trace("delivGetCustId START");
            rs = RestQuery.restReadQuery(wr, delivGetCustId, w.getId());
            if (trace) LOG.trace("delivGetCustId END");

            if (rs.isEmpty()) {
                String msg = String.format("Failed to retrieve ORDER record [W_ID=%d, D_ID=%d, O_ID=%d]",
                                           w_id, d_id, no_o_id);
                if (trace) LOG.warn(msg);
                throw new RuntimeException(msg);
            }
            if (rs.get(0).get("CACHE_HIT") == null) {
                txnHit = false;
            }

            c_id = (int) Double.parseDouble(rs.get(0).get("o_c_id").toString());

            String delivUpdateCarrierId = "UPDATE " + TPCCConstants.TABLENAME_OPENORDER +
                    "   SET O_CARRIER_ID = " + o_carrier_id +
                    " WHERE O_ID = " + no_o_id +
                    "   AND O_D_ID = " + d_id +
                    "   AND O_W_ID = " + w_id;
            if (trace) LOG.trace("delivUpdateCarrierId START");
            result = RestQuery.restOtherQuery(wr, delivUpdateCarrierId, w.getId());
            if (trace) LOG.trace("delivUpdateCarrierId END");

            if (result != 1) {
                String msg = String.format("Failed to update ORDER record [W_ID=%d, D_ID=%d, O_ID=%d]",
                                           w_id, d_id, no_o_id);
                if (trace) LOG.warn(msg);
                throw new RuntimeException(msg);
            }

            String delivUpdateDeliveryDate = "UPDATE " + TPCCConstants.TABLENAME_ORDERLINE +
                    "   SET OL_DELIVERY_D = '" + timestamp + "' " +
                    " WHERE OL_O_ID = " + no_o_id +
                    "   AND OL_D_ID = " + d_id +
                    "   AND OL_W_ID =  " + w_id;
            if (trace) LOG.trace("delivUpdateDeliveryDate START");
            result = RestQuery.restOtherQuery(wr, delivUpdateDeliveryDate, w.getId());
            if (trace) LOG.trace("delivUpdateDeliveryDate END");

            if (result == 0){
                String msg = String.format("Failed to update ORDER_LINE records [W_ID=%d, D_ID=%d, O_ID=%d]",
                                           w_id, d_id, no_o_id);
                if (trace) LOG.warn(msg);
                throw new RuntimeException(msg);
            }

            String delivSumOrderAmount = "SELECT SUM(OL_AMOUNT) AS OL_TOTAL " +
                    "  FROM " + TPCCConstants.TABLENAME_ORDERLINE +
                    " WHERE OL_O_ID = " + no_o_id +
                    "   AND OL_D_ID = " + d_id +
                    "   AND OL_W_ID = " + w_id;
            if (trace) LOG.trace("delivSumOrderAmount START");
            rs = RestQuery.restReadQuery(wr, delivSumOrderAmount, w.getId());
            if (trace) LOG.trace("delivSumOrderAmount END");

            if (rs.isEmpty()) {
                String msg = String.format("Failed to retrieve ORDER_LINE records [W_ID=%d, D_ID=%d, O_ID=%d]",
                                           w_id, d_id, no_o_id);
                if (trace) LOG.warn(msg);
                throw new RuntimeException(msg);
            }
            if (rs.get(0).get("CACHE_HIT") == null) {
                txnHit = false;
            }
            ol_total = (float) Double.parseDouble(rs.get(0).get("ol_total").toString());

            int idx = 1; // HACK: So that we can debug this query
            String delivUpdateCustBalDelivCnt = "UPDATE " + TPCCConstants.TABLENAME_CUSTOMER +
                    "   SET C_BALANCE = C_BALANCE + " + ol_total + ", " +
                    "       C_DELIVERY_CNT = C_DELIVERY_CNT + 1 " +
                    " WHERE C_W_ID = " + w_id +
                    "   AND C_D_ID = " + d_id +
                    "   AND C_ID = " + c_id;
            if (trace) LOG.trace("delivUpdateCustBalDelivCnt START");
            result = RestQuery.restOtherQuery(wr, delivUpdateCustBalDelivCnt, w.getId());
            if (trace) LOG.trace("delivUpdateCustBalDelivCnt END");

            if (result == 0) {
                String msg = String.format("Failed to update CUSTOMER record [W_ID=%d, D_ID=%d, C_ID=%d]",
                                           w_id, d_id, c_id);
                if (trace) LOG.warn(msg);
                throw new RuntimeException(msg);
            }
        }

        conn.commit();
         
        if (trace) {
            StringBuilder terminalMessage = new StringBuilder();
            terminalMessage
                    .append("\n+---------------------------- DELIVERY ---------------------------+\n");
            terminalMessage.append(" Date: ");
            terminalMessage.append(TPCCUtil.getCurrentTime());
            terminalMessage.append("\n\n Warehouse: ");
            terminalMessage.append(w_id);
            terminalMessage.append("\n Carrier:   ");
            terminalMessage.append(o_carrier_id);
            terminalMessage.append("\n\n Delivered Orders\n");
            for (int i = 1; i <= TPCCConfig.configDistPerWhse; i++) {
                if (orderIDs[i - 1] >= 0) {
                    terminalMessage.append("  District ");
                    terminalMessage.append(i < 10 ? " " : "");
                    terminalMessage.append(i);
                    terminalMessage.append(": Order number ");
                    terminalMessage.append(orderIDs[i - 1]);
                    terminalMessage.append(" was delivered.\n");
                }
            } // FOR
            terminalMessage.append("+-----------------------------------------------------------------+\n\n");
            LOG.trace(terminalMessage.toString());
        }
	
		return txnHit;
    }

}
