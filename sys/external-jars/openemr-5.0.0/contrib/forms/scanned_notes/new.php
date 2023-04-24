<?php
/**
* Encounter form for entering clinical data as a scanned document.
*
* Copyright (C) 2006-2013 Rod Roark <rod@sunsetsystems.com>
*
* LICENSE: This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://opensource.org/licenses/gpl-license.php>.
*
* @package   OpenEMR
* @author    Rod Roark <rod@sunsetsystems.com>
*/

// NOTE: HTML escaping still needs to be done for this script.

$sanitize_all_escapes  = true;
$fake_register_globals = false;

require_once("../../globals.php");
require_once("$srcdir/api.inc");
require_once("$srcdir/forms.inc");
require_once("$srcdir/acl.inc");

$row = array();

if (! $encounter) { // comes from globals.php
 die("Internal error: we do not seem to be in an encounter!");
}

$formid = $_GET['id'];
$imagedir = $GLOBALS['OE_SITE_DIR'] . "/documents/$pid/encounters";

// If Save was clicked, save the info.
//
if ($_POST['bn_save']) {

 // If updating an existing form...
 //
 if ($formid) {
  $query = "UPDATE form_scanned_notes SET notes = ? WHERE id = ?";
  sqlStatement($query, array($_POST['form_notes'], $formid));
 }

 // If adding a new form...
 //
 else {
  $query = "INSERT INTO form_scanned_notes (notes) VALUES (?)";
  $formid = sqlInsert($query, array($_POST['form_notes']));
  addForm($encounter, "Scanned Notes", $formid, "scanned_notes", $pid, $userauthorized);
 }

 $imagepath = "$imagedir/${encounter}_$formid.jpg";

 // Upload new or replacement document.
 // Always convert it to jpeg.
 if ($_FILES['form_image']['size']) {
  // If the patient's encounter image directory does not yet exist, create it.
  if (! is_dir($imagedir)) {
   $tmp0 = exec("mkdir -p '$imagedir'", $tmp1, $tmp2);
   if ($tmp2) die("mkdir returned $tmp2: $tmp0");
   exec("touch '$imagedir/index.html'");
  }
  // Remove any previous image files for this encounter and form ID.
  for ($i = -1; true; ++$i) {
    $suffix = ($i < 0) ? "" : "-$i";
    $path = "$imagedir/${encounter}_$formid$suffix.jpg";
    if (is_file($path)) {
      unlink($path);
    }
    else {
      if ($i >= 0) break;
    }
  }
  $tmp_name = $_FILES['form_image']['tmp_name'];
  // default density is 72 dpi, we change to 96.  And -append was removed
  // to create a separate image file for each page.
  $cmd = "convert -density 96 '$tmp_name' '$imagepath'";
  $tmp0 = exec($cmd, $tmp1, $tmp2);
  if ($tmp2) die("\"$cmd\" returned $tmp2: $tmp0");
 }

 // formHeader("Redirecting....");
 // formJump();
 // formFooter();
 // exit;
}

$imagepath = "$imagedir/${encounter}_$formid.jpg";
$imageurl = "$web_root/sites/" . $_SESSION['site_id'] .
  "/documents/$pid/encounters/${encounter}_$formid.jpg";

if ($formid) {
 $row = sqlQuery("SELECT * FROM form_scanned_notes WHERE " .
  "id = ? AND activity = '1'",
  array($formid));
 $formrow = sqlQuery("SELECT id FROM forms WHERE " .
  "form_id = ? AND formdir = 'scanned_notes'",
  array($formid));
}
?>
<html>
<head>
<?php html_header_show();?>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
<style type="text/css">
 .dehead    { color:#000000; font-family:sans-serif; font-size:10pt; font-weight:bold }
 .detail    { color:#000000; font-family:sans-serif; font-size:10pt; font-weight:normal }
</style>
<script type="text/javascript" src="../../../library/dialog.js?v=<?php echo $v_js_includes; ?>"></script>

<script language='JavaScript'>

 function newEvt() {
  dlgopen('../../main/calendar/add_edit_event.php?patientid=<?php echo $pid ?>',
   '_blank', 775, 500);
  return false;
 }

 // Process click on Delete button.
 function deleteme() {
  dlgopen('../../patient_file/deleter.php?formid=<?php echo $formrow['id'] ?>', '_blank', 500, 450);
  return false;
 }

 // Called by the deleteme.php window on a successful delete.
 function imdeleted() {
  top.restoreSession();
  location = '<?php echo $GLOBALS['form_exit_url']; ?>';
 }

</script>

</head>

<body class="body_top">

<form method="post" enctype="multipart/form-data"
 action="<?php echo $rootdir ?>/forms/scanned_notes/new.php?id=<?php echo $formid ?>"
 onsubmit="return top.restoreSession()">

<center>

<p>
<table border='1' width='95%'>

 <tr bgcolor='#dddddd' class='dehead'>
  <td colspan='2' align='center'>Scanned Encounter Notes</td>
 </tr>

 <tr>
  <td width='5%'  class='dehead' nowrap>&nbsp;Comments&nbsp;</td>
  <td width='95%' class='detail' nowrap>
   <textarea name='form_notes' rows='4' style='width:100%'><?php echo $row['notes'] ?></textarea>
  </td>
 </tr>

 <tr>
  <td class='dehead' nowrap>&nbsp;Document&nbsp;</td>
  <td class='detail' nowrap>
<?php
if ($formid && is_file($imagepath)) {
 echo "   <img src='$imageurl' />\n";
}
?>
   <p>&nbsp;
   <?php xl('Upload this file:','e') ?>
   <input type="hidden" name="MAX_FILE_SIZE" value="12000000" />
   <input name="form_image" type="file" />
   <br />&nbsp;</p>
  </td>
 </tr>

</table>

<p>
<input type='submit' name='bn_save' value='Save' />
&nbsp;
<input type='button' value='Add Appointment' onclick='newEvt()' />
&nbsp;
<input type='button' value='Back' onclick="top.restoreSession();location='<?php echo $GLOBALS['form_exit_url']; ?>'" />
<?php if ($formrow['id'] && acl_check('admin', 'super')) { ?>
&nbsp;
<input type='button' value='Delete' onclick='deleteme()' style='color:red' />
<?php } ?>
</p>

</center>

</form>
</body>
</html>
