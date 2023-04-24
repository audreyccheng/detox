<?php
//------------Forms generated from formsWiz
include_once(dirname(__FILE__).'/../../globals.php');
include_once($GLOBALS["srcdir"]."/api.inc");
function reviewofs_report( $pid, $encounter, $cols, $id) {
$count = 0;
$data = formFetch("form_reviewofs", $id);
if ($data) {
print "<table><tr>";
foreach($data as $key => $value) {
if ($key == "id" || $key == "pid" || $key == "user" || $key == "groupname" || $key == "authorized" || $key == "activity" || $key == "date" || $value == "" || $value == "0000-00-00 00:00:00") {
	continue;
}
if ($value == "on") {
$value = "yes";
}
$key=ucwords(str_replace("_"," ",$key));
    
//modified by BM 07-2009 for internationalization
if ($key == "Additional Notes") {
        print "<td><span class=bold>" . xl($key) . ": </span><span class=text>" . text($value) . "</span></td>";
}
else {
        print "<td><span class=bold>" . xl($key) . ": </span><span class=text>" . xl($value) . "</span></td>";
}
    
$count++;
if ($count == $cols) {
$count = 0;
print "</tr><tr>\n";
}
}
}
print "</tr></table>";
}
?> 
