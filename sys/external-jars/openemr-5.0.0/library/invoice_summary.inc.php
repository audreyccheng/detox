<?php
// Copyright (C) 2005-2010 Rod Roark <rod@sunsetsystems.com>
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This returns an associative array keyed on procedure code, representing
// all charge items for one invoice.  This array's values are themselves
// associative arrays having the following keys:
//
//  chg - the sum of line items, including adjustments, for the code
//  bal - the unpaid balance
//  adj - the (positive) sum of inverted adjustments
//  ins - the id of the insurance company that was billed (obsolete)
//  dtl - associative array of details, if requested
//
// Where details are requested, each dtl array is keyed on a string
// beginning with a date in yyyy-mm-dd format, or blanks in the case
// of the original charge items.  The value array is:
//
//  pmt - payment amount as a positive number, only for payments
//  src - check number or other source, only for payments
//  chg - invoice line item amount amount, only for charges or
//        adjustments (adjustments may be zero)
//  rsn - adjustment reason, only for adjustments
//  plv - provided for "integrated A/R" only: 0=pt, 1=Ins1, etc.
//  dsc - for tax charges, a description of the tax
//  arseq - ar_activity.sequence_no when it applies.

require_once("sl_eob.inc.php");
require_once(dirname(__FILE__) . "/../custom/code_types.inc.php");


// for Integrated A/R.
//
function ar_get_invoice_summary($patient_id, $encounter_id, $with_detail = false) {
  $codes = array();
  $keysuff1 = 1000;
  $keysuff2 = 5000;

  // Get charges from services.
  $res = sqlStatement("SELECT " .
    "date, code_type, code, modifier, code_text, fee " .
    "FROM billing WHERE " .
    "pid = ? AND encounter = ? AND " .
    "activity = 1 AND fee != 0.00 ORDER BY id", array($patient_id,$encounter_id) );

  while ($row = sqlFetchArray($res)) {
    $amount = sprintf('%01.2f', $row['fee']);

      $code = $row['code'];
      if (! $code) $code = "Unknown";
      if ($row['modifier']) $code .= ':' . $row['modifier'];
      $codes[$code]['chg'] += $amount;
      $codes[$code]['bal'] += $amount;

    // Pass the code type, code and code_text fields
    // Although not all used yet, useful information
    // to improve the statement reporting etc.
    $codes[$code]['code_type'] = $row['code_type'];
    $codes[$code]['code_value'] = $row['code'];
    $codes[$code]['modifier'] = $row['modifier'];
    $codes[$code]['code_text'] = $row['code_text'];

    // Add the details if they want 'em.
    if ($with_detail) {
      if (! $codes[$code]['dtl']) $codes[$code]['dtl'] = array();
      $tmp = array();
      $tmp['chg'] = $amount;
      $tmpkey = "          " . $keysuff1++;
      $codes[$code]['dtl'][$tmpkey] = $tmp;
    }
  }

  // Get charges from product sales.
  $query = "SELECT s.drug_id, s.sale_date, s.fee, s.quantity " .
    "FROM drug_sales AS s " .
    "WHERE " .
    "s.pid = ? AND s.encounter = ? AND s.fee != 0 " .
    "ORDER BY s.sale_id";
  $res = sqlStatement($query, array($patient_id,$encounter_id) );
  while ($row = sqlFetchArray($res)) {
    $amount = sprintf('%01.2f', $row['fee']);
    $code = 'PROD:' . $row['drug_id'];
    $codes[$code]['chg'] += $amount;
    $codes[$code]['bal'] += $amount;
    // Add the details if they want 'em.
    if ($with_detail) {
      if (! $codes[$code]['dtl']) $codes[$code]['dtl'] = array();
      $tmp = array();
      $tmp['chg'] = $amount;
      $tmpkey = "          " . $keysuff1++;
      $codes[$code]['dtl'][$tmpkey] = $tmp;
    }
  }

  // Get payments and adjustments. (includes copays)
  $res = sqlStatement("SELECT " .
    "a.code_type, a.code, a.modifier, a.memo, a.payer_type, a.adj_amount, a.pay_amount, a.reason_code, " .
    "a.post_time, a.session_id, a.sequence_no, a.account_code, " .
    "s.payer_id, s.reference, s.check_date, s.deposit_date " .
    ",i.name " .
    "FROM ar_activity AS a " .
    "LEFT OUTER JOIN ar_session AS s ON s.session_id = a.session_id " .
    "LEFT OUTER JOIN insurance_companies AS i ON i.id = s.payer_id " .
    "WHERE a.pid = ? AND a.encounter = ? " .
    "ORDER BY s.check_date, a.sequence_no", array($patient_id,$encounter_id) );
  while ($row = sqlFetchArray($res)) {
    $code = $row['code'];
    if (! $code) $code = "Unknown";
    if ($row['modifier']) $code .= ':' . $row['modifier'];
    $ins_id = 0 + $row['payer_id'];
    $codes[$code]['bal'] -= $row['pay_amount'];
    $codes[$code]['bal'] -= $row['adj_amount'];
    $codes[$code]['chg'] -= $row['adj_amount'];
    $codes[$code]['adj'] += $row['adj_amount'];
    if ($ins_id) $codes[$code]['ins'] = $ins_id;
    // Add the details if they want 'em.
    if ($with_detail) {
      if (! $codes[$code]['dtl']) $codes[$code]['dtl'] = array();
      $tmp = array();
      $paydate = empty($row['deposit_date']) ? substr($row['post_time'], 0, 10) : $row['deposit_date'];
      if ($row['pay_amount'] != 0) $tmp['pmt'] = $row['pay_amount'];
      if ( isset($row['reason_code'] ) ) {
      	$tmp['msp'] = $row['reason_code'];
      }
      if ($row['adj_amount'] != 0 || $row['pay_amount'] == 0) {
        $tmp['chg'] = 0 - $row['adj_amount'];
        // $tmp['rsn'] = (empty($row['memo']) || empty($row['session_id'])) ? 'Unknown adjustment' : $row['memo'];
        $tmp['rsn'] = empty($row['memo']) ? 'Unknown adjustment' : $row['memo'];
        $tmpkey = $paydate . $keysuff1++;
      }
      else {
        $tmpkey = $paydate . $keysuff2++;
      }
      if ($row['account_code'] == "PCP") {
        //copay
        $tmp['src'] = 'Pt Paid';
      }
      else {
        $tmp['src'] = empty($row['session_id']) ? $row['memo'] : $row['reference'];
      }
      $tmp['insurance_company'] = substr($row['name'], 0, 10);
      if ($ins_id) $tmp['ins'] = $ins_id;
      $tmp['plv'] = $row['payer_type'];
      $tmp['arseq'] = $row['sequence_no'];
      $codes[$code]['dtl'][$tmpkey] = $tmp;
    }
  }
  return $codes;
}

// This determines the party from whom payment is currently expected.
// Returns: -1=Nobody, 0=Patient, 1=Ins1, 2=Ins2, 3=Ins3.
// for Integrated A/R.
//
function ar_responsible_party($patient_id, $encounter_id) {
  $row = sqlQuery("SELECT date, last_level_billed, last_level_closed " .
    "FROM form_encounter WHERE " .
    "pid = ? AND encounter = ? " .
    "ORDER BY id DESC LIMIT 1", array($patient_id,$encounter_id) );
  if (empty($row)) return -1;
  $next_level = $row['last_level_closed'] + 1;
  if ($next_level <= $row['last_level_billed'])
    return $next_level;
  if (arGetPayerID($patient_id, substr($row['date'], 0, 10), $next_level))
    return $next_level;
  // There is no unclosed insurance, so see if there is an unpaid balance.
  // Currently hoping that form_encounter.balance_due can be discarded.
  $balance = 0;
  $codes = ar_get_invoice_summary($patient_id, $encounter_id);
  foreach ($codes as $cdata) $balance += $cdata['bal'];
  if ($balance > 0) return 0;
  return -1;
}
?>
