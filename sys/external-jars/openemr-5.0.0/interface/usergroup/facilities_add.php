<?php
require_once("../globals.php");
require_once("../../library/acl.inc");
require_once("$srcdir/sql.inc");
require_once("$srcdir/formdata.inc.php");
require_once("$srcdir/classes/POSRef.class.php");
require_once("$srcdir/options.inc.php");
require_once("$srcdir/erx_javascript.inc.php");

$alertmsg = '';
?>
<html>
<head>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
<link rel="stylesheet" type="text/css" href="<?php echo $GLOBALS['webroot'] ?>/library/js/fancybox/jquery.fancybox-1.2.6.css" media="screen" />
<script type="text/javascript" src="<?php echo $GLOBALS['webroot'] ?>/library/dialog.js?v=<?php echo $v_js_includes; ?>"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative'] ?>/jquery-min-1-9-1/index.js"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['webroot'] ?>/library/js/common.js"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['webroot'] ?>/library/js/fancybox/jquery.fancybox-1.2.6.js"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['webroot'] ?>/library/js/jquery-ui.js"></script>
<script type="text/javascript" src="../main/calendar/modules/PostCalendar/pnincludes/AnchorPosition.js"></script>
<script type="text/javascript" src="../main/calendar/modules/PostCalendar/pnincludes/PopupWindow.js"></script>
<script type="text/javascript" src="../main/calendar/modules/PostCalendar/pnincludes/ColorPicker2.js"></script>

<!-- validation library -->
<!--//Not lbf forms use the new validation, please make sure you have the corresponding values in the list Page validation-->
<?php    $use_validate_js = 1;?>
<?php  require_once($GLOBALS['srcdir'] . "/validation/validation_script.js.php"); ?>
<?php  require_once($GLOBALS['srcdir'] . "/validation/validate_core.php"); ?>
<?php
//Gets validation rules from Page Validation list.
//Note that for technical reasons, we are bypassing the standard validateUsingPageRules() call.
$collectthis = collectValidationPageRules("/interface/usergroup/facilities_add.php");
if (empty($collectthis)) {
    $collectthis = "undefined";
}
else {
    $collectthis = $collectthis["facility-add"]["rules"];
}

// Old Browser comp trigger on js

if (isset($_POST["mode"]) && $_POST["mode"] == "facility") {
  	echo '
<script type="text/javascript">
<!--
parent.$.fn.fancybox.close();
//-->
</script>

	';
}
?>
<script type="text/javascript">
/// todo, move this to a common library

var collectvalidation = <?php echo($collectthis); ?>;

function submitform() {

    var valid = submitme(1, undefined, 'facility-add', collectvalidation);
    if (!valid) return;

	<?php if($GLOBALS['erx_enable']){ ?>
	alertMsg='';
	f=document.forms[0];
	for(i=0;i<f.length;i++){
		if(f[i].type=='text' && f[i].value)
		{
			if(f[i].name == 'facility' || f[i].name == 'Washington')
			{
				alertMsg += checkLength(f[i].name,f[i].value,35);
				alertMsg += checkFacilityName(f[i].name,f[i].value);
			}
			else if(f[i].name == 'street')
			{
				alertMsg += checkLength(f[i].name,f[i].value,35);
				alertMsg += checkAlphaNumeric(f[i].name,f[i].value);
			}
			else if(f[i].name == 'phone' || f[i].name == 'fax')
			{
				alertMsg += checkPhone(f[i].name,f[i].value);
			}
			else if(f[i].name == 'federal_ein')
			{
				alertMsg += checkLength(f[i].name,f[i].value,10);
				alertMsg += checkFederalEin(f[i].name,f[i].value);
			}
		}
	}
	if(alertMsg)
	{
		alert(alertMsg);
		return false;
	}
	<?php } ?>

    top.restoreSession();
    document.forms[0].submit();
}

function toggle( target, div ) {

    $mode = $(target).find(".indicator").text();
    if ( $mode == "collapse" ) {
        $(target).find(".indicator").text( "expand" );
        $(div).hide();
    } else {
        $(target).find(".indicator").text( "collapse" );
        $(div).show();
    }

}

$(document).ready(function(){

    $("#dem_view").click( function() {
        toggle( $(this), "#DEM" );
    });

    // fancy box
    enable_modals();

    tabbify();

    // special size for
	$(".large_modal").fancybox( {
		'overlayOpacity' : 0.0,
		'showCloseButton' : true,
		'frameHeight' : 600,
		'frameWidth' : 1000
	});

    // special size for
	$(".medium_modal").fancybox( {
		'overlayOpacity' : 0.0,
		'showCloseButton' : true,
		'frameHeight' : 260,
		'frameWidth' : 510
	});

});

$(document).ready(function(){
    $("#cancel").click(function() {
		  parent.$.fn.fancybox.close();
	 });

    /**
     * add required/star sign to required form fields
     */
    for (var prop in collectvalidation) {
        //if (collectvalidation[prop].requiredSign)
        if (collectvalidation[prop].presence)
            jQuery("input[name='" + prop + "']").after('*');
    }
});
var cp = new ColorPicker('window');
  // Runs when a color is clicked
function pickColor(color) {
 	document.getElementById('ncolor').value = color;
}
var field;
function pick(anchorname,target) {
	var cp = new ColorPicker('window');
  	field=target;
        cp.show(anchorname);
}
function displayAlert()
{
	if(document.getElementById('primary_business_entity').checked==false)
	alert("<?php echo addslashes(xl('Primary Business Entity tax id is used as account id for NewCrop ePrescription. Changing the facility will affect the working in NewCrop.'));?>");
	else if(document.getElementById('primary_business_entity').checked==true)
	alert("<?php echo addslashes(xl('Once the Primary Business Facility is set, it should not be changed. Changing the facility will affect the working in NewCrop ePrescription.'));?>");
}
</script>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">

</head>
<body class="body_top">
<table>
<tr><td>
    <span class="title"><?php xl('Add Facility','e'); ?></span>&nbsp;&nbsp;&nbsp;</td>
    <td colspan=5 align=center style="padding-left:2px;">
        <a onclick="submitform();" class="css_button large_button" name='form_save' id='form_save' href='#'>
            <span class='css_button_span large_button_span'><?php xl('Save','e');?></span>
        </a>
        <a class="css_button large_button" id='cancel' href='#' >
            <span class='css_button_span large_button_span'><?php xl('Cancel','e');?></span>
        </a>
</td></tr>
</table>

<br>

<form name='facility-add' id='facility-add' method='post' action="facilities.php" target='_parent'>
    <input type=hidden name=mode value="facility">
    <table border=0 cellpadding=0 cellspacing=0>
        <tr>
        <td><span class="text"><?php xl('Name','e'); ?>: </span></td><td><input type=entry name=facility size=20 value=""></td>
        <td width=20>&nbsp;</td>
        <td><span class="text"><?php xl('Phone','e'); ?>: </span></td><td><input type=entry name=phone size=20 value=""></td>
        </tr>
        <tr>
        <td><span class="text"><?php xl('Address','e'); ?>: </span></td><td><input type=entry size=20 name=street value=""></td>
        <td>&nbsp;</td>
        <td><span class="text"><?php xl('Fax','e'); ?>: </span></td><td><input type=entry name=fax size=20 value=""></td>
        </tr>
        <tr>
        <td><span class="text"><?php xl('City','e'); ?>: </span></td><td><input type=entry size=20 name=city value=""></td>
        <td>&nbsp;</td>
        <td><span class="text"><?php xl('Zip Code','e'); ?>: </span></td><td><input type=entry size=20 name=postal_code value=""></td>
        </tr>
        <tr>
        <td><span class="text"><?php xl('State','e'); ?>: </span></td><td><input type=entry size=20 name=state value=""></td>
        <td>&nbsp;</td>
        <td><span class="text"><?php xl('Tax ID','e'); ?>: </span></td><td><select name=tax_id_type><option value="EI"><?php xl('EIN','e'); ?></option><option value="SY"><?php xl('SSN','e'); ?></option></select><input type=entry size=11 name=federal_ein value=""></td>
        </tr>
        <tr>
        <td height="22"><span class="text"><?php xl('Country','e'); ?>: </span></td><td><input type=entry size=20 name=country_code value=""></td>
        <td>&nbsp;</td>
        <td><span class="text"><?php ($GLOBALS['simplified_demographics'] ? xl('Facility Code','e') : xl('Facility NPI','e')); ?>:
        </span></td><td><input type=entry size=20 name=facility_npi value=""></td>
        </tr>
		<tr>
        <td><span class="text"><?php xl('Website','e'); ?>: </span></td><td><input type=entry size=20 name=website value=""></td>
        <td>&nbsp;</td>
        <td><span class="text"><?php xl('Email','e'); ?>: </span></td><td><input type=entry size=20 name=email value=""></td>
        </tr>

        <tr>
          <td><span class='text'><?php xl('Billing Location','e'); ?>: </span></td><td><input type='checkbox' name='billing_location' value = '1'></td>
          <td>&nbsp;</td>
          <td><span class='text'><?php xl('Accepts Assignment','e'); ?><br>(<?php xl('only if billing location','e'); ?>): </span></td> <td><input type='checkbox' name='accepts_assignment' value = '1'></td>
        </tr>
        <tr>
          <td><span class='text'><?php xl('Service Location','e'); ?>: </span></td> <td><input type='checkbox' name='service_location' value = '1'></td>
          <td>&nbsp;</td>
          <td><span class='text'><?php echo htmlspecialchars(xl('Color'),ENT_QUOTES); ?>: </span></td> <td><input type=entry name=ncolor id=ncolor size=20 value=""><span>[<a href="javascript:void(0);" onClick="pick('pick','newcolor');return false;" NAME="pick" ID="pick"><?php echo htmlspecialchars(xl('Pick'),ENT_QUOTES); ?></a>]</span></td>
        </tr>
	<?php
	 $disabled='';
	 $resPBE=sqlStatement("select * from facility where primary_business_entity='1' and id!='".$my_fid."'");
	 if(sqlNumRows($resPBE)>0)
	 $disabled='disabled';
	 ?>
	 <tr>
          <td><span class='text'><?php xl('Primary Business Entity','e'); ?>: </span></td>
          <td><input type='checkbox' name='primary_business_entity' id='primary_business_entity' value='1' <?php if ($facility['primary_business_entity'] == 1) echo 'checked'; ?> <?php if($GLOBALS['erx_enable']){ ?> onchange='return displayAlert()' <?php } ?> <?php echo $disabled;?>></td>
          <td>&nbsp;</td>
         </tr>
        <tr>
            <td><span class=text><?php xl('POS Code','e'); ?>: </span></td>
            <td colspan="6">
                <select name="pos_code">
                <?php
                $pc = new POSRef();

                foreach ($pc->get_pos_ref() as $pos) {
                    echo "<option value=\"" . $pos["code"] . "\" ";
                    echo ">" . $pos['code']  . ": ". text($pos['title']);
                    echo "</option>\n";
                }

                ?>
                </select>
            </td>
        </tr>
        <tr>
            <td><span class="text"><?php xl('Billing Attn','e'); ?>:</span></td>
            <td colspan="4"><input type="text" name="attn" size="45"></td>
        </tr>
        <tr>
            <td><span class="text"><?php xl('CLIA Number','e'); ?>:</span></td>
            <td colspan="4"><input type="text" name="domain_identifier" size="45"></td>
        </tr>
        <tr>
            <td><span class="text"><?php xl('Facility ID','e'); ?>:</span></td>
            <td colspan="4"><input type="text" name="facility_id" size="20"></td>
        </tr>
        <tr height="25" style="valign:bottom;">
        <td><font class="mandatory">*</font><span class="text"> <?php echo xl('Required','e'); ?></span></td><td>&nbsp;</td><td>&nbsp;</td>
        <td>&nbsp;</td><td>&nbsp;</td>
        </tr>
    </table>
</form>

<script language="JavaScript">
<?php
  if ($alertmsg = trim($alertmsg)) {
    echo "alert('$alertmsg');\n";
  }
?>
</script>

</body>
</html>
