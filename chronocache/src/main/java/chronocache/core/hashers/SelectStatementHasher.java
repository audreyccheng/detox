package chronocache.core.hashers;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SelectStatementHasher implements SelectVisitor {

	private long hash;
	private long qsh_ignored_hash;
	private Logger log = LoggerFactory.getLogger( this.getClass() );
	public SelectStatementHasher(){
		hash = 0;
		qsh_ignored_hash = 0;
	}

	public long getIgnoredHash() {
		return qsh_ignored_hash;
	}
	
	public void visit(PlainSelect plainSelect) {
		Distinct d = plainSelect.getDistinct();
		if( d != null ){
			hash ^= CCJSqlParser.K_DISTINCT;
			hash = Long.rotateLeft(hash, 7);
			
			SelectItemHasher sih = new SelectItemHasher();
			for( SelectItem si : d.getOnSelectItems() ) {
				si.accept(sih);
				qsh_ignored_hash ^= sih.getIgnoredHash();
				qsh_ignored_hash = Long.rotateLeft( qsh_ignored_hash, 7 );

			}

			log.trace("Ignored hash: {}", qsh_ignored_hash );
			hash ^= sih.getHash();
			hash = Long.rotateLeft(hash, 7);
		}
		FromItemHasher fih = new FromItemHasher();
		if( plainSelect.getFromItem() != null ){
			hash ^= CCJSqlParser.K_FROM;
			hash = Long.rotateLeft(hash, 7);
			plainSelect.getFromItem().accept(fih);
			hash ^= fih.getHash();
			hash = Long.rotateLeft(hash, 7);
		}
		List<Expression> groupBys = plainSelect.getGroupByColumnReferences();
		if( groupBys != null && !groupBys.isEmpty() ){
			hash ^= CCJSqlParser.K_GROUP;
			hash = Long.rotateLeft(hash, 7);
			ExpressionHasher eh = new ExpressionHasher();
			for( Expression groupBy : groupBys){
				groupBy.accept(eh);
			}
			hash ^= eh.getHash();
			hash = Long.rotateLeft(hash, 7);
		}
		Expression e = plainSelect.getHaving();
		if( e != null ){
			hash ^= CCJSqlParser.K_HAVING;
			hash = Long.rotateLeft(hash, 7);
			ExpressionHasher eh = new ExpressionHasher();
			e.accept(eh);
			hash ^= eh.getHash();
			hash = Long.rotateLeft(hash, 7);
		}
		plainSelect.getInto();
		List<Join> joins = plainSelect.getJoins();
		if( joins != null && !joins.isEmpty()){
			hash ^= CCJSqlParser.K_JOIN;
			hash = Long.rotateLeft(hash, 7);
			for( Join j : joins){
				//Hash the join
				hashJoin(j);
			}
		}
		plainSelect.getLimit();
		plainSelect.getOrderByElements();
		List<SelectItem> sis = plainSelect.getSelectItems();
		//TODO: Don't hash constants with same value in select list
		if( sis != null && !sis.isEmpty()){
			SelectItemHasher sih = new SelectItemHasher();
			for(SelectItem si : sis){
				si.accept(sih);
				qsh_ignored_hash ^= sih.getIgnoredHash();
				qsh_ignored_hash = Long.rotateLeft( qsh_ignored_hash, 7 );
			}

			log.trace("Ignored hash: {}", qsh_ignored_hash );
			hash ^= sih.getHash();
			hash = Long.rotateLeft(hash, 7);
		}
		Expression where = plainSelect.getWhere();
		if( where != null ){
			hash ^= CCJSqlParser.K_WHERE;
			hash ^= Long.rotateLeft(hash, 7);
			ExpressionHasher eh = new ExpressionHasher();
			where.accept(eh);
			hash ^= eh.getHash();
			hash ^= Long.rotateLeft(hash, 7);
		}
	}

	/**
	 * Add the join expression to the hash
	 * @param j
	 */
	private void hashJoin(Join j) {
		if( j.getOnExpression() != null ){
			ExpressionHasher eh = new ExpressionHasher();
			j.getOnExpression().accept(eh);
			hash ^= eh.getHash();
			hash = Long.rotateLeft(hash, 7);
		}
		FromItemHasher fih2 = new FromItemHasher();
		j.getRightItem().accept(fih2);
		hash ^= fih2.getHash();
		hash = Long.rotateLeft(hash, 7);
		if( j.isCross() ){
			hash ^= CCJSqlParser.K_CROSS;
			hash = Long.rotateLeft(hash, 7);
		}
		if( j.isFull()){
			hash ^= CCJSqlParser.K_FULL;
			hash = Long.rotateLeft(hash, 7);
		}
		if( j.isSimple() ){
			hash ^= "SIMPLE".hashCode();
			hash = Long.rotateLeft(hash, 7);
		}
		if( j.isInner() || j.isSimple()){
			hash ^= CCJSqlParser.K_INNER;
			hash = Long.rotateLeft(hash, 7);
		}
		if( j.isLeft() ){
			hash ^= CCJSqlParser.K_LEFT;
			hash = Long.rotateLeft(hash, 7);
		}
		if( j.isNatural() ){
			hash ^= CCJSqlParser.K_NATURAL;
			hash = Long.rotateLeft(hash, 7);
		}
		if( j.isOuter() ){
			hash ^= CCJSqlParser.K_OUTER;
			hash = Long.rotateLeft(hash, 7);
		}
		if( j.isRight() ){
			hash ^= CCJSqlParser.K_RIGHT;
			hash = Long.rotateLeft(hash, 7);
		}
	}

	public void visit(SetOperationList setOpList) {
		Limit lim = setOpList.getLimit();
		if( lim != null ){
			hash ^= CCJSqlParser.K_LIMIT;
			hash = Long.rotateLeft(hash, 7);
			hash ^= lim.getOffset();
			hash = Long.rotateLeft(hash, 7);
			hash ^= lim.getRowCount();
			hash = Long.rotateLeft(hash, 7);
		}
		for(SetOperation so : setOpList.getOperations()){
			hash ^= so.toString().hashCode();
			hash = Long.rotateLeft(hash, 7);
		}
		hash ^= CCJSqlParser.K_ORDER;
		hash = Long.rotateLeft(hash, 7);
		ExpressionHasher eh = new ExpressionHasher();
		for(OrderByElement ob : setOpList.getOrderByElements() ){
			ob.getExpression().accept(eh);
			hash ^= eh.getHash();
			hash = Long.rotateLeft(hash, 7);
			// TODO ignore null ordering for now
		}
		for(PlainSelect ps : setOpList.getPlainSelects()){
			ps.accept(this);
		}
		
	}

	public void visit(WithItem withItem) {
		hash ^= CCJSqlParser.K_WITH;
		hash = Long.rotateLeft(hash, 7);
		hash ^= withItem.getName().hashCode();
		hash = Long.rotateLeft(hash, 7);
		withItem.getSelectBody().accept(this);
		
	}

	public long getHash() {
		return hash;
	}

}
