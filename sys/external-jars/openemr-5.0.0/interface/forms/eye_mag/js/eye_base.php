<?php

/**
 * forms/eye_mag/js/eye_base.php
 *
 * JS Functions for eye_mag form(s), built with php features for run-time options and translations
 *
 * Copyright (C) 2016 Raymond Magauran <magauran@MedFetch.com>
 *
 * LICENSE: This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @package OpenEMR
 * @author Ray Magauran <magauran@MedFetch.com>
 * @link http://www.open-emr.org
 */


    include_once("../../../globals.php");
    include_once("$srcdir/htmlspecialchars.inc.php");
    include_once("$srcdir/acl.inc");
    include_once("$srcdir/api.inc");
    include_once("$srcdir/forms.inc");
    include_once("$srcdir/patient.inc");

    $providerID = $_REQUEST['providerID'];

    ?>
var prior_field;
var prior_text;
var response = [];
var update_chart;
var obj= [];
var IMP_order = [];
var CODING_items=[];
var CPT_92060='';
var IMP_target ="0";
var detail_reached_exam ='0';
var detail_reached_HPI ='0';
var chronic_reached_HPI ='0';
    //Coding Engine Defaults
var Code_group="Eyes";//options are Eyes - anything else and Coding Engine prefers E&M Codes
var digit_2="2"; //Eye Code
var digit_4="1"; //Established
var digit_5="4"; //Level 4
var visit_code;
var Code_new_est;
var config_byday;
var $root = $('html, body');
var scroll;

/*
 * Functions to add a quick pick selection to the correct fields on the form.
 */
function fill_QP_2fields(PEZONE, ODOSOU, LOCATION_text, selection, fill_action, Code_to_process) {
    var prefix = document.getElementById(PEZONE+'_prefix').value;
    if (prefix > '' && prefix !='off') {prefix = prefix + " ";}
    if ((prefix =='off')||(LOCATION_text =='')) { prefix=''; }
    var saved_prefix = prefix;
    if (ODOSOU =="OU") {
        fill_QP_field(PEZONE, "OD", LOCATION_text, selection, fill_action, Code_to_process);
        fill_QP_field(PEZONE, "OS", LOCATION_text, selection, fill_action, Code_to_process,saved_prefix);
    } else if  (ODOSOU =="B") {
        fill_QP_field(PEZONE, "R", LOCATION_text, selection, fill_action, Code_to_process);
        fill_QP_field(PEZONE, "L", LOCATION_text, selection, fill_action, Code_to_process,saved_prefix);
    }
}
function fill_QP_field(PEZONE, ODOSOU, LOCATION_text, selection, fill_action, Code_to_process,saved_prefix) {
    if (ODOSOU > '') {
        var FIELDID =  ODOSOU  + LOCATION_text;
    } else {
        var FIELDID =  document.getElementById(PEZONE+'_'+ODOSOU).value  + LOCATION_text;
    }
    var bgcolor = $("#" +FIELDID).css("background-color");

    if (saved_prefix) {
        var prefix = saved_prefix;
    } else {
        var prefix = document.getElementById(PEZONE+'_prefix').value;
    }

    var Fvalue = document.getElementById(FIELDID).value;
    if (prefix > '' && prefix !='off') {prefix = prefix + " ";}
    if (prefix =='off') { prefix=''; }
    if (fill_action =="REPLACE") {
        $("#" +FIELDID).val(prefix +selection);
        $("#" +FIELDID).css("background-color","#F0F8FF");
    } else if (fill_action =="APPEND") {
        $("#" +FIELDID).val(Fvalue+selection).css("background-color","#F0F8FF");
    } else {
        if (($("#" +FIELDID).css("background-color")=="rgb(245, 245, 220)") || (Fvalue ==''))  {
                //rgb(245, 245, 220) is beige - the field is untouched
            $("#" +FIELDID).val(prefix+selection).css("background-color","#F0F8FF");
        } else if (Fvalue.match(/x$/)) {
            $("#" +FIELDID).val(Fvalue+selection).css("background-color","#F0F8FF");
        } else {
            if (Fvalue >'') prefix = ", "+prefix;
            $("#" +FIELDID).val(Fvalue + prefix +selection).css("background-color","#F0F8FF");
        }
    }
    submit_form(FIELDID);
    $('#'+PEZONE+'_prefix').val('off').trigger('change');
}

/*
 * This is the core function of the form.
 * It submits the data in the background via ajax.
 * It is the reason we don't use a submit button.
 * It is called often, perhaps too often for some installs because it uses bandwidth.
 * It needs to be keenly looked at by developers as it will affect scalability.
 * It return either "Code 400" or positive hits from the clinical data passed through the Coding engine.
 * It ensures ownership of the form or provides background updates to READ-ONLY instances of the form.
 * It doesn't unlock a form to change ownership/provide write privileges.  This is done via the unlock() function.
 */
function submit_form(action) {
    var url = "../../forms/eye_mag/save.php?sub=1&mode=update&id=" + $("#form_id").val();
    if ($("#COPY_SECTION").value == "READONLY") return;
    formData = $("form#eye_mag").serialize();
    $("#menustate").val('0');
    top.restoreSession();
    $.ajax({
           type   : 'POST',
           url    : url,
           data   : formData //,           dataType: "json"
           }).done(function(result) {
                   if (result == 'Code 400') {
                   code_400(); //Not the owner: read-only mode or take ownership
                   } else {
                   // ACTIVE chart.
                   // Coding engine returns any positive Clinical findings.
                   //List these findings in the IMP_PLAN Builder
                   populate_form(result);
                   }
                   });
};

/*
 *  This function alerts the user that they have lost write privileges to another user.
 *  The form is locked (fields disabled) and they enter the READ-ONLY mode.
 *  In READ-ONLY mode the form is refreshed every 15 seconds showing changes made by the user with write privileges.
 */
function code_400() {
        //User lost ownership.  Just watching now...
        //now we should get every variable and update the form, every 15 seconds...
    $("#active_flag").html(" READ-ONLY ");
    toggle_active_flags("off");
    alert("Another user has taken control of this form.\rEntering READ-ONLY mode.");
    update_READONLY();
    this_form_id = $("#form_id").val();
    $("#COPY_SECTION").val("READONLY");
    update_chart = setInterval(function() {
                               if ($("#chart_status").value == "on") { clearInterval(update_chart); }
                               update_READONLY();
                               }, 15000);
}

/**
 *  Convert the DB datetime values into date objects in JS
 *
 * "You should parse them to get a Date object, for that format I always use the following function:
 * http://stackoverflow.com/questions/2627650/why-javascript-gettime-is-not-a-function"
 *
 */
function parseDate(input) {
    var parts = input.match(/(\d+)/g);
        // new Date(year, month [, date [, hours[, minutes[, seconds[, ms]]]]])
    return new Date(parts[0], parts[1]-1, parts[2]); // months are 0-based
}
/*
 *  Function to check locked state
 */
function check_lock(modify) {
    var locked = $("#LOCKED").val();
    var locked_by = $("#LOCKEDBY").val();
    if ($("#LOCKEDDATE").val() > '') {
        var locked_date = parseDate($("#LOCKEDDATE").val());
    } else{
        var locked_date= new Date('2000-01-01');
    }
    var uniqueID = $('#uniqueID').val();

    var url = "../../forms/eye_mag/save.php?mode=update&id=" + $("#form_id").val();
    clearInterval(update_chart);
        //if the form was locked > 1 hour ago, tag we are it - we should auto-get ownership
        //if not we have to physically take it.
    var now =new Date();
    now_time = now.getTime();
    var interval = locked_date.setTime(locked_date.getTime() + (60*60*1000));//locked timestamp + 1 hour
    if (modify=='1') {
        if ($("#chart_status").val() == "on") {
            unlock();
            toggle_active_flags("off");
            update_chart = setInterval(function() {
                                       if ($("#chart_status").val() == "on") { clearInterval(update_chart);}
                                       update_READONLY();
                                       }, 15000);
            if ($("#chart_status").value == "on") { clearInterval(update_chart); }
        } else {
            top.restoreSession();
            $.ajax({
                   type   : 'POST',
                   url    : url,
                   data   : {
                   'acquire_lock'  : '1',
                   'uniqueID'      : uniqueID,
                   'form_id'       : $("#form_id").val(),
                   'locked_by'     : $("#LOCKEDBY").val()
                   }
                   }).done(function(d) {
                           $("#LOCKEDBY").val(uniqueID);
                           toggle_active_flags("on");
                           clearInterval(update_chart);
                           });
        }
    } else if (locked =='1' && (interval < now_time)) { //it was locked more than an hour ago, take ownership quietly
        top.restoreSession();
        $.ajax({
               type   : 'POST',
               url    : url,
               data   : {
                 'acquire_lock'  : '1',
                 'uniqueID'      : uniqueID, //this user is becoming the new owner
                 'locked_by'     : locked_by, //this is the old owner
                 'form_id'       : $("#form_id").val()
               }
               }).done(function(d) {
                       $("#LOCKEDBY").val(uniqueID);
                       $("#LOCKEDDATE").val(d);
                       toggle_active_flags("on");
                       }
                       );
    } else if (locked =='1' && locked_by >'' && (uniqueID != locked_by)) {
            //form is locked by someone else, less than an hour ago...
        $("#active_flag").html(" READ-ONLY ");
        if (confirm('\tLOCKED by another user:\t\n\tSelect OK to take ownership or\t\n\tCANCEL to enter READ-ONLY mode.\t')) {
            top.restoreSession();
            $.ajax({
                   type   : 'POST',
                   url    : url,
                   data     : {
                   'acquire_lock'  : '1',
                   'uniqueID'      : uniqueID, //this user is becoming the new owner
                   'locked_by'     : locked_by, //this is the old owner
                   'form_id'       : $("#form_id").val()
                   }
                   }).done(function(d) {
                           $("#LOCKEDBY").val(uniqueID);
                           toggle_active_flags("on");
                           }
                           );
        } else {
                //User selected "Cancel" -- ie. doesn't want ownership.  Just watching...
            toggle_active_flags("off");
            update_chart = setInterval(function() {
                                       $("#COPY_SECTION").trigger('change');
                                       if ($("#chart_status").val() == "on") { clearInterval(update_chart);}
                                       update_READONLY();
                                       }, 15000);
            if ($("#chart_status").value == "on") { clearInterval(update_chart); }

        }
    }
}
/*
 * Function to save a canvas by zone
 */
function submit_canvas(zone) {
    var id_here = document.getElementById('myCanvas_'+zone);
    var dataURL = id_here.toDataURL('image/jpeg');
    top.restoreSession();
    $.ajax({
           type: "POST",
           url: "../../forms/eye_mag/save.php?canvas="+zone+"&id="+$("#form_id").val(),
           data: {
           imgBase64     : dataURL,  //this contains the canvas + new strokes, the sketch.js foreground
           'zone'        : zone,
           'visit_date'  : $("#visit_date").val(),
           'encounter'   : $("#encounter").val(),
           'pid'         : $("#pid").val()
           }

           }).done(function(o) {
                   });
}
/*
 *  Function to update the user's preferences
 */
function update_PREFS() {
    var url = "../../forms/eye_mag/save.php";
    var formData = {
        'AJAX_PREFS'            : "1",
        'PREFS_VA'              : $('#PREFS_VA').val(),
        'PREFS_W'               : $('#PREFS_W').val(),
        'PREFS_MR'              : $('#PREFS_MR').val(),
        'PREFS_W_width'         : $('#PREFS_W_width').val(),
        'PREFS_MR_width'        : $('#PREFS_MR_width').val(),
        'PREFS_CR'              : $('#PREFS_CR').val(),
        'PREFS_CTL'             : $('#PREFS_CTL').val(),
        'PREFS_ADDITIONAL'      : $('#PREFS_ADDITIONAL').val(),
        'PREFS_VAX'             : $('#PREFS_VAX').val(),
        'PREFS_IOP'             : $('#PREFS_IOP').val(),
        'PREFS_CLINICAL'        : $('#PREFS_CLINICAL').val(),
        'PREFS_EXAM'            : $('#PREFS_EXAM').val(),
        'PREFS_CYL'             : $('#PREFS_CYL').val(),
        'PREFS_EXT_VIEW'        : $('#PREFS_EXT_VIEW').val(),
        'PREFS_ANTSEG_VIEW'     : $('#PREFS_ANTSEG_VIEW').val(),
        'PREFS_RETINA_VIEW'     : $('#PREFS_RETINA_VIEW').val(),
        'PREFS_NEURO_VIEW'      : $('#PREFS_NEURO_VIEW').val(),
        'PREFS_ACT_VIEW'        : $('#PREFS_ACT_VIEW').val(),
        'PREFS_ACT_SHOW'        : $('#PREFS_ACT_SHOW').val(),
        'PREFS_HPI_RIGHT'       : $('#PREFS_HPI_RIGHT').val(),
        'PREFS_PMH_RIGHT'       : $('#PREFS_PMH_RIGHT').val(),
        'PREFS_EXT_RIGHT'       : $('#PREFS_EXT_RIGHT').val(),
        'PREFS_ANTSEG_RIGHT'    : $('#PREFS_ANTSEG_RIGHT').val(),
        'PREFS_RETINA_RIGHT'    : $('#PREFS_RETINA_RIGHT').val(),
        'PREFS_NEURO_RIGHT'     : $('#PREFS_NEURO_RIGHT').val(),
        'PREFS_PANEL_RIGHT'     : $('#PREFS_PANEL_RIGHT').val(),
        'PREFS_IMPPLAN_RIGHT'   : $('#PREFS_IMPPLAN_DRAW').val(),
        'PREFS_KB'              : $('#PREFS_KB').val(),
        'PREFS_TOOLTIPS'        : $('#PREFS_TOOLTIPS').val()
    };
    top.restoreSession();
    $.ajax({
           type     : 'POST',
           url      : url,
           data     : formData
           });
}
/*
 *  Function to unlock the form - remove temporary lock at DB level.
 */
function unlock() {
    var url = "../../forms/eye_mag/save.php?mode=update&id=" + $("#form_id").val();
    var formData = {
        'action'           : "unlock",
        'unlock'           : "1",
        'encounter'        : $('#encounter').val(),
        'pid'              : $('#pid').val(),
        'LOCKEDBY'         : $('#LOCKEDBY').val(),
        'form_id'          : $("#form_id").val()
    };
    top.restoreSession();
    $.ajax({
           type     : 'POST',
           url          : url,
           data     : formData }).done(function(o) {
                                           $("#warning").removeClass("nodisplay");
                                           $('#LOCKEDBY').val('');
                                           $('#chart_status').val('off');
                                           });
}
/*
 *  Function to fax this visit report to someone.
 */
function create_task(to_id,task,to_type) {
    var url = "../../forms/eye_mag/taskman.php";
    var formData = {
        'action'            : "make_task",
        'from_id'           : '<?php echo $providerID; ?>',
        'to_id'             : to_id,
        'pid'               : $('#pid').val(),
        'doc_type'          : task,
        'enc'               : $('#encounter').val(),
        'form_id'           : $('#form_id').val()
    };
    top.restoreSession();
    $.ajax({
           type         : 'POST',
           url          : url,
           data         : formData
           }).done(function(result) {
                   //OPTIONS to consider: we could return a status code from the server.
                   //maybe 1 = no doc, 2= doc made and queued, 3= sent
                   //maybe this is a checkbox.  Check to create the task, uncheck to delete it.
                   //if the task is completed, checkbox is checked and disabled?
                   //return doc_id and display it in html for id='status_'+task+'_'+to_type
                   obj = JSON.parse(result);
                   if (obj.DOC_link) {
                   $('#status_'+task+'_'+to_type).html(obj.DOC_link);
                   }
                   if (obj.comments) alert(obj.comments);
                   //maybe change an icon to sent?  Think.
                   });
}


/*
 *  START OF PMSFH FUNCTIONS
 */
function alter_issue2(issue_number,issue_type,index) {
    if (!obj.PMSFH) { refresh_page(); }
    if (typeof obj.PMSFH == "undefined") { submit_form(); }
    var here = obj.PMSFH[issue_type][index];
    window.frames[0].frameElement.contentWindow.newtype(issue_type);
    if (issue_type !='SOCH' && issue_type !='FH' && issue_type !='ROS') {
        $('iframe').contents().find('#delete_button').removeClass('nodisplay');
    } else {
        $('iframe').contents().find('#delete_button').addClass('nodisplay');
    }
    $('iframe').contents().find('#issue'                ).val(issue_number);
    if (typeof here !== "undefined") {
        $('iframe').contents().find('#form_title'           ).val(here.title);
        $('iframe').contents().find('#form_diagnosis'       ).val(here.diagnosis);
        $('iframe').contents().find('#form_begin'           ).val(here.begdate);
        $('iframe').contents().find('#form_end'             ).val(here.enddate);
        $('iframe').contents().find('#form_reaction'        ).val(here.reaction);
        $('iframe').contents().find('#form_referredby'      ).val(here.referredby);
        $('iframe').contents().find('#form_classification'  ).val(here.classification);
        $('iframe').contents().find('#form_occur'           ).val(here.occurrence);
        $('iframe').contents().find('#form_comments'        ).val(here.comments);
        $('iframe').contents().find('#form_outcome'         ).val(here.outcome);
        $('iframe').contents().find('#form_destination'     ).val(here.destination);
        if (here.row_subtype =='eye') {
            $('iframe').contents().find('#form_eye_subtype' ).prop("checked","checked");
        } else {
            $('iframe').contents().find('#form_eye_subtype' ).prop("checked",false);
        }
        if (here.enddate > '') {
            $('iframe').contents().find('#form_active' ).prop("checked",true);
            $('iframe').contents().find('#delete_button').addClass("nodisplay");
        } else {
            $('iframe').contents().find('#form_active' ).prop("checked",false);
        }
    }
    var location = $("#PMH_left").offset().top -55;
    $root.animate({scrollTop: location }, "slow");
}
function showArray(arr) {
    var tS = new String();
    for (var iI in arr) {
        tS += "Index "+iI+", Type "+(typeof arr[iI])+", Value "+arr[iI]+"\n";
    }
    return tS;
}

/*
 * Function to delete an issue from server via ajax
 * Ajax returns json obj.PMSFH
 * Refresh displays (right_panel and QP_PMH panel)
 */
function delete_issue2(issue_number,PMSFH_type) {
    $('#form#theform issue').val(issue_number);
    $('iframe').contents().find('#issue').val(issue_number);
    $('form#theform form_type');

    var url = '../../forms/eye_mag/a_issue.php';
    var formData = {
        'a_issue'           : issue_number,
        'deletion'            : '1',
        'PMSFH'             : '1'
    };
    top.restoreSession();
    $.ajax({
           type     : 'POST',
           url          : url,
           data     : formData,
           success:(function(result) {
                    populate_PMSFH(result);
                    })
           });
    show_QP();
    return false;
}

/*
 *  Function to save the PMSFH array to the server.
 *  This can be removed in the future - save for now
 */
function submit_PMSFH() {
    var url = "../../forms/eye_mag/save.php?PMSFH_save=1&mode=update";
    formData = $("[id^=f]").serialize();
    var f = document.forms[0];
    top.restoreSession();
    $.ajax({
           type   : 'POST',
           url    : url,
           data   : formData
           }).done(function(result){
                   f.form_title.value = '';
                   f.form_diagnosis.value = '';
                   f.form_begin.value ='';
                   f.form_end.value ='';
                   f.form_referredby.value ='';
                   f.form_reaction.value ='';
                   f.form_classification.value ='';
                   f.form_occur.value='';
                   f.form_comments.value ='';
                   f.form_outcome.value ='';
                   f.form_destination.value ='';
                   f.issue.value ='';
                   populate_form(result);
                   });}

/*
 *  END OF PMSFH FUNCTIONS
 */

/*
 *  Function to refresh the issues, the panels and the Impression/coding areas.
 */
function refresh_page() {
    var url = '../../forms/eye_mag/view.php?display=PMSFH';
    var formData = {
        'action'           : "refresh",
        'id'               : $('#form_id').val(),
        'encounter'        : $('#encounter').val(),
        'pid'              : $('#pid').val(),
        'refresh'          : 'page'
    };
    top.restoreSession();
    $.ajax({
           type     : 'POST',
           url          : url,
           data     : formData,
           success:(function(result) {
                    populate_form(result);
                    })
           });
        //Make the height of the panels equal if they grow really large
    if ($('#PMH_right').height() > $('#PMH_left').height()) {
        $('#PMH_left').height($('#PMH_right').height());
    } else { $('#PMH_left').height($('#PMH_right').height()); }
    return false;
}

/*
 *  Function to refresh the Glaucoma Flow Sheet.
 */
function refresh_GFS() {
    if (typeof config_byday == "undefined") { return; }

    var indexToUpdate = '0';
    $.each(config_byday.data.labels, function(key,value) {
           if (value == visit_date) {
           indexToUpdate = key;
           }
           });

        //var indexToUpdate = config_byday.data.labels.length-1;
    var ODIOP=0;
    var OSIOP=0;
    if ( $('#ODIOPAP').val()) {
        ODIOP =  $('#ODIOPAP').val();
    } else if (  $('#ODIOPTPN').val()) {
        ODIOP =  $('#ODIOPTPN').val();
    }

    if ( $('#OSIOPAP').val() >'0') {
        OSIOP =  $('#OSIOPAP').val();
    } else if (  $('#OSIOPTPN').val() > '0') {
        OSIOP =  $('#OSIOPTPN').val();
    }

    config_byday.data.datasets[0].data[indexToUpdate] = $('#ODIOPTARGET').val();
    config_byday.data.datasets[1].data[indexToUpdate] = ODIOP;
    config_byday.data.datasets[2].data[indexToUpdate] = OSIOP;
    myLine.update();

    var time = $('#IOPTIME').val();
    times = time.match(/^(\d{1,2}):(\d{2})/);
    if (times[1] < 10) times[1] = "0"+''+times[1];
    time = times[1]+':'+times[2];
        //alert("time is "+time);
    var indexToUpdate2 = '0';
    $.each(config_byhour.data.labels, function(key,value) {
           if (value == time) {
           indexToUpdate2 = key;
           }
           });
    config_byhour.data.datasets[0].data[indexToUpdate2] = ODIOP;
    config_byhour.data.datasets[1].data[indexToUpdate2] = OSIOP;
    myLine2.update();
        // Update one of the points in the second dataset
        //  myLine.data.datasets[1].data[indexToUpdate].val($('#ODIOPAP').val());
        //alert(config_byday.data.datasets[1].data[indexToUpdate].val()+' is ending _bydat val');
        //myLine.update();
        //ctx2.update();
    return;
    /*
     this should refresh locally and not go back to the server
     the only things that would trigger a refresh are
     a change in IOP
     change in IOPTARGET
     change in Eye Meds
     change in GONIO fields
     additional tests (VF/OCT) would not affect this in its live format

     submit_form();
     var url = '../../forms/eye_mag/view.php?display=GFS';
     var formData = {
     'action'           : "refresh_GFS",
     'id'               : $('#form_id').val(),
     'encounter'        : $('#encounter').val(),
     'pid'              : $('#pid').val(),
     'refresh'          : 'GFS'
     };
     top.restoreSession();
     $.ajax({
     type     : 'POST',
     url          : url,
     data     : formData,
     success:(function(result) {
     populate_GFS(result);
     })
     });
     */
}

function populate_GFS(result) {
    $("#LayerVision_IOP").html(result);
}

/*
 *  Server returns a json encoding object: obj to update the page
 *  Here we refresh the PMSFH display panels,
 *  Rebuild the Impression/Plan Builder DX lists,
 *  the Impression Plan area
 *  and the CHRONIC fields.
 */
function populate_form(result) {
    obj = JSON.parse(result);
    $("#QP_PMH").html(obj.PMH_panel);
    if ($('#PMH_right').height() > $('#PMH_left').height()) {
        $('#PMH_left').height($('#PMH_right').height());
    } else { $('#PMH_left').height($('#PMH_right').height()); }
    $("#right_panel_refresh").html(obj.right_panel);
    build_IMPPLAN(obj.IMPPLAN_items);
    build_Chronics(obj);
    build_DX_list(obj); //build the list of DXs to show in the Impression/Plan Builder
}
/*
 *  Function to auto-fill CHRONIC fields
 *  To reach a detailed E&M level of documentation the chart
 *  may comment on the status of 3 or more CHRONIC/Inactive problems.
 *  The user can type them into the CHRONIC fields manually, or
 *  we can do it programatically if the user does the following:
 *     1.  documenting a PMH diagnosis in the PMSFH area
 *     2.  listing it as "Chronic"
 *     3.  making a comment about it
 *  With these three steps completed, this build_CHRONIC function displays the changes
 *  in the CHRONIC1-3 textareas, if not already filled in, for today's visit.
 *  On subsequent visits, the CHRONIC1-3 fields are blank, unless the above steps
 *  were performed previously, then they are filled in automatically on loading of the new form.
 */
function build_Chronics(obj) {
    if (typeof obj.PMSFH === "undefined") return;
    var CHRONICS = obj.PMSFH['CHRONIC'];
    var chronic_value;
    var local_comment;
    var here_already;
    $.each(CHRONICS, function(key, value) {
           local_comment = CHRONICS[key].title+" "+CHRONICS[key].diagnosis+"\n"+CHRONICS[key].comments;
           here_already ='0';
           for (i=1; i < 4; i++) {
           chronic_value = $('#CHRONIC'+i).val();
           if (chronic_value == local_comment) {
           here_already='1';  //this is here, move to next CHRONICS
           break;
           }
           }
           if (here_already !='1') {
           for (i=1; i < 4; i++) {
           chronic_value = $('#CHRONIC'+i).val();
           if (chronic_value == '') {  //if the CHRONIC1-3 field is empty, fill it.
           $('textarea#CHRONIC'+i).val(local_comment);
           break;
           }
           }
           }
           });
    return false;
}
/*
 * Function to autocreate a PDF of this form as a document linked to this encounter.
 * Each time it is runs it updates by replacing the encounter's PDF.
 * This used to be fired often,  but it is a server resource beast.
 * Use it sparingly, and intentionally only.
 * Currently only invoked via the bootstrap menu: Menu->File->Print/Store PDF
 */
function store_PDF() {
    var url = "../../forms/eye_mag/save.php?mode=update";
    var formData = {
        'action'        : 'store_PDF',
        'patient_id'    : $('#pid').val(),
        'pdf'           : '1',
        'printable'     : '1',
        'form_folder'   : $('#form_folder').val(),
        'form_id'       : $('#form_id').val(),
        'encounter'     : $('#encounter').val(),
        'uniqueID'      : $('#uniqueID').val()
    };
    top.restoreSession();
    $.ajax({
           type         : 'POST',
           url          : url,
           data     : formData
           });
}

/* START Functions related to form VIEW */
/*
 * Function to blow out the form and display the right side of every section.
 */
function show_right() {
    $("#HPI_1").removeClass("size50").addClass("size100");
    $("#PMH_1").removeClass("size50").addClass("size100");
    $("#EXT_1").removeClass("size50").addClass("size100");
    $("#ANTSEG_1").removeClass("size50").addClass("size100");
    $("#NEURO_1").removeClass("size50").addClass("size100");
    $("#RETINA_1").removeClass("size50").addClass("size100");
    $("#IMPPLAN_1").removeClass("size50").addClass("size100");
    $("#HPI_right").removeClass('nodisplay');
    $("#PMH_right").removeClass('nodisplay');
    $("#EXT_right").removeClass('nodisplay');
    $("#ANTSEG_right").removeClass('nodisplay');
    $("#NEURO_right").removeClass('nodisplay');
    $("#RETINA_right").removeClass('nodisplay');
    $("#IMPPLAN_right").removeClass('nodisplay');
    $("#PMH_1").addClass("clear_both");
    $("#ANTSEG_1").addClass("clear_both");
    $("#RETINA_1").addClass("clear_both");
    $("#NEURO_1").addClass("clear_both");
    $("#IMPPLAN_1").addClass("clear_both");
    hide_PRIORS();
}
/*
 * Function to implode the form and hide the right side of every section.
 */
function hide_right() {
    $("#HPI_1").removeClass("size100").addClass("size50");
    $("#PMH_1").removeClass("size100").addClass("size50");
    $("#EXT_1").removeClass("size100").addClass("size50");
    $("#ANTSEG_1").removeClass("size100").addClass("size50");
    $("#NEURO_1").removeClass("size100").addClass("size50");
    $("#RETINA_1").removeClass("size100").addClass("size50");
    $("#IMPPLAN_1").removeClass("size100").addClass("size50");
    $("#HPI_right").addClass('nodisplay');
    $("#PMH_right").addClass('nodisplay');
    $("#EXT_right").addClass('nodisplay');
    $("#ANTSEG_right").addClass('nodisplay');
    $("#NEURO_right").addClass('nodisplay');
    $("#RETINA_right").addClass('nodisplay');
    $("#PMH_1").removeClass("clear_both");
    $("#ANTSEG_1").removeClass("clear_both");
    $("#RETINA_1").removeClass("clear_both");
    $("#NEURO_1").removeClass("clear_both");
    update_PREFS();
}
/*
 * Function to explode the form and show the left side of every section.
 */
function show_left() {
    $("#HPI_1").removeClass("size100").addClass("size50");
    $("#PMH_1").removeClass("size100").addClass("size50");
    $("#EXT_1").removeClass("size100").addClass("size50");
    $("#ANTSEG_1").removeClass("size100").addClass("size50");
    $("#NEURO_1").removeClass("size100").addClass("size50");
    $("#RETINA_1").removeClass("size100").addClass("size50");
    $("#IMPPLAN_1").removeClass("size100").addClass("size50");
    $("#HPI_left").removeClass('nodisplay');
    $("#PMH_left").removeClass('nodisplay');
    $("#EXT_left").removeClass('nodisplay');
    $("#ANTSEG_left").removeClass('nodisplay');
    $("#RETINA_left").removeClass('nodisplay');
    $("#NEURO_left").removeClass('nodisplay');
    $("#IMPPLAN_left").removeClass('nodisplay');
    $("[name$='_left']").removeClass('nodisplay');
}
/*
 * Function to implode the form and hide the left side of every section.
 */
function hide_left() {
    $("#HPI_1").removeClass("size100").addClass("size50");
    $("#PMH_1").removeClass("size100").addClass("size50");
    $("#EXT_1").removeClass("size100").addClass("size50");
    $("#ANTSEG_1").removeClass("size100").addClass("size50");
    $("#NEURO_1").removeClass("size100").addClass("size50");
    $("#RETINA_1").removeClass("size100").addClass("size50");
    $("#IMPPLAN_1").removeClass("size100").addClass("size50");
    $("#HPI_left").addClass('nodisplay');
    $("#PMH_left").addClass('nodisplay');
    $("#EXT_left").addClass('nodisplay');
    $("#ANTSEG_left").addClass('nodisplay');
    $("#RETINA_left").addClass('nodisplay');
    $("#NEURO_left").addClass('nodisplay');
    $("#IMPPLAN_left").addClass('nodisplay');
    $("[name $='_left']").addClass('nodisplay');
}
/*
 * Function to display only the DRAW panels of every section.
 * The technical section, between HPI and Clinical section is still viible.
 */
function show_DRAW() {
    hide_QP();
    hide_TEXT();
    hide_PRIORS();
    hide_left();
    hide_KB();
    show_right();

    $("#HPI_right").addClass('canvas');
    $("#PMH_right").addClass('canvas');
    $("#EXT_right").addClass('canvas');
    $("#ANTSEG_right").addClass('canvas');
    $("#RETINA_right").addClass('canvas');
    $("#NEURO_right").addClass('canvas');
    $("#IMPPLAN_right").addClass('canvas');
    $(".Draw_class").removeClass('nodisplay');
    if ($("#PREFS_CLINICAL").val() !='1') {
        $("#PREFS_CLINICAL").val('1');
        $("#PREFS_EXAM").val('DRAW');
    }
    update_PREFS();
}
/*
 * Function to display only the TEXT panels in every section.
 */
function show_TEXT() {
    $("#PMH_1").removeClass('nodisplay');
    $("#NEURO_1").removeClass('nodisplay');
    $("#IMPPLAN_1").removeClass('nodisplay');
    $(".TEXT_class").removeClass('nodisplay');
    show_left();
    hide_right(); //this hides the right half
    hide_QP();
    hide_DRAW();
    hide_PRIORS();
    if ($("#PREFS_CLINICAL").val() !='1') {
            // we want to show text_only which are found on left half
        $("#PREFS_CLINICAL").val('1');
    }
    $("#PREFS_EXAM").val('TEXT');
    $("#IMPPLAN_right").addClass('canvas').removeClass('nodisplay');
    $("#QP_IMPPLAN").removeClass('nodisplay');
    $("#DRAW_"+zone).addClass('nodisplay');
    $("#IMPPLAN_1").removeClass('nodisplay');
    $("#IMPPLAN_left").removeClass('nodisplay');
    $("#PREFS_IMPPLAN_RIGHT").val('QP');
    if (!scroll) scrollTo("HPI_left");
    update_PREFS();
}
/*
 * Function to display the PRIORS panels in every right section.
 */
function show_PRIORS() {
    $("#NEURO_sections").removeClass('nodisplay');
    hide_DRAW();
    $("#EXT_right").addClass("PRIORS_color");
    show_TEXT();
    show_right();
    hide_QP();
    $("#QP_HPI").removeClass('nodisplay');
    $("#QP_PMH").removeClass('nodisplay');
    $("#HPI_right").addClass('canvas');
    $("#PMH_right").addClass('canvas');
    $("#IMPPLAN_right").addClass('canvas');
    $("#EXT_right").addClass('canvas');
    $("#ANTSEG_right").addClass('canvas');
    $("#RETINA_right").addClass('canvas');
    $("#NEURO_right").addClass('canvas');
    $(".PRIORS_class").removeClass('nodisplay');
    if ($("#PREFS_CLINICAL").val() !='1') {
            // we want to show text_only which are found on left half now that PRIORS are visible.
        $("#PREFS_CLINICAL").val('1');
    }
    $("#PREFS_EXAM").val('PRIORS');
    update_PREFS();
}
/*
 * Function to show the Quick Picks panel on the right side of every section.
 */
function show_QP() {
    hide_DRAW();
    hide_PRIORS();
    hide_KB();
    show_TEXT();
    show_right();
    show_left();
    $("#HPI_right").addClass('canvas');
    $("#PMH_right").addClass('canvas');
    $("#EXT_right").addClass('canvas');
    $("#ANTSEG_right").addClass('canvas');
    $("#RETINA_right").addClass('canvas');
    $("#NEURO_right").addClass('canvas');
    $("#IMPPLAN_right").addClass('canvas');
    $(".QP_class").removeClass('nodisplay');
    $(".QP_class2").removeClass('nodisplay');
    $("#PREFS_EXAM").val('QP');
    update_PREFS();
}
/*
 * Function to display only one DRAW panel of one section.
 */
function show_DRAW_section(zone) {
    $("#QP_"+zone).addClass('nodisplay');
    $("#"+zone+"_1").removeClass('nodisplay');
    $("#"+zone+"_left").removeClass('nodisplay');
    $("#"+zone+"_right").addClass('canvas').removeClass('nodisplay');
    $("#Draw_"+zone).addClass('canvas');
    $("#Draw_"+zone).removeClass('nodisplay');
    $("#PREFS_"+zone+"_DRAW").val(1);
    update_PREFS();
}
/*
 * Function to display only one PRIORS panel of one section.
 */
function show_PRIORS_section(section,newValue) {
    var url = "../../forms/eye_mag/save.php?mode=retrieve";

    var formData = {
        'PRIORS_query'          : "1",
        'zone'                  : section,
        'id_to_show'            : newValue,
        'pid'                   : $('#pid').val(),
        'orig_id'               : $('#form_id').val()
    }
    top.restoreSession();
    $.ajax({
           type     : 'POST',
           url       : url,
           data     : formData,
           success   : function(result) {
           $("#PRIORS_" + section + "_left_text").html(result);
           }
           });
}
/*
 * Function to show one of the Quick Picks section on the right side of its section.
 */
function show_QP_section(zone,scroll) {
    $("#"+zone+"_right").addClass('canvas').removeClass('nodisplay');
    $("#QP_"+zone).removeClass('nodisplay');
    $("#DRAW_"+zone).addClass('nodisplay');
    $("#"+zone+"_1").removeClass('nodisplay');
    $("#"+zone+"_left").removeClass('nodisplay');
    $("#PREFS_"+zone+"_RIGHT").val('QP');
    if (!scroll) scrollTo(zone+"_left");
   }
/*
 * Function to hide all the DRAW panels of every section.
 */
function hide_DRAW() {
    $(".Draw_class").addClass('nodisplay');
    hide_right();
    $("#LayerTechnical_sections").removeClass('nodisplay');
    $("#REFRACTION_sections").removeClass('nodisplay');
    $("#PMH_sections").removeClass('nodisplay');
    $("#HPI_right").addClass('nodisplay');
    $("#HPI_right").removeClass('canvas');
    $("#EXT_right").removeClass('canvas');
    $("#RETINA_right").removeClass('canvas');
    $("#ANTSEG_right").removeClass('canvas');
}
/*
 * Function to hide all the Quick Pick panels of every section.
 */
function hide_QP() {
    $(".QP_class").addClass('nodisplay');
    $(".QP_class2").addClass('nodisplay');
    $("[name$='_right']").removeClass('canvas');
}
/*
 * Function to hide all the TEXT panels of every section.
 */
function hide_TEXT() {
    $(".TEXT_class").addClass('nodisplay');
}
/*
 * Function to hide all the PIORS panels of every section.
 */
function hide_PRIORS() {
    $("#EXT_right").removeClass("PRIORS_color");
    $("#PRIORS_EXT_left_text").addClass('nodisplay');
    $("#PRIORS_ANTSEG_left_text").addClass('nodisplay');
    $("#PRIORS_RETINA_left_text").addClass('nodisplay');
    $("#PRIORS_NEURO_left_text").addClass('nodisplay');
    $(".PRIORS_class").addClass('nodisplay');
}
/*
 * Function to hide Shorthand/Keyboard Entry panel.
 */
function hide_KB() {
    $('.kb').addClass('nodisplay');
    $('.kb_off').removeClass('nodisplay');
    if ($("#PREFS_KB").val() > 0) {
        $("#PREFS_KB").val('0');
    }
}
/*
 * Function to show the Shorthand/Keyboard panel.
 */
function show_KB() {
    $('.kb').toggleClass('nodisplay');
    $('.kb_off').toggleClass('nodisplay');
    if ($('#PREFS_EXAM').val() == 'DRAW') {
        show_TEXT();
    }

    if ($("#PREFS_KB").val() > 0) {
        $("#PREFS_KB").val('0');
    } else {
        $("#PREFS_KB").val('1');
    }
    update_PREFS();
}
/* END Functions related to form VIEW */

/*
 * Function contains menu commands specific to this form.
 */
function menu_select(zone,che) {
    $("#menu_"+zone).addClass('active');
    if (zone =='PREFERENCES') {
        window.parent.RTop.document.location.href = base+"interface/super/edit_globals.php";
        var url = base+"/interface/super/edit_globals.php";
        var formData = {
            'id'               : $('#id').val(),
            'encounter'        : $('#encounter').val(),
            'pid'              : $('#pid').val(),
        };
        top.restoreSession();
        $.ajax({
               type     : 'GET',
               url          : url,
               data     : formData,
               success      : function(result) {
               window.parent.RTop.document.result;
               }
               });
    }
    if (zone =='PRIORS') $("#PRIORS_ALL_minus_one").trigger("click");
    if (zone =='QP') show_QP();
    if (zone =='KB') show_KB();
    if (zone =='DRAW') show_DRAW();
    if (zone =='TEXT') show_TEXT();
    if (zone =='IOP_graph') $("#LayerVision_IOP_lightswitch").trigger('click');
    if (zone == "HPI") scrollTo("HPI_left");
    if (zone == "PMH") scrollTo("PMH_left");
    if (zone == "EXT") scrollTo("EXT_left");
    if (zone == "ANTSEG") scrollTo("ANTSEG_left");
    if (zone == "POSTSEG") scrollTo("RETINA_left");
    if (zone == "NEURO") scrollTo("NEURO_left");
}


/*
 * Function to test blowing up any section to fullscren - towards tablet functionality?
 * Currently not used.
 */
function show_Section(section) {
        //hide everything, show the section.  For fullscreen perhaps Tablet view per section
    show_right();
    $("div[name='_sections']").style.display= "none"; //
    $('#'+section+'_sections').style.display= "block";
        //.show().appendTo('form_container');
}
/*
 * Function to display Chief Complaint 1-3
 */
function show_CC(CC_X) {
    $("[name^='CC_']").addClass('nodisplay');
    $("#CC_"+CC_X).removeClass('nodisplay');
    $("#CC_"+CC_X).index;
}

/* START Functions related to CODING */

/*
 * Function to determine if add on NeuroSensory(92060) code can be billed.
 */
function check_CPT_92060() {
    var neuro1='';
    var neuro2 ='';
    if ($("#STEREOPSIS").val() > '') (neuro1="1");
    $(".neurosens2").each(function(index) {
                          if ($( this ).val() > '') {
                          neuro2="1";
                          }
                          });
    if (neuro1 && neuro2){
        $("#neurosens_code").removeClass('nodisplay');
        CPT_92060 = 'here';
    } else {
        $("#neurosens_code").addClass('nodisplay');
        CPT_92060 = '';
    }
}
/*
 * Function to check documentation level for coding purposes
 * And make suggestions to end user.
 */
function check_exam_detail() {
    detail_reached_HPI='0';
    chronic_reached_HPI='0';
    $(".count_HPI").each(function(index) {
                         if ($( this ).val() > '') detail_reached_HPI++;
                         });
    if (detail_reached_HPI > '3') {
        $(".detail_4_elements").css("color","red");
        $(".CODE_LOW").addClass("nodisplay");
        $(".CODE_HIGH").removeClass("nodisplay");
        $(".detailed_HPI").css("color","red");
    } else {
        $(".detail_4_elements").css("color","#876F6F");
    }
    $(".chronic_HPI").each(function(index) {
                           if ($( this ).val() > '') chronic_reached_HPI++;
                           });
    if (chronic_reached_HPI > '2') {
        $(".chronic_3_elements").css("color","red");
        $(".CODE_LOW").addClass("nodisplay");
        $(".CODE_HIGH").removeClass("nodisplay");
        $(".detailed_HPI").css("color","red");
    } else {
        $(".chronic_3_elements").css("color","#876F6F");
    }
    if ((chronic_reached_HPI > '2')||(detail_reached_HPI > '3')) {
        $(".CODE_LOW").addClass("nodisplay");
        $(".CODE_HIGH").removeClass("nodisplay");
        $(".detailed_HPI").css("color","red");
        detail_reached_HPI = '1';
    } else {
        $(".CODE_LOW").removeClass("nodisplay");
        $(".CODE_HIGH").addClass("nodisplay");
        $(".detailed_HPI").css("color","#876F6F");
        detail_reached_HPI = '0';
    }
    if ((($("#DIL_RISKS").is(':checked')) || ($(".dil_drug").is(':checked'))) && (($('#ODPERIPH').val() >'') || ($('#OSPERIPH').val() >''))) {
        $(".EXAM_LOW").addClass("nodisplay");
        $(".DIL_RISKS").removeClass("nodisplay");
        $("#DIL_RISKS").prop("checked","checked");
        detail_reached_exam = '1';
    } else {
        $(".EXAM_LOW").removeClass("nodisplay");
        $(".DIL_RISKS").addClass("nodisplay");
        detail_reached_exam = '0';
    }
    Suggest_visit_code();
}

/* END Functions related to CODING */

/* START Functions related to IMPPLAN Builder */
/*
 * Function to update the list of Dxs available for Impression/Plan and Coding(?).
 * Will use actual list from obj.IMPPLAN_items for coding.
 * After a new DX is added via PMSFH (or other ways), it updates the sortable and draggable list of DXs
 * available to build the Impression/Plan from.
 */
function build_DX_list(obj) {
    var out = "";
    var diagnosis;
    $( "#build_DX_list" ).empty();
        //add in inc_FIELDCODES culled from the datafields
    if (typeof obj.PMSFH === "undefined") return;
    if (typeof obj.Clinical === "undefined") submit_form('obj.clinical is undefined');
    if (!obj.PMSFH['POH']  && !obj.PMSFH['PMH'] && !obj.Clinical) {
        out = '<br /><span class="bold">The Past Ocular History (POH) and Past Medical History (PMH) are negative and no diagnosis was auto-generated from the clinical findings.</span><br /><br>Update the chart to activate the Builder.<br />';
        $( "#build_DX_list" ).html(out);
        return;
    }
    build_IMPPLAN(obj.IMPPLAN_items);
    if ($('#inc_PE').is(':checked') && obj.Clinical) {
        $.each(obj.Clinical, function(key, value) {
               diagnosis='';
               if (obj.Clinical[key][0].diagnosis > '') { //so we are just showing this first item of each Dx (Eg bilateral, x4 pterygium, only first shows up)
               diagnosis = "<code class='pull-right ICD_CODE'>"+obj.Clinical[key][0].code+"</code>";
               }
               out += "<li class='ui-widget-content'><span name='DX_Clinical_"+key+"' id='DX_Clinical_"+key+"'>"+obj.Clinical[key][0].title+"</span> "+diagnosis+"</li> ";
               });
    }

    if ($('#inc_POH').is(':checked') && (obj.PMSFH['POH']||obj.PMSFH['POS'])) {
        $.each(obj.PMSFH['POH'], function(key, value) {
               diagnosis='';
               if (obj.PMSFH['POH'][key].diagnosis > '' ) {
               diagnosis = "<code class='pull-right ICD_CODE'>"+obj.PMSFH['POH'][key].code+"</code>";
               }
               out += "<li class='ui-widget-content'><span name='DX_POH_"+key+"' id='DX_POH_"+key+"'>"+obj.PMSFH['POH'][key].title+"</span> "+diagnosis+"</li>";
               });
        $.each(obj.PMSFH['POS'], function(key, value) {
               diagnosis='';
               if (obj.PMSFH['POS'][key].diagnosis > '' ) {
               diagnosis = "<code class='pull-right ICD_CODE'>"+obj.PMSFH['POS'][key].code+"</code>";
               }
               out += "<li class='ui-widget-content'><span name='DX_POS_"+key+"' id='DX_POS_"+key+"'>"+obj.PMSFH['POS'][key].title+"</span> "+diagnosis+"</li>";
               });
    }
    if ($('#inc_PMH').is(':checked') && obj.PMSFH['PMH']) {
        $.each(obj.PMSFH['PMH'], function(key, value) {
               diagnosis='';
               if (obj.PMSFH['PMH'][key].diagnosis > '') {
               diagnosis = "<code class='pull-right ICD_CODE'>"+obj.PMSFH['PMH'][key].code+"</code>";
               }
               out += "<li class='ui-widget-content'><span name='DX_PMH_"+key+"' id='DX_PMH_"+key+"'>"+obj.PMSFH['PMH'][key].title+"</span>"+diagnosis+"</li> ";
               });
    }
        //add in inc_FIELDCODES culled from the datafields
    if (out !="") {
        rebuild_IMP($( "#build_DX_list" ));
        $( "#build_DX_list" )
        .html(out).sortable({ handle: ".handle",stop: function(event, ui){ rebuild_IMP($( "#build_DX_list" )) } })
        .selectable({ filter: "li", cancel: ".handle",stop: function(event, ui){ rebuild_IMP($( "#build_DX_list" )) } })
        .find( "li" )
        .addClass( "ui-corner-all  ui-selected" )
        .dblclick(function(){
                  rebuild_IMP($( "#build_DX_list" ));
                  $('#make_new_IMP').trigger('click'); //any items selected are sent to IMPPLAN directly.
                  })
            //this places the handle for the user to drag the item around.
        .prepend( "<div class='handle '><i class='fa fa-arrows fa-1'></i></div>" );
    } else {
        out = '<br /><span class="bold"><?php echo xlt("Build Your Plan")."."; ?></span><br /><br>';
        out += '<?php echo xlt('Suggestions for the Imp/Plan are built from the Exam, the Past Ocular History (POH and POS) and the Past Medical History (PMH)')."."; ?><br />';
        out += '<?php echo xlt('Update the chart to build this list')."."; ?><br />';
        $( "#build_DX_list" ).html(out);
    }
}
/**
 * Function:  After the Builder DX list is built from all the available options,
 * the end user can select to use only certain Dxs and change their sort order of importance.
 * This function builds the list of DXs selected and in the order as the user sorted them,
 * so we know what to use to build the Impression/Plan area and in what order to display them.
 */
function rebuild_IMP(obj2) {
    var surface;
    IMP_order=[];
    k='0';
    $( ".ui-selected", obj2 ).each(function() {
                                   var index = $( "#build_DX_list li" ).index( this );
                                   if ($('#build_DX_list li span')[index].id.match(/DX_POH_(.*)/)) {
                                   surface = 'POH_' + $( "#build_DX_list li span" )[index].id.match(/DX_POH_(.*)/)[1];
                                   IMP_order[k] = surface;
                                   }else if ($('#build_DX_list li span')[index].id.match(/DX_POS_(.*)/)) {
                                   surface = 'POS_' + $( "#build_DX_list li span" )[index].id.match(/DX_POS_(.*)/)[1];
                                   IMP_order[k] = surface;
                                   } else if ($('#build_DX_list li span')[index].id.match(/DX_PMH_(.*)/)) {
                                   surface = 'PMH_' + $( "#build_DX_list li span" )[index].id.match(/DX_PMH_(.*)/)[1];
                                   IMP_order[k] = surface;
                                   } else if ($('#build_DX_list li span')[index].id.match(/DX_Clinical_(.*)/)) {
                                   surface = 'CLINICAL_' + $( "#build_DX_list li span" )[index].id.match(/DX_Clinical_(.*)/)[1];
                                   IMP_order[k] = surface;
                                   }
                                   k++;
                                   });
}
/*
 * This function builds the Impression/Plan area using the object supplied: items
 * It appends "items" into the Impression Plan area, complete with:
 *      contenteditable Titles (the Impression),
 *      its code (if part of the item object),
 *      Plan textareas (autofilled with the item/object's "comment")
 * for each member of "items".
 * Duplicates are removed by server.
 */
function build_IMPPLAN(items,nodisplay) {
    var contents_here;
    if (typeof nodisplay == "undefined") {
      $('#IMPPLAN_zone').html("");
    }
      $('#Coding_DX_Codes').html("");
    
    if ((items == null) || ((typeof items == "undefined")|| (items.length =='0'))) {
        items = [];
        $('#IMPPLAN_text').removeClass('nodisplay'); //Display Builder instructions for starting out
        $('#IMPPLAN_zone').addClass('nodisplay');
    } else {
            //ok we have at least one item, display them in order; hide the Builder instructions
        $('#IMPPLAN_text').addClass('nodisplay');
        $('#IMPPLAN_zone').removeClass('nodisplay');
        count_dx=0;
        $.each(items, function( index, value ) {
               if (!value.codetext) value.codetext="";
               if (!value.code) value.code="";
               if ((value.code==="") || (value.code.match(/Code/) || (value.code==null))) {
               value.code="<i class='fa fa-search-plus'></i>&nbsp;Code";
               } else {
               count_dx++;
               if (value.code.match(/\,/g)) {
               // If there is a comma in there, there is more than one code present for this item. Split them out.
               // If code is manually changed or copied from a prior visit - item will not have a PMSFH_link
               // PMSFH_link is only present when the Builder was used to make the entry.
               if ((typeof value.PMSFH_link !== "undefined") || (value.PMSFH_link !== null)) {
               //The Title should have the description.
               var CodeArr =  value.code.split(",");
               var TitleArr = value.codedesc.split("\r");
               for (i=0;i < CodeArr.length;i++) {
               if (CodeArr.length == (TitleArr.length-1)) { //there is a trailing \r
               $('#Coding_DX_Codes').append(count_dx +'. '+CodeArr[i]+': '+TitleArr[i]+'<br />');
               } else {
               //just look it up via ajax or tell them to code it manually on the feesheet ;).
               $('#Coding_DX_Codes').append(CodeArr[i]+': <?php echo xlt('Manually retrieve description on Fee Sheet'); ?> <br />');
               }
               }
               } else  {
               //this works for Clinical-derived terms with more than one Dx Code (found in more than one location/field)
               if (value.PMSFH_link.match(/Clinical_(.*)/)) {
               if (typeof obj.Clinical !== "undefined") {
               var location = value.PMSFH_link.match(/Clinical_(.*)/)[1];
               if (obj.Clinical[location]!=null ) {
               for (i=0; i< obj.Clinical[location].length; i++) {
               $('#Coding_DX_Codes').append(count_dx +'. '+obj.Clinical[location][i].code+': '+obj.Clinical[location][i].codedesc+'<br />');
               }
               } else {
               //item has a PMSFH_link but it is not from a Clinical field
               alert("Houston, we have a problem!");
               }
               }
               }
               }
               } else { //all is good, one code only
               $('#Coding_DX_Codes').append(count_dx +'. '+value.code+': '+value.codedesc+'<br />');
               }
               }
               var title2 = value.title.replace(/(\')/g, '');
               contents_here = ( index + 1 ) +
               ". <span contenteditable title='<?php echo xla('Click to edit'); ?>' id='IMPRESSION_"+index+"'>" +
               value.title +"</span>"+
               "<span contenteditable class='pull-right' onclick='sel_diagnosis("+index+",\""+title2+"\");' title='"+value.codetext+"' id='CODE_"+index+"'>"+
               value.code + "</span>&nbsp;"+
               "<br /><textarea id='PLAN_"+index+"' name='PLAN_"+index+
               "' style='width:100%;max-width:100%;height:auto;min-height:3em;overflow-y: hidden;padding-top: 1.1em; '>"+
               value.plan +"</textarea><br />";
               $('#IMPPLAN_zone').append('<div id="IMPPLAN_zone_'+index+'" class="IMPPLAN_class">'+
                                         '<i class="pull-right fa fa-close" id="BUTTON_IMPPLAN_'+index+'"></i>'+
                                         contents_here+'</div>');
               $('#BUTTON_IMPPLAN_'+index).click(function() {//delete/close icon
                                                 var item = this.id.match(/BUTTON_IMPPLAN_(.*)/)[1];
                                                 obj.IMPPLAN_items.splice(item,1);
                                                 build_IMPPLAN(obj.IMPPLAN_items);
                                                 store_IMPPLAN(obj.IMPPLAN_items,'1');
                                                 });
               $('#PLAN_'+index).css("background-color","#F0F8FF");

               });
            //end each

            // The IMPRESSION DXs are "contenteditable" spans.
            // If the user changes the words in an IMPRESSION Diagnosis area, store it.
        $('[id^=IMPRESSION_]').blur(function(e) {
                                    e.preventDefault();
                                    var item = this.id.match(/IMPRESSION_(.*)/)[1];
                                    var content = this.innerText || this.innerHTML;
                                    obj.IMPPLAN_items[item].title = content;
                                    store_IMPPLAN(obj.IMPPLAN_items,'1');
                                    //$(this).css('background-color','#F0F8FF');
                                    return false;
                                    });
        $('[id^=CODE_]').blur(function() {
                              var item = this.id.match(/CODE_(.*)/)[1];
                              var new_code = this.innerText || this.innerHTML;
                              obj.IMPPLAN_items[item].code =  new_code;
                              //obj.IMPPLAN_items[item].codetext = '';
                              //obj.IMPPLAN_items[item].codedesc = '';
                              $(this).css('background-color','#F0F8FF');
                              store_IMPPLAN(obj.IMPPLAN_items,'1');
                              });

        $('[id^=PLAN_]').change(function() {
                                var item = this.id.match(/PLAN_(.*)/)[1];
                                obj.IMPPLAN_items[item].plan =  $(this).val();
                                store_IMPPLAN(obj.IMPPLAN_items,'1');
                                $(this).css('background-color','#F0F8FF');
                                });

        $('#IMPPLAN_zone').on( 'keyup', 'textarea', function (e){
                              $(this).css('height', 'auto' );
                              $(this).height( this.scrollHeight );
                              });
        $('#IMPPLAN_zone').find( 'textarea' ).keyup();
        obj.IMPPLAN_items = items;
    }
}


/*
 * This functions updates a PMSFH item's code on the server via its issue number
 */
function update_PMSFH_code(the_issue,new_code){
    var url = "../../forms/eye_mag/save.php?mode=update";
    top.restoreSession();
    $.ajax({
           type         : 'POST',
           url          :  url,
           data     : {
           action       : 'code_PMSFH',
           pid          : $('#pid').val(),
           form_id      : $('#form_id').val(),
           encounter    : $('#encounter').val(),
           uniqueID     : $('#uniqueID').val(),
           issue        : the_issue,
           code         : new_code
           }
           }).done(function(result) {
                   if (result == 'Code 400') {
                   code_400(); //the user does not have write privileges!
                   return;
                   }
                   });
}


/*
 *  This function sends the obj.IMPPLAN_items to the server for storage
 */
function store_IMPPLAN(storage,nodisplay) {
    if (typeof storage !== "undefined") {
        var url = "../../forms/eye_mag/save.php?mode=update&store_IMPPLAN";
        var formData =  JSON.stringify(storage);
        top.restoreSession();
        $.ajax({
               type         : 'POST',
               url          :  url,
               dataType     : 'json',
               data         : {
                 parameter     : formData,
                 action        : 'store_IMPPLAN',
                 pid           : $('#pid').val(),
                 form_id       : $('#form_id').val(),
                 encounter     : $('#encounter').val(),
                 uniqueID      : $('#uniqueID').val()
               }
               }).done(function(result) {
                       if (result == "Code 400") {
                       code_400(); //the user does not have write privileges!
                       return;
                       }
                       obj.IMPPLAN_items = result;
                    //   if (typeof nodisplay === "undefined") {
                          build_IMPPLAN(obj.IMPPLAN_items,nodisplay);
                      // }
                       });
    }
}


/*
 *  This submits any codes we have in the obj.IMPPLAN_items variable, ie. what is in the Impression Plan currently, to the coding engine.
 *
 */
function CODING_to_feesheet(CODING_items) {
    if (typeof CODING_items !== "undefined") {
        var url = "../../forms/eye_mag/save.php?mode=update&track=ThingOne";
        var formData =  JSON.stringify(CODING_items);
        top.restoreSession();
        $.ajax({
               type         : 'POST',
               url          :  url,
               data     : {
               parameter     : formData,
               action        : 'code_visit',
               pid           : $('#pid').val(),
               form_id       : $('#form_id').val(),
               encounter     : $('#encounter').val(),
               uniqueID      : $('#uniqueID').val()
               }
               }).done(function(result) {
                       if (result == "Code 400") {
                        code_400(); //the user does not have write privileges!
                        return;
                       } else {
                        $("#goto_fee_sheet").removeClass('nodisplay');
                       }
                       });
    }

}

/*
 * This function allows the user to drag a DX from the Impression/Plan Builder list directly onto the Impression Plan list.
 * This item is appended to the $('#IMPPLAN_zone').
 */
function dragto_IMPPLAN_zone(event, ui) {
    var findme = ui.draggable.find("span").attr("id");
    var group = findme.match(/DX_(.*)_(.*)/)[1];
    var location = findme.match(/DX_(.*)_(.*)/)[2];
    var the_code ='';
    var the_codedesc ='';
    var the_codetext ='';
    var the_plan ='';
    if (obj.IMPPLAN_items ==null) obj.IMPPLAN_items = [];
    if (group =="Clinical") {
            //more than one field can contain this DX.
            //Group them into one IMPPLAN.
        for (i=0;i < obj.Clinical[location].length; i++) {
            the_code += obj.Clinical[location][i]['code']+',';
            the_codedesc = obj.Clinical[location][i]['codedesc'];
            the_codetext = obj.Clinical[location][i]['codetext'];
            the_plan += obj.Clinical[location][i]['codedesc'] + "\r";
        }
        if (i > 0) the_code = the_code.slice(0, -1);
        obj.IMPPLAN_items.push({
                               code:        the_code,
                               codedesc:    the_codedesc,
                               codetext:    the_codetext,
                               codetype:    obj.Clinical[location][0]['codetype'],
                               plan:        the_plan,
                               PMSFH_link:  obj.Clinical[location][0]['PMSFH_link'],
                               title:       obj.Clinical[location][0]['title']
                               });

    } else {
        obj.IMPPLAN_items.push({
                               code:        obj.PMSFH[group][location]['code'],
                               codedesc:    obj.PMSFH[group][location]['codedesc'],
                               codetext:    obj.PMSFH[group][location]['codetext'],
                               codetype:    obj.PMSFH[group][location]['codetype'],
                               plan:        obj.PMSFH[group][location]['comments'],
                               PMSFH_link:  obj.PMSFH[group][location]['PMSFH_link'],
                               title:       obj.PMSFH[group][location]['title']

                               });
    }
    store_IMPPLAN(obj.IMPPLAN_items); //redisplay the items
}
/*
 * This function allows the user to drag a DX from the IMPRESSION list directly into the New Dx field $('#IMP') <-- New Dx textarea
 * The data is appended to the end of the text.
 * It doesn't know what is already there (yet) so numbering if desired must be done manually.
 */
function dragto_IMPPLAN(event, ui) {
    var findme = ui.draggable.find("span").attr("id");
    var group = findme.match(/DX_(.*)_(.*)/)[1];
    var location = findme.match(/DX_(.*)_(.*)/)[2];
    var draggable2 = ui.draggable;
    if (group =="Clinical") {
        $('#IMP').val(ui.draggable[0].textContent+"\n");
    } else {
        $('#IMP').val(ui.draggable[0].textContent+"\n"+obj.PMSFH[group][location]['comments']);
    }
}
/* END Functions related to IMPPLAN Builder */

function Suggest_visit_code() {
        //assume Eyes and established patient
    (Code_group != 'Eyes')  ? (digit_2 = '9') : digit_2 = '2'; //920XX or 990XX
    (Code_new_est == 'New')  ? (digit_4 = '0') : digit_4 = '1'; //9X01X or 9X00X
    if (detail_reached_exam =='1' && (detail_reached_HPI =='1')) {
        (Code_group =='Eyes') ? (digit_5 = '4') : (digit_5='3'); //920X4 or 990X3
        detailed = "comprehensive";
    } else {
        digit_5 = '2'; //920X2
        detailed = "intermediate";
    }
    visit_desc = Code_new_est +" "+ detailed +" "+digit_5;
    visit_code = "9"+digit_2+"0"+digit_4+digit_5;
    $('#visit_codes').val("CPT4|"+visit_code+"|").change();
}
/*
 *  This function builds the codes and populates the billing table for this encounter.
 */
function build_CODING_list() {
    CODING_items =[];
    /*  the following things get billed:
     1. Visit code(s) including neurosensory if performed
     2. Tests performed
     3. Diagnostic codes
     */
        //1.  Visit Codes.
    CODING_items.push({
                      code:     visit_code,
                      codedesc: visit_desc,
                      codetext: '',
                      codetype: 'CPT4',
                      title:    'Visit Code'
                      });
        //neurosensory
    if (CPT_92060 == 'here') {
        CODING_items.push({
                          code:     '92060',
                          codedesc: 'Sensorimotor exam',
                          codetext: 'Sensorimotor exam (CPT4:92060)',
                          codetype: 'CPT4',
                          title:    'Neuro/Sensorimotor Code',
                          justify:  visit_justify
                          });
    }
        //2. Tests/procedures performed to bill
    $('.TESTS').each(function(i, obj) {
                     //test
                     if  ($(this).is(':checked')) {
                     var codetype = obj.value.match(/(.*):(.*)/)[1];
                     var code = obj.value.match(/(.*):(.*)/)[2];
                     var modifier = $('#'+obj.id+'_modifier').val();
                     //alert(modifier);
                     CODING_items.push({
                                       'code'     : code,
                                       'codedesc' : obj.title,
                                       'codetext' : obj.codetext,
                                       'codetype' : codetype,
                                       'title'    : obj.title,
                                       'modifier' : modifier
                                       });
                     }
                     });
        //3. Diagnostic Codes
    $.each(obj.IMPPLAN_items, function( index, value ) {
           if (value['codetype']) {
           if (value['code'].match(/\,/g)) {
           //physical finding found in more than one location, more than one code...
           //if there is a comma in there, there is more than one code present. Split them out.
           // And all those in one group have the same link out (PMSFH_link) value
           var location = value.PMSFH_link.match(/Clinical_(.*)/)[1];
           for (i=0; i< obj.Clinical[location].length; i++) {
           CODING_items.push({
                             code:     obj.Clinical[location][i]['code'],
                             codedesc: obj.Clinical[location][i]['codedesc'],
                             codetext: obj.Clinical[location][i]['codetext'],
                             codetype: obj.Clinical[location][i]['codetype'],
                             title:    obj.Clinical[location][i]['title']
                             });
           }
           } else {
           CODING_items.push({
                             code:     value['code'],
                             codedesc: value['codedesc'],
                             codetext: value['codetext'],
                             codetype: value['codetype'],
                             title:    value['title']
                             });
           }
           }
           });
    CODING_to_feesheet(CODING_items);
}

/*
 * Function to make the form fields inactive or active depending on the form's state (Active vs. READ-ONLY)
 */
function toggle_active_flags(new_state) {
    if (($("#chart_status").val() == "off") || (new_state == "on")) {
            //  we are read-only and we want to go active.
        $("#chart_status").val("on");
        $("#active_flag").html(" Active Chart ");
        $("#active_icon").html("<i class='fa fa-toggle-on'></i>");
        $("#warning").addClass("nodisplay");
        $('input, select, textarea, a').removeAttr('disabled');
        $('input, textarea').removeAttr('readonly');
    } else {
            //else clicking this means we want to go from active to read-only
        $("#chart_status").val("off");
        $("#active_flag").html(" READ-ONLY ");
        $("#active_icon").html("<i class='fa fa-toggle-off'></i>");
        $("#warning").removeClass("nodisplay");
            //we should tell the form fields to be disabled. should already be...
        $('input, select, textarea, a').attr('disabled', 'disabled');
        $('input, textarea').attr('readonly', 'readonly');
            //need to also disable Ductions and Versions, PRIORS, Quicks Picks and Drawing!!! AND IMPPLAN area.
            //Either way a save in READ-ONLY mode fails - just returns this pop_up again, without saving...
        this_form_id = $("#form_id").val();
        $("#COPY_SECTION").val("READONLY-"+this_form_id);
    }
}
/*
 * Function to update a form in READ-ONLY mode with any data added by the Active version of this form_id/encounter form
 */
function update_READONLY() {
    var data = {
        'action'      : 'retrieve',
        'copy'        : 'READONLY',
        'zone'        : 'READONLY',
        'copy_to'     : $("#form_id").val(),
        'copy_from'   : $("#form_id").val(),
        'pid'         : $("#pid").val()
    };
        //we are going to update the whole form
        //Imagine you are watching on your browser while the tech adds stuff in another room on another computer.
        //We are not ready to actively chart, just looking to see how far along our staff is...
        //or maybe just looking ahead to see the who's being worked up in the next room?
        //Either way, we are looking at a record that at present will be disabled/we cannot change...
        // yet it is updating every 10-15 seconds if another user is making changes.
    top.restoreSession();
    $.ajax({
           type   : 'POST',
           dataType : 'json',
           url      :  "../../forms/eye_mag/save.php?copy=READONLY",
           data   : data,
           success  : function(result) {
           $.map(result, function(valhere, keyhere) {
                 if ($("#"+keyhere).val() != valhere) {
                 $("#"+keyhere).val(valhere).css("background-color","#CCF");
                 }
                 if (keyhere.match(/MOTILITY_/)) {
                 // Copy forward ductions and versions visually
                 // Make each blank, and rebuild them
                 $("[name='"+keyhere+"_1']").html('');
                 $("[name='"+keyhere+"_2']").html('');
                 $("[name='"+keyhere+"_3']").html('');
                 $("[name='"+keyhere+"_4']").html('');
                 if (keyhere.match(/(_RS|_LS|_RI|_LI|_RRSO|_RRIO|_RLSO|_RLIO|_LRSO|_LRIO|_LLSO|_LLIO)/)) {
                 // Show a horizontal (minus) tag.
                 hash_tag = '<i class="fa fa-minus"></i>';
                 } else { //show vertical tag
                 hash_tag = '<i class="fa fa-minus rotate-left"></i>';
                 }
                 for (index =1; index <= valhere; ++index) {
                 $("#"+keyhere+"_"+index).html(hash_tag);
                 }
                 } else if (keyhere.match(/^(ODVF|OSVF)\d$/)) {
                 if (valhere =='1') {
                 $("#FieldsNormal").prop('checked', false);
                 $("#"+keyhere).prop('checked', true);
                 $("#"+keyhere).val('1');
                 } else {
                 $("#"+keyhere).val('0');
                 $("#"+keyhere).prop('checked', false);
                 }
                 } else if (keyhere.match(/AMSLERO(.)/)) {
                 var sidehere = keyhere.match(/AMSLERO(.)/);
                 if (valhere < '1') valhere ='0';
                 $("#"+keyhere).val(valhere);
                 var srcvalue="AmslerO"+sidehere[1];
                 document.getElementById(srcvalue).src = document.getElementById(srcvalue).src.replace(/\_\d/g,"_"+valhere);
                 $("#AmslerO"+sidehere[1]+"value").text(valhere);
                 } else if (keyhere.match(/VA$/)) {
                 $("#"+keyhere+"_copy").val(valhere).css("background-color","#F0F8FF");;
                 $("#"+keyhere+"_copy_brd").val(valhere).css("background-color","#F0F8FF");;
                 } else if (keyhere.match(/^O.VA_/)) {
                 var side=keyhere.match(/(O.)VA_(.)/)[1];
                 var rx_number=keyhere.match(/(O.)VA_(.)/)[2];
                 if (rx_number == '1') { //update VA_1_copy and VA_1_copy_brd (first wearing RX only)
                 $('#'+side+'VA_1_copy').val(valhere).css("background-color","#F0F8FF");;
                 $('#'+side+'VA_1_copy_brd').val(valhere).css("background-color","#F0F8FF");;
                 }
                 } else if (keyhere.match(/^RX_TYPE_\d$/)) {
                 if (typeof $('input:radio[name='+keyhere+']')[valhere] !== "undefined") {
                 $('input:radio[name='+keyhere+']')[valhere].checked = true;
                 }
                 } else if (keyhere.match(/(alert|oriented|confused|PUPIL_NORMAL)/)) {
                 if (valhere =='1') {
                 $('#'+keyhere).val(valhere).prop('checked', true);
                 } else {
                 $('#'+keyhere).val(valhere).prop('checked', false);
                 }
                 }
                 });
           }});
}
function dopopup(url) {
    window.open(url, 'clinical', 'width=fullscreen,height=fullscreen,resizable=1,scrollbars=1,directories=0,titlebar=0,toolbar=0,location=0,status=0,menubar=0');
}
function goto_url(url) {
    window.open(url);
}
function openImage() {
    dlgopen(base+'/controller.php?document&retrieve&patient_id=3&document_id=10&as_file=false', '_blank', 600, 475);
}
/*
 *  Keyboard shortcut commands.
 */

shortcut.add("Control+T",function() {
             show_TEXT();
             });
shortcut.add("Meta+T",function() {
             show_TEXT();
             });
shortcut.add("Control+D",function() {
             show_DRAW();
             });
shortcut.add("Meta+D",function() {
             show_DRAW();
             });
shortcut.add("Control+P",function() {
             $("#PRIOR_ALL").val($('#form_id').val()).trigger("change");
             });
shortcut.add("Meta+P",function() {
             show_PRIORS();
             $("#PRIOR_ALL").val($('#form_id').val()).trigger("change");
             });
shortcut.add("Control+B",function() {
             show_QP();
             });
shortcut.add("Meta+B",function() {
             show_QP();
             });
shortcut.add("Control+K",function() {
             show_KB();
             });
shortcut.add("Meta+K",function() {
             show_KB();
             });
$(function(){
  /*
   * this swallows backspace keys on the "rx" elements.
   * stops backspace -> back a page in the browser, a very annoying thing indeed.
   */
  var rx = /INPUT|SELECT|TEXTAREA|SPAN|DIV/i;

  $(document).bind("keydown keypress", function(e){
                   if( e.which == 8 ){ // 8 == backspace
                   if(!rx.test(e.target.tagName) || e.target.disabled || e.target.readOnly ){
                   e.preventDefault();
                   }
                   }
                   });
  });

/* Undo feature
 *  RIGHT NOW THIS WORKS PER FIELD ONLY in FF. In Chrome it works great.  Not sure about IE at all.
 *  In FF, you select a field and CTRL-Z reverses/Shift-Ctrl-Z forwards value
 *  To get true Undo Redo, we will need to create two arrays, one with the command/field, prior value, next value to undo
 *  and when undone, add this to the REDO array.  When an Undo command is followed by anything other than Redo, it erases REDO array.
 *  Ctrl-Z works without this extra code!  Fuzzy on the details for specific browsers so TODO.
 */


/**
 *  Function to update the PCP and referring person
 *
 */
function update_DOCS() {
    var url = "../../forms/eye_mag/save.php?mode=update";
    top.restoreSession();
    $.ajax({
           type         : 'POST',
           url          :  url,
           data     : {
           action       : 'docs',
           pid          : $('#pid').val(),
           pcp          : $('#form_PCP').val(),
           rDOC         : $('#form_rDOC').val(),
           form_id      : $('#form_id').val(),
           encounter    : $('#encounter').val(),
           uniqueID     : $('#uniqueID').val()
           }
           }).done(function(result) {
                   if (result == "Code 400") {
                   code_400(); //the user does not have write privileges!
                   return;
                   }
                   });
}

/**
 *  Function to convert ophthalmic prescriptions between plus cylinder and minus cylinder
 *
 */
function reverse_cylinder(target) {
        //target can be revW1-5,AR,MR,CR,CTL,
    var prefix;
    var suffix;
    if (target.match(/^(AR|MR|CR|CTL)$/)) {
        prefix = target;
        suffix = '';
    }
    if (target.match(/^revW[1-5]{1}$/)) { //matches on digit only, here 1-5
        target = target.replace("revW","");
        prefix = '';
        suffix = '_'+target;
    }
    var Rsph  = $('#'+prefix+'ODSPH'+suffix).val();
    var Rcyl  = $('#'+prefix+'ODCYL'+suffix).val();
    var Raxis = $('#'+prefix+'ODAXIS'+suffix).val();
    var Lsph  = $('#'+prefix+'OSSPH'+suffix).val();
    var Lcyl  = $('#'+prefix+'OSCYL'+suffix).val();
    var Laxis = $('#'+prefix+'OSAXIS'+suffix).val();
    if (Rsph=='' && Rcyl =='' && Lsph=='' && lcyl =='') return;
    if ((!Rcyl.match(/SPH/i)) && (Rcyl >'')) {
        if (Rsph.match(/plano/i)) Rsph ='0';
        Rsph = Number(Rsph);
        Rcyl = Number(Rcyl);
        Rnewsph = Rsph + Rcyl;
        if (Rnewsph ==0) Rnewsph ="PLANO";
        Rnewcyl = Rcyl * -1;
        if (Rnewcyl > 0) Rnewcyl = "+"+Rnewcyl;
        if (parseInt(Raxis) < 90) {
            Rnewaxis = parseInt(Raxis) + 90;
        } else {
            Rnewaxis = parseInt(Raxis) - 90;
        }
        if (Rnewcyl=='0') Rnewcyl = "SPH";
        if (Rnewsph =='0') {
            Rnewsph ="PLANO";
            if (Rnewcyl =="SPH") Rnewcyl = '';
        }
        $('#'+prefix+'ODSPH'+suffix).val(Rnewsph);
        $('#'+prefix+'ODCYL'+suffix).val(Rnewcyl);
        $('#'+prefix+'ODAXIS'+suffix).val(Rnewaxis);
        $('#'+prefix+'ODAXIS'+suffix).trigger('blur');
        $('#'+prefix+'ODSPH'+suffix).trigger('blur');
        $('#'+prefix+'ODCYL'+suffix).trigger('blur');
    }
    if ((!Lcyl.match(/SPH/i)) && (Lcyl >'')) {
        if (!Lsph.match(/\d/)) Lsph ='0';
        Lsph = Number(Lsph);
        Lcyl = Number(Lcyl);
        Lnewsph = Lsph + Lcyl;
        Lnewcyl = Lcyl * -1;
        if (Lnewcyl > 0) Lnewcyl = "+"+ Lnewcyl;
        if (parseInt(Laxis) < 90) {
            Lnewaxis = parseInt(Laxis) + 90;
        } else {
            Lnewaxis = parseInt(Laxis) - 90;
        }

        if (Lnewcyl=='0') Lnewcyl = "SPH";
        if (Lnewsph =='0') {
            Lnewsph ="PLANO";
            if (Lnewcyl =="SPH") Lnewcyl = '';
        }

        $('#'+prefix+'OSSPH'+suffix).val(Lnewsph);
        $('#'+prefix+'OSCYL'+suffix).val(Lnewcyl);
        $('#'+prefix+'OSAXIS'+suffix).val(Lnewaxis);
        $('#'+prefix+'OSAXIS'+suffix).trigger('blur');
        $('#'+prefix+'OSSPH'+suffix).trigger('blur');
        $('#'+prefix+'OSCYL'+suffix).trigger('blur');
    }
}
function scrollTo(target) {
  //if (scroll !== '1') return;
  var offset;
  var scrollSpeed = 500;
  var wheight = $(window).height();
  offset = $("#"+target).offset().top - (wheight / 2)+200;
  if (offset > (window.pageYOffset +150)||offset < (window.pageYOffset -150)) {
    $('html, body').animate({scrollTop:offset}, scrollSpeed);
  }
}

$(document).ready(function() {
                  check_lock();
                  $('[title]').qtip({
                                    position: {
                                    my: 'top Right',  // Position my top left...
                                    at: 'bottom Left', // at the bottom right of...
                                    target: 'mouse' // my target
                                    }
                                    }
                                    );
                  $('#form_PCP,#form_rDOC').change(function() {
                                                   update_DOCS();
                                                   });
                  $('#tooltips_status').html($('#PREFS_TOOLTIPS').val());
                  if ($("#PREFS_TOOLTIPS").val() == "<?php echo xla('Off'); ?>") {
                  $('[title]').qtip('disable');
                  }
                  $('#tooltips_toggle,#tooltips_status').click(function() {
                                                               if ($("#PREFS_TOOLTIPS").val() == "<?php echo xla('On'); ?>") {
                                                               $('#PREFS_TOOLTIPS').val('<?php echo xla('Off'); ?>');
                                                               $("#tooltips_status").html('<?php echo xla('are off'); ?>');
                                                               $('[title]').qtip('disable');
                                                               } else {
                                                               $('#PREFS_TOOLTIPS').val('<?php echo xla('On'); ?>');
                                                               $('#tooltips_status').html('<?php echo xla('are on'); ?>');
                                                               $('[title]').qtip('enable');
                                                               }
                                                               update_PREFS();
                                                               });
                  $('#toggle_drugs').click(function(){
                                           $('.hideme_drugs').toggleClass('nodisplay');
                                           $(this).find('i').toggleClass('fa-toggle-down fa-toggle-up')
                                           return false;
                                           });
                  $('#toggle_VFs').click(function(){
                                         $('.hideme_VFs').toggleClass('nodisplay');
                                         $(this).find('i').toggleClass('fa-toggle-down fa-toggle-up')
                                         return false;
                                         });
                  $('#toggle_OCTs').click(function(){
                                          $('.hideme_OCTs').toggleClass('nodisplay');
                                          $(this).find('i').toggleClass('fa-toggle-down fa-toggle-up')
                                          return false;
                                          });
                  $('#toggle_cups').click(function(){
                                          $('.hideme_cups').toggleClass('nodisplay');
                                          $(this).find('i').toggleClass('fa-toggle-down fa-toggle-up')
                                          return false;
                                          });
                  $('#toggle_gonios').click(function(){
                                            $('.hideme_gonios').toggleClass('nodisplay');
                                            $(this).find('i').toggleClass('fa-toggle-down fa-toggle-up')
                                            return false;
                                            });
                  $('.close').click(function(){
                                    $('#GFS_accordion .hide').slideUp();
                                    });
                  $('#ODIOPTARGET').change(function() {
                                           $('#OSIOPTARGET').val($('#ODIOPTARGET').val());
                                           refresh_GFS();
                                           });
                  $('#ODIOPAP,#OSIOPAP,#ODIOPTARGET').change(function() {
                                                             //this is failing if there is no config_by_day variable.
                                                             refresh_GFS();
                                                             });
                  if ($("#PREFS_KB").val() =='1') {
                  $(".kb").removeClass('nodisplay');
                  $(".kb_off").addClass('nodisplay');
                  } else {
                  $(".kb").addClass('nodisplay');
                  $(".kb_off").removeClass('nodisplay');
                  }

                  $("[name$='_kb']").click(function() {
                                           $('.kb').toggleClass('nodisplay');
                                           $('.kb_off').toggleClass('nodisplay');
                                           if ($('#PREFS_EXAM').val() == 'DRAW') {
                                           show_TEXT();
                                           }

                                           if ($("#PREFS_KB").val() > 0) {
                                           $("#PREFS_KB").val('0');
                                           } else {
                                           $("#PREFS_KB").val('1');
                                           }
                                           update_PREFS();
                                           });
                  $('.ke').mouseover(function() {
                                     $(this).toggleClass('yellow');
                                     });
                  $('.ke').mouseout(function() {
                                    $(this).toggleClass('yellow');
                                    });
                  $("[id$='_keyboard'],[id$='_keyboard_left']").on('keydown', function(e) {
                                                                   //this is the Shorthand engine's ignition
                                                                   if (e.which == 13|| e.keyCode == 13||e.which == 9|| e.keyCode == 9) {
                                                                   e.preventDefault();
                                                                   var data_all = $(this).val();
                                                                   var data_seg = data_all.match(/([^;]*)/gm);
                                                                   var field2 ='';
                                                                   var appendix =".a";
                                                                   var zone;
                                                                   for (index=0; index < data_seg.length; ++index) {
                                                                     if (data_seg[index] =='') continue;
                                                                     data_seg[index] = data_seg[index].replace(/^[\n\v\f\r\x85\u2028\u2029\W]*/,'');
                                                                     data_seg[index] = data_seg[index].replace(/^[\s]*/,'');
                                                                     if (data_seg[index].match(/^D($|;)/i)) {
                                                                     $("#EXT_defaults").trigger("click");
                                                                     $("#ANTSEG_defaults").trigger("click");
                                                                     $("#RETINA_defaults").trigger("click");
                                                                     $("#NEURO_defaults").trigger("click");
                                                                     continue;
                                                                     }
                                                                     if (data_seg[index].match(/^DEXT($|;)/i)) {
                                                                     $("#EXT_defaults").trigger("click");
                                                                     continue;
                                                                     }
                                                                     if (data_seg[index].match(/^DANTSEG($|;)/i)) {
                                                                     $("#ANTSEG_defaults").trigger("click");
                                                                     continue;
                                                                     }
                                                                     if (data_seg[index].match(/^DAS($|;)/i)) {
                                                                     $("#ANTSEG_defaults").trigger("click");
                                                                     continue;
                                                                     }
                                                                     if (data_seg[index].match(/^DRETINA($|;)/i)) {
                                                                     $("#RETINA_defaults").trigger("click");
                                                                     continue;
                                                                     }
                                                                     if (data_seg[index].match(/^DRET($|;)/i)) {
                                                                     $("#RETINA_defaults").trigger("click");
                                                                     continue;
                                                                     }
                                                                     if (data_seg[index].match(/^DNEURO($|;)/i)) {
                                                                     $("#NEURO_defaults").trigger("click");
                                                                     continue;
                                                                    }
                                                                   if ((data_seg[index].match(/^CLEAREXT($|;)/i))||
                                                                       (data_seg[index].match(/^CEXT($|;)/i)))  {
                                                                        $(".EXT").val('');
                                                                        continue;
                                                                   }
                                                                   if ((data_seg[index].match(/^CLEARAS($|;)/i))||
                                                                       (data_seg[index].match(/^CLEARANTSEG($|;)/i))||
                                                                       (data_seg[index].match(/^CANTSEG($|;)/i))||
                                                                       (data_seg[index].match(/^CANT($|;)/i))||
                                                                       (data_seg[index].match(/^CAS($|;)/i))) {
                                                                          $(".ANTSEG").val('');
                                                                          continue;
                                                                   }
                                                                   if ((data_seg[index].match(/^CLEARRET($|;)/i))||
                                                                       (data_seg[index].match(/^CRET($|;)/i)) ||
                                                                       (data_seg[index].match(/^CLEARRETINA($|;)/i))||
                                                                       (data_seg[index].match(/^CRETINA($|;)/i)))  {
                                                                        $(".RETINA").val('');
                                                                        continue;
                                                                   }

                                                                   appendix=".a";
                                                                   var data = data_seg[index].match(/^(\w*)\:?(.*)/);
                                                                   (data[2].match(/\.a$/))?(data[2] = data[2].replace(/\.a$/,'')):(appendix = "nope");
                                                                   var field = data[1].toUpperCase();
                                                                   var text = data[2];
                                                                   text = expand_vocab(text);
                                                                   priors = process_kb(field,text,appendix,prior_field,prior_text);
                                                                   prior_field = priors['field'];
                                                                   prior_text = priors['prior_text'];

                                                                   }
                                                                   submit_form('2');
                                                                   $(this).val('');

                                                                   }
                                                                   });
                  $("[id^='sketch_tools_']").click(function() {
                                                   var zone = this.id.match(/sketch_tools_(.*)_/)[1];
                                                   $("[id^='sketch_tools_"+zone+"']").css("height","30px");
                                                   $(this).css("height","50px");
                                                   $("#sketch_tool_"+zone+"_color").css("background-color",$("#selColor_"+zone).val());
                                                   });
                  $("[id^='sketch_sizes_']").click(function() {
                                                   var zone = this.id.match(/sketch_sizes_(.*)_/)[1];
                                                   $("[id^='sketch_sizes_"+zone+"']").css("background","").css("border-bottom","");
                                                   $(this).css("border-bottom","2pt solid black");
                                                   });

                  //  Here we get CC1 to show
                  $(".tab_content").addClass('nodisplay');
                  $("#tab1_CC_text").removeClass('nodisplay');
                  $("#tab1_HPI_text").removeClass('nodisplay');
                  $("[id$='_CC'],[id$='_HPI_tab']").click(function() {
                                                          //  First remove class "active" from currently active tabs
                                                          $("[id$='_CC']").removeClass('active');
                                                          $("[id$='_HPI_tab']").removeClass('active');
                                                          //  Hide all tab content
                                                          $(".tab_content").addClass('nodisplay');
                                                          //  Here we get the href value of the selected tab
                                                          var selected_tab = $(this).find("a").attr("href");
                                                          //  Now add class "active" to the selected/clicked tab and content
                                                          $(selected_tab+"_CC").addClass('active');
                                                          $(selected_tab+"_CC_text").removeClass('nodisplay');
                                                          $(selected_tab+"_HPI_tab").addClass('active');
                                                          $(selected_tab+"_HPI_text").removeClass('nodisplay');
                                                          //  At the end, we add return false so that the click on the link is not executed
                                                          return false;
                                                          });
                  $("[id^='CONSTRUCTION_']").toggleClass('nodisplay');
                  $("input,textarea,text").css("background-color","#FFF8DC");
                  $("#IOPTIME").css("background-color","#FFFFFF");
                  $("#refraction_width").css("width","8.5in");
                  $(".Draw_class").addClass('nodisplay');
                  $(".PRIORS_class").addClass('nodisplay');
                  hide_DRAW();
                  hide_right();
                  $(window).resize(function() {
                                   if (window.innerWidth >'900') {
                                   $("#refraction_width").css("width","900px");
                                   $("#LayerVision2").css("padding","4px");
                                   }
                                   if (window.innerWidth >'1300') {
                                   $("#refraction_width").css("width","1300px");
                                   //$("#first").css("width","1300px");
                                   }
                                   if (window.innerWidth >'1900') {
                                   $("#refraction_width").css("width","1600px");
                                   }

                                   });
                  $(window).resize();

                  var hash_tag = '<i class="fa fa-minus"></i>';
                  var index;
                  // display any stored MOTILITY values
                  $("#MOTILITY_RS").value = parseInt($("#MOTILITY_RS").val());
                  if ($("#MOTILITY_RS").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_RS").val()); ++index) {
                  $("#MOTILITY_RS_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_RI").value = parseInt($("#MOTILITY_RI").val());
                  if ($("#MOTILITY_RI").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_RI").val()); ++index) {
                  $("#MOTILITY_RI_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_LS").value = parseInt($("#MOTILITY_LS").val());
                  if ($("#MOTILITY_LS").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_LS").val()); ++index) {
                  $("#MOTILITY_LS_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_LI").value = parseInt($("#MOTILITY_LI").val());
                  if ($("#MOTILITY_LI").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_LI").val()); ++index) {
                  $("#MOTILITY_LI_"+index).html(hash_tag);
                  }
                  }

                  $("#MOTILITY_RRSO").value = parseInt($("#MOTILITY_RRSO").val());
                  if ($("#MOTILITY_RRSO").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_RRSO").val()); ++index) {
                  $("#MOTILITY_RRSO_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_RRIO").value = parseInt($("#MOTILITY_RRIO").val());
                  if ($("#MOTILITY_RRIO").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_RRIO").val()); ++index) {
                  $("#MOTILITY_RRIO_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_RLIO").value = parseInt($("#MOTILITY_RLIO").val());
                  if ($("#MOTILITY_RLIO").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_RLIO").val()); ++index) {
                  $("#MOTILITY_RLIO_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_RLSO").value = parseInt($("#MOTILITY_RLSO").val());
                  if ($("#MOTILITY_RLSO").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_RLSO").val()); ++index) {
                  $("#MOTILITY_RLSO_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_LRSO").value = parseInt($("#MOTILITY_LRSO").val());
                  if ($("#MOTILITY_LRSO").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_LRSO").val()); ++index) {
                  $("#MOTILITY_LRSO_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_LRIO").value = parseInt($("#MOTILITY_LRIO").val());
                  if ($("#MOTILITY_LRIO").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_LRIO").val()); ++index) {
                  $("#MOTILITY_LRIO_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_LLSO").value = parseInt($("#MOTILITY_LLSO").val());
                  if ($("#MOTILITY_LLSO").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_LLSO").val()); ++index) {
                  $("#MOTILITY_LLSO_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_LLIO").value = parseInt($("#MOTILITY_LLIO").val());
                  if ($("#MOTILITY_LLIO").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_LLIO").val()); ++index) {
                  $("#MOTILITY_LLIO_"+index).html(hash_tag);
                  }
                  }

                  var hash_tag = '<i class="fa fa-minus rotate-left"></i>';
                  $("#MOTILITY_LR").value = parseInt($("#MOTILITY_LR").val());
                  if ($("#MOTILITY_LR").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_LR").val()); ++index) {
                  $("#MOTILITY_LR_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_LL").value = parseInt($("#MOTILITY_LL").val());
                  if ($("#MOTILITY_LL").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_LL").val()); ++index) {
                  $("#MOTILITY_LL_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_RR").value = parseInt($("#MOTILITY_RR").val());
                  if ($("#MOTILITY_RR").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_RR").val()); ++index) {
                  $("#MOTILITY_RR_"+index).html(hash_tag);
                  }
                  }
                  $("#MOTILITY_RL").value = parseInt($("#MOTILITY_RL").val());
                  if ($("#MOTILITY_RL").val() > '0') {
                  $("#MOTILITYNORMAL").removeAttr('checked');
                  for (index =1; index <= ($("#MOTILITY_RL").val()); ++index) {
                  $("#MOTILITY_RL_"+index).html(hash_tag);
                  }
                  }

                  $(".chronic_HPI,.count_HPI").blur(function() {
                                                    check_exam_detail();
                                                    });
                  // Dilation status
                  $("#DIL_RISKS").change(function(o) {
                                         ($(this).is(':checked')) ? ($(".DIL_RISKS").removeClass("nodisplay")) : ($(".DIL_RISKS").addClass("nodisplay"));
                                         check_exam_detail();
                                         });
                  $(".dil_drug").change(function(o) {
                                        if ($(this).is(':checked')) {
                                        //($(".DIL_RISKS").removeClass("nodisplay"));
                                        $("#DIL_RISKS").prop("checked","checked");
                                        check_exam_detail();
                                        }});

                  //neurosens exam = stereopsis + strab||NPC||NPA||etc
                  $(".neurosens,.neurosens2").blur(function() {
                                                   check_CPT_92060();
                                                   });
                  // END AUTO-CODING FEATURES

                  //  functions to improve flow of refraction input
                  $("input[name$='PRISM'],input[class^='prism']").blur(function() {
                                                                       //make it all caps
                                                                       var str = $(this).val();
                                                                       str = str.toUpperCase();
                                                                       $(this).val(str);
                                                                       });
                  $('input[class^="sphere"],input[name$="SPH"]').blur(function() {
                                                                      var mid = $(this).val();
                                                                      if (mid.match(/PLANO/i)) {
                                                                      $(this).val('PLANO');
                                                                      return;
                                                                      }
                                                                      if (mid.match(/^[\+\-]?\d{1}$/)) {
                                                                      mid = mid+".00";
                                                                      }
                                                                      if (mid.match(/\.[27]$/)) {
                                                                      mid = mid + '5';
                                                                      }
                                                                      if (mid.match(/\.\d$/)) {
                                                                      mid = mid + '0';
                                                                      }
                                                                      //if near is +2. make it +2.00
                                                                      if (mid.match(/\.$/)) {
                                                                      mid= mid + '00';
                                                                      }
                                                                      if ((!mid.match(/\./))&&(mid.match(00|25|50|75))) {
                                                                      var front = mid.match(/(\d{0,2})(00|25|50|75)/)[1];
                                                                      var back = mid.match(/(\d{0,2})(00|25|50|75)/)[2];
                                                                      if (front =='') front ='0';
                                                                      mid = front + "." + back;
                                                                      }
                                                                      if (!mid.match(/\./)) {
                                                                      var front = mid.match(/([\+\-]?\d{0,2})(\d{2})/)[1];
                                                                      var back  = mid.match(/(\d{0,2})(\d{2})/)[2];
                                                                      if (front =='') front ='0';
                                                                      if (front =='-') front ='-0';
                                                                      mid = front + "." + back;
                                                                      }
                                                                      if (!mid.match(/^(\+|\-){1}/)) {
                                                                      mid = "+" + mid;
                                                                      }
                                                                      $(this).val(mid);
                                                                      });

                  $("input[class^='presbyopia'],input[name$='ADD'],#ODADD_1,#ODADD_2,#OSADD_1,#OSADD_2").blur(function() {
                                                                                                              var add = $(this).val();
                                                                                                              add = add.replace(/=/g,"+");
                                                                                                              //if add is one digit, eg. 2, make it +2.00
                                                                                                              if (add.match(/^\d{1}$/)) {
                                                                                                              add = "+"+add+".00";
                                                                                                              }
                                                                                                              //if add is '+'one digit, eg. +2, make it +2.00
                                                                                                              if (add.match(/^\+\d{1}$/)) {
                                                                                                              add = add+".00";
                                                                                                              }
                                                                                                              //if add is 2.5 or 2.0 make it 2.50 or 2.00
                                                                                                              if (add.match(/\.[05]$/)) {
                                                                                                              add = add + '0';
                                                                                                              }
                                                                                                              //if add is 2.2 or 2.7 make it 2.25 or 2.75
                                                                                                              if (add.match(/\.[27]$/)) {
                                                                                                              add = add + '5';
                                                                                                              }
                                                                                                              //if add is +2. make it +2.00
                                                                                                              if (add.match(/\.$/)) {
                                                                                                              add = add + '00';
                                                                                                              }
                                                                                                              if ((!add.match(/\./))&&(add.match(/(0|25|50|75)$/))) {
                                                                                                              var front = add.match(/([\+]?\d{0,1})(00|25|50|75)/)[1];
                                                                                                              var back  = add.match(/([\+]?\d{0,1})(00|25|50|75)/)[2];
                                                                                                              if (front =='') front ='0';
                                                                                                              add = front + "." + back;
                                                                                                              }
                                                                                                              if (!add.match(/^(\+)/) && (add.length >  0)) {
                                                                                                              add= "+" + add;
                                                                                                              }
                                                                                                              $(this).val(add);
                                                                                                              if (this.id=="ODADD_1") $('#OSADD_1').val(add);
                                                                                                              if (this.id=="ODMIDADD_1") $('#OSMIDADD_1').val(add);
                                                                                                              if (this.id=="ODADD_2") $('#OSADD_2').val(add);
                                                                                                              if (this.id=="ODMIDADD_2") $('#OSMIDADD_2').val(add);
                                                                                                              if (this.id=="ODADD_3") $('#OSADD_3').val(add);
                                                                                                              if (this.id=="ODMIDADD_3") $('#OSMIDADD_3').val(add);
                                                                                                              if (this.id=="ODADD_4") $('#OSADD_4').val(add);
                                                                                                              if (this.id=="ODMIDADD_4") $('#OSMIDADD_4').val(add);
                                                                                                              if (this.id=="ODADD_5") $('#OSADD_5').val(add);
                                                                                                              if (this.id=="ODMIDADD_5") $('#OSMIDADD_5').val(add);
                                                                                                              if (this.id=="MRODADD") $('#MROSADD').val(add);
                                                                                                              if (this.id=="ARODADD") $('#AROSADD').val(add);
                                                                                                              if (this.id=="CTLODADD") $('#CTLOSADD').val(add);
                                                                                                              });

                  $("input[class^='axis'],input[name$='AXIS']").blur(function() {
                                                                     // Make this a 3 digit leading zeros number.
                                                                     // we are not translating text to numbers, just numbers to
                                                                     // a 3 digit format with leading zeroes as needed.
                                                                     // assume the end user KNOWS there are only numbers presented and
                                                                     // more than 3 digits is a mistake...
                                                                     // (although this may change with topography)
                                                                     var axis = $(this).val();
                                                                     var group = this.name.replace("AXIS", "CYL");;
                                                                     var cyl = $("#"+group).val();
                                                                     if ((cyl > '') && (cyl != 'SPH')) {
                                                                     if (!axis.match(/\d\d\d/)) {
                                                                     if (!axis.match(/\d\d/)) {
                                                                     if (!axis.match(/\d/)) {
                                                                     axis = '0';
                                                                     }
                                                                     axis = '0' + axis;
                                                                     }
                                                                     axis = '0' + axis;
                                                                     }
                                                                     } else {
                                                                     axis = '';
                                                                     }
                                                                     //we can utilize a phoropter dial feature, we can start them at their age appropriate with/against the rule value.
                                                                     //requires touch screen. requires complete touch interface development. Exists in refraction lanes. Would
                                                                     //be nice to tie them all together.  Would require manufacturers to publish their APIs to communicate with
                                                                     //the devices.
                                                                     $(this).val(axis);
                                                                     });
                  $("input[class^='cylinder'],input[name$='CYL']").blur(function() {
                                                                        var mid = $(this).val();
                                                                        var group = this.name.replace("CYL", "SPH");;
                                                                        var sphere = $("#"+group).val();
                                                                        if (((mid.length == 0) && (sphere.length >  0))||(mid.match(/sph/i))) {
                                                                        $(this).val('SPH');
                                                                        if (sphere.match(/plano/i)) $(this).val('');
                                                                        var axis = this.name.replace("CYL", "AXIS");
                                                                        $("#"+axis).val('');
                                                                        submit_form($(this));
                                                                        return;
                                                                        } else if (sphere.length >  0) {
                                                                        if (mid.match(/^[\+\-]?\d{1}$/)) {
                                                                        mid = mid+".00";
                                                                        }
                                                                        if (mid.match(/^(\d)(\d)$/)) {
                                                                        mid = mid[0] + '.' +mid[1];
                                                                        }

                                                                        //if mid is 2.5 or 2.0 make it 2.50 or 2.00
                                                                        if (mid.match(/\.[05]$/)) {
                                                                        mid = mid + '0';
                                                                        }
                                                                        //if mid is 2.2 or 2.7 make it 2.25 or 2.75
                                                                        if (mid.match(/\.[27]$/)) {
                                                                        mid = mid + '5';
                                                                        }
                                                                        //if mid is +2. make it +2.00
                                                                        if (mid.match(/\.$/)) {
                                                                        mid = mid + '00';
                                                                        }
                                                                        if (mid.match(/([\+\-]?\d{0,2})\.?(00|25|50|75)/)) {
                                                                        var front = mid.match(/([\+\-]?\d{0,2})\.?(00|25|50|75)/)[1];
                                                                        var back  = mid.match(/([\+\-]?\d{0,2})\.?(00|25|50|75)/)[2];
                                                                        if (front =='') front ='0';
                                                                        mid = front + "." + back;
                                                                        }
                                                                        if (!$('#PREFS_CYL').val()) {
                                                                        $('#PREFS_CYL').val('+');
                                                                        update_PREFS();
                                                                        }
                                                                        if (!mid.match(/^(\+|\-){1}/) && (sphere.length >  0)) {
                                                                        //no +/- sign at the start of the field.
                                                                        //ok so there is a preference set
                                                                        //Since it doesn't start with + or - then give it the preference value
                                                                        mid = $('#PREFS_CYL').val() + mid;
                                                                        } else if (mid.match(/^(\+|\-){1}/)) {
                                                                        pref = mid.match(/^(\+|\-){1}/)[0];
                                                                        //so they used a value + or - at the start of the field.
                                                                        //The only reason to work on this is to change to cylinder preference
                                                                        if ($('#PREFS_CYL').val() != pref){
                                                                        //and that is what they are doing here
                                                                        $('#PREFS_CYL').val(pref);
                                                                        update_PREFS();
                                                                        }
                                                                        }
                                                                        $(this).val(mid);
                                                                        }
                                                                        });
                  //bootstrap menu functions
                  $("[class='dropdown-toggle']").hover(function(){
                                                       $("[class='dropdown-toggle']").parent().removeClass('open');
                                                       var menuitem = this.id.match(/(.*)/)[1];
                                                       //if the menu is active through a prior click, show it
                                                       // Have to override Bootstrap then
                                                       if ($("#menustate").val() !="1") { //menu not active -> ignore
                                                       $("#"+menuitem).css("background-color", "#C9DBF2");
                                                       $("#"+menuitem).css("color","#000"); /*#262626;*/
                                                       } else { //menu is active -> respond
                                                       $("#"+menuitem).css("background-color", "#1C5ECF");
                                                       $("#"+menuitem).css("color","#fff"); /*#262626;*/
                                                       $("#"+menuitem).css("text-decoration","none");
                                                       $("#"+menuitem).parent().addClass('open');
                                                       }
                                                       },function() {
                                                       var menuitem = this.id.match(/(.*)/)[1];
                                                       $("#"+menuitem).css("color","#000"); /*#262626;*/
                                                       $("#"+menuitem).css("background-color", "#C9DBF2");
                                                       }
                                                       );
                  $("[class='dropdown-toggle']").click(function() {
                                                       $("#menustate").val('1');
                                                       var menuitem = this.id.match(/(.*)/)[1];
                                                       $("#"+menuitem).css("background-color", "#1C5ECF");
                                                       $("#"+menuitem).css("color","#fff"); /*#262626;*/
                                                       $("#"+menuitem).css("text-decoration","none");
                                                       });
                  $("#right-panel-link, #close-panel-bt,#right-panel-link_2").click(function() {
                                                                                    if ($("#PREFS_PANEL_RIGHT").val() =='1') {
                                                                                    $("#PREFS_PANEL_RIGHT").val('0');
                                                                                    } else {
                                                                                    $("#PREFS_PANEL_RIGHT").val('1');
                                                                                    }
                                                                                    update_PREFS();
                                                                                    });
                  $("[name^='menu_']").click(function() {
                                             $("[name^='menu_']").removeClass('active');
                                             var menuitem = this.id.match(/menu_(.*)/)[1];
                                             $(this).addClass('active');
                                             $("#menustate").val('1');
                                             menu_select(menuitem);
                                             });
                  // set display functions for Draw panel appearance
                  // for each DRAW area, if the value AREA_DRAW = 1, show it.
                  var zones = ["PMH","HPI","EXT","ANTSEG","RETINA","NEURO","IMPPLAN"];
                  for (index = '0'; index < zones.length; ++index) {
                  if ($("#PREFS_"+zones[index]+"_RIGHT").val() =='DRAW') {
                  show_DRAW_section(zones[index]);
                  } else if ($("#PREFS_"+zones[index]+"_RIGHT").val() =='QP') {
                  show_QP_section(zones[index]);
                  }
                  }
                  $("body").on("click","[name$='_text_view']" , function() {
                               var header = this.id.match(/(.*)_text_view$/)[1];
                               $("#"+header+"_text_list").toggleClass('wide_textarea');
                               $("#"+header+"_text_list").toggleClass('narrow_textarea');
                               $(this).toggleClass('fa-plus-square-o');
                               $(this).toggleClass('fa-minus-square-o');
                               if (header != /PRIOR/) {
                               var imagine = $("#PREFS_"+header+"_VIEW").val();
                               imagine ^= true;
                               $("#PREFS_"+header+"_VIEW").val(imagine);
                               update_PREFS();
                               }
                               return false;
                               });
                  $("body").on("change", "select", function(e){
                               if (this.name.match(/PRIOR_(.*)/)) {
                               var new_section = this.name.match(/PRIOR_(.*)/);
                               if (new_section[1] =='') return;
                               if (new_section[1] == /\_/){
                               return;
                               }
                               var newValue = this.value;
                               if (newValue == $("#form_id").val()) {
                               if (new_section[1] =="ALL") {
                                 //click updates prefs too
                                 $('#EXAM_QP').trigger("click");
                                    if ($('#PMH_right').height() > $('#PMH_left').height()) {
                                      $('#PMH_left').height($('#PMH_right').height());
                                      $('#PMH_1').height($('#PMH_right').height()+20);
                                    } else { $('#PMH_1').height($('#HPI_1').height()); }
                                 } else {
                                  $('#BUTTON_QP_'+new_section[1]).trigger("click");
                                 }
                                 $("#LayerTechnical_sections_1").css("clear","both");
                                 return;
                               }
                               //now go get the prior page via ajax
                               var newValue = this.value;
                               $("#PRIORS_"+ new_section[1] +"_left_text").removeClass('nodisplay');
                               $("#DRAWS_" + new_section[1] + "_right").addClass('nodisplay');
                               $("#QP_" + new_section[1]).addClass('nodisplay');

                               if (new_section[1] =="ALL") {
                               show_PRIORS();
                               show_PRIORS_section("ALL",newValue);
                               show_PRIORS_section("EXT",newValue);
                               show_PRIORS_section("ANTSEG",newValue);
                               show_PRIORS_section("RETINA",newValue);
                               show_PRIORS_section("NEURO",newValue);
                               show_PRIORS_section("IMPPLAN",newValue);
                               scrollTo("EXT_left");
                               } else {
                               show_PRIORS_section(new_section[1],newValue);
                               }
                               }
                               });
                  $("body").on("click","[id^='Close_PRIORS_']", function() {
                               var new_section = this.id.match(/Close_PRIORS_(.*)$/)[1];
                               $("#PRIORS_"+ new_section +"_left_text").addClass('nodisplay');
                               $("#QP_" + new_section).removeClass('nodisplay');
                               });
                  $("#pupils,#vision_tab,[name='CTL'],[name^='more_'],#ACTTRIGGER").mouseover(function() {
                                                                                              $(this).toggleClass('buttonRefraction_selected').toggleClass('underline').css( 'cursor', 'pointer' );
                                                                                              });
                  $("#pupils,#vision_tab,[name='CTL']").mouseout(function() {
                                                                 $(this).toggleClass('buttonRefraction_selected').toggleClass('underline');
                                                                 });
                  $("#pupils").click(function(){
                                     if ($("#dim_pupils_panel").hasClass("nodisplay")) {
                                        $("#dim_pupils_panel").removeClass('nodisplay');
                                      } else {
                                        $("#dim_pupils_panel").fadeToggle();
                                      }
                                     });
                  $("#vision_tab").click(function(){
                                         $("#REFRACTION_sections").toggleClass('nodisplay');
                                         ($("#PREFS_VA").val() =='1') ? ($("#PREFS_VA").val('0')) : $("#PREFS_VA").val('1');
                                         });
                  //set wearing to single vision or bifocal? Bifocal
                  $(".WNEAR").removeClass('nodisplay');
                  $("#WNEARODAXIS").addClass('nodisplay');
                  $("#WNEARODCYL").addClass('nodisplay');
                  $("#WNEARODPRISM").addClass('nodisplay');
                  $("#WNEAROSAXIS").addClass('nodisplay');
                  $("#WNEAROSCYL").addClass('nodisplay');
                  $("#WNEAROSPRISM").addClass('nodisplay');
                  $("#Single").click(function(){
                                     $("#WNEARODAXIS").addClass('nodisplay');
                                     $("#WNEARODCYL").addClass('nodisplay');
                                     $("#WNEARODPRISM").addClass('nodisplay');
                                     $("#WODADD2").addClass('nodisplay');
                                     $("#WOSADD2").addClass('nodisplay');
                                     $("#WNEAROSAXIS").addClass('nodisplay');
                                     $("#WNEAROSCYL").addClass('nodisplay');
                                     $("#WNEAROSPRISM").addClass('nodisplay');
                                     $(".WSPACER").removeClass('nodisplay');
                                     });
                  $("#Bifocal").click(function(){
                                      $(".WSPACER").addClass('nodisplay');
                                      $(".WNEAR").removeClass('nodisplay');
                                      $(".WMid").addClass('nodisplay');
                                      $(".WHIDECYL").removeClass('nodisplay');
                                      $("[name=RX]").val(["1"]);
                                      $("#WNEARODAXIS").addClass('nodisplay');
                                      $("#WNEARODCYL").addClass('nodisplay');
                                      $("#WNEARODPRISM").addClass('nodisplay');
                                      $("#WNEAROSAXIS").addClass('nodisplay');
                                      $("#WNEAROSCYL").addClass('nodisplay');
                                      $("#WNEAROSPRISM").addClass('nodisplay');
                                      $("#WODADD2").removeClass('nodisplay');
                                      $("#WOSADD2").removeClass('nodisplay');
                                      });
                  $("#Trifocal").click(function(){
                                       $(".WSPACER").addClass('nodisplay');
                                       $(".WNEAR").removeClass('nodisplay');
                                       $(".WMid").removeClass('nodisplay');
                                       $(".WHIDECYL").addClass('nodisplay');
                                       $("[name=RX]").val(["2"]);
                                       $("#WNEARODAXIS").addClass('nodisplay');
                                       $("#WNEARODCYL").addClass('nodisplay');
                                       $("#WNEARODPRISM").addClass('nodisplay');
                                       $("#WNEAROSAXIS").addClass('nodisplay');
                                       $("#WNEAROSCYL").addClass('nodisplay');
                                       $("#WNEAROSPRISM").addClass('nodisplay');
                                       $("#WODADD2").removeClass('nodisplay');
                                       $("#WOSADD2").removeClass('nodisplay');
                                       });
                  $("#Progressive").click(function(){
                                          $(".WSPACER").addClass('nodisplay');
                                          $(".WNEAR").removeClass('nodisplay');
                                          $(".WMid").addClass('nodisplay');
                                          $(".WHIDECYL").removeClass('nodisplay');
                                          $("[name=RX]").val(["3"]);
                                          $("#WNEARODAXIS").addClass('nodisplay');
                                          $("#WNEARODCYL").addClass('nodisplay');
                                          $("#WNEARODPRISM").addClass('nodisplay');
                                          $("#WNEAROSAXIS").addClass('nodisplay');
                                          $("#WNEAROSCYL").addClass('nodisplay');
                                          $("#WNEAROSPRISM").addClass('nodisplay');
                                          $("#WODADD2").removeClass('nodisplay');
                                          $("#WOSADD2").removeClass('nodisplay');
                                          });
                  $("[name=W_width_display]").click(function() {
                                                    if ($("#PREFS_W_width").val() !="1") {
                                                    $("#PREFS_W_width").val('1');
                                                    //make each display W wide
                                                    $("[name=currentRX]").addClass('refraction_wide');
                                                    $("[name=W_wide]").removeClass('nodisplay');
                                                    $("[name=W_wide2]").removeClass('nodisplay');
                                                    } else {
                                                    $("#PREFS_W_width").val('0');
                                                    //make each display W narrow
                                                    $("[name=currentRX]").removeClass('refraction_wide');
                                                    $("[name=W_wide]").addClass('nodisplay');
                                                    $("[name=W_wide2]").addClass('nodisplay');
                                                    }
                                                    update_PREFS();

                                                    });
                  if ($("#PREFS_W_width").val() == '1') {
                    $("[name=W_wide]").removeClass('nodisplay');
                    $("[name=W_wide2]").removeClass('nodisplay')
                  } else {
                    $("[name=W_wide]").addClass('nodisplay');
                    $("[name=W_wide2]").addClass('nodisplay');
                  }
                  $("#Amsler-Normal").change(function() {
                                             if ($(this).is(':checked')) {
                                             var number1 = document.getElementById("AmslerOD").src.match(/(Amsler_\d)/)[1];
                                             document.getElementById("AmslerOD").src = document.getElementById("AmslerOD").src.replace(number1,"Amsler_0");
                                             var number2 = document.getElementById("AmslerOS").src.match(/(Amsler_\d)/)[1];
                                             document.getElementById("AmslerOS").src = document.getElementById("AmslerOS").src.replace(number2,"Amsler_0");
                                             $("#AMSLEROD").val("0");
                                             $("#AMSLEROS").val("0");
                                             $("#AmslerODvalue").text("0");
                                             $("#AmslerOSvalue").text("0");
                                             submit_form("eye_mag");
                                             return;
                                             }
                                             });
                  $("#PUPIL_NORMAL").change(function() {
                                            if ($(this).is(':checked')) {
                                            $("#ODPUPILSIZE1").val('3.0');
                                            $("#OSPUPILSIZE1").val('3.0');
                                            $("#ODPUPILSIZE2").val('2.0');
                                            $("#OSPUPILSIZE2").val('2.0');
                                            $("#ODPUPILREACTIVITY").val('+2');
                                            $("#OSPUPILREACTIVITY").val('+2');
                                            $("#ODAPD").val('0');
                                            $("#OSAPD").val('0');
                                            submit_form("eye_mag");
                                            return;
                                            }
                                            });
                  $("[name$='PUPILREACTIVITY']").change(function() {
                                                        var react = $(this).val();
                                                        if (react.match(/^\d{1}$/)) {
                                                        react = "+"+react;
                                                        }
                                                        $(this).val(react);
                                                        });

                  $("[name^='EXAM']").mouseover(function(){
                                                $(this).toggleClass("borderShadow2").css( 'cursor', 'pointer' );
                                                });
                  $("[name^='EXAM']").mouseout(function(){
                                               $(this).toggleClass("borderShadow2");
                                               });
                  $("#AmslerOD, #AmslerOS").click(function() {
                                                  if ($('#chart_status').val() !="on") return;
                                                  var number1 = this.src.match(/Amsler_(\d)/)[1];
                                                  var number2 = +number1 +1;
                                                  this.src = this.src.replace('Amsler_'+number1,'Amsler_'+number2);
                                                  this.src = this.src.replace('Amsler_6','Amsler_0');
                                                  $("#Amsler-Normal").removeAttr('checked');
                                                  var number3 = this.src.match(/Amsler_(\d)/)[1];
                                                  this.html =  number3;
                                                  if (number3 =="6") {
                                                  number3 = "0";
                                                  }
                                                  if ($(this).attr("id")=="AmslerOD") {
                                                  $("#AmslerODvalue").text(number3);
                                                  $('#AMSLEROD').val(number3);
                                                  } else {
                                                  $('#AMSLEROS').val(number3);
                                                  $("#AmslerOSvalue").text(number3);
                                                  }
                                                  var title = "#"+$(this).attr("id")+"_tag";
                                                  });

                  $("#AmslerOD, #AmslerOS").mouseout(function() {
                                                     submit_form("eye_mag");
                                                     });
                  $("[name^='ODVF'],[name^='OSVF']").click(function() {
                                                           if ($(this).is(':checked') == true) {
                                                           $("#FieldsNormal").prop('checked', false);
                                                           $(this).val('1');
                                                           }else{
                                                           $(this).val('0');
                                                           $(this).prop('checked', false);
                                                           }
                                                           submit_form("eye_mag");
                                                           });
                  $("#FieldsNormal").click(function() {
                                           if ($(this).is(':checked')) {
                                           $("#ODVF1").removeAttr('checked');
                                           $("#ODVF2").removeAttr('checked');
                                           $("#ODVF3").removeAttr('checked');
                                           $("#ODVF4").removeAttr('checked');
                                           $("#OSVF1").removeAttr('checked');
                                           $("#OSVF2").removeAttr('checked');
                                           $("#OSVF3").removeAttr('checked');
                                           $("#OSVF4").removeAttr('checked');
                                           }
                                           });
                  $("[id^='EXT_prefix']").change(function() {
                                                 var newValue =$('#EXT_prefix').val();
                                                 newValue = newValue.replace('+', '');
                                                 if (newValue =="off") {$(this).val('');}
                                                 if (newValue =="clear") {
                                                 if (confirm('\tSelect OK to clear all the External Exam values\t\n\t or CANCEL to continue.\t')) {
                                                 $(this).val('');
                                                 $(".EXT").val('');
                                                 }
                                                 } else {
                                                 $("[name^='EXT_prefix_']").removeClass('eye_button_selected');
                                                 $("#EXT_prefix_"+ newValue).addClass("eye_button_selected");
                                                 }
                                                 });
                  $("#ANTSEG_prefix").change(function() {
                                             var newValue = $(this).val().replace('+', '');
                                             if ($(this).value =="off") {$(this).val('');}
                                             if (newValue =="clear") {
                                             if (confirm('\tSelect OK to clear all the Anterior Segment Exam values\t\n\t or CANCEL to continue.\t')) {
                                             $(this).val('');
                                             $(".ANTSEG").val('');
                                             }
                                             } else {
                                             $("[name^='ANTSEG_prefix_']").removeClass('eye_button_selected');
                                             $("#ANTSEG_prefix_"+ newValue).addClass("eye_button_selected");
                                             }
                                             });
                  $("#RETINA_prefix").change(function() {
                                             var newValue = $("#RETINA_prefix").val().replace('+', '');
                                             if ($(this).value =="off") {$(this).val('');}
                                             if (newValue =="clear") {
                                             if (confirm('\tSelect OK to clear all the Retina Exam values\t\n\t or CANCEL to continue.\t')) {
                                             $(this).val('');
                                             $(".RETINA").val('');
                                             }
                                             } else {
                                             $("[name^='RETINA_prefix_']").removeClass('eye_button_selected');
                                             $("#RETINA_prefix_"+ newValue).addClass("eye_button_selected");
                                             }
                                             });
                  $("#NEURO_ACT_zone").change(function() {
                                              var newValue = $(this).val();
                                              $("[name^='NEURO_ACT_zone']").removeClass('eye_button_selected');
                                              $("#NEURO_ACT_zone_"+ newValue).addClass("eye_button_selected");
                                              $("#PREFS_ACT_SHOW").val(newValue);
                                              update_PREFS;
                                              $("#ACT_tab_"+newValue).trigger('click');
                                              });
                  $("#NEURO_side").change(function() {
                                          var newValue = $(this).val();
                                          $("[name^='NEURO_side']").removeClass('eye_button_selected');
                                          $("#NEURO_side_"+ newValue).addClass("eye_button_selected");
                                          });
                  $('.ACT').focus(function() {
                                  var id = this.id.match(/ACT(\d*)/);
                                  $('#NEURO_field').val(''+id[1]).trigger('change');
                                  });
                  $("#NEURO_field").change(function() {
                                           var newValue = $(this).val();
                                           $("[name^='NEURO_field']").removeClass('eye_button_selected');
                                           $("#NEURO_field_"+ newValue).addClass("eye_button_selected");
                                           $('.ACT').each(function(i){
                                                          var color = $(this).css('background-color');
                                                          if ((color == 'rgb(255, 255, 153)')) {// =='blue' <- IE hack
                                                          $(this).css("background-color","red");
                                                          }
                                                          });
                                           //change to highlight field in zone entry is for
                                           var zone = $("#NEURO_ACT_zone").val();
                                           $("#ACT"+newValue+zone).css("background-color","yellow");
                                           });
                  $("[name^='NEURO_ACT_strab']").click(function() {
                                                       var newValue = $(this).val();
                                                       $("[name^='NEURO_ACT_strab']").removeClass('eye_button_selected');
                                                       $(this).addClass("eye_button_selected");
                                                       });
                  $("#NEURO_value").change(function() {
                                           var newValue = $(this).val();
                                           $("[name^='NEURO_value']").removeClass('eye_button_selected');
                                           $("#NEURO_value_"+ newValue).addClass("eye_button_selected");
                                           if (newValue == "ortho") {
                                           $("#NEURO_ACT_strab").val('');
                                           $("[name^='NEURO_ACT_strab']").removeClass('eye_button_selected');
                                           $("#NEURO_side").val('');
                                           $("[name^='NEURO_side']").removeClass('eye_button_selected');
                                           }
                                           });
                  $("#NEURO_RECORD").mouseover(function() {
                                               $("#NEURO_RECORD").addClass('borderShadow2').css( 'cursor', 'pointer' );
                                               });
                  $("#NEURO_RECORD").mouseout(function() {
                                              $("#NEURO_RECORD").removeClass('borderShadow2');
                                              });
                  $("#NEURO_RECORD").mousedown(function() {
                                               $("#NEURO_RECORD").removeClass('borderShadow2');
                                               $(this).toggleClass('button_over');
                                               });
                  $("#NEURO_RECORD").mouseup(function() {
                                             $("#NEURO_RECORD").removeClass('borderShadow2');
                                             $(this).toggleClass('button_over');
                                             });
                  $("#NEURO_RECORD").click(function() {
                                           //find out the field we are updating
                                           var number = $("#NEURO_field").val();
                                           var zone = $("#NEURO_ACT_zone").val();
                                           var strab = $("#NEURO_value").val() + ' '+ $("#NEURO_side").val() + $("#NEURO_ACT_strab").val();

                                           $("#ACT"+number+zone).val(strab).css("background-color","#F0F8FF");


                                           });

                  $("#LayerMood,#LayerVision, #LayerTension, #LayerMotility, #LayerAmsler, #LayerFields, #LayerPupils,#dim_pupils_panel,#PRIORS_ALL_left_text").mouseover(function(){
                                                                                                                                                                          $(this).addClass("borderShadow2");
                                                                                                                                                                          });
                  $("#LayerMood,#LayerVision, #LayerTension, #LayerMotility, #LayerAmsler, #LayerFields, #LayerPupils,#dim_pupils_panel,#PRIORS_ALL_left_text").mouseout(function(){
                                                                                                                                                                         $(this).removeClass("borderShadow2");
                                                                                                                                                                         });
                  $("[id$='_lightswitch']").click(function() {
                                                  var section = "#"+this.id.match(/(.*)_lightswitch$/)[1];
                                                  var section2 = this.id.match(/(.*)_(.*)_lightswitch$/)[2];
                                                  var elem = document.getElementById("PREFS_"+section2);
                                                  $("#PREFS_VA").val('0');
                                                  if (section2 != "IOP")$("#REFRACTION_sections").removeClass('nodisplay');
                                                  if (elem.value == "0" || elem.value =='') {
                                                  elem.value='1';
                                                  if (section2 =="ADDITIONAL") {
                                                  $("#LayerVision_ADDITIONAL").removeClass('nodisplay');
                                                  }
                                                  if (section2 =="IOP") {
                                                  $("#LayerVision_IOP").removeClass('nodisplay');
                                                  //plot_IOPs();
                                                  }
                                                  $(section).removeClass('nodisplay');
                                                  $(this).addClass("buttonRefraction_selected");
                                                  } else {
                                                  elem.value='0';
                                                  $(section).addClass('nodisplay');
                                                  if (section2 =="VAX") {
                                                  $("#LayerVision_ADDITIONAL_VISION").addClass('nodisplay');
                                                  }
                                                  if (section2 =="IOP") {
                                                  $("#LayerVision_IOP").addClass('nodisplay');
                                                  }
                                                  $(this).removeClass("buttonRefraction_selected");
                                                  }
                                                  $(this).css( 'cursor', 'pointer' );
                                                  update_PREFS();
                                                  });

                  $('[id$=_lightswitch]').mouseover(function() {
                                                    $(this).addClass('buttonRefraction_selected').css( 'cursor', 'pointer' );

                                                    var section = this.id.match(/(.*)_(.*)_lightswitch$/)[2];
                                                    if (section == 'IOP') {
                                                    $("#LayerTension").addClass("borderShadow2");
                                                    } else {
                                                    $("#LayerVision").addClass("borderShadow2");
                                                    }
                                                    });
                  $('[id$=_lightswitch]').mouseout(function() {
                                                   var section2 = this.id.match(/(.*)_(.*)_lightswitch$/)[2];
                                                   var elem = document.getElementById("PREFS_"+section2);

                                                   if (elem.value != "1") {
                                                   $(this).removeClass('buttonRefraction_selected');
                                                   } else {
                                                   $(this).addClass('buttonRefraction_selected');
                                                   }                                                                });

                  // let users enter "=" sign for "+" to cut down on keyboard movements (keyCode 61)
                  // "+" == "shift" + "=" ==> now "=" == "+", "j" ==> "J" for Jaeger acuity (keyCode 74)
                  // "-" is still == "-"
                  $("input[class^='jaeger'],input[name$='VA'],input[name$='VA_copy'],input[name$='VA_copy_brd'],input[name$='SPH'],input[name$='CYL'],input[name$='REACTIVITY'],input[name$='APD']").on('keyup', function(e) {
                                                                                                                                                                                                        if (e.keyCode=='61' || e.keyCode=='74') {
                                                                                                                                                                                                        now = $(this).val();
                                                                                                                                                                                                        now = now.replace(/=/g,"+").replace(/^j/g,"J");
                                                                                                                                                                                                        $(this).val(now);
                                                                                                                                                                                                        }
                                                                                                                                                                                                        });
                  //useful to make all VA fields stay in sync
                  $("input[name$='VA']").on('change',function() {
                                            var hereValue = $(this).val();
                                            var newValue = $(this).attr('name').replace('VA', 'VA_copy');
                                            $("#" + newValue).val(hereValue).css("background-color","#F0F8FF");;
                                            $("#" + newValue + "_brd").val(hereValue).css("background-color","#F0F8FF");;
                                            });
                  $("input[class^='jaeger'],input[name$='VA_1']").on('change',function() {
                                                                     var hereValue = $(this).val();
                                                                     hereValue = hereValue.replace(/=$/g,"+").replace(/^j/g,"J");
                                                                     $(this).val(hereValue);
                                                                     if (this.name.match(/_1$/)) {
                                                                     var newValue = $(this).attr('name').replace('VA_1', 'VA_1_copy');
                                                                     $("#" + newValue).val(hereValue).css("background-color","#F0F8FF");;
                                                                     $("#" + newValue + "_brd").val(hereValue).css("background-color","#F0F8FF");
                                                                     }
                                                                     });

                  $("input[name$='_copy']").blur(function() {
                                                 var hereValue = $(this).val();
                                                 var newValue = $(this).attr('name').replace('_copy', '');
                                                 $("#" + newValue).val(hereValue).css("background-color","#F0F8FF");;
                                                 $("#" + newValue + "_copy_brd").val(hereValue).css("background-color","#F0F8FF");;
                                                 });
                  $("input[name$='_copy_brd']").change(function() {
                                                       var hereValue = $(this).val();
                                                       var newValue = $(this).attr('name').replace('_copy_brd', '');
                                                       $("#" + newValue).val(hereValue).css("background-color","#F0F8FF");;
                                                       $("#" + newValue + "_copy").val(hereValue).css("background-color","#F0F8FF");;
                                                       });
                  $("[name^='more_']").mouseout(function() {
                                                $(this).toggleClass('buttonRefraction_selected').toggleClass('underline');
                                                });
                  $("[name^='more_']").click(function() {
                                             $("#Visions_A").toggleClass('nodisplay');
                                             $("#Visions_B").toggleClass('nodisplay');
                                             });
                  $("#EXAM_defaults").click(function() {
                                            <?php
                                            // This query is specific to the provider.
                                            $query  = "select seq from list_options where option_id=?";
                                            $result = sqlStatement($query,array("Eye_defaults_$providerID"));

                                            $list = sqlFetchArray($result);
                                            $SEQ = $list['seq'];
                                            if (!$SEQ) {
                                              // If there is no list for this provider, we create it here.
                                              // Instead of the below code, we should be copying the Eye_Defaults_for_GENERAL
                                              // to Eye_defaults_$providerID
                                              // This list is part of the idea to create a way to add Eye_defaults_$providerID specific to the
                                              // subspecialty of the doctor. ie. Eye_defaults_for_GENERAL (the only one that exists today)
                                              // or Eye_defaults_for_CORNEA, RETINA, NEURO, PLASTICS, REFRACTIVE, PEDS, UVEITIS
                                              // Also, each field should be "display:none" if desired, via another user specific list.
                                              // This would be another list.  Let's see if the public likes the form itself before
                                              // developing these features...
                                            $query = "SELECT max(seq) as maxseq FROM list_options WHERE list_id= 'lists'";
                                            $pres = sqlStatement($query);
                                            $maxseq = sqlFetchArray($pres);

                                            $seq=$maxseq['maxseq'];
                                            $query = "INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`) VALUES
                                            ('lists', ?, ?, ?, '1', '0', '', '', '')";
                                            sqlStatement($query,array("Eye_defaults_$providerID","Eye Exam Defaults $providerNAME ",$seq));
                                            $query = "INSERT INTO `list_options` (`list_id`, `option_id`, `title`,`notes`,`seq`) VALUES
                                            ('Eye_defaults_".$providerID."','RUL','normal lids and lashes','EXT','10'),
                                            ('Eye_defaults_".$providerID."','LUL','normal lids and lashes','EXT','20'),
                                            ('Eye_defaults_".$providerID."','RLL','good tone','EXT','30'),
                                            ('Eye_defaults_".$providerID."','LLL','good tone','EXT','40'),
                                            ('Eye_defaults_".$providerID."','RBROW','no brow ptosis','EXT','50'),
                                            ('Eye_defaults_".$providerID."','LBROW','no brow ptosis','EXT','60'),
                                            ('Eye_defaults_".$providerID."','RMCT','no masses','EXT','70'),
                                            ('Eye_defaults_".$providerID."','LMCT','no masses','EXT','80'),
                                            ('Eye_defaults_".$providerID."','RADNEXA','normal lacrimal gland and orbit','EXT','90'),
                                            ('Eye_defaults_".$providerID."','LADNEXA','normal lacrimal gland and orbit','EXT','100'),
                                            ('Eye_defaults_".$providerID."','RMRD','+3','EXT','110'),
                                            ('Eye_defaults_".$providerID."','LMRD','+3','EXT','120'),
                                            ('Eye_defaults_".$providerID."','RLF','17','EXT','130'),
                                            ('Eye_defaults_".$providerID."','LLF','17','EXT','140'),
                                            ('Eye_defaults_".$providerID."','OSCONJ','quiet','ANTSEG','150'),
                                            ('Eye_defaults_".$providerID."','ODCONJ','quiet','ANTSEG','160'),
                                            ('Eye_defaults_".$providerID."','ODCORNEA','clear','ANTSEG','170'),
                                            ('Eye_defaults_".$providerID."','OSCORNEA','clear','ANTSEG','180'),
                                            ('Eye_defaults_".$providerID."','ODAC','deep and quiet','ANTSEG','190'),
                                            ('Eye_defaults_".$providerID."','OSAC','deep and quiet','ANTSEG','200'),
                                            ('Eye_defaults_".$providerID."','ODLENS','clear','ANTSEG','210'),
                                            ('Eye_defaults_".$providerID."','OSLENS','clear','ANTSEG','220'),
                                            ('Eye_defaults_".$providerID."','ODIRIS','round','ANTSEG','230'),
                                            ('Eye_defaults_".$providerID."','OSIRIS','round','ANTSEG','240'),
                                            ('Eye_defaults_".$providerID."','ODPUPILSIZE1','3','NEURO','250'),
                                            ('Eye_defaults_".$providerID."','ODPUPILSIZE2','2','NEURO','260'),
                                            ('Eye_defaults_".$providerID."','ODPUPILREACTIVITY','+2','NEURO','270'),
                                            ('Eye_defaults_".$providerID."','ODAPD','0','NEURO','280'),
                                            ('Eye_defaults_".$providerID."','OSPUPILSIZE1','3','NEURO','290'),
                                            ('Eye_defaults_".$providerID."','OSPUPILSIZE2','2','NEURO','300'),
                                            ('Eye_defaults_".$providerID."','OSPUPILREACTIVITY','+2','NEURO','310'),
                                            ('Eye_defaults_".$providerID."','OSAPD','0','NEURO','320'),
                                            ('Eye_defaults_".$providerID."','ODVFCONFRONTATION1','0','NEURO','330'),
                                            ('Eye_defaults_".$providerID."','ODVFCONFRONTATION2','0','NEURO','340'),
                                            ('Eye_defaults_".$providerID."','ODVFCONFRONTATION3','0','NEURO','350'),
                                            ('Eye_defaults_".$providerID."','ODVFCONFRONTATION4','0','NEURO','360'),
                                            ('Eye_defaults_".$providerID."','ODVFCONFRONTATION5','0','NEURO','370'),
                                            ('Eye_defaults_".$providerID."','OSVFCONFRONTATION1','0','NEURO','380'),
                                            ('Eye_defaults_".$providerID."','OSVFCONFRONTATION2','0','NEURO','390'),
                                            ('Eye_defaults_".$providerID."','OSVFCONFRONTATION3','0','NEURO','400'),
                                            ('Eye_defaults_".$providerID."','OSVFCONFRONTATION4','0','NEURO','410'),
                                            ('Eye_defaults_".$providerID."','OSVFCONFRONTATION5','0','NEURO','420'),
                                            ('Eye_defaults_".$providerID."','ODDISC','pink','RETINA','430'),
                                            ('Eye_defaults_".$providerID."','OSDISC','pink','RETINA','440'),
                                            ('Eye_defaults_".$providerID."','ODCUP','0.3','RETINA','450'),
                                            ('Eye_defaults_".$providerID."','OSCUP','0.3','RETINA','460'),
                                            ('Eye_defaults_".$providerID."','ODMACULA','flat','RETINA','470'),
                                            ('Eye_defaults_".$providerID."','OSMACULA','flat','RETINA','480'),
                                            ('Eye_defaults_".$providerID."','ODVESSELS','2:3','RETINA','490'),
                                            ('Eye_defaults_".$providerID."','OSVESSELS','2:3','RETINA','500'),
                                            ('Eye_defaults_".$providerID."','ODPERIPH','flat','RETINA','510'),
                                            ('Eye_defaults_".$providerID."','OSPERIPH','flat','RETINA','520')";
                                            sqlStatement($query);
                                            }
                                            $query = "select * from list_options where list_id =? and activity='1' order by seq";

                                            $DEFAULT_data =sqlStatement($query,array("Eye_defaults_$providerID"));
                                            while ($row = sqlFetchArray($DEFAULT_data)) {
                                            //$row['notes'] is the clinical zone (EXT,ANTSEG,RETINA,NEURO)
                                            //$row['option_id'] is the field name
                                            //$row['title'] is the default value to use for this provider
                                            ${$row[notes]}[$row[option_id]] = $row[title]; //This builds each clinical section into its own array (used below)
                                            echo '$("#'.$row['option_id'].'").val("'.$row['title'].'").css("background-color","beige");
                                            ';
                                            }
                                            ?>
                                            submit_form("eye_mag");
                                            });

                  $("#EXT_defaults").click(function() {
                                           <?php
                                           foreach ($EXT as $item => $value) {
                                           echo '$("#'.$item.'").val("'.$value.'").css("background-color","beige");
                                           ';
                                           }
                                           ?>
                                           submit_form("eye_mag");
                                           });

                  $("#ANTSEG_defaults").click(function() {
                                              <?php
                                              foreach ($ANTSEG as $item => $value) {
                                              echo '$("#'.$item.'").val("'.$value.'").css("background-color","beige");
                                              ';
                                              }
                                              ?>
                                              submit_form("eye_mag");
                                              });
                  $("#RETINA_defaults").click(function() {
                                              <?php
                                              foreach ($RETINA as $item => $value) {
                                              echo '$("#'.$item.'").val("'.$value.'").css("background-color","beige");
                                              ';
                                              }
                                              ?>
                                              submit_form("eye_mag");
                                              });
                  $("#NEURO_defaults").click(function() {
                                             <?php
                                             foreach ($NEURO as $item => $value) {
                                             echo '$("#'.$item.'").val("'.$value.'").css("background-color","beige");
                                             ';
                                             }
                                             ?>
                                             submit_form("eye_mag");
                                             });


                  $("#MOTILITYNORMAL").click(function() {
                                             $("#MOTILITY_RS").val('0');
                                             $("#MOTILITY_RI").val('0');
                                             $("#MOTILITY_RR").val('0');
                                             $("#MOTILITY_RL").val('0');
                                             $("#MOTILITY_LS").val('0');
                                             $("#MOTILITY_LI").val('0');
                                             $("#MOTILITY_LR").val('0');
                                             $("#MOTILITY_LL").val('0');

                                             $("#MOTILITY_RRSO").val('0');
                                             $("#MOTILITY_RRIO").val('0');
                                             $("#MOTILITY_RLSO").val('0');
                                             $("#MOTILITY_RLIO").val('0');
                                             $("#MOTILITY_LRSO").val('0');
                                             $("#MOTILITY_LRIO").val('0');
                                             $("#MOTILITY_LLSO").val('0');
                                             $("#MOTILITY_LLIO").val('0');

                                             for (index = '0'; index < 5; ++index) {
                                             $("#MOTILITY_RS_"+index).html('');
                                             $("#MOTILITY_RI_"+index).html('');
                                             $("#MOTILITY_RR_"+index).html('');
                                             $("#MOTILITY_RL_"+index).html('');
                                             $("#MOTILITY_LS_"+index).html('');
                                             $("#MOTILITY_LI_"+index).html('');
                                             $("#MOTILITY_LR_"+index).html('');
                                             $("#MOTILITY_LL_"+index).html('');

                                             $("#MOTILITY_RRSO_"+index).html('');
                                             $("#MOTILITY_RRIO_"+index).html('');
                                             $("#MOTILITY_RLSO_"+index).html('');
                                             $("#MOTILITY_RLIO_"+index).html('');
                                             $("#MOTILITY_LRSO_"+index).html('');
                                             $("#MOTILITY_LRIO_"+index).html('');
                                             $("#MOTILITY_LLSO_"+index).html('');
                                             $("#MOTILITY_LLIO_"+index).html('');
                                             }
                                             submit_form('eye_mag');
                                             });

                  $("[name^='MOTILITY_']").click(function()  {
                                                 $("#MOTILITYNORMAL").removeAttr('checked');

                                                 if (this.id.match(/(MOTILITY_([A-Z]{4}))_(.)/)) {
                                                 var zone = this.id.match(/(MOTILITY_([A-Z]{4}))_(.)/);
                                                 var index   = '0';
                                                 var valued = isNaN($("#"+zone[1]).val());
                                                 if ((zone[2] =='RLSO')||(zone[2] =='LLSO')||(zone[2] =='RRIO')||(zone[2] =='LRIO')) {
                                                 //find or make a hash tage for "\"
                                                 var hash_tag = '<i class="fa fa-minus"></i>';
                                                 } else {
                                                 //find or make a hash tage for "/"
                                                 var hash_tag = '<i class="fa fa-minus"></i>';
                                                 }
                                                 } else {
                                                 var zone = this.id.match(/(MOTILITY_..)_(.)/);
                                                 var section = this.id.match(/MOTILITY_(.)(.)_/);
                                                 var section2 = section[2];
                                                 var Eye = section[1];
                                                 var SupInf = section2.search(/S|I/);
                                                 var RorLside   = section2.search(/R|L/);


                                                 if (RorLside =='0') {
                                                 var hash_tag = '<i class="fa fa-minus rotate-left"></i>';
                                                 } else {
                                                 var hash_tag = '<i class="fa fa-minus"></i>';
                                                 }
                                                 }
                                                 if (valued != true && $("#"+zone[1]).val() <'4') {
                                                 valued=$("#"+zone[1]).val();
                                                 valued++;
                                                 } else {
                                                 valued = '0';
                                                 $("#"+zone[1]).val('0');
                                                 }

                                                 $("#"+zone[1]).val(valued);

                                                 for (index = '0'; index < 5; ++index) {
                                                 $("#"+zone[1]+"_"+index).html('');
                                                 }
                                                 if (valued > '0') {
                                                 for (index =1; index < (valued+1); ++index) {
                                                 $("#"+zone[1]+"_"+index).html(hash_tag);
                                                 }
                                                 }

                                                 submit_form('3');
                                                 });

                  $("[name^='Close_']").click(function()  {
                                              var section = this.id.match(/Close_(.*)$/)[1];
                                              if (this.id.match(/Close_W_(.*)$/) != null) {
                                              var W_section = this.id.match(/Close_W_(.*)$/)[1];
                                              if (W_section > '1') {
                                              $('#LayerVision_W_'+W_section).addClass('nodisplay');
                                              $('[name$=SPH_'+W_section+']').val('');
                                              $('[name$=CYL_'+W_section+']').val('');
                                              $('[name$=AXIS_'+W_section+']').val('');
                                              $('[name$=ADD_'+W_section+']').val('');
                                              $('[name$=PRISM_'+W_section+']').val('');
                                              $('[name$=VA_'+W_section+']').val('');
                                              $('#RX_TYPE_'+W_section).val('');
                                              $('#Add_Glasses').removeClass('nodisplay');
                                              $('#W_'+W_section).val('');
                                              submit_form('4');
                                              } else {
                                              $("#LayerVision_W_lightswitch").click();
                                              }
                                              } else if (section =="ACTMAIN") {
                                              $("#ACTTRIGGER").trigger( "click" );
                                              } else {
                                              $("#LayerVision_"+section+"_lightswitch").click();
                                              }
                                              });


                  $("#EXAM_DRAW, #BUTTON_DRAW_menu, #PANEL_DRAW").click(function() {
                                                                        if ($("#PREFS_CLINICAL").value !='0') {
                                                                        show_right();
                                                                        $("#PREFS_CLINICAL").val('0');
                                                                        update_PREFS();
                                                                        }
                                                                        if ($("#PREFS_EXAM").val() != 'DRAW') {
                                                                        $("#PREFS_EXAM").val('DRAW');
                                                                        $("#EXAM_QP").removeClass('button_selected');
                                                                        $("#EXAM_DRAW").addClass('button_selected');
                                                                        $("#EXAM_TEXT").removeClass('button_selected');
                                                                        update_PREFS();
                                                                        }
                                                                        show_DRAW();
                                                                        });
                  $("#EXAM_QP,#PANEL_QP").click(function() {
                                                if ($("#PREFS_CLINICAL").value !='0') {
                                                $("#PREFS_CLINICAL").val('0');
                                                update_PREFS();
                                                }
                                                if ($("#PREFS_EXAM").value != 'QP') {
                                                $("#PREFS_EXAM").val('QP');
                                                $("#EXAM_QP").addClass('button_selected');
                                                $("#EXAM_DRAW").removeClass('button_selected');
                                                $("#EXAM_TEXT").removeClass('button_selected');
                                                update_PREFS();
                                                }
                                                show_QP();
                                                scrollTo("EXT_left");
                                                });

                  $("#EXAM_TEXT,#PANEL_TEXT").click(function() {

                                                    // also hide QP, DRAWs, and PRIORS
                                                    hide_DRAW();
                                                    hide_QP();
                                                    hide_PRIORS();
                                                    hide_right();
                                                    show_TEXT();
                                                    for (index = '0'; index < zones.length; ++index) {
                                                    $("#PREFS_"+zones[index]+"_RIGHT").val(0);
                                                    }
                                                    update_PREFS();

                                                    $("#EXAM_DRAW").removeClass('button_selected');
                                                    $("#EXAM_QP").removeClass('button_selected');
                                                    $("#EXAM_TEXT").addClass('button_selected');
                                                    scrollTo("EXT_left");
                                                    });
                  $("[id^='BUTTON_TEXT_']").click(function() {
                                                  var zone = this.id.match(/BUTTON_TEXT_(.*)/)[1];
                                                  if (zone != "menu") {
                                                  $("#"+zone+"_right").addClass('nodisplay');
                                                  $("#"+zone+"_left").removeClass('display');
                                                  $("#"+zone+"_left_text").removeClass('display');
                                                  $("#PREFS_"+zone+"_RIGHT").val(0);
                                                  update_PREFS();
                                                  }
                                                  show_TEXT();
                                                  scrollTo("EXT_left");
                                                  });
                  $("[id^='BUTTON_TEXTD_']").click(function() {
                                                   var zone = this.id.match(/BUTTON_TEXTD_(.*)/)[1];
                                                   if (zone != "menu") {
                                                     if ((zone =="PMH") || (zone == "HPI")) {
                                                       $("#PMH_right").addClass('nodisplay');
                                                       $("#PREFS_PMH_RIGHT").val(1);
                                                       $("#HPI_right").addClass('nodisplay');
                                                       $("#PREFS_HPI_RIGHT").val(1);
                                                       var reset = $("#HPI_1").height();
                                                       $("#PMH_1").height(reset);
                                                       $("#PMH_left").height(reset-40);
                                                       $("#LayerTechnical_sections_1").css("clear","both");
                                                     } else {
                                                       $("#"+zone+"_right").addClass('nodisplay');
                                                       $("#PREFS_"+zone+"_RIGHT").val(1);
                                                     }
                                                     scrollTo(zone+"_left");
                                                     update_PREFS();

                                                   }
                                                   });

                  $("#EXAM_TEXT").addClass('button_selected');
                  if (($("#PREFS_CLINICAL").val() !='1')) {
                  var actionQ = "#EXAM_"+$("#PREFS_EXAM").val();
                  $(actionQ).trigger('click');
                  } else {
                  $("#EXAM_TEXT").addClass('button_selected');
                  }
                  if ($("#ANTSEG_prefix").val() > '') {
                  $("#ANTSEG_prefix_"+$("#ANTSEG_prefix").val()).addClass('button_selected');
                  } else {
                  $("#ANTSEG_prefix").val('off').trigger('change');
                  }
                  $("[name^='ACT_tab_']").mouseover(function() {
                                                    $(this).toggleClass('underline').css( 'cursor', 'pointer' );
                                                    });
                  $("[name^='ACT_tab_']").mouseout(function() {
                                                   $(this).toggleClass('underline');
                                                   });

                  $("[name^='ACT_tab_']").click(function()  {
                                                var section = this.id.match(/ACT_tab_(.*)/)[1];
                                                $("[name^='ACT_']").addClass('nodisplay');
                                                $("[name^='ACT_tab_']").removeClass('nodisplay').removeClass('ACT_selected').addClass('ACT_deselected');
                                                $("#ACT_tab_" + section).addClass('ACT_selected').removeClass('ACT_deselected');
                                                $("#ACT_" + section).removeClass('nodisplay');
                                                $("#PREFS_ACT_SHOW").val(section);
                                                //selection correct QP zone
                                                $("[name^='NEURO_ACT_zone']").removeClass('eye_button_selected');
                                                $("#NEURO_ACT_zone_"+ section).addClass("eye_button_selected");
                                                $("#NEURO_ACT_zone").val(section);
                                                update_PREFS();
                                                });
                  $("#ACTTRIGGER").mouseout(function() {
                                            $("#ACTTRIGGER").toggleClass('buttonRefraction_selected').toggleClass('underline');
                                            });
                  if ($("#PREFS_ACT_VIEW").val() == '1') {
                  $("#ACTMAIN").toggleClass('nodisplay');
                  $("#NPCNPA").toggleClass('nodisplay');
                  $("#ACTNORMAL_CHECK").toggleClass('nodisplay');
                  $("#ACTTRIGGER").toggleClass('underline');
                  var show = $("#PREFS_ACT_SHOW").val();
                  $("#ACT_tab_"+show).trigger('click');
                  }
                  $("#ACTTRIGGER").click(function() {
                                         $("#ACTMAIN").toggleClass('nodisplay').toggleClass('ACT_TEXT');
                                         $("#NPCNPA").toggleClass('nodisplay');
                                         $("#ACTNORMAL_CHECK").toggleClass('nodisplay');
                                         $("#ACTTRIGGER").toggleClass('underline');
                                         if ($("#PREFS_ACT_VIEW").val()=='1') {
                                         $("#PREFS_ACT_VIEW").val('0');
                                         } else {
                                         $("#PREFS_ACT_VIEW").val('1');
                                         }
                                         var show = $("#PREFS_ACT_SHOW").val();
                                         $("#ACT_tab_"+show).trigger('click');
                                         update_PREFS();
                                         });
                  $("#NEURO_COLOR").click(function() {
                                          $("#ODCOLOR").val("11/11");
                                          $("#OSCOLOR").val("11/11");
                                          submit_form("eye_mag");
                                          });

                  $("#NEURO_COINS").click(function() {
                                          $("#ODCOINS").val("1.00");
                                          //leave currency symbol out unless it is an openEMR defined option
                                          $("#OSCOINS").val("1.00");
                                          submit_form("eye_mag");
                                          });

                  $("#NEURO_REDDESAT").click(function() {
                                             $("#ODREDDESAT").val("100");
                                             $("#OSREDDESAT").val("100");
                                             submit_form("eye_mag");
                                             });

                  $("[id^='myCanvas_']").mouseout(function() {
                                                  var zone = this.id.match(/myCanvas_(.*)/)[1];
                                                  submit_canvas(zone);
                                                  });
                  $("[id^='Undo_']").click(function() {
                                           var zone = this.id.match(/Undo_Canvas_(.*)/)[1];
                                           submit_canvas(zone);
                                           });
                  $("[id^='Redo_']").click(function() {
                                           var zone = this.id.match(/Redo_Canvas_(.*)/)[1];
                                           submit_canvas(zone);
                                           });
                  $("[id^='Clear_']").click(function() {
                                            var zone = this.id.match(/Clear_Canvas_(.*)/)[1];
                                            submit_canvas(zone);
                                            });
                  $("[id^='Blank_']").click(function() { 

                                           var zone = this.id.match(/Blank_Canvas_(.*)/)[1];
                                           $("#url_"+zone).val("../../forms/eye_mag/images/BLANK_BASE.png");
                                           //canvas.renderAll();
                                           drawImage(zone);
                                           });

                  $("#COPY_SECTION").change(function() {
                                            var start = $("#COPY_SECTION").val();
                                            if (start =='') return;
                                            var value = start.match(/(\w*)-(\w*)/);
                                            var zone = value[1];
                                            var copy_from = value[2];
                                            if (zone =="READONLY") copy_from = $("#form_id").val();
                                            var count_changes='0';

                                            var data = {
                                            action      : 'copy',
                                            copy        : zone,
                                            zone        : zone,
                                            copy_to     : $("#form_id").val(),
                                            copy_from   : copy_from,
                                            pid         : $("#pid").val()
                                            };
                                            if (zone =="READONLY") {
                                            //we are going to update the whole form
                                            //Imagine you are watching on your browser while the tech adds stuff in another room on another computer.
                                            //We are not ready to actively chart, just looking to see how far along our staff is...
                                            //or maybe just looking ahead to see who's next in the next room?
                                            //Either way, we are looking at a record that at present will be disabled/we cannot change...
                                            // yet it is updating every 10 seconds if another user is making changes.

                                            //      READONLY does not show IMPPLAN changes!!!!
                                            } else {
                                            //here we are retrieving an old record to copy forward to today's active chart.
                                            data = $("#"+zone+"_left_text").serialize() + "&" + $.param(data);
                                            }
                                            top.restoreSession();
                                            $.ajax({
                                                   type   : 'POST',
                                                   dataType : 'json',
                                                   url      :  "../../forms/eye_mag/save.php",
                                                   data   : data,
                                                   success  : function(result) {
                                                   //we have to process impplan differently
                                                   if (zone =='IMPPLAN') {
                                                   //we get a json result.IMPPLAN back from the prior visit
                                                   //we need to add that to the current list? Replace for now.
                                                   build_IMPPLAN(result.IMPPLAN);
                                                   store_IMPPLAN(result.IMPPLAN);
                                                   //   need to make the Plan areas purple?
                                                   } else {
                                                   $.map(result, function(valhere, keyhere) {
                                                         if ($("#"+keyhere).val() != valhere) {
                                                         $("#"+keyhere).val(valhere).css("background-color","#CCF");
                                                         } else if (keyhere.match(/MOTILITY_/)) {
                                                         // Copy forward ductions and versions visually
                                                         // Make each blank, and rebuild them
                                                         $("[name='"+keyhere+"_1']").html('');
                                                         $("[name='"+keyhere+"_2']").html('');
                                                         $("[name='"+keyhere+"_3']").html('');
                                                         $("[name='"+keyhere+"_4']").html('');
                                                         if (keyhere.match(/(_RS|_LS|_RI|_LI|_RRSO|_RRIO|_RLSO|_RLIO|_LRSO|_LRIO|_LLSO|_LLIO)/)) {
                                                         // Show a horizontal (minus) tag.  When "/" and "\" fa-icons are available will need to change.
                                                         // Maybe just use small font "/" and "\" directly.
                                                         hash_tag = '<i class="fa fa-minus"></i>';
                                                         } else { //show vertical tag
                                                         hash_tag = '<i class="fa fa-minus rotate-left"></i>';
                                                         }
                                                         for (index =1; index <= valhere; ++index) {
                                                         $("#"+keyhere+"_"+index).html(hash_tag);
                                                         }
                                                         } else if (keyhere.match(/^(ODVF|OSVF)\d$/)) {
                                                         if (valhere =='1') {
                                                         $("#FieldsNormal").prop('checked', false);
                                                         $("#"+keyhere).prop('checked', true);
                                                         $("#"+keyhere).val('1');
                                                         } else {
                                                         $("#"+keyhere).val('0');
                                                         $("#"+keyhere).prop('checked', false);
                                                         }
                                                         } else if (keyhere.match(/AMSLERO(.)/)) {
                                                         var sidehere = keyhere.match(/AMSLERO(.)/);
                                                         if (valhere < '1') valhere ='0';
                                                         $("#"+keyhere).val(valhere);
                                                         var srcvalue="AmslerO"+sidehere[1];
                                                         document.getElementById(srcvalue).src = document.getElementById(srcvalue).src.replace(/\_\d/g,"_"+valhere);
                                                         $("#AmslerO"+sidehere[1]+"value").text(valhere);
                                                         } else if (keyhere.match(/VA$/)) {
                                                         $("#"+keyhere+"_copy").val(valhere).css("background-color","#F0F8FF");;
                                                         $("#"+keyhere+"_copy_brd").val(valhere).css("background-color","#F0F8FF");;
                                                         }
                                                         });
                                                   if (zone != "READONLY") { submit_form("eye_mag"); }
                                                   }
                                                   }});
                                            });
                  $("[id^='BUTTON_DRAW_']").click(function() {
                                                  var zone =this.id.match(/BUTTON_DRAW_(.*)$/)[1];
                                                  if (zone =="ALL") {
                                                  } else {
                                                  if ($('#PREFS_'+zone+'_RIGHT').val() =="DRAW") {
                                                  $('#BUTTON_TEXTD_'+zone).trigger("click");//closes draw
                                                  //maybe this should revert to last right panel state (qp,text)
                                                  return;
                                                  }
                                                  $("#"+zone+"_1").removeClass('nodisplay');
                                                  $("#"+zone+"_right").addClass('canvas').removeClass('nodisplay');
                                                  $("#QP_"+zone).addClass('nodisplay');
                                                  $("#PRIORS_"+zone+"_left_text").addClass('nodisplay');
                                                  $("#Draw_"+zone).removeClass('nodisplay');
                                                  $("#PREFS_"+zone+"_RIGHT").val('DRAW');
                                                  scrollTo(zone+"_left");
                                                  //alert("ok?");
                                                  update_PREFS();
                                                  }
                                                  });
                  $("[id^='BUTTON_QP_']").click(function() {
                                                var zone = this.id.match(/BUTTON_QP_(.*)$/)[1].replace(/_\d*/,'');
                                                if (zone =='IMPPLAN2') {
                                                  $('#IMP_start_acc').slideDown();
                                                  zone='IMPPLAN';
                                                }
                                                if ($("#PREFS_"+zone+"_RIGHT").val() =='QP') {
                                                  $('#BUTTON_TEXTD_'+zone).trigger("click");
                                                  return;
                                                }
                                                $("#PRIORS_"+zone+"_left_text").addClass('nodisplay');
                                                $("#Draw_"+zone).addClass('nodisplay');
                                                show_QP_section(zone);
                                                $("#PREFS_"+zone+"_RIGHT").val('QP');
                                                if ((zone != 'PMH')&&(zone != 'HPI')) {
                                                }
                                                if (zone == 'PMH') {
                                                if($('#HPI_right').css('display') == 'none') {
                                                $("#PRIORS_HPI_left_text").addClass('nodisplay');
                                                $("#Draw_HPI").addClass('nodisplay');
                                                show_QP_section('HPI');
                                                $("#PREFS_HPI_RIGHT").val('QP');
                                                //$("html,body").animate({scrollTop: '400'}, "slow");
                                                }
                                                if ($('#PMH_right').height() > $('#PMH_left').height()) {
                                                $('#PMH_left').height($('#PMH_right').height());
                                                $('#PMH_1').height($('#PMH_right').height()+20);
                                                } else { $('#PMH_1').height($('#HPI_1').height()); }
                                                }
                                                else if (zone == 'HPI') {
                                                if($('#PMH_right').css('display') == 'none') {
                                                $("#PRIORS_PMH_left_text").addClass('nodisplay');
                                                $("#Draw_PMH").addClass('nodisplay');
                                                show_QP_section('PMH','1');
                                                $("#PREFS_PMH_RIGHT").val('QP');
                                                }
                                                if ($('#PMH_right').height() > $('#PMH_left').height()) {
                                                $('#PMH_left').height($('#PMH_right').height());
                                                } else { $('#PMH_1').height($('#HPI_1').height()); }
                                                } else if (zone == 'menu') {
                                                show_QP();
                                                } else if (zone == 'IMPPLAN') {
                                                show_QP_section('IMPPLAN');
                                                update_PREFS();
                                                }


                                                });

                  // set default to ccDist.  Change as desired.
                  $('#NEURO_ACT_zone').val('CCDIST').trigger('change');
                  if ($("#RXStart").val() =="2") {
                  $("#Trifocal").trigger('click');
                  }
                  $("[id$='_loading']").addClass('nodisplay');
                  $("[id$='_sections']").removeClass('nodisplay');

                  if ($('#PMH_right').height() > $('#PMH_left').height()) {
                  $('#PMH_left').height($('#PMH_right').height());
                  } else { $('#PMH_1').height($('#HPI_1').height()); }

                  $('#left-panel').css("right","0px");
                  $('#EXAM_KB').css({position: 'fixed', top: '29px'});
                  $('#EXAM_KB').css('display', 'block');
                  $('#EXAM_KB').draggable();
                  $('#IMP').droppable({ drop: dragto_IMPPLAN } );
                  $('#IMPPLAN_zone').droppable({ drop: dragto_IMPPLAN_zone } );
                  $('#IMPPLAN_text').droppable({ drop: dragto_IMPPLAN_zone } );

                  $('[id^="PLANS"]').draggable(  { cursor: 'move', revert: true });
                  $('[id^="PLAN_"]').height( $(this).scrollHeight );

                  /*  Sorting of diagnoses in IMP/PLAN right panel builds IMP_order[] array.
                   Foreach index => value in IMP_order[order,PMSFH[type][i]]:
                   retrieve PMSFH[type][value] and build the IMPRESSION/PLAN area
                   openEMR ICD-10 seems to have newlines in codetext?  strip them with replace.
                   All the ISSUE_TYPES and their fields are available in obj.PMSFH:
                   'title' => $disptitle,
                   'status' => $statusCompute,
                   'enddate' => $row['enddate'],
                   'reaction' => $row['reaction'],
                   'referredby' => $row['referredby'],
                   'extrainfo' => $row['extrainfo'],
                   'diagnosis' => $row['diagnosis'],
                   'code' => $code,
                   'codedesc' => $codedesc,
                   'codetext' => $codetext,
                   'codetype' => $codetype,
                   'comments' => $row['comments'],
                   'rowid' => $row['id'],
                   'row_type' => $row['type']
                   eg. obj.IMPPLAN_items[index] =  code: obj.PMSFH['POH'][value]['code'],
                   codedesc:  obj.PMSFH['POH'][value]['codedesc'],
                   codetype:  obj.PMSFH['POH'][value]['codetype']
                   */

                  $('#make_new_IMP').click(function() {
                                           var issue='';
                                           if (IMP_order.length ==0) rebuild_IMP($( "#build_DX_list" ));
                                           if (obj.IMPPLAN_items ==null) obj.IMPPLAN_items = [];
                                           $.each(IMP_order, function( index, value ) {
                                                  issue= value.match(/(.*)_(.*)/);
                                                  if (issue[1] == "CLINICAL") {
                                                  if (!$('#inc_PE').is(':checked')) { return; }

                                                  var the_code='';
                                                  var the_codedesc='';
                                                  var the_codetext='';
                                                  var the_plan='';
                                                  for (i=0;i < obj.Clinical[issue[2]].length; i++) {
                                                  if (i == 0) {
                                                  the_code = obj.Clinical[issue[2]][i]['code'];
                                                  } else if (i < obj.Clinical[issue[2]].length) {
                                                  the_code += ', '+ obj.Clinical[issue[2]][i]['code'];
                                                  }
                                                  the_codedesc += obj.Clinical[issue[2]][i]['codedesc'] + "\r";
                                                  the_codetext += obj.Clinical[issue[2]][i]['codetext'] + "\r";
                                                  the_plan += obj.Clinical[issue[2]][i]['codedesc'] + "\r";
                                                  }
                                                  obj.IMPPLAN_items.push({
                                                                         title:obj.Clinical[issue[2]][0]['title'],
                                                                         code: the_code,
                                                                         codetype: obj.Clinical[issue[2]][0]['codetype'],
                                                                         codedesc: the_codedesc,
                                                                         codetext: the_codetext,
                                                                         plan: the_plan,
                                                                         PMSFH_link: obj.Clinical[issue[2]][0]['PMSFH_link']
                                                                         });
                                                  } else {
                                                  if (issue[1] == "PMH") {
                                                  if (!$('#inc_PMH').is(':checked')) { return; }
                                                  } else if (issue[1] == "POH"){
                                                  if (!$('#inc_POH').is(':checked')) { return; }
                                                  } else if (issue[1] == "POS"){
                                                  if (!$('#inc_POH').is(':checked')) { return; }
                                                  }
                                                  obj.IMPPLAN_items.push({
                                                                         title:         obj.PMSFH[issue[1]][issue[2]]['title'],
                                                                         code:          obj.PMSFH[issue[1]][issue[2]]['code'],
                                                                         codetype:      obj.PMSFH[issue[1]][issue[2]]['codetype'],
                                                                         codedesc:      obj.PMSFH[issue[1]][issue[2]]['codedesc'],
                                                                         codetext:      obj.PMSFH[issue[1]][issue[2]]['codetext'].replace(/(\r\n|\n|\r)/gm,""),
                                                                         plan:          obj.PMSFH[issue[1]][issue[2]]['comments'],
                                                                         PMSFH_link:    obj.PMSFH[issue[1]][issue[2]]['PMSFH_link']
                                                                         });
                                                  }
                                                  });
                                           build_IMPPLAN(obj.IMPPLAN_items);
                                           store_IMPPLAN(obj.IMPPLAN_items,'1');
                                           });


                  var allPanels = $('.building_blocks > dd').hide();
                  var allPanels2 = $('.building_blocks2 > dd').hide();

                  $('.building_blocks > dt > span').click(function() {
                                                          allPanels.slideUp();
                                                          $(this).parent().next().slideDown();
                                                          return false;
                                                          });
                  $('.building_blocks2 > dt > span').click(function() {
                                                           allPanels2.slideUp();
                                                           $(this).parent().next().slideDown();
                                                           return false;
                                                           });
                  $('#IMP_start_acc').slideDown();
                  $('[id^=inc_]').click(function() {
                                        build_DX_list(obj);
                                        });

                  $('#active_flag').click(function() { check_lock('1'); });
                  $('#active_icon').click(function() { check_lock('1'); });

                  $("input,textarea,text,checkbox").change(function(){
                                                           $(this).css("background-color","#F0F8FF");
                                                           submit_form($(this));
                                                           });
                  $('#IMP').blur(function() {
                                 //add this DX to the obj.IMPPLAN_items array
                                 //take the first line as the impression and the rest as the plan
                                 var total_imp = $('#IMP').val();
                                 var local_plan = '';
                                 var local_code= '';
                                 if (total_imp.length < '2') return; //reject text under two letters?
                                 var re = /\r\n|[\n\v\f\r\x85\u2028\u2029]/; //official list of line delimiters for a regex
                                 //local_impression is first line only[1]
                                 var local_imp = total_imp.match(/^(.*)(?:\r\n|[\n\v\f\r\x85\u2028\u2029])(.*)/);
                                 if (local_imp == null || local_imp[1] == null) {
                                 local_imp = total_imp;
                                 } else {
                                 // If the first line was dropped in from the Builder via a draggable DX_list
                                 // it will include the IMPRESSION + CODE.
                                 // Consider stripping out the CODE
                                 var local_imp_code = local_imp[1].match(/(.*)(ICD.*)$/);
                                 if (local_imp_code) {
                                 local_imp = local_imp_code[1];
                                 local_code = local_imp_code[2];
                                 local_plan = total_imp.replace(local_imp_code[0],''); //plan is line 2+ if present, strip off first line
                                 local_plan = local_plan.replace(/^\r\n|[\n\v\f\r\x85\u2028\u2029]/,'');
                                 } else {
                                 local_imp = local_imp[1];
                                 local_code = '';
                                 local_plan = total_imp.replace(local_imp,''); //plan is line 2+ if present, strip off first line
                                 local_plan = local_plan.replace(/^\r\n|[\n\v\f\r\x85\u2028\u2029]/,'');
                                 }
                                 }
                                 if (obj.IMPPLAN_items ==null) obj.IMPPLAN_items = [];//can't push if array does not exist
                                 obj.IMPPLAN_items.push({
                                                        form_id: $('#form_id').val(),
                                                        pid: $('#pid').val(),
                                                        title: local_imp,
                                                        plan: local_plan,
                                                        code: local_code,
                                                        codetext:'',
                                                        codetype:'',
                                                        codedesc:'',
                                                        PMSFH_link: ''
                                                        });
                                 build_IMPPLAN(obj.IMPPLAN_items);
                                 store_IMPPLAN(obj.IMPPLAN_items,'1');
                                 $('#IMP').val('');//clear the box
                                 submit_form('1');//tell the server where we stand
                                 });
                  $('#Add_Glasses').click(function() {
                                          for (i=2; i <6; i++) { //come on, 5 current rx glasses should be enough...
                                          if ($('#W_'+i).val() != '1') {
                                          $('#W_'+i).val('1');
                                          $('#LayerVision_W_'+i).removeClass('nodisplay');
                                          if (i==5) { $('#Add_Glasses').addClass('nodisplay'); }
                                          break;
                                          }
                                          }
                                          });
                  $("[name='reverseme']").click(function() {
                                                var target = this.id;
                                                reverse_cylinder(target);
                                                });

                  $('#code_me_now').click(function(event) {
                                          event.preventDefault();
                                          build_CODING_list();
                                          });
                  $( ".widget a" ).button();

                  $('#goto_fee_sheet2').click(function(event) {
                    goto_url('<?php echo $GLOBALS['webroot']; ?>/interface/patient_file/encounter/load_form.php?formname=fee_sheet');

                                            });
                  $( "button" ).button().click(function( event ) {
                         event.preventDefault();
                         });
                  refresh_page();
                  // AUTO- CODING FEATURES
                  check_CPT_92060();
                  check_exam_detail();
                  if ($("#PRIOR_ALL").value == "undefined") {
                  var Code_new_est="New";
                  } else {
                  var Code_new_est="Est";
                  }
                  Suggest_visit_code();
                  $('#visit_codes').change(function() {
                                           var data_all = $(this).val();
                                           var data = data_all.match(/^(.*)\|(.*)\|/);
                                           visit_code = data[2];
                                           visit_type = data[1];
                                           });
                  show_QP_section('IMPPLAN','1');
                  $('.modifier').on('click', function () {
                    var item = this.id.match(/visit_mod_(.*)/)[1];
                    if ($(this).hasClass('status_on')) {
                        $(this).css("background-color","navy");
                        $(this).removeClass('status_on');
                        delete visit_modifier[''+item];
                      } else {
                        $(this).css("background-color","red");
                        $(this).addClass('status_on');
                        visit_modifier.push(item);
                      } 
                  });
                  build_IMPPLAN(obj.IMPPLAN_items);
                  scroll='1';
                  <?php if ($GLOBALS['new_tabs_layout'] !=='1') { ?>  $("[class='tabHide']").css("display","inline-block"); <?php } ?>
                  $("input,textarea,text").focus(function(){
                                                 $(this).css("background-color","#ffff99");
                                                 });
                  $(window).bind('onbeforeunload', function(){
                                 if ($('#chart_status').val()=="on") {
                                 unlock(); }
                                 });
                  });
