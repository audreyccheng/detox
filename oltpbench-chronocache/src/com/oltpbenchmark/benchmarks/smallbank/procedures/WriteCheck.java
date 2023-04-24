/***************************************************************************
 *  Copyright (C) 2013 by H-Store Project                                  *
 *  Brown University                                                       *
 *  Massachusetts Institute of Technology                                  *
 *  Yale University                                                        *
 *                                                                         *
 *  Permission is hereby granted, free of charge, to any person obtaining  *
 *  a copy of this software and associated documentation files (the        *
 *  "Software"), to deal in the Software without restriction, including    *
 *  without limitation the rights to use, copy, modify, merge, publish,    *
 *  distribute, sublicense, and/or sell copies of the Software, and to     *
 *  permit persons to whom the Software is furnished to do so, subject to  *
 *  the following conditions:                                              *
 *                                                                         *
 *  The above copyright notice and this permission notice shall be         *
 *  included in all copies or substantial portions of the Software.        *
 *                                                                         *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,        *
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF     *
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. *
 *  IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR      *
 *  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,  *
 *  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR  *
 *  OTHER DEALINGS IN THE SOFTWARE.                                        *
 ***************************************************************************/
package com.oltpbenchmark.benchmarks.smallbank.procedures;

import com.sun.jersey.api.client.WebResource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.api.RestQuery;
import com.oltpbenchmark.benchmarks.smallbank.SmallBankConstants;

/**
 * WriteCheck Procedure
 * Original version by Mohammad Alomari and Michael Cahill
 * @author pavlo
 */
public class WriteCheck extends Procedure {

    
    public boolean run(WebResource resource, String custName, double amount, int id) throws SQLException {
        boolean txnHit = true;
        // First convert the custName to the custId
        String stmt0 = "SELECT custid, name FROM " + SmallBankConstants.TABLENAME_ACCOUNTS +
                " WHERE name = '" + custName + "'";
        List<Map<String, Object>> r0 = RestQuery.restReadQuery(resource, stmt0, id);
        if (r0.isEmpty()) {
            String msg = "Invalid account '" + custName + "'";
            throw new UserAbortException(msg);
        }
        if (r0.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }
        long custId = (long) Double.parseDouble(r0.get(0).get("custid").toString());
        
        // Then get their account balances
        String balStmt0 = "SELECT bal FROM " + SmallBankConstants.TABLENAME_SAVINGS +
                " WHERE custid = " + custId;
        List<Map<String, Object>> balRes0 = RestQuery.restReadQuery(resource, balStmt0, id);

        if (balRes0.isEmpty()) {
            String msg = String.format("No %s for customer #%d",
                                       SmallBankConstants.TABLENAME_SAVINGS, 
                                       custId);
            throw new UserAbortException(msg);
        }
        if (balRes0.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }

        String balStmt1 = "SELECT bal FROM " + SmallBankConstants.TABLENAME_CHECKING +
                " WHERE custid = " + custId;
        List<Map<String, Object>> balRes1 = RestQuery.restReadQuery(resource, balStmt1, id);
        if (balRes1.isEmpty()) {
            String msg = String.format("No %s for customer #%d",
                                       SmallBankConstants.TABLENAME_CHECKING, 
                                       custId);
            throw new UserAbortException(msg);
        }
        if (balRes1.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }
        double total = Double.parseDouble(balRes0.get(0).get("bal").toString()) + Double.parseDouble(balRes1.get(0).get("bal").toString());
        
        String updateStmt;
        if (total < amount) {
            updateStmt = "UPDATE " + SmallBankConstants.TABLENAME_CHECKING +
                    "   SET bal = bal - " + (amount - 1) +
                    " WHERE custid = " + custId;
        } else {
            updateStmt = "UPDATE " + SmallBankConstants.TABLENAME_CHECKING +
                    "   SET bal = bal - " + amount +
                    " WHERE custid = " + custId;        }
        int status = RestQuery.restOtherQuery(resource, updateStmt, id);
        assert(status == 1) :
            String.format("Failed to update %s for customer #%d [total=%.2f / amount=%.2f]",
                          SmallBankConstants.TABLENAME_CHECKING, custId, total, amount);
        
        return txnHit;
    }
}