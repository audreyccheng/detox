package chronocache.core.extractor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.statement.select.Distinct;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperation;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;

public class SelectConstantExtractor implements SelectVisitor {

	List<String> constants;
	public SelectConstantExtractor(){
		constants = new LinkedList<String>();
	}
	
	public void visit(PlainSelect plainSelect) {
		FromItemConstantExtractor fice = new FromItemConstantExtractor();
		if( plainSelect.getFromItem() != null ){
			plainSelect.getFromItem().accept(fice);
		}
		List<Expression> groupBys = plainSelect.getGroupByColumnReferences();
		if( groupBys != null && !groupBys.isEmpty() ){
;
			ExpressionConstantExtractor ece = new ExpressionConstantExtractor();
			for( Expression groupBy : groupBys){
				groupBy.accept(ece);
			}
			constants.addAll(ece.getConstants());
		}
		Expression e = plainSelect.getHaving();
		if( e != null ){
			ExpressionConstantExtractor ece = new ExpressionConstantExtractor();
			e.accept(ece);
			constants.addAll(ece.getConstants());
		}
		plainSelect.getInto();
		List<Join> joins = plainSelect.getJoins();
		if( joins != null && !joins.isEmpty()){
			for( Join j : joins){
				//Hash the join
				extractConstantsFromJoin(j);
			}
		}
		plainSelect.getLimit();
		plainSelect.getOrderByElements();
		Expression where = plainSelect.getWhere();
		if( where != null ){
			ExpressionConstantExtractor ece = new ExpressionConstantExtractor();
			where.accept(ece);
			constants.addAll(ece.getConstants());
		}
	}

	/**
	 * Add the join expression to the hash
	 * @param j
	 */
	private void extractConstantsFromJoin(Join j) {
		if( j.getOnExpression() != null ){
			ExpressionConstantExtractor ece = new ExpressionConstantExtractor();
			j.getOnExpression().accept(ece);
			constants.addAll(ece.getConstants());
		}
		FromItemConstantExtractor fice = new FromItemConstantExtractor();
		j.getRightItem().accept(fice);
		constants.addAll(fice.getConstants());
	}

	public void visit(SetOperationList setOpList) {
		ExpressionConstantExtractor ece = new ExpressionConstantExtractor();
		for(OrderByElement ob : setOpList.getOrderByElements() ){
			ob.getExpression().accept(ece);
			constants.addAll(ece.getConstants());
			// TODO ignore null ordering for now
		}
		for(PlainSelect ps : setOpList.getPlainSelects()){
			ps.accept(this);
		}
		
	}

	public void visit(WithItem withItem) {	
	}

	public List<String> getConstants() {
		return constants;
	}

}
