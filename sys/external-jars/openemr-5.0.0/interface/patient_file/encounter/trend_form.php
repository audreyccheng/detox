<?php
// Copyright (C) 2010 Brady Miller <brady@sparmy.com>
// Modified 2011 Rod Roark <rod@sunsetsystems.com>
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

$special_timeout = 3600;
include_once("../../globals.php");

$formname = $_GET["formname"];
$is_lbf = substr($formname, 0, 3) === 'LBF';

if ($is_lbf) {
  // Determine the default field ID and its title for graphing.
  // This is from the last graphable field in the form.
  $default = sqlQuery("SELECT field_id, title FROM layout_options WHERE " .
    "form_id = ? AND uor > 0 AND edit_options LIKE '%G%' " .
    "ORDER BY group_name DESC, seq DESC, title DESC LIMIT 1",
    array($formname));
}

//Bring in the style sheet
?>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
<?php 
// Hide the current value css entries. This is currently specific
//  for the vitals form but could use this mechanism for other
//  forms.
// Hiding classes:
//  currentvalues - input boxes
//  valuesunfocus - input boxes that are auto-calculated
//  editonly      - the edit and cancel buttons
// Showing class:
//  readonly      - the link back to summary screen
// Also customize the 'graph' class to look like links.
?>
<style>
  .currentvalues { display: none;}
  .valuesunfocus { display: none;}
  .editonly      { display: none;}

  .graph {color:#0000cc;}

  #chart {
    border-style:solid;
    border-width:2px;
    margin:0em 1em 2em 2em;
  }
</style>

<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-3-2/index.js"></script>
<script type="text/javascript" src="../../../library/openflashchart/js/json/json2.js"></script>
<script type="text/javascript" src="../../../library/openflashchart/js/swfobject.js"></script>
<script type="text/javascript">

// variable that holds graph information for the open_flash_chart_data() function
var data;

// Function that is built into swfobject.js script that will collect the
//   data used for the graph.
function open_flash_chart_data()
{
  return JSON.stringify(data);
}
	
// Show the selected chart in the 'chart' div element
function show_graph(table_graph, name_graph, title_graph)
{
  top.restoreSession();
  $.ajax({ url: '../../../library/openflashchart/graphs.php',
           type: 'POST',
           data: ({ table: table_graph,
	            name: name_graph,
	            title: title_graph
           }),
           dataType: "json",
           success: function(returnData){
	     // place the raw graph data in the data variable
             data=returnData;
	     // this function will automatically call open_flash_chart_data() in order to collect the raw data
             swfobject.embedSWF('../../../library/openflashchart/open-flash-chart.swf', "chart", "650", "200", "9.0.0");
	     // ensure show the chart div
	     $('#chart').show();
           },
	   error: function() {
	     // hide the chart div
	     $('#chart').hide();
	   }	
        });
}

$(document).ready(function(){

  // Use jquery to show the 'readonly' class entries
  $('.readonly').show();

  // Place click callback for graphing
<?php if ($is_lbf) { ?>
  // For LBF the <td> has an id of label_id_$fieldid
  $(".graph").click(function(e){ show_graph('<?php echo $formname; ?>', this.id.substring(9), $(this).text()) });
<?php } else { ?>
  $(".graph").click(function(e){ show_graph('form_vitals', this.id, $(this).text()) });
<?php } ?>

  // Show hovering effects for the .graph links
  $(".graph").hover(
    function(){
         $(this).css({color:'#ff5555'}); //mouseover
    },
    function(){
         $(this).css({color:'#0000cc'}); // mouseout
    }
  );

  // show blood pressure graph by default
<?php if ($is_lbf) { ?>
  show_graph('<?php echo $formname; ?>','<?php echo $default['field_id']; ?>','<?php echo $default['title']; ?>');
<?php } else { ?>
  show_graph('form_vitals','bps','');
<?php } ?>
});
</script>
	
<?php
if ($is_lbf) {
  // Use the List Based Forms engine for all LBFxxxxx forms.
  include_once("$incdir/forms/LBF/new.php");
}
else {

  // ensure the path variable has no illegal characters
  check_file_dir_name($formname);

  include_once("$incdir/forms/$formname/new.php");
}
?>
