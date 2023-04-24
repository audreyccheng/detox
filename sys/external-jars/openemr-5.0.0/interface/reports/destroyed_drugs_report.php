<?php
 // Copyright (C) 2006-2016 Rod Roark <rod@sunsetsystems.com>
 //
 // This program is free software; you can redistribute it and/or
 // modify it under the terms of the GNU General Public License
 // as published by the Free Software Foundation; either version 2
 // of the License, or (at your option) any later version.

 // This report lists destroyed drug lots within a specified date
 // range.

 require_once("../globals.php");
 require_once("$srcdir/patient.inc");
 require_once("../drugs/drugs.inc.php");
 require_once("$srcdir/formatting.inc.php");

 $form_from_date  = fixDate($_POST['form_from_date'], date('Y-01-01'));
 $form_to_date    = fixDate($_POST['form_to_date']  , date('Y-m-d'));
?>
<html>
<head>
<?php html_header_show();?>
<title><?php xl('Destroyed Drugs','e'); ?></title>
<link rel='stylesheet' href='<?php echo $css_header ?>' type='text/css'>

<style  type="text/css">@import url(../../library/dynarch_calendar.css);</style>

<style>
table.mymaintable, table.mymaintable td, table.mymaintable th {
 border: 1px solid #aaaaaa;
 border-collapse: collapse;
}
table.mymaintable td, table.mymaintable th {
 padding: 1pt 4pt 1pt 4pt;
}
</style>

<script type="text/javascript" src="../../library/textformat.js?v=<?php echo $v_js_includes; ?>"></script>
<script type="text/javascript" src="../../library/dynarch_calendar.js"></script>
<?php include_once("{$GLOBALS['srcdir']}/dynarch_calendar_en.inc.php"); ?>
<script type="text/javascript" src="../../library/dynarch_calendar_setup.js"></script>
<script type="text/javascript" src="../../library/dialog.js?v=<?php echo $v_js_includes; ?>"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-9-1/index.js"></script>
<script type="text/javascript" src="../../library/js/report_helper.js?v=<?php echo $v_js_includes; ?>"></script>

<script language="JavaScript">

 var mypcc = '<?php echo $GLOBALS['phone_country_code'] ?>';

$(document).ready(function() {
  oeFixedHeaderSetup(document.getElementById('mymaintable'));
  var win = top.printLogSetup ? top : opener.top;
  win.printLogSetup(document.getElementById('printbutton'));
});

</script>
</head>

<body leftmargin='0' topmargin='0' marginwidth='0' marginheight='0'>

<center>

<h2><?php xl('Destroyed Drugs','e'); ?></h2>

<form name='theform' method='post' action='destroyed_drugs_report.php'>

<table border='0' cellpadding='3'>

 <tr>
  <td>
   <?php xl('From','e'); ?>:
   <input type='text' name='form_from_date' id='form_from_date'
    size='10' value='<?php echo $form_from_date ?>'
    onkeyup='datekeyup(this,mypcc)' onblur='dateblur(this,mypcc)' title=<?php xl('yyyy-mm-dd','e','\'','\''); ?>>
   <img src='../pic/show_calendar.gif' align='absbottom' width='24' height='22'
    id='img_from_date' border='0' alt='[?]' style='cursor:pointer'
    title=<?php xl('Click here to choose a date','e','\'','\''); ?>>

   &nbsp;<?php xl('To','e'); ?>:
   <input type='text' name='form_to_date' id='form_to_date'
    size='10' value='<?php echo $form_to_date ?>'
    onkeyup='datekeyup(this,mypcc)' onblur='dateblur(this,mypcc)' title=<?php xl('yyyy-mm-dd','e','\'','\''); ?>>
   <img src='../pic/show_calendar.gif' align='absbottom' width='24' height='22'
    id='img_to_date' border='0' alt='[?]' style='cursor:pointer'
    title=<?php xl('Click here to choose a date','e','\'','\''); ?>>

   &nbsp;
   <input type='submit' name='form_refresh' value=<?php xl('Refresh','e'); ?>>
   &nbsp;
   <input type='button' value='<?php echo xla('Print'); ?>' id='printbutton' />
  </td>
 </tr>

 <tr>
  <td height="1">
  </td>
 </tr>

</table>

<table width='98%' id='mymaintable' class='mymaintable'>
 <thead>
 <tr bgcolor="#dddddd">
  <td class='dehead'>
   <?php xl('Drug Name','e'); ?>
  </td>
  <td class='dehead'>
   <?php xl('NDC','e'); ?>
  </td>
  <td class='dehead'>
   <?php xl('Lot','e'); ?>
  </td>
  <td class='dehead'>
   <?php xl('Qty','e'); ?>
  </td>
  <td class='dehead'>
   <?php xl('Date Destroyed','e'); ?>
  </td>
  <td class='dehead'>
   <?php xl('Method','e'); ?>
  </td>
  <td class='dehead'>
   <?php xl('Witness','e'); ?>
  </td>
  <td class='dehead'>
   <?php xl('Notes','e'); ?>
  </td>
 </tr>
 </thead>
 <tbody>
<?php
 if ($_POST['form_refresh']) {
  $where = "i.destroy_date >= '$form_from_date' AND " .
   "i.destroy_date <= '$form_to_date'";

  $query = "SELECT i.inventory_id, i.lot_number, i.on_hand, i.drug_id, " .
   "i.destroy_date, i.destroy_method, i.destroy_witness, i.destroy_notes, " .
   "d.name, d.ndc_number " .
   "FROM drug_inventory AS i " .
   "LEFT OUTER JOIN drugs AS d ON d.drug_id = i.drug_id " .
   "WHERE $where " .
   "ORDER BY d.name, i.drug_id, i.destroy_date, i.lot_number";

  // echo "<!-- $query -->\n"; // debugging
  $res = sqlStatement($query);

  $last_drug_id = 0;
  while ($row = sqlFetchArray($res)) {
   $drug_name       = $row['name'];
   $ndc_number      = $row['ndc_number'];
   if ($row['drug_id'] == $last_drug_id) {
    $drug_name  = '&nbsp;';
    $ndc_number = '&nbsp;';
   }
?>
 <tr>
  <td class='detail'>
   <?php echo $drug_name ?>
  </td>
  <td class='detail'>
   <?php echo $ndc_number ?>
  </td>
  <td class='detail'>
   <a href='../drugs/destroy_lot.php?drug=<?php echo $row['drug_id'] ?>&lot=<?php echo $row['inventory_id'] ?>'
    style='color:#0000ff' target='_blank'>
   <?php echo $row['lot_number'] ?>
   </a>
  </td>
  <td class='detail'>
   <?php echo $row['on_hand'] ?>
  </td>
  <td class='detail'>
   <?php echo oeFormatShortDate($row['destroy_date']) ?>
  </td>
  <td class='detail'>
   <?php echo $row['destroy_method'] ?>
  </td>
  <td class='detail'>
   <?php echo $row['destroy_witness'] ?>
  </td>
  <td class='detail'>
   <?php echo $row['destroy_notes'] ?>
  </td>
 </tr>
<?php
   $last_drug_id = $row['drug_id'];
  } // end while
 } // end if
?>

 </tbody>
</table>
</form>
</center>
<script language='JavaScript'>
 Calendar.setup({inputField:"form_from_date", ifFormat:"%Y-%m-%d", button:"img_from_date"});
 Calendar.setup({inputField:"form_to_date", ifFormat:"%Y-%m-%d", button:"img_to_date"});
</script>
</body>
</html>
