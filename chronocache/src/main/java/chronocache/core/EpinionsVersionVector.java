package chronocache.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EpinionsVersionVector extends VersionVector {

	public static final Map<String,Integer> relationIndexes = new HashMap<String,Integer>() {{
		put("useracct", 0 );
		put("item", 1 );
		put("review", 2 );
		put("review_rating", 3 );
		put("trust", 4 );
	}};

	public static final String[] indexToRelation = new String[] {
		"useracct",
		"item",
		"review",
		"review_rating",
		"trust",
	};

	public EpinionsVersionVector() {
		super( new ArrayList<Long>( Collections.nCopies( 5, 1L ) ) );
	}


}

