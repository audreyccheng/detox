package chronocache.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TaobenchVersionVector extends VersionVector {
    public static final Map<String,Integer> relationIndexes = new HashMap<String,Integer>() {{
        put("objects", 0 );
    }};

    public static final String[] indexToRelation = new String[] {
        "objects",
    };

    public TaobenchVersionVector() {
        super( new ArrayList<Long>( Collections.nCopies( 1, 1L ) ) );
    }

}
