package chronocache.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import chronocache.core.parser.AntlrParser;
import chronocache.core.qry.Query;
import chronocache.core.qry.QueryIdentifier;
import chronocache.core.Vectorizable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

import org.junit.Test;

// Tests the Loop class
public class LoopTest
{
	@Test
	public void testSingleCreate() {
		// Boilerplate code
		AntlrParser p = new AntlrParser();
		String triggerString = "SELECT s_id FROM students";
		String bodyString = "SELECT grade FROM grades WHERE s_id = 5";
		Query triggerQuery = new Query( triggerString, p.buildParseTree( triggerString ) );
		Query bodyQuery = new Query( bodyString, p.buildParseTree( bodyString ) );

		Multimap<Integer,String> mappings = HashMultimap.create();
		mappings.put( 0, "student_id" );
		QueryMappingEntry mappingEntry = new QueryMappingEntry( triggerQuery.getId(), bodyQuery, mappings );

		DependencyGraph loopDependencyGraph = new DependencyGraph();
		loopDependencyGraph.addBaseQuery( triggerQuery.getId(), triggerQuery );
		loopDependencyGraph.addDependencyForQuery( bodyQuery.getId(), mappingEntry );
		assertThat( loopDependencyGraph.isVectorizable(), equalTo( true ) );

		// Create the loop
		VectorizableType vectorizableType = new VectorizableType();
		vectorizableType.markAsLoopBaseQuery();
		Vectorizable test = new Vectorizable( loopDependencyGraph, triggerQuery.getId(),
				triggerQuery.getQueryString(), vectorizableType );
		assertThat( test.isReady(), equalTo( false ) );
		assertThat( test.getVectorizationDependencies(), sameInstance( loopDependencyGraph ) );
		Collection<QueryIdentifier> queriesWeNeedTextFor = test.getAllQueryIdsWeNeedTextFor();
		assertThat( queriesWeNeedTextFor.size(), equalTo( 1 ) );
		assertThat( queriesWeNeedTextFor.iterator().next(), equalTo( triggerQuery.getId() ) );
		test.markDependencyAsExecuted( triggerQuery.getId(), triggerString );
		assertThat( test.isReady(), equalTo( true ) );

	}

	@Test
	public void testMultipleCreate()
	{
		AntlrParser p = new AntlrParser();
		String triggerQueryText = "SELECT s_id FROM students";
		String triggerQueryText2 = "SELECT s_id FROM teachers";
		String gradeQuery = "SELECT grade FROM grades WHERE s_id = 5";
		String addressQuery = "SELECT address FROM addresses WHERE s_id = 5";

		Multimap<Integer, String> mappings = HashMultimap.create();
		mappings.put( 0, "student_id" );

		// Boilerplate code
		Query triggerL1Query = new Query( triggerQueryText, p.buildParseTree( triggerQueryText ) );
		Query bodyL11Query = new Query( gradeQuery, p.buildParseTree( gradeQuery ) );
		Query bodyL12Query = new Query( addressQuery, p.buildParseTree( addressQuery ) );
		DependencyGraph loopDependencyGraph1 = new DependencyGraph();
		loopDependencyGraph1.addBaseQuery( triggerL1Query.getId(), triggerL1Query );
		QueryMappingEntry bodyL11MappingEntry = new QueryMappingEntry( triggerL1Query.getId(), bodyL11Query, mappings );
		loopDependencyGraph1.addDependencyForQuery( bodyL11Query.getId(), bodyL11MappingEntry );
		QueryMappingEntry bodyL12MappingEntry = new QueryMappingEntry( triggerL1Query.getId(), bodyL12Query, mappings );
		loopDependencyGraph1.addDependencyForQuery( bodyL12Query.getId(), bodyL12MappingEntry );

		assertThat( loopDependencyGraph1.isVectorizable(), equalTo( true ) );

		VectorizableType vTypeL1 = new VectorizableType();
		vTypeL1.markAsLoopBaseQuery();
		Vectorizable testL1 = new Vectorizable( loopDependencyGraph1,
				triggerL1Query.getId(), triggerL1Query.getQueryString(),
				vTypeL1 );
		assertThat( testL1.isReady(), equalTo( false ) );
		Collection<QueryIdentifier> queryIdsWeNeedTextFor = testL1.getAllQueryIdsWeNeedTextFor();
		assertThat( queryIdsWeNeedTextFor.size(), equalTo( 1 ) );
		assertThat( queryIdsWeNeedTextFor.iterator().next(), equalTo( triggerL1Query.getId() ) );
		testL1.markDependencyAsExecuted( triggerL1Query.getId(), triggerL1Query.getQueryString() );
		assertThat( testL1.isReady(), equalTo( true ) );

		Query triggerL2Query = new Query( triggerQueryText2, p.buildParseTree( triggerQueryText2 ) );
		Query bodyL21Query = new Query( gradeQuery, p.buildParseTree( gradeQuery ) );
		Query bodyL22Query = new Query( addressQuery, p.buildParseTree( addressQuery ) );
		DependencyGraph loopDependencyGraph2 = new DependencyGraph();
		loopDependencyGraph2.addBaseQuery( triggerL2Query.getId(), triggerL2Query );
		QueryMappingEntry bodyL21MappingEntry = new QueryMappingEntry( triggerL2Query.getId(), bodyL21Query, mappings );
		loopDependencyGraph1.addDependencyForQuery( bodyL21Query.getId(), bodyL21MappingEntry );
		QueryMappingEntry bodyL22MappingEntry = new QueryMappingEntry( triggerL2Query.getId(), bodyL22Query, mappings );
		loopDependencyGraph1.addDependencyForQuery( bodyL22Query.getId(), bodyL22MappingEntry );

		VectorizableType vTypeL2 = new VectorizableType();
		vTypeL2.markAsLoopBaseQuery();
		Vectorizable testL2 = new Vectorizable( loopDependencyGraph2,
				triggerL2Query.getId(), triggerL2Query.getQueryString(),
				vTypeL2 );
		assertThat( testL2.isReady(), equalTo( false ) );
		queryIdsWeNeedTextFor = testL2.getAllQueryIdsWeNeedTextFor();
		assertThat( queryIdsWeNeedTextFor.size(), equalTo( 1 ) );
		assertThat( queryIdsWeNeedTextFor.iterator().next(), equalTo( triggerL2Query.getId() ) );
		testL2.markDependencyAsExecuted( triggerL2Query.getId(), triggerL2Query.getQueryString() );
		assertThat( testL2.isReady(), equalTo( true ) );
	}
}
