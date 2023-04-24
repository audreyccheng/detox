<?php

 // Copyright (C) 2011 Ken Chapple
 //
 // This program is free software; you can redistribute it and/or
 // modify it under the terms of the GNU General Public License
 // as published by the Free Software Foundation; either version 2
 // of the License, or (at your option) any later version.

 // Holds library functions (and hashes) used by the appointment reporting module



require_once(dirname(__FILE__)."/encounter_events.inc.php");
require_once(dirname(__FILE__)."/../interface/main/calendar/modules/PostCalendar/pnincludes/Date/Calc.php");


$COMPARE_FUNCTION_HASH = array(
    'doctor' => 'compareAppointmentsByDoctorName',
    'patient' => 'compareAppointmentsByPatientName',
    'pubpid' => 'compareAppointmentsByPatientId',
    'date' => 'compareAppointmentsByDate',
    'time' => 'compareAppointmentsByTime',
    'type' => 'compareAppointmentsByType',
    'comment' => 'compareAppointmentsByComment',
    'status' => 'compareAppointmentsByStatus',
    'completed' => 'compareAppointmentsByCompletedDrugScreen',
    'trackerstatus' => 'compareAppointmentsByTrackerStatus'
);

$ORDERHASH = array(
  	'doctor' => array( 'doctor', 'date', 'time' ),
  	'patient' => array( 'patient', 'date', 'time' ),
  	'pubpid' => array( 'pubpid', 'date', 'time' ),
  	'date' => array( 'date', 'time', 'type', 'patient' ),
  	'time' => array( 'time', 'date', 'patient' ),
  	'type' => array( 'type', 'date', 'time', 'patient' ),
  	'comment' => array( 'comment', 'date', 'time', 'patient' ),
    'status' => array( 'status', 'date', 'time', 'patient' ),
    'completed' => array( 'completed', 'date', 'time', 'patient' ),
    'trackerstatus' => array( 'trackerstatus', 'date', 'time', 'patient' ),
);

/*Arrays for the interpretation of recurrence patterns.*/
$REPEAT_FREQ = array(
	'1' => xl('Every'),
	'2' => xl('Every 2nd'),
	'3' => xl('Every 3rd'),
	'4' => xl('Every 4th'),
	'5' => xl('Every 5th'),
	'6' => xl('Every 6th')
);

$REPEAT_FREQ_TYPE = array(
	'0' => xl('day'),
	'1' => xl('week'),
	'2' => xl('month'),
	'3' => xl('year'),
	'4' => xl('workday')
);

$REPEAT_ON_NUM = array(
	'1' => xl('1st'),
	'2' => xl('2nd'),
	'3' => xl('3rd'),
	'4' => xl('4th'),
	'5' => xl('Last')
);

$REPEAT_ON_DAY = array(
	'0' => xl('Sunday'),
	'1' => xl('Monday'),
	'2' => xl('Tuesday'),
	'3' => xl('Wednesday'),
	'4' => xl('Thursday'),
	'5' => xl('Friday'),
	'6' => xl('Saturday')
);

function fetchEvents( $from_date, $to_date, $where_param = null, $orderby_param = null, $tracker_board = false, $nextX = 0, $bind_param = null, $query_param = null )
{
	
  $sqlBindArray = array();

  if($query_param) {

    $query = $query_param;

    if($bind_param) $sqlBindArray = $bind_param;

  } else {
    //////
    if($nextX) {

      $where =
        "((e.pc_endDate >= ? AND e.pc_recurrtype > '0') OR " .
        "(e.pc_eventDate >= ?))";

      array_push($sqlBindArray, $from_date, $from_date);

    } else {
    //////
      $where =
        "((e.pc_endDate >= ? AND e.pc_eventDate <= ? AND e.pc_recurrtype > '0') OR " .
        "(e.pc_eventDate >= ? AND e.pc_eventDate <= ?))";

      array_push($sqlBindArray, $from_date, $to_date, $from_date, $to_date);

    }

    if ( $where_param ) $where .= $where_param;
	
    $order_by = "e.pc_eventDate, e.pc_startTime";
    if ( $orderby_param ) {
       $order_by = $orderby_param;
    }

    // Tracker Board specific stuff
    $tracker_fields = '';
    $tracker_joins = '';
    if ($tracker_board) {
    $tracker_fields = "e.pc_room, e.pc_pid, t.id, t.date, t.apptdate, t.appttime, t.eid, t.pid, t.original_user, t.encounter, t.lastseq, t.random_drug_test, t.drug_screen_completed, " .
    "q.pt_tracker_id, q.start_datetime, q.room, q.status, q.seq, q.user, " .
    "s.toggle_setting_1, s.toggle_setting_2, s.option_id, ";
    $tracker_joins = "LEFT OUTER JOIN patient_tracker AS t ON t.pid = e.pc_pid AND t.apptdate = e.pc_eventDate AND t.appttime = e.pc_starttime AND t.eid = e.pc_eid " .
    "LEFT OUTER JOIN patient_tracker_element AS q ON q.pt_tracker_id = t.id AND q.seq = t.lastseq " .
    "LEFT OUTER JOIN list_options AS s ON s.list_id = 'apptstat' AND s.option_id = q.status AND s.activity = 1 " ;
    }

    $query = "SELECT " .
    "e.pc_eventDate, e.pc_endDate, e.pc_startTime, e.pc_endTime, e.pc_duration, e.pc_recurrtype, e.pc_recurrspec, e.pc_recurrfreq, e.pc_catid, e.pc_eid, " .
    "e.pc_title, e.pc_hometext, e.pc_apptstatus, " .
    "p.fname, p.mname, p.lname, p.pid, p.pubpid, p.phone_home, p.phone_cell, " .
    "u.fname AS ufname, u.mname AS umname, u.lname AS ulname, u.id AS uprovider_id, " .
    "f.name, " .
    "$tracker_fields" .
    "c.pc_catname, c.pc_catid " .
    "FROM openemr_postcalendar_events AS e " .
    "$tracker_joins" .
    "LEFT OUTER JOIN facility AS f ON e.pc_facility = f.id " .
    "LEFT OUTER JOIN patient_data AS p ON p.pid = e.pc_pid " .
    "LEFT OUTER JOIN users AS u ON u.id = e.pc_aid " .
    "LEFT OUTER JOIN openemr_postcalendar_categories AS c ON c.pc_catid = e.pc_catid " .
    "WHERE $where " .
    "ORDER BY $order_by";

    if($bind_param) $sqlBindArray = array_merge($sqlBindArray, $bind_param);

  }


  ///////////////////////////////////////////////////////////////////////
  // The following code is from the calculateEvents function in the    //
  // PostCalendar Module modified and inserted here by epsdky          //
  ///////////////////////////////////////////////////////////////////////

  $events2 = array();

  $res = sqlStatement($query, $sqlBindArray);

  ////////
  if($nextX) {
  global $resNotNull;
  $resNotNull = (isset($res) && $res != null);
  }

  while ($event = sqlFetchArray($res)) {
    ///////
    if($nextX) $stopDate = $event['pc_endDate'];
    else $stopDate = ($event['pc_endDate'] <= $to_date) ? $event['pc_endDate'] : $to_date;
    ///////
    $incX = 0;
    switch($event['pc_recurrtype']) {

      case '0' :

        $events2[] = $event;

        break;
//////
      case '1' :
      case '3' :
        $event_recurrspec = @unserialize($event['pc_recurrspec']);

        $rfreq = $event_recurrspec['event_repeat_freq'];
        $rtype = $event_recurrspec['event_repeat_freq_type'];
        $exdate = $event_recurrspec['exdate'];

        list($ny,$nm,$nd) = explode('-',$event['pc_eventDate']);
//        $occurance = Date_Calc::dateFormat($nd,$nm,$ny,'%Y-%m-%d');
        $occurance = $event['pc_eventDate'];

        while($occurance < $from_date) {
          $occurance =& __increment($nd,$nm,$ny,$rfreq,$rtype);
          list($ny,$nm,$nd) = explode('-',$occurance);
        }

        while($occurance <= $stopDate) {

            $excluded = false;
            if (isset($exdate)) {
                foreach (explode(",", $exdate) as $exception) {
                    // occurrance format == yyyy-mm-dd
                    // exception format == yyyymmdd
                    if (preg_replace("/-/", "", $occurance) == $exception) {
                        $excluded = true;
                    }
                }
            }

            if ($excluded == false) {
              $event['pc_eventDate'] = $occurance;
              $event['pc_endDate'] = '0000-00-00';
              $events2[] = $event;
              //////
              if ($nextX) {
              	++$incX;
              	if($incX == $nextX) break;
              }
              //////
            }
            
            $occurance =& __increment($nd,$nm,$ny,$rfreq,$rtype);
            list($ny,$nm,$nd) = explode('-',$occurance);

        }

        break;

//////
      case '2' :

        $event_recurrspec = @unserialize($event['pc_recurrspec']);

        $rfreq = $event_recurrspec['event_repeat_on_freq'];
        $rnum  = $event_recurrspec['event_repeat_on_num'];
        $rday  = $event_recurrspec['event_repeat_on_day'];
        $exdate = $event_recurrspec['exdate'];

        list($ny,$nm,$nd) = explode('-',$event['pc_eventDate']);

        if(isset($event_recurrspec['rt2_pf_flag']) && $event_recurrspec['rt2_pf_flag']) $nd = 1;

        $occuranceYm = "$ny-$nm"; // YYYY-mm
        $from_dateYm = substr($from_date,0,7); // YYYY-mm
        $stopDateYm = substr($stopDate,0,7); // YYYY-mm

        // $nd will sometimes be 29, 30 or 31 and if used in the mktime functions below
        // a problem with overflow will occur so it is set to 1 to avoid this (for rt2
        // appointments set prior to fix $nd remains unchanged). This can be done since
        // $nd has no influence past the mktime functions.
        while($occuranceYm < $from_dateYm) {
          $occuranceYmX = date('Y-m-d',mktime(0,0,0,$nm+$rfreq,$nd,$ny));
          list($ny,$nm,$nd) = explode('-',$occuranceYmX);
          $occuranceYm = "$ny-$nm";
        }

        while($occuranceYm <= $stopDateYm) {
 
          // (YYYY-mm)-dd
          $dnum = $rnum;
          do {
            $occurance = Date_Calc::NWeekdayOfMonth($dnum--,$rday,$nm,$ny,$format="%Y-%m-%d");
          } while($occurance === -1);

          if($occurance >= $from_date && $occurance <= $stopDate) {
        
            $excluded = false;
            if (isset($exdate)) {
                foreach (explode(",", $exdate) as $exception) {
                    // occurrance format == yyyy-mm-dd
                    // exception format == yyyymmdd
                    if (preg_replace("/-/", "", $occurance) == $exception) {
                        $excluded = true;
                    }
                }
            }

            if ($excluded == false) {

              $event['pc_eventDate'] = $occurance;
              $event['pc_endDate'] = '0000-00-00';
              $events2[] = $event;
              //////
              if($nextX) {
              	++$incX;
              	if($incX == $nextX) break;
              }
              //////

            }

          }

          $occuranceYmX = date('Y-m-d',mktime(0,0,0,$nm+$rfreq,$nd,$ny));
          list($ny,$nm,$nd) = explode('-',$occuranceYmX);
          $occuranceYm = "$ny-$nm";

        }

        break;


	    
    }

  }
  return $events2;
////////////////////// End of code inserted by epsdky
}

function fetchAllEvents( $from_date, $to_date, $provider_id = null, $facility_id = null )
{
	$sqlBindArray = array();

	$where = "";

	if ( $provider_id ) {
		$where .= " AND e.pc_aid = ?";
		array_push($sqlBindArray, $provider_id);
	}

	if ( $facility_id ) {
		$where .= " AND e.pc_facility = ? AND u.facility_id = ?";
		array_push($sqlBindArray, $facility_id, $facility_id);
	}

	$appointments = fetchEvents( $from_date, $to_date, $where, null, false, 0, $sqlBindArray );
	return $appointments;
}

function fetchAppointments( $from_date, $to_date, $patient_id = null, $provider_id = null, $facility_id = null, $pc_appstatus = null, $with_out_provider = null, $with_out_facility = null, $pc_catid = null, $tracker_board = false, $nextX = 0 )
{
	$sqlBindArray = array();

	$where = "";

	if ( $provider_id ) {
		$where .= " AND e.pc_aid = ?";
		array_push($sqlBindArray, $provider_id);
	}

	if ( $patient_id ) {
		$where .= " AND e.pc_pid = ?";
		array_push($sqlBindArray, $patient_id);
	} else {
		$where .= " AND e.pc_pid != ''";
	}

	if ( $facility_id ) {
		$where .= " AND e.pc_facility = ?";
		array_push($sqlBindArray, $facility_id);
	}

	//Appointment Status Checking
	if($pc_appstatus != ''){
		$where .= " AND e.pc_apptstatus = ?";
		array_push($sqlBindArray, $pc_appstatus);
	}

	if($pc_catid !=null) {
		$where .= " AND e.pc_catid = ?";
		array_push($sqlBindArray, $pc_catid);
	}

	//Without Provider checking
	if($with_out_provider != ''){
		$where .= " AND e.pc_aid = ''";
	}

	//Without Facility checking
	if($with_out_facility != ''){
		$where .= " AND e.pc_facility = 0";
	}

	$appointments = fetchEvents( $from_date, $to_date, $where, '', $tracker_board, $nextX, $sqlBindArray );
	return $appointments;
}

function fetchNextXAppts($from_date, $patient_id, $nextX = 1) {

  $appts = array();
  $nextXAppts = array();
  $appts = fetchAppointments( $from_date, null, $patient_id, null, null, null, null, null, null, false, $nextX );
  if($appts) {
    $appts = sortAppointments($appts);
    $nextXAppts = array_slice($appts, 0, $nextX);
  }
  return $nextXAppts;

}

// get the event slot size in seconds
function getSlotSize()
{
	if ( isset( $GLOBALS['calendar_interval'] ) ) {
		return $GLOBALS['calendar_interval'] * 60;
	}
	return 15 * 60;
}

function getAvailableSlots( $from_date, $to_date, $provider_id = null, $facility_id = null )
{
	$appointments = fetchAllEvents( $from_date, $to_date, $provider_id, $facility_id );
	$appointments = sortAppointments( $appointments, "date" );
	$from_datetime = strtotime( $from_date." 00:00:00" );
	$to_datetime = strtotime( $to_date." 23:59:59" );
	$availableSlots = array();
	$start_time = 0;
	$date = 0;
	for ( $i = 0; $i < count( $appointments ); ++$i )
	{
		if ( $appointments[$i]['pc_catid'] == 2 ) { // 2 == In Office
			$start_time = $appointments[$i]['pc_startTime'];
			$date = $appointments[$i]['pc_eventDate'];
			$provider_id = $appointments[$i]['uprovider_id'];
		} else if ( $appointments[$i]['pc_catid'] == 3 ) { // 3 == Out Of Office
			continue;
		} else {
			$start_time = $appointments[$i]['pc_endTime'];
			$date = $appointments[$i]['pc_eventDate'];
			$provider_id = $appointments[$i]['uprovider_id'];
		}

		// find next appointment with the same provider
		$next_appointment_date = 0;
		$next_appointment_time = 0;
		for ( $j = $i+1; $j < count( $appointments ); ++$j ) {
			if ( $appointments[$j]['uprovider_id'] == $provider_id ) {
				$next_appointment_date = $appointments[$j]['pc_eventDate'];
				$next_appointment_time = $appointments[$j]['pc_startTime'];
				break;
			}
		}

		$same_day = ( strtotime( $next_appointment_date ) == strtotime( $date ) ) ? true : false;

		if ( $next_appointment_time && $same_day ) {
			// check the start time of the next appointment

			$start_datetime = strtotime( $date." ".$start_time );
			$next_appointment_datetime = strtotime( $next_appointment_date." ".$next_appointment_time );
			$curr_time = $start_datetime;
			while ( $curr_time < $next_appointment_datetime - (getSlotSize() / 2) ) {
				//create a new appointment ever 15 minutes
				$time = date( "H:i:s", $curr_time );
				$available_slot = createAvailableSlot(
					$appointments[$i]['pc_eventDate'],
					$time,
					$appointments[$i]['ufname'],
					$appointments[$i]['ulname'],
					$appointments[$i]['umname'] );
				$availableSlots []= $available_slot;
				$curr_time += getSlotSize(); // add a 15-minute slot
			}
		}
	}

	return $availableSlots;
}

function createAvailableSlot( $event_date, $start_time, $provider_fname, $provider_lname, $provider_mname = "", $cat_name = "Available" )
{
	$newSlot = array();
	$newSlot['ulname'] = $provider_lname;
	$newSlot['ufname'] = $provider_fname;
	$newSlot['umname'] = $provider_mname;
	$newSlot['pc_eventDate'] = $event_date;
	$newSlot['pc_startTime'] = $start_time;
	$newSlot['pc_endTime'] = $start_time;
	$newSlot['pc_catname'] = $cat_name;
	return $newSlot;
}

function getCompareFunction( $code ) {
	global $COMPARE_FUNCTION_HASH;
	return $COMPARE_FUNCTION_HASH[$code];
}

function getComparisonOrder( $code ) {
	global $ORDERHASH;
	return $ORDERHASH[$code];
}


function sortAppointments( array $appointments, $orderBy = 'date' )
{
	global $appointment_sort_order;
	$appointment_sort_order = $orderBy;
	usort( $appointments, "compareAppointments" );
	return $appointments;
}

// cmp_function for usort
// The comparison function must return an integer less than, equal to,
// or greater than zero if the first argument is considered to be respectively less than, equal to, or greater than the second.
function compareAppointments( $appointment1, $appointment2 )
{
	global $appointment_sort_order;
	$comparisonOrder = getComparisonOrder( $appointment_sort_order );
	foreach ( $comparisonOrder as $comparison )
	{
		$cmp_function = getCompareFunction( $comparison );
		$result = $cmp_function( $appointment1, $appointment2 );
		if ( 0 != $result ) {
			return $result;
		}
	}

	return 0;
}

function compareBasic( $e1, $e2 )
{
	if ( $e1 < $e2 ) {
		return -1;
	} else if ( $e1 > $e2 ) {
		return 1;
	}
	
	return 0;
}

function compareAppointmentsByDate( $appointment1, $appointment2 )
{
	$date1 = strtotime( $appointment1['pc_eventDate'] );
	$date2 = strtotime( $appointment2['pc_eventDate'] );

	return compareBasic( $date1, $date2 );
}

function compareAppointmentsByTime( $appointment1, $appointment2 )
{
	$time1 = strtotime( $appointment1['pc_startTime'] );
	$time2 = strtotime( $appointment2['pc_startTime'] );

	return compareBasic( $time1, $time2 );
}

function compareAppointmentsByDoctorName( $appointment1, $appointment2 )
{
	$name1 = $appointment1['ulname'];
	$name2 = $appointment2['ulname'];
	$cmp = compareBasic( $name1, $name2 );
	if ( $cmp == 0 ) {
		$name1 = $appointment1['ufname'];
		$name2 = $appointment2['ufname'];
		return compareBasic( $name1, $name2 );
	}

	return $cmp;
}

function compareAppointmentsByPatientName( $appointment1, $appointment2 )
{
	$name1 = $appointment1['lname'];
	$name2 = $appointment2['lname'];
	$cmp = compareBasic( $name1, $name2 );
	if ( $cmp == 0 ) {
		$name1 = $appointment1['fname'];
		$name2 = $appointment2['fname'];
		return compareBasic( $name1, $name2 );
	}

	return $cmp;
}

function compareAppointmentsByType( $appointment1, $appointment2 )
{
	$type1 = $appointment1['pc_catid'];
	$type2 = $appointment2['pc_catid'];
	return compareBasic( $type1, $type2 );
}

function compareAppointmentsByPatientId( $appointment1, $appointment2 )
{
	$id1 = $appointment1['pubpid'];
	$id2 = $appointment2['pubpid'];
	return compareBasic( $id1, $id2 );
}

function compareAppointmentsByComment( $appointment1, $appointment2 )
{
	$comment1 = $appointment1['pc_hometext'];
	$comment2 = $appointment2['pc_hometext'];
	return compareBasic( $comment1, $comment2 );
}

function compareAppointmentsByStatus( $appointment1, $appointment2 )
{
	$status1 = $appointment1['pc_apptstatus'];
	$status2 = $appointment2['pc_apptstatus'];
	return compareBasic( $status1, $status2 );
}

function compareAppointmentsByTrackerStatus( $appointment1, $appointment2 )
{
	$trackerstatus1 = $appointment1['status'];
	$trackerstatus2 = $appointment2['status'];
	return compareBasic( $trackerstatus1, $trackerstatus2 );
}

function compareAppointmentsByCompletedDrugScreen( $appointment1, $appointment2 )
{
	$completed1 = $appointment1['drug_screen_completed'];
	$completed2 = $appointment2['drug_screen_completed'];
	return compareBasic( $completed1, $completed2 );
}

function fetchAppointmentCategories()
{
     $catSQL= " SELECT pc_catid as id, pc_catname as category "
            . " FROM openemr_postcalendar_categories WHERE pc_recurrtype=0 and pc_cattype=0 ORDER BY category";
     return sqlStatement($catSQL);
}

function interpretRecurrence($recurr_freq, $recurr_type){
	global $REPEAT_FREQ, $REPEAT_FREQ_TYPE, $REPEAT_ON_NUM, $REPEAT_ON_DAY;
	$interpreted = "";
	$recurr_freq = unserialize($recurr_freq);
	if($recurr_type == 1){
		$interpreted = $REPEAT_FREQ[$recurr_freq['event_repeat_freq']];
		$interpreted .= " " . $REPEAT_FREQ_TYPE[$recurr_freq['event_repeat_freq_type']];
	}
	elseif($recurr_type == 2){
		$interpreted = $REPEAT_FREQ[$recurr_freq['event_repeat_on_freq']];
		$interpreted .= " " . $REPEAT_ON_NUM[$recurr_freq['event_repeat_on_num']];
		$interpreted .= " " . $REPEAT_ON_DAY[$recurr_freq['event_repeat_on_day']];
	}
	elseif ($recurr_type == 3){
		$interpreted = $REPEAT_FREQ[1];
		$comma = "";
		$day_arr = explode(",", $recurr_freq['event_repeat_freq']);
		foreach ($day_arr as $day){
			$interpreted .= $comma . " " . $REPEAT_ON_DAY[$day - 1];
			$comma = ",";
		}

	}

	return $interpreted;
}

function fetchRecurrences($pid){
    $query = "SELECT pe.pc_title, pe.pc_endDate, pe.pc_recurrtype, pe.pc_recurrspec, pc.pc_catname FROM openemr_postcalendar_events AS pe "
					. "JOIN openemr_postcalendar_categories AS pc ON pe.pc_catid=pc.pc_catid "
					. "WHERE pe.pc_pid = ?  AND pe.pc_recurrtype > 0;";
	$sqlBindArray = array();
	array_push($sqlBindArray, $pid);
	$res = sqlStatement($query, $sqlBindArray);
	$row = 0;
	while($res_arr[$row] = sqlFetchArray($res)) {
		$res_arr[$row]['pc_recurrspec'] = interpretRecurrence($res_arr[$row]['pc_recurrspec'], $res_arr[$row]['pc_recurrtype']);
		$row++;
	}
	return $res_arr;
}

function ends_in_a_week($end_date){
	$timestamp_in_a_week = strtotime('+7 day');
	$timestamp_end_date = strtotime($end_date);
	if($timestamp_in_a_week > $timestamp_end_date){
		return true; //ends in a week
	}
	return false; // ends in more than a week
}

//Checks if recurrence is current (didn't end yet).
function recurrence_is_current($end_date){
	$end_date_timestamp = strtotime($end_date);
	$current_timestamp = time();
	if($current_timestamp <= $end_date_timestamp){
		return true; //recurrence is current
	}
	return false;
}

?>
