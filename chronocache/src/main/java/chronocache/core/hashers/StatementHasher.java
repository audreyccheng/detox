package chronocache.core.hashers;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;

public class StatementHasher implements StatementVisitor{

	private long hash;
	public StatementHasher(){
		hash = 0;
	}

	public void visit(Select select) {
		SelectStatementHasher ssh = new SelectStatementHasher();
		select.getSelectBody().accept(ssh);
		hash ^= ssh.getHash();
		hash = Long.rotateLeft(hash, 7);
		hash ^= CCJSqlParser.K_WITH;
		hash = Long.rotateLeft(hash, 7);;
		for( WithItem wi : select.getWithItemsList() ) {
			ssh = new SelectStatementHasher();
			wi.accept(ssh);
			hash ^= ssh.getHash();
			hash = Long.rotateLeft(hash, 7);
		}
		
	}

	public void visit(Delete delete) {
		// TODO Auto-generated method stub
		
	}

	public void visit(Update update) {
		// TODO Auto-generated method stub
		
	}

	public void visit(Insert insert) {
		// TODO Auto-generated method stub
		
	}

	public void visit(Replace replace) {
		// TODO Auto-generated method stub
		
	}

	public void visit(Drop drop) {
		// TODO Auto-generated method stub
		
	}

	public void visit(Truncate truncate) {
		// TODO Auto-generated method stub
		
	}

	public void visit(CreateIndex createIndex) {
		// TODO Auto-generated method stub
		
	}

	public void visit(CreateTable createTable) {
		// TODO Auto-generated method stub
		
	}

	public void visit(CreateView createView) {
		// TODO Auto-generated method stub
		
	}

	public void visit(Alter alter) {
		// TODO Auto-generated method stub
		
	}

	public void visit(Statements stmts) {
		// TODO Auto-generated method stub
		
	}
}
