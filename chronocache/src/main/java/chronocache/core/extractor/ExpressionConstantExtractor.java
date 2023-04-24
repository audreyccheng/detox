package chronocache.core.extractor;

import java.util.LinkedList;
import java.util.List;

import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.IntervalExpression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.OracleHierarchicalExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;

public class ExpressionConstantExtractor implements ExpressionVisitor {

	private List<String> constants;

	public ExpressionConstantExtractor(){
		constants = new LinkedList<String>();
	}
	public void visit(NullValue nullValue) {
		constants.add(nullValue.toString());
		
	}

	public void visit(Function function) {
		ExpressionList el = function.getParameters();
		for(Expression e : el.getExpressions()){
			e.accept(this);
		}		
	}

	public void visit(SignedExpression signedExpression) {
		String sign = String.valueOf(signedExpression.getSign());
		ExpressionConstantExtractor ece = new ExpressionConstantExtractor();
		signedExpression.getExpression().accept(ece);
		for(String s : ece.getConstants()){
			constants.add(sign + s);
		}
	}

	public void visit(JdbcParameter jdbcParameter) {
		constants.add(jdbcParameter.toString());
	}

	public void visit(JdbcNamedParameter jdbcNamedParameter) {
		constants.add(jdbcNamedParameter.toString());
		
	}

	public void visit(DoubleValue doubleValue) {
		constants.add(doubleValue.toString());
		
	}

	public void visit(LongValue longValue) {
		constants.add(longValue.toString());
		
	}

	public void visit(DateValue dateValue) {
		constants.add(dateValue.toString());
		
	}

	public void visit(TimeValue timeValue) {
		constants.add(timeValue.toString());
		
	}

	public void visit(TimestampValue timestampValue) {
		constants.add(timestampValue.toString());
		
	}

	public void visit(Parenthesis parenthesis) {
		parenthesis.getExpression().accept(this);
		
	}

	public void visit(StringValue stringValue) {
		constants.add(stringValue.toString());
		
	}

	public void visit(Addition addition) {
		addition.getLeftExpression().accept(this);
		addition.getRightExpression().accept(this);
		
	}

	public void visit(Division division) {
		division.getLeftExpression().accept(this);
		division.getRightExpression().accept(this);	
	}

	public void visit(Multiplication multiplication) {
		multiplication.getLeftExpression().accept(this);
		multiplication.getRightExpression().accept(this);
		
	}

	public void visit(Subtraction subtraction) {
		subtraction.getLeftExpression().accept(this);
		subtraction.getRightExpression().accept(this);
		
	}

	public void visit(AndExpression andExpression) {
		andExpression.getLeftExpression().accept(this);
		andExpression.getRightExpression().accept(this);
		
	}

	public void visit(OrExpression orExpression) {
		orExpression.getLeftExpression().accept(this);
		orExpression.getRightExpression().accept(this);
		
	}

	public void visit(Between between) {
		between.getLeftExpression().accept(this);
		between.getBetweenExpressionStart().accept(this);
		between.getBetweenExpressionStart().accept(this);
		
	}

	public void visit(EqualsTo equalsTo) {
		equalsTo.getLeftExpression().accept(this);
		equalsTo.getRightExpression().accept(this);		
	}

	public void visit(GreaterThan greaterThan) {
		greaterThan.getLeftExpression().accept(this);
		greaterThan.getRightExpression().accept(this);
		
	}

	public void visit(GreaterThanEquals greaterThanEquals) {
		greaterThanEquals.getLeftExpression().accept(this);
		greaterThanEquals.getRightExpression().accept(this);
		
	}

	public void visit(InExpression inExpression) {
		inExpression.getLeftExpression().accept(this);
		ItemListConstantExtractor ilh = new ItemListConstantExtractor();
		inExpression.getRightItemsList().accept(ilh);
		constants.addAll(ilh.getConstants());
		
	}

	public void visit(IsNullExpression isNullExpression) {
		isNullExpression.getLeftExpression().accept(this);
	}

	public void visit(LikeExpression likeExpression) {
		likeExpression.getLeftExpression().accept(this);
		likeExpression.getRightExpression().accept(this);
		
	}

	public void visit(MinorThan minorThan) {
		minorThan.getLeftExpression().accept(this);
		minorThan.getRightExpression().accept(this);
	}

	public void visit(MinorThanEquals minorThanEquals) {
		minorThanEquals.getLeftExpression().accept(this);
		minorThanEquals.getRightExpression().accept(this);
		
	}

	public void visit(NotEqualsTo notEqualsTo) {
		notEqualsTo.getLeftExpression().accept(this);
		notEqualsTo.getRightExpression().accept(this);
		
	}

	public void visit(Column tableColumn) {		
	}

	public void visit(SubSelect subSelect) {
		SelectConstantExtractor sse = new  SelectConstantExtractor();
		subSelect.getSelectBody().accept(sse);
		constants.addAll(sse.getConstants());
	}

	public void visit(CaseExpression caseExpression) {
		caseExpression.getSwitchExpression().accept(this);
		for( Expression e : caseExpression.getWhenClauses() ){
			e.accept(this);
		}
		caseExpression.getElseExpression().accept(this);
		
	}

	public void visit(WhenClause whenClause) {
		whenClause.getWhenExpression().accept(this);
		whenClause.getThenExpression().accept(this);
		
	}

	public void visit(ExistsExpression existsExpression) {
		existsExpression.getRightExpression().accept(this);
		
	}

	public void visit(AllComparisonExpression allComparisonExpression) {
		allComparisonExpression.getSubSelect().accept(this);
		
	}

	public void visit(AnyComparisonExpression anyComparisonExpression) {
		anyComparisonExpression.getSubSelect().accept(this);
		
	}

	public void visit(Concat concat) {
		concat.getLeftExpression().accept(this);
		concat.getRightExpression().accept(this);
		
	}

	public void visit(Matches matches) {
		matches.getLeftExpression().accept(this);
		matches.getRightExpression().accept(this);
	}

	public void visit(BitwiseAnd bitwiseAnd) {
		bitwiseAnd.getLeftExpression().accept(this);
		bitwiseAnd.getRightExpression().accept(this);
		
	}

	public void visit(BitwiseOr bitwiseOr) {
		bitwiseOr.getLeftExpression().accept(this);
		bitwiseOr.getRightExpression().accept(this);
		
	}

	public void visit(BitwiseXor bitwiseXor) {
		bitwiseXor.getLeftExpression().accept(this);
		bitwiseXor.getRightExpression().accept(this);
		
	}

	public void visit(CastExpression cast) {
		//TODO ignore this for now
		
		
	}

	public void visit(Modulo modulo) {
		modulo.getLeftExpression().accept(this);
		modulo.getRightExpression().accept(this);
		
	}

	//Ignoring these for now
	public void visit(AnalyticExpression aexpr) {
		// TODO Auto-generated method stub
		
	}

	public void visit(ExtractExpression eexpr) {
		// TODO Auto-generated method stub
		
	}

	public void visit(IntervalExpression iexpr) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OracleHierarchicalExpression oexpr) {
		// TODO Auto-generated method stub
		
	}

	public void visit(RegExpMatchOperator rexpr) {
		// TODO Auto-generated method stub
		
	}
	public List<String> getConstants() {
		return constants;
	}
}
