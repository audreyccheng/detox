package chronocache.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import chronocache.core.qry.QueryIdentifier;
import chronocache.core.qry.QueryStream;
import chronocache.core.qry.ExecutedQuery;

import org.joda.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MarkovMultiModel {
	private static final Logger logger = LoggerFactory.getLogger(MarkovMultiModel.class);

	private class ModelDurationEntry {
		private MarkovConstructor model;
		private Duration endPoint;
		public ModelDurationEntry( MarkovConstructor model, Duration endPoint ) {
			this.model = model;
			this.endPoint = endPoint;
		}
		public MarkovConstructor getModel() {
			return model;
		}

		public Duration getEndPoint() {
			return endPoint;
		}
	}

	protected MarkovConstructor invalidateModel;
	protected MarkovConstructor predictModel;
	protected LinkedList<QueryStream> queryStreams;
	protected LinkedList<ModelDurationEntry> models;
	protected long clientId;

	public MarkovMultiModel( long clientId, int minWidth, double widthFactor, int maxExp ) {
		this.clientId = clientId;
		models = new LinkedList<>();
		queryStreams = new LinkedList<>();

		assert( maxExp >= 1 );
		for( int i = 0; i < maxExp; i++ ) {
			Duration width = new Duration( 300 );
			//Duration width = new Duration( (minWidth*1000) + (int) (Math.pow( widthFactor, i )*1000) );
			logger.info( "creating model of width {}", width);
			QueryStream qStream = new QueryStream();
			MarkovConstructor model = new MarkovConstructor( clientId, qStream, width );
			Thread t1 = new Thread( model );
			queryStreams.add( qStream );
			ModelDurationEntry entry = new ModelDurationEntry( model, width );
			models.add( entry );
			t1.start();
			logger.debug( "model of width {} started...", width);
		}
	}

	public MarkovGraph getContainingGraph( int expectedQueryRunTimeMs ) {
		//We could binary search this, but we don't because we assume that maxExp is fairly small
		Duration runTimeDur = new Duration( expectedQueryRunTimeMs );
		for( ModelDurationEntry mde : models ) {
			if( runTimeDur.isShorterThan( mde.getEndPoint() ) ) {
				return mde.getModel().getGraph();
			}
		}
		return null;
	}

	public MarkovGraph getPredictGraph() {
		return models.getLast().getModel().getGraph();
	}

	public void pushQueryToAllModels( ExecutedQuery q, String queryString ) {
		for( ModelDurationEntry e : models ) {
			MarkovGraph g = e.getModel().getGraph();
			addToMarkovGraph( g, q, queryString );
		}
	}

	private void addToMarkovGraph( MarkovGraph g, ExecutedQuery q, String queryString ) {
		g.getOrAddNode( q.getId() );
		g.addQueryString( q.getId(), queryString );
	}

	public LinkedList<QueryStream> getQueryStreams() {
		return queryStreams;
	}

	public void stop() {
		logger.trace("Model stop called.");
		for( ModelDurationEntry e : models ) {
			e.getModel().stop();
		}
	}

	/* For unit tests */
	public void swapConstructor( MarkovConstructor c ) {
		//Stop existing constructors
		stop();
		models.clear();
		//Set model
		models.add( new ModelDurationEntry( c, c.getDelta() ) );
	}
}
