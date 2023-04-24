package chronocache.core.parser;

import java.util.Iterator;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.Top;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;

public class SelectDeparserNoSelList extends SelectDeParser{
	
	private SelectDeParser subDeparser;
	
	public SelectDeparserNoSelList(ExpDeparser expDeparser, SelectDeParser subDeParser) {
		super(expDeparser, subDeParser.getBuffer());
		this.subDeparser = subDeParser; 
	}

	@Override
	public void visit(PlainSelect plainSelect) {
		this.getBuffer().append("SELECT ");
		Top top = plainSelect.getTop();
		if (top != null) {
			this.getBuffer().append(top).append(" ");
		}
		if (plainSelect.getDistinct() != null) {
			this.getBuffer().append("DISTINCT ");
			if (plainSelect.getDistinct().getOnSelectItems() != null) {
				this.getBuffer().append("ON (");
				for (Iterator<SelectItem> iter = plainSelect.getDistinct().getOnSelectItems().iterator(); iter.hasNext();) {
					SelectItem selectItem = iter.next();
					selectItem.accept(this);
					if (iter.hasNext()) {
						this.getBuffer().append(", ");
					}
				}
				this.getBuffer().append(") ");
			}

		}
		//NO SELECT ITEMS

		for (Iterator<SelectItem> iter = plainSelect.getSelectItems().iterator(); iter.hasNext();) {
			SelectItem selectItem = iter.next();
			selectItem.accept(new SelectDeParser(new ExpressionDeParser(new SelectDeParser(), getBuffer()), getBuffer()));
			if (iter.hasNext()) {
				this.getBuffer().append(", ");
			}
		}

		if (plainSelect.getFromItem() != null) {
			this.getBuffer().append(" FROM ");
			plainSelect.getFromItem().accept(this);
		}

		if (plainSelect.getJoins() != null) {
			for (Join join : plainSelect.getJoins()) {
				deparseJoin(join);
			}
		}

		if (plainSelect.getWhere() != null) {
			this.getBuffer().append(" WHERE ");
			plainSelect.getWhere().accept(getExpressionVisitor());
		}

		if (plainSelect.getOracleHierarchical() != null) {
			plainSelect.getOracleHierarchical().accept(getExpressionVisitor());
		}

		if (plainSelect.getGroupByColumnReferences() != null) {
			this.getBuffer().append(" GROUP BY ");
			for (Iterator<Expression> iter = plainSelect.getGroupByColumnReferences().iterator(); iter.hasNext();) {
				Expression columnReference = iter.next();
				columnReference.accept(getExpressionVisitor());
				if (iter.hasNext()) {
					this.getBuffer().append(", ");
				}
			}
		}

		if (plainSelect.getHaving() != null) {
			getBuffer().append(" HAVING ");
			plainSelect.getHaving().accept(getExpressionVisitor());
		}

		if (plainSelect.getOrderByElements() != null) {
			deparseOrderBy(plainSelect.isOracleSiblings(), plainSelect.getOrderByElements());
		}

		if (plainSelect.getLimit() != null) {
			deparseLimit(plainSelect.getLimit());
		}

	}
}
