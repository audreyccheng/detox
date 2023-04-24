<?php
include_once("../globals.php");
include_once("../../library/acl.inc");

$_SESSION["encounter"] = "";

if (/*$userauthorized*/ true) {
?>
<HTML>
<HEAD>
<TITLE>
<?php echo $openemr_name ?>
</TITLE>

<script language='JavaScript'>
<?php require($GLOBALS['srcdir'] . "/restoreSession.php"); ?>
</script>

</HEAD>
<frameset rows="<?php echo "$GLOBALS[navBarHeight],$GLOBALS[titleBarHeight]" ?>,*" cols="*" frameborder="NO" border="0" framespacing="0">
  <frame src="usergroup_navigation.php" name="Navigation" scrolling="no" noresize frameborder="NO">
  <frame src="usergroup_title.php" name="Title" scrolling="no" noresize frameborder="NO">
  <frame
<?php if (acl_check('admin', 'users')) { ?>
   src="usergroup_admin.php"
<?php } else if (acl_check('admin', 'forms')) { ?>
   src="../forms_admin/forms_admin.php"
<?php } else if (acl_check('admin', 'practice')) { ?>
   src="<?php echo $GLOBALS['webroot']?>/controller.php?practice_settings"
<?php } else if (acl_check('admin', 'calendar')) { ?>
   src="../main/calendar/index.php?module=PostCalendar&type=admin&func=modifyconfig"
<?php } else if ( (!$GLOBALS['disable_phpmyadmin_link']) && (acl_check('admin', 'database')) ) { ?>
   src="../../phpmyadmin/index.php"
<?php } else { ?>
   src="<?php echo $rootdir?>/logview/logview.php"
<?php } ?>
   name="Main" scrolling="auto" noresize frameborder="NO">
</frameset>

<noframes><body bgcolor="#FFFFFF">

</body></noframes>

</HTML>

<?php
} else {
?>

<html>
<body>
<script language="Javascript">

window.location="<?php echo "$rootdir/main/main_screen.php";?>";

</script>

</body>
</html>

<?php
}
?>
