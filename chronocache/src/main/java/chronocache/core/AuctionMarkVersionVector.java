package chronocache.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AuctionMarkVersionVector extends VersionVector {

	public static final Map<String,Integer> relationIndexes = new HashMap<String,Integer>() {{
		 put("CATEGORY", 0);
		 put("CONFIG_PROFILE", 1);
		 put("GLOBAL_ATTRIBUTE_GROUP", 2);
		 put("GLOBAL_ATTRIBUTE_VALUE", 3);
		 put("ITEM", 4);
		 put("ITEM_ATTRIBUTE", 5);
		 put("ITEM_BID", 6);
		 put("ITEM_COMMENT", 7);
		 put("ITEM_IMAGE", 8);
		 put("ITEM_MAX_BID", 9);
		 put("ITEM_PURCHASE", 10);
		 put("REGION", 11);
		 put("USERACCT", 12);
		 put("USERACCT_ATTRIBUTES", 13);
		 put("USERACCT_FEEDBACK", 14);
		 put("USERACCT_ITEM", 15);
		 put("USERACCT_WATCH", 16);
	}};

	public static final String[] indexToRelation = new String[] {
		"CATEGORY",
		"CONFIG_PROFILE",
		"GLOBAL_ATTRIBUTE_GROUP",
		"GLOBAL_ATTRIBUTE_VALUE",
		"ITEM",
		"ITEM_ATTRIBUTE",
		"ITEM_BID",
		"ITEM_COMMENT",
		"ITEM_IMAGE",
		"ITEM_MAX_BID",
		"ITEM_PURCHASE",
		"REGION",
		"USERACCT",
		"USERACCT_ATTRIBUTES",
		"USERACCT_FEEDBACK",
		"USERACCT_ITEM",
		"USERACCT_WATCH"
	};

	public AuctionMarkVersionVector() {
		super( new ArrayList<Long>( Collections.nCopies( 17, 1L ) ) );
	}


}

