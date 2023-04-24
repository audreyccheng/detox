<?php
@define('__POSTCALENDAR__','PostCalendar');
/**
 *  $Id$
 *
 *  PostCalendar::PostNuke Events Calendar Module
 *  Copyright (C) 2002  The PostCalendar Team
 *  http://postcalendar.tv
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  To read the license please read the docs/license.txt or visit
 *  http://www.gnu.org/copyleft/gpl.html
 *
 */

//=========================================================================
//  Load the API Functions and Language defines
//=========================================================================
pnModAPILoad(__POSTCALENDAR__,'user');

// Added to improve security and standardization of input data to be used in
//  database insertion.
require_once($GLOBALS['srcdir']."/formdata.inc.php");

//=========================================================================
//  start the main postcalendar application
//=========================================================================
function postcalendar_user_main()
{
 // check the authorization

    if (!pnSecAuthAction(0, 'PostCalendar::', '::', ACCESS_OVERVIEW)) { return _POSTCALENDARNOAUTH; }
    // get the date and go to the view function
    $Date = postcalendar_getDate();
    return postcalendar_user_view(array('Date'=>$Date));
}


/**
 * view items
 * This is a standard function to provide an overview of all of the items
 * available from the module.
 */
function postcalendar_user_view()
{
    if (!pnSecAuthAction(0, 'PostCalendar::', '::', ACCESS_OVERVIEW)) { return _POSTCALENDARNOAUTH; }
    // get the vars that were passed in
    list($Date,
         $print,
         $viewtype,
         $jumpday,
         $jumpmonth,
         $jumpyear) = pnVarCleanFromInput('Date',
                                          'print',
                                          'viewtype',
                                          'jumpday',
                                          'jumpmonth',
                                          'jumpyear');
    $Date =postcalendar_getDate();
    if(!isset($viewtype))   $viewtype = _SETTING_DEFAULT_VIEW;
    
    // added to allow the view & providers to remain as the user last saw it -- JRM
    if ($_SESSION['viewtype']) $viewtype = $_SESSION['viewtype'];
    if ($_SESSION['pc_username']) $pc_username = $_SESSION['pc_username'];

    return postcalendar_user_display(array('viewtype'=>$viewtype,'Date'=>$Date,'print'=>$print)) . postcalendar_footer();
}

/**
 * display item
 * This is a standard function to provide detailed information on a single item
 * available from the module.
 */
function postcalendar_user_display($args)
{
    list($eid, $viewtype, $tplview,
         $pc_username, $Date, $print, $category, $topic, $pc_facility) = pnVarCleanFromInput('eid', 'viewtype', 'tplview',
                                                         'pc_username', 'Date', 'print', 'pc_category', 'pc_topic', 'pc_facility');
    // added to allow the view & providers to remain as the user last saw it -- JRM
    if ($_SESSION['viewtype']) $viewtype = $_SESSION['viewtype'];
    if ($_SESSION['pc_username']) $pc_username = $_SESSION['pc_username'];

    // funky things happen if the view is 'details' and we don't have an event ID
    // so in such a case, we're going to revert to the 'day' view -- JRM
    if ($viewtype == 'details' && (!isset($eid) || $eid == "")) {
        $_SESSION['viewtype'] = 'day';
        $viewtype = $_SESSION['viewtype'];
    }

    extract($args);
    if(empty($Date) && empty($viewtype)) { return false; }
    if(empty($tplview)) $tplview = 'default';

    $uid = pnUserGetVar('uid');
    $theme = pnUserGetTheme();

    //$cacheid = md5($Date.$viewtype.$tplview._SETTING_TEMPLATE.$eid.$print.$uid.'u'.$pc_username.$theme.'c'.$category.'t'.$topic);
    $cacheid = md5(strtotime("now"));

    switch ($viewtype)
    {
        case 'details':
            if (!(bool)PC_ACCESS_READ) { return _POSTCALENDARNOAUTH; }
            $event = pnModAPIFunc('PostCalendar','user','eventDetail',array('eid'=>$eid,
                                                                           'Date'=>$Date,
                                                                           'print'=>$print,
                                                                           'cacheid'=>$cacheid));
            if($event === false) {
                pnRedirect(pnModURL(__POSTCALENDAR__,'user'));
            }
            $out  = "\n\n<!-- START user_display -->\n\n";
            $out .= $event;
            $out .= "\n\n<!-- END user_display -->\n\n";
            break;

        default :
            if (!(bool)PC_ACCESS_OVERVIEW) {
                return _POSTCALENDARNOAUTH;
            }
            $out  = "\n\n<!-- START user_display -->\n\n";
            $out .= pnModAPIFunc('PostCalendar','user','buildView',array('Date'=>$Date,
                                                                         'viewtype'=>$viewtype,
                                                                         'cacheid'=>$cacheid));
            $out .= "\n\n<!-- END user_display -->\n\n";
            break;
    }
    // Return the output that has been generated by this function
    return $out;
}
function postcalendar_user_delete()
{
    if(!(bool)PC_ACCESS_ADD) {
        return _POSTCALENDAR_NOAUTH;
    }

    $output = new pnHTML();
    $output->SetInputMode(_PNH_VERBATIMINPUT);

    $uname = $_SESSION['authUser'];
    list($action,$pc_event_id) = pnVarCleanFromInput('action','pc_event_id');
    $event =& postcalendar_userapi_pcGetEventDetails($pc_event_id);
    if($uname != $event['uname']) {
        if (!validateGroupStatus($uname,getUsername($event['uname']))) {

         return _PC_CAN_NOT_DELETE;
        }
    }
    //if($uname != $event['uname']) {
    //    return _PC_CAN_NOT_DELETE;
    //}
    unset($event);

    $output->FormStart(pnModUrl(__POSTCALENDAR__,'user','deleteevents'));
    $output->FormHidden('pc_eid',$pc_event_id);
    $output->Text(_PC_DELETE_ARE_YOU_SURE.' ');
    $output->FormSubmit(_PC_ADMIN_YES);
    $output->FormEnd();
    $output->Linebreak(2);
    $output->Text(pnModAPIFunc(__POSTCALENDAR__,'user','eventDetail',array('eid'=>$pc_event_id,'cacheid'=>'','print'=>0,'Date'=>'')));
    $output->Linebreak(2);


    return $output->GetOutput();
}
function postcalendar_user_deleteevents()
{
    if(!(bool)PC_ACCESS_ADD) {
        return _POSTCALENDAR_NOAUTH;
    }


    $pc_eid = pnVarCleanFromInput('pc_eid');
    $event =& postcalendar_userapi_pcGetEventDetails($pc_eid);
    $uname = $_SESSION['authUser'];
    if($uname != $event['uname']) {
        if (!validateGroupStatus($uname,getUsername($event['uname']))) {

         return _PC_CAN_NOT_DELETE;
        }
    }
    unset($event);

    $output = new pnHTML();
    $output->SetInputMode(_PNH_VERBATIMINPUT);
    list($dbconn) = pnDBGetConn();
    $pntable = pnDBGetTables();
    $events_table = $pntable['postcalendar_events'];
    $events_column = &$pntable['postcalendar_events_column'];
    //hipaa doesn't allow for actual deletes, so just change to inactive
    //$sql = "DELETE FROM $events_table WHERE $events_column[eid] = '$pc_eid'";
    $sql = "UPDATE $events_table SET pc_eventstatus = 0 WHERE $events_column[eid] = '$pc_eid'";
    $dbconn->Execute($sql);
    $tpl = new pcSmarty();
    $template_name = _SETTING_TEMPLATE;
    if(!isset($template_name)) {
        $template_name = 'default';
    }
    $tpl->assign('STYLE',$GLOBALS['style']);
    $output->Text($tpl->fetch($template_name . "/views/header.html"));
    $output->Text($tpl->fetch($template_name . "/views/global/navigation.html"));
    $output->Text("<br /><br />");


    if ($dbconn->ErrorNo() != 0) {
        $output->Text(_PC_ADMIN_EVENT_ERROR);
    } else {


        $output->Text(_PC_ADMIN_EVENTS_DELETED);
    }
    $output->Text($tpl->fetch($template_name . "/views/footer.html"));
    // clear the template cache
    $tpl->clear_all_cache();

    return $output->GetOutput();
}

//this function is only used by the system to delete temp events used in certain
//collision calculations
function delete_event($title)
{
    list($dbconn) = pnDBGetConn();
    $pntable = pnDBGetTables();
    $events_table = $pntable['postcalendar_events'];
    $events_column = &$pntable['postcalendar_events_column'];
    //this function is only used by the system to delete temp events used in certain
    //collision calculations
    $sql = "DELETE FROM $events_table WHERE pc_eventstatus = " ._EVENT_TEMPORARY ." AND pc_title = '$title'";
    $dbconn->Execute($sql);
    if ($dbconn->ErrorNo() != 0) {
        return 0;
    } else {
        return 1;
    }

}

/**
 * submit an event
 */
function postcalendar_user_edit($args) {return postcalendar_user_submit($args); }
function postcalendar_user_submit2($args)
{

    if (!(bool)PC_ACCESS_ADD) {
        return _POSTCALENDARNOAUTH;
    }
    extract($args);
    //print_r($_GET);
    $category = pnVarCleanFromInput('event_category');
    //print_r($category);
    print "dble is ".pnVarCleanFromInput('double_book')." data_loaded is ".pnVarCleanFromInput('data_loaded');
    //print_r($_POST);
    if(pnVarCleanFromInput('data_loaded') || !empty($category))    //submitting
    {
        return postcalendar_user_submit2($agrs);
    }
    else
    {
        //select the category you wish to add,
        //using the info from that category we can populate some data

        $output = new pnHTML();
        $output->SetInputMode(_PNH_VERBATIMINPUT);
        $output->Text('<body bgcolor="'.$GLOBALS['style']['BGCOLOR2'].'"></body>');
        // get the theme globals :: is there a better way to do this?
        pnThemeLoad(pnUserGetTheme());
        $all_categories = pnModAPIFunc(__POSTCALENDAR__,'admin','getCategories');
        $output->Text('<form name="cats" method="post" action="'.pnModURL(__POSTCALENDAR__,'user','submit2', $args).'">');
        $output->FormHidden('no_nav', $_GET['no_nav']);
        $output->FormHidden('event_startampm', $_GET['event_startampm']);
        $output->FormHidden('event_starttimeh', $_GET['event_starttimeh']);
        $output->FormHidden('event_starttimem', $_GET['event_starttimem']);
        $output->FormHidden('event_startmonth', $_GET['event_startmonth']);
        $output->FormHidden('event_startday', $_GET['event_startday']);
        $output->FormHidden('event_startyear', $_GET['event_startyear']);
        $output->FormHidden('event_category', $_GET['event_category']);
        $output->FormHidden('event_dur_minutes', $_GET['event_dur_minutes']);
        $output->FormHidden('provider_id',$_GET['provider_id']);
        $output->FormHidden('patient_id', $_GET['patient_id']);
        $output->FormHidden('module', $_GET['module']);
        $output->FormHidden('func', $_GET['func']);
        $output->FormHidden('Date', $_GET['Date']);
        $select = array();

        foreach($all_categories as $cat)
        {
            array_push($select, array('name'=>$cat['name'],'id'=>base64_encode(serialize($cat))));
        }
        $output->Text('Select a Category');
        $output->FormSelectMultiple('category', $select);
        $output->FormSubmit();
        return $output->GetOutput();
    }
    //return postcalendar_user_submit2($args);
}

function postcalendar_user_submit($args)
{
    // We need at least ADD permission to submit an event
    if (!(bool)PC_ACCESS_ADD) {
        return _POSTCALENDARNOAUTH;
    }

    $output = new pnHTML();
    $output->SetInputMode(_PNH_VERBATIMINPUT);


    // get the theme globals :: is there a better way to do this?
    pnThemeLoad(pnUserGetTheme());
    global $bgcolor1, $bgcolor2, $bgcolor3, $bgcolor4, $bgcolor5, $textcolor1, $textcolor2;

    // $category = pnVarCleanFromInput('event_category');
    $category = pnVarCleanFromInput('category');

    if(!empty($category))
    {
        $category = unserialize(base64_decode($category));
        //print_r($category);
    }
    else
    { //print_r($_POST);
        $cat = $_POST['category'];

        $category = unserialize(base64_decode($cat));
        //print_r($category);
    }
    //print_r($category);

    // echo("<!-- Here is the argument array: -->\n");
    // foreach ($args as $tmpkey => $tmpval) { // debugging
    //  echo("<!-- $tmpkey => '$tmpval' -->\n");
    // }

    extract($args);

    $Date =& postcalendar_getDate();
    $year   = substr($Date,0,4);
    $month  = substr($Date,4,2);
    $day    = substr($Date,6,2);

    // basic event information
    $event_desc     = pnVarCleanFromInput('event_desc');
    $event_category = pnVarCleanFromInput('event_category');
    $event_subject  = pnVarCleanFromInput('event_subject');
    $event_sharing  = pnVarCleanFromInput('event_sharing');
    $event_topic    = pnVarCleanFromInput('event_topic');

    //id of the user the event is for
    $event_userid = pnVarCleanFromInput('event_userid');
    if (!is_numeric($event_userid))
        $event_userid = 0;
    $event_pid = pnVarCleanFromInput('event_pid');

    if (!is_numeric($event_pid))
        $event_pid = "";

    // event start information
    $event_startmonth    = pnVarCleanFromInput('event_startmonth');
    $event_startday      = pnVarCleanFromInput('event_startday');
    $event_startyear     = pnVarCleanFromInput('event_startyear');
    $event_starttimeh    = pnVarCleanFromInput('event_starttimeh');
    $event_starttimem    = pnVarCleanFromInput('event_starttimem');
    $event_startampm     = pnVarCleanFromInput('event_startampm');

    // location data
    $event_location      = pnVarCleanFromInput('event_location');
    $event_street1       = pnVarCleanFromInput('event_street1');
    $event_street2       = pnVarCleanFromInput('event_street2');
    $event_city          = pnVarCleanFromInput('event_city');
    $event_state         = pnVarCleanFromInput('event_state');
    $event_postal        = pnVarCleanFromInput('event_postal');
    $event_location_info = serialize(compact('event_location', 'event_street1', 'event_street2',
                                           'event_city', 'event_state', 'event_postal'));
    // contact data
    $event_contname      = pnVarCleanFromInput('event_contname');
    $event_conttel       = pnVarCleanFromInput('event_conttel');
    $event_contemail     = pnVarCleanFromInput('event_contemail');
    $event_website       = pnVarCleanFromInput('event_website');
    $event_fee           = pnVarCleanFromInput('event_fee');
    $event_patient_name  = pnVarCleanFromInput('patient_name');

    // event repeating data
    if( is_array($category) )
    {
        //$event_subject        =
        $event_desc         = $category['desc'];
        $event_category     = $category['id'];

        $event_duration     = $category['event_duration']; //seconds of the event
        $event_dur_hours     = $event_duration/(60 * 60);    //seconds divided by 60 seconds * 60 minutes
        $event_dur_minutes  = ($event_duration%(60 * 60))/60;
        $event_repeat         = $category['event_repeat'];
        $event_repeat_freq  = $category['event_repeat_freq'];
        $event_repeat_freq_type = $category['event_repeat_freq_type'];
        $event_repeat_on_num = $category['event_repeat_on_num'];
        $event_repeat_on_day = $category['event_repeat_on_day'];
        $event_repeat_on_freq = $category['event_repeat_on_freq'];
        $event_recurrspec = serialize(compact('event_repeat_freq', 'event_repeat_freq_type', 'event_repeat_on_num',
                                          'event_repeat_on_day', 'event_repeat_on_freq'));

        // event end information
        $multiple = $category['end_date_freq']." ";
        switch($category['end_date_type'])
        {
            case REPEAT_EVERY_DAY:
            case REPEAT_EVERY_WORK_DAY:    //end date is in days
                $multiple .= "days";
                break;
            case REPEAT_EVERY_WEEK;        //end date is in weeks
                $multiple .= "weeks";
                break;
            case REPEAT_EVERY_MONTH;    //end date is in months
                $multiple .= "months";
                break;
            case REPEAT_EVERY_YEAR:        //end date is in years
                $multiple .= "years";
                break;
        }

        $edate = strtotime(pnVarCleanFromInput('Date'));
        $event_startmonth     = date("m", $edate);
        $event_startday     = date("d", $edate);
        $event_startyear     = date("Y", $edate);
        $event_enddate = strtotime(pnVarCleanFromInput('Date')." + ".$multiple);
        $event_endmonth     = date("m",$event_enddate);
        $event_endday         = date("d",$event_enddate);
        $event_endyear      = date("Y",$event_enddate);
        $event_endtype      = $category['end_date_flag'];

        // I'm pretty sure this was a bug since 'event_all_day' appears nowhere
        // else in the code, but it's hard to tell WTF is going on.
//    $event_allday         = $category['event_all_day'];
        $event_allday         = $category['all_day'];
    }
    else
    {
        $event_dur_hours     = pnVarCleanFromInput('event_dur_hours');
        $event_dur_minutes  = pnVarCleanFromInput('event_dur_minutes');
        $event_duration     = (60*60*$event_dur_hours) + (60*$event_dur_minutes);
        $event_repeat         = pnVarCleanFromInput('event_repeat');
        $event_repeat_freq  = pnVarCleanFromInput('event_repeat_freq');
        $event_repeat_freq_type = pnVarCleanFromInput('event_repeat_freq_type');
        $event_repeat_on_num = pnVarCleanFromInput('event_repeat_on_num');
        $event_repeat_on_day = pnVarCleanFromInput('event_repeat_on_day');
        $event_repeat_on_freq = pnVarCleanFromInput('event_repeat_on_freq');
        $event_recurrspec = serialize(compact('event_repeat_freq', 'event_repeat_freq_type', 'event_repeat_on_num',
                                          'event_repeat_on_day', 'event_repeat_on_freq'));

        // event end information
        $event_endmonth     = pnVarCleanFromInput('event_endmonth');
        $event_endday         = pnVarCleanFromInput('event_endday');
        $event_endyear      = pnVarCleanFromInput('event_endyear');
        $event_endtype      = pnVarCleanFromInput('event_endtype');
        $event_allday         = pnVarCleanFromInput('event_allday');
    }

    // Added by Rod:
    if ($event_allday) {
        $event_starttimeh  = 0;
        $event_starttimem  = 0;
        $event_startampm   = 1;
        $event_dur_hours   = 24;
        $event_dur_minutes = 0;
        $event_duration    = 60 * 60 * $event_dur_hours;
    }

    $form_action = pnVarCleanFromInput('form_action');
    $pc_html_or_text = pnVarCleanFromInput('pc_html_or_text');
    $pc_event_id = pnVarCleanFromInput('pc_event_id');
    $data_loaded = pnVarCleanFromInput('data_loaded');
    $is_update   = pnVarCleanFromInput('is_update');
    $authid      = pnVarCleanFromInput('authid');

    //pennfirm uname matchup future fix
    //if(pnUserLoggedIn()) { $uname = pnUserGetVar('uname'); }
    //else { $uname = pnConfigGetVar('anonymous'); }
    $uname = $_SESSION['authUser'];
    if(!isset($event_repeat)) { $event_repeat = 0; }

    if(!isset($pc_event_id) || empty($pc_event_id) || $data_loaded) {
        // lets wrap all the data into array for passing to submit and preview functions
        $eventdata = compact('event_subject','event_desc','event_sharing','event_category','event_topic',
        'event_startmonth','event_startday','event_startyear','event_starttimeh','event_starttimem','event_startampm',
        'event_endmonth','event_endday','event_endyear','event_endtype','event_dur_hours','event_dur_minutes',
        'event_duration','event_allday','event_location','event_street1','event_street2','event_city','event_state',
        'event_postal','event_location_info','event_contname','event_conttel','event_contemail',
        'event_website','event_fee','event_repeat','event_repeat_freq','event_repeat_freq_type',
        'event_repeat_on_num','event_repeat_on_day','event_repeat_on_freq','event_recurrspec','uname',"event_userid","event_pid",
        'Date','year','month','day','pc_html_or_text','event_patient_name','event_pid');
        $eventdata['is_update'] = $is_update;
        $eventdata['pc_event_id'] = $pc_event_id;
        $eventdata['data_loaded'] = true;
        $eventdata['category'] = base64_encode(serialize($category));
    } else {
        $event =& postcalendar_userapi_pcGetEventDetails($pc_event_id);

        //echo "uname is:$uname  other name is: ".$event['uname'] . "<br />";
        if($uname != $event['uname']) {
            if (!validateGroupStatus($uname,getUsername($event['uname']))) {
                return _PC_CAN_NOT_EDIT;
            }
        }
        $eventdata['event_subject'] = $event['title'];
        $eventdata['event_desc'] = $event['hometext'];
        $eventdata['event_sharing'] = $event['sharing'];
        $eventdata['event_category'] = $event['catid'];
        $eventdata['event_topic'] = $event['topic'];
        $eventdata['event_startmonth'] = substr($event['eventDate'],5,2);
        $eventdata['event_startday'] = substr($event['eventDate'],8,2);
        $eventdata['event_startyear'] = substr($event['eventDate'],0,4);
        $eventdata['event_starttimeh'] = substr($event['startTime'],0,2);
        $eventdata['event_starttimem'] = substr($event['startTime'],3,2);
        $eventdata['event_startampm'] = $eventdata['event_starttimeh'] < 12 ? 1 : 2 ; //1 is am , 2 is pm
        $eventdata['event_endmonth'] = substr($event['endDate'],5,2);
        $eventdata['event_endday'] = substr($event['endDate'],8,2);
        $eventdata['event_endyear'] = substr($event['endDate'],0,4);
        $eventdata['event_endtype'] = $event['endDate'] == '0000-00-00' ? '0' : '1' ;
        $eventdata['event_dur_hours'] = $event['duration_hours'];
        $eventdata['event_dur_minutes'] = $event['duration_minutes'];
        $eventdata['event_duration'] = $event['duration'];
        $eventdata['event_allday'] = $event['alldayevent'];
        $loc_data = unserialize($event['location']);
        $eventdata['event_location'] = $loc_data['event_location'];
        $eventdata['event_street1'] = $loc_data['event_street1'];
        $eventdata['event_street2'] = $loc_data['event_street2'];
        $eventdata['event_city'] = $loc_data['event_city'];
        $eventdata['event_state'] = $loc_data['event_state'];
        $eventdata['event_postal'] = $loc_data['event_postal'];
        $eventdata['event_location_info'] = $loc_data;
        $eventdata['event_contname'] = $event['contname'];
        $eventdata['event_conttel'] = $event['conttel'];
        $eventdata['event_contemail'] = $event['contemail'];
        $eventdata['event_website'] = $event['website'];
        $eventdata['event_fee'] = $event['fee'];
        $eventdata['event_repeat'] = $event['recurrtype'];
        $rspecs = unserialize($event['recurrspec']);
        $eventdata['event_repeat_freq'] = $rspecs['event_repeat_freq'];
        $eventdata['event_repeat_freq_type'] = $rspecs['event_repeat_freq_type'];
        $eventdata['event_repeat_on_num'] = $rspecs['event_repeat_on_num'];
        $eventdata['event_repeat_on_day'] = $rspecs['event_repeat_on_day'];
        $eventdata['event_repeat_on_freq'] = $rspecs['event_repeat_on_freq'];
        $eventdata['event_recurrspec'] = $rspecs;
        $eventdata['uname'] = $uname;
        $eventdata['event_userid'] = $event['event_userid'];
        $eventdata['event_pid'] = $event['pid'];
        $eventdata['event_aid'] = $event['aid'];
        $eventdata['Date'] = $Date;
        $eventdata['year'] = $year;
        $eventdata['month'] = $month;
        $eventdata['day'] = $day;
        $eventdata['is_update'] = true;
        $eventdata['pc_event_id'] = $pc_event_id;
        $event_data['patient_name'] = $event_patient_name;
        $eventdata['data_loaded'] = true;
        $eventdata['pc_html_or_text'] = $pc_html_or_text;
        $eventdata['category'] = base64_encode(serialize($category));
    }

    // lets get the module's information
    $modinfo = pnModGetInfo(pnModGetIDFromName(__POSTCALENDAR__));
    $categories = pnModAPIFunc(__POSTCALENDAR__,'user','getCategories');
    $output->tabindex=1;

    //================================================================
    //    ERROR CHECKING
    //================================================================
    // removed event_desc as a required_var

    $required_vars = array('event_subject');
    $required_name = array(_PC_EVENT_TITLE,_PC_EVENT_DESC);
    $error_msg = '';
    $output->SetOutputMode(_PNH_RETURNOUTPUT);
    $reqCount = count($required_vars);
    //print_r($eventdata);
    for ($r=0; $r<$reqCount; $r++) {
        if(empty($$required_vars[$r]) || !preg_match('/\S/i',$$required_vars[$r])) {
            $error_msg .= $output->Text('<b>'.$required_name[$r].'</b> '._PC_SUBMIT_ERROR4);
            $error_msg .= $output->Linebreak();
        }
    }
    unset($reqCount);
    // check repeating frequencies
    if($event_repeat == REPEAT) {

        //can't have a repeating event that doesnt have an end date
        if ($event_endtype == 0) {
            $error_msg .= $output->Text("Repeating events must have an end date set.");
            $error_msg .= $output->Linebreak();
        }
        if(!isset($event_repeat_freq) ||  $event_repeat_freq < 1 || empty($event_repeat_freq)) {
            $error_msg .= $output->Text(_PC_SUBMIT_ERROR5);
            $error_msg .= $output->Linebreak();
        } elseif(!is_numeric($event_repeat_freq)) {
            $error_msg .= $output->Text(_PC_SUBMIT_ERROR6);
            $error_msg .= $output->Linebreak();
        }
    } elseif($event_repeat == REPEAT_ON) {
        //can't have a repeating event that doesnt have an end date
        if ($event_endtype == 0) {
            $error_msg .= $output->Text("Repeating events must have an end date set.");
            $error_msg .= $output->Linebreak();
        }
        if(!isset($event_repeat_on_freq) || $event_repeat_on_freq < 1 || empty($event_repeat_on_freq)) {
            $error_msg .= $output->Text(_PC_SUBMIT_ERROR5);
            $error_msg .= $output->Linebreak();
        } elseif(!is_numeric($event_repeat_on_freq)) {
            $error_msg .= $output->Text(_PC_SUBMIT_ERROR6);
            $error_msg .= $output->Linebreak();
        }
    }
    // check date validity
    if(_SETTING_TIME_24HOUR) {
        $startTime = $event_starttimeh.':'.$event_starttimem;
        $endTime =   $event_endtimeh.':'.$event_endtimem;
    } else {
        if($event_startampm == _AM_VAL) {
            $event_starttimeh = $event_starttimeh == 12 ? '00' : $event_starttimeh;
        } else {
            $event_starttimeh =  $event_starttimeh != 12 ? $event_starttimeh+=12 : $event_starttimeh;
        }
        $startTime = $event_starttimeh.':'.$event_starttimem;
    }
    $sdate = strtotime($event_startyear.'-'.$event_startmonth.'-'.$event_startday);
    $edate = strtotime($event_endyear.'-'.$event_endmonth.'-'.$event_endday);
    $tdate = strtotime(date('Y-m-d'));
    if($edate < $sdate && $event_endtype == 1) {
        $error_msg .= $output->Text(_PC_SUBMIT_ERROR1);
        $error_msg .= $output->Linebreak();
    }
    if(!checkdate($event_startmonth,$event_startday,$event_startyear)) {
        $error_msg .= $output->Text(_PC_SUBMIT_ERROR2 . " '$event_startyear-$event_startmonth-$event_startday'");
        $error_msg .= $output->Linebreak();
    }
    if(!checkdate($event_endmonth,$event_endday,$event_endyear)) {
        $error_msg .= $output->Text(_PC_SUBMIT_ERROR3 . " '$event_endyear-$event_endmonth-$event_endday'");
        $error_msg .= $output->Linebreak();
    }

    //check limit on category
    if(($ret = checkCategoryLimits($eventdata)) != null)
    {
        $error_msg .= $output->Text("This category has a limit of $ret[limit] between $ret[start] and $ret[end] which you have exceeded.");
        $error_msg .= $output->Linebreak();
        //$output->Text(pnModAPIFunc('PostCalendar','user','buildSubmitForm',$eventdata));
        //return $output->GetOutput();
    }
    //echo "fa: " . $form_action . " double_book: " . pnVarCleanFromInput("double_book") . " update: " . $eventdata['is_update'] . " em: " . $error_msg;
    //event collision check

    if($form_action == "commit" && pnVarCleanFromInput("double_book") != 1 && !$eventdata['is_update'] && empty($error_msg) ) {
        //check on new shceduling events(in or out of office) to make sure that
        //you don't have more than one set per day
        //event category 1 is in office, event category 2 is out of office

        if ($eventdata['event_category'] == 2 || $eventdata['event_category'] == 3)  {
            $searchargs = array();
            $searchargs['start'] = $eventdata['event_startmonth'] . "/" . $eventdata['event_startday'] ."/". $eventdata['event_startyear'];
            $searchargs['end'] = $eventdata['event_endmonth'] . "/" . $eventdata['event_endday'] ."/". $eventdata['event_endyear'];
            $searchargs['provider_id'] = $eventdata['event_userid'];

            //faFLag uses pcgeteventsfa, which can search on provider
            $searchargs['faFlag'] = true;
            $searchargs['s_keywords'] = " (a.pc_catid = 2 OR a.pc_catid = 3) ";
            //print_r($searchargs);

            $eventsByDate =& postcalendar_userapi_pcGetEvents($searchargs);
            $ekey = md5($event_data['subject'] . date("U") . rand(0,1000));
            $oldstatus = $eventdata['event_status'];
            $oldtitle = $eventdata['event_subject'];
            $old_patient_name = $eventdata['patient_name'];
            $old_dur_hours = $eventdata['event_dur_hours'];
            $old_dur_min = $eventdata['event_dur_minutes'];
            $old_duration = $eventdata['event_duration'];
            $eventdata['event_subject'] = add_escape_custom($ekey);
            $eventdata['event_status'] = _EVENT_TEMPORARY;

            if (!pnModAPIFunc(__POSTCALENDAR__,'user','submitEvent',$eventdata)) {
                $error_msg .= $output->Text('<center><div style="padding:5px; border:1px solid red; background-color: pink;">');
                $error_msg .= $output->Text("<b>The system was unable to check you event for conflicts with other events because there was a problem with your database.</b><br />");
                $error_msg .= $output->Text('</div></center>');
                $error_msg .= $output->Linebreak();
                $error_msg .= $output->Text($dbconn->ErrorMsg());
            }
            $searchargs['s_keywords'] = " (a.pc_catid = 2 OR a.pc_catid = 3) AND a.pc_title = '" . $eventdata['event_subject']  . "' ";
            $searchargs['event_status'] = _EVENT_TEMPORARY;
            $submitEventByDate =& postcalendar_userapi_pcGetEvents($searchargs);

            if(!delete_event($ekey)) {
                $error_msg .= $output->Text('<center><div style="padding:5px; border:1px solid red; background-color: pink;">');
                $error_msg .= $output->Text("<b>The system was unable to delete a temporary record it created, this may have left the database in an inconsistent state.</b><br />");
                $error_msg .= $output->Text('</div></center>');
                $error_msg .= $output->Linebreak();
                $error_msg .= $output->Text($dbconn->ErrorMsg());
            }

            $eventdata['event_status'] = $oldstatus;
            $eventdata['event_subject'] = $oldtitle;
            $eventdata['patient_name '] = $old_patient_name;
            $eventdata['event_dur_hours'] = $old_dur_hour;
            $eventdata['event_dur_minutes'] = $old_dur_min;

            foreach ($submitEventByDate as $date => $newevent) {
                if (count($eventsByDate[$date]) > 0 && count($newevent) > 0) {
                    foreach ($eventsByDate[$date] as $con_event)  {
                        if ($con_event['catid'] == $newevent[0]['catid']) {
                            $error_msg .= $output->Text('There is a conflict on ' . $date . ' with event ' . $con_event['title']);
                            $error_msg .= $output->Linebreak();
                        }
                    }
                }
            }
            /*echo "<br /><br />";
            print_r($eventsByDate);
            echo "<br /><br />";
            print_r($submitEventByDate);*/
        }

        $colls = checkEventCollision($eventdata);
        if (count($colls) > 0) {
            foreach ($colls as $coll) {
                $error_msg .= $output->Text("Event Collides with: " . $coll['title'] . " at " . date("g:i a", strtotime($coll['startTime'])) . "<br />");
                $error_msg .= $output->Linebreak();
            }
            $error_msg .= $output->Text("Submit again to \"Double Book\" <br />To change values click back in your browser.");
            $error_msg .= $output->Linebreak();
            // the following line will display "DOUBLE BOOKED" if when adding an event there is a collistion with anothe appointment
            //$eventdata['event_subject'] = "DOUBLE BOOKED " . $eventdata['event_subject'];
            $eventdata['double_book'] = 1;
        }
    }

    $output->SetOutputMode(_PNH_KEEPOUTPUT);
    if($form_action == 'preview') {
        //================================================================
        //  Preview the event
        //================================================================
        // check authid
        if (!pnSecConfirmAuthKey()) { return(_NO_DIRECT_ACCESS); }
        if(!empty($error_msg)) {
            $preview = false;
            $output->Text('<table border="0" width="100%" cellpadding="1" cellspacing="0"><tr><td bgcolor="red">');
            $output->Text('<table border="0" width="100%" cellpadding="1" cellspacing="0"><tr><td bgcolor="pink">');
            $output->Text('<center><b>'._PC_SUBMIT_ERROR.'</b></center>');
            $output->Linebreak();
            $output->Text($error_msg);
            $output->Text('</td></td></table>');
            $output->Text('</td></td></table>');
            $output->Linebreak(2);
        } else {
            $output->Text(pnModAPIFunc(__POSTCALENDAR__,'user','eventPreview',$eventdata));
            $output->Linebreak();
        }
    } elseif($form_action == 'commit') {

        //================================================================
        //  Enter the event into the DB
        //================================================================
        if (!empty($error_msg)) {
            if (!pnSecConfirmAuthKey(true)) { return(_NO_DIRECT_ACCESS); }
        }
        else {
            if (!pnSecConfirmAuthKey()) { return(_NO_DIRECT_ACCESS); }
        }
        if(!empty($error_msg)) {
            $preview = false;
            $output->Text('<table border="0" width="100%" cellpadding="1" cellspacing="0"><tr><td bgcolor="red">');
            $output->Text('<table border="0" width="100%" cellpadding="1" cellspacing="0"><tr><td bgcolor="pink">');
            $output->Text('<center><b>'._PC_SUBMIT_ERROR.'</b></center>');
            $output->Linebreak();
            $output->Text($error_msg);
            $output->Text('</td></td></table>');
            $output->Text('</td></td></table>');
            $output->Linebreak(2);
        } else {
            if (!pnModAPIFunc(__POSTCALENDAR__,'user','submitEvent',$eventdata)) {
                $output->Text('<center><div style="padding:5px; border:1px solid red; background-color: pink;">');
                $output->Text("<b>"._PC_EVENT_SUBMISSION_FAILED."</b>");
                $output->Text('</div></center>');
                $output->Linebreak();
                $output->Text($dbconn->ErrorMsg());
            } else {
                // clear the Smarty cache
                $tpl = new pcSmarty();
                $tpl->clear_all_cache();
                $output->Text('<center><div style="padding:5px; border:1px solid green; background-color: lightgreen;">');
                if($is_update) {
                    $output->Text("<b>"._PC_EVENT_EDIT_SUCCESS."</b>");
                } else {
                    $output->Text("<b>"._PC_EVENT_SUBMISSION_SUCCESS."</b>");
                }
                $output->Text('</div></center>');
                $output->Linebreak();
                // clear the form vars
                $event_subject=$event_desc=$event_sharing=$event_category=$event_topic=
                $event_startmonth=$event_startday=$event_startyear=$event_starttimeh=$event_starttimem=$event_startampm=
                $event_endmonth=$event_endday=$event_endyear=$event_endtype=$event_dur_hours=$event_dur_minutes=
                $event_duration=$event_allday=$event_location=$event_street1=$event_street2=$event_city=$event_state=
                $event_postal=$event_location_info=$event_contname=$event_conttel=$event_contemail=
                $event_website=$event_fee=$event_repeat=$event_repeat_freq=$event_repeat_freq_type=
                $event_repeat_on_num=$event_repeat_on_day=$event_repeat_on_freq=$event_recurrspec=$uname=
                $Date=$year=$month=$day=$pc_html_or_text=$event_patient_name=$evnet_pid=null;
                $is_update = false;
                $pc_event_id = 0;
    
                //$_SESSION['category'] = "";
                // lets wrap all the data into array for passing to submit and preview functions
                $eventdata = compact('event_subject','event_desc','event_sharing','event_category','event_topic',
                'event_startmonth','event_startday','event_startyear','event_starttimeh','event_starttimem','event_startampm',
                'event_endmonth','event_endday','event_endyear','event_endtype','event_dur_hours','event_dur_minutes',
                'event_duration','event_allday','event_location','event_street1','event_street2','event_city','event_state',
                'event_postal','event_location_info','event_contname','event_conttel','event_contemail',
                'event_website','event_fee','event_repeat','event_repeat_freq','event_repeat_freq_type',
                'event_repeat_on_num','event_repeat_on_day','event_repeat_on_freq','event_recurrspec','uname',
                'Date','year','month','day','pc_html_or_text','is_update','pc_event_id','event_patient_name');
                //if no using the no_nav format then show form again after submit
                if (pnVarCleanFromInput("no_nav") == 1) {
                    return $output->GetOutput();
                }
            }
        }
    }

    $output->Text(pnModAPIFunc('PostCalendar','user','buildSubmitForm',$eventdata));
    return $output->GetOutput();
}


/**
 * search events
 */
function postcalendar_user_search()
{
    if (!(bool)PC_ACCESS_OVERVIEW) { return _POSTCALENDARNOAUTH; }

    $tpl = new pcSmarty();
    $k = formData("pc_keywords","R"); //from library/formdata.inc.php
    $k_andor = pnVarCleanFromInput('pc_keywords_andor');
    $pc_category = pnVarCleanFromInput('pc_category');
    $pc_facility = pnVarCleanFromInput('pc_facility');
    $pc_topic = pnVarCleanFromInput('pc_topic');
    $submit = pnVarCleanFromInput('submit');
    $event_dur_hours = pnVarCleanFromInput('event_dur_hours');
    $event_dur_minutes = pnVarCleanFromInput('event_dur_minutes');
    $start = pnVarCleanFromInput('start');
    $end = pnVarCleanFromInput('end');

    // get list of categories for the user to choose from
    $categories = postcalendar_userapi_getCategories();
    $cat_options = '';
    foreach($categories as $category) {
        $selected = "";
        if ($pc_category == $category[id]) { $selected = " SELECTED "; }
	//modified 8/09 by BM to allow translation if applicable
        $cat_options .= "<option value=\"$category[id]\" $selected>" . xl_appt_category($category[name]) . "</option>";
    }
    $tpl->assign_by_ref('CATEGORY_OPTIONS',$cat_options);

    $tpl->assign('event_dur_hours', $event_dur_hours);
    $tpl->assign('event_dur_minutes', $event_dur_minutes);

    // create default start and end dates for the search form
    if (isset($start) && $start != "") $tpl->assign('DATE_START', $start);
    else $tpl->assign('DATE_START', date("m/d/Y"));
    if (isset($end) && $end!= "") $tpl->assign('DATE_END', $end);
    else $tpl->assign('DATE_END', date("m/d/Y", strtotime("+7 Days", time())));

    // then override the setting if we have a value from the submitted form
    $ProviderID = pnVarCleanFromInput("provider_id");
    if (is_numeric($ProviderID)) { $tpl->assign('ProviderID', $ProviderID);; }
    elseif ($ProviderID == "_ALL_") { } // do nothing
    else { $tpl->assign('ProviderID', ""); }

    $provinfo = getProviderInfo();
    $tpl->assign('providers', $provinfo);
    // build a list of provider-options for the select box on the input form -- JRM
    $provider_options = "<option value='_ALL_' ";
    if ($ProviderID == "_ALL_") { $provider_options .= " SELECTED "; }
    $provider_options .= ">" . xl('All Providers') . "</option>";
    foreach ($provinfo as $provider) {
        $selected = "";
        // if we don't have a ProviderID chosen, pick the first one from the 
        // pc_username Session variable
        if ($ProviderID == "") {
            // that variable stores the 'username' and not the numeric 'id'
            if ($_SESSION['pc_username'][0] == $provider['username']) {
                $selected = " SELECTED ";
            }
        }
        else if ($ProviderID == $provider['id']) { $selected = " SELECTED "; }
        $provider_options .= "<option value=\"".$provider['id']."\" ".$selected.">";
        $provider_options .= $provider['lname'].", ".$provider['fname']."</option>";
    }
    $tpl->assign_by_ref('PROVIDER_OPTIONS',$provider_options);

    // build a list of facility options for the select box on the input form -- JRM
    $facilities = getFacilities();
    $fac_options = "<option value=''>" . xl('All Facilities') . "</option>";
    foreach ($facilities as $facility) {
        $selected = "";
        if ($facility['id'] == $pc_facility) $selected = " SELECTED ";
        $fac_options .= "<option value=\"".$facility['id']."\" ".$selected.">";
        $fac_options .= $facility['name']."</option>";
    }
    $tpl->assign_by_ref('FACILITY_OPTIONS',$fac_options);

    $PatientID = pnVarCleanFromInput("patient_id");
    // limit the number of results returned by getPatientPID
    // this helps to prevent the server from stalling on a request with
    // no PID and thousands of PIDs in the database -- JRM
    // the function getPatientPID($pid, $given, $orderby, $limit, $start) <-- defined in library/patient.inc
    $plistlimit = 500;
    if (is_numeric($PatientID)) {
      $tpl->assign('PatientList', getPatientPID(array('pid'=>$PatientID, 'limit'=>$plistlimit)));
    }
    else {
      $tpl->assign('PatientList', getPatientPID(array('limit' =>$plistlimit)));
    }
    $event_endday = pnVarCleanFromInput("event_endday");
    $event_endmonth = pnVarCleanFromInput("event_endmonth");
    $event_endyear = pnVarCleanFromInput("event_endyear");

    $event_startday = pnVarCleanFromInput("event_startday");
    $event_startmonth = pnVarCleanFromInput("event_startmonth");
    $event_startyear = pnVarCleanFromInput("event_startyear");
    if($event_startday > $event_endday) { $event_endday = $event_startday; }
    if($event_startmonth > $event_endmonth) { $event_endmonth = $event_startmonth; }
    if($event_startyear > $event_endyear) { $event_endyear = $event_startyear; }

    $tpl->assign('patient_id', $PatientID);
    $tpl->assign('provider_id', $ProviderID);
    $tpl->assign("event_category", pnVarCleanFromInput("event_category"));
    $tpl->assign("event_subject", pnVarCleanFromInput("event_subject"));
    $output = new pnHTML();
    $output->SetOutputMode(_PNH_RETURNOUTPUT);
    if(_SETTING_USE_INT_DATES) {

        $sel_data = pnModAPIFunc(__POSTCALENDAR__,'user','buildDaySelect',array('pc_day'=>$day,'selected'=>$event_startday));
        $formdata = $output->FormSelectMultiple('event_startday', $sel_data);
        $sel_data = pnModAPIFunc(__POSTCALENDAR__,'user','buildMonthSelect',array('pc_month'=>$month,'selected'=>$event_startmonth));
        $formdata .= $output->FormSelectMultiple('event_startmonth', $sel_data);
    } else {
        $sel_data = pnModAPIFunc(__POSTCALENDAR__,'user','buildMonthSelect',array('pc_month'=>$month,'selected'=>$event_startmonth));
        $formdata = $output->FormSelectMultiple('event_startmonth', $sel_data);
        $sel_data = pnModAPIFunc(__POSTCALENDAR__,'user','buildDaySelect',array('pc_day'=>$day,'selected'=>$event_startday));
        $formdata .= $output->FormSelectMultiple('event_startday', $sel_data);
    }
    $sel_data = pnModAPIFunc(__POSTCALENDAR__,'user','buildYearSelect',array('pc_year'=>$year,'selected'=>$event_startyear));
    $formdata .= $output->FormSelectMultiple('event_startyear', $sel_data);
    $output->SetOutputMode(_PNH_KEEPOUTPUT);
    $tpl->assign('SelectDateTimeStart', $formdata);
    $output->SetOutputMode(_PNH_RETURNOUTPUT);
    if(_SETTING_USE_INT_DATES) {
        $sel_data = pnModAPIFunc(__POSTCALENDAR__,'user','buildDaySelect',array('pc_day'=>$day,'selected'=>$event_endday));
        $formdata = $output->FormSelectMultiple('event_endday', $sel_data);
        $sel_data = pnModAPIFunc(__POSTCALENDAR__,'user','buildMonthSelect',array('pc_month'=>$month,'selected'=>$event_endmonth));
        $formdata .= $output->FormSelectMultiple('event_endmonth', $sel_data);
    } else {
        $sel_data = pnModAPIFunc(__POSTCALENDAR__,'user','buildMonthSelect',array('pc_month'=>$month,'selected'=>$event_endmonth));
        $formdata = $output->FormSelectMultiple('event_endmonth', $sel_data);
        $sel_data = pnModAPIFunc(__POSTCALENDAR__,'user','buildDaySelect',array('pc_day'=>$day,'selected'=>$event_endday ));
        $formdata .= $output->FormSelectMultiple('event_endday', $sel_data);
    }
    $sel_data = pnModAPIFunc(__POSTCALENDAR__,'user','buildYearSelect',array('pc_year'=>$year,'selected'=>$event_endyear));
    $formdata .= $output->FormSelectMultiple('event_endyear', $sel_data);
    $output->SetOutputMode(_PNH_KEEPOUTPUT);
    $tpl->assign('SelectDateTimeEnd', $formdata);
    $output = null;
    if(_SETTING_DISPLAY_TOPICS) {
        $topics = postcalendar_userapi_getTopics();
        $top_options = '';
        foreach($topics as $topic) {
            $top_options .= "<option value=\"$topic[id]\">$topic[text]</option>";
        }
        $tpl->assign_by_ref('TOPIC_OPTIONS',$top_options);
    }
    //=================================================================
    //  Find out what Template we're using
    //=================================================================
    $template_name = _SETTING_TEMPLATE;
    if(!isset($template_name)) {
        $template_name = 'default';
    }
    //=================================================================
    //  Output the search form
    //=================================================================
    $tpl->assign('FORM_ACTION',pnModURL(__POSTCALENDAR__,'user','search'));
    //=================================================================
    //  Perform the search if we have data
    //=================================================================
    if(!empty($submit) && strtolower($submit) == "find first") {
        // not sure how we get here...
        $searchargs = array();
        $searchargs['start'] = pnVarCleanFromInput("event_startmonth") . "/" . pnVarCleanFromInput("event_startday") ."/". pnVarCleanFromInput("event_startyear");
        $searchargs['end'] = pnVarCleanFromInput("event_endmonth") . "/" . pnVarCleanFromInput("event_endday") ."/". pnVarCleanFromInput("event_endyear");
        $searchargs['provider_id'] = pnVarCleanFromInput("provider_id");
        $searchargs['faFlag'] = true;
        //print_r($searchargs);
        //echo "<br />";
        //set defaults to current week if empty
        if ($searchargs['start'] == "//") {
            $searchargs['start'] = date("m/d/Y");
        }
        if ($searchargs['end'] == "//") {
            $searchargs['end'] = date("m/d/Y", strtotime("+7 Days", strtotime($searchargs['start'])));
        }
        //print_r($searchargs);
        $eventsByDate =& postcalendar_userapi_pcGetEvents($searchargs);
        //print_r($eventsByDate);
        $found = findFirstAvailable($eventsByDate);
        $tpl->assign('available_times',$found);
        //print_r($_POST);

        $tpl->assign('SEARCH_PERFORMED',true);
        $tpl->assign('A_EVENTS',$eventsByDate);
    }
    if(!empty($submit) && strtolower($submit) == "listapps") {
        // not sure how we get here...
        $searchargs = array();
        $searchargs['start'] = date("m/d/Y");
        $searchargs['end'] = date("m/d/Y",strtotime("+1 year",strtotime($searchargs['start'])));
        $searchargs['patient_id'] = pnVarCleanFromInput("patient_id");
        $searchargs['listappsFlag'] = true;

        $sqlKeywords .= "(a.pc_pid = '" . pnVarCleanFromInput("patient_id") . "' )";

        $searchargs['s_keywords'] = $sqlKeywords;
        //print_r($searchargs);
        $eventsByDate =& postcalendar_userapi_pcGetEvents($searchargs);
        //print_r($eventsByDate);
        $tpl->assign('appointments',$eventsByDate);
        //print_r($_POST);

        $tpl->assign('SEARCH_PERFORMED',true);
        $tpl->assign('A_EVENTS',$eventsByDate);
    }
    elseif(!empty($submit)) {


        // we get here by searching via the PostCalendar search
        $sqlKeywords = '';
        $keywords = explode(' ',$k);
        // build our search query
        foreach($keywords as $word) {
            if(!empty($sqlKeywords)) $sqlKeywords .= " $k_andor ";
            $sqlKeywords .= '(';
            $sqlKeywords .= "pd.lname LIKE '%$word%' OR ";
            $sqlKeywords .= "pd.fname LIKE '%$word%' OR ";
            $sqlKeywords .= "u.lname LIKE '%$word%' OR ";
            $sqlKeywords .= "u.fname LIKE '%$word%' OR ";
            $sqlKeywords .= "a.pc_title LIKE '%$word%' OR ";
            $sqlKeywords .= "a.pc_hometext LIKE '%$word%' OR ";
            $sqlKeywords .= "a.pc_location LIKE '%$word%'";
            $sqlKeywords .= ') ';
        }


        if(!empty($pc_category)) {
            $s_category = "a.pc_catid = '$pc_category'";
        }

        if(!empty($pc_topic)) {
            $s_topic = "a.pc_topic = '$pc_topic'";
        }

        $searchargs = array();
        if(!empty($sqlKeywords)) $searchargs['s_keywords'] = $sqlKeywords;
        if(!empty($s_category)) $searchargs['s_category'] = $s_category;
        if(!empty($s_topic)) $searchargs['s_topic'] = $s_topic;
        
        // some new search parameters introduced in the ajax_search form...  JRM March 2008

        // the ajax_search form has form parameters for 'start' and 'end' already built in
        // so use them if available
        $tmpDate = pnVarCleanFromInput("start");
        if (isset($tmpDate) && $tmpDate != "")
            $searchargs['start'] = pnVarCleanFromInput("start");
        else $searchargs['start'] = "//";
        $tmpDate = pnVarCleanFromInput("end");
        if (isset($tmpDate) && $tmpDate != "")
            $searchargs['end'] = pnVarCleanFromInput("end");
        else $searchargs['end'] = "//";

        // we can limit our search by provider -- JRM March 2008
        if (isset($ProviderID) && $ProviderID != "") { // && $ProviderID != "_ALL_") {
            $searchargs['provider_id'] = array();
            array_push($searchargs['provider_id'], $ProviderID);
        }
        $eventsByDate =& postcalendar_userapi_pcGetEvents($searchargs);

        // we can limit our search by facility -- JRM March 2008
        if (isset($pc_facility) && $pc_facility != "") {
            $searchargs['pc_facility'] = $pc_facility;
        }

        //print_r($eventsByDate);
        $tpl->assign('SEARCH_PERFORMED',true);
        $tpl->assign('A_EVENTS',$eventsByDate);
    }
    $tpl->caching = false;
    $tpl->assign('STYLE',$GLOBALS['style']);
    $pageSetup =& pnModAPIFunc(__POSTCALENDAR__,'user','pageSetup');
    if (pnVarCleanFromInput("no_nav") == 1) {
      $return = $pageSetup . $tpl->fetch($template_name.'/user/findfirst.html');
    }
    elseif (pnVarCleanFromInput("no_nav") == 2) {
      $return = $pageSetup . $tpl->fetch($template_name.'/user/listapps.html');
    }
    else {
     $return = $pageSetup . $tpl->fetch($template_name.'/user/search.html');
    }
    return $return;
}

function checkCategoryLimits($eventdata)
{
    extract($eventdata);
    //print_r($eventdata);
    //print "$event_starttimeh:$event_starttimem";

    $limits = & pnModAPIFunc(__POSTCALENDAR__,'user','getCategoryLimits');
    //print_r($limits);
    foreach($limits as $limit)
    {
        if($limit['catid'] == $event_category) //have a limit
        {
            //print_r($limit);
            $sdate = ($event_startmonth.'/'.$event_startday.'/'
                        .$event_startyear);
            $edate = $sdate;
            $stime = date("H:i:00",strtotime($limit['startTime']));
            $etime = date("H:i:00",strtotime($limit['endTime']));
            if($is_update)
            {
                $searchText = "a.pc_eid != '$pc_event_id' AND ";
            }
            //echo "stime is: $stime, etime is: $etime sdate is: $sdate edate is: $edate<br />";
            $a = array('s_category' => " a.pc_catid = $event_category",'start'=>$edate,
                'end'=>$sdate, 'stime' => $stime, 'etime' => $etime,'providerID'=>$event_userid,
                's_keywords'=>$searchText."a.pc_starttime >= '$stime' AND a.pc_endtime <= '$etime'");
            $eventsByDate =& postcalendar_userapi_pcGetEvents($a);
            //print_r($eventsByDate);
            $ret = null;
            foreach ($eventsByDate as $day)
            {
                //if event time falls within limit time check
                //hour from forms is 12 not 24 format, convert here
                if($event_startampm == 2 && $event_starttimeh != 12)
                    $event_starttimeh += 12;
                 elseif ($event_startampm == 1 && $event_starttimeh == 12)
                    $event_starttimeh -= 12;
                $event_starttime = date("H:i:00",strtotime($event_starttimeh.":".$event_starttimem.":"."00"));
                $event_endtime = date("H:i:00",strtotime($event_endtimeh.":".$event_endtimem.":"."00"));

                if( $event_starttime >= $limit['startTime'])
                {
                    $numToday = count($day);

                    if($numToday >= $limit['limit'])
                    {
                        //reached limit
                        $ret = array("start"=>$limit['startTime'],"end"=>$limit['endTime'],
                            "limit"=>$limit['limit']);
                        return $ret;
                    }
                }//if in limit time span
            }
        }
    }

    return null;
 }
    /*list($dbconn) = pnDBGetConn();
    $pntable = pnDBGetTables();
    $event_table = $pntable['postcalendar_events'];
    //get all of todays events
    $starting_date = date('m/d/Y',mktime(0,0,0,$the_month,$the_day,$the_year));
    $ending_date   = date('m/d/Y',mktime(0,0,0,$the_month,$the_day,$the_year));
    //select all of the limits
    $limits = & pnModAPIFunc(__POSTCALENDAR__,'user','getCategoryLimits');
    //for each limit for this category id, make sure you won't exceed the limit
    foreach($limits as $limit)
    {
        if(($key = array_search($eventdata['cat_id'])) != false)
        {
            $sql = "SELECT count(pc_eid) from $event_table where "
        }
    }

}*/
?>
