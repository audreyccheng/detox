package chronocache.core.parser;

import static org.junit.Assert.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;

import org.junit.Test;

import chronocache.core.parser.AntlrParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntlrParserTest {
	private AntlrParser parser = new AntlrParser();
	private Logger logger = LoggerFactory.getLogger( this.getClass() );

	@Test
	public void simpleParse() {
		String stmt = "select a from b";
		String stmt2 = "insert into b values(1)";
		parser.buildParseTree(stmt);
		parser.buildParseTree(stmt2);
	}

	@Test(expected=ParseCancellationException.class)
	public void failToParse() {
		String stmt = "not a sql statement";
		parser.buildParseTree(stmt);
	}

	/**
	 * The parser cannot handle complicated SQL Server syntax for some updates
	 * @
	 */
	@Test(expected=ParseCancellationException.class)
	public void testComplicatedUpdate() {
		String stmt = "UPDATE u SET u.assid = s.assid FROM ud u INNER JOIN sale s ON u.id = s.udid";
		parser.buildParseTree(stmt);
	}

	@Test
	public void testParseQuery() {
		String stmt = "WITH q0 AS ( SELECT t_ca_id, t_exec_name, t_is_cash, t_trade_price, t_qty, t_dts, t_id, t_tt_id, ROW_NUMBER() OVER () AS rn FROM trade WHERE t_s_symb = 'ABV' AND t_dts >= '2005-6-7 15:46:31' AND t_dts <= '2006-2-27 9:15:0' AND t_ca_id <= 43000050000 ORDER BY t_dts ASC LIMIT 20 ), q1 AS ( SELECT se_amt, se_cash_due_date, se_cash_type, se_t_id, ROW_NUMBER() OVER () AS rn FROM settlement ) SELECT * FROM q0 LEFT JOIN q1 ON q1.se_t_id = q0.t_id";
		parser.buildParseTree( stmt );
	}

	@Test
	public void testParseQuery2() {
		String stmt = "SELECT P0.rn0 AS ordKey0, U.* FROM ( SELECT *, ROW_NUMBER() OVER () AS rn0 FROM ( SELECT s_name, co_id, co_name, co_sp_rate, co_ceo, co_desc, co_open_date, co_st_id, ca.ad_line1, ca.ad_line2, zca.zc_town, zca.zc_div, ca.ad_zc_code, ca.ad_ctry, s_num_out, s_start_date, s_exch_date, s_pe, s_52wk_high, s_52wk_high_date, s_52wk_low, s_52wk_low_date, s_dividend, s_yield, zea.zc_div, ea.ad_ctry, ea.ad_line1, ea.ad_line2, zea.zc_town, ea.ad_zc_code, ex_close, ex_desc, ex_name, ex_num_symb, ex_open FROM security, company, address ca, address ea, zip_code zca, zip_code zea, exchange  WHERE s_symb = 'ADMPRB' AND co_id = s_co_id AND ca.ad_id = co_ad_id AND ea.ad_id = ex_ad_id AND ex_id = s_ex_id AND ca.ad_zc_code = zca.zc_code AND ea.ad_zc_code = zea.zc_code ) k ) P0, LATERAL ( SELECT 0, P0.*, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM DTOneRow UNION ALL SELECT 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, I0.* FROM ( SELECT *, ROW_NUMBER() OVER (ORDER BY fi_year ASC, fi_qtr) AS rn0 FROM ( SELECT fi_year, fi_qtr, fi_qtr_start_date, fi_revenue, fi_net_earn, fi_basic_eps, fi_dilut_eps, fi_margin, fi_inventory, fi_assets, fi_liability, fi_out_basic, fi_out_dilut FROM financial WHERE fi_co_id = P0.co_id ORDER BY fi_year ASC, fi_qtr LIMIT 120 ) k) I0) U( type0, u0, u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12, u13, u14, u15, u16, u17, u18, u19, u20, u21, u22, u23, u24, u25, u26, u27, u28, u29, u30, u31, u32, u33, u34, u35, u36, u37, u38, u39, u40, u41, u42, u43 ) ORDER BY ordKey0";
		parser.buildParseTree( stmt );
	}

	@Test
	public void testSelectListAdjust() {
		String stmt = "SELECT a AS A, b FROM T";
		CharStream stream = new ANTLRInputStream( stmt );
		CaseChangingCharStream upperStream = new CaseChangingCharStream( stream, true );
		PlSqlLexer lexer  = new PlSqlLexer( upperStream );
		CommonTokenStream tokens = new CommonTokenStream( lexer );
		PlSqlParser parser = new PlSqlParser( tokens );
		List<String> colsToReplace = new LinkedList<String>() {{ add( "C" ); add( "B" ); }};
		SelectListAliasAdjuster adjuster = new SelectListAliasAdjuster( tokens, colsToReplace );
		ParseTreeWalker.DEFAULT.walk( adjuster, parser.dml_compilation_unit() );

		assertThat( "SELECT a AS C, b AS B FROM T", equalTo( adjuster.getText() ) );

	}

	@Test
	public void testSelectListAdjustBypassSubquery() {
		String stmt = "SELECT ( SELECT a, b FROM T2 ), b FROM T";
		CharStream stream = new ANTLRInputStream( stmt );
		CaseChangingCharStream upperStream = new CaseChangingCharStream( stream, true );
		PlSqlLexer lexer  = new PlSqlLexer( upperStream );
		CommonTokenStream tokens = new CommonTokenStream( lexer );
		PlSqlParser parser = new PlSqlParser( tokens );
		List<String> colsToReplace = new LinkedList<String>() {{ add( null ); add( "B" ); }};
		SelectListAliasAdjuster adjuster = new SelectListAliasAdjuster( tokens, colsToReplace );
		ParseTreeWalker.DEFAULT.walk( adjuster, parser.dml_compilation_unit() );

		assertThat( "SELECT ( SELECT a, b FROM T2 ), b AS B FROM T", equalTo( adjuster.getText() ) );
	}

	@Test
	public void testSelectListAdjustModifySubquery() {
		String stmt = "SELECT ( SELECT a, b FROM T2 ), b FROM T";
		CharStream stream = new ANTLRInputStream( stmt );
		CaseChangingCharStream upperStream = new CaseChangingCharStream( stream, true );
		PlSqlLexer lexer  = new PlSqlLexer( upperStream );
		CommonTokenStream tokens = new CommonTokenStream( lexer );
		PlSqlParser parser = new PlSqlParser( tokens );
		List<String> colsToReplace = new LinkedList<String>() {{ add( "A" ); add( null ); }};
		SelectListAliasAdjuster adjuster = new SelectListAliasAdjuster( tokens, colsToReplace );
		ParseTreeWalker.DEFAULT.walk( adjuster, parser.dml_compilation_unit() );

		assertThat( "SELECT ( SELECT a, b FROM T2 ) AS A, b FROM T", equalTo( adjuster.getText() ) );
	}
}
