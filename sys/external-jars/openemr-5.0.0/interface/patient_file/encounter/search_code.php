<?php

////////////////////////////////////////////////////////////////////////////////
// THIS MODULE REPLACES cptcm_codes.php, hcpcs_codes.php AND icd9cm_codes.php.
////////////////////////////////////////////////////////////////////////////////

include_once("../../globals.php");
include_once("../../../custom/code_types.inc.php");
include_once("$srcdir/sql.inc");

//the maximum number of records to pull out with the search:
$M = 30;

//the number of records to display before starting a second column:
$N = 15;

$code_type = $_GET['type'];
?>

<html>
<head>
<?php html_header_show();?>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">

<!-- add jQuery support -->
<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-2-2/index.js"></script>

</head>
<body class="body_bottom">
<div id="patient_search_code">

<table border=0 cellspacing=0 cellpadding=0 height=100%>
<tr>

<td valign=top>

<form name="search_form" id="search_form" method="post" action="search_code.php?type=<?php echo $code_type ?>">
<input type="hidden" name="mode" value="search">

<span class="title"><?php echo $code_type ?> <?php xl('Codes','e'); ?></span><br>

<input type="textbox" id="text" name="text" size=15>

<input type='submit' id="submitbtn" name="submitbtn" value='<?php xl('Search','e'); ?>'>
<div id="searchspinner" style="display: inline; visibility:hidden;"><img src="<?php echo $GLOBALS['webroot'] ?>/interface/pic/ajax-loader.gif"></div>

</form>

<?php
if (isset($_POST["mode"]) && $_POST["mode"] == "search" && $_POST["text"] == "") {
    echo "<div id='resultsummary' style='background-color:lightgreen;'>";
    echo "Enter search criteria above</div>";
}

if (isset($_POST["mode"]) && $_POST["mode"] == "search" && $_POST["text"] != "") {
  // $sql = "SELECT * FROM codes WHERE (code_text LIKE '%" . $_POST["text"] .
  //   "%' OR code LIKE '%" . $_POST["text"] . "%') AND code_type = '" .
  //   $code_types[$code_type]['id'] . "' ORDER BY code LIMIT " . ($M + 1);

  // The above is obsolete now, fees come from the prices table:
  $sql = "SELECT codes.*, prices.pr_price FROM codes " .
    "LEFT OUTER JOIN patient_data ON patient_data.pid = '$pid' " .
    "LEFT OUTER JOIN prices ON prices.pr_id = codes.id AND " .
    "prices.pr_selector = '' AND " .
    "prices.pr_level = patient_data.pricelevel " .
    "WHERE (code_text LIKE '%" . $_POST["text"] . "%' OR " .
    "code LIKE '%" . $_POST["text"] . "%') AND " .
    "code_type = '" . $code_types[$code_type]['id'] . "' " .
    "ORDER BY code ".
    " LIMIT " . ($M + 1).
    "";

	if ($res = sqlStatement($sql) ) {
		for($iter=0; $row=sqlFetchArray($res); $iter++)
		{
			$result[$iter] = $row;
		}
        echo "<div id='resultsummary' style='background-color:lightgreen;'>";
        if (count($result) > $M) {
            echo "Showing the first ".$M." results";
        }
        else if (count($result) == 0) {
            echo "No results found";
        }
        else {
            echo "Showing all ".count($result)." results";
        }
        echo "</div>";
?>
<div id="results">
<table><tr class='text'><td valign='top'>
<?php
$count = 0;
$total = 0;

if ($result) {
    foreach ($result as $iter) {
        if ($count == $N) {
            echo "</td><td valign='top'>\n";
            $count = 0;
        }
   
        echo "<div class='oneresult' style='padding: 3px 0px 3px 0px;'>";
        echo "<a target='".xl('Diagnosis')."' href='diagnosis.php?mode=add" .
            "&type="     . urlencode($code_type) .
            "&code="     . urlencode($iter{"code"}) .
            "&modifier=" . urlencode($iter{"modifier"}) .
            "&units="    . urlencode($iter{"units"}) .
            // "&fee="      . urlencode($iter{"fee"}) .
            "&fee="      . urlencode($iter['pr_price']) .
            "&text="     . urlencode($iter{"code_text"}) .
            "' onclick='top.restoreSession()'>";
        echo ucwords("<b>" . strtoupper($iter{"code"}) . "&nbsp;" . $iter['modifier'] .
            "</b>" . " " . strtolower($iter{"code_text"}));
        echo "</a><br>\n";
        echo "</div>";
    
        $count++;
        $total++;
        
        if ($total == $M) {
            echo "</span><span class=alert>".xl('Some codes were not displayed.')."</span>\n";
            break;
        }
    }
}
?>
</td></tr></table>
</div>
<?php
	}
}
?>

</td>
</tr>
</table>

</div> <!-- end large outer patient_search_code DIV -->
</body>

<script language="javascript">

// jQuery stuff to make the page a little easier to use

$(document).ready(function(){
    $("#text").focus();
    $(".oneresult").mouseover(function() { $(this).toggleClass("highlight"); });
    $(".oneresult").mouseout(function() { $(this).toggleClass("highlight"); });
    //$(".oneresult").click(function() { SelectPatient(this); });
    $("#search_form").submit(function() { SubmitForm(this); });
});

// show the 'searching...' status and submit the form
var SubmitForm = function(eObj) {
    $("#submitbtn").attr("disabled", "true"); 
    $("#submitbtn").css("disabled", "true");
    $("#searchspinner").css("visibility", "visible");
    return top.restoreSession();
}

</script>

</html>
