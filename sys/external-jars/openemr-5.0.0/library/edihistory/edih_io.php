<?php
/*
 * edih_io.php
 * 
 * Copyright 2016 Kevin McCormick Longview, Texas
 * 
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 or later.  You should have 
 * received a copy of the GNU General Public License along with this program; 
 * if not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *  <http://opensource.org/licenses/gpl-license.php>
 * 
 * 
 * @author Kevin McCormick
 * @link: http://www.open-emr.org
 * @package OpenEMR
 * @subpackage ediHistory
 */

/**
 * jQuery adds a special HTTP header for ajax requests
 * 
 * @return bool
 */
function is_xhr() {
  return @ $_SERVER[ 'HTTP_X_REQUESTED_WITH' ] === 'XMLHttpRequest';
}

/**
 * Get some values from php ini functions for interface
 * 
 * @return array     json
 */
function edih_php_inivals() {
    $ival = array();
    $td = basename(sys_get_temp_dir());
    $ival['maxfsize'] = ini_get('upload_max_filesize');
    $ival['maxfuploads'] = ini_get('max_file_uploads');
    $ival['postmaxsize'] = ini_get('post_max_size');
    $ival['tmpdir'] = $td;
    $json = json_encode($ival);
    //
    return $json;
}

/**
 * display the log file selected
 * 
 * @uses csv_log_html()
 * @return string
 */
function edih_disp_log() {
	$lfn = '';
	if ( isset($_GET['log_select']) ) {
		$lfn = filter_input(INPUT_GET, 'log_select', FILTER_SANITIZE_STRING);
	}
	$str_html = csv_log_html($lfn);
	return $str_html;
}

function edih_disp_logfiles() {
	$str_html = '';
	$lst = true;
	if ( isset($_GET['loglist']) ) {
		// loglist: 'yes'
		$lval = filter_input(INPUT_GET, 'loglist', FILTER_SANITIZE_STRING);
		$lst = ($lval == 'yes') ? true : false;
	} elseif ( isset($_GET['archivelog']) ) {
		// archivelog: 'yes'
		$lval = filter_input(INPUT_GET, 'archivelog', FILTER_SANITIZE_STRING);
		$lst = ($lval == 'yes') ? false : true;
	} else {
		csv_edihist_log('edih_disp_logfiles: input parameter error');
		return "input parameter error<br />";
	}
	// returns json array
	$str_html = csv_log_manage($lst);
	return $str_html;
}
/**
 * read or write simple notes to a text file
 * 
 * @uses csv_notes_file()
 * @return string
 */
function edih_user_notes() {
	//
	if ( isset($_GET['getnotes']) ) {
		$getnt = filter_input(INPUT_GET, 'getnotes', FILTER_SANITIZE_STRING);
		if ($getnt == 'yes') {
			$str_html = csv_notes_file();
		}
	} elseif ( isset($_POST['notes_hidden']) && isset($_POST['txtnotes']) ) {
		$putnt = filter_input(INPUT_POST, 'putnotes', FILTER_SANITIZE_STRING);
		if ($putnt == 'yes') {
			$notetext = trim($_POST['txtnotes']);
			$filtered = filter_var($notetext, FILTER_SANITIZE_STRING);
			//echo $filtered .PHP_EOL;
			$str_html = csv_notes_file($filtered, false);
		}
	} else {
		csv_edihist_log('edih_user_notes: invalid values in request');
		$str_html = "<p>User Notes: invalid values in request.</p>";
	}
	return $str_html;
}

/**
 * generate the heading string for an html page
 * 
 * @return string     html heading stanza
 */
function edih_html_heading($option, $title='') {
	//
	//if (!is_string($title)) { $title=''; }
    $title = (is_string($title)) ? $title : '';
    //$srcdir = $GLOBALS['srcdir'];
    $webdir = $GLOBALS['webroot'];
    $vendordir = $GLOBALS['assets_static_relative'];
    
	$str_html = "<!DOCTYPE html>".PHP_EOL."<html>".PHP_EOL."<head>".PHP_EOL;
	$str_html .= " <meta http-equiv='content-type' content='text/html;charset=utf-8' />".PHP_EOL;
	$str_html .= " <title>##TITLE##</title>".PHP_EOL;
	//$str_html .= " <link rel='stylesheet' href='jscript/style/csv_new.css' type='text/css' media='print, projection, screen' />".PHP_EOL;
	//$str_html .= " <link rel='stylesheet' href='../css/edi_history.css' type='text/css' />".PHP_EOL;
    $str_html .= " <link rel='stylesheet' href='$webdir/library/css/edi_history.css' type='text/css' />".PHP_EOL;
    $str_html .= " <link type='text/javascript' src='$vendordir/jquery-min-1-9-1/index.js'  />".PHP_EOL;
    $str_html .= " <link type='text/javascript' src='$webdir/library/js/jquery-ui-1.8.21.custom.min.js'  />".PHP_EOL;
    //
	$str_html .= "</head>".PHP_EOL."<body>".PHP_EOL;
		
	if ($option == 'newfiles') {
		$str_html = str_replace('##TITLE##', 'Process New Files '.$title, $str_html);
	} elseif ($option == 'eradisplay') {
		$str_html = str_replace('##TITLE##', 'ERA Display '.$title, $str_html);
	} elseif ($option == 'claimstatus') {
		$str_html = str_replace('##TITLE##', 'Claim Status '.$title, $str_html);
	} elseif ($option == 'eligibility') {
		$str_html = str_replace('##TITLE##', 'Eligiility '.$title, $str_html);
	} elseif ($option == 'authorization') {
		$str_html = str_replace('##TITLE##', 'Authorization '.$title, $str_html);
	} elseif ($option == 'x12display') {
		$str_html = str_replace('##TITLE##', 'x12 File '.$title, $str_html);
	} elseif ($option == 'csvtable') {
		$str_html = str_replace('##TITLE##', 'CSV Table '.$title, $str_html);
	} elseif ($option == 'textdisplay') {
		$str_html = str_replace('##TITLE##', 'Text '.$title, $str_html);
	} elseif ($option == 'readme') {
		$str_html = str_replace('##TITLE##', 'Readme '.$title, $str_html);
	} else {
		$str_html = str_replace('##TITLE##', 'OEMR edi_history '.$title, $str_html);
	}
	//
	return $str_html;
}

/**
 * generate the trailing tags for html page
 * 
 * @return string
 */
function edih_html_tail() {
	$str_html = PHP_EOL."</body></html>";
	return $str_html;
}


/**
 * Restore an existing archive
 * 
 * @uses edih_archive_restore()
 * 
 * @return string
 */
function edih_disp_archive_restore() {
	//name="archrestore_sel" { archrestore: 'yes', archfile: archf };
	$fn = (isset($_POST['archrestore_sel'])) ? filter_input(INPUT_POST, 'archrestore_sel', FILTER_SANITIZE_STRING) : '';
	if (strlen($fn)) {
		$str_html = edih_archive_restore($fn);
	} else {
		$str_html = "<p>Invalid archive name for archive resstore function</p>".PHP_EOL;
	}
	return $str_html;
}

/**
 * Create a report on edi files and csv tables
 * 
 * @uses edih_archive_report()
 * 
 * @return string
 */
function edih_disp_archive_report() {
	//
	$str_html = '';
	$la = filter_input(INPUT_GET, 'archivereport', FILTER_SANITIZE_STRING);
	$pd = (isset($_GET['period'])) ? filter_input(INPUT_GET, 'period', FILTER_SANITIZE_STRING) : '';
	//
	csv_edihist_log("GET archivereport:  archivereport $la period $pd");
	//
	if ($la == 'yes') {
		$str_html = edih_archive_report($pd);
	} else {
		$str_html = "File Information report input parameter error<br>";
	}
	//
	return $str_html;
}
	
		
/**
 * Archive of old edi files
 * 
 * @uses edih_archive_main()
 * 
 * @return string
 */
function edih_disp_archive() {
	//
	$pd = (isset($_POST['archive_sel'])) ? filter_input(INPUT_POST, 'archive_sel', FILTER_SANITIZE_STRING) : '';
	//
	if ($pd) {
		$str_html = edih_archive_main($pd);
	} else {
		$str_html = "<p>Invalid aging period for archive function</p>".PHP_EOL;
	}
	return $str_html;
}

/**
 * call new uploaded files process functions
 * 
 * @todo    save the newfiles lists to file so they can
 *          be re-displayed if user has to close app before
 *          finishing review (need to have csv_write option)
 * 
 * @uses csv_newfile_list()
 * @uses edih_parse_select()
 * @uses edih_csv_write()
 * @uses edih_csv_process_html()
 * 
 * @return string  html format
 */
function edih_disp_file_process() {
	//file "fileUplMulti "submit" "uplsubmit" "button""uplreset"
	//
	// debug
	if (isset($_GET)) {
		$dbg_str = 'GET vars ';
		foreach($_GET as $k=>$v) {
			$dbg_str .= $k.' '.$v.'  ';
		}
		csv_edihist_log("edih_disp_file_process $dbg_str");
	}
	//
	if (!isset($_GET['ProcessFiles']) ) {
		// should only be called with this value existing
		$str_html = "Error: invalid value for Process New <br />".PHP_EOL;
		return $str_html;
	}
	$htm = $er = false;
	if (isset($_GET['process_html'])) {
		// show tables for process results
		$htmval = filter_input(INPUT_GET, 'process_html', FILTER_SANITIZE_STRING);
		$htm = ($htmval == 'htm') ? true : false;
	}
	if (isset($_GET['process_err'])) {
		// show only claims with errors (denied, rejected, etc)
		$errval = filter_input(INPUT_GET, 'process_err', FILTER_SANITIZE_STRING);
		$er = ($errval == 'err') ? true : false;
	}
	$str_html = "";
	//
    $p = csv_parameters();
    $ftype = array_keys($p);
    $fct = 0;
    //
	foreach($ftype as $tp) {
        $checkdir = false;
        // check for directory contents
        $fdir = $p[$tp]['directory'];
        if (is_dir($fdir)) {
            $dh = opendir($fdir);
            if ($dh) {
                while (($file = readdir($dh)) !== false) {
                    if ($file != '.' && $file != '..') {
                        $checkdir = true;
                        break;
                    }
                }
                closedir($dh);
            }
        }
        // if false, no files in directory
        if (!$checkdir) { continue; }
        //
		$upload_ar = csv_newfile_list($tp);
		//
		if ( is_array($upload_ar) && count($upload_ar) ) {
			$dirct = count($upload_ar);
			if ($htm) {
				$dtl = ($er) ? "(claims: errors only)" : "(claims: all)";
				//$hvals = csv_table_header($tp, 'file');
				//$col_ct = count($hvals);
				////csv_table_header(
				//$str_html .= "<table class='$tp' cols=$col_ct><caption>$tp Files Summary $dtl</caption>".PHP_EOL;
				//$str_html .= csv_thead_html($tp, 'file');
				//$str_html .= "<tbody>".PHP_EOL;
				$str_html .= "<h2 class='prcs'>$tp $dirct files $dtl</h2>".PHP_EOL;
				$str_html .= "<dl class='$tp'>".PHP_EOL;
			}
			foreach($upload_ar as $fn) {
				$fp = $fdir.DS.$fn;
				$csvdata = edih_parse_select($fp);
				$csvchr = edih_csv_write($csvdata);
				$fct++;
				if ($htm) {
					$str_html .= edih_csv_process_html($csvdata, $er);
				}
			}
			//$str_html .= ($htm) ? "</tbody>".PHP_EOL."</table>".PHP_EOL : "";
			$str_html .= ($htm) ? "</dl>".PHP_EOL : "";
		} else {
			$str_html .= "<p>No new $tp files</p>";
		}
	}
	$capt_html = "<p>Process new files ($fct files)</p>".PHP_EOL;
	return $capt_html . $str_html;
}

/**
 * uploading of new files
 * 
 * @uses edih_upload_files()
 * @uses edih_sort_upload()
 * @return string
 */
function edih_disp_file_upload() {
	// multiple file upload
	$str_html = '';
	if ( isset($_FILES) && count($_FILES) ) {
		$f_array = edih_upload_files();
		if ( is_array($f_array) && count($f_array) ) {
			$str_html .= edih_sort_upload($f_array);
		} else {
			$str_html .= "no files accepted <br />".PHP_EOL;
		}
	} else {
		$str_html .= "no files submitted <br />" . PHP_EOL;
	}
	//
	return $str_html;
}

function edih_disp_denied_claims() {
	//
	$fn = isset($_GET['fname']) ? filter_input(INPUT_GET, 'fname', FILTER_SANITIZE_STRING) : '';
	$ft = isset($_GET['ftype']) ? filter_input(INPUT_GET, 'ftype', FILTER_SANITIZE_STRING) : '';
	$trace = isset($_GET['trace']) ? filter_input(INPUT_GET, 'trace', FILTER_SANITIZE_STRING) : '';
	//
	$str_html = edih_list_denied_claims($ft, $fn, $trace);
	//
	return $str_html;
}

/**
 * display the contents of an x12_edi transaction selected from
 * a csv table or processed files table
 * 
 * @uses csv_file_by_enctr()
 * @uses csv_file_by_controlnum()
 * @uses ibr_batch_get_st_block()
 * @return string
 */
function edih_disp_x12trans() {
	//
	// query source ['gtbl']  file claim hist
	//
	// file: FileName  fname=$fn1&ftype=$ft&fmt=htm'                  filename x12type format
	// file: Control   fname=$fn1&ftype=$ft&icn=$ctl&fmt=seg         filename x12type isa13 format
	// file: Trace     trace=$tr&ftype=$typ&fmt=htm                  trace x12type format
	//
	// claim: FileName fname=$fn1&ftype=$ft&fmt=htm                      filename x12type format:html 
	// claim: Control  fname=$fn1&ftype=$ft&icn=$ctl&fmt=seg             filename x12type icn  format:segment text 
	// claim: CLM01    fname=$fn1&ftype=$ft&pid=$pid                     filename x12type pid-enctr  
	// claim: Status   fname=fname=$fn1&ftype=$ft&pid=$pid&summary=yes    filename x12type pid-enctr summary
	// claim: Status   fname=$fn1&ftype=$ft&pid=$pid&summary=no'          filename x12type pid-enctr detail
	// claim: Trace  (835)  fname=$fn1&ftype=$ft&trace=$trc               trace filename x12type
	// claim: Trace  (999)  trace=$trc&rsptype=$typ&ftype=$ft             trace(bht03syn) response-type x12type 
	// claim: Trace  (277)  trace=$v&ftype=$tp&rsptype=f837&fmt=seg'    trace(clm01) response-type {837) x12type
	// claim: BHT03  (27x)  fname=$fn1&ftype=$ft&bht03=$bht03&fmt=htm          filename x12type bht03 
	// claim: err_seg  fname=$fn1&ftype=$ft&trace=$trc&rsptype=$typ&err=$err   filename x12type trace(bht03syn) response_type error_segment
	//
	// use files (1) x12 display of file segments (2) 835 html RA or Payment Trace (3) trace from 997 or 271/277/278
	//					$fn or $icn	& $ft						$fn $icn  $trace & $ft    $trace & $rsptype
	//    claims (1) html of transaction (2) segments of transaction (3) trace to precedent transaction
	//					$fn	& $ft $ pid										$trace & $rsptype
	//
	$str_htm = '';
	if ( isset($_GET['gtbl']) ) {
		$qs = filter_input(INPUT_GET, 'gtbl', FILTER_SANITIZE_STRING);
	}
	if (!$qs) {
		$str_htm .= '<p>edih_disp_x12 error: missing parameter</p>';
		csv_edihist_log("edih_io_disp_x12: missing parameter, no 'gtbl' value");
		return $str_htm;
	}
	//
	$fmt = isset($_GET['fmt']) ? filter_input(INPUT_GET, 'fmt', FILTER_SANITIZE_STRING) : '';
	//	
	$fn = isset($_GET['fname']) ? filter_input(INPUT_GET, 'fname', FILTER_SANITIZE_STRING) : '';
	$ft = isset($_GET['ftype']) ? filter_input(INPUT_GET, 'ftype', FILTER_SANITIZE_STRING) : '';
	$icn = isset($_GET['icn']) ? filter_input(INPUT_GET, 'icn', FILTER_SANITIZE_STRING) : '';
	$rsptype = isset($_GET['rsptype']) ? filter_input(INPUT_GET, 'rsptype', FILTER_SANITIZE_STRING) : '';
	//
	$clm01 = isset($_GET['pid']) ? filter_input(INPUT_GET, 'pid', FILTER_SANITIZE_STRING) : '';
	$trace = isset($_GET['trace']) ? filter_input(INPUT_GET, 'trace', FILTER_SANITIZE_STRING) : '';
	$bht03 = isset($_GET['bht03']) ? filter_input(INPUT_GET, 'bht03', FILTER_SANITIZE_STRING) : '';
	$err = isset($_GET['err']) ? filter_input(INPUT_GET, 'err', FILTER_SANITIZE_STRING) : '';
	$summary = isset($_GET['summary']) ? filter_input(INPUT_GET, 'summary', FILTER_SANITIZE_STRING) : false;
	//
	// debug
	//$str_htm .= "<p>edih_disp_x12trans values: <br>".PHP_EOL;
	//$str_htm .= "qs $qs fmt $fmt fn $fn ft $ft icn $icn rsptype $rsptype clm01 $clm01 trace $trace bht03 $bht03 err $err summary $summary</p>".PHP_EOL;
	//
	if ($ft) { $ft = csv_file_type($ft); }
	//
	if ($qs == 'claim') {
		if ($ft == 'f997') {
			if ($trace && $rsptype){
				$fname = csv_file_by_trace($trace, $ft, $rsptype);
				if ($fname) {
					$str_htm .= edih_display_text($fname, $rsptype, $trace, $err);
				} else {
					$str_htm .= "<p>Did not find $trace in the $rsptype claims table.</p>";
				}
				//$fnar = csv_file_by_enctr($trace, $rsptype, $srchtype='ptidn' );
				//if (is_array($fnar) && count($fnar)) {
					//foreach($fnar as $fa) {
						//$fname = $fa['FileName'];
						//$str_htm .= edih_display_text($fname, $rsptype, $trace, $err);
					//}
				//} else {
					//$str_htm .= "<p>Did not find $trace in the $rsptype claims table.</p>";
				//}
			}
		} elseif ($ft == 'f837') {
			// either transaction or file
			$str_htm .= edih_display_text($fn, $ft, $clm01);
		} elseif ($ft == 'f835') {
			if ($fmt == 'seg') {
				// either transaction or file
				$str_htm .= edih_display_text($fn, $ft, $clm01);
			} elseif ($trace) {
				// the check trace
				$str_htm .= edih_835_html($fn, $trace);
			} elseif ($clm01) {
				// this claim payment
				$str_htm .= edih_835_html($fn, '', $clm01, $summary);
			}
		} elseif ( strpos('|f270|f271|f276|f277|f278', $ft) ) {
			if ($fmt == 'seg') {
				if ($trace && $rsptype) {
					// 270|276|278|837 claim or request segments
					// here the 'trace' is from trace or clm01
					//$fnar = csv_file_by_enctr($trace, $rsptype, $srchtype='ptidn' );
					//if (is_array($fnar) && count($fnar)) {
						//foreach($fnar as $fa) {
							//$fname = $fa['FileName'];
							//$str_htm .= edih_display_text($fname, $rsptype, $trace);
						//}
					$fname = csv_file_by_trace($trace, $ft, $rsptype);
					if ($fname) {
						$str_htm .= edih_display_text($fname, $rsptype, $trace);
					} else {
						$str_htm .= "<p>Did not find $trace in type $rsptype csv_claims table</p>".PHP_EOL;
						csv_edihist_log("edih_disp_x12trans: Did not find $trace in type $rsptype csv_claims table");
					}
				} else {
					// entire file or transaction if bht03 has a value
					$str_htm .= edih_display_text($fn, $ft, $bht03);
				}
			} else {
				// html format
				if ($ft == 'f277') {
					$str_htm .= edih_277_html($fn, $bht03);
				} elseif ($ft == 'f271') {
					$str_htm .= edih_271_html($fn, $bht03);
				} elseif  ($ft == 'f278') {
					$str_htm .= edih_278_html($fn, $bht03);
				} else {
					// html display not available, use segments
					$str_htm .= edih_display_text($fn, $ft, $bht03);
				}
			}
		}
	} elseif ($qs == 'hist') {
		if ($fn && $ft == 'f837') {
			if ($clm01) {
				$str_htm .= edih_display_text($fn, $ft, $clm01);
			} else {
				$str_htm .= edih_display_text($fn, $ft);
			}
		} elseif ($fn && $ft == 'f997') {
			if ($trace && $rsptype && $err) {
				$str_htm .= edih_display_text($fn, $rsptype, $trace, true, $err);
			} elseif ($trace && $rsptype) {
				$str_htm .= edih_display_text($fn, $rsptype);
			} else {
				$str_htm .= edih_display_text($fn, $ft);
			}
		} elseif ($fn && $ft == 'f277') {
			if ($trace && $rsptype) {
				$fname = csv_file_by_trace($trace, $ft, $rsptype);
				if ($fname) {
					$str_htm .= edih_display_text($fname, $rsptype, $trace);
				} else {
					$str_htm .= "<p>Did not find $trace in type $rsptype csv_claims table</p>".PHP_EOL;
					csv_edihist_log("edih_disp_x12trans: Did not find $trace in type $rsptype csv_claims table");
				}
			} elseif ($clm01) {
				$str_htm .= edih_277_html($fn, $clm01);
			} elseif ($bht03) {
				$str_htm .= edih_277_html($fn, $bht03);
			} else {
				$str_htm .= edih_display_text($fn, $ft);
			}
		} elseif ($fn && $ft == 'f835') {
			if ($clm01) {
				if ($summary == 'yes') {
					$str_htm .= edih_835_html($fn, '', $clm01, true);
				} else {
					$str_htm .= edih_835_html($fn, '', $clm01);
				}
			} elseif ($trace) {
				$str_htm .= edih_835_html($fn, $trace);
			}
		}
	} else {
		$str_htm .= 'error: could not process request.';
	}

	return $str_htm;
}

/**
 * display fule uploaded from x12 File tab
 * wrap individual transactions in accordian jquery ui widget
 *
 * @uses csv_check_x12_obj()
 * @uses edih_html_heading()
 * @uses edih_271_transaction_html()
 * @uses edih_278_transaction_html()
 * @uses edih_277_transaction_html()
 * @uses edih_835_html_page()
 * @uses edih_display_text()
 *
 * @param string  path to x12 file
 * @return string
 */
function edih_disp_x12file() {
	//
	$str_htm = '';
	$fn = $ft = $icn = $trace = $rsptype = $format = '';
	//
	if ( isset($_POST['x12_html']) ) {
		$htmval = filter_input(INPUT_POST, 'x12_html', FILTER_SANITIZE_STRING);
		$format = ($htmval == 'html') ? 'htm' : 'seg';
		$upldir = csv_edih_tmpdir();
	} else {
		$format = 'seg';
	}
	// post request from x12 file tab
	if ( count($_FILES) && isset($_FILES['fileUplx12']) ) {
		$fnupl = htmlentities($_FILES['fileUplx12']['name']);
		// the upload files validator
		$f_array = edih_upload_files();
		//		
		if ( is_array($f_array) && count($f_array) ) {
			// was file rejected?
			if ( isset($f_array['reject']) ) {
				$fn = (count($f_array['reject'][0])) ? $f_array['reject'][0]['name'] : '';
				$cmt = (count($f_array['reject'][0])) ? $f_array['reject'][0]['comment'] : '';
				//$str_html = edih_html_heading('error');
				$str_htm .= "<p>Rejected file:</p>".PHP_EOL;
				$str_htm .= "<p>$fn<br>".PHP_EOL;
				$str_htm .= " -- $cmt</p>".PHP_EOL;
				//
				csv_edihist_log("edih_disp_x12file: rejected file $fn comment: $cmt");
				//
				return $str_htm;
			} else {
				$fnar = reset($f_array);  // type filename array
				$ft = key($f_array);    			// type
				$fn1 = $f_array[$ft][0];   //$upldir.DS.
				$fn = csv_check_filepath($fn1);
				csv_edihist_log("edih_disp_x12file: submitted POST $format $ft $fn1 $fnupl");
				//
				if (!$fn) {
					//$str_htm = edih_html_heading('error');
					$str_htm .= "<p>Path error for $fn1</p>" . PHP_EOL;
					csv_edihist_log("edih_disp_x12file: Path error for $fn1");
					return $str_htm;
				}
			}
		} else {
			//$str_htm = edih_html_heading('error');
			$str_htm .= "<p>File not accepted $fnupl</p>" . PHP_EOL;
			csv_edihist_log("edih_disp_x12file: File not accepted $fnupl");
			return $str_htm;
		}
	} elseif ( isset($_GET['gtbl']) && $_GET['gtbl'] == 'file') {
		// this is a GET request from csv files table
		// assemble variables
		$fn = isset($_GET['fname']) ? filter_input(INPUT_GET, 'fname', FILTER_SANITIZE_STRING) : '';
		$ft = isset($_GET['ftype']) ? filter_input(INPUT_GET, 'ftype', FILTER_SANITIZE_STRING) : '';
		$icn = isset($_GET['icn']) ? filter_input(INPUT_GET, 'icn', FILTER_SANITIZE_STRING) : '';
		$trace = isset($_GET['trace']) ? filter_input(INPUT_GET, 'trace', FILTER_SANITIZE_STRING) : '';
		$rsptype = isset($_GET['rsptype']) ? filter_input(INPUT_GET, 'rsptype', FILTER_SANITIZE_STRING) : '';
		$format = isset($_GET['fmt']) ? filter_input(INPUT_GET, 'fmt', FILTER_SANITIZE_STRING) : '';
		//
	} else {
		//$str_htm = edih_html_heading('error');
		$str_htm .= "<p>Error: No request received by server</p>" . PHP_EOL;
		csv_edihist_log("edih_disp_x12file: No request received by server");
		return $str_htm;
	}
	//
	if (!$fn) {
		if ($ft && $icn) {
			$fnr = csv_file_by_controlnum($ft, $icn);
			$fn = csv_check_filepath($fnr);
		} elseif ($ft && $trace && $rsptype) {
			$fnr = csv_file_by_trace($trace, $ft, $rsptype);
			$fn = csv_check_filepath($fnr);
			$ft = $rsptype;
			$trace = '';
		} elseif ($ft == 'f835' && $trace) {
			$fnr = csv_file_by_trace($trace, $ft, $rsptype);
			$fn = csv_check_filepath($fnr);
		} elseif ($ft == 'f997' && $trace && $rsptype) {
			$fnr = csv_file_by_controlnum($rsptype, $trace);
			$fn = csv_check_filepath($fnr);
			$ft = $rsptype;
			$trace = '';
			if (!$fn) {
				$str_htm .= "<p>997/999 Trace value $trace not found for type $rsptype</p>" . PHP_EOL;
				csv_edihist_log("edih_disp_x12file: 997/999 Trace value $trace not found for type $rsptype");
				return $str_htm;
			}
		}
	}

	if (!$fn) {
		//$str_htm = edih_html_heading('error');
		$str_htm .= "<p>Name error for file: type $ft icn $icn trace $trace rsp $rsptype</p>" . PHP_EOL;
		csv_edihist_log("edih_disp_x12file: Name error for file: type $ft icn $icn trace $trace rsp $rsptype");
		return $str_htm;
	}
	//
	if ( $format == 'seg' ) {
		if ($ft == 'f835' && $trace) {
			$str_htm .= edih_display_text($fn, $ft, $trace, true);
		} elseif ($icn) {
			$str_htm .= edih_display_text($fn, $ft, $icn, true);
		} else {
			$str_htm .= edih_display_text($fn, $ft);
		}
		csv_edihist_log("edih_disp_x12file: segments display $fn");
		//
		return $str_htm;
	}
	// request is for html display
	// now go through each file type
	// 'HB'=>'271', 'HS'=>'270', 'HR'=>'276', 'HI'=>'278','HN'=>'277',
	// 'HP'=>'835', 'FA'=>'999', 'HC'=>'837');
	if ($ft == 'f271' || $ft == 'f270') {
		//$str_htm .= edih_html_heading('eligibility', $fn);
		$str_htm .= edih_271_html($fn);
		//$str_htm .= "</body>".PHP_EOL."</html>".PHP_EOL;
	} elseif ($ft == 'f276' || $ft == 'f277') {
		//$str_htm .= edih_html_heading('claimstatus', $fn);
		$str_htm .= edih_277_html($fn);
		//$str_htm .= "</body>".PHP_EOL."</html>".PHP_EOL;
	} elseif ($ft == 'f278') {
		//$str_htm .= edih_html_heading('claimstatus', $fn);
		$str_htm .= edih_278_html($fn);
		//$str_htm .= "</body>".PHP_EOL."</html>".PHP_EOL;
	} elseif ($ft == 'f835') {
		//$str_htm .= edih_html_heading('eradisplay', $fn);
		$str_htm = edih_835_html($fn, $trace);
		//$str_htm .= "</body>".PHP_EOL."</html>".PHP_EOL;
	} else {
		// no html format for this type 
		// object is created in edih_display_text function
		// edih_display_text($filepath, $filetype='', $claimid='', $trace=false, $err_info='') 
		//$str_htm .= edih_html_heading('x12display', $fn);
		$str_htm .= edih_display_text($fn, $ft);
		//$str_htm .= "</body>".PHP_EOL."</html>".PHP_EOL;
	}
	//		
	return $str_htm;
}


/**
 * csv tables filter input and generate table
 * 
 * @uses csv_to_html()
 * @return string
 */
function edih_disp_csvtable() {
	//
	$str_html = '';
	$prd = (isset($_GET['csv_period'])) ? filter_input(INPUT_GET, 'csv_period', FILTER_SANITIZE_STRING) : '';
	$dts = (isset($_GET['csv_date_start'])) ? filter_input(INPUT_GET, 'csv_date_start', FILTER_SANITIZE_NUMBER_INT) : '';
	$dte = (isset($_GET['csv_date_end'])) ? filter_input(INPUT_GET, 'csv_date_end', FILTER_SANITIZE_NUMBER_INT) : '';
	$csvfile = (isset($_GET['csvtables'])) ? filter_input(INPUT_GET, 'csvtables', FILTER_SANITIZE_STRING) : '';
	//
	// debug
	csv_edihist_log("edih_disp_csvtable: $csvfile period $prd datestart $dts dateend $dte");
	//
	if ( $dts && strpos($dts, '-') != 4 ) {
		if ( strlen($_GET['csv_date_start']) == 10 && strpos($_GET['csv_date_start'], '/') == 4 ) {
			$dts = str_replace('/', '-', $dts);
		} else {
			$str_html = "<p>Date $dts must be in YYYY-MM-DD format, no / or . please</p>".PHP_EOL;
			csv_edihist_log("invalid date $dts submitted for csv_table filter");
			return $str_html;
		}
	}
	if ( $dte && strpos($dte, '-') != 4 ) {
		if ( strlen($_GET['csv_date_end']) == 10 && strpos($_GET['csv_date_end'], '/') == 4 ) {
			$dte = str_replace('/', '-', $dte);
		} else {
			$dte = '';
		}
	}
	if ( !$csvfile || $csvfile == NULL || $csvfile === FALSE ) {
		// here we have an error and must quit
		$str_html = "<p>Error in CSV table name </p>".PHP_EOL;
		return $str_html;
	} else {
		$tp_ar = explode('_', $csvfile);
		$tbl_type = ($tp_ar[0] == 'claims') ? 'claim' : 'file';
		$f_type = strval($tp_ar[1]);
		if ( ctype_digit($f_type) ) {
			$f_type = 'f'.$f_type;
		}
	}
	$str_html = edih_csv_to_html($f_type, $tbl_type, $prd, $dts, $dte);
	//
	return $str_html;
}


/**
 * Report 835 file as processed by billing routine if the file name
 * is found in the 'era' directory.  The file name is a concatenation
 * of GS04_TRN04_ISA13.edi per parse_era_inc.php
 *
 * 
 * @param string
 * @return bool
 */
function edih_disp_835_processed($erasavename) {
	// openemr/interface/billing/era_payments.php
	// openemr/library/parse_era.inc.php
	//  OpenEMR filename for era should be just the upload filename or 
	//  $out['gs_date'] . '_' . $out['payer_id'] . '_' .$out['isa_control_number']
	//  with 'payer_id' taken from BPR10 or TRN03 (same value) and not from TRN04
	//
	// search for YYYYMMDD_NNNNNNNN_ISA13
	$eraname = $out['gs_date'] . '_' . ltrim($out['isa_control_number'], '0') .
    '_' . ltrim($out['payer_id'], '0');
    $out['payer_id'] = trim($seg[4]);  //TRN04
    //
    $srchlen = strlen($erasavename);
    $found = false;
    $eradir = $GLOBALS['OE_SITE_DIR'].DS.'edi'.DS.'era';
    //
    if ($hd = opendir($eradir)) {
		while (false !== ($entry = readdir($hd))) {
			if (strncmp($entry, $erasavename, $srchlen) === 0 ) {
				$found = true;
				break;
			}
		}
		closedir($hd);
    } else {
		csv_edihist_log("edih_disp_processed_835: did not find processed era directory");
	}
    return $found;
}

function edih_disp_clmhist() {
	//
	if ( isset($_GET['hist_enctr']) ) {
		$enctr = filter_input(INPUT_GET, 'hist_enctr', FILTER_SANITIZE_STRING);
		if ($enctr) {
			$str_html = edih_claim_history($enctr);
		} else {
			$str_html = "Invalid or unknown encounter number".PHP_EOL;
		}
	} else {
		$str_html = "Invalid or unknown encounter number".PHP_EOL;
	}
	return $str_html;
}


/**
 * display the message part of a 999 response
 * 
 * @uses ibr_997_errscan()
 * @return string
 */
function ibr_disp_997_message() {
	//
	$fname = ''; $akval = ''; $errval = '';
	$fname = filter_input(INPUT_GET, 'fv997', FILTER_SANITIZE_STRING);
	if (isset($_GET['aknum'])) { $akval = filter_input(INPUT_GET, 'aknum', FILTER_SANITIZE_STRING); }
	if (isset($_GET['err997'])) { $errval = filter_input(INPUT_GET, 'err997', FILTER_SANITIZE_STRING); }
	if (!$fname) {
		$str_html = "Missing file name.<br />".PHP_EOL;
	} else {
		$str_html = ibr_997_errscan($fname, $akval);
	}
	return $str_html;
}

/**
 * display the message part of a ACK or TA1 response
 * 
 * @uses ibr_ack_error()
 * @return string
 */
function ibr_disp_ta1_message() {
	//
	$fname = ''; $code = '';
	$fname = filter_input(INPUT_GET, 'ackfile', FILTER_SANITIZE_STRING);
	if (isset($_GET['ackcode'])) $code = filter_input(INPUT_GET, 'ackcode', FILTER_SANITIZE_STRING);
	if ($fname && $code) {
		$str_html = ibr_ack_error($fname, $code);
	} else {
		$str_html = 'Code value invalid <br />'.PHP_EOL;
	}
	return $str_html;
}


/**
 * check if the batch control number is found in the 997/999 files table
 * 
 * @uses csv_search_record()
 * @return string 
 */
function ibr_disp_997_for_batch() {
    $str_html = '';
    $batch_icn = filter_input(INPUT_GET, 'batchicn', FILTER_SANITIZE_STRING);
    if ($batch_icn) {
        $ctln = (strlen($batch_icn) >= 9) ? substr($batch_icn, 0, 9) : trim(strval($batch_icn));
        $search = array('s_val'=>$ctln, 's_col'=>3, 'r_cols'=>'all');
        $result = csv_search_record('f997', 'file', $search, "1");
        //
        // should be matching row(s) from files_997.csv
        if (is_array($result) && count($result)) {
            $str_html .= "<p>Acknowledgement information</p>".PHP_EOL;
            foreach($result as $rslt) {
                $ext = substr($rslt[1], -3);
                //
                $str_html .= "Date: {$rslt[0]} <br />".PHP_EOL;
                $str_html .= "File: <a target=\"blank\" href=edi_history_main.php?fvkey={$rslt[1]}>{$rslt[1]}</a> <br />".PHP_EOL;
                $str_html .= "Batch ICN: {$rslt[3]} <br />";
                // error count or code in position 4
                if ($ext == '999' || $ext == '997') {
                    $str_html .= "Rejects: {$rslt[4]} <br />".PHP_EOL;
                    // don't have dialog from this dialog, so don't link
                    //$str_html .= "Rejects: <a class=\"codeval\" target=\"_blank\" href=\"edi_history_main.php?fv997={$rslt[1]}&err997={$rslt[4]}\">{$rslt[4]}</a><br />".PHP_EOL;
                } elseif ($ext == 'ta1' || $ext == 'ack') {
                    $str_html .= "Code: {$rslt[4]} <br />".PHP_EOL;
                    //$str_html .= "Code: <a class=\"codeval\" target=\"_blank\" href=\"edi_history_main.php?ackfile={$rslt[1]}&ackcode={$rslt[4]}\">{$rslt[4]}</a><br />".PHP_EOL;
                }
            }
        } else {
            $str_html .= "Did not find corresponding 997/999 file for $ctln<br />".PHP_EOL;
        }
    } else {
        $str_html .= "Invalid value for ICN number<br />".PHP_EOL;
    }
    return $str_html;
}

/**
 * function to check whether an era payment has been processed and applied
 * 
 * @uses sqlQuery()
 * 
 * @return string
 */
function edih_disp_era_processed() {
    // 
    $str_html = '';
    $ckno = filter_input(INPUT_GET, 'tracecheck', FILTER_SANITIZE_STRING);
    if ($ckno) {
        $srchval = 'ePay - '.$ckno;
        // reference like '%".$srchval."%'"
        $row = sqlQuery("SELECT reference, pay_total, global_amount FROM ar_session WHERE reference = ?", array($srchval) );
        if (!empty($row)) {
            $str_html .= "trace {$row['reference']} total \${$row['pay_total']}";
            if ($row['global_amount'] === '0') {
                $str_html .= " fully allocated";
            } else {
                $str_html .= " ({$row['global_amount']} not allocated)";
            }
        } else {
            $str_html .= "trace $ckno not posted";
        }
    } else {
        $str_html .= "trace $ckno not found";
    }
    return $str_html;
}
	


?>
