<?php
/**
* Encounter form to track any clinical parameter.
*
* Copyright (C) 2014 Joe Slam <trackanything@produnis.de>
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
* along with this program. If not, see <http://opensource.org/licenses/gpl-license.php>.
*
* @package OpenEMR
* @author Joe Slam <trackanything@produnis.de>
* @link http://www.open-emr.org
*/

// Some initial api-inputs
$sanitize_all_escapes  = true;
$fake_register_globals = false;
require_once("../../globals.php");
require_once("$srcdir/api.inc");
require_once("$srcdir/forms.inc");
require_once("$srcdir/acl.inc");
formHeader("Form: Track anything");

// check if we are inside an encounter
if (! $encounter) { // comes from globals.php
 die("Internal error: we do not seem to be in an encounter!");
}

// get vars posted by FORMs
if (!$formid){
	$formid = $_GET['id'];
	if (!$formid){ $formid = $_POST['formid'];	}
}

$myprocedureid =  $_POST['procedure2track'];

echo "<html><head>";
?> 
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
<link rel="stylesheet" href="<?php echo $web_root; ?>/interface/forms/track_anything/style.css" type="text/css">  
<style type="text/css">@import url(../../../library/dynarch_calendar.css);</style>
<script type="text/javascript" src="../../../library/textformat.js"></script>
<script type="text/javascript" src="../../../library/dynarch_calendar.js"></script>
<?php include_once("{$GLOBALS['srcdir']}/dynarch_calendar_en.inc.php"); ?>
<script type="text/javascript" src="../../../library/dynarch_calendar_setup.js"></script>
<script type="text/javascript" src="../../../library/dialog.js?v=<?php echo $v_js_includes; ?>"></script>

<?php 
echo "</head><body class='body_top'>";
echo "<div id='track_anything'>";

// check if this Track is new
if (!$formid){
	// this is a new Track

	// check if procedure is selcted
	if ($_POST['bn_select']) {
		// "save"-Button was clicked, saving Form into db

		// save inbto db
		if($myprocedureid){
			$query = "INSERT INTO form_track_anything (procedure_type_id) VALUES (?)";
			$formid = sqlInsert($query, $myprocedureid);
			$spell = "SELECT name FROM form_track_anything_type WHERE track_anything_type_id = ?";
			$myrow = sqlQuery($spell,array($myprocedureid));
			$myprocedurename = $myrow["name"];
			$register_as = "Track: " . $myprocedurename;
			// adding Form
			addForm($encounter, $register_as, $formid, "track_anything", $pid, $userauthorized);
		} else {
				echo xlt('No track selected'). ".<br>";
?><input type='button' value='<?php echo xla('Back'); ?>' onclick="top.restoreSession();location='<?php echo $GLOBALS['form_exit_url']; ?>'" /><?php	
		}

	}else{
	// procedure is not yet selected
		echo "<table>";
		echo "<tr>";
		echo "<th>" . xlt('Select Track') .":</th>";
		echo "</tr><tr>";
		echo "<td>";
		echo "<form method='post' action='" . $rootdir . "/forms/track_anything/new.php' onsubmit='return top.restoreSession()'>";

		echo "<select name='procedure2track' size='10' style='width: 300px'>";
		$spell  = "SELECT * FROM form_track_anything_type ";
		$spell .= "WHERE parent = 0 AND active = 1 ";
		$spell .= "ORDER BY position ASC, name ASC ";
		$testi = sqlStatement($spell);
		while($myrow = sqlFetchArray($testi)){
			$myprocedureid = $myrow["track_anything_type_id"];
			$myprocedurename = $myrow["name"];
			echo "<option value='" . attr($myprocedureid) . "'>" . text($myprocedurename) . "</option>";
		}
		echo "</select>";
		echo "</td></tr><tr><td align='center'>";
		echo "<input type='submit' name='bn_select' value='" . xla('Select') . "' />";
?><input type='button' value='<?php echo  xla('Back'); ?>' onclick="top.restoreSession();location='<?php echo $GLOBALS['form_exit_url']; ?>'" /><?php
		echo "</form>";
		echo "<br>&nbsp;</td></tr>";

		echo "<tr><td align='center'>";
		echo "<input type='submit' name='create_track' value='" . xla('Configure tracks') . "' ";
		?> onclick="top.restoreSession();location='<?php echo $web_root ?>/interface/forms/track_anything/create.php'"<?php 
		echo " />";
		echo "</td></tr>";
		echo "</table>";
	}

}


// instead of "else", we check again for "formid"
if ($formid){
	// this is an existing Track
	//----------------------------------------------------	
	// get submitted item-Ids
	$mylist = $_POST['liste'];
	#echo $mylist;
	$length = count($mylist);
	$thedate = $_POST['datetime'];
	#echo $thedate;
	//check if whole input is NULL
	$all_are_null = 0;
	for($i= 0; $i < $length; $i++){
		#echo "beep";
		$thisid = $mylist[$i];
		$thisvalue = $_POST[$thisid];
		if ($thisvalue != NULL && $thisvalue != '') {
			$all_are_null++;
		}
	}

	// if all of the input is NULL, we do nothing
	// if at least one entrie is NOT NULL, we save all into db
	if ($all_are_null > 0) {
		for($i= 0; $i < $length; $i++){
			$thisid = $mylist[$i];
			$thisvalue = $_POST[$thisid];

			// store data to track_anything_db
			$query = "INSERT INTO form_track_anything_results (track_anything_id, track_timestamp, itemid, result) VALUES (?, ?, ?, ?)";
			sqlInsert($query, array($formid,$thedate,$thisid,$thisvalue));
		}
	}
	//----------------------------------------------------	



	// update corrected old items
	// ---------------------------

	// getting old entries from <form>
	$old_id 	= $_POST['old_id'];
	$old_time 	= $_POST['old_time'];
	$old_value 	= $_POST['old_value'];

	$how_many = count($old_time);
	// do this for each data row	
	for ($x=0; $x<=$how_many; $x++) {
		// how many columns do we have
		$how_many_cols = count($old_value[$x]);
		for($y=0; $y<$how_many_cols; $y++){
				// here goes the UPDATE sql-spruch
				$insertspell  = "UPDATE form_track_anything_results ";
				$insertspell .= "SET track_timestamp = ? , result = ? ";
				$insertspell .= "WHERE id = ? ";
				sqlStatement($insertspell, array($old_time[$x], $old_value[$x][$y], $old_id[$x][$y]));
		}
		
	}
//--------------------------------------------------


	//get procedure ID
	if (!$myprocedureid){
		$spell = "SELECT procedure_type_id FROM form_track_anything WHERE id = ?";
		$myrow = sqlQuery($spell, array($formid));
		$myprocedureid = $myrow["procedure_type_id"];
		
	}
	echo "<br><b>" . xlt('Enter new data') . "</b>:<br>";
	echo "<form method='post' action='" . $rootdir . "/forms/track_anything/new.php' onsubmit='return top.restoreSession()'>";
	echo "<table>";
	echo "<tr><th class='item'>" . xlt('Item') . "</th>";
	echo "<th class='value'>" . xlt('Value') . "</th></tr>";


	echo "<tr><td>" . xlt('Date Time') . "</td>";
	echo "<td><input type='text' size='16' name='datetime' id='datetime'" .
             "value='" . attr(date('Y-m-d H:i:s', time())) . "'" .
             "onkeyup='datekeyup(this,mypcc,true)' onblur='dateblur(this,mypcc,true)' />" .
             "<img src='" . $rootdir . "/pic/show_calendar.gif' id='img_date' align='absbottom'" .
             "width='24' height='22' border='0' alt='[?]' style='cursor:pointer' /></td></tr>";
        ?>
        <script language="javascript">
        Calendar.setup({inputField:"datetime", ifFormat:"%Y-%m-%d %H:%M:%S", button:"img_date", showsTime:true});
        </script>

	<?php
	// get items to track
	$liste = array();
	$spell = "SELECT * FROM form_track_anything_type WHERE parent = ? AND active = 1 ORDER BY position ASC, name ASC ";
	$query = sqlStatement($spell, array($myprocedureid));
	while($myrow = sqlFetchArray($query)){
		echo "<input type='hidden' name='liste[]' value='". attr($myrow['track_anything_type_id']) . "'>";
		echo "<tr><td> " . text($myrow['name']) . "</td>";
		echo "<td><input size='12' type='text' name='" . attr($myrow['track_anything_type_id'])  . "'></td></tr>";
	}

	echo "</table>";
	echo "<input type='hidden' name='formid' value='". attr($formid) . "'>";
	echo "<input type='submit' name='bn_save' value='" . xla('Save') . "' />";
?><input type='button' value='<?php echo  xla('Stop'); ?>' onclick="top.restoreSession();location='<?php echo $GLOBALS['form_exit_url']; ?>'" /><?php


	// show old entries of track
	//-----------------------------------
	// get unique timestamps of track
	echo "<br><br><hr><br>";
	echo "<b>" . xlt('Edit your entered data') . ":</b><br>";
	$shownameflag = 0;	// flag if this is <table>-headline 
	echo "<table border='1'>";

	$spell0 = "SELECT DISTINCT track_timestamp FROM form_track_anything_results WHERE track_anything_id = ? ORDER BY track_timestamp DESC";
	$query = sqlStatement($spell0,array($formid));
	$main_counter=0; // this counts 'number of rows'  of old entries
	while($myrow = sqlFetchArray($query)){
		$thistime = $myrow['track_timestamp'];
		$shownameflag++;
		
		$spell  = "SELECT form_track_anything_results.id AS result_id, form_track_anything_results.itemid, form_track_anything_results.result, form_track_anything_type.name AS the_name ";
		$spell .= "FROM form_track_anything_results ";
		$spell .= "INNER JOIN form_track_anything_type ON form_track_anything_results.itemid = form_track_anything_type.track_anything_type_id ";
		$spell .= "WHERE track_anything_id = ? AND track_timestamp = ? AND form_track_anything_type.active = 1 ";
		$spell .= "ORDER BY form_track_anything_type.position ASC, the_name ASC ";
		$query2  = sqlStatement($spell,array($formid ,$thistime));
		
		// <table> heading line
		if ($shownameflag==1){
			echo "<tr><th class='time'>" . xlt('Time') . "</th>";
			while($myrow2 = sqlFetchArray($query2)){
				echo "<th class='item'>" . text($myrow2['the_name']) . "</th>";
			}
			echo "</tr>";
		}
		
		echo "<tr><td bgcolor=#eeeeec>";
		$main_counter++; // next row
		echo "<input type='text' size='12' name='old_time[" . attr($main_counter) . "]' value='" . attr($thistime) . "'></td>";
		$query2  = sqlStatement($spell,array($formid ,$thistime));
		
		$counter = 0; // this counts columns 
		while($myrow2 = sqlFetchArray($query2)){
			echo "<td>";
			echo "<input type='hidden' name='old_id[" . attr($main_counter) . "][" . attr($counter) . "]' value='". attr($myrow2['result_id']) . "'>";
			echo "<input type='text' size='12' name='old_value[" . attr($main_counter) . "][" . attr($counter) . "]' value='" . attr($myrow2['result']) . "'></td>";
			$counter++; // next cloumn
		}
		echo "</tr>";

	}
	echo "</tr></table>";
	echo "<input type='hidden' name='formid' value='". attr($formid) . "'>";
	echo "<input type='submit' name='bn_save' value='" . xla('Save') . "' />";
?><input type='button' value='<?php echo xla('Stop'); ?>' onclick="top.restoreSession();location='<?php echo $GLOBALS['form_exit_url']; ?>'" /><?php

	echo "</form>";
}//end if($formid)
echo "</div>";
formFooter();
?>
