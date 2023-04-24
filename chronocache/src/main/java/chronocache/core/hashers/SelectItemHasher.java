package chronocache.core.hashers;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectItemHasher implements SelectItemVisitor{

	private long hash;
	private long qsh_ignored_hash;
	private Logger log;
	
	public SelectItemHasher(){
		hash = 0;
		qsh_ignored_hash = 0;
		log = LoggerFactory.getLogger(this.getClass());
	}

	public long getIgnoredHash(){	
		return qsh_ignored_hash;
	}

	public void visit(AllColumns allColumns) {
		hash ^= "*".hashCode();
		
	}

	public void visit(AllTableColumns allTableColumns) {
		hash ^= allTableColumns.hashCode();
		
	}

	public void visit(SelectExpressionItem selectExpressionItem) {
		Alias alias = selectExpressionItem.getAlias();
		if( alias != null ){
			hash ^= alias.getName().hashCode();
			hash = Long.rotateLeft(hash, 7);
		}
		ExpressionHasher eh = new ExpressionHasher(true);
		selectExpressionItem.getExpression().accept(eh);
		hash ^= eh.getHash();

		hash = Long.rotateLeft(hash, 7);
		qsh_ignored_hash = eh.getIgnoredHash();
		log.trace("qsh ignored hash: {}", qsh_ignored_hash );
	}
	public long getHash() {
		return hash;
	}

	
}
