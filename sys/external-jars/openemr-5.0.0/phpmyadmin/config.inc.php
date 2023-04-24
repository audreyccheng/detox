<?php
/*
 * Customized for OpenEMR.
 *
 */

// Access control is dealt with by the ACL check
$fake_register_globals=false;
$sanitize_all_escapes=true;
$ignoreAuth = true;
require_once(dirname(__FILE__)."/../interface/globals.php");
require_once(dirname(__FILE__)."/../library/acl.inc");
if ($GLOBALS['disable_phpmyadmin_link']) {
  echo "You do not have access to this resource<br>";
  exit;
}
if (! acl_check('admin', 'database')) {
  echo "You do not have access to this resource<br>";
  exit;
}

/* Servers configuration */
$i = 0;

/* Server localhost (config:openemr) [1] */
$i++;

/* For standard OpenEMR database access */
$cfg['Servers'][$i]['auth_type'] = 'config';
$cfg['Servers'][$i]['host'] = $sqlconf['host'];
$cfg['Servers'][$i]['port'] = $sqlconf['port'];
$cfg['Servers'][$i]['user'] = $sqlconf['login'];
$cfg['Servers'][$i]['password'] = $sqlconf['pass'];
$cfg['Servers'][$i]['only_db'] = $sqlconf['dbase'];

/* Other mods for OpenEMR */
$cfg['AllowThirdPartyFraming'] = TRUE;
$cfg['ShowCreateDb'] = false;
$cfg['ShowPhpInfo'] = TRUE;
?>
