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
import com.oltpbenchmark.benchmarks.tpcc.TPCCConfig;
import com.oltpbenchmark.benchmarks.tpcc.pojo.Customer;

public class Payment extends TPCCProcedure {

    private static final Logger LOG = Logger.getLogger(Payment.class);
    private boolean txnHit;

    // Payment Txn

    public boolean run(Connection conn, WebResource wr, Random gen,
                         int w_id, int numWarehouses,
                         int terminalDistrictLowerID, int terminalDistrictUpperID, TPCCWorker w) throws SQLException {
        txnHit = true;
        int districtID = TPCCUtil.randomNumber(terminalDistrictLowerID, terminalDistrictUpperID, gen);
        int customerID = TPCCUtil.getCustomerID(gen);

        int x = TPCCUtil.randomNumber(1, 100, gen);
        int customerDistrictID;
        int customerWarehouseID;
        if (x <= 85) {
            customerDistrictID = districtID;
            customerWarehouseID = w_id;
        } else {
            customerDistrictID = TPCCUtil.randomNumber(1, TPCCConfig.configDistPerWhse, gen);
            do {
                customerWarehouseID = TPCCUtil.randomNumber(1, numWarehouses, gen);
            } while (customerWarehouseID == w_id && numWarehouses > 1);
        }

        long y = TPCCUtil.randomNumber(1, 100, gen);
        boolean customerByName;
        String customerLastName = null;
        customerID = -1;
        if (y <= 60) {
            // 60% lookups by last name
            customerByName = true;
            customerLastName = TPCCUtil.getNonUniformRandomLastNameForRun(gen);
        } else {
            // 40% lookups by customer ID
            customerByName = false;
            customerID = TPCCUtil.getCustomerID(gen);
        }

        float paymentAmount = (float) (TPCCUtil.randomNumber(100, 500000, gen) / 100.0);

        String w_street_1, w_street_2, w_city, w_state, w_zip, w_name;
        String d_street_1, d_street_2, d_city, d_state, d_zip, d_name;

        String payUpdateWhse = "UPDATE " + TPCCConstants.TABLENAME_WAREHOUSE +
                "   SET W_YTD = W_YTD + " + paymentAmount +
                " WHERE W_ID = " + w_id;
        // MySQL reports deadlocks due to lock upgrades:
        // t1: read w_id = x; t2: update w_id = x; t1 update w_id = x
        int result = RestQuery.restOtherQuery(wr, payUpdateWhse, w.getId());
        if (result == 0)
            throw new RuntimeException("W_ID=" + w_id + " not found!");

        String payGetWhse = "SELECT W_STREET_1, W_STREET_2, W_CITY, W_STATE, W_ZIP, W_NAME" +
                "  FROM " + TPCCConstants.TABLENAME_WAREHOUSE +
                " WHERE W_ID = " + w_id;
        List<Map<String, Object>> rs = RestQuery.restReadQuery(wr, payGetWhse, w.getId());
        if (rs.isEmpty())
            throw new RuntimeException("W_ID=" + w_id + " not found!");
        if (rs.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }
        w_street_1 = rs.get(0).get("w_street_1").toString();
        w_street_2 = rs.get(0).get("w_street_2").toString();
        w_city = rs.get(0).get("w_city").toString();
        w_state = rs.get(0).get("w_state").toString();
        w_zip = rs.get(0).get("w_zip").toString();
        w_name = rs.get(0).get("w_name").toString();
        rs = null;

        String payUpdateDist = "UPDATE " + TPCCConstants.TABLENAME_DISTRICT +
                "   SET D_YTD = D_YTD + " + paymentAmount +
                " WHERE D_W_ID = " + w_id +
                "   AND D_ID = " + districtID;
        result = RestQuery.restOtherQuery(wr, payUpdateDist, w.getId());
        if (result == 0)
            throw new RuntimeException("D_ID=" + districtID + " D_W_ID=" + w_id + " not found!");

        String payGetDist = "SELECT D_STREET_1, D_STREET_2, D_CITY, D_STATE, D_ZIP, D_NAME" +
                "  FROM " + TPCCConstants.TABLENAME_DISTRICT +
                " WHERE D_W_ID = " + w_id +
                "   AND D_ID = " + districtID;
        rs = RestQuery.restReadQuery(wr, payGetDist, w.getId());
        if (rs.isEmpty())
            throw new RuntimeException("D_ID=" + districtID + " D_W_ID=" + w_id + " not found!");
        if (rs.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }
        d_street_1 = rs.get(0).get("d_street_1").toString();
        d_street_2 = rs.get(0).get("d_street_2").toString();
        d_city = rs.get(0).get("d_city").toString();
        d_state = rs.get(0).get("d_state").toString();
        d_zip = rs.get(0).get("d_zip").toString();
        d_name = rs.get(0).get("d_name").toString();
        rs = null;

        Customer c;
        if (customerByName) {
            assert customerID <= 0;
            c = getCustomerByName(wr, customerWarehouseID, customerDistrictID, customerLastName, w);
        } else {
            assert customerLastName == null;
            c = getCustomerById(wr, customerWarehouseID, customerDistrictID, customerID, conn, w);
        }

        c.c_balance -= paymentAmount;
        c.c_ytd_payment += paymentAmount;
        c.c_payment_cnt += 1;
        String c_data = null;
        if (c.c_credit.equals("BC")) { // bad credit
            String payGetCustCdata = "SELECT C_DATA " +
                    "  FROM " + TPCCConstants.TABLENAME_CUSTOMER +
                    " WHERE C_W_ID = " + customerWarehouseID +
                    "   AND C_D_ID = " + customerDistrictID +
                    "   AND C_ID = " + c.c_id;
            rs = RestQuery.restReadQuery(wr, payGetCustCdata, w.getId());
            if (rs.isEmpty())
                throw new RuntimeException("C_ID=" + c.c_id + " C_W_ID=" + customerWarehouseID + " C_D_ID=" + customerDistrictID + " not found!");
            if (rs.get(0).get("CACHE_HIT") == null) {
                txnHit = false;
            }
            c_data = rs.get(0).get("c_data").toString();
            rs = null;

            c_data = c.c_id + " " + customerDistrictID + " " + customerWarehouseID + " " + districtID + " " + w_id + " " + paymentAmount + " | " + c_data;
            if (c_data.length() > 500)
                c_data = c_data.substring(0, 500);

            String payUpdateCustBalCdata = "UPDATE " + TPCCConstants.TABLENAME_CUSTOMER +
                    "   SET C_BALANCE = " + c.c_balance + ", " +
                    "       C_YTD_PAYMENT = " + c.c_ytd_payment + ", " +
                    "       C_PAYMENT_CNT = " + c.c_payment_cnt + ", " +
                    "       C_DATA = '" + c_data + "'" +
                    " WHERE C_W_ID = " + customerWarehouseID +
                    "   AND C_D_ID = " + customerDistrictID +
                    "   AND C_ID = " + c.c_id;
            result = RestQuery.restOtherQuery(wr, payUpdateCustBalCdata, w.getId());

            if (result == 0)
                throw new RuntimeException("Error in PYMNT Txn updating Customer C_ID=" + c.c_id + " C_W_ID=" + customerWarehouseID + " C_D_ID=" + customerDistrictID);

        } else { // GoodCredit

            String payUpdateCustBal =  "UPDATE " + TPCCConstants.TABLENAME_CUSTOMER +
                    "   SET C_BALANCE = " + c.c_balance + ", " +
                    "       C_YTD_PAYMENT = " + c.c_ytd_payment + ", " +
                    "       C_PAYMENT_CNT = " + c.c_payment_cnt +
                    " WHERE C_W_ID = " + customerWarehouseID +
                    "   AND C_D_ID = " + customerDistrictID +
                    "   AND C_ID = " + c.c_id;
            result = RestQuery.restOtherQuery(wr, payUpdateCustBal, w.getId());

            if (result == 0)
                throw new RuntimeException("C_ID=" + c.c_id + " C_W_ID=" + customerWarehouseID + " C_D_ID=" + customerDistrictID + " not found!");

        }

        if (w_name.length() > 10)
            w_name = w_name.substring(0, 10);
        if (d_name.length() > 10)
            d_name = d_name.substring(0, 10);
        String h_data = w_name + "    " + d_name;

        String payInsertHist = "INSERT INTO " + TPCCConstants.TABLENAME_HISTORY +
                " (H_C_D_ID, H_C_W_ID, H_C_ID, H_D_ID, H_W_ID, H_DATE, H_AMOUNT, H_DATA) " +
                " VALUES (" + customerDistrictID + "," + customerWarehouseID + "," + c.c_id + "," + districtID + "," +
                w_id + ", '" + w.getBenchmarkModule().getTimestamp(System.currentTimeMillis()) + "'," + paymentAmount +
                ", '" + h_data + "')";
        RestQuery.restOtherQuery(wr, payInsertHist, w.getId());

        conn.commit();

        if (LOG.isTraceEnabled()) {
            StringBuilder terminalMessage = new StringBuilder();
            terminalMessage.append("\n+---------------------------- PAYMENT ----------------------------+");
            terminalMessage.append("\n Date: " + TPCCUtil.getCurrentTime());
            terminalMessage.append("\n\n Warehouse: ");
            terminalMessage.append(w_id);
            terminalMessage.append("\n   Street:  ");
            terminalMessage.append(w_street_1);
            terminalMessage.append("\n   Street:  ");
            terminalMessage.append(w_street_2);
            terminalMessage.append("\n   City:    ");
            terminalMessage.append(w_city);
            terminalMessage.append("   State: ");
            terminalMessage.append(w_state);
            terminalMessage.append("  Zip: ");
            terminalMessage.append(w_zip);
            terminalMessage.append("\n\n District:  ");
            terminalMessage.append(districtID);
            terminalMessage.append("\n   Street:  ");
            terminalMessage.append(d_street_1);
            terminalMessage.append("\n   Street:  ");
            terminalMessage.append(d_street_2);
            terminalMessage.append("\n   City:    ");
            terminalMessage.append(d_city);
            terminalMessage.append("   State: ");
            terminalMessage.append(d_state);
            terminalMessage.append("  Zip: ");
            terminalMessage.append(d_zip);
            terminalMessage.append("\n\n Customer:  ");
            terminalMessage.append(c.c_id);
            terminalMessage.append("\n   Name:    ");
            terminalMessage.append(c.c_first);
            terminalMessage.append(" ");
            terminalMessage.append(c.c_middle);
            terminalMessage.append(" ");
            terminalMessage.append(c.c_last);
            terminalMessage.append("\n   Street:  ");
            terminalMessage.append(c.c_street_1);
            terminalMessage.append("\n   Street:  ");
            terminalMessage.append(c.c_street_2);
            terminalMessage.append("\n   City:    ");
            terminalMessage.append(c.c_city);
            terminalMessage.append("   State: ");
            terminalMessage.append(c.c_state);
            terminalMessage.append("  Zip: ");
            terminalMessage.append(c.c_zip);
            terminalMessage.append("\n   Since:   ");
            if (c.c_since != null) {
                terminalMessage.append(c.c_since.toString());
            } else {
                terminalMessage.append("");
            }
            terminalMessage.append("\n   Credit:  ");
            terminalMessage.append(c.c_credit);
            terminalMessage.append("\n   %Disc:   ");
            terminalMessage.append(c.c_discount);
            terminalMessage.append("\n   Phone:   ");
            terminalMessage.append(c.c_phone);
            terminalMessage.append("\n\n Amount Paid:      ");
            terminalMessage.append(paymentAmount);
            terminalMessage.append("\n Credit Limit:     ");
            terminalMessage.append(c.c_credit_lim);
            terminalMessage.append("\n New Cust-Balance: ");
            terminalMessage.append(c.c_balance);
            if (c.c_credit.equals("BC")) {
                if (c_data.length() > 50) {
                    terminalMessage.append("\n\n Cust-Data: " + c_data.substring(0, 50));
                    int data_chunks = c_data.length() > 200 ? 4 : c_data.length() / 50;
                    for (int n = 1; n < data_chunks; n++)
                        terminalMessage.append("\n            " + c_data.substring(n * 50, (n + 1) * 50));
                } else {
                    terminalMessage.append("\n\n Cust-Data: " + c_data);
                }
            }
            terminalMessage.append("\n+-----------------------------------------------------------------+\n\n");

            LOG.trace(terminalMessage.toString());
        }

        return txnHit;
    }

    // attention duplicated code across trans... ok for now to maintain separate
    // prepared statements
    public Customer getCustomerById(WebResource wr, int c_w_id, int c_d_id, int c_id,
                                    Connection conn, TPCCWorker w) throws SQLException {

        String payGetCust = "SELECT C_FIRST, C_MIDDLE, C_LAST, C_STREET_1, C_STREET_2, " +
                "       C_CITY, C_STATE, C_ZIP, C_PHONE, C_CREDIT, C_CREDIT_LIM, " +
                "       C_DISCOUNT, C_BALANCE, C_YTD_PAYMENT, C_PAYMENT_CNT, C_SINCE " +
                "  FROM " + TPCCConstants.TABLENAME_CUSTOMER +
                " WHERE C_W_ID = " + c_w_id +
                "   AND C_D_ID = " + c_d_id +
                "   AND C_ID = " + c_id;
        List<Map<String, Object>> rs = RestQuery.restReadQuery(wr, payGetCust, w.getId());
        if (rs.isEmpty()) {
            throw new RuntimeException("C_ID=" + c_id + " C_D_ID=" + c_d_id + " C_W_ID=" + c_w_id + " not found!");
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
    public Customer getCustomerByName(WebResource wr, int c_w_id, int c_d_id,
                                      String customerLastName, TPCCWorker w) throws SQLException {
        ArrayList<Customer> customers = new ArrayList<Customer>();

        String customerByName =  "SELECT C_FIRST, C_MIDDLE, C_ID, C_STREET_1, C_STREET_2, C_CITY, " +
                "       C_STATE, C_ZIP, C_PHONE, C_CREDIT, C_CREDIT_LIM, C_DISCOUNT, " +
                "       C_BALANCE, C_YTD_PAYMENT, C_PAYMENT_CNT, C_SINCE " +
                "  FROM " + TPCCConstants.TABLENAME_CUSTOMER +
                " WHERE C_W_ID = " + c_w_id +
                "   AND C_D_ID = " + c_d_id +
                "   AND C_LAST = '" + customerLastName + "'" +
                " ORDER BY C_FIRST";
        List<Map<String, Object>> rs = RestQuery.restReadQuery(wr, customerByName, w.getId());
        if (LOG.isTraceEnabled()) LOG.trace("C_LAST=" + customerLastName + " C_D_ID=" + c_d_id + " C_W_ID=" + c_w_id);
        if (rs.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }

        for (Map<String, Object> r : rs) {
            Customer c = TPCCUtil.newCustomerFromResults(r);
            c.c_id = (int) Double.parseDouble(rs.get(0).get("c_id").toString());
            c.c_last = customerLastName;
            customers.add(c);
        }

        if (customers.size() == 0) {
            throw new RuntimeException("C_LAST=" + customerLastName + " C_D_ID=" + c_d_id + " C_W_ID=" + c_w_id + " not found!");
        }

        // TPC-C 2.5.2.2: Position n / 2 rounded up to the next integer, but
        // that
        // counts starting from 1.
        int index = customers.size() / 2;
        if (customers.size() % 2 == 0) {
            index -= 1;
        }
        return customers.get(index);
    }


}
