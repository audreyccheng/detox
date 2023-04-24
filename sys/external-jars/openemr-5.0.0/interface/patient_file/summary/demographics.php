<?php
/**
 *
 * Patient summary screen.
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
 * @author  Brady Miller <brady@sparmy.com>
 * @link    http://www.open-emr.org
 */

//SANITIZE ALL ESCAPES
$sanitize_all_escapes=true;
//

//STOP FAKE REGISTER GLOBALS
$fake_register_globals=false;
//

 require_once("../../globals.php");
 require_once("$srcdir/patient.inc");
 require_once("$srcdir/acl.inc");
 require_once("$srcdir/classes/Address.class.php");
 require_once("$srcdir/classes/InsuranceCompany.class.php");
 require_once("$srcdir/classes/Document.class.php");
 require_once("$srcdir/options.inc.php");
 require_once("../history/history.inc.php");
 require_once("$srcdir/formatting.inc.php");
 require_once("$srcdir/edi.inc");
 require_once("$srcdir/invoice_summary.inc.php");
 require_once("$srcdir/clinical_rules.php");
 require_once("$srcdir/options.js.php");
 ////////////
 require_once(dirname(__FILE__)."/../../../library/appointments.inc.php");
 
  if (isset($_GET['set_pid'])) {
  include_once("$srcdir/pid.inc");
  setpid($_GET['set_pid']);
 }

  $active_reminders = false;
  $all_allergy_alerts = false;
  if ($GLOBALS['enable_cdr']) {
    //CDR Engine stuff
    if ($GLOBALS['enable_allergy_check'] && $GLOBALS['enable_alert_log']) {
      //Check for new allergies conflicts and throw popup if any exist(note need alert logging to support this)
      $new_allergy_alerts = allergy_conflict($pid,'new',$_SESSION['authUser']);
      if (!empty($new_allergy_alerts)) {
        $pop_warning = '<script type="text/javascript">alert(\'' . xls('WARNING - FOLLOWING ACTIVE MEDICATIONS ARE ALLERGIES') . ':\n';
        foreach ($new_allergy_alerts as $new_allergy_alert) {
          $pop_warning .= addslashes($new_allergy_alert) . '\n';
        }
        $pop_warning .= '\')</script>';
        echo $pop_warning;
      }
    }
    if ((!isset($_SESSION['alert_notify_pid']) || ($_SESSION['alert_notify_pid'] != $pid)) && isset($_GET['set_pid']) && $GLOBALS['enable_cdr_crp']) {
      // showing a new patient, so check for active reminders and allergy conflicts, which use in active reminder popup
      $active_reminders = active_alert_summary($pid,"reminders-due",'','default',$_SESSION['authUser'],TRUE);
      if ($GLOBALS['enable_allergy_check']) {
        $all_allergy_alerts = allergy_conflict($pid,'all',$_SESSION['authUser'],TRUE);
      }
    }
  }

function print_as_money($money) {
	preg_match("/(\d*)\.?(\d*)/",$money,$moneymatches);
	$tmp = wordwrap(strrev($moneymatches[1]),3,",",1);
	$ccheck = strrev($tmp);
	if ($ccheck[0] == ",") {
		$tmp = substr($ccheck,1,strlen($ccheck)-1);
	}
	if ($moneymatches[2] != "") {
		return "$ " . strrev($tmp) . "." . $moneymatches[2];
	} else {
		return "$ " . strrev($tmp);
	}
}

// get an array from Photos category
function pic_array($pid,$picture_directory) {
    $pics = array();
    $sql_query = "select documents.id from documents join categories_to_documents " .
                 "on documents.id = categories_to_documents.document_id " .
                 "join categories on categories.id = categories_to_documents.category_id " .
                 "where categories.name like ? and documents.foreign_id = ?";
    if ($query = sqlStatement($sql_query, array($picture_directory,$pid))) {
      while( $results = sqlFetchArray($query) ) {
            array_push($pics,$results['id']);
        }
      }
    return ($pics);
}
// Get the document ID of the first document in a specific catg.
function get_document_by_catg($pid,$doc_catg) {

    $result = array();

	if ($pid and $doc_catg) {
	  $result = sqlQuery("SELECT d.id, d.date, d.url FROM " .
	    "documents AS d, categories_to_documents AS cd, categories AS c " .
	    "WHERE d.foreign_id = ? " .
	    "AND cd.document_id = d.id " .
	    "AND c.id = cd.category_id " .
	    "AND c.name LIKE ? " .
	    "ORDER BY d.date DESC LIMIT 1", array($pid, $doc_catg) );
	    }

	return($result['id']);
}

// Display image in 'widget style'
function image_widget($doc_id,$doc_catg)
{
        global $pid, $web_root;
        $docobj = new Document($doc_id);
        $image_file = $docobj->get_url_file();
        $image_width = $GLOBALS['generate_doc_thumb'] == 1 ? '' : 'width=100';
        $extension = substr($image_file, strrpos($image_file,"."));
        $viewable_types = array('.png','.jpg','.jpeg','.png','.bmp','.PNG','.JPG','.JPEG','.PNG','.BMP'); // image ext supported by fancybox viewer
        if ( in_array($extension,$viewable_types) ) { // extention matches list
                $to_url = "<td> <a href = $web_root" .
				"/controller.php?document&retrieve&patient_id=$pid&document_id=$doc_id&as_file=false&original_file=true&disable_exit=false&show_original=true" .
				"/tmp$extension" .  // Force image type URL for fancybo
				" onclick=top.restoreSession(); class='image_modal'>" .
                " <img src = $web_root" .
				"/controller.php?document&retrieve&patient_id=$pid&document_id=$doc_id&as_file=false" .
				" $image_width alt='$doc_catg:$image_file'>  </a> </td> <td valign='center'>".
                htmlspecialchars($doc_catg) . '<br />&nbsp;' . htmlspecialchars($image_file) .
				"</td>";
        }
     	else {
				$to_url = "<td> <a href='" . $web_root . "/controller.php?document&retrieve" .
                    "&patient_id=$pid&document_id=$doc_id'" .
                    " onclick='top.restoreSession()' class='css_button_small'>" .
                    "<span>" .
                    htmlspecialchars( xl("View"), ENT_QUOTES )."</a> &nbsp;" .
					htmlspecialchars( "$doc_catg - $image_file", ENT_QUOTES ) .
                    "</span> </td>";
		}
        echo "<table><tr>";
        echo $to_url;
        echo "</tr></table>";
}

// Determine if the Vitals form is in use for this site.
$tmp = sqlQuery("SELECT count(*) AS count FROM registry WHERE " .
  "directory = 'vitals' AND state = 1");
$vitals_is_registered = $tmp['count'];

// Get patient/employer/insurance information.
//
$result  = getPatientData($pid, "*, DATE_FORMAT(DOB,'%Y-%m-%d') as DOB_YMD");
$result2 = getEmployerData($pid);
$result3 = getInsuranceData($pid, "primary", "copay, provider, DATE_FORMAT(`date`,'%Y-%m-%d') as effdate");
$insco_name = "";
if ($result3['provider']) {   // Use provider in case there is an ins record w/ unassigned insco
  $insco_name = getInsuranceProvider($result3['provider']);
}
?>
<html>

<head>
<?php html_header_show();?>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
<link rel="stylesheet" type="text/css" href="../../../library/js/fancybox/jquery.fancybox-1.2.6.css" media="screen" />
<style type="text/css">@import url(../../../library/dynarch_calendar.css);</style>
<script type="text/javascript" src="../../../library/textformat.js"></script>
<script type="text/javascript" src="../../../library/dynarch_calendar.js"></script>
<?php include_once("{$GLOBALS['srcdir']}/dynarch_calendar_en.inc.php"); ?>
<script type="text/javascript" src="../../../library/dynarch_calendar_setup.js"></script>
<script type="text/javascript" src="../../../library/dialog.js?v=<?php echo $v_js_includes; ?>"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-6-4/index.js"></script>
<script type="text/javascript" src="../../../library/js/common.js"></script>
<script type="text/javascript" src="../../../library/js/fancybox/jquery.fancybox-1.2.6.js"></script>
<script type="text/javascript" language="JavaScript">

 var mypcc = '<?php echo htmlspecialchars($GLOBALS['phone_country_code'],ENT_QUOTES); ?>';
 //////////
 function oldEvt(apptdate, eventid) {
  dlgopen('../../main/calendar/add_edit_event.php?date=' + apptdate + '&eid=' + eventid, '_blank', 775, 500);
 }

 function advdirconfigure() {
   dlgopen('advancedirectives.php', '_blank', 500, 450);
  }

 function refreshme() {
  top.restoreSession();
  location.reload();
 }

 // Process click on Delete link.
 function deleteme() {
  dlgopen('../deleter.php?patient=<?php echo htmlspecialchars($pid,ENT_QUOTES); ?>', '_blank', 500, 450);
  return false;
 }

 // Called by the deleteme.php window on a successful delete.
 function imdeleted() {
  parent.left_nav.clearPatient();
 }

 function newEvt() {
  dlgopen('../../main/calendar/add_edit_event.php?patientid=<?php echo htmlspecialchars($pid,ENT_QUOTES); ?>', '_blank', 775, 500);
  return false;
 }

function sendimage(pid, what) {
 // alert('Not yet implemented.'); return false;
 dlgopen('../upload_dialog.php?patientid=' + pid + '&file=' + what,
  '_blank', 500, 400);
 return false;
}

</script>

<script type="text/javascript">

function toggleIndicator(target,div) {

    $mode = $(target).find(".indicator").text();
    if ( $mode == "<?php echo htmlspecialchars(xl('collapse'),ENT_QUOTES); ?>" ) {
        $(target).find(".indicator").text( "<?php echo htmlspecialchars(xl('expand'),ENT_QUOTES); ?>" );
        $("#"+div).hide();
	$.post( "../../../library/ajax/user_settings.php", { target: div, mode: 0 });
    } else {
        $(target).find(".indicator").text( "<?php echo htmlspecialchars(xl('collapse'),ENT_QUOTES); ?>" );
        $("#"+div).show();
	$.post( "../../../library/ajax/user_settings.php", { target: div, mode: 1 });
    }
}

$(document).ready(function(){
  var msg_updation='';
	<?php
	if($GLOBALS['erx_enable']){
		//$soap_status=sqlQuery("select soap_import_status from patient_data where pid=?",array($pid));
		$soap_status=sqlStatement("select soap_import_status,pid from patient_data where pid=? and soap_import_status in ('1','3')",array($pid));
		while($row_soapstatus=sqlFetchArray($soap_status)){
			//if($soap_status['soap_import_status']=='1' || $soap_status['soap_import_status']=='3'){ ?>
			top.restoreSession();
			$.ajax({
				type: "POST",
				url: "../../soap_functions/soap_patientfullmedication.php",
				dataType: "html",
				data: {
					patient:<?php echo $row_soapstatus['pid']; ?>,
				},
				async: false,
				success: function(thedata){
					//alert(thedata);
					msg_updation+=thedata;
				},
				error:function(){
					alert('ajax error');
				}	
			});
			<?php
			//}	
			//elseif($soap_status['soap_import_status']=='3'){ ?>
			top.restoreSession();
			$.ajax({
				type: "POST",
				url: "../../soap_functions/soap_allergy.php",
				dataType: "html",
				data: {
					patient:<?php echo $row_soapstatus['pid']; ?>,
				},
				async: false,
				success: function(thedata){
					//alert(thedata);
					msg_updation+=thedata;
				},
				error:function(){
					alert('ajax error');
				}	
			});
			<?php
			if($GLOBALS['erx_import_status_message']){ ?>
			if(msg_updation)
			  alert(msg_updation);
			<?php
			}
			//} 
		}
	}
	?>
    // load divs
    $("#stats_div").load("stats.php", { 'embeddedScreen' : true }, function() {
	// (note need to place javascript code here also to get the dynamic link to work)
        $(".rx_modal").fancybox( {
                'overlayOpacity' : 0.0,
                'showCloseButton' : true,
                'frameHeight' : 500,
                'frameWidth' : 800,
        	'centerOnScroll' : false,
        	'callbackOnClose' : function()  {
                refreshme();
        	}
        });
    });
    $("#pnotes_ps_expand").load("pnotes_fragment.php");
    $("#disclosures_ps_expand").load("disc_fragment.php");

    <?php if ($GLOBALS['enable_cdr'] && $GLOBALS['enable_cdr_crw']) { ?>
      top.restoreSession();
      $("#clinical_reminders_ps_expand").load("clinical_reminders_fragment.php", { 'embeddedScreen' : true }, function() {
          // (note need to place javascript code here also to get the dynamic link to work)
          $(".medium_modal").fancybox( {
                  'overlayOpacity' : 0.0,
                  'showCloseButton' : true,
                  'frameHeight' : 500,
                  'frameWidth' : 800,
                  'centerOnScroll' : false,
                  'callbackOnClose' : function()  {
                  refreshme();
                  }
          });
      });
    <?php } // end crw?>

    <?php if ($GLOBALS['enable_cdr'] && $GLOBALS['enable_cdr_prw']) { ?>
      top.restoreSession();
      $("#patient_reminders_ps_expand").load("patient_reminders_fragment.php");
    <?php } // end prw?>

<?php if ($vitals_is_registered && acl_check('patients', 'med')) { ?>
    // Initialize the Vitals form if it is registered and user is authorized.
    $("#vitals_ps_expand").load("vitals_fragment.php");
<?php } ?>

    // Initialize track_anything
    $("#track_anything_ps_expand").load("track_anything_fragment.php");
    
    
    // Initialize labdata
    $("#labdata_ps_expand").load("labdata_fragment.php");
<?php
  // Initialize for each applicable LBF form.
  $gfres = sqlStatement("SELECT option_id FROM list_options WHERE " .
    "list_id = 'lbfnames' AND option_value > 0 AND activity = 1 ORDER BY seq, title");
  while($gfrow = sqlFetchArray($gfres)) {
?>
    $("#<?php echo $gfrow['option_id']; ?>_ps_expand").load("lbf_fragment.php?formname=<?php echo $gfrow['option_id']; ?>");
<?php
  }
?>

    // fancy box
    enable_modals();

    tabbify();

// modal for dialog boxes
  $(".large_modal").fancybox( {
    'overlayOpacity' : 0.0,
    'showCloseButton' : true,
    'frameHeight' : 600,
    'frameWidth' : 1000,
    'centerOnScroll' : false
  });

// modal for image viewer
  $(".image_modal").fancybox( {
    'overlayOpacity' : 0.0,
    'showCloseButton' : true,
    'centerOnScroll' : false,
    'autoscale' : true
  });
  
  $(".iframe1").fancybox( {
  'left':10,
	'overlayOpacity' : 0.0,
	'showCloseButton' : true,
	'frameHeight' : 300,
	'frameWidth' : 350
  });
// special size for patient portal
  $(".small_modal").fancybox( {
	'overlayOpacity' : 0.0,
	'showCloseButton' : true,
	'frameHeight' : 200,
	'frameWidth' : 380,
            'centerOnScroll' : false
  });

  <?php if ($active_reminders || $all_allergy_alerts) { ?>
    // show the active reminder modal
    $("#reminder_popup_link").fancybox({
      'overlayOpacity' : 0.0,
      'showCloseButton' : true,
      'frameHeight' : 500,
      'frameWidth' : 500,
      'centerOnScroll' : false
    }).trigger('click');
  <?php } ?>

});

// JavaScript stuff to do when a new patient is set.
//
function setMyPatient() {
 // Avoid race conditions with loading of the left_nav or Title frame.
 if (!parent.allFramesLoaded()) {
  setTimeout("setMyPatient()", 500);
  return;
 }
<?php if (isset($_GET['set_pid'])) { ?>
 parent.left_nav.setPatient(<?php echo "'" . addslashes($result['fname']) . " " . addslashes($result['lname']) .
   "'," . addslashes($pid) . ",'" . addslashes($result['pubpid']) .
   "','', ' " . xls('DOB') . ": " . addslashes(oeFormatShortDate($result['DOB_YMD'])) . " " . xls('Age') . ": " . addslashes(getPatientAgeDisplay($result['DOB_YMD'])) . "'"; ?>);
 var EncounterDateArray = new Array;
 var CalendarCategoryArray = new Array;
 var EncounterIdArray = new Array;
 var Count = 0;
<?php
  //Encounter details are stored to javacript as array.
  $result4 = sqlStatement("SELECT fe.encounter,fe.date,openemr_postcalendar_categories.pc_catname FROM form_encounter AS fe ".
    " left join openemr_postcalendar_categories on fe.pc_catid=openemr_postcalendar_categories.pc_catid  WHERE fe.pid = ? order by fe.date desc", array($pid));
  if(sqlNumRows($result4)>0) {
    while($rowresult4 = sqlFetchArray($result4)) {
?>
 EncounterIdArray[Count] = '<?php echo addslashes($rowresult4['encounter']); ?>';
 EncounterDateArray[Count] = '<?php echo addslashes(oeFormatShortDate(date("Y-m-d", strtotime($rowresult4['date'])))); ?>';
 CalendarCategoryArray[Count] = '<?php echo addslashes(xl_appt_category($rowresult4['pc_catname'])); ?>';
 Count++;
<?php
    }
  }
?>
 parent.left_nav.setPatientEncounter(EncounterIdArray,EncounterDateArray,CalendarCategoryArray);
<?php } // end setting new pid ?>
 parent.left_nav.syncRadios();
<?php if ( (isset($_GET['set_pid']) ) && (isset($_GET['set_encounterid'])) && ( intval($_GET['set_encounterid']) > 0 ) ) {
 $encounter = intval($_GET['set_encounterid']);
 $_SESSION['encounter'] = $encounter;
 $query_result = sqlQuery("SELECT `date` FROM `form_encounter` WHERE `encounter` = ?", array($encounter)); ?>
 encurl = 'encounter/encounter_top.php?set_encounter=' + <?php echo attr($encounter);?> + '&pid=' + <?php echo attr($pid);?>;
 <?php if ($GLOBALS['new_tabs_layout']) { ?>
  parent.left_nav.setEncounter('<?php echo oeFormatShortDate(date("Y-m-d", strtotime($query_result['date']))); ?>', '<?php echo attr($encounter); ?>', 'enc');
  top.restoreSession();
  parent.left_nav.loadFrame('enc2', 'enc', 'patient_file/' + encurl);
 <?php } else  { ?>
  var othername = (window.name == 'RTop') ? 'RBot' : 'RTop';
  parent.left_nav.setEncounter('<?php echo oeFormatShortDate(date("Y-m-d", strtotime($query_result['date']))); ?>', '<?php echo attr($encounter); ?>', othername);
  top.restoreSession();
  parent.frames[othername].location.href = '../' + encurl;
 <?php } ?>
<?php } // end setting new encounter id (only if new pid is also set) ?>
}

$(window).load(function() {
 setMyPatient();
});

</script>

<style type="css/text">
#pnotes_ps_expand {
  height:auto;
  width:100%;
}
</style>

</head>

<body class="body_top patient-demographics">

<a href='../reminder/active_reminder_popup.php' id='reminder_popup_link' style='visibility: false;' class='iframe' onclick='top.restoreSession()'></a>

<?php
$thisauth = acl_check('patients', 'demo');
if ($thisauth) {
    if ($result['squad'] && ! acl_check('squads', $result['squad'])) {
        $thisauth = 0;
    }
}
if (!$thisauth) {
    echo "<p>(" . htmlspecialchars(xl('Demographics not authorized'),ENT_NOQUOTES) . ")</p>\n";
    echo "</body>\n</html>\n";
    exit();
}
if ($thisauth): ?>

<table class="table_header">
    <tr>
        <td>
            <span class='title'>
                <?php echo htmlspecialchars(getPatientName($pid),ENT_NOQUOTES); ?>
            </span>
        </td>
        <?php if (acl_check('admin', 'super') && $GLOBALS['allow_pat_delete']) : ?>
        <td style='padding-left:1em;' class="delete">
            <a class='css_button iframe' 
               href='../deleter.php?patient="<?php echo htmlspecialchars($pid,ENT_QUOTES);?>' 
               onclick='top.restoreSession()'>
                <span><?php echo htmlspecialchars(xl('Delete'),ENT_NOQUOTES);?></span>
            </a>
        </td>
        <?php endif; // Allow PT delete
        if($GLOBALS['erx_enable']): ?>
        <td style="padding-left:1em;" class="erx">
            <a class="css_button" href="../../eRx.php?page=medentry" onclick="top.restoreSession()">
                <span><?php echo htmlspecialchars(xl('NewCrop MedEntry'),ENT_NOQUOTES);?></span>
            </a>
        </td>
        <td style="padding-left:1em;">
            <a class="css_button iframe1" 
               href="../../soap_functions/soap_accountStatusDetails.php" 
               onclick="top.restoreSession()">
                <span><?php echo htmlspecialchars(xl('NewCrop Account Status'),ENT_NOQUOTES);?></span>
            </a>
        </td>
        <td id='accountstatus'></td>
        <?php endif; // eRX Enabled
        //Patient Portal
        $portalUserSetting = true; //flag to see if patient has authorized access to portal
        if($GLOBALS['portal_onsite_enable'] && $GLOBALS['portal_onsite_address']):
            $portalStatus = sqlQuery("SELECT allow_patient_portal FROM patient_data WHERE pid=?",array($pid));
            if ($portalStatus['allow_patient_portal']=='YES'):
                $portalLogin = sqlQuery("SELECT pid FROM `patient_access_onsite` WHERE `pid`=?", array($pid));?>
                <td style='padding-left:1em;'>
                    <a class='css_button iframe small_modal' 
                       href='create_portallogin.php?portalsite=on&patient=<?php echo htmlspecialchars($pid,ENT_QUOTES);?>' 
                       onclick='top.restoreSession()'>
                        <?php $display = (empty($portalLogin)) ? xlt('Create Onsite Portal Credentials') : xlt('Reset Onsite Portal Credentials'); ?>
                        <span><?php echo $display; ?></span>
                    </a>
                </td>
            <?php
            else:
                $portalUserSetting = false;
            endif; // allow patient portal
        endif; // Onsite Patient Portal
        if($GLOBALS['portal_offsite_enable'] && $GLOBALS['portal_offsite_address']):
            $portalStatus = sqlQuery("SELECT allow_patient_portal FROM patient_data WHERE pid=?",array($pid));
            if ($portalStatus['allow_patient_portal']=='YES'):
                $portalLogin = sqlQuery("SELECT pid FROM `patient_access_offsite` WHERE `pid`=?", array($pid));
                ?>
                <td style='padding-left:1em;'>
                    <a class='css_button iframe small_modal' 
                       href='create_portallogin.php?portalsite=off&patient=<?php echo htmlspecialchars($pid,ENT_QUOTES);?>' 
                       onclick='top.restoreSession()'>
                        <span>
                            <?php $text = (empty($portalLogin)) ? xlt('Create Offsite Portal Credentials') : xlt('Reset Offsite Portal Credentials'); ?>
                            <?php echo $text; ?>
                        </span>
                    </a>
                </td>
            <?php 
            else:
                $portalUserSetting = false;
            endif; // allow_patient_portal
        endif; // portal_offsite_enable
        if (!($portalUserSetting)): // Show that the patient has not authorized portal access ?>
            <td style='padding-left:1em;'>
                <?php echo htmlspecialchars( xl('Patient has not authorized the Patient Portal.'), ENT_NOQUOTES);?>
            </td>
        <?php endif;
        //Patient Portal

        // If patient is deceased, then show this (along with the number of days patient has been deceased for)
        $days_deceased = is_patient_deceased($pid);
        if ($days_deceased != null): ?>
            <td class="deceased" style="padding-left:1em;font-weight:bold;color:red">
                <?php
                if ($days_deceased == 0) {
                    echo xlt("DECEASED (Today)");
                }
                else if ($days_deceased == 1) {
                    echo xlt("DECEASED (1 day ago)");
                }
                else {
                    echo xlt("DECEASED") . " (" . text($days_deceased) . " " . xlt("days ago") . ")";
                } ?>
            </td>
        <?php endif; ?>
    </tr>
</table>

<?php
endif; // $thisauth
?>

<?php
// Get the document ID of the patient ID card if access to it is wanted here.
$idcard_doc_id = false;
if ($GLOBALS['patient_id_category_name']) {
  $idcard_doc_id = get_document_by_catg($pid, $GLOBALS['patient_id_category_name']);
}

?>
<table cellspacing='0' cellpadding='0' border='0' class="subnav">
  <tr>
      <td class="small" colspan='4'>
          <a href="../history/history.php" onclick='top.restoreSession()'>
          <?php echo htmlspecialchars(xl('History'),ENT_NOQUOTES); ?></a>
          |
          <?php //note that we have temporarily removed report screen from the modal view ?>
          <a href="../report/patient_report.php" onclick='top.restoreSession()'>
          <?php echo htmlspecialchars(xl('Report'),ENT_NOQUOTES); ?></a>
          |
          <?php //note that we have temporarily removed document screen from the modal view ?>
          <a href="../../../controller.php?document&list&patient_id=<?php echo $pid;?>" onclick='top.restoreSession()'>
          <?php echo htmlspecialchars(xl('Documents'),ENT_NOQUOTES); ?></a>
          |
          <a href="../transaction/transactions.php" class='iframe large_modal' onclick='top.restoreSession()'>
          <?php echo htmlspecialchars(xl('Transactions'),ENT_NOQUOTES); ?></a>
          |
          <a href="stats_full.php?active=all" onclick='top.restoreSession()'>
          <?php echo htmlspecialchars(xl('Issues'),ENT_NOQUOTES); ?></a>
          |
          <a href="../../reports/pat_ledger.php?form=1&patient_id=<?php echo attr($pid);?>" onclick='top.restoreSession()'>
          <?php echo xlt('Ledger'); ?></a>
          |
          <a href="../../reports/external_data.php" onclick='top.restoreSession()'>
          <?php echo xlt('External Data'); ?></a>

<!-- DISPLAYING HOOKS STARTS HERE -->
<?php
	$module_query = sqlStatement("SELECT msh.*,ms.menu_name,ms.path,m.mod_ui_name,m.type FROM modules_hooks_settings AS msh
					LEFT OUTER JOIN modules_settings AS ms ON obj_name=enabled_hooks AND ms.mod_id=msh.mod_id
					LEFT OUTER JOIN modules AS m ON m.mod_id=ms.mod_id 
					WHERE fld_type=3 AND mod_active=1 AND sql_run=1 AND attached_to='demographics' ORDER BY mod_id");
	$DivId = 'mod_installer';
	if (sqlNumRows($module_query)) {
		$jid 	= 0;
		$modid 	= '';
		while ($modulerow = sqlFetchArray($module_query)) {
			$DivId 		= 'mod_'.$modulerow['mod_id'];
			$new_category 	= $modulerow['mod_ui_name'];
			$modulePath 	= "";
			$added      	= "";
			if($modulerow['type'] == 0) {
				$modulePath 	= $GLOBALS['customModDir'];
				$added		= "";
			}
			else{
				$added		= "index";
				$modulePath 	= $GLOBALS['zendModDir'];
			}
			$relative_link 	= "../../modules/".$modulePath."/".$modulerow['path'];
			$nickname 	= $modulerow['menu_name'] ? $modulerow['menu_name'] : 'Noname';
			$jid++;
			$modid = $modulerow['mod_id'];
			?>
			|
			<a href="<?php echo $relative_link; ?>" onclick='top.restoreSession()'>
			<?php echo xlt($nickname); ?></a>
		<?php	
		}
	}
	?>
<!-- DISPLAYING HOOKS ENDS HERE -->

        </td>
    </tr>
</table> <!-- end header -->

<div style='margin-top:10px' class="main"> <!-- start main content div -->
    <table border="0" cellspacing="0" cellpadding="0" width="100%">
        <tr>
            <td class="demographics-box" align="left" valign="top">
                <!-- start left column div -->
                <div style='float:left; margin-right:20px'>

                    <table cellspacing=0 cellpadding=0>
                    <?php if (!$GLOBALS['hide_billing_widget'])  { ?>
                        <tr>
                            <td>
                                <?php
                                // Billing expand collapse widget
                                $widgetTitle = xl("Billing");
                                $widgetLabel = "billing";
                                $widgetButtonLabel = xl("Edit");
                                $widgetButtonLink = "return newEvt();";
                                $widgetButtonClass = "";
                                $linkMethod = "javascript";
                                $bodyClass = "notab";
                                $widgetAuth = false;
                                $fixedWidth = true;
                                if ($GLOBALS['force_billing_widget_open']) {
                                  $forceExpandAlways = true;
                                }
                                else {
                                  $forceExpandAlways = false;
                                }
                                expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel,
                                  $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass,
                                  $widgetAuth, $fixedWidth, $forceExpandAlways);
                                ?>
        <br>
<?php
		//PATIENT BALANCE,INS BALANCE naina@capminds.com
		$patientbalance = get_patient_balance($pid, false);
		//Debit the patient balance from insurance balance
		$insurancebalance = get_patient_balance($pid, true) - $patientbalance;
	   $totalbalance=$patientbalance + $insurancebalance;

 // Show current balance and billing note, if any.
  echo "<table border='0'><tr><td>" .
  "<table ><tr><td><span class='bold'><font color='red'>" .
   xlt('Patient Balance Due') .
   " : " . text(oeFormatMoney($patientbalance)) .
   "</font></span></td></tr>".
     "<tr><td><span class='bold'><font color='red'>" .
   xlt('Insurance Balance Due') .
   " : " . text(oeFormatMoney($insurancebalance)) .
   "</font></span></td></tr>".
   "<tr><td><span class='bold'><font color='red'>" .
   xlt('Total Balance Due').
   " : " . text(oeFormatMoney($totalbalance)) .
   "</font></span></td></td></tr>";
 if (!empty($result['billing_note'])) {
   echo "<tr><td><span class='bold'><font color='red'>" .
    xlt('Billing Note') . ":" .
    text($result['billing_note']) .
    "</font></span></td></tr>";
  }
  if ($result3['provider']) {   // Use provider in case there is an ins record w/ unassigned insco
   echo "<tr><td><span class='bold'>" .
    xlt('Primary Insurance') . ': ' . text($insco_name) .
    "</span>&nbsp;&nbsp;&nbsp;";
   if ($result3['copay'] > 0) {
    echo "<span class='bold'>" .
    xlt('Copay') . ': ' .  text($result3['copay']) .
     "</span>&nbsp;&nbsp;&nbsp;";
   }
   echo "<span class='bold'>" .
    xlt('Effective Date') . ': ' .  text(oeFormatShortDate($result3['effdate'])) .
    "</span></td></tr>";
  }
  echo "</table></td></tr></td></tr></table><br>";

?>
        </div> <!-- required for expand_collapse_widget -->
       </td>
      </tr>
      <?php } ?>
      <tr>
       <td>
<?php
// Demographics expand collapse widget
$widgetTitle = xl("Demographics");
$widgetLabel = "demographics";
$widgetButtonLabel = xl("Edit");
$widgetButtonLink = "demographics_full.php";
$widgetButtonClass = "";
$linkMethod = "html";
$bodyClass = "";
$widgetAuth = acl_check('patients', 'demo', '', 'write');
$fixedWidth = true;
expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel,
  $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass,
  $widgetAuth, $fixedWidth);
?>
         <div id="DEM" >
          <ul class="tabNav">
           <?php display_layout_tabs('DEM', $result, $result2); ?>
          </ul>
          <div class="tabContainer">
           <?php display_layout_tabs_data('DEM', $result, $result2); ?>
          </div>
         </div>
        </div> <!-- required for expand_collapse_widget -->
       </td>
      </tr>

      <tr>
       <td>
<?php
$insurance_count = 0;
foreach (array('primary','secondary','tertiary') as $instype) {
  $enddate = 'Present';
  $query = "SELECT * FROM insurance_data WHERE " .
    "pid = ? AND type = ? " .
    "ORDER BY date DESC";
  $res = sqlStatement($query, array($pid, $instype) );
  while( $row = sqlFetchArray($res) ) {
    if ($row['provider'] ) $insurance_count++;
  }
}

if ( $insurance_count > 0 ) {
  // Insurance expand collapse widget
  $widgetTitle = xl("Insurance");
  $widgetLabel = "insurance";
  $widgetButtonLabel = xl("Edit");
  $widgetButtonLink = "demographics_full.php";
  $widgetButtonClass = "";
  $linkMethod = "html";
  $bodyClass = "";
  $widgetAuth = acl_check('patients', 'demo', '', 'write');
  $fixedWidth = true;
  expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel,
    $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass,
    $widgetAuth, $fixedWidth);

  if ( $insurance_count > 0 ) {
?>

        <ul class="tabNav"><?php
					///////////////////////////////// INSURANCE SECTION
					$first = true;
					foreach (array('primary','secondary','tertiary') as $instype) {

						$query = "SELECT * FROM insurance_data WHERE " .
						"pid = ? AND type = ? " .
						"ORDER BY date DESC";
						$res = sqlStatement($query, array($pid, $instype) );

						$enddate = 'Present';

						  while( $row = sqlFetchArray($res) ) {
							if ($row['provider'] ) {

								$ins_description  = ucfirst($instype);
	                                                        $ins_description = xl($ins_description);
								$ins_description  .= strcmp($enddate, 'Present') != 0 ? " (".xl('Old').")" : "";
								?>
								<li <?php echo $first ? 'class="current"' : '' ?>><a href="#">
								<?php echo htmlspecialchars($ins_description,ENT_NOQUOTES); ?></a></li>
								<?php
								$first = false;
							}
							$enddate = $row['date'];
						}
					}
					// Display the eligibility tab
					echo "<li><a href='#'>" .
						htmlspecialchars( xl('Eligibility'), ENT_NOQUOTES) . "</a></li>";

					?></ul><?php

				} ?>

				<div class="tabContainer">
					<?php
					$first = true;
					foreach (array('primary','secondary','tertiary') as $instype) {
					  $enddate = 'Present';

						$query = "SELECT * FROM insurance_data WHERE " .
						"pid = ? AND type = ? " .
						"ORDER BY date DESC";
						$res = sqlStatement($query, array($pid, $instype) );
					  while( $row = sqlFetchArray($res) ) {
						if ($row['provider'] ) {
							?>
								<div class="tab <?php echo $first ? 'current' : '' ?>">
								<table border='0' cellpadding='0' width='100%'>
								<?php
								$icobj = new InsuranceCompany($row['provider']);
								$adobj = $icobj->get_address();
								$insco_name = trim($icobj->get_name());
								?>
								<tr>
								 <td valign='top' colspan='3'>
								  <span class='text'>
								  <?php if (strcmp($enddate, 'Present') != 0) echo htmlspecialchars(xl("Old"),ENT_NOQUOTES)." "; ?>
								  <?php $tempinstype=ucfirst($instype); echo htmlspecialchars(xl($tempinstype.' Insurance'),ENT_NOQUOTES); ?>
								  <?php if (strcmp($row['date'], '0000-00-00') != 0) { ?>
								  <?php echo htmlspecialchars(xl('from','',' ',' ').$row['date'],ENT_NOQUOTES); ?>
								  <?php } ?>
						                  <?php echo htmlspecialchars(xl('until','',' ',' '),ENT_NOQUOTES);
								    echo (strcmp($enddate, 'Present') != 0) ? $enddate : htmlspecialchars(xl('Present'),ENT_NOQUOTES); ?>:</span>
								 </td>
								</tr>
								<tr>
								 <td valign='top'>
								  <span class='text'>
								  <?php
								  if ($insco_name) {
									echo htmlspecialchars($insco_name,ENT_NOQUOTES) . '<br>';
									if (trim($adobj->get_line1())) {
									  echo htmlspecialchars($adobj->get_line1(),ENT_NOQUOTES) . '<br>';
									  echo htmlspecialchars($adobj->get_city() . ', ' . $adobj->get_state() . ' ' . $adobj->get_zip(),ENT_NOQUOTES);
									}
								  } else {
									echo "<font color='red'><b>".htmlspecialchars(xl('Unassigned'),ENT_NOQUOTES)."</b></font>";
								  }
								  ?>
								  <br>
								  <?php echo htmlspecialchars(xl('Policy Number'),ENT_NOQUOTES); ?>: 
								  <?php echo htmlspecialchars($row['policy_number'],ENT_NOQUOTES) ?><br>
								  <?php echo htmlspecialchars(xl('Plan Name'),ENT_NOQUOTES); ?>: 
								  <?php echo htmlspecialchars($row['plan_name'],ENT_NOQUOTES); ?><br>
								  <?php echo htmlspecialchars(xl('Group Number'),ENT_NOQUOTES); ?>: 
								  <?php echo htmlspecialchars($row['group_number'],ENT_NOQUOTES); ?></span>
								 </td>
								 <td valign='top'>
								  <span class='bold'><?php echo htmlspecialchars(xl('Subscriber'),ENT_NOQUOTES); ?>: </span><br>
								  <span class='text'><?php echo htmlspecialchars($row['subscriber_fname'] . ' ' . $row['subscriber_mname'] . ' ' . $row['subscriber_lname'],ENT_NOQUOTES); ?>
							<?php
								  if ($row['subscriber_relationship'] != "") {
									echo "(" . htmlspecialchars($row['subscriber_relationship'],ENT_NOQUOTES) . ")";
								  }
							?>
								  <br>
								  <?php echo htmlspecialchars(xl('S.S.'),ENT_NOQUOTES); ?>: 
								  <?php echo htmlspecialchars($row['subscriber_ss'],ENT_NOQUOTES); ?><br>
								  <?php echo htmlspecialchars(xl('D.O.B.'),ENT_NOQUOTES); ?>:
								  <?php if ($row['subscriber_DOB'] != "0000-00-00 00:00:00") echo htmlspecialchars($row['subscriber_DOB'],ENT_NOQUOTES); ?><br>
								  <?php echo htmlspecialchars(xl('Phone'),ENT_NOQUOTES); ?>: 
								  <?php echo htmlspecialchars($row['subscriber_phone'],ENT_NOQUOTES); ?>
								  </span>
								 </td>
								 <td valign='top'>
								  <span class='bold'><?php echo htmlspecialchars(xl('Subscriber Address'),ENT_NOQUOTES); ?>: </span><br>
								  <span class='text'><?php echo htmlspecialchars($row['subscriber_street'],ENT_NOQUOTES); ?><br>
								  <?php echo htmlspecialchars($row['subscriber_city'],ENT_NOQUOTES); ?>
								  <?php if($row['subscriber_state'] != "") echo ", "; echo htmlspecialchars($row['subscriber_state'],ENT_NOQUOTES); ?>
								  <?php if($row['subscriber_country'] != "") echo ", "; echo htmlspecialchars($row['subscriber_country'],ENT_NOQUOTES); ?>
								  <?php echo " " . htmlspecialchars($row['subscriber_postal_code'],ENT_NOQUOTES); ?></span>

							<?php if (trim($row['subscriber_employer'])) { ?>
								  <br><span class='bold'><?php echo htmlspecialchars(xl('Subscriber Employer'),ENT_NOQUOTES); ?>: </span><br>
								  <span class='text'><?php echo htmlspecialchars($row['subscriber_employer'],ENT_NOQUOTES); ?><br>
								  <?php echo htmlspecialchars($row['subscriber_employer_street'],ENT_NOQUOTES); ?><br>
								  <?php echo htmlspecialchars($row['subscriber_employer_city'],ENT_NOQUOTES); ?>
								  <?php if($row['subscriber_employer_city'] != "") echo ", "; echo htmlspecialchars($row['subscriber_employer_state'],ENT_NOQUOTES); ?>
								  <?php if($row['subscriber_employer_country'] != "") echo ", "; echo htmlspecialchars($row['subscriber_employer_country'],ENT_NOQUOTES); ?>
								  <?php echo " " . htmlspecialchars($row['subscriber_employer_postal_code'],ENT_NOQUOTES); ?>
								  </span>
							<?php } ?>

								 </td>
								</tr>
								<tr>
								 <td>
							<?php if ($row['copay'] != "") { ?>
								  <span class='bold'><?php echo htmlspecialchars(xl('CoPay'),ENT_NOQUOTES); ?>: </span>
								  <span class='text'><?php echo htmlspecialchars($row['copay'],ENT_NOQUOTES); ?></span>
                  <br />
							<?php } ?>
								  <span class='bold'><?php echo htmlspecialchars(xl('Accept Assignment'),ENT_NOQUOTES); ?>:</span>
								  <span class='text'><?php if($row['accept_assignment'] == "TRUE") echo xl("YES"); ?>
								  <?php if($row['accept_assignment'] == "FALSE") echo xl("NO"); ?></span>
							<?php if (!empty($row['policy_type'])) { ?>
                  <br />
								  <span class='bold'><?php echo htmlspecialchars(xl('Secondary Medicare Type'),ENT_NOQUOTES); ?>: </span>
								  <span class='text'><?php echo htmlspecialchars($policy_types[$row['policy_type']],ENT_NOQUOTES); ?></span>
							<?php } ?>
								 </td>
								 <td valign='top'></td>
								 <td valign='top'></td>
							   </tr>

							</table>
							</div>
							<?php

						} // end if ($row['provider'])
						$enddate = $row['date'];
						$first = false;
					  } // end while
					} // end foreach

					// Display the eligibility information
					echo "<div class='tab'>";
					show_eligibility_information($pid,true);
					echo "</div>";

			///////////////////////////////// END INSURANCE SECTION
			?>
			</div>

			<?php } // ?>

			</td>
		</tr>

		<tr>
			<td width='650px'>

<?php
// Notes expand collapse widget
$widgetTitle = xl("Notes");
$widgetLabel = "pnotes";
$widgetButtonLabel = xl("Edit");
$widgetButtonLink = "pnotes_full.php?form_active=1";
$widgetButtonClass = "";
$linkMethod = "html";
$bodyClass = "notab";
$widgetAuth = true;
$fixedWidth = true;
expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel,
  $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass,
  $widgetAuth, $fixedWidth);
?>

                    <br/>
                    <div style='margin-left:10px' class='text'><img src='../../pic/ajax-loader.gif'/></div><br/>
                </div>
			</td>
		</tr>
                <?php if ( (acl_check('patients', 'med')) && ($GLOBALS['enable_cdr'] && $GLOBALS['enable_cdr_prw']) ) {
                echo "<tr><td width='650px'>";
                // patient reminders collapse widget
                $widgetTitle = xl("Patient Reminders");
                $widgetLabel = "patient_reminders";
                $widgetButtonLabel = xl("Edit");
                $widgetButtonLink = "../reminder/patient_reminders.php?mode=simple&patient_id=".$pid;
                $widgetButtonClass = "";
                $linkMethod = "html";
                $bodyClass = "notab";
                $widgetAuth = true;
                $fixedWidth = true;
                expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel , $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass, $widgetAuth, $fixedWidth); ?>
                    <br/>
                    <div style='margin-left:10px' class='text'><image src='../../pic/ajax-loader.gif'/></div><br/>
                </div>
                        </td>
                </tr>
                <?php } //end if prw is activated  ?>
              
       <tr>
       <td width='650px'>
<?php
// disclosures expand collapse widget
$widgetTitle = xl("Disclosures");
$widgetLabel = "disclosures";
$widgetButtonLabel = xl("Edit");
$widgetButtonLink = "disclosure_full.php";
$widgetButtonClass = "";
$linkMethod = "html";
$bodyClass = "notab";
$widgetAuth = true;
$fixedWidth = true;
expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel,
  $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass,
  $widgetAuth, $fixedWidth);
?>
                    <br/>
                    <div style='margin-left:10px' class='text'><img src='../../pic/ajax-loader.gif'/></div><br/>
                </div>
     </td>
    </tr>		
<?php if ($GLOBALS['amendments']) { ?>
  <tr>
       <td width='650px'>
       	<?php // Amendments widget
       	$widgetTitle = xlt('Amendments');
    $widgetLabel = "amendments";
    $widgetButtonLabel = xlt("Edit");
	$widgetButtonLink = $GLOBALS['webroot'] . "/interface/patient_file/summary/main_frameset.php?feature=amendment";
	$widgetButtonClass = "iframe rx_modal";
    $linkMethod = "html";
    $bodyClass = "summary_item small";
    $widgetAuth = true;
    $fixedWidth = false;
    expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel , $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass, $widgetAuth, $fixedWidth);
       	$sql = "SELECT * FROM amendments WHERE pid = ? ORDER BY amendment_date DESC";
  $result = sqlStatement($sql, array($pid) );

  if (sqlNumRows($result) == 0) {
    echo " <table><tr>\n";
    echo "  <td colspan='$numcols' class='text'>&nbsp;&nbsp;" . xlt('None') . "</td>\n";
    echo " </tr></table>\n";
  }
  
  while ($row=sqlFetchArray($result)){
    echo "&nbsp;&nbsp;";
    echo "<a class= '" . $widgetButtonClass . "' href='" . $widgetButtonLink . "&id=" . attr($row['amendment_id']) . "' onclick='top.restoreSession()'>" . text($row['amendment_date']);
	echo "&nbsp; " . text($row['amendment_desc']);

    echo "</a><br>\n";
  } ?>
  </td>
    </tr>
<?php } ?>    		
 <?php // labdata ?>
    <tr>
     <td width='650px'>
<?php // labdata expand collapse widget
  $widgetTitle = xl("Labs");
  $widgetLabel = "labdata";
  $widgetButtonLabel = xl("Trend");
  $widgetButtonLink = "../summary/labdata.php";#"../encounter/trend_form.php?formname=labdata";
  $widgetButtonClass = "";
  $linkMethod = "html";
  $bodyClass = "notab";
  // check to see if any labdata exist
  $spruch = "SELECT procedure_report.date_collected AS date " .
			"FROM procedure_report " .
			"JOIN procedure_order ON  procedure_report.procedure_order_id = procedure_order.procedure_order_id " .
			"WHERE procedure_order.patient_id = ? " .
			"ORDER BY procedure_report.date_collected DESC ";
  $existLabdata = sqlQuery($spruch, array($pid) );
  if ($existLabdata) {
    $widgetAuth = true;
  }
  else {
    $widgetAuth = false;
  }
  $fixedWidth = true;
  expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel,
    $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass,
    $widgetAuth, $fixedWidth);
?>
      <br/>
      <div style='margin-left:10px' class='text'><img src='../../pic/ajax-loader.gif'/></div><br/>
      </div>
     </td>
    </tr>
<?php  // end labdata ?>




<?php if ($vitals_is_registered && acl_check('patients', 'med')) { ?>
    <tr>
     <td width='650px'>
<?php // vitals expand collapse widget
  $widgetTitle = xl("Vitals");
  $widgetLabel = "vitals";
  $widgetButtonLabel = xl("Trend");
  $widgetButtonLink = "../encounter/trend_form.php?formname=vitals";
  $widgetButtonClass = "";
  $linkMethod = "html";
  $bodyClass = "notab";
  // check to see if any vitals exist
  $existVitals = sqlQuery("SELECT * FROM form_vitals WHERE pid=?", array($pid) );
  if ($existVitals) {
    $widgetAuth = true;
  }
  else {
    $widgetAuth = false;
  }
  $fixedWidth = true;
  expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel,
    $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass,
    $widgetAuth, $fixedWidth);
?>
      <br/>
      <div style='margin-left:10px' class='text'><img src='../../pic/ajax-loader.gif'/></div><br/>
      </div>
     </td>
    </tr>
<?php } // end if ($vitals_is_registered && acl_check('patients', 'med')) ?>

<?php
  // This generates a section similar to Vitals for each LBF form that
  // supports charting.  The form ID is used as the "widget label".
  //
  $gfres = sqlStatement("SELECT option_id, title FROM list_options WHERE " .
    "list_id = 'lbfnames' AND " .
    "option_value > 0 AND activity = 1 " .
    "ORDER BY seq, title");
  while($gfrow = sqlFetchArray($gfres)) {
?>
    <tr>
     <td width='650px'>
<?php // vitals expand collapse widget
    $vitals_form_id = $gfrow['option_id'];
    $widgetTitle = $gfrow['title'];
    $widgetLabel = $vitals_form_id;
    $widgetButtonLabel = xl("Trend");
    $widgetButtonLink = "../encounter/trend_form.php?formname=$vitals_form_id";
    $widgetButtonClass = "";
    $linkMethod = "html";
    $bodyClass = "notab";
    // check to see if any instances exist for this patient
    $existVitals = sqlQuery(
      "SELECT * FROM forms WHERE pid = ? AND formdir = ? AND deleted = 0",
      array($pid, $vitals_form_id));
    $widgetAuth = $existVitals ? true : false;
    $fixedWidth = true;
    expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel,
      $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass,
      $widgetAuth, $fixedWidth);
?>
       <br/>
       <div style='margin-left:10px' class='text'>
        <image src='../../pic/ajax-loader.gif'/>
       </div>
       <br/>
      </div> <!-- This is required by expand_collapse_widget(). -->
     </td>
    </tr>
<?php
  } // end while
?>

   </table>

  </div>
    <!-- end left column div -->

    <!-- start right column div -->
	<div>
    <table>
    <tr>
    <td>

<div>
    <?php

    // If there is an ID Card or any Photos show the widget
    $photos = pic_array($pid, $GLOBALS['patient_photo_category_name']);
    if ($photos or $idcard_doc_id )
    {
        $widgetTitle = xl("ID Card") . '/' . xl("Photos");
        $widgetLabel = "photos";
        $linkMethod = "javascript";
        $bodyClass = "notab-right";
        $widgetAuth = false;
        $fixedWidth = false;
        expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel ,
                $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass,
                $widgetAuth, $fixedWidth);
?>
<br />
<?php
    	if ($idcard_doc_id) {
        	image_widget($idcard_doc_id, $GLOBALS['patient_id_category_name']);
		}

        foreach ($photos as $photo_doc_id) {
            image_widget($photo_doc_id, $GLOBALS['patient_photo_category_name']);
        }
    }
?>

<br />
</div>
<div>
 <?php
    // Advance Directives
    if ($GLOBALS['advance_directives_warning']) {
	// advance directives expand collapse widget
	$widgetTitle = xl("Advance Directives");
	$widgetLabel = "directives";
	$widgetButtonLabel = xl("Edit");
	$widgetButtonLink = "return advdirconfigure();";
	$widgetButtonClass = "";
	$linkMethod = "javascript";
	$bodyClass = "summary_item small";
	$widgetAuth = true;
	$fixedWidth = false;
	expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel , $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass, $widgetAuth, $fixedWidth);
          $counterFlag = false; //flag to record whether any categories contain ad records
          $query = "SELECT id FROM categories WHERE name='Advance Directive'";
          $myrow2 = sqlQuery($query);
          if ($myrow2) {
          $parentId = $myrow2['id'];
          $query = "SELECT id, name FROM categories WHERE parent=?";
          $resNew1 = sqlStatement($query, array($parentId) );
          while ($myrows3 = sqlFetchArray($resNew1)) {
              $categoryId = $myrows3['id'];
              $nameDoc = $myrows3['name'];
              $query = "SELECT documents.date, documents.id " .
                   "FROM documents " .
                   "INNER JOIN categories_to_documents " .
                   "ON categories_to_documents.document_id=documents.id " .
                   "WHERE categories_to_documents.category_id=? " .
                   "AND documents.foreign_id=? " .
                   "ORDER BY documents.date DESC";
              $resNew2 = sqlStatement($query, array($categoryId, $pid) );
              $limitCounter = 0; // limit to one entry per category
              while (($myrows4 = sqlFetchArray($resNew2)) && ($limitCounter == 0)) {
                  $dateTimeDoc = $myrows4['date'];
              // remove time from datetime stamp
              $tempParse = explode(" ",$dateTimeDoc);
              $dateDoc = $tempParse[0];
              $idDoc = $myrows4['id'];
              echo "<a href='$web_root/controller.php?document&retrieve&patient_id=" .
                    htmlspecialchars($pid,ENT_QUOTES) . "&document_id=" .
                    htmlspecialchars($idDoc,ENT_QUOTES) . "&as_file=true' onclick='top.restoreSession()'>" .
                    htmlspecialchars(xl_document_category($nameDoc),ENT_NOQUOTES) . "</a> " .
                    htmlspecialchars($dateDoc,ENT_NOQUOTES);
              echo "<br>";
              $limitCounter = $limitCounter + 1;
              $counterFlag = true;
              }
          }
          }
          if (!$counterFlag) {
              echo "&nbsp;&nbsp;" . htmlspecialchars(xl('None'),ENT_NOQUOTES);
          } ?>
      </div>
 <?php  }  // close advanced dir block

	// This is a feature for a specific client.  -- Rod
	if ($GLOBALS['cene_specific']) {
	  echo "   <br />\n";

          $imagedir  = $GLOBALS['OE_SITE_DIR'] . "/documents/$pid/demographics";
          $imagepath = "$web_root/sites/" . $_SESSION['site_id'] . "/documents/$pid/demographics";

	  echo "   <a href='' onclick=\"return sendimage($pid, 'photo');\" " .
		"title='Click to attach patient image'>\n";
	  if (is_file("$imagedir/photo.jpg")) {
		echo "   <img src='$imagepath/photo.jpg' /></a>\n";
	  } else {
		echo "   Attach Patient Image</a><br />\n";
	  }
	  echo "   <br />&nbsp;<br />\n";

	  echo "   <a href='' onclick=\"return sendimage($pid, 'fingerprint');\" " .
		"title='Click to attach fingerprint'>\n";
	  if (is_file("$imagedir/fingerprint.jpg")) {
		echo "   <img src='$imagepath/fingerprint.jpg' /></a>\n";
	  } else {
		echo "   Attach Biometric Fingerprint</a><br />\n";
	  }
	  echo "   <br />&nbsp;<br />\n";
	}

     // Show Clinical Reminders for any user that has rules that are permitted.
     $clin_rem_check = resolve_rules_sql('','0',TRUE,'',$_SESSION['authUser']);
     if ( (!empty($clin_rem_check)) && ($GLOBALS['enable_cdr'] && $GLOBALS['enable_cdr_crw']) ) {
        // clinical summary expand collapse widget
        $widgetTitle = xl("Clinical Reminders");
        $widgetLabel = "clinical_reminders";
        $widgetButtonLabel = xl("Edit");
        $widgetButtonLink = "../reminder/clinical_reminders.php?patient_id=".$pid;;
        $widgetButtonClass = "";
        $linkMethod = "html";
        $bodyClass = "summary_item small";
        $widgetAuth = true;
        $fixedWidth = false;
        expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel , $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass, $widgetAuth, $fixedWidth);
        echo "<br/>";
        echo "<div style='margin-left:10px' class='text'><image src='../../pic/ajax-loader.gif'/></div><br/>";
        echo "</div>";
        } // end if crw


      // Show current and upcoming appointments.
      //
      // Recurring appointment support and Appointment Display Sets
      // added to Appointments by Ian Jardine ( epsdky ).
      //
      if (isset($pid) && !$GLOBALS['disable_calendar']) {
      //
        $current_date2 = date('Y-m-d');
        $events = array();
        $apptNum = (int)$GLOBALS['number_of_appts_to_show'];
        if($apptNum != 0) $apptNum2 = abs($apptNum);
        else $apptNum2 = 10;
        //
        $mode1 = !$GLOBALS['appt_display_sets_option'];
        $colorSet1 = $GLOBALS['appt_display_sets_color_1'];
        $colorSet2 = $GLOBALS['appt_display_sets_color_2'];
        $colorSet3 = $GLOBALS['appt_display_sets_color_3'];
        $colorSet4 = $GLOBALS['appt_display_sets_color_4'];
        //
        if($mode1) $extraAppts = 1;
        else $extraAppts = 6;
        $events = fetchNextXAppts($current_date2, $pid, $apptNum2 + $extraAppts);
        //////
        if($events) {
          $selectNum = 0;
          $apptNumber = count($events);
          //
          if($apptNumber <= $apptNum2) {
            $extraApptDate = '';
            //
          } else if($mode1 && $apptNumber == $apptNum2 + 1) {
            $extraApptDate = $events[$apptNumber - 1]['pc_eventDate'];
            array_pop($events);
            --$apptNumber;
            $selectNum = 1;
            //
          } else if($apptNumber == $apptNum2 + 6) {
            $extraApptDate = $events[$apptNumber - 1]['pc_eventDate'];
            array_pop($events);
            --$apptNumber;
            $selectNum = 2;
            //
          } else { // mode 2 - $apptNum2 < $apptNumber < $apptNum2 + 6
            $extraApptDate = '';
            $selectNum = 2;
            //
          }
          //
          $limitApptIndx = $apptNum2 - 1;
          $limitApptDate = $events[$limitApptIndx]['pc_eventDate'];
          //
          switch ($selectNum) {
            //
            case 2:
              $lastApptIndx = $apptNumber - 1;
              $thisNumber = $lastApptIndx - $limitApptIndx;
              for($i = 1; $i <= $thisNumber; ++$i) {
                if($events[$limitApptIndx + $i]['pc_eventDate'] != $limitApptDate) {
                  $extraApptDate = $events[$limitApptIndx + $i]['pc_eventDate'];
                  $events = array_slice($events, 0, $limitApptIndx + $i);
                  break;
                }
              }
              //
              case 1:
                $firstApptIndx = 0;
                for($i = 1; $i <= $limitApptIndx; ++$i) {
                  if($events[$limitApptIndx - $i]['pc_eventDate'] != $limitApptDate) {
                    $firstApptIndx = $apptNum2 - $i;
                    break;
                  }
                }
                //
          }
          //
          if($extraApptDate) {
            if($extraApptDate != $limitApptDate) $apptStyle2 = " style='background-color:" . attr($colorSet3) . ";'";
            else $apptStyle2 = " style='background-color:" . attr($colorSet4) . ";'";
          }
        }
        //////

        // appointments expand collapse widget
        $widgetTitle = xl("Appointments");
        $widgetLabel = "appointments";
        $widgetButtonLabel = xl("Add");
        $widgetButtonLink = "return newEvt();";
        $widgetButtonClass = "";
        $linkMethod = "javascript";
        $bodyClass = "summary_item small";
        $widgetAuth = $resNotNull; // $resNotNull reflects state of query in fetchAppointments
        $fixedWidth = false;
        expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel , $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass, $widgetAuth, $fixedWidth);
        $count = 0;
        //
        $toggleSet = true;
        $priorDate = "";
        //
        foreach($events as $row) { //////
            $count++;
            $dayname = date("l", strtotime($row['pc_eventDate'])); //////
            $dispampm = "am";
            $disphour = substr($row['pc_startTime'], 0, 2) + 0;
            $dispmin  = substr($row['pc_startTime'], 3, 2);
            if ($disphour >= 12) {
                $dispampm = "pm";
                if ($disphour > 12) $disphour -= 12;
            }
            $etitle = xl('(Click to edit)');
            if ($row['pc_hometext'] != "") {
                $etitle = xl('Comments').": ".($row['pc_hometext'])."\r\n".$etitle;
            }
            //////
            if($extraApptDate && $count > $firstApptIndx) {
              $apptStyle = $apptStyle2;
            } else {
              if($row['pc_eventDate'] != $priorDate) {
                $priorDate = $row['pc_eventDate'];
                $toggleSet = !$toggleSet;
              }
              if($toggleSet) $apptStyle = " style='background-color:" . attr($colorSet2) . ";'";
              else $apptStyle = " style='background-color:" . attr($colorSet1) . ";'";
            }
            //////
            echo "<div " . $apptStyle . ">";
            echo "<a href='javascript:oldEvt(" . htmlspecialchars(preg_replace("/-/", "", $row['pc_eventDate']),ENT_QUOTES) . ', ' . htmlspecialchars($row['pc_eid'],ENT_QUOTES) . ")' title='" . htmlspecialchars($etitle,ENT_QUOTES) . "'>";
            echo "<b>" . htmlspecialchars($row['pc_eventDate'],ENT_NOQUOTES) . ", ";
            echo htmlspecialchars(sprintf("%02d", $disphour) .":$dispmin " . xl($dispampm) . " (" . xl($dayname),ENT_NOQUOTES)  . ")</b> ";
            if ($row['pc_recurrtype']) echo "<img src='" . $GLOBALS['webroot'] . "/interface/main/calendar/modules/PostCalendar/pntemplates/default/images/repeating8.png' border='0' style='margin:0px 2px 0px 2px;' title='".htmlspecialchars(xl("Repeating event"),ENT_QUOTES)."' alt='".htmlspecialchars(xl("Repeating event"),ENT_QUOTES)."'>";
            echo "<span title='" . generate_display_field(array('data_type'=>'1','list_id'=>'apptstat'),$row['pc_apptstatus']) . "'>";
            echo "<br>" . xlt('Status') . "( " . htmlspecialchars($row['pc_apptstatus'],ENT_NOQUOTES) . " ) </span>";
            echo htmlspecialchars(xl_appt_category($row['pc_catname']),ENT_NOQUOTES) . "\n";
            if ($row['pc_hometext']) echo " <span style='color:green'> Com</span>";
            echo "<br>" . htmlspecialchars($row['ufname'] . " " . $row['ulname'],ENT_NOQUOTES) . "</a></div>\n";
            //////
        }
        if ($resNotNull) { //////
            if ( $count < 1 ) {
                echo "&nbsp;&nbsp;" . htmlspecialchars(xl('None'),ENT_NOQUOTES);
            } else { //////
              if($extraApptDate) echo "<div style='color:#0000cc;'><b>" . attr($extraApptDate) . " ( + ) </b></div>";
              else echo "<div><hr></div>";
            }
            echo "</div>";
        }
      } // End of Appointments.


      /* Widget that shows recurrences for appointments. */
     if (isset($pid) && !$GLOBALS['disable_calendar'] && $GLOBALS['appt_recurrences_widget']) {

         $widgetTitle = xl("Recurrent Appointments");
         $widgetLabel = "recurrent_appointments";
         $widgetButtonLabel = xl("Add");
         $widgetButtonLink = "return newEvt();";
         $widgetButtonClass = "";
         $linkMethod = "javascript";
         $bodyClass = "summary_item small";
         $widgetAuth = false;
         $fixedWidth = false;
         expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel, $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass, $widgetAuth, $fixedWidth);
         $count = 0;
         $toggleSet = true;
         $priorDate = "";

         //Fetch patient's recurrences. Function returns array with recurrence appointments' category, recurrence pattern (interpreted), and end date.
         $recurrences = fetchRecurrences($pid);
         if($recurrences[0] == false){ //if there are no recurrent appointments:
             echo "<div>";
             echo "<span>" . xlt('None') . "</span>";
             echo "</div>";
             echo "<br>";
         }
         else {
             foreach ($recurrences as $row) {
                 //checks if there are recurrences and if they are current (git didn't end yet)
                 if ($row == false || !recurrence_is_current($row['pc_endDate']))
                     continue;
                 echo "<div>";
                 echo "<span>" . xlt('Appointment Category') . ': ' . xlt($row['pc_catname']) . "</span>";
                 echo "<br>";
                 echo "<span>" . xlt('Recurrence') . ': ' . text($row['pc_recurrspec']) . "</span>";
                 echo "<br>";
                 $red_text = ""; //if ends in a week, make font red
                 if (ends_in_a_week($row['pc_endDate'])) {
                     $red_text = " style=\"color:red;\" ";
                 }
                 echo "<span" . $red_text . ">" . xlt('End Date') . ': ' . text($row['pc_endDate']) . "</span>";
                 echo "</div>";
                 echo "<br>";
             }
         }
     }
     /* End of recurrence widget */


	// Show PAST appointments.
	// added by Terry Hill to allow reverse sorting of the appointments
 	$direction = "ASC";
	if ($GLOBALS['num_past_appointments_to_show'] < 0) {
	   $direction = "DESC";
	   ($showpast = -1 * $GLOBALS['num_past_appointments_to_show'] );
	   }
	   else
	   {
	   $showpast = $GLOBALS['num_past_appointments_to_show'];
	   }
	   
	if (isset($pid) && !$GLOBALS['disable_calendar'] && $showpast > 0) {
	 $query = "SELECT e.pc_eid, e.pc_aid, e.pc_title, e.pc_eventDate, " .
	  "e.pc_startTime, e.pc_hometext, u.fname, u.lname, u.mname, " .
	  "c.pc_catname, e.pc_apptstatus " .
	  "FROM openemr_postcalendar_events AS e, users AS u, " .
	  "openemr_postcalendar_categories AS c WHERE " .
	  "e.pc_pid = ? AND e.pc_eventDate < CURRENT_DATE AND " .
	  "u.id = e.pc_aid AND e.pc_catid = c.pc_catid " .
	  "ORDER BY e.pc_eventDate $direction , e.pc_startTime DESC " .
      "LIMIT " . $showpast;
	
     $pres = sqlStatement($query, array($pid) );

	// appointments expand collapse widget
        $widgetTitle = xl("Past Appointments");
        $widgetLabel = "past_appointments";
        $widgetButtonLabel = '';
        $widgetButtonLink = '';
        $widgetButtonClass = '';
        $linkMethod = "javascript";
        $bodyClass = "summary_item small";
        $widgetAuth = false; //no button
        $fixedWidth = false;
        expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel , $widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass, $widgetAuth, $fixedWidth);
        $count = 0;
        while($row = sqlFetchArray($pres)) {
            $count++;
            $dayname = date("l", strtotime($row['pc_eventDate']));
            $dispampm = "am";
            $disphour = substr($row['pc_startTime'], 0, 2) + 0;
            $dispmin  = substr($row['pc_startTime'], 3, 2);
            if ($disphour >= 12) {
                $dispampm = "pm";
                if ($disphour > 12) $disphour -= 12;
            }
            if ($row['pc_hometext'] != "") {
                $etitle = xl('Comments').": ".($row['pc_hometext'])."\r\n".$etitle;
            }
            echo "<a href='javascript:oldEvt(" . htmlspecialchars(preg_replace("/-/", "", $row['pc_eventDate']),ENT_QUOTES) . ', ' . htmlspecialchars($row['pc_eid'],ENT_QUOTES) . ")' title='" . htmlspecialchars($etitle,ENT_QUOTES) . "'>";
            echo "<b>" . htmlspecialchars(xl($dayname) . ", " . $row['pc_eventDate'],ENT_NOQUOTES) . "</b>" . xlt("Status") .  "(";
            echo " " .  generate_display_field(array('data_type'=>'1','list_id'=>'apptstat'),$row['pc_apptstatus']) . ")<br>";   // can't use special char parser on this
            echo htmlspecialchars("$disphour:$dispmin ") . xl($dispampm) . " ";
            echo htmlspecialchars($row['fname'] . " " . $row['lname'],ENT_NOQUOTES) . "</a><br>\n";
        }
        if (isset($pres) && $res != null) {
           if ( $count < 1 ) {
               echo "&nbsp;&nbsp;" . htmlspecialchars(xl('None'),ENT_NOQUOTES);
           }
        echo "</div>";
        }
    }
// END of past appointments            

			?>
		</div>

		<div id='stats_div'>
            <br/>
            <div style='margin-left:10px' class='text'><img src='../../pic/ajax-loader.gif'/></div><br/>
        </div>
    </td>
    </tr>
    
           <?php // TRACK ANYTHING -----

		// Determine if track_anything form is in use for this site.
		$tmp = sqlQuery("SELECT count(*) AS count FROM registry WHERE " .
						"directory = 'track_anything' AND state = 1");
		$track_is_registered = $tmp['count'];
		if($track_is_registered){
			echo "<tr> <td>";
			// track_anything expand collapse widget
			$widgetTitle = xl("Tracks");
			$widgetLabel = "track_anything";
			$widgetButtonLabel = xl("Tracks");
			$widgetButtonLink = "../../forms/track_anything/create.php";
			$widgetButtonClass = "";
			$widgetAuth = "";  // don't show the button
			$linkMethod = "html";
			$bodyClass = "notab";
			// check to see if any tracks exist
			$spruch = "SELECT id " .
				"FROM forms " .
				"WHERE pid = ? " .
				"AND formdir = ? ";
			$existTracks = sqlQuery($spruch, array($pid, "track_anything") );

			$fixedWidth = false;
			expand_collapse_widget($widgetTitle, $widgetLabel, $widgetButtonLabel,
				$widgetButtonLink, $widgetButtonClass, $linkMethod, $bodyClass,
				$widgetAuth, $fixedWidth);
?>
      <br/>
      <div style='margin-left:10px' class='text'><img src='../../pic/ajax-loader.gif'/></div><br/>
      </div>
     </td>
    </tr>
<?php  }  // end track_anything ?>
    </table>

	</div> <!-- end right column div -->

  </td>

 </tr>
</table>

</div> <!-- end main content div -->

<script language='JavaScript'>
// Array of skip conditions for the checkSkipConditions() function.
var skipArray = [
<?php echo $condition_str; ?>
];
checkSkipConditions();
</script>

</body>
</html>
