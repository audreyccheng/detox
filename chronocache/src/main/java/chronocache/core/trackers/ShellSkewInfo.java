package chronocache.core.trackers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chronocache.core.Parameters;
import chronocache.core.qry.QueryIdentifier;

public class ShellSkewInfo {

	private class MostLikelyParam {
		public MostLikelyParam(String val){
			this.val = val;
			this.timesSeen = 0;
		}
		public void set(String val, int timesSeen){
			this.val = val;
			this.timesSeen = timesSeen;
		}
		public String val;
		public int timesSeen;
	};
	
	private QueryIdentifier id;
	private List<Map<String, Integer>> paramSkews;
	private long timesTracked;
	private List<MostLikelyParam> mostLikelyParamForIndexes;
	
	public ShellSkewInfo(QueryIdentifier id, int numParams){
		this.id = id;
		timesTracked = 0;
		paramSkews = new ArrayList<>(numParams);
		for( int i = 0; i < numParams; i++){
			paramSkews.add(i, new HashMap<>());
		}
		mostLikelyParamForIndexes = new ArrayList<>(numParams);
		for( int i = 0; i < numParams; i++){
			mostLikelyParamForIndexes.add(i, new MostLikelyParam("BAD"));
		}
	}
	
	public void addParamData(List<String> params){
		synchronized( this ){
			for( int i = 0; i < params.size(); i++){
				Map<String, Integer> paramCounts = paramSkews.get(i);
				String param = params.get(i);
				Integer count = paramCounts.get(param);
				if( count == null ){
					count = 1;
					paramCounts.put(param, 1);
				} else {
					count++;
					paramCounts.put(param, count);
				}
				MostLikelyParam mostLikelyParam = mostLikelyParamForIndexes.get(i);
				if(  count >= mostLikelyParam.timesSeen  ){
					mostLikelyParam.set(param, count);
				}
			}
			timesTracked++;
		}
	}
	
	public String predictParamForIndex(int paramIndex){
		synchronized( this ){
			MostLikelyParam mlp = mostLikelyParamForIndexes.get(paramIndex);
			return ((double)mlp.timesSeen / timesTracked) >= Parameters.SHELL_SKEW_THRESHOLD_TO_PREDICT &&
					mlp.timesSeen >= Parameters.SHELL_SKEW_REQUIRED_TIMES_SEEN ? mlp.val : null;
		}
	}
}
