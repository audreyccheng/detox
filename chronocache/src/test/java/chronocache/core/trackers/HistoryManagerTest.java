package chronocache.core.trackers;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import chronocache.core.qry.QueryIdentifier;
import chronocache.core.Parameters;
public class HistoryManagerTest {

	/**
	 * Test that the information in available in the history manager
	 * is only available after the training period
	 */
	@Test
	public void testAddHistoryTrainingPeriod(){
		Parameters.TRACKING_PERIOD = 3;
		HistoryManager manager = new HistoryManager();
		QueryIdentifier id = new QueryIdentifier(1);
		QueryIdentifier other = new QueryIdentifier(2);
		Map<QueryIdentifier, Multimap<Integer,String>> queryPairings = new HashMap<>();
		Multimap<Integer,String> pairings = ArrayListMultimap.create();
		pairings.put(1, "1");
		queryPairings.put(other, pairings);
		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), nullValue());
		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), nullValue());
		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), not(nullValue()));
		queryPairings =  manager.getMappingsIfReady(id);
		assertThat(queryPairings.size(), equalTo(1));
		assertThat(queryPairings.get(other).size(), equalTo(1));
	}

	/**
	 * Test that only values which are the same are preserved
	 */
	@Test
	public void testAddHistoryMapPairingIntersect(){
		Parameters.TRACKING_PERIOD = 3;
		HistoryManager manager = new HistoryManager();
		QueryIdentifier id = new QueryIdentifier(1);
		QueryIdentifier other = new QueryIdentifier(2);
		Map<QueryIdentifier, Multimap<Integer,String>> queryPairings = new HashMap<>();
		Multimap<Integer,String> pairings = ArrayListMultimap.create();
		pairings.put(1, "1");
		pairings.put(2, "2");
		pairings.put(3, "3");
		queryPairings.put(other, pairings);
		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), nullValue());
		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), nullValue());
		pairings.remove(2, "2");
		queryPairings.put(other, pairings);
		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), not(nullValue()));
		queryPairings =  manager.getMappingsIfReady(id);
		assertThat(queryPairings.size(), equalTo(1));
		assertThat(queryPairings.get(other).size(), equalTo(2));
	}


	/**
	 * Test that only queryidentifiers which are the same remain
	 */
	@Test
	public void testAddHistoryPairingIntersect(){
		Parameters.TRACKING_PERIOD = 3;
		HistoryManager manager = new HistoryManager();
		QueryIdentifier id = new QueryIdentifier(1);
		QueryIdentifier other = new QueryIdentifier(2);
		QueryIdentifier other2 = new QueryIdentifier(3);
		Map<QueryIdentifier, Multimap<Integer,String>> queryPairings = new HashMap<>();
		Multimap<Integer,String> pairings = ArrayListMultimap.create();
		pairings.put(1, "1");
		pairings.put(2, "2");
		pairings.put(3, "3");
		queryPairings.put(other, pairings);
		queryPairings.put(other2, pairings);
		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), nullValue());
		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), nullValue());
		queryPairings.remove(other);
		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), not(nullValue()));
		queryPairings =  manager.getMappingsIfReady(id);
		assertThat(queryPairings.size(), equalTo(1));
		assertThat(queryPairings.keySet().size(), equalTo(1));
		assertThat(queryPairings.get(other2).size(), equalTo(3));
	}

	/**
	 * Test that values(collections) of the Multimap intersect correctly.
	 */
	@Test
	public void testAddHistorymultimapIntersect() {
		Parameters.TRACKING_PERIOD = 3;
		HistoryManager manager = new HistoryManager();
		QueryIdentifier id = new QueryIdentifier(1);
		QueryIdentifier other = new QueryIdentifier(2);

		Map<QueryIdentifier, Multimap<Integer, String>> queryPairings = new HashMap<>();
		Multimap<Integer, String> pairings = ArrayListMultimap.create();
		pairings.put(1, "1");
		pairings.put(1, "2");
		pairings.put(1, "3");
		pairings.put(2, "4");
		pairings.put(3, "5");
		queryPairings.put(other, pairings);

		Map<QueryIdentifier, Multimap<Integer, String>> queryPairings2 = new HashMap<>();
		Multimap<Integer, String> pairings2 = ArrayListMultimap.create();
		pairings2.put(1, "1");
		pairings2.put(1, "4");
		pairings2.put(2, "1");
		pairings2.put(2, "4");
		queryPairings2.put(other, pairings2);

		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), nullValue());
		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), nullValue());
		manager.addToHistory(id, queryPairings);
		assertThat(manager.getMappingsIfReady(id), not(nullValue()));

		manager.addToHistory(id, queryPairings2);
		assertThat(manager.getMappingsIfReady(id), not(nullValue()));

		queryPairings2 = manager.getMappingsIfReady(id);
		assertThat(queryPairings.size(), equalTo(1));
		assertThat(queryPairings.get(other).size(), equalTo(2));
		assertThat(queryPairings.get(other).get(1).size(), equalTo(1));
		assertThat(queryPairings.get(other).get(2).size(), equalTo(1));
	}

}
