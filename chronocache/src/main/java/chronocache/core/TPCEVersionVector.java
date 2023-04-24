package chronocache.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TPCEVersionVector extends VersionVector {

	public static final Map<String,Integer> relationIndexes = new HashMap<String,Integer>() {{
		put("account_permission", 0);
		put("address", 1);
		put("broker", 2);
		put("cash_transaction", 3);
		put("charge", 4);
		put("commission_rate", 5);
		put("company", 6);
		put("company_competitor", 7);
		put("customer", 8);
		put("customer_account", 9);
		put("customer_taxrate", 10);
		put("daily_market", 11);
		put("exchange", 12);
		put("financial", 13);
		put("holding", 14);
		put("holding_history", 15);
		put("holding_summary", 16);
		put("industry", 17);
		put("last_trade", 18);
		put("news_item", 19);
		put("news_xref", 20);
		put("sector", 21);
		put("security", 22);
		put("settlement", 23);
		put("status_type", 24);
		put("taxrate", 25);
		put("trade", 26);
		put("trade_history", 27);
		put("trade_request", 28);
		put("trade_type", 29);
		put("watch_item", 30);
		put("watch_list", 31);
		put("zip_code", 32);
	}};

	public static final String[] indexToRelation = new String[] {
		"account_permission",
		"address",
		"broker",
		"cash_transaction",
		"charge",
		"commission_rate",
		"company",
		"company_competitor",
		"customer",
		"customer_account",
		"customer_taxrate",
		"daily_market",
		"exchange",
		"financial",
		"holding",
		"holding_history",
		"holding_summary",
		"industry",
		"last_trade",
		"news_item",
		"news_xref",
		"sector",
		"security",
		"settlement",
		"status_type",
		"taxrate",
		"trade",
		"trade_history",
		"trade_request",
		"trade_type",
		"watch_item",
		"watch_list",
		"zip_code"
	};

	public TPCEVersionVector() {
		//TODO: fill out correct # here
		super( new ArrayList<Long>( Collections.nCopies( 33, 1L ) ) );
	}
}
