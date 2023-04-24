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


package com.oltpbenchmark.benchmarks.tpcc;

/*
 * jTPCCTerminal - Terminal emulator code for jTPCC (transactions)
 *
 * Copyright (C) 2003, Raul Barbosa
 * Copyright (C) 2004-2006, Denis Lussier
 *
 */

import java.sql.SQLException;
import java.util.Random;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.apache.log4j.Logger;

import com.oltpbenchmark.api.Procedure.UserAbortException;
import com.oltpbenchmark.api.TransactionType;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.benchmarks.tpcc.procedures.TPCCProcedure;
import com.oltpbenchmark.types.TransactionStatus;

public class TPCCWorker extends Worker<TPCCBenchmark> {

    private static final Logger LOG = Logger.getLogger(TPCCWorker.class);

	private final int terminalWarehouseID;
	/** Forms a range [lower, upper] (inclusive). */
	private final int terminalDistrictLowerID;
	private final int terminalDistrictUpperID;
	// private boolean debugMessages;
	private final Random gen = new Random();

	private int numWarehouses;

	private long txnHits = 0;
	private long txnTotal = 0;

	private WebResource wr;

	public long getTxnHits() {
		return txnHits;
	}

	public long getTxnTotal() {
		return txnTotal;
	}

	public void resetTxnStats() {
		txnHits = 0;
		txnTotal = 0;
	}

	public TPCCWorker(TPCCBenchmark benchmarkModule, int id,
					  int terminalWarehouseID, int terminalDistrictLowerID,
					  int terminalDistrictUpperID, int numWarehouses, Client client)
			throws SQLException {
		super(benchmarkModule, id);
		
		this.terminalWarehouseID = terminalWarehouseID;
		this.terminalDistrictLowerID = terminalDistrictLowerID;
		this.terminalDistrictUpperID = terminalDistrictUpperID;
		assert this.terminalDistrictLowerID >= 1;
		assert this.terminalDistrictUpperID <= TPCCConfig.configDistPerWhse;
		assert this.terminalDistrictLowerID <= this.terminalDistrictUpperID;
		this.numWarehouses = numWarehouses;

		String hostname = getWorkloadConfiguration().getDBRestHost();
		String target = "http://" + hostname + ":8080/chronocache/rest/query/" + this.getId();
		this.wr = client.resource(target);
	}

	/**
	 * Executes a single TPCC transaction of type transactionType.
	 */
	@Override
    protected TransactionStatus executeWork(TransactionType nextTransaction) throws UserAbortException, SQLException {
            TPCCProcedure proc = (TPCCProcedure) this.getProcedure(nextTransaction.getProcedureClass());
            if (proc.run(conn, wr, gen, terminalWarehouseID, numWarehouses,
                    terminalDistrictLowerID, terminalDistrictUpperID, this)) {
				txnHits += 1;
			}

		txnTotal += 1;
        conn.commit();
        return (TransactionStatus.SUCCESS);
	}
}
