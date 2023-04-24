package chronocache.core.qry;

/**
 * An identifier for an executed query, usually a hash
 * of a parse tree
 * @author bjglasbe
 *
 */
public class QueryIdentifier {
	private long id;

	public QueryIdentifier(long id){
		this.id = id;
	}
	
	public QueryIdentifier() {
		super();
	}

	public long getId(){
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryIdentifier other = (QueryIdentifier) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Long.toString( id );
	}

}
