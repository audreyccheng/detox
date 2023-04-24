<?php
include_once("../../globals.php");
include_once("../../../custom/code_types.inc.php");
include_once("$srcdir/billing.inc");
include_once("$srcdir/sql.inc");
require_once("$srcdir/formdata.inc.php");

//the number of rows to display before resetting and starting a new column:
$N=10;

$mode     = $_GET['mode'];
$type     = $_GET['type'];
$modifier = $_GET['modifier'];
$units    = $_GET['units'];
$fee      = $_GET['fee'];
$code     = $_GET['code'];
$text     = $_GET['text'];

if (isset($mode)) {
	if ($mode == "add") {
		if (strtolower($type) == "copay") {
			addBilling($encounter, $type, sprintf("%01.2f", $code), strip_escape_custom($text), $pid, $userauthorized,$_SESSION['authUserID'],$modifier,$units,sprintf("%01.2f", 0 - $code));
		}
		elseif (strtolower($type) == "other") {
			addBilling($encounter, $type, $code, strip_escape_custom($text), $pid, $userauthorized,$_SESSION['authUserID'],$modifier,$units,sprintf("%01.2f", $fee));
		}
		else {
			addBilling($encounter, $type, $code, strip_escape_custom($text), $pid, $userauthorized,$_SESSION['authUserID'],$modifier,$units,$fee);
		}
	}
}
?>
<html>
<head>
<?php html_header_show();?>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
</head>
<body class="body_bottom">

<table border=0 cellspacing=0 cellpadding=0 >
<tr>
<td valign=top>

<dl>

<dt>

<a href="superbill_custom_full.php" onclick="top.restoreSession()">
<span class=title><?php xl('Superbill','e'); ?></span>
<font class=more><?php echo $tmore;?></font></a>

<a href="encounter_bottom.php" onclick="top.restoreSession()">

<font class=more><?php echo $tback;?></font></a>

</dt>
</td></tr>
</table>

<table border=0 width=100% cellpadding=0 cellspacing=1>
<?php
$res = sqlStatement("select * from codes where superbill = 1 order by code_type, code, code_text");

$codes = array();
echo " <tr>\n";
foreach ($code_types as $key => $value) {
	$codes[$key] = array();
	echo "  <th align='left'>$key Codes</th>\n";
}
echo " </tr>\n";

for ($iter = 0; $row = sqlFetchArray($res); $iter++){
	foreach ($code_types as $key => $value) {
		if ($value['id'] == $row['code_type']) {
			$codes[$key][] = $row;
			break;
		}
	}
}

$index=0;

$numlines = 0;
foreach ($codes as $value)
	$numlines = max($numlines, count($value));

while ($index < $numlines) {
	echo " <tr>\n";
	foreach ($codes as $key => $value) {
		echo "  <td valign='top'>\n";
		if(!empty($value[$index])) {
			$code = $value[$index];
			echo "   <dd><a class='text' ";
			echo "href='superbill_codes.php?back=1&mode=add" .
				"&type="     . urlencode($key) .
				"&modifier=" . urlencode($code{"modifier"}) .
				"&units="    . urlencode($code{"units"}) .
				"&fee="      . urlencode($code{"fee"}) .
				"&code="     . urlencode($code{"code"}) .
				"&text="     . urlencode($code{"code_text"}) .
        "' onclick='top.restoreSession()'>";
			echo "<b>" . $code['code'] . "</b>" . "&nbsp;" . $code['modifier'] . "&nbsp;" . $code['code_text'] ;
			echo "</a></dd>\n";
		}
		echo "  </td>\n";
	}
	echo " </tr>\n";
	++$index;
}

?>

</table>

</dl>

</body>
</html>
