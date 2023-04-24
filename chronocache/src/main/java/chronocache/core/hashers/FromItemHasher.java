package chronocache.core.hashers;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.ValuesList;

public class FromItemHasher implements FromItemVisitor {

	private long hash;
	
	public FromItemHasher(){
		hash = 0;
	}
	
	public void visit(Table tableName) {
		hash ^= tableName.getFullyQualifiedName().hashCode();
		hash = Long.rotateLeft(hash, 7);
		
	}
	
	public long getHash(){
		return hash;
	}

	public void visit(SubSelect subSelect) {
		SelectStatementHasher ssh = new SelectStatementHasher();
		subSelect.getSelectBody().accept(ssh);
		hash ^= ssh.getHash();
		hash = Long.rotateLeft(hash, 7);
		
	}

	public void visit(SubJoin subjoin) {
		hash ^= CCJSqlParser.K_JOIN;
		hash = Long.rotateLeft(hash, 7);
		subjoin.getLeft().accept(this);
		ExpressionHasher eh = new ExpressionHasher();
		subjoin.getJoin().getOnExpression().accept(eh);
		hash ^= eh.getHash();
		hash = Long.rotateLeft(hash, 7);
		subjoin.getJoin().getRightItem().accept(this);
		if( subjoin.getJoin().isCross() ){
			hash ^= CCJSqlParser.K_CROSS;
			hash = Long.rotateLeft(hash, 7);
		}
		if( subjoin.getJoin().isFull()){
			hash ^= CCJSqlParser.K_FULL;
			hash = Long.rotateLeft(hash, 7);
		}
		if( subjoin.getJoin().isInner() ) {
			hash ^= CCJSqlParser.K_INNER;
			hash = Long.rotateLeft(hash, 7);
		}
		if( subjoin.getJoin().isSimple()){
			hash ^= "SIMPLE".hashCode();
			hash = Long.rotateLeft(hash, 7);
		}
		if( subjoin.getJoin().isLeft() ){
			hash ^= CCJSqlParser.K_LEFT;
			hash = Long.rotateLeft(hash, 7);
		}
		if( subjoin.getJoin().isNatural() ){
			hash ^= CCJSqlParser.K_NATURAL;
			hash = Long.rotateLeft(hash, 7);
		}
		if( subjoin.getJoin().isOuter() ){
			hash ^= CCJSqlParser.K_OUTER;
			hash = Long.rotateLeft(hash, 7);
		}
		if( subjoin.getJoin().isRight() ){
			hash ^= CCJSqlParser.K_RIGHT;
			hash = Long.rotateLeft(hash, 7);
		}
		
	}

	public void visit(LateralSubSelect lateralSubSelect) {
		// TODO Auto-generated method stub
		
	}

	public void visit(ValuesList valuesList) {
		ItemListHasher ilh = new ItemListHasher();
		valuesList.getMultiExpressionList().accept(ilh);
		hash ^= ilh.getHash();
		hash = Long.rotateLeft(hash, 7);
		
	}

}
