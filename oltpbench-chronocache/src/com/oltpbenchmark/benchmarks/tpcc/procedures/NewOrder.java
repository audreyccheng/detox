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
import com.oltpbenchmark.benchmarks.tpcc.TPCCConfig;

public class NewOrder extends TPCCProcedure {

    private static final Logger LOG = Logger.getLogger(NewOrder.class);


	// NewOrder Txn

    public boolean run(Connection conn, WebResource wr, Random gen,
			int terminalWarehouseID, int numWarehouses,
			int terminalDistrictLowerID, int terminalDistrictUpperID,
			TPCCWorker w) throws SQLException {
		boolean txnHit = true;

		int districtID = TPCCUtil.randomNumber(terminalDistrictLowerID,terminalDistrictUpperID, gen);
		int customerID = TPCCUtil.getCustomerID(gen);

		int numItems = (int) TPCCUtil.randomNumber(5, 15, gen);
		int[] itemIDs = new int[numItems];
		int[] supplierWarehouseIDs = new int[numItems];
		int[] orderQuantities = new int[numItems];
		int allLocal = 1;
		for (int i = 0; i < numItems; i++) {
			itemIDs[i] = TPCCUtil.getItemID(gen);
			if (TPCCUtil.randomNumber(1, 100, gen) > 1) {
				supplierWarehouseIDs[i] = terminalWarehouseID;
			} else {
				do {
					supplierWarehouseIDs[i] = TPCCUtil.randomNumber(1,
							numWarehouses, gen);
				} while (supplierWarehouseIDs[i] == terminalWarehouseID
						&& numWarehouses > 1);
				allLocal = 0;
			}
			orderQuantities[i] = TPCCUtil.randomNumber(1, 10, gen);
		}

		// we need to cause 1% of the new orders to be rolled back.
		if (TPCCUtil.randomNumber(1, 100, gen) == 1)
			itemIDs[numItems - 1] = TPCCConfig.INVALID_ITEM_ID;


		txnHit &= newOrderTransaction(wr, terminalWarehouseID, districtID,
						customerID, numItems, allLocal, itemIDs,
						supplierWarehouseIDs, orderQuantities, conn, w);
		return txnHit;

    }




	private boolean newOrderTransaction(WebResource wr, int w_id, int d_id, int c_id,
			int o_ol_cnt, int o_all_local, int[] itemIDs,
			int[] supplierWarehouseIDs, int[] orderQuantities, Connection conn, TPCCWorker w)
			throws SQLException {
		boolean txnHit = true;
		float c_discount, w_tax, d_tax = 0, i_price;
		int d_next_o_id, o_id = -1, s_quantity;
		String c_last = null, c_credit = null, i_name, i_data, s_data;
		String s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05;
		String s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, ol_dist_info = null;
		float[] itemPrices = new float[o_ol_cnt];
		float[] orderLineAmounts = new float[o_ol_cnt];
		String[] itemNames = new String[o_ol_cnt];
		int[] stockQuantities = new int[o_ol_cnt];
		char[] brandGeneric = new char[o_ol_cnt];
		int ol_supply_w_id, ol_i_id, ol_quantity;
		int s_remote_cnt_increment;
		float ol_amount, total_amount = 0;
		
		try {
			String stmtGetCust = "SELECT C_DISCOUNT, C_LAST, C_CREDIT" +
					"  FROM " + TPCCConstants.TABLENAME_CUSTOMER +
					" WHERE C_W_ID = " + w_id +
					"   AND C_D_ID = " + d_id +
					"   AND C_ID = " + c_id;
			List<Map<String, Object>> rs = RestQuery.restReadQuery(wr, stmtGetCust, w.getId());
			if (rs.isEmpty())
				throw new RuntimeException("C_D_ID=" + d_id
						+ " C_ID=" + c_id + " not found!");
			if (rs.get(0).get("CACHE_HIT") == null) {
				txnHit = false;
			}
			c_discount = (float) Double.parseDouble(rs.get(0).get("c_discount").toString());
			c_last = rs.get(0).get("c_last").toString();
			c_credit = rs.get(0).get("c_credit").toString();
			rs = null;

			String stmtGetWhse = "SELECT W_TAX " +
					"  FROM " + TPCCConstants.TABLENAME_WAREHOUSE +
					" WHERE W_ID = " + w_id;
			rs = RestQuery.restReadQuery(wr, stmtGetWhse, w.getId());
			if (rs.isEmpty())
				throw new RuntimeException("W_ID=" + w_id + " not found!");
			if (rs.get(0).get("CACHE_HIT") == null) {
				txnHit = false;
			}
			w_tax = (float) Double.parseDouble(rs.get(0).get("w_tax").toString());
			rs = null;

			String stmtGetDist = "SELECT D_NEXT_O_ID, D_TAX " +
					"  FROM " + TPCCConstants.TABLENAME_DISTRICT +
					" WHERE D_W_ID = " + w_id + " AND D_ID = " + d_id + " FOR UPDATE";
			rs = RestQuery.restReadQuery(wr, stmtGetDist, w.getId());
			if (rs.isEmpty()) {
				throw new RuntimeException("D_ID=" + d_id + " D_W_ID=" + w_id
						+ " not found!");
			}
			if (rs.get(0).get("CACHE_HIT") == null) {
				txnHit = false;
			}
			d_next_o_id = (int) Double.parseDouble(rs.get(0).get("d_next_o_id").toString());
			d_tax = (float) Double.parseDouble(rs.get(0).get("d_tax").toString());
			rs = null;

			//woonhak, need to change order because of foreign key constraints
			//update next_order_id first, but it might doesn't matter
			String stmtUpdateDist = "UPDATE " + TPCCConstants.TABLENAME_DISTRICT +
					"   SET D_NEXT_O_ID = D_NEXT_O_ID + 1 " +
					" WHERE D_W_ID = " + w_id +
					"   AND D_ID = " + d_id;
			int result = RestQuery.restOtherQuery(wr, stmtUpdateDist, w.getId());
			if (result == 0)
				throw new RuntimeException(
						"Error!! Cannot update next_order_id on district for D_ID="
								+ d_id + " D_W_ID=" + w_id);

			o_id = d_next_o_id;

			// woonhak, need to change order, because of foreign key constraints
			//[[insert ooder first
			String stmtInsertOOrder = "INSERT INTO " + TPCCConstants.TABLENAME_OPENORDER +
					" (O_ID, O_D_ID, O_W_ID, O_C_ID, O_ENTRY_D, O_OL_CNT, O_ALL_LOCAL)" +
					" VALUES (" + o_id + ", " + d_id + ", " + w_id + ", " + c_id + ", '" +
					w.getBenchmarkModule().getTimestamp(System.currentTimeMillis()) + "', " +
					o_ol_cnt + ", " + o_all_local + ")";
			RestQuery.restOtherQuery(wr, stmtInsertOOrder, w.getId());
			//insert ooder first]]
			/*TODO: add error checking */

			String stmtInsertNewOrder = "INSERT INTO " + TPCCConstants.TABLENAME_NEWORDER +
					" (NO_O_ID, NO_D_ID, NO_W_ID) " +
					" VALUES ( " + o_id + ", " + d_id + ", " + w_id + ")";
			RestQuery.restOtherQuery(wr, stmtInsertNewOrder, w.getId());
			/*TODO: add error checking */


			/* woonhak, [[change order				 
			stmtInsertOOrder.setInt(1, o_id);
			stmtInsertOOrder.setInt(2, d_id);
			stmtInsertOOrder.setInt(3, w_id);
			stmtInsertOOrder.setInt(4, c_id);
			stmtInsertOOrder.setTimestamp(5,
					new Timestamp(System.currentTimeMillis()));
			stmtInsertOOrder.setInt(6, o_ol_cnt);
			stmtInsertOOrder.setInt(7, o_all_local);
			stmtInsertOOrder.executeUpdate();
			change order]]*/

			for (int ol_number = 1; ol_number <= o_ol_cnt; ol_number++) {
				ol_supply_w_id = supplierWarehouseIDs[ol_number - 1];
				ol_i_id = itemIDs[ol_number - 1];
				ol_quantity = orderQuantities[ol_number - 1];
				String stmtGetItem = "SELECT I_PRICE, I_NAME , I_DATA " +
						"  FROM " + TPCCConstants.TABLENAME_ITEM +
						" WHERE I_ID = " + ol_i_id;
				rs = RestQuery.restReadQuery(wr, stmtGetItem, w.getId());
				if (rs.isEmpty()) {
					// This is (hopefully) an expected error: this is an
					// expected new order rollback
					assert ol_number == o_ol_cnt;
					assert ol_i_id == TPCCConfig.INVALID_ITEM_ID;
					throw new UserAbortException(
							"EXPECTED new order rollback: I_ID=" + ol_i_id
									+ " not found!");
				}
				if (rs.get(0).get("CACHE_HIT") == null) {
					txnHit = false;
				}

				i_price = (float) Double.parseDouble(rs.get(0).get("i_price").toString());
				i_name = rs.get(0).get("i_name").toString();
				i_data = rs.get(0).get("i_data").toString();
				rs = null;

				itemPrices[ol_number - 1] = i_price;
				itemNames[ol_number - 1] = i_name;

				String stmtGetStock = "SELECT S_QUANTITY, S_DATA, S_DIST_01, S_DIST_02, S_DIST_03, S_DIST_04, S_DIST_05, " +
						"       S_DIST_06, S_DIST_07, S_DIST_08, S_DIST_09, S_DIST_10" +
						"  FROM " + TPCCConstants.TABLENAME_STOCK +
						" WHERE S_I_ID = " + ol_i_id +
						"   AND S_W_ID = " + ol_supply_w_id + " FOR UPDATE";
				rs = RestQuery.restReadQuery(wr, stmtGetStock, w.getId());
				if (rs.isEmpty())
					throw new RuntimeException("I_ID=" + ol_i_id
							+ " not found!");
				if (rs.get(0).get("CACHE_HIT") == null) {
					txnHit = false;
				}
				s_quantity = (int) Double.parseDouble(rs.get(0).get("s_quantity").toString());
				s_data = rs.get(0).get("s_data").toString();
				s_dist_01 = rs.get(0).get("s_dist_01").toString();
				s_dist_02 = rs.get(0).get("s_dist_02").toString();
				s_dist_03 = rs.get(0).get("s_dist_03").toString();
				s_dist_04 = rs.get(0).get("s_dist_04").toString();
				s_dist_05 = rs.get(0).get("s_dist_05").toString();
				s_dist_06 = rs.get(0).get("s_dist_06").toString();
				s_dist_07 = rs.get(0).get("s_dist_07").toString();
				s_dist_08 = rs.get(0).get("s_dist_08").toString();
				s_dist_09 = rs.get(0).get("s_dist_09").toString();
				s_dist_10 = rs.get(0).get("s_dist_10").toString();
				rs = null;

				stockQuantities[ol_number - 1] = s_quantity;

				if (s_quantity - ol_quantity >= 10) {
					s_quantity -= ol_quantity;
				} else {
					s_quantity += -ol_quantity + 91;
				}

				if (ol_supply_w_id == w_id) {
					s_remote_cnt_increment = 0;
				} else {
					s_remote_cnt_increment = 1;
				}



				String stmtUpdateStock = "UPDATE " + TPCCConstants.TABLENAME_STOCK +
						"   SET S_QUANTITY = " + s_quantity + " , " +
						"       S_YTD = S_YTD + " + ol_quantity + " , " +
						"       S_ORDER_CNT = S_ORDER_CNT + 1, " +
						"       S_REMOTE_CNT = S_REMOTE_CNT + " + s_remote_cnt_increment +
						" WHERE S_I_ID = " + ol_i_id +
						"   AND S_W_ID = " + ol_supply_w_id;
				RestQuery.restOtherQuery(wr, stmtUpdateStock, w.getId());


				ol_amount = ol_quantity * i_price;
				orderLineAmounts[ol_number - 1] = ol_amount;
				total_amount += ol_amount;

				if (i_data.indexOf("ORIGINAL") != -1
						&& s_data.indexOf("ORIGINAL") != -1) {
					brandGeneric[ol_number - 1] = 'B';
				} else {
					brandGeneric[ol_number - 1] = 'G';
				}

				switch ((int) d_id) {
				case 1:
					ol_dist_info = s_dist_01;
					break;
				case 2:
					ol_dist_info = s_dist_02;
					break;
				case 3:
					ol_dist_info = s_dist_03;
					break;
				case 4:
					ol_dist_info = s_dist_04;
					break;
				case 5:
					ol_dist_info = s_dist_05;
					break;
				case 6:
					ol_dist_info = s_dist_06;
					break;
				case 7:
					ol_dist_info = s_dist_07;
					break;
				case 8:
					ol_dist_info = s_dist_08;
					break;
				case 9:
					ol_dist_info = s_dist_09;
					break;
				case 10:
					ol_dist_info = s_dist_10;
					break;
				}

				String stmtInsertOrderLine = "INSERT INTO " + TPCCConstants.TABLENAME_ORDERLINE +
						" (OL_O_ID, OL_D_ID, OL_W_ID, OL_NUMBER, OL_I_ID, OL_SUPPLY_W_ID, OL_QUANTITY, OL_AMOUNT, OL_DIST_INFO) " +
						" VALUES (" + o_id + ", " + d_id + "," + w_id + ", " + ol_number + ", " + ol_i_id + ", " +
						ol_supply_w_id + ", " + ol_quantity + ", " + ol_amount + ", '" + ol_dist_info + "')";
				RestQuery.restOtherQuery(wr, stmtInsertOrderLine, w.getId());

			} // end-for

			total_amount *= (1 + w_tax + d_tax) * (1 - c_discount);
		} catch(UserAbortException userEx)
		{
		    LOG.debug("Caught an expected error in New Order");
		    throw userEx;
		}

		return txnHit;
	}

}
