package chronocache.core.parser;

import java.util.List;

import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

/**
 * An expression deparser used to replace constants and generate a SQL
 * string
 * N.B. This does NOT handle signed subexpressions
 * a > -(col1 + 5)
 * @author bjglasbe
 *
 */
public class ExpDeparser extends ExpressionDeParser{
	
	private List<String> constants;
	
	public ExpDeparser(){
		super();
	}
	
	public ExpDeparser(SelectVisitor selectVisitor, StringBuilder buffer){
		super(selectVisitor, buffer);
	}
	
	public void setReplaceConstants(List<String> constantsToReplace){
		constants = constantsToReplace;
	}
	
	@Override
	public void visit(LongValue longVal){
		 this.getBuffer().append(constants.remove(0));
	}
	
	@Override
	public void visit(DoubleValue doubleVal){
		 this.getBuffer().append(constants.remove(0));
	}
	
	@Override
	public void visit(StringValue stringVal){
		 this.getBuffer().append("'" + constants.remove(0) + "'");
	}
	
	@Override
	public void visit(SignedExpression exp){
		this.getBuffer().append(constants.remove(0));
	}

}
