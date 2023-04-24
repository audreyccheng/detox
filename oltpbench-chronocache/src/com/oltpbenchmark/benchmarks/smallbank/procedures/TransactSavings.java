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
import com.oltpbenchmark.api.RestQuery;
import com.oltpbenchmark.benchmarks.smallbank.SmallBankConstants;

/**
 * TransactSavings Procedure
 * Original version by Mohammad Alomari and Michael Cahill
 * @author pavlo
 */
public class TransactSavings extends Procedure {
    public boolean run(WebResource resource, String custName, double amount, int id) throws SQLException {
        boolean txnHit = true;
        // First convert the custName to the acctId
        String stmt = "SELECT custid, name FROM " + SmallBankConstants.TABLENAME_ACCOUNTS +
                " WHERE name = '" + custName + "'";
        List<Map<String, Object>> result = RestQuery.restReadQuery(resource, stmt, id);
        if (result.isEmpty()) {
            String msg = "Invalid account '" + custName + "'";
            throw new UserAbortException(msg);
        }
        if (result.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }
        long custId = (long) Double.parseDouble(result.get(0).get("custid").toString());

        // Get Balance Information
        stmt = "SELECT bal FROM " + SmallBankConstants.TABLENAME_SAVINGS +
                " WHERE custid = " + custId;
        result = RestQuery.restReadQuery(resource, stmt, id);
        if (result.isEmpty()) {
            String msg = String.format("No %s for customer #%d",
                                       SmallBankConstants.TABLENAME_SAVINGS, 
                                       custId);
            throw new UserAbortException(msg);
        }
        if (result.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }
        double balance = Double.parseDouble(result.get(0).get("bal").toString()) - amount;
        
        // Make sure that they have enough
        if (balance < 0) {
            String msg = String.format("Negative %s balance for customer #%d",
                                       SmallBankConstants.TABLENAME_SAVINGS, 
                                       custId);
            throw new UserAbortException(msg);
        }
        
        // Then update their savings balance
        stmt = "UPDATE " + SmallBankConstants.TABLENAME_SAVINGS +
                "   SET bal = bal - " + amount +
                " WHERE custid = " + custId;
        int status = RestQuery.restOtherQuery(resource, stmt, id);
        assert(status == 1) :
            String.format("Failed to update %s for customer #%d [balance=%.2f / amount=%.2f]",
                          SmallBankConstants.TABLENAME_CHECKING, custId, balance, amount);
        
        return txnHit;
    }
}