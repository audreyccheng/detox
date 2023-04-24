package chronocache.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TPCWVersionVector extends VersionVector {

	public static final Map<String,Integer> relationIndexes = new HashMap<String,Integer>() {{
		put( "address", 0 );
		put( "author", 1 );
		put( "cc_xacts", 2 );
		put( "country", 3 );
		put( "customer", 4 );
		put( "item", 5 );
		put( "order_line", 6 );
		put( "orders", 7 );
		put( "shopping_cart", 8 );
		put( "shopping_cart_line", 9 );
	}};

	public static final String indexToRelation[] = new String[] {
		"address",
		"author",
		"cc_xacts",
		"country",
		"customer",
		"item",
		"order_line",
		"orders",
		"shopping_cart",
		"shopping_cart_line",
	};


	public TPCWVersionVector() {
		super( new ArrayList<Long>( Collections.nCopies( 10, 1L ) ) );
	}
}
