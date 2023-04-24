<?php
/**
 * Report - Cash receipts by Provider
 *
 * This module was written for one of my clients to report on cash
 * receipts by practitioner.  It is not as complete as it should be
 * but I wanted to make the code available to the project because
 * many other practices have this same need. - rod@sunsetsystems.com
 *
 * Copyright (C) 2016 Terry Hill <terry@lillysystems.com>
 * Copyright (C) 2006-2016 Rod Roark <rod@sunsetsystems.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * @package OpenEMR
 * @author  Rod Roark <rod@sunsetsystems.com>
 * @author  Terry Hill <terry@lillysystems.com>
 * @link    http://open-emr.org
 */
 
$sanitize_all_escapes=true;
$fake_register_globals=false;

require_once('../globals.php');
require_once($GLOBALS['srcdir'].'/patient.inc');
require_once($GLOBALS['srcdir'].'/acl.inc');
require_once($GLOBALS['srcdir'].'/formatting.inc.php');
require_once($GLOBALS['srcdir'].'/options.inc.php');
require_once($GLOBALS['srcdir'].'/formdata.inc.php');
require_once($GLOBALS['fileroot'].'/custom/code_types.inc.php');

  // This determines if a particular procedure code corresponds to receipts
  // for the "Clinic" column as opposed to receipts for the practitioner.  Each
  // practice will have its own policies in this regard, so you'll probably
  // have to customize this function.  If you use the "fee sheet" encounter
  // form then the code below may work for you.
  //
  require_once('../forms/fee_sheet/codes.php');
  function is_clinic($code) {
    global $bcodes;
    $i = strpos($code, ':');
    if ($i) $code = substr($code, 0, $i);
    return ($bcodes['CPT4'][xl('Lab')][$code]     ||
      $bcodes['CPT4'][xl('Immunizations')][$code] ||
      $bcodes['HCPCS'][xl('Therapeutic Injections')][$code]);
  }

  function bucks($amount) {
    if ($amount) echo attr(oeFormatMoney($amount));
  }

  if (! acl_check('acct', 'rep')) die(xlt("Unauthorized access."));


  $form_use_edate  = $_POST['form_use_edate'];

  $form_proc_codefull = trim($_POST['form_proc_codefull']);
  // Parse the code type and the code from <code_type>:<code>
  $tmp_code_array = explode(':',$form_proc_codefull);
  $form_proc_codetype = $tmp_code_array[0];
  $form_proc_code = $tmp_code_array[1];

  $form_dx_codefull  = trim($_POST['form_dx_codefull']);
  // Parse the code type and the code from <code_type>:<code>
  $tmp_code_array = explode(':',$form_dx_codefull);
  $form_dx_codetype = $tmp_code_array[0];
  $form_dx_code = $tmp_code_array[1];

  $form_procedures = empty($_POST['form_procedures']) ? 0 : 1;
  $form_from_date  = fixDate($_POST['form_from_date'], date('Y-m-01'));
  $form_to_date    = fixDate($_POST['form_to_date'], date('Y-m-d'));
  $form_facility   = $_POST['form_facility'];
?>
<html>
<head>
<?php if (function_exists('html_header_show')) html_header_show(); ?>
<style type="text/css">
/* specifically include & exclude from printing */
@media print {
    #report_parameters {
        visibility: hidden;
        display: none;
    }
    #report_parameters_daterange {
        visibility: visible;
        display: inline;
    }
    #report_results {
       margin-top: 30px;
    }
}

/* specifically exclude some from the screen */
@media screen {      N
    #report_parameters_daterange {
        visibility: hidden;
        display: none;
    }
}
</style>

<script type="text/javascript" src="<?php echo $GLOBALS['webroot']; ?>/library/dialog.js?v=<?php echo $v_js_includes; ?>"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-9-1/index.js"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['webroot']; ?>/library/js/report_helper.js?v=<?php echo $v_js_includes; ?>"></script>

<script language="JavaScript">

 $(document).ready(function() {
  oeFixedHeaderSetup(document.getElementById('mymaintable'));
  var win = top.printLogSetup ? top : opener.top;
  win.printLogSetup(document.getElementById('printbutton'));
 });

// This is for callback by the find-code popup.
// Erases the current entry
// The target element is set by the find-code popup
//  (this allows use of this in multiple form elements on the same page)
function set_related_target(codetype, code, selector, codedesc, target_element) {
 var f = document.forms[0];
 var s = f[target_element].value;
 if (code) {
  s = codetype + ':' + code;
 } else {
  s = '';
 }
 f[target_element].value = s;
}

// This invokes the find-code (procedure/service codes) popup.
function sel_procedure() {
 dlgopen('../patient_file/encounter/find_code_popup.php?target_element=form_proc_codefull&codetype=<?php echo attr(collect_codetypes("procedure","csv")) ?>', '_blank', 500, 400);
}

// This invokes the find-code (diagnosis codes) popup.
function sel_diagnosis() {
 dlgopen('../patient_file/encounter/find_code_popup.php?target_element=form_dx_codefull&codetype=<?php echo attr(collect_codetypes("diagnosis","csv")) ?>', '_blank', 500, 400);
}

</script>

<title><?php echo xlt('Cash Receipts by Provider')?></title>
</head>

<body class="body_top">

<span class='title'><?php echo xlt('Report'); ?> - <?php echo xlt('Cash Receipts by Provider'); ?></span>

<form method='post' action='sl_receipts_report.php' id='theform'>

<div id="report_parameters">

<input type='hidden' name='form_refresh' id='form_refresh' value=''/>

<table>
 <tr>
  <td width='660px'>
	<div style='float:left'>

	<table class='text'>
		<tr>
			<td class='label'>
				<?php echo xlt('Facility'); ?>:
			</td>
			<td>
			<?php dropdown_facility($form_facility, 'form_facility'); ?>
			</td>
			<td class='label'>
			   <?php echo xlt('Provider'); ?>:
			</td>
			<td>
				<?php
				if (acl_check('acct', 'rep_a')) {
					// Build a drop-down list of providers.
					//
					$query = "select id, lname, fname from users where " .
						"authorized = 1 order by lname, fname";
					$res = sqlStatement($query);
					echo "   &nbsp;<select name='form_doctor'>\n";
					echo "    <option value=''>-- " . xlt('All Providers') . " --\n";
					while ($row = sqlFetchArray($res)) {
						$provid = $row['id'];
                        echo "    <option value='". attr($provid) ."'";
						if ($provid == $_POST['form_doctor']) echo " selected";
						echo ">" . text($row['lname']) . ", " . text($row['fname']) . "\n";
					}
					echo "   </select>\n";
				} else {
					echo "<input type='hidden' name='form_doctor' value='" . attr($_SESSION['authUserID']) . "'>";
				}
			?>
			</td>
			<td>
			   <select name='form_use_edate'>
				<option value='0'><?php echo xlt('Payment Date'); ?></option>
				<option value='1'<?php if ($form_use_edate) echo ' selected' ?>><?php echo xlt('Invoice Date'); ?></option>
			   </select>
			</td>
		</tr>
		<tr>
			<td class='label'>
			   <?php echo xlt('From'); ?>:
			</td>
			<td>
			   <input type='text' name='form_from_date' id="form_from_date" size='10' value='<?php  echo attr($form_from_date); ?>'
				title='<?php echo xla('Date of appointments mm/dd/yyyy'); ?>' >
			   <img src='../pic/show_calendar.gif' align='absbottom' width='24' height='22'
				id='img_from_date' border='0' alt='[?]' style='cursor:pointer'
				title='<?php echo xla('Click here to choose a date'); ?>'>
			</td>
			<td class='label'>
			   <?php echo xlt('To'); ?>:
			</td>
			<td>
			   <input type='text' name='form_to_date' id="form_to_date" size='10' value='<?php  echo attr($form_to_date); ?>'
				title='<?php echo xla('Optional end date mm/dd/yyyy'); ?>' >
			   <img src='../pic/show_calendar.gif' align='absbottom' width='24' height='22'
				id='img_to_date' border='0' alt='[?]' style='cursor:pointer'
				title='<?php echo xla('Click here to choose a date'); ?>'>
			</td>
			<td>&nbsp;</td>
		</tr>
		<tr>
			<td>
				<?php if (!$GLOBALS['simplified_demographics']) echo '&nbsp;' . xlt('Procedure/Service') . ':'; ?>
			</td>
			<td>
			   <input type='text' name='form_proc_codefull' size='11' value='<?php echo attr($form_proc_codefull); ?>' onclick='sel_procedure()'
				title='<?php echo xla('Optional procedure/service code'); ?>' 
				<?php if ($GLOBALS['simplified_demographics']) echo "style='display:none'"; ?>>
			</td>

			<td>
			   <?php if (!$GLOBALS['simplified_demographics']) echo '&nbsp;' . xlt('Diagnosis') . ':'; ?>
			</td>
			<td>
			   <input type='text' name='form_dx_codefull' size='11' value='<?php echo attr($form_dx_codefull); ?>' onclick='sel_diagnosis()'
				title='<?php echo xla('Enter a diagnosis code to exclude all invoices not containing it'); ?>'
				<?php if ($GLOBALS['simplified_demographics']) echo "style='display:none'"; ?>>
			</td>

			<td>
			   <input type='checkbox' name='form_details' value='1'<?php if ($_POST['form_details']) echo " checked"; ?>><?php echo xlt('Details')?>
			   <input type='checkbox' name='form_procedures' value='1'<?php if ($form_procedures) echo " checked"; ?>><?php echo xlt('Procedures')?>
			</td>
		</tr>
        
	</table>

	</div>

  </td>
  
  <td align='left' valign='middle' height="100%">
  
	<table style='border-left:1px solid; width:100%; height:100%' >
		<tr>
			<td>
				<div style='margin-left:15px'>
					<a href='#' class='css_button' onclick='$("#form_refresh").attr("value","true"); $("#theform").submit();'>
					<span>
						<?php echo xlt('Submit'); ?>
					</span>
					</a>

					<?php if ($_POST['form_refresh']) { ?>
					<a href='#' class='css_button' id='printbutton'>
						<span>
							<?php echo xlt('Print'); ?>
						</span>
					</a>
					<?php } ?>
				</div>
			</td>
		</tr>
	</table>
  </td>
 </tr>
</table>
</div>

<?php
 if ($_POST['form_refresh']) {
?>
<div id="report_results">
<table border='0' cellpadding='1' cellspacing='2' width='98%' id='mymaintable'>
 <thead>
  <th>
   <?php echo xlt('Practitioner') ?>
  </th>
  <th>
   <?php echo xlt('Date') ?>
  </th>
<?php if ($form_procedures) { ?>
  <th>
   <?php if ($GLOBALS['cash_receipts_report_invoice'] == '0') {
    echo xlt('Invoice');
   } else {
    echo xlt('Name');
   }?>
  </th>
<?php } ?>
<?php if ($form_proc_codefull) { ?>
  <th align='right'>
   <?php echo xlt('InvAmt') ?>
  </th>
<?php } ?>
<?php if ($form_proc_codefull) { ?>
  <th>
   <?php echo xlt('Insurance') ?>
  </th>
<?php } ?>
<?php if ($form_procedures) { ?>
  <th>
   <?php echo xlt('Procedure') ?>
  </th>
  <th align="right">
   <?php echo xlt('Prof.') ?>
  </th>
  <th align="right">
   <?php echo xlt('Clinic') ?>
  </th>
<?php } else { ?>
  <th align="right">
   <?php echo xlt('Received') ?>
  </th>
<?php } ?>
 </thead>
<?php
  if ($_POST['form_refresh']) {
    $form_doctor = $_POST['form_doctor'];
    $arows = array();

      $ids_to_skip = array();
      $irow = 0;

      // Get copays.  These will be ignored if a CPT code was specified.
      //
      if (!$form_proc_code || !$form_proc_codetype) {
        /*************************************************************
        $query = "SELECT b.fee, b.pid, b.encounter, b.code_type, b.code, b.modifier, " .
          "fe.date, fe.id AS trans_id, u.id AS docid " .
          "FROM billing AS b " .
          "JOIN form_encounter AS fe ON fe.pid = b.pid AND fe.encounter = b.encounter " .
          "JOIN forms AS f ON f.pid = b.pid AND f.encounter = b.encounter AND f.formdir = 'newpatient' " .
          "LEFT OUTER JOIN users AS u ON u.username = f.user " .
          "WHERE b.code_type = 'COPAY' AND b.activity = 1 AND " .
          "fe.date >= '$form_from_date 00:00:00' AND fe.date <= '$form_to_date 23:59:59'";
        // If a facility was specified.
        if ($form_facility) {
          $query .= " AND fe.facility_id = '$form_facility'";
        }
        // If a doctor was specified.
        if ($form_doctor) {
          $query .= " AND u.id = '$form_doctor'";
        }
        *************************************************************/
        $sqlBindArray = array();
        $query = "SELECT b.fee, b.pid, b.encounter, b.code_type, b.code, b.modifier, " .
          "fe.date, fe.id AS trans_id, fe.provider_id AS docid, fe.invoice_refno " .
          "FROM billing AS b " .
          "JOIN form_encounter AS fe ON fe.pid = b.pid AND fe.encounter = b.encounter " .
          "WHERE b.code_type = 'COPAY' AND b.activity = 1 AND " .
          "fe.date >= ? AND fe.date <= ?";
          array_push($sqlBindArray,$form_from_date . " 00:00:00",$form_to_date . " 23:59:59");
        // If a facility was specified.
        if ($form_facility) {
          $query .= " AND fe.facility_id = ?";
          array_push($sqlBindArray,$form_facility);
        }
        // If a doctor was specified.
        if ($form_doctor) {
          $query .= " AND fe.provider_id = ?";
          array_push($sqlBindArray,$form_doctor);
        }
        /************************************************************/
        //
        $res = sqlStatement($query,$sqlBindArray);
        while ($row = sqlFetchArray($res)) {
          $trans_id = $row['trans_id'];
          $thedate = substr($row['date'], 0, 10);
          $patient_id = $row['pid'];
          $encounter_id = $row['encounter'];
          //
          if (!empty($ids_to_skip[$trans_id])) continue;
          //
          // If a diagnosis code was given then skip any invoices without
          // that diagnosis.
          if ($form_dx_code && $form_dx_codetype) {
            $tmp = sqlQuery("SELECT count(*) AS count FROM billing WHERE " .
              "pid = ? AND encounter = ? AND " .
              "code_type = ? AND code LIKE ? AND " .
              "activity = 1", array($patient_id,$encounter_id,$form_dx_codetype,$form_dx_code));
            if (empty($tmp['count'])) {
              $ids_to_skip[$trans_id] = 1;
              continue;
            }
          }
          //
          $key = sprintf("%08u%s%08u%08u%06u", $row['docid'], $thedate,
            $patient_id, $encounter_id, ++$irow);
          $arows[$key] = array();
          $arows[$key]['transdate'] = $thedate;
          $arows[$key]['amount'] = $row['fee'];
          $arows[$key]['docid'] = $row['docid'];
          $arows[$key]['project_id'] = 0;
          $arows[$key]['memo'] = '';
          if ($GLOBALS['cash_receipts_report_invoice'] == '0') {
          $arows[$key]['invnumber'] = "$patient_id.$encounter_id";
          } else{
            $arows[$key]['invnumber'] = "$patient_name";
          }
          $arows[$key]['irnumber'] = $row['invoice_refno'];
        } // end while
      } // end copays (not $form_proc_code)

      // Get ar_activity (having payments), form_encounter, forms, users, optional ar_session
      /***************************************************************
      $query = "SELECT a.pid, a.encounter, a.post_time, a.code, a.modifier, a.pay_amount, " .
        "fe.date, fe.id AS trans_id, u.id AS docid, s.deposit_date, s.payer_id " .
        "FROM ar_activity AS a " .
        "JOIN form_encounter AS fe ON fe.pid = a.pid AND fe.encounter = a.encounter " .
        "JOIN forms AS f ON f.pid = a.pid AND f.encounter = a.encounter AND f.formdir = 'newpatient' " .
        "LEFT OUTER JOIN users AS u ON u.username = f.user " .
        "LEFT OUTER JOIN ar_session AS s ON s.session_id = a.session_id " .
        "WHERE a.pay_amount != 0 AND ( " .
        "a.post_time >= '$form_from_date 00:00:00' AND a.post_time <= '$form_to_date 23:59:59' " .
        "OR fe.date >= '$form_from_date 00:00:00' AND fe.date <= '$form_to_date 23:59:59' " .
        "OR s.deposit_date >= '$form_from_date' AND s.deposit_date <= '$form_to_date' )";
      // If a procedure code was specified.
      if ($form_proc_code) $query .= " AND a.code = '$form_proc_code'";
      // If a facility was specified.
      if ($form_facility) $query .= " AND fe.facility_id = '$form_facility'";
      // If a doctor was specified.
      if ($form_doctor) $query .= " AND u.id = '$form_doctor'";
      ***************************************************************/
      $sqlBindArray = array();
      $query = "SELECT a.pid, a.encounter, a.post_time, a.code, a.modifier, a.pay_amount, " .
        "fe.date, fe.id AS trans_id, fe.provider_id AS docid, fe.invoice_refno, s.deposit_date, s.payer_id, " .
        "b.provider_id, concat(p.lname, ' ', p.fname) as 'pat_fulname' " .
        "FROM ar_activity AS a " .
        "JOIN form_encounter AS fe ON fe.pid = a.pid AND fe.encounter = a.encounter " .
        "LEFT OUTER JOIN ar_session AS s ON s.session_id = a.session_id " .
        "LEFT OUTER JOIN patient_data AS p ON p.pid = a.pid " .
        "LEFT OUTER JOIN billing AS b ON b.pid = a.pid AND b.encounter = a.encounter AND " .
        "b.code = a.code AND b.modifier = a.modifier AND b.activity = 1 AND " .
        "b.code_type != 'COPAY' AND b.code_type != 'TAX' " .
        "WHERE a.pay_amount != 0 AND ( " .
        "a.post_time >= ? AND a.post_time <= ? " .
        "OR fe.date >= ? AND fe.date <= ? " .
        "OR s.deposit_date >= ? AND s.deposit_date <= ? )";
        array_push($sqlBindArray,$form_from_date . " 00:00:00",$form_to_date . " 23:59:59",$form_from_date . " 00:00:00",$form_to_date . " 23:59:59",$form_from_date,$form_to_date);
      // If a procedure code was specified.
      // Support code type if it is in the ar_activity table. Note it is not always included, so
      // also support a blank code type in ar_activity table.
      if ($form_proc_codetype && $form_proc_code) {
        $query .= " AND (a.code_type = ? OR a.code_type = '') AND a.code = ?";
        array_push($sqlBindArray,$form_proc_codetype,$form_proc_code);
      }
      // If a facility was specified.
      if ($form_facility) {
        $query .= " AND fe.facility_id = ?";
        array_push($sqlBindArray,$form_facility);
      }
      // If a doctor was specified.
      if ($form_doctor) {
        $query .= " AND ( b.provider_id = ? OR " .
          "( ( b.provider_id IS NULL OR b.provider_id = 0 ) AND " .
          "fe.provider_id = ? ) )";
          array_push($sqlBindArray,$form_doctor,$form_doctor);
      }
      /**************************************************************/
      //
      $res = sqlStatement($query,$sqlBindArray);
      while ($row = sqlFetchArray($res)) {
        $trans_id = $row['trans_id'];
        $patient_id = $row['pid'];
        $encounter_id = $row['encounter'];
        $patient_name = $row['pat_fulname'];
        //
        if (!empty($ids_to_skip[$trans_id])) continue;
        //
        if ($form_use_edate) {
          $thedate = substr($row['date'], 0, 10);
        } else {
          if (!empty($row['deposit_date']))
            $thedate = $row['deposit_date'];
          else
            $thedate = substr($row['post_time'], 0, 10);
        }
        if (strcmp($thedate, $form_from_date) < 0 || strcmp($thedate, $form_to_date) > 0) continue;
        //
        // If a diagnosis code was given then skip any invoices without
        // that diagnosis.
        if ($form_dx_code && $form_dx_codetype) {
          $tmp = sqlQuery("SELECT count(*) AS count FROM billing WHERE " .
            "pid = ? AND encounter = ? AND " .
            "code_type = ? AND code LIKE ? AND " .
            "activity = 1", array($patient_id,$encounter_id,$form_dx_codetype,$form_dx_code));
          if (empty($tmp['count'])) {
            $ids_to_skip[$trans_id] = 1;
            continue;
          }
        }
        //
        $docid = empty($row['encounter_id']) ? $row['docid'] : $row['encounter_id'];
        $key = sprintf("%08u%s%08u%08u%06u", $docid, $thedate,
          $patient_id, $encounter_id, ++$irow);
        $arows[$key] = array();
        $arows[$key]['transdate'] = $thedate;
        $arows[$key]['amount'] = 0 - $row['pay_amount'];
        $arows[$key]['docid'] = $docid;
        $arows[$key]['project_id'] = empty($row['payer_id']) ? 0 : $row['payer_id'];
        $arows[$key]['memo'] = $row['code'];
        if ($GLOBALS['cash_receipts_report_invoice'] == '0') {
          $arows[$key]['invnumber'] = "$patient_id.$encounter_id";
        } else{
          $arows[$key]['invnumber'] = "$patient_name";
        }
        $arows[$key]['irnumber'] = $row['invoice_refno'];
      } // end while

    ksort($arows);
    $docid = 0;

    foreach ($arows as $row) {

      // Get insurance company name
      $insconame = '';
      if ($form_proc_codefull  && $row['project_id']) {
        $tmp = sqlQuery("SELECT name FROM insurance_companies WHERE " .
          "id = ?", array($row['project_id']));
        $insconame = $tmp['name'];
      }

      $amount1 = 0;
      $amount2 = 0;
      if ($form_procedures && is_clinic($row['memo']))
        $amount2 -= $row['amount'];
      else
        $amount1 -= $row['amount'];

      // if ($docid != $row['employee_id']) {
      if ($docid != $row['docid']) {
        if ($docid) {
          // Print doc totals.
?>

 <tr bgcolor="#ddddff">
  <td class="detail" colspan="<?php echo ($form_proc_codefull ? 4 : 2) + ($form_procedures ? 2 : 0); ?>">
   <?php echo xlt('Totals for ') . text($docname) ?>
  </td>
  <td align="right">
   <?php bucks($doctotal1) ?>
  </td>
<?php if ($form_procedures) { ?>
  <td align="right">
   <?php bucks($doctotal2) ?>
  </td>
<?php } ?>
 </tr>
<?php
        }
        $doctotal1 = 0;
        $doctotal2 = 0;

        $docid = $row['docid'];
        $tmp = sqlQuery("SELECT lname, fname FROM users WHERE id = ?", array($docid));
        $docname = empty($tmp) ? xl('Unknown') : $tmp['fname'] . ' ' . $tmp['lname'];

        $docnameleft = $docname;
      }

      if ($_POST['form_details']) {
?>

 <tr>
  <td class="detail">
   <?php echo text($docnameleft); $docnameleft = " " ?>
  </td>
  <td class="detail">
   <?php echo oeFormatShortDate($row['transdate']) ?>
  </td>
<?php if ($form_procedures) { ?>
  <td class="detail">
   <?php echo empty($row['irnumber']) ? text($row['invnumber']) : text($row['irnumber']); ?>
  </td>
<?php } ?>
<?php
        if ($form_proc_code && $form_proc_codetype) {
          echo "  <td class='detail' align='right'>";
            list($patient_id, $encounter_id) = explode(".", $row['invnumber']);
            $tmp = sqlQuery("SELECT SUM(fee) AS sum FROM billing WHERE " .
              "pid = ? AND encounter = ? AND " .
              "code_type = ? AND code = ? AND activity = 1", array($patient_id,$encounter_id,$form_proc_codetype,$form_proc_code));
            bucks($tmp['sum']);
          echo "  </td>\n";
        }
?>
<?php if ($form_proc_codefull) { ?>
  <td class="detail">
   <?php echo text($insconame) ?>
  </td>
<?php } ?>
<?php if ($form_procedures) { ?>
  <td class="detail">
   <?php echo text($row['memo']) ?>
  </td>
<?php } ?>
  <td class="detail" align="right">
   <?php bucks($amount1) ?>
  </td>
<?php if ($form_procedures) { ?>
  <td class="detail" align="right">
   <?php bucks($amount2) ?>
  </td>
<?php } ?>
 </tr>
<?php
      } // end details
      $doctotal1   += $amount1;
      $doctotal2   += $amount2;
      $grandtotal1 += $amount1;
      $grandtotal2 += $amount2;
    }
?>

 <tr bgcolor="#ddddff">
  <td class="detail" colspan="<?php echo ($form_proc_codefull ? 4 : 2) + ($form_procedures ? 2 : 0); ?>">
   <?php echo xlt('Totals for ') . text($docname) ?>
  </td>
  <td align="right">
   <?php bucks($doctotal1) ?>
  </td>
<?php if ($form_procedures) { ?>
  <td align="right">
   <?php bucks($doctotal2) ?>
  </td>
<?php } ?>
 </tr>

 <tr bgcolor="#ffdddd">
  <td class="detail" colspan="<?php echo ($form_proc_codefull ? 4 : 2) + ($form_procedures ? 2 : 0); ?>">
   <?php echo xlt('Grand Totals') ?>
  </td>
  <td align="right">
   <?php bucks($grandtotal1) ?>
  </td>
<?php if ($form_procedures) { ?>
  <td align="right">
   <?php bucks($grandtotal2) ?>
  </td>
<?php } ?>
 </tr>
 <?php $report_from_date = oeFormatShortDate($form_from_date)  ;
       $report_to_date = oeFormatShortDate($form_to_date)  ;
 ?>
<div align='right'><span class='title' ><?php echo xlt('Report Date'). ' '; ?><?php echo text($report_from_date);?> - <?php echo text($report_to_date);?></span></div>

<?php
  }
?>

</table>
</div>
<?php } else { ?>
<div class='text'>
 	<?php echo xlt('Please input search criteria above, and click Submit to view results.'); ?>
</div>
<?php } ?>

</form>
</body>

<!-- stuff for the popup calendar -->
<link rel='stylesheet' href='<?php echo $css_header ?>' type='text/css'>
<style type="text/css">@import url(<?php echo $GLOBALS['webroot']; ?>/library/dynarch_calendar.css);</style>
<script type="text/javascript" src="<?php echo $GLOBALS['webroot']; ?>/library/dynarch_calendar.js"></script>
<?php require_once($GLOBALS['srcdir'].'/dynarch_calendar_en.inc.php'); ?>
<script type="text/javascript" src="<?php echo $GLOBALS['webroot']; ?>/library/dynarch_calendar_setup.js"></script>

<script language="Javascript">
 Calendar.setup({inputField:"form_from_date", ifFormat:"%Y-%m-%d", button:"img_from_date"});
 Calendar.setup({inputField:"form_to_date", ifFormat:"%Y-%m-%d", button:"img_to_date"});
</script>

</html>
