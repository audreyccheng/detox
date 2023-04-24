<?php
include_once("../../globals.php");
include_once("$srcdir/sql.inc");

// This may be more appropriate to move to the library
// later
require_once("{$GLOBALS['srcdir']}/sql.inc");
function getInsuranceCompanies($pid) {
  $res = sqlStatement("SELECT * FROM insurance_data WHERE pid = '$pid' " .
    "ORDER BY type ASC, date DESC");
  $prevtype = '';
  for($iter = 0; $row = sqlFetchArray($res); $iter++) {
    if (strcmp($row['type'], $prevtype) == 0) continue;
    $prevtype = $row['type'];
    $all[$iter] = $row;
  }
  return $all;
}

//the number of rows to display before resetting and starting a new column:
$N=10
?>
<html>
<head>
<script type="text/javascript">
function cleartext(atrib)
{
document.copay_form.code.value=document.copay_form.codeH.value;
document.copay_form.codeH.value="";
}
</script>


<?php html_header_show();?>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
</head>
<body class="body_bottom">

<table border='0' cellspacing='0' cellpadding='0' height='100%'>
<tr>

<td valign=top>

<dl>

<form method='post' name='copay_form' action="diagnosis.php?mode=add&type=COPAY&text=copay"
 target='Diagnosis' onsubmit='return top.restoreSession()'>

<dt><span class=title><?php xl('Copay','e'); ?></span></dt>

<br>
<input type=hidden name=code>
<span class='text'><?php xl('$','e'); ?> </span><input type='entry' name='codeH' value='' size='5' />

<input type="SUBMIT" value="<?php xl('Save','e');?>" onclick="cleartext('clear')"><br><br>


<div<?php if ($GLOBALS['simplified_copay']) echo " style='display:none;'"; ?>>
<input type="RADIO" name="payment_method" value="cash" checked><?php xl('cash','e'); ?>
<input type="RADIO" name="payment_method" value="credit card"><?php xl('credit','e'); ?>
<input type="RADIO" name="payment_method" value="check"><?php xl('check','e'); ?>
<input type="RADIO" name="payment_method" value="other"><?php xl('other','e'); ?><br><br>
<input type="RADIO" name="payment_method" value="insurance"><?php xl('insurance','e'); ?>
<?php
if ($ret=getInsuranceCompanies($pid)) {
	if (sizeof($ret)>0) {
		echo "<select name=insurance_company>\n";
		foreach($ret as $iter) {
			$plan_name = trim($iter['plan_name']);
			if ($plan_name != '') {
				echo "<option value='"
				.$plan_name
				."'>".$plan_name ."\n";
			}
		}
		echo "</select>\n";
	}
}
?>
<br><br>
<input type="RADIO" name="payment_method" value="write off"><?php xl('write off','e'); ?>

</div>

</form>

</dl>

</td>
</tr>
</table>

</body>
</html>
