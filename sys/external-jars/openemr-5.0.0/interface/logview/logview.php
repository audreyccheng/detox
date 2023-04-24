<?php
include_once("../globals.php");
include_once("$srcdir/log.inc");
include_once("$srcdir/formdata.inc.php");
require_once("$srcdir/formatting.inc.php");
?>
<html>
<head>
<?php html_header_show();?>
<link rel="stylesheet" href='<?php echo $GLOBALS['webroot'] ?>/library/dynarch_calendar.css' type='text/css'>
<script type="text/javascript" src="<?php echo $GLOBALS['webroot'] ?>/library/dialog.js?v=<?php echo $v_js_includes; ?>"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['webroot'] ?>/library/dynarch_calendar.js"></script>
<?php include_once("{$GLOBALS['srcdir']}/dynarch_calendar_en.inc.php"); ?>
<script type="text/javascript" src="<?php echo $GLOBALS['webroot'] ?>/library/dynarch_calendar_setup.js"></script>

<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-2-2/index.js"></script>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
<style>
#logview {
    width: 100%;
}
#logview table {
    width:100%;
    border-collapse: collapse;
}
#logview th {
    background-color: #cccccc;
    cursor: pointer; cursor: hand;
    padding: 5px 5px;
    align: left;
    text-align: left;
}

#logview td {
    background-color: #ffffff;
    border-bottom: 1px solid #808080;
    cursor: default;
    padding: 5px 5px;
    vertical-align: top;
}
.highlight {
    background-color: #336699;
    color: #336699;
}
</style>
<script>
//function to disable the event type field if the event name is disclosure
function eventTypeChange(eventname)
{
         if (eventname == "disclosure") {
            document.theform.type_event.disabled = true;
          }
         else {
            document.theform.type_event.disabled = false;
         }              
}

// VicarePlus :: This invokes the find-patient popup.
 function sel_patient() {
  dlgopen('../main/calendar/find_patient_popup.php?pflag=0', '_blank', 500, 400);
 }

// VicarePlus :: This is for callback by the find-patient popup.
 function setpatient(pid, lname, fname, dob) {
  var f = document.theform;
  f.form_patient.value = lname + ', ' + fname;
  f.form_pid.value = pid;
 }

</script>
</head>
<body class="body_top">
<font class="title"><?php  xl('Logs Viewer','e'); ?></font>
<br>
<?php 
$err_message=0;
if ($_GET["start_date"])
$start_date = formData('start_date','G');

if ($_GET["end_date"])
$end_date = formData('end_date','G');

if ($_GET["form_patient"])
$form_patient = formData('form_patient','G');

/*
 * Start date should not be greater than end date - Date Validation
 */
if ($start_date && $end_date)
{
	if($start_date > $end_date){
		echo "<table><tr class='alert'><td colspan=7>"; xl('Start Date should not be greater than End Date',e);
		echo "</td></tr></table>";
		$err_message=1;
	}
}

?>
<?php
$form_user = formData('form_user','R');
$form_pid = formData('form_pid','R');
if ($form_patient == '' ) $form_pid = '';

$res = sqlStatement("select distinct LEFT(date,10) as date from log order by date desc limit 30");
for($iter=0;$row=sqlFetchArray($res);$iter++) {
  $ret[$iter] = $row;
}

// Get the users list.
$sqlQuery = "SELECT username, fname, lname FROM users " .
  "WHERE active = 1 AND ( info IS NULL OR info NOT LIKE '%Inactive%' ) ";

$ures = sqlStatement($sqlQuery);
?>

<?php
$get_sdate=$start_date ? $start_date : date("Y-m-d H:i:s");
$get_edate=$end_date ? $end_date : date("Y-m-d H:i:s");

?>

<br>
<FORM METHOD="GET" name="theform" id="theform">
<?php

$sortby = formData('sortby','G') ;
$direction = formData('direction','G') ;
?>
<input type="hidden" name="direction" id="direction" value="<?php echo !empty($direction) ? $direction : 'asc'; ?>">
<input type="hidden" name="sortby" id="sortby" value="<?php echo $sortby; ?>">
<input type=hidden name=csum value="">
<table>
<tr><td>
<span class="text"><?php  xl('Start Date','e'); ?>: </span>
</td><td>
<input type="text" size="18" name="start_date" id="start_date" value="<?php echo $start_date ? $start_date : (date("Y-m-d") . " 00:00:00"); ?>" title="<?php  xl('yyyy-mm-dd H:m Start Date','e'); ?>" onkeyup="datekeyup(this,mypcc,true)" onblur="dateblur(this,mypcc,true)" />
<img src="../pic/show_calendar.gif" align="absbottom" width="24" height="22" id="img_begin_date" border="0" alt="[?]" style="cursor: pointer; cursor: hand" title="<?php  xl('Click here to choose date time','e'); ?>">&nbsp;
</td>
<td>
<span class="text"><?php  xl('End Date','e'); ?>: </span>
</td><td>
<input type="text" size="18" name="end_date" id="end_date" value="<?php echo $end_date ? $end_date : (date("Y-m-d") . " 23:59:00"); ?>" title="<?php  xl('yyyy-mm-dd H:m End Date','e'); ?>" onkeyup="datekeyup(this,mypcc,true)" onblur="dateblur(this,mypcc,true)" />
<img src="../pic/show_calendar.gif" align="absbottom" width="24" height="22" id="img_end_date" border="0" alt="[?]" style="cursor: pointer; cursor: hand" title="<?php  xl('Click here to choose date time','e'); ?>">&nbsp;
</td>
<!--VicarePlus :: Feature For Generating Log For The Selected Patient --!>
<td>
&nbsp;&nbsp;<span class='text'><?php echo htmlspecialchars(xl('Patient'),ENT_NOQUOTES); ?>: </span>
</td>
<td>
<input type='text' size='20' name='form_patient' style='width:100%;cursor:pointer;cursor:hand' value='<?php echo $form_patient ? $form_patient : htmlspecialchars(xl('Click To Select'),ENT_QUOTES); ?>' onclick='sel_patient()' title='<?php echo htmlspecialchars(xl('Click to select patient'),ENT_QUOTES); ?>' />
<input type='hidden' name='form_pid' value='<?php echo $form_pid; ?>' />
</td>
</tr>
<tr><td>
<span class='text'><?php  xl('User','e'); ?>: </span>
</td>
<td>
<?php
echo "<select name='form_user'>\n";
echo " <option value=''>" . xl('All') . "</option>\n";
while ($urow = sqlFetchArray($ures)) {
  if (!trim($urow['username'])) continue;
  echo " <option value='" . $urow['username'] . "'";
  if ($urow['username'] == $form_user) echo " selected";
  echo ">" . $urow['lname'];
  if ($urow['fname']) echo ", " . $urow['fname'];
  echo "</option>\n";
}
echo "</select>\n";
?>
</td>
<td>
<!-- list of events name -->
<span class='text'><?php  xl('Name of Events','e'); ?>: </span>
</td>
<td>
<?php 
$res = sqlStatement("select distinct event from log order by event ASC");
$ename_list=array(); $j=0;
while ($erow = sqlFetchArray($res)) {
	 if (!trim($erow['event'])) continue;
	 $data = explode('-', $erow['event']);
	 $data_c = count($data);
	 $ename=$data[0];
	 for($i=1;$i<($data_c-1);$i++)
	 {
	 	$ename.="-".$data[$i];
	}
	$ename_list[$j]=$ename;
	$j=$j+1;
}
$res1 = sqlStatement("select distinct event from  extended_log order by event ASC");
// $j=0; // This can't be right!  -- Rod 2013-08-23
while ($row = sqlFetchArray($res1)) {
         if (!trim($row['event'])) continue;
         $new_event = explode('-', $row['event']);
         $no = count($new_event);
         $events=$new_event[0];
         for($i=1;$i<($no-1);$i++)
         {
                $events.="-".$new_event[$i];
        }
        if ($events=="disclosure")
        $ename_list[$j]=$events;
        $j=$j+1;
}
$ename_list=array_unique($ename_list);
$ename_list=array_merge($ename_list);
$ecount=count($ename_list);
echo "<select name='eventname' onchange='eventTypeChange(this.options[this.selectedIndex].value);'>\n";
echo " <option value=''>" . xl('All') . "</option>\n";
for($k=0;$k<$ecount;$k++) {
echo " <option value='" .$ename_list[$k]. "'";
  if ($ename_list[$k] == $eventname && $ename_list[$k]!= "") echo " selected";
  echo ">" . $ename_list[$k];
  echo "</option>\n";
}
echo "</select>\n";
?>
</td>
<!-- type of events ends  -->
<td>
&nbsp;&nbsp;<span class='text'><?php  xl('Type of Events','e'); ?>: </span>
</td><td>
<?php 
$event_types=array("select", "update", "insert", "delete", "replace");
$lcount=count($event_types);
if($eventname=="disclosure"){
 echo "<select name='type_event' disabled='disabled'>\n";
 echo " <option value=''>" . xl('All') . "</option>\n";
 echo "</option>\n";
}
else{
  echo "<select name='type_event'>\n";}
  echo " <option value=''>" . xl('All') . "</option>\n";
  for($k=0;$k<$lcount;$k++) {
  echo " <option value='" .$event_types[$k]. "'";
  if ($event_types[$k] == $type_event && $event_types[$k]!= "") echo " selected";
  echo ">" . preg_replace('/^select$/','Query',$event_types[$k]); // Convert select to Query for MU2 requirement
  echo "</option>\n";
}
echo "</select>\n";
?>
</td>
<tr><td>
<span class='text'><?php xl('Include Checksum','e'); ?>: </span>
</td><td>
<?php

$check_sum = formData('check_sum','G');
?>
<input type="checkbox" name="check_sum" " <?php if ($check_sum == 'on') echo "checked";  ?>"></input>
</td>
<td>
<input type=hidden name="event" value=<?php echo $event ; ?>>
<a href="javascript:document.theform.submit();" class='link_submit'>[<?php  xl('Refresh','e'); ?>]</a>
</td>
<td>
<div id='valid_button'>
<input type=button id='validate_log' onclick='validatelog();' value='<?php echo xla('Validate Log'); ?>'></input>
</div>
<div id='log_loading' style="display: none">
<img src='../../images/loading.gif'/>
</div>
</td>
</tr>
</table>
</FORM>


<?php if ($start_date && $end_date && $err_message!=1) { ?>
<div id="logview">
<table>
 <tr>
  <!-- <TH><?php  xl('Date', 'e'); ?><TD> -->
  <th id="sortby_date" class="text sortby" title="<?php xl('Sort by date/time','e'); ?>"><?php xl('Date','e'); ?></th>
  <th id="sortby_event" class="text sortby" title="<?php xl('Sort by Event','e'); ?>"><?php  xl('Event','e'); ?></th>
  <th id="sortby_category" class="text sortby" title="<?php xl('Sort by Category','e'); ?>"><?php  xl('Category','e'); ?></th>
  <th id="sortby_user" class="text sortby" title="<?php xl('Sort by User','e'); ?>"><?php  xl('User','e'); ?></th>
  <th id="sortby_cuser" class="text sortby" title="<?php xl('Sort by Crt User','e'); ?>"><?php  xl('Certificate User','e'); ?></th>
  <th id="sortby_group" class="text sortby" title="<?php xl('Sort by Group','e'); ?>"><?php  xl('Group','e'); ?></th>
  <th id="sortby_pid" class="text sortby" title="<?php xl('Sort by PatientID','e'); ?>"><?php  xl('PatientID','e'); ?></th>
  <th id="sortby_success" class="text sortby" title="<?php xl('Sort by Success','e'); ?>"><?php  xl('Success','e'); ?></th>
  <th id="sortby_comments" class="text sortby" title="<?php xl('Sort by Comments','e'); ?>"><?php  xl('Comments','e'); ?></th>
 <?php  if($check_sum) {?>
  <th id="sortby_checksum" class="text sortby" title="<?php xl('Sort by Checksum','e'); ?>"><?php  xl('Checksum','e'); ?></th>
  <?php } ?>
 </tr>
<?php

$eventname = formData('eventname','G');
$type_event = formData('type_event','G');
?>
<input type=hidden name=event value=<?php echo $eventname."-".$type_event ?>>
<?php

$tevent=""; $gev="";
if($eventname != "" && $type_event != "")
{
	$getevent=$eventname."-".$type_event;
}
      
	if(($eventname == "") && ($type_event != ""))
    {	$tevent=$type_event;
    }
	else if($type_event =="" && $eventname != "")
    {$gev=$eventname;}
    else if ($eventname == "")
 	{$gev = "";}
 else
    {$gev = $getevent;}
    
if ($ret = getEvents(array('sdate' => $get_sdate,'edate' => $get_edate, 'user' => $form_user, 'patient' => $form_pid, 'sortby' => $_GET['sortby'], 'levent' =>$gev, 'tevent' =>$tevent,'direction' => $_GET['direction']))) {


  foreach ($ret as $iter) {
    //translate comments
    $patterns = array ('/^success/','/^failure/','/ encounter/');
	$replace = array ( xl('success'), xl('failure'), xl('encounter','',' '));
	
	$log_id = $iter['id'];
	$commentEncrStatus = "No";
	$logEncryptData = logCommentEncryptData($log_id);
	if(count($logEncryptData) > 0){
		$commentEncrStatus = $logEncryptData['encrypt'];
	}
	
	//July 1, 2014: Ensoftek: Decrypt comment data if encrypted
	if($commentEncrStatus == "Yes"){
		$trans_comments = preg_replace($patterns, $replace, aes256Decrypt($iter["comments"]));
	}else{
		$trans_comments = preg_replace($patterns, $replace, $iter["comments"]);
	}
	
?>
 <TR class="oneresult">
  <TD class="text"><?php echo oeFormatShortDate(substr($iter["date"], 0, 10)) . substr($iter["date"], 10) ?></TD>
  <TD class="text"><?php echo preg_replace('/select$/','Query',$iter["event"]); //Convert select term to Query for MU2 requirements ?></TD>
  <TD class="text"><?php echo $iter["category"]?></TD>
  <TD class="text"><?php echo $iter["user"]?></TD>
  <TD class="text"><?php echo $iter["crt_user"]?></TD>
  <TD class="text"><?php echo $iter["groupname"]?></TD>
  <TD class="text"><?php echo $iter["patient_id"]?></TD>
  <TD class="text"><?php echo $iter["success"]?></TD>
  <TD class="text"><?php echo nl2br(text(preg_replace('/^select/i','Query',$trans_comments))); //Convert select term to Query for MU2 requirements ?></TD>
  <?php  if($check_sum) { ?>
  <TD class="text"><?php echo $iter["checksum"]?></TD>
  <?php } ?>
 </TR>

<?php

    }
  }
if (($eventname=="disclosure") || ($gev == ""))
{
$eventname="disclosure";
if ($ret = getEvents(array('sdate' => $get_sdate,'edate' => $get_edate, 'user' => $form_user, 'patient' => $form_pid, 'sortby' => $_GET['sortby'], 'event' =>$eventname))) {
foreach ($ret as $iter) {
        $comments=xl('Recipient Name').":".$iter["recipient"].";".xl('Disclosure Info').":".$iter["description"];
?>
<TR class="oneresult">
  <TD class="text"><?php echo htmlspecialchars(oeFormatShortDate(substr($iter["date"], 0, 10)) . substr($iter["date"], 10),ENT_NOQUOTES); ?></TD>
  <TD class="text"><?php echo htmlspecialchars(xl($iter["event"]),ENT_NOQUOTES);?></TD>
  <TD class="text"><?php echo htmlspecialchars(xl($iter["category"]),ENT_NOQUOTES);?></TD>
  <TD class="text"><?php echo htmlspecialchars($iter["user"],ENT_NOQUOTES);?></TD>
  <TD class="text"><?php echo htmlspecialchars($iter["crt_user"],ENT_NOQUOTES);?></TD>
  <TD class="text"><?php echo htmlspecialchars($iter["groupname"],ENT_NOQUOTES);?></TD>
  <TD class="text"><?php echo htmlspecialchars($iter["patient_id"],ENT_NOQUOTES);?></TD>
  <TD class="text"><?php echo htmlspecialchars($iter["success"],ENT_NOQUOTES);?></TD>
  <TD class="text"><?php echo htmlspecialchars($comments,ENT_NOQUOTES);?></TD>
  <?php  if($check_sum) { ?>
  <TD class="text"><?php echo htmlspecialchars($iter["checksum"],ENT_NOQUOTES);?></TD>
  <?php } ?>
 </TR>
<?php
    }
  }
}
?>
</table>
</div>

<?php } ?>

</body>

<script language="javascript">

// jQuery stuff to make the page a little easier to use
$(document).ready(function(){
    // funny thing here... good learning experience
    // the TR has TD children which have their own background and text color
    // toggling the TR color doesn't change the TD color
    // so we need to change all the TR's children (the TD's) just as we did the TR
    // thus we have two calls to toggleClass:
    // 1 - for the parent (the TR)
    // 2 - for each of the children (the TDs)
    $(".oneresult").mouseover(function() { $(this).toggleClass("highlight"); $(this).children().toggleClass("highlight"); });
    $(".oneresult").mouseout(function() { $(this).toggleClass("highlight"); $(this).children().toggleClass("highlight"); });

    // click-able column headers to sort the list
    $('.sortby')
    $("#sortby_date").click(function() { set_sort_direction(); $("#sortby").val("date"); $("#theform").submit(); });
    $("#sortby_event").click(function() { set_sort_direction(); $("#sortby").val("event"); $("#theform").submit(); });
    $("#sortby_category").click(function() { set_sort_direction(); $("#sortby").val("category"); $("#theform").submit(); });
    $("#sortby_user").click(function() { set_sort_direction(); $("#sortby").val("user"); $("#theform").submit(); });
    $("#sortby_cuser").click(function() { set_sort_direction(); $("#sortby").val("user"); $("#theform").submit(); });
    $("#sortby_group").click(function() { set_sort_direction(); $("#sortby").val("groupname"); $("#theform").submit(); });
    $("#sortby_pid").click(function() { set_sort_direction(); $("#sortby").val("patient_id"); $("#theform").submit(); });
    $("#sortby_success").click(function() { set_sort_direction(); $("#sortby").val("success"); $("#theform").submit(); });
    $("#sortby_comments").click(function() { set_sort_direction(); $("#sortby").val("comments"); $("#theform").submit(); });
    $("#sortby_checksum").click(function() { set_sort_direction(); $("#sortby").val("checksum"); $("#theform").submit(); });
});

function set_sort_direction(){
	if($('#direction').val() == 'asc') 
		$('#direction').val('desc'); 
	else 
		$('#direction').val('asc');
}



/* required for popup calendar */
Calendar.setup({inputField:"start_date", ifFormat:"%Y-%m-%d %H:%M:%S", button:"img_begin_date", showsTime:true});
Calendar.setup({inputField:"end_date", ifFormat:"%Y-%m-%d %H:%M:%S", button:"img_end_date", showsTime:true});

function validatelog(){
	 var img = document.getElementById('log_loading');
	 var btn = document.getElementById('valid_button');
	 if(img){
		 if(img.style.display == "block"){
			 return false;
		 }
		 img.style.display = "block";
	 	if(btn){btn.style.display = "none"}
	 }
	 $.ajax({
		 	url:"../../library/log_validation.php",
	        asynchronous : true,
	        method: "post",
	        success :function(response){
	                if(img){
	                        img.style.display="none";
	                        if(btn){btn.style.display="block";}
	                }
	                alert(response);
	                },
	        failure :function(){
	                if(img){
	                        img.style.display="none";
	                        if(btn){btn.style.display="block";}
	                }
	                alert('<?php echo xls("Audit Log Validation Failed"); ?>');
	        }
	 });
		 
}
</script>

</html>
