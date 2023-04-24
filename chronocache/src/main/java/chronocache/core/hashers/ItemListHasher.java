package chronocache.core.hashers;
import java.util.List;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.statement.select.SubSelect;

public class ItemListHasher implements ItemsListVisitor {
	private long hash;
	public ItemListHasher(){
		hash = 0;
	}
	public void visit(SubSelect subSelect) {
		ExpressionHasher eh = new ExpressionHasher();
		subSelect.accept(eh);
		hash ^= eh.getHash();
		hash = Long.rotateLeft(hash, 7);
		
	}
	public void visit(ExpressionList expressionList) {
		ExpressionHasher eh = new ExpressionHasher();
		for( Expression e : expressionList.getExpressions() ){
			e.accept(eh);
		}
		hash ^= eh.getHash();
		hash = Long.rotateLeft(hash, 7);
		
	}
	public void visit(MultiExpressionList multiExprList) {
		ExpressionHasher eh = new ExpressionHasher();
		List<ExpressionList> mel = multiExprList.getExprList();
		for( ExpressionList el : mel ){
			for( Expression e : el.getExpressions() ){
				e.accept(eh);
			}
		}
		hash ^= eh.getHash();
		hash = Long.rotateLeft(hash, 7);
	}
	
	public long getHash(){
		return hash;
	}

}
