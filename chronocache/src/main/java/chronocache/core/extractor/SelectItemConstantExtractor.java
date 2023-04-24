package chronocache.core.extractor;

import java.util.LinkedList;
import java.util.List;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;

public class SelectItemConstantExtractor implements SelectItemVisitor{

	private List<String> constants;
	
	public SelectItemConstantExtractor(){
		constants = new LinkedList<String>();
	}
	public void visit(AllColumns allColumns) {
	}

	public void visit(AllTableColumns allTableColumns) {	
	}

	public void visit(SelectExpressionItem selectExpressionItem) {
		ExpressionConstantExtractor ece = new ExpressionConstantExtractor();
		selectExpressionItem.getExpression().accept(ece);
		constants.addAll(ece.getConstants());
	}
	public List<String> getConstants() {
		return constants;
	}

	
}
