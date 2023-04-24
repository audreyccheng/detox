<?php
// Copyright (C) 2008-2011 Rod Roark <rod@sunsetsystems.com>
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

require_once("Claim.class.php");
require_once("gen_hcfa_1500_02_12.inc.php");

$hcfa_curr_line = 1;
$hcfa_curr_col = 1;
$hcfa_data = '';
$hcfa_proc_index = 0;


/**
 * take the data element and place it at the correct coordinates on the page
 *
 * @global int $hcfa_curr_line
 * @global type $hcfa_curr_col
 * @global type $hcfa_data
 * @param type $line
 * @param type $col
 * @param type $maxlen
 * @param type $data
 * @param type $strip   regular expression for what to strip from the data. period and has are the defaults
 *                      02/12 version needs to include periods in the diagnoses hence the need to override
 */
function put_hcfa($line, $col, $maxlen, $data,$strip='/[.#]/') {
  global $hcfa_curr_line, $hcfa_curr_col, $hcfa_data;
  if ($line < $hcfa_curr_line)
    die("Data item at ($line, $col) precedes current line.");
  while ($hcfa_curr_line < $line) {
    $hcfa_data .= "\n";
    ++$hcfa_curr_line;
    $hcfa_curr_col = 1;
  }
  if ($col < $hcfa_curr_col)
    die("Data item at ($line, $col) precedes current column.");
  while ($hcfa_curr_col < $col) {
    $hcfa_data .= " ";
    ++$hcfa_curr_col;
  }
  $data = preg_replace($strip, '', strtoupper($data));
  $len = min(strlen($data), $maxlen);
  $hcfa_data .= substr($data, 0, $len);
  $hcfa_curr_col += $len;
}

function gen_hcfa_1500($pid, $encounter, &$log) {
  global $hcfa_data, $hcfa_proc_index;

  $hcfa_data = '';
  $hcfa_proc_index = 0;

  $today = time();
  $claim = new Claim($pid, $encounter);

  $log .= "Generating HCFA claim $pid-$encounter for " .
    $claim->patientFirstName()  . ' ' .
    $claim->patientMiddleName() . ' ' .
    $claim->patientLastName()   . ' on ' .
    date('Y-m-d H:i', $today) . ".\n";

  while ($hcfa_proc_index < $claim->procCount()) {
    if ($hcfa_proc_index) $hcfa_data .= "\014"; // append form feed for new page
    gen_hcfa_1500_page($pid, $encounter, $log, $claim);
  }

  $log .= "\n";
  return $hcfa_data;
}

function gen_hcfa_1500_page($pid, $encounter, &$log, &$claim) {
  global $hcfa_curr_line, $hcfa_curr_col, $hcfa_data, $hcfa_proc_index;

  $hcfa_curr_line = 1;
  $hcfa_curr_col = 1;

  // According to:
  // http://www.ngsmedicare.com/NGSMedicare/PartB/EducationandSupport/ToolsandMaterials/CMS_ClaimFormInst.aspx
  // Medicare interprets sections 9 and 11 of the claim form in its own
  // special way.  This flag tells us to do that.  However I'm not 100%
  // sure that it applies nationwide, and if you find that it is not right
  // for you then set it to false.  -- Rod 2009-03-26
  $new_medicare_logic = $claim->claimType() == 'MB';

  // Payer name, attn, street.
  put_hcfa(2, 41, 31, $claim->payerName());
  put_hcfa(3, 41, 31, $claim->payerAttn());
  put_hcfa(4, 41, 31, $claim->payerStreet());

  // Payer city, state, zip.
  $tmp = $claim->payerCity() ? ($claim->payerCity() . ', ') : '';
  put_hcfa(5, 41, 31, $tmp . $claim->payerState() . ' ' . $claim->payerZip());

  // Box 1. Insurance Type
  // claimTypeRaw() gets the integer value from insurance_companies.ins_type_code.
  // Previous version of this code called claimType() which maps ins_type_code to
  // a 2-character code and that was not specific enough.
  $ct = $claim->claimTypeRaw();
  $tmpcol = 45;                    // Other
  if      ($ct == 2) $tmpcol =  1; // Medicare
  else if ($ct == 3) $tmpcol =  8; // Medicaid
  else if ($ct == 5) $tmpcol = 15; // TriCare (formerly CHAMPUS)
  else if ($ct == 4) $tmpcol = 24; // Champus VA
  else if ($ct == 6) $tmpcol = 31; // Group Health Plan (only BCBS?)
  else if ($ct == 7) $tmpcol = 39; // FECA
  put_hcfa(8, $tmpcol, 1, 'X');

  // Box 1a. Insured's ID Number
  put_hcfa(8, 50, 17, $claim->policyNumber());

  // Box 2. Patient's Name
  $tmp = $claim->patientLastName() . ', ' . $claim->patientFirstName();
  if ($claim->patientMiddleName())
    $tmp .= ', ' . substr($claim->patientMiddleName(),0,1);
  put_hcfa(10, 1, 28, $tmp);

  // Box 3. Patient's Birth Date and Sex
  $tmp = $claim->patientDOB();
  put_hcfa(10, 31, 2, substr($tmp,4,2));
  put_hcfa(10, 34, 2, substr($tmp,6,2));
  put_hcfa(10, 37, 4, substr($tmp,0,4));
  put_hcfa(10, $claim->patientSex() == 'M' ? 42 : 47, 1, 'X');

  // Box 4. Insured's Name
  $tmp = $claim->insuredLastName() . ', ' . $claim->insuredFirstName();
  if ($claim->insuredMiddleName())
    $tmp .= ', ' . substr($claim->insuredMiddleName(),0,1);
  put_hcfa(10, 50, 28, $tmp);

  // Box 5. Patient's Address
  put_hcfa(12, 1, 28, $claim->patientStreet());

  // Box 6. Patient Relationship to Insured
  $tmp = $claim->insuredRelationship();
  $tmpcol = 47;                         // Other
  if      ($tmp === '18') $tmpcol = 33; // self
  else if ($tmp === '01') $tmpcol = 38; // spouse
  else if ($tmp === '19') $tmpcol = 42; // child
  put_hcfa(12, $tmpcol, 1, 'X');

  // Box 7. Insured's Address
  put_hcfa(12, 50, 28, $claim->insuredStreet());

  // Box 5 continued. Patient's City and State
  put_hcfa(14,  1, 20, $claim->patientCity());
  put_hcfa(14, 26,  2, $claim->patientState());

  // Box 8. Patient (Marital) Status
  if(!hcfa_1500_version_02_12())  // Box 8 Reserved for NUCC Use in 02/12
  {
    $tmp = $claim->patientStatus();
    $tmpcol = 47;                        // Other
    if      ($tmp === 'S') $tmpcol = 35; // Single
    else if ($tmp === 'M') $tmpcol = 41; // Married
    put_hcfa(14, $tmpcol, 1, 'X');
  }

  // Box 7 continued. Insured's City and State
  put_hcfa(14, 50, 20, $claim->insuredCity());
  put_hcfa(14, 74,  2, $claim->insuredState());

  // Box 5 continued. Patient's Zip Code and Telephone
  put_hcfa(16,  1, 10, $claim->patientZip());
  $tmp = $claim->patientPhone();
  put_hcfa(16, 15,  3, substr($tmp,0,3));
  put_hcfa(16, 19,  7, substr($tmp,3));

  // Box 8 continued. Patient (Employment) Status
  if(!hcfa_1500_version_02_12())  // Box 8 Reserved for NUCC Use in 02/12
  {
    $tmp = $claim->patientOccupation();
    if      ($tmp === 'STUDENT'   ) put_hcfa(16, 41, 1, 'X');
    else if ($tmp === 'PT STUDENT') put_hcfa(16, 47, 1, 'X');
    else if ($tmp !== 'UNEMPLOYED') put_hcfa(16, 35, 1, 'X');
  }

  // Box 7 continued. Insured's Zip Code and Telephone
  put_hcfa(16, 50, 10, $claim->insuredZip());
  $tmp = $claim->insuredPhone();
  put_hcfa(16, 65,  3, substr($tmp,0,3));
  put_hcfa(16, 69,  7, substr($tmp,3));

  // Box 9. Other Insured's Name
  if ($new_medicare_logic) {
    // TBD: Medigap stuff? How do we know if this is a Medigap transfer?
  }
  else {
    if ($claim->payerCount() > 1) {
      $tmp = $claim->insuredLastName(1) . ', ' . $claim->insuredFirstName(1);
      if ($claim->insuredMiddleName(1))
        $tmp .= ', ' . substr($claim->insuredMiddleName(1),0,1);
      put_hcfa(18, 1, 28, $tmp);
    }
  }

  // Box 11. Insured's Group Number
  if ($new_medicare_logic) {
    // If this is Medicare secondary then we need the primary's policy number
    // here, otherwise the word "NONE".
    $tmp = $claim->payerSequence() == 'P' ? 'NONE' : $claim->policyNumber(1);
  }
  else {
    $tmp = $claim->groupNumber();
  }
  put_hcfa(18, 50, 30, $tmp);

  // Box 9a. Other Insured's Policy or Group Number
  if ($new_medicare_logic) {
    // TBD: Medigap stuff?
  }
  else {
    if ($claim->payerCount() > 1) {
      put_hcfa(20, 1, 28, $claim->policyNumber(1));
    }
  }

  // Box 10a. Employment Related
  put_hcfa(20, $claim->isRelatedEmployment() ? 35 : 41, 1, 'X');

  // Box 11a. Insured's Birth Date and Sex
  if ($new_medicare_logic) {
    $tmpdob = $tmpsex = '';
    if ($claim->payerSequence() != 'P') {
      $tmpdob = $claim->insuredDOB(1);
      $tmpsex = $claim->insuredSex(1);
    }
   }
  else {
    $tmpdob = $claim->insuredDOB();
    $tmpsex = $claim->insuredSex();
  }
  if ($tmpdob) {
    put_hcfa(20, 53, 2, substr($tmpdob,4,2));
    put_hcfa(20, 56, 2, substr($tmpdob,6,2));
    put_hcfa(20, 59, 4, substr($tmpdob,0,4));
  }
  if ($tmpsex) {
    put_hcfa(20, $tmpsex == 'M' ? 68 : 75, 1, 'X');
  }

  // Box 9b. Other Insured's Birth Date and Sex
  if(!hcfa_1500_version_02_12())  // Box 9b Reserved for NUCC Use in 02/12
  {
    if ($new_medicare_logic) {
      // TBD: Medigap stuff?
    }
    else {
      if ($claim->payerCount() > 1) {
        $tmp = $claim->insuredDOB(1);
        put_hcfa(22, 2, 2, substr($tmp,4,2));
        put_hcfa(22, 5, 2, substr($tmp,6,2));
        put_hcfa(22, 8, 4, substr($tmp,0,4));
        put_hcfa(22, $claim->insuredSex(1) == 'M' ? 18 : 24, 1, 'X');
      }
    }
  }

  // Box 10b. Auto Accident
  put_hcfa(22, $claim->isRelatedAuto() ? 35 : 41, 1, 'X');
  if ($claim->isRelatedAuto())
    put_hcfa(22, 45, 2, $claim->autoAccidentState());

  // Box 11b. Insured's Employer/School Name
  if ($new_medicare_logic) {
    $tmp = $claim->payerSequence() == 'P' ? '' : $claim->groupName(1);
  }
  else {
    $tmp = $claim->groupName();
  }
  put_hcfa(22, 50, 30, $tmp);

  // Box 9c. Other Insured's Employer/School Name
  if(!hcfa_1500_version_02_12())  // Box 9c Reserved for NUCC Use in 02/12
  {
    if ($new_medicare_logic) {
      // TBD: Medigap stuff?
    }
    else {
      if ($claim->payerCount() > 1) {
        put_hcfa(24, 1, 28, $claim->groupName(1));
      }
    }
  }

  // Box 10c. Other Accident
  put_hcfa(24, $claim->isRelatedOther() ? 35 : 41, 1, 'X');

  // Box 11c. Insurance Plan Name or Program Name
  if ($new_medicare_logic) {
    $tmp = '';
    if ($claim->payerSequence() != 'P') {
      $tmp = $claim->planName(1);
      if (!$tmp) $tmp = $claim->payerName(1);
    }
  }
  else {
    $tmp = $claim->planName();
  }
  put_hcfa(24, 50, 30, $tmp);

  // Box 9d. Other Insurance Plan Name or Program Name
  if ($new_medicare_logic) {
    // TBD: Medigap stuff?
  }
  else {
    if ($claim->payerCount() > 1) {
      put_hcfa(26, 1, 28, $claim->planName(1));
    }
  }
  
  # Box 10d. Claim Codes  medicaid_referral_code
  
  if($claim->epsdtFlag()) {
      put_hcfa(26, 34, 2, $claim->medicaidReferralCode());
    }

  # Box 10d. Claim Codes  medicaid_referral_code

  if($claim->epsdtFlag()) {
      put_hcfa(26, 34, 2, $claim->medicaidReferralCode());
    }

  // Box 11d. Is There Another Health Benefit Plan
  if (!$new_medicare_logic) {
    put_hcfa(26, $claim->payerCount() > 1 ? 52 : 57, 1, 'X');
  }

  // Box 12. Patient's or Authorized Person's Signature
  put_hcfa(29, 7, 17, 'Signature on File');
  // Note: Date does not apply unless the person physically signs the form.

  // Box 13. Insured's or Authorized Person's Signature
  put_hcfa(29, 55, 17, 'Signature on File');

  // Box 14. Date of Current Illness/Injury/Pregnancy
  $tmp = $claim->onsetDate();
  put_hcfa(32, 2, 2, substr($tmp,4,2));
  put_hcfa(32, 5, 2, substr($tmp,6,2));
  put_hcfa(32, 8, 4, substr($tmp,0,4));

  if(hcfa_1500_version_02_12() && !empty($tmp))
  {
    // Only include the Box 14 qualifier if there we are using version 02/12 and there is a Box 14 date.
    put_hcfa(32, 16, 3, $claim->box14qualifier());

  }
  // Box 15. First Date of Same or Similar Illness, if applicable
  $tmp = $claim->dateInitialTreatment();
  if(hcfa_1500_version_02_12() && !empty($tmp))
  {
    // Only include the Box 15 qualifier if there we are using version 02/12 and there is a Box 15 date.
    put_hcfa(32, 31, 3, $claim->box15qualifier());
  }


  put_hcfa(32,37, 2, substr($tmp,4,2));
  put_hcfa(32,40, 2, substr($tmp,6,2));
  put_hcfa(32,43, 4, substr($tmp,0,4));


  // Box 16. Dates Patient Unable to Work in Current Occupation
  if ($claim->isUnableToWork()) {
    $tmp = $claim->offWorkFrom();
    put_hcfa(32, 54, 2, substr($tmp,4,2));
    put_hcfa(32, 57, 2, substr($tmp,6,2));
    put_hcfa(32, 60, 4, substr($tmp,0,4));
    $tmp = $claim->offWorkTo();
    put_hcfa(32, 68, 2, substr($tmp,4,2));
    put_hcfa(32, 71, 2, substr($tmp,6,2));
    put_hcfa(32, 74, 4, substr($tmp,0,4));
  }

  // Referring provider stuff.  Reports are that for primary care doctors,
  // Medicare forbids an entry here and other payers require one.
  // There is still confusion over this.
  //
  if ($claim->referrerLastName() || $claim->billingProviderLastName() &&
    (empty($GLOBALS['MedicareReferrerIsRenderer']) || $claim->claimType() != 'MB'))
  {
    // Box 17a. Referring Provider Alternate Identifier
    // Commented this out because UPINs are obsolete, leaving the code as an
    // example in case some other identifier needs to be supported.
    /*****************************************************************
    if ($claim->referrerUPIN() && $claim->claimType() != 'MB') {
      put_hcfa(33, 30,  2, '1G');
      put_hcfa(33, 33, 15, $claim->referrerUPIN());
    }
    *****************************************************************/
  if ($claim->claimType() == 'MC') {
    put_hcfa(33, 30,  2, 'ZZ');
    put_hcfa(33, 33, 14, $claim->referrerTaxonomy());
  }



    // Box 17. Name of Referring Provider or Other Source leave it like it is just check if there is info in misc_billing the use it provider_qualifier_code
    # Changed to look first at the misc hcfa billing form to complete this box if nothing on misc hcfa form use referrer
    if (strlen($claim->billingProviderLastName()) !=0) {
      $tmp2 = $claim->billingProviderLastName() . ', ' . $claim->billingProviderFirstName();
      if ($claim->billingProviderMiddleName())
        $tmp2 .= ', ' . substr($claim->billingProviderMiddleName(),0,1);
      put_hcfa(34, 1, 3, $claim->billing_options['provider_qualifier_code']);
      put_hcfa(34, 4, 25, $tmp2);
      if ($claim->billingProviderNPI()) {
        put_hcfa(34, 33, 15, $claim->billingProviderNPI());
      }
    }
    else
    {
    $tmp = $claim->referrerLastName() . ', ' . $claim->referrerFirstName();
    if ($claim->referrerMiddleName())
      $tmp .= ', ' . substr($claim->referrerMiddleName(),0,1);
    put_hcfa(34, 1, 3, 'DN');
    put_hcfa(34, 4, 25, $tmp);
    if ($claim->referrerNPI()) {
      put_hcfa(34, 33, 15, $claim->referrerNPI());
    }
  }
  }

  // Box 18. Hospitalization Dates Related to Current Services
  if ($claim->isHospitalized()) {
    $tmp = $claim->hospitalizedFrom();
    put_hcfa(34, 54, 2, substr($tmp,4,2));
    put_hcfa(34, 57, 2, substr($tmp,6,2));
    put_hcfa(34, 60, 4, substr($tmp,0,4));
    $tmp = $claim->hospitalizedTo();
    put_hcfa(34, 68, 2, substr($tmp,4,2));
    put_hcfa(34, 71, 2, substr($tmp,6,2));
    put_hcfa(34, 74, 4, substr($tmp,0,4));
  }

  // Box 19. Reserved for Local Use
  put_hcfa(36, 1, 48, $claim->additionalNotes());

  // Box 20. Outside Lab
  put_hcfa(36, $claim->isOutsideLab() ? 52 : 57, 1, 'X');
  if ($claim->isOutsideLab()) {
    // Note here that put_hcfa strips the decimal point, as required.
    // We right-justify this amount (ending in col. 69).
    put_hcfa(36, 63, 8, sprintf('%8s', $claim->outsideLabAmount()));
  }

  if(hcfa_1500_version_02_12())
  {
      process_diagnoses_02_12($claim,$log);
  }
  else
  {
        // Box 21. Diagnoses
        $tmp = $claim->diagArray();
        $diags = array();
        foreach ($tmp as $diag) $diags[] = $diag;
        if (!empty($diags[0])) {
          put_hcfa(38, 3, 3, substr($diags[0], 0, 3));
          put_hcfa(38, 7, 2, substr($diags[0], 3));
        }
        if (!empty($diags[2])) {
          put_hcfa(38, 30, 3, substr($diags[2], 0, 3));
          put_hcfa(38, 34, 2, substr($diags[2], 3));
        }

        // Box 22. Medicaid Resubmission Code and Original Ref. No.
        put_hcfa(38, 50, 10, $claim->medicaidResubmissionCode());
        put_hcfa(38, 62, 15, $claim->medicaidOriginalReference());

        // Box 21 continued. Diagnoses
        if (!empty($diags[1])) {
          put_hcfa(40, 3, 3, substr($diags[1], 0, 3));
          put_hcfa(40, 7, 2, substr($diags[1], 3));
        }
        if (!empty($diags[3])) {
          put_hcfa(40, 30, 3, substr($diags[3], 0, 3));
          put_hcfa(40, 34, 2, substr($diags[3], 3));
        }

        // Box 23. Prior Authorization Number
        put_hcfa(40, 50, 28, $claim->priorAuth());
  }
  $proccount = $claim->procCount(); // number of procedures

  // Charges, adjustments and payments are accumulated by line item so that
  // each page of a multi-page claim will stand alone.  Payments include the
  // co-pay for the first page only.
  $clm_total_charges = 0;
  $clm_amount_adjusted = 0;
  $clm_amount_paid = $hcfa_proc_index ? 0 : $claim->patientPaidAmount();

  // Procedure loop starts here.
  //
  for ($svccount = 0; $svccount < 6 && $hcfa_proc_index < $proccount; ++$hcfa_proc_index) {
    $dia = $claim->diagIndexArray($hcfa_proc_index);

    if (!$claim->cptCharges($hcfa_proc_index)) {
      $log .= "*** Procedure '" . $claim->cptKey($hcfa_proc_index) .
        "' has no charges!\n";
    }

    if (empty($dia)) {
      $log .= "*** Procedure '" . $claim->cptKey($hcfa_proc_index) .
        "' is not justified!\n";
    }

    $clm_total_charges += $claim->cptCharges($hcfa_proc_index);

    // Compute prior payments and "hard" adjustments.
    for ($ins = 1; $ins < $claim->payerCount(); ++$ins) {
      if ($claim->payerSequence($ins) > $claim->payerSequence())
        continue; // skip future payers
      $payerpaid = $claim->payerTotals($ins, $claim->cptKey($hcfa_proc_index));
      $clm_amount_paid += $payerpaid[1];
      $clm_amount_adjusted += $payerpaid[2];
    }

    ++$svccount;
    $lino = $svccount * 2 + 41;

    // Drug Information. Medicaid insurers want this with HCPCS codes.
    //
    $ndc = $claim->cptNDCID($hcfa_proc_index);
    if ($ndc) {
      if (preg_match('/^(\d\d\d\d\d)-(\d\d\d\d)-(\d\d)$/', $ndc, $tmp)) {
        $ndc = $tmp[1] . $tmp[2] . $tmp[3];
      }
      else if(preg_match('/^\d{11}$/', $ndc)){

      }
      else {
        $log .= "*** NDC code '$ndc' has invalid format!\n";
      }
      put_hcfa($lino, 1, 50, "N4$ndc   " . $claim->cptNDCUOM($hcfa_proc_index) .
        $claim->cptNDCQuantity($hcfa_proc_index));
    }

    //Note Codes.
    put_hcfa($lino, 25, 7, $claim->cptNotecodes($hcfa_proc_index));

    // 24i and 24j Top. ID Qualifier and Rendering Provider ID
    if ($claim->supervisorNumber()) {
      // If there is a supervising provider and that person has a
      // payer-specific provider number, then we assume that the SP
      // must be identified on the claim and this is how we do it
      // (but the NPI of the actual rendering provider appears below).
      // BCBS of TN indicated they want it this way.  YMMV.  -- Rod
      put_hcfa($lino, 65,  2, $claim->supervisorNumberType());
      put_hcfa($lino, 68, 10, $claim->supervisorNumber());
    }
    else if ($claim->providerNumber($hcfa_proc_index)) {
      put_hcfa($lino, 65,  2, $claim->providerNumberType($hcfa_proc_index));
      put_hcfa($lino, 68, 10, $claim->providerNumber($hcfa_proc_index));
    }
    else if ($claim->claimType() == 'MC') {
     put_hcfa($lino, 65,  2, 'ZZ');
     put_hcfa($lino, 68, 14, $claim->providerTaxonomy());
    }

    ++$lino;

    // 24a. Date of Service
    $tmp = $claim->serviceDate();
    put_hcfa($lino, 1, 2, substr($tmp,4,2));
    put_hcfa($lino, 4, 2, substr($tmp,6,2));
    put_hcfa($lino, 7, 2, substr($tmp,2,2));
    put_hcfa($lino,10, 2, substr($tmp,4,2));
    put_hcfa($lino,13, 2, substr($tmp,6,2));
    put_hcfa($lino,16, 2, substr($tmp,2,2));

    // 24b. Place of Service
    put_hcfa($lino, 19, 2, $claim->facilityPOS());

    // 24c. EMG
    // Not currently supported.

    // 24d. Procedures, Services or Supplies
    put_hcfa($lino, 25, 7, $claim->cptCode($hcfa_proc_index));
    // replace colon with space for printing
    put_hcfa($lino, 33, 12, str_replace(':', ' ', $claim->cptModifier($hcfa_proc_index)));

    // 24e. Diagnosis Pointer
    $tmp = '';
    foreach ($claim->diagIndexArray($hcfa_proc_index) as $value)
    {
        if(hcfa_1500_version_02_12())// For 02/12 Version convert number to letter.
        {
            // ASCII A is 65, since diagIndexArray is ones based, this will make 1->A, 2->B...
            $value=chr($value+64);
        }
        $tmp .= $value;
    }
    put_hcfa($lino, 45, 4, $tmp);

    // 24f. Charges
    put_hcfa($lino, 50, 8, str_replace('.', ' ',
      sprintf('%8.2f', $claim->cptCharges($hcfa_proc_index))));

    // 24g. Days or Units
    put_hcfa($lino, 59, 3, $claim->cptUnits($hcfa_proc_index));

    // 24h. EPSDT Family Plan
    //
    if($claim->epsdtFlag()) {
      put_hcfa($lino, 63, 2, '03');
    }

    // 24j. Rendering Provider NPI
    put_hcfa($lino, 68, 10, $claim->providerNPI($hcfa_proc_index));
  }

  // 25. Federal Tax ID Number
  // FrreB hard coded EIN. Changed it to included SSN as well.
  put_hcfa(56, 1, 15, $claim->billingFacilityETIN());
  if($claim->federalIdType()=='SY'){
  put_hcfa(56, 17, 1, 'X'); // The SSN checkbox
  }
  else{
  put_hcfa(56, 19, 1, 'X'); // The EIN checkbox
  }

  // 26. Patient's Account No.
  // Instructions say hyphens are not allowed.
  put_hcfa(56, 23, 15, "$pid-$encounter");

  // 27. Accept Assignment
  put_hcfa(56, $claim->billingFacilityAssignment() ? 38 : 43, 1, 'X');

  // 28. Total Charge
  put_hcfa(56, 52, 8, str_replace('.',' ',sprintf('%8.2f',$clm_total_charges)));
  if (!$clm_total_charges) {
    $log .= "*** This claim has no charges!\n";
  }

  // 29. Amount Paid
  put_hcfa(56, 62, 8, str_replace('.',' ',sprintf('%8.2f',$clm_amount_paid)));

  // 30. Balance Due
  // For secondary payers this reflects primary "contracted rate" adjustments,
  // so in general box 30 will not equal box 28 minus box 29.
  if(!hcfa_1500_version_02_12())  // Box 30 Reserved for NUCC Use in 02/12
  {
      put_hcfa(56, 71, 8, str_replace('.',' ',sprintf('%8.2f',
        $clm_total_charges - $clm_amount_paid - $clm_amount_adjusted)));
  }

  // 33. Billing Provider: Phone Number
  $tmp = $claim->billingContactPhone();
  put_hcfa(57, 66,  3, substr($tmp,0,3));
  put_hcfa(57, 70, 3, substr($tmp,3)); // slight adjustment for better look smw 030315
  put_hcfa(57, 73, 1, '-');
  put_hcfa(57, 74, 4, substr($tmp,6));

  // 32. Service Facility Location Information: Name
  put_hcfa(58, 23, 25, $claim->facilityName());

  // 33. Billing Provider: Name
  if($claim->federalIdType()=='SY'){
    $tempName = $claim->billingFacilityName();
    $partsName = explode(' ', $tempName);// entity == person
    $num_parts = count($partsName);
    switch ($num_parts) {
      case "2":
        $firstName = $partsName[0];
        $lastName = $partsName[1];
        $billingProviderName = $lastName . ", " . $firstName;
        break;
      case "3":
        $firstName = $partsName[0];
        $middleName = $partsName[1];
        $lastName = $partsName[2];
        $billingProviderName = $lastName . ", " . $firstName. ", " . $middleName;
        break;
      case "4":
        $firstName = $partsName[0];
        $middleName = $partsName[1];
        $lastName = $partsName[2];
        $suffixName = $partsName[3];
        $billingProviderName = $lastName . ", " . $firstName. ", " . $middleName. ", " . $suffixName;
        break;
      default:
        $log .= "*** individual name has more than 4 parts and may not be desirable on the claim form\n";
        $firstName = $partsName[0];
        $middleName = $partsName[1];
        $lastName = $partsName[2];
        $suffixName = $partsName[3];
        $billingProviderName = $lastName . ", " . $firstName. ", " . $middleName. ", " . $suffixName;
    }
    put_hcfa(58, 50, 25, $billingProviderName);
  }
  else {
  put_hcfa(58, 50, 25, $claim->billingFacilityName());
  }
  // 32. Service Facility Location Information: Street
  put_hcfa(59, 23, 25, $claim->facilityStreet());

  // 33. Billing Provider: Name
  put_hcfa(59, 50, 25, $claim->billingFacilityStreet());

  // 31. Signature of Physician or Supplier

   if($GLOBALS['cms_1500_box_31_format']==0)
   {
      put_hcfa(60, 1, 20, 'Signature on File');
   }
   else if($GLOBALS['cms_1500_box_31_format']==1)
   {
      put_hcfa(60, 1, 22, $claim->providerFirstName()." ".$claim->providerLastName());
   }
  //
  // $tmp = $claim->providerFirstName();
  // if ($claim->providerMiddleName()) $tmp .= ' ' . substr($claim->providerMiddleName(),0,1);
  // put_hcfa(60, 1, 20, $tmp . ' ' . $claim->providerLastName());

  // 32. Service Facility Location Information: City State Zip
  $tmp = $claim->facilityCity() ? ($claim->facilityCity() . ' ') : '';
  put_hcfa(60, 23, 27, $tmp . $claim->facilityState() . ' ' .
    $claim->facilityZip());

  // 33. Billing Provider: City State Zip
  $tmp = $claim->billingFacilityCity() ? ($claim->billingFacilityCity() . ' ') : '';
  put_hcfa(60, 50, 27, $tmp . $claim->billingFacilityState() . ' ' .
    $claim->billingFacilityZip());

  // 31. Signature of Physician or Supplier: Date
   if($GLOBALS['cms_1500_box_31_date']>0)
   {
       if($GLOBALS['cms_1500_box_31_date']==1)
       {
            $date_of_service= $claim->serviceDate();
            $MDY=substr($date_of_service,4,2)." ".substr($date_of_service,6,2)." ".substr($date_of_service,2,2);
       }
       else if($GLOBALS['cms_1500_box_31_date']==2)
       {
           $MDY=date("m/d/y");
       }
       put_hcfa(61,6,10,$MDY);
   }

  // 32a. Service Facility NPI
  put_hcfa(61, 23, 10, $claim->facilityNPI());

  // 32b. Service Facility Other ID
  // Note that Medicare does NOT want this any more.
  if ($claim->providerGroupNumber()) {
    put_hcfa(61, 36,  2, $claim->providerNumberType());
    put_hcfa(61, 38, 11, $claim->providerGroupNumber());
  }

  // 33a. Billing Facility NPI
  put_hcfa(61, 50, 10, $claim->billingFacilityNPI());

  // 33b. Billing Facility Other ID
  // Note that Medicare does NOT want this any more.
  if ($claim->claimType() == 'MC') {
    put_hcfa(61, 63,  2, 'ZZ');
    put_hcfa(61, 65, 14, $claim->providerTaxonomy());
  }
  if ($claim->providerGroupNumber() && $claim->claimType() != 'MB') {
    put_hcfa(61, 63,  2, $claim->providerNumberType());
    put_hcfa(61, 65, 14, $claim->providerGroupNumber());
  }

  // Put an extra line here for compatibility with old hcfa text generated form
    put_hcfa(62, 1, 1, ' ');
  // put a couple more in so that multiple claims correctly print through the text file download
    put_hcfa(63, 1, 1, ' ');
    put_hcfa(64, 1, 1, ' ');
  return;
}
?>
