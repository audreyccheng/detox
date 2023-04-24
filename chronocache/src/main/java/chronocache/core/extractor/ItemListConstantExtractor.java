package chronocache.core.extractor;
import java.util.LinkedList;
import java.util.List;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.statement.select.SubSelect;

public class ItemListConstantExtractor implements ItemsListVisitor {
	private List<String> constants;
	public ItemListConstantExtractor(){
		constants = new LinkedList<String>();
	}
	public void visit(SubSelect subSelect) {
		ExpressionConstantExtractor ece = new ExpressionConstantExtractor();
		subSelect.accept(ece);
		constants.addAll(ece.getConstants());
		
		
	}
	public void visit(ExpressionList expressionList) {
		ExpressionConstantExtractor ece = new ExpressionConstantExtractor();
		for( Expression e : expressionList.getExpressions() ){
			e.accept(ece);
		}
		constants.addAll(ece.getConstants());
		
	}
	public void visit(MultiExpressionList multiExprList) {
		ExpressionConstantExtractor ece = new ExpressionConstantExtractor();
		List<ExpressionList> mel = multiExprList.getExprList();
		for( ExpressionList el : mel ){
			for( Expression e : el.getExpressions() ){
				e.accept(ece);
			}
		}
		constants.addAll(ece.getConstants());
	}
	
	public List<String> getConstants(){
		return constants;
	}

}
