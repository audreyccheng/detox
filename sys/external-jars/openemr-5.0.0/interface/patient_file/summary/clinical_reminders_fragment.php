<?php
//
// Copyright (C) 2010 Brady Miller (brady@sparmy.com)
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This simply shows the Clinical Reminder Widget
//

//SANITIZE ALL ESCAPES
$sanitize_all_escapes=true;
//

//STOP FAKE REGISTER GLOBALS
$fake_register_globals=false;
//

require_once( dirname(__FILE__) . "/../../globals.php");
require_once("$srcdir/clinical_rules.php");

//To improve performance and not freeze the session when running this
// report, turn off session writing. Note that php session variables
// can not be modified after the line below. So, if need to do any php
// session work in the future, then will need to remove this line.
session_write_close();

clinical_summary_widget($pid,"reminders-due",'','default',$_SESSION['authUser']);
?>
