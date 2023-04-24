<?php
/** 
* library/daysheet.inc.php Functions used in the end of day report. 
* 
* Functions for Generating an End of Day report
* 
* 
* Copyright (C) 2014-2015 Terry Hill <terry@lillysystems.com> 
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
* along with this program. If not, see <http://opensource.org/licenses/gpl-license.php>;. 
* 
* @package OpenEMR 
* @author Terry Hill <terry@lillysystems.com>
* @link http://www.open-emr.org 
*/

/**
* @return Returns the array sorted as required
* @param $aryData Array containing data to sort
* @param $strIndex Name of column to use as an index
* @param $strSortBy Column to sort the array by
* @param $strSortType String containing either asc or desc [default to asc]
* @desc Naturally sorts an array using by the column $strSortBy
*/
function array_natsort($aryData, $strIndex, $strSortBy, $strSortType=false) {
    //    if the parameters are invalid
    if (!is_array($aryData) || !$strIndex || !$strSortBy)
        //    return the array
        return $aryData;
    //    create our temporary arrays
    $arySort = $aryResult = array();
    //    loop through the array
    foreach ($aryData as $aryRow)
        //    set up the value in the array
        $arySort[$aryRow[$strIndex]] = $aryRow[$strSortBy];
    //    apply the natural sort
    natsort($arySort);
    //    if the sort type is descending
    if ($strSortType=="desc")
        //    reverse the array
        arsort($arySort);
    //    loop through the sorted and original data
    foreach ($arySort as $arySortKey => $arySorted)
        foreach ($aryData as $aryOriginal)
            //    if the key matches
            if ($aryOriginal[$strIndex]==$arySortKey)
                //    add it to the output array
                array_push($aryResult, $aryOriginal);
    //    return the return
    return $aryResult;
}

    function GenerateTheQueryPart()
     {
        global $query_part,$query_part2,$query_part_day,$query_part_day1,$billstring,$auth;
        //Search Criteria section.
        $billstring='';
        $auth='';
        $query_part='';
		$query_part_day='';
		$query_part_day1='';
        $query_part2='';

        if(isset($_REQUEST['final_this_page_criteria']))
         {
            foreach($_REQUEST['final_this_page_criteria'] as $criteria_key => $criteria_value)
             {

              $criteria_value=PrepareSearchItem($criteria_value); // this escapes for sql
              $SplitArray=array();
              //---------------------------------------------------------
              if(strpos($criteria_value,"billing.billed = '1'")!== false)
               {
                $billstring .= ' AND '.$criteria_value;
               }
              elseif(strpos($criteria_value,"billing.billed = '0'")!== false)
               {
                //3 is an error condition
                $billstring .= ' AND '."(billing.billed is null or billing.billed = '0' or (billing.billed = '1' and billing.bill_process = '3'))";
               }
              elseif(strpos($criteria_value,"billing.billed = '7'")!== false)
               {
                $billstring .= ' AND '."billing.bill_process = '7'";
               }
              //---------------------------------------------------------
              elseif(strpos($criteria_value,"billing.id = 'null'")!== false)
               {
                $billstring .= ' AND '."billing.id is null";
               }
              //---------------------------------------------------------
              elseif(strpos($criteria_value,"billing.id = 'not null'")!== false)
               {
                $billstring .= ' AND '."billing.id is not null";
               }
              //---------------------------------------------------------
              elseif(strpos($criteria_value,"patient_data.fname")!== false)
               {
                $SplitArray=explode(' like ',$criteria_value);
                $query_part .= " AND ($criteria_value or patient_data.lname like ".$SplitArray[1].")";
               }
              //---------------------------------------------------------
              elseif(strpos($criteria_value,"billing.authorized")!== false)
               {
                $auth = ' AND '.$criteria_value;
               }
              //---------------------------------------------------------
              elseif(strpos($criteria_value,"form_encounter.pid")!== false)
               {//comes like '781,780'
                $SplitArray=explode(" = '",$criteria_value);//comes like 781,780'
                $SplitArray[1]=substr($SplitArray[1], 0, -1);//comes like 781,780
                $query_part .= ' AND form_encounter.pid in ('.$SplitArray[1].')';
                $query_part2 .= ' AND pid in ('.$SplitArray[1].')';
               }
              //---------------------------------------------------------
              elseif(strpos($criteria_value,"form_encounter.encounter")!== false)
               {//comes like '781,780'
                $SplitArray=explode(" = '",$criteria_value);//comes like 781,780'
                $SplitArray[1]=substr($SplitArray[1], 0, -1);//comes like 781,780
                $query_part .= ' AND form_encounter.encounter in ('.$SplitArray[1].')';
               }
              //---------------------------------------------------------
              elseif(strpos($criteria_value,"insurance_data.provider = '1'")!== false)
               {
                $query_part .= ' AND '."insurance_data.provider > '0' and insurance_data.date <= form_encounter.date";
               }
              elseif(strpos($criteria_value,"insurance_data.provider = '0'")!== false)
               {
                $query_part .= ' AND '."(insurance_data.provider = '0' or insurance_data.date > form_encounter.date)";
               }
              //---------------------------------------------------------
              else
               {
                $query_part .= ' AND '.$criteria_value;

			    if (substr($criteria_value,1,8) === 'form_enc') {
				  $query_part_day .=  ' AND ' . '(ar_activity.post_time'. substr($criteria_value,20) ;
				}
				if (substr($criteria_value,1,12) === 'billing.date') {
				  $query_part_day .=  ' AND ' . '(ar_activity.post_time'. substr($criteria_value,13) ;
				}
				if (substr($criteria_value,1,14) === 'claims.process') {
				  $query_part_day .=  ' AND ' . '(ar_activity.post_time'. substr($criteria_value,20) ;
				}
				if (substr($criteria_value,0,12) === 'billing.user') {
				  $query_part_day .=  ' AND ' . 'ar_activity.post_user'. substr($criteria_value,12) ;
				}
				if (substr($criteria_value,1,8) === 'form_enc') {
				  $query_part_day1 .=  ' AND ' . '(dtime'. substr($query_part,25,58) ;
				}
				if (substr($criteria_value,1,12) === 'billing.date') {
				  $query_part_day1 .=  ' AND ' . '(dtime'. substr($query_part,18,58) ;
				}
				if (substr($criteria_value,1,14) === 'claims.process') {
				  $query_part_day1 .=  ' AND ' . '(dtime'. substr($query_part,25,58) ;
				}
				
               }
              }
         }
     }
    //date must be in nice format (e.g. 2002-07-11)

    function getBillsBetweendayReport( $code_type,
        $cols = "id,date,pid,code_type,code,user,authorized,x12_partner_id")
    {
        GenerateTheQueryPart();
        global $query_part,$query_part2,$query_part_day,$query_part_day1,$billstring,$auth;

		$sql = "SELECT distinct form_encounter.pid AS enc_pid, form_encounter.date AS enc_date, concat(lname, ' ', fname) as 'fulname', lname as 'last', fname as 'first', " .
			"form_encounter.encounter AS enc_encounter, form_encounter.provider_id AS enc_provider_id, billing.* , date(billing.date) as date  " .
            "FROM form_encounter " .
            "LEFT OUTER JOIN billing ON " .
            "billing.encounter = form_encounter.encounter AND " .
            "billing.pid = form_encounter.pid AND " .
            "billing.code_type LIKE ? AND " .
            "billing.activity = 1 " .
            "LEFT OUTER JOIN patient_data ON patient_data.pid = form_encounter.pid " .
            "LEFT OUTER JOIN claims on claims.patient_id = form_encounter.pid and claims.encounter_id = form_encounter.encounter " .
            "LEFT OUTER JOIN insurance_data on insurance_data.pid = form_encounter.pid and insurance_data.type = 'primary' ".
			"WHERE 1=1 $query_part AND billing.fee!=0 " . " $auth " ." $billstring " .
            "ORDER BY  fulname ASC, date ASC, pid ";

        $res = sqlStatement($sql,array($code_type));
        $all = False;
        for($iter=0; $row=sqlFetchArray($res); $iter++)
        {
            $all[$iter] = $row;
        }

		$query = sqlStatement("SELECT ar_activity.pid as pid, 'Patient Payment' AS code_type, ar_activity.pay_amount AS pat_code, date(ar_activity.post_time) AS date,".
            "ar_activity.encounter AS encounter_ar, concat(lname, ' ', fname) as 'fulname', lname as 'last', fname as 'first', ar_activity.payer_type AS payer,".
            "ar_activity.session_id AS sesid, ar_activity.account_code AS paytype, ar_activity.post_user AS user, ar_activity.memo AS reason,".
            "ar_activity.adj_amount AS pat_adjust_dollar, providerid as 'provider_id' ".
            "FROM ar_activity LEFT OUTER JOIN patient_data ON patient_data.pid = ar_activity.pid " .
			"where 1=1 $query_part_day AND payer_type=0 ORDER BY fulname ASC, date ASC, pid");
           
			for($iter; $row=sqlFetchArray($query); $iter++)
        {
            $all[$iter] = $row;
        }

		$query = sqlStatement("SELECT ar_activity.pid as pid, 'Insurance Payment' AS code_type, ar_activity.pay_amount AS ins_code, date(ar_activity.post_time) AS date,".
            "ar_activity.encounter AS encounter_ar, concat(lname, ' ', fname) as 'fulname', lname as 'last', fname as 'first', ar_activity.payer_type AS payer,".
            "ar_activity.session_id AS sesid, ar_activity.account_code AS paytype, ar_activity.post_user AS user, ar_activity.memo AS reason,".
            "ar_activity.adj_amount AS ins_adjust_dollar, providerid as 'provider_id' ".
            "FROM ar_activity LEFT OUTER JOIN patient_data ON patient_data.pid = ar_activity.pid " .
			"where 1=1 $query_part_day AND payer_type!=0 ORDER BY  fulname ASC, date ASC, pid");

        for($iter; $row=sqlFetchArray($query); $iter++)
        {
            $all[$iter] = $row;
        }
		
        return $all;
    }


