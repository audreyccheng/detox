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

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
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
import com.oltpbenchmark.benchmarks.tpcc.pojo.Customer;

public class OrderStatus extends TPCCProcedure {

    private static final Logger LOG = Logger.getLogger(OrderStatus.class);
    private boolean txnHit;

    public boolean run(Connection conn, WebResource wr, Random gen, int w_id, int numWarehouses, int terminalDistrictLowerID, int terminalDistrictUpperID, TPCCWorker w) throws SQLException {
        boolean trace = LOG.isTraceEnabled();
        txnHit = true;

        int d_id = TPCCUtil.randomNumber(terminalDistrictLowerID, terminalDistrictUpperID, gen);
        boolean c_by_name = false;
        int y = TPCCUtil.randomNumber(1, 100, gen);
        String c_last = null;
        int c_id = -1;
        if (y <= 60) {
            c_by_name = true;
            c_last = TPCCUtil.getNonUniformRandomLastNameForRun(gen);
        } else {
            c_by_name = false;
            c_id = TPCCUtil.getCustomerID(gen);
        }

        int o_id = -1, o_carrier_id = -1;
        Timestamp o_entry_d;
        ArrayList<String> orderLines = new ArrayList<String>();

        Customer c;
        if (c_by_name) {
            assert c_id <= 0;
            // TODO: This only needs c_balance, c_first, c_middle, c_id
            // only fetch those columns?
            c = getCustomerByName(w_id, d_id, c_last, wr, w);
        } else {
            assert c_last == null;
            c = getCustomerById(w_id, d_id, c_id, conn, wr, w);
        }

        // find the newest order for the customer
        // retrieve the carrier & order date for the most recent order.
        String ordStatGetNewestOrd = "SELECT O_ID, O_CARRIER_ID, O_ENTRY_D " +
                "  FROM " + TPCCConstants.TABLENAME_OPENORDER +
                " WHERE O_W_ID = " + w_id +
                "   AND O_D_ID = " + d_id +
                "   AND O_C_ID = " + c.c_id +
                " ORDER BY O_ID DESC LIMIT 1";
        if (trace) LOG.trace("ordStatGetNewestOrd START");
        List<Map<String, Object>> rs = RestQuery.restReadQuery(wr, ordStatGetNewestOrd, w.getId());
        if (trace) LOG.trace("ordStatGetNewestOrd END");

        if (rs.isEmpty()) {
            String msg = String.format("No order records for CUSTOMER [C_W_ID=%d, C_D_ID=%d, C_ID=%d]",
                                       w_id, d_id, c.c_id);
            if (trace) LOG.warn(msg);
            throw new RuntimeException(msg);
        }
        if (rs.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }

        o_id = (int) Double.parseDouble(rs.get(0).get("o_id").toString());
        // o_carrier_id = (int) Double.parseDouble(rs.get(0).get("o_carrier_id").toString());

        try {
            o_entry_d = new Timestamp(Long.parseLong(rs.get(0).get("o_entry_d").toString()));
        } catch (java.lang.NumberFormatException e) {
            o_entry_d = new Timestamp(Date.parse(rs.get(0).get("o_entry_d").toString()));
        }

        // retrieve the order lines for the most recent order
        String ordStatGetOrderLines = "SELECT OL_I_ID, OL_SUPPLY_W_ID, OL_QUANTITY, OL_AMOUNT, OL_DELIVERY_D " +
                "  FROM " + TPCCConstants.TABLENAME_ORDERLINE +
                " WHERE OL_O_ID = " + o_id +
                "   AND OL_D_ID = " + d_id +
                "   AND OL_W_ID = " + w_id;
        if (trace) LOG.trace("ordStatGetOrderLines START");
        rs = RestQuery.restReadQuery(wr, ordStatGetOrderLines, w.getId());
        if (trace) LOG.trace("ordStatGetOrderLines END");
        if (rs.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }

        /*
        for (Map<String, Object> r : rs) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append((long) Double.parseDouble(r.get("ol_supply_w_id").toString()));
            sb.append(" - ");
            sb.append((long) Double.parseDouble(r.get("ol_i_id").toString()));
            sb.append(" - ");
            sb.append((long) Double.parseDouble(r.get("ol_quantity").toString()));
            sb.append(" - ");
            sb.append(TPCCUtil.formattedDouble(Double.parseDouble(r.get("ol_amount").toString())));
            sb.append(" - ");
            if (r.get("ol_delivery_d") != null)
                sb.append(r.get("ol_delivery_d"));
            else
                sb.append("99-99-9999");
            sb.append("]");
            orderLines.add(sb.toString());
        }
        */

        // commit the transaction
        conn.commit();
        
        if (orderLines.isEmpty()) {
            String msg = String.format("Order record had no order line items [C_W_ID=%d, C_D_ID=%d, C_ID=%d, O_ID=%d]",
                                       w_id, d_id, c.c_id, o_id);
            if (trace) LOG.warn(msg);
        }

        if (trace) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            sb.append("+-------------------------- ORDER-STATUS -------------------------+\n");
            sb.append(" Date: ");
            sb.append(TPCCUtil.getCurrentTime());
            sb.append("\n\n Warehouse: ");
            sb.append(w_id);
            sb.append("\n District:  ");
            sb.append(d_id);
            sb.append("\n\n Customer:  ");
            sb.append(c.c_id);
            sb.append("\n   Name:    ");
            sb.append(c.c_first);
            sb.append(" ");
            sb.append(c.c_middle);
            sb.append(" ");
            sb.append(c.c_last);
            sb.append("\n   Balance: ");
            sb.append(c.c_balance);
            sb.append("\n\n");
            if (o_id == -1) {
                sb.append(" Customer has no orders placed.\n");
            } else {
                sb.append(" Order-Number: ");
                sb.append(o_id);
                sb.append("\n    Entry-Date: ");
                sb.append(o_entry_d);
                sb.append("\n    Carrier-Number: ");
                sb.append(o_carrier_id);
                sb.append("\n\n");
                if (orderLines.size() != 0) {
                    sb.append(" [Supply_W - Item_ID - Qty - Amount - Delivery-Date]\n");
                    for (String orderLine : orderLines) {
                        sb.append(" ");
                        sb.append(orderLine);
                        sb.append("\n");
                    }
                } else {
                    LOG.trace(" This Order has no Order-Lines.\n");
                }
            }
            sb.append("+-----------------------------------------------------------------+\n\n");
            LOG.trace(sb.toString());
        }
        
        return txnHit;
    }

    // attention duplicated code across trans... ok for now to maintain separate
    // prepared statements
    public Customer getCustomerById(int c_w_id, int c_d_id, int c_id, Connection conn, WebResource wr, TPCCWorker w) throws SQLException {
        boolean trace = LOG.isTraceEnabled();
        String payGetCust =
                "SELECT C_FIRST, C_MIDDLE, C_LAST, C_STREET_1, C_STREET_2, " +
                        "       C_CITY, C_STATE, C_ZIP, C_PHONE, C_CREDIT, C_CREDIT_LIM, " +
                        "       C_DISCOUNT, C_BALANCE, C_YTD_PAYMENT, C_PAYMENT_CNT, C_SINCE " +
                        "  FROM " + TPCCConstants.TABLENAME_CUSTOMER +
                        " WHERE C_W_ID = " + c_w_id +
                        "   AND C_D_ID = " + c_d_id +
                        "   AND C_ID = " + c_id;
        if (trace) LOG.trace("payGetCust START");
        List<Map<String, Object>> rs = RestQuery.restReadQuery(wr, payGetCust, w.getId());
        if (trace) LOG.trace("payGetCust END");
        if (rs.isEmpty()) {
            String msg = String.format("Failed to get CUSTOMER [C_W_ID=%d, C_D_ID=%d, C_ID=%d]",
                                       c_w_id, c_d_id, c_id);
            if (trace) LOG.warn(msg);
            throw new RuntimeException(msg);
        }
        if (rs.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }

        Customer c = TPCCUtil.newCustomerFromResults(rs.get(0));
        c.c_id = c_id;
        c.c_last = rs.get(0).get("c_last").toString();
        return c;
    }

    // attention this code is repeated in other transacitons... ok for now to
    // allow for separate statements.
    public Customer getCustomerByName(int c_w_id, int c_d_id, String c_last, WebResource wr, TPCCWorker w) throws SQLException {
        ArrayList<Customer> customers = new ArrayList<Customer>();
        boolean trace = LOG.isDebugEnabled();

        String customerByName =
                "SELECT C_FIRST, C_MIDDLE, C_ID, C_STREET_1, C_STREET_2, C_CITY, " +
                        "       C_STATE, C_ZIP, C_PHONE, C_CREDIT, C_CREDIT_LIM, C_DISCOUNT, " +
                        "       C_BALANCE, C_YTD_PAYMENT, C_PAYMENT_CNT, C_SINCE " +
                        "  FROM " + TPCCConstants.TABLENAME_CUSTOMER +
                        " WHERE C_W_ID = " + c_w_id +
                        "   AND C_D_ID = " + c_d_id +
                        "   AND C_LAST = '" + c_last + "'" +
                        " ORDER BY C_FIRST";

        if (trace) LOG.trace("customerByName START");
        List<Map<String, Object>> rs = RestQuery.restReadQuery(wr, customerByName, w.getId());
        if (trace) LOG.trace("customerByName END");

        if (rs.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }
        for (Map<String, Object> r : rs) {
            Customer c = TPCCUtil.newCustomerFromResults(r);
            c.c_id = (int) Double.parseDouble(r.get("c_id").toString());
            c.c_last = c_last;
            customers.add(c);
        }

        if (customers.size() == 0) {
            String msg = String.format("Failed to get CUSTOMER [C_W_ID=%d, C_D_ID=%d, C_LAST=%s]",
                                       c_w_id, c_d_id, c_last);
            if (trace) LOG.warn(msg);
            throw new RuntimeException(msg);
        }

        // TPC-C 2.5.2.2: Position n / 2 rounded up to the next integer, but
        // that counts starting from 1.
        int index = customers.size() / 2;
        if (customers.size() % 2 == 0) {
            index -= 1;
        }
        return customers.get(index);
    }



}



