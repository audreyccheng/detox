<?php
/**
 * Display a message indicating that the user's password has/will expire. 
 *
 * Copyright 2010 ViCarePlus Team, Visolve <vicareplus_engg@visolve.com>
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
 * along with this program. If not, see <http://opensource.org/licenses/gpl-license.php>;.
 *
 * @package OpenEMR
 * @author  ViCarePlus Team, Visolve <vicareplus_engg@visolve.com>
 * @link    http://www.open-emr.org
 */

$fake_register_globals=false;
$sanitize_all_escapes=true;

include_once("../globals.php");
include_once("$srcdir/sql.inc");
require_once("$srcdir/translation.inc.php");

$pwd_expires = "";
$q = $_SESSION["authUserID"];
$result = sqlStatement("select username, pwd_expiration_date from users where id = ?", array($q));
if($row = sqlFetchArray($result)) {
  $pwd_expires = $row['pwd_expiration_date'];
  $username = $row['username'];
}
$current_date = date("Y-m-d");
$grace_time = date("Y-m-d", strtotime($pwd_expires . "+" . $GLOBALS['password_grace_time'] . "days"));
$pwd_alert = date("Y-m-d", strtotime($pwd_expires . "-7 days"));
$msg_alert = "";

// Determine the expiration message to display
if (($pwd_expires == "0000-00-00") or ($pwd_expires == "")) {
  $msg_alert = xl("Your Password Expired. Please change your password.");
  $case="alertmsg1";
}
else if ((strtotime($current_date) > strtotime($pwd_expires)) && ($grace_time != "") &&
         ($pwd_expires != "") && (strtotime($current_date) < strtotime($grace_time)))  {

  //display warning if user is in grace period to change password
  $msg_alert = xl("You are in Grace Login period. Change your password before")." ".$grace_time;
  $case="alertmsg1";
}
else if (strtotime($pwd_expires) == strtotime($current_date)) {
  // Display warning if password expires on current day
  $msg_alert = xl("Your Password Expires today. Please change your password.");
  $case="alertmsg2";
}
else if ((strtotime($current_date) >= strtotime($pwd_alert)) && strtotime($pwd_alert) != "") {
  // Display a notice that password expires soon
  $msg_alert = xl("Your Password Expires on")." ".$pwd_expires.". ".xl("Please change your password.");
  $case="alertmsg3";
}
?>


<html>
<head>
<meta http-equiv="Content-Language" content="en-us">
<?php html_header_show();?>
<link rel='stylesheet' href="<?php echo $css_header;?>" type="text/css">
</head>
<body class="body_bottom">

<br/><br/><br/><span class="pwdalert <?php echo attr($case); ?>">
<table align="center" >

  <tr valign="top">
    <td>&nbsp;</td>
    <td rowspan="3"><?php echo xlt("Welcome"); echo " ".text($username);?>,<br>
      <br>
      <?php  echo text($msg_alert);?>
      <br>
    </td>
    <td>&nbsp;</td>
  </tr>
 
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>
  <tr>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>
</table></span>


</body>
</html>
