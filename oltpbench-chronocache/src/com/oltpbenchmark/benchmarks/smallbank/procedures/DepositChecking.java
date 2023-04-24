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
 * DepositChecking Procedure
 * Original version by Mohammad Alomari and Michael Cahill
 * @author pavlo
 */
public class DepositChecking extends Procedure {
    
    public final SQLStmt GetAccount = new SQLStmt(
        "SELECT * FROM " + SmallBankConstants.TABLENAME_ACCOUNTS +
        " WHERE name = ?"
    );
    
    public final SQLStmt UpdateCheckingBalance = new SQLStmt(
        "UPDATE " + SmallBankConstants.TABLENAME_CHECKING + 
        "   SET bal = bal + ? " +
        " WHERE custid = ?"
    );
    
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

        // Then update their checking balance
        String stmt1 = "UPDATE " + SmallBankConstants.TABLENAME_CHECKING +
                "   SET bal = bal + " + amount +
                " WHERE custid = " + custId;
        int status = RestQuery.restOtherQuery(resource, stmt1, id);
        assert(status == 1) :
            String.format("Failed to update %s for customer #%d [amount=%.2f]",
                          SmallBankConstants.TABLENAME_CHECKING, custId, amount);
        
        return txnHit;
    }
}