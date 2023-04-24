package chronocache.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SmallBankVersionVector extends VersionVector {
    public static final Map<String,Integer> relationIndexes = new HashMap<String,Integer>() {{
        put("ACCOUNTS", 0 );
        put("CHECKING", 1 );
        put("SAVINGS", 2 );
    }};

    public static final String[] indexToRelation = new String[] {
        "ACCOUNTS",
        "CHECKING",
        "SAVINGS",
    };

    public SmallBankVersionVector() {
        super( new ArrayList<Long>( Collections.nCopies( 10, 1L ) ) );
    }


}
