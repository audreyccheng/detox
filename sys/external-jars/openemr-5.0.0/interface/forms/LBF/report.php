<?php
// Copyright (C) 2009, 2014 Rod Roark <rod@sunsetsystems.com>
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

include_once(dirname(__FILE__).'/../../globals.php');
include_once($GLOBALS["srcdir"] . "/api.inc");

// This function is invoked from printPatientForms in report.inc
// when viewing a "comprehensive patient report".  Also from
// interface/patient_file/encounter/forms.php.

function lbf_report($pid, $encounter, $cols, $id, $formname, $no_wrap = false) {
  require_once($GLOBALS["srcdir"] . "/options.inc.php");
  $arr = array();
  $shrow = getHistoryData($pid);
  $fres = sqlStatement("SELECT * FROM layout_options " .
    "WHERE form_id = ? AND uor > 0 " .
    "ORDER BY group_name, seq", array($formname));
  while ($frow = sqlFetchArray($fres)) {
    $field_id  = $frow['field_id'];
    $currvalue = '';
    if ($frow['edit_options'] == 'H') {
      if (isset($shrow[$field_id])) $currvalue = $shrow[$field_id];
    } else {
      $currvalue = lbf_current_value($frow, $id, $encounter);
      if ($currvalue === FALSE) continue; // should not happen
    }
    // For brevity, skip fields without a value.
    if ($currvalue === '') continue;
    // $arr[$field_id] = $currvalue;
    // A previous change did this instead of the above, not sure if desirable? -- Rod
    // $arr[$field_id] = wordwrap($currvalue, 30, "\n", true);
    // Hi Rod content width issue in Encounter Summary - epsdky
    if($no_wrap) $arr[$field_id] = $currvalue; // Making wordwrap selectable - epsdky 2017
    else $arr[$field_id] = wordwrap($currvalue, 30, "\n", true);
  }
  echo "<table>\n";
  display_layout_rows($formname, $arr);
  echo "</table>\n";
 }
?>
