package chronocache.core.fido;

import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultiset;

public class FidoIndex {

    private Multimap<String, Integer> elements;

    public FidoIndex() {
        elements = ArrayListMultimap.create();
    }

    public boolean addIndexElement(String key, Integer value) {
        return elements.put(key, value);
    }

    public Collection<Integer> findElement(String key) {
        Collection<Integer> found = elements.get(key);
        if(found == null) {
            found = Collections.emptyList();
        }
        return found;
    }

}
