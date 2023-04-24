<?php
/*
 * save.php for the saving of information from the misc_billing_form
 *
 * This program saves data from the misc_billing_form
 *
 * Copyright (C) 2007 Bo Huynh
 * Copyright (C) 2016 Terry Hill <terry@lillysystems.com>
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
 * along with this program. If not, see http://opensource.org/licenses/gpl-license.php.
 *
 * @package OpenEMR
 * @author Terry Hill <terry@lilysystems.com>
 * @author Brady Miller <brady.g.miller@gmail.com>
 * @link http://www.open-emr.org
 */
require_once("../../globals.php");
require_once("$srcdir/api.inc");
require_once("$srcdir/forms.inc");
require_once("$srcdir/formdata.inc.php");

if (! $encounter) { // comes from globals.php
 die(xlt("Internal error: we do not seem to be in an encounter!"));
}

if ($_POST["off_work_from"] == "0000-00-00" || $_POST["off_work_from"] == "")
	{ $_POST["is_unable_to_work"] = "0"; $_POST["off_work_to"] = "";}
	else {$_POST["is_unable_to_work"] = "1";}

if ($_POST["hospitalization_date_from"] == "0000-00-00" || $_POST["hospitalization_date_from"] == "")
	{ $_POST["is_hospitalized"] = "0"; $_POST["hospitalization_date_to"] = "";}
	else {$_POST["is_hospitalized"] = "1";}

$id = formData('id','G') + 0;

$sets = "pid = {$_SESSION["pid"]},
  groupname = '" . $_SESSION["authProvider"] . "',
  user = '" . $_SESSION["authUser"] . "',
  authorized = $userauthorized, activity=1, date = NOW(),
  employment_related          = '" . formData("employment_related") . "',
  auto_accident               = '" . formData("auto_accident") . "',
  accident_state              = '" . formData("accident_state") . "',
  other_accident              = '" . formData("other_accident") . "',
  outside_lab                 = '" . formData("outside_lab") . "',
  medicaid_referral_code      = '" . formData("medicaid_referral_code") . "',
  epsdt_flag                  = '" . formData("epsdt_flag") . "',
  provider_id                 = '" . formData("provider_id")  . "',
  provider_qualifier_code     = '" . formData("provider_qualifier_code") . "',
  lab_amount                  = '" . formData("lab_amount") . "',
  is_unable_to_work           = '" . formData("is_unable_to_work") . "',
  date_initial_treatment      = '" . formData("date_initial_treatment") . "',
  off_work_from               = '" . formData("off_work_from") . "',
  off_work_to                 = '" . formData("off_work_to") . "',
  is_hospitalized             = '" . formData("is_hospitalized") . "',
  hospitalization_date_from   = '" . formData("hospitalization_date_from") . "',
  hospitalization_date_to     = '" . formData("hospitalization_date_to") . "',
  medicaid_resubmission_code  = '" . formData("medicaid_resubmission_code") . "',
  medicaid_original_reference = '" . formData("medicaid_original_reference") . "',
  prior_auth_number           = '" . formData("prior_auth_number") . "',
  replacement_claim           = '" . formData("replacement_claim") . "',
  icn_resubmission_number     = '" . formData("icn_resubmission_number") . "',
  box_14_date_qual            = '" . formData("box_14_date_qual") . "',
  box_15_date_qual            = '" . formData("box_15_date_qual") . "',
  comments                    = '" . formData("comments") . "'";

if (empty($id)) {
  $newid = sqlInsert("INSERT INTO form_misc_billing_options SET $sets");
  addForm($encounter, "Misc Billing Options", $newid, "misc_billing_options", $pid, $userauthorized);
}
else {
  sqlStatement("UPDATE form_misc_billing_options SET $sets WHERE id = $id");
}

formHeader("Redirecting....");
formJump();
formFooter();
?>
