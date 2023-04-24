package chronocache.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SEATSVersionVector extends VersionVector {

	public static final Map<String,Integer> relationIndexes = new HashMap<String,Integer>() {{
		put("AIRLINE", 0 );
		put("AIRPORT", 1 );
		put("AIRPORT_DISTANCE", 2 );
		put("CONFIG_HISTOGRAMS", 3 );
		put("CONFIG_PROFILE", 4 );
		put("COUNTRY", 5 );
		put("CUSTOMER", 6 );
		put("FLIGHT", 7 );
		put("FREQUENT_FLYER", 8 );
		put("RESERVATION", 9 );
	}};

	public static final String[] indexToRelation = new String[] {
		"AIRLINE",
		"AIRPORT",
		"AIRPORT_DISTANCE",
		"CONFIG_HISTOGRAMS",
		"CONFIG_PROFILE",
		"COUNTRY",
		"CUSTOMER",
		"FLIGHT",
		"FREQUENT_FLYER",
		"RESERVATION"

	};

	public SEATSVersionVector() {
		super( new ArrayList<Long>( Collections.nCopies( 10, 1L ) ) );
	}


}

