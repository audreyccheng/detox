package chronocache.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WIKIVersionVector extends VersionVector {
	
	public static final Map<String,Integer> relationIndexes = new HashMap<String,Integer>() {{
		put("ipblocks", 0);
		put("logging", 1);
		put("page", 2);
		put("page_backup", 3);
		put("page_restrictions", 4);
		put("recentchanges", 5);
		put("revision", 6);
		put("text", 7);
		put("user_groups", 8);
		put("useracct", 9);
		put("value_backup", 10);
		put("watchlist", 11);
	}};

	public static final String[] indexToRelation = new String[] {
		"ipblocks",
		"logging",
		"page",
		"page_backup",
		"page_restrictions",
		"recentchanges",
		"revision",
		"text",
		"user_groups",
		"useracct",
		"value_backup",
		"watchlist"
	};

	public WIKIVersionVector() {
		//TODO: fill out correct # here
		super( new ArrayList<Long>( Collections.nCopies( 12, 1L ) ) );
	}
}
