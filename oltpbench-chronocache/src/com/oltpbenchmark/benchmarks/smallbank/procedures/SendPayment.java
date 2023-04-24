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
 * SendPayment Procedure
 * @author pavlo
 */
public class SendPayment extends Procedure {
    public boolean run(WebResource resource, long sendAcct, long destAcct, double amount, int id) throws SQLException {
        boolean txnHit = true;
        // Get Account Information
        String stmt0 = "SELECT custid, name FROM " + SmallBankConstants.TABLENAME_ACCOUNTS +
                " WHERE custid = " + sendAcct;
        List<Map<String, Object>> r0 = RestQuery.restReadQuery(resource, stmt0, id);
        if (r0.isEmpty()) {
            String msg = "Invalid account '" + sendAcct + "'";
            throw new UserAbortException(msg);
        }
        if (r0.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }
        
        String stmt1 = "SELECT custid, name FROM " + SmallBankConstants.TABLENAME_ACCOUNTS +
                " WHERE custid = " + destAcct;
        List<Map<String, Object>> r1 = RestQuery.restReadQuery(resource, stmt1, id);
        if (r1.isEmpty()) {
            String msg = "Invalid account '" + destAcct + "'";
            throw new UserAbortException(msg);
        }
        if (r1.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }
        
        // Get the sender's account balance
        String balStmt0 = "SELECT bal FROM " + SmallBankConstants.TABLENAME_CHECKING +
                " WHERE custid = " + sendAcct;
        List<Map<String, Object>> balRes0 = RestQuery.restReadQuery(resource, balStmt0, id);
        if (balRes0.isEmpty()) {
            String msg = String.format("No %s for customer #%d",
                                       SmallBankConstants.TABLENAME_CHECKING, 
                                       sendAcct);
            throw new UserAbortException(msg);
        }
        if (balRes0.get(0).get("CACHE_HIT") == null) {
            txnHit = false;
        }
        double balance = Double.parseDouble(balRes0.get(0).get("bal").toString());
        
        // Make sure that they have enough money
        if (balance < amount) {
            String msg = String.format("Insufficient %s funds for customer #%d",
                                       SmallBankConstants.TABLENAME_CHECKING, sendAcct);
            throw new UserAbortException(msg);
        }
        
        // Debt
        String updateStmt = "UPDATE " + SmallBankConstants.TABLENAME_CHECKING +
                "   SET bal = bal + " + (amount * -1d) +
                " WHERE custid = " + sendAcct;
        int status = RestQuery.restOtherQuery(resource, updateStmt, id);
        assert(status == 1) :
            String.format("Failed to update %s for customer #%d [amount=%.2f]",
                          SmallBankConstants.TABLENAME_CHECKING, sendAcct, amount);
        
        // Credit
        updateStmt = "UPDATE " + SmallBankConstants.TABLENAME_CHECKING +
                "   SET bal = bal + " + amount +
                " WHERE custid = " + destAcct;
        status = RestQuery.restOtherQuery(resource, updateStmt, id);
        assert(status == 1) :
            String.format("Failed to update %s for customer #%d [amount=%.2f]",
                          SmallBankConstants.TABLENAME_CHECKING, destAcct, amount);
        
        return txnHit;
    }
}