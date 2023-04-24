<?php
/**
 * This popup is called when choosing a foreign field ID for a form layout.
 *
 * Copyright (C) 2014 Rod Roark <rod@sunsetsystems.com>
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
 * along with this program. If not, see <http://opensource.org/licenses/gpl-license.php>.
 *
 * @package OpenEMR
 * @author  Rod Roark <rod@sunsetsystems.com>
 * @link    http://www.open-emr.org
 */

$sanitize_all_escapes  = true;
$fake_register_globals = false;

include_once("../globals.php");

$form_encounter_layout = array(
  array('field_id'     => 'date',
        'title'        => xl('Visit Date'),
        'uor'          => '1',
        'data_type'    => '4',               // Text-date
        'list_id'      => '',
        'edit_options' => '',
       ),
  array('field_id'     => 'facility_id',
        'title'        => xl('Service Facility'),
        'uor'          => '1',
        'data_type'    => '35',              // Facilities
        'list_id'      => '',
        'edit_options' => '',
       ),
  array('field_id'     => 'pc_catid',
        'title'        => xl('Visit Category'),
        'uor'          => '1',
        'data_type'    => '18',              // Visit Category
        'list_id'      => '',
        'edit_options' => '',
       ),
  array('field_id'     => 'reason',
        'title'        => xl('Reason for Visit'),
        'uor'          => '1',
        'data_type'    => '2',               // Text
        'list_id'      => '',
        'edit_options' => '',
       ),
  array('field_id'     => 'onset_date',
        'title'        => xl('Date of Onset'),
        'uor'          => '1',
        'data_type'    => '4',               // Text-date
        'list_id'      => '',
        'edit_options' => '',
       ),
  array('field_id'     => 'referral_source',
        'title'        => xl('Referral Source'),
        'uor'          => '1',
        'data_type'    => '1',               // List
        'list_id'      => 'refsource',
        'edit_options' => '',
       ),
  array('field_id'     => 'shift',
        'title'        => xl('Shift'),
        'uor'          => '1',
        'data_type'    => '1',               // List
        'list_id'      => 'shift',
        'edit_options' => '',
       ),
  array('field_id'     => 'billing_facility',
        'title'        => xl('Billing Facility'),
        'uor'          => '1',
        'data_type'    => '35',              // Facilities
        'list_id'      => '',
        'edit_options' => '',
       ),
  array('field_id'     => 'voucher_number',
        'title'        => xl('Voucher Number'),
        'uor'          => '1',
        'data_type'    => '2',               // Text
        'list_id'      => '',
        'edit_options' => '',
       ),
);

$source = empty($_REQUEST['source']) ? 'D' : $_REQUEST['source'];

function gsr_fixup(&$row, $fldid, $default='') {
  if (isset($row[$fldid])) {
    return addslashes($row[$fldid]);
  }
  return $default;
}

function gen_sel_row($row) {
  echo " <tr class='oneresult' onclick='selectField(";
  echo '"' . gsr_fixup($row, 'field_id'      ) . '",';
  echo '"' . gsr_fixup($row, 'title'         ) . '",';
  echo '"' . gsr_fixup($row, 'data_type'     ) . '",';
  echo '"' . gsr_fixup($row, 'uor'           ) . '",';
  echo '"' . gsr_fixup($row, 'fld_length', 20) . '",';
  echo '"' . gsr_fixup($row, 'max_length',  0) . '",';
  echo '"' . gsr_fixup($row, 'list_id'       ) . '",';
  echo '"' . gsr_fixup($row, 'titlecols' ,  1) . '",';
  echo '"' . gsr_fixup($row, 'datacols'  ,  3) . '",';
  echo '"' . gsr_fixup($row, 'edit_options'  ) . '",';
  echo '"' . gsr_fixup($row, 'description'   ) . '",';
  echo '"' . gsr_fixup($row, 'fld_rows'  ,  0) . '"';
  echo ")'>";
  echo "<td>" . text($row['field_id']) . "</td>";
  echo "<td>" . text($row['title'   ]) . "</td>";
  echo "</tr>\n";
}
?>
<html>
<head>
<?php html_header_show(); ?>
<title><?php echo xlt('List layout items'); ?></title>
<link rel="stylesheet" href='<?php echo $css_header ?>' type='text/css'>

<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-2-2/index.js"></script>

<script language="javascript">

function selectField(field_id, title, data_type, uor, fld_length, max_length,
  list_id, titlecols, datacols, edit_options, description, fld_rows)
{
  if (opener.closed || ! opener.SetField) {
    alert('<?php echo xls('The destination form was closed; I cannot act on your selection.'); ?>');
  }
  else {
    opener.SetField(field_id, title, data_type, uor, fld_length, max_length,
      list_id, titlecols, datacols, edit_options, description, fld_rows);
  }
  window.close();
  return false;
}

function newField() {
  return selectField(document.forms[0].new_field_id.value, '', 2, 1, 10, 255,
    '', 1, 3, '', '', 0);
}

$(document).ready(function(){

  $('.oneresult').mouseover(function() { $(this).toggleClass('highlight'); });
  $('.oneresult').mouseout(function()  { $(this).toggleClass('highlight'); });

  // $('.oneresult').click(function()     { SelectField(this); });
  // var SelectField = function(obj) {
  //   return setAFieldID($(obj).attr('id'));
  // };

});

</script>

<style>
h1 {
    font-size: 120%;
    padding: 3px;
    margin: 3px;
}
ul {
    list-style: none;
    padding: 3px;
    margin: 3px;
}
li {
    cursor: pointer;
    border-bottom: 1px solid #ccc;
    background-color: white;
}
.highlight {
    background-color: #336699;
    color: white;
}    
</style>

</head>

<body class="body_top text">
<div id="lists">

<h1>
<?php
// F should never happen, but just in case.
if ($source == 'F') echo xlt('Fields in This Form' ); else
if ($source == 'D') echo xlt('Demographics Fields' ); else
if ($source == 'H') echo xlt('History Fields'      ); else
if ($source == 'E') echo xlt('Visit Attributes'    ); else
if ($source == 'V') echo xlt('Visit Form Attributes');
?>
</h1>

<?php
echo "<table>\n";
if ($source == 'V') {
  foreach ($form_encounter_layout as $lrow) {
    gen_sel_row($lrow);
  }
}
else {
  if ($source == 'D' || $source == 'H') {
    $res = sqlStatement("SELECT * FROM layout_options " .
      "WHERE form_id = ? AND uor > 0 ORDER BY field_id",
      array($source == 'D' ? 'DEM' : 'HIS'));
  }
  else {
    $res = sqlStatement("SELECT * FROM layout_options WHERE " .
      "form_id LIKE ? AND uor > 0 AND source = ? ORDER BY field_id, form_id",
      array('LBF%', 'E'));
  }
  $last_field_id = '';
  while ($row = sqlFetchArray($res)) {
    if ($row['field_id'] === $last_field_id) continue;
    $last_field_id = $row['field_id'];
    gen_sel_row($row);
  }
}
echo "</table>\n";
?>

<?php if ($source == 'E') { ?>
<p>
<form>
<center>
<input type='text' name='new_field_id' size='20' />&nbsp;
<input type='button' value='<?php echo xla('Or create this new field ID') ?>' onclick='newField()' />
</center>
</form>
</p>
<?php } ?>

</div>
</body>
</html>
