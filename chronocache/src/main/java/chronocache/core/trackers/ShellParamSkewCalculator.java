package chronocache.core.trackers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chronocache.core.qry.QueryIdentifier;

public class ShellParamSkewCalculator {
	
	private Map<QueryIdentifier, ShellSkewInfo> shellSkewMap;
	private Logger log;
	
	public ShellParamSkewCalculator(){
		shellSkewMap = new HashMap<>();
		log = LoggerFactory.getLogger(this.getClass());
	}
	
	public void addParamsForShell(QueryIdentifier id, List<String> params){
		synchronized( this ){
			log.debug("Adding {} for shell {}", params, id.getId());
			if( !shellSkewMap.containsKey(id) ){
				ShellSkewInfo ssi = new ShellSkewInfo(id, params.size());
				shellSkewMap.put(id, ssi);
				ssi.addParamData(params);
			} else {
				ShellSkewInfo ssi = shellSkewMap.get(id);
				ssi.addParamData(params);
			}
		}
	}
	
	public ShellSkewInfo getSkewInfoForQueryId(QueryIdentifier id){
		log.debug("Looking up parameters for query {}", id.getId());
		return shellSkewMap.get(id);
	}

}
