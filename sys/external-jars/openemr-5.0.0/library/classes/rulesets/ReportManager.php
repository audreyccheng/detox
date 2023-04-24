<?php
// Copyright (C) 2011 Ken Chapple <ken@mi-squared.com>
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
require_once( "ReportTypes.php" );

class ReportManager
{
    public function __construct()
    {
        foreach ( glob( dirname(__FILE__)."/library/*.php" ) as $filename ) {
            require_once( $filename );
        }
        
        foreach ( glob( dirname(__FILE__)."/Cqm/*.php" ) as $filename ) {
            require_once( $filename );
        }
        
        foreach ( glob( dirname(__FILE__)."/Amc/*.php" ) as $filename ) {
            require_once( $filename );
        }
    }

    public function runReport( $rowRule, $patients, $dateTarget, $options=array() )
    {
        $ruleId = $rowRule['id'];
        $patientData = array();
        foreach( $patients as $patient ) {
            $patientData []= $patient['pid'];
        }
        
        $reportFactory = null;
        if ( ReportTypes::getType( $ruleId ) == ReportTypes::CQM ) {
            $reportFactory = new CqmReportFactory();
        } else if ( ReportTypes::getType( $ruleId ) == ReportTypes::AMC ) {
            $reportFactory = new AmcReportFactory();
        } else {
            throw new Exception( "Unknown rule: ".$ruleId );
        }
        
        $report = null;
        if ( $reportFactory instanceof  RsReportFactoryAbstract ) {
            $report = $reportFactory->createReport( ReportTypes::getClassName( $ruleId ), $rowRule, $patientData, $dateTarget, $options );
        }
        
        $results = array();
        if ( $report instanceof RsReportIF &&
            !$report instanceof RsUnimplementedIF ) {
            $report->execute();
            $results = $report->getResults();
        }
        
        return RsHelper::formatClinicalRules( $results );
    }
}
