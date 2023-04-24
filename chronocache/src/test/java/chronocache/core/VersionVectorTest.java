package chronocache.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.equalTo;


public class VersionVectorTest {
	private ArrayList<Long> version1 = new ArrayList<Long>() {{
		add( 12L );
		add( 3L );
	}};

	private ArrayList<Long> version2 = new ArrayList<Long>() {{
		add( 14L );
		add( 7L );
	}};


	@Test
	public void testCreate() {
		VersionVector vector1 = new VersionVector( version1 );
		VersionVector copyVector = new VersionVector( vector1 );
		assertThat( vector1, equalTo( copyVector ) );
		assertThat( vector1.isSameVersion( copyVector ), is( true )  );

		VersionVector vector2 = new VersionVector( version2 );
		assertThat( vector2, not( equalTo( version1 ) ) );
	}

}
