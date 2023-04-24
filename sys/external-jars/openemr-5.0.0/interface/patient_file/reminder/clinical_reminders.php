<?php
// Copyright (C) 2011 by following authors:
//   -Brady Miller <brady@sparmy.com>
//   -Ensofttek, LLC
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

//SANITIZE ALL ESCAPES
$sanitize_all_escapes=true;

//STOP FAKE REGISTER GLOBALS
$fake_register_globals=false;

require_once("../../globals.php");
require_once("$srcdir/options.inc.php");
require_once("$srcdir/clinical_rules.php");
?>

<html>
<head>
<?php html_header_show();?>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
<link rel="stylesheet" type="text/css" href="../../../library/js/fancybox/jquery.fancybox-1.2.6.css" media="screen" />
<style type="text/css">@import url(../../../library/dynarch_calendar.css);</style>
<script type="text/javascript" src="../../../library/dialog.js?v=<?php echo $v_js_includes; ?>"></script>
<script type="text/javascript" src="../../../library/textformat.js"></script>
<script type="text/javascript" src="../../../library/dynarch_calendar.js"></script>
<?php include_once("{$GLOBALS['srcdir']}/dynarch_calendar_en.inc.php"); ?>
<script type="text/javascript" src="../../../library/dynarch_calendar_setup.js"></script>
<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-3-2/index.js"></script>
<script type="text/javascript" src="../../../library/js/common.js"></script>
<script type="text/javascript" src="../../../library/js/fancybox/jquery.fancybox-1.2.6.js"></script>
</head>

<?php
$patient_id = ($_GET['patient_id']) ? $_GET['patient_id'] : "";
?>

<body class='body_top'>
<div>
  <span class='title'><?php echo htmlspecialchars( xl('Clinical Reminders'), ENT_NOQUOTES); ?></span>
</div>
<div id='namecontainer_creminders' class='namecontainer_creminders' style='float:left;margin-right:10px'>
  <?php echo htmlspecialchars( xl('for'), ENT_NOQUOTES);?>&nbsp;
  <span class="title">
    <a href="../summary/demographics.php" onclick="top.restoreSession()"><?php echo htmlspecialchars( getPatientName($pid), ENT_NOQUOTES); ?></a>
  </span>
</div>
<div>
  <a href="../summary/demographics.php" class="css_button" onclick="top.restoreSession()">
    <span><?php echo htmlspecialchars( xl('Back To Patient'), ENT_NOQUOTES);?></span>
  </a>
</div>

<br>
<br>
<br>

<?php
  // collect the pertinent plans and rules
  $plans_default = resolve_plans_sql('','0',TRUE);
  $rules_default = resolve_rules_sql('','0',TRUE,'',$_SESSION['authUser']);
?>

<ul class="tabNav">
  <li class='current'><a href='#' onclick='top.restoreSession()'><?php echo htmlspecialchars( xl('Main'), ENT_NOQUOTES); ?></a></li>
  <li><a href='#' onclick='top.restoreSession()'><?php echo htmlspecialchars( xl('Plans'), ENT_NOQUOTES); ?></a></li>
  <li><a href='#' onclick='top.restoreSession()'><?php echo htmlspecialchars( xl('Admin'), ENT_NOQUOTES); ?></a></li>
</ul>

<div class="tabContainer">
  <div class="tab current text" style="height:auto;width:97%;">
    <?php
      clinical_summary_widget($pid,"reminders-all",'','default',$_SESSION['authUser']);
    ?>
  </div>

  <div class="tab text" style="height:auto;width:97%;">
    <?php
      clinical_summary_widget($pid,"reminders-all",'',"plans",$_SESSION['authUser']);
    ?>
  </div>

  <div class="tab" style="height:auto;width:97%;">
    <div id='report_results'>
      <table>
        <tr>
          <th rowspan="2"><?php echo htmlspecialchars( xl('Plan'), ENT_NOQUOTES); ?></th>
          <th colspan="2"><?php echo htmlspecialchars( xl('Show'), ENT_NOQUOTES); ?></th>
        </tr>
        <tr>
          <th><?php echo htmlspecialchars( xl('Patient Setting'), ENT_NOQUOTES); ?></th>
          <th style="left-margin:1em;"><?php echo htmlspecialchars( xl('Practice Default Setting'), ENT_NOQUOTES); ?></th>
        </tr>
        <?php foreach ($plans_default as $plan) { ?>
          <?php
          //only show the plan if there are any rules in it that the user has access to
          $plan_check = resolve_rules_sql('','0',TRUE,$plan['id'],$_SESSION['authUser']);
          if (empty($plan_check)) {
            continue;
          }
          ?>
          <tr>
            <td style="border-right:1px solid black;"><?php echo generate_display_field(array('data_type'=>'1','list_id'=>'clinical_plans'), $plan['id']); ?></td>
            <td align="center">
              <?php

              $patient_plan = collect_plan($plan['id'],$patient_id);

              // Set the patient specific setting for gui
              if (empty($patient_plan)) {
                $select = "default";
              }
              else {
                if ($patient_plan['normal_flag'] == "1") {
                  $select = "on";
                }
                else if ($patient_plan['normal_flag'] == "0"){
                  $select = "off";
                }
                else { // $patient_rule['normal_flag'] == NULL
                  $select = "default";
                }
              } ?>
              <select class="plan_show" name="<?php echo htmlspecialchars( $plan['id'], ENT_NOQUOTES); ?>">
                <option value="default" <?php if ($select == "default") echo "selected"; ?>><?php echo htmlspecialchars( xl('Default'), ENT_NOQUOTES); ?></option>
                <option value="on" <?php if ($select == "on") echo "selected"; ?>><?php echo htmlspecialchars( xl('On'), ENT_NOQUOTES); ?></option>
                <option value="off" <?php if ($select == "off") echo "selected"; ?>><?php echo htmlspecialchars( xl('Off'), ENT_NOQUOTES); ?></option>
              </select>
            </td>
            <td align="center" style="border-right:1px solid black;">
              <?php if ($plan['normal_flag'] == "1") {
                echo htmlspecialchars( xl('On'), ENT_NOQUOTES);
              }
              else {
                echo htmlspecialchars( xl('Off'), ENT_NOQUOTES);
              } ?>
            </td>
          </tr>
        <?php } ?>
      </table>
      <br>
      <br>
      <table>
        <tr>
          <th rowspan="2"><?php echo htmlspecialchars( xl('Rule'), ENT_NOQUOTES); ?></th>
          <th colspan="2"><?php echo htmlspecialchars( xl('Passive Alert'), ENT_NOQUOTES); ?></th>
          <th colspan="2"><?php echo htmlspecialchars( xl('Active Alert'), ENT_NOQUOTES); ?></th>
        </tr>
        <tr>
          <th><?php echo htmlspecialchars( xl('Patient Setting'), ENT_NOQUOTES); ?></th>
          <th style="left-margin:1em;"><?php echo htmlspecialchars( xl('Practice Default Setting'), ENT_NOQUOTES); ?></th>
          <th><?php echo htmlspecialchars( xl('Patient Setting'), ENT_NOQUOTES); ?></th>
          <th style="left-margin:1em;"><?php echo htmlspecialchars( xl('Practice Default Setting'), ENT_NOQUOTES); ?></th>
        </tr>
        <?php foreach ($rules_default as $rule) { ?>
          <tr>
            <td style="border-right:1px solid black;"><?php echo generate_display_field(array('data_type'=>'1','list_id'=>'clinical_rules'), $rule['id']); ?></td>
            <td align="center">
              <?php
              $patient_rule = collect_rule($rule['id'],$patient_id);
              // Set the patient specific setting for gui
              if (empty($patient_rule)) {
                $select = "default";
              }
              else {
                if ($patient_rule['passive_alert_flag'] == "1") {
                  $select = "on";
                }
                else if ($patient_rule['passive_alert_flag'] == "0"){
                  $select = "off";
                }
                else { // $patient_rule['passive_alert_flag'] == NULL
                  $select = "default";
                }
              } ?>
              <select class="passive_alert" name="<?php echo htmlspecialchars( $rule['id'], ENT_NOQUOTES); ?>">
                <option value="default" <?php if ($select == "default") echo "selected"; ?>><?php echo htmlspecialchars( xl('Default'), ENT_NOQUOTES); ?></option>
                <option value="on" <?php if ($select == "on") echo "selected"; ?>><?php echo htmlspecialchars( xl('On'), ENT_NOQUOTES); ?></option>
                <option value="off" <?php if ($select == "off") echo "selected"; ?>><?php echo htmlspecialchars( xl('Off'), ENT_NOQUOTES); ?></option>
              </select>
            </td>
            <td align="center" style="border-right:1px solid black;">
              <?php if ($rule['passive_alert_flag'] == "1") {
                echo htmlspecialchars( xl('On'), ENT_NOQUOTES);
              }
              else {
                echo htmlspecialchars( xl('Off'), ENT_NOQUOTES);
              } ?>
            </td>
            <td align="center">
              <?php
              // Set the patient specific setting for gui
              if (empty($patient_rule)) {
                $select = "default";
              }
              else {
                if ($patient_rule['active_alert_flag'] == "1") {
                  $select = "on";
                }
                else if ($patient_rule['active_alert_flag'] == "0"){
                  $select = "off";
                }
                else { // $patient_rule['active_alert_flag'] == NULL
                  $select = "default";
                }
              } ?>
              <select class="active_alert" name="<?php echo htmlspecialchars( $rule['id'], ENT_NOQUOTES); ?>">
                <option value="default" <?php if ($select == "default") echo "selected"; ?>><?php echo htmlspecialchars( xl('Default'), ENT_NOQUOTES); ?></option>
                <option value="on" <?php if ($select == "on") echo "selected"; ?>><?php echo htmlspecialchars( xl('On'), ENT_NOQUOTES); ?></option>
                <option value="off" <?php if ($select == "off") echo "selected"; ?>><?php echo htmlspecialchars( xl('Off'), ENT_NOQUOTES); ?></option>
              </select>
            </td>
            <td align="center">
              <?php if ($rule['active_alert_flag'] == "1") {
                echo htmlspecialchars( xl('On'), ENT_NOQUOTES);
              }
              else {
                echo htmlspecialchars( xl('Off'), ENT_NOQUOTES);
              } ?>
            </td>
          </tr>
        <?php } ?>
      </table>
    </div>
  </div>
</div>

<script type="text/javascript">
  $(document).ready(function() {

    enable_modals();

    tabbify();

    $(".passive_alert").change(function() {
      top.restoreSession();
      $.post( "../../../library/ajax/rule_setting.php", {
        rule: this.name,
        type: 'passive_alert',
        setting: this.value,
        patient_id: '<?php echo htmlspecialchars($patient_id, ENT_QUOTES); ?>'
      });
    });

    $(".active_alert").change(function() {
      top.restoreSession();
      $.post( "../../../library/ajax/rule_setting.php", {
        rule: this.name,
        type: 'active_alert',
        setting: this.value,
        patient_id: '<?php echo htmlspecialchars($patient_id, ENT_QUOTES); ?>'
      });
    });

    $(".plan_show").change(function() {
      top.restoreSession();
      $.post( "../../../library/ajax/plan_setting.php", {
        plan: this.name,
        type: 'normal',
        setting: this.value,
        patient_id: '<?php echo htmlspecialchars($patient_id, ENT_QUOTES); ?>'
      });
    });

    $(".medium_modal").fancybox( {
      'overlayOpacity' : 0.0,
      'showCloseButton' : true,
      'frameHeight' : 500,
      'frameWidth' : 800,
      'centerOnScroll' : false,
      'callbackOnClose' : function()  {
        refreshme();
      }
    });

    function refreshme() {
      top.restoreSession();
      location.reload();
    }

  });
</script>

</body>
</html>

