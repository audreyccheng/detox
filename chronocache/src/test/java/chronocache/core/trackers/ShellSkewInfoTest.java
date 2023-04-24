package chronocache.core.trackers;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;

import chronocache.core.Parameters;
import chronocache.core.qry.QueryIdentifier;

public class ShellSkewInfoTest {

	/**
	 * Test that we don't predict params with no information
	 */
	@Test
	public void testNoPredictWithNoData(){
		ShellSkewInfo ssi = new ShellSkewInfo(new QueryIdentifier(5), 1);
		assertThat( ssi.predictParamForIndex(0), nullValue() );
	}

	/**
	 * Test that we require a base number of data points before we will predict
	 * params
	 */
	@Test
	public void testNoPredictWithNotEnoughDataPoints(){
		Parameters.TRACKING_PERIOD = 3;
		ShellSkewInfo ssi = new ShellSkewInfo(new QueryIdentifier(5), 1);
		assertThat( ssi.predictParamForIndex(0), nullValue() );
		List<String> params = new LinkedList<>();
		params.add("1");
		ssi.addParamData(params);
		assertThat( ssi.predictParamForIndex(0), nullValue() );
		ssi.addParamData(params);
		assertThat( ssi.predictParamForIndex(0), nullValue() );
		ssi.addParamData(params);
		assertThat( ssi.predictParamForIndex(0), equalTo("1") );
	}

	/**
	 * Test that we can track multiple parameters at the same time
	 */
	@Test
	public void testMultipleParamTracking(){
		Parameters.TRACKING_PERIOD = 3;
		ShellSkewInfo ssi = new ShellSkewInfo(new QueryIdentifier(5), 2);
		assertThat( ssi.predictParamForIndex(0), nullValue() );
		List<String> params = new LinkedList<>();
		params.add("1");
		params.add("2");
		ssi.addParamData(params);
		assertThat( ssi.predictParamForIndex(0), nullValue() );
		assertThat( ssi.predictParamForIndex(1), nullValue() );
		ssi.addParamData(params);
		assertThat( ssi.predictParamForIndex(0), nullValue() );
		assertThat( ssi.predictParamForIndex(1), nullValue() );
		ssi.addParamData(params);
		assertThat( ssi.predictParamForIndex(0), equalTo("1") );
		assertThat( ssi.predictParamForIndex(1), equalTo("2") );
	}

	/**
	 * Test that we don't predict if the probability of a given value of a parameter
	 * is lower than the threshold
	 */
	@Test
	public void testNoPredictWithLowProb(){
		Parameters.TRACKING_PERIOD = 3;
		Parameters.SHELL_SKEW_THRESHOLD_TO_PREDICT = 0.8;
		ShellSkewInfo ssi = new ShellSkewInfo(new QueryIdentifier(5), 1);
		assertThat( ssi.predictParamForIndex(0), nullValue() );
		List<String> params1 = new LinkedList<>();
		params1.add("1");
		List<String> params2 = new LinkedList<>();
		params2.add("2");
		ssi.addParamData(params1);
		assertThat( ssi.predictParamForIndex(0), nullValue() );
		ssi.addParamData(params1);
		assertThat( ssi.predictParamForIndex(0), nullValue() );
		ssi.addParamData(params1);
		assertThat( ssi.predictParamForIndex(0), equalTo("1") );
		ssi.addParamData(params2); // 1/4
		assertThat( ssi.predictParamForIndex(0), nullValue() );
		ssi.addParamData(params2); // 2/5
		ssi.addParamData(params2); // 3/6
		ssi.addParamData(params2); // 4/7
		ssi.addParamData(params2); // 5/8
		ssi.addParamData(params2); // 6/9
		ssi.addParamData(params2); // 7/10
		ssi.addParamData(params2); // 8/11
		ssi.addParamData(params2); // 9/12
		ssi.addParamData(params2); // 10/13
		ssi.addParamData(params2); // 11/14
		ssi.addParamData(params2); // 12/15
		assertThat( ssi.predictParamForIndex(0), equalTo("2") );
	}
}
