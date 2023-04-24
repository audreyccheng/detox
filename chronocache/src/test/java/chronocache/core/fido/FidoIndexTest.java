package chronocache.core.fido;

import static org.junit.Assert.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Collection;

import org.junit.Test;

import chronocache.core.fido.FidoIndex;

public class FidoIndexTest {

	@Test
	public void testFind() {
        FidoIndex index = new FidoIndex();

        assertTrue(index.findElement("a").isEmpty());
        assertTrue(index.addIndexElement("a", 1));

        Collection<Integer> found = index.findElement("a");

        assertEquals(1, found.size());
        assertTrue(found.contains(1));

        assertTrue(index.addIndexElement("a", 2));

        found = index.findElement("a");

        assertEquals(2, found.size());
        assertTrue(found.contains(1));
        assertTrue(found.contains(2));

    }
}
