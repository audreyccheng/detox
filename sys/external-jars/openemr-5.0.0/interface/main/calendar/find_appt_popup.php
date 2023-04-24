<?php
/**
 *
 * Script to find open appointmnent slots
 *
 * Copyright (C) 2005-2013 Rod Roark <rod@sunsetsystems.com>
 *
 * LICENSE: This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation; either version 3 
 * of the License, or (at your option) any later version. 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU General Public License for more details. 
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://opensource.org/licenses/gpl-license.php>;.
 *
 * @package OpenEMR
 * @author Rod Roark <rod@sunsetsystems.com>
 * @author Ian Jardine ( github.com/epsdky )
 * @author Roberto Vasquez <robertogagliotta@gmail.com>
 * @link http://www.open-emr.org
*/

 $fake_register_globals=false;
 $sanitize_all_escapes=true;

 include_once("../../globals.php");
 include_once("$srcdir/patient.inc");
 require_once(dirname(__FILE__)."/../../../library/appointments.inc.php");
 require_once($GLOBALS['incdir']."/main/holidays/Holidays_Controller.php");

 ?>
    <script type="text/javascript" src="<?php echo $webroot ?>/interface/main/tabs/js/include_opener.js"></script>
<?php
 // check access controls
 if (!acl_check('patients','appt','',array('write','wsome') ))
  die(xlt('Access not allowed'));

 // If the caller is updating an existing event, then get its ID so
 // we don't count it as a reserved time slot.
 $eid = empty($_REQUEST['eid']) ? 0 : 0 + $_REQUEST['eid'];

 $input_catid = $_REQUEST['catid'];

 // Record an event into the slots array for a specified day.
 function doOneDay($catid, $udate, $starttime, $duration, $prefcatid) {
  global $slots, $slotsecs, $slotstime, $slotbase, $slotcount, $input_catid;
  $udate = strtotime($starttime, $udate);
  if ($udate < $slotstime) return;
  $i = (int) ($udate / $slotsecs) - $slotbase;
  $iend = (int) (($duration + $slotsecs - 1) / $slotsecs) + $i;
  if ($iend > $slotcount) $iend = $slotcount;
  if ($iend <= $i) $iend = $i + 1;
  for (; $i < $iend; ++$i) {
   if ($catid == 2) {        // in office
    // If a category ID was specified when this popup was invoked, then select
    // only IN events with a matching preferred category or with no preferred
    // category; other IN events are to be treated as OUT events.
    if ($input_catid) {
     if ($prefcatid == $input_catid || !$prefcatid)
      $slots[$i] |= 1;
     else
      $slots[$i] |= 2;
    } else {
     $slots[$i] |= 1;
    }
    break; // ignore any positive duration for IN
   } else if ($catid == 3) { // out of office
    $slots[$i] |= 2;
    break; // ignore any positive duration for OUT
   } else { // all other events reserve time
    $slots[$i] |= 4;
   }
  }
 }

 // seconds per time slot
 $slotsecs = $GLOBALS['calendar_interval'] * 60;
 

 $catslots = 1;
 if ($input_catid) {
  $srow = sqlQuery("SELECT pc_duration FROM openemr_postcalendar_categories WHERE pc_catid = ?", array($input_catid) );
  if ($srow['pc_duration']) $catslots = ceil($srow['pc_duration'] / $slotsecs);
 }

 $info_msg = "";

 $searchdays = 7; // default to a 1-week lookahead
 if ($_REQUEST['searchdays']) $searchdays = $_REQUEST['searchdays'];

 // Get a start date.
 if ($_REQUEST['startdate'] && preg_match("/(\d\d\d\d)\D*(\d\d)\D*(\d\d)/",
     $_REQUEST['startdate'], $matches))
 {
  $sdate = $matches[1] . '-' . $matches[2] . '-' . $matches[3];
 } else {
  $sdate = date("Y-m-d");
 }

 // Get an end date - actually the date after the end date.
 preg_match("/(\d\d\d\d)\D*(\d\d)\D*(\d\d)/", $sdate, $matches);
 $edate = date("Y-m-d",
  mktime(0, 0, 0, $matches[2], $matches[3] + $searchdays, $matches[1]));

 // compute starting time slot number and number of slots.
 $slotstime = strtotime("$sdate 00:00:00");
 $slotetime = strtotime("$edate 00:00:00");
 $slotbase  = (int) ($slotstime / $slotsecs);
 $slotcount = (int) ($slotetime / $slotsecs) - $slotbase;

 if ($slotcount <= 0 || $slotcount > 100000) die(xlt("Invalid date range"));

 $slotsperday = (int) (60 * 60 * 24 / $slotsecs);

 // Compute the number of time slots for the given event duration, or if
 // none is given then assume the default category duration.
 $evslots = $catslots;
 if (isset($_REQUEST['evdur'])) {
  
  // bug fix #445 -- Craig Bezuidenhout 09 Aug 2016
  // if the event duration is less than or equal to zero, use the global calander interval
  // if the global calendar interval is less than or equal to zero, use 10 mins
  if(intval($_REQUEST['evdur']) <= 0){
   if(intval($GLOBALS['calendar_interval']) <= 0){
     $_REQUEST['evdur'] = 10;
   }else{
     $_REQUEST['evdur'] = intval($GLOBALS['calendar_interval']);
   }
  }
  $evslots = 60 * $_REQUEST['evdur'];
  $evslots = (int) (($evslots + $slotsecs - 1) / $slotsecs);
 }

 // If we have a provider, search.
 //
 if ($_REQUEST['providerid']) {
  $providerid = $_REQUEST['providerid'];

  // Create and initialize the slot array. Values are bit-mapped:
  //   bit 0 = in-office occurs here
  //   bit 1 = out-of-office occurs here
  //   bit 2 = reserved
  // So, values may range from 0 to 7.
  //
  $slots = array_pad(array(), $slotcount, 0);

  $sqlBindArray = array();

  // Note there is no need to sort the query results.
  $query = "SELECT pc_eventDate, pc_endDate, pc_startTime, pc_duration, " .
   "pc_recurrtype, pc_recurrspec, pc_alldayevent, pc_catid, pc_prefcatid " .
   "FROM openemr_postcalendar_events " .
   "WHERE pc_aid = ? AND " .
   "pc_eid != ? AND " .
   "((pc_endDate >= ? AND pc_eventDate < ? ) OR " .
   "(pc_endDate = '0000-00-00' AND pc_eventDate >= ? AND pc_eventDate < ?))";

   array_push($sqlBindArray, $providerid, $eid, $sdate, $edate, $sdate, $edate);

  // phyaura whimmel facility filtering
  if ($_REQUEST['facility'] > 0 ) {
    $facility = $_REQUEST['facility'];
    $query .= " AND pc_facility = ?";
    array_push($sqlBindArray, $facility);
  }
  // end facility filtering whimmel 29apr08

  //////
  $events2 = fetchEvents($sdate, $edate, null, null, false, 0, $sqlBindArray, $query);
  foreach($events2 as $row) {
    $thistime = strtotime($row['pc_eventDate'] . " 00:00:00");
    doOneDay($row['pc_catid'], $thistime, $row['pc_startTime'],
             $row['pc_duration'], $row['pc_prefcatid']);
  }
  //////

  // Mark all slots reserved where the provider is not in-office.
  // Actually we could do this in the display loop instead.
  $inoffice = false;
  for ($i = 0; $i < $slotcount; ++$i) {
   if (($i % $slotsperday) == 0) $inoffice = false;
   if ($slots[$i] & 1) $inoffice = true;
   if ($slots[$i] & 2) $inoffice = false;
   if (! $inoffice) { $slots[$i] |= 4; $prov[$i] = $i; }
  }
 }
$ckavail = true;
// If the requested date is a holiday/closed date we need to alert the user about it and let him choose if he wants to proceed
//////
$is_holiday=false;
$holidays_controller = new Holidays_Controller();
$holidays = $holidays_controller->get_holidays_by_date_range($sdate,$edate);
if(in_array($sdate,$holidays)){
    $is_holiday=true;
    $ckavail=true;
}
//////



 // The cktime parameter is a number of minutes into the starting day of a
 // tentative appointment that is to be checked.  If it is present then we are
 // being asked to check if this indicated slot is available, and to submit
 // the opener and go away quietly if it is.  If it's not then we have more
 // work to do.

 if (isset($_REQUEST['cktime'])) {
  $cktime = 0 + $_REQUEST['cktime'];
  $ckindex = (int) ($cktime * 60 / $slotsecs);
  for ($j = $ckindex; $j < $ckindex + $evslots; ++$j) {
	  if ($slots[$j] >= 4) {
		$ckavail = false;
		$isProv = FALSE;
		if(isset($prov[$j])){
			$isProv = 'TRUE';
		}
	  }
  }

  if ($ckavail) {
    // The chosen appointment time is available.
    echo "<html>"
      . "<script language='JavaScript'>\n";
    echo "function mytimeout() {\n";
    echo " opener.top.restoreSession();\n";
    echo " opener.document.forms[0].submit();\n";
    echo " window.close();\n";
    echo "}\n";
    echo "</script></head><body onload='setTimeout(\"mytimeout()\",250);'>" .
      xlt('Time slot is open, saving event') . "...</body></html>";
    exit();
  }
  // The appointment slot is not available.  A message will be displayed
  // after this page is loaded.
 }
?>
<html>
<head>
<?php html_header_show(); ?>
<title><?php echo xlt('Find Available Appointments'); ?></title>
<link rel="stylesheet" href='<?php echo $css_header ?>' type='text/css'>

<!-- for the pop up calendar -->
<style type="text/css">@import url(../../../library/dynarch_calendar.css);</style>
<script type="text/javascript" src="../../../library/dynarch_calendar.js"></script>
<?php include_once("{$GLOBALS['srcdir']}/dynarch_calendar_en.inc.php"); ?>
<script type="text/javascript" src="../../../library/dynarch_calendar_setup.js"></script>

<!-- for ajax-y stuff -->
<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-2-2/index.js"></script>

<script language="JavaScript">

 function setappt(year,mon,mday,hours,minutes) {
  if (opener.closed || ! opener.setappt)
   alert('<?php echo xls('The destination form was closed; I cannot act on your selection.'); ?>');
  else
   opener.setappt(year,mon,mday,hours,minutes);
  window.close();
  return false;
 }

</script>

<style>
form {
    /* this eliminates the padding normally around a FORM tag */
    padding: 0px;
    margin: 0px;
}
#searchCriteria {
    text-align: center;
    width: 100%;
    font-size: 0.8em;
    background-color: #ddddff;
    font-weight: bold;
    padding: 3px;
}
#searchResultsHeader { 
    width: 100%;
    background-color: lightgrey;
}
#searchResultsHeader table { 
    width: 96%;  /* not 100% because the 'searchResults' table has a scrollbar */
    border-collapse: collapse;
}
#searchResultsHeader th {
    font-size: 0.7em;
}
#searchResults {
    width: 100%;
    height: 350px; 
    overflow: auto;
}

.srDate { width: 20%; }
.srTimes { width: 80%; }

#searchResults table {
    width: 100%;
    border-collapse: collapse;
    background-color: white;
}
#searchResults td {
    font-size: 0.7em;
    border-bottom: 1px solid gray;
    padding: 1px 5px 1px 5px;
}
.highlight { background-color: #ff9; }
.blue_highlight { background-color: #336699; color: white; }
#am {
    border-bottom: 1px solid lightgrey;
    color: #00c;
}
#pm { color: #c00; }
#pm a { color: #c00; }
</style>

</head>

<body class="body_top">

<div id="searchCriteria">
<form method='post' name='theform' action='find_appt_popup.php?providerid=<?php echo attr($providerid) ?>&catid=<?php echo attr($input_catid) ?>'>
   <?php echo xlt('Start date:'); ?>
   <input type='text' name='startdate' id='startdate' size='10' value='<?php echo attr($sdate) ?>'
    title='<?php echo xla('yyyy-mm-dd starting date for search'); ?> '/>
   <img src='../../pic/show_calendar.gif' align='absbottom' width='24' height='22'
    id='img_date' border='0' alt='[?]' style='cursor:pointer'
    title='<?php echo xla('Click here to choose a date'); ?>'>
   <?php echo xlt('for'); ?>
   <input type='text' name='searchdays' size='3' value='<?php echo attr($searchdays) ?>'
    title='<?php echo xla('Number of days to search from the start date'); ?>' />
   <?php echo xlt('days'); ?>&nbsp;
   <input type='submit' value='<?php echo xla('Search'); ?>'>
</div>

<?php if (!empty($slots)) : ?>

<div id="searchResultsHeader">
<table>
 <tr>
  <th class="srDate"><?php echo xlt('Day'); ?></th>
  <th class="srTimes"><?php echo xlt('Available Times'); ?></th>
 </tr>
</table>
</div>

<div id="searchResults">
<table> 
<?php
    $lastdate = "";
    $ampmFlag = "am"; // establish an AM-PM line break flag
    for ($i = 0; $i < $slotcount; ++$i) {

        $available = true;
        for ($j = $i; $j < $i + $evslots; ++$j) {
            if ($slots[$j] >= 4) $available = false;
        }
        if (!$available) continue; // skip reserved slots

        $utime = ($slotbase + $i) * $slotsecs;
        $thisdate = date("Y-m-d", $utime);
        if ($thisdate != $lastdate) {
            // if a new day, start a new row
            if ($lastdate) {
                echo "</div>";
                echo "</td>\n";
                echo " </tr>\n";
            }
            $lastdate = $thisdate;
            $dayName = date("l", $utime);
            echo " <tr class='oneresult'>\n";
            echo "  <td class='srDate'>" . xlt($dayName)."<br>".date("Y-m-d", $utime) . "</td>\n";
            echo "  <td class='srTimes'>";
            echo "<div id='am'>AM ";
            $ampmFlag = "am";  // reset the AMPM flag
        }
        
        $ampm = date('a', $utime);
        if ($ampmFlag != $ampm) { echo "</div><div id='pm'>PM "; }
        $ampmFlag = $ampm;

        $atitle = "Choose ".date("h:i a", $utime);
        $adate = getdate($utime);
        $anchor = "<a href='' onclick='return setappt(" .
            $adate['year'] . "," .
            $adate['mon'] . "," .
            $adate['mday'] . "," .
            $adate['hours'] . "," .
            $adate['minutes'] . ")'".
            " title='$atitle' alt='$atitle'".
            ">";
        echo (strlen(date('g',$utime)) < 2 ? "<span style='visibility:hidden'>0</span>" : "") .
            $anchor . date("g:i", $utime) . "</a> ";

        // If the duration is more than 1 slot, increment $i appropriately.
        // This is to avoid reporting available times on undesirable boundaries.
        $i += $evslots - 1;
    }
    if ($lastdate) {
        echo "</td>\n";
        echo " </tr>\n";
    } else {
        echo " <tr><td colspan='2'> " . xlt('No openings were found for this period.') . "</td></tr>\n";
    }
?>
</table>
</div>
</div>
<?php endif; ?>

</form>
</body>

<!-- for the pop up calendar -->
<script language='JavaScript'>
 Calendar.setup({inputField:"startdate", ifFormat:"%Y-%m-%d", button:"img_date"});

// jQuery stuff to make the page a little easier to use

$(document).ready(function(){
    $(".oneresult").mouseover(function() { $(this).toggleClass("highlight"); });
    $(".oneresult").mouseout(function() { $(this).toggleClass("highlight"); });
    $(".oneresult a").mouseover(function () { $(this).toggleClass("blue_highlight"); $(this).children().toggleClass("blue_highlight"); });
    $(".oneresult a").mouseout(function() { $(this).toggleClass("blue_highlight"); $(this).children().toggleClass("blue_highlight"); });
    //$(".event").dblclick(function() { EditEvent(this); });
});


<?php if (!$ckavail) { ?>
<?php if (acl_check('patients','appt','','write')) {
if($is_holiday){?>
    if (confirm('<?php echo xls('On this date there is a holiday, use it anyway?'); ?>')) {
     opener.top.restoreSession();
     opener.document.forms[0].submit();
     window.close();
    }
<?php }else{
if($isProv): ?>
if (confirm('<?php echo xls('Provider not available, use it anyway?'); ?>')) {
<?php else: ?>
if (confirm('<?php echo xls('This appointment slot is already used, use it anyway?'); ?>')) {
<?php endif; ?>
opener.top.restoreSession();
opener.document.forms[0].submit();
window.close();
}
<?php } ?>
<?php } else {
if($is_holiday){?>
    alert('<?php echo xls('On this date there is a holiday, use it anyway?'); ?>');
<?php }else{
if($isProv): ?>
alert('<?php echo xls('Provider not available, please choose another.'); ?>');
<?php else: ?>
alert('<?php echo xls('This appointment slot is already used, please choose another.'); ?>');
<?php endif; ?>
<?php } ?>//close if is holiday
<?php } ?>
<?php } ?>


</script>

</html>
