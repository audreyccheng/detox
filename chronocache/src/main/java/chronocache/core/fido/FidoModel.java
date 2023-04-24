package chronocache.core.fido;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedList;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;

import chronocache.core.fido.FidoIndex;
import chronocache.core.fido.UnitPattern;

public class FidoModel {

	private Logger logger = LoggerFactory.getLogger(FidoModel.class);

	private int prefixLength;
	private List<FidoIndex> columnIndices;
	private List<UnitPattern> patternMemory;
	// Map cache key to the actual query;
	private Map<String, String> queryMappings;

	public FidoModel(int prefixLength, List<FidoIndex> columnIndices,
			List<UnitPattern> patternMemory,
			Map<String, String> queryMappings) {
		this.prefixLength = prefixLength;
		this.columnIndices = columnIndices;
		this.patternMemory = patternMemory;
		this.queryMappings = queryMappings;
	}

	public List<String> makePrediction(List<String> pastQueries, int maxNumPredictions) {
		if (pastQueries.size() != getPrefixLength()) {
			return Collections.emptyList();
		}

		List<Integer> matchedPatterns = getOrderedNeighbours(pastQueries, columnIndices);
		List<String> predictions = getPrediction(matchedPatterns, maxNumPredictions);
		return predictions;
	}

	public int getPrefixLength() {
		return prefixLength;
	}

	private List<String> getPrediction(List<Integer> matchedPatterns,
			int maxNumPredictions) {
		List<String> predictions = new LinkedList<>();
		Set<String> seenPredictions = new HashSet<>();

		logger.trace("getting prediction from {} patterns", matchedPatterns.size());
		for (Integer unitPatternIndex : matchedPatterns) {
			logger.trace("prediction index {}", unitPatternIndex);
			UnitPattern unitPattern = patternMemory.get(unitPatternIndex);

			logger.trace("suffix {}", unitPattern.suffix);
			for (String suffixKey : unitPattern.suffix) {
				if (!seenPredictions.contains(suffixKey)) {
					String queryString = queryMappings.get(suffixKey);
					logger.debug("adding prediction {}:{}", suffixKey, queryString);
					predictions.add(queryString);
					seenPredictions.add(suffixKey);

					if (predictions.size() == maxNumPredictions) {
						return predictions;
					}
				}
			}
		}

		return predictions;
	}

	public static Multiset<Integer> findNeighbours(List<String> pattern,
			List<FidoIndex> columnIndices) {
		Multiset<Integer> unitPatternCounts = HashMultiset.create();

		for (int i = 0; i < pattern.size(); i++) {
			String pastQuery = pattern.get(i);
			FidoIndex columnIndex = columnIndices.get(i);
			Collection<Integer> unitPatterns = columnIndex.findElement(pastQuery);

			unitPatternCounts.addAll(unitPatterns);
		}
		return unitPatternCounts;
	}

	public static List<Integer>
		getOrderedNeighbours(List<String> pattern, List<FidoIndex> columnIndices) {
			ImmutableMultiset<Integer> neighbours =
				Multisets.copyHighestCountFirst(findNeighbours(pattern, columnIndices));
			List<Integer> orderedNeighbours = new ArrayList<>();
			Set<Integer> seenNeighbours = new HashSet<>();

			for (Integer neighbour : neighbours) {
				if (!seenNeighbours.contains(neighbour)) {
					orderedNeighbours.add(neighbour);
					seenNeighbours.add(neighbour);
				}
			}

			return orderedNeighbours;
		}
}
