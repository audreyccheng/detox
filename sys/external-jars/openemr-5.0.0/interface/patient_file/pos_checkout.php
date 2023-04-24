<?php
/**
 * Checkout Module.
 *
 * This module supports a popup window to handle patient checkout
 * as a point-of-sale transaction.  Support for in-house drug sales
 * is included.
 *
 * <pre>
 * Important notes about system design:
 * (1) Drug sales may or may not be associated with an encounter;
 *     they are if they are paid for concurrently with an encounter, or
 *     if they are "product" (non-prescription) sales via the Fee Sheet.
 * (2) Drug sales without an encounter will have 20YYMMDD, possibly
 *     with a suffix, as the encounter-number portion of their invoice
 *     number.
 * (3) Payments are saved as AR only, don't mess with the billing table.
 *     See library/classes/WSClaim.class.php for posting code.
 * (4) On checkout, the billing and drug_sales table entries are marked
 *     as billed and so become unavailable for further billing.
 * (5) Receipt printing must be a separate operation from payment,
 *     and repeatable.
 *
 * TBD:
 * If this user has 'irnpool' set
 *   on display of checkout form
 *     show pending next invoice number
 *   on applying checkout
 *     save next invoice number to form_encounter
 *     compute new next invoice number
 *   on receipt display
 *     show invoice number
 * </pre>
 *
 * Copyright (C) 2006-2016 Rod Roark <rod@sunsetsystems.com>
 *
 * LICENSE: This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://opensource.org/licenses/gpl-license.php>;.
 *
 * @package OpenEMR
 * @author  Rod Roark <rod@sunsetsystems.com>
 * @author  Brady Miller <brady@sparmy.com>
 * @link    http://www.open-emr.org
 */


$fake_register_globals=false;
$sanitize_all_escapes=true;

require_once("../globals.php");
require_once("$srcdir/acl.inc");
require_once("$srcdir/patient.inc");
require_once("$srcdir/billing.inc");
require_once("$srcdir/formatting.inc.php");
require_once("$srcdir/formdata.inc.php");
require_once("../../custom/code_types.inc.php");

$currdecimals = $GLOBALS['currency_decimals'];

$details = empty($_GET['details']) ? 0 : 1;

$patient_id = empty($_GET['ptid']) ? $pid : 0 + $_GET['ptid'];

// Get the patient's name and chart number.
$patdata = getPatientData($patient_id, 'fname,mname,lname,pubpid,street,city,state,postal_code');

// Output HTML for an invoice line item.
//
$prevsvcdate = '';
function receiptDetailLine($svcdate, $description, $amount, $quantity) {
  global $prevsvcdate, $details;
  if (!$details) return;
  $amount = sprintf('%01.2f', $amount);
  if (empty($quantity)) $quantity = 1;
  $price = sprintf('%01.4f', $amount / $quantity);
  $tmp = sprintf('%01.2f', $price);
  if ($price == $tmp) $price = $tmp;
  echo " <tr>\n";
  echo "  <td>" . ($svcdate == $prevsvcdate ? '&nbsp;' : text(oeFormatShortDate($svcdate))) . "</td>\n";
  echo "  <td>" . text($description) . "</td>\n";
  echo "  <td align='right'>" . text(oeFormatMoney($price)) . "</td>\n";
  echo "  <td align='right'>" . text($quantity) . "</td>\n";
  echo "  <td align='right'>" . text(oeFormatMoney($amount)) . "</td>\n";
  echo " </tr>\n";
  $prevsvcdate = $svcdate;
}

// Output HTML for an invoice payment.
//
function receiptPaymentLine($paydate, $amount, $description='') {
  $amount = sprintf('%01.2f', 0 - $amount); // make it negative
  echo " <tr>\n";
  echo "  <td>" . text(oeFormatShortDate($paydate)) . "</td>\n";
  echo "  <td>" . xlt('Payment') . " " . text($description) . "</td>\n";
  echo "  <td colspan='2'>&nbsp;</td>\n";
  echo "  <td align='right'>" . text(oeFormatMoney($amount)) . "</td>\n";
  echo " </tr>\n";
}

// Generate a receipt from the last-billed invoice for this patient,
// or for the encounter specified as a GET parameter.
//
function generate_receipt($patient_id, $encounter=0) {
  global $sl_err, $sl_cash_acc, $css_header, $details;

  // Get details for what we guess is the primary facility.
  $frow = sqlQuery("SELECT * FROM facility " .
    "ORDER BY billing_location DESC, accepts_assignment DESC, id LIMIT 1");

  $patdata = getPatientData($patient_id, 'fname,mname,lname,pubpid,street,city,state,postal_code,providerID');

  // Get the most recent invoice data or that for the specified encounter.
  //
  // Adding a provider check so that their info can be displayed on receipts
    if ($encounter) {
      $ferow = sqlQuery("SELECT id, date, encounter, provider_id FROM form_encounter " .
        "WHERE pid = ? AND encounter = ?", array($patient_id,$encounter) );
    } else {
      $ferow = sqlQuery("SELECT id, date, encounter, provider_id FROM form_encounter " .
        "WHERE pid = ? " .
        "ORDER BY id DESC LIMIT 1", array($patient_id) );
    }
    if (empty($ferow)) die(xlt("This patient has no activity."));
    $trans_id = $ferow['id'];
    $encounter = $ferow['encounter'];
    $svcdate = substr($ferow['date'], 0, 10);
    
    if ($GLOBALS['receipts_by_provider']){
      if (isset($ferow['provider_id']) ) {
        $encprovider = $ferow['provider_id'];
      } else if (isset($patdata['providerID'])){
        $encprovider = $patdata['providerID'];
      } else { $encprovider = -1; }
    }
    
    if ($encprovider){
      $providerrow = sqlQuery("SELECT fname, mname, lname, title, street, streetb, " .
        "city, state, zip, phone, fax FROM users WHERE id = ?", array($encprovider) );
    }

  // Get invoice reference number.
  $encrow = sqlQuery("SELECT invoice_refno FROM form_encounter WHERE " .
    "pid = ? AND encounter = ? LIMIT 1", array($patient_id,$encounter) );
  $invoice_refno = $encrow['invoice_refno'];
?>
<html>
<head>
<?php html_header_show(); ?>
<link rel='stylesheet' href='<?php echo $css_header ?>' type='text/css'>
<title><?php echo xlt('Receipt for Payment'); ?></title>
<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-2-2/index.js"></script>
<script type="text/javascript" src="../../library/dialog.js?v=<?php echo $v_js_includes; ?>"></script>
<script language="JavaScript">

<?php require($GLOBALS['srcdir'] . "/restoreSession.php"); ?>

 $(document).ready(function() {
  var win = top.printLogSetup ? top : opener.top;
  win.printLogSetup(document.getElementById('printbutton'));
 });

 // Process click on Print button.
 function printlog_before_print() {
  var divstyle = document.getElementById('hideonprint').style;
  divstyle.display = 'none';
 }

 // Process click on Delete button.
 function deleteme() {
  dlgopen('deleter.php?billing=<?php echo attr("$patient_id.$encounter"); ?>', '_blank', 500, 450);
  return false;
 }

 // Called by the deleteme.php window on a successful delete.
 function imdeleted() {
  window.close();
 }

</script>
</head>
<body class="body_top">
<center>
<?php 
  if ( $GLOBALS['receipts_by_provider'] && !empty($providerrow) ) { printProviderHeader($providerrow); }
  else { printFacilityHeader($frow); }
?>
<?php
  echo xlt("Receipt Generated") . ":" . text(date(' F j, Y'));
  if ($invoice_refno) echo " " . xlt("Invoice Number") . ": " . text($invoice_refno) . " " . xlt("Service Date")  . ": " . text($svcdate);
?>
<br>&nbsp;
</b></p>
</center>
<p>
<?php echo text($patdata['fname']) . ' ' . text($patdata['mname']) . ' ' . text($patdata['lname']) ?>
<br><?php echo text($patdata['street']) ?>
<br><?php echo text($patdata['city']) . ', ' . text($patdata['state']) . ' ' . text($patdata['postal_code']) ?>
<br>&nbsp;
</p>
<center>
<table cellpadding='5'>
 <tr>
  <td><b><?php echo xlt('Date'); ?></b></td>
  <td><b><?php echo xlt('Description'); ?></b></td>
  <td align='right'><b><?php echo $details ? xlt('Price') : '&nbsp;'; ?></b></td>
  <td align='right'><b><?php echo $details ? xlt('Qty'  ) : '&nbsp;'; ?></b></td>
  <td align='right'><b><?php echo xlt('Total'); ?></b></td>
 </tr>

<?php
  $charges = 0.00;

    // Product sales
    $inres = sqlStatement("SELECT s.sale_id, s.sale_date, s.fee, " .
      "s.quantity, s.drug_id, d.name " .
      "FROM drug_sales AS s LEFT JOIN drugs AS d ON d.drug_id = s.drug_id " .
      // "WHERE s.pid = '$patient_id' AND s.encounter = '$encounter' AND s.fee != 0 " .
      "WHERE s.pid = ? AND s.encounter = ? " .
      "ORDER BY s.sale_id", array($patient_id,$encounter) );
    while ($inrow = sqlFetchArray($inres)) {
      $charges += sprintf('%01.2f', $inrow['fee']);
      receiptDetailLine($inrow['sale_date'], $inrow['name'],
        $inrow['fee'], $inrow['quantity']);
    }
    // Service and tax items
    $inres = sqlStatement("SELECT * FROM billing WHERE " .
      "pid = ? AND encounter = ? AND " .
      // "code_type != 'COPAY' AND activity = 1 AND fee != 0 " .
      "code_type != 'COPAY' AND activity = 1 " .
      "ORDER BY id", array($patient_id,$encounter) );
    while ($inrow = sqlFetchArray($inres)) {
      $charges += sprintf('%01.2f', $inrow['fee']);
      receiptDetailLine($svcdate, $inrow['code_text'],
        $inrow['fee'], $inrow['units']);
    }
    // Adjustments.
    $inres = sqlStatement("SELECT " .
      "a.code_type, a.code, a.modifier, a.memo, a.payer_type, a.adj_amount, a.pay_amount, " .
      "s.payer_id, s.reference, s.check_date, s.deposit_date " .
      "FROM ar_activity AS a " .
      "LEFT JOIN ar_session AS s ON s.session_id = a.session_id WHERE " .
      "a.pid = ? AND a.encounter = ? AND " .
      "a.adj_amount != 0 " .
      "ORDER BY s.check_date, a.sequence_no", array($patient_id,$encounter) );
    while ($inrow = sqlFetchArray($inres)) {
      $charges -= sprintf('%01.2f', $inrow['adj_amount']);
      $payer = empty($inrow['payer_type']) ? 'Pt' : ('Ins' . $inrow['payer_type']);
      receiptDetailLine($svcdate, $payer . ' ' . $inrow['memo'],
        0 - $inrow['adj_amount'], 1);
    }
?>

 <tr>
  <td colspan='5'>&nbsp;</td>
 </tr>
 <tr>
  <td><?php echo text(oeFormatShortDate($svcdispdate)); ?></td>
  <td><b><?php echo xlt('Total Charges'); ?></b></td>
  <td align='right'>&nbsp;</td>
  <td align='right'>&nbsp;</td>
  <td align='right'><?php echo text(oeFormatMoney($charges, true)) ?></td>
 </tr>
 <tr>
  <td colspan='5'>&nbsp;</td>
 </tr>

<?php
    // Get co-pays.
    $inres = sqlStatement("SELECT fee, code_text FROM billing WHERE " .
      "pid = ? AND encounter = ?  AND " .
      "code_type = 'COPAY' AND activity = 1 AND fee != 0 " .
      "ORDER BY id", array($patient_id,$encounter) );
    while ($inrow = sqlFetchArray($inres)) {
      $charges += sprintf('%01.2f', $inrow['fee']);
      receiptPaymentLine($svcdate, 0 - $inrow['fee'], $inrow['code_text']);
    }
    // Get other payments.
    $inres = sqlStatement("SELECT " .
      "a.code_type, a.code, a.modifier, a.memo, a.payer_type, a.adj_amount, a.pay_amount, " .
      "s.payer_id, s.reference, s.check_date, s.deposit_date " .
      "FROM ar_activity AS a " .
      "LEFT JOIN ar_session AS s ON s.session_id = a.session_id WHERE " .
      "a.pid = ? AND a.encounter = ? AND " .
      "a.pay_amount != 0 " .
      "ORDER BY s.check_date, a.sequence_no", array($patient_id,$encounter) );
    while ($inrow = sqlFetchArray($inres)) {
      $payer = empty($inrow['payer_type']) ? 'Pt' : ('Ins' . $inrow['payer_type']);
      $charges -= sprintf('%01.2f', $inrow['pay_amount']);
      receiptPaymentLine($svcdate, $inrow['pay_amount'],
        $payer . ' ' . $inrow['reference']);
    }
?>
 <tr>
  <td colspan='5'>&nbsp;</td>
 </tr>
 <tr>
  <td>&nbsp;</td>
  <td><b><?php echo xlt('Balance Due'); ?></b></td>
  <td colspan='2'>&nbsp;</td>
  <td align='right'><?php echo text(oeFormatMoney($charges, true)) ?></td>
 </tr>
</table>
</center>
<div id='hideonprint'>
<p>
&nbsp;
<a href='#' id='printbutton'><?php echo xlt('Print'); ?></a>
<?php if (acl_check('acct','disc')) { ?>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<a href='#' onclick='return deleteme();'><?php echo xlt('Undo Checkout'); ?></a>
<?php } ?>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<?php if ($details) { ?>
<a href='pos_checkout.php?details=0&ptid=<?php echo attr($patient_id); ?>&enc=<?php echo attr($encounter); ?>' onclick='top.restoreSession()'><?php echo xlt('Hide Details'); ?></a>
<?php } else { ?>
<a href='pos_checkout.php?details=1&ptid=<?php echo attr($patient_id); ?>&enc=<?php echo attr($encounter); ?>' onclick='top.restoreSession()'><?php echo xlt('Show Details'); ?></a>
<?php } ?>
</p>
</div>
</body>
</html>
<?php
} // end function generate_receipt()

// Function to output a line item for the input form.
//
$lino = 0;
function write_form_line($code_type, $code, $id, $date, $description,
  $amount, $units, $taxrates) {
  global $lino;
  $amount = sprintf("%01.2f", $amount);
  if (empty($units)) $units = 1;
  $price = $amount / $units; // should be even cents, but ok here if not
  if ($code_type == 'COPAY' && !$description) $description = xl('Payment');
  echo " <tr>\n";
  echo "  <td>" . text(oeFormatShortDate($date));
  echo "<input type='hidden' name='line[$lino][code_type]' value='" . attr($code_type) . "'>";
  echo "<input type='hidden' name='line[$lino][code]' value='" . attr($code) . "'>";
  echo "<input type='hidden' name='line[$lino][id]' value='" . attr($id) . "'>";
  echo "<input type='hidden' name='line[$lino][description]' value='" . attr($description) . "'>";
  echo "<input type='hidden' name='line[$lino][taxrates]' value='" . attr($taxrates) . "'>";
  echo "<input type='hidden' name='line[$lino][price]' value='" . attr($price) . "'>";
  echo "<input type='hidden' name='line[$lino][units]' value='" . attr($units) . "'>";
  echo "</td>\n";
  echo "  <td>" . text($description) . "</td>";
  echo "  <td align='right'>" . text($units) . "</td>";
  echo "  <td align='right'><input type='text' name='line[$lino][amount]' " .
       "value='" . attr($amount) . "' size='6' maxlength='8'";
  // Modifying prices requires the acct/disc permission.
  // if ($code_type == 'TAX' || ($code_type != 'COPAY' && !acl_check('acct','disc')))
  echo " style='text-align:right;background-color:transparent' readonly";
  // else echo " style='text-align:right' onkeyup='computeTotals()'";
  echo "></td>\n";
  echo " </tr>\n";
  ++$lino;
}

// Create the taxes array.  Key is tax id, value is
// (description, rate, accumulated total).
$taxes = array();
$pres = sqlStatement("SELECT option_id, title, option_value " .
  "FROM list_options WHERE list_id = 'taxrate' AND activity = 1 ORDER BY seq, title, option_id");
while ($prow = sqlFetchArray($pres)) {
  $taxes[$prow['option_id']] = array($prow['title'], $prow['option_value'], 0);
}

// Print receipt header for facility
function printFacilityHeader($frow){
	echo "<p><b>" . text($frow['name']) .
    "<br>" . text($frow['street']) .
    "<br>" . text($frow['city']) . ', ' . text($frow['state']) . ' ' . text($frow['postal_code']) .
    "<br>" . text($frow['phone']) .
    "<br>&nbsp" .
    "<br>";
}

// Pring receipt header for Provider
function printProviderHeader($pvdrow){
	echo "<p><b>" . text($pvdrow['title']) . " " . text($pvdrow['fname']) . " " . text($pvdrow['mname']) . " " . text($pvdrow['lname']) . " " .
    "<br>" . text($pvdrow['street']) .
    "<br>" . text($pvdrow['city']) . ', ' . text($pvdrow['state']) . ' ' . text($pvdrow['postal_code']) .
    "<br>" . text($pvdrow['phone']) .
    "<br>&nbsp" .
    "<br>";
}

// Mark the tax rates that are referenced in this invoice.
function markTaxes($taxrates) {
  global $taxes;
  $arates = explode(':', $taxrates);
  if (empty($arates)) return;
  foreach ($arates as $value) {
    if (!empty($taxes[$value])) $taxes[$value][2] = '1';
  }
}

$payment_methods = array(
  'Cash',
  'Check',
  'MC',
  'VISA',
  'AMEX',
  'DISC',
  'Other');

$alertmsg = ''; // anything here pops up in an alert box

// If the Save button was clicked...
//
if ($_POST['form_save']) {

  // On a save, do the following:
  // Flag drug_sales and billing items as billed.
  // Post the corresponding invoice with its payment(s) to sql-ledger
  // and be careful to use a unique invoice number.
  // Call the generate-receipt function.
  // Exit.

  $form_pid = $_POST['form_pid'];
  $form_encounter = $_POST['form_encounter'];

  // Get the posting date from the form as yyyy-mm-dd.
  $dosdate = date("Y-m-d");
  if (preg_match("/(\d\d\d\d)\D*(\d\d)\D*(\d\d)/", $_POST['form_date'], $matches)) {
    $dosdate = $matches[1] . '-' . $matches[2] . '-' . $matches[3];
  }

  // If there is no associated encounter (i.e. this invoice has only
  // prescriptions) then assign an encounter number of the service
  // date, with an optional suffix to ensure that it's unique.
  //
  if (! $form_encounter) {
    $form_encounter = substr($dosdate,0,4) . substr($dosdate,5,2) . substr($dosdate,8,2);
    $tmp = '';
      while (true) {
        $ferow = sqlQuery("SELECT id FROM form_encounter WHERE " .
          "pid = ? AND encounter = ?", array($form_pid, $form_encounter.$tmp) );
        if (empty($ferow)) break;
        $tmp = $tmp ? $tmp + 1 : 1;
      }
    $form_encounter .= $tmp;
  }

    // Delete any TAX rows from billing because they will be recalculated.
    sqlStatement("UPDATE billing SET activity = 0 WHERE " .
      "pid = ? AND encounter = ? AND " .
      "code_type = 'TAX'", array($form_pid,$form_encounter) );

  $form_amount = $_POST['form_amount'];
  $lines = $_POST['line'];

  for ($lino = 0; $lines[$lino]['code_type']; ++$lino) {
    $line = $lines[$lino];
    $code_type = $line['code_type'];
    $id        = $line['id'];
    $amount    = sprintf('%01.2f', trim($line['amount']));


    if ($code_type == 'PROD') {
      // Product sales. The fee and encounter ID may have changed.
      $query = "update drug_sales SET fee = ?, " .
      "encounter = ?, billed = 1 WHERE " .
      "sale_id = ?";
      sqlQuery($query, array($amount,$form_encounter,$id) );
    }
    else if ($code_type == 'TAX') {
      // In the SL case taxes show up on the invoice as line items.
      // Otherwise we gotta save them somewhere, and in the billing
      // table with a code type of TAX seems easiest.
      // They will have to be stripped back out when building this
      // script's input form.
      addBilling($form_encounter, 'TAX', 'TAX', 'Taxes', $form_pid, 0, 0,
        '', '', $amount, '', '', 1);
    }
    else {
      // Because there is no insurance here, there is no need for a claims
      // table entry and so we do not call updateClaim().  Note we should not
      // eliminate billed and bill_date from the billing table!
      $query = "UPDATE billing SET fee = ?, billed = 1, " .
      "bill_date = NOW() WHERE id = ?";
      sqlQuery($query, array($amount,$id) );
    }
  }

  // Post discount.
  if ($_POST['form_discount']) {
    if ($GLOBALS['discount_by_money']) {
      $amount  = sprintf('%01.2f', trim($_POST['form_discount']));
    }
    else {
      $amount  = sprintf('%01.2f', trim($_POST['form_discount']) * $form_amount / 100);
    }
    $memo = xl('Discount');
      $time = date('Y-m-d H:i:s');
      sqlBeginTrans();
      $sequence_no = sqlQuery( "SELECT IFNULL(MAX(sequence_no),0) + 1 AS increment FROM ar_activity WHERE pid = ? AND encounter = ?", array($form_pid, $form_encounter));
      $query = "INSERT INTO ar_activity ( " .
        "pid, encounter, sequence_no, code, modifier, payer_type, post_user, post_time, " .
        "session_id, memo, adj_amount " .
        ") VALUES ( " .
        "?, " .
        "?, " .
        "?, " .
        "'', " .
        "'', " .
        "'0', " .
        "?, " .
        "?, " .
        "'0', " .
        "?, " .
        "? " .
        ")";
      sqlStatement($query, array($form_pid,$form_encounter,$sequence_no['increment'],$_SESSION['authUserID'],$time,$memo,$amount) );
      sqlCommitTrans();
    }

  // Post payment.
  if ($_POST['form_amount']) {
    $amount  = sprintf('%01.2f', trim($_POST['form_amount']));
    $form_source = trim($_POST['form_source']);
    $paydesc = trim($_POST['form_method']);
      //Fetching the existing code and modifier
			$ResultSearchNew = sqlStatement("SELECT * FROM billing LEFT JOIN code_types ON billing.code_type=code_types.ct_key ".
				"WHERE code_types.ct_fee=1 AND billing.activity!=0 AND billing.pid =? AND encounter=? ORDER BY billing.code,billing.modifier",
				array($form_pid,$form_encounter));
			if($RowSearch = sqlFetchArray($ResultSearchNew))
			{
                                $Codetype=$RowSearch['code_type'];
				$Code=$RowSearch['code'];
				$Modifier=$RowSearch['modifier'];
			}else{
                                $Codetype='';
				$Code='';
				$Modifier='';
			}
      $session_id=sqlInsert("INSERT INTO ar_session (payer_id,user_id,reference,check_date,deposit_date,pay_total,".
        " global_amount,payment_type,description,patient_id,payment_method,adjustment_code,post_to_date) ".
        " VALUES ('0',?,?,now(),?,?,'','patient','COPAY',?,?,'patient_payment',now())",
        array($_SESSION['authId'],$form_source,$dosdate,$amount,$form_pid,$paydesc));

      sqlBeginTrans();
      $sequence_no = sqlQuery( "SELECT IFNULL(MAX(sequence_no),0) + 1 AS increment FROM ar_activity WHERE pid = ? AND encounter = ?", array($form_pid, $form_encounter));
      $insrt_id=sqlInsert("INSERT INTO ar_activity (pid,encounter,sequence_no,code_type,code,modifier,payer_type,post_time,post_user,session_id,pay_amount,account_code)".
        " VALUES (?,?,?,?,?,?,0,?,?,?,?,'PCP')",
        array($form_pid,$form_encounter,$sequence_no['increment'],$Codetype,$Code,$Modifier,$dosdate,$_SESSION['authId'],$session_id,$amount));
      sqlCommitTrans();
  }

  // If applicable, set the invoice reference number.
  $invoice_refno = '';
  if (isset($_POST['form_irnumber'])) {
    $invoice_refno = trim($_POST['form_irnumber']);
  }
  else {
    $invoice_refno = updateInvoiceRefNumber();
  }
  if ($invoice_refno) {
    sqlStatement("UPDATE form_encounter " .
      "SET invoice_refno = ? " .
      "WHERE pid = ? AND encounter = ?", array($invoice_refno,$form_pid,$form_encounter) );
  }

  generate_receipt($form_pid, $form_encounter);
  exit();
}

// If an encounter ID was given, then we must generate a receipt.
//
if (!empty($_GET['enc'])) {
  generate_receipt($patient_id, $_GET['enc']);
  exit();
}

// Get the unbilled billing table items for this patient.
$query = "SELECT id, date, code_type, code, modifier, code_text, " .
  "provider_id, payer_id, units, fee, encounter " .
  "FROM billing WHERE pid = ? AND activity = 1 AND " .
  "billed = 0 AND code_type != 'TAX' " .
  "ORDER BY encounter DESC, id ASC";
$bres = sqlStatement($query, array($patient_id) );

// Get the product sales for this patient.
$query = "SELECT s.sale_id, s.sale_date, s.prescription_id, s.fee, " .
  "s.quantity, s.encounter, s.drug_id, d.name, r.provider_id " .
  "FROM drug_sales AS s " .
  "LEFT JOIN drugs AS d ON d.drug_id = s.drug_id " .
  "LEFT OUTER JOIN prescriptions AS r ON r.id = s.prescription_id " .
  "WHERE s.pid = ? AND s.billed = 0 " .
  "ORDER BY s.encounter DESC, s.sale_id ASC";
$dres = sqlStatement($query, array($patient_id) );

// If there are none, just redisplay the last receipt and exit.
//
if (sqlNumRows($bres) == 0 && sqlNumRows($dres) == 0) {
  generate_receipt($patient_id);
  exit();
}

// Get the valid practitioners, including those not active.
$arr_users = array();
$ures = sqlStatement("SELECT id, username FROM users WHERE " .
  "( authorized = 1 OR info LIKE '%provider%' ) AND username != ''");
while ($urow = sqlFetchArray($ures)) {
  $arr_users[$urow['id']] = '1';
}

// Now write a data entry form:
// List unbilled billing items (cpt, hcpcs, copays) for the patient.
// List unbilled product sales for the patient.
// Present an editable dollar amount for each line item, a total
// which is also the default value of the input payment amount,
// and OK and Cancel buttons.
?>
<html>
<head>
<link rel='stylesheet' href='<?php echo $css_header ?>' type='text/css'>
<title><?php echo xlt('Patient Checkout'); ?></title>
<style>
</style>
<style type="text/css">@import url(../../library/dynarch_calendar.css);</style>
<script type="text/javascript" src="../../library/textformat.js"></script>
<script type="text/javascript" src="../../library/dynarch_calendar.js"></script>
<?php include_once("{$GLOBALS['srcdir']}/dynarch_calendar_en.inc.php"); ?>
<script type="text/javascript" src="../../library/dynarch_calendar_setup.js"></script>
<script type="text/javascript" src="../../library/dialog.js?v=<?php echo $v_js_includes; ?>"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-2-2/index.js"></script>
<script language="JavaScript">
 var mypcc = '<?php echo $GLOBALS['phone_country_code'] ?>';

<?php require($GLOBALS['srcdir'] . "/restoreSession.php"); ?>

 // This clears the tax line items in preparation for recomputing taxes.
 function clearTax(visible) {
  var f = document.forms[0];
  for (var lino = 0; true; ++lino) {
   var pfx = 'line[' + lino + ']';
   if (! f[pfx + '[code_type]']) break;
   if (f[pfx + '[code_type]'].value != 'TAX') continue;
   f[pfx + '[price]'].value = '0.00';
   if (visible) f[pfx + '[amount]'].value = '0.00';
  }
 }

 // For a given tax ID and amount, compute the tax on that amount and add it
 // to the "price" (same as "amount") of the corresponding tax line item.
 // Note the tax line items include their "taxrate" to make this easy.
 function addTax(rateid, amount, visible) {
  if (rateid.length == 0) return 0;
  var f = document.forms[0];
  for (var lino = 0; true; ++lino) {
   var pfx = 'line[' + lino + ']';
   if (! f[pfx + '[code_type]']) break;
   if (f[pfx + '[code_type]'].value != 'TAX') continue;
   if (f[pfx + '[code]'].value != rateid) continue;
   var tax = amount * parseFloat(f[pfx + '[taxrates]'].value);
   tax = parseFloat(tax.toFixed(<?php echo $currdecimals ?>));
   var cumtax = parseFloat(f[pfx + '[price]'].value) + tax;
   f[pfx + '[price]'].value  = cumtax.toFixed(<?php echo $currdecimals ?>); // requires JS 1.5
   if (visible) f[pfx + '[amount]'].value = cumtax.toFixed(<?php echo $currdecimals ?>); // requires JS 1.5
   if (isNaN(tax)) alert('Tax rate not numeric at line ' + lino);
   return tax;
  }
  return 0;
 }

 // This mess recomputes the invoice total and optionally applies a discount.
 function computeDiscountedTotals(discount, visible) {
  clearTax(visible);
  var f = document.forms[0];
  var total = 0.00;
  for (var lino = 0; f['line[' + lino + '][code_type]']; ++lino) {
   var code_type = f['line[' + lino + '][code_type]'].value;
   // price is price per unit when the form was originally generated.
   // By contrast, amount is the dynamically-generated discounted line total.
   var price = parseFloat(f['line[' + lino + '][price]'].value);
   if (isNaN(price)) alert('Price not numeric at line ' + lino);
   if (code_type == 'COPAY' || code_type == 'TAX') {
    // This works because the tax lines come last.
    total += parseFloat(price.toFixed(<?php echo $currdecimals ?>));
    continue;
   }
   var units = f['line[' + lino + '][units]'].value;
   var amount = price * units;
   amount = parseFloat(amount.toFixed(<?php echo $currdecimals ?>));
   if (visible) f['line[' + lino + '][amount]'].value = amount.toFixed(<?php echo $currdecimals ?>);
   total += amount;
   var taxrates  = f['line[' + lino + '][taxrates]'].value;
   var taxids = taxrates.split(':');
   for (var j = 0; j < taxids.length; ++j) {
    addTax(taxids[j], amount, visible);
   }
  }
  return total - discount;
 }

 // Recompute displayed amounts with any discount applied.
 function computeTotals() {
  var f = document.forms[0];
  var discount = parseFloat(f.form_discount.value);
  if (isNaN(discount)) discount = 0;
<?php if (!$GLOBALS['discount_by_money']) { ?>
  // This site discounts by percentage, so convert it to a money amount.
  if (discount > 100) discount = 100;
  if (discount < 0  ) discount = 0;
  discount = 0.01 * discount * computeDiscountedTotals(0, false);
<?php } ?>
  var total = computeDiscountedTotals(discount, true);
  f.form_amount.value = total.toFixed(<?php echo $currdecimals ?>);
  return true;
 }

</script>
</head>

<body class="body_top">

<form method='post' action='pos_checkout.php'>
<input type='hidden' name='form_pid' value='<?php echo attr($patient_id) ?>' />

<center>

<p>
<table cellspacing='5'>
 <tr>
  <td colspan='3' align='center'>
   <b><?php echo xlt('Patient Checkout for '); ?><?php echo text($patdata['fname']) . " " .
    text($patdata['lname']) . " (" . text($patdata['pubpid']) . ")" ?></b>
  </td>
 </tr>
 <tr>
  <td><b><?php echo xlt('Date'); ?></b></td>
  <td><b><?php echo xlt('Description'); ?></b></td>
  <td align='right'><b><?php echo xlt('Qty'); ?></b></td>
  <td align='right'><b><?php echo xlt('Amount'); ?></b></td>
 </tr>
<?php
$inv_encounter = '';
$inv_date      = '';
$inv_provider  = 0;
$inv_payer     = 0;
$gcac_related_visit = false;
$gcac_service_provided = false;

// Process billing table items.
// Items that are not allowed to have a fee are skipped.
//
while ($brow = sqlFetchArray($bres)) {
  // Skip all but the most recent encounter.
  if ($inv_encounter && $brow['encounter'] != $inv_encounter) continue;

  $thisdate = substr($brow['date'], 0, 10);
  $code_type = $brow['code_type'];

  // Collect tax rates, related code and provider ID.
  $taxrates = '';
  $related_code = '';
  $sqlBindArray = array();
  if (!empty($code_types[$code_type]['fee'])) {
    $query = "SELECT taxrates, related_code FROM codes WHERE code_type = ? " .
      " AND " .
      "code = ? AND ";
    array_push($sqlBindArray,$code_types[$code_type]['id'],$brow['code']);
    if ($brow['modifier']) {
      $query .= "modifier = ?";
      array_push($sqlBindArray,$brow['modifier']);
    } else {
      $query .= "(modifier IS NULL OR modifier = '')";
    }
    $query .= " LIMIT 1";
    $tmp = sqlQuery($query,$sqlBindArray);
    $taxrates = $tmp['taxrates'];
    $related_code = $tmp['related_code'];
    markTaxes($taxrates);
  }

  write_form_line($code_type, $brow['code'], $brow['id'], $thisdate,
    $brow['code_text'], $brow['fee'], $brow['units'],
    $taxrates);
  if (!$inv_encounter) $inv_encounter = $brow['encounter'];
  $inv_payer = $brow['payer_id'];
  if (!$inv_date || $inv_date < $thisdate) $inv_date = $thisdate;

  // Custom logic for IPPF to determine if a GCAC issue applies.
  if ($GLOBALS['ippf_specific'] && $related_code) {
    $relcodes = explode(';', $related_code);
    foreach ($relcodes as $codestring) {
      if ($codestring === '') continue;
      list($codetype, $code) = explode(':', $codestring);
      if ($codetype !== 'IPPF') continue;
      if (preg_match('/^25222/', $code)) {
        $gcac_related_visit = true;
        if (preg_match('/^25222[34]/', $code))
          $gcac_service_provided = true;
      }
    }
  }
}

// Process copays
//
$totalCopay = getPatientCopay($patient_id,$encounter);
if ($totalCopay < 0) {
  write_form_line("COPAY", "", "", "", "", $totalCopay, "", "");
}

// Process drug sales / products.
//
while ($drow = sqlFetchArray($dres)) {
  if ($inv_encounter && $drow['encounter'] && $drow['encounter'] != $inv_encounter) continue;

  $thisdate = $drow['sale_date'];
  if (!$inv_encounter) $inv_encounter = $drow['encounter'];

  if (!$inv_provider && !empty($arr_users[$drow['provider_id']]))
    $inv_provider = $drow['provider_id'] + 0;

  if (!$inv_date || $inv_date < $thisdate) $inv_date = $thisdate;

  // Accumulate taxes for this product.
  $tmp = sqlQuery("SELECT taxrates FROM drug_templates WHERE drug_id = ? " .
                  " ORDER BY selector LIMIT 1", array($drow['drug_id']) );
  // accumTaxes($drow['fee'], $tmp['taxrates']);
  $taxrates = $tmp['taxrates'];
  markTaxes($taxrates);

  write_form_line('PROD', $drow['drug_id'], $drow['sale_id'],
   $thisdate, $drow['name'], $drow['fee'], $drow['quantity'], $taxrates);
}

// Write a form line for each tax that has money, adding to $total.
foreach ($taxes as $key => $value) {
  if ($value[2]) {
    write_form_line('TAX', $key, $key, date('Y-m-d'), $value[0], 0, 1, $value[1]);
  }
}

// Besides copays, do not collect any other information from ar_activity,
// since this is for appt checkout.

if ($inv_encounter) {
  $erow = sqlQuery("SELECT provider_id FROM form_encounter WHERE " .
    "pid = ? AND encounter = ? " .
    "ORDER BY id DESC LIMIT 1", array($patient_id,$inv_encounter) );
  $inv_provider = $erow['provider_id'] + 0;
}
?>
</table>

<p>
<table border='0' cellspacing='4'>

 <tr>
  <td>
   <?php echo $GLOBALS['discount_by_money'] ? xlt('Discount Amount') : xlt('Discount Percentage'); ?>:
  </td>
  <td>
   <input type='text' name='form_discount' size='6' maxlength='8' value=''
    style='text-align:right' onkeyup='computeTotals()'>
  </td>
 </tr>

 <tr>
  <td>
   <?php echo xlt('Payment Method'); ?>:
  </td>
  <td>
   <select name='form_method'>
    <?php
    $query1112 = "SELECT * FROM list_options where list_id=?  ORDER BY seq, title ";
    $bres1112 = sqlStatement($query1112,array('payment_method'));
    while ($brow1112 = sqlFetchArray($bres1112))
     {
      if($brow1112['option_id']=='electronic' || $brow1112['option_id']=='bank_draft')
     continue;
    echo "<option value='".attr($brow1112['option_id'])."'>".text(xl_list_label($brow1112['title']))."</option>";
     }
    ?>
   </select>
  </td>
 </tr>

 <tr>
  <td>
   <?php echo xlt('Check/Reference Number'); ?>:
  </td>
  <td>
   <input type='text' name='form_source' size='10' value=''>
  </td>
 </tr>

 <tr>
  <td>
   <?php echo xlt('Amount Paid'); ?>:
  </td>
  <td>
   <input type='text' name='form_amount' size='10' value='0.00'>
  </td>
 </tr>

 <tr>
  <td>
   <?php echo xlt('Posting Date'); ?>:
  </td>
  <td>
   <input type='text' size='10' name='form_date' id='form_date'
    value='<?php echo attr($inv_date) ?>'
    title='yyyy-mm-dd date of service'
    onkeyup='datekeyup(this,mypcc)' onblur='dateblur(this,mypcc)' />
   <img src='../pic/show_calendar.gif' align='absbottom' width='24' height='22'
    id='img_date' border='0' alt='[?]' style='cursor:pointer'
    title='<?php echo xla("Click here to choose a date"); ?>'>
  </td>
 </tr>

<?php
// If this user has a non-empty irnpool assigned, show the pending
// invoice reference number.
$irnumber = getInvoiceRefNumber();
if (!empty($irnumber)) {
?>
 <tr>
  <td>
   <?php echo xlt('Tentative Invoice Ref No'); ?>:
  </td>
  <td>
   <?php echo text($irnumber); ?>
  </td>
 </tr>
<?php
}
// Otherwise if there is an invoice reference number mask, ask for the refno.
else if (!empty($GLOBALS['gbl_mask_invoice_number'])) {
?>
 <tr>
  <td>
   <?php echo xlt('Invoice Reference Number'); ?>:
  </td>
  <td>
   <input type='text' name='form_irnumber' size='10' value=''
    onkeyup='maskkeyup(this,"<?php echo addslashes($GLOBALS['gbl_mask_invoice_number']); ?>")'
    onblur='maskblur(this,"<?php echo addslashes($GLOBALS['gbl_mask_invoice_number']); ?>")'
    />
  </td>
 </tr>
<?php
}
?>

 <tr>
  <td colspan='2' align='center'>
   &nbsp;<br>
   <input type='submit' name='form_save' value='<?php echo xla('Save'); ?>' /> &nbsp;
<?php if (empty($_GET['framed'])) { ?>
   <input type='button' value='<?php echo xla('Cancel'); ?>' onclick='window.close()' />
<?php } ?>
   <input type='hidden' name='form_provider'  value='<?php echo attr($inv_provider)  ?>' />
   <input type='hidden' name='form_payer'     value='<?php echo attr($inv_payer)     ?>' />
   <input type='hidden' name='form_encounter' value='<?php echo attr($inv_encounter) ?>' />
  </td>
 </tr>

</table>
</center>

</form>

<script language='JavaScript'>
 Calendar.setup({inputField:"form_date", ifFormat:"%Y-%m-%d", button:"img_date"});
 computeTotals();
 
<?php
if ($gcac_related_visit && !$gcac_service_provided) {
  // Skip this warning if the GCAC visit form is not allowed.
  $grow = sqlQuery("SELECT COUNT(*) AS count FROM list_options " .
    "WHERE list_id = 'lbfnames' AND option_id = 'LBFgcac' AND activity = 1");
  if (!empty($grow['count'])) { // if gcac is used
    // Skip this warning if referral or abortion in TS.
    $grow = sqlQuery("SELECT COUNT(*) AS count FROM transactions " .
      "WHERE title = 'Referral' AND refer_date IS NOT NULL AND " .
      "refer_date = ? AND pid = ?", array($inv_date,$patient_id) );
    if (empty($grow['count'])) { // if there is no referral
      $grow = sqlQuery("SELECT COUNT(*) AS count FROM forms " .
        "WHERE pid = ? AND encounter = ? AND " .
        "deleted = 0 AND formdir = 'LBFgcac'", array($patient_id,$inv_encounter) );
      if (empty($grow['count'])) { // if there is no gcac form
        echo " alert('" . addslashes(xl('This visit will need a GCAC form, referral or procedure service.')) . "');\n";
      }
    }
  }
} // end if ($gcac_related_visit)
?>
</script>

</body>
</html>

