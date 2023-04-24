package chronocache.core.fido;

import java.io.File;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import com.google.common.collect.Multiset;

import chronocache.core.Parameters;
import chronocache.core.fido.FidoIndex;
import chronocache.core.fido.FidoModel;
import chronocache.core.fido.UnitPattern;
import chronocache.core.parser.ParserPool;
import chronocache.core.parser.AntlrParser;
import chronocache.core.parser.AntlrQueryMetadata;
import chronocache.core.parser.AntlrQueryType;
import chronocache.core.qry.Query;

public class Trainer {
	private Logger logger = LoggerFactory.getLogger(Trainer.class);

	private Map<String, String> queryMappings;
	private List<String> queryCacheKeys;
	private List<UnitPattern> unitPatterns;
	private List<FidoIndex> indices;

	public Trainer() {
		queryMappings = new HashMap<>();
		queryCacheKeys = new ArrayList<>();
		unitPatterns = new ArrayList<>();
		indices = new ArrayList<>();
	}

	public FidoModel train() {
		AntlrParser parser = new AntlrParser();
		loadDataSet( parser, loadFile( Parameters.FIDO_TRAIN_FILE ) );
		buildTrainingModel( Parameters.FIDO_PREFIX_LEN,
				Parameters.FIDO_SUFFIX_LEN, Parameters.FIDO_OVERLAP );

		return new FidoModel( Parameters.FIDO_PREFIX_LEN, indices,
				unitPatterns, queryMappings );
	}

	private List<String> loadFile( String fileName ) {
		List<String> lines = new ArrayList<>();

		try {
			Path path = Paths.get( fileName );
			lines = Files.readAllLines( path, Charset.forName( "utf-8" ) );
		} catch( Exception e ) {
			logger.error( "Could not load training data {}, Exception {}", fileName, e );
		}
		return lines;
	}

	private void loadDataSet( AntlrParser parser, List<String> rawQueries ) {
		for( String rawQuery : rawQueries ) {
			String queryString = rawQuery.trim();
			Query query = getQueryFromString( parser, queryString );
			if( query != null ) {
				queryMappings.put( query.getCacheKey(), queryString );
				queryCacheKeys.add( query.getCacheKey() );
				logger.info( "CacheKey {} maps to {}", query.getCacheKey(), queryString );
			}
		}
	}

	private void buildTrainingModel( int prefixLength, int suffixLength, int overlap ) {
		constructInitialModel( prefixLength, suffixLength, overlap );
		predictionPass( prefixLength, suffixLength );
	}

	private void predictionPass( int prefixLength, int suffixLength ) {
		LinkedList<String> prefix = new LinkedList<>();
		LinkedList<String> suffix = new LinkedList<>();

		for( String query : queryCacheKeys ) {
			if( prefix.size() == prefixLength && suffix.size() == suffixLength ) {
				testPrediction( prefix, suffix );

				prefix.removeFirst();
				prefix.offerLast( suffix.removeFirst() );
				suffix.offerLast( query );
			} else if( prefix.size() == prefixLength ) {
				suffix.offerLast( query );
			} else {
				prefix.offerLast( query );
			}
		}
	}

	private void testPrediction( LinkedList<String> prefix, LinkedList<String> suffix ) {
		List<Integer> orderedNeighbours = FidoModel.getOrderedNeighbours( prefix, indices );

		Set<String> uniquePredictions = new HashSet<>();

		for( Integer neighbourPosition : orderedNeighbours ) {
			UnitPattern neighbourPattern = unitPatterns.get( neighbourPosition );

			for( String prediction : neighbourPattern.suffix ) {
				if( suffix.contains( prediction ) ) {
					logger.info( "Correct prediction {}", prediction );
					neighbourPattern.addCorrectPrediction();
				} else {
					logger.info( "Incorrect prediction {}", prediction );
					neighbourPattern.addIncorrectPrediction();
				}

				uniquePredictions.add( prediction );
				if( uniquePredictions.size() == Parameters.FIDO_MAX_NUM_PREDICTIONS ) {
					return;
				}
			}
		}
	}

	private void constructInitialModel( int prefixLength, int suffixLength, int overlap ) {

		int unitPatternLength = prefixLength + suffixLength;
		for( int i = 0; i < unitPatternLength; i++ ) {
			indices.add( new FidoIndex() );
		}

		int start = 0;
		int end = start + unitPatternLength;

		while( end <= queryCacheKeys.size() ) {
			logger.info("Prefix ranges from {} to {}, suffix from {} to {}", start, start+prefixLength,
					start+prefixLength, end);
			List<String> prefix = queryCacheKeys.subList( start, start+prefixLength );
			List<String> suffix = queryCacheKeys.subList( start+prefixLength, end );
			List<String> fullPattern = queryCacheKeys.subList( start, end );

			logger.info( "Prefix: {}, suffix:{}, fullPattern:{}", prefix, suffix, fullPattern );
			List<Integer> neighbours = FidoModel.getOrderedNeighbours( fullPattern, indices );

			if( !patternCovered( prefix, suffix, neighbours ) ) {
				UnitPattern unitPattern = new UnitPattern( prefix, suffix );
				addPattern( unitPattern, fullPattern );
			}
			start = end - overlap;
			end = start + unitPatternLength;
		}
	}

	private void addPattern( UnitPattern unitPattern, List<String> fullPattern ) {
		int positionInUnitPatterns = unitPatterns.size();
		unitPatterns.add( unitPattern );

		for( int pos = 0; pos < fullPattern.size(); pos++ ) {
			FidoIndex index = indices.get( pos );
			index.addIndexElement( fullPattern.get( pos ), positionInUnitPatterns );
		}
	}

	private boolean patternCovered( List<String> prefix, List<String> suffix,
			List<Integer> neighbours ) {
		for( Integer neighbourPosition : neighbours ) {
			UnitPattern neighbourPattern = unitPatterns.get( neighbourPosition );
			if( neighbourPattern.withinThresholdDistance( prefix,
						suffix, Parameters.FIDO_DISTANCE_THRESHOLD ) ) {
				return true;
			}
		}
		return false;
	}

	private Query getQueryFromString( AntlrParser parser, String queryString ) {
		Query query = null;
		try {
			AntlrParser.ParseResult parseResult = parser.buildParseTree( queryString );
			ParseTree parseTree = parseResult.getParseTree();
			AntlrQueryMetadata queryMetadata = parser.getQueryMetadata( parseTree );

			// We don't perform constant extraction or tracking on write queries
			if( queryMetadata.getQueryType() == AntlrQueryType.SELECT ) {
				query = new Query( queryString, parseResult, queryMetadata, parser );
			}
		} catch( ParseCancellationException e ) {
			logger.warn( "Query cannot be parsed {}: {}", queryString, e );
		}
		return query;
	}

}
