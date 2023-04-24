<?php
include_once("../globals.php");
include_once("../../library/acl.inc");
?>
<html>
<head>
<title><?php xl('Navigation','e'); ?></title>

<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">

</head>
<body class="body_nav">

<form border=0 method=post target="_top" name="find_patient" action="../main/finder/patient_finder.php">

<table border="0" cellspacing="0" cellpadding="0" width="100%" height="100%">
<tr>

<?php if (acl_check('admin', 'users')) { ?>
<td valign="middle" nowrap>
&nbsp;&nbsp;<a class=menu target=Main href="facilities.php"
 onclick="top.restoreSession()"
 title="Add or Edit Facilities"><?php xl('Facilities','e'); ?></a>&nbsp;
</td>
<?php } ?>

<?php if (acl_check('admin', 'users')) { ?>
<td valign="middle" nowrap>
&nbsp;<a class=menu target=Main href="usergroup_admin.php"
 onclick="top.restoreSession()"
 title="Add or Edit Users and Groups"><?php xl('Users','e'); ?></a>&nbsp;
</td>
<?php } ?>

<?php if (acl_check('admin', 'forms')) { ?>
<td valign="middle" nowrap>
&nbsp;<a class=menu target=Main href="../forms_admin/forms_admin.php"
 onclick="top.restoreSession()"
 title="Activate New Forms"><?php xl('Forms','e'); ?></a>&nbsp;
</td>
<?php } ?>

<?php if (acl_check('admin', 'practice')) { ?>
<td valign="middle" nowrap>
&nbsp;<a class=menu target=Main href="<?php echo $GLOBALS['webroot']?>/controller.php?practice_settings"
 onclick="top.restoreSession()"
 title="Practice Settings"><?php xl('Practice','e');?></a>&nbsp;
</td>
<?php } ?>

<?php if (acl_check('admin', 'acl') && isset($phpgacl_location)) { ?>
<td valign="middle" nowrap>
&nbsp;<a class=menu target=Main href="adminacl.php"
 onclick="top.restoreSession()"
 title="Access Control List Administration"><?php xl('ACL','e');?></a>&nbsp;
</td>
<?php } ?>
	
<?php if (acl_check('admin', 'calendar')) { ?>
<td valign="middle" nowrap>
&nbsp;<a class=menu target=Main href="../main/calendar/index.php?module=PostCalendar&type=admin&func=modifyconfig"
 onclick="top.restoreSession()"
 title="Calendar Settings"><?php xl('Calendar','e'); ?></a>&nbsp;
</td>
<?php } ?>

<?php if ( (!$GLOBALS['disable_phpmyadmin_link']) && (acl_check('admin', 'database')) ) { ?>
<td valign="middle" nowrap>
&nbsp;<a class=menu target=Main href="../../phpmyadmin/index.php"
 onclick="top.restoreSession()"
 title="Database Reporting"><?php xl('Database','e'); ?></a>&nbsp;
</td>
<?php } ?>

<?php if (acl_check('admin', 'batchcom')) { ?>
<td valign="middle" nowrap>
&nbsp;<a class=menu target=Main href="../batchcom/batchcom.php"
 onclick="top.restoreSession()"
   title="Batch Communication and Export"><?php xl('Notification','e');?></a>&nbsp;
     </td>
<?php } ?>

<?php if ($GLOBALS['inhouse_pharmacy'] && acl_check('admin', 'drugs')) { ?>
<td valign="middle" nowrap>
&nbsp;<a class=menu target=Main href="../drugs/drug_inventory.php"
 onclick="top.restoreSession()"
 title="Drug Inventory Management"><?php xl('Drugs','e');?></a>&nbsp;
</td>
<?php } ?>

<?php if (acl_check('admin', 'language')) { ?>
<td valign="middle" nowrap>
&nbsp;<a class=menu target=Main href="../language/language.php"
 onclick="top.restoreSession()"
 title="Language Management"><?php xl('Language','e'); ?></a>&nbsp;
</td>
<?php } ?>

<?php if (acl_check('admin', 'super')) { ?>
<td valign="middle" nowrap>
&nbsp;<a class="menu" target=Main href="../super/edit_globals.php"
 onclick="top.restoreSession()"
 title="Global System Parameters"><?php xl('Globals','e'); ?></a>&nbsp;
</td>
<td valign="middle" nowrap>
&nbsp;<a class="menu" target=Main href="../super/edit_list.php"
 onclick="top.restoreSession()"
 title="Selection List Management"><?php xl('Lists','e'); ?></a>&nbsp;
</td>
<td valign="middle" nowrap>
&nbsp;<a class="menu" target=Main href="../super/edit_layout.php"
 onclick="top.restoreSession()"
 title="Form Layout Management"><?php xl('Layouts','e'); ?></a>&nbsp;
</td>
<td valign="middle" nowrap>
&nbsp;<a class="menu" target=Main href="../super/manage_site_files.php"
 onclick="top.restoreSession()"
 title="Site Files Management"><?php xl('Files','e'); ?></a>&nbsp;
</td>
<td valign="middle" nowrap>
&nbsp;<a class="menu" target=Main href="../main/backup.php"
 onclick="top.restoreSession()"
 title="System Backup"><?php xl('Backup','e'); ?></a>&nbsp;
</td>
<?php } ?>

<?php if (acl_check('admin', 'users')) { ?>
<td valign="middle" nowrap>
&nbsp;<a class=menu target=Main href="<?php echo $rootdir?>/logview/logview.php"
 onclick="top.restoreSession()"
 title="View Logs"><?php xl('Logs','e'); ?></a>&nbsp;
</td>
<?php } ?>

</tr>
</table>

</form>

</body>
</html>
