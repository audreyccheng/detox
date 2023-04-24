package chronocache.core.extractor;

import java.util.LinkedList;
import java.util.List;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.ValuesList;

public class FromItemConstantExtractor implements FromItemVisitor {

	private List<String> constants;
	
	public FromItemConstantExtractor(){
		constants = new LinkedList<String>();
	}
	
	public void visit(Table tableName) {
		
	}
	
	public List<String> getConstants(){
		return constants;
	}

	public void visit(SubSelect subSelect) {
		SelectConstantExtractor sse = new SelectConstantExtractor();
		subSelect.getSelectBody().accept(sse);
		constants.addAll(sse.getConstants());
		
	}

	public void visit(SubJoin subjoin) {
		subjoin.getLeft().accept(this);
		ExpressionConstantExtractor ece = new ExpressionConstantExtractor();
		subjoin.getJoin().getOnExpression().accept(ece);
		constants.addAll(ece.getConstants());
		subjoin.getJoin().getRightItem().accept(this);
		
	}

	public void visit(LateralSubSelect lateralSubSelect) {
		// TODO Auto-generated method stub
		
	}

	public void visit(ValuesList valuesList) {
		//TODO: pretty sure we don't need to iterate over this
//		ItemListHasher ilh = new ItemListHasher();
//		valuesList.getMultiExpressionList().accept(ilh);
		
	}

}
