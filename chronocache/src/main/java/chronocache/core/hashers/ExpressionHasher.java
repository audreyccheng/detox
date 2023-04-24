package chronocache.core.hashers;

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
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExpressionHasher implements ExpressionVisitor {

	private long hash;
	private long qsh_ignored_hash;
	private boolean isSelectList;
	// Some large prime out of range of other constants ot avoid hash collisions
	private static long CONSTANT_HASH_VAL = 131311;
	private Logger log = LoggerFactory.getLogger(this.getClass());

	public ExpressionHasher() {
		hash = 0;
		qsh_ignored_hash = 0;
		isSelectList = false;
	}

	public long getIgnoredHash() {
		return qsh_ignored_hash;
	}

	public ExpressionHasher(boolean isSelectList) {
		hash = 0;
		this.isSelectList = isSelectList;
	}

	public void visit(NullValue nullValue) {
		hash ^= isSelectList ? nullValue.toString().hashCode() : CONSTANT_HASH_VAL;
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(Function function) {
		log.trace("Found function: {}", function.getName() );
		if( function.getName().equals( "qsh_ignore" ) ) {
			ExpressionList el = function.getParameters();
			if (el != null) {
				for (Expression e : el.getExpressions()) {
					ExpressionHasher eh = new ExpressionHasher(true);
					e.accept(eh);
					qsh_ignored_hash ^= eh.getHash();
					qsh_ignored_hash = Long.rotateLeft(qsh_ignored_hash, 7);
				}
			}
			return;
		}
		hash ^= function.getName().hashCode();
		hash = Long.rotateLeft(hash, 7);
		ExpressionList el = function.getParameters();
		if (el != null) {
			for (Expression e : el.getExpressions()) {
				e.accept(this);
				hash = Long.rotateLeft(hash, 7);
			}
		}

	}

	public void visit(SignedExpression signedExpression) {
		hash ^= String.valueOf(signedExpression.getSign()).hashCode();
		hash = Long.rotateLeft(hash, 7);
		signedExpression.getExpression().accept(this);
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(JdbcParameter jdbcParameter) {
		hash ^= isSelectList ? jdbcParameter.toString().hashCode() : CONSTANT_HASH_VAL;
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(JdbcNamedParameter jdbcNamedParameter) {
		hash ^= isSelectList ? jdbcNamedParameter.toString().hashCode() : CONSTANT_HASH_VAL;
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(DoubleValue doubleValue) {
		hash ^= isSelectList ? doubleValue.toString().hashCode() : CONSTANT_HASH_VAL;
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(LongValue longValue) {
		hash ^= isSelectList ? longValue.toString().hashCode() : CONSTANT_HASH_VAL;
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(DateValue dateValue) {
		hash ^= isSelectList ? dateValue.toString().hashCode() : CONSTANT_HASH_VAL;
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(TimeValue timeValue) {
		hash ^= isSelectList ? timeValue.toString().hashCode() : CONSTANT_HASH_VAL;
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(TimestampValue timestampValue) {
		hash ^= isSelectList ? timestampValue.toString().hashCode() : CONSTANT_HASH_VAL;
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(Parenthesis parenthesis) {
		hash ^= "(".hashCode();
		hash = Long.rotateLeft(hash, 7);
		parenthesis.getExpression().accept(this);

	}

	public void visit(StringValue stringValue) {
		hash ^= isSelectList ? stringValue.toString().hashCode() : CONSTANT_HASH_VAL;
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(Addition addition) {
		hash ^= "+".hashCode();
		hash = Long.rotateLeft(hash, 7);
		addition.getLeftExpression().accept(this);
		addition.getRightExpression().accept(this);

	}

	public void visit(Division division) {
		hash ^= "/".hashCode();
		hash = Long.rotateLeft(hash, 7);
		division.getLeftExpression().accept(this);
		division.getRightExpression().accept(this);
	}

	public void visit(Multiplication multiplication) {
		hash ^= "*".hashCode();
		hash = Long.rotateLeft(hash, 7);
		multiplication.getLeftExpression().accept(this);
		multiplication.getRightExpression().accept(this);

	}

	public void visit(Subtraction subtraction) {
		hash ^= "-".hashCode();
		hash = Long.rotateLeft(hash, 7);
		subtraction.getLeftExpression().accept(this);
		subtraction.getRightExpression().accept(this);

	}

	public void visit(AndExpression andExpression) {
		hash ^= CCJSqlParser.K_AND;
		hash = Long.rotateLeft(hash, 7);
		andExpression.getLeftExpression().accept(this);
		andExpression.getRightExpression().accept(this);

	}

	public void visit(OrExpression orExpression) {
		hash ^= CCJSqlParser.K_OR;
		hash = Long.rotateLeft(hash, 7);
		orExpression.getLeftExpression().accept(this);
		orExpression.getRightExpression().accept(this);

	}

	public void visit(Between between) {
		hash ^= CCJSqlParser.K_BETWEEN;
		hash = Long.rotateLeft(hash, 7);
		between.getLeftExpression().accept(this);
		between.getBetweenExpressionStart().accept(this);
		between.getBetweenExpressionStart().accept(this);

	}

	public void visit(EqualsTo equalsTo) {
		hash ^= "=".hashCode();
		hash = Long.rotateLeft(hash, 7);
		equalsTo.getLeftExpression().accept(this);
		equalsTo.getRightExpression().accept(this);
	}

	public void visit(GreaterThan greaterThan) {
		hash ^= ">".hashCode();
		hash = Long.rotateLeft(hash, 7);
		greaterThan.getLeftExpression().accept(this);
		greaterThan.getRightExpression().accept(this);

	}

	public void visit(GreaterThanEquals greaterThanEquals) {
		hash ^= ">=".hashCode();
		hash = Long.rotateLeft(hash, 7);
		greaterThanEquals.getLeftExpression().accept(this);
		greaterThanEquals.getRightExpression().accept(this);

	}

	public void visit(InExpression inExpression) {
		hash ^= CCJSqlParser.K_IN;
		hash = Long.rotateLeft(hash, 7);
		inExpression.getLeftExpression().accept(this);
		ItemListHasher ilh = new ItemListHasher();
		inExpression.getRightItemsList().accept(ilh);
		hash ^= ilh.getHash();
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(IsNullExpression isNullExpression) {
		// TODO find appropriate token
		hash ^= "IS_NULL".hashCode();
		hash = Long.rotateLeft(hash, 7);
		isNullExpression.getLeftExpression().accept(this);
	}

	public void visit(LikeExpression likeExpression) {
		hash ^= CCJSqlParser.K_LIKE;
		hash = Long.rotateLeft(hash, 7);
		likeExpression.getLeftExpression().accept(this);
		likeExpression.getRightExpression().accept(this);

	}

	public void visit(MinorThan minorThan) {
		hash ^= "<".hashCode();
		hash = Long.rotateLeft(hash, 7);
		minorThan.getLeftExpression().accept(this);
		minorThan.getRightExpression().accept(this);
	}

	public void visit(MinorThanEquals minorThanEquals) {
		hash ^= "<=".hashCode();
		hash = Long.rotateLeft(hash, 7);
		minorThanEquals.getLeftExpression().accept(this);
		minorThanEquals.getRightExpression().accept(this);

	}

	public void visit(NotEqualsTo notEqualsTo) {
		hash ^= "<>".hashCode();
		hash = Long.rotateLeft(hash, 7);
		notEqualsTo.getLeftExpression().accept(this);
		notEqualsTo.getRightExpression().accept(this);

	}

	public void visit(Column tableColumn) {
		hash ^= tableColumn.getFullyQualifiedName().hashCode();
		hash = Long.rotateLeft(hash, 7);

	}

	public void visit(SubSelect subSelect) {
		if (subSelect.getAlias() != null) {
			hash ^= CCJSqlParser.K_AS;
			hash = Long.rotateLeft(hash, 7);
			hash ^= subSelect.getAlias().getName().hashCode();
			hash = Long.rotateLeft(hash, 7);
		}
		// Ignore pivot for now
		// if( subSelect.getPivot() != null ){
		// subSelect.getPivot().
		// }
		SelectStatementHasher ssh = new SelectStatementHasher();
		subSelect.getSelectBody().accept(ssh);
		hash ^= ssh.getHash();
		hash = Long.rotateLeft(hash, 7);
	}

	public void visit(CaseExpression caseExpression) {
		hash ^= CCJSqlParser.K_CASE;
		hash = Long.rotateLeft(hash, 7);
		caseExpression.getSwitchExpression().accept(this);
		for (Expression e : caseExpression.getWhenClauses()) {
			e.accept(this);
		}
		hash ^= CCJSqlParser.K_ELSE;
		hash = Long.rotateLeft(hash, 7);
		caseExpression.getElseExpression().accept(this);

	}

	public void visit(WhenClause whenClause) {
		hash ^= CCJSqlParser.K_WHEN;
		hash = Long.rotateLeft(hash, 7);
		whenClause.getWhenExpression().accept(this);
		hash ^= CCJSqlParser.K_THEN;
		hash = Long.rotateLeft(hash, 7);
		whenClause.getThenExpression().accept(this);

	}

	public void visit(ExistsExpression existsExpression) {
		hash ^= CCJSqlParser.K_EXISTS;
		hash = Long.rotateLeft(hash, 7);
		existsExpression.getRightExpression().accept(this);

	}

	public void visit(AllComparisonExpression allComparisonExpression) {
		hash ^= CCJSqlParser.K_ALL;
		hash = Long.rotateLeft(hash, 7);
		allComparisonExpression.getSubSelect().accept(this);

	}

	public void visit(AnyComparisonExpression anyComparisonExpression) {
		hash ^= CCJSqlParser.K_ANY;
		hash = Long.rotateLeft(hash, 7);
		anyComparisonExpression.getSubSelect().accept(this);

	}

	public void visit(Concat concat) {
		hash ^= "||".hashCode();
		hash = Long.rotateLeft(hash, 7);
		concat.getLeftExpression().accept(this);
		concat.getRightExpression().accept(this);

	}

	public void visit(Matches matches) {
		hash ^= "@@".hashCode();
		hash = Long.rotateLeft(hash, 7);
		matches.getLeftExpression().accept(this);
		matches.getRightExpression().accept(this);
	}

	public void visit(BitwiseAnd bitwiseAnd) {
		hash ^= "&".hashCode();
		hash = Long.rotateLeft(hash, 7);
		bitwiseAnd.getLeftExpression().accept(this);
		bitwiseAnd.getRightExpression().accept(this);

	}

	public void visit(BitwiseOr bitwiseOr) {
		hash ^= "|".hashCode();
		hash = Long.rotateLeft(hash, 7);
		bitwiseOr.getLeftExpression().accept(this);
		bitwiseOr.getRightExpression().accept(this);

	}

	public void visit(BitwiseXor bitwiseXor) {
		hash ^= "^".hashCode();
		hash = Long.rotateLeft(hash, 7);
		bitwiseXor.getLeftExpression().accept(this);
		bitwiseXor.getRightExpression().accept(this);

	}

	public void visit(CastExpression cast) {
		hash ^= CCJSqlParser.K_CAST;
		hash = Long.rotateLeft(hash, 7);
		cast.getLeftExpression().accept(this);
		hash ^= cast.getType().getDataType().hashCode();
		hash = Long.rotateLeft(hash, 7);
		if (cast.getType().getArgumentsStringList() != null) {
			for (String s : cast.getType().getArgumentsStringList()) {
				hash ^= s.hashCode();
				hash = Long.rotateLeft(hash, 7);
			}
		}
		// TODO Ignore Charset for now

	}

	public void visit(Modulo modulo) {
		hash ^= "%".hashCode();
		hash = Long.rotateLeft(hash, 7);
		modulo.getLeftExpression().accept(this);
		modulo.getRightExpression().accept(this);

	}

	// Ignoring these for now
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

	public long getHash() {
		return hash;
	}
}
