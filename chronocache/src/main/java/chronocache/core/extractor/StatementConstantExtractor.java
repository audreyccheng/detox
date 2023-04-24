package chronocache.core.extractor;

import java.util.LinkedList;
import java.util.List;

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

public class StatementConstantExtractor implements StatementVisitor{

	List<String> constants;
	public StatementConstantExtractor(){
		constants = new LinkedList<String>();
	}

	public void visit(Select select) {
		SelectConstantExtractor sse = new SelectConstantExtractor();
		select.getSelectBody().accept(sse);
		constants.addAll(sse.getConstants());
		
	}
	
	public List<String> getConstants(){
		return constants;
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
