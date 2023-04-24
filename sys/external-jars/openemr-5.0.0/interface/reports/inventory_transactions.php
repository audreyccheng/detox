<?php
// Copyright (C) 2010-2016 Rod Roark <rod@sunsetsystems.com>
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This is an inventory transactions list.

//SANITIZE ALL ESCAPES
$sanitize_all_escapes=true;
//

//STOP FAKE REGISTER GLOBALS
$fake_register_globals=false;
//

require_once("../globals.php");
require_once("$srcdir/patient.inc");
require_once("$srcdir/acl.inc");
require_once("$srcdir/formatting.inc.php");

function bucks($amount) {
  if ($amount != 0) return oeFormatMoney($amount);
  return '';
}

function esc4Export($str) {
  return str_replace('"', '\\"', $str);
}

function thisLineItem($row, $xfer=false) {
  global $grandtotal, $grandqty, $encount, $form_action;

  $invnumber = '';
  $dpname = '';

  if (!empty($row['pid'])) {
    $ttype = xl('Sale');
    $dpname = $row['plname'];
    if (!empty($row['pfname'])) {
      $dpname .= ', ' . $row['pfname'];
      if (!empty($row['pmname'])) $dpname .= ' ' . $row['pmname'];
    }
    $invnumber = empty($row['invoice_refno']) ?
      "{$row['pid']}.{$row['encounter']}" : $row['invoice_refno'];
  }
  else if (!empty($row['distributor_id'])) {
    $ttype = xl('Distribution');
    if (!empty($row['organization'])) {
      $dpname = $row['organization'];
    }
    else {
      $dpname = $row['dlname'];
      if (!empty($row['dfname'])) {
        $dpname .= ', ' . $row['dfname'];
        if (!empty($row['dmname'])) $dpname .= ' ' . $row['dmname'];
      }
    }
  }
  else if (!empty($row['xfer_inventory_id']) || $xfer) {
    $ttype = xl('Transfer');
  }
  else if ($row['fee'] != 0) {
    $ttype = xl('Purchase');
  }
  else {
    $ttype = xl('Adjustment');
  }

  if ($form_action == 'export') {
    echo '"' . oeFormatShortDate($row['sale_date']) . '",';
    echo '"' . $ttype                               . '",';
    echo '"' . esc4Export($row['name'])             . '",';
    echo '"' . esc4Export($row['lot_number'])       . '",';
    echo '"' . esc4Export($row['warehouse'])        . '",';
    echo '"' . esc4Export($dpname)                  . '",';
    echo '"' . (0 - $row['quantity'])               . '",';
    echo '"' . bucks($row['fee'])                   . '",';
    echo '"' . $row['billed']                       . '",';
    echo '"' . esc4Export($row['notes'])            . '"' . "\n";
  }
  else {
    $bgcolor = (++$encount & 1) ? "#ddddff" : "#ffdddd";
?>

 <tr bgcolor="<?php echo $bgcolor; ?>">
  <td class="detail">
   <?php echo htmlspecialchars(oeFormatShortDate($row['sale_date'])); ?>
  </td>
  <td class="detail">
   <?php echo htmlspecialchars($ttype); ?>
  </td>
  <td class="detail">
   <?php echo htmlspecialchars($row['name']); ?>
  </td>
  <td class="detail">
   <?php echo htmlspecialchars($row['lot_number']); ?>
  </td>
  <td class="detail">
   <?php echo htmlspecialchars($row['warehouse']); ?>
  </td>
  <td class="detail">
   <?php echo htmlspecialchars($dpname); ?>
  </td>
  <td class="detail" align="right">
   <?php echo htmlspecialchars(0 - $row['quantity']); ?>
  </td>
  <td class="detail" align="right">
   <?php echo htmlspecialchars(bucks($row['fee'])); ?>
  </td>
  <td class="detail" align="center">
   <?php echo empty($row['billed']) ? '&nbsp;' : '*'; ?>
  </td>
  <td class="detail">
   <?php echo htmlspecialchars($row['notes']); ?>
  </td>
 </tr>
<?php
  } // End not csv export

  $grandtotal   += $row['fee'];
  $grandqty     -= $row['quantity'];

  // In the special case of a transfer, generate a second line item for
  // the source lot.
  if (!empty($row['xfer_inventory_id'])) {
    $row['xfer_inventory_id'] = 0;
    $row['lot_number'] = $row['lot_number_2'];
    $row['warehouse'] = $row['warehouse_2'];
    $row['quantity'] = 0 - $row['quantity'];
    $row['fee'] = 0 - $row['fee'];
    thisLineItem($row, true);
  }

} // end function

if (! acl_check('acct', 'rep')) die(htmlspecialchars(xl("Unauthorized access."), ENT_NOQUOTES));

// this is "" or "submit" or "export".
$form_action = $_POST['form_action'];

$form_from_date  = fixDate($_POST['form_from_date'], date('Y-m-d'));
$form_to_date    = fixDate($_POST['form_to_date']  , date('Y-m-d'));
$form_trans_type = isset($_POST['form_trans_type']) ? $_POST['form_trans_type'] : '0';

$encount = 0;

if ($form_action == 'export') {
  header("Pragma: public");
  header("Expires: 0");
  header("Cache-Control: must-revalidate, post-check=0, pre-check=0");
  header("Content-Type: application/force-download");
  header("Content-Disposition: attachment; filename=inventory_transactions.csv");
  header("Content-Description: File Transfer");
  // CSV headers:
  echo '"' . xl('Date'       ) . '",';
  echo '"' . xl('Transaction') . '",';
  echo '"' . xl('Product'    ) . '",';
  echo '"' . xl('Lot'        ) . '",';
  echo '"' . xl('Warehouse'  ) . '",';
  echo '"' . xl('Who'        ) . '",';
  echo '"' . xl('Qty'        ) . '",';
  echo '"' . xl('Amount'     ) . '",';
  echo '"' . xl('Billed'     ) . '",';
  echo '"' . xl('Notes'      ) . '"' . "\n";
} // end export
else {
?>
<html>
<head>
<?php html_header_show(); ?>
<title><?php echo htmlspecialchars(xl('Inventory Transactions'), ENT_NOQUOTES) ?></title>
<link rel='stylesheet' href='<?php echo $css_header ?>' type='text/css'>

<style type="text/css">
 /* specifically include & exclude from printing */
 @media print {
  #report_parameters {visibility: hidden; display: none;}
  #report_parameters_daterange {visibility: visible; display: inline;}
  #report_results {margin-top: 30px;}
 }

 /* specifically exclude some from the screen */
 @media screen {
  #report_parameters_daterange {visibility: hidden; display: none;}
 }

 body       { font-family:sans-serif; font-size:10pt; font-weight:normal }
 .dehead    { color:#000000; font-family:sans-serif; font-size:10pt; font-weight:bold }
 .detail    { color:#000000; font-family:sans-serif; font-size:10pt; font-weight:normal }

 #report_results table thead {
  font-size:10pt;
 }
</style>

<style type="text/css">@import url(../../library/dynarch_calendar.css);</style>
<script type="text/javascript" src="../../library/dynarch_calendar.js"></script>
<?php include_once("{$GLOBALS['srcdir']}/dynarch_calendar_en.inc.php"); ?>
<script type="text/javascript" src="../../library/dynarch_calendar_setup.js"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-9-1/index.js"></script>
<script type="text/javascript" src="../../library/js/report_helper.js?v=<?php echo $v_js_includes; ?>"></script>

<script language='JavaScript'>

 $(document).ready(function() {
  oeFixedHeaderSetup(document.getElementById('mymaintable'));
  var win = top.printLogSetup ? top : opener.top;
  win.printLogSetup(document.getElementById('printbutton'));
 });

 function mysubmit(action) {
  var f = document.forms[0];
  f.form_action.value = action;
  top.restoreSession();
  f.submit();
 }

</script>

</head>

<body leftmargin='0' topmargin='0' marginwidth='0' marginheight='0' class='body_top'>
<center>

<h2><?php echo htmlspecialchars(xl('Inventory Transactions'), ENT_NOQUOTES) ?></h2>

<form method='post' action='inventory_transactions.php'>

<div id="report_parameters">
<!-- form_action is set to "submit" or "export" at form submit time -->
<input type='hidden' name='form_action' value='' />
<table>
 <tr>
  <td width='50%'>
   <table class='text'>
    <tr>
     <td class='label'>
      <?php echo htmlspecialchars(xl('Type'), ENT_NOQUOTES); ?>:
     </td>
     <td nowrap>
      <select name='form_trans_type' onchange='trans_type_changed()'>
<?php
foreach (array(
  '0' => xl('All'),
  '2' => xl('Purchase/Return'),
  '1' => xl('Sale'),
  '6' => xl('Distribution'),
  '4' => xl('Transfer'),
  '5' => xl('Adjustment'),
) as $key => $value)
{
  echo "       <option value='$key'";
  if ($key == $form_trans_type) echo " selected";
  echo ">" . htmlspecialchars($value, ENT_NOQUOTES) . "</option>\n";
}
?>
      </select>
     </td>
     <td class='label'>
      <?php echo htmlspecialchars(xl('From'), ENT_NOQUOTES); ?>:
     </td>
     <td nowrap>
      <input type='text' name='form_from_date' id="form_from_date" size='10'
       value='<?php echo htmlspecialchars($form_from_date, ENT_QUOTES) ?>'
       title='<?php echo htmlspecialchars(xl('yyyy-mm-dd'), ENT_QUOTES) ?>'
       onkeyup='datekeyup(this,mypcc)' onblur='dateblur(this,mypcc)'>
      <img src='../pic/show_calendar.gif' align='absbottom' width='24' height='22'
       id='img_from_date' border='0' alt='[?]' style='cursor:pointer'
       title='<?php echo htmlspecialchars(xl('Click here to choose a date'), ENT_QUOTES); ?>'>
     </td>
     <td class='label'>
      <?php xl('To','e'); ?>:
     </td>
     <td nowrap>
      <input type='text' name='form_to_date' id="form_to_date" size='10'
       value='<?php echo htmlspecialchars($form_to_date, ENT_QUOTES) ?>'
       title='<?php echo htmlspecialchars(xl('yyyy-mm-dd'), ENT_QUOTES) ?>'
       onkeyup='datekeyup(this,mypcc)' onblur='dateblur(this,mypcc)'>
      <img src='../pic/show_calendar.gif' align='absbottom' width='24' height='22'
       id='img_to_date' border='0' alt='[?]' style='cursor:pointer'
       title='<?php echo htmlspecialchars(xl('Click here to choose a date'), ENT_QUOTES); ?>'>
     </td>
    </tr>
   </table>
  </td>
  <td align='left' valign='middle'>
   <table style='border-left:1px solid; width:100%; height:100%'>
    <tr>
     <td valign='middle'>
      <a href='#' class='css_button' onclick='mysubmit("submit")' style='margin-left:1em'>
       <span><?php echo htmlspecialchars(xl('Submit'), ENT_NOQUOTES); ?></span>
      </a>
<?php if ($form_action) { ?>
      <a href='#' class='css_button' id='printbutton' style='margin-left:1em'>
       <span><?php echo htmlspecialchars(xl('Print'), ENT_NOQUOTES); ?></span>
      </a>
      <a href='#' class='css_button' onclick='mysubmit("export")' style='margin-left:1em'>
       <span><?php echo htmlspecialchars(xl('CSV Export'), ENT_NOQUOTES); ?></span>
      </a>
<?php } ?>
     </td>
    </tr>
   </table>
  </td>
 </tr>
</table>
</div>

<?php if ($form_action) { // if submit (already not export here) ?>

<div id="report_results">
<table border='0' cellpadding='1' cellspacing='2' width='98%' id='mymaintable' class='mymaintable'>
 <thead>
 <tr bgcolor="#dddddd">
  <td class="dehead">
   <?php echo htmlspecialchars(xl('Date'), ENT_NOQUOTES); ?>
  </td>
  <td class="dehead">
   <?php echo htmlspecialchars(xl('Transaction'), ENT_NOQUOTES); ?>
  </td>
  <td class="dehead">
   <?php echo htmlspecialchars(xl('Product'), ENT_NOQUOTES); ?>
  </td>
  <td class="dehead">
   <?php echo htmlspecialchars(xl('Lot'), ENT_NOQUOTES); ?>
  </td>
  <td class="dehead">
   <?php echo htmlspecialchars(xl('Warehouse'), ENT_NOQUOTES); ?>
  </td>
  <td class="dehead">
   <?php echo htmlspecialchars(xl('Who'), ENT_NOQUOTES); ?>
  </td>
  <td class="dehead" align="right">
   <?php echo htmlspecialchars(xl('Qty'), ENT_NOQUOTES); ?>
  </td>
  <td class="dehead" align="right">
   <?php echo htmlspecialchars(xl('Amount'), ENT_NOQUOTES); ?>
  </td>
  <td class="dehead" align="Center">
   <?php echo htmlspecialchars(xl('Billed'), ENT_NOQUOTES); ?>
  </td>
  <td class="dehead">
   <?php echo htmlspecialchars(xl('Notes'), ENT_NOQUOTES); ?>
  </td>
 </tr>
 </thead>
 <tbody>
<?php
} // end if submit
} // end not export

if ($form_action) { // if submit or export
  $from_date = $form_from_date;
  $to_date   = $form_to_date;

  $grandtotal = 0;
  $grandqty = 0;

  $query = "SELECT s.sale_date, s.fee, s.quantity, s.pid, s.encounter, " .
    "s.billed, s.notes, s.distributor_id, s.xfer_inventory_id, " .
    "p.fname AS pfname, p.mname AS pmname, p.lname AS plname, " .
    "u.fname AS dfname, u.mname AS dmname, u.lname AS dlname, u.organization, " .
    "d.name, fe.date, fe.invoice_refno, " .
    "i1.lot_number, i2.lot_number AS lot_number_2, " .
    "lo1.title AS warehouse, lo2.title AS warehouse_2 " .
    "FROM drug_sales AS s " .
    "JOIN drugs AS d ON d.drug_id = s.drug_id " .
    "LEFT JOIN drug_inventory AS i1 ON i1.inventory_id = s.inventory_id " .
    "LEFT JOIN drug_inventory AS i2 ON i2.inventory_id = s.xfer_inventory_id " .
    "LEFT JOIN patient_data AS p ON p.pid = s.pid " .
    "LEFT JOIN users AS u ON u.id = s.distributor_id " .
    "LEFT JOIN list_options AS lo1 ON lo1.list_id = 'warehouse' AND " .
    "lo1.option_id = i1.warehouse_id AND lo1.activity = 1 " .
    "LEFT JOIN list_options AS lo2 ON lo2.list_id = 'warehouse' AND " .
    "lo2.option_id = i2.warehouse_id AND lo2.activity = 1 " .
    "LEFT JOIN form_encounter AS fe ON fe.pid = s.pid AND fe.encounter = s.encounter " .
    "WHERE s.sale_date >= ? AND s.sale_date <= ? ";
  if ($form_trans_type == 2) { // purchase/return
    $query .= "AND s.pid = 0 AND s.distributor_id = 0 AND s.xfer_inventory_id = 0 AND s.fee != 0 ";
  }
  else if ($form_trans_type == 4) { // transfer
    $query .= "AND s.xfer_inventory_id != 0 ";
  }
  else if ($form_trans_type == 5) { // adjustment
    $query .= "AND s.pid = 0 AND s.distributor_id = 0 AND s.xfer_inventory_id = 0 AND s.fee = 0 ";
  }
  else if ($form_trans_type == 6) { // distribution
    $query .= "AND s.distributor_id != 0 ";
  }
  else if ($form_trans_type == 1) { // sale
    $query .= "AND s.pid != 0 ";
  }
  $query .= "ORDER BY s.sale_date, s.sale_id";
  //
  $res = sqlStatement($query, array($from_date, $to_date));
  while ($row = sqlFetchArray($res)) {
    thisLineItem($row);
  }

  // Grand totals line.
  if ($form_action != 'export') { // if submit
?>

 <tr bgcolor="#dddddd">
  <td class="dehead" colspan="6">
   <?php echo htmlspecialchars(xl('Grand Total'), ENT_NOQUOTES); ?>
  </td>
  <td class="dehead" align="right">
   <?php echo htmlspecialchars($grandqty, ENT_NOQUOTES); ?>
  </td>
  <td class="dehead" align="right">
   <?php echo htmlspecialchars(bucks($grandtotal), ENT_NOQUOTES); ?>
  </td>
  <td class="dehead" colspan="2">

  </td>
 </tr>

<?php
  } // End if submit
} // end if submit or export

if ($form_action != 'export') {
  if ($form_action) {
?>
 </tbody>
</table>
</div>
<?php
  } // end if ($form_action)
?>

</form>
</center>
</body>

<!-- stuff for the popup calendar -->
<script language="Javascript">
 Calendar.setup({inputField:"form_from_date", ifFormat:"%Y-%m-%d", button:"img_from_date"});
 Calendar.setup({inputField:"form_to_date", ifFormat:"%Y-%m-%d", button:"img_to_date"});
</script>

</html>
<?php
} // End not export
?>
