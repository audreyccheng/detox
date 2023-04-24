<?php
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

include_once("../../globals.php");
require_once $GLOBALS['srcdir'].'/ESign/Api.php';

$esignApi = new Esign\Api();
?>
<?php if (empty($hide)) { // if not included by forms.php ?>
<html>
<head>
<?php } ?>
<?php html_header_show();?>
<link rel="stylesheet" href="<?php echo $css_header; ?>" type="text/css">

<script language="JavaScript">

function openNewForm(sel) {
  top.restoreSession();
  FormNameValueArray = sel.split('formname=');
  if(FormNameValueArray[1] == 'newpatient')
  {
    parent.location.href = sel;
  }
  else if (!parent.Forms)
  {
    location.href = sel;
  }
  else
  {
    parent.Forms.location.href = sel;
  }
}

function toggleFrame1(fnum) {
  top.frames['left_nav'].document.forms[0].cb_top.checked=false;
  top.window.parent.left_nav.toggleFrame(fnum);
 }
</script>
<style type="text/css">
#sddm
{	margin: 0;
	padding: 0;
	z-index: 30;
}

</style>
<script type="text/javascript" language="javascript">

var timeout	= 500;
var closetimer	= 0;
var ddmenuitem	= 0;
var oldddmenuitem = 0;
var flag = 0;

// open hidden layer
function mopen(id)
{
	// cancel close timer
	//mcancelclosetime();

	flag=10;

	// close old layer
	//if(ddmenuitem) ddmenuitem.style.visibility = 'hidden';
	//if(ddmenuitem) ddmenuitem.style.display = 'none';

	// get new layer and show it
        oldddmenuitem = ddmenuitem;
	ddmenuitem = document.getElementById(id);
        if((ddmenuitem.style.visibility == '')||(ddmenuitem.style.visibility == 'hidden')){
            if(oldddmenuitem) oldddmenuitem.style.visibility = 'hidden';
            if(oldddmenuitem) oldddmenuitem.style.display = 'none';
            ddmenuitem.style.visibility = 'visible';
            ddmenuitem.style.display = 'block';
        }else{
            ddmenuitem.style.visibility = 'hidden';
            ddmenuitem.style.display = 'none';
        }
}
// close showed layer
function mclose()
{
	if(flag==10)
	 {
	  flag=11;
	  return;
	 }
	if(ddmenuitem) ddmenuitem.style.visibility = 'hidden';
	if(ddmenuitem) ddmenuitem.style.display = 'none';
}

// close layer when click-out
document.onclick = mclose;
//=================================================
function findPosX(id)
  {
    obj=document.getElementById(id);
	var curleft = 0;
    if(obj.offsetParent)
        while(1)
        {
          curleft += obj.offsetLeft;
          if(!obj.offsetParent)
            break;
          obj = obj.offsetParent;
        }
    else if(obj.x)
        curleft += obj.x;
   PropertyWidth=document.getElementById(id).offsetWidth;
   if(PropertyWidth>curleft)
    {
	 document.getElementById(id).style.left=0;
	}
  }

  function findPosY(obj)
  {
    var curtop = 0;
    if(obj.offsetParent)
        while(1)
        {
          curtop += obj.offsetTop;
          if(!obj.offsetParent)
            break;
          obj = obj.offsetParent;
        }
    else if(obj.y)
        curtop += obj.y;
    return curtop;
  }
</script>

</head>
<body class="bgcolor2">
<dl>
<?php //DYNAMIC FORM RETREIVAL
include_once("$srcdir/registry.inc");

function myGetRegistered($state="1", $limit="unlimited", $offset="0") {
  $sql = "SELECT category, nickname, name, state, directory, id, sql_run, " .
    "unpackaged, date FROM registry WHERE " .
    "state LIKE \"$state\" ORDER BY category, priority, name";
  if ($limit != "unlimited") $sql .= " limit $limit, $offset";
  $res = sqlStatement($sql);
  if ($res) {
    for($iter=0; $row=sqlFetchArray($res); $iter++) {
      $all[$iter] = $row;
    }
  }
  else {
    return false;
  }
  return $all;
}

$reg = myGetRegistered();
$old_category = '';

  $DivId=1;

// To see if the encounter is locked. If it is, no new forms can be created
$encounterLocked = false;
if ( $esignApi->lockEncounters() &&
isset($GLOBALS['encounter']) &&
!empty($GLOBALS['encounter']) ) {

  $esign = $esignApi->createEncounterESign( $GLOBALS['encounter'] );
  if ( $esign->isLocked() ) {
      $encounterLocked = true;
  }
}

if (!empty($reg)) {
  $StringEcho= '<ul id="sddm">';
  if(isset($hide)){
    $StringEcho.= "<li><a id='enc2' >" . htmlspecialchars( xl('Encounter Summary'),ENT_NOQUOTES) . "</a></li>";
  }else{
    if ($GLOBALS['new_tabs_layout']) {
      $StringEcho.= "<li><a href='JavaScript:void(0);' id='enc2' onclick=\" return top.window.parent.left_nav.loadFrame('enc2','enc','patient_file/encounter/encounter_top.php')\">" . htmlspecialchars( xl('Encounter Summary'),ENT_NOQUOTES) . "</a></li>";
    }
    else {
      $StringEcho.= "<li><a href='JavaScript:void(0);' id='enc2' onclick=\" return top.window.parent.left_nav.loadFrame2('enc2','RBot','patient_file/encounter/encounter_top.php')\">" . htmlspecialchars( xl('Encounter Summary'),ENT_NOQUOTES) . "</a></li>";
    }
  }
  if ( $encounterLocked === false ) {
      foreach ($reg as $entry) {
        $new_category = trim($entry['category']);
        $new_nickname = trim($entry['nickname']);
        if ($new_category == '') {
          $new_category = htmlspecialchars(xl('Miscellaneous'),ENT_QUOTES);
        }else{
          $new_category = htmlspecialchars(xl($new_category),ENT_QUOTES);
        }
        if ($new_nickname != '') {$nickname = $new_nickname;}
        else {$nickname = $entry['name'];}
        if ($old_category != $new_category) {
          $new_category_ = $new_category;
          $new_category_ = str_replace(' ','_',$new_category_);
          if ($old_category != '') {$StringEcho.= "</table></div></li>";}
          $StringEcho.= "<li class=\"encounter-form-category-li\"><a href='JavaScript:void(0);' onClick=\"mopen('$DivId');\" >$new_category</a><div id='$DivId' ><table border='0' cellspacing='0' cellpadding='0'>";
          $old_category = $new_category;
          $DivId++;
        }
        $StringEcho.= "<tr><td style='border-top: 1px solid #000000;padding:0px;'><a onclick=\"openNewForm('" . $rootdir .'/patient_file/encounter/load_form.php?formname=' .urlencode($entry['directory']) .
        "')\" href='JavaScript:void(0);'>" . xl_form_title($nickname) . "</a></td></tr>";
      }
  }
  $StringEcho.= '</table></div></li>';
}
if($StringEcho){
  $StringEcho2= '<div style="clear:both"></div>';
}else{
  $StringEcho2="";
}
?>
<!--<table   style="border:solid 1px black" cellspacing="0" cellpadding="0">
 <tr>
    <td valign="top"><?php //echo $StringEcho; ?></td>
  </tr>
</table>-->
<?php
//$StringEcho='';
// This shows Layout Based Form names just like the above.
//
if ( $encounterLocked === false ) {
    $lres = sqlStatement("SELECT * FROM list_options " .
      "WHERE list_id = 'lbfnames' AND activity = 1 ORDER BY seq, title");
    if (sqlNumRows($lres)) {
      if(!$StringEcho){
        $StringEcho= '<ul id="sddm">';
      }
      $StringEcho.= "<li class=\"encounter-form-category-li\"><a href='JavaScript:void(0);' onClick=\"mopen('lbf');\" >".xl('Layout Based') ."</a><div id='lbf' ><table border='0'  cellspacing='0' cellpadding='0'>";
      while ($lrow = sqlFetchArray($lres)) {
      $option_id = $lrow['option_id']; // should start with LBF
      $title = $lrow['title'];
      $StringEcho.= "<tr><td style='border-top: 1px solid #000000;padding:0px;'><a href='" . $rootdir .'/patient_file/encounter/load_form.php?formname='
    				.urlencode($option_id) ."' >" . xl_form_title($title) . "</a></td></tr>";
      }
    }
}
?>
<!-- DISPLAYING HOOKS STARTS HERE -->
<?php
	$module_query = sqlStatement("SELECT msh.*,ms.menu_name,ms.path,m.mod_ui_name,m.type FROM modules_hooks_settings AS msh LEFT OUTER JOIN modules_settings AS ms ON
                                    obj_name=enabled_hooks AND ms.mod_id=msh.mod_id LEFT OUTER JOIN modules AS m ON m.mod_id=ms.mod_id
                                    WHERE fld_type=3 AND mod_active=1 AND sql_run=1 AND attached_to='encounter' ORDER BY mod_id");
  $DivId = 'mod_installer';
  if (sqlNumRows($module_query)) {
    $jid = 0;
    $modid = '';
    while ($modulerow = sqlFetchArray($module_query)) {
      $DivId = 'mod_'.$modulerow['mod_id'];
      $new_category = $modulerow['mod_ui_name'];
      $modulePath = "";
      $added      = "";
      if($modulerow['type'] == 0) {
        $modulePath = $GLOBALS['customModDir'];
        $added		= "";
      }
      else{
        $added		= "index";
        $modulePath = $GLOBALS['zendModDir'];
      }
      $relative_link = "../../modules/".$modulePath."/".$modulerow['path'];
      $nickname = $modulerow['menu_name'] ? $modulerow['menu_name'] : 'Noname';
      if($jid==0 || ($modid!=$modulerow['mod_id'])){
        if($modid!='')
        $StringEcho.= '</table></div></li>';
      $StringEcho.= "<li><a href='JavaScript:void(0);' onClick=\"mopen('$DivId');\" >$new_category</a><div id='$DivId' ><table border='0' cellspacing='0' cellpadding='0'>";
      }
      $jid++;
      $modid = $modulerow['mod_id'];
      $StringEcho.= "<tr><td style='border-top: 1px solid #000000;padding:0px;'><a onclick=\"openNewForm('$relative_link')\" href='JavaScript:void(0);'>" . xl_form_title($nickname) . "</a></td></tr>";
   }
  }
	?>
<!-- DISPLAYING HOOKS ENDS HERE -->
<?php
if($StringEcho){
  $StringEcho.= "</table></div></li></ul>".$StringEcho2;
}
?>
<table cellspacing="0" cellpadding="0" align="center">
  <tr>
    <td valign="top"><?php echo $StringEcho; ?></td>
  </tr>
</table>
</dl>
<?php if (empty($hide)) { ?>
</body>
</html>
<?php } ?>
