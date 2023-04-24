package chronocache.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TPCCVersionVector extends VersionVector {

	public static final Map<String,Integer> relationIndexes = new HashMap<String,Integer>() {{
		put( "customer", 0 );
		put( "district", 1 );
		put( "history", 2 );
		put( "item", 3 );
		put( "new_order", 4 );
		put( "orders", 5 );
		put( "order_line", 6 );
		put( "stock", 7 );
		put( "warehouse", 8 );
	}};

	public static final String[] indexToRelation = new String[] {
		"customer",
		"district",
		"history",
		"item",
		"new_order",
		"orders",
		"order_line",
		"stock",
		"warehouse"
	};

	public TPCCVersionVector() {
		//TODO: fill out correct # here
		super( new ArrayList<Long>( Collections.nCopies( 9, 1L ) ) );
	}
}
