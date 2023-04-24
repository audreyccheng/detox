package chronocache.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import chronocache.core.qry.QueryIdentifier;

public class MarkovNodeTest {

	@Test
	public void testConstructor(){
		QueryIdentifier id = new QueryIdentifier(0);
		MarkovNode node = new MarkovNode(id);
		assertThat( node.getId(), equalTo(id) );
		assertThat( node.getHitCount(), equalTo(0l) );
	}
	
	@Test
	public void testSimplePrediction(){
		QueryIdentifier id = new QueryIdentifier(0);
		QueryIdentifier id2 = new QueryIdentifier(1);
		MarkovNode node = new MarkovNode(id);
		node.increaseHitCounter();
		assertThat(node.getHitCount(), equalTo(1l));
		MarkovNode node2 = new MarkovNode(id2);
		node.addEdgeTraversal(node2);
		assertThat( node.computeEdgeTraversalProbability(node2), equalTo(1.0));
		assertThat( node.computeEdgeTraversalProbability(node), equalTo(0.0));
	}
	
	@Test
	public void testMultipleHits(){
		QueryIdentifier id = new QueryIdentifier(0);
		QueryIdentifier id2 = new QueryIdentifier(1);
		MarkovNode node = new MarkovNode(id);
		node.increaseHitCounter();
		assertThat(node.getHitCount(), equalTo(1l));
		MarkovNode node2 = new MarkovNode(id2);
		node.addEdgeTraversal(node2);
		assertThat( node.computeEdgeTraversalProbability(node2), equalTo(1.0));
		node.increaseHitCounter();
		assertThat( node.computeEdgeTraversalProbability(node2), equalTo(0.5));
	}

}
