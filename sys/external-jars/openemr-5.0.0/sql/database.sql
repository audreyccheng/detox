--
-- Database: `openemr`
--

-- --------------------------------------------------------

--
-- Table structure for table `addresses`
--

DROP TABLE IF EXISTS `addresses`;
CREATE TABLE `addresses` (
  `id` int(11) NOT NULL default '0',
  `line1` varchar(255) default NULL,
  `line2` varchar(255) default NULL,
  `city` varchar(255) default NULL,
  `state` varchar(35) default NULL,
  `zip` varchar(10) default NULL,
  `plus_four` varchar(4) default NULL,
  `country` varchar(255) default NULL,
  `foreign_id` int(11) default NULL,
  PRIMARY KEY  (`id`),
  KEY `foreign_id` (`foreign_id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `amc_misc_data`
--

DROP TABLE IF EXISTS `amc_misc_data`;
CREATE TABLE `amc_misc_data` (
  `amc_id` varchar(31) NOT NULL DEFAULT '' COMMENT 'Unique and maps to list_options list clinical_rules',
  `pid` bigint(20) default NULL,
  `map_category` varchar(255) NOT NULL default '' COMMENT 'Maps to an object category (such as prescriptions etc.)',
  `map_id` bigint(20) NOT NULL default '0' COMMENT 'Maps to an object id (such as prescription id etc.)',
  `date_created` datetime default NULL,
  `date_completed` datetime default NULL,
  `soc_provided` datetime default NULL,
  KEY  (`amc_id`,`pid`,`map_id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `amendments`
--

DROP TABLE IF EXISTS `amendments`;
CREATE TABLE IF NOT EXISTS `amendments` (
  `amendment_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Amendment ID',
  `amendment_date` date	NOT NULL COMMENT 'Amendement request date',
  `amendment_by` varchar(50) NOT NULL COMMENT 'Amendment requested from',
  `amendment_status` varchar(50) NULL COMMENT 'Amendment status accepted/rejected/null',
  `pid` int(11) NOT NULL COMMENT 'Patient ID from patient_data',
  `amendment_desc` text COMMENT 'Amendment Details',
  `created_by` int(11) NOT NULL COMMENT 'references users.id for session owner',
  `modified_by`	int(11) NULL COMMENT 'references users.id for session owner',
  `created_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT 'created time',
  `modified_time` timestamp NULL COMMENT 'modified time',
  PRIMARY KEY amendments_id(`amendment_id`),
  KEY amendment_pid(`pid`)
) ENGINE = InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `amendments_history`
--

DROP TABLE IF EXISTS `amendments_history`;
CREATE TABLE IF NOT EXISTS `amendments_history` (
  `amendment_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Amendment ID',
  `amendment_note` text COMMENT 'Amendment requested from',
  `amendment_status` VARCHAR(50) NULL COMMENT 'Amendment Request Status',
  `created_by` int(11) NOT NULL COMMENT 'references users.id for session owner',
  `created_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT 'created time',
KEY amendment_history_id(`amendment_id`)
) ENGINE = InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `array`
--

DROP TABLE IF EXISTS `array`;
CREATE TABLE `array` (
  `array_key` varchar(255) default NULL,
  `array_value` longtext
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `audit_master`
--

DROP TABLE IF EXISTS `audit_master`;
CREATE TABLE `audit_master` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `pid` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL COMMENT 'The Id of the user who approves or denies',
  `approval_status` tinyint(4) NOT NULL COMMENT '1-Pending,2-Approved,3-Denied,4-Appointment directly updated to calendar table,5-Cancelled appointment',
  `comments` text,
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modified_time` datetime NOT NULL,
  `ip_address` varchar(100) NOT NULL,
  `type` tinyint(4) NOT NULL COMMENT '1-new patient,2-existing patient,3-change is only in the document,4-Patient upload,5-random key,10-Appointment',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

--
-- Table structure for table `audit_details`
--

DROP TABLE IF EXISTS `audit_details`;
CREATE TABLE `audit_details` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `table_name` VARCHAR(100) NOT NULL COMMENT 'openemr table name',
  `field_name` VARCHAR(100) NOT NULL COMMENT 'openemr table''s field name',
  `field_value` TEXT COMMENT 'openemr table''s field value',
  `audit_master_id` BIGINT(20) NOT NULL COMMENT 'Id of the audit_master table',
  `entry_identification` VARCHAR(255) NOT NULL DEFAULT '1' COMMENT 'Used when multiple entry occurs from the same table.1 means no multiple entry',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

--
-- Table structure for table `background_services`
--

DROP TABLE IF EXISTS `background_services`;
CREATE TABLE `background_services` (
  `name` varchar(31) NOT NULL,
  `title` varchar(127) NOT NULL COMMENT 'name for reports',
  `active` tinyint(1) NOT NULL default '0',
  `running` tinyint(1) NOT NULL default '-1',
  `next_run` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `execute_interval` int(11) NOT NULL default '0' COMMENT 'minimum number of minutes between function calls,0=manual mode',
  `function` varchar(127) NOT NULL COMMENT 'name of background service function',
  `require_once` varchar(255) default NULL COMMENT 'include file (if necessary)',
  `sort_order` int(11) NOT NULL default '100' COMMENT 'lower numbers will be run first',
  PRIMARY KEY  (`name`)
) ENGINE=InnoDB;

--
-- Dumping data for table `background_services`
--

INSERT INTO `background_services` (`name`, `title`, `execute_interval`, `function`, `require_once`, `sort_order`) VALUES
('phimail', 'phiMail Direct Messaging Service', 5, 'phimail_check', '/library/direct_message_check.inc', 100);

-- --------------------------------------------------------

--
-- Table structure for table `batchcom`
--

DROP TABLE IF EXISTS `batchcom`;
CREATE TABLE `batchcom` (
  `id` bigint(20) NOT NULL auto_increment,
  `patient_id` int(11) NOT NULL default '0',
  `sent_by` bigint(20) NOT NULL default '0',
  `msg_type` varchar(60) default NULL,
  `msg_subject` varchar(255) default NULL,
  `msg_text` mediumtext,
  `msg_date_sent` datetime NOT NULL default '0000-00-00 00:00:00',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `billing`
--

DROP TABLE IF EXISTS `billing`;
CREATE TABLE `billing` (
  `id` int(11) NOT NULL auto_increment,
  `date` datetime default NULL,
  `code_type` varchar(15) default NULL,
  `code` varchar(20) default NULL,
  `pid` int(11) default NULL,
  `provider_id` int(11) default NULL,
  `user` int(11) default NULL,
  `groupname` varchar(255) default NULL,
  `authorized` tinyint(1) default NULL,
  `encounter` int(11) default NULL,
  `code_text` longtext,
  `billed` tinyint(1) default NULL,
  `activity` tinyint(1) default NULL,
  `payer_id` int(11) default NULL,
  `bill_process` tinyint(2) NOT NULL default '0',
  `bill_date` datetime default NULL,
  `process_date` datetime default NULL,
  `process_file` varchar(255) default NULL,
  `modifier` varchar(12) default NULL,
  `units` int(11) default NULL,
  `fee` decimal(12,2) default NULL,
  `justify` varchar(255) default NULL,
  `target` varchar(30) default NULL,
  `x12_partner_id` int(11) default NULL,
  `ndc_info` varchar(255) default NULL,
  `notecodes` varchar(25) NOT NULL default '',
  `external_id` VARCHAR(20) DEFAULT NULL,
  `pricelevel` varchar(31) default '',
  PRIMARY KEY  (`id`),
  KEY `pid` (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
  `id` int(11) NOT NULL default '0',
  `name` varchar(255) default NULL,
  `value` varchar(255) default NULL,
  `parent` int(11) NOT NULL default '0',
  `lft` int(11) NOT NULL default '0',
  `rght` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `parent` (`parent`),
  KEY `lft` (`lft`,`rght`)
) ENGINE=InnoDB;

--
-- Dumping data for table `categories`
--
INSERT INTO `categories` VALUES (1, 'Categories', '', 0, 0, 51);
INSERT INTO `categories` VALUES (2, 'Lab Report', '', 1, 1, 2);
INSERT INTO `categories` VALUES (3, 'Medical Record', '', 1, 3, 4);
INSERT INTO `categories` VALUES (4, 'Patient Information', '', 1, 5, 10);
INSERT INTO `categories` VALUES (5, 'Patient ID card', '', 4, 6, 7);
INSERT INTO `categories` VALUES (6, 'Advance Directive', '', 1, 11, 18);
INSERT INTO `categories` VALUES (7, 'Do Not Resuscitate Order', '', 6, 12, 13);
INSERT INTO `categories` VALUES (8, 'Durable Power of Attorney', '', 6, 14, 15);
INSERT INTO `categories` VALUES (9, 'Living Will', '', 6, 16, 17);
INSERT INTO `categories` VALUES (10, 'Patient Photograph', '', 4, 8, 9);
INSERT INTO `categories` VALUES (11, 'CCR', '', 1, 19, 20);
INSERT INTO `categories` VALUES (12, 'CCD', '', 1, 21, 22);
INSERT INTO `categories` VALUES (13, 'CCDA', '', 1, 23, 24);
INSERT INTO `categories` VALUES (14, 'Eye Module', '', 1, 25, 50);
INSERT INTO `categories` VALUES (15, 'Communication - Eye', '', 14, 26, 27);
INSERT INTO `categories` VALUES (16, 'Encounters - Eye', '', 14, 28, 29);
INSERT INTO `categories` VALUES (17, 'Imaging - Eye', '', 14, 30, 49);
INSERT INTO `categories` VALUES (18, 'OCT - Eye', 'POSTSEG', 17, 31, 32);
INSERT INTO `categories` VALUES (19, 'FA/ICG - Eye', 'POSTSEG', 17, 33, 34);
INSERT INTO `categories` VALUES (20, 'External Photos - Eye', 'EXT', 17, 35, 36);
INSERT INTO `categories` VALUES (21, 'AntSeg Photos - Eye', 'ANTSEG', 17, 37, 38);
INSERT INTO `categories` VALUES (22, 'Optic Disc - Eye', 'POSTSEG', 17, 39, 40);
INSERT INTO `categories` VALUES (23, 'Fundus - Eye', 'POSTSEG', 17, 41, 42);
INSERT INTO `categories` VALUES (24, 'Radiology - Eye', 'NEURO', 17, 43, 44);
INSERT INTO `categories` VALUES (25, 'VF - Eye', 'NEURO', 17, 45, 46);
INSERT INTO `categories` VALUES (26, 'Drawings - Eye', '', 17, 47, 48);

-- --------------------------------------------------------

--
-- Table structure for table `categories_seq`
--

DROP TABLE IF EXISTS `categories_seq`;
CREATE TABLE `categories_seq` (
  `id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB;

--
-- Dumping data for table `categories_seq`
--

INSERT INTO `categories_seq` VALUES (26);

-- --------------------------------------------------------

--
-- Table structure for table `categories_to_documents`
--

DROP TABLE IF EXISTS `categories_to_documents`;
CREATE TABLE `categories_to_documents` (
  `category_id` int(11) NOT NULL default '0',
  `document_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`category_id`,`document_id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `claims`
--

DROP TABLE IF EXISTS `claims`;
CREATE TABLE `claims` (
  `patient_id` int(11) NOT NULL,
  `encounter_id` int(11) NOT NULL,
  `version` int(10) unsigned NOT NULL COMMENT 'Claim version, incremented in code',
  `payer_id` int(11) NOT NULL default '0',
  `status` tinyint(2) NOT NULL default '0',
  `payer_type` tinyint(4) NOT NULL default '0',
  `bill_process` tinyint(2) NOT NULL default '0',
  `bill_time` datetime default NULL,
  `process_time` datetime default NULL,
  `process_file` varchar(255) default NULL,
  `target` varchar(30) default NULL,
  `x12_partner_id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`patient_id`,`encounter_id`,`version`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `clinical_plans`
--

DROP TABLE IF EXISTS `clinical_plans`;
CREATE TABLE `clinical_plans` (
  `id` varchar(31) NOT NULL DEFAULT '' COMMENT 'Unique and maps to list_options list clinical_plans',
  `pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '0 is default for all patients, while > 0 is id from patient_data table',
  `normal_flag` tinyint(1) COMMENT 'Normal Activation Flag',
  `cqm_flag` tinyint(1) COMMENT 'Clinical Quality Measure flag (unable to customize per patient)',
  `cqm_2011_flag` tinyint(1) COMMENT '2011 Clinical Quality Measure flag (unable to customize per patient)',
  `cqm_2014_flag` tinyint(1) COMMENT '2014 Clinical Quality Measure flag (unable to customize per patient)',
  `cqm_measure_group` varchar(10) NOT NULL default '' COMMENT 'Clinical Quality Measure Group Identifier',
  PRIMARY KEY  (`id`,`pid`)
) ENGINE=InnoDB ;

--
-- Clinical Quality Measure (CMQ) plans
--
-- Measure Group A: Diabetes Mellitus
INSERT INTO `clinical_plans` ( `id`, `pid`, `normal_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_measure_group` ) VALUES ('dm_plan_cqm', 0, 0, 1, 1, 'A');
-- Measure Group C: Chronic Kidney Disease (CKD)
INSERT INTO `clinical_plans` ( `id`, `pid`, `normal_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_measure_group` ) VALUES ('ckd_plan_cqm', 0, 0, 1, 1, 'C');
-- Measure Group D: Preventative Care
INSERT INTO `clinical_plans` ( `id`, `pid`, `normal_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_measure_group` ) VALUES ('prevent_plan_cqm', 0, 0, 1, 1, 'D');
-- Measure Group E: Perioperative Care
INSERT INTO `clinical_plans` ( `id`, `pid`, `normal_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_measure_group` ) VALUES ('periop_plan_cqm', 0, 0, 1, 1, 'E');
-- Measure Group F: Rheumatoid Arthritis
INSERT INTO `clinical_plans` ( `id`, `pid`, `normal_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_measure_group` ) VALUES ('rheum_arth_plan_cqm', 0, 0, 1, 1, 'F');
-- Measure Group G: Back Pain
INSERT INTO `clinical_plans` ( `id`, `pid`, `normal_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_measure_group` ) VALUES ('back_pain_plan_cqm', 0, 0, 1, 1, 'G');
-- Measure Group H: Coronary Artery Bypass Graft (CABG)
INSERT INTO `clinical_plans` ( `id`, `pid`, `normal_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_measure_group` ) VALUES ('cabg_plan_cqm', 0, 0, 1, 1, 'H');
--
-- Standard clinical plans
--
-- Diabetes Mellitus
INSERT INTO `clinical_plans` ( `id`, `pid`, `normal_flag`, `cqm_flag`, `cqm_measure_group` ) VALUES ('dm_plan', 0, 1, 0, '');
INSERT INTO `clinical_plans` ( `id`, `pid`, `normal_flag`, `cqm_flag`, `cqm_measure_group` ) VALUES ('prevent_plan', 0, 1, 0, '');

-- --------------------------------------------------------

--
-- Table structure for table `clinical_plans_rules`
--

DROP TABLE IF EXISTS `clinical_plans_rules`;
CREATE TABLE `clinical_plans_rules` (
  `plan_id` varchar(31) NOT NULL DEFAULT '' COMMENT 'Unique and maps to list_options list clinical_plans',
  `rule_id` varchar(31) NOT NULL DEFAULT '' COMMENT 'Unique and maps to list_options list clinical_rules',
  PRIMARY KEY  (`plan_id`,`rule_id`)
) ENGINE=InnoDB ;

--
-- Clinical Quality Measure (CMQ) plans to rules mappings
--
-- Measure Group A: Diabetes Mellitus
--   NQF 0059 (PQRI 1)   Diabetes: HbA1c Poor Control
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('dm_plan_cqm', 'rule_dm_a1c_cqm');
--   NQF 0064 (PQRI 2)   Diabetes: LDL Management & Control
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('dm_plan_cqm', 'rule_dm_ldl_cqm');
--   NQF 0061 (PQRI 3)   Diabetes: Blood Pressure Management
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('dm_plan_cqm', 'rule_dm_bp_control_cqm');
--   NQF 0055 (PQRI 117) Diabetes: Eye Exam
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('dm_plan_cqm', 'rule_dm_eye_cqm');
--   NQF 0056 (PQRI 163) Diabetes: Foot Exam
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('dm_plan_cqm', 'rule_dm_foot_cqm');
-- Measure Group D: Preventative Care
--   NQF 0041 (PQRI 110) Influenza Immunization for Patients >= 50 Years Old
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan_cqm', 'rule_influenza_ge_50_cqm');
--   NQF 0043 (PQRI 111) Pneumonia Vaccination Status for Older Adults
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan_cqm', 'rule_pneumovacc_ge_65_cqm');
--   NQF 0421 (PQRI 128) Adult Weight Screening and Follow-Up
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan_cqm', 'rule_adult_wt_screen_fu_cqm');
--
-- Standard clinical plans to rules mappings
--
-- Diabetes Mellitus
--   Hemoglobin A1C
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('dm_plan', 'rule_dm_hemo_a1c');
--   Urine Microalbumin
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('dm_plan', 'rule_dm_urine_alb');
--   Eye Exam
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('dm_plan', 'rule_dm_eye');
--   Foot Exam
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('dm_plan', 'rule_dm_foot');
-- Preventative Care
--   Hypertension: Blood Pressure Measurement
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan', 'rule_htn_bp_measure');
--   Tobacco Use Assessment
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan', 'rule_tob_use_assess');
--   Tobacco Cessation Intervention
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan', 'rule_tob_cess_inter');
--   Adult Weight Screening and Follow-Up
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan', 'rule_adult_wt_screen_fu');
--   Weight Assessment and Counseling for Children and Adolescents
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan', 'rule_wt_assess_couns_child');
--   Influenza Immunization for Patients >= 50 Years Old
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan', 'rule_influenza_ge_50');
--   Pneumonia Vaccination Status for Older Adults
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan', 'rule_pneumovacc_ge_65');
--   Cancer Screening: Mammogram
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan', 'rule_cs_mammo');
--   Cancer Screening: Pap Smear
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan', 'rule_cs_pap');
--   Cancer Screening: Colon Cancer Screening
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan', 'rule_cs_colon');
--   Cancer Screening: Prostate Cancer Screening
INSERT INTO `clinical_plans_rules` ( `plan_id`, `rule_id` ) VALUES ('prevent_plan', 'rule_cs_prostate');

-- --------------------------------------------------------

--
-- Table structure for table `clinical_rules`
--

DROP TABLE IF EXISTS `clinical_rules`;
CREATE TABLE `clinical_rules` (
  `id` varchar(31) NOT NULL DEFAULT '' COMMENT 'Unique and maps to list_options list clinical_rules',
  `pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '0 is default for all patients, while > 0 is id from patient_data table',
  `active_alert_flag` tinyint(1) COMMENT 'Active Alert Widget Module flag - note not yet utilized',
  `passive_alert_flag` tinyint(1) COMMENT 'Passive Alert Widget Module flag',
  `cqm_flag` tinyint(1) COMMENT 'Clinical Quality Measure flag (unable to customize per patient)',
  `cqm_2011_flag` tinyint(1) COMMENT '2011 Clinical Quality Measure flag (unable to customize per patient)',
  `cqm_2014_flag` tinyint(1) COMMENT '2014 Clinical Quality Measure flag (unable to customize per patient)',
  `cqm_nqf_code` varchar(10) NOT NULL default '' COMMENT 'Clinical Quality Measure NQF identifier',
  `cqm_pqri_code` varchar(10) NOT NULL default '' COMMENT 'Clinical Quality Measure PQRI identifier',
  `amc_flag` tinyint(1) COMMENT 'Automated Measure Calculation flag (unable to customize per patient)',
  `amc_2011_flag` tinyint(1) COMMENT '2011 Automated Measure Calculation flag for (unable to customize per patient)',
  `amc_2014_flag` tinyint(1) COMMENT '2014 Automated Measure Calculation flag for (unable to customize per patient)',
  `amc_code` varchar(10) NOT NULL default '' COMMENT 'Automated Measure Calculation indentifier (MU rule)',
  `amc_code_2014` varchar(30) NOT NULL default '' COMMENT 'Automated Measure Calculation 2014 indentifier (MU rule)',
  `amc_2014_stage1_flag` tinyint(1) COMMENT '2014 Stage 1 - Automated Measure Calculation flag for (unable to customize per patient)',
  `amc_2014_stage2_flag` tinyint(1) COMMENT '2014 Stage 2 - Automated Measure Calculation flag for (unable to customize per patient)',
  `patient_reminder_flag` tinyint(1) COMMENT 'Clinical Reminder Module flag',
  `developer` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Clinical Rule Developer',
  `funding_source` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Clinical Rule Funding Source',
  `release_version` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Clinical Rule Release Version',
  `web_reference` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Clinical Rule Web Reference',
  `access_control` VARCHAR(255) NOT NULL DEFAULT 'patients:med' COMMENT 'ACO link for access control',
  PRIMARY KEY  (`id`,`pid`)
) ENGINE=InnoDB ;

--
-- Automated Measure Calculation (AMC) rules
--
-- MU 170.302(c) Maintain an up-to-date problem list of current and active diagnoses (2014-MU-AMC:170.314(g)(1)/(2)–4)
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `amc_2014_flag`, `amc_code_2014`, `patient_reminder_flag`, `amc_2014_stage1_flag` ) VALUES ('problem_list_amc', 0, 0, 0, 0, '', '', 1, 1, '170.302(c)', 1, '170.314(g)(1)/(2)–4', 0, 1);
-- MU 170.302(d) Maintain active medication list
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `amc_2014_flag`, `amc_code_2014`, `patient_reminder_flag`, `amc_2014_stage1_flag` ) VALUES ('med_list_amc', 0, 0, 0, 0, '', '', 1, 1, '170.302(d)', 1, '170.314(g)(1)/(2)–5', 0, 1);
-- MU 170.302(e) Maintain active medication allergy list
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `amc_2014_flag`, `amc_code_2014`, `patient_reminder_flag`, `amc_2014_stage1_flag` ) VALUES ('med_allergy_list_amc', 0, 0, 0, 0, '', '', 1, 1, '170.302(e)', 1, '170.314(g)(1)/(2)–6', 0, 1);
-- MU 170.302(f) Record and chart changes in vital signs
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('record_vitals_amc', 0, 0, 0, 0, '', '', 1, 1, '170.302(f)', 0);
-- MU 170.302(g) Record smoking status for patients 13 years old or older
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `amc_2014_flag`, `amc_code_2014`, `patient_reminder_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag` ) VALUES ('record_smoke_amc', 0, 0, 0, 0, '', '', 1, 1, '170.302(g)', 1, '170.314(g)(1)/(2)–11', 0, 1, 1);
-- MU 170.302(h) Incorporate clinical lab-test results into certified EHR technology as
--               structured data
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `amc_2014_flag`, `amc_code_2014`, `patient_reminder_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag` ) VALUES ('lab_result_amc', 0, 0, 0, 0, '', '', 1, 1, '170.302(h)', 1, '170.314(g)(1)/(2)–12', 0, 1, 1);
-- MU 170.302(j) The EP, eligible hospital or CAH who receives a patient from another
--               setting of care or provider of care or believes an encounter is relevant
--               should perform medication reconciliation
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `amc_2014_flag`, `amc_code_2014`, `patient_reminder_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag` ) VALUES ('med_reconc_amc', 0, 0, 0, 0, '', '', 1, 1, '170.302(j)', 1, '170.314(g)(1)/(2)–17', 0, 1, 1);
-- MU 170.302(m) Use certified EHR technology to identify patient-specific education resources
--              and provide those resources to the patient if appropriate
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `patient_reminder_flag`, `amc_2014_flag`, `amc_code_2014`, `amc_2014_stage1_flag` ) VALUES ('patient_edu_amc', 0, 0, 0, 0, '', '', 1, 1, '170.302(m)', 0, 1, '170.314(g)(1)/(2)–16', 1);
-- MU 170.304(a) Use CPOE for medication orders directly entered by any licensed healthcare
--              professional who can enter orders into the medical record per state, local
--              and professional guidelines
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `patient_reminder_flag`, `amc_2014_flag`, `amc_code_2014`, `amc_2014_stage1_flag` ) VALUES ('cpoe_med_amc', 0, 0, 0, 0, '', '', 1, 1, '170.304(a)', 0, 1, '170.314(g)(1)/(2)–7', 1);
-- MU 170.304(b) Generate and transmit permissible prescriptions electronically (eRx)
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('e_prescribe_amc', 0, 0, 0, 0, '', '', 1, 1, '170.304(b)', 0);
-- MU 170.304(c) Record demographics
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `amc_2014_flag`, `amc_code_2014`, `patient_reminder_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag` ) VALUES ('record_dem_amc', 0, 0, 0, 0, '', '', 1, 1, '170.304(c)', 1, '170.314(g)(1)/(2)–9', 0, 1, 1);
-- MU 170.304(d) Send reminders to patients per patient preference for preventive/follow up care
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `patient_reminder_flag`, `amc_2014_flag`, `amc_code_2014`, `amc_2014_stage1_flag` ) VALUES ('send_reminder_amc', 0, 0, 0, 0, '', '', 1, 1, '170.304(d)', 0, 1, '170.314(g)(1)/(2)–13', 1);
-- MU 170.304(f) Provide patients with an electronic copy of their health information
--               (including diagnostic test results, problem list, medication lists,
--               medication allergies), upon request
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('provide_rec_pat_amc', 0, 0, 0, 0, '', '', 1, 1, '170.304(f)', 0);
-- MU 170.304(g) Provide patients with timely electronic access to their health information
--              (including lab results, problem list, medication lists, medication allergies)
--              within four business days of the information being available to the EP
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('timely_access_amc', 0, 0, 0, 0, '', '', 1, 1, '170.304(g)', 0);
-- MU 170.304(h) Provide clinical summaries for patients for each office visit
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `patient_reminder_flag`, `amc_2014_flag`, `amc_code_2014`, `amc_2014_stage1_flag` ) VALUES ('provide_sum_pat_amc', 0, 0, 0, 0, '', '', 1, 1, '170.304(h)', 0, 1, '170.314(g)(1)/(2)–15', 1);
-- MU 170.304(i) The EP, eligible hospital or CAH who transitions their patient to
--               another setting of care or provider of care or refers their patient to
--               another provider of care should provide summary of care record for
--               each transition of care or referral
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_2011_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('send_sum_amc', 0, 0, 0, 0, '', '', 1, 1, '170.304(i)', 0);
--
-- Clinical Quality Measure (CQM) rules
--
-- NQF 0013 Hypertension: Blood Pressure Measurement
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `cqm_2014_flag` ) VALUES ('rule_htn_bp_measure_cqm', 0, 0, 0, 1, 1, '0018', '', 0, '', 0, 1);
-- NQF 0028a Tobacco Use Assessment
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_tob_use_assess_cqm', 0, 0, 0, 1, 1, '0028a', '', 0, '', 0);
-- NQF 0028b Tobacco Cessation Intervention
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_tob_cess_inter_cqm', 0, 0, 0, 1, 1, '0028b', '', 0, '', 0);
-- NQF 0421 (PQRI 128) Adult Weight Screening and Follow-Up
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `cqm_2014_flag` ) VALUES ('rule_adult_wt_screen_fu_cqm', 0, 0, 0, 1, 1, '0421', '128', 0, '', 0, 1);
-- NQF 0024 Weight Assessment and Counseling for Children and Adolescents
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, cqm_2014_flag ) VALUES ('rule_wt_assess_couns_child_cqm', 0, 0, 0, 1, 1, '0024', '', 0, '', 0, 1);
-- NQF 0041 (PQRI 110) Influenza Immunization for Patients >= 50 Years Old
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `cqm_2014_flag` ) VALUES ('rule_influenza_ge_50_cqm', 0, 0, 0, 1, 1, '0041', '110', 0, '', 0, 1);
-- NQF 0038 Childhood immunization Status
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_child_immun_stat_cqm', 0, 0, 0, 1, 1, '0038', '', 0, '', 0);
-- NQF 0043 (PQRI 111) Pneumonia Vaccination Status for Older Adults
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `cqm_2014_flag` ) VALUES ('rule_pneumovacc_ge_65_cqm', 0, 0, 0, 1, 1, '0043', '111', 0, '', 0, 1);
-- NQF 0055 (PQRI 117) Diabetes: Eye Exam
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_dm_eye_cqm', 0, 0, 0, 1, 1, '0055', '117', 0, '', 0);
-- NQF 0056 (PQRI 163) Diabetes: Foot Exam
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_dm_foot_cqm', 0, 0, 0, 1, 1, '0056', '163', 0, '', 0);
-- NQF 0059 (PQRI 1) Diabetes: HbA1c Poor Control
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `cqm_2014_flag` ) VALUES ('rule_dm_a1c_cqm', 0, 0, 0, 1, 1, '0059', '1', 0, '', 0, 1);
-- NQF 0061 (PQRI 3) Diabetes: Blood Pressure Management
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_dm_bp_control_cqm', 0, 0, 0, 1, 1, '0061', '3', 0, '', 0);
-- NQF 0064 (PQRI 2) Diabetes: LDL Management & Control
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_2011_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_dm_ldl_cqm', 0, 0, 0, 1, 1, '0064', '2', 0, '', 0);
-- NQF 0002 Rule Children Pharyngitis
INSERT INTO `clinical_rules` (`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`) VALUES
('rule_children_pharyngitis_cqm', 0, 0, 0, 1, '0002', '', 0, '', 0, 0, 0, '', 1, 1);
-- NQF 0101 Rule Fall Screening
INSERT INTO `clinical_rules` (`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`) VALUES
('rule_fall_screening_cqm', 0, 0, 0, 1, '0101', '', 0, '', 0, 0, 0, '', 1, 1);
-- NQF 0384 Rule Pain Intensity
INSERT INTO `clinical_rules` (`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`) VALUES
('rule_pain_intensity_cqm', 0, 0, 0, 1, '0384', '', 0, '', 0, 0, 0, '', 1, 1);
-- NQF 0038 Rule Child Immunization Status
INSERT INTO `clinical_rules` (`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('rule_child_immun_stat_2014_cqm', 0, 0, 0, 1, '0038', '', 0, '', 0, 0, 0, '', 0, 1, 0, 0);
-- NQF 0028 Rule Tobacco Use
INSERT INTO `clinical_rules` (`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('rule_tob_use_2014_cqm', 0, 0, 0, 1, '0028', '', 0, '', 0, 0, 0, '', 0, 1, 0, 0);
--
-- Standard clinical rules
--
-- Hypertension: Blood Pressure Measurement
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_htn_bp_measure', 0, 0, 1, 0, '', '', 0, '', 0);
-- Tobacco Use Assessment
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_tob_use_assess', 0, 0, 1, 0, '', '', 0, '', 0);
-- Tobacco Cessation Intervention
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_tob_cess_inter', 0, 0, 1, 0, '', '', 0, '', 0);
-- Adult Weight Screening and Follow-Up
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_adult_wt_screen_fu', 0, 0, 1, 0, '', '', 0, '', 0);
-- Weight Assessment and Counseling for Children and Adolescents
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_wt_assess_couns_child', 0, 0, 1, 0, '', '', 0, '', 0);
-- Influenza Immunization for Patients >= 50 Years Old
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_influenza_ge_50', 0, 0, 1, 0, '', '', 0, '', 0);
-- Pneumonia Vaccination Status for Older Adults
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_pneumovacc_ge_65', 0, 0, 1, 0, '', '', 0, '', 0);
-- Diabetes: Hemoglobin A1C
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_dm_hemo_a1c', 0, 0, 1, 0, '', '', 0, '', 0);
-- Diabetes: Urine Microalbumin
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_dm_urine_alb', 0, 0, 1, 0, '', '', 0, '', 0);
-- Diabetes: Eye Exam
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_dm_eye', 0, 0, 1, 0, '', '', 0, '', 0);
-- Diabetes: Foot Exam
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_dm_foot', 0, 0, 1, 0, '', '', 0, '', 0);
-- Cancer Screening: Mammogram
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_cs_mammo', 0, 0, 1, 0, '', '', 0, '', 0);
-- Cancer Screening: Pap Smear
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_cs_pap', 0, 0, 1, 0, '', '', 0, '', 0);
-- Cancer Screening: Colon Cancer Screening
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_cs_colon', 0, 0, 1, 0, '', '', 0, '', 0);
-- Cancer Screening: Prostate Cancer Screening
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_cs_prostate', 0, 0, 1, 0, '', '', 0, '', 0);
--
-- Rules to specifically demonstrate passing of NIST criteria
--
-- Coumadin Management - INR Monitoring
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_inr_monitor', 0, 0, 1, 0, '', '', 0, '', 0);
--
-- Rule to specifically demonstrate MU2 for CDR engine
--
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `access_control` ) VALUES ('rule_socsec_entry', 0, 0, 0, 0, '', '', 0, '', 0, 'admin:practice');
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_penicillin_allergy', 0, 0, 0, 0, '', '', 0, '', 0);
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_blood_pressure', 0, 0, 0, 0, '', '', 0, '', 0);
INSERT INTO `clinical_rules` ( `id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag` ) VALUES ('rule_inr_measure', 0, 0, 0, 0, '', '', 0, '', 0);
--
-- MU2 AMC rules
--
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('image_results_amc', 0, 0, 0, 0, '', '', 1, '', 0, 0, 1, '170.314(g)(1)/(2)–20', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('family_health_history_amc', 0, 0, 0, 0, '', '', 1, '', 0, 0, 1, '170.314(g)(1)/(2)–21', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('electronic_notes_amc', 0, 0, 0, 0, '', '', 1, '', 0, 0, 1, '170.314(g)(1)/(2)–22', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('secure_messaging_amc', 0, 0, 0, 0, '', '', 1, '', 0, 0, 1, '170.314(g)(1)/(2)-19', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('view_download_transmit_amc', 0, 0, 0, 0, '', '', 1, '', 0, 0, 1, '170.314(g)(1)/(2)–14', 0, 0, 1, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('cpoe_radiology_amc', 0, 0, 0, 0, '', '', 1, '170.304(a)', 0, 0, 1, '170.314(g)(1)/(2)–7', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('cpoe_proc_orders_amc', 0, 0, 0, 0, '', '', 1, '170.304(a)', 0, 0, 1, '170.314(g)(1)/(2)–7', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('send_reminder_stage2_amc', 0, 0, 0, 0, '', '', 1, '170.304(d)', 0, 0, 1, '170.314(g)(1)/(2)–13', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('cpoe_med_stage1_amc_alternative', 0, 0, 0, 0, '', '', 1, '170.304(a)', 0, 0, 1, '170.314(g)(1)/(2)–7', 0, 0, 1, 0);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('cpoe_med_stage2_amc', 0, 0, 0, 0, '', '', 1, '170.304(a)', 0, 0, 1, '170.314(g)(1)/(2)–7', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('patient_edu_stage2_amc', 0, 0, 0, 0, '', '', 1, '170.302(m)', 0, 0, 1, '170.314(g)(1)/(2)–16', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('record_vitals_1_stage1_amc', 0, 0, 0, 0, '', '', 1, '170.302(f)', 0, 0, 1, '170.314(g)(1)/(2)–10', 0, 0, 0, 0);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('record_vitals_2_stage1_amc', 0, 0, 0, 0, '', '', 1, '170.302(f)', 0, 0, 1, '170.314(g)(1)/(2)–10', 0, 0, 1, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('record_vitals_3_stage1_amc', 0, 0, 0, 0, '', '', 1, '170.302(f)', 0, 0, 1, '170.314(g)(1)/(2)–10', 0, 0, 1, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('record_vitals_4_stage1_amc', 0, 0, 0, 0, '', '', 1, '170.302(f)', 0, 0, 1, '170.314(g)(1)/(2)–10', 0, 0, 1, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('record_vitals_stage2_amc', 0, 0, 0, 0, '', '', 1, '170.302(f)', 0, 0, 1, '170.314(g)(1)/(2)–10', 0, 0, 0, 0);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('provide_sum_pat_stage2_amc', 0, 0, 0, 0, '', '', 1, '170.304(h)', 0, 0, 1, '170.314(g)(1)/(2)–15', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('vdt_stage2_amc', 0, 0, 0, 0, '', '', 1, '', 0, 0, 1, '170.314(g)(1)/(2)–14', 0, 0, 1, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('send_sum_stage1_amc', 0, 0, 0, 0, '', '', 1, '170.304(i)', 0, 0, 1, '170.314(g)(1)/(2)–18', 0, 0, 1, 0);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('send_sum_1_stage2_amc', 0, 0, 0, 0, '', '', 1, '170.304(i)', 0, 0, 1, '170.314(g)(1)/(2)–18', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('send_sum_stage2_amc', 0, 0, 0, 0, '', '', 1, '170.304(i)', 0, 0, 1, '170.314(g)(1)/(2)–18', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('e_prescribe_stage1_amc', 0, 0, 0, 0, '', '', 1, '170.304(b)', 0, 0, 1, '170.314(g)(1)/(2)–8', 0, 0, 1, 0);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('e_prescribe_1_stage2_amc', 0, 0, 0, 0, '', '', 1, '170.304(b)', 0, 0, 1, '170.314(g)(1)/(2)–8', 0, 0, 0, 1);
INSERT INTO `clinical_rules`
(`id`, `pid`, `active_alert_flag`, `passive_alert_flag`, `cqm_flag`, `cqm_nqf_code`, `cqm_pqri_code`, `amc_flag`, `amc_code`, `patient_reminder_flag`, `amc_2011_flag`, `amc_2014_flag`, `amc_code_2014`, `cqm_2011_flag`, `cqm_2014_flag`, `amc_2014_stage1_flag`, `amc_2014_stage2_flag`) VALUES
('e_prescribe_2_stage2_amc', 0, 0, 0, 0, '', '', 1, '170.304(b)', 0, 0, 1, '170.314(g)(1)/(2)–8', 0, 0, 0, 1);


-- --------------------------------------------------------

--
-- Table structure for table `clinical_rules_log
--

DROP TABLE IF EXISTS `clinical_rules_log`;
CREATE TABLE `clinical_rules_log` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime DEFAULT NULL,
  `pid` bigint(20) NOT NULL DEFAULT '0',
  `uid` bigint(20) NOT NULL DEFAULT '0',
  `category` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'An example category is clinical_reminder_widget',
  `value` TEXT,
  `new_value` TEXT,
  PRIMARY KEY (`id`),
  KEY `pid` (`pid`),
  KEY `uid` (`uid`),
  KEY `category` (`category`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;


-- --------------------------------------------------------

--
-- Table structure for table `codes`
--

DROP TABLE IF EXISTS `codes`;
CREATE TABLE `codes` (
  `id` int(11) NOT NULL auto_increment,
  `code_text` varchar(255) NOT NULL default '',
  `code_text_short` varchar(24) NOT NULL default '',
  `code` varchar(25) NOT NULL default '',
  `code_type` smallint(6) default NULL,
  `modifier` varchar(12) NOT NULL default '',
  `units` int(11) default NULL,
  `fee` decimal(12,2) default NULL,
  `superbill` varchar(31) NOT NULL default '',
  `related_code` varchar(255) NOT NULL default '',
  `taxrates` varchar(255) NOT NULL default '',
  `cyp_factor` float NOT NULL DEFAULT 0 COMMENT 'quantity representing a years supply',
  `active` TINYINT(1) DEFAULT 1 COMMENT '0 = inactive, 1 = active',
  `reportable` TINYINT(1) DEFAULT 0 COMMENT '0 = non-reportable, 1 = reportable',
  `financial_reporting` TINYINT(1) DEFAULT 0 COMMENT '0 = negative, 1 = considered important code in financial reporting',
  PRIMARY KEY  (`id`),
  KEY `code` (`code`),
  KEY `code_type` (`code_type`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('suspension','C60928',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('tablet','C42998',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('capsule','C25158',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('solution','C42986',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('tsp','C48544',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('ml','C28254',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('units','C44278',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('inhalations','C42944',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('gtts(drops)','C48491',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('cream','C28944',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('ointment','C42966',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('Per Oris','C38288',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('Inhale','C38216',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('Intramuscular','C28161',112);
INSERT INTO `codes` (`code_text`,`code`,`code_type`) VALUES ('mg','C28253',112);

-- --------------------------------------------------------

--
-- Table structure for table `syndromic_surveillance`
--

DROP TABLE IF EXISTS `syndromic_surveillance`;
CREATE TABLE `syndromic_surveillance` (
  `id` bigint(20) NOT NULL auto_increment,
  `lists_id` bigint(20) NOT NULL,
  `submission_date` datetime NOT NULL,
  `filename` varchar(255) NOT NULL default '',
  PRIMARY KEY  (`id`),
  KEY (`lists_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `config`
--

DROP TABLE IF EXISTS `config`;
CREATE TABLE `config` (
  `id` int(11) NOT NULL default '0',
  `name` varchar(255) default NULL,
  `value` varchar(255) default NULL,
  `parent` int(11) NOT NULL default '0',
  `lft` int(11) NOT NULL default '0',
  `rght` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `parent` (`parent`),
  KEY `lft` (`lft`,`rght`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `config_seq`
--

DROP TABLE IF EXISTS `config_seq`;
CREATE TABLE `config_seq` (
  `id` int(11) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB;

--
-- Dumping data for table `config_seq`
--

INSERT INTO `config_seq` VALUES (0);

-- --------------------------------------------------------

--
-- Table structure for table `dated_reminders`
--

DROP TABLE IF EXISTS `dated_reminders`;
CREATE TABLE `dated_reminders` (
  `dr_id` int(11) NOT NULL AUTO_INCREMENT,
  `dr_from_ID` int(11) NOT NULL,
  `dr_message_text` varchar(160) NOT NULL,
  `dr_message_sent_date` datetime NOT NULL,
  `dr_message_due_date` date NOT NULL,
  `pid` int(11) NOT NULL,
  `message_priority` tinyint(1) NOT NULL,
  `message_processed` tinyint(1) NOT NULL DEFAULT '0',
  `processed_date` timestamp NULL DEFAULT NULL,
  `dr_processed_by` int(11) NOT NULL,
  PRIMARY KEY (`dr_id`),
  KEY `dr_from_ID` (`dr_from_ID`,`dr_message_due_date`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- --------------------------------------------------------

--
-- Table structure for table `dated_reminders_link`
--

DROP TABLE IF EXISTS `dated_reminders_link`;
CREATE TABLE `dated_reminders_link` (
  `dr_link_id` int(11) NOT NULL AUTO_INCREMENT,
  `dr_id` int(11) NOT NULL,
  `to_id` int(11) NOT NULL,
  PRIMARY KEY (`dr_link_id`),
  KEY `to_id` (`to_id`),
  KEY `dr_id` (`dr_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- --------------------------------------------------------

--
-- Table structure for table `direct_message_log`
--

DROP TABLE IF EXISTS `direct_message_log`;
CREATE TABLE `direct_message_log` (
  `id` bigint(20) NOT NULL auto_increment,
  `msg_type` char(1) NOT NULL COMMENT 'S=sent,R=received',
  `msg_id` varchar(127) NOT NULL,
  `sender` varchar(255) NOT NULL,
  `recipient` varchar(255) NOT NULL,
  `create_ts` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `status` char(1) NOT NULL COMMENT 'Q=queued,D=dispatched,R=received,F=failed',
  `status_info` varchar(511) default NULL,
  `status_ts` timestamp NULL default NULL,
  `patient_id` bigint(20) default NULL,
  `user_id` bigint(20) default NULL,
  PRIMARY KEY  (`id`),
  KEY `msg_id` (`msg_id`),
  KEY `patient_id` (`patient_id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `documents`
--

DROP TABLE IF EXISTS `documents`;
CREATE TABLE `documents` (
  `id` int(11) NOT NULL default '0',
  `type` enum('file_url','blob','web_url') default NULL,
  `size` int(11) default NULL,
  `date` datetime default NULL,
  `url` varchar(255) default NULL,
  `thumb_url` varchar(255) default NULL,
  `mimetype` varchar(255) default NULL,
  `pages` int(11) default NULL,
  `owner` int(11) default NULL,
  `revision` timestamp NOT NULL,
  `foreign_id` int(11) default NULL,
  `docdate` date default NULL,
  `hash` varchar(40) DEFAULT NULL COMMENT '40-character SHA-1 hash of document',
  `list_id` bigint(20) NOT NULL default '0',
  `couch_docid` VARCHAR(100) DEFAULT NULL,
  `couch_revid` VARCHAR(100) DEFAULT NULL,
  `storagemethod` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '0->Harddisk,1->CouchDB',
  `path_depth` TINYINT DEFAULT '1' COMMENT 'Depth of path to use in url to find document. Not applicable for CouchDB.',
  `imported` TINYINT DEFAULT 0 NULL COMMENT 'Parsing status for CCR/CCD/CCDA importing',
  `encounter_id` bigint(20) NOT NULL DEFAULT '0' COMMENT 'Encounter id if tagged',
  `encounter_check`	TINYINT(1) NOT NULL DEFAULT '0' COMMENT 'If encounter is created while tagging',
  `audit_master_approval_status` TINYINT NOT NULL DEFAULT 1 COMMENT 'approval_status from audit_master table',
  `audit_master_id` int(11) default NULL,
  `documentationOf` varchar(255) DEFAULT NULL,
  PRIMARY KEY  (`id`),
  KEY `revision` (`revision`),
  KEY `foreign_id` (`foreign_id`),
  KEY `owner` (`owner`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `documents_legal_detail`
--

DROP TABLE IF EXISTS `documents_legal_detail`;
CREATE TABLE `documents_legal_detail` (
  `dld_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `dld_pid` int(10) unsigned DEFAULT NULL,
  `dld_facility` int(10) unsigned DEFAULT NULL,
  `dld_provider` int(10) unsigned DEFAULT NULL,
  `dld_encounter` int(10) unsigned DEFAULT NULL,
  `dld_master_docid` int(10) unsigned NOT NULL,
  `dld_signed` smallint(5) unsigned NOT NULL COMMENT '0-Not Signed or Cannot Sign(Layout),1-Signed,2-Ready to sign,3-Denied(Pat Regi),4-Patient Upload,10-Save(Layout)',
  `dld_signed_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `dld_filepath` varchar(75) DEFAULT NULL,
  `dld_filename` varchar(45) NOT NULL,
  `dld_signing_person` varchar(50) NOT NULL,
  `dld_sign_level` int(11) NOT NULL COMMENT 'Sign flow level',
  `dld_content` varchar(50) NOT NULL COMMENT 'Layout sign position',
  `dld_file_for_pdf_generation` blob NOT NULL COMMENT 'The filled details in the fdf file is stored here.Patient Registration Screen',
  `dld_denial_reason` longtext,
  `dld_moved` tinyint(4) NOT NULL DEFAULT '0',
  `dld_patient_comments` text COMMENT 'Patient comments stored here',
  PRIMARY KEY (`dld_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `documents_legal_master`
--

DROP TABLE IF EXISTS `documents_legal_master`;
CREATE TABLE `documents_legal_master` (
  `dlm_category` int(10) unsigned DEFAULT NULL,
  `dlm_subcategory` int(10) unsigned DEFAULT NULL,
  `dlm_document_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `dlm_document_name` varchar(75) NOT NULL,
  `dlm_filepath` varchar(75) NOT NULL,
  `dlm_facility` int(10) unsigned DEFAULT NULL,
  `dlm_provider` int(10) unsigned DEFAULT NULL,
  `dlm_sign_height` double NOT NULL,
  `dlm_sign_width` double NOT NULL,
  `dlm_filename` varchar(45) NOT NULL,
  `dlm_effective_date` datetime NOT NULL,
  `dlm_version` int(10) unsigned NOT NULL,
  `content` varchar(255) NOT NULL,
  `dlm_savedsign` varchar(255) DEFAULT NULL COMMENT '0-Yes 1-No',
  `dlm_review` varchar(255) DEFAULT NULL COMMENT '0-Yes 1-No',
  `dlm_upload_type` tinyint(4) DEFAULT '0' COMMENT '0-Provider Uploaded,1-Patient Uploaded',
  PRIMARY KEY (`dlm_document_id`)
) ENGINE=InnoDB COMMENT='List of Master Docs to be signed' AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `documents_legal_categories`
--

DROP TABLE IF EXISTS `documents_legal_categories`;
CREATE TABLE `documents_legal_categories` (
  `dlc_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `dlc_category_type` int(10) unsigned NOT NULL COMMENT '1 category 2 subcategory',
  `dlc_category_name` varchar(45) NOT NULL,
  `dlc_category_parent` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`dlc_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 ;

--
-- Dumping data for table `documents_legal_categories`
--

INSERT INTO `documents_legal_categories` (`dlc_id`, `dlc_category_type`, `dlc_category_name`, `dlc_category_parent`) VALUES
(3, 1, 'Category', NULL),
(4, 2, 'Sub Category', 1),
(5, 1, 'Layout Form', 0),
(6, 2, 'Layout Signed', 5);

--
-- Table structure for table `drug_inventory`
--

DROP TABLE IF EXISTS `drug_inventory`;
CREATE TABLE `drug_inventory` (
  `inventory_id` int(11) NOT NULL auto_increment,
  `drug_id` int(11) NOT NULL,
  `lot_number` varchar(20) default NULL,
  `expiration` date default NULL,
  `manufacturer` varchar(255) default NULL,
  `on_hand` int(11) NOT NULL default '0',
  `warehouse_id` varchar(31) NOT NULL DEFAULT '',
  `vendor_id` bigint(20) NOT NULL DEFAULT 0,
  `last_notify` date NOT NULL default '0000-00-00',
  `destroy_date` date default NULL,
  `destroy_method` varchar(255) default NULL,
  `destroy_witness` varchar(255) default NULL,
  `destroy_notes` varchar(255) default NULL,
  PRIMARY KEY  (`inventory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `drug_sales`
--

DROP TABLE IF EXISTS `drug_sales`;
CREATE TABLE `drug_sales` (
  `sale_id` int(11) NOT NULL auto_increment,
  `drug_id` int(11) NOT NULL,
  `inventory_id` int(11) NOT NULL,
  `prescription_id` int(11) NOT NULL default '0',
  `pid` int(11) NOT NULL default '0',
  `encounter` int(11) NOT NULL default '0',
  `user` varchar(255) default NULL,
  `sale_date` date NOT NULL,
  `quantity` int(11) NOT NULL default '0',
  `fee` decimal(12,2) NOT NULL default '0.00',
  `billed` tinyint(1) NOT NULL default '0' COMMENT 'indicates if the sale is posted to accounting',
  `xfer_inventory_id` int(11) NOT NULL DEFAULT 0,
  `distributor_id` bigint(20) NOT NULL DEFAULT 0 COMMENT 'references users.id',
  `notes` varchar(255) NOT NULL DEFAULT '',
  `bill_date` datetime default NULL,
  `pricelevel` varchar(31) default '',
  `selector` varchar(255) default '' comment 'references drug_templates.selector',
  PRIMARY KEY  (`sale_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `drug_templates`
--

DROP TABLE IF EXISTS `drug_templates`;
CREATE TABLE `drug_templates` (
  `drug_id` int(11) NOT NULL,
  `selector` varchar(255) NOT NULL default '',
  `dosage` varchar(10) default NULL,
  `period` int(11) NOT NULL default '0',
  `quantity` int(11) NOT NULL default '0',
  `refills` int(11) NOT NULL default '0',
  `taxrates` varchar(255) default NULL,
  PRIMARY KEY  (`drug_id`,`selector`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `drugs`
--

DROP TABLE IF EXISTS `drugs`;
CREATE TABLE `drugs` (
  `drug_id` int(11) NOT NULL auto_increment,
  `name` varchar(255) NOT NULL DEFAULT '',
  `ndc_number` varchar(20) NOT NULL DEFAULT '',
  `on_order` int(11) NOT NULL default '0',
  `reorder_point` float NOT NULL DEFAULT 0.0,
  `max_level` float NOT NULL DEFAULT 0.0,
  `last_notify` date NOT NULL default '0000-00-00',
  `reactions` text,
  `form` int(3) NOT NULL default '0',
  `size` varchar(25) NOT NULL default '',
  `unit` int(11) NOT NULL default '0',
  `route` int(11) NOT NULL default '0',
  `substitute` int(11) NOT NULL default '0',
  `related_code` varchar(255) NOT NULL DEFAULT '' COMMENT 'may reference a related codes.code',
  `cyp_factor` float NOT NULL DEFAULT 0 COMMENT 'quantity representing a years supply',
  `active` TINYINT(1) DEFAULT 1 COMMENT '0 = inactive, 1 = active',
  `allow_combining` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1 = allow filling an order from multiple lots',
  `allow_multiple`  tinyint(1) NOT NULL DEFAULT 1 COMMENT '1 = allow multiple lots at one warehouse',
  `drug_code` varchar(25) NULL,
  `consumable` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1 = will not show on the fee sheet',
  `dispensable` tinyint(1) NOT NULL DEFAULT 1 COMMENT '0 = pharmacy elsewhere, 1 = dispensed here',
  PRIMARY KEY  (`drug_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `eligibility_response`
--

DROP TABLE IF EXISTS `eligibility_response`;
CREATE TABLE `eligibility_response` (
  `response_id` bigint(20) NOT NULL auto_increment,
  `response_description` varchar(255) default NULL,
  `response_status` enum('A','D') NOT NULL default 'A',
  `response_vendor_id` bigint(20) default NULL,
  `response_create_date` date default NULL,
  `response_modify_date` date default NULL,
  PRIMARY KEY  (`response_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- --------------------------------------------------------

--
-- Table structure for table `eligibility_verification`
--

DROP TABLE IF EXISTS `eligibility_verification`;
CREATE TABLE `eligibility_verification` (
  `verification_id` bigint(20) NOT NULL auto_increment,
  `response_id` bigint(20) default NULL,
  `insurance_id` bigint(20) default NULL,
  `eligibility_check_date` datetime default NULL,
  `copay` int(11) default NULL,
  `deductible` int(11) default NULL,
  `deductiblemet` enum('Y','N') default 'Y',
  `create_date` date default NULL,
  PRIMARY KEY  (`verification_id`),
  KEY `insurance_id` (`insurance_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- --------------------------------------------------------

--
-- Table structure for table `employer_data`
--

DROP TABLE IF EXISTS `employer_data`;
CREATE TABLE `employer_data` (
  `id` bigint(20) NOT NULL auto_increment,
  `name` varchar(255) default NULL,
  `street` varchar(255) default NULL,
  `postal_code` varchar(255) default NULL,
  `city` varchar(255) default NULL,
  `state` varchar(255) default NULL,
  `country` varchar(255) default NULL,
  `date` datetime default NULL,
  `pid` bigint(20) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `pid` (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `enc_category_map`
--
-- Mapping of rule encounter categories to category ids from the event category in openemr_postcalendar_categories
--

DROP TABLE IF EXISTS `enc_category_map`;
CREATE TABLE `enc_category_map` (
  `rule_enc_id` varchar(31) NOT NULL DEFAULT '' COMMENT 'encounter id from rule_enc_types list in list_options',
  `main_cat_id` int(11) NOT NULL DEFAULT 0 COMMENT 'category id from event category in openemr_postcalendar_categories',
  KEY  (`rule_enc_id`,`main_cat_id`)
) ENGINE=InnoDB ;

INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_outpatient', 5);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_outpatient', 9);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_outpatient', 10);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_nurs_fac', 5);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_nurs_fac', 9);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_nurs_fac', 10);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_off_vis', 5);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_off_vis', 9);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_off_vis', 10);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_hea_and_beh', 12);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_occ_ther', 5);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_occ_ther', 9);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_occ_ther', 10);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_psych_and_psych', 5);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_psych_and_psych', 9);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_psych_and_psych', 10);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_pre_med_ser_18_older', 13);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_pre_med_ser_40_older', 13);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_pre_ind_counsel', 13);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_pre_med_group_counsel', 13);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_pre_med_other_serv', 13);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_out_pcp_obgyn', 5);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_out_pcp_obgyn', 9);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_out_pcp_obgyn', 10);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_nurs_discharge', 5);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_nurs_discharge', 9);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_nurs_discharge', 10);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_acute_inp_or_ed', 5);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_acute_inp_or_ed', 9);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_acute_inp_or_ed', 10);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_nonac_inp_out_or_opth', 5);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_nonac_inp_out_or_opth', 9);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_nonac_inp_out_or_opth', 10);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_influenza', 5);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_influenza', 9);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_influenza', 10);
INSERT INTO `enc_category_map` ( `rule_enc_id`, `main_cat_id` ) VALUES ('enc_ophthal_serv', 14);


-- --------------------------------------------------------

--
-- Table structure for table `erx_ttl_touch`
--
-- Store records last update per patient data process
--

DROP TABLE IF EXISTS `erx_ttl_touch`;
CREATE  TABLE `erx_ttl_touch` (
  `patient_id` BIGINT(20) UNSIGNED NOT NULL COMMENT 'Patient record Id' ,
  `process` ENUM('allergies','medications') NOT NULL COMMENT 'NewCrop eRx SOAP process' ,
  `updated` DATETIME NOT NULL COMMENT 'Date and time of last process update for patient' ,
  PRIMARY KEY (`patient_id`, `process`)
) ENGINE = InnoDB COMMENT = 'Store records last update per patient data process' ;

-- --------------------------------------------------------

--
-- Table structure for table `standardized_tables_track`
--

DROP TABLE IF EXISTS `standardized_tables_track`;
CREATE TABLE `standardized_tables_track` (
  `id` int(11) NOT NULL auto_increment,
  `imported_date` datetime default NULL,
  `name` varchar(255) NOT NULL default '' COMMENT 'name of standardized tables such as RXNORM',
  `revision_version` varchar(255) NOT NULL default '' COMMENT 'revision of standardized tables that were imported',
  `revision_date` datetime default NULL COMMENT 'revision of standardized tables that were imported',
  `file_checksum` varchar(32) NOT NULL DEFAULT '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `facility`
--

DROP TABLE IF EXISTS `facility`;
CREATE TABLE `facility` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) default NULL,
  `phone` varchar(30) default NULL,
  `fax` varchar(30) default NULL,
  `street` varchar(255) default NULL,
  `city` varchar(255) default NULL,
  `state` varchar(50) default NULL,
  `postal_code` varchar(11) default NULL,
  `country_code` varchar(10) default NULL,
  `federal_ein` varchar(15) default NULL,
  `website` varchar(255) default NULL,
  `email` varchar(255) default NULL,
  `service_location` tinyint(1) NOT NULL default '1',
  `billing_location` tinyint(1) NOT NULL default '0',
  `accepts_assignment` tinyint(1) NOT NULL default '0',
  `pos_code` tinyint(4) default NULL,
  `x12_sender_id` varchar(25) default NULL,
  `attn` varchar(65) default NULL,
  `domain_identifier` varchar(60) default NULL,
  `facility_npi` varchar(15) default NULL,
  `tax_id_type` VARCHAR(31) NOT NULL DEFAULT '',
  `color` VARCHAR(7) NOT NULL DEFAULT '',
  `primary_business_entity` INT(10) NOT NULL DEFAULT '0' COMMENT '0-Not Set as business entity 1-Set as business entity',
  `facility_code` VARCHAR(31) default NULL,
  `extra_validation` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 ;

--
-- Dumping data for table `facility`
--

INSERT INTO `facility` VALUES (3, 'Your Clinic Name Here', '000-000-0000', '000-000-0000', '', '', '', '', '', '', NULL, NULL, 1, 1, 0, NULL, '', '', '', '', '','#99FFFF','0', '', '1');


-- --------------------------------------------------------

--
-- Table structure for table `facility_user_ids`
--

DROP TABLE IF EXISTS `facility_user_ids`;
CREATE TABLE  `facility_user_ids` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uid` bigint(20) DEFAULT NULL,
  `facility_id` bigint(20) DEFAULT NULL,
  `field_id`    varchar(31)  NOT NULL COMMENT 'references layout_options.field_id',
  `field_value` TEXT,
  PRIMARY KEY (`id`),
  KEY `uid` (`uid`,`facility_id`,`field_id`)
) ENGINE=InnoDB  AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `fee_sheet_options`
--

DROP TABLE IF EXISTS `fee_sheet_options`;
CREATE TABLE `fee_sheet_options` (
  `fs_category` varchar(63) default NULL,
  `fs_option` varchar(63) default NULL,
  `fs_codes` varchar(255) default NULL
) ENGINE=InnoDB;

--
-- Dumping data for table `fee_sheet_options`
--

INSERT INTO `fee_sheet_options` VALUES ('1New Patient', '1Brief', 'CPT4|99201|');
INSERT INTO `fee_sheet_options` VALUES ('1New Patient', '2Limited', 'CPT4|99202|');
INSERT INTO `fee_sheet_options` VALUES ('1New Patient', '3Detailed', 'CPT4|99203|');
INSERT INTO `fee_sheet_options` VALUES ('1New Patient', '4Extended', 'CPT4|99204|');
INSERT INTO `fee_sheet_options` VALUES ('1New Patient', '5Comprehensive', 'CPT4|99205|');
INSERT INTO `fee_sheet_options` VALUES ('2Established Patient', '1Brief', 'CPT4|99211|');
INSERT INTO `fee_sheet_options` VALUES ('2Established Patient', '2Limited', 'CPT4|99212|');
INSERT INTO `fee_sheet_options` VALUES ('2Established Patient', '3Detailed', 'CPT4|99213|');
INSERT INTO `fee_sheet_options` VALUES ('2Established Patient', '4Extended', 'CPT4|99214|');
INSERT INTO `fee_sheet_options` VALUES ('2Established Patient', '5Comprehensive', 'CPT4|99215|');

-- --------------------------------------------------------

--
-- Table structure for table `form_dictation`
--

DROP TABLE IF EXISTS `form_dictation`;
CREATE TABLE `form_dictation` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `pid` bigint(20) default NULL,
  `user` varchar(255) default NULL,
  `groupname` varchar(255) default NULL,
  `authorized` tinyint(4) default NULL,
  `activity` tinyint(4) default NULL,
  `dictation` longtext,
  `additional_notes` longtext,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `form_encounter`
--

DROP TABLE IF EXISTS `form_encounter`;
CREATE TABLE `form_encounter` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `reason` longtext,
  `facility` longtext,
  `facility_id` int(11) NOT NULL default '0',
  `pid` bigint(20) default NULL,
  `encounter` bigint(20) default NULL,
  `onset_date` datetime default NULL,
  `sensitivity` varchar(30) default NULL,
  `billing_note` text,
  `pc_catid` int(11) NOT NULL default '5' COMMENT 'event category from openemr_postcalendar_categories',
  `last_level_billed` int  NOT NULL DEFAULT 0 COMMENT '0=none, 1=ins1, 2=ins2, etc',
  `last_level_closed` int  NOT NULL DEFAULT 0 COMMENT '0=none, 1=ins1, 2=ins2, etc',
  `last_stmt_date`    date DEFAULT NULL,
  `stmt_count`        int  NOT NULL DEFAULT 0,
  `provider_id` INT(11) DEFAULT '0' COMMENT 'default and main provider for this visit',
  `supervisor_id` INT(11) DEFAULT '0' COMMENT 'supervising provider, if any, for this visit',
  `invoice_refno` varchar(31) NOT NULL DEFAULT '',
  `referral_source` varchar(31) NOT NULL DEFAULT '',
  `billing_facility` INT(11) NOT NULL DEFAULT 0,
  `external_id` VARCHAR(20) DEFAULT NULL,
  `pos_code` tinyint(4) default NULL,
  PRIMARY KEY  (`id`),
  KEY `pid_encounter` (`pid`, `encounter`),
  KEY `encounter_date` (`date`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `form_misc_billing_options`
--

DROP TABLE IF EXISTS `form_misc_billing_options`;
CREATE TABLE `form_misc_billing_options` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `pid` bigint(20) default NULL,
  `user` varchar(255) default NULL,
  `groupname` varchar(255) default NULL,
  `authorized` tinyint(4) default NULL,
  `activity` tinyint(4) default NULL,
  `employment_related` tinyint(1) default NULL,
  `auto_accident` tinyint(1) default NULL,
  `accident_state` varchar(2) default NULL,
  `other_accident` tinyint(1) default NULL,
  `medicaid_referral_code` varchar(2)   default NULL,
  `epsdt_flag` tinyint(1) default NULL,
  `provider_qualifier_code` varchar(2) default NULL,
  `provider_id` int(11) default NULL,
  `outside_lab` tinyint(1) default NULL,
  `lab_amount` decimal(5,2) default NULL,
  `is_unable_to_work` tinyint(1) default NULL,
  `date_initial_treatment` date default NULL,
  `off_work_from` date default NULL,
  `off_work_to` date default NULL,
  `is_hospitalized` tinyint(1) default NULL,
  `hospitalization_date_from` date default NULL,
  `hospitalization_date_to` date default NULL,
  `medicaid_resubmission_code` varchar(10) default NULL,
  `medicaid_original_reference` varchar(15) default NULL,
  `prior_auth_number` varchar(20) default NULL,
  `comments` varchar(255) default NULL,
  `replacement_claim` tinyint(1) default 0,
  `icn_resubmission_number` varchar(35) default NULL,
  `box_14_date_qual` char(3) default NULL,
  `box_15_date_qual` char(3) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `form_reviewofs`
--

DROP TABLE IF EXISTS `form_reviewofs`;
CREATE TABLE `form_reviewofs` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `pid` bigint(20) default NULL,
  `user` varchar(255) default NULL,
  `groupname` varchar(255) default NULL,
  `authorized` tinyint(4) default NULL,
  `activity` tinyint(4) default NULL,
  `fever` varchar(5) default NULL,
  `chills` varchar(5) default NULL,
  `night_sweats` varchar(5) default NULL,
  `weight_loss` varchar(5) default NULL,
  `poor_appetite` varchar(5) default NULL,
  `insomnia` varchar(5) default NULL,
  `fatigued` varchar(5) default NULL,
  `depressed` varchar(5) default NULL,
  `hyperactive` varchar(5) default NULL,
  `exposure_to_foreign_countries` varchar(5) default NULL,
  `cataracts` varchar(5) default NULL,
  `cataract_surgery` varchar(5) default NULL,
  `glaucoma` varchar(5) default NULL,
  `double_vision` varchar(5) default NULL,
  `blurred_vision` varchar(5) default NULL,
  `poor_hearing` varchar(5) default NULL,
  `headaches` varchar(5) default NULL,
  `ringing_in_ears` varchar(5) default NULL,
  `bloody_nose` varchar(5) default NULL,
  `sinusitis` varchar(5) default NULL,
  `sinus_surgery` varchar(5) default NULL,
  `dry_mouth` varchar(5) default NULL,
  `strep_throat` varchar(5) default NULL,
  `tonsillectomy` varchar(5) default NULL,
  `swollen_lymph_nodes` varchar(5) default NULL,
  `throat_cancer` varchar(5) default NULL,
  `throat_cancer_surgery` varchar(5) default NULL,
  `heart_attack` varchar(5) default NULL,
  `irregular_heart_beat` varchar(5) default NULL,
  `chest_pains` varchar(5) default NULL,
  `shortness_of_breath` varchar(5) default NULL,
  `high_blood_pressure` varchar(5) default NULL,
  `heart_failure` varchar(5) default NULL,
  `poor_circulation` varchar(5) default NULL,
  `vascular_surgery` varchar(5) default NULL,
  `cardiac_catheterization` varchar(5) default NULL,
  `coronary_artery_bypass` varchar(5) default NULL,
  `heart_transplant` varchar(5) default NULL,
  `stress_test` varchar(5) default NULL,
  `emphysema` varchar(5) default NULL,
  `chronic_bronchitis` varchar(5) default NULL,
  `interstitial_lung_disease` varchar(5) default NULL,
  `shortness_of_breath_2` varchar(5) default NULL,
  `lung_cancer` varchar(5) default NULL,
  `lung_cancer_surgery` varchar(5) default NULL,
  `pheumothorax` varchar(5) default NULL,
  `stomach_pains` varchar(5) default NULL,
  `peptic_ulcer_disease` varchar(5) default NULL,
  `gastritis` varchar(5) default NULL,
  `endoscopy` varchar(5) default NULL,
  `polyps` varchar(5) default NULL,
  `colonoscopy` varchar(5) default NULL,
  `colon_cancer` varchar(5) default NULL,
  `colon_cancer_surgery` varchar(5) default NULL,
  `ulcerative_colitis` varchar(5) default NULL,
  `crohns_disease` varchar(5) default NULL,
  `appendectomy` varchar(5) default NULL,
  `divirticulitis` varchar(5) default NULL,
  `divirticulitis_surgery` varchar(5) default NULL,
  `gall_stones` varchar(5) default NULL,
  `cholecystectomy` varchar(5) default NULL,
  `hepatitis` varchar(5) default NULL,
  `cirrhosis_of_the_liver` varchar(5) default NULL,
  `splenectomy` varchar(5) default NULL,
  `kidney_failure` varchar(5) default NULL,
  `kidney_stones` varchar(5) default NULL,
  `kidney_cancer` varchar(5) default NULL,
  `kidney_infections` varchar(5) default NULL,
  `bladder_infections` varchar(5) default NULL,
  `bladder_cancer` varchar(5) default NULL,
  `prostate_problems` varchar(5) default NULL,
  `prostate_cancer` varchar(5) default NULL,
  `kidney_transplant` varchar(5) default NULL,
  `sexually_transmitted_disease` varchar(5) default NULL,
  `burning_with_urination` varchar(5) default NULL,
  `discharge_from_urethra` varchar(5) default NULL,
  `rashes` varchar(5) default NULL,
  `infections` varchar(5) default NULL,
  `ulcerations` varchar(5) default NULL,
  `pemphigus` varchar(5) default NULL,
  `herpes` varchar(5) default NULL,
  `osetoarthritis` varchar(5) default NULL,
  `rheumotoid_arthritis` varchar(5) default NULL,
  `lupus` varchar(5) default NULL,
  `ankylosing_sondlilitis` varchar(5) default NULL,
  `swollen_joints` varchar(5) default NULL,
  `stiff_joints` varchar(5) default NULL,
  `broken_bones` varchar(5) default NULL,
  `neck_problems` varchar(5) default NULL,
  `back_problems` varchar(5) default NULL,
  `back_surgery` varchar(5) default NULL,
  `scoliosis` varchar(5) default NULL,
  `herniated_disc` varchar(5) default NULL,
  `shoulder_problems` varchar(5) default NULL,
  `elbow_problems` varchar(5) default NULL,
  `wrist_problems` varchar(5) default NULL,
  `hand_problems` varchar(5) default NULL,
  `hip_problems` varchar(5) default NULL,
  `knee_problems` varchar(5) default NULL,
  `ankle_problems` varchar(5) default NULL,
  `foot_problems` varchar(5) default NULL,
  `insulin_dependent_diabetes` varchar(5) default NULL,
  `noninsulin_dependent_diabetes` varchar(5) default NULL,
  `hypothyroidism` varchar(5) default NULL,
  `hyperthyroidism` varchar(5) default NULL,
  `cushing_syndrom` varchar(5) default NULL,
  `addison_syndrom` varchar(5) default NULL,
  `additional_notes` longtext,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `form_ros`
--

DROP TABLE IF EXISTS `form_ros`;
CREATE TABLE `form_ros` (
  `id` int(11) NOT NULL auto_increment,
  `pid` int(11) NOT NULL,
  `activity` int(11) NOT NULL default '1',
  `date` datetime default NULL,
  `weight_change` varchar(3) default NULL,
  `weakness` varchar(3) default NULL,
  `fatigue` varchar(3) default NULL,
  `anorexia` varchar(3) default NULL,
  `fever` varchar(3) default NULL,
  `chills` varchar(3) default NULL,
  `night_sweats` varchar(3) default NULL,
  `insomnia` varchar(3) default NULL,
  `irritability` varchar(3) default NULL,
  `heat_or_cold` varchar(3) default NULL,
  `intolerance` varchar(3) default NULL,
  `change_in_vision` varchar(3) default NULL,
  `glaucoma_history` varchar(3) default NULL,
  `eye_pain` varchar(3) default NULL,
  `irritation` varchar(3) default NULL,
  `redness` varchar(3) default NULL,
  `excessive_tearing` varchar(3) default NULL,
  `double_vision` varchar(3) default NULL,
  `blind_spots` varchar(3) default NULL,
  `photophobia` varchar(3) default NULL,
  `hearing_loss` varchar(3) default NULL,
  `discharge` varchar(3) default NULL,
  `pain` varchar(3) default NULL,
  `vertigo` varchar(3) default NULL,
  `tinnitus` varchar(3) default NULL,
  `frequent_colds` varchar(3) default NULL,
  `sore_throat` varchar(3) default NULL,
  `sinus_problems` varchar(3) default NULL,
  `post_nasal_drip` varchar(3) default NULL,
  `nosebleed` varchar(3) default NULL,
  `snoring` varchar(3) default NULL,
  `apnea` varchar(3) default NULL,
  `breast_mass` varchar(3) default NULL,
  `breast_discharge` varchar(3) default NULL,
  `biopsy` varchar(3) default NULL,
  `abnormal_mammogram` varchar(3) default NULL,
  `cough` varchar(3) default NULL,
  `sputum` varchar(3) default NULL,
  `shortness_of_breath` varchar(3) default NULL,
  `wheezing` varchar(3) default NULL,
  `hemoptsyis` varchar(3) default NULL,
  `asthma` varchar(3) default NULL,
  `copd` varchar(3) default NULL,
  `chest_pain` varchar(3) default NULL,
  `palpitation` varchar(3) default NULL,
  `syncope` varchar(3) default NULL,
  `pnd` varchar(3) default NULL,
  `doe` varchar(3) default NULL,
  `orthopnea` varchar(3) default NULL,
  `peripheal` varchar(3) default NULL,
  `edema` varchar(3) default NULL,
  `legpain_cramping` varchar(3) default NULL,
  `history_murmur` varchar(3) default NULL,
  `arrythmia` varchar(3) default NULL,
  `heart_problem` varchar(3) default NULL,
  `dysphagia` varchar(3) default NULL,
  `heartburn` varchar(3) default NULL,
  `bloating` varchar(3) default NULL,
  `belching` varchar(3) default NULL,
  `flatulence` varchar(3) default NULL,
  `nausea` varchar(3) default NULL,
  `vomiting` varchar(3) default NULL,
  `hematemesis` varchar(3) default NULL,
  `gastro_pain` varchar(3) default NULL,
  `food_intolerance` varchar(3) default NULL,
  `hepatitis` varchar(3) default NULL,
  `jaundice` varchar(3) default NULL,
  `hematochezia` varchar(3) default NULL,
  `changed_bowel` varchar(3) default NULL,
  `diarrhea` varchar(3) default NULL,
  `constipation` varchar(3) default NULL,
  `polyuria` varchar(3) default NULL,
  `polydypsia` varchar(3) default NULL,
  `dysuria` varchar(3) default NULL,
  `hematuria` varchar(3) default NULL,
  `frequency` varchar(3) default NULL,
  `urgency` varchar(3) default NULL,
  `incontinence` varchar(3) default NULL,
  `renal_stones` varchar(3) default NULL,
  `utis` varchar(3) default NULL,
  `hesitancy` varchar(3) default NULL,
  `dribbling` varchar(3) default NULL,
  `stream` varchar(3) default NULL,
  `nocturia` varchar(3) default NULL,
  `erections` varchar(3) default NULL,
  `ejaculations` varchar(3) default NULL,
  `g` varchar(3) default NULL,
  `p` varchar(3) default NULL,
  `ap` varchar(3) default NULL,
  `lc` varchar(3) default NULL,
  `mearche` varchar(3) default NULL,
  `menopause` varchar(3) default NULL,
  `lmp` varchar(3) default NULL,
  `f_frequency` varchar(3) default NULL,
  `f_flow` varchar(3) default NULL,
  `f_symptoms` varchar(3) default NULL,
  `abnormal_hair_growth` varchar(3) default NULL,
  `f_hirsutism` varchar(3) default NULL,
  `joint_pain` varchar(3) default NULL,
  `swelling` varchar(3) default NULL,
  `m_redness` varchar(3) default NULL,
  `m_warm` varchar(3) default NULL,
  `m_stiffness` varchar(3) default NULL,
  `muscle` varchar(3) default NULL,
  `m_aches` varchar(3) default NULL,
  `fms` varchar(3) default NULL,
  `arthritis` varchar(3) default NULL,
  `loc` varchar(3) default NULL,
  `seizures` varchar(3) default NULL,
  `stroke` varchar(3) default NULL,
  `tia` varchar(3) default NULL,
  `n_numbness` varchar(3) default NULL,
  `n_weakness` varchar(3) default NULL,
  `paralysis` varchar(3) default NULL,
  `intellectual_decline` varchar(3) default NULL,
  `memory_problems` varchar(3) default NULL,
  `dementia` varchar(3) default NULL,
  `n_headache` varchar(3) default NULL,
  `s_cancer` varchar(3) default NULL,
  `psoriasis` varchar(3) default NULL,
  `s_acne` varchar(3) default NULL,
  `s_other` varchar(3) default NULL,
  `s_disease` varchar(3) default NULL,
  `p_diagnosis` varchar(3) default NULL,
  `p_medication` varchar(3) default NULL,
  `depression` varchar(3) default NULL,
  `anxiety` varchar(3) default NULL,
  `social_difficulties` varchar(3) default NULL,
  `thyroid_problems` varchar(3) default NULL,
  `diabetes` varchar(3) default NULL,
  `abnormal_blood` varchar(3) default NULL,
  `anemia` varchar(3) default NULL,
  `fh_blood_problems` varchar(3) default NULL,
  `bleeding_problems` varchar(3) default NULL,
  `allergies` varchar(3) default NULL,
  `frequent_illness` varchar(3) default NULL,
  `hiv` varchar(3) default NULL,
  `hai_status` varchar(3) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `form_soap`
--

DROP TABLE IF EXISTS `form_soap`;
CREATE TABLE `form_soap` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `pid` bigint(20) default '0',
  `user` varchar(255) default NULL,
  `groupname` varchar(255) default NULL,
  `authorized` tinyint(4) default '0',
  `activity` tinyint(4) default '0',
  `subjective` text,
  `objective` text,
  `assessment` text,
  `plan` text,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `form_vitals`
--

DROP TABLE IF EXISTS `form_vitals`;
CREATE TABLE `form_vitals` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `pid` bigint(20) default '0',
  `user` varchar(255) default NULL,
  `groupname` varchar(255) default NULL,
  `authorized` tinyint(4) default '0',
  `activity` tinyint(4) default '0',
  `bps` varchar(40) default NULL,
  `bpd` varchar(40) default NULL,
  `weight` float(5,2) default '0.00',
  `height` float(5,2) default '0.00',
  `temperature` float(5,2) default '0.00',
  `temp_method` varchar(255) default NULL,
  `pulse` float(5,2) default '0.00',
  `respiration` float(5,2) default '0.00',
  `note` varchar(255) default NULL,
  `BMI` float(4,1) default '0.0',
  `BMI_status` varchar(255) default NULL,
  `waist_circ` float(5,2) default '0.00',
  `head_circ` float(4,2) default '0.00',
  `oxygen_saturation` float(5,2) default '0.00',
  `external_id` VARCHAR(20) DEFAULT NULL,
  PRIMARY KEY  (`id`),
  KEY `pid` (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `forms`
--

DROP TABLE IF EXISTS `forms`;
CREATE TABLE `forms` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `encounter` bigint(20) default NULL,
  `form_name` longtext,
  `form_id` bigint(20) default NULL,
  `pid` bigint(20) default NULL,
  `user` varchar(255) default NULL,
  `groupname` varchar(255) default NULL,
  `authorized` tinyint(4) default NULL,
  `deleted` tinyint(4) DEFAULT '0' NOT NULL COMMENT 'flag indicates form has been deleted',
  `formdir` longtext,
  PRIMARY KEY  (`id`),
  KEY `pid_encounter` (`pid`, `encounter`),
  KEY `form_id` (`form_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `geo_country_reference`
--

DROP TABLE IF EXISTS `geo_country_reference`;
CREATE TABLE `geo_country_reference` (
  `countries_id` int(5) NOT NULL auto_increment,
  `countries_name` varchar(64) default NULL,
  `countries_iso_code_2` char(2) NOT NULL default '',
  `countries_iso_code_3` char(3) NOT NULL default '',
  PRIMARY KEY  (`countries_id`),
  KEY `IDX_COUNTRIES_NAME` (`countries_name`)
) ENGINE=InnoDB AUTO_INCREMENT=240 ;

--
-- Dumping data for table `geo_country_reference`
--

INSERT INTO `geo_country_reference` VALUES (1, 'Afghanistan', 'AF', 'AFG');
INSERT INTO `geo_country_reference` VALUES (2, 'Albania', 'AL', 'ALB');
INSERT INTO `geo_country_reference` VALUES (3, 'Algeria', 'DZ', 'DZA');
INSERT INTO `geo_country_reference` VALUES (4, 'American Samoa', 'AS', 'ASM');
INSERT INTO `geo_country_reference` VALUES (5, 'Andorra', 'AD', 'AND');
INSERT INTO `geo_country_reference` VALUES (6, 'Angola', 'AO', 'AGO');
INSERT INTO `geo_country_reference` VALUES (7, 'Anguilla', 'AI', 'AIA');
INSERT INTO `geo_country_reference` VALUES (8, 'Antarctica', 'AQ', 'ATA');
INSERT INTO `geo_country_reference` VALUES (9, 'Antigua and Barbuda', 'AG', 'ATG');
INSERT INTO `geo_country_reference` VALUES (10, 'Argentina', 'AR', 'ARG');
INSERT INTO `geo_country_reference` VALUES (11, 'Armenia', 'AM', 'ARM');
INSERT INTO `geo_country_reference` VALUES (12, 'Aruba', 'AW', 'ABW');
INSERT INTO `geo_country_reference` VALUES (13, 'Australia', 'AU', 'AUS');
INSERT INTO `geo_country_reference` VALUES (14, 'Austria', 'AT', 'AUT');
INSERT INTO `geo_country_reference` VALUES (15, 'Azerbaijan', 'AZ', 'AZE');
INSERT INTO `geo_country_reference` VALUES (16, 'Bahamas', 'BS', 'BHS');
INSERT INTO `geo_country_reference` VALUES (17, 'Bahrain', 'BH', 'BHR');
INSERT INTO `geo_country_reference` VALUES (18, 'Bangladesh', 'BD', 'BGD');
INSERT INTO `geo_country_reference` VALUES (19, 'Barbados', 'BB', 'BRB');
INSERT INTO `geo_country_reference` VALUES (20, 'Belarus', 'BY', 'BLR');
INSERT INTO `geo_country_reference` VALUES (21, 'Belgium', 'BE', 'BEL');
INSERT INTO `geo_country_reference` VALUES (22, 'Belize', 'BZ', 'BLZ');
INSERT INTO `geo_country_reference` VALUES (23, 'Benin', 'BJ', 'BEN');
INSERT INTO `geo_country_reference` VALUES (24, 'Bermuda', 'BM', 'BMU');
INSERT INTO `geo_country_reference` VALUES (25, 'Bhutan', 'BT', 'BTN');
INSERT INTO `geo_country_reference` VALUES (26, 'Bolivia', 'BO', 'BOL');
INSERT INTO `geo_country_reference` VALUES (27, 'Bosnia and Herzegowina', 'BA', 'BIH');
INSERT INTO `geo_country_reference` VALUES (28, 'Botswana', 'BW', 'BWA');
INSERT INTO `geo_country_reference` VALUES (29, 'Bouvet Island', 'BV', 'BVT');
INSERT INTO `geo_country_reference` VALUES (30, 'Brazil', 'BR', 'BRA');
INSERT INTO `geo_country_reference` VALUES (31, 'British Indian Ocean Territory', 'IO', 'IOT');
INSERT INTO `geo_country_reference` VALUES (32, 'Brunei Darussalam', 'BN', 'BRN');
INSERT INTO `geo_country_reference` VALUES (33, 'Bulgaria', 'BG', 'BGR');
INSERT INTO `geo_country_reference` VALUES (34, 'Burkina Faso', 'BF', 'BFA');
INSERT INTO `geo_country_reference` VALUES (35, 'Burundi', 'BI', 'BDI');
INSERT INTO `geo_country_reference` VALUES (36, 'Cambodia', 'KH', 'KHM');
INSERT INTO `geo_country_reference` VALUES (37, 'Cameroon', 'CM', 'CMR');
INSERT INTO `geo_country_reference` VALUES (38, 'Canada', 'CA', 'CAN');
INSERT INTO `geo_country_reference` VALUES (39, 'Cape Verde', 'CV', 'CPV');
INSERT INTO `geo_country_reference` VALUES (40, 'Cayman Islands', 'KY', 'CYM');
INSERT INTO `geo_country_reference` VALUES (41, 'Central African Republic', 'CF', 'CAF');
INSERT INTO `geo_country_reference` VALUES (42, 'Chad', 'TD', 'TCD');
INSERT INTO `geo_country_reference` VALUES (43, 'Chile', 'CL', 'CHL');
INSERT INTO `geo_country_reference` VALUES (44, 'China', 'CN', 'CHN');
INSERT INTO `geo_country_reference` VALUES (45, 'Christmas Island', 'CX', 'CXR');
INSERT INTO `geo_country_reference` VALUES (46, 'Cocos (Keeling) Islands', 'CC', 'CCK');
INSERT INTO `geo_country_reference` VALUES (47, 'Colombia', 'CO', 'COL');
INSERT INTO `geo_country_reference` VALUES (48, 'Comoros', 'KM', 'COM');
INSERT INTO `geo_country_reference` VALUES (49, 'Congo', 'CG', 'COG');
INSERT INTO `geo_country_reference` VALUES (50, 'Cook Islands', 'CK', 'COK');
INSERT INTO `geo_country_reference` VALUES (51, 'Costa Rica', 'CR', 'CRI');
INSERT INTO `geo_country_reference` VALUES (52, 'Cote D Ivoire', 'CI', 'CIV');
INSERT INTO `geo_country_reference` VALUES (53, 'Croatia', 'HR', 'HRV');
INSERT INTO `geo_country_reference` VALUES (54, 'Cuba', 'CU', 'CUB');
INSERT INTO `geo_country_reference` VALUES (55, 'Cyprus', 'CY', 'CYP');
INSERT INTO `geo_country_reference` VALUES (56, 'Czech Republic', 'CZ', 'CZE');
INSERT INTO `geo_country_reference` VALUES (57, 'Denmark', 'DK', 'DNK');
INSERT INTO `geo_country_reference` VALUES (58, 'Djibouti', 'DJ', 'DJI');
INSERT INTO `geo_country_reference` VALUES (59, 'Dominica', 'DM', 'DMA');
INSERT INTO `geo_country_reference` VALUES (60, 'Dominican Republic', 'DO', 'DOM');
INSERT INTO `geo_country_reference` VALUES (61, 'East Timor', 'TP', 'TMP');
INSERT INTO `geo_country_reference` VALUES (62, 'Ecuador', 'EC', 'ECU');
INSERT INTO `geo_country_reference` VALUES (63, 'Egypt', 'EG', 'EGY');
INSERT INTO `geo_country_reference` VALUES (64, 'El Salvador', 'SV', 'SLV');
INSERT INTO `geo_country_reference` VALUES (65, 'Equatorial Guinea', 'GQ', 'GNQ');
INSERT INTO `geo_country_reference` VALUES (66, 'Eritrea', 'ER', 'ERI');
INSERT INTO `geo_country_reference` VALUES (67, 'Estonia', 'EE', 'EST');
INSERT INTO `geo_country_reference` VALUES (68, 'Ethiopia', 'ET', 'ETH');
INSERT INTO `geo_country_reference` VALUES (69, 'Falkland Islands (Malvinas)', 'FK', 'FLK');
INSERT INTO `geo_country_reference` VALUES (70, 'Faroe Islands', 'FO', 'FRO');
INSERT INTO `geo_country_reference` VALUES (71, 'Fiji', 'FJ', 'FJI');
INSERT INTO `geo_country_reference` VALUES (72, 'Finland', 'FI', 'FIN');
INSERT INTO `geo_country_reference` VALUES (73, 'France', 'FR', 'FRA');
INSERT INTO `geo_country_reference` VALUES (74, 'France, MEtropolitan', 'FX', 'FXX');
INSERT INTO `geo_country_reference` VALUES (75, 'French Guiana', 'GF', 'GUF');
INSERT INTO `geo_country_reference` VALUES (76, 'French Polynesia', 'PF', 'PYF');
INSERT INTO `geo_country_reference` VALUES (77, 'French Southern Territories', 'TF', 'ATF');
INSERT INTO `geo_country_reference` VALUES (78, 'Gabon', 'GA', 'GAB');
INSERT INTO `geo_country_reference` VALUES (79, 'Gambia', 'GM', 'GMB');
INSERT INTO `geo_country_reference` VALUES (80, 'Georgia', 'GE', 'GEO');
INSERT INTO `geo_country_reference` VALUES (81, 'Germany', 'DE', 'DEU');
INSERT INTO `geo_country_reference` VALUES (82, 'Ghana', 'GH', 'GHA');
INSERT INTO `geo_country_reference` VALUES (83, 'Gibraltar', 'GI', 'GIB');
INSERT INTO `geo_country_reference` VALUES (84, 'Greece', 'GR', 'GRC');
INSERT INTO `geo_country_reference` VALUES (85, 'Greenland', 'GL', 'GRL');
INSERT INTO `geo_country_reference` VALUES (86, 'Grenada', 'GD', 'GRD');
INSERT INTO `geo_country_reference` VALUES (87, 'Guadeloupe', 'GP', 'GLP');
INSERT INTO `geo_country_reference` VALUES (88, 'Guam', 'GU', 'GUM');
INSERT INTO `geo_country_reference` VALUES (89, 'Guatemala', 'GT', 'GTM');
INSERT INTO `geo_country_reference` VALUES (90, 'Guinea', 'GN', 'GIN');
INSERT INTO `geo_country_reference` VALUES (91, 'Guinea-bissau', 'GW', 'GNB');
INSERT INTO `geo_country_reference` VALUES (92, 'Guyana', 'GY', 'GUY');
INSERT INTO `geo_country_reference` VALUES (93, 'Haiti', 'HT', 'HTI');
INSERT INTO `geo_country_reference` VALUES (94, 'Heard and Mc Donald Islands', 'HM', 'HMD');
INSERT INTO `geo_country_reference` VALUES (95, 'Honduras', 'HN', 'HND');
INSERT INTO `geo_country_reference` VALUES (96, 'Hong Kong', 'HK', 'HKG');
INSERT INTO `geo_country_reference` VALUES (97, 'Hungary', 'HU', 'HUN');
INSERT INTO `geo_country_reference` VALUES (98, 'Iceland', 'IS', 'ISL');
INSERT INTO `geo_country_reference` VALUES (99, 'India', 'IN', 'IND');
INSERT INTO `geo_country_reference` VALUES (100, 'Indonesia', 'ID', 'IDN');
INSERT INTO `geo_country_reference` VALUES (101, 'Iran (Islamic Republic of)', 'IR', 'IRN');
INSERT INTO `geo_country_reference` VALUES (102, 'Iraq', 'IQ', 'IRQ');
INSERT INTO `geo_country_reference` VALUES (103, 'Ireland', 'IE', 'IRL');
INSERT INTO `geo_country_reference` VALUES (104, 'Israel', 'IL', 'ISR');
INSERT INTO `geo_country_reference` VALUES (105, 'Italy', 'IT', 'ITA');
INSERT INTO `geo_country_reference` VALUES (106, 'Jamaica', 'JM', 'JAM');
INSERT INTO `geo_country_reference` VALUES (107, 'Japan', 'JP', 'JPN');
INSERT INTO `geo_country_reference` VALUES (108, 'Jordan', 'JO', 'JOR');
INSERT INTO `geo_country_reference` VALUES (109, 'Kazakhstan', 'KZ', 'KAZ');
INSERT INTO `geo_country_reference` VALUES (110, 'Kenya', 'KE', 'KEN');
INSERT INTO `geo_country_reference` VALUES (111, 'Kiribati', 'KI', 'KIR');
INSERT INTO `geo_country_reference` VALUES (112, 'Korea, Democratic Peoples Republic of', 'KP', 'PRK');
INSERT INTO `geo_country_reference` VALUES (113, 'Korea, Republic of', 'KR', 'KOR');
INSERT INTO `geo_country_reference` VALUES (114, 'Kuwait', 'KW', 'KWT');
INSERT INTO `geo_country_reference` VALUES (115, 'Kyrgyzstan', 'KG', 'KGZ');
INSERT INTO `geo_country_reference` VALUES (116, 'Lao Peoples Democratic Republic', 'LA', 'LAO');
INSERT INTO `geo_country_reference` VALUES (117, 'Latvia', 'LV', 'LVA');
INSERT INTO `geo_country_reference` VALUES (118, 'Lebanon', 'LB', 'LBN');
INSERT INTO `geo_country_reference` VALUES (119, 'Lesotho', 'LS', 'LSO');
INSERT INTO `geo_country_reference` VALUES (120, 'Liberia', 'LR', 'LBR');
INSERT INTO `geo_country_reference` VALUES (121, 'Libyan Arab Jamahiriya', 'LY', 'LBY');
INSERT INTO `geo_country_reference` VALUES (122, 'Liechtenstein', 'LI', 'LIE');
INSERT INTO `geo_country_reference` VALUES (123, 'Lithuania', 'LT', 'LTU');
INSERT INTO `geo_country_reference` VALUES (124, 'Luxembourg', 'LU', 'LUX');
INSERT INTO `geo_country_reference` VALUES (125, 'Macau', 'MO', 'MAC');
INSERT INTO `geo_country_reference` VALUES (126, 'Macedonia, The Former Yugoslav Republic of', 'MK', 'MKD');
INSERT INTO `geo_country_reference` VALUES (127, 'Madagascar', 'MG', 'MDG');
INSERT INTO `geo_country_reference` VALUES (128, 'Malawi', 'MW', 'MWI');
INSERT INTO `geo_country_reference` VALUES (129, 'Malaysia', 'MY', 'MYS');
INSERT INTO `geo_country_reference` VALUES (130, 'Maldives', 'MV', 'MDV');
INSERT INTO `geo_country_reference` VALUES (131, 'Mali', 'ML', 'MLI');
INSERT INTO `geo_country_reference` VALUES (132, 'Malta', 'MT', 'MLT');
INSERT INTO `geo_country_reference` VALUES (133, 'Marshall Islands', 'MH', 'MHL');
INSERT INTO `geo_country_reference` VALUES (134, 'Martinique', 'MQ', 'MTQ');
INSERT INTO `geo_country_reference` VALUES (135, 'Mauritania', 'MR', 'MRT');
INSERT INTO `geo_country_reference` VALUES (136, 'Mauritius', 'MU', 'MUS');
INSERT INTO `geo_country_reference` VALUES (137, 'Mayotte', 'YT', 'MYT');
INSERT INTO `geo_country_reference` VALUES (138, 'Mexico', 'MX', 'MEX');
INSERT INTO `geo_country_reference` VALUES (139, 'Micronesia, Federated States of', 'FM', 'FSM');
INSERT INTO `geo_country_reference` VALUES (140, 'Moldova, Republic of', 'MD', 'MDA');
INSERT INTO `geo_country_reference` VALUES (141, 'Monaco', 'MC', 'MCO');
INSERT INTO `geo_country_reference` VALUES (142, 'Mongolia', 'MN', 'MNG');
INSERT INTO `geo_country_reference` VALUES (143, 'Montserrat', 'MS', 'MSR');
INSERT INTO `geo_country_reference` VALUES (144, 'Morocco', 'MA', 'MAR');
INSERT INTO `geo_country_reference` VALUES (145, 'Mozambique', 'MZ', 'MOZ');
INSERT INTO `geo_country_reference` VALUES (146, 'Myanmar', 'MM', 'MMR');
INSERT INTO `geo_country_reference` VALUES (147, 'Namibia', 'NA', 'NAM');
INSERT INTO `geo_country_reference` VALUES (148, 'Nauru', 'NR', 'NRU');
INSERT INTO `geo_country_reference` VALUES (149, 'Nepal', 'NP', 'NPL');
INSERT INTO `geo_country_reference` VALUES (150, 'Netherlands', 'NL', 'NLD');
INSERT INTO `geo_country_reference` VALUES (151, 'Netherlands Antilles', 'AN', 'ANT');
INSERT INTO `geo_country_reference` VALUES (152, 'New Caledonia', 'NC', 'NCL');
INSERT INTO `geo_country_reference` VALUES (153, 'New Zealand', 'NZ', 'NZL');
INSERT INTO `geo_country_reference` VALUES (154, 'Nicaragua', 'NI', 'NIC');
INSERT INTO `geo_country_reference` VALUES (155, 'Niger', 'NE', 'NER');
INSERT INTO `geo_country_reference` VALUES (156, 'Nigeria', 'NG', 'NGA');
INSERT INTO `geo_country_reference` VALUES (157, 'Niue', 'NU', 'NIU');
INSERT INTO `geo_country_reference` VALUES (158, 'Norfolk Island', 'NF', 'NFK');
INSERT INTO `geo_country_reference` VALUES (159, 'Northern Mariana Islands', 'MP', 'MNP');
INSERT INTO `geo_country_reference` VALUES (160, 'Norway', 'NO', 'NOR');
INSERT INTO `geo_country_reference` VALUES (161, 'Oman', 'OM', 'OMN');
INSERT INTO `geo_country_reference` VALUES (162, 'Pakistan', 'PK', 'PAK');
INSERT INTO `geo_country_reference` VALUES (163, 'Palau', 'PW', 'PLW');
INSERT INTO `geo_country_reference` VALUES (164, 'Panama', 'PA', 'PAN');
INSERT INTO `geo_country_reference` VALUES (165, 'Papua New Guinea', 'PG', 'PNG');
INSERT INTO `geo_country_reference` VALUES (166, 'Paraguay', 'PY', 'PRY');
INSERT INTO `geo_country_reference` VALUES (167, 'Peru', 'PE', 'PER');
INSERT INTO `geo_country_reference` VALUES (168, 'Philippines', 'PH', 'PHL');
INSERT INTO `geo_country_reference` VALUES (169, 'Pitcairn', 'PN', 'PCN');
INSERT INTO `geo_country_reference` VALUES (170, 'Poland', 'PL', 'POL');
INSERT INTO `geo_country_reference` VALUES (171, 'Portugal', 'PT', 'PRT');
INSERT INTO `geo_country_reference` VALUES (172, 'Puerto Rico', 'PR', 'PRI');
INSERT INTO `geo_country_reference` VALUES (173, 'Qatar', 'QA', 'QAT');
INSERT INTO `geo_country_reference` VALUES (174, 'Reunion', 'RE', 'REU');
INSERT INTO `geo_country_reference` VALUES (175, 'Romania', 'RO', 'ROM');
INSERT INTO `geo_country_reference` VALUES (176, 'Russian Federation', 'RU', 'RUS');
INSERT INTO `geo_country_reference` VALUES (177, 'Rwanda', 'RW', 'RWA');
INSERT INTO `geo_country_reference` VALUES (178, 'Saint Kitts and Nevis', 'KN', 'KNA');
INSERT INTO `geo_country_reference` VALUES (179, 'Saint Lucia', 'LC', 'LCA');
INSERT INTO `geo_country_reference` VALUES (180, 'Saint Vincent and the Grenadines', 'VC', 'VCT');
INSERT INTO `geo_country_reference` VALUES (181, 'Samoa', 'WS', 'WSM');
INSERT INTO `geo_country_reference` VALUES (182, 'San Marino', 'SM', 'SMR');
INSERT INTO `geo_country_reference` VALUES (183, 'Sao Tome and Principe', 'ST', 'STP');
INSERT INTO `geo_country_reference` VALUES (184, 'Saudi Arabia', 'SA', 'SAU');
INSERT INTO `geo_country_reference` VALUES (185, 'Senegal', 'SN', 'SEN');
INSERT INTO `geo_country_reference` VALUES (186, 'Seychelles', 'SC', 'SYC');
INSERT INTO `geo_country_reference` VALUES (187, 'Sierra Leone', 'SL', 'SLE');
INSERT INTO `geo_country_reference` VALUES (188, 'Singapore', 'SG', 'SGP');
INSERT INTO `geo_country_reference` VALUES (189, 'Slovakia (Slovak Republic)', 'SK', 'SVK');
INSERT INTO `geo_country_reference` VALUES (190, 'Slovenia', 'SI', 'SVN');
INSERT INTO `geo_country_reference` VALUES (191, 'Solomon Islands', 'SB', 'SLB');
INSERT INTO `geo_country_reference` VALUES (192, 'Somalia', 'SO', 'SOM');
INSERT INTO `geo_country_reference` VALUES (193, 'south Africa', 'ZA', 'ZAF');
INSERT INTO `geo_country_reference` VALUES (194, 'South Georgia and the South Sandwich Islands', 'GS', 'SGS');
INSERT INTO `geo_country_reference` VALUES (195, 'Spain', 'ES', 'ESP');
INSERT INTO `geo_country_reference` VALUES (196, 'Sri Lanka', 'LK', 'LKA');
INSERT INTO `geo_country_reference` VALUES (197, 'St. Helena', 'SH', 'SHN');
INSERT INTO `geo_country_reference` VALUES (198, 'St. Pierre and Miquelon', 'PM', 'SPM');
INSERT INTO `geo_country_reference` VALUES (199, 'Sudan', 'SD', 'SDN');
INSERT INTO `geo_country_reference` VALUES (200, 'Suriname', 'SR', 'SUR');
INSERT INTO `geo_country_reference` VALUES (201, 'Svalbard and Jan Mayen Islands', 'SJ', 'SJM');
INSERT INTO `geo_country_reference` VALUES (202, 'Swaziland', 'SZ', 'SWZ');
INSERT INTO `geo_country_reference` VALUES (203, 'Sweden', 'SE', 'SWE');
INSERT INTO `geo_country_reference` VALUES (204, 'Switzerland', 'CH', 'CHE');
INSERT INTO `geo_country_reference` VALUES (205, 'Syrian Arab Republic', 'SY', 'SYR');
INSERT INTO `geo_country_reference` VALUES (206, 'Taiwan, Province of China', 'TW', 'TWN');
INSERT INTO `geo_country_reference` VALUES (207, 'Tajikistan', 'TJ', 'TJK');
INSERT INTO `geo_country_reference` VALUES (208, 'Tanzania, United Republic of', 'TZ', 'TZA');
INSERT INTO `geo_country_reference` VALUES (209, 'Thailand', 'TH', 'THA');
INSERT INTO `geo_country_reference` VALUES (210, 'Togo', 'TG', 'TGO');
INSERT INTO `geo_country_reference` VALUES (211, 'Tokelau', 'TK', 'TKL');
INSERT INTO `geo_country_reference` VALUES (212, 'Tonga', 'TO', 'TON');
INSERT INTO `geo_country_reference` VALUES (213, 'Trinidad and Tobago', 'TT', 'TTO');
INSERT INTO `geo_country_reference` VALUES (214, 'Tunisia', 'TN', 'TUN');
INSERT INTO `geo_country_reference` VALUES (215, 'Turkey', 'TR', 'TUR');
INSERT INTO `geo_country_reference` VALUES (216, 'Turkmenistan', 'TM', 'TKM');
INSERT INTO `geo_country_reference` VALUES (217, 'Turks and Caicos Islands', 'TC', 'TCA');
INSERT INTO `geo_country_reference` VALUES (218, 'Tuvalu', 'TV', 'TUV');
INSERT INTO `geo_country_reference` VALUES (219, 'Uganda', 'UG', 'UGA');
INSERT INTO `geo_country_reference` VALUES (220, 'Ukraine', 'UA', 'UKR');
INSERT INTO `geo_country_reference` VALUES (221, 'United Arab Emirates', 'AE', 'ARE');
INSERT INTO `geo_country_reference` VALUES (222, 'United Kingdom', 'GB', 'GBR');
INSERT INTO `geo_country_reference` VALUES (223, 'United States', 'US', 'USA');
INSERT INTO `geo_country_reference` VALUES (224, 'United States Minor Outlying Islands', 'UM', 'UMI');
INSERT INTO `geo_country_reference` VALUES (225, 'Uruguay', 'UY', 'URY');
INSERT INTO `geo_country_reference` VALUES (226, 'Uzbekistan', 'UZ', 'UZB');
INSERT INTO `geo_country_reference` VALUES (227, 'Vanuatu', 'VU', 'VUT');
INSERT INTO `geo_country_reference` VALUES (228, 'Vatican City State (Holy See)', 'VA', 'VAT');
INSERT INTO `geo_country_reference` VALUES (229, 'Venezuela', 'VE', 'VEN');
INSERT INTO `geo_country_reference` VALUES (230, 'Viet Nam', 'VN', 'VNM');
INSERT INTO `geo_country_reference` VALUES (231, 'Virgin Islands (British)', 'VG', 'VGB');
INSERT INTO `geo_country_reference` VALUES (232, 'Virgin Islands (U.S.)', 'VI', 'VIR');
INSERT INTO `geo_country_reference` VALUES (233, 'Wallis and Futuna Islands', 'WF', 'WLF');
INSERT INTO `geo_country_reference` VALUES (234, 'Western Sahara', 'EH', 'ESH');
INSERT INTO `geo_country_reference` VALUES (235, 'Yemen', 'YE', 'YEM');
INSERT INTO `geo_country_reference` VALUES (236, 'Yugoslavia', 'YU', 'YUG');
INSERT INTO `geo_country_reference` VALUES (237, 'Zaire', 'ZR', 'ZAR');
INSERT INTO `geo_country_reference` VALUES (238, 'Zambia', 'ZM', 'ZMB');
INSERT INTO `geo_country_reference` VALUES (239, 'Zimbabwe', 'ZW', 'ZWE');

-- --------------------------------------------------------

--
-- Table structure for table `geo_zone_reference`
--

DROP TABLE IF EXISTS `geo_zone_reference`;
CREATE TABLE `geo_zone_reference` (
  `zone_id` int(5) NOT NULL auto_increment,
  `zone_country_id` int(5) NOT NULL default '0',
  `zone_code` varchar(5) default NULL,
  `zone_name` varchar(32) default NULL,
  PRIMARY KEY  (`zone_id`)
) ENGINE=InnoDB AUTO_INCREMENT=83 ;

--
-- Dumping data for table `geo_zone_reference`
--

INSERT INTO `geo_zone_reference` VALUES (1, 223, 'AL', 'Alabama');
INSERT INTO `geo_zone_reference` VALUES (2, 223, 'AK', 'Alaska');
INSERT INTO `geo_zone_reference` VALUES (3, 223, 'AS', 'American Samoa');
INSERT INTO `geo_zone_reference` VALUES (4, 223, 'AZ', 'Arizona');
INSERT INTO `geo_zone_reference` VALUES (5, 223, 'AR', 'Arkansas');
INSERT INTO `geo_zone_reference` VALUES (6, 223, 'AF', 'Armed Forces Africa');
INSERT INTO `geo_zone_reference` VALUES (7, 223, 'AA', 'Armed Forces Americas');
INSERT INTO `geo_zone_reference` VALUES (8, 223, 'AC', 'Armed Forces Canada');
INSERT INTO `geo_zone_reference` VALUES (9, 223, 'AE', 'Armed Forces Europe');
INSERT INTO `geo_zone_reference` VALUES (10, 223, 'AM', 'Armed Forces Middle East');
INSERT INTO `geo_zone_reference` VALUES (11, 223, 'AP', 'Armed Forces Pacific');
INSERT INTO `geo_zone_reference` VALUES (12, 223, 'CA', 'California');
INSERT INTO `geo_zone_reference` VALUES (13, 223, 'CO', 'Colorado');
INSERT INTO `geo_zone_reference` VALUES (14, 223, 'CT', 'Connecticut');
INSERT INTO `geo_zone_reference` VALUES (15, 223, 'DE', 'Delaware');
INSERT INTO `geo_zone_reference` VALUES (16, 223, 'DC', 'District of Columbia');
INSERT INTO `geo_zone_reference` VALUES (17, 223, 'FM', 'Federated States Of Micronesia');
INSERT INTO `geo_zone_reference` VALUES (18, 223, 'FL', 'Florida');
INSERT INTO `geo_zone_reference` VALUES (19, 223, 'GA', 'Georgia');
INSERT INTO `geo_zone_reference` VALUES (20, 223, 'GU', 'Guam');
INSERT INTO `geo_zone_reference` VALUES (21, 223, 'HI', 'Hawaii');
INSERT INTO `geo_zone_reference` VALUES (22, 223, 'ID', 'Idaho');
INSERT INTO `geo_zone_reference` VALUES (23, 223, 'IL', 'Illinois');
INSERT INTO `geo_zone_reference` VALUES (24, 223, 'IN', 'Indiana');
INSERT INTO `geo_zone_reference` VALUES (25, 223, 'IA', 'Iowa');
INSERT INTO `geo_zone_reference` VALUES (26, 223, 'KS', 'Kansas');
INSERT INTO `geo_zone_reference` VALUES (27, 223, 'KY', 'Kentucky');
INSERT INTO `geo_zone_reference` VALUES (28, 223, 'LA', 'Louisiana');
INSERT INTO `geo_zone_reference` VALUES (29, 223, 'ME', 'Maine');
INSERT INTO `geo_zone_reference` VALUES (30, 223, 'MH', 'Marshall Islands');
INSERT INTO `geo_zone_reference` VALUES (31, 223, 'MD', 'Maryland');
INSERT INTO `geo_zone_reference` VALUES (32, 223, 'MA', 'Massachusetts');
INSERT INTO `geo_zone_reference` VALUES (33, 223, 'MI', 'Michigan');
INSERT INTO `geo_zone_reference` VALUES (34, 223, 'MN', 'Minnesota');
INSERT INTO `geo_zone_reference` VALUES (35, 223, 'MS', 'Mississippi');
INSERT INTO `geo_zone_reference` VALUES (36, 223, 'MO', 'Missouri');
INSERT INTO `geo_zone_reference` VALUES (37, 223, 'MT', 'Montana');
INSERT INTO `geo_zone_reference` VALUES (38, 223, 'NE', 'Nebraska');
INSERT INTO `geo_zone_reference` VALUES (39, 223, 'NV', 'Nevada');
INSERT INTO `geo_zone_reference` VALUES (40, 223, 'NH', 'New Hampshire');
INSERT INTO `geo_zone_reference` VALUES (41, 223, 'NJ', 'New Jersey');
INSERT INTO `geo_zone_reference` VALUES (42, 223, 'NM', 'New Mexico');
INSERT INTO `geo_zone_reference` VALUES (43, 223, 'NY', 'New York');
INSERT INTO `geo_zone_reference` VALUES (44, 223, 'NC', 'North Carolina');
INSERT INTO `geo_zone_reference` VALUES (45, 223, 'ND', 'North Dakota');
INSERT INTO `geo_zone_reference` VALUES (46, 223, 'MP', 'Northern Mariana Islands');
INSERT INTO `geo_zone_reference` VALUES (47, 223, 'OH', 'Ohio');
INSERT INTO `geo_zone_reference` VALUES (48, 223, 'OK', 'Oklahoma');
INSERT INTO `geo_zone_reference` VALUES (49, 223, 'OR', 'Oregon');
INSERT INTO `geo_zone_reference` VALUES (50, 223, 'PW', 'Palau');
INSERT INTO `geo_zone_reference` VALUES (51, 223, 'PA', 'Pennsylvania');
INSERT INTO `geo_zone_reference` VALUES (52, 223, 'PR', 'Puerto Rico');
INSERT INTO `geo_zone_reference` VALUES (53, 223, 'RI', 'Rhode Island');
INSERT INTO `geo_zone_reference` VALUES (54, 223, 'SC', 'South Carolina');
INSERT INTO `geo_zone_reference` VALUES (55, 223, 'SD', 'South Dakota');
INSERT INTO `geo_zone_reference` VALUES (56, 223, 'TN', 'Tenessee');
INSERT INTO `geo_zone_reference` VALUES (57, 223, 'TX', 'Texas');
INSERT INTO `geo_zone_reference` VALUES (58, 223, 'UT', 'Utah');
INSERT INTO `geo_zone_reference` VALUES (59, 223, 'VT', 'Vermont');
INSERT INTO `geo_zone_reference` VALUES (60, 223, 'VI', 'Virgin Islands');
INSERT INTO `geo_zone_reference` VALUES (61, 223, 'VA', 'Virginia');
INSERT INTO `geo_zone_reference` VALUES (62, 223, 'WA', 'Washington');
INSERT INTO `geo_zone_reference` VALUES (63, 223, 'WV', 'West Virginia');
INSERT INTO `geo_zone_reference` VALUES (64, 223, 'WI', 'Wisconsin');
INSERT INTO `geo_zone_reference` VALUES (65, 223, 'WY', 'Wyoming');
INSERT INTO `geo_zone_reference` VALUES (66, 38, 'AB', 'Alberta');
INSERT INTO `geo_zone_reference` VALUES (67, 38, 'BC', 'British Columbia');
INSERT INTO `geo_zone_reference` VALUES (68, 38, 'MB', 'Manitoba');
INSERT INTO `geo_zone_reference` VALUES (69, 38, 'NF', 'Newfoundland');
INSERT INTO `geo_zone_reference` VALUES (70, 38, 'NB', 'New Brunswick');
INSERT INTO `geo_zone_reference` VALUES (71, 38, 'NS', 'Nova Scotia');
INSERT INTO `geo_zone_reference` VALUES (72, 38, 'NT', 'Northwest Territories');
INSERT INTO `geo_zone_reference` VALUES (73, 38, 'NU', 'Nunavut');
INSERT INTO `geo_zone_reference` VALUES (74, 38, 'ON', 'Ontario');
INSERT INTO `geo_zone_reference` VALUES (75, 38, 'PE', 'Prince Edward Island');
INSERT INTO `geo_zone_reference` VALUES (76, 38, 'QC', 'Quebec');
INSERT INTO `geo_zone_reference` VALUES (77, 38, 'SK', 'Saskatchewan');
INSERT INTO `geo_zone_reference` VALUES (78, 38, 'YT', 'Yukon Territory');
INSERT INTO `geo_zone_reference` VALUES (79, 61, 'QLD', 'Queensland');
INSERT INTO `geo_zone_reference` VALUES (80, 61, 'SA', 'South Australia');
INSERT INTO `geo_zone_reference` VALUES (81, 61, 'ACT', 'Australian Capital Territory');
INSERT INTO `geo_zone_reference` VALUES (82, 61, 'VIC', 'Victoria');

-- --------------------------------------------------------

--
-- Table structure for table `groups`
--

DROP TABLE IF EXISTS `groups`;
CREATE TABLE `groups` (
  `id` bigint(20) NOT NULL auto_increment,
  `name` longtext,
  `user` longtext,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `history_data`
--

DROP TABLE IF EXISTS `history_data`;
CREATE TABLE `history_data` (
  `id` bigint(20) NOT NULL auto_increment,
  `coffee` longtext,
  `tobacco` longtext,
  `alcohol` longtext,
  `sleep_patterns` longtext,
  `exercise_patterns` longtext,
  `seatbelt_use` longtext,
  `counseling` longtext,
  `hazardous_activities` longtext,
  `recreational_drugs` longtext,
  `last_breast_exam` varchar(255) default NULL,
  `last_mammogram` varchar(255) default NULL,
  `last_gynocological_exam` varchar(255) default NULL,
  `last_rectal_exam` varchar(255) default NULL,
  `last_prostate_exam` varchar(255) default NULL,
  `last_physical_exam` varchar(255) default NULL,
  `last_sigmoidoscopy_colonoscopy` varchar(255) default NULL,
  `last_ecg` varchar(255) default NULL,
  `last_cardiac_echo` varchar(255) default NULL,
  `last_retinal` varchar(255) default NULL,
  `last_fluvax` varchar(255) default NULL,
  `last_pneuvax` varchar(255) default NULL,
  `last_ldl` varchar(255) default NULL,
  `last_hemoglobin` varchar(255) default NULL,
  `last_psa` varchar(255) default NULL,
  `last_exam_results` varchar(255) default NULL,
  `history_mother` longtext,
  `dc_mother` text,
  `history_father` longtext,
  `dc_father`  text,
  `history_siblings` longtext,
  `dc_siblings` text,
  `history_offspring` longtext,
  `dc_offspring` text,
  `history_spouse` longtext,
  `dc_spouse` text,
  `relatives_cancer` longtext,
  `relatives_tuberculosis` longtext,
  `relatives_diabetes` longtext,
  `relatives_high_blood_pressure` longtext,
  `relatives_heart_problems` longtext,
  `relatives_stroke` longtext,
  `relatives_epilepsy` longtext,
  `relatives_mental_illness` longtext,
  `relatives_suicide` longtext,
  `cataract_surgery` datetime default NULL,
  `tonsillectomy` datetime default NULL,
  `cholecystestomy` datetime default NULL,
  `heart_surgery` datetime default NULL,
  `hysterectomy` datetime default NULL,
  `hernia_repair` datetime default NULL,
  `hip_replacement` datetime default NULL,
  `knee_replacement` datetime default NULL,
  `appendectomy` datetime default NULL,
  `date` datetime default NULL,
  `pid` bigint(20) NOT NULL default '0',
  `name_1` varchar(255) default NULL,
  `value_1` varchar(255) default NULL,
  `name_2` varchar(255) default NULL,
  `value_2` varchar(255) default NULL,
  `additional_history` text,
  `exams` text,
  `usertext11` TEXT,
  `usertext12` varchar(255) NOT NULL DEFAULT '',
  `usertext13` varchar(255) NOT NULL DEFAULT '',
  `usertext14` varchar(255) NOT NULL DEFAULT '',
  `usertext15` varchar(255) NOT NULL DEFAULT '',
  `usertext16` varchar(255) NOT NULL DEFAULT '',
  `usertext17` varchar(255) NOT NULL DEFAULT '',
  `usertext18` varchar(255) NOT NULL DEFAULT '',
  `usertext19` varchar(255) NOT NULL DEFAULT '',
  `usertext20` varchar(255) NOT NULL DEFAULT '',
  `usertext21` varchar(255) NOT NULL DEFAULT '',
  `usertext22` varchar(255) NOT NULL DEFAULT '',
  `usertext23` varchar(255) NOT NULL DEFAULT '',
  `usertext24` varchar(255) NOT NULL DEFAULT '',
  `usertext25` varchar(255) NOT NULL DEFAULT '',
  `usertext26` varchar(255) NOT NULL DEFAULT '',
  `usertext27` varchar(255) NOT NULL DEFAULT '',
  `usertext28` varchar(255) NOT NULL DEFAULT '',
  `usertext29` varchar(255) NOT NULL DEFAULT '',
  `usertext30` varchar(255) NOT NULL DEFAULT '',
  `userdate11` date DEFAULT NULL,
  `userdate12` date DEFAULT NULL,
  `userdate13` date DEFAULT NULL,
  `userdate14` date DEFAULT NULL,
  `userdate15` date DEFAULT NULL,
  `userarea11` text,
  `userarea12` text,
  PRIMARY KEY  (`id`),
  KEY `pid` (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `icd9_dx_code`
--

DROP TABLE IF EXISTS `icd9_dx_code`;
CREATE TABLE `icd9_dx_code` (
  `dx_id` SERIAL,
  `dx_code`             varchar(5),
  `formatted_dx_code`   varchar(6),
  `short_desc`          varchar(60),
  `long_desc`           varchar(300),
  `active` tinyint default 0,
  `revision` int default 0,
  KEY `dx_code` (`dx_code`),
  KEY `formatted_dx_code` (`formatted_dx_code`),
  KEY `active` (`active`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `icd9_sg_code`
--

DROP TABLE IF EXISTS `icd9_sg_code`;
CREATE TABLE `icd9_sg_code` (
  `sg_id` SERIAL,
  `sg_code`             varchar(5),
  `formatted_sg_code`   varchar(6),
  `short_desc`          varchar(60),
  `long_desc`           varchar(300),
  `active` tinyint default 0,
  `revision` int default 0,
  KEY `sg_code` (`sg_code`),
  KEY `formatted_sg_code` (`formatted_sg_code`),
  KEY `active` (`active`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `icd9_dx_long_code`
--

DROP TABLE IF EXISTS `icd9_dx_long_code`;
CREATE TABLE `icd9_dx_long_code` (
  `dx_id` SERIAL,
  `dx_code`             varchar(5),
  `long_desc`           varchar(300),
  `active` tinyint default 0,
  `revision` int default 0
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `icd9_sg_long_code`
--

DROP TABLE IF EXISTS `icd9_sg_long_code`;
CREATE TABLE `icd9_sg_long_code` (
  `sq_id` SERIAL,
  `sg_code`             varchar(5),
  `long_desc`           varchar(300),
  `active` tinyint default 0,
  `revision` int default 0
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `icd10_dx_order_code`
--

DROP TABLE IF EXISTS `icd10_dx_order_code`;
CREATE TABLE `icd10_dx_order_code` (
  `dx_id`               SERIAL,
  `dx_code`             varchar(7),
  `formatted_dx_code`   varchar(10),
  `valid_for_coding`    char,
  `short_desc`          varchar(60),
  `long_desc`           varchar(300),
  `active` tinyint default 0,
  `revision` int default 0,
  KEY `formatted_dx_code` (`formatted_dx_code`),
  KEY `active` (`active`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `icd10_pcs_order_code`
--

DROP TABLE IF EXISTS `icd10_pcs_order_code`;
CREATE TABLE `icd10_pcs_order_code` (
  `pcs_id`              SERIAL,
  `pcs_code`            varchar(7),
  `valid_for_coding`    char,
  `short_desc`          varchar(60),
  `long_desc`           varchar(300),
  `active` tinyint default 0,
  `revision` int default 0,
  KEY `pcs_code` (`pcs_code`),
  KEY `active` (`active`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `icd10_gem_pcs_9_10`
--

DROP TABLE IF EXISTS `icd10_gem_pcs_9_10`;
CREATE TABLE `icd10_gem_pcs_9_10` (
  `map_id` SERIAL,
  `pcs_icd9_source` varchar(5) default NULL,
  `pcs_icd10_target` varchar(7) default NULL,
  `flags` varchar(5) default NULL,
  `active` tinyint default 0,
  `revision` int default 0
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `icd10_gem_pcs_10_9`
--

DROP TABLE IF EXISTS `icd10_gem_pcs_10_9`;
CREATE TABLE `icd10_gem_pcs_10_9` (
  `map_id` SERIAL,
  `pcs_icd10_source` varchar(7) default NULL,
  `pcs_icd9_target` varchar(5) default NULL,
  `flags` varchar(5) default NULL,
  `active` tinyint default 0,
  `revision` int default 0
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `icd10_gem_dx_9_10`
--

DROP TABLE IF EXISTS `icd10_gem_dx_9_10`;
CREATE TABLE `icd10_gem_dx_9_10` (
  `map_id` SERIAL,
  `dx_icd9_source` varchar(5) default NULL,
  `dx_icd10_target` varchar(7) default NULL,
  `flags` varchar(5) default NULL,
  `active` tinyint default 0,
  `revision` int default 0
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `icd10_gem_dx_10_9`
--

DROP TABLE IF EXISTS `icd10_gem_dx_10_9`;
CREATE TABLE `icd10_gem_dx_10_9` (
  `map_id` SERIAL,
  `dx_icd10_source` varchar(7) default NULL,
  `dx_icd9_target` varchar(5) default NULL,
  `flags` varchar(5) default NULL,
  `active` tinyint default 0,
  `revision` int default 0
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `icd10_reimbr_dx_9_10`
--

DROP TABLE IF EXISTS `icd10_reimbr_dx_9_10`;
CREATE TABLE `icd10_reimbr_dx_9_10` (
  `map_id` SERIAL,
  `code`        varchar(8),
  `code_cnt`    tinyint,
  `ICD9_01`     varchar(5),
  `ICD9_02`     varchar(5),
  `ICD9_03`     varchar(5),
  `ICD9_04`     varchar(5),
  `ICD9_05`     varchar(5),
  `ICD9_06`     varchar(5),
  `active` tinyint default 0,
  `revision` int default 0
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `icd10_reimbr_pcs_9_10`
--

DROP TABLE IF EXISTS `icd10_reimbr_pcs_9_10`;
CREATE TABLE `icd10_reimbr_pcs_9_10` (
  `map_id`      SERIAL,
  `code`        varchar(8),
  `code_cnt`    tinyint,
  `ICD9_01`     varchar(5),
  `ICD9_02`     varchar(5),
  `ICD9_03`     varchar(5),
  `ICD9_04`     varchar(5),
  `ICD9_05`     varchar(5),
  `ICD9_06`     varchar(5),
  `active` tinyint default 0,
  `revision` int default 0
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `immunizations`
--

DROP TABLE IF EXISTS `immunizations`;
CREATE TABLE `immunizations` (
  `id` bigint(20) NOT NULL auto_increment,
  `patient_id` int(11) default NULL,
  `administered_date` datetime default NULL,
  `immunization_id` int(11) default NULL,
  `cvx_code` varchar(10) default NULL,
  `manufacturer` varchar(100) default NULL,
  `lot_number` varchar(50) default NULL,
  `administered_by_id` bigint(20) default NULL,
  `administered_by` VARCHAR( 255 ) default NULL COMMENT 'Alternative to administered_by_id',
  `education_date` date default NULL,
  `vis_date` date default NULL COMMENT 'Date of VIS Statement',
  `note` text,
  `create_date` datetime default NULL,
  `update_date` timestamp NOT NULL,
  `created_by` bigint(20) default NULL,
  `updated_by` bigint(20) default NULL,
  `amount_administered` float DEFAULT NULL,
  `amount_administered_unit` varchar(50) DEFAULT NULL,
  `expiration_date` date DEFAULT NULL,
  `route` varchar(100) DEFAULT NULL,
  `administration_site` varchar(100) DEFAULT NULL,
  `added_erroneously` tinyint(1) NOT NULL DEFAULT '0',
  `external_id` VARCHAR(20) DEFAULT NULL,
  `completion_status` VARCHAR(50) DEFAULT NULL,
  `information_source` VARCHAR(31) DEFAULT NULL,
  `refusal_reason` VARCHAR(31) DEFAULT NULL,
  `ordering_provider` INT(11) DEFAULT NULL,
  PRIMARY KEY  (`id`),
  KEY `patient_id` (`patient_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `insurance_companies`
--

DROP TABLE IF EXISTS `insurance_companies`;
CREATE TABLE `insurance_companies` (
  `id` int(11) NOT NULL default '0',
  `name` varchar(255) default NULL,
  `attn` varchar(255) default NULL,
  `cms_id` varchar(15) default NULL,
  `ins_type_code` tinyint(2) default NULL,
  `x12_receiver_id` varchar(25) default NULL,
  `x12_default_partner_id` int(11) default NULL,
  `alt_cms_id` varchar(15) NOT NULL DEFAULT '',
  `inactive` int(1) NOT NULL DEFAULT '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `insurance_data`
--

DROP TABLE IF EXISTS `insurance_data`;
CREATE TABLE `insurance_data` (
  `id` bigint(20) NOT NULL auto_increment,
  `type` enum('primary','secondary','tertiary') default NULL,
  `provider` varchar(255) default NULL,
  `plan_name` varchar(255) default NULL,
  `policy_number` varchar(255) default NULL,
  `group_number` varchar(255) default NULL,
  `subscriber_lname` varchar(255) default NULL,
  `subscriber_mname` varchar(255) default NULL,
  `subscriber_fname` varchar(255) default NULL,
  `subscriber_relationship` varchar(255) default NULL,
  `subscriber_ss` varchar(255) default NULL,
  `subscriber_DOB` date default NULL,
  `subscriber_street` varchar(255) default NULL,
  `subscriber_postal_code` varchar(255) default NULL,
  `subscriber_city` varchar(255) default NULL,
  `subscriber_state` varchar(255) default NULL,
  `subscriber_country` varchar(255) default NULL,
  `subscriber_phone` varchar(255) default NULL,
  `subscriber_employer` varchar(255) default NULL,
  `subscriber_employer_street` varchar(255) default NULL,
  `subscriber_employer_postal_code` varchar(255) default NULL,
  `subscriber_employer_state` varchar(255) default NULL,
  `subscriber_employer_country` varchar(255) default NULL,
  `subscriber_employer_city` varchar(255) default NULL,
  `copay` varchar(255) default NULL,
  `date` date NOT NULL default '0000-00-00',
  `pid` bigint(20) NOT NULL default '0',
  `subscriber_sex` varchar(25) default NULL,
  `accept_assignment` varchar(5) NOT NULL DEFAULT 'TRUE',
  `policy_type` varchar(25) NOT NULL default '',
  PRIMARY KEY  (`id`),
  UNIQUE KEY `pid_type_date` (`pid`,`type`,`date`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `insurance_numbers`
--

DROP TABLE IF EXISTS `insurance_numbers`;
CREATE TABLE `insurance_numbers` (
  `id` int(11) NOT NULL default '0',
  `provider_id` int(11) NOT NULL default '0',
  `insurance_company_id` int(11) default NULL,
  `provider_number` varchar(20) default NULL,
  `rendering_provider_number` varchar(20) default NULL,
  `group_number` varchar(20) default NULL,
  `provider_number_type` varchar(4) default NULL,
  `rendering_provider_number_type` varchar(4) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `issue_encounter`
--

DROP TABLE IF EXISTS `issue_encounter`;
CREATE TABLE `issue_encounter` (
  `pid` int(11) NOT NULL,
  `list_id` int(11) NOT NULL,
  `encounter` int(11) NOT NULL,
  `resolved` tinyint(1) NOT NULL,
  PRIMARY KEY  (`pid`,`list_id`,`encounter`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `issue_types`
--

DROP TABLE IF EXISTS `issue_types`;
CREATE TABLE `issue_types` (
    `active` tinyint(1) NOT NULL DEFAULT '1',
    `category` varchar(75) NOT NULL DEFAULT '',
    `type` varchar(75) NOT NULL DEFAULT '',
    `plural` varchar(75) NOT NULL DEFAULT '',
    `singular` varchar(75) NOT NULL DEFAULT '',
    `abbreviation` varchar(75) NOT NULL DEFAULT '',
    `style` smallint(6) NOT NULL DEFAULT '0',
    `force_show` smallint(6) NOT NULL DEFAULT '0',
    `ordering` int(11) NOT NULL DEFAULT '0',
    PRIMARY KEY (`category`,`type`)
) ENGINE=InnoDB;

--
-- Dumping data for table `issue_types`
--

INSERT INTO issue_types(`ordering`,`category`,`type`,`plural`,`singular`,`abbreviation`,`style`,`force_show`) VALUES ('10','default','medical_problem','Medical Problems','Problem','P','0','1');
INSERT INTO issue_types(`ordering`,`category`,`type`,`plural`,`singular`,`abbreviation`,`style`,`force_show`) VALUES ('30','default','medication','Medications','Medication','M','0','1');
INSERT INTO issue_types(`ordering`,`category`,`type`,`plural`,`singular`,`abbreviation`,`style`,`force_show`) VALUES ('20','default','allergy','Allergies','Allergy','A','0','1');
INSERT INTO issue_types(`ordering`,`category`,`type`,`plural`,`singular`,`abbreviation`,`style`,`force_show`) VALUES ('40','default','surgery','Surgeries','Surgery','S','0','0');
INSERT INTO issue_types(`ordering`,`category`,`type`,`plural`,`singular`,`abbreviation`,`style`,`force_show`) VALUES ('50','default','dental','Dental Issues','Dental','D','0','0');
INSERT INTO issue_types(`ordering`,`category`,`type`,`plural`,`singular`,`abbreviation`,`style`,`force_show`) VALUES ('10','ippf_specific','medical_problem','Medical Problems','Problem','P','0','1');
INSERT INTO issue_types(`ordering`,`category`,`type`,`plural`,`singular`,`abbreviation`,`style`,`force_show`) VALUES ('30','ippf_specific','medication','Medications','Medication','M','0','1');
INSERT INTO issue_types(`ordering`,`category`,`type`,`plural`,`singular`,`abbreviation`,`style`,`force_show`) VALUES ('20','ippf_specific','allergy','Allergies','Allergy','Y','0','1');
INSERT INTO issue_types(`ordering`,`category`,`type`,`plural`,`singular`,`abbreviation`,`style`,`force_show`) VALUES ('40','ippf_specific','surgery','Surgeries','Surgery','S','0','0');
INSERT INTO issue_types(`ordering`,`category`,`type`,`plural`,`singular`,`abbreviation`,`style`,`force_show`) VALUES ('50','ippf_specific','ippf_gcac','Abortions','Abortion','A','3','0');
INSERT INTO issue_types(`ordering`,`category`,`type`,`plural`,`singular`,`abbreviation`,`style`,`force_show`) VALUES ('60','ippf_specific','contraceptive','Contraception','Contraception','C','4','0');

-- --------------------------------------------------------

--
-- Table structure for table `lang_constants`
--

DROP TABLE IF EXISTS `lang_constants`;
CREATE TABLE `lang_constants` (
  `cons_id` int(11) NOT NULL auto_increment,
  `constant_name` mediumtext BINARY,
  UNIQUE KEY `cons_id` (`cons_id`),
  KEY `constant_name` (`constant_name`(100))
) ENGINE=InnoDB ;

--
-- Table structure for table `lang_definitions`
--

DROP TABLE IF EXISTS `lang_definitions`;
CREATE TABLE `lang_definitions` (
  `def_id` int(11) NOT NULL auto_increment,
  `cons_id` int(11) NOT NULL default '0',
  `lang_id` int(11) NOT NULL default '0',
  `definition` mediumtext,
  UNIQUE KEY `def_id` (`def_id`),
  KEY `cons_id` (`cons_id`)
) ENGINE=InnoDB ;

--
-- Table structure for table `lang_languages`
--

DROP TABLE IF EXISTS `lang_languages`;
CREATE TABLE `lang_languages` (
  `lang_id` int(11) NOT NULL auto_increment,
  `lang_code` char(2) NOT NULL default '',
  `lang_description` varchar(100) default NULL,
  `lang_is_rtl` TINYINT DEFAULT 0 COMMENT 'Set this to 1 for RTL languages Arabic, Farsi, Hebrew, Urdu etc.',
  UNIQUE KEY `lang_id` (`lang_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 ;

--
-- Dumping data for table `lang_languages`
--

INSERT INTO `lang_languages` VALUES (1, 'en', 'English', 0);

-- --------------------------------------------------------

--
-- Table structure for table `lang_custom`
--

DROP TABLE IF EXISTS `lang_custom`;
CREATE TABLE `lang_custom` (
  `lang_description` varchar(100) NOT NULL default '',
  `lang_code` char(2) NOT NULL default '',
  `constant_name` mediumtext,
  `definition` mediumtext
) ENGINE=InnoDB ;

-- --------------------------------------------------------

--
-- Table structure for table `layout_options`
--

DROP TABLE IF EXISTS `layout_options`;
CREATE TABLE `layout_options` (
  `form_id` varchar(31) NOT NULL default '',
  `field_id` varchar(31) NOT NULL default '',
  `group_name` varchar(31) NOT NULL default '',
  `title` varchar(63) NOT NULL default '',
  `seq` int(11) NOT NULL default '0',
  `data_type` tinyint(3) NOT NULL default '0',
  `uor` tinyint(1) NOT NULL default '1',
  `fld_length` int(11) NOT NULL default '15',
  `max_length` int(11) NOT NULL default '0',
  `list_id` varchar(31) NOT NULL default '',
  `titlecols` tinyint(3) NOT NULL default '1',
  `datacols` tinyint(3) NOT NULL default '1',
  `default_value` varchar(255) NOT NULL default '',
  `edit_options` varchar(36) NOT NULL default '',
  `description` text,
  `fld_rows` int(11) NOT NULL default '0',
  `list_backup_id` varchar(31) NOT NULL default '',
  `source` char(1) NOT NULL default 'F' COMMENT 'F=Form, D=Demographics, H=History, E=Encounter',
  `conditions` text COMMENT 'serialized array of skip conditions',
  `validation` varchar(100) default NULL,
  PRIMARY KEY  (`form_id`,`field_id`,`seq`)
) ENGINE=InnoDB;

--
-- Dumping data for table `layout_options`
--

INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'title', '1Who', 'Name', 1, 1, 1, 0, 0, 'titles', 1, 1, '', 'N', 'Title', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'fname', '1Who', '', 2, 2, 2, 10, 63, '', 0, 0, '', 'CD', 'First Name', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'mname', '1Who', '', 3, 2, 1, 2, 63, '', 0, 0, '', 'C', 'Middle Name', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'lname', '1Who', '', 4, 2, 2, 10, 63, '', 0, 0, '', 'CD', 'Last Name', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'pubpid', '1Who', 'External ID', 5, 2, 1, 10, 15, '', 1, 1, '', 'ND', 'External identifier', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'DOB', '1Who', 'DOB', 6, 4, 2, 10, 10, '', 1, 1, '', 'D', 'Date of Birth', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'sex', '1Who', 'Sex', 7, 1, 2, 0, 0, 'sex', 1, 1, '', 'N', 'Sex', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'ss', '1Who', 'S.S.', 8, 2, 1, 11, 11, '', 1, 1, '', '', 'Social Security Number', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'drivers_license', '1Who', 'License/ID', 9, 2, 1, 15, 63, '', 1, 1, '', '', 'Drivers License or State ID', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'status', '1Who', 'Marital Status', 10, 1, 1, 0, 0, 'marital', 1, 3, '', '', 'Marital Status', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'genericname1', '1Who', 'User Defined', 11, 2, 1, 15, 63, '', 1, 3, '', '', 'User Defined Field', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'genericval1', '1Who', '', 12, 2, 1, 15, 63, '', 0, 0, '', '', 'User Defined Field', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'genericname2', '1Who', '', 13, 2, 1, 15, 63, '', 0, 0, '', '', 'User Defined Field', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'genericval2', '1Who', '', 14, 2, 1, 15, 63, '', 0, 0, '', '', 'User Defined Field', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'squad', '1Who', 'Squad', 15, 13, 0, 0, 0, '', 1, 3, '', '', 'Squad Membership', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'pricelevel', '1Who', 'Price Level', 16, 1, 0, 0, 0, 'pricelevel', 1, 1, '', '', 'Discount Level', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'billing_note', '1Who', 'Billing Note', 17, 2, 1, 60, 0, '', 1, 3, '', '', 'Patient Level Billing Note (Collections)', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'street', '2Contact', 'Address', 1, 2, 1, 25, 63, '', 1, 1, '', 'C', 'Street and Number', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'city', '2Contact', 'City', 2, 2, 1, 15, 63, '', 1, 1, '', 'C', 'City Name', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'state', '2Contact', 'State', 3, 26, 1, 0, 0, 'state', 1, 1, '', '', 'State/Locality', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'postal_code', '2Contact', 'Postal Code', 4, 2, 1, 6, 63, '', 1, 1, '', '', 'Postal Code', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'country_code', '2Contact', 'Country', 5, 26, 1, 0, 0, 'country', 1, 1, '', '', 'Country', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'county', '2Contact', 'County', 5, 26, 1, 0, 0, 'county', 1, 1, '', '', 'County', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'mothersname', '2Contact', 'Mother''s Name', 6, 2, 1, 20, 63, '', 1, 1, '', '', '', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'contact_relationship', '2Contact', 'Emergency Contact', 8, 2, 1, 10, 63, '', 1, 1, '', 'C', 'Emergency Contact Person', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'phone_contact', '2Contact', 'Emergency Phone', 9, 2, 1, 20, 63, '', 1, 1, '', 'P', 'Emergency Contact Phone Number', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'phone_home', '2Contact', 'Home Phone', 10, 2, 1, 20, 63, '', 1, 1, '', 'P', 'Home Phone Number', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'phone_biz', '2Contact', 'Work Phone', 11, 2, 1, 20, 63, '', 1, 1, '', 'P', 'Work Phone Number', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'phone_cell', '2Contact', 'Mobile Phone', 12, 2, 1, 20, 63, '', 1, 1, '', 'P', 'Cell Phone Number', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'email', '2Contact', 'Contact Email', 13, 2, 1, 30, 95, '', 1, 1, '', '', 'Contact Email Address', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'email_direct', '2Contact', 'Trusted Email', 14, 2, 1, 30, 95, '', 1, 1, '', '', 'Trusted Direct Email Address', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'providerID', '3Choices', 'Provider', 1, 11, 1, 0, 0, '', 1, 3, '', '', 'Provider', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'ref_providerID', '3Choices', 'Referring Provider', 2, 11, 1, 0, 0, '', 1, 3, '', '', 'Referring Provider', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'pharmacy_id', '3Choices', 'Pharmacy', 3, 12, 1, 0, 0, '', 1, 3, '', '', 'Preferred Pharmacy', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'hipaa_notice', '3Choices', 'HIPAA Notice Received', 4, 1, 1, 0, 0, 'yesno', 1, 1, '', '', 'Did you receive a copy of the HIPAA Notice?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'hipaa_voice', '3Choices', 'Allow Voice Message', 5, 1, 1, 0, 0, 'yesno', 1, 1, '', '', 'Allow telephone messages?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'hipaa_message', '3Choices', 'Leave Message With', 6, 2, 1, 20, 63, '', 1, 1, '', '', 'With whom may we leave a message?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'hipaa_mail', '3Choices', 'Allow Mail Message', 7, 1, 1, 0, 0, 'yesno', 1, 1, '', '', 'Allow email messages?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'hipaa_allowsms'  , '3Choices', 'Allow SMS'  , 8, 1, 1, 0, 0, 'yesno', 1, 1, '', '', 'Allow SMS (text messages)?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'hipaa_allowemail', '3Choices', 'Allow Email', 9, 1, 1, 0, 0, 'yesno', 1, 1, '', '', 'Allow Email?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'allow_imm_reg_use', '3Choices', 'Allow Immunization Registry Use', 10, 1, 1, 0, 0, 'yesno', 1, 1, '', '', '', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'allow_imm_info_share', '3Choices', 'Allow Immunization Info Sharing', 11, 1, 1, 0, 0, 'yesno', 1, 1, '', '', '', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'allow_health_info_ex', '3Choices', 'Allow Health Information Exchange', 12, 1, 1, 0, 0, 'yesno', 1, 1, '', '', '', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'allow_patient_portal', '3Choices', 'Allow Patient Portal', 13, 1, 1, 0, 0, 'yesno', 1, 1, '', '', '', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'care_team', '3Choices', 'Care Team', 14, 11, 1, 0, 0, '', 1, 1, '', '', '', 0) ;
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'cmsportal_login', '3Choices', 'CMS Portal Login', 15, 2, 1, 30, 60, '', 1, 1, '', '', 'CMS Portal Login ID', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'imm_reg_status'  , '3Choices', 'Immunization Registry Status'  ,16, 1, 1,1,0, 'immunization_registry_status', 1, 1, '', '', 'Immunization Registry Status', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'imm_reg_stat_effdate'  , '3Choices', 'Immunization Registry Status Effective Date'  ,17, 4, 1,10,10, '', 1, 1, '', '', 'Immunization Registry Status Effective Date', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'publicity_code'  , '3Choices', 'Publicity Code'  ,18, 1, 1,1,0, 'publicity_code', 1, 1, '', '', 'Publicity Code', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'publ_code_eff_date'  , '3Choices', 'Publicity Code Effective Date'  ,19, 4, 1,10,10, '', 1, 1, '', '', 'Publicity Code Effective Date', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'protect_indicator'  , '3Choices', 'Protection Indicator'  ,20, 1, 1,1,0, 'yesno', 1, 1, '', '', 'Protection Indicator', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'prot_indi_effdate'  , '3Choices', 'Protection Indicator Effective Date'  ,21, 4, 1,10,10, '', 1, 1, '', '', 'Protection Indicator Effective Date', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'occupation', '4Employer', 'Occupation', 1, 26, 1, 0, 0, 'Occupation', 1, 1, '', '', 'Occupation', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'industry', '4Employer', 'Industry', 1, 26, 1, 0, 0, 'Industry', 1, 1, '', '', 'Industry', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'em_name', '4Employer', 'Employer Name', 2, 2, 1, 20, 63, '', 1, 1, '', 'C', 'Employer Name', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'em_street', '4Employer', 'Employer Address', 3, 2, 1, 25, 63, '', 1, 1, '', 'C', 'Street and Number', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'em_city', '4Employer', 'City', 4, 2, 1, 15, 63, '', 1, 1, '', 'C', 'City Name', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'em_state', '4Employer', 'State', 5, 26, 1, 0, 0, 'state', 1, 1, '', '', 'State/Locality', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'em_postal_code', '4Employer', 'Postal Code', 6, 2, 1, 6, 63, '', 1, 1, '', '', 'Postal Code', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'em_country', '4Employer', 'Country', 7, 26, 1, 0, 0, 'country', 1, 1, '', '', 'Country', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'language', '5Stats', 'Language', 1, 1, 1, 0, 0, 'language', 1, 3, '', '', 'Preferred Language', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`,`list_backup_id`) VALUES ('DEM', 'ethnicity', '5Stats', 'Ethnicity', 2, 33, 1, 0, 0, 'ethnicity', 1, 1, '', '', 'Ethnicity', 0, 'ethrace');
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`,`list_backup_id`) VALUES ('DEM', 'race', '5Stats', 'Race', 3, 36, 1, 0, 0, 'race', 1, 1, '', '', 'Race', 0, 'ethrace');
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'financial_review', '5Stats', 'Financial Review Date', 4, 2, 1, 10, 20, '', 1, 1, '', 'D', 'Financial Review Date', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'family_size', '5Stats', 'Family Size', 4, 2, 1, 20, 63, '', 1, 1, '', '', 'Family Size', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'monthly_income', '5Stats', 'Monthly Income', 5, 2, 1, 20, 63, '', 1, 1, '', '', 'Monthly Income', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'homeless', '5Stats', 'Homeless, etc.', 6, 2, 1, 20, 63, '', 1, 1, '', '', 'Homeless or similar?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'interpretter', '5Stats', 'Interpreter', 7, 2, 1, 20, 63, '', 1, 1, '', '', 'Interpreter needed?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'migrantseasonal', '5Stats', 'Migrant/Seasonal', 8, 2, 1, 20, 63, '', 1, 1, '', '', 'Migrant or seasonal worker?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'contrastart', '5Stats', 'Contraceptives Start',9,4,0,10,10,'',1,1,'','','Date contraceptive services initially provided', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'referral_source', '5Stats', 'Referral Source',10, 26, 1, 0, 0, 'refsource', 1, 1, '', '', 'How did they hear about us', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'vfc', '5Stats', 'VFC', 12, 1, 1, 20, 0, 'eligibility', 1, 1, '', '', 'Eligibility status for Vaccine for Children supplied vaccine', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'religion', '5Stats', 'Religion', 13, 1, 1, 0, 0, 'religious_affiliation', 1, 3, '', '', 'Patient Religion', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'deceased_date', '6Misc', 'Date Deceased', 1, 4, 1, 20, 20, '', 1, 3, '', 'D', 'If person is deceased, then enter date of death.', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'deceased_reason', '6Misc', 'Reason Deceased', 2, 2, 1, 30, 255, '', 1, 3, '', '', 'Reason for Death', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'usertext1', '6Misc', 'User Defined Text 1', 3, 2, 0, 10, 63, '', 1, 1, '', '', 'User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'usertext2', '6Misc', 'User Defined Text 2', 4, 2, 0, 10, 63, '', 1, 1, '', '', 'User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'usertext3', '6Misc', 'User Defined Text 3', 5,2,0,10,63,'',1,1,'','','User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'usertext4', '6Misc', 'User Defined Text 4', 6,2,0,10,63,'',1,1,'','','User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'usertext5', '6Misc', 'User Defined Text 5', 7,2,0,10,63,'',1,1,'','','User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'usertext6', '6Misc', 'User Defined Text 6', 8,2,0,10,63,'',1,1,'','','User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'usertext7', '6Misc', 'User Defined Text 7', 9,2,0,10,63,'',1,1,'','','User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'usertext8', '6Misc', 'User Defined Text 8',10,2,0,10,63,'',1,1,'','','User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'userlist1', '6Misc', 'User Defined List 1',11, 1, 0, 0, 0, 'userlist1', 1, 1, '', '', 'User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'userlist2', '6Misc', 'User Defined List 2',12, 1, 0, 0, 0, 'userlist2', 1, 1, '', '', 'User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'userlist3', '6Misc', 'User Defined List 3',13, 1, 0, 0, 0, 'userlist3', 1, 1, '', '' , 'User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'userlist4', '6Misc', 'User Defined List 4',14, 1, 0, 0, 0, 'userlist4', 1, 1, '', '' , 'User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'userlist5', '6Misc', 'User Defined List 5',15, 1, 0, 0, 0, 'userlist5', 1, 1, '', '' , 'User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'userlist6', '6Misc', 'User Defined List 6',16, 1, 0, 0, 0, 'userlist6', 1, 1, '', '' , 'User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'userlist7', '6Misc', 'User Defined List 7',17, 1, 0, 0, 0, 'userlist7', 1, 1, '', '' , 'User Defined', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'regdate'  , '6Misc', 'Registration Date'  ,18, 4, 0,10,10, ''         , 1, 1, '', 'D', 'Start Date at This Clinic', 0);

INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','refer_date'      ,'1Referral','Referral Date'                  , 1, 4,2, 0,  0,''         ,1,1,'C','D','Date of referral', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','refer_from'      ,'1Referral','Refer By'                       , 2,10,2, 0,  0,''         ,1,1,'' ,'' ,'Referral By', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','refer_external'  ,'1Referral','External Referral'              , 3, 1,1, 0,  0,'boolean'  ,1,1,'' ,'' ,'External referral?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','refer_to'        ,'1Referral','Refer To'                       , 4,14,2, 0,  0,''         ,1,1,'' ,'' ,'Referral To', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','body'            ,'1Referral','Reason'                         , 5, 3,2,30,  0,''         ,1,1,'' ,'' ,'Reason for referral', 3);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','refer_diag'      ,'1Referral','Referrer Diagnosis'             , 6, 2,1,30,255,''         ,1,1,'' ,'X','Referrer diagnosis', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','refer_risk_level','1Referral','Risk Level'                     , 7, 1,1, 0,  0,'risklevel',1,1,'' ,'' ,'Level of urgency', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','refer_vitals'    ,'1Referral','Include Vitals'                 , 8, 1,1, 0,  0,'boolean'  ,1,1,'' ,'' ,'Include vitals data?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','refer_related_code','1Referral','Requested Service'            , 9,15,1,30,255,''         ,1,1,'' ,'' ,'Billing Code for Requested Service', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','reply_date'      ,'2Counter-Referral','Reply Date'             ,10, 4,1, 0,  0,''         ,1,1,'' ,'D','Date of reply', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','reply_from'      ,'2Counter-Referral','Reply From'             ,11, 2,1,30,255,''         ,1,1,'' ,'' ,'Who replied?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','reply_init_diag' ,'2Counter-Referral','Presumed Diagnosis'     ,12, 2,1,30,255,''         ,1,1,'' ,'' ,'Presumed diagnosis by specialist', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','reply_final_diag','2Counter-Referral','Final Diagnosis'        ,13, 2,1,30,255,''         ,1,1,'' ,'' ,'Final diagnosis by specialist', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','reply_documents' ,'2Counter-Referral','Documents'              ,14, 2,1,30,255,''         ,1,1,'' ,'' ,'Where may related scanned or paper documents be found?', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','reply_findings'  ,'2Counter-Referral','Findings'               ,15, 3,1,30,  0,''         ,1,1,'' ,'' ,'Findings by specialist', 3);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','reply_services'  ,'2Counter-Referral','Services Provided'      ,16, 3,1,30,  0,''         ,1,1,'' ,'' ,'Service provided by specialist', 3);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','reply_recommend' ,'2Counter-Referral','Recommendations'        ,17, 3,1,30,  0,''         ,1,1,'' ,'' ,'Recommendations by specialist', 3);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTref','reply_rx_refer'  ,'2Counter-Referral','Prescriptions/Referrals',18, 3,1,30,  0,''         ,1,1,'' ,'' ,'Prescriptions and/or referrals by specialist', 3);

INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTptreq','body','1','Details',10,3,2,30,0,'',1,3,'','','Content',5);

INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTphreq','body','1','Details',10,3,2,30,0,'',1,3,'','','Content',5);

INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTlegal','body','1','Details',10,3,2,30,0,'',1,3,'','','Content',5);

INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('LBTbill' ,'body','1','Details',10,3,2,30,0,'',1,3,'','','Content',5);

INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','usertext11'       ,'1General'       ,'Risk Factors',1,21,1,0,0,'riskfactors',1,1,'','' ,'Risk Factors', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','exams'            ,'1General'       ,'Exams/Tests' ,2,23,1,0,0,'exams'      ,1,1,'','' ,'Exam and test results', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','history_father'   ,'2Family History','Father'                 , 1, 2,1,20,  0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','dc_father'        ,'2Family History','Diagnosis Code'         , 2,15,1, 0,255,'',1,1,'','', '', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','history_mother'   ,'2Family History','Mother'                 , 3, 2,1,20,  0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','dc_mother'        ,'2Family History','Diagnosis Code'         , 4,15,1, 0,255,'',1,1,'','', '', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','history_siblings' ,'2Family History','Siblings'               , 5, 2,1,20,  0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','dc_siblings'      ,'2Family History','Diagnosis Code'         , 6,15,1, 0,255,'',1,1,'','', '', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','history_spouse'   ,'2Family History','Spouse'                 , 7, 2,1,20,  0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','dc_spouse'        ,'2Family History','Diagnosis Code'         , 8,15,1, 0,255,'',1,1,'','', '', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','history_offspring','2Family History','Offspring'              , 9, 2,1,20,  0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','dc_offspring'     ,'2Family History','Diagnosis Code'         ,10,15,1, 0,255,'',1,1,'','', '', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','relatives_cancer'             ,'3Relatives','Cancer'             ,1, 2,1,20,0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','relatives_tuberculosis'       ,'3Relatives','Tuberculosis'       ,2, 2,1,20,0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','relatives_diabetes'           ,'3Relatives','Diabetes'           ,3, 2,1,20,0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','relatives_high_blood_pressure','3Relatives','High Blood Pressure',4, 2,1,20,0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','relatives_heart_problems'     ,'3Relatives','Heart Problems'     ,5, 2,1,20,0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','relatives_stroke'             ,'3Relatives','Stroke'             ,6, 2,1,20,0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','relatives_epilepsy'           ,'3Relatives','Epilepsy'           ,7, 2,1,20,0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','relatives_mental_illness'     ,'3Relatives','Mental Illness'     ,8, 2,1,20,0,'',1,1,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','relatives_suicide'            ,'3Relatives','Suicide'            ,9, 2,1,20,0,'',1,3,'','' ,'', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','coffee'              ,'4Lifestyle','Coffee'              ,2,28,1,20,0,'',1,3,'','' ,'Caffeine consumption', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','tobacco'             ,'4Lifestyle','Tobacco'             ,1,32,1,0,0,'smoking_status',1,3,'','' ,'Tobacco use', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','alcohol'             ,'4Lifestyle','Alcohol'             ,3,28,1,20,0,'',1,3,'','' ,'Alcohol consumption', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','recreational_drugs'  ,'4Lifestyle','Recreational Drugs'  ,4,28,1,20,0,'',1,3,'','' ,'Recreational drug use', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','counseling'          ,'4Lifestyle','Counseling'          ,5,28,1,20,0,'',1,3,'','' ,'Counseling activities', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','exercise_patterns'   ,'4Lifestyle','Exercise Patterns'   ,6,28,1,20,0,'',1,3,'','' ,'Exercise patterns', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','hazardous_activities','4Lifestyle','Hazardous Activities',7,28,1,20,0,'',1,3,'','' ,'Hazardous activities', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','sleep_patterns'      ,'4Lifestyle','Sleep Patterns'      ,8, 2,1,20,0,'',1,3,'','' ,'Sleep patterns', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','seatbelt_use'        ,'4Lifestyle','Seatbelt Use'        ,9, 2,1,20,0,'',1,3,'','' ,'Seatbelt use', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','name_1'            ,'5Other','Name/Value'        ,1, 2,1,10,255,'',1,1,'','' ,'Name 1', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','value_1'           ,'5Other',''                  ,2, 2,1,10,255,'',0,0,'','' ,'Value 1', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','name_2'            ,'5Other','Name/Value'        ,3, 2,1,10,255,'',1,1,'','' ,'Name 2', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','value_2'           ,'5Other',''                  ,4, 2,1,10,255,'',0,0,'','' ,'Value 2', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','additional_history','5Other','Additional History',5, 3,1,30,  0,'',1,3,'' ,'' ,'Additional history notes', 3);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','userarea11'        ,'5Other','User Defined Area 11',6,3,0,30,0,'',1,3,'','','User Defined', 3);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('HIS','userarea12'        ,'5Other','User Defined Area 12',7,3,0,30,0,'',1,3,'','','User Defined', 3);

INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('FACUSR', 'provider_id', '1General', 'Provider ID', 1, 2, 1, 15, 63, '', 1, 1, '', '', 'Provider ID at Specified Facility', 0);

INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'guardiansname'  , '8Guardian', 'Name'  ,10, 2, 1,25,63, '', 1, 1, '', '', 'Guardian Name', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'guardianrelationship'  , '8Guardian', 'Relationship'  ,20, 1, 1,0,0, 'next_of_kin_relationship', 1, 1, '', '', 'Relationship', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'guardiansex'  , '8Guardian', 'Sex'  ,30, 1, 1,0,0, 'sex', 1, 1, '', '', 'Sex', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'guardianaddress'  , '8Guardian', 'Address'  ,40, 2, 1,25,63, '', 1, 1, '', '', 'Address', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'guardiancity'  , '8Guardian', 'City'  ,50, 2, 1,15,63, '', 1, 1, '', '', 'City', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'guardianstate'  , '8Guardian', 'State'  ,60, 26, 1,0,0, 'state', 1, 1, '', '', 'State', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'guardianpostalcode'  , '8Guardian', 'Postal Code'  ,70, 2, 1,6,63, '', 1, 1, '', '', 'Postal Code', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'guardiancountry'  , '8Guardian', 'Country'  ,80, 26, 1,0,0, 'country', 1, 1, '', '', 'Country', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'guardianphone'  , '8Guardian', 'Phone'  ,90, 2, 1,20,63, '', 1, 1, '', '', 'Phone', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'guardianworkphone'  , '8Guardian', 'Work Phone'  ,100, 2, 1,20,63, '', 1, 1, '', '', 'Work Phone', 0);
INSERT INTO `layout_options` (`form_id`,`field_id`,`group_name`,`title`,`seq`,`data_type`,`uor`,`fld_length`,`max_length`,`list_id`,`titlecols`,`datacols`,`default_value`,`edit_options`,`description`,`fld_rows`) VALUES ('DEM', 'guardianemail'  , '8Guardian', 'Email'  ,110, 2, 1,20,63, '', 1, 1, '', '', 'Guardian Email Address', 0);
-- --------------------------------------------------------

--
-- Table structure for table `list_options`
--

DROP TABLE IF EXISTS `list_options`;
CREATE TABLE `list_options` (
  `list_id` varchar(31) NOT NULL default '',
  `option_id` varchar(31) NOT NULL default '',
  `title` varchar(255) NOT NULL default '',
  `seq` int(11) NOT NULL default '0',
  `is_default` tinyint(1) NOT NULL default '0',
  `option_value` float NOT NULL default '0',
  `mapping` varchar(31) NOT NULL DEFAULT '',
  `notes` TEXT,
  `codes` varchar(255) NOT NULL DEFAULT '',
  `toggle_setting_1` tinyint(1) NOT NULL default '0',
  `toggle_setting_2` tinyint(1) NOT NULL default '0',
  `activity` TINYINT DEFAULT 1 NOT NULL,
  `subtype` varchar(31) NOT NULL DEFAULT '',
  PRIMARY KEY  (`list_id`,`option_id`)
) ENGINE=InnoDB;

--
-- Dumping data for table `list_options`
--

INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('yesno', 'NO', 'NO', 1, 0, 'N');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('yesno', 'YES', 'YES', 2, 0, 'Y');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('titles', 'Mr.', 'Mr.', 1, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('titles', 'Mrs.', 'Mrs.', 2, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('titles', 'Ms.', 'Ms.', 3, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('titles', 'Dr.', 'Dr.', 4, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('sex', 'Female', 'Female', 1, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('sex', 'Male', 'Male', 2, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('marital', 'married', 'Married', 1, 0, 'M');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('marital', 'single', 'Single', 2, 0, 'S');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('marital', 'divorced', 'Divorced', 3, 0, 'D');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('marital', 'widowed', 'Widowed', 4, 0, 'W');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('marital', 'separated', 'Separated', 5, 0, 'L');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('marital', 'domestic partner', 'Domestic Partner', 6, 0, 'T');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value ) VALUES ('language', 'declne_to_specfy', 'Declined To Specify', 0, 0, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'abkhazian', 'Abkhazian', 10, 0, 0, 'abk');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'afar', 'Afar', 20, 0, 0, 'aar');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'afrikaans', 'Afrikaans', 30, 0, 0, 'afr');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'akan', 'Akan', 40, 0, 0, 'aka');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'albanian', 'Albanian', 50, 0, 0, 'alb(B)|sqi(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'amharic', 'Amharic', 60, 0, 0, 'amh');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'arabic', 'Arabic', 70, 0, 0, 'ara');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'aragonese', 'Aragonese', 80, 0, 0, 'arg');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'armenian', 'Armenian', 90, 0, 0, 'arm(B)|hye(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'assamese', 'Assamese', 100, 0, 0, 'asm');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'avaric', 'Avaric', 110, 0, 0, 'ava');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'avestan', 'Avestan', 120, 0, 0, 'ave');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'aymara', 'Aymara', 130, 0, 0, 'aym');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'azerbaijani', 'Azerbaijani', 140, 0, 0, 'aze');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'bambara', 'Bambara', 150, 0, 0, 'bam');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'bashkir', 'Bashkir', 160, 0, 0, 'bak');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'basque', 'Basque', 170, 0, 0, 'baq(B)|eus(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'belarusian', 'Belarusian', 180, 0, 0, 'bel');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'bengali', 'Bengali', 190, 0, 0, 'ben');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'bihari_languages', 'Bihari languages', 200, 0, 0, 'bih');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'bislama', 'Bislama', 210, 0, 0, 'bis');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'bokmal_norwegian_norwegian_bok', 'Bokmål, Norwegian; Norwegian Bokmål', 220, 0, 0, 'nob');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'bosnian', 'Bosnian', 230, 0, 0, 'bos');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'breton', 'Breton', 240, 0, 0, 'bre');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'bulgarian', 'Bulgarian', 250, 0, 0, 'bul');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'burmese', 'Burmese', 260, 0, 0, 'bur(B)|mya(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'catalan_valencian', 'Catalan; Valencian', 270, 0, 0, 'cat');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'central_khmer', 'Central Khmer', 280, 0, 0, 'khm');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'chamorro', 'Chamorro', 290, 0, 0, 'cha');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'chechen', 'Chechen', 300, 0, 0, 'che');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'chichewa_chewa_nyanja', 'Chichewa; Chewa; Nyanja', 310, 0, 0, 'nya');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'chinese', 'Chinese', 320, 0, 0, 'chi(B)|zho(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'church_slavic_old_slavonic_chu', 'Church Slavic; Old Slavonic; Church Slavonic; Old Bulgarian; Old Church Slavonic', 330, 0, 0, 'chu');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'chuvash', 'Chuvash', 340, 0, 0, 'chv');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'cornish', 'Cornish', 350, 0, 0, 'cor');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'corsican', 'Corsican', 360, 0, 0, 'cos');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'cree', 'Cree', 370, 0, 0, 'cre');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'croatian', 'Croatian', 380, 0, 0, 'hrv');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'czech', 'Czech', 390, 0, 0, 'cze(B)|ces(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'danish', 'Danish', 400, 0, 0, 'dan');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'divehi_dhivehi_maldivian', 'Divehi; Dhivehi; Maldivian', 410, 0, 0, 'div');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'dutch_flemish', 'Dutch; Flemish', 420, 0, 0, 'dut(B)|nld(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'dzongkha', 'Dzongkha', 430, 0, 0, 'dzo');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'English', 'English', 440, 0, 0, 'eng');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'esperanto', 'Esperanto', 450, 0, 0, 'epo');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'estonian', 'Estonian', 460, 0, 0, 'est');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'ewe', 'Ewe', 470, 0, 0, 'ewe');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'faroese', 'Faroese', 480, 0, 0, 'fao');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'fijian', 'Fijian', 490, 0, 0, 'fij');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'finnish', 'Finnish', 500, 0, 0, 'fin');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'french', 'French', 510, 0, 0, 'fre(B)|fra(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'fulah', 'Fulah', 520, 0, 0, 'ful');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'gaelic_scottish_gaelic', 'Gaelic; Scottish Gaelic', 530, 0, 0, 'gla');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'galician', 'Galician', 540, 0, 0, 'glg');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'ganda', 'Ganda', 550, 0, 0, 'lug');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'georgian', 'Georgian', 560, 0, 0, 'geo(B)|kat(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'german', 'German', 570, 0, 0, 'ger(B)|deu(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'greek', 'Greek, Modern (1453-)', 580, 0, 0, 'gre(B)|ell(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'guarani', 'Guarani', 590, 0, 0, 'grn');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'gujarati', 'Gujarati', 600, 0, 0, 'guj');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'haitian_haitian_creole', 'Haitian; Haitian Creole', 610, 0, 0, 'hat');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'hausa', 'Hausa', 620, 0, 0, 'hau');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'hebrew', 'Hebrew', 630, 0, 0, 'heb');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'herero', 'Herero', 640, 0, 0, 'her');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'hindi', 'Hindi', 650, 0, 0, 'hin');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'hiri_motu', 'Hiri Motu', 660, 0, 0, 'hmo');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'hungarian', 'Hungarian', 670, 0, 0, 'hun');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'icelandic', 'Icelandic', 680, 0, 0, 'ice(B)|isl(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'ido', 'Ido', 690, 0, 0, 'ido');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'igbo', 'Igbo', 700, 0, 0, 'ibo');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'indonesian', 'Indonesian', 710, 0, 0, 'ind');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'interlingua_international_auxi', 'Interlingua (International Auxiliary Language Association)', 720, 0, 0, 'ina');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'interlingue_occidental', 'Interlingue; Occidental', 730, 0, 0, 'ile');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'inuktitut', 'Inuktitut', 740, 0, 0, 'iku');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'inupiaq', 'Inupiaq', 750, 0, 0, 'ipk');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'irish', 'Irish', 760, 0, 0, 'gle');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'italian', 'Italian', 770, 0, 0, 'ita');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'japanese', 'Japanese', 780, 0, 0, 'jpn');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'javanese', 'Javanese', 790, 0, 0, 'jav');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'kalaallisut_greenlandic', 'Kalaallisut; Greenlandic', 800, 0, 0, 'kal');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'kannada', 'Kannada', 810, 0, 0, 'kan');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'kanuri', 'Kanuri', 820, 0, 0, 'kau');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'kashmiri', 'Kashmiri', 830, 0, 0, 'kas');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'kazakh', 'Kazakh', 840, 0, 0, 'kaz');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'kikuyu_gikuyu', 'Kikuyu; Gikuyu', 850, 0, 0, 'kik');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'kinyarwanda', 'Kinyarwanda', 860, 0, 0, 'kin');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'kirghiz_kyrgyz', 'Kirghiz; Kyrgyz', 870, 0, 0, 'kir');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'komi', 'Komi', 880, 0, 0, 'kom');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'kongo', 'Kongo', 890, 0, 0, 'kon');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'korean', 'Korean', 900, 0, 0, 'kor');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'kuanyama_kwanyama', 'Kuanyama; Kwanyama', 910, 0, 0, 'kua');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'kurdish', 'Kurdish', 920, 0, 0, 'kur');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'laotian', 'Lao', 930, 0, 0, 'lao');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'latin', 'Latin', 940, 0, 0, 'lat');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'latvian', 'Latvian', 950, 0, 0, 'lav');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'limburgan_limburger_limburgish', 'Limburgan; Limburger; Limburgish', 960, 0, 0, 'lim');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'lingala', 'Lingala', 970, 0, 0, 'lin');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'lithuanian', 'Lithuanian', 980, 0, 0, 'lit');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'luba-katanga', 'Luba-Katanga', 990, 0, 0, 'lub');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'luxembourgish_letzeburgesch', 'Luxembourgish; Letzeburgesch', 1000, 0, 0, 'ltz');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'macedonian', 'Macedonian', 1010, 0, 0, 'mac(B)|mkd(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'malagasy', 'Malagasy', 1020, 0, 0, 'mlg');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'malay', 'Malay', 1030, 0, 0, 'may(B)|msa(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'malayalam', 'Malayalam', 1040, 0, 0, 'mal');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'maltese', 'Maltese', 1050, 0, 0, 'mlt');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'manx', 'Manx', 1060, 0, 0, 'glv');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'maori', 'Maori', 1070, 0, 0, 'mao(B)|mri(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'marathi', 'Marathi', 1080, 0, 0, 'mar');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'marshallese', 'Marshallese', 1090, 0, 0, 'mah');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'mongolian', 'Mongolian', 1100, 0, 0, 'mon');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'nauru', 'Nauru', 1110, 0, 0, 'nau');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'navajo_navaho', 'Navajo; Navaho', 1120, 0, 0, 'nav');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'ndebele_north_north_ndebele', 'Ndebele, North; North Ndebele', 1130, 0, 0, 'nde');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'ndebele_south_south_ndebele', 'Ndebele, South; South Ndebele', 1140, 0, 0, 'nbl');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'ndonga', 'Ndonga', 1150, 0, 0, 'ndo');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'nepali', 'Nepali', 1160, 0, 0, 'nep');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'northern_sami', 'Northern Sami', 1170, 0, 0, 'sme');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'norwegian', 'Norwegian', 1180, 0, 0, 'nor');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'norwegian_nynorsk_nynorsk_norw', 'Norwegian Nynorsk; Nynorsk, Norwegian', 1190, 0, 0, 'nno');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'occitan_post_1500', 'Occitan (post 1500)', 1200, 0, 0, 'oci');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'ojibwa', 'Ojibwa', 1210, 0, 0, 'oji');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'oriya', 'Oriya', 1220, 0, 0, 'ori');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'oromo', 'Oromo', 1230, 0, 0, 'orm');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'ossetian_ossetic', 'Ossetian; Ossetic', 1240, 0, 0, 'oss');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'pali', 'Pali', 1250, 0, 0, 'pli');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'persian', 'Persian', 1260, 0, 0, 'per(B)|fas(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'polish', 'Polish', 1270, 0, 0, 'pol');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'portuguese', 'Portuguese', 1280, 0, 0, 'por');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'punjabi', 'Punjabi', 1290, 0, 0, 'pan');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'pushto_pashto', 'Pushto; Pashto', 1300, 0, 0, 'pus');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'quechua', 'Quechua', 1310, 0, 0, 'que');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'romanian_moldavian_moldovan', 'Romanian; Moldavian; Moldovan', 1320, 0, 0, 'rum(B)|ron(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'romansh', 'Romansh', 1330, 0, 0, 'roh');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'rundi', 'Rundi', 1340, 0, 0, 'run');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'russian', 'Russian', 1350, 0, 0, 'rus');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'samoan', 'Samoan', 1360, 0, 0, 'smo');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'sango', 'Sango', 1370, 0, 0, 'sag');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'sanskrit', 'Sanskrit', 1380, 0, 0, 'san');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'sardinian', 'Sardinian', 1390, 0, 0, 'srd');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'serbian', 'Serbian', 1400, 0, 0, 'srp');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'shona', 'Shona', 1410, 0, 0, 'sna');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'sichuan_yi_nuosu', 'Sichuan Yi; Nuosu', 1420, 0, 0, 'iii');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'sindhi', 'Sindhi', 1430, 0, 0, 'snd');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'sinhala_sinhalese', 'Sinhala; Sinhalese', 1440, 0, 0, 'sin');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'slovak', 'Slovak', 1450, 0, 0, 'slo(B)|slk(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'slovenian', 'Slovenian', 1460, 0, 0, 'slv');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'somali', 'Somali', 1470, 0, 0, 'som');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'sotho_southern', 'Sotho, Southern', 1480, 0, 0, 'sot');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'Spanish', 'Spanish', 1490, 0, 0, 'spa');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'sundanese', 'Sundanese', 1500, 0, 0, 'sun');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'swahili', 'Swahili', 1510, 0, 0, 'swa');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'swati', 'Swati', 1520, 0, 0, 'ssw');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'swedish', 'Swedish', 1530, 0, 0, 'swe');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'tagalog', 'Tagalog', 1540, 0, 0, 'tgl');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'tahitian', 'Tahitian', 1550, 0, 0, 'tah');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'tajik', 'Tajik', 1560, 0, 0, 'tgk');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'tamil', 'Tamil', 1570, 0, 0, 'tam');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'tatar', 'Tatar', 1580, 0, 0, 'tat');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'telugu', 'Telugu', 1590, 0, 0, 'tel');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'thai', 'Thai', 1600, 0, 0, 'tha');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'tibetan', 'Tibetan', 1610, 0, 0, 'tib(B)|bod(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'tigrinya', 'Tigrinya', 1620, 0, 0, 'tir');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'tonga_tonga_islands', 'Tonga (Tonga Islands)', 1630, 0, 0, 'ton');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'tsonga', 'Tsonga', 1640, 0, 0, 'tso');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'tswana', 'Tswana', 1650, 0, 0, 'tsn');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'turkish', 'Turkish', 1660, 0, 0, 'tur');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'turkmen', 'Turkmen', 1670, 0, 0, 'tuk');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'twi', 'Twi', 1680, 0, 0, 'twi');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'uighur_uyghur', 'Uighur; Uyghur', 1690, 0, 0, 'uig');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'ukrainian', 'Ukrainian', 1700, 0, 0, 'ukr');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'urdu', 'Urdu', 1710, 0, 0, 'urd');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'uzbek', 'Uzbek', 1720, 0, 0, 'uzb');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'venda', 'Venda', 1730, 0, 0, 'ven');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'vietnamese', 'Vietnamese', 1740, 0, 0, 'vie');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'volapuk', 'Volapük', 1750, 0, 0, 'vol');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'walloon', 'Walloon', 1760, 0, 0, 'wln');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'welsh', 'Welsh', 1770, 0, 0, 'wel(B)|cym(T)');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'western_frisian', 'Western Frisian', 1780, 0, 0, 'fry');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'wolof', 'Wolof', 1790, 0, 0, 'wol');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'xhosa', 'Xhosa', 1800, 0, 0, 'xho');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'yiddish', 'Yiddish', 1810, 0, 0, 'yid');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'yoruba', 'Yoruba', 1820, 0, 0, 'yor');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'zhuang_chuang', 'Zhuang; Chuang', 1830, 0, 0, 'zha');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value, notes ) VALUES ('language', 'zulu', 'Zulu', 1840, 0, 0, 'zul');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value ) VALUES ('ethrace', 'declne_to_specfy', 'Declined To Specify', 0, 0, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'aleut', 'ALEUT', 10,  0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'amer_indian', 'American Indian', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'Asian', 'Asian', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'Black', 'Black', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'cambodian', 'Cambodian', 50, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'Caucasian', 'Caucasian', 60, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'cs_american', 'Central/South American', 70, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'chinese', 'Chinese', 80, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'cuban', 'Cuban', 90, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'eskimo', 'Eskimo', 100, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'filipino', 'Filipino', 110, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'guamanian', 'Guamanian', 120, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'hawaiian', 'Hawaiian', 130, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'Hispanic', 'Hispanic', 140, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'othr_us', 'Hispanic - Other (Born in US)', 150, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'othr_non_us', 'Hispanic - Other (Born outside US)', 160, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'hmong', 'Hmong', 170, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'indian', 'Indian', 180, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'japanese', 'Japanese', 190, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'korean', 'Korean', 200, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'laotian', 'Laotian', 210, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'mexican', 'Mexican/MexAmer/Chicano', 220, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'mlt-race', 'Multiracial', 230, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'othr', 'Other', 240, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'othr_spec', 'Other - Specified', 250, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'pac_island', 'Pacific Islander', 260, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'puerto_rican', 'Puerto Rican', 270, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'refused', 'Refused To State', 280, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'samoan', 'Samoan', 290, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'spec', 'Specified', 300, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'thai', 'Thai', 310, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'unknown', 'Unknown', 320, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'unspec', 'Unspecified', 330, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'vietnamese', 'Vietnamese', 340, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'white', 'White', 350, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'withheld', 'Withheld', 360, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('userlist1', 'sample', 'Sample', 1, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('userlist2', 'sample', 'Sample', 1, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('userlist3','sample','Sample',1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('userlist4','sample','Sample',1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('userlist5','sample','Sample',1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('userlist6','sample','Sample',1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('userlist7','sample','Sample',1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('pricelevel', 'standard', 'Standard', 1, 1);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('risklevel', 'low', 'Low', 1, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('risklevel', 'medium', 'Medium', 2, 1);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('risklevel', 'high', 'High', 3, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('boolean', '0', 'No', 1, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('boolean', '1', 'Yes', 2, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('country', 'USA', 'USA', 1, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','AL','Alabama'             , 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','AK','Alaska'              , 2,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','AZ','Arizona'             , 3,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','AR','Arkansas'            , 4,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','CA','California'          , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','CO','Colorado'            , 6,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','CT','Connecticut'         , 7,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','DE','Delaware'            , 8,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','DC','District of Columbia', 9,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','FL','Florida'             ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','GA','Georgia'             ,11,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','HI','Hawaii'              ,12,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','ID','Idaho'               ,13,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','IL','Illinois'            ,14,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','IN','Indiana'             ,15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','IA','Iowa'                ,16,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','KS','Kansas'              ,17,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','KY','Kentucky'            ,18,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','LA','Louisiana'           ,19,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','ME','Maine'               ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','MD','Maryland'            ,21,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','MA','Massachusetts'       ,22,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','MI','Michigan'            ,23,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','MN','Minnesota'           ,24,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','MS','Mississippi'         ,25,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','MO','Missouri'            ,26,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','MT','Montana'             ,27,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','NE','Nebraska'            ,28,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','NV','Nevada'              ,29,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','NH','New Hampshire'       ,30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','NJ','New Jersey'          ,31,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','NM','New Mexico'          ,32,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','NY','New York'            ,33,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','NC','North Carolina'      ,34,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','ND','North Dakota'        ,35,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','OH','Ohio'                ,36,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','OK','Oklahoma'            ,37,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','OR','Oregon'              ,38,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','PA','Pennsylvania'        ,39,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','RI','Rhode Island'        ,40,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','SC','South Carolina'      ,41,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','SD','South Dakota'        ,42,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','TN','Tennessee'           ,43,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','TX','Texas'               ,44,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','UT','Utah'                ,45,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','VT','Vermont'             ,46,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','VA','Virginia'            ,47,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','WA','Washington'          ,48,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','WV','West Virginia'       ,49,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','WI','Wisconsin'           ,50,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('state','WY','Wyoming'             ,51,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('refsource','Patient'      ,'Patient'      , 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('refsource','Employee'     ,'Employee'     , 2,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('refsource','Walk-In'      ,'Walk-In'      , 3,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('refsource','Newspaper'    ,'Newspaper'    , 4,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('refsource','Radio'        ,'Radio'        , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('refsource','T.V.'         ,'T.V.'         , 6,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('refsource','Direct Mail'  ,'Direct Mail'  , 7,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('refsource','Coupon'       ,'Coupon'       , 8,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('refsource','Referral Card','Referral Card', 9,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('refsource','Other'        ,'Other'        ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','vv' ,'Varicose Veins'                      , 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','ht' ,'Hypertension'                        , 2,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','db' ,'Diabetes'                            , 3,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','sc' ,'Sickle Cell'                         , 4,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','fib','Fibroids'                            , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','pid','PID (Pelvic Inflammatory Disease)'   , 6,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','mig','Severe Migraine'                     , 7,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','hd' ,'Heart Disease'                       , 8,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','str','Thrombosis/Stroke'                   , 9,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','hep','Hepatitis'                           ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','gb' ,'Gall Bladder Condition'              ,11,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','br' ,'Breast Disease'                      ,12,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','dpr','Depression'                          ,13,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','all','Allergies'                           ,14,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','inf','Infertility'                         ,15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','ast','Asthma'                              ,16,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','ep' ,'Epilepsy'                            ,17,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','cl' ,'Contact Lenses'                      ,18,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','coc','Contraceptive Complication (specify)',19,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('riskfactors','oth','Other (specify)'                     ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'brs','Breast Exam'          , 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'cec','Cardiac Echo'         , 2,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'ecg','ECG'                  , 3,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'gyn','Gynecological Exam'   , 4,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'mam','Mammogram'            , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'phy','Physical Exam'        , 6,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'pro','Prostate Exam'        , 7,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'rec','Rectal Exam'          , 8,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'sic','Sigmoid/Colonoscopy'  , 9,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'ret','Retinal Exam'         ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'flu','Flu Vaccination'      ,11,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'pne','Pneumonia Vaccination',12,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'ldl','LDL'                  ,13,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'hem','Hemoglobin'           ,14,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('exams' ,'psa','PSA'                  ,15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_form','0',''           ,0,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','1','suspension' ,1,0,'NCI-CONCEPT-ID:C60928');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','2','tablet'     ,2,0,'NCI-CONCEPT-ID:C42998');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','3','capsule'    ,3,0,'NCI-CONCEPT-ID:C25158');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','4','solution'   ,4,0,'NCI-CONCEPT-ID:C42986');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','5','tsp'        ,5,0,'NCI-CONCEPT-ID:C48544');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','6','ml'         ,6,0,'NCI-CONCEPT-ID:C28254');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','7','units'      ,7,0,'NCI-CONCEPT-ID:C44278');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','8','inhalations',8,0,'NCI-CONCEPT-ID:C42944');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','9','gtts(drops)',9,0,'NCI-CONCEPT-ID:C48491');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','10','cream'   ,10,0,'NCI-CONCEPT-ID:C28944');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','11','ointment',11,0,'NCI-CONCEPT-ID:C42966');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_form','12','puff',12,0,'NCI-CONCEPT-ID:C42944');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_units','0',''          ,0,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('drug_units','1','mg'    ,1,0,'NCI-CONCEPT-ID:C28253');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_units','2','mg/1cc',2,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_units','3','mg/2cc',3,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_units','4','mg/3cc',4,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_units','5','mg/4cc',5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_units','6','mg/5cc',6,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_units','7','mcg'   ,7,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_units','8','grams' ,8,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_units','9','mL' ,9,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_route', '0',''                 , 0,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes, codes ) VALUES ('drug_route', '1','Per Oris'         , 1,0, 'PO', 'NCI-CONCEPT-ID:C38288');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route', '2','Per Rectum'       , 2,0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route', '3','To Skin'          , 3,0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route', '4','To Affected Area' , 4,0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route', '5','Sublingual'       , 5,0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route', '6','OS'               , 6,0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route', '7','OD'               , 7,0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route', '8','OU'               , 8,0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route', '9','SQ'               , 9,0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route','10','IM'               ,10,0, 'IM');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route','11','IV'               ,11,0, 'IV');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route','12','Per Nostril'      ,12,0, 'NS');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route','13','Both Ears',13,0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route','14','Left Ear' ,14,0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route','15','Right Ear',15,0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route', 'intradermal', 'Intradermal', 16, 0, 'ID');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route', 'other', 'Other/Miscellaneous', 18, 0, 'OTH');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('drug_route', 'transdermal', 'Transdermal', 19, 0, 'TD');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes, codes ) VALUES ('drug_route','intramuscular','Intramuscular' ,20, 0, 'IM', 'NCI-CONCEPT-ID:C28161');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes, codes ) VALUES ('drug_route','inhale','Inhale' ,16, 0, 'RESPIR', 'NCI-CONCEPT-ID:C38216');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','0',''      ,0,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','1','b.i.d.',1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','2','t.i.d.',2,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','3','q.i.d.',3,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','4','q.3h'  ,4,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','5','q.4h'  ,5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','6','q.5h'  ,6,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','7','q.6h'  ,7,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','8','q.8h'  ,8,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','9','q.d.'  ,9,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','10','a.c.'  ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','11','p.c.'  ,11,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','12','a.m.'  ,12,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','13','p.m.'  ,13,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','14','ante'  ,14,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','15','h'     ,15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','16','h.s.'  ,16,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','17','p.r.n.',17,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('drug_interval','18','stat'  ,18,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('chartloc','fileroom','File Room'              ,1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'boolean'      ,'Boolean'            , 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'chartloc'     ,'Chart Storage Locations',1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'country'      ,'Country'            , 2,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'drug_form'    ,'Drug Forms'         , 3,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'drug_units'   ,'Drug Units'         , 4,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'drug_route'   ,'Drug Routes'        , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'drug_interval','Drug Intervals'     , 6,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'exams'        ,'Exams/Tests'        , 7,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'feesheet'     ,'Fee Sheet'          , 8,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'language'     ,'Language'           , 9,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'lbfnames'     ,'Layout-Based Visit Forms',9,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'marital'      ,'Marital Status'     ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'pricelevel'   ,'Price Level'        ,11,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'ethrace'      ,'Race/Ethnicity'     ,12,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'refsource'    ,'Referral Source'    ,13,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'riskfactors'  ,'Risk Factors'       ,14,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'risklevel'    ,'Risk Level'         ,15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'superbill'    ,'Service Category'   ,16,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'sex'          ,'Sex'                ,17,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'state'        ,'State'              ,18,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'taxrate'      ,'Tax Rate'           ,19,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'titles'       ,'Titles'             ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'yesno'        ,'Yes/No'             ,21,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'userlist1'    ,'User Defined List 1',22,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'userlist2'    ,'User Defined List 2',23,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'userlist3'    ,'User Defined List 3',24,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'userlist4'    ,'User Defined List 4',25,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'userlist5'    ,'User Defined List 5',26,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'userlist6'    ,'User Defined List 6',27,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'userlist7'    ,'User Defined List 7',28,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'    ,'adjreason'      ,'Adjustment Reasons',1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Adm adjust'     ,'Adm adjust'     , 5,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','After hrs calls','After hrs calls',10,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Bad check'      ,'Bad check'      ,15,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Bad debt'       ,'Bad debt'       ,20,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Coll w/o'       ,'Coll w/o'       ,25,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Discount'       ,'Discount'       ,30,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Hardship w/o'   ,'Hardship w/o'   ,35,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Ins adjust'     ,'Ins adjust'     ,40,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Ins bundling'   ,'Ins bundling'   ,45,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Ins overpaid'   ,'Ins overpaid'   ,50,5);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Ins refund'     ,'Ins refund'     ,55,5);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Pt overpaid'    ,'Pt overpaid'    ,60,5);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Pt refund'      ,'Pt refund'      ,65,5);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Pt released'    ,'Pt released'    ,70,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Sm debt w/o'    ,'Sm debt w/o'    ,75,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','To copay'       ,'To copay'       ,80,2);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','To ded\'ble'    ,'To ded\'ble'    ,85,3);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('adjreason','Untimely filing','Untimely filing',90,1);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'       ,'sub_relation','Subscriber Relationship',18,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('sub_relation','self'        ,'Self'                   , 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('sub_relation','spouse'      ,'Spouse'                 , 2,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('sub_relation','child'       ,'Child'                  , 3,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('sub_relation','other'       ,'Other'                  , 4,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'     ,'occurrence','Occurrence'                  ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('occurrence','0'         ,'Unknown or N/A'              , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('occurrence','1'         ,'First'                       ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('occurrence','6'         ,'Early Recurrence (<2 Mo)'    ,15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('occurrence','7'         ,'Late Recurrence (2-12 Mo)'   ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('occurrence','8'         ,'Delayed Recurrence (> 12 Mo)',25,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('occurrence','4'         ,'Chronic/Recurrent'           ,30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('occurrence','5'         ,'Acute on Chronic'            ,35,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'  ,'outcome','Outcome'         ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('outcome','0'      ,'Unassigned'      , 2,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('outcome','1'      ,'Resolved'        , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('outcome','2'      ,'Improved'        ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('outcome','3'      ,'Status quo'      ,15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('outcome','4'      ,'Worse'           ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('outcome','5'      ,'Pending followup',25,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'    ,'note_type'      ,'Patient Note Types',10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Unassigned'     ,'Unassigned'        , 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Chart Note'     ,'Chart Note'        , 2,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Insurance'      ,'Insurance'         , 3,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','New Document'   ,'New Document'      , 4,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Pharmacy'       ,'Pharmacy'          , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Prior Auth'     ,'Prior Auth'        , 6,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Referral'       ,'Referral'          , 7,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Test Scheduling','Test Scheduling'   , 8,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Bill/Collect'   ,'Bill/Collect'      , 9,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Other'          ,'Other'             ,10,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'        ,'immunizations','Immunizations'           ,  8,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','1'            ,'DTaP 1'                  , 30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','2'            ,'DTaP 2'                  , 35,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','3'            ,'DTaP 3'                  , 40,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','4'            ,'DTaP 4'                  , 45,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','5'            ,'DTaP 5'                  , 50,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','6'            ,'DT 1'                    ,  5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','7'            ,'DT 2'                    , 10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','8'            ,'DT 3'                    , 15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','9'            ,'DT 4'                    , 20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','10'           ,'DT 5'                    , 25,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','11'           ,'IPV 1'                   ,110,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','12'           ,'IPV 2'                   ,115,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','13'           ,'IPV 3'                   ,120,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','14'           ,'IPV 4'                   ,125,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','15'           ,'Hib 1'                   , 80,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','16'           ,'Hib 2'                   , 85,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','17'           ,'Hib 3'                   , 90,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','18'           ,'Hib 4'                   , 95,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','19'           ,'Pneumococcal Conjugate 1',140,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','20'           ,'Pneumococcal Conjugate 2',145,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','21'           ,'Pneumococcal Conjugate 3',150,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','22'           ,'Pneumococcal Conjugate 4',155,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','23'           ,'MMR 1'                   ,130,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','24'           ,'MMR 2'                   ,135,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','25'           ,'Varicella 1'             ,165,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','26'           ,'Varicella 2'             ,170,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','27'           ,'Hepatitis B 1'           , 65,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','28'           ,'Hepatitis B 2'           , 70,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','29'           ,'Hepatitis B 3'           , 75,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','30'           ,'Influenza 1'             ,100,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','31'           ,'Influenza 2'             ,105,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','32'           ,'Td'                      ,160,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','33'           ,'Hepatitis A 1'           , 55,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','34'           ,'Hepatitis A 2'           , 60,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('immunizations','35'           ,'Other'                   ,175,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'apptstat','Appointment Statuses', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('apptstat','-'       ,'- None'              , 5,0,'FEFDCF|0');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('apptstat','*'       ,'* Reminder done'     ,10,0,'FFC9F8|0');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('apptstat','+'       ,'+ Chart pulled'      ,15,0,'87FF1F|0');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('apptstat','x'       ,'x Canceled'          ,20,0,'BFBFBF|0');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('apptstat','?'       ,'? No show'           ,25,0,'BFBFBF|0');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes, toggle_setting_1 ) VALUES ('apptstat','@'       ,'@ Arrived'           ,30,0,'FF2414|10','1');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes, toggle_setting_1 ) VALUES ('apptstat','~'       ,'~ Arrived late'      ,35,0,'FF6619|10','1');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes, toggle_setting_2 ) VALUES ('apptstat','!'       ,'! Left w/o visit'    ,40,0,'0BBA34|0','1');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('apptstat','#'       ,'# Ins/fin issue'     ,45,0,'FFFF2B|0');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('apptstat','<'       ,'< In exam room'      ,50,0,'52D9DE|10');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes, toggle_setting_2 ) VALUES ('apptstat','>'       ,'> Checked out'       ,55,0,'FEFDCF|0','1');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('apptstat','$'       ,'$ Coding done'       ,60,0,'C0FF96|0');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('apptstat','%'       ,'% Canceled < 24h'    ,65,0,'BFBFBF|0');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'    ,'warehouse','Warehouses',21,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('warehouse','onsite'   ,'On Site'   , 5,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','abook_type'  ,'Address Book Types'  , 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('abook_type','ord_img','Imaging Service'     , 5,3);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('abook_type','ord_imm','Immunization Service',10,3);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('abook_type','ord_lab','Lab Service'         ,15,3);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('abook_type','spe'    ,'Specialist'          ,20,2);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('abook_type','vendor' ,'Vendor'              ,25,3);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('abook_type','dist'   ,'Distributor'         ,30,3);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('abook_type','oth'    ,'Other'               ,95,1);
INSERT INTO list_options ( list_id, option_id, title, seq, option_value ) VALUES ('abook_type','ccda', 'Care Coordination', 35, 2);
INSERT INTO list_options (list_id, option_id, title , seq, option_value ) VALUES ('abook_type','emr_direct', 'EMR Direct' ,105,4);
INSERT INTO list_options (list_id, option_id, title , seq, option_value ) VALUES ('abook_type','external_provider', 'External Provider' ,110,1);
INSERT INTO list_options (list_id, option_id, title , seq, option_value ) VALUES ('abook_type','external_org', 'External Organization' ,120,1);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_type','Procedure Types', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_type','grp','Group'          ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_type','ord','Procedure Order',20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_type','res','Discrete Result',30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_type','rec','Recommendation' ,40,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_body_site','Procedure Body Sites', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_body_site','arm'    ,'Arm'    ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_body_site','buttock','Buttock',20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_body_site','oth'    ,'Other'  ,90,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_specimen','Procedure Specimen Types', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_specimen','blood' ,'Blood' ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_specimen','saliva','Saliva',20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_specimen','urine' ,'Urine' ,30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_specimen','oth'   ,'Other' ,90,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_route','Procedure Routes', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_route','inj' ,'Injection',10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_route','oral','Oral'     ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_route','oth' ,'Other'    ,90,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_lat','Procedure Lateralities', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_lat','left' ,'Left'     ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_lat','right','Right'    ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_lat','bilat','Bilateral',30,0);

INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('lists','proc_unit','Procedure Units', 1);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','bool'       ,'Boolean'    ,  5);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','cu_mm'      ,'CU.MM'      , 10);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','fl'         ,'FL'         , 20);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','g_dl'       ,'G/DL'       , 30);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','gm_dl'      ,'GM/DL'      , 40);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','hmol_l'     ,'HMOL/L'     , 50);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','iu_l'       ,'IU/L'       , 60);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','mg_dl'      ,'MG/DL'      , 70);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','mil_cu_mm'  ,'Mil/CU.MM'  , 80);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','percent'    ,'Percent'    , 90);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','percentile' ,'Percentile' ,100);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','pg'         ,'PG'         ,110);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','ratio'      ,'Ratio'      ,120);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','thous_cu_mm','Thous/CU.MM',130);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','units'      ,'Units'      ,140);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','units_l'    ,'Units/L'    ,150);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','days'       ,'Days'       ,600);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','weeks'      ,'Weeks'      ,610);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','months'     ,'Months'     ,620);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('proc_unit','oth'        ,'Other'      ,990);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','ord_priority','Order Priorities', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_priority','high'  ,'High'  ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_priority','normal','Normal',20,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','ord_status','Order Statuses', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_status','pending' ,'Pending' ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_status','routed'  ,'Routed'  ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_status','complete','Complete',30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_status','canceled','Canceled',40,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_rep_status','Procedure Report Statuses', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','final'  ,'Final'      ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','review' ,'Reviewed'   ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','prelim' ,'Preliminary',30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','cancel' ,'Canceled'   ,40,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','error'  ,'Error'      ,50,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','correct','Corrected'  ,60,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_res_abnormal','Procedure Result Abnormal', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_abnormal','no'  ,'No'  ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_abnormal','yes' ,'Yes' ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_abnormal','high','High',30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_abnormal','low' ,'Low' ,40,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_abnormal', 'vhigh', 'Above upper panic limits', 50,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_abnormal', 'vlow', 'Below lower panic limits', 60,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_res_status','Procedure Result Statuses', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','final'     ,'Final'      ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','prelim'    ,'Preliminary',20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','cancel'    ,'Canceled'   ,30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','error'     ,'Error'      ,40,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','correct'   ,'Corrected'  ,50,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','incomplete','Incomplete' ,60,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_res_bool','Procedure Boolean Results', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_bool','neg' ,'Negative',10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_bool','pos' ,'Positive',20,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'         ,'message_status','Message Status',45,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('message_status','Done'           ,'Done'         , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('message_status','Forwarded'      ,'Forwarded'    ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('message_status','New'            ,'New'          ,15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('message_status','Read'           ,'Read'         ,20,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Lab Results' ,'Lab Results', 15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','New Orders' ,'New Orders', 20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Patient Reminders' ,'Patient Reminders', 25,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Image Results' ,'Image Results', 30,0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'irnpool','Invoice Reference Number Pools', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('irnpool','main','Main',1,1,'000001');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists', 'eligibility', 'Eligibility', 60, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('eligibility', 'eligible', 'Eligible', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('eligibility', 'ineligible', 'Ineligible', 20, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists', 'transactions', 'Layout-Based Transaction Forms', 9, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('transactions', 'LBTref'  , 'Referral'         , 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('transactions', 'LBTptreq', 'Patient Request'  , 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('transactions', 'LBTphreq', 'Physician Request', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('transactions', 'LBTlegal', 'Legal'            , 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('transactions', 'LBTbill' , 'Billing'          , 50, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_adjustment_code','Payment Adjustment Code', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_adjustment_code', 'family_payment', 'Family Payment', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_adjustment_code', 'group_payment', 'Group Payment', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_adjustment_code', 'insurance_payment', 'Insurance Payment', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_adjustment_code', 'patient_payment', 'Patient Payment', 50, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_adjustment_code', 'pre_payment', 'Pre Payment', 60, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_ins','Payment Ins', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_ins', '0', 'Pat', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_ins', '1', 'Ins1', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_ins', '2', 'Ins2', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_ins', '3', 'Ins3', 30, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_method','Payment Method', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_method', 'bank_draft', 'Bank Draft', 50, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_method', 'cash', 'Cash', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_method', 'check_payment', 'Check Payment', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_method', 'credit_card', 'Credit Card', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_method', 'electronic', 'Electronic', 40, 0);
insert into `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) values('payment_method','authorize_net','Authorize.net','60','0','0','','');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_sort_by','Payment Sort By', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'check_date', 'Check Date', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'payer_id', 'Ins Code', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'payment_method', 'Payment Method', 50, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'payment_type', 'Paying Entity', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'pay_total', 'Amount', 70, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'reference', 'Check Number', 60, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'session_id', 'Id', 10, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_status','Payment Status', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_status', 'fully_paid', 'Fully Paid', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_status', 'unapplied', 'Unapplied', 20, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_type','Payment Type', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_type', 'insurance', 'Insurance', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_type', 'patient', 'Patient', 20, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists', 'date_master_criteria', 'Date Master Criteria', 33, 1);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'all', 'All', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'today', 'Today', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'this_month_to_date', 'This Month to Date', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'last_month', 'Last Month', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'this_week_to_date', 'This Week to Date', 50, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'this_calendar_year', 'This Calendar Year', 60, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'last_calendar_year', 'Last Calendar Year', 70, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'custom', 'Custom', 80, 0);

-- Clinical Plan Titles
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'clinical_plans','Clinical Plans', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_plans', 'dm_plan_cqm', 'Diabetes Mellitus', 5, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_plans', 'ckd_plan_cqm', 'Chronic Kidney Disease (CKD)', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_plans', 'prevent_plan_cqm', 'Preventative Care', 15, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_plans', 'periop_plan_cqm', 'Perioperative Care', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_plans', 'rheum_arth_plan_cqm', 'Rheumatoid Arthritis', 25, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_plans', 'back_pain_plan_cqm', 'Back Pain', 30, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_plans', 'cabg_plan_cqm', 'Coronary Artery Bypass Graft (CABG)', 35, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_plans', 'dm_plan', 'Diabetes Mellitus', 500, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_plans', 'prevent_plan', 'Preventative Care', 510, 0);

-- Clinical Rule Titles
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'clinical_rules','Clinical Rules', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'problem_list_amc', 'Maintain an up-to-date problem list of current and active diagnoses.', 5, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'med_list_amc', 'Maintain active medication list.', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'med_allergy_list_amc', 'Maintain active medication allergy list.', 15, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'record_vitals_amc', 'Record and chart changes in vital signs.', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'record_smoke_amc', 'Record smoking status for patients 13 years old or older.', 25, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'lab_result_amc', 'Incorporate clinical lab-test results into certified EHR technology as structured data.', 30, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'med_reconc_amc', 'The EP, eligible hospital or CAH who receives a patient from another setting of care or provider of care or believes an encounter is relevant should perform medication reconciliation.', 35, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'patient_edu_amc', 'Use certified EHR technology to identify patient-specific education resources and provide those resources to the patient if appropriate.', 40, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'cpoe_med_amc', 'Use CPOE for medication orders directly entered by any licensed healthcare professional who can enter orders into the medical record per state, local and professional guidelines.', 45, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'e_prescribe_amc', 'Generate and transmit permissible prescriptions electronically.', 50, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'record_dem_amc', 'Record demographics.', 55, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'send_reminder_amc', 'Send reminders to patients per patient preference for preventive/follow up care.', 60, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'provide_rec_pat_amc', 'Provide patients with an electronic copy of their health information (including diagnostic test results, problem list, medication lists, medication allergies), upon request.', 65, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'timely_access_amc', 'Provide patients with timely electronic access to their health information (including lab results, problem list, medication lists, medication allergies) within four business days of the information being available to the EP.', 70, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'provide_sum_pat_amc', 'Provide clinical summaries for patients for each office visit.', 75, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'send_sum_amc', 'The EP, eligible hospital or CAH who transitions their patient to another setting of care or provider of care or refers their patient to another provider of care should provide summary of care record for each transition of care or referral.', 80, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_htn_bp_measure_cqm', 'Hypertension: Blood Pressure Measurement (CQM)', 200, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_tob_use_assess_cqm', 'Tobacco Use Assessment (CQM)', 205, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_tob_cess_inter_cqm', 'Tobacco Cessation Intervention (CQM)', 210, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_adult_wt_screen_fu_cqm', 'Adult Weight Screening and Follow-Up (CQM)', 220, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_wt_assess_couns_child_cqm', 'Weight Assessment and Counseling for Children and Adolescents (CQM)', 230, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_influenza_ge_50_cqm', 'Influenza Immunization for Patients >= 50 Years Old (CQM)', 240, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_child_immun_stat_cqm', 'Childhood immunization Status (CQM)', 250, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_pneumovacc_ge_65_cqm', 'Pneumonia Vaccination Status for Older Adults (CQM)', 260, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_dm_eye_cqm', 'Diabetes: Eye Exam (CQM)', 270, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_dm_foot_cqm', 'Diabetes: Foot Exam (CQM)', 280, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_dm_a1c_cqm', 'Diabetes: HbA1c Poor Control (CQM)', 285, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_dm_bp_control_cqm', 'Diabetes: Blood Pressure Management (CQM)', 290, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_dm_ldl_cqm', 'Diabetes: LDL Management & Control (CQM)', 300, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'rule_children_pharyngitis_cqm', 'Appropriate Testing for Children with Pharyngitis (CQM)', 502, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'rule_fall_screening_cqm', 'Falls: Screening, Risk-Assessment, and Plan of Care to Prevent Future Falls (CQM)', 504, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'rule_pain_intensity_cqm', 'Oncology: Medical and Radiation – Pain Intensity Quantified (CQM)', 506, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'rule_child_immun_stat_2014_cqm', 'Childhood immunization Status (CQM)', 250, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'rule_tob_use_2014_cqm', 'Preventive Care and Screening: Tobacco Use: Screening and Cessation Intervention (CQM)', 210, 0, 0, '', '', '', 0, 0);

INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_htn_bp_measure', 'Hypertension: Blood Pressure Measurement', 500, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_tob_use_assess', 'Tobacco Use Assessment', 510, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_tob_cess_inter', 'Tobacco Cessation Intervention', 520, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_adult_wt_screen_fu', 'Adult Weight Screening and Follow-Up', 530, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_wt_assess_couns_child', 'Weight Assessment and Counseling for Children and Adolescents', 540, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_influenza_ge_50', 'Influenza Immunization for Patients >= 50 Years Old', 550, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_pneumovacc_ge_65', 'Pneumonia Vaccination Status for Older Adults', 570, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_dm_hemo_a1c', 'Diabetes: Hemoglobin A1C', 570, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_dm_urine_alb', 'Diabetes: Urine Microalbumin', 590, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_dm_eye', 'Diabetes: Eye Exam', 600, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_dm_foot', 'Diabetes: Foot Exam', 610, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_cs_mammo', 'Cancer Screening: Mammogram', 620, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_cs_pap', 'Cancer Screening: Pap Smear', 630, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_cs_colon', 'Cancer Screening: Colon Cancer Screening', 640, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_cs_prostate', 'Cancer Screening: Prostate Cancer Screening', 650, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_inr_monitor', 'Coumadin Management - INR Monitoring', 1000, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_socsec_entry', 'Data Entry - Social Security Number', 1500, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_penicillin_allergy', 'Assess Penicillin Allergy', 1600, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_blood_pressure', 'Measure Blood Pressure', 1610, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_inr_measure', 'Measure INR', 1620, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('clinical_rules', 'rule_appt_reminder', 'Appointment Reminder Rule', 2000, 0);



INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'image_results_amc', 'Image Results', 3000, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'family_health_history_amc', 'Family Health History', 3100, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'electronic_notes_amc', 'Electronic Notes', 3200, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'secure_messaging_amc', 'Secure Electronic Messaging', 3400, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'view_download_transmit_amc', 'View, Download, Transmit (VDT)  (Measure B)', 3500, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'cpoe_radiology_amc', 'Use CPOE for radiology orders.', 46, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'cpoe_proc_orders_amc', 'Use CPOE for procedure orders.', 47, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'send_reminder_stage2_amc', 'Send reminders to patients per patient preference for preventive/follow up care.', 60, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'cpoe_med_stage2_amc', 'Use CPOE for medication orders.', 47, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'cpoe_med_stage1_amc_alternative', 'Use CPOE for medication orders.(Alternative)', 48, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'patient_edu_stage2_amc', 'Use certified EHR technology to identify patient-specific education resources and provide those resources to the patient if appropriate(New).', 40, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'record_vitals_1_stage1_amc', 'Record and chart changes in vital signs (SET 1).', 20, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'record_vitals_2_stage1_amc', 'Record and chart changes in vital signs (BP out of scope).', 20, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'record_vitals_3_stage1_amc', 'Record and chart changes in vital signs (Height / Weight out of scope).', 20, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'record_vitals_4_stage1_amc', 'Record and chart changes in vital signs ( Height / Weight / BP with in scope ).', 20, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'record_vitals_stage2_amc', 'Record and chart changes in vital signs (New).', 20, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'provide_sum_pat_stage2_amc', 'Provide clinical summaries for patients for each office visit (New).', 75, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'vdt_stage2_amc', 'View, Download, Transmit (VDT) (Measure A)', 3500, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'send_sum_stage1_amc', 'The EP, eligible hospital or CAH who transitions their patient to another setting of care or provider of care or refers their patient to another provider of care should provide summary of care record for each transition of care or referral.', 80, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'send_sum_1_stage2_amc', 'The EP, eligible hospital or CAH who transitions their patient to another setting of care or provider of care or refers their patient to another provider of care should provide summary of care record for each transition of care or referral (Measure A).', 80, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'send_sum_stage2_amc', 'The EP, eligible hospital or CAH who transitions their patient to another setting of care or provider of care or refers their patient to another provider of care should provide summary of care record for each transition of care or referral (Measure B).', 80, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'e_prescribe_stage1_amc', 'Generate and transmit permissible prescriptions electronically (Not including controlled substances).', 50, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'e_prescribe_1_stage2_amc', 'Generate and transmit permissible prescriptions electronically (All Prescriptions).', 50, 0, 0, '', '', '', 0, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`) VALUES
('clinical_rules', 'e_prescribe_2_stage2_amc', 'Generate and transmit permissible prescriptions electronically (Not including controlled substances).', 50, 0, 0, '', '', '', 0, 0);

-- order types
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','order_type','Order Types', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('order_type','procedure','Procedure',10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('order_type','intervention','Intervention',20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('order_type','laboratory_test','Laboratory Test',30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('order_type','physical_exam','Physical Exam',40,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('order_type','risk_category','Risk Category Assessment',50,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('order_type','patient_characteristics','Patient Characteristics',60,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('order_type','imaging','Imaging',70,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('order_type','enc_checkup_procedure','Encounter Checkup Procedure',80,0);


-- Clinical Rule Target Methods
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_targets', 'Clinical Rule Target Methods', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_targets' ,'target_database', 'Database', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_targets' ,'target_interval', 'Interval', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_targets' ,'target_proc', 'Procedure', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_targets' ,'target_appt', 'Appointment', 30, 0);

-- Clinical Rule Target Intervals
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_target_intervals', 'Clinical Rules Target Intervals', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_target_intervals' ,'year', 'Year', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_target_intervals' ,'month', 'Month', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_target_intervals' ,'week', 'Week', 30, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_target_intervals' ,'day', 'Day', 40, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_target_intervals' ,'hour', 'Hour', 50, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_target_intervals' ,'minute', 'Minute', 60, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_target_intervals' ,'second', 'Second', 70, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_target_intervals' ,'flu_season', 'Flu Season', 80, 0);

-- Clinical Rule Comparisons
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_comparisons', 'Clinical Rules Comparisons', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_comparisons' ,'EXIST', 'Exist', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_comparisons' ,'lt', 'Less Than', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_comparisons' ,'le', 'Less Than or Equal To', 30, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_comparisons' ,'gt', 'Greater Than', 40, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_comparisons' ,'ge', 'Greater Than or Equal To', 50, 0);

-- Clinical Rule Filter Methods
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_filters','Clinical Rule Filter Methods', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_filters', 'filt_database', 'Database', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_filters', 'filt_diagnosis', 'Diagnosis', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_filters', 'filt_sex', 'Gender', 30, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_filters', 'filt_age_max', 'Maximum Age', 40, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_filters', 'filt_age_min', 'Minimum Age', 50, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_filters', 'filt_proc', 'Procedure', 60, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_filters', 'filt_lists', 'Lists', 70, 0);

-- Clinical Rule Age Intervals
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_age_intervals', 'Clinical Rules Age Intervals', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_age_intervals' ,'year', 'Year', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_age_intervals' ,'month', 'Month', 20, 0);

-- Encounter Types (needed for mapping encounters for CQM rules)
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_enc_types', 'Clinical Rules Encounter Types', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_outpatient', 'encounter outpatient', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_nurs_fac', 'encounter nursing facility', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_off_vis', 'encounter office visit', 30, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_hea_and_beh', 'encounter health and behavior assessment', 40, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_occ_ther', 'encounter occupational therapy', 50, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_psych_and_psych', 'encounter psychiatric & psychologic', 60, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_pre_med_ser_18_older', 'encounter preventive medicine services 18 and older', 70, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_pre_med_ser_40_older', 'encounter preventive medicine 40 and older', 75, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_pre_ind_counsel', 'encounter preventive medicine - individual counseling', 80, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_pre_med_group_counsel', 'encounter preventive medicine group counseling', 90, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_pre_med_other_serv', 'encounter preventive medicine other services', 100, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_out_pcp_obgyn', 'encounter outpatient w/PCP & obgyn', 110, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_pregnancy', 'encounter pregnancy', 120, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_nurs_discharge', 'encounter nursing discharge', 130, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_acute_inp_or_ed', 'encounter acute inpatient or ED', 130, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_nonac_inp_out_or_opth', 'Encounter: encounter non-acute inpt, outpatient, or ophthalmology', 140, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_enc_types' ,'enc_influenza', 'encounter influenza', 150, 0);

-- Clinical Rule Action Categories
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_action_category', 'Clinical Rule Action Category', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action_category' ,'act_cat_assess', 'Assessment', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action_category' ,'act_cat_edu', 'Education', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action_category' ,'act_cat_exam', 'Examination', 30, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action_category' ,'act_cat_inter', 'Intervention', 40, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action_category' ,'act_cat_measure', 'Measurement', 50, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action_category' ,'act_cat_treat', 'Treatment', 60, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action_category' ,'act_cat_remind', 'Reminder', 70, 0);

-- Clinical Rule Action Items
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_action', 'Clinical Rule Action Item', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_bp', 'Blood Pressure', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_influvacc', 'Influenza Vaccine', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_tobacco', 'Tobacco', 30, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_wt', 'Weight', 40, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_bmi', 'BMI', 43, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_nutrition', 'Nutrition', 45, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_exercise', 'Exercise', 47, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_pneumovacc', 'Pneumococcal Vaccine', 60, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_hemo_a1c', 'Hemoglobin A1C', 70, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_urine_alb', 'Urine Microalbumin', 80, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_eye', 'Opthalmic', 90, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_foot', 'Podiatric', 100, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_mammo', 'Mammogram', 110, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_pap', 'Pap Smear', 120, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_colon_cancer_screen', 'Colon Cancer Screening', 130, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_prostate_cancer_screen', 'Prostate Cancer Screening', 140, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_lab_inr', 'INR', 150, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_soc_sec', 'Social Security Number', 155, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_penicillin_allergy', 'Penicillin Allergy', 157, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_action' ,'act_appointment', 'Appointment', 160, 0);

-- Clinical Rule Reminder Intervals
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_reminder_intervals', 'Clinical Rules Reminder Intervals', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_intervals' ,'month', 'Month', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_intervals' ,'week', 'Week', 20, 0);

-- Clinical Rule Reminder Methods
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_reminder_methods', 'Clinical Rules Reminder Methods', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_methods' ,'clinical_reminder_pre', 'Past Due Interval (Clinical Reminders)', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_methods' ,'patient_reminder_pre', 'Past Due Interval (Patient Reminders)', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_methods' ,'clinical_reminder_post', 'Soon Due Interval (Clinical Reminders)', 30, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_methods' ,'patient_reminder_post', 'Soon Due Interval (Patient Reminders)', 40, 0);

-- Clinical Rule Reminder Due Options
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_reminder_due_opt', 'Clinical Rules Reminder Due Options', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_due_opt' ,'due', 'Due', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_due_opt' ,'soon_due', 'Due Soon', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_due_opt' ,'past_due', 'Past Due', 30, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_due_opt' ,'not_due', 'Not Due', 30, 0);

-- Clinical Rule Reminder Inactivate Options
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('lists' ,'rule_reminder_inactive_opt', 'Clinical Rules Reminder Inactivation Options', 3, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_inactive_opt' ,'auto', 'Automatic', 10, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_inactive_opt' ,'due_status_update', 'Due Status Update', 20, 0);
INSERT INTO `list_options` ( `list_id`, `option_id`, `title`, `seq`, `is_default` ) VALUES ('rule_reminder_inactive_opt' ,'manual', 'Manual', 20, 0);

-- eRx User Roles
INSERT INTO list_options (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('newcrop_erx_role','erxadmin','NewCrop Admin','5','0','0','','');
INSERT INTO list_options (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('newcrop_erx_role','erxdoctor','NewCrop Doctor','20','0','0','','');
INSERT INTO list_options (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('newcrop_erx_role','erxmanager','NewCrop Manager','15','0','0','','');
INSERT INTO list_options (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('newcrop_erx_role','erxmidlevelPrescriber','NewCrop Midlevel Prescriber','25','0','0','','');
INSERT INTO list_options (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('newcrop_erx_role','erxnurse','NewCrop Nurse','10','0','0','','');
INSERT INTO list_options (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('newcrop_erx_role','erxsupervisingDoctor','NewCrop Supervising Doctor','30','0','0','','');
INSERT INTO list_options (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('lists','newcrop_erx_role','NewCrop eRx Role','221','0','0','','');

-- MSP remit codes
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('lists','msp_remit_codes','MSP Remit Codes','221','0','0','','');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '1', '1', 1, 0, 0, '', 'Deductible Amount');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '2', '2', 2, 0, 0, '', 'Coinsurance Amount');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '3', '3', 3, 0, 0, '', 'Co-payment Amount');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '4', '4', 4, 0, 0, '', 'The procedure code is inconsistent with the modifier used or a required modifier is missing. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '9', '9', 9, 0, 0, '', 'The diagnosis is inconsistent with the patient''s age. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '10', '10', 10, 0, 0, '', 'The diagnosis is inconsistent with the patient''s gender. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '11', '11', 11, 0, 0, '', 'The diagnosis is inconsistent with the procedure. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '12', '12', 12, 0, 0, '', 'The diagnosis is inconsistent with the provider type. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '13', '13', 13, 0, 0, '', 'The date of death precedes the date of service.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '14', '14', 14, 0, 0, '', 'The date of birth follows the date of service.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '15', '15', 15, 0, 0, '', 'The authorization number is missing, invalid, or does not apply to the billed services or provider.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '16', '16', 16, 0, 0, '', 'Claim/service lacks information which is needed for adjudication. At least one Remark Code must be provided (may be comprised of either the NCPDP Reject Reason Code, or Remittance Advice Remark Code that is not an ALERT.)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '18', '18', 17, 0, 0, '', 'Duplicate claim/service.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '19', '19', 18, 0, 0, '', 'This is a work-related injury/illness and thus the liability of the Worker''s Compensation Carrier.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '20', '20', 19, 0, 0, '', 'This injury/illness is covered by the liability carrier.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '21', '21', 20, 0, 0, '', 'This injury/illness is the liability of the no-fault carrier.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '22', '22', 21, 0, 0, '', 'This care may be covered by another payer per coordination of benefits.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '23', '23', 22, 0, 0, '', 'The impact of prior payer(s) adjudication including payments and/or adjustments.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '24', '24', 23, 0, 0, '', 'Charges are covered under a capitation agreement/managed care plan.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '26', '26', 24, 0, 0, '', 'Expenses incurred prior to coverage.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '27', '27', 25, 0, 0, '', 'Expenses incurred after coverage terminated.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '29', '29', 26, 0, 0, '', 'The time limit for filing has expired.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '31', '31', 27, 0, 0, '', 'Patient cannot be identified as our insured.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '32', '32', 28, 0, 0, '', 'Our records indicate that this dependent is not an eligible dependent as defined.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '33', '33', 29, 0, 0, '', 'Insured has no dependent coverage.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '34', '34', 30, 0, 0, '', 'Insured has no coverage for newborns.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '35', '35', 31, 0, 0, '', 'Lifetime benefit maximum has been reached.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '38', '38', 32, 0, 0, '', 'Services not provided or authorized by designated (network/primary care) providers.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '39', '39', 33, 0, 0, '', 'Services denied at the time authorization/pre-certification was requested.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '40', '40', 34, 0, 0, '', 'Charges do not meet qualifications for emergent/urgent care. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '44', '44', 35, 0, 0, '', 'Prompt-pay discount.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '45', '45', 36, 0, 0, '', 'Charge exceeds fee schedule/maximum allowable or contracted/legislated fee arrangement. (Use Group Codes PR or CO depending upon liability).');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '49', '49', 37, 0, 0, '', 'These are non-covered services because this is a routine exam or screening procedure done in conjunction with a routine exam. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '50', '50', 38, 0, 0, '', 'These are non-covered services because this is not deemed a ''medical necessity'' by the payer. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '51', '51', 39, 0, 0, '', 'These are non-covered services because this is a pre-existing condition. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '53', '53', 40, 0, 0, '', 'Services by an immediate relative or a member of the same household are not covered.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '54', '54', 41, 0, 0, '', 'Multiple physicians/assistants are not covered in this case. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '55', '55', 42, 0, 0, '', 'Procedure/treatment is deemed experimental/investigational by the payer. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '56', '56', 43, 0, 0, '', 'Procedure/treatment has not been deemed ''proven to be effective'' by the payer. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '58', '58', 44, 0, 0, '', 'Treatment was deemed by the payer to have been rendered in an inappropriate or invalid place of service. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '59', '59', 45, 0, 0, '', 'Processed based on multiple or concurrent procedure rules. (For example multiple surgery or diagnostic imaging, concurrent anesthesia.) Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '60', '60', 46, 0, 0, '', 'Charges for outpatient services are not covered when performed within a period of time prior to or after inpatient services.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '61', '61', 47, 0, 0, '', 'Penalty for failure to obtain second surgical opinion. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '66', '66', 48, 0, 0, '', 'Blood Deductible.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '69', '69', 49, 0, 0, '', 'Day outlier amount.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '70', '70', 50, 0, 0, '', 'Cost outlier - Adjustment to compensate for additional costs.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '74', '74', 51, 0, 0, '', 'Indirect Medical Education Adjustment.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '75', '75', 52, 0, 0, '', 'Direct Medical Education Adjustment.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '76', '76', 53, 0, 0, '', 'Disproportionate Share Adjustment.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '78', '78', 54, 0, 0, '', 'Non-Covered days/Room charge adjustment.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '85', '85', 55, 0, 0, '', 'Patient Interest Adjustment (Use Only Group code PR)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '87', '87', 56, 0, 0, '', 'Transfer amount.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '89', '89', 57, 0, 0, '', 'Professional fees removed from charges.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '90', '90', 58, 0, 0, '', 'Ingredient cost adjustment. Note: To be used for pharmaceuticals only.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '91', '91', 59, 0, 0, '', 'Dispensing fee adjustment.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '94', '94', 60, 0, 0, '', 'Processed in Excess of charges.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '95', '95', 61, 0, 0, '', 'Plan procedures not followed.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '96', '96', 62, 0, 0, '', 'Non-covered charge(s). At least one Remark Code must be provided (may be comprised of either the NCPDP Reject Reason Code, or Remittance Advice Remark Code that is not an ALERT.) Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 S');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '97', '97', 63, 0, 0, '', 'The benefit for this service is included in the payment/allowance for another service/procedure that has already been adjudicated. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '100', '100', 64, 0, 0, '', 'Payment made to patient/insured/responsible party/employer.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '101', '101', 65, 0, 0, '', 'Predetermination: anticipated payment upon completion of services or claim adjudication.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '102', '102', 66, 0, 0, '', 'Major Medical Adjustment.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '103', '103', 67, 0, 0, '', 'Provider promotional discount (e.g., Senior citizen discount).');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '104', '104', 68, 0, 0, '', 'Managed care withholding.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '105', '105', 69, 0, 0, '', 'Tax withholding.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '106', '106', 70, 0, 0, '', 'Patient payment option/election not in effect.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '107', '107', 71, 0, 0, '', 'The related or qualifying claim/service was not identified on this claim. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '108', '108', 72, 0, 0, '', 'Rent/purchase guidelines were not met. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '109', '109', 73, 0, 0, '', 'Claim not covered by this payer/contractor. You must send the claim to the correct payer/contractor.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '110', '110', 74, 0, 0, '', 'Billing date predates service date.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '111', '111', 75, 0, 0, '', 'Not covered unless the provider accepts assignment.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '112', '112', 76, 0, 0, '', 'Service not furnished directly to the patient and/or not documented.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '114', '114', 77, 0, 0, '', 'Procedure/product not approved by the Food and Drug Administration.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '115', '115', 78, 0, 0, '', 'Procedure postponed, canceled, or delayed.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '116', '116', 79, 0, 0, '', 'The advance indemnification notice signed by the patient did not comply with requirements.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '117', '117', 80, 0, 0, '', 'Transportation is only covered to the closest facility that can provide the necessary care.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '118', '118', 81, 0, 0, '', 'ESRD network support adjustment.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '119', '119', 82, 0, 0, '', 'Benefit maximum for this time period or occurrence has been reached.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '121', '121', 83, 0, 0, '', 'Indemnification adjustment - compensation for outstanding member responsibility.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '122', '122', 84, 0, 0, '', 'Psychiatric reduction.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '125', '125', 85, 0, 0, '', 'Submission/billing error(s). At least one Remark Code must be provided (may be comprised of either the NCPDP Reject Reason Code, or Remittance Advice Remark Code that is not an ALERT.)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '128', '128', 86, 0, 0, '', 'Newborn''s services are covered in the mother''s Allowance.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '129', '129', 87, 0, 0, '', 'Prior processing information appears incorrect. At least one Remark Code must be provided (may be comprised of either the NCPDP Reject Reason Code, or Remittance Advice Remark Code that is not an ALERT.)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '130', '130', 88, 0, 0, '', 'Claim submission fee.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '131', '131', 89, 0, 0, '', 'Claim specific negotiated discount.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '132', '132', 90, 0, 0, '', 'Prearranged demonstration project adjustment.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '133', '133', 91, 0, 0, '', 'The disposition of this claim/service is pending further review.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '134', '134', 92, 0, 0, '', 'Technical fees removed from charges.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '135', '135', 93, 0, 0, '', 'Interim bills cannot be processed.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '136', '136', 94, 0, 0, '', 'Failure to follow prior payer''s coverage rules. (Use Group Code OA).');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '137', '137', 95, 0, 0, '', 'Regulatory Surcharges, Assessments, Allowances or Health Related Taxes.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '138', '138', 96, 0, 0, '', 'Appeal procedures not followed or time limits not met.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '139', '139', 97, 0, 0, '', 'Contracted funding agreement - Subscriber is employed by the provider of services.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '140', '140', 98, 0, 0, '', 'Patient/Insured health identification number and name do not match.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '141', '141', 99, 0, 0, '', 'Claim spans eligible and ineligible periods of coverage.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '142', '142', 100, 0, 0, '', 'Monthly Medicaid patient liability amount.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '143', '143', 101, 0, 0, '', 'Portion of payment deferred.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '144', '144', 102, 0, 0, '', 'Incentive adjustment, e.g. preferred product/service.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '146', '146', 103, 0, 0, '', 'Diagnosis was invalid for the date(s) of service reported.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '147', '147', 104, 0, 0, '', 'Provider contracted/negotiated rate expired or not on file.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '148', '148', 105, 0, 0, '', 'Information from another provider was not provided or was insufficient/incomplete. At least one Remark Code must be provided (may be comprised of either the NCPDP Reject Reason Code, or Remittance Advice Remark Code that is not an ALERT.)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '149', '149', 106, 0, 0, '', 'Lifetime benefit maximum has been reached for this service/benefit category.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '150', '150', 107, 0, 0, '', 'Payer deems the information submitted does not support this level of service.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '151', '151', 108, 0, 0, '', 'Payment adjusted because the payer deems the information submitted does not support this many/frequency of services.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '152', '152', 109, 0, 0, '', 'Payer deems the information submitted does not support this length of service. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '153', '153', 110, 0, 0, '', 'Payer deems the information submitted does not support this dosage.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '154', '154', 111, 0, 0, '', 'Payer deems the information submitted does not support this day''s supply.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '155', '155', 112, 0, 0, '', 'Patient refused the service/procedure.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '157', '157', 113, 0, 0, '', 'Service/procedure was provided as a result of an act of war.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '158', '158', 114, 0, 0, '', 'Service/procedure was provided outside of the United States.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '159', '159', 115, 0, 0, '', 'Service/procedure was provided as a result of terrorism.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '160', '160', 116, 0, 0, '', 'Injury/illness was the result of an activity that is a benefit exclusion.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '161', '161', 117, 0, 0, '', 'Provider performance bonus');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '162', '162', 118, 0, 0, '', 'State-mandated Requirement for Property and Casualty, see Claim Payment Remarks Code for specific explanation.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '163', '163', 119, 0, 0, '', 'Attachment referenced on the claim was not received.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '164', '164', 120, 0, 0, '', 'Attachment referenced on the claim was not received in a timely fashion.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '165', '165', 121, 0, 0, '', 'Referral absent or exceeded.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '166', '166', 122, 0, 0, '', 'These services were submitted after this payers responsibility for processing claims under this plan ended.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '167', '167', 123, 0, 0, '', 'This (these) diagnosis(es) is (are) not covered. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '168', '168', 124, 0, 0, '', 'Service(s) have been considered under the patient''s medical plan. Benefits are not available under this dental plan.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '169', '169', 125, 0, 0, '', 'Alternate benefit has been provided.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '170', '170', 126, 0, 0, '', 'Payment is denied when performed/billed by this type of provider. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '171', '171', 127, 0, 0, '', 'Payment is denied when performed/billed by this type of provider in this type of facility. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '172', '172', 128, 0, 0, '', 'Payment is adjusted when performed/billed by a provider of this specialty. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '173', '173', 129, 0, 0, '', 'Service was not prescribed by a physician.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '174', '174', 130, 0, 0, '', 'Service was not prescribed prior to delivery.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '175', '175', 131, 0, 0, '', 'Prescription is incomplete.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '176', '176', 132, 0, 0, '', 'Prescription is not current.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '177', '177', 133, 0, 0, '', 'Patient has not met the required eligibility requirements.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '178', '178', 134, 0, 0, '', 'Patient has not met the required spend down requirements.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '179', '179', 135, 0, 0, '', 'Patient has not met the required waiting requirements. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '180', '180', 136, 0, 0, '', 'Patient has not met the required residency requirements.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '181', '181', 137, 0, 0, '', 'Procedure code was invalid on the date of service.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '182', '182', 138, 0, 0, '', 'Procedure modifier was invalid on the date of service.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '183', '183', 139, 0, 0, '', 'The referring provider is not eligible to refer the service billed. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '184', '184', 140, 0, 0, '', 'The prescribing/ordering provider is not eligible to prescribe/order the service billed. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '185', '185', 141, 0, 0, '', 'The rendering provider is not eligible to perform the service billed. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '186', '186', 142, 0, 0, '', 'Level of care change adjustment.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '187', '187', 143, 0, 0, '', 'Consumer Spending Account payments (includes but is not limited to Flexible Spending Account, Health Savings Account, Health Reimbursement Account, etc.)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '188', '188', 144, 0, 0, '', 'This product/procedure is only covered when used according to FDA recommendations.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '189', '189', 145, 0, 0, '', '''''Not otherwise classified'' or ''unlisted'' procedure code (CPT/HCPCS) was billed when there is a specific procedure code for this procedure/service''');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '190', '190', 146, 0, 0, '', 'Payment is included in the allowance for a Skilled Nursing Facility (SNF) qualified stay.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '191', '191', 147, 0, 0, '', 'Not a work related injury/illness and thus not the liability of the workers'' compensation carrier Note: If adjustment is at the Claim Level, the payer must send and the provider should refer to the 835 Insurance Policy Number Segment (Loop 2100 Other Clai');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '192', '192', 148, 0, 0, '', 'Non standard adjustment code from paper remittance. Note: This code is to be used by providers/payers providing Coordination of Benefits information to another payer in the 837 transaction only. This code is only used when the non-standard code cannot be ');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '193', '193', 149, 0, 0, '', 'Original payment decision is being maintained. Upon review, it was determined that this claim was processed properly.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '194', '194', 150, 0, 0, '', 'Anesthesia performed by the operating physician, the assistant surgeon or the attending physician.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '195', '195', 151, 0, 0, '', 'Refund issued to an erroneous priority payer for this claim/service.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '197', '197', 152, 0, 0, '', 'Precertification/authorization/notification absent.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '198', '198', 153, 0, 0, '', 'Precertification/authorization exceeded.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '199', '199', 154, 0, 0, '', 'Revenue code and Procedure code do not match.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '200', '200', 155, 0, 0, '', 'Expenses incurred during lapse in coverage');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '201', '201', 156, 0, 0, '', 'Workers Compensation case settled. Patient is responsible for amount of this claim/service through WC ''Medicare set aside arrangement'' or other agreement. (Use group code PR).');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '202', '202', 157, 0, 0, '', 'Non-covered personal comfort or convenience services.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '203', '203', 158, 0, 0, '', 'Discontinued or reduced service.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '204', '204', 159, 0, 0, '', 'This service/equipment/drug is not covered under the patient?s current benefit plan');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '205', '205', 160, 0, 0, '', 'Pharmacy discount card processing fee');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '206', '206', 161, 0, 0, '', 'National Provider Identifier - missing.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '207', '207', 162, 0, 0, '', 'National Provider identifier - Invalid format');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '208', '208', 163, 0, 0, '', 'National Provider Identifier - Not matched.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '209', '209', 164, 0, 0, '', 'Per regulatory or other agreement. The provider cannot collect this amount from the patient. However, this amount may be billed to subsequent payer. Refund to patient if collected. (Use Group code OA)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '210', '210', 165, 0, 0, '', 'Payment adjusted because pre-certification/authorization not received in a timely fashion');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '211', '211', 166, 0, 0, '', 'National Drug Codes (NDC) not eligible for rebate, are not covered.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '212', '212', 167, 0, 0, '', 'Administrative surcharges are not covered');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '213', '213', 168, 0, 0, '', 'Non-compliance with the physician self referral prohibition legislation or payer policy.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '214', '214', 169, 0, 0, '', 'Workers'' Compensation claim adjudicated as non-compensable. This Payer not liable for claim or service/treatment. Note: If adjustment is at the Claim Level, the payer must send and the provider should refer to the 835 Insurance Policy Number Segment (Loop');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '215', '215', 170, 0, 0, '', 'Based on subrogation of a third party settlement');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '216', '216', 171, 0, 0, '', 'Based on the findings of a review organization');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '217', '217', 172, 0, 0, '', 'Based on payer reasonable and customary fees. No maximum allowable defined by legislated fee arrangement. (Note: To be used for Workers'' Compensation only)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '218', '218', 173, 0, 0, '', 'Based on entitlement to benefits. Note: If adjustment is at the Claim Level, the payer must send and the provider should refer to the 835 Insurance Policy Number Segment (Loop 2100 Other Claim Related Information REF qualifier ''IG'') for the jurisdictional');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '219', '219', 174, 0, 0, '', 'Based on extent of injury. Note: If adjustment is at the Claim Level, the payer must send and the provider should refer to the 835 Insurance Policy Number Segment (Loop 2100 Other Claim Related Information REF qualifier ''IG'') for the jurisdictional regula');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '220', '220', 175, 0, 0, '', 'The applicable fee schedule does not contain the billed code. Please resubmit a bill with the appropriate fee schedule code(s) that best describe the service(s) provided and supporting documentation if required. (Note: To be used for Workers'' Compensation');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '221', '221', 176, 0, 0, '', 'Workers'' Compensation claim is under investigation. Note: If adjustment is at the Claim Level, the payer must send and the provider should refer to the 835 Insurance Policy Number Segment (Loop 2100 Other Claim Related Information REF qualifier ''IG'') for ');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '222', '222', 177, 0, 0, '', 'Exceeds the contracted maximum number of hours/days/units by this provider for this period. This is not patient specific. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '223', '223', 178, 0, 0, '', 'Adjustment code for mandated federal, state or local law/regulation that is not already covered by another code and is mandated before a new code can be created.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '224', '224', 179, 0, 0, '', 'Patient identification compromised by identity theft. Identity verification required for processing this and future claims.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '225', '225', 180, 0, 0, '', 'Penalty or Interest Payment by Payer (Only used for plan to plan encounter reporting within the 837)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '226', '226', 181, 0, 0, '', 'Information requested from the Billing/Rendering Provider was not provided or was insufficient/incomplete. At least one Remark Code must be provided (may be comprised of either the NCPDP Reject Reason Code, or Remittance Advice Remark Code that is not an ');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '227', '227', 182, 0, 0, '', 'Information requested from the patient/insured/responsible party was not provided or was insufficient/incomplete. At least one Remark Code must be provided (may be comprised of either the NCPDP Reject Reason Code, or Remittance Advice Remark Code that is ');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '228', '228', 183, 0, 0, '', 'Denied for failure of this provider, another provider or the subscriber to supply requested information to a previous payer for their adjudication');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '229', '229', 184, 0, 0, '', 'Partial charge amount not considered by Medicare due to the initial claim Type of Bill being 12X. Note: This code can only be used in the 837 transaction to convey Coordination of Benefits information when the secondary payer?s cost avoidance policy allow');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '230', '230', 185, 0, 0, '', 'No available or correlating CPT/HCPCS code to describe this service. Note: Used only by Property and Casualty.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '231', '231', 186, 0, 0, '', 'Mutually exclusive procedures cannot be done in the same day/setting. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '232', '232', 187, 0, 0, '', 'Institutional Transfer Amount. Note - Applies to institutional claims only and explains the DRG amount difference when the patient care crosses multiple institutions.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '233', '233', 188, 0, 0, '', 'Services/charges related to the treatment of a hospital-acquired condition or preventable medical error.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '234', '234', 189, 0, 0, '', 'This procedure is not paid separately. At least one Remark Code must be provided (may be comprised of either the NCPDP Reject Reason Code, or Remittance Advice Remark Code that is not an ALERT.)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '235', '235', 190, 0, 0, '', 'Sales Tax');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '236', '236', 191, 0, 0, '', 'This procedure or procedure/modifier combination is not compatible with another procedure or procedure/modifier combination provided on the same day according to the National Correct Coding Initiative.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', '237', '237', 192, 0, 0, '', 'Legislated/Regulatory Penalty. At least one Remark Code must be provided (may be comprised of either the NCPDP Reject Reason Code, or Remittance Advice Remark Code that is not an ALERT.)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'A0', 'A0', 193, 0, 0, '', 'Patient refund amount.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'A1', 'A1', 194, 0, 0, '', 'Claim/Service denied. At least one Remark Code must be provided (may be comprised of either the NCPDP Reject Reason Code, or Remittance Advice Remark Code that is not an ALERT.)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'A5', 'A5', 195, 0, 0, '', 'Medicare Claim PPS Capital Cost Outlier Amount.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'A6', 'A6', 196, 0, 0, '', 'Prior hospitalization or 30 day transfer requirement not met.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'A7', 'A7', 197, 0, 0, '', 'Presumptive Payment Adjustment');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'A8', 'A8', 198, 0, 0, '', 'Ungroupable DRG.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B1', 'B1', 199, 0, 0, '', 'Non-covered visits.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B10', 'B10', 200, 0, 0, '', 'Allowed amount has been reduced because a component of the basic procedure/test was paid. The beneficiary is not liable for more than the charge limit for the basic procedure/test.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B11', 'B11', 201, 0, 0, '', 'The claim/service has been transferred to the proper payer/processor for processing. Claim/service not covered by this payer/processor.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B12', 'B12', 202, 0, 0, '', 'Services not documented in patients'' medical records.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B13', 'B13', 203, 0, 0, '', 'Previously paid. Payment for this claim/service may have been provided in a previous payment.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B14', 'B14', 204, 0, 0, '', 'Only one visit or consultation per physician per day is covered.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B15', 'B15', 205, 0, 0, '', 'This service/procedure requires that a qualifying service/procedure be received and covered. The qualifying other service/procedure has not been received/adjudicated. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payme');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B16', 'B16', 206, 0, 0, '', '''''New Patient'' qualifications were not met.''');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B20', 'B20', 207, 0, 0, '', 'Procedure/service was partially or fully furnished by another provider.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B22', 'B22', 208, 0, 0, '', 'This payment is adjusted based on the diagnosis.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B23', 'B23', 209, 0, 0, '', 'Procedure billed is not authorized per your Clinical Laboratory Improvement Amendment (CLIA) proficiency test.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B4', 'B4', 210, 0, 0, '', 'Late filing penalty.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B5', 'B5', 211, 0, 0, '', 'Coverage/program guidelines were not met or were exceeded.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B7', 'B7', 212, 0, 0, '', 'This provider was not certified/eligible to be paid for this procedure/service on this date of service. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B8', 'B8', 213, 0, 0, '', 'Alternative services were available, and should have been utilized. Note: Refer to the 835 Healthcare Policy Identification Segment (loop 2110 Service Payment Information REF), if present.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'B9', 'B9', 214, 0, 0, '', 'Patient is enrolled in a Hospice.');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'D23', 'D23', 215, 0, 0, '', 'This dual eligible patient is covered by Medicare Part D per Medicare Retro-Eligibility. At least one Remark Code must be provided (may be comprised of either the NCPDP Reject Reason Code, or Remittance Advice Remark Code that is not an ALERT.)');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'W1', 'W1', 216, 0, 0, '', 'Workers'' compensation jurisdictional fee schedule adjustment. Note: If adjustment is at the Claim Level, the payer must send and the provider should refer to the 835 Class of Contract Code Identification Segment (Loop 2100 Other Claim Related Information ');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) VALUES ('msp_remit_codes', 'W2', 'W2', 217, 0, 0, '', 'Payment reduced or denied based on workers'' compensation jurisdictional regulations or payment policies, use only if no other code is applicable. Note: If adjustment is at the Claim Level, the payer must send and the provider should refer to the 835 Insur');

INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`) VALUES ('lists','nation_notes_replace_buttons','Nation Notes Replace Buttons',1);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`) VALUES ('nation_notes_replace_buttons','Yes','Yes',10);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`) VALUES ('nation_notes_replace_buttons','No','No',20);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`) VALUES ('nation_notes_replace_buttons','Normal','Normal',30);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`) VALUES ('nation_notes_replace_buttons','Abnormal','Abnormal',40);
insert into `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) values('lists','payment_gateways','Payment Gateways','297','1','0','','');
insert into `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`) values('payment_gateways','authorize_net','Authorize.net','1','0','0','','');

insert into list_options (list_id, option_id, title, seq, option_value, mapping, notes) values('lists','ptlistcols','Patient List Columns','1','0','','');
insert into list_options (list_id, option_id, title, seq, option_value, mapping, notes) values('ptlistcols','name'      ,'Full Name'     ,'10','3','','');
insert into list_options (list_id, option_id, title, seq, option_value, mapping, notes) values('ptlistcols','phone_home','Home Phone'    ,'20','3','','');
insert into list_options (list_id, option_id, title, seq, option_value, mapping, notes) values('ptlistcols','ss'        ,'SSN'           ,'30','3','','');
insert into list_options (list_id, option_id, title, seq, option_value, mapping, notes) values('ptlistcols','DOB'       ,'Date of Birth' ,'40','3','','');
insert into list_options (list_id, option_id, title, seq, option_value, mapping, notes) values('ptlistcols','pubpid'    ,'External ID'   ,'50','3','','');

-- Medical Problem Issue List
INSERT INTO list_options(list_id,option_id,title) VALUES ('lists','medical_problem_issue_list','Medical Problem Issue List');
INSERT INTO list_options(list_id,option_id,title,seq,codes) VALUES ('medical_problem_issue_list', 'HTN', 'HTN', 10,'ICD10:I10');
INSERT INTO list_options(list_id,option_id,title,seq,codes) VALUES ('medical_problem_issue_list', 'asthma', 'asthma', 20,'ICD10:J45.909');
INSERT INTO list_options(list_id,option_id,title,seq,codes) VALUES ('medical_problem_issue_list', 'diabetes', 'diabetes', 30, '');
INSERT INTO list_options(list_id,option_id,title,seq,codes) VALUES ('medical_problem_issue_list', 'hyperlipidemia', 'hyperlipidemia', 40,'ICD10:E78.5');

-- Ophthalmology: Medical Problem Issue List
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('medical_problem_issue_list', 'poag','POAG', 10,'ICD10:H40.11X4','eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('medical_problem_issue_list', 'dermatochalasis','Dermatochalasis', 20,'ICD10:H02.839','eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('medical_problem_issue_list', 'niddm_bdr','NIDDM w/ BDR', 30,',ICD10:E11.319','eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('medical_problem_issue_list', 'ns_cataract','NS Cataract', 40,'ICD10:H25.10','eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('medical_problem_issue_list', 'BCC','BCC', 50,'ICD10:C44.191','eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('medical_problem_issue_list', 'iddm_bdr','IDDM w/ BDR', 60,'ICD10:E10.329','eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('medical_problem_issue_list', 'Keratoconus','Keratoconus', 70,'ICD10:H18.603','eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('medical_problem_issue_list', 'dry_eye','Dry Eye', 80,'ICD10:H04.123','eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('medical_problem_issue_list', 'SCC','SCC', 90,'ICD10:C44.191','eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('medical_problem_issue_list', 'stye','stye', 100,'ICD10:H00.029','eye');

-- Medication Issue List
INSERT INTO list_options(list_id,option_id,title) VALUES ('lists','medication_issue_list','Medication Issue List');
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('medication_issue_list', 'Norvasc', 'Norvasc', 10);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('medication_issue_list', 'Lipitor', 'Lipitor', 20);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('medication_issue_list', 'Metformin', 'Metformin', 30);

-- Allergy Issue List
INSERT INTO list_options(list_id,option_id,title) VALUES ('lists','allergy_issue_list','Allergy Issue List');
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('allergy_issue_list', 'penicillin', 'penicillin', 10);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('allergy_issue_list', 'sulfa', 'sulfa', 20);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('allergy_issue_list', 'iodine', 'iodine', 30);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('allergy_issue_list', 'codeine', 'codeine', 40);

-- Surgery Issue List
INSERT INTO list_options(list_id,option_id,title) VALUES ('lists','surgery_issue_list','Surgery Issue List');
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('surgery_issue_list', 'tonsillectomy', 'tonsillectomy', 10);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('surgery_issue_list', 'appendectomy', 'appendectomy', 20);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('surgery_issue_list', 'cholecystectomy', 'cholecystectomy', 30);
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('surgery_issue_list', 'Bleph_Upper', 'Blepharoplasty', 40, 'CPT4:15823-50', 'eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('surgery_issue_list', 'Phaco_IOL_OD', 'Phaco/IOL OD', 50, 'CPT4:66984-RT', 'eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('surgery_issue_list', 'Phaco_IOL_OS', 'Phaco/IOL OS', 60, 'CPT4:66984-LT', 'eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('surgery_issue_list', 'LPI_OD', 'LPI OD', 70, 'CPT4:66761-RT', 'eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('surgery_issue_list', 'LPI_OS', 'LPI OS', 80, 'CPT4:66761-LT', 'eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('surgery_issue_list', 'ALT_OD', 'ALT OD', 90, 'CPT4:65855-RT', 'eye');
INSERT INTO list_options(list_id,option_id,title,seq,codes,subtype) VALUES ('surgery_issue_list', 'ALT_OS', 'ALT OS', 100, 'CPT4:65855-LT', 'eye');

-- Dental Issue List
INSERT INTO list_options(list_id,option_id,title) VALUES ('lists','dental_issue_list','Dental Issue List');

-- General Issue List
INSERT INTO list_options(list_id,option_id,title) VALUES ('lists','general_issue_list','General Issue List');
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('general_issue_list', 'Osteopathy', 'Osteopathy', 10);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('general_issue_list', 'Chiropractic', 'Chiropractic', 20);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('general_issue_list', 'Prevention Rehab', 'Prevention Rehab', 30);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('general_issue_list', 'Podiatry', 'Podiatry', 40);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('general_issue_list', 'Strength and Conditioning', 'Strength and Conditioning', 50);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('general_issue_list', 'Nutritional', 'Nutritional', 60);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('general_issue_list', 'Fitness Testing', 'Fitness Testing', 70);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('general_issue_list', 'Pre Participation Assessment', 'Pre Participation Assessment', 80);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('general_issue_list', 'Screening / Testing', 'Screening / Testing', 90);

-- Issue Types List
INSERT INTO list_options (`list_id`,`option_id`,`title`) VALUES ('lists','issue_types','Issue Types');

-- Issue Subtypes List
INSERT INTO list_options (list_id,option_id,title) VALUES ('lists','issue_subtypes','Issue Subtypes');
INSERT INTO list_options (list_id, option_id,title, seq) VALUES ('issue_subtypes', 'eye', 'Eye',10);

-- Insurance Types List
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`) VALUES ('lists','insurance_types','Insurance Types',1);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`) VALUES ('insurance_types','primary'  ,'Primary'  ,10);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`) VALUES ('insurance_types','secondary','Secondary',20);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`) VALUES ('insurance_types','tertiary' ,'Tertiary' ,30);

-- Amendment Statuses
INSERT INTO list_options(list_id,option_id,title) VALUES ('lists' ,'amendment_status','Amendment Status');
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('amendment_status' ,'approved','Approved', 10);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('amendment_status' ,'rejected','Rejected', 20);

-- Amendment request from
INSERT INTO list_options(list_id,option_id,title) VALUES ('lists' ,'amendment_from','Amendment From');
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('amendment_from' ,'patient','Patient', 10);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('amendment_from' ,'insurance','Insurance', 20);

-- Patient Flow Board Rooms
INSERT INTO list_options(list_id,option_id,title) VALUES ('lists','patient_flow_board_rooms','Patient Flow Board Rooms');
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('patient_flow_board_rooms', '1', 'Room 1', 10);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('patient_flow_board_rooms', '2', 'Room 2', 20);
INSERT INTO list_options(list_id,option_id,title,seq) VALUES ('patient_flow_board_rooms', '3', 'Room 3', 30);

-- Religious Affiliation
INSERT INTO list_options(list_id,option_id,title) VALUES ('lists','religious_affiliation','Religious Affiliation');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','adventist','1001','Adventist','5');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','african_religions','1002','African Religions','15');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','afro-caribbean_religions','1003','Afro-Caribbean Religions','25');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','agnosticism','1004','Agnosticism','35');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','anglican','1005','Anglican','45');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','animism','1006','Animism','55');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','assembly_of_god','1061','Assembly of God','65');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','atheism','1007','Atheism','75');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','babi_bahai_faiths','1008','Babi & Baha\'I faiths','85');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','baptist','1009','Baptist','95');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','bon','1010','Bon','105');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','brethren','1062','Brethren','115');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','cao_dai','1011','Cao Dai','125');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','celticism','1012','Celticism','135');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','christiannoncatholicnonspecifc','1013','Christian (non-Catholic, non-specific)','145');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','christian_scientist','1063','Christian Scientist','155');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','church_of_christ','1064','Church of Christ','165');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','church_of_god','1065','Church of God','175');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','confucianism','1014','Confucianism','185');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','congregational','1066','Congregational','195');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','cyberculture_religions','1015','Cyberculture Religions','205');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','disciples_of_christ','1067','Disciples of Christ','215');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','divination','1016','Divination','225');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','eastern_orthodox','1068','Eastern Orthodox','235');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','episcopalian','1069','Episcopalian','245');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','evangelical_covenant','1070','Evangelical Covenant','255');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','fourth_way','1017','Fourth Way','265');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','free_daism','1018','Free Daism','275');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','friends','1071','Friends','285');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','full_gospel','1072','Full Gospel','295');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','gnosis','1019','Gnosis','305');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','hinduism','1020','Hinduism','315');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','humanism','1021','Humanism','325');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','independent','1022','Independent','335');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','islam','1023','Islam','345');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','jainism','1024','Jainism','355');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','jehovahs_witnesses','1025','Jehovah\'s Witnesses','365');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','judaism','1026','Judaism','375');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','latter_day_saints','1027','Latter Day Saints','385');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','lutheran','1028','Lutheran','395');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','mahayana','1029','Mahayana','405');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','meditation','1030','Meditation','415');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','messianic_judaism','1031','Messianic Judaism','425');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','methodist','1073','Methodist','435');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','mitraism','1032','Mitraism','445');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','native_american','1074','Native American','455');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','nazarene','1075','Nazarene','465');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','new_age','1033','New Age','475');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','non-roman_catholic','1034','non-Roman Catholic','485');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','occult','1035','Occult','495');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','orthodox','1036','Orthodox','505');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','paganism','1037','Paganism','515');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','pentecostal','1038','Pentecostal','525');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','presbyterian','1076','Presbyterian','535');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','process_the','1039','Process, The','545');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','protestant','1077','Protestant','555');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','protestant_no_denomination','1078','Protestant, No Denomination','565');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','reformed','1079','Reformed','575');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','reformed_presbyterian','1040','Reformed/Presbyterian','585');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','roman_catholic_church','1041','Roman Catholic Church','595');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','salvation_army','1080','Salvation Army','605');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','satanism','1042','Satanism','615');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','scientology','1043','Scientology','625');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','shamanism','1044','Shamanism','635');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','shiite_islam','1045','Shiite (Islam)','645');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','shinto','1046','Shinto','655');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','sikism','1047','Sikism','665');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','spiritualism','1048','Spiritualism','675');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','sunni_islam','1049','Sunni (Islam)','685');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','taoism','1050','Taoism','695');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','theravada','1051','Theravada','705');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','unitarian_universalist','1081','Unitarian Universalist','715');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','unitarian-universalism','1052','Unitarian-Universalism','725');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','united_church_of_christ','1082','United Church of Christ','735');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','universal_life_church','1053','Universal Life Church','745');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','vajrayana_tibetan','1054','Vajrayana (Tibetan)','755');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','veda','1055','Veda','765');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','voodoo','1056','Voodoo','775');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','wicca','1057','Wicca','785');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','yaohushua','1058','Yaohushua','795');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','zen_buddhism','1059','Zen Buddhism','805');
INSERT INTO list_options (list_id, option_id, notes,title, seq) VALUES ('religious_affiliation','zoroastrianism','1060','Zoroastrianism','815');

-- Relationship
INSERT INTO list_options(list_id,option_id,title) VALUES ('lists','personal_relationship','Relationship');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','ADOPT','Adopted Child','ADOPT','10');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','AUNT','Aunt','AUNT','20');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','CHILD','Child','CHILD','30');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','CHLDINLAW','Child in-law','CHLDINLAW','40');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','COUSN','Cousin','COUSN','50');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','DOMPART','Domestic Partner','DOMPART','60');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','FAMMEMB','Family Member','FAMMEMB','70');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','CHLDFOST','Foster Child','CHLDFOST','80');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','GRNDCHILD','Grandchild','GRNDCHILD','90');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','GPARNT','Grandparent','GPARNT','100');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','GRPRN','Grandparent','GRPRN','110');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','GGRPRN','Great Grandparent','GGRPRN','120');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','HSIB','Half-Sibling','HSIB','130');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','MAUNT','MaternalAunt','MAUNT','140');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','MCOUSN','MaternalCousin','MCOUSN','150');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','MGRPRN','MaternalGrandparent','MGRPRN','160');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','MGGRPRN','MaternalGreatgrandparent','MGGRPRN','170');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','MUNCLE','MaternalUncle','MUNCLE','180');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','NCHILD','Natural Child','NCHILD','190');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','NPRN','Natural Parent','NPRN','200');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','NSIB','Natural Sibling','NSIB','210');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','NBOR','Neighbor','NBOR','220');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','NIENEPH','Niece/Nephew','NIENEPH','230');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','PRN','Parent','PRN','240');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','PRNINLAW','parent in-law','PRNINLAW','250');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','PAUNT','PaternalAunt','PAUNT','260');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','PCOUSN','PaternalCousin','PCOUSN','270');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','PGRPRN','PaternalGrandparent','PGRPRN','280');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','PGGRPRN','PaternalGreatgrandparent','PGGRPRN','290');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','PUNCLE','PaternalUncle','PUNCLE','300');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','ROOM','Roommate','ROOM','310');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','SIB','Sibling','SIB','320');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','SIBINLAW','Sibling in-law','SIBINLAW','330');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','SIGOTHR','Significant Other','SIGOTHR','340');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','SPS','Spouse','SPS','350');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','STEP','Step Child','STEP','360');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','STPPRN','Step Parent','STPPRN','370');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','STPSIB','Step Sibling','STPSIB','380');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','UNCLE','Uncle','UNCLE','390');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('personal_relationship','FRND','Unrelated Friend','FRND','400');

-- Severity
INSERT INTO list_options (list_id, option_id, title) VALUES ('lists','severity_ccda','Severity');
INSERT INTO list_options (list_id, option_id, title, codes, seq) values ('severity_ccda','unassigned','Unassigned','','10');
INSERT INTO list_options (list_id, option_id, title, codes, seq) values ('severity_ccda','mild','Mild','SNOMED-CT:255604002','20');
INSERT INTO list_options (list_id, option_id, title, codes, seq) values ('severity_ccda','mild_to_moderate','Mild to moderate','SNOMED-CT:371923003','30');
INSERT INTO list_options (list_id, option_id, title, codes, seq) values ('severity_ccda','moderate','Moderate','SNOMED-CT:6736007','40');
INSERT INTO list_options (list_id, option_id, title, codes, seq) values ('severity_ccda','moderate_to_severe','Moderate to severe','SNOMED-CT:371924009','50');
INSERT INTO list_options (list_id, option_id, title, codes, seq) values ('severity_ccda','severe','Severe','SNOMED-CT:24484000','60');
INSERT INTO list_options (list_id, option_id, title, codes, seq) values ('severity_ccda','life_threatening_severity','Life threatening severity','SNOMED-CT:442452003','70');
INSERT INTO list_options (list_id, option_id, title, codes, seq) values ('severity_ccda','fatal','Fatal','SNOMED-CT:399166001','80');

-- Physician Type

INSERT INTO list_options (list_id,option_id,title) VALUES ('lists','physician_type','Physician Type');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','attending_physician','SNOMED-CT:405279007','Attending physician', '10');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','audiological_physician','SNOMED-CT:310172001','Audiological physician', '20');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','chest_physician','SNOMED-CT:309345004','Chest physician', '30');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','community_health_physician','SNOMED-CT:23278007','Community health physician', '40');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','consultant_physician','SNOMED-CT:158967008','Consultant physician', '50');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','general_physician','SNOMED-CT:59058001','General physician', '60');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','genitourinarymedicinephysician','SNOMED-CT:309358003','Genitourinary medicine physician', '70');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','occupational_physician','SNOMED-CT:158973009','Occupational physician', '80');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','palliative_care_physician','SNOMED-CT:309359006','Palliative care physician', '90');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','physician','SNOMED-CT:309343006','Physician', '100');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','public_health_physician','SNOMED-CT:56466003','Public health physician', '110');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','rehabilitation_physician','SNOMED-CT:309360001','Rehabilitation physician', '120');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','resident_physician','SNOMED-CT:405277009','Resident physician', '130');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','specialized_physician','SNOMED-CT:69280009','Specialized physician', '140');
INSERT INTO list_options (list_id, option_id, codes,title, seq) VALUES ('physician_type','thoracic_physician','SNOMED-CT:309346003','Thoracic physician', '150');

-- Industry

INSERT INTO `list_options` (`list_id`, `option_id`, `title`) VALUES('lists','Industry','Industry');
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('Industry', 'law_firm', 'Law Firm', 10);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('Industry', 'engineering_firm', 'Engineering Firm', 20);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('Industry', 'construction_firm', 'Construction Firm', 30);

-- Occupation

INSERT INTO `list_options` (`list_id`, `option_id`, `title`) VALUES('lists','Occupation','Occupation');
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('Occupation', 'lawyer', 'Lawyer', 10);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('Occupation', 'engineer', 'Engineer', 20);
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('Occupation', 'site_worker', 'Site Worker', 30);

-- Reaction

INSERT INTO `list_options` (`list_id`, `option_id`, `title`) VALUES('lists','reaction','Reaction');
INSERT INTO list_options ( list_id, option_id, title, seq, codes ) VALUES ('reaction', 'unassigned', 'Unassigned', 10, '');
INSERT INTO list_options ( list_id, option_id, title, seq, codes ) VALUES ('reaction', 'hives', 'Hives', 20, 'SNOMED-CT:247472004');
INSERT INTO list_options ( list_id, option_id, title, seq, codes ) VALUES ('reaction', 'nausea', 'Nausea', 30, 'SNOMED-CT:422587007');
INSERT INTO list_options ( list_id, option_id, title, seq, codes ) VALUES ('reaction', 'shortness_of_breath', 'Shortness of Breath', 40, 'SNOMED-CT:267036007');

-- County

INSERT INTO list_options (list_id, option_id, title) VALUES ('lists','county','County');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('county','adair','ADAIR','001', '10');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('county','andrew','ANDREW','003', '20');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('county','atchison','ATCHISON','005', '30');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('county','audrain','AUDRAIN','007', '40');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('county','barry','BARRY','009', '50');

-- Immunization Manufacturers

INSERT INTO list_options (list_id, option_id, title) VALUES ('lists','Immunization_Manufacturer','Immunization Manufacturer');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','AB','Abbott Laboratories','AB','10');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','ACA','Acambis, Inc','ACA','20');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','AD','Adams Laboratories, Inc.','AD','30');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','AKR','Akorn, Inc','AKR','40');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','ALP','Alpha Therapeutic Corporation','ALP','50');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','AR','Armour','AR','60');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','AVB','Aventis Behring L.L.C.','AVB','70');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','AVI','Aviron','AVI','80');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','BRR','Barr Laboratories','BRR','90');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','BAH','Baxter Healthcare Corporation','BAH','100');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','BA','Baxter Healthcare Corporation-inactive','BA','110');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','BAY','Bayer Corporation','BAY','120');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','BP','Berna Products','BP','130');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','BPC','Berna Products Corporation','BPC','140');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','BTP','Biotest Pharmaceuticals Corporation','BTP','150');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','CNJ','Cangene Corporation','CNJ','160');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','CMP','Celltech Medeva Pharmaceuticals','CMP','170');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','CEN','Centeon L.L.C.','CEN','180');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','CHI','Chiron Corporation','CHI','190');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','CON','Connaught','CON','200');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','CRU','Crucell','CRU','210');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','CSL','CSL Behring, Inc','CSL','220');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','DVC','DynPort Vaccine Company, LLC','DVC','230');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','MIP','Emergent BioDefense Operations Lansing','MIP','240');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','EVN','Evans Medical Limited','EVN','250');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','GEO','GeoVax Labs, Inc.','GEO','260');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','SKB','GlaxoSmithKline','SKB','270');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','GRE','Greer Laboratories, Inc.','GRE','280');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','GRF','Grifols','GRF','290');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','IDB','ID Biomedical','IDB','300');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','IAG','Immuno International AG','IAG','310');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','IUS','Immuno-U.S., Inc.','IUS','320');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','INT','Intercell Biomedical','INT','330');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','JNJ','Johnson and Johnson','JNJ','340');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','KGC','Korea Green Cross Corporation','KGC','350');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','LED','Lederle','LED','360');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','MBL','Massachusetts Biologic Laboratories','MBL','370');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','MA','Massachusetts Public Health Biologic Laboratories','MA','380');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','MED','MedImmune, Inc.','MED','390');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','MSD','Merck and Co., Inc.','MSD','400');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','IM','Merieux','IM','410');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','MIL','Miles','MIL','420');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','NAB','NABI','NAB','430');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','NYB','New York Blood Center','NYB','440');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','NAV','North American Vaccine, Inc.','NAV','450');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','NOV','Novartis Pharmaceutical Corporation','NOV','460');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','NVX','Novavax, Inc.','NVX','470');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','OTC','Organon Teknika Corporation','OTC','480');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','ORT','Ortho-clinical Diagnostics','ORT','490');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','OTH','Other manufacturer','OTH','500');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','PD','Parkedale Pharmaceuticals','PD','510');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','PFR','Pfizer, Inc','PFR','520');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','PWJ','PowderJect Pharmaceuticals','PWJ','530');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','PRX','Praxis Biologics','PRX','540');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','PSC','Protein Sciences','PSC','550');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','PMC','sanofi pasteur','PMC','560');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','SCL','Sclavo, Inc.','SCL','570');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','SOL','Solvay Pharmaceuticals','SOL','580');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','SI','Swiss Serum and Vaccine Inst.','SI','590');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','TAL','Talecris Biotherapeutics','TAL','600');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','JPN','The Research Foundation for Microbial Diseases of Osaka University (BIKEN)','JPN','610');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','USA','United States Army Medical Research and Material Command','USA','620');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','UNK','Unknown manufacturer','UNK','630');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','VXG','VaxGen','VXG','640');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','WAL','Wyeth','WAL','650');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','WA','Wyeth-Ayerst','WA','660');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Manufacturer','ZLB','ZLB Behring','ZLB','670');

-- Immunization Completion Status

INSERT INTO list_options (list_id, option_id, title) VALUES ('lists','Immunization_Completion_Status','Immunization Completion Status');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Completion_Status','Completed','completed','CP', '10');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Completion_Status','Refused','Refused','RE', '20');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Completion_Status','Not_Administered','Not Administered','NA', '30');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('Immunization_Completion_Status','Partially_Administered','Partially Administered','PA', '40');

-- Immunization Registry Status

INSERT INTO list_options (list_id, option_id, title) VALUES ('lists','immunization_registry_status','Immunization Registry Status');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_registry_status','active','Active','A', '10');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_registry_status','inactive_lost_to_follow_up','Inactive - Lost to follow - up','L', '20');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_registry_status','inactive_moved_gone_elsewhere','Inactive - Moved or gone elsewhere','M', '30');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_registry_status','inactive_permanently_inactive','Inactive - Permanently inactive','P', '40');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_registry_status','inactive_unspecified','Inactive - Unspecified','I', '50');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_registry_status','unknown','Unknown','U', '60');

-- Publicity Code

INSERT INTO list_options (list_id, option_id, title) VALUES ('lists','publicity_code','Publicity Code');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','no_reminder_recall','No reminder/recall','SI', '10');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','reminder_recall_any_method','Reminder/recall - any method','02', '20');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','reminder_recall_no_calls','Reminder/recall - no calls','03', '30');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','reminder_only_any_method','Reminder only - any method','04', '40');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','reminder_only_no_calls','Reminder only - no calls','05', '50');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','recall_only_any_method','Recall only - any method','06', '60');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','recall_only_no_calls','Recall only - no calls','07', '70');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','reminder_recall_to_provider','Reminder/recall - to provider','08', '80');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','reminder_to_provider','Reminder to provider','09', '90');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','reminder_to_provider_no_recall','Only reminder to provider, no recall','10', '100');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','recall_to_provider','Recall to provider','11', '110');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('publicity_code','recall_to_provider_no_reminder','Only recall to provider, no reminder','12', '120');

-- Immunization Refusal Reason

INSERT INTO list_options (list_id, option_id, title) VALUES ('lists','immunization_refusal_reason','Immunization Refusal Reason');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_refusal_reason','parental_decision','Parental decision','00', '10');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_refusal_reason','religious_exemption','Religious exemption','01', '20');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_refusal_reason','other','Other','02', '30');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_refusal_reason','patient_decision','Patient decision','03', '40');

-- Immunization Information Source

INSERT INTO list_options (list_id, option_id, title) VALUES ('lists','immunization_informationsource','Immunization Information Source');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_informationsource','new_immunization_record','New Immunization Record','00', '10');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_informationsource','hist_inf_src_unspecified','Historical information -source unspecified','01', '20');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_informationsource','other_provider','Other Provider','02', '30');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_informationsource','parent_written_record','Parent Written Record','03', '40');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_informationsource','parent_recall','Parent Recall','04', '50');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_informationsource','other_registry','Other Registry','05', '60');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_informationsource','birth_certificate','Birth Certificate','06', '70');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_informationsource','school_record','School Record','07', '80');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_informationsource','public_agency','Public Agency','08', '90');

-- Next of kin Relationship
INSERT INTO `list_options` (list_id, option_id, title) VALUES ('lists','next_of_kin_relationship','Next of Kin Relationship');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','associate','Associate','10','ASC');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) values ('next_of_kin_relationship','brother','Brother','20','BRO');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','care_giver','Care giver','30','CGV');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','child','Child','40','CHD');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','handicapped_dependent','Handicapped dependent','50','DEP');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','life_partner','Life partner','60','DOM');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','emergency_contact','Emergency contact','70','EMC');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','employee','Employee','80','EME');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','employer','Employer','90','EMR');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','extended_family','Extended family','100','EXF');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','foster_child','Foster Child','110','FCH');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','friend','Friend','120','FND');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','father','Father','130','FTH');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','grandchild','Grandchild','140','GCH');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','guardian','Guardian','150','GRD');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','grandparent','Grandparent','160','GRP');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','manager','Manager','170','MGR');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','mother','Mother','180','MTH');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','natural_child','Natural child','190','NCH');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','none','None','200','NON');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','other_adult','Other adult','210','OAD');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','other','Other','220','OTH');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','owner','Owner','230','OWN');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','parent','Parent','240','PAR');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','stepchild','Stepchild','250','SCH');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','self','Self','260','SEL');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','sibling','Sibling','270','SIB');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','sister','Sister','280','SIS');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','spouse','Spouse','290','SPO');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','trainer','Trainer','300','TRA');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','unknown','Unknown','310','UNK');
INSERT INTO `list_options` (list_id, option_id, title, seq, notes) VALUES ('next_of_kin_relationship','ward_of_court','Ward of court','320','WRD');

-- Immunization Administered Site
INSERT INTO list_options (list_id, option_id, title) VALUES ('lists','immunization_administered_site','Immunization Administered Site');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','left_thigh','Left Thigh','LT', '10');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','left_arm','Left Arm','LA', '20');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','left_deltoid','Left Deltoid','LD', '30');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','left_gluteus_medius','Left Gluteus Medius','LG', '40');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','left_vastus_lateralis','Left Vastus Lateralis','LVL', '50');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','left_lower_forearm','Left Lower Forearm','LLFA', '60');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','nose','Nose','Nose', '70');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','right_arm','Right Arm','RA', '80');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','right_thigh','Right Thigh','RT', '90');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','right_vastus_lateralis','Right Vastus Lateralis','RVL', '100');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','right_gluteus_medius','Right Gluteus Medius','RG', '110');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','right_deltoid','Right Deltoid','RD', '120');
INSERT INTO list_options (list_id, option_id, title, notes, seq) VALUES ('immunization_administered_site','right_lower_forearm','Right Lower Forearm','RLFA', '130');

-- Immunization Observation Criteria
INSERT INTO `list_options`(`list_id`, `option_id`, `title`) VALUES ('lists','immunization_observation','Immunization Observation Criteria');
INSERT INTO `list_options`(`list_id`, `option_id`, `title`, `seq`, `notes`, `codes`) VALUES ('immunization_observation','funding_program_eligibility','Vaccine funding program eligibility category','10','LN','LOINC:64994-7');
INSERT INTO `list_options`(`list_id`, `option_id`, `title`, `seq`, `notes`, `codes`) VALUES ('immunization_observation','vaccine_type','Vaccine Type','20','LN','LOINC:30956-7');
INSERT INTO `list_options`(`list_id`, `option_id`, `title`, `seq`, `notes`, `codes`) VALUES ('immunization_observation','disease_with_presumed_immunity','Disease with presumed immunity','30','LN','LOINC:59784-9');

-- Immunization Vaccine Eligibility Results
INSERT INTO `list_options`(`list_id`, `option_id`, `title`) VALUES ('lists','imm_vac_eligibility_results','Immunization Vaccine Eligibility Results');
INSERT INTO `list_options`(list_id, option_id, title, seq, notes) VALUES ('imm_vac_eligibility_results','not_vfc_eligible','Not VFC eligible','10','V01');
INSERT INTO `list_options`(list_id, option_id, title, seq, notes) VALUES ('imm_vac_eligibility_results','medicaid_managed_care','VFC eligible-Medicaid/Medicaid Managed Care','20','V02');
INSERT INTO `list_options`(list_id, option_id, title, seq, notes) VALUES ('imm_vac_eligibility_results','uninsured','VFC eligible- Uninsured','30','V03');
INSERT INTO `list_options`(list_id, option_id, title, seq, notes) VALUES ('imm_vac_eligibility_results','american_indian_alaskan_native','VFC eligible- American Indian/Alaskan Native','40','V04');
INSERT INTO `list_options`(list_id, option_id, title, seq, notes) VALUES ('imm_vac_eligibility_results','health_center_patient','VFC eligible-Federally Qualified Health Center Patient (under-insured)','50','V05');

--  LBF Validation

INSERT INTO `list_options` ( list_id, option_id, title) VALUES ( 'lists','LBF_Validations','LBF_Validations');
INSERT INTO `list_options` (`list_id`,`option_id`,`title`,`notes`, `seq`) VALUES ('LBF_Validations','int1','Integers1-100','{\"numericality\": {\"onlyInteger\": true,\"greaterThanOrEqualTo\": 1,\"lessThanOrEqualTo\":100}}','10');
INSERT INTO `list_options` (`list_id`,`option_id`,`title`,`notes`, `seq`) VALUES ('LBF_Validations','names','Names','{"format\":{\"pattern\":\"[a-zA-z]+([ \'-\\\\s][a-zA-Z]+)*\"}}','20');
INSERT INTO `list_options` (`list_id`,`option_id`,`title`,`notes`, `seq`) VALUES ('LBF_Validations','past_date','Past Date','{\"pastDate\":{\"message\":\"must be past date\"}}','30');
INSERT INTO `list_options` (`list_id`,`option_id`,`title`,`notes`,`seq`) VALUES ('LBF_Validations','past_year','Past Year','{\"date\":{\"dateOnly\":true},\"pastDate\":{\"onlyYear\":true}}','35');
INSERT INTO `list_options` (`list_id`,`option_id`,`title`,`notes`,`seq`) VALUES ('LBF_Validations','email','E-Mail','{\"email\":true}','40');
INSERT INTO `list_options` (`list_id`,`option_id`,`title`,`notes`,`seq`) VALUES ('LBF_Validations','url','URL','{\"url\":true}','50');
INSERT INTO `list_options` (`list_id`,`option_id`,`title`,`notes`,`seq`) VALUES ('LBF_Validations','luhn','Luhn','{"numericality": {"onlyInteger": true}, "luhn":true}','80');

--  Form Keys

INSERT INTO list_options (list_id, option_id, title,activity) VALUES ('lists','formdir_keys','Form Keys',1);
INSERT INTO list_options (list_id,option_id,title,seq,notes,activity) VALUES ('formdir_keys','newpatient','"tbl":"form_encounter"',10,'Patient encounter table has non-std name',1);
INSERT INTO list_options (list_id,option_id,title,seq,notes,activity) VALUES ('formdir_keys','procedure_order','"tbl":"procedure_order","id":"procedure_order_id"',20,'Lab order header table has non-std name and id',1);
INSERT INTO list_options (list_id,option_id,title,seq,notes,activity) VALUES ('formdir_keys','physical_exam','"id":"forms_id","limit":"*"',30,'Physical exam form table has non-std id and n records',1);


-- --------------------------------------------------------
-- provider_qualifier_code

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','provider_qualifier_code','Provider Qualifier Code', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('provider_qualifier_code','dk','DK',10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('provider_qualifier_code','dn','DN',20,0);

--
-- Table structure for table `lists`
--

DROP TABLE IF EXISTS `lists`;
CREATE TABLE `lists` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `type` varchar(255) default NULL,
  `subtype` varchar(31) NOT NULL DEFAULT '',
  `title` varchar(255) default NULL,
  `begdate` date default NULL,
  `enddate` date default NULL,
  `returndate` date default NULL,
  `occurrence` int(11) default '0',
  `classification` int(11) default '0',
  `referredby` varchar(255) default NULL,
  `extrainfo` varchar(255) default NULL,
  `diagnosis` varchar(255) default NULL,
  `activity` tinyint(4) default NULL,
  `comments` longtext,
  `pid` bigint(20) default NULL,
  `user` varchar(255) default NULL,
  `groupname` varchar(255) default NULL,
  `outcome` int(11) NOT NULL default '0',
  `destination` varchar(255) default NULL,
  `reinjury_id` bigint(20)  NOT NULL DEFAULT 0,
  `injury_part` varchar(31) NOT NULL DEFAULT '',
  `injury_type` varchar(31) NOT NULL DEFAULT '',
  `injury_grade` varchar(31) NOT NULL DEFAULT '',
  `reaction` varchar(255) NOT NULL DEFAULT '',
  `external_allergyid` INT(11) DEFAULT NULL,
  `erx_source` ENUM('0','1') DEFAULT '0' NOT NULL  COMMENT '0-OpenEMR 1-External',
  `erx_uploaded` ENUM('0','1') DEFAULT '0' NOT NULL  COMMENT '0-Pending NewCrop upload 1-Uploaded TO NewCrop',
  `modifydate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `severity_al` VARCHAR( 50 ) DEFAULT NULL,
  `external_id` VARCHAR(20) DEFAULT NULL,
  PRIMARY KEY  (`id`),
  KEY `pid` (`pid`),
  KEY `type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `lists_touch`
--

DROP TABLE IF EXISTS `lists_touch`;
CREATE TABLE `lists_touch` (
  `pid` bigint(20) NOT NULL default '0',
  `type` varchar(255) NOT NULL default '',
  `date` datetime default NULL,
  PRIMARY KEY  (`pid`,`type`)
) ENGINE=InnoDB ;

-- --------------------------------------------------------

--
-- Table structure for table `log`
--

DROP TABLE IF EXISTS `log`;
CREATE TABLE `log` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `event` varchar(255) default NULL,
  `category` varchar(255) default NULL,
  `user` varchar(255) default NULL,
  `groupname` varchar(255) default NULL,
  `comments` longtext,
  `user_notes` longtext,
  `patient_id` bigint(20) default NULL,
  `success` tinyint(1) default 1,
  `checksum` longtext,
  `crt_user` varchar(255) default NULL,
  `log_from` VARCHAR(20) DEFAULT 'open-emr',
  `menu_item_id` INT(11) DEFAULT NULL,
  `ccda_doc_id` INT(11) DEFAULT NULL COMMENT 'CCDA document id from ccda',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;


-- --------------------------------------------------------

--
-- Table structure for table `modules`
--
CREATE TABLE `modules` (
  `mod_id` INT(11) NOT NULL AUTO_INCREMENT,
  `mod_name` VARCHAR(64) NOT NULL DEFAULT '0',
  `mod_directory` VARCHAR(64) NOT NULL DEFAULT '',
  `mod_parent` VARCHAR(64) NOT NULL DEFAULT '',
  `mod_type` VARCHAR(64) NOT NULL DEFAULT '',
  `mod_active` INT(1) UNSIGNED NOT NULL DEFAULT '0',
  `mod_ui_name` VARCHAR(20) NOT NULL DEFAULT '''',
  `mod_relative_link` VARCHAR(64) NOT NULL DEFAULT '',
  `mod_ui_order` TINYINT(3) NOT NULL DEFAULT '0',
  `mod_ui_active` INT(1) UNSIGNED NOT NULL DEFAULT '0',
  `mod_description` VARCHAR(255) NOT NULL DEFAULT '',
  `mod_nick_name` VARCHAR(25) NOT NULL DEFAULT '',
  `mod_enc_menu` VARCHAR(10) NOT NULL DEFAULT 'no',
  `permissions_item_table` CHAR(100) DEFAULT NULL,
  `directory` VARCHAR(255) NOT NULL,
  `date` DATETIME NOT NULL,
  `sql_run` TINYINT(4) DEFAULT '0',
  `type` TINYINT(4) DEFAULT '0',
  PRIMARY KEY (`mod_id`,`mod_directory`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `module_acl_group_settings`
--
CREATE TABLE `module_acl_group_settings` (
  `module_id` int(11) NOT NULL,
  `group_id` int(11) NOT NULL,
  `section_id` int(11) NOT NULL,
  `allowed` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`module_id`,`group_id`,`section_id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `module_acl_sections`
--
CREATE TABLE `module_acl_sections` (
  `section_id` int(11) DEFAULT NULL,
  `section_name` varchar(255) DEFAULT NULL,
  `parent_section` int(11) DEFAULT NULL,
  `section_identifier` varchar(50) DEFAULT NULL,
  `module_id` int(11) DEFAULT NULL
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `module_acl_user_settings`
--
CREATE TABLE `module_acl_user_settings` (
  `module_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `section_id` int(11) NOT NULL,
  `allowed` int(1) DEFAULT NULL,
  PRIMARY KEY (`module_id`,`user_id`,`section_id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `module_configuration`
--
CREATE TABLE `module_configuration` (
  `module_config_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `module_id` int(10) unsigned NOT NULL,
  `field_name` varchar(45) NOT NULL,
  `field_value` varchar(255) NOT NULL,
  PRIMARY KEY (`module_config_id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `modules_hooks_settings`
--
CREATE TABLE `modules_hooks_settings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mod_id` int(11) DEFAULT NULL,
  `enabled_hooks` varchar(255) DEFAULT NULL,
  `attached_to` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `modules_settings`
--
CREATE TABLE `modules_settings` (
  `mod_id` INT(11) DEFAULT NULL,
  `fld_type` SMALLINT(6) DEFAULT NULL COMMENT '1=>ACL,2=>preferences,3=>hooks',
  `obj_name` VARCHAR(255) DEFAULT NULL,
  `menu_name` VARCHAR(255) DEFAULT NULL,
  `path` VARCHAR(255) DEFAULT NULL
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `notes`
--

DROP TABLE IF EXISTS `notes`;
CREATE TABLE `notes` (
  `id` int(11) NOT NULL default '0',
  `foreign_id` int(11) NOT NULL default '0',
  `note` varchar(255) default NULL,
  `owner` int(11) default NULL,
  `date` datetime default NULL,
  `revision` timestamp NOT NULL,
  PRIMARY KEY  (`id`),
  KEY `foreign_id` (`owner`),
  KEY `foreign_id_2` (`foreign_id`),
  KEY `date` (`date`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `onotes`
--

DROP TABLE IF EXISTS `onotes`;
CREATE TABLE `onotes` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `body` longtext,
  `user` varchar(255) default NULL,
  `groupname` varchar(255) default NULL,
  `activity` tinyint(4) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `openemr_module_vars`
--

DROP TABLE IF EXISTS `openemr_module_vars`;
CREATE TABLE `openemr_module_vars` (
  `pn_id` int(11) unsigned NOT NULL auto_increment,
  `pn_modname` varchar(64) default NULL,
  `pn_name` varchar(64) default NULL,
  `pn_value` longtext,
  PRIMARY KEY  (`pn_id`),
  KEY `pn_modname` (`pn_modname`),
  KEY `pn_name` (`pn_name`)
) ENGINE=InnoDB AUTO_INCREMENT=235 ;

--
-- Dumping data for table `openemr_module_vars`
--

INSERT INTO `openemr_module_vars` VALUES (234, 'PostCalendar', 'pcNotifyEmail', '');
INSERT INTO `openemr_module_vars` VALUES (233, 'PostCalendar', 'pcNotifyAdmin', '0');
INSERT INTO `openemr_module_vars` VALUES (232, 'PostCalendar', 'pcCacheLifetime', '3600');
INSERT INTO `openemr_module_vars` VALUES (231, 'PostCalendar', 'pcUseCache', '0');
INSERT INTO `openemr_module_vars` VALUES (230, 'PostCalendar', 'pcDefaultView', 'day');
INSERT INTO `openemr_module_vars` VALUES (229, 'PostCalendar', 'pcTimeIncrement', '5');
INSERT INTO `openemr_module_vars` VALUES (228, 'PostCalendar', 'pcAllowUserCalendar', '1');
INSERT INTO `openemr_module_vars` VALUES (227, 'PostCalendar', 'pcAllowSiteWide', '1');
INSERT INTO `openemr_module_vars` VALUES (226, 'PostCalendar', 'pcTemplate', 'default');
INSERT INTO `openemr_module_vars` VALUES (225, 'PostCalendar', 'pcEventDateFormat', '%Y-%m-%d');
INSERT INTO `openemr_module_vars` VALUES (224, 'PostCalendar', 'pcDisplayTopics', '0');
INSERT INTO `openemr_module_vars` VALUES (223, 'PostCalendar', 'pcListHowManyEvents', '15');
INSERT INTO `openemr_module_vars` VALUES (222, 'PostCalendar', 'pcAllowDirectSubmit', '1');
INSERT INTO `openemr_module_vars` VALUES (221, 'PostCalendar', 'pcUsePopups', '0');
INSERT INTO `openemr_module_vars` VALUES (220, 'PostCalendar', 'pcDayHighlightColor', '#EEEEEE');
INSERT INTO `openemr_module_vars` VALUES (219, 'PostCalendar', 'pcFirstDayOfWeek', '1');
INSERT INTO `openemr_module_vars` VALUES (218, 'PostCalendar', 'pcUseInternationalDates', '0');
INSERT INTO `openemr_module_vars` VALUES (217, 'PostCalendar', 'pcEventsOpenInNewWindow', '0');
INSERT INTO `openemr_module_vars` VALUES (216, 'PostCalendar', 'pcTime24Hours', '0');

-- --------------------------------------------------------

--
-- Table structure for table `openemr_modules`
--

DROP TABLE IF EXISTS `openemr_modules`;
CREATE TABLE `openemr_modules` (
  `pn_id` int(11) unsigned NOT NULL auto_increment,
  `pn_name` varchar(64) default NULL,
  `pn_type` int(6) NOT NULL default '0',
  `pn_displayname` varchar(64) default NULL,
  `pn_description` varchar(255) default NULL,
  `pn_regid` int(11) unsigned NOT NULL default '0',
  `pn_directory` varchar(64) default NULL,
  `pn_version` varchar(10) default NULL,
  `pn_admin_capable` tinyint(1) NOT NULL default '0',
  `pn_user_capable` tinyint(1) NOT NULL default '0',
  `pn_state` tinyint(1) NOT NULL default '0',
  PRIMARY KEY  (`pn_id`)
) ENGINE=InnoDB AUTO_INCREMENT=47 ;

--
-- Dumping data for table `openemr_modules`
--

INSERT INTO `openemr_modules` VALUES (46, 'PostCalendar', 2, 'PostCalendar', 'PostNuke Calendar Module', 0, 'PostCalendar', '4.0.0', 1, 1, 3);

-- --------------------------------------------------------

--
-- Table structure for table `openemr_postcalendar_categories`
--

DROP TABLE IF EXISTS `openemr_postcalendar_categories`;
CREATE TABLE `openemr_postcalendar_categories` (
  `pc_catid` int(11) unsigned NOT NULL auto_increment,
  `pc_catname` varchar(100) default NULL,
  `pc_catcolor` varchar(50) default NULL,
  `pc_catdesc` text,
  `pc_recurrtype` int(1) NOT NULL default '0',
  `pc_enddate` date default NULL,
  `pc_recurrspec` text,
  `pc_recurrfreq` int(3) NOT NULL default '0',
  `pc_duration` bigint(20) NOT NULL default '0',
  `pc_end_date_flag` tinyint(1) NOT NULL default '0',
  `pc_end_date_type` int(2) default NULL,
  `pc_end_date_freq` int(11) NOT NULL default '0',
  `pc_end_all_day` tinyint(1) NOT NULL default '0',
  `pc_dailylimit` int(2) NOT NULL default '0',
  `pc_cattype` INT( 11 ) NOT NULL COMMENT 'Used in grouping categories',
  `pc_active` tinyint(1) NOT NULL default 1,
  `pc_seq` int(11) NOT NULL default '0',
  PRIMARY KEY  (`pc_catid`),
  KEY `basic_cat` (`pc_catname`,`pc_catcolor`)
) ENGINE=InnoDB AUTO_INCREMENT=11 ;

--
-- Dumping data for table `openemr_postcalendar_categories`
--

INSERT INTO `openemr_postcalendar_categories` VALUES (5, 'Office Visit', '#FFFFCC', 'Normal Office Visit', 0, NULL, 'a:5:{s:17:"event_repeat_freq";s:1:"0";s:22:"event_repeat_freq_type";s:1:"0";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}', 0, 900, 0, 0, 0, 0, 0,0,1,5);
INSERT INTO `openemr_postcalendar_categories` VALUES (4, 'Vacation', '#EFEFEF', 'Reserved for use to define Scheduled Vacation Time', 0, NULL, 'a:5:{s:17:"event_repeat_freq";s:1:"0";s:22:"event_repeat_freq_type";s:1:"0";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}', 0, 0, 0, 0, 0, 1, 0, 1,1,4);
INSERT INTO `openemr_postcalendar_categories` VALUES (1, 'No Show', '#DDDDDD', 'Reserved to define when an event did not occur as specified.', 0, NULL, 'a:5:{s:17:"event_repeat_freq";s:1:"0";s:22:"event_repeat_freq_type";s:1:"0";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}', 0, 0, 0, 0, 0, 0, 0, 0,1,1);
INSERT INTO `openemr_postcalendar_categories` VALUES (2, 'In Office', '#99CCFF', 'Reserved todefine when a provider may haveavailable appointments after.', 1, NULL, 'a:5:{s:17:"event_repeat_freq";s:1:"1";s:22:"event_repeat_freq_type";s:1:"4";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}', 0, 0, 1, 3, 2, 0, 0, 1,1,2);
INSERT INTO `openemr_postcalendar_categories` VALUES (3, 'Out Of Office', '#99FFFF', 'Reserved to define when a provider may not have available appointments after.', 1, NULL, 'a:5:{s:17:"event_repeat_freq";s:1:"1";s:22:"event_repeat_freq_type";s:1:"4";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}', 0, 0, 1, 3, 2, 0, 0, 1,1,3);
INSERT INTO `openemr_postcalendar_categories` VALUES (6,'Holidays','#9676DB','Clinic holiday',0,NULL,'a:5:{s:17:"event_repeat_freq";s:1:"1";s:22:"event_repeat_freq_type";s:1:"4";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}',0,86400,1,3,2,0,0,2,1,6);
INSERT INTO `openemr_postcalendar_categories` VALUES (7,'Closed','#2374AB','Clinic closed',0,NULL,'a:5:{s:17:"event_repeat_freq";s:1:"1";s:22:"event_repeat_freq_type";s:1:"4";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}',0,86400,1,3,2,0,0,2,1,7);
INSERT INTO `openemr_postcalendar_categories` VALUES (8, 'Lunch', '#FFFF33', 'Lunch', 1, NULL, 'a:5:{s:17:"event_repeat_freq";s:1:"1";s:22:"event_repeat_freq_type";s:1:"4";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}', 0, 3600, 0, 3, 2, 0, 0, 1,1,8);
INSERT INTO `openemr_postcalendar_categories` VALUES (9, 'Established Patient', '#CCFF33', '', 0, NULL, 'a:5:{s:17:"event_repeat_freq";s:1:"0";s:22:"event_repeat_freq_type";s:1:"0";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}', 0, 900, 0, 0, 0, 0, 0, 0,1,9);
INSERT INTO `openemr_postcalendar_categories` VALUES (10,'New Patient', '#CCFFFF', '', 0, NULL, 'a:5:{s:17:"event_repeat_freq";s:1:"0";s:22:"event_repeat_freq_type";s:1:"0";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}', 0, 1800, 0, 0, 0, 0, 0, 0,1,10);
INSERT INTO `openemr_postcalendar_categories` VALUES (11,'Reserved','#FF7777','Reserved',1,NULL,'a:5:{s:17:\"event_repeat_freq\";s:1:\"1\";s:22:\"event_repeat_freq_type\";s:1:\"4\";s:19:\"event_repeat_on_num\";s:1:\"1\";s:19:\"event_repeat_on_day\";s:1:\"0\";s:20:\"event_repeat_on_freq\";s:1:\"0\";}',0,900,0,3,2,0,0, 1,1,11);
INSERT INTO `openemr_postcalendar_categories` VALUES (12, 'Health and Behavioral Assessment', '#C7C7C7', 'Health and Behavioral Assessment', 0, NULL, 'a:5:{s:17:"event_repeat_freq";s:1:"0";s:22:"event_repeat_freq_type";s:1:"0";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}', 0, 900, 0, 0, 0, 0, 0,0,1,12);
INSERT INTO `openemr_postcalendar_categories` VALUES (13, 'Preventive Care Services', '#CCCCFF', 'Preventive Care Services', 0, NULL, 'a:5:{s:17:"event_repeat_freq";s:1:"0";s:22:"event_repeat_freq_type";s:1:"0";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}', 0, 900, 0, 0, 0, 0, 0,0,1,13);

INSERT INTO `openemr_postcalendar_categories` VALUES (14, 'Ophthalmological Services', '#F89219', 'Ophthalmological Services', 0, NULL, 'a:5:{s:17:"event_repeat_freq";s:1:"0";s:22:"event_repeat_freq_type";s:1:"0";s:19:"event_repeat_on_num";s:1:"1";s:19:"event_repeat_on_day";s:1:"0";s:20:"event_repeat_on_freq";s:1:"0";}', 0, 900, 0, 0, 0, 0, 0,0,1,14);
-- --------------------------------------------------------

--
-- Table structure for table `openemr_postcalendar_events`
--

DROP TABLE IF EXISTS `openemr_postcalendar_events`;
CREATE TABLE `openemr_postcalendar_events` (
  `pc_eid` int(11) unsigned NOT NULL auto_increment,
  `pc_catid` int(11) NOT NULL default '0',
  `pc_multiple` int(10) unsigned NOT NULL,
  `pc_aid` varchar(30) default NULL,
  `pc_pid` varchar(11) default NULL,
  `pc_title` varchar(150) default NULL,
  `pc_time` datetime default NULL,
  `pc_hometext` text,
  `pc_comments` int(11) default '0',
  `pc_counter` mediumint(8) unsigned default '0',
  `pc_topic` int(3) NOT NULL default '1',
  `pc_informant` varchar(20) default NULL,
  `pc_eventDate` date NOT NULL default '0000-00-00',
  `pc_endDate` date NOT NULL default '0000-00-00',
  `pc_duration` bigint(20) NOT NULL default '0',
  `pc_recurrtype` int(1) NOT NULL default '0',
  `pc_recurrspec` text,
  `pc_recurrfreq` int(3) NOT NULL default '0',
  `pc_startTime` time default NULL,
  `pc_endTime` time default NULL,
  `pc_alldayevent` int(1) NOT NULL default '0',
  `pc_location` text,
  `pc_conttel` varchar(50) default NULL,
  `pc_contname` varchar(50) default NULL,
  `pc_contemail` varchar(255) default NULL,
  `pc_website` varchar(255) default NULL,
  `pc_fee` varchar(50) default NULL,
  `pc_eventstatus` int(11) NOT NULL default '0',
  `pc_sharing` int(11) NOT NULL default '0',
  `pc_language` varchar(30) default NULL,
  `pc_apptstatus` varchar(15) NOT NULL default '-',
  `pc_prefcatid` int(11) NOT NULL default '0',
  `pc_facility` smallint(6) NOT NULL default '0' COMMENT 'facility id for this event',
  `pc_sendalertsms` VARCHAR(3) NOT NULL DEFAULT 'NO',
  `pc_sendalertemail` VARCHAR( 3 ) NOT NULL DEFAULT 'NO',
  `pc_billing_location` SMALLINT (6) NOT NULL DEFAULT '0',
  `pc_room` varchar(20) NOT NULL DEFAULT '',
  PRIMARY KEY  (`pc_eid`),
  KEY `basic_event` (`pc_catid`,`pc_aid`,`pc_eventDate`,`pc_endDate`,`pc_eventstatus`,`pc_sharing`,`pc_topic`),
  KEY `pc_eventDate` (`pc_eventDate`)
) ENGINE=InnoDB AUTO_INCREMENT=7 ;

-- --------------------------------------------------------

--
-- Table structure for table `openemr_postcalendar_limits`
--

DROP TABLE IF EXISTS `openemr_postcalendar_limits`;
CREATE TABLE `openemr_postcalendar_limits` (
  `pc_limitid` int(11) NOT NULL auto_increment,
  `pc_catid` int(11) NOT NULL default '0',
  `pc_starttime` time NOT NULL default '00:00:00',
  `pc_endtime` time NOT NULL default '00:00:00',
  `pc_limit` int(11) NOT NULL default '1',
  PRIMARY KEY  (`pc_limitid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `openemr_postcalendar_topics`
--

DROP TABLE IF EXISTS `openemr_postcalendar_topics`;
CREATE TABLE `openemr_postcalendar_topics` (
  `pc_catid` int(11) unsigned NOT NULL auto_increment,
  `pc_catname` varchar(100) default NULL,
  `pc_catcolor` varchar(50) default NULL,
  `pc_catdesc` text,
  PRIMARY KEY  (`pc_catid`),
  KEY `basic_cat` (`pc_catname`,`pc_catcolor`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `openemr_session_info`
--

DROP TABLE IF EXISTS `openemr_session_info`;
CREATE TABLE `openemr_session_info` (
  `pn_sessid` varchar(32) NOT NULL default '',
  `pn_ipaddr` varchar(20) default NULL,
  `pn_firstused` int(11) NOT NULL default '0',
  `pn_lastused` int(11) NOT NULL default '0',
  `pn_uid` int(11) NOT NULL default '0',
  `pn_vars` blob,
  PRIMARY KEY  (`pn_sessid`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `patient_access_onsite`
--

DROP TABLE IF EXISTS `patient_access_onsite`;
CREATE TABLE `patient_access_onsite`(
  `id` INT NOT NULL AUTO_INCREMENT ,
  `pid` INT(11),
  `portal_username` VARCHAR(100) ,
  `portal_pwd` VARCHAR(100) ,
  `portal_pwd_status` TINYINT DEFAULT '1' COMMENT '0=>Password Created Through Demographics by The provider or staff. Patient Should Change it at first time it.1=>Pwd updated or created by patient itself',
  `portal_salt` VARCHAR(100) ,
  PRIMARY KEY (`id`)
)ENGINE=InnoDB AUTO_INCREMENT=1;

-- --------------------------------------------------------

--
-- Table structure for table `patient_data`
--

DROP TABLE IF EXISTS `patient_data`;
CREATE TABLE `patient_data` (
  `id` bigint(20) NOT NULL auto_increment,
  `title` varchar(255) NOT NULL default '',
  `language` varchar(255) NOT NULL default '',
  `financial` varchar(255) NOT NULL default '',
  `fname` varchar(255) NOT NULL default '',
  `lname` varchar(255) NOT NULL default '',
  `mname` varchar(255) NOT NULL default '',
  `DOB` date default NULL,
  `street` varchar(255) NOT NULL default '',
  `postal_code` varchar(255) NOT NULL default '',
  `city` varchar(255) NOT NULL default '',
  `state` varchar(255) NOT NULL default '',
  `country_code` varchar(255) NOT NULL default '',
  `drivers_license` varchar(255) NOT NULL default '',
  `ss` varchar(255) NOT NULL default '',
  `occupation` longtext,
  `phone_home` varchar(255) NOT NULL default '',
  `phone_biz` varchar(255) NOT NULL default '',
  `phone_contact` varchar(255) NOT NULL default '',
  `phone_cell` varchar(255) NOT NULL default '',
  `pharmacy_id` int(11) NOT NULL default '0',
  `status` varchar(255) NOT NULL default '',
  `contact_relationship` varchar(255) NOT NULL default '',
  `date` datetime default NULL,
  `sex` varchar(255) NOT NULL default '',
  `referrer` varchar(255) NOT NULL default '',
  `referrerID` varchar(255) NOT NULL default '',
  `providerID` int(11) default NULL,
  `ref_providerID` int(11) default NULL,
  `email` varchar(255) NOT NULL default '',
  `email_direct` varchar(255) NOT NULL default '',
  `ethnoracial` varchar(255) NOT NULL default '',
  `race` varchar(255) NOT NULL default '',
  `ethnicity` varchar(255) NOT NULL default '',
  `religion` varchar(40) NOT NULL default '',
  `interpretter` varchar(255) NOT NULL default '',
  `migrantseasonal` varchar(255) NOT NULL default '',
  `family_size` varchar(255) NOT NULL default '',
  `monthly_income` varchar(255) NOT NULL default '',
  `billing_note` text,
  `homeless` varchar(255) NOT NULL default '',
  `financial_review` datetime default NULL,
  `pubpid` varchar(255) NOT NULL default '',
  `pid` bigint(20) NOT NULL default '0',
  `genericname1` varchar(255) NOT NULL default '',
  `genericval1` varchar(255) NOT NULL default '',
  `genericname2` varchar(255) NOT NULL default '',
  `genericval2` varchar(255) NOT NULL default '',
  `hipaa_mail` varchar(3) NOT NULL default '',
  `hipaa_voice` varchar(3) NOT NULL default '',
  `hipaa_notice` varchar(3) NOT NULL default '',
  `hipaa_message` varchar(20) NOT NULL default '',
  `hipaa_allowsms` VARCHAR(3) NOT NULL DEFAULT 'NO',
  `hipaa_allowemail` VARCHAR(3) NOT NULL DEFAULT 'NO',
  `squad` varchar(32) NOT NULL default '',
  `fitness` int(11) NOT NULL default '0',
  `referral_source` varchar(30) NOT NULL default '',
  `usertext1` varchar(255) NOT NULL DEFAULT '',
  `usertext2` varchar(255) NOT NULL DEFAULT '',
  `usertext3` varchar(255) NOT NULL DEFAULT '',
  `usertext4` varchar(255) NOT NULL DEFAULT '',
  `usertext5` varchar(255) NOT NULL DEFAULT '',
  `usertext6` varchar(255) NOT NULL DEFAULT '',
  `usertext7` varchar(255) NOT NULL DEFAULT '',
  `usertext8` varchar(255) NOT NULL DEFAULT '',
  `userlist1` varchar(255) NOT NULL DEFAULT '',
  `userlist2` varchar(255) NOT NULL DEFAULT '',
  `userlist3` varchar(255) NOT NULL DEFAULT '',
  `userlist4` varchar(255) NOT NULL DEFAULT '',
  `userlist5` varchar(255) NOT NULL DEFAULT '',
  `userlist6` varchar(255) NOT NULL DEFAULT '',
  `userlist7` varchar(255) NOT NULL DEFAULT '',
  `pricelevel` varchar(255) NOT NULL default 'standard',
  `regdate`     date DEFAULT NULL COMMENT 'Registration Date',
  `contrastart` date DEFAULT NULL COMMENT 'Date contraceptives initially used',
  `completed_ad` VARCHAR(3) NOT NULL DEFAULT 'NO',
  `ad_reviewed` date DEFAULT NULL,
  `vfc` varchar(255) NOT NULL DEFAULT '',
  `mothersname` varchar(255) NOT NULL DEFAULT '',
  `guardiansname` TEXT,
  `allow_imm_reg_use` varchar(255) NOT NULL DEFAULT '',
  `allow_imm_info_share` varchar(255) NOT NULL DEFAULT '',
  `allow_health_info_ex` varchar(255) NOT NULL DEFAULT '',
  `allow_patient_portal` varchar(31) NOT NULL DEFAULT '',
  `deceased_date` datetime default NULL,
  `deceased_reason` varchar(255) NOT NULL default '',
  `soap_import_status` TINYINT(4) DEFAULT NULL COMMENT '1-Prescription Press 2-Prescription Import 3-Allergy Press 4-Allergy Import',
  `cmsportal_login` varchar(60) NOT NULL default '',
  `care_team` int(11) DEFAULT NULL,
  `county` varchar(40) NOT NULL default '',
  `industry` TEXT,
  `imm_reg_status` TEXT,
  `imm_reg_stat_effdate` TEXT,
  `publicity_code` TEXT,
  `publ_code_eff_date` TEXT,
  `protect_indicator` TEXT,
  `prot_indi_effdate` TEXT,
  `guardianrelationship` TEXT,
  `guardiansex` TEXT,
  `guardianaddress` TEXT,
  `guardiancity` TEXT,
  `guardianstate` TEXT,
  `guardianpostalcode` TEXT,
  `guardiancountry` TEXT,
  `guardianphone` TEXT,
  `guardianworkphone` TEXT,
  `guardianemail` TEXT,
  UNIQUE KEY `pid` (`pid`),
  KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;
-- --------------------------------------------------------

--
-- Table structure for table `patient_portal_menu`
--

CREATE TABLE `patient_portal_menu` (
  `patient_portal_menu_id` INT(11) NOT NULL AUTO_INCREMENT,
  `patient_portal_menu_group_id` INT(11) DEFAULT NULL,
  `menu_name` VARCHAR(40) DEFAULT NULL,
  `menu_order` SMALLINT(4) DEFAULT NULL,
  `menu_status` TINYINT(2) DEFAULT '1',
  PRIMARY KEY (`patient_portal_menu_id`)
) ENGINE=INNODB AUTO_INCREMENT=14;

INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (1,1,'Dashboard',3,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (2,1,'My Profile',6,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (3,1,'Appointments',9,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (4,1,'Documents',12,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (5,1,'Med Records',15,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (6,1,'My Account',18,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (7,1,'Mailbox',21,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (8,1,'Password',24,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (9,1,'View Log',27,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (10,1,'Logout',30,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (11,1,'View Health Information',33,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (12,1,'Download Health Information',36,1);
INSERT  INTO `patient_portal_menu`(`patient_portal_menu_id`,`patient_portal_menu_group_id`,`menu_name`,`menu_order`,`menu_status`) VALUES (13,1,'Transmit Health Information',39,1);
-- --------------------------------------------------------

--
-- Table structure for table `patient_reminders`
--

DROP TABLE IF EXISTS `patient_reminders`;
CREATE TABLE `patient_reminders` (
  `id` bigint(20) NOT NULL auto_increment,
  `active` tinyint(1) NOT NULL default 1 COMMENT '1 if active and 0 if not active',
  `date_inactivated` datetime DEFAULT NULL,
  `reason_inactivated` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to list_options list rule_reminder_inactive_opt',
  `due_status` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to list_options list rule_reminder_due_opt',
  `pid` bigint(20) NOT NULL COMMENT 'id from patient_data table',
  `category` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to the category item in the rule_action_item table',
  `item` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to the item column in the rule_action_item table',
  `date_created` datetime DEFAULT NULL,
  `date_sent` datetime DEFAULT NULL,
  `voice_status` tinyint(1) NOT NULL default 0 COMMENT '0 if not sent and 1 if sent',
  `sms_status` tinyint(1) NOT NULL default 0 COMMENT '0 if not sent and 1 if sent',
  `email_status` tinyint(1) NOT NULL default 0 COMMENT '0 if not sent and 1 if sent',
  `mail_status` tinyint(1) NOT NULL default 0 COMMENT '0 if not sent and 1 if sent',
  PRIMARY KEY (`id`),
  KEY `pid` (`pid`),
  KEY (`category`,`item`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

-- --------------------------------------------------------

--
-- Table structure for table `patient_access_offsite`
--

DROP TABLE IF EXISTS `patient_access_offsite`;
CREATE TABLE  `patient_access_offsite` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pid` int(11) NOT NULL,
  `portal_username` varchar(100) NOT NULL,
  `portal_pwd` varchar(100) NOT NULL,
  `portal_pwd_status` tinyint(4) DEFAULT '1' COMMENT '0=>Password Created Through Demographics by The provider or staff. Patient Should Change it at first time it.1=>Pwd updated or created by patient itself',
  `authorize_net_id` VARCHAR(20) COMMENT 'authorize.net profile id',
  `portal_relation` VARCHAR(100) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pid` (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

--
-- Table structure for table `patient_tracker`
--

DROP TABLE IF EXISTS `patient_tracker`;
CREATE TABLE IF NOT EXISTS `patient_tracker` (
  `id`                     bigint(20)   NOT NULL auto_increment,
  `date`                   datetime     DEFAULT NULL,
  `apptdate`               date         DEFAULT NULL,
  `appttime`               time         DEFAULT NULL,
  `eid`                    bigint(20)   NOT NULL default '0',
  `pid`                    bigint(20)   NOT NULL default '0',
  `original_user`          varchar(255) NOT NULL default '' COMMENT 'This is the user that created the original record',
  `encounter`              bigint(20)   NOT NULL default '0',
  `lastseq`                varchar(4)   NOT NULL default '' COMMENT 'The element file should contain this number of elements',
  `random_drug_test`       TINYINT(1)   DEFAULT NULL COMMENT 'NULL if not randomized. If randomized, 0 is no, 1 is yes',
  `drug_screen_completed`  TINYINT(1)   NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY (`eid`),
  KEY (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

--
-- Table structure for table `patient_tracker_element`
--

DROP TABLE IF EXISTS `patient_tracker_element`;
CREATE TABLE IF NOT EXISTS `patient_tracker_element` (
  `pt_tracker_id`      bigint(20)   NOT NULL default '0' COMMENT 'maps to id column in patient_tracker table',
  `start_datetime`     datetime     DEFAULT NULL,
  `room`               varchar(20)  NOT NULL default '',
  `status`             varchar(31)  NOT NULL default '',
  `seq`                varchar(4)   NOT NULL default '' COMMENT 'This is a numerical sequence for this pt_tracker_id events',
  `user`               varchar(255) NOT NULL default '' COMMENT 'This is the user that created this element',
  KEY  (`pt_tracker_id`,`seq`)
) ENGINE=InnoDB;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
CREATE TABLE `payments` (
  `id` bigint(20) NOT NULL auto_increment,
  `pid` bigint(20) NOT NULL default '0',
  `dtime` datetime NOT NULL,
  `encounter` bigint(20) NOT NULL default '0',
  `user` varchar(255) default NULL,
  `method` varchar(255) default NULL,
  `source` varchar(255) default NULL,
  `amount1` decimal(12,2) NOT NULL default '0.00',
  `amount2` decimal(12,2) NOT NULL default '0.00',
  `posted1` decimal(12,2) NOT NULL default '0.00',
  `posted2` decimal(12,2) NOT NULL default '0.00',
  PRIMARY KEY  (`id`),
  KEY `pid` (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `payment_gateway_details`
--
CREATE TABLE `payment_gateway_details` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `service_name` varchar(100) DEFAULT NULL,
  `login_id` varchar(255) DEFAULT NULL,
  `transaction_key` varchar(255) DEFAULT NULL,
  `md5` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `pharmacies`
--

DROP TABLE IF EXISTS `pharmacies`;
CREATE TABLE `pharmacies` (
  `id` int(11) NOT NULL default '0',
  `name` varchar(255) default NULL,
  `transmit_method` int(11) NOT NULL default '1',
  `email` varchar(255) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `phone_numbers`
--

DROP TABLE IF EXISTS `phone_numbers`;
CREATE TABLE `phone_numbers` (
  `id` int(11) NOT NULL default '0',
  `country_code` varchar(5) default NULL,
  `area_code` char(3) default NULL,
  `prefix` char(3) default NULL,
  `number` varchar(4) default NULL,
  `type` int(11) default NULL,
  `foreign_id` int(11) default NULL,
  PRIMARY KEY  (`id`),
  KEY `foreign_id` (`foreign_id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `pma_bookmark`
--

DROP TABLE IF EXISTS `pma_bookmark`;
CREATE TABLE `pma_bookmark` (
  `id` int(11) NOT NULL auto_increment,
  `dbase` varchar(255) default NULL,
  `user` varchar(255) default NULL,
  `label` varchar(255) default NULL,
  `query` text,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB COMMENT='Bookmarks' AUTO_INCREMENT=10 ;

--
-- Dumping data for table `pma_bookmark`
--

INSERT INTO `pma_bookmark` VALUES (2, 'openemr', 'openemr', 'Aggregate Race Statistics', 'SELECT ethnoracial as "Race/Ethnicity", count(*) as Count FROM  `patient_data` WHERE 1 group by ethnoracial');
INSERT INTO `pma_bookmark` VALUES (9, 'openemr', 'openemr', 'Search by Code', 'SELECT  b.code, concat(pd.fname," ", pd.lname) as "Patient Name", concat(u.fname," ", u.lname) as "Provider Name", en.reason as "Encounter Desc.", en.date\r\nFROM billing as b\r\nLEFT JOIN users AS u ON b.user = u.id\r\nLEFT JOIN patient_data as pd on b.pid = pd.pid\r\nLEFT JOIN form_encounter as en on b.encounter = en.encounter and b.pid = en.pid\r\nWHERE 1 /* and b.code like ''%[VARIABLE]%'' */ ORDER BY b.code');
INSERT INTO `pma_bookmark` VALUES (8, 'openemr', 'openemr', 'Count No Shows By Provider since Interval ago', 'SELECT concat( u.fname,  " ", u.lname )  AS  "Provider Name", u.id AS  "Provider ID", count(  DISTINCT ev.pc_eid )  AS  "Number of No Shows"/* , concat(DATE_FORMAT(NOW(),''%Y-%m-%d''), '' and '',DATE_FORMAT(DATE_ADD(now(), INTERVAL [VARIABLE]),''%Y-%m-%d'') ) as "Between Dates" */ FROM  `openemr_postcalendar_events`  AS ev LEFT  JOIN users AS u ON ev.pc_aid = u.id WHERE ev.pc_catid =1/* and ( ev.pc_eventDate >= DATE_SUB(now(), INTERVAL [VARIABLE]) )  */\r\nGROUP  BY u.id;');
INSERT INTO `pma_bookmark` VALUES (6, 'openemr', 'openemr', 'Appointments By Race/Ethnicity from today plus interval', 'SELECT  count(pd.ethnoracial) as "Number of Appointments", pd.ethnoracial AS  "Race/Ethnicity" /* , concat(DATE_FORMAT(NOW(),''%Y-%m-%d''), '' and '',DATE_FORMAT(DATE_ADD(now(), INTERVAL [VARIABLE]),''%Y-%m-%d'') ) as "Between Dates" */ FROM openemr_postcalendar_events AS ev LEFT  JOIN   `patient_data`  AS pd ON  pd.pid = ev.pc_pid where ev.pc_eventstatus=1 and ev.pc_catid = 5 and ev.pc_eventDate >= now()  /* and ( ev.pc_eventDate <= DATE_ADD(now(), INTERVAL [VARIABLE]) )  */ group by pd.ethnoracial');

-- --------------------------------------------------------

--
-- Table structure for table `pma_column_info`
--

DROP TABLE IF EXISTS `pma_column_info`;
CREATE TABLE `pma_column_info` (
  `id` int(5) unsigned NOT NULL auto_increment,
  `db_name` varchar(64) default NULL,
  `table_name` varchar(64) default NULL,
  `column_name` varchar(64) default NULL,
  `comment` varchar(255) default NULL,
  `mimetype` varchar(255) default NULL,
  `transformation` varchar(255) default NULL,
  `transformation_options` varchar(255) default NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `db_name` (`db_name`,`table_name`,`column_name`)
) ENGINE=InnoDB COMMENT='Column Information for phpMyAdmin' AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `pma_history`
--

DROP TABLE IF EXISTS `pma_history`;
CREATE TABLE `pma_history` (
  `id` bigint(20) unsigned NOT NULL auto_increment,
  `username` varchar(64) default NULL,
  `db` varchar(64) default NULL,
  `table` varchar(64) default NULL,
  `timevalue` timestamp NOT NULL,
  `sqlquery` text,
  PRIMARY KEY  (`id`),
  KEY `username` (`username`,`db`,`table`,`timevalue`)
) ENGINE=InnoDB COMMENT='SQL history' AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `pma_pdf_pages`
--

DROP TABLE IF EXISTS `pma_pdf_pages`;
CREATE TABLE `pma_pdf_pages` (
  `db_name` varchar(64) default NULL,
  `page_nr` int(10) unsigned NOT NULL auto_increment,
  `page_descr` varchar(50) default NULL,
  PRIMARY KEY  (`page_nr`),
  KEY `db_name` (`db_name`)
) ENGINE=InnoDB COMMENT='PDF Relationpages for PMA' AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `pma_relation`
--

DROP TABLE IF EXISTS `pma_relation`;
CREATE TABLE `pma_relation` (
  `master_db` varchar(64) NOT NULL default '',
  `master_table` varchar(64) NOT NULL default '',
  `master_field` varchar(64) NOT NULL default '',
  `foreign_db` varchar(64) default NULL,
  `foreign_table` varchar(64) default NULL,
  `foreign_field` varchar(64) default NULL,
  PRIMARY KEY  (`master_db`,`master_table`,`master_field`),
  KEY `foreign_field` (`foreign_db`,`foreign_table`)
) ENGINE=InnoDB COMMENT='Relation table';

-- --------------------------------------------------------

--
-- Table structure for table `pma_table_coords`
--

DROP TABLE IF EXISTS `pma_table_coords`;
CREATE TABLE `pma_table_coords` (
  `db_name` varchar(64) NOT NULL default '',
  `table_name` varchar(64) NOT NULL default '',
  `pdf_page_number` int(11) NOT NULL default '0',
  `x` float unsigned NOT NULL default '0',
  `y` float unsigned NOT NULL default '0',
  PRIMARY KEY  (`db_name`,`table_name`,`pdf_page_number`)
) ENGINE=InnoDB COMMENT='Table coordinates for phpMyAdmin PDF output';

-- --------------------------------------------------------

--
-- Table structure for table `pma_table_info`
--

DROP TABLE IF EXISTS `pma_table_info`;
CREATE TABLE `pma_table_info` (
  `db_name` varchar(64) NOT NULL default '',
  `table_name` varchar(64) NOT NULL default '',
  `display_field` varchar(64) default NULL,
  PRIMARY KEY  (`db_name`,`table_name`)
) ENGINE=InnoDB COMMENT='Table information for phpMyAdmin';

-- --------------------------------------------------------

--
-- Table structure for table `pnotes`
--

DROP TABLE IF EXISTS `pnotes`;
CREATE TABLE `pnotes` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `body` longtext,
  `pid` bigint(20) default NULL,
  `user` varchar(255) default NULL,
  `groupname` varchar(255) default NULL,
  `activity` tinyint(4) default NULL,
  `authorized` tinyint(4) default NULL,
  `title` varchar(255) default NULL,
  `assigned_to` varchar(255) default NULL,
  `deleted` tinyint(4) default 0 COMMENT 'flag indicates note is deleted',
  `message_status` VARCHAR(20) NOT NULL DEFAULT 'New',
  `portal_relation` VARCHAR(100) NULL,
  `is_msg_encrypted` TINYINT(2) DEFAULT '0' COMMENT 'Whether messsage encrypted 0-Not encrypted, 1-Encrypted',
  PRIMARY KEY  (`id`),
  KEY `pid` (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `prescriptions`
--

DROP TABLE IF EXISTS `prescriptions`;
CREATE TABLE `prescriptions` (
  `id` int(11) NOT NULL auto_increment,
  `patient_id` int(11) default NULL,
  `filled_by_id` int(11) default NULL,
  `pharmacy_id` int(11) default NULL,
  `date_added` date default NULL,
  `date_modified` date default NULL,
  `provider_id` int(11) default NULL,
  `encounter` int(11) default NULL,
  `start_date` date default NULL,
  `drug` varchar(150) default NULL,
  `drug_id` int(11) NOT NULL default '0',
  `rxnorm_drugcode` INT(11) DEFAULT NULL,
  `form` int(3) default NULL,
  `dosage` varchar(100) default NULL,
  `quantity` varchar(31) default NULL,
  `size` varchar(25) DEFAULT NULL,
  `unit` int(11) default NULL,
  `route` int(11) default NULL,
  `interval` int(11) default NULL,
  `substitute` int(11) default NULL,
  `refills` int(11) default NULL,
  `per_refill` int(11) default NULL,
  `filled_date` date default NULL,
  `medication` int(11) default NULL,
  `note` text,
  `active` int(11) NOT NULL default '1',
  `datetime` DATETIME DEFAULT NULL,
  `user` VARCHAR(50) DEFAULT NULL,
  `site` VARCHAR(50) DEFAULT NULL,
  `prescriptionguid` VARCHAR(50) DEFAULT NULL,
  `erx_source` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '0-OpenEMR 1-External',
  `erx_uploaded` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '0-Pending NewCrop upload 1-Uploaded to NewCrop',
  `drug_info_erx` TEXT,
  `external_id` VARCHAR(20) DEFAULT NULL,
  `end_date` date default NULL,
  `indication` text,
  `prn` VARCHAR(30) DEFAULT NULL,
  PRIMARY KEY  (`id`),
  KEY `patient_id` (`patient_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `prices`
--

DROP TABLE IF EXISTS `prices`;
CREATE TABLE `prices` (
  `pr_id` varchar(11) NOT NULL default '',
  `pr_selector` varchar(255) NOT NULL default '' COMMENT 'template selector for drugs, empty for codes',
  `pr_level` varchar(31) NOT NULL default '',
  `pr_price` decimal(12,2) NOT NULL default '0.00' COMMENT 'price in local currency',
  PRIMARY KEY  (`pr_id`,`pr_selector`,`pr_level`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `registry`
--

DROP TABLE IF EXISTS `registry`;
CREATE TABLE `registry` (
  `name` varchar(255) default NULL,
  `state` tinyint(4) default NULL,
  `directory` varchar(255) default NULL,
  `id` bigint(20) NOT NULL auto_increment,
  `sql_run` tinyint(4) default NULL,
  `unpackaged` tinyint(4) default NULL,
  `date` datetime default NULL,
  `priority` int(11) default '0',
  `category` varchar(255) default NULL,
  `nickname` varchar(255) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 ;

--
-- Dumping data for table `registry`
--

INSERT INTO `registry` VALUES ('New Encounter Form', 1, 'newpatient', 1, 1, 1, '2003-09-14 15:16:45', 0, 'Administrative', '');
INSERT INTO `registry` VALUES ('Review of Systems Checks', 1, 'reviewofs', 9, 1, 1, '2003-09-14 15:16:45', 0, 'Clinical', '');
INSERT INTO `registry` VALUES ('Speech Dictation', 1, 'dictation', 10, 1, 1, '2003-09-14 15:16:45', 0, 'Clinical', '');
INSERT INTO `registry` VALUES ('SOAP', 1, 'soap', 11, 1, 1, '2005-03-03 00:16:35', 0, 'Clinical', '');
INSERT INTO `registry` VALUES ('Vitals', 1, 'vitals', 12, 1, 1, '2005-03-03 00:16:34', 0, 'Clinical', '');
INSERT INTO `registry` VALUES ('Review Of Systems', 1, 'ros', 13, 1, 1, '2005-03-03 00:16:30', 0, 'Clinical', '');
INSERT INTO `registry` VALUES ('Fee Sheet', 1, 'fee_sheet', 14, 1, 1, '2007-07-28 00:00:00', 0, 'Administrative', '');
INSERT INTO `registry` VALUES ('Misc Billing Options HCFA', 1, 'misc_billing_options', 15, 1, 1, '2007-07-28 00:00:00', 0, 'Administrative', '');
INSERT INTO `registry` VALUES ('Procedure Order', 1, 'procedure_order', 16, 1, 1, '2010-02-25 00:00:00', 0, 'Administrative', '');
INSERT INTO `registry` VALUES ('Observation', 1, 'observation', 17, 1, 1, '2015-09-09 00:00:00', 0, 'Clinical', '');
INSERT INTO `registry` VALUES ('Care Plan', 1, 'care_plan', 18, 1, 1, '2015-09-09 00:00:00', 0, 'Clinical', '');
INSERT INTO `registry` VALUES ('Functional and Cognitive Status', 1, 'functional_cognitive_status', 19, 1, 1, '2015-09-09 00:00:00', 0, 'Clinical', '');
INSERT INTO `registry` VALUES ('Clinical Instructions', 1, 'clinical_instructions', 20, 1, 1, '2015-09-09 00:00:00', 0, 'Clinical', '');
INSERT INTO `registry` VALUES ('Eye Exam', 1, 'eye_mag', 21, 1, 1, '2015-10-15 00:00:00', 0, 'Clinical', '');

-- --------------------------------------------------------

--
-- Table structure for table `report_itemized`
-- (goal is optimize insert performance, so only one key)

DROP TABLE IF EXISTS `report_itemized`;
CREATE TABLE `report_itemized` (
  `report_id` bigint(20) NOT NULL,
  `itemized_test_id` smallint(6) NOT NULL,
  `numerator_label` varchar(25) NOT NULL DEFAULT '' COMMENT 'Only used in special cases',
  `pass` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0 is fail, 1 is pass, 2 is excluded',
  `pid` bigint(20) NOT NULL,
  KEY (`report_id`,`itemized_test_id`,`numerator_label`,`pass`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `report_results`
--

DROP TABLE IF EXISTS `report_results`;
CREATE TABLE `report_results` (
  `report_id` bigint(20) NOT NULL,
  `field_id` varchar(31) NOT NULL default '',
  `field_value` text,
  PRIMARY KEY (`report_id`,`field_id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `rule_action`
--

DROP TABLE IF EXISTS `rule_action`;
CREATE TABLE `rule_action` (
  `id` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to the id column in the clinical_rules table',
  `group_id` bigint(20) NOT NULL DEFAULT 1 COMMENT 'Contains group id to identify collection of targets in a rule',
  `category` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to the category item in the rule_action_item table',
  `item` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to the item column in the rule_action_item table',
  KEY  (`id`)
) ENGINE=InnoDB ;

--
-- Standard clinical rule actions
--
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_htn_bp_measure', 1, 'act_cat_measure', 'act_bp');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_tob_use_assess', 1, 'act_cat_assess', 'act_tobacco');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_tob_cess_inter', 1, 'act_cat_inter', 'act_tobacco');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_adult_wt_screen_fu', 1, 'act_cat_measure', 'act_wt');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_wt_assess_couns_child', 1, 'act_cat_measure', 'act_wt');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_wt_assess_couns_child', 2, 'act_cat_edu', 'act_wt');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_wt_assess_couns_child', 3, 'act_cat_edu', 'act_nutrition');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_wt_assess_couns_child', 4, 'act_cat_edu', 'act_exercise');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_wt_assess_couns_child', 5, 'act_cat_measure', 'act_bmi');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_influenza_ge_50', 1, 'act_cat_treat', 'act_influvacc');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_pneumovacc_ge_65', 1, 'act_cat_treat', 'act_pneumovacc');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_dm_hemo_a1c', 1, 'act_cat_measure', 'act_hemo_a1c');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_dm_urine_alb', 1, 'act_cat_measure', 'act_urine_alb');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_dm_eye', 1, 'act_cat_exam', 'act_eye');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_dm_foot', 1, 'act_cat_exam', 'act_foot');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_cs_mammo', 1, 'act_cat_measure', 'act_mammo');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_cs_pap', 1, 'act_cat_exam', 'act_pap');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_cs_colon', 1, 'act_cat_assess', 'act_colon_cancer_screen');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_cs_prostate', 1, 'act_cat_assess', 'act_prostate_cancer_screen');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_inr_monitor', 1, 'act_cat_measure', 'act_lab_inr');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_socsec_entry', 1, 'act_cat_assess', 'act_soc_sec');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_penicillin_allergy', 1, 'act_cat_assess', 'act_penicillin_allergy');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_blood_pressure', 1, 'act_cat_measure', 'act_bp');
INSERT INTO `rule_action` ( `id`, `group_id`, `category`, `item` ) VALUES ('rule_inr_measure', 1, 'act_cat_measure', 'act_lab_inr');

-- --------------------------------------------------------

--
-- Table structure for table `rule_action_item`
--

DROP TABLE IF EXISTS `rule_action_item`;
CREATE TABLE `rule_action_item` (
  `category` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to list_options list rule_action_category',
  `item` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to list_options list rule_action',
  `clin_rem_link` varchar(255) NOT NULL DEFAULT '' COMMENT 'Custom html link in clinical reminder widget',
  `reminder_message` TEXT COMMENT 'Custom message in patient reminder',
  `custom_flag` tinyint(1) NOT NULL default 0 COMMENT '1 indexed to rule_patient_data, 0 indexed within main schema',
  PRIMARY KEY  (`category`,`item`)
) ENGINE=InnoDB ;

INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_measure', 'act_bp', '', '', 0);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_assess', 'act_tobacco', '', '', 0);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_inter', 'act_tobacco', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_measure', 'act_wt', '', '', 0);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_edu', 'act_wt', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_measure', 'act_bmi', '', '', 0);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_edu', 'act_nutrition', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_edu', 'act_exercise', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_treat', 'act_influvacc', '', '', 0);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_treat', 'act_pneumovacc', '', '', 0);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_measure', 'act_hemo_a1c', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_measure', 'act_urine_alb', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_exam', 'act_eye', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_exam', 'act_foot', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_measure', 'act_mammo', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_exam', 'act_pap', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_assess', 'act_colon_cancer_screen', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_assess', 'act_prostate_cancer_screen', '', '', 1);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_measure', 'act_lab_inr', '', '', 0);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_assess', 'act_soc_sec', '', '', 0);
INSERT INTO `rule_action_item` ( `category`, `item`, `clin_rem_link`, `reminder_message`, `custom_flag` ) VALUES ('act_cat_assess', 'act_penicillin_allergy', '', '', 1);

-- --------------------------------------------------------

--
-- Table structure for table `rule_filter`
--

DROP TABLE IF EXISTS `rule_filter`;
CREATE TABLE `rule_filter` (
  `id` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to the id column in the clinical_rules table',
  `include_flag` tinyint(1) NOT NULL default 0 COMMENT '0 is exclude and 1 is include',
  `required_flag` tinyint(1) NOT NULL default 0 COMMENT '0 is required and 1 is optional',
  `method` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to list_options list rule_filters',
  `method_detail` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to list_options lists rule__intervals',
  `value` varchar(255) NOT NULL DEFAULT '',
  KEY  (`id`)
) ENGINE=InnoDB ;

--
-- Standard clinical rule filters
--
-- Hypertension: Blood Pressure Measurement
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'CUSTOM::HTN');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::401.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::401.1');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::401.9');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::402.00');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::402.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::402.10');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::402.11');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::402.90');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::402.91');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::403.00');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::403.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::403.10');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::403.11');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::403.90');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::403.91');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.00');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.10');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.11');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.12');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.13');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.90');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.91');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.92');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::404.93');
-- Tobacco Use Assessment
-- no filters
-- Tobacco Cessation Intervention
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_tob_cess_inter', 1, 1, 'filt_database', '', 'LIFESTYLE::tobacco::current');
-- Adult Weight Screening and Follow-Up
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_adult_wt_screen_fu', 1, 1, 'filt_age_min', 'year', '18');
-- Weight Assessment and Counseling for Children and Adolescents
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_wt_assess_couns_child', 1, 1, 'filt_age_max', 'year', '18');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_wt_assess_couns_child', 1, 1, 'filt_age_min', 'year', '2');
-- Influenza Immunization for Patients >= 50 Years Old
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_influenza_ge_50', 1, 1, 'filt_age_min', 'year', '50');
-- Pneumonia Vaccination Status for Older Adults
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_pneumovacc_ge_65', 1, 1, 'filt_age_min', 'year', '65');
-- Diabetes: Hemoglobin A1C
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'CUSTOM::diabetes');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.00');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.10');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.11');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.12');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.13');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.20');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.21');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.22');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.23');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.30');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.31');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.32');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.33');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.4');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.40');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.41');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.42');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.43');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.50');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.51');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.52');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.53');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.60');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.61');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.62');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.63');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.7');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.70');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.71');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.72');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.73');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.80');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.81');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.82');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.83');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.9');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.90');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.91');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.92');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.93');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::357.2');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.04');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.05');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.06');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::366.41');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.00');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.04');
-- Diabetes: Urine Microalbumin
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'CUSTOM::diabetes');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.00');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.10');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.11');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.12');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.13');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.20');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.21');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.22');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.23');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.30');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.31');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.32');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.33');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.4');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.40');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.41');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.42');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.43');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.50');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.51');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.52');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.53');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.60');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.61');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.62');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.63');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.7');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.70');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.71');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.72');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.73');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.80');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.81');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.82');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.83');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.9');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.90');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.91');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.92');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.93');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::357.2');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.04');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.05');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.06');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::366.41');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.00');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.04');
-- Diabetes: Eye Exam
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'CUSTOM::diabetes');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.00');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.10');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.11');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.12');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.13');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.20');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.21');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.22');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.23');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.30');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.31');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.32');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.33');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.4');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.40');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.41');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.42');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.43');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.50');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.51');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.52');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.53');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.60');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.61');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.62');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.63');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.7');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.70');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.71');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.72');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.73');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.80');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.81');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.82');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.83');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.9');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.90');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.91');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.92');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.93');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::357.2');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.04');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.05');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.06');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::366.41');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.00');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.04');
-- Diabetes: Foot Exam
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'CUSTOM::diabetes');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.00');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.10');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.11');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.12');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.13');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.20');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.21');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.22');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.23');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.30');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.31');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.32');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.33');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.4');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.40');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.41');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.42');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.43');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.50');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.51');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.52');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.53');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.60');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.61');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.62');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.63');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.7');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.70');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.71');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.72');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.73');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.80');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.81');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.82');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.83');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.9');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.90');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.91');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.92');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::250.93');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::357.2');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.04');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.05');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::362.06');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::366.41');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.0');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.00');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.01');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.02');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.03');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 1, 0, 'filt_lists', 'medical_problem', 'ICD9::648.04');
-- Cancer Screening: Mammogram
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_mammo', 1, 1, 'filt_age_min', 'year', '40');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_mammo', 1, 1, 'filt_sex', '', 'Female');
-- Cancer Screening: Pap Smear
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_pap', 1, 1, 'filt_age_min', 'year', '18');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_pap', 1, 1, 'filt_sex', '', 'Female');
-- Cancer Screening: Colon Cancer Screening
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_colon', 1, 1, 'filt_age_min', 'year', '50');
-- Cancer Screening: Prostate Cancer Screening
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_prostate', 1, 1, 'filt_age_min', 'year', '50');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_prostate', 1, 1, 'filt_sex', '', 'Male');
--
-- Rule filters to specifically demonstrate passing of NIST criteria
--
-- Coumadin Management - INR Monitoring
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_inr_monitor', 1, 0, 'filt_lists', 'medication', 'coumadin');
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_inr_monitor', 1, 0, 'filt_lists', 'medication', 'warfarin');
-- Penicillin Allergy Assessment
INSERT INTO `rule_filter` ( `id`, `include_flag`, `required_flag`, `method`, `method_detail`, `value` ) VALUES ('rule_penicillin_allergy', 1, 0, 'filt_lists', 'allergy', 'penicillin');

-- --------------------------------------------------------

--
-- Table structure for table `rule_patient_data`
--

DROP TABLE IF EXISTS `rule_patient_data`;
CREATE TABLE `rule_patient_data` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime DEFAULT NULL,
  `pid` bigint(20) NOT NULL,
  `category` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to the category item in the rule_action_item table',
  `item` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to the item column in the rule_action_item table',
  `complete` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to list_options list yesno',
  `result` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY  (`id`),
  KEY (`pid`),
  KEY (`category`,`item`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `rule_reminder`
--

DROP TABLE IF EXISTS `rule_reminder`;
CREATE TABLE `rule_reminder` (
  `id` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to the id column in the clinical_rules table',
  `method` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to list_options list rule_reminder_methods',
  `method_detail` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to list_options list rule_reminder_intervals',
  `value` varchar(255) NOT NULL DEFAULT '',
  KEY  (`id`)
) ENGINE=InnoDB ;

-- Hypertension: Blood Pressure Measurement
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_htn_bp_measure', 'patient_reminder_post', 'month', '1');
-- Tobacco Use Assessment
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_tob_use_assess', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_tob_use_assess', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_tob_use_assess', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_tob_use_assess', 'patient_reminder_post', 'month', '1');
-- Tobacco Cessation Intervention
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_tob_cess_inter', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_tob_cess_inter', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_tob_cess_inter', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_tob_cess_inter', 'patient_reminder_post', 'month', '1');
-- Adult Weight Screening and Follow-Up
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_adult_wt_screen_fu', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_adult_wt_screen_fu', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_adult_wt_screen_fu', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_adult_wt_screen_fu', 'patient_reminder_post', 'month', '1');
-- Weight Assessment and Counseling for Children and Adolescents
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_wt_assess_couns_child', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_wt_assess_couns_child', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_wt_assess_couns_child', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_wt_assess_couns_child', 'patient_reminder_post', 'month', '1');
-- Influenza Immunization for Patients >= 50 Years Old
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_influenza_ge_50', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_influenza_ge_50', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_influenza_ge_50', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_influenza_ge_50', 'patient_reminder_post', 'month', '1');
-- Pneumonia Vaccination Status for Older Adults
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_pneumovacc_ge_65', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_pneumovacc_ge_65', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_pneumovacc_ge_65', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_pneumovacc_ge_65', 'patient_reminder_post', 'month', '1');
-- Diabetes: Hemoglobin A1C
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_hemo_a1c', 'patient_reminder_post', 'month', '1');
-- Diabetes: Urine Microalbumin
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_urine_alb', 'patient_reminder_post', 'month', '1');
-- Diabetes: Eye Exam
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_eye', 'patient_reminder_post', 'month', '1');
-- Diabetes: Foot Exam
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_dm_foot', 'patient_reminder_post', 'month', '1');
-- Cancer Screening: Mammogram
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_mammo', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_mammo', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_mammo', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_mammo', 'patient_reminder_post', 'month', '1');
-- Cancer Screening: Pap Smear
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_pap', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_pap', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_pap', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_pap', 'patient_reminder_post', 'month', '1');
-- Cancer Screening: Colon Cancer Screening
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_colon', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_colon', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_colon', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_colon', 'patient_reminder_post', 'month', '1');
-- Cancer Screening: Prostate Cancer Screening
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_prostate', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_prostate', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_prostate', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_cs_prostate', 'patient_reminder_post', 'month', '1');
-- Coumadin Management - INR Monitoring
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_inr_monitor', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_inr_monitor', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_inr_monitor', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_inr_monitor', 'patient_reminder_post', 'month', '1');
-- Data Entry - Social Security Number
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_socsec_entry', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_socsec_entry', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_socsec_entry', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_socsec_entry', 'patient_reminder_post', 'month', '1');
-- Penicillin Allergy Assessment
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_penicillin_allergy', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_penicillin_allergy', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_penicillin_allergy', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_penicillin_allergy', 'patient_reminder_post', 'month', '1');
-- Blood Pressure Measurement
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_blood_pressure', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_blood_pressure', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_blood_pressure', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_blood_pressure', 'patient_reminder_post', 'month', '1');
-- INR Measurement
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_inr_measure', 'clinical_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_inr_measure', 'clinical_reminder_post', 'month', '1');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_inr_measure', 'patient_reminder_pre', 'week', '2');
INSERT INTO `rule_reminder` ( `id`, `method`, `method_detail`, `value` ) VALUES ('rule_inr_measure', 'patient_reminder_post', 'month', '1');

-- --------------------------------------------------------

--
-- Table structure for table `rule_target`
--

DROP TABLE IF EXISTS `rule_target`;
CREATE TABLE `rule_target` (
  `id` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to the id column in the clinical_rules table',
  `group_id` bigint(20) NOT NULL DEFAULT 1 COMMENT 'Contains group id to identify collection of targets in a rule',
  `include_flag` tinyint(1) NOT NULL default 0 COMMENT '0 is exclude and 1 is include',
  `required_flag` tinyint(1) NOT NULL default 0 COMMENT '0 is required and 1 is optional',
  `method` varchar(31) NOT NULL DEFAULT '' COMMENT 'Maps to list_options list rule_targets',
  `value` varchar(255) NOT NULL DEFAULT '' COMMENT 'Data is dependent on the method',
  `interval` bigint(20) NOT NULL DEFAULT 0 COMMENT 'Only used in interval entries',
  KEY  (`id`)
) ENGINE=InnoDB ;

--
-- Standard clinical rule targets
--
-- Hypertension: Blood Pressure Measurement
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_htn_bp_measure', 1, 1, 1, 'target_interval', 'year', 1);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_htn_bp_measure', 1, 1, 1, 'target_database', '::form_vitals::bps::::::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_htn_bp_measure', 1, 1, 1, 'target_database', '::form_vitals::bpd::::::ge::1', 0);
-- Tobacco Use Assessment
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_tob_use_assess', 1, 1, 1, 'target_database', 'LIFESTYLE::tobacco::', 0);
-- Tobacco Cessation Intervention
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_tob_cess_inter', 1, 1, 1, 'target_interval', 'year', 1);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_tob_cess_inter', 1, 1, 1, 'target_database', 'CUSTOM::act_cat_inter::act_tobacco::YES::ge::1', 0);
-- Adult Weight Screening and Follow-Up
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_adult_wt_screen_fu', 1, 1, 1, 'target_database', '::form_vitals::weight::::::ge::1', 0);
-- Weight Assessment and Counseling for Children and Adolescents
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_wt_assess_couns_child', 1, 1, 1, 'target_database', '::form_vitals::weight::::::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_wt_assess_couns_child', 1, 1, 1, 'target_interval', 'year', 3);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_wt_assess_couns_child', 2, 1, 1, 'target_database', 'CUSTOM::act_cat_edu::act_wt::YES::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_wt_assess_couns_child', 2, 1, 1, 'target_interval', 'year', 3);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_wt_assess_couns_child', 3, 1, 1, 'target_database', 'CUSTOM::act_cat_edu::act_nutrition::YES::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_wt_assess_couns_child', 3, 1, 1, 'target_interval', 'year', 3);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_wt_assess_couns_child', 4, 1, 1, 'target_database', 'CUSTOM::act_cat_edu::act_exercise::YES::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_wt_assess_couns_child', 4, 1, 1, 'target_interval', 'year', 3);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_wt_assess_couns_child', 5, 1, 1, 'target_database', '::form_vitals::BMI::::::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_wt_assess_couns_child', 5, 1, 1, 'target_interval', 'year', 3);
-- Influenza Immunization for Patients >= 50 Years Old
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 1, 'target_interval', 'flu_season', 1);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::15::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::16::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::88::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::111::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::125::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::126::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::127::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::128::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::135::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::140::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::141::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_influenza_ge_50', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::144::ge::1', 0);
-- Pneumonia Vaccination Status for Older Adults
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_pneumovacc_ge_65', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::33::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_pneumovacc_ge_65', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::100::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_pneumovacc_ge_65', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::109::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_pneumovacc_ge_65', 1, 1, 0, 'target_database', '::immunizations::cvx_code::eq::133::ge::1', 0);
-- Diabetes: Hemoglobin A1C
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_dm_hemo_a1c', 1, 1, 1, 'target_interval', 'month', 3);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_dm_hemo_a1c', 1, 1, 1, 'target_database', 'CUSTOM::act_cat_measure::act_hemo_a1c::YES::ge::1', 0);
-- Diabetes: Urine Microalbumin
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_dm_urine_alb', 1, 1, 1, 'target_interval', 'year', 1);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_dm_urine_alb', 1, 1, 1, 'target_database', 'CUSTOM::act_cat_measure::act_urine_alb::YES::ge::1', 0);
-- Diabetes: Eye Exam
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_dm_eye', 1, 1, 1, 'target_interval', 'year', 1);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_dm_eye', 1, 1, 1, 'target_database', 'CUSTOM::act_cat_exam::act_eye::YES::ge::1', 0);
-- Diabetes: Foot Exam
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_dm_foot', 1, 1, 1, 'target_interval', 'year', 1);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_dm_foot', 1, 1, 1, 'target_database', 'CUSTOM::act_cat_exam::act_foot::YES::ge::1', 0);
-- Cancer Screening: Mammogram
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_cs_mammo', 1, 1, 1, 'target_interval', 'year', 1);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_cs_mammo', 1, 1, 1, 'target_database', 'CUSTOM::act_cat_measure::act_mammo::YES::ge::1', 0);
-- Cancer Screening: Pap Smear
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_cs_pap', 1, 1, 1, 'target_interval', 'year', 1);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_cs_pap', 1, 1, 1, 'target_database', 'CUSTOM::act_cat_exam::act_pap::YES::ge::1', 0);
-- Cancer Screening: Colon Cancer Screening
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_cs_colon', 1, 1, 1, 'target_database', 'CUSTOM::act_cat_assess::act_colon_cancer_screen::YES::ge::1', 0);
-- Cancer Screening: Prostate Cancer Screening
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_cs_prostate', 1, 1, 1, 'target_database', 'CUSTOM::act_cat_assess::act_prostate_cancer_screen::YES::ge::1', 0);
--
-- Rule targets to specifically demonstrate passing of NIST criteria
--
-- Coumadin Management - INR Monitoring
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_inr_monitor', 1, 1, 1, 'target_interval', 'week', 3);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_inr_monitor', 1, 1, 1, 'target_proc', 'INR::CPT4:85610::::::ge::1', 0);
-- Data entry - Social security number.
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_socsec_entry', 1, 1, 1, 'target_database', '::patient_data::ss::::::ge::1', 0);
-- Penicillin allergy assessment.
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_penicillin_allergy', 1, 1, 1, 'target_interval', 'year', 1);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_penicillin_allergy', 1, 1, 1, 'target_database', 'CUSTOM::act_cat_assess::act_penicillin_allergy::YES::ge::1', 0);
-- Blood Pressure Measurement
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_blood_pressure', 1, 1, 1, 'target_database', '::form_vitals::bps::::::ge::1', 0);
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_blood_pressure', 1, 1, 1, 'target_database', '::form_vitals::bpd::::::ge::1', 0);
-- INR Measurement
INSERT INTO `rule_target` ( `id`, `group_id`, `include_flag`, `required_flag`, `method`, `value`, `interval` ) VALUES ('rule_inr_measure', 1, 1, 1, 'target_proc', 'INR::CPT4:85610::::::ge::1', 0);

-- --------------------------------------------------------

--
-- Table structure for table `sequences`
--

DROP TABLE IF EXISTS `sequences`;
CREATE TABLE `sequences` (
  `id` int(11) unsigned NOT NULL default '0'
) ENGINE=InnoDB;

--
-- Dumping data for table `sequences`
--

INSERT INTO `sequences` VALUES (1);

-- --------------------------------------------------------

--
-- Table structure for table `supported_external_dataloads`
--

DROP TABLE IF EXISTS `supported_external_dataloads`;
CREATE TABLE `supported_external_dataloads` (
  `load_id` SERIAL,
  `load_type` varchar(24) NOT NULL DEFAULT '',
  `load_source` varchar(24) NOT NULL DEFAULT 'CMS',
  `load_release_date` date NOT NULL,
  `load_filename` varchar(256) NOT NULL DEFAULT '',
  `load_checksum` varchar(32) NOT NULL DEFAULT ''
) ENGINE=InnoDB;

--
-- Dumping data for table `supported_external_dataloads`
--

INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD9', 'CMS', '2011-10-01', 'cmsv29_master_descriptions.zip', 'c360c2b5a29974d6c58617c7378dd7c4');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD9', 'CMS', '2012-10-01', 'cmsv30_master_descriptions.zip', 'eb26446536435f5f5e677090a7976b15');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD9', 'CMS', '2013-10-01', 'cmsv31-master-descriptions.zip', 'fe0d7f9a5338f5ff187683b4737ad2b7');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2011-10-01', '2012_PCS_long_and_abbreviated_titles.zip', '201a732b649d8c7fba807cc4c083a71a');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2011-10-01', 'DiagnosisGEMs_2012.zip', '6758c4a3384c47161ce24f13a2464b53');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2011-10-01', 'ICD10OrderFiles_2012.zip', 'a76601df7a9f5270d8229828a833f6a1');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2011-10-01', 'ProcedureGEMs_2012.zip', 'f37416d8fab6cd2700b634ca5025295d');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2011-10-01', 'ReimbursementMapping_2012.zip', '8b438d1fd1f34a9bb0e423c15e89744b');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2012-10-01', '2013_PCS_long_and_abbreviated_titles.zip', '04458ed0631c2c122624ee0a4ca1c475');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2012-10-01', '2013-DiagnosisGEMs.zip', '773aac2a675d6aefd1d7dd149883be51');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2012-10-01', 'ICD10CMOrderFiles_2013.zip', '1c175a858f833485ef8f9d3e66b4d8bd');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2012-10-01', 'ProcedureGEMs_2013.zip', '92aa7640e5ce29b9629728f7d4fc81db');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2012-10-01', '2013-ReimbursementMapping_dx.zip', '0d5d36e3f4519bbba08a9508576787fb');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2012-10-01', 'ReimbursementMapping_pr_2013.zip', '4c3920fedbcd9f6af54a1dc9069a11ca');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2013-10-01', '2014-Reimbursement-Mappings-PR.zip', 'f306a0e8c9edb34d28fd6ce8af82b646');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2013-10-01', '2014-Reimbursement-Mappings-DX.zip', '614b3957304208e3ef7d3ba8b3618888');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2013-10-01', 'ProcedureGEMs-2014.zip', 'be46de29f4f40f97315d04821273acf9');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2013-10-01', '2014-ICD10-Code-Descriptions.zip', '5458b95f6f37228b5cdfa03aefc6c8bb');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2013-10-01', 'DiagnosisGEMs-2014.zip', '3ed7b7c5a11c766102b12d97d777a11b');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2013-10-01', '2014-PCS-long-and-abbreviated-titles.zip', '2d03514a0c66d92cf022a0bc28c83d38');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD9', 'CMS', '2014-10-01', 'ICD-9-CM-v32-master-descriptions.zip', 'b852b85f770c83433201dc8ae2c59074');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2014-10-01', '2015-PCS-long-and-abbreviated-titles.zip', 'd1504d6cbc40e008e52dbc50600a4b66');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2014-10-01', 'DiagnosisGEMs_2015.zip', 'a4505805edf25ba4eacda07f23934e63');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2014-10-01', '2015-code-descriptions.zip', '6a8c0ab630d5afa7482daa417950846a');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2014-10-01', 'ProcedureGEMs_2015.zip', 'fcba4e4c96851f4c900345bc557483e2');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2014-10-01', 'Reimbursement_Mapping_dx_2015.zip', '0990d5bcac13ccf5e288249be5261fd7');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2014-10-01', 'Reimbursement_Mapping_pr_2015.zip', '493c022db17a70fcdcbb41bf0ad61a47');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2015-10-01', '2016-PCS-Long-Abbrev-Titles.zip', 'd5ea519d0257db0ed7deb0406a4d0503');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2015-10-01', '2016-General-Equivalence-Mappings.zip', '3324a45b6040be7e48ab770a0d3ca695');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2015-10-01', '2016-Code-Descriptions-in-Tabular-Order.zip', '518a47fe9e268e4fb72fecf633d15f17');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2015-10-01', '2016-ProcedureGEMs.zip', '45a8d9da18d8aed57f0c6ea91e3e8fe4');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2015-10-01', 'Reimbursement_Mapping_dx_2016.zip', '1b53b512e10c1fdf7ae4cfd1baa8dfbb');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2015-10-01', 'Reimbursement_Mapping_pr_2016.zip', '3c780dd103d116aa57980decfddd4f19');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2016-10-01', '2017-PCS-Long-Abbrev-Titles.zip', '4669c47f6a9ca34bf4c14d7f93b37993');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2016-10-01', '2017-GEM-DC.zip', '5a0affdc77a152e6971781233ee969c1');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2016-10-01', '2017-ICD10-Code-Descriptions.zip', 'ed9c159cb4ac4ae4f145062e15f83291');
INSERT INTO `supported_external_dataloads` (`load_type`, `load_source`, `load_release_date`, `load_filename`, `load_checksum`) VALUES
('ICD10', 'CMS', '2016-10-01', '2017-GEM-PCS.zip', 'a4e08b08fb9a53c81385867c82aa8a9e');

-- --------------------------------------------------------
-- Table structure for table `transactions`
--

DROP TABLE IF EXISTS `transactions`;
CREATE TABLE `transactions` (
  `id`                      bigint(20)   NOT NULL auto_increment,
  `date`                    datetime     default NULL,
  `title`                   varchar(255) NOT NULL DEFAULT '',
  `pid`                     bigint(20)   default NULL,
  `user`                    varchar(255) NOT NULL DEFAULT '',
  `groupname`               varchar(255) NOT NULL DEFAULT '',
  `authorized`              tinyint(4)   default NULL,
  PRIMARY KEY  (`id`),
  KEY `pid` (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL auto_increment,
  `username` varchar(255) default NULL,
  `password` longtext,
  `authorized` tinyint(4) default NULL,
  `info` longtext,
  `source` tinyint(4) default NULL,
  `fname` varchar(255) default NULL,
  `mname` varchar(255) default NULL,
  `lname` varchar(255) default NULL,
  `suffix` varchar(255) default NULL,
  `federaltaxid` varchar(255) default NULL,
  `federaldrugid` varchar(255) default NULL,
  `upin` varchar(255) default NULL,
  `facility` varchar(255) default NULL,
  `facility_id` int(11) NOT NULL default '0',
  `see_auth` int(11) NOT NULL default '1',
  `active` tinyint(1) NOT NULL default '1',
  `npi` varchar(15) default NULL,
  `title` varchar(30) default NULL,
  `specialty` varchar(255) default NULL,
  `billname` varchar(255) default NULL,
  `email` varchar(255) default NULL,
  `email_direct` varchar(255) NOT NULL default '',
  `url` varchar(255) default NULL,
  `assistant` varchar(255) default NULL,
  `organization` varchar(255) default NULL,
  `valedictory` varchar(255) default NULL,
  `street` varchar(60) default NULL,
  `streetb` varchar(60) default NULL,
  `city` varchar(30) default NULL,
  `state` varchar(30) default NULL,
  `zip` varchar(20) default NULL,
  `street2` varchar(60) default NULL,
  `streetb2` varchar(60) default NULL,
  `city2` varchar(30) default NULL,
  `state2` varchar(30) default NULL,
  `zip2` varchar(20) default NULL,
  `phone` varchar(30) default NULL,
  `fax` varchar(30) default NULL,
  `phonew1` varchar(30) default NULL,
  `phonew2` varchar(30) default NULL,
  `phonecell` varchar(30) default NULL,
  `notes` text,
  `cal_ui` tinyint(4) NOT NULL default '1',
  `taxonomy` varchar(30) NOT NULL DEFAULT '207Q00000X',
  `calendar` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = appears in calendar',
  `abook_type` varchar(31) NOT NULL DEFAULT '',
  `pwd_expiration_date` date default NULL,
  `pwd_history1` longtext,
  `pwd_history2` longtext,
  `default_warehouse` varchar(31) NOT NULL DEFAULT '',
  `irnpool` varchar(31) NOT NULL DEFAULT '',
  `state_license_number` VARCHAR(25) DEFAULT NULL,
  `newcrop_user_role` VARCHAR(30) DEFAULT NULL,
  `cpoe` tinyint(1) NULL DEFAULT NULL,
  `physician_type` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

--
-- Dumping data for table `users`
--
-- NOTE THIS IS DONE AFTER INSTALLATION WHERE THE sql/official_additional_users.sql script is called durig setup
--  (so these inserts can be found in the sql/official_additional_users.sql script)


-- --------------------------------------------------------

--
-- Table structure for table `user_secure`
--
CREATE TABLE `users_secure` (
  `id` bigint(20) NOT NULL,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255),
  `salt` varchar(255),
  `last_update` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `password_history1` varchar(255),
  `salt_history1` varchar(255),
  `password_history2` varchar(255),
  `salt_history2` varchar(255),
  PRIMARY KEY (`id`),
  UNIQUE KEY `USERNAME_ID` (`id`,`username`)
) ENGINE=InnoDb;

-- --------------------------------------------------------

--
-- Table structure for table `user_settings`
--

CREATE TABLE `user_settings` (
  `setting_user`  bigint(20)   NOT NULL DEFAULT 0,
  `setting_label` varchar(100)  NOT NULL,
  `setting_value` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`setting_user`, `setting_label`)
) ENGINE=InnoDB;

--
-- Dumping data for table `user_settings`
--

INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'allergy_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'appointments_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'billing_ps_expand', '0');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'clinical_reminders_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'demographics_ps_expand', '0');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'dental_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'directives_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'disclosures_ps_expand', '0');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'immunizations_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'insurance_ps_expand', '0');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'medical_problem_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'medication_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'patient_reminders_ps_expand', '0');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'pnotes_ps_expand', '0');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'prescriptions_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'surgery_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'vitals_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'gacl_protect', '0');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (1, 'gacl_protect', '1');

-- --------------------------------------------------------

--
-- Table structure for table `voids`
--

DROP TABLE IF EXISTS `voids`;
CREATE TABLE `voids` (
  `void_id`                bigint(20)    NOT NULL AUTO_INCREMENT,
  `patient_id`             bigint(20)    NOT NULL            COMMENT 'references patient_data.pid',
  `encounter_id`           bigint(20)    NOT NULL DEFAULT 0  COMMENT 'references form_encounter.encounter',
  `what_voided`            varchar(31)   NOT NULL            COMMENT 'checkout,receipt and maybe other options later',
  `date_original`          datetime      DEFAULT NULL        COMMENT 'time of original action that is now voided',
  `date_voided`            datetime      NOT NULL            COMMENT 'time of void action',
  `user_id`                bigint(20)    NOT NULL            COMMENT 'references users.id',
  `amount1`                decimal(12,2) NOT NULL DEFAULT 0  COMMENT 'for checkout,receipt total voided adjustments',
  `amount2`                decimal(12,2) NOT NULL DEFAULT 0  COMMENT 'for checkout,receipt total voided payments',
  `other_info`             text                              COMMENT 'for checkout,receipt the old invoice refno',
  PRIMARY KEY (`void_id`),
  KEY datevoided (date_voided),
  KEY pidenc (patient_id, encounter_id)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `x12_partners`
--

DROP TABLE IF EXISTS `x12_partners`;
CREATE TABLE `x12_partners` (
  `id` int(11) NOT NULL default '0',
  `name` varchar(255) default NULL,
  `id_number` varchar(255) default NULL,
  `x12_sender_id` varchar(255) default NULL,
  `x12_receiver_id` varchar(255) default NULL,
  `x12_version` varchar(255) default NULL,
  `processing_format` enum('standard','medi-cal','cms','proxymed') default NULL,
  `x12_isa01` VARCHAR( 2 ) NOT NULL DEFAULT '00' COMMENT 'User logon Required Indicator',
  `x12_isa02` VARCHAR( 10 ) NOT NULL DEFAULT '          ' COMMENT 'User Logon',
  `x12_isa03` VARCHAR( 2 ) NOT NULL DEFAULT '00' COMMENT 'User password required Indicator',
  `x12_isa04` VARCHAR( 10 ) NOT NULL DEFAULT '          ' COMMENT 'User Password',
  `x12_isa05` char(2)     NOT NULL DEFAULT 'ZZ',
  `x12_isa07` char(2)     NOT NULL DEFAULT 'ZZ',
  `x12_isa14` char(1)     NOT NULL DEFAULT '0',
  `x12_isa15` char(1)     NOT NULL DEFAULT 'P',
  `x12_gs02`  varchar(15) NOT NULL DEFAULT '',
  `x12_per06` varchar(80) NOT NULL DEFAULT '',
  `x12_gs03`  varchar(15) NOT NULL DEFAULT '',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------------
-- Table structure for table `automatic_notification`
--

DROP TABLE IF EXISTS `automatic_notification`;
CREATE TABLE `automatic_notification` (
  `notification_id` int(5) NOT NULL auto_increment,
  `sms_gateway_type` varchar(255) NOT NULL,
  `next_app_date` date NOT NULL,
  `next_app_time` varchar(10) NOT NULL,
  `provider_name` varchar(100) NOT NULL,
  `message` text,
  `email_sender` varchar(100) NOT NULL,
  `email_subject` varchar(100) NOT NULL,
  `type` enum('SMS','Email') NOT NULL default 'SMS',
  `notification_sent_date` datetime NOT NULL,
  PRIMARY KEY  (`notification_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 ;

--
-- Dumping data for table `automatic_notification`
--

INSERT INTO `automatic_notification` (`notification_id`, `sms_gateway_type`, `next_app_date`, `next_app_time`, `provider_name`, `message`, `email_sender`, `email_subject`, `type`, `notification_sent_date`) VALUES (1, 'CLICKATELL', '0000-00-00', ':', 'EMR GROUP 1 .. SMS', 'Welcome to EMR GROUP 1.. SMS', '', '', 'SMS', '0000-00-00 00:00:00'),
(2, '', '2007-10-02', '05:50', 'EMR GROUP', 'Welcome to EMR GROUP . Email', 'EMR Group', 'Welcome to EMR GROUP', 'Email', '2007-09-30 00:00:00');

-- --------------------------------------------------------

--
-- Table structure for table `notification_log`
--

DROP TABLE IF EXISTS `notification_log`;
CREATE TABLE `notification_log` (
  `iLogId` int(11) NOT NULL auto_increment,
  `pid` int(7) NOT NULL,
  `pc_eid` int(11) unsigned NULL,
  `sms_gateway_type` varchar(50) NOT NULL,
  `smsgateway_info` varchar(255) NOT NULL,
  `message` text,
  `email_sender` varchar(255) NOT NULL,
  `email_subject` varchar(255) NOT NULL,
  `type` enum('SMS','Email') NOT NULL,
  `patient_info` text,
  `pc_eventDate` date NOT NULL,
  `pc_endDate` date NOT NULL,
  `pc_startTime` time NOT NULL,
  `pc_endTime` time NOT NULL,
  `dSentDateTime` datetime NOT NULL,
  PRIMARY KEY  (`iLogId`)
) ENGINE=InnoDB AUTO_INCREMENT=5 ;

-- --------------------------------------------------------

--
-- Table structure for table `notification_settings`
--

DROP TABLE IF EXISTS `notification_settings`;
CREATE TABLE `notification_settings` (
  `SettingsId` int(3) NOT NULL auto_increment,
  `Send_SMS_Before_Hours` int(3) NOT NULL,
  `Send_Email_Before_Hours` int(3) NOT NULL,
  `SMS_gateway_username` varchar(100) NOT NULL,
  `SMS_gateway_password` varchar(100) NOT NULL,
  `SMS_gateway_apikey` varchar(100) NOT NULL,
  `type` varchar(50) NOT NULL,
  PRIMARY KEY  (`SettingsId`)
) ENGINE=InnoDB AUTO_INCREMENT=2 ;

--
-- Dumping data for table `notification_settings`
--

INSERT INTO `notification_settings` (`SettingsId`, `Send_SMS_Before_Hours`, `Send_Email_Before_Hours`, `SMS_gateway_username`, `SMS_gateway_password`, `SMS_gateway_apikey`, `type`) VALUES (1, 150, 150, 'sms username', 'sms password', 'sms api key', 'SMS/Email Settings');

-- -------------------------------------------------------------------

CREATE TABLE chart_tracker (
  ct_pid            int(11)       NOT NULL,
  ct_when           datetime      NOT NULL,
  ct_userid         bigint(20)    NOT NULL DEFAULT 0,
  ct_location       varchar(31)   NOT NULL DEFAULT '',
  PRIMARY KEY (ct_pid, ct_when)
) ENGINE=InnoDB;

CREATE TABLE ar_session (
  session_id     int unsigned  NOT NULL AUTO_INCREMENT,
  payer_id       int(11)       NOT NULL            COMMENT '0=pt else references insurance_companies.id',
  user_id        int(11)       NOT NULL            COMMENT 'references users.id for session owner',
  closed         tinyint(1)    NOT NULL DEFAULT 0  COMMENT '0=no, 1=yes',
  reference      varchar(255)  NOT NULL DEFAULT '' COMMENT 'check or EOB number',
  check_date     date          DEFAULT NULL,
  deposit_date   date          DEFAULT NULL,
  pay_total      decimal(12,2) NOT NULL DEFAULT 0,
  created_time timestamp NOT NULL default CURRENT_TIMESTAMP,
  modified_time datetime NOT NULL,
  global_amount decimal( 12, 2 ) NOT NULL ,
  payment_type varchar( 50 ) NOT NULL ,
  description text,
  adjustment_code varchar( 50 ) NOT NULL ,
  post_to_date date NOT NULL ,
  patient_id int( 11 ) NOT NULL ,
  payment_method varchar( 25 ) NOT NULL,
  PRIMARY KEY (session_id),
  KEY user_closed (user_id, closed),
  KEY deposit_date (deposit_date)
) ENGINE=InnoDB;

CREATE TABLE ar_activity (
  pid            int(11)       NOT NULL,
  encounter      int(11)       NOT NULL,
  sequence_no    int unsigned  NOT NULL            COMMENT 'Ar_activity sequence_no, incremented in code',
  `code_type`    varchar(12)   NOT NULL DEFAULT '',
  code           varchar(20)   NOT NULL            COMMENT 'empty means claim level',
  modifier       varchar(12)   NOT NULL DEFAULT '',
  payer_type     int           NOT NULL            COMMENT '0=pt, 1=ins1, 2=ins2, etc',
  post_time      datetime      NOT NULL,
  post_user      int(11)       NOT NULL            COMMENT 'references users.id',
  session_id     int unsigned  NOT NULL            COMMENT 'references ar_session.session_id',
  memo           varchar(255)  NOT NULL DEFAULT '' COMMENT 'adjustment reasons go here',
  pay_amount     decimal(12,2) NOT NULL DEFAULT 0  COMMENT 'either pay or adj will always be 0',
  adj_amount     decimal(12,2) NOT NULL DEFAULT 0,
  modified_time datetime NOT NULL,
  follow_up char(1) NOT NULL,
  follow_up_note text,
  account_code varchar(15) NOT NULL,
  reason_code varchar(255) DEFAULT NULL COMMENT 'Use as needed to show the primary payer adjustment reason code',
  PRIMARY KEY (pid, encounter, sequence_no),
  KEY session_id (session_id)
) ENGINE=InnoDB;

CREATE TABLE `users_facility` (
  `tablename` varchar(64) NOT NULL,
  `table_id` int(11) NOT NULL,
  `facility_id` int(11) NOT NULL,
  PRIMARY KEY (`tablename`,`table_id`,`facility_id`)
) ENGINE=InnoDB COMMENT='joins users or patient_data to facility table';

CREATE TABLE `lbf_data` (
  `form_id`     int(11)      NOT NULL AUTO_INCREMENT COMMENT 'references forms.form_id',
  `field_id`    varchar(31)  NOT NULL COMMENT 'references layout_options.field_id',
  `field_value` TEXT,
  PRIMARY KEY (`form_id`,`field_id`)
) ENGINE=InnoDB COMMENT='contains all data from layout-based forms';

CREATE TABLE `lbt_data` (
  `form_id`     bigint(20)   NOT NULL COMMENT 'references transactions.id',
  `field_id`    varchar(31)  NOT NULL COMMENT 'references layout_options.field_id',
  `field_value` TEXT,
  PRIMARY KEY (`form_id`,`field_id`)
) ENGINE=InnoDB COMMENT='contains all data from layout-based transactions';

CREATE TABLE gprelations (
  type1 int(2)     NOT NULL,
  id1   bigint(20) NOT NULL,
  type2 int(2)     NOT NULL,
  id2   bigint(20) NOT NULL,
  PRIMARY KEY (type1,id1,type2,id2),
  KEY key2  (type2,id2)
) ENGINE=InnoDB COMMENT='general purpose relations';

CREATE TABLE `procedure_providers` (
  `ppid`         bigint(20)   NOT NULL auto_increment,
  `name`         varchar(255) NOT NULL DEFAULT '',
  `npi`          varchar(15)  NOT NULL DEFAULT '',
  `send_app_id`  varchar(255) NOT NULL DEFAULT ''  COMMENT 'Sending application ID (MSH-3.1)',
  `send_fac_id`  varchar(255) NOT NULL DEFAULT ''  COMMENT 'Sending facility ID (MSH-4.1)',
  `recv_app_id`  varchar(255) NOT NULL DEFAULT ''  COMMENT 'Receiving application ID (MSH-5.1)',
  `recv_fac_id`  varchar(255) NOT NULL DEFAULT ''  COMMENT 'Receiving facility ID (MSH-6.1)',
  `DorP`         char(1)      NOT NULL DEFAULT 'D' COMMENT 'Debugging or Production (MSH-11)',
  `direction`    char(1)      NOT NULL DEFAULT 'B' COMMENT 'Bidirectional or Results-only',
  `protocol`     varchar(15)  NOT NULL DEFAULT 'DL',
  `remote_host`  varchar(255) NOT NULL DEFAULT '',
  `login`        varchar(255) NOT NULL DEFAULT '',
  `password`     varchar(255) NOT NULL DEFAULT '',
  `orders_path`  varchar(255) NOT NULL DEFAULT '',
  `results_path` varchar(255) NOT NULL DEFAULT '',
  `notes`        text,
  `lab_director` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`ppid`)
) ENGINE=InnoDB;

CREATE TABLE `procedure_type` (
  `procedure_type_id`   bigint(20)   NOT NULL AUTO_INCREMENT,
  `parent`              bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references procedure_type.procedure_type_id',
  `name`                varchar(63)  NOT NULL DEFAULT '' COMMENT 'name for this category, procedure or result type',
  `lab_id`              bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references procedure_providers.ppid, 0 means default to parent',
  `procedure_code`      varchar(31)  NOT NULL DEFAULT '' COMMENT 'code identifying this procedure',
  `procedure_type`      varchar(31)  NOT NULL DEFAULT '' COMMENT 'see list proc_type',
  `body_site`           varchar(31)  NOT NULL DEFAULT '' COMMENT 'where to do injection, e.g. arm, buttok',
  `specimen`            varchar(31)  NOT NULL DEFAULT '' COMMENT 'blood, urine, saliva, etc.',
  `route_admin`         varchar(31)  NOT NULL DEFAULT '' COMMENT 'oral, injection',
  `laterality`          varchar(31)  NOT NULL DEFAULT '' COMMENT 'left, right, ...',
  `description`         varchar(255) NOT NULL DEFAULT '' COMMENT 'descriptive text for procedure_code',
  `standard_code`       varchar(255) NOT NULL DEFAULT '' COMMENT 'industry standard code type and code (e.g. CPT4:12345)',
  `related_code`        varchar(255) NOT NULL DEFAULT '' COMMENT 'suggested code(s) for followup services if result is abnormal',
  `units`               varchar(31)  NOT NULL DEFAULT '' COMMENT 'default for procedure_result.units',
  `range`               varchar(255) NOT NULL DEFAULT '' COMMENT 'default for procedure_result.range',
  `seq`                 int(11)      NOT NULL default 0  COMMENT 'sequence number for ordering',
  `activity`            tinyint(1)   NOT NULL default 1  COMMENT '1=active, 0=inactive',
  `notes`               varchar(255) NOT NULL default '' COMMENT 'additional notes to enhance description',
  PRIMARY KEY (`procedure_type_id`),
  KEY parent (parent)
) ENGINE=InnoDB;

CREATE TABLE `procedure_questions` (
  `lab_id`              bigint(20)   NOT NULL DEFAULT 0   COMMENT 'references procedure_providers.ppid to identify the lab',
  `procedure_code`      varchar(31)  NOT NULL DEFAULT ''  COMMENT 'references procedure_type.procedure_code to identify this order type',
  `question_code`       varchar(31)  NOT NULL DEFAULT ''  COMMENT 'code identifying this question',
  `seq`                 int(11)      NOT NULL default 0   COMMENT 'sequence number for ordering',
  `question_text`       varchar(255) NOT NULL DEFAULT ''  COMMENT 'descriptive text for question_code',
  `required`            tinyint(1)   NOT NULL DEFAULT 0   COMMENT '1 = required, 0 = not',
  `maxsize`             int          NOT NULL DEFAULT 0   COMMENT 'maximum length if text input field',
  `fldtype`             char(1)      NOT NULL DEFAULT 'T' COMMENT 'Text, Number, Select, Multiselect, Date, Gestational-age',
  `options`             text                              COMMENT 'choices for fldtype S and T',
  `tips`                varchar(255) NOT NULL DEFAULT ''  COMMENT 'Additional instructions for answering the question',
  `activity`            tinyint(1)   NOT NULL DEFAULT 1   COMMENT '1 = active, 0 = inactive',
  PRIMARY KEY (`lab_id`, `procedure_code`, `question_code`)
) ENGINE=InnoDB;

CREATE TABLE `procedure_order` (
  `procedure_order_id`     bigint(20)   NOT NULL AUTO_INCREMENT,
  `provider_id`            bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references users.id, the ordering provider',
  `patient_id`             bigint(20)   NOT NULL            COMMENT 'references patient_data.pid',
  `encounter_id`           bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references form_encounter.encounter',
  `date_collected`         datetime     DEFAULT NULL        COMMENT 'time specimen collected',
  `date_ordered`           date         DEFAULT NULL,
  `order_priority`         varchar(31)  NOT NULL DEFAULT '',
  `order_status`           varchar(31)  NOT NULL DEFAULT '' COMMENT 'pending,routed,complete,canceled',
  `patient_instructions`   text,
  `activity`               tinyint(1)   NOT NULL DEFAULT 1  COMMENT '0 if deleted',
  `control_id`             varchar(255) NOT NULL DEFAULT '' COMMENT 'This is the CONTROL ID that is sent back from lab',
  `lab_id`                 bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references procedure_providers.ppid',
  `specimen_type`          varchar(31)  NOT NULL DEFAULT '' COMMENT 'from the Specimen_Type list',
  `specimen_location`      varchar(31)  NOT NULL DEFAULT '' COMMENT 'from the Specimen_Location list',
  `specimen_volume`        varchar(30)  NOT NULL DEFAULT '' COMMENT 'from a text input field',
  `date_transmitted`       datetime     DEFAULT NULL        COMMENT 'time of order transmission, null if unsent',
  `clinical_hx`            varchar(255) NOT NULL DEFAULT '' COMMENT 'clinical history text that may be relevant to the order',
  `external_id` VARCHAR(20) DEFAULT NULL,
  `history_order` enum('0','1') DEFAULT '0' COMMENT 'references order is added for history purpose only.',
  PRIMARY KEY (`procedure_order_id`),
  KEY datepid (date_ordered, patient_id),
  KEY `patient_id` (`patient_id`)
) ENGINE=InnoDB;

CREATE TABLE `procedure_order_code` (
  `procedure_order_id`  bigint(20)  NOT NULL                COMMENT 'references procedure_order.procedure_order_id',
  `procedure_order_seq` int(11)     NOT NULL COMMENT 'Supports multiple tests per order. Procedure_order_seq, incremented in code',
  `procedure_code`      varchar(31) NOT NULL DEFAULT ''     COMMENT 'like procedure_type.procedure_code',
  `procedure_name`      varchar(255) NOT NULL DEFAULT ''    COMMENT 'descriptive name of the procedure code',
  `procedure_source`    char(1)     NOT NULL DEFAULT '1'    COMMENT '1=original order, 2=added after order sent',
  `diagnoses`           text                                COMMENT 'diagnoses and maybe other coding (e.g. ICD9:111.11)',
  `do_not_send`         tinyint(1)  NOT NULL DEFAULT '0'    COMMENT '0 = normal, 1 = do not transmit to lab',
  `procedure_order_title` varchar( 255 ) NULL DEFAULT NULL,
  PRIMARY KEY (`procedure_order_id`, `procedure_order_seq`)
) ENGINE=InnoDB;

CREATE TABLE `procedure_answers` (
  `procedure_order_id`  bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references procedure_order.procedure_order_id',
  `procedure_order_seq` int(11)      NOT NULL DEFAULT 0  COMMENT 'references procedure_order_code.procedure_order_seq',
  `question_code`       varchar(31)  NOT NULL DEFAULT '' COMMENT 'references procedure_questions.question_code',
  `answer_seq`          int(11)      NOT NULL COMMENT 'supports multiple-choice questions. answer_seq, incremented in code',
  `answer`              varchar(255) NOT NULL DEFAULT '' COMMENT 'answer data',
  PRIMARY KEY (`procedure_order_id`, `procedure_order_seq`, `question_code`, `answer_seq`)
) ENGINE=InnoDB;

CREATE TABLE `procedure_report` (
  `procedure_report_id` bigint(20)     NOT NULL AUTO_INCREMENT,
  `procedure_order_id`  bigint(20)     DEFAULT NULL   COMMENT 'references procedure_order.procedure_order_id',
  `procedure_order_seq` int(11)        NOT NULL DEFAULT 1  COMMENT 'references procedure_order_code.procedure_order_seq',
  `date_collected`      datetime       DEFAULT NULL,
  `date_collected_tz`   varchar(5)     DEFAULT ''          COMMENT '+-hhmm offset from UTC',
  `date_report`         datetime       DEFAULT NULL,
  `date_report_tz`      varchar(5)     DEFAULT ''          COMMENT '+-hhmm offset from UTC',
  `source`              bigint(20)     NOT NULL DEFAULT 0  COMMENT 'references users.id, who entered this data',
  `specimen_num`        varchar(63)    NOT NULL DEFAULT '',
  `report_status`       varchar(31)    NOT NULL DEFAULT '' COMMENT 'received,complete,error',
  `review_status`       varchar(31)    NOT NULL DEFAULT 'received' COMMENT 'pending review status: received,reviewed',
  `report_notes`        text           COMMENT 'notes from the lab',
  PRIMARY KEY (`procedure_report_id`),
  KEY procedure_order_id (procedure_order_id)
) ENGINE=InnoDB;

CREATE TABLE `procedure_result` (
  `procedure_result_id` bigint(20)   NOT NULL AUTO_INCREMENT,
  `procedure_report_id` bigint(20)   NOT NULL            COMMENT 'references procedure_report.procedure_report_id',
  `result_data_type`    char(1)      NOT NULL DEFAULT 'S' COMMENT 'N=Numeric, S=String, F=Formatted, E=External, L=Long text as first line of comments',
  `result_code`         varchar(31)  NOT NULL DEFAULT '' COMMENT 'LOINC code, might match a procedure_type.procedure_code',
  `result_text`         varchar(255) NOT NULL DEFAULT '' COMMENT 'Description of result_code',
  `date`                datetime     DEFAULT NULL        COMMENT 'lab-provided date specific to this result',
  `facility`            varchar(255) NOT NULL DEFAULT '' COMMENT 'lab-provided testing facility ID',
  `units`               varchar(31)  NOT NULL DEFAULT '',
  `result`              varchar(255) NOT NULL DEFAULT '',
  `range`               varchar(255) NOT NULL DEFAULT '',
  `abnormal`            varchar(31)  NOT NULL DEFAULT '' COMMENT 'no,yes,high,low',
  `comments`            text                             COMMENT 'comments from the lab',
  `document_id`         bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references documents.id if this result is a document',
  `result_status`       varchar(31)  NOT NULL DEFAULT '' COMMENT 'preliminary, cannot be done, final, corrected, incompete...etc.',
  PRIMARY KEY (`procedure_result_id`),
  KEY procedure_report_id (procedure_report_id)
) ENGINE=InnoDB;

CREATE TABLE `globals` (
  `gl_name`             varchar(63)    NOT NULL,
  `gl_index`            int(11)        NOT NULL DEFAULT 0,
  `gl_value`            varchar(255)   NOT NULL DEFAULT '',
  PRIMARY KEY (`gl_name`, `gl_index`)
) ENGINE=InnoDB;

CREATE TABLE code_types (
  ct_key  varchar(15) NOT NULL           COMMENT 'short alphanumeric name',
  ct_id   int(11)     UNIQUE NOT NULL    COMMENT 'numeric identifier',
  ct_seq  int(11)     NOT NULL DEFAULT 0 COMMENT 'sort order',
  ct_mod  int(11)     NOT NULL DEFAULT 0 COMMENT 'length of modifier field',
  ct_just varchar(15) NOT NULL DEFAULT ''COMMENT 'ct_key of justify type, if any',
  ct_mask varchar(9)  NOT NULL DEFAULT ''COMMENT 'formatting mask for code values',
  ct_fee  tinyint(1)  NOT NULL default 0 COMMENT '1 if fees are used',
  ct_rel  tinyint(1)  NOT NULL default 0 COMMENT '1 if can relate to other code types',
  ct_nofs tinyint(1)  NOT NULL default 0 COMMENT '1 if to be hidden in the fee sheet',
  ct_diag tinyint(1)  NOT NULL default 0 COMMENT '1 if this is a diagnosis type',
  ct_active tinyint(1) NOT NULL default 1 COMMENT '1 if this is active',
  ct_label varchar(31) NOT NULL default '' COMMENT 'label of this code type',
  ct_external tinyint(1) NOT NULL default 0 COMMENT '0 if stored codes in codes tables, 1 or greater if codes stored in external tables',
  ct_claim tinyint(1) NOT NULL default 0 COMMENT '1 if this is used in claims',
  ct_proc tinyint(1) NOT NULL default 0 COMMENT '1 if this is a procedure type',
  ct_term tinyint(1) NOT NULL default 0 COMMENT '1 if this is a clinical term',
  ct_problem tinyint(1) NOT NULL default 0 COMMENT '1 if this code type is used as a medical problem',
  ct_drug tinyint(1) NOT NULL default 0 COMMENT '1 if this code type is used as a medication',
  PRIMARY KEY (ct_key)
) ENGINE=InnoDB;

INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('ICD9' , 2, 1, 0, ''    , 0, 0, 0, 1, 1, 'ICD9 Diagnosis', 4, 1, 0, 0, 1);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('CPT4' , 1, 2, 12, 'ICD9', 1, 0, 0, 0, 1, 'CPT4 Procedure/Service', 0, 1, 1, 0, 0);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('HCPCS', 3, 3, 12, 'ICD9', 1, 0, 0, 0, 1, 'HCPCS Procedure/Service', 0, 1, 1, 0, 0);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('CVX'  , 100, 100, 0, '', 0, 0, 1, 0, 1, 'CVX Immunization', 0, 0, 0, 0, 0);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('DSMIV' , 101, 101, 0, '', 0, 0, 0, 1, 0, 'DSMIV Diagnosis', 0, 1, 0, 0, 1);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('ICD10' , 102, 102, 0, '', 0, 0, 0, 1, 1, 'ICD10 Diagnosis', 1, 1, 0, 0, 1);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('SNOMED' , 103, 103, 0, '', 0, 0, 0, 1, 0, 'SNOMED Diagnosis', 2, 1, 0, 0, 1);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('CPTII' , 104, 104, 0, 'ICD9', 0, 0, 0, 0, 0, 'CPTII Performance Measures', 0, 1, 0, 0, 0);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('ICD9-SG' , 105, 105, 12, 'ICD9', 1, 0, 0, 0, 0, 'ICD9 Procedure/Service', 5, 1, 1, 0, 0);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('ICD10-PCS' , 106, 106, 12, 'ICD10', 1, 0, 0, 0, 0, 'ICD10 Procedure/Service', 6, 1, 1, 0, 0);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('SNOMED-CT' , 107, 107, 0, '', 0, 0, 1, 0, 0, 'SNOMED Clinical Term', 7, 0, 0, 1, 0);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('SNOMED-PR' , 108, 108, 0, 'SNOMED', 1, 0, 0, 0, 0, 'SNOMED Procedure', 9, 1, 1, 0, 0);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem, ct_drug ) VALUES ('RXCUI', 109, 109, 0, '', 0, 0, 1, 0, 0, 'RXCUI Medication', 0, 0, 0, 0, 0, 1);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('LOINC', 110, 110, 0, '', 0, 0, 1, 0, 1, 'LOINC', 0, 0, 0, 0, 0);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('PHIN Questions', 111, 111, 0, '', 0, 0, 1, 0, 1, 'PHIN Questions', 0, 0, 0, 0, 0);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag, ct_active, ct_label, ct_external, ct_claim, ct_proc, ct_term, ct_problem ) VALUES ('NCI-CONCEPT-ID', 112, 112, 0, '', 0, 0, 1, 0, 1, 'NCI CONCEPT ID', 0, 0, 0, 0, 0);

INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('lists', 'code_types', 'Code Types', 1);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'disclosure_type','Disclosure Type', 3,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('disclosure_type', 'disclosure-treatment', 'Treatment', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('disclosure_type', 'disclosure-payment', 'Payment', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('disclosure_type', 'disclosure-healthcareoperations', 'Health Care Operations', 30, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'smoking_status','Smoking Status', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('smoking_status', '1', 'Current every day smoker', 10, 0, 'SNOMED-CT:449868002');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('smoking_status', '2', 'Current some day smoker', 20, 0, 'SNOMED-CT:428041000124106');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('smoking_status', '3', 'Former smoker', 30, 0, 'SNOMED-CT:8517006');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('smoking_status', '4', 'Never smoker', 40, 0, 'SNOMED-CT:266919005');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('smoking_status', '5', 'Smoker, current status unknown', 50, 0, 'SNOMED-CT:77176002');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('smoking_status', '9', 'Unknown if ever smoked', 60, 0, 'SNOMED-CT:266927001');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('smoking_status', '15', 'Heavy tobacco smoker', 70, 0, 'SNOMED-CT:428071000124103');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, codes ) VALUES ('smoking_status', '16', 'Light tobacco smoker', 80, 0, 'SNOMED-CT:428061000124105');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'race','Race', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value ) VALUES ('race', 'declne_to_specfy', 'Declined To Specify', 0, 0, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('race', 'amer_ind_or_alaska_native', 'American Indian or Alaska Native', 10, 0, '1002-5');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('race', 'Asian', 'Asian',20,0, '2028-9');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('race', 'black_or_afri_amer', 'Black or African American',30,0, '2054-5');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('race', 'native_hawai_or_pac_island', 'Native Hawaiian or Other Pacific Islander',40,0, '2076-8');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('race', 'white', 'White',50,0, '2106-3');
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','abenaki','1006-6','Abenaki', '0',60);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','absentee_shawnee','1579-2','Absentee Shawnee', '0',70);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','acoma','1490-2','Acoma', '0',80);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','afghanistani','2126-1','Afghanistani', '0',90);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','african','2060-2','African', '0',100);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','african_american','2058-6','African American', '0',110);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','agdaagux','1994-3','Agdaagux', '0',120);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','agua_caliente','1212-0','Agua Caliente', '0',130);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','agua_caliente_cahuilla','1045-4','Agua Caliente Cahuilla', '0',140);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ahtna','1740-0','Ahtna', '0',150);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ak-chin','1654-3','Ak-Chin', '0',160);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','akhiok','1993-5','Akhiok', '0',170);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','akiachak','1897-8','Akiachak', '0',180);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','akiak','1898-6','Akiak', '0',190);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','akutan','2007-3','Akutan', '0',200);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alabama_coushatta','1187-4','Alabama Coushatta', '0',210);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alabama_creek','1194-0','Alabama Creek', '0',220);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alabama_quassarte','1195-7','Alabama Quassarte', '0',230);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alakanuk','1899-4','Alakanuk', '0',240);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alamo_navajo','1383-9','Alamo Navajo', '0',250);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alanvik','1744-2','Alanvik', '0',260);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alaska_indian','1737-6','Alaska Indian', '0',270);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alaska_native','1735-0','Alaska Native', '0',280);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alaskan_athabascan','1739-2','Alaskan Athabascan', '0',290);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alatna','1741-8','Alatna', '0',300);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','aleknagik','1900-0','Aleknagik', '0',310);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','aleut','1966-1','Aleut', '0',320);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','aleut_corporation','2008-1','Aleut Corporation', '0',330);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','aleutian','2009-9','Aleutian', '0',340);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','aleutian_islander','2010-7','Aleutian Islander', '0',350);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alexander','1742-6','Alexander', '0',360);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','algonquian','1008-2','Algonquian', '0',370);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','allakaket','1743-4','Allakaket', '0',380);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','allen_canyon','1671-7','Allen Canyon', '0',390);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alpine','1688-1','Alpine', '0',400);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alsea','1392-0','Alsea', '0',410);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','alutiiq_aleut','1968-7','Alutiiq Aleut', '0',420);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ambler','1845-7','Ambler', '0',430);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','american_indian','1004-1','American Indian', '0',440);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','anaktuvuk','1846-5','Anaktuvuk', '0',460);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','anaktuvuk_pass','1847-3','Anaktuvuk Pass', '0',470);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','andreafsky','1901-8','Andreafsky', '0',480);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','angoon','1814-3','Angoon', '0',490);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','aniak','1902-6','Aniak', '0',500);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','anvik','1745-9','Anvik', '0',510);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','apache','1010-8','Apache', '0',520);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','arab','2129-5','Arab', '0',530);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','arapaho','1021-5','Arapaho', '0',540);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','arctic','1746-7','Arctic', '0',550);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','arctic_slope_corporation','1849-9','Arctic Slope Corporation', '0',560);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','arctic_slope_inupiat','1848-1','Arctic Slope Inupiat', '0',570);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','arikara','1026-4','Arikara', '0',580);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','arizona_tewa','1491-0','Arizona Tewa', '0',590);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','armenian','2109-7','Armenian', '0',600);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','aroostook','1366-4','Aroostook', '0',610);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','asian_indian','2029-7','Asian Indian', '0',630);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','assiniboine','1028-0','Assiniboine', '0',640);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','assiniboine_sioux','1030-6','Assiniboine Sioux', '0',650);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','assyrian','2119-6','Assyrian', '0',660);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','atka','2011-5','Atka', '0',670);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','atmautluak','1903-4','Atmautluak', '0',680);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','atqasuk','1850-7','Atqasuk', '0',690);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','atsina','1265-8','Atsina', '0',700);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','attacapa','1234-4','Attacapa', '0',710);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','augustine','1046-2','Augustine', '0',720);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bad_river','1124-7','Bad River', '0',730);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bahamian','2067-7','Bahamian', '0',740);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bangladeshi','2030-5','Bangladeshi', '0',750);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bannock','1033-0','Bannock', '0',760);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','barbadian','2068-5','Barbadian', '0',770);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','barrio_libre','1712-9','Barrio Libre', '0',780);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','barrow','1851-5','Barrow', '0',790);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','battle_mountain','1587-5','Battle Mountain', '0',800);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bay_mills_chippewa','1125-4','Bay Mills Chippewa', '0',810);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','beaver','1747-5','Beaver', '0',820);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','belkofski','2012-3','Belkofski', '0',830);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bering_straits_inupiat','1852-3','Bering Straits Inupiat', '0',840);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bethel','1904-2','Bethel', '0',850);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bhutanese','2031-3','Bhutanese', '0',860);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','big_cypress','1567-7','Big Cypress', '0',870);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bill_moores_slough','1905-9',"Bill Moore's Slough", '0',880);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','biloxi','1235-1','Biloxi', '0',890);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','birch_creek','1748-3','Birch Creek', '0',900);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bishop','1417-5','Bishop', '0',910);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','black','2056-0','Black', '0',920);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','blackfeet','1035-5','Blackfeet', '0',940);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','blackfoot_sioux','1610-5','Blackfoot Sioux', '0',950);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bois_forte','1126-2','Bois Forte', '0',960);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','botswanan','2061-0','Botswanan', '0',970);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','brevig_mission','1853-1','Brevig Mission', '0',980);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bridgeport','1418-3','Bridgeport', '0',990);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','brighton','1568-5','Brighton', '0',1000);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bristol_bay_aleut','1972-9','Bristol Bay Aleut', '0',1010);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','bristol_bay_yupik','1906-7','Bristol Bay Yupik', '0',1020);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','brotherton','1037-1','Brotherton', '0',1030);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','brule_sioux','1611-3','Brule Sioux', '0',1040);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','buckland','1854-9','Buckland', '0',1050);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','burmese','2032-1','Burmese', '0',1060);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','burns_paiute','1419-1','Burns Paiute', '0',1070);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','burt_lake_band','1039-7','Burt Lake Band', '0',1080);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','burt_lake_chippewa','1127-0','Burt Lake Chippewa', '0',1090);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','burt_lake_ottawa','1412-6','Burt Lake Ottawa', '0',1100);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cabazon','1047-0','Cabazon', '0',1110);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','caddo','1041-3','Caddo', '0',1120);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cahto','1054-6','Cahto', '0',1130);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cahuilla','1044-7','Cahuilla', '0',1140);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','california_tribes','1053-8','California Tribes', '0',1150);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','calista_yupik','1907-5','Calista Yupik', '0',1160);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cambodian','2033-9','Cambodian', '0',1170);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','campo','1223-7','Campo', '0',1180);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','canadian_latinamerican_indian','1068-6','Canadian and Latin American Indian', '0',1190);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','canadian_indian','1069-4','Canadian Indian', '0',1200);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','canoncito_navajo','1384-7','Canoncito Navajo', '0',1210);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cantwell','1749-1','Cantwell', '0',1220);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','capitan_grande','1224-5','Capitan Grande', '0',1230);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','carolinian','2092-5','Carolinian', '0',1240);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','carson','1689-9','Carson', '0',1250);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','catawba','1076-9','Catawba', '0',1260);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cayuga','1286-4','Cayuga', '0',1270);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cayuse','1078-5','Cayuse', '0',1280);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cedarville','1420-9','Cedarville', '0',1290);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','celilo','1393-8','Celilo', '0',1300);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','central_american_indian','1070-2','Central American Indian', '0',1310);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tlingit_and_haida_tribes','1815-0','Central Council of Tlingit and Haida Tribes', '0',1320);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','central_pomo','1465-4','Central Pomo', '0',1330);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chalkyitsik','1750-9','Chalkyitsik', '0',1340);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chamorro','2088-3','Chamorro', '0',1350);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chefornak','1908-3','Chefornak', '0',1360);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chehalis','1080-1','Chehalis', '0',1370);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chemakuan','1082-7','Chemakuan', '0',1380);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chemehuevi','1086-8','Chemehuevi', '0',1390);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chenega','1985-1','Chenega', '0',1400);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cherokee','1088-4','Cherokee', '0',1410);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cherokee_alabama','1089-2','Cherokee Alabama', '0',1420);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cherokee_shawnee','1100-7','Cherokee Shawnee', '0',1430);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cherokees_of_northeast_alabama','1090-0','Cherokees of Northeast Alabama', '0',1440);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cherokees_of_southeast_alabama','1091-8','Cherokees of Southeast Alabama', '0',1450);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chevak','1909-1','Chevak', '0',1460);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cheyenne','1102-3','Cheyenne', '0',1470);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cheyenne_river_sioux','1612-1','Cheyenne River Sioux', '0',1480);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cheyenne-arapaho','1106-4','Cheyenne-Arapaho', '0',1490);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chickahominy','1108-0','Chickahominy', '0',1500);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chickaloon','1751-7','Chickaloon', '0',1510);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chickasaw','1112-2','Chickasaw', '0',1520);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chignik','1973-7','Chignik', '0',1530);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chignik_lagoon','2013-1','Chignik Lagoon', '0',1540);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chignik_lake','1974-5','Chignik Lake', '0',1550);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chilkat','1816-8','Chilkat', '0',1560);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chilkoot','1817-6','Chilkoot', '0',1570);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chimariko','1055-3','Chimariko', '0',1580);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chinese','2034-7','Chinese', '0',1590);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chinik','1855-6','Chinik', '0',1600);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chinook','1114-8','Chinook', '0',1610);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chippewa','1123-9','Chippewa', '0',1620);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chippewa_cree','1150-2','Chippewa Cree', '0',1630);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chiricahua','1011-6','Chiricahua', '0',1640);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chistochina','1752-5','Chistochina', '0',1650);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chitimacha','1153-6','Chitimacha', '0',1660);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chitina','1753-3','Chitina', '0',1670);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','choctaw','1155-1','Choctaw', '0',1680);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chuathbaluk','1910-9','Chuathbaluk', '0',1690);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chugach_aleut','1984-4','Chugach Aleut', '0',1700);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chugach_corporation','1986-9','Chugach Corporation', '0',1710);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chukchansi','1718-6','Chukchansi', '0',1720);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chumash','1162-7','Chumash', '0',1730);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','chuukese','2097-4','Chuukese', '0',1740);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','circle','1754-1','Circle', '0',1750);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','citizen_band_potawatomi','1479-5','Citizen Band Potawatomi', '0',1760);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','clarks_point','1911-7',"Clark's Point", '0',1770);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','clatsop','1115-5','Clatsop', '0',1780);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','clear_lake','1165-0','Clear Lake', '0',1790);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','clifton_choctaw','1156-9','Clifton Choctaw', '0',1800);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','coast_miwok','1056-1','Coast Miwok', '0',1810);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','coast_yurok','1733-5','Coast Yurok', '0',1820);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cochiti','1492-8','Cochiti', '0',1830);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cocopah','1725-1','Cocopah', '0',1840);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','coeur_dalene','1167-6',"Coeur D'Alene", '0',1850);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','coharie','1169-2','Coharie', '0',1860);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','colorado_river','1171-8','Colorado River', '0',1870);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','columbia','1394-6','Columbia', '0',1880);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','columbia_river_chinook','1116-3','Columbia River Chinook', '0',1890);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','colville','1173-4','Colville', '0',1900);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','comanche','1175-9','Comanche', '0',1910);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cook_inlet','1755-8','Cook Inlet', '0',1920);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','coos','1180-9','Coos', '0',1930);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','coos_lower_umpqua_siuslaw','1178-3','Coos, Lower Umpqua, Siuslaw', '0',1940);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','copper_center','1756-6','Copper Center', '0',1950);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','copper_river','1757-4','Copper River', '0',1960);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','coquilles','1182-5','Coquilles', '0',1970);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','costanoan','1184-1','Costanoan', '0',1980);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','council','1856-4','Council', '0',1990);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','coushatta','1186-6','Coushatta', '0',2000);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cow_creek_umpqua','1668-3','Cow Creek Umpqua', '0',2010);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cowlitz','1189-0','Cowlitz', '0',2020);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','craig','1818-4','Craig', '0',2030);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cree','1191-6','Cree', '0',2040);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','creek','1193-2','Creek', '0',2050);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','croatan','1207-0','Croatan', '0',2060);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','crooked_creek','1912-5','Crooked Creek', '0',2070);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','crow','1209-6','Crow', '0',2080);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','crow_creek_sioux','1613-9','Crow Creek Sioux', '0',2090);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cupeno','1211-2','Cupeno', '0',2100);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','cuyapaipe','1225-2','Cuyapaipe', '0',2110);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','dakota_sioux','1614-7','Dakota Sioux', '0',2120);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','deering','1857-2','Deering', '0',2130);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','delaware','1214-6','Delaware', '0',2140);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','diegueno','1222-9','Diegueno', '0',2150);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','digger','1057-9','Digger', '0',2160);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','dillingham','1913-3','Dillingham', '0',2170);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','dominica_islander','2070-1','Dominica Islander', '0',2180);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','dominican','2069-3','Dominican', '0',2190);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','dot_lake','1758-2','Dot Lake', '0',2200);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','douglas','1819-2','Douglas', '0',2210);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','doyon','1759-0','Doyon', '0',2220);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','dresslerville','1690-7','Dresslerville', '0',2230);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','dry_creek','1466-2','Dry Creek', '0',2240);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','duck_valley','1603-0','Duck Valley', '0',2250);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','duckwater','1588-3','Duckwater', '0',2260);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','duwamish','1519-8','Duwamish', '0',2270);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eagle','1760-8','Eagle', '0',2280);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eastern_cherokee','1092-6','Eastern Cherokee', '0',2290);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eastern_chickahominy','1109-8','Eastern Chickahominy', '0',2300);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eastern_creek','1196-5','Eastern Creek', '0',2310);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eastern_delaware','1215-3','Eastern Delaware', '0',2320);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eastern_muscogee','1197-3','Eastern Muscogee', '0',2330);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eastern_pomo','1467-0','Eastern Pomo', '0',2340);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eastern_shawnee','1580-0','Eastern Shawnee', '0',2350);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eastern_tribes','1233-6','Eastern Tribes', '0',2360);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','echota_cherokee','1093-4','Echota Cherokee', '0',2370);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eek','1914-1','Eek', '0',2380);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','egegik','1975-2','Egegik', '0',2390);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','egyptian','2120-4','Egyptian', '0',2400);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eklutna','1761-6','Eklutna', '0',2410);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ekuk','1915-8','Ekuk', '0',2420);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ekwok','1916-6','Ekwok', '0',2430);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','elim','1858-0','Elim', '0',2440);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','elko','1589-1','Elko', '0',2450);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ely','1590-9','Ely', '0',2460);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','emmonak','1917-4','Emmonak', '0',2470);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','english','2110-5','English', '0',2480);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','english_bay','1987-7','English Bay', '0',2490);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eskimo','1840-8','Eskimo', '0',2500);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','esselen','1250-0','Esselen', '0',2510);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ethiopian','2062-8','Ethiopian', '0',2520);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','etowah_cherokee','1094-2','Etowah Cherokee', '0',2530);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','european','2108-9','European', '0',2540);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','evansville','1762-4','Evansville', '0',2550);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','eyak','1990-1','Eyak', '0',2560);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fallon','1604-8','Fallon', '0',2570);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','false_pass','2015-6','False Pass', '0',2580);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fijian','2101-4','Fijian', '0',2590);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','filipino','2036-2','Filipino', '0',2600);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','flandreau_santee','1615-4','Flandreau Santee', '0',2610);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','florida_seminole','1569-3','Florida Seminole', '0',2620);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fond_du_lac','1128-8','Fond du Lac', '0',2630);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','forest_county','1480-3','Forest County', '0',2640);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fort_belknap','1252-6','Fort Belknap', '0',2650);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fort_berthold','1254-2','Fort Berthold', '0',2660);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fort_bidwell','1421-7','Fort Bidwell', '0',2670);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fort_hall','1258-3','Fort Hall', '0',2680);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fort_independence','1422-5','Fort Independence', '0',2690);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fort_mcdermitt','1605-5','Fort McDermitt', '0',2700);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fort_mcdowell','1256-7','Fort Mcdowell', '0',2710);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fort_peck','1616-2','Fort Peck', '0',2720);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fort_peck_assiniboine_sioux','1031-4','Fort Peck Assiniboine Sioux', '0',2730);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fort_sill_apache','1012-4','Fort Sill Apache', '0',2740);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','fort_yukon','1763-2','Fort Yukon', '0',2750);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','french','2111-3','French', '0',2760);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','french_american_indian','1071-0','French American Indian', '0',2770);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','gabrieleno','1260-9','Gabrieleno', '0',2780);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','gakona','1764-0','Gakona', '0',2790);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','galena','1765-7','Galena', '0',2800);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','gambell','1892-9','Gambell', '0',2810);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','gay_head_wampanoag','1680-8','Gay Head Wampanoag', '0',2820);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','georgetown_eastern_tribes','1236-9','Georgetown (Eastern Tribes)', '0',2830);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','georgetown_yupik-eskimo','1962-0','Georgetown (Yupik-Eskimo)', '0',2840);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','german','2112-1','German', '0',2850);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','gila_bend','1655-0','Gila Bend', '0',2860);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','gila_river_pima-maricopa','1457-1','Gila River Pima-Maricopa', '0',2870);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','golovin','1859-8','Golovin', '0',2880);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','goodnews_bay','1918-2','Goodnews Bay', '0',2890);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','goshute','1591-7','Goshute', '0',2900);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','grand_portage','1129-6','Grand Portage', '0',2910);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','grand_ronde','1262-5','Grand Ronde', '0',2920);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','grand_traverse_band','1130-4','Grand Traverse Band of Ottawa/Chippewa', '0',2930);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','grayling','1766-5','Grayling', '0',2940);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','greenland_eskimo','1842-4','Greenland Eskimo', '0',2950);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','gros_ventres','1264-1','Gros Ventres', '0',2960);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','guamanian','2087-5','Guamanian', '0',2970);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','guamanian_or_chamorro','2086-7','Guamanian or Chamorro', '0',2980);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','gulkana','1767-3','Gulkana', '0',2990);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','haida','1820-0','Haida', '0',3000);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','haitian','2071-9','Haitian', '0',3010);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','haliwa','1267-4','Haliwa', '0',3020);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hannahville','1481-1','Hannahville', '0',3030);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','havasupai','1726-9','Havasupai', '0',3040);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','healy_lake','1768-1','Healy Lake', '0',3050);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hidatsa','1269-0','Hidatsa', '0',3060);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hmong','2037-0','Hmong', '0',3070);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ho-chunk','1697-2','Ho-chunk', '0',3080);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hoh','1083-5','Hoh', '0',3090);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hollywood_seminole','1570-1','Hollywood Seminole', '0',3100);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','holy_cross','1769-9','Holy Cross', '0',3110);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hoonah','1821-8','Hoonah', '0',3120);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hoopa','1271-6','Hoopa', '0',3130);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hoopa_extension','1275-7','Hoopa Extension', '0',3140);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hooper_bay','1919-0','Hooper Bay', '0',3150);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hopi','1493-6','Hopi', '0',3160);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','houma','1277-3','Houma', '0',3170);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hualapai','1727-7','Hualapai', '0',3180);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hughes','1770-7','Hughes', '0',3190);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','huron_potawatomi','1482-9','Huron Potawatomi', '0',3200);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','huslia','1771-5','Huslia', '0',3210);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','hydaburg','1822-6','Hydaburg', '0',3220);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','igiugig','1976-0','Igiugig', '0',3230);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','iliamna','1772-3','Iliamna', '0',3240);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','illinois_miami','1359-9','Illinois Miami', '0',3250);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','inaja-cosmit','1279-9','Inaja-Cosmit', '0',3260);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','inalik_diomede','1860-6','Inalik Diomede', '0',3270);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','indian_township','1442-3','Indian Township', '0',3280);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','indiana_miami','1360-7','Indiana Miami', '0',3290);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','indonesian','2038-8','Indonesian', '0',3300);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','inupiaq','1861-4','Inupiaq', '0',3310);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','inupiat_eskimo','1844-0','Inupiat Eskimo', '0',3320);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','iowa','1281-5','Iowa', '0',3330);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','iowa_of_kansas-nebraska','1282-3','Iowa of Kansas-Nebraska', '0',3340);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','iowa_of_oklahoma','1283-1','Iowa of Oklahoma', '0',3350);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','iowa_sac_and_fox','1552-9','Iowa Sac and Fox', '0',3360);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','iqurmuit_russian_mission','1920-8','Iqurmuit (Russian Mission)', '0',3370);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','iranian','2121-2','Iranian', '0',3380);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','iraqi','2122-0','Iraqi', '0',3390);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','irish','2113-9','Irish', '0',3400);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','iroquois','1285-6','Iroquois', '0',3410);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','isleta','1494-4','Isleta', '0',3420);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','israeili','2127-9','Israeili', '0',3430);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','italian','2114-7','Italian', '0',3440);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ivanof_bay','1977-8','Ivanof Bay', '0',3450);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','iwo_jiman','2048-7','Iwo Jiman', '0',3460);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','jamaican','2072-7','Jamaican', '0',3470);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','jamestown','1313-6','Jamestown', '0',3480);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','japanese','2039-6','Japanese', '0',3490);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','jemez','1495-1','Jemez', '0',3500);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','jena_choctaw','1157-7','Jena Choctaw', '0',3510);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','jicarilla_apache','1013-2','Jicarilla Apache', '0',3520);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','juaneno','1297-1','Juaneno', '0',3530);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kaibab','1423-3','Kaibab', '0',3540);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kake','1823-4','Kake', '0',3550);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kaktovik','1862-2','Kaktovik', '0',3560);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kalapuya','1395-3','Kalapuya', '0',3570);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kalispel','1299-7','Kalispel', '0',3580);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kalskag','1921-6','Kalskag', '0',3590);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kaltag','1773-1','Kaltag', '0',3600);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','karluk','1995-0','Karluk', '0',3610);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','karuk','1301-1','Karuk', '0',3620);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kasaan','1824-2','Kasaan', '0',3630);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kashia','1468-8','Kashia', '0',3640);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kasigluk','1922-4','Kasigluk', '0',3650);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kathlamet','1117-1','Kathlamet', '0',3660);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kaw','1303-7','Kaw', '0',3670);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kawaiisu','1058-7','Kawaiisu', '0',3680);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kawerak','1863-0','Kawerak', '0',3690);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kenaitze','1825-9','Kenaitze', '0',3700);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','keres','1496-9','Keres', '0',3710);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kern_river','1059-5','Kern River', '0',3720);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ketchikan','1826-7','Ketchikan', '0',3730);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','keweenaw','1131-2','Keweenaw', '0',3740);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kialegee','1198-1','Kialegee', '0',3750);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kiana','1864-8','Kiana', '0',3760);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kickapoo','1305-2','Kickapoo', '0',3770);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kikiallus','1520-6','Kikiallus', '0',3780);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','king_cove','2014-9','King Cove', '0',3790);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','king_salmon','1978-6','King Salmon', '0',3800);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kiowa','1309-4','Kiowa', '0',3810);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kipnuk','1923-2','Kipnuk', '0',3820);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kiribati','2096-6','Kiribati', '0',3830);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kivalina','1865-5','Kivalina', '0',3840);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','klallam','1312-8','Klallam', '0',3850);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','klamath','1317-7','Klamath', '0',3860);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','klawock','1827-5','Klawock', '0',3870);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kluti_kaah','1774-9','Kluti Kaah', '0',3880);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','knik','1775-6','Knik', '0',3890);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kobuk','1866-3','Kobuk', '0',3900);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kodiak','1996-8','Kodiak', '0',3910);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kokhanok','1979-4','Kokhanok', '0',3920);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','koliganek','1924-0','Koliganek', '0',3930);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kongiganak','1925-7','Kongiganak', '0',3940);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','koniag_aleut','1992-7','Koniag Aleut', '0',3950);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','konkow','1319-3','Konkow', '0',3960);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kootenai','1321-9','Kootenai', '0',3970);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','korean','2040-4','Korean', '0',3980);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kosraean','2093-3','Kosraean', '0',3990);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kotlik','1926-5','Kotlik', '0',4000);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kotzebue','1867-1','Kotzebue', '0',4010);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','koyuk','1868-9','Koyuk', '0',4020);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','koyukuk','1776-4','Koyukuk', '0',4030);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kwethluk','1927-3','Kwethluk', '0',4040);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kwigillingok','1928-1','Kwigillingok', '0',4050);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','kwiguk','1869-7','Kwiguk', '0',4060);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','la_jolla','1332-6','La Jolla', '0',4070);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','la_posta','1226-0','La Posta', '0',4080);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lac_courte_oreilles','1132-0','Lac Courte Oreilles', '0',4090);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lac_du_flambeau','1133-8','Lac du Flambeau', '0',4100);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lac_vieux_desert_chippewa','1134-6','Lac Vieux Desert Chippewa', '0',4110);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','laguna','1497-7','Laguna', '0',4120);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lake_minchumina','1777-2','Lake Minchumina', '0',4130);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lake_superior','1135-3','Lake Superior', '0',4140);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lake_traverse_sioux','1617-0','Lake Traverse Sioux', '0',4150);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','laotian','2041-2','Laotian', '0',4160);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','larsen_bay','1997-6','Larsen Bay', '0',4170);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','las_vegas','1424-1','Las Vegas', '0',4180);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lassik','1323-5','Lassik', '0',4190);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lebanese','2123-8','Lebanese', '0',4200);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','leech_lake','1136-1','Leech Lake', '0',4210);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lenni-lenape','1216-1','Lenni-Lenape', '0',4220);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','levelock','1929-9','Levelock', '0',4230);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','liberian','2063-6','Liberian', '0',4240);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lime','1778-0','Lime', '0',4250);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lipan_apache','1014-0','Lipan Apache', '0',4260);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','little_shell_chippewa','1137-9','Little Shell Chippewa', '0',4270);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lone_pine','1425-8','Lone Pine', '0',4280);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','long_island','1325-0','Long Island', '0',4290);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','los_coyotes','1048-8','Los Coyotes', '0',4300);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lovelock','1426-6','Lovelock', '0',4310);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lower_brule_sioux','1618-8','Lower Brule Sioux', '0',4320);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lower_elwha','1314-4','Lower Elwha', '0',4330);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lower_kalskag','1930-7','Lower Kalskag', '0',4340);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lower_muscogee','1199-9','Lower Muscogee', '0',4350);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lower_sioux','1619-6','Lower Sioux', '0',4360);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lower_skagit','1521-4','Lower Skagit', '0',4370);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','luiseno','1331-8','Luiseno', '0',4380);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lumbee','1340-9','Lumbee', '0',4390);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','lummi','1342-5','Lummi', '0',4400);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','machis_lower_creek_indian','1200-5','Machis Lower Creek Indian', '0',4410);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','madagascar','2052-9','Madagascar', '0',4420);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','maidu','1344-1','Maidu', '0',4430);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','makah','1348-2','Makah', '0',4440);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','malaysian','2042-0','Malaysian', '0',4450);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','maldivian','2049-5','Maldivian', '0',4460);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','malheur_paiute','1427-4','Malheur Paiute', '0',4470);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','maliseet','1350-8','Maliseet', '0',4480);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mandan','1352-4','Mandan', '0',4490);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','manley_hot_springs','1780-6','Manley Hot Springs', '0',4500);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','manokotak','1931-5','Manokotak', '0',4510);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','manzanita','1227-8','Manzanita', '0',4520);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mariana_islander','2089-1','Mariana Islander', '0',4530);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','maricopa','1728-5','Maricopa', '0',4540);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','marshall','1932-3','Marshall', '0',4550);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','marshallese','2090-9','Marshallese', '0',4560);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','marshantucket_pequot','1454-8','Marshantucket Pequot', '0',4570);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','marys_igloo','1889-5',"Mary's Igloo", '0',4580);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mashpee_wampanoag','1681-6','Mashpee Wampanoag', '0',4590);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','matinecock','1326-8','Matinecock', '0',4600);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mattaponi','1354-0','Mattaponi', '0',4610);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mattole','1060-3','Mattole', '0',4620);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mauneluk_inupiat','1870-5','Mauneluk Inupiat', '0',4630);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mcgrath','1779-8','Mcgrath', '0',4640);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mdewakanton_sioux','1620-4','Mdewakanton Sioux', '0',4650);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mekoryuk','1933-1','Mekoryuk', '0',4660);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','melanesian','2100-6','Melanesian', '0',4670);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','menominee','1356-5','Menominee', '0',4680);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mentasta_lake','1781-4','Mentasta Lake', '0',4690);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mesa_grande','1228-6','Mesa Grande', '0',4700);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mescalero_apache','1015-7','Mescalero Apache', '0',4710);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','metlakatla','1838-2','Metlakatla', '0',4720);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mexican_american_indian','1072-8','Mexican American Indian', '0',4730);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','miami','1358-1','Miami', '0',4740);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','miccosukee','1363-1','Miccosukee', '0',4750);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','michigan_ottawa','1413-4','Michigan Ottawa', '0',4760);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','micmac','1365-6','Micmac', '0',4770);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','micronesian','2085-9','Micronesian', '0',4780);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','middle_eastern_north_african','2118-8','Middle Eastern or North African', '0',4790);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mille_lacs','1138-7','Mille Lacs', '0',4800);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','miniconjou','1621-2','Miniconjou', '0',4810);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','minnesota_chippewa','1139-5','Minnesota Chippewa', '0',4820);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','minto','1782-2','Minto', '0',4830);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mission_indians','1368-0','Mission Indians', '0',4840);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mississippi_choctaw','1158-5','Mississippi Choctaw', '0',4850);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','missouri_sac_and_fox','1553-7','Missouri Sac and Fox', '0',4860);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','miwok','1370-6','Miwok', '0',4870);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','moapa','1428-2','Moapa', '0',4880);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','modoc','1372-2','Modoc', '0',4890);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mohave','1729-3','Mohave', '0',4900);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mohawk','1287-2','Mohawk', '0',4910);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mohegan','1374-8','Mohegan', '0',4920);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','molala','1396-1','Molala', '0',4930);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mono','1376-3','Mono', '0',4940);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','montauk','1327-6','Montauk', '0',4950);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','moor','1237-7','Moor', '0',4960);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','morongo','1049-6','Morongo', '0',4970);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mountain_maidu','1345-8','Mountain Maidu', '0',4980);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mountain_village','1934-9','Mountain Village', '0',4990);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','mowa_band_of_choctaw','1159-3','Mowa Band of Choctaw', '0',5000);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','muckleshoot','1522-2','Muckleshoot', '0',5010);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','munsee','1217-9','Munsee', '0',5020);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','naknek','1935-6','Naknek', '0',5030);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nambe','1498-5','Nambe', '0',5040);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','namibian','2064-4','Namibian', '0',5050);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nana_inupiat','1871-3','Nana Inupiat', '0',5060);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nansemond','1238-5','Nansemond', '0',5070);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nanticoke','1378-9','Nanticoke', '0',5080);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','napakiak','1937-2','Napakiak', '0',5090);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','napaskiak','1938-0','Napaskiak', '0',5100);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','napaumute','1936-4','Napaumute', '0',5110);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','narragansett','1380-5','Narragansett', '0',5120);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','natchez','1239-3','Natchez', '0',5130);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','native_hawaiian','2079-2','Native Hawaiian', '0',5140);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nausu_waiwash','1240-1','Nausu Waiwash', '0',5160);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','navajo','1382-1','Navajo', '0',5170);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nebraska_ponca','1475-3','Nebraska Ponca', '0',5180);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nebraska_winnebago','1698-0','Nebraska Winnebago', '0',5190);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nelson_lagoon','2016-4','Nelson Lagoon', '0',5200);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nenana','1783-0','Nenana', '0',5210);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nepalese','2050-3','Nepalese', '0',5220);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','new_hebrides','2104-8','New Hebrides', '0',5230);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','new_stuyahok','1940-6','New Stuyahok', '0',5240);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','newhalen','1939-8','Newhalen', '0',5250);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','newtok','1941-4','Newtok', '0',5260);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nez_perce','1387-0','Nez Perce', '0',5270);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nigerian','2065-1','Nigerian', '0',5280);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nightmute','1942-2','Nightmute', '0',5290);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nikolai','1784-8','Nikolai', '0',5300);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nikolski','2017-2','Nikolski', '0',5310);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ninilchik','1785-5','Ninilchik', '0',5320);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nipmuc','1241-9','Nipmuc', '0',5330);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nishinam','1346-6','Nishinam', '0',5340);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nisqually','1523-0','Nisqually', '0',5350);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','noatak','1872-1','Noatak', '0',5360);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nomalaki','1389-6','Nomalaki', '0',5370);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nome','1873-9','Nome', '0',5380);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nondalton','1786-3','Nondalton', '0',5390);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nooksack','1524-8','Nooksack', '0',5400);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','noorvik','1874-7','Noorvik', '0',5410);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','northern_arapaho','1022-3','Northern Arapaho', '0',5420);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','northern_cherokee','1095-9','Northern Cherokee', '0',5430);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','northern_cheyenne','1103-1','Northern Cheyenne', '0',5440);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','northern_paiute','1429-0','Northern Paiute', '0',5450);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','northern_pomo','1469-6','Northern Pomo', '0',5460);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','northway','1787-1','Northway', '0',5470);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','northwest_tribes','1391-2','Northwest Tribes', '0',5480);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nuiqsut','1875-4','Nuiqsut', '0',5490);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nulato','1788-9','Nulato', '0',5500);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','nunapitchukv','1943-0','Nunapitchukv', '0',5510);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oglala_sioux','1622-0','Oglala Sioux', '0',5520);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','okinawan','2043-8','Okinawan', '0',5530);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_apache','1016-5','Oklahoma Apache', '0',5540);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_cado','1042-1','Oklahoma Cado', '0',5550);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_choctaw','1160-1','Oklahoma Choctaw', '0',5560);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_comanche','1176-7','Oklahoma Comanche', '0',5570);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_delaware','1218-7','Oklahoma Delaware', '0',5580);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_kickapoo','1306-0','Oklahoma Kickapoo', '0',5590);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_kiowa','1310-2','Oklahoma Kiowa', '0',5600);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_miami','1361-5','Oklahoma Miami', '0',5610);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_ottawa','1414-2','Oklahoma Ottawa', '0',5620);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_pawnee','1446-4','Oklahoma Pawnee', '0',5630);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_peoria','1451-4','Oklahoma Peoria', '0',5640);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_ponca','1476-1','Oklahoma Ponca', '0',5650);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_sac_and_fox','1554-5','Oklahoma Sac and Fox', '0',5660);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oklahoma_seminole','1571-9','Oklahoma Seminole', '0',5670);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','old_harbor','1998-4','Old Harbor', '0',5680);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','omaha','1403-5','Omaha', '0',5690);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oneida','1288-0','Oneida', '0',5700);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','onondaga','1289-8','Onondaga', '0',5710);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ontonagon','1140-3','Ontonagon', '0',5720);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oregon_athabaskan','1405-0','Oregon Athabaskan', '0',5730);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','osage','1407-6','Osage', '0',5740);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','oscarville','1944-8','Oscarville', '0',5750);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','other_pacific_islander','2500-7','Other Pacific Islander', '0',5760);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','other_race','2131-1','Other Race', '0',5770);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','otoe-missouria','1409-2','Otoe-Missouria', '0',5780);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ottawa','1411-8','Ottawa', '0',5790);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ouzinkie','1999-2','Ouzinkie', '0',5800);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','owens_valley','1430-8','Owens Valley', '0',5810);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','paiute','1416-7','Paiute', '0',5820);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pakistani','2044-6','Pakistani', '0',5830);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pala','1333-4','Pala', '0',5840);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','palauan','2091-7','Palauan', '0',5850);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','palestinian','2124-6','Palestinian', '0',5860);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pamunkey','1439-9','Pamunkey', '0',5870);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','panamint','1592-5','Panamint', '0',5880);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','papua_new_guinean','2102-2','Papua New Guinean', '0',5890);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pascua_yaqui','1713-7','Pascua Yaqui', '0',5900);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','passamaquoddy','1441-5','Passamaquoddy', '0',5910);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','paugussett','1242-7','Paugussett', '0',5920);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pauloff_harbor','2018-0','Pauloff Harbor', '0',5930);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pauma','1334-2','Pauma', '0',5940);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pawnee','1445-6','Pawnee', '0',5950);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','payson_apache','1017-3','Payson Apache', '0',5960);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pechanga','1335-9','Pechanga', '0',5970);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pedro_bay','1789-7','Pedro Bay', '0',5980);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pelican','1828-3','Pelican', '0',5990);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','penobscot','1448-0','Penobscot', '0',6000);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','peoria','1450-6','Peoria', '0',6010);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pequot','1453-0','Pequot', '0',6020);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','perryville','1980-2','Perryville', '0',6030);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','petersburg','1829-1','Petersburg', '0',6040);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','picuris','1499-3','Picuris', '0',6050);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pilot_point','1981-0','Pilot Point', '0',6060);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pilot_station','1945-5','Pilot Station', '0',6070);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pima','1456-3','Pima', '0',6080);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pine_ridge_sioux','1623-8','Pine Ridge Sioux', '0',6090);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pipestone_sioux','1624-6','Pipestone Sioux', '0',6100);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','piro','1500-8','Piro', '0',6110);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','piscataway','1460-5','Piscataway', '0',6120);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pit_river','1462-1','Pit River', '0',6130);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pitkas_point','1946-3','Pitkas Point', '0',6140);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','platinum','1947-1','Platinum', '0',6150);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pleasant_point_passamaquoddy','1443-1','Pleasant Point Passamaquoddy', '0',6160);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','poarch_band','1201-3','Poarch Band', '0',6170);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pocomoke_acohonock','1243-5','Pocomoke Acohonock', '0',6180);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pohnpeian','2094-1','Pohnpeian', '0',6190);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','point_hope','1876-2','Point Hope', '0',6200);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','point_lay','1877-0','Point Lay', '0',6210);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pojoaque','1501-6','Pojoaque', '0',6220);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pokagon_potawatomi','1483-7','Pokagon Potawatomi', '0',6230);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','polish','2115-4','Polish', '0',6240);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','polynesian','2078-4','Polynesian', '0',6250);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pomo','1464-7','Pomo', '0',6260);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ponca','1474-6','Ponca', '0',6270);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','poospatuck','1328-4','Poospatuck', '0',6280);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','port_gamble_klallam','1315-1','Port Gamble Klallam', '0',6290);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','port_graham','1988-5','Port Graham', '0',6300);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','port_heiden','1982-8','Port Heiden', '0',6310);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','port_lions','2000-8','Port Lions', '0',6320);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','port_madison','1525-5','Port Madison', '0',6330);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','portage_creek','1948-9','Portage Creek', '0',6340);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','potawatomi','1478-7','Potawatomi', '0',6350);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','powhatan','1487-8','Powhatan', '0',6360);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','prairie_band','1484-5','Prairie Band', '0',6370);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','prairie_island_sioux','1625-3','Prairie Island Sioux', '0',6380);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','principal_creek_indian_nation','1202-1','Principal Creek Indian Nation', '0',6390);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','prior_lake_sioux','1626-1','Prior Lake Sioux', '0',6400);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pueblo','1489-4','Pueblo', '0',6410);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','puget_sound_salish','1518-0','Puget Sound Salish', '0',6420);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','puyallup','1526-3','Puyallup', '0',6430);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','pyramid_lake','1431-6','Pyramid Lake', '0',6440);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','qagan_toyagungin','2019-8','Qagan Toyagungin', '0',6450);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','qawalangin','2020-6','Qawalangin', '0',6460);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','quapaw','1541-2','Quapaw', '0',6470);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','quechan','1730-1','Quechan', '0',6480);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','quileute','1084-3','Quileute', '0',6490);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','quinault','1543-8','Quinault', '0',6500);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','quinhagak','1949-7','Quinhagak', '0',6510);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ramah_navajo','1385-4','Ramah Navajo', '0',6520);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','rampart','1790-5','Rampart', '0',6530);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','rampough_mountain','1219-5','Rampough Mountain', '0',6540);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','rappahannock','1545-3','Rappahannock', '0',6550);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','red_cliff_chippewa','1141-1','Red Cliff Chippewa', '0',6560);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','red_devil','1950-5','Red Devil', '0',6570);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','red_lake_chippewa','1142-9','Red Lake Chippewa', '0',6580);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','red_wood','1061-1','Red Wood', '0',6590);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','reno-sparks','1547-9','Reno-Sparks', '0',6600);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','rocky_boys_chippewa_cree','1151-0',"Rocky Boy's Chippewa Cree", '0',6610);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','rosebud_sioux','1627-9','Rosebud Sioux', '0',6620);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','round_valley','1549-5','Round Valley', '0',6630);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ruby','1791-3','Ruby', '0',6640);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ruby_valley','1593-3','Ruby Valley', '0',6650);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sac_and_fox','1551-1','Sac and Fox', '0',6660);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','saginaw_chippewa','1143-7','Saginaw Chippewa', '0',6670);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','saipanese','2095-8','Saipanese', '0',6680);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','salamatof','1792-1','Salamatof', '0',6690);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','salinan','1556-0','Salinan', '0',6700);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','salish','1558-6','Salish', '0',6710);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','salish_and_kootenai','1560-2','Salish and Kootenai', '0',6720);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','salt_river_pima-maricopa','1458-9','Salt River Pima-Maricopa', '0',6730);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','samish','1527-1','Samish', '0',6740);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','samoan','2080-0','Samoan', '0',6750);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','san_carlos_apache','1018-1','San Carlos Apache', '0',6760);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','san_felipe','1502-4','San Felipe', '0',6770);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','san_ildefonso','1503-2','San Ildefonso', '0',6780);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','san_juan','1506-5','San Juan', '0',6790);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','san_juan_de','1505-7','San Juan De', '0',6800);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','san_juan_pueblo','1504-0','San Juan Pueblo', '0',6810);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','san_juan_southern_paiute','1432-4','San Juan Southern Paiute', '0',6820);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','san_manual','1574-3','San Manual', '0',6830);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','san_pasqual','1229-4','San Pasqual', '0',6840);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','san_xavier','1656-8','San Xavier', '0',6850);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sand_hill','1220-3','Sand Hill', '0',6860);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sand_point','2023-0','Sand Point', '0',6870);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sandia','1507-3','Sandia', '0',6880);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sans_arc_sioux','1628-7','Sans Arc Sioux', '0',6890);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','santa_ana','1508-1','Santa Ana', '0',6900);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','santa_clara','1509-9','Santa Clara', '0',6910);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','santa_rosa','1062-9','Santa Rosa', '0',6920);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','santa_rosa_cahuilla','1050-4','Santa Rosa Cahuilla', '0',6930);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','santa_ynez','1163-5','Santa Ynez', '0',6940);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','santa_ysabel','1230-2','Santa Ysabel', '0',6950);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','santee_sioux','1629-5','Santee Sioux', '0',6960);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','santo_domingo','1510-7','Santo Domingo', '0',6970);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sauk-suiattle','1528-9','Sauk-Suiattle', '0',6980);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sault_ste_marie_chippewa','1145-2','Sault Ste. Marie Chippewa', '0',6990);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','savoonga','1893-7','Savoonga', '0',7000);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','saxman','1830-9','Saxman', '0',7010);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','scammon_bay','1952-1','Scammon Bay', '0',7020);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','schaghticoke','1562-8','Schaghticoke', '0',7030);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','scott_valley','1564-4','Scott Valley', '0',7040);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','scottish','2116-2','Scottish', '0',7050);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','scotts_valley','1470-4','Scotts Valley', '0',7060);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','selawik','1878-8','Selawik', '0',7070);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','seldovia','1793-9','Seldovia', '0',7080);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sells','1657-6','Sells', '0',7090);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','seminole','1566-9','Seminole', '0',7100);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','seneca','1290-6','Seneca', '0',7110);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','seneca_nation','1291-4','Seneca Nation', '0',7120);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','seneca-cayuga','1292-2','Seneca-Cayuga', '0',7130);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','serrano','1573-5','Serrano', '0',7140);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','setauket','1329-2','Setauket', '0',7150);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','shageluk','1795-4','Shageluk', '0',7160);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','shaktoolik','1879-6','Shaktoolik', '0',7170);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','shasta','1576-8','Shasta', '0',7180);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','shawnee','1578-4','Shawnee', '0',7190);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sheldons_point','1953-9',"Sheldon's Point", '0',7200);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','shinnecock','1582-6','Shinnecock', '0',7210);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','shishmaref','1880-4','Shishmaref', '0',7220);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','shoalwater_bay','1584-2','Shoalwater Bay', '0',7230);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','shoshone','1586-7','Shoshone', '0',7240);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','shoshone_paiute','1602-2','Shoshone Paiute', '0',7250);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','shungnak','1881-2','Shungnak', '0',7260);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','siberian_eskimo','1891-1','Siberian Eskimo', '0',7270);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','siberian_yupik','1894-5','Siberian Yupik', '0',7280);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','siletz','1607-1','Siletz', '0',7290);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','singaporean','2051-1','Singaporean', '0',7300);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sioux','1609-7','Sioux', '0',7310);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sisseton_sioux','1631-1','Sisseton Sioux', '0',7320);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sisseton-wahpeton','1630-3','Sisseton-Wahpeton', '0',7330);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sitka','1831-7','Sitka', '0',7340);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','siuslaw','1643-6','Siuslaw', '0',7350);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','skokomish','1529-7','Skokomish', '0',7360);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','skull_valley','1594-1','Skull Valley', '0',7370);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','skykomish','1530-5','Skykomish', '0',7380);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','slana','1794-7','Slana', '0',7390);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sleetmute','1954-7','Sleetmute', '0',7400);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','snohomish','1531-3','Snohomish', '0',7410);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','snoqualmie','1532-1','Snoqualmie', '0',7420);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','soboba','1336-7','Soboba', '0',7430);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sokoagon_chippewa','1146-0','Sokoagon Chippewa', '0',7440);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','solomon','1882-0','Solomon', '0',7450);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','solomon_islander','2103-0','Solomon Islander', '0',7460);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','south_american_indian','1073-6','South American Indian', '0',7470);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','south_fork_shoshone','1595-8','South Fork Shoshone', '0',7480);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','south_naknek','2024-8','South Naknek', '0',7490);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','southeast_alaska','1811-9','Southeast Alaska', '0',7500);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','southeastern_indians','1244-3','Southeastern Indians', '0',7510);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','southern_arapaho','1023-1','Southern Arapaho', '0',7520);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','southern_cheyenne','1104-9','Southern Cheyenne', '0',7530);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','southern_paiute','1433-2','Southern Paiute', '0',7540);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','spanish_american_indian','1074-4','Spanish American Indian', '0',7550);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','spirit_lake_sioux','1632-9','Spirit Lake Sioux', '0',7560);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','spokane','1645-1','Spokane', '0',7570);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','squaxin_island','1533-9','Squaxin Island', '0',7580);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sri_lankan','2045-3','Sri Lankan', '0',7590);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','st_croix_chippewa','1144-5','St. Croix Chippewa', '0',7600);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','st_george','2021-4','St. George', '0',7610);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','st_marys','1963-8',"St. Mary's", '0',7620);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','st_michael','1951-3','St. Michael', '0',7630);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','st_paul','2022-2','St. Paul', '0',7640);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','standing_rock_sioux','1633-7','Standing Rock Sioux', '0',7650);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','star_clan_of_muscogee_creeks','1203-9','Star Clan of Muscogee Creeks', '0',7660);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','stebbins','1955-4','Stebbins', '0',7670);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','steilacoom','1534-7','Steilacoom', '0',7680);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','stevens','1796-2','Stevens', '0',7690);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','stewart','1647-7','Stewart', '0',7700);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','stillaguamish','1535-4','Stillaguamish', '0',7710);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','stockbridge','1649-3','Stockbridge', '0',7720);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','stony_river','1797-0','Stony River', '0',7730);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','stonyford','1471-2','Stonyford', '0',7740);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sugpiaq','2002-4','Sugpiaq', '0',7750);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sulphur_bank','1472-0','Sulphur Bank', '0',7760);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','summit_lake','1434-0','Summit Lake', '0',7770);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','suqpigaq','2004-0','Suqpigaq', '0',7780);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','suquamish','1536-2','Suquamish', '0',7790);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','susanville','1651-9','Susanville', '0',7800);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','susquehanock','1245-0','Susquehanock', '0',7810);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','swinomish','1537-0','Swinomish', '0',7820);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','sycuan','1231-0','Sycuan', '0',7830);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','syrian','2125-3','Syrian', '0',7840);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','table_bluff','1705-3','Table Bluff', '0',7850);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tachi','1719-4','Tachi', '0',7860);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tahitian','2081-8','Tahitian', '0',7870);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','taiwanese','2035-4','Taiwanese', '0',7880);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','takelma','1063-7','Takelma', '0',7890);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','takotna','1798-8','Takotna', '0',7900);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','talakamish','1397-9','Talakamish', '0',7910);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tanacross','1799-6','Tanacross', '0',7920);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tanaina','1800-2','Tanaina', '0',7930);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tanana','1801-0','Tanana', '0',7940);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tanana_chiefs','1802-8','Tanana Chiefs', '0',7950);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','taos','1511-5','Taos', '0',7960);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tatitlek','1969-5','Tatitlek', '0',7970);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tazlina','1803-6','Tazlina', '0',7980);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','telida','1804-4','Telida', '0',7990);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','teller','1883-8','Teller', '0',8000);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','temecula','1338-3','Temecula', '0',8010);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','te-moak_western_shoshone','1596-6','Te-Moak Western Shoshone', '0',8020);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tenakee_springs','1832-5','Tenakee Springs', '0',8030);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tenino','1398-7','Tenino', '0',8040);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tesuque','1512-3','Tesuque', '0',8050);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tetlin','1805-1','Tetlin', '0',8060);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','teton_sioux','1634-5','Teton Sioux', '0',8070);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tewa','1513-1','Tewa', '0',8080);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','texas_kickapoo','1307-8','Texas Kickapoo', '0',8090);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','thai','2046-1','Thai', '0',8100);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','thlopthlocco','1204-7','Thlopthlocco', '0',8110);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tigua','1514-9','Tigua', '0',8120);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tillamook','1399-5','Tillamook', '0',8130);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','timbi-sha_shoshone','1597-4','Timbi-Sha Shoshone', '0',8140);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tlingit','1833-3','Tlingit', '0',8150);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tlingit-haida','1813-5','Tlingit-Haida', '0',8160);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tobagoan','2073-5','Tobagoan', '0',8170);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','togiak','1956-2','Togiak', '0',8180);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tohono_oodham','1653-5',"Tohono O'Odham", '0',8190);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tok','1806-9','Tok', '0',8200);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tokelauan','2083-4','Tokelauan', '0',8210);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','toksook','1957-0','Toksook', '0',8220);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tolowa','1659-2','Tolowa', '0',8230);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tonawanda_seneca','1293-0','Tonawanda Seneca', '0',8240);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tongan','2082-6','Tongan', '0',8250);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tonkawa','1661-8','Tonkawa', '0',8260);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','torres-martinez','1051-2','Torres-Martinez', '0',8270);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','trinidadian','2074-3','Trinidadian', '0',8280);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','trinity','1272-4','Trinity', '0',8290);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tsimshian','1837-4','Tsimshian', '0',8300);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tuckabachee','1205-4','Tuckabachee', '0',8310);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tulalip','1538-8','Tulalip', '0',8320);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tule_river','1720-2','Tule River', '0',8330);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tulukskak','1958-8','Tulukskak', '0',8340);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tunica_biloxi','1246-8','Tunica Biloxi', '0',8350);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tuntutuliak','1959-6','Tuntutuliak', '0',8360);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tununak','1960-4','Tununak', '0',8370);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','turtle_mountain','1147-8','Turtle Mountain', '0',8380);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tuscarora','1294-8','Tuscarora', '0',8390);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tuscola','1096-7','Tuscola', '0',8400);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','twenty-nine_palms','1337-5','Twenty-Nine Palms', '0',8410);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','twin_hills','1961-2','Twin Hills', '0',8420);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','two_kettle_sioux','1635-2','Two Kettle Sioux', '0',8430);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tygh','1663-4','Tygh', '0',8440);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','tyonek','1807-7','Tyonek', '0',8450);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ugashik','1970-3','Ugashik', '0',8460);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','uintah_ute','1672-5','Uintah Ute', '0',8470);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','umatilla','1665-9','Umatilla', '0',8480);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','umkumiate','1964-6','Umkumiate', '0',8490);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','umpqua','1667-5','Umpqua', '0',8500);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','unalakleet','1884-6','Unalakleet', '0',8510);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','unalaska','2025-5','Unalaska', '0',8520);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','unangan_aleut','2006-5','Unangan Aleut', '0',8530);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','unga','2026-3','Unga', '0',8540);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','united_ketowah_band_of_cheroke','1097-5','United Keetowah Band of Cherokee', '0',8550);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','upper_chinook','1118-9','Upper Chinook', '0',8560);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','upper_sioux','1636-0','Upper Sioux', '0',8570);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','upper_skagit','1539-6','Upper Skagit', '0',8580);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ute','1670-9','Ute', '0',8590);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','ute_mountain_ute','1673-3','Ute Mountain Ute', '0',8600);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','utu_utu_gwaitu_paiute','1435-7','Utu Utu Gwaitu Paiute', '0',8610);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','venetie','1808-5','Venetie', '0',8620);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','vietnamese','2047-9','Vietnamese', '0',8630);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','waccamaw-siousan','1247-6','Waccamaw-Siousan', '0',8640);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wahpekute_sioux','1637-8','Wahpekute Sioux', '0',8650);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wahpeton_sioux','1638-6','Wahpeton Sioux', '0',8660);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wailaki','1675-8','Wailaki', '0',8670);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wainwright','1885-3','Wainwright', '0',8680);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wakiakum_chinook','1119-7','Wakiakum Chinook', '0',8690);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wales','1886-1','Wales', '0',8700);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','walker_river','1436-5','Walker River', '0',8710);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','walla-walla','1677-4','Walla-Walla', '0',8720);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wampanoag','1679-0','Wampanoag', '0',8730);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wappo','1064-5','Wappo', '0',8740);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','warm_springs','1683-2','Warm Springs', '0',8750);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wascopum','1685-7','Wascopum', '0',8760);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','washakie','1598-2','Washakie', '0',8770);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','washoe','1687-3','Washoe', '0',8780);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wazhaza_sioux','1639-4','Wazhaza Sioux', '0',8790);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wenatchee','1400-1','Wenatchee', '0',8800);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','west_indian','2075-0','West Indian', '0',8810);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','western_cherokee','1098-3','Western Cherokee', '0',8820);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','western_chickahominy','1110-6','Western Chickahominy', '0',8830);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','whilkut','1273-2','Whilkut', '0',8840);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','white_earth','1148-6','White Earth', '0',8860);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','white_mountain','1887-9','White Mountain', '0',8870);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','white_mountain_apache','1019-9','White Mountain Apache', '0',8880);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','white_mountain_inupiat','1888-7','White Mountain Inupiat', '0',8890);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wichita','1692-3','Wichita', '0',8900);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wicomico','1248-4','Wicomico', '0',8910);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','willapa_chinook','1120-5','Willapa Chinook', '0',8920);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wind_river','1694-9','Wind River', '0',8930);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wind_river_arapaho','1024-9','Wind River Arapaho', '0',8940);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wind_river_shoshone','1599-0','Wind River Shoshone', '0',8950);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','winnebago','1696-4','Winnebago', '0',8960);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','winnemucca','1700-4','Winnemucca', '0',8970);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wintun','1702-0','Wintun', '0',8980);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wisconsin_potawatomi','1485-2','Wisconsin Potawatomi', '0',8990);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wiseman','1809-3','Wiseman', '0',9000);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wishram','1121-3','Wishram', '0',9010);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wiyot','1704-6','Wiyot', '0',9020);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wrangell','1834-1','Wrangell', '0',9030);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','wyandotte','1295-5','Wyandotte', '0',9040);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yahooskin','1401-9','Yahooskin', '0',9050);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yakama','1707-9','Yakama', '0',9060);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yakama_cowlitz','1709-5','Yakama Cowlitz', '0',9070);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yakutat','1835-8','Yakutat', '0',9080);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yana','1065-2','Yana', '0',9090);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yankton_sioux','1640-2','Yankton Sioux', '0',9100);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yanktonai_sioux','1641-0','Yanktonai Sioux', '0',9110);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yapese','2098-2','Yapese', '0',9120);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yaqui','1711-1','Yaqui', '0',9130);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yavapai','1731-9','Yavapai', '0',9140);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yavapai_apache','1715-2','Yavapai Apache', '0',9150);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yerington_paiute','1437-3','Yerington Paiute', '0',9160);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yokuts','1717-8','Yokuts', '0',9170);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yomba','1600-6','Yomba', '0',9180);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yuchi','1722-8','Yuchi', '0',9190);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yuki','1066-0','Yuki', '0',9200);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yuman','1724-4','Yuman', '0',9210);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yupik_eskimo','1896-0','Yupik Eskimo', '0',9220);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','yurok','1732-7','Yurok', '0',9230);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','zairean','2066-9','Zairean', '0',9240);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','zia','1515-6','Zia', '0',9250);
INSERT INTO list_options (list_id, option_id, notes, title, activity, seq) VALUES ('race','zuni','1516-4','Zuni', '0',9260);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'ethnicity','Ethnicity', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, option_value ) VALUES ('ethnicity', 'declne_to_specfy', 'Declined To Specify', 0, 0, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('ethnicity', 'hisp_or_latin', 'Hispanic or Latino', 10, 0, '2135-2');
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('ethnicity', 'not_hisp_or_latin', 'Not Hispanic or Latino', 10, 0, '2186-5');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_date','Payment Date', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_date', 'date_val', 'Date', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_date', 'post_to_date', 'Post To Date', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_date', 'deposit_date', 'Deposit Date', 30, 0);

INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`) VALUES ('lists', 'page_validation', 'Page Validation', 298);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `notes`, `activity`) VALUES ('page_validation', 'add_edit_issue#theform', '/interface/patient_file/summary/add_edit_issue.php', 10, '{form_title:{presence: true}}', 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `notes`, `activity`) VALUES ('page_validation', 'common#new_encounter', '/interface/forms/newpatient/common.php', 50, '{pc_catid:{exclusion: ["_blank"]}}', 1);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `notes`, `activity`) VALUES ('page_validation', 'add_edit_event#theform', '/interface/main/calendar/add_edit_event.php', 30, '{form_patient:{presence: {message: "Patient Name Required"}}}', 1);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `notes`, `activity`) VALUES ('page_validation', 'usergroup_admin_add#new_user', '/interface/usergroup/usergroup_admin_add.php', 70, '{rumple:{presence: {message:"Required field missing: Please enter the User Name"}}, stiltskin:{presence: {message:"Please enter the password"}}, fname:{presence: {message:"Required field missing: Please enter the First name"}}, lname:{presence: {message:"Required field missing: Please enter the Last name"}}}', 1);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `notes`, `activity`) VALUES ('page_validation', 'user_admin#user_form', '/interface/usergroup/user_admin.php', 80, '{fname:{presence: {message:"Required field missing: Please enter the First name"}}, lname:{presence: {message:"Required field missing: Please enter the Last name"}}}', 1);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `notes`, `activity`) VALUES ('page_validation', 'facility_admin#facility-form', '/interface/usergroup/facility_admin.php', 90, '{facility:{presence: true}, ncolor:{presence: true}}', 1);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `notes`, `activity`) VALUES ('page_validation', 'facilities_add#facility-add', '/interface/usergroup/facilities_add.php', 100, '{facility:{presence: true}, ncolor:{presence: true}}', 1);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `notes`, `activity`) VALUES ('page_validation', 'addrbook_edit#theform', '/interface/usergroup/addrbook_edit.php', 110, '{}', 1);

-- list_options for `form_eye_mag`
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'    ,'CTLManufacturer', 'Eye Contact Lens Manufacturer list', 1, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLManufacturer', 'BNL', 'Bausch&Lomb', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLManufacturer', 'CibaVision', 'Ciba Vision', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLManufacturer', 'Cooper', 'CooperVision', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLManufacturer', 'JNJ', 'Johnson&Johnson', 40, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'    ,'CTLSupplier', 'Eye Contact Lens Supplier list', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLSupplier', 'ABB', 'ABB Optical', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLSupplier', 'JNJ', 'Johnson&Johnson', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLSupplier', 'LF', 'Lens Ferry', 30, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'CTLBrand', 'Eye Contact Lens Brand list', 1, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLBrand', 'Acuvue', 'Acuvue', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLBrand', 'Acuvue2', 'Acuvue 2', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLBrand', 'AcuvueOa', 'Acuvue Oasys', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLBrand', 'SF66', 'SofLens Toric', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('CTLBrand', 'PVMF', 'PureVision MultiFocal', 50, 0);

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'Eye_Coding_Fields', 'Eye Coding Fields', 1, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`, `activity`, `subtype`) VALUES
('Eye_Coding_Fields', 'RUL', 'RUL', 10, 0, 0, '', 'right upper eyelid', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'RLL', 'RLL', 20, 0, 0, '', 'right lower eyelid', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'LUL', 'LUL', 30, 0, 0, '', 'left upper eyelid', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'LLL', 'LLL', 40, 0, 0, '', 'left lower eyelid', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'RBROW', 'RBROW', 50, 0, 0, '', 'forehead', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'LBROW', 'LBROW', 60, 0, 0, '', 'forehead', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'RMCT', 'RMCT', 70, 0, 0, '', 'canthus', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'LMCT', 'LMCT', 80, 0, 0, '', 'canthus', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'RBROW_unspec', 'RBROW', 90, 0, 0, '', 'unspecified', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'LBROW_unspec', 'LBROW', 100, 0, 0, '', 'unspecified', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'RADNEXA', 'RADNEXA', 110, 0, 0, '', 'unspecified', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'LADNEXA', 'LADNEXA', 120, 0, 0, '', 'unspecified', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'ODCONJ', 'ODCONJ', 130, 0, 0, '', 'right conjunctiva', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'OSCONJ', 'OSCONJ', 140, 0, 0, '', 'left conjunctiva', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'ODCORNEA', 'ODCORNEA', 150, 0, 0, '', 'right cornea', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'OSCORNEA', 'OSCORNEA', 160, 0, 0, '', 'left cornea', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'ODAC', 'ODAC', 170, 0, 0, '', 'right anterior chamber', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'OSAC', 'OSAC', 180, 0, 0, '', 'left anterior chamber', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'ODLENS', 'ODLENS', 190, 0, 0, '', 'right lens', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'OSLENS', 'OSLENS', 200, 0, 0, '', 'left lens', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'ODIRIS', 'ODIRIS', 210, 0, 0, '', 'right iris', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'OSIRIS', 'OSIRIS', 220, 0, 0, '', 'left iris', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'ODDISC', 'ODDISC', 230, 0, 0, '', 'right', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'OSDISC', 'OSDISC', 240, 0, 0, '', 'left', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'ODMAC', 'ODMACULA', 250, 0, 0, '', 'right macula', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'OSMAC', 'OSMACULA', 260, 0, 0, '', 'left macula', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'ODVESSELS', 'ODVESSELS', 270, 0, 0, '', 'right', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'OSVESSELS', 'OSVESSELS', 280, 0, 0, '', 'left', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'ODPERIPH', 'ODPERIPH', 290, 0, 0, '', 'right', '', 0, 0, 1, ''),
('Eye_Coding_Fields', 'OSPERIPH', 'OSPERIPH', 300, 0, 0, '', 'left', '', 0, 0, 1, '');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'Eye_Coding_Terms', 'Eye Coding Terms', 1, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`, `activity`, `subtype`) VALUES
('Eye_Coding_Terms', 'dermato_RUL', 'dermatochalasis', 10, 0, 0, '', 'RUL', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'dermato_RLL', 'dermatochalasis', 20, 0, 0, '', 'RLL', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'dermato_LUL', 'dermatochalasis', 30, 0, 0, '', 'LUL', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'dermato_LLL', 'dermatochalasis', 40, 0, 0, '', 'LLL', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'ptosis_RUL', 'ptosis', 50, 0, 0, '', 'RUL', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'ptosis_LUL', 'ptosis', 60, 0, 0, '', 'LUL', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'chalazion_RUL', 'chalazion', 70, 0, 0, '', 'RUL', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'chalazion_RLL', 'chalazion', 80, 0, 0, '', 'RLL', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'chalazion_LUL', 'chalazion', 90, 0, 0, '', 'LUL', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'chalazion_LLL', 'chalazion', 100, 0, 0, '', 'LLL', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'cicectr_RUL', 'cicatricial ectropion', 110, 0, 0, '', 'RUL', 'ICD10:H02.111', 0, 0, 1, ''),
('Eye_Coding_Terms', 'cicectr_RLL', 'cicatricial ectropion', 120, 0, 0, '', 'RLL', 'ICD10:H02.112', 0, 0, 1, ''),
('Eye_Coding_Terms', 'cicectr_LUL', 'cicatricial ectropion', 130, 0, 0, '', 'LUL', 'ICD10:H02.114', 0, 0, 1, ''),
('Eye_Coding_Terms', 'cicectr_LLL', 'cicatricial ectropion', 140, 0, 0, '', 'LLL', 'ICD10:H02.115', 0, 0, 1, ''),
('Eye_Coding_Terms', 'spasectr_RUL', 'spastic ectropion', 150, 0, 0, '', 'RUL', 'ICD10:H02.141', 0, 0, 1, ''),
('Eye_Coding_Terms', 'spasentr_RUL', 'spastic entropion', 160, 0, 0, '', 'RUL', 'ICD10:H02.041', 0, 0, 1, ''),
('Eye_Coding_Terms', 'spasectr_RLL', 'spastic ectropion', 170, 0, 0, '', 'RLL', 'ICD10:H02.142', 0, 0, 1, ''),
('Eye_Coding_Terms', 'spasentr_RLL', 'spastic entropion', 180, 0, 0, '', 'RLL', 'ICD10:H02.042', 0, 0, 1, ''),
('Eye_Coding_Terms', 'spasectr_LUL', 'spastic ectropion', 190, 0, 0, '', 'LUL', 'ICD10:H02.144', 0, 0, 1, ''),
('Eye_Coding_Terms', 'spasentr_LUL', 'spastic entropion', 200, 0, 0, '', 'LUL', 'ICD10:H02.044', 0, 0, 1, ''),
('Eye_Coding_Terms', 'spasectr_LLL', 'spastic ectropion', 210, 0, 0, '', 'LLL', 'ICD10:H02.145', 0, 0, 1, ''),
('Eye_Coding_Terms', 'spasentr_LLL', 'spastic entropion', 220, 0, 0, '', 'LLL', 'ICD10:H02.045', 0, 0, 1, ''),
('Eye_Coding_Terms', 'cicentr_RUL', 'cicatricial entropion', 230, 0, 0, '', 'RUL', 'ICD10:H02.111', 0, 0, 1, ''),
('Eye_Coding_Terms', 'cicentr_RLL', 'cicatricial entropion', 240, 0, 0, '', 'RLL', 'ICD10:H02.112', 0, 0, 1, ''),
('Eye_Coding_Terms', 'cicentr_LUL', 'cicatricial entropion', 250, 0, 0, '', 'LUL', 'ICD10:H02.114', 0, 0, 1, ''),
('Eye_Coding_Terms', 'cicentr_LLL', 'cicatricial entropion', 260, 0, 0, '', 'LLL', 'ICD10:H02.115', 0, 0, 1, ''),
('Eye_Coding_Terms', 'ect_RUL', 'ectropion', 270, 0, 0, '', 'RUL', 'ICD10:H02.101', 0, 0, 1, ''),
('Eye_Coding_Terms', 'ect_RLL', 'ectropion', 280, 0, 0, '', 'RLL', 'ICD10:H02.102', 0, 0, 1, ''),
('Eye_Coding_Terms', 'ect_LUL', 'ectropion', 290, 0, 0, '', 'LUL', 'ICD10:H02.104', 0, 0, 1, ''),
('Eye_Coding_Terms', 'ect_LLL', 'ectropion', 300, 0, 0, '', 'LLL', 'ICD10:H02.105', 0, 0, 1, ''),
('Eye_Coding_Terms', 'ent_RUL', 'entropion', 310, 0, 0, '', 'RUL', 'ICD10:H02.001', 0, 0, 1, ''),
('Eye_Coding_Terms', 'ent_RLL', 'entropion', 320, 0, 0, '', 'RLL', 'ICD10:H02.002', 0, 0, 1, ''),
('Eye_Coding_Terms', 'ent_LUL', 'entropion', 330, 0, 0, '', 'LUL', 'ICD10:H02.004', 0, 0, 1, ''),
('Eye_Coding_Terms', 'ent_LLL', 'entropion', 340, 0, 0, '', 'LLL', 'ICD10:H02.005', 0, 0, 1, ''),
('Eye_Coding_Terms', 'trich_RUL', 'trichiasis', 350, 0, 0, '', 'RUL', 'ICD10:H02.051', 0, 0, 1, ''),
('Eye_Coding_Terms', 'trich_RLL', 'trichiasis', 360, 0, 0, '', 'RLL', 'ICD10:H02.052', 0, 0, 1, ''),
('Eye_Coding_Terms', 'trich_LUL', 'trichiasis', 370, 0, 0, '', 'LUL', 'ICD10:H02.054', 0, 0, 1, ''),
('Eye_Coding_Terms', 'trich_LLL', 'trichiasis', 380, 0, 0, '', 'LLL', 'ICD10:H02.055', 0, 0, 1, ''),
('Eye_Coding_Terms', 'stye_RUL', 'stye', 390, 0, 0, '', 'RUL', 'ICD10:H00.011', 0, 0, 1, ''),
('Eye_Coding_Terms', 'stye_RLL', 'stye', 400, 0, 0, '', 'RLL', 'ICD10:H00.012', 0, 0, 1, ''),
('Eye_Coding_Terms', 'stye_LUL', 'stye', 410, 0, 0, '', 'LUL', 'ICD10:H00.014', 0, 0, 1, ''),
('Eye_Coding_Terms', 'stye_LLL', 'stye', 420, 0, 0, '', 'LLL', 'ICD10:H00.015', 0, 0, 1, ''),
('Eye_Coding_Terms', 'papillae_ODCONJ', 'papilla', 430, 0, 0, '', 'ODCONJ', 'ICD10:H10.401', 0, 0, 1, ''),
('Eye_Coding_Terms', 'papillae_OSCONJ', 'papilla', 440, 0, 0, '', 'OSCONJ', 'ICD10:H10.402', 0, 0, 1, ''),
('Eye_Coding_Terms', 'folllicles_ODCONJ', 'folllicles', 450, 0, 0, '', 'ODCONJ', 'ICD10:H10.011', 0, 0, 1, ''),
('Eye_Coding_Terms', 'folllicles_OSCONJ', 'folllicles', 460, 0, 0, '', 'OSCONJ', 'ICD10:H10.012', 0, 0, 1, ''),
('Eye_Coding_Terms', 'pterygium_ODCORNEA', 'pterygium', 470, 0, 0, '', 'ODCORNEA', 'ICD10:H11.051', 0, 0, 1, ''),
('Eye_Coding_Terms', 'pterygium_ODCONJ', 'pterygium', 480, 0, 0, '', 'ODCONJ', 'ICD10:H11.811', 0, 0, 1, ''),
('Eye_Coding_Terms', 'pterygium_OSCONJ', 'pterygium', 490, 0, 0, '', 'OSCONJ', 'ICD10:H11.812', 0, 0, 1, ''),
('Eye_Coding_Terms', 'pterygium_OSCORNEA', 'pterygium', 500, 0, 0, '', 'OSCORNEA', 'ICD10:H11.052', 0, 0, 1, ''),
('Eye_Coding_Terms', 'abrasion_ODCORNEA', 'abrasion', 510, 0, 0, '', 'ODCORNEA', 'ICD10:S05.01XA', 0, 0, 1, ''),
('Eye_Coding_Terms', 'abrasion_OSCORNEA', 'abrasion', 520, 0, 0, '', 'OSCORNEA', 'ICD10:S05.02XA', 0, 0, 1, ''),
('Eye_Coding_Terms', 'FB_ODCORNEA', 'FB', 530, 0, 0, '', 'ODCORNEA', 'ICD10:T15.01XA', 0, 0, 1, ''),
('Eye_Coding_Terms', 'FB_OSCORNEA', 'FB', 540, 0, 0, '', 'OSCORNEA', 'ICD10:T15.02XA', 0, 0, 1, ''),
('Eye_Coding_Terms', 'dendrite_ODCORNEA', 'dendrite', 550, 0, 0, '', 'ODCORNEA', 'ICD10:B00.52', 0, 0, 1, ''),
('Eye_Coding_Terms', 'dendrite_OSCORNEA', 'dendrite', 560, 0, 0, '', 'OSCORNEA', 'ICD10:B00.52', 0, 0, 1, ''),
('Eye_Coding_Terms', 'MDF_ODCORNEA', 'MDF', 570, 0, 0, '', 'ODCORNEA', 'ICD10:H18.59', 0, 0, 1, ''),
('Eye_Coding_Terms', 'MDF_OSCORNEA', 'MDF', 580, 0, 0, '', 'OSCORNEA', 'ICD10:H18.59', 0, 0, 1, ''),
('Eye_Coding_Terms', 'NS_ODLENS', 'NS', 590, 0, 0, '', 'ODLENS', 'ICD10:H25.11', 0, 0, 1, ''),
('Eye_Coding_Terms', 'NS_OSLENS', 'NS', 600, 0, 0, '', 'OSLENS', 'ICD10:H25.12', 0, 0, 1, ''),
('Eye_Coding_Terms', 'PSC_ODLENS', 'PSC', 610, 0, 0, '', 'ODLENS', 'ICD10:H25.041', 0, 0, 1, ''),
('Eye_Coding_Terms', 'PSC_OSLENS', 'PSC', 620, 0, 0, '', 'OSLENS', 'ICD10:H25.042', 0, 0, 1, ''),
('Eye_Coding_Terms', 'PCIOL_ODLENS', 'PCIOL', 630, 0, 0, '', 'ODLENS', 'ICD10:Z96.1', 0, 0, 1, ''),
('Eye_Coding_Terms', 'PCIOL_OSLENS', 'PCIOL', 640, 0, 0, '', 'OSLENS', 'ICD10:Z96.1', 0, 0, 1, ''),
('Eye_Coding_Terms', 'hyphema_ODAC', 'hyphema', 650, 0, 0, '', 'ODAC', 'ICD10:H21.01', 0, 0, 1, ''),
('Eye_Coding_Terms', 'hyphema_OSAC', 'hyphema', 660, 0, 0, '', 'OSAC', 'ICD10:H21.02', 0, 0, 1, ''),
('Eye_Coding_Terms', 'horseshoe_ODPERIPHERY', 'horseshoe', 670, 0, 0, '', 'ODPERIPH', 'ICD10:H33.311', 0, 0, 1, ''),
('Eye_Coding_Terms', 'horseshoe_OSPERIPHERY', 'horseshoe', 680, 0, 0, '', 'OSPERIPH', 'ICD10:H33.312', 0, 0, 1, ''),
('Eye_Coding_Terms', 'hole_ODPERIPHERY', 'hole', 690, 0, 0, '', 'ODPERIPH', 'ICD10:H33.321', 0, 0, 1, ''),
('Eye_Coding_Terms', 'hole_OSPERIPHERY', 'hole', 700, 0, 0, '', 'OSPERIPH', 'ICD10:H33.322', 0, 0, 1, ''),
('Eye_Coding_Terms', 'CSR_ODMACULA', 'CSR', 710, 0, 0, '', 'ODMACULA', 'ICD10:H35.711', 0, 0, 1, ''),
('Eye_Coding_Terms', 'hole_ODMACULA', 'Mac hole', 720, 0, 0, '', 'ODMACULA', 'ICD10:H35.341', 0, 0, 1, ''),
('Eye_Coding_Terms', 'CSR_OSMACULA', 'CSR', 730, 0, 0, '', 'OSMACULA', 'ICD10:H35.712', 0, 0, 1, ''),
('Eye_Coding_Terms', 'hole_OSMACULA', 'Mac hole', 740, 0, 0, '', 'OSMACULA', 'ICD10:H35.342', 0, 0, 1, ''),
('Eye_Coding_Terms', 'drusen_ODMACULA', 'drusen', 750, 0, 0, '', 'ODMACULA', 'ICD10:H35.361', 0, 0, 1, ''),
('Eye_Coding_Terms', 'drusen_OSMACULA', 'drusen', 760, 0, 0, '', 'OSMACULA', 'ICD10:H35.362', 0, 0, 1, ''),
('Eye_Coding_Terms', 'drusen_ODDISC', 'drusen', 770, 0, 0, '', 'ODDISC', 'ICD10:H47.321', 0, 0, 1, ''),
('Eye_Coding_Terms', 'drusen_OSDISC', 'drusen', 780, 0, 0, '', 'ODDISC', 'ICD10:H47.322', 0, 0, 1, ''),
('Eye_Coding_Terms', 'BRVO_ODPERIPHERY', 'BRVO', 790, 0, 0, '', 'ODVESSELS', 'ICD10:H34.831', 0, 0, 1, ''),
('Eye_Coding_Terms', 'CRVO_ODPERIPHERY', 'CRVO', 800, 0, 0, '', 'ODVESSELS', 'ICD10:H34.811', 0, 0, 1, ''),
('Eye_Coding_Terms', 'lattice_ODPERIPHERY', 'lattice', 810, 0, 0, '', 'ODPERIPH', 'ICD10:H35.412', 0, 0, 1, ''),
('Eye_Coding_Terms', 'BRVO_OSPERIPHERY', 'BRVO', 820, 0, 0, '', 'OSVESSELS', 'ICD10:H34.832', 0, 0, 1, ''),
('Eye_Coding_Terms', 'CRVO_OSPERIPHERY', 'CRVO', 830, 0, 0, '', 'OSVESSELS', 'ICD10:H34.812', 0, 0, 1, ''),
('Eye_Coding_Terms', 'lattice_OSPERIPHERY', 'lattice', 840, 0, 0, '', 'OSPERIPH', 'ICD10:H35.412', 0, 0, 1, ''),
('Eye_Coding_Terms', 'NLDO_RMCT', 'NLDO', 850, 0, 0, '', 'RMCT', 'ICD10:H04.411', 0, 0, 1, ''),
('Eye_Coding_Terms', 'NLDO_LMCT', 'NLDO', 860, 0, 0, '', 'LMCT', 'ICD10:H04.412', 0, 0, 1, ''),
('Eye_Coding_Terms', 'NVD_ODDISC', 'NVD:DM', 870, 0, 0, '', 'ODDISC', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'NVD_OSDISC', 'NVD:DM', 880, 0, 0, '', 'OSDISC', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'CSME_ODMACULA', 'CSME:DM|IOL|RVO', 890, 0, 0, '', 'ODMACULA', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'CSME_OSMACULA', 'CSME:DM|IOL|RVO', 900, 0, 0, '', 'OSMACULA', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'NVE_ODVESSELS', 'NVE:DM', 910, 0, 0, '', 'ODVESSELS', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'NVE_OSVESSELS', 'NVE:DM', 920, 0, 0, '', 'OSVESSELS', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'NVE_ODPERIPHERY', 'NVE:DM', 930, 0, 0, '', 'ODPERIPH', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'NVE_OSPERIPHERY', 'NVE:DM', 940, 0, 0, '', 'OSPERIPH', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'BDR_ODMACULA', 'BDR:DM', 950, 0, 0, '', 'ODMACULA', '', 0, 0, 1, ''),
('Eye_Coding_Terms', 'BDR_OSMACULA', 'BDR:DM', 960, 0, 0, '', 'OSMACULA', '', 0, 0, 1, '');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'Eye_QP_ANTSEG_defaults', 'Eye QP List ANTSEG for New Providers', 1, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`, `activity`, `subtype`) VALUES
('Eye_QP_ANTSEG_defaults', 'ODCONJ_cl', 'c: clear field', 10, 0, 0, 'CONJ', '', '', 0, 0, 1, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCONJ_cl', 'c: clear field', 20, 0, 0, 'CONJ', '', '', 0, 0, 1, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCONJ_cl', 'c: clear field', 30, 0, 0, 'CONJ', '', '', 0, 0, 1, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCONJ_quiet', 'c: quiet', 40, 0, 0, 'CONJ', 'quiet', '', 0, 0, 1, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCONJ_quiet', 'c: quiet', 50, 0, 0, 'CONJ', 'quiet', '', 0, 0, 1, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCONJ_quiet', 'c: quiet', 60, 0, 0, 'CONJ', 'quiet', '', 0, 0, 1, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCONJ_inj', 'c: injection', 70, 0, 0, 'CONJ', 'injection', 'ICD10:H10.31', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCONJ_inj', 'c: injection', 80, 0, 0, 'CONJ', 'injection', 'ICD10:H10.32', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCONJ_inj', 'c: injection', 90, 0, 0, 'CONJ', 'injection', 'ICD10:H10.33', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCONJ_pap', 'c: papillae', 100, 0, 0, 'CONJ', 'papillae', 'ICD10:H10.31', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCONJ_pap', 'c: papillae', 110, 0, 0, 'CONJ', 'papillae', 'ICD10:H10.32', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCONJ_pap', 'c: papillae', 120, 0, 0, 'CONJ', 'papillae', 'ICD10:H10.33', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCONJ_gpap', 'c: giant pap', 130, 0, 0, 'CONJ', 'giant papillae', 'ICD10:H10.411', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCONJ_gpap', 'c: giant pap', 140, 0, 0, 'CONJ', 'giant papillae', 'ICD10:H10.412', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCONJ_gpap', 'c: giant pap', 150, 0, 0, 'CONJ', 'giant papillae', 'ICD10:H10.413', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCONJ_pinq', 'c: pinquecula', 160, 0, 0, 'CONJ', 'pinquecula', 'ICD10:H11.151', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCONJ_pinq', 'c: pinquecula', 170, 0, 0, 'CONJ', 'pinquecula', 'ICD10:H11.152', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCONJ_pinq', 'c: pinquecula', 180, 0, 0, 'CONJ', 'pinquecula', 'ICD10:H11.153', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCONJ_foll', 'c: follicles', 190, 0, 0, 'CONJ', 'follicles', 'ICD10:H10.011', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCONJ_foll', 'c: follicles', 200, 0, 0, 'CONJ', 'follicles', 'ICD10:H10.012', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCONJ_foll', 'c: follicles', 210, 0, 0, 'CONJ', 'follicles', 'ICD10:H10.013', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCONJ_mucop', 'c: mucopurulence', 220, 0, 0, 'CONJ', 'mucopurulence', 'ICD10:H10.013', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCONJ_mucop', 'c: mucopurulence', 230, 0, 0, 'CONJ', 'mucopurulence', 'ICD10:H10.013', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCONJ_mucop', 'c: mucopurulence', 240, 0, 0, 'CONJ', 'mucopurulence', 'ICD10:H10.013', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCONJ_bleb', 'c: mod bleb', 250, 0, 0, 'CONJ', 'moderate bleb', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCONJ_bleb', 'c: mod bleb', 260, 0, 0, 'CONJ', 'moderate bleb', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCONJ_bleb', 'c: mod bleb', 270, 0, 0, 'CONJ', 'moderate bleb', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCONJ_sied', 'c: siedel negative', 280, 0, 0, 'CONJ', 'siedel negative', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCONJ_sied', 'c: siedel negative', 290, 0, 0, 'CONJ', 'siedel negative', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCONJ_sied', 'c: siedel negative', 300, 0, 0, 'CONJ', 'siedel negative', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCORNEA_cl', 'k: clear field', 310, 0, 0, 'CORNEA', '', '', 0, 0, 1, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCORNEA_cl', 'k: clear field', 320, 0, 0, 'CORNEA', '', '', 0, 0, 1, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCORNEA_cl', 'k: clear field', 330, 0, 0, 'CORNEA', '', '', 0, 0, 1, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCORNEA_clear', 'k: clear', 340, 0, 0, 'CORNEA', 'clear', '', 0, 0, 1, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCORNEA_clear', 'k: clear', 350, 0, 0, 'CORNEA', 'clear', '', 0, 0, 1, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCORNEA_clear', 'k: clear', 360, 0, 0, 'CORNEA', 'clear', '', 0, 0, 1, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCORNEA_abr', 'k: abrasion', 370, 0, 0, 'CORNEA', 'abrasion', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCORNEA_abr', 'k: abrasion', 380, 0, 0, 'CORNEA', 'abrasion', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCORNEA_abr', 'k: abrasion', 390, 0, 0, 'CORNEA', 'abrasion', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCORNEA_MDF', 'k: MDFP dystrophy', 400, 0, 0, 'CORNEA', 'MDFP dystrophy', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCORNEA_MDF', 'k: MDFP dystrophy', 410, 0, 0, 'CORNEA', 'MDFP dystrophy', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCORNEA_MDF', 'k: MDFP dystrophy', 420, 0, 0, 'CORNEA', 'MDFP dystrophy', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCORNEA_FB', 'k: metallic FB', 430, 0, 0, 'CORNEA', 'metallic FB', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCORNEA_FB', 'k: metallic FB', 440, 0, 0, 'CORNEA', 'metallic FB', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCORNEA_FB', 'k: metallic FB', 450, 0, 0, 'CORNEA', 'metallic FB', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCORNEA_edema', 'k: edema', 460, 0, 0, 'CORNEA', 'edema', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCORNEA_edema', 'k: edema', 470, 0, 0, 'CORNEA', 'edema', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCORNEA_edema', 'k: edema', 480, 0, 0, 'CORNEA', 'edema', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCORNEA_HSV', 'k: dendrite', 490, 0, 0, 'CORNEA', 'dendrite', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCORNEA_HSV', 'k: dendrite', 500, 0, 0, 'CORNEA', 'dendrite', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCORNEA_HSV', 'k: dendrite', 510, 0, 0, 'CORNEA', 'dendrite', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCORNEA_scar', 'k: stromal scar', 520, 0, 0, 'CORNEA', 'stromal scar', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCORNEA_scar', 'k: stromal scar', 530, 0, 0, 'CORNEA', 'stromal scar', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCORNEA_scar', 'k: stromal scar', 540, 0, 0, 'CORNEA', 'stromal scar', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCORNEA_gut', 'k: guttatae', 550, 0, 0, 'CORNEA', 'guttatae', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCORNEA_gut', 'k: guttatae', 560, 0, 0, 'CORNEA', 'guttatae', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCORNEA_gut', 'k: guttatae', 570, 0, 0, 'CORNEA', 'guttatae', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCORNEA_fkp', 'k: fine KPs', 580, 0, 0, 'CORNEA', 'fine KPs', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCORNEA_fkp', 'k: fine KPs', 590, 0, 0, 'CORNEA', 'fine KPs', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCORNEA_fkp', 'k: fine KPs', 600, 0, 0, 'CORNEA', 'fine KPs', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODCORNEA_mkp', 'k: mutton-fat KPs', 610, 0, 0, 'CORNEA', 'mutton-fat keratic precipitates', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSCORNEA_mkp', 'k: mutton-fat KPs', 620, 0, 0, 'CORNEA', 'mutton-fat keratic precipitates', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUCORNEA_mkp', 'k: mutton-fat KPs', 630, 0, 0, 'CORNEA', 'mutton-fat keratic precipitates', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODAC_cl', 'ac: clear field', 640, 0, 0, 'AC', '', '', 0, 0, 1, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSAC_cl', 'ac: clear field', 650, 0, 0, 'AC', '', '', 0, 0, 1, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUAC_cl', 'ac: clear field', 660, 0, 0, 'AC', '', '', 0, 0, 1, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODAC_clear', 'ac: clear', 670, 0, 0, 'AC', 'clear', '', 0, 0, 1, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSAC_clear', 'ac: clear', 680, 0, 0, 'AC', 'clear', '', 0, 0, 1, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUAC_clear', 'ac: clear', 690, 0, 0, 'AC', 'clear', '', 0, 0, 1, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODAC_fc', 'ac: F/C', 700, 0, 0, 'AC', 'F/C', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSAC_fc', 'ac: F/C', 710, 0, 0, 'AC', 'F/C', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUAC_fc', 'ac: F/C', 720, 0, 0, 'AC', 'F/C', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODAC_nar', 'ac :narrow', 730, 0, 0, 'AC', 'narrow', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSAC_nar', 'ac :narrow', 740, 0, 0, 'AC', 'narrow', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUAC_nar', 'ac :narrow', 750, 0, 0, 'AC', 'narrow', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODAC_hyp', 'ac: hyphema', 760, 0, 0, 'AC', 'hyphema', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSAC_hyp', 'ac: hyphema', 770, 0, 0, 'AC', 'hyphema', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUAC_hyp', 'ac: hyphema', 780, 0, 0, 'AC', 'hyphema', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODLENS_cl', 'lens: clear field', 790, 0, 0, 'LENS', '', '', 0, 0, 1, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSLENS_cl', 'lens: clear field', 800, 0, 0, 'LENS', '', '', 0, 0, 1, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OULENS_cl', 'lens: clear field', 810, 0, 0, 'LENS', '', '', 0, 0, 1, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODLENS_pxe', 'lens: PXE', 820, 0, 0, 'LENS', 'PXE', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSLENS_pxe', 'lens: PXE', 830, 0, 0, 'LENS', 'PXE', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OULENS_pxw', 'lens: PXE', 840, 0, 0, 'LENS', 'PXE', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODLENS_psc', 'lens: PSC', 850, 0, 0, 'LENS', 'PSC', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSLENS_psc', 'lens: PSC', 860, 0, 0, 'LENS', 'PSC', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OULENS_psc', 'lens: PSC', 870, 0, 0, 'LENS', 'PSC', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODLENS_ns', 'lens: NS', 880, 0, 0, 'LENS', 'NS', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSLENS_ns', 'lens: NS', 890, 0, 0, 'LENS', 'NS', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OULENS_ns', 'lens: NS', 900, 0, 0, 'LENS', 'NS', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODLENS_cort', 'lens: cortical', 910, 0, 0, 'LENS', 'cortical', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSLENS_cort', 'lens: cortical', 920, 0, 0, 'LENS', 'cortical', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OULENS_cort', 'lens: cortical', 930, 0, 0, 'LENS', 'cortical', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODLENS_PC', 'lens: PCIOL', 940, 0, 0, 'LENS', 'PCIOL', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSLENS_PC', 'lens: PCIOL', 950, 0, 0, 'LENS', 'PCIOL', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OULENS_PC', 'lens: PCIOL', 960, 0, 0, 'LENS', 'PCIOL', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODLENS_yag', 'lens: p YAG', 970, 0, 0, 'LENS', 'PC open', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSLENS_yag', 'lens: p YAG', 980, 0, 0, 'LENS', 'PC open', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OULENS_yag', 'lens: p YAG', 990, 0, 0, 'LENS', 'PC open', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODIRIS_cl', 'iris: clear field', 1000, 0, 0, 'IRIS', '', '', 0, 0, 1, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSIRIS_cl', 'iris: clear field', 1010, 0, 0, 'IRIS', '', '', 0, 0, 1, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUIRIS_cl', 'iris: clear field', 1020, 0, 0, 'IRIS', '', '', 0, 0, 1, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODIRIS_pxe', 'iris: PXE', 1030, 0, 0, 'IRIS', 'PXE', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSIRIS_px', 'iris: PXE', 1040, 0, 0, 'IRIS', 'PXE', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUIRIS_px', 'iris: PXE', 1050, 0, 0, 'IRIS', 'PXE', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODIRIS_pi', 'iris: PI', 1060, 0, 0, 'IRIS', 'patent PI', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSIRIS_pi', 'iris: PI', 1070, 0, 0, 'IRIS', 'patent PI', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUIRIS_pi', 'iris: PI', 1080, 0, 0, 'IRIS', 'patent PI', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODIRIS_nev', 'iris: nevus', 1090, 0, 0, 'IRIS', 'nevus', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSIRIS_nev', 'iris: nevus', 1100, 0, 0, 'IRIS', 'nevus', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUIRIS_nev', 'iris: nevus', 1110, 0, 0, 'IRIS', 'nevus', '', 0, 0, 0, 'OU'),
('Eye_QP_ANTSEG_defaults', 'ODIRIS_nv', 'iris: NVI', 1120, 0, 0, 'IRIS', 'NVI', '', 0, 0, 0, 'OD'),
('Eye_QP_ANTSEG_defaults', 'OSIRIS_nv', 'iris: NVI', 1130, 0, 0, 'LENS', 'NVI', '', 0, 0, 0, 'OS'),
('Eye_QP_ANTSEG_defaults', 'OUIRIS_nv', 'iris: NVI', 1140, 0, 0, 'IRIS', 'NVI', '', 0, 0, 0, 'OU');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'Eye_QP_EXT_defaults', 'Eye QP List EXT for New Providers', 1, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`, `activity`, `subtype`) VALUES
('Eye_QP_EXT_defaults', 'RBROW', 'BROW: clear field', 10, 0, 0, 'BROW', '', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LBROW', 'BROW: clear field', 20, 0, 0, 'BROW', '', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BBROW', 'BROW: clear field', 30, 0, 0, 'BROW', '', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RBROW_ptosis', 'BROW: ptosis', 40, 0, 0, 'BROW', 'ptosis', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LBROW_ptosis', 'BROW: ptosis', 50, 0, 0, 'BROW', 'ptosis', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BBROW_ptosis', 'BROW: ptosis', 60, 0, 0, 'BROW', 'ptosis', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RBROW_rhytids', 'BROW: rhytids', 70, 0, 0, 'BROW', 'rhytids', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LBROW_rhytids', 'BROW: rhytids', 80, 0, 0, 'BROW', 'rhytids', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BBROW_rhytids', 'BROW: rhytids', 90, 0, 0, 'BROW', 'rhytids', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RBROW_scar', 'BROW: scar', 100, 0, 0, 'BROW', 'scar', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LBROW_scar', 'BROW: scar', 110, 0, 0, 'BROW', 'scar', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BBROW_scar', 'BROW: scar', 120, 0, 0, 'BROW', 'scar', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RBROW_sebk', 'BROW: seb ker', 130, 0, 0, 'BROW', 'seb ker', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LBROW_sebk', 'BROW: seb ker', 140, 0, 0, 'BROW', 'seb ker', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BBROW_sebk', 'BROW: seb ker', 150, 0, 0, 'BROW', 'seb ker', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RBROW_AK', 'BROW: act ker', 160, 0, 0, 'BROW', 'actinic keratosis', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LBROW_AK', 'BROW: act ker', 170, 0, 0, 'BROW', 'actinic keratosis', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BBROW_AK', 'BROW: act ker', 180, 0, 0, 'BROW', 'actinic keratosis', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RBROW_bcc', 'BROW: BCC', 190, 0, 0, 'BROW', 'BCC', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LBROW_bcc', 'BROW: BCC', 200, 0, 0, 'BROW', 'BCC', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BBROW_bcc', 'BROW: BCC', 210, 0, 0, 'BROW', 'BCC', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RBROW_scc', 'BROW: SCC', 220, 0, 0, 'BROW', 'SCC', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LBROW_scc', 'BROW: SCC', 230, 0, 0, 'BROW', 'SCC', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BBROW_scc', 'BROW: SCC', 240, 0, 0, 'BROW', 'SCC', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RMRD_mrd0', 'MRD: 0', 250, 0, 0, 'MRD', '0', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LMRD_mrd0', 'MRD: 0', 260, 0, 0, 'MRD', '0', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BMRD_mrd0', 'MRD: 0', 270, 0, 0, 'MRD', '0', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RMRD_mrd1', 'MRD: 1', 280, 0, 0, 'MRD', '1', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LMRD_mrd1', 'MRD: 1', 290, 0, 0, 'MRD', '1', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BMRD_mrd1', 'MRD: 1', 300, 0, 0, 'MRD', '1', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RMRD_mrd2', 'MRD: 2', 310, 0, 0, 'MRD', '2', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LMRD_mrd2', 'MRD: 2', 320, 0, 0, 'MRD', '2', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BMRD_mrd2', 'MRD: 2', 330, 0, 0, 'MRD', '2', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RMRD_mrd3', 'MRD: 3', 340, 0, 0, 'MRD', '3', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LMRD_mrd3', 'MRD: 3', 350, 0, 0, 'MRD', '3', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BMRD_mrd3', 'MRD: 3', 360, 0, 0, 'MRD', '3', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RLF_17', 'LF: 17', 370, 0, 0, 'LF', '17', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LLF_17', 'LF: 17', 380, 0, 0, 'LF', '17', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BLF_17', 'LF: 17', 390, 0, 0, 'LF', '17', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RLF_15', 'LF: 15', 400, 0, 0, 'LF', '15', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LLF_15', 'LF: 15', 410, 0, 0, 'LF', '15', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BLF_15', 'LF: 15', 420, 0, 0, 'LF', '15', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RLF_13', 'LF: 13', 430, 0, 0, 'LF', '13', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LLF_13', 'LF: 13', 440, 0, 0, 'LF', '13', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BLF_13', 'LF: 13', 450, 0, 0, 'LF', '13', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RUL_clear', 'UL: clear field', 460, 0, 0, 'UL', '', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LUL_clear', 'UL: clear field', 470, 0, 0, 'UL', '', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BUL_clear', 'UL: clear field', 480, 0, 0, 'UL', '', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RUL_norm', 'UL: normal', 490, 0, 0, 'UL', 'normal lids and lashes', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LUL_norm', 'UL: normal', 500, 0, 0, 'UL', 'normal lids and lashes', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BUL_norm', 'UL: normal', 510, 0, 0, 'UL', 'normal lids and lashes', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RUL_der', 'UL: dermatochalasis', 520, 0, 0, 'UL', 'dermatochalasis', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LUL_der', 'UL: dermatochalasis', 530, 0, 0, 'UL', 'dermatochalasis', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BUL_der', 'UL: dermatochalasis', 540, 0, 0, 'UL', 'dermatochalasis', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RLL_der', 'LL: dermatochalasis', 550, 0, 0, 'LL', 'dermatochalasis', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LLL_der', 'LL: dermatochalasis', 560, 0, 0, 'LL', 'dermatochalasis', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BLL_der', 'LL: dermatochalasis', 570, 0, 0, 'LL', 'dermatochalasis', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RUL_pto2', 'UL: 2mm ptosis', 580, 0, 0, 'UL', '2mm ptosis', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LUL_pto2', 'UL: 2mm ptosis', 590, 0, 0, 'UL', '2mm ptosis', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BUL_pto2', 'UL: 2mm ptosis', 600, 0, 0, 'UL', '2mm ptosis', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RUL_pto3', 'UL: 3mm ptosis', 610, 0, 0, 'UL', '3mm ptosis', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LUL_pto3', 'UL: 3mm ptosis', 620, 0, 0, 'UL', '3mm ptosis', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BUL_pto3', 'UL: 3mm ptosis', 630, 0, 0, 'UL', '3mm ptosis', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RUL_lesion', 'UL: lesion', 640, 0, 0, 'UL', 'lesion', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LUL_lesion', 'UL: lesion', 650, 0, 0, 'UL', 'lesion', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BUL_lesion', 'UL: lesion', 660, 0, 0, 'UL', 'lesion', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RUL_chalazion', 'UL: chalazion', 670, 0, 0, 'UL', 'chalazion', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LUL_chalazion', 'UL: chalazion', 680, 0, 0, 'UL', 'chalazion', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BUL_chalazion', 'UL: chalazion', 690, 0, 0, 'UL', 'chalazion', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RUL_stye', 'UL: stye', 700, 0, 0, 'UL', 'stye', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LUL_stye', 'UL: stye', 710, 0, 0, 'UL', 'stye', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BUL_stye', 'UL: stye', 720, 0, 0, 'UL', 'stye', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RMCT_les', 'MCT: lesion', 730, 0, 0, 'MCT', 'lesion', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LMCT_les', 'MCT: lesion', 740, 0, 0, 'MCT', 'lesion', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BMCT_les', 'MCT: lesion', 750, 0, 0, 'MCT', 'lesion', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RMCT_NLDa', 'MCT: NLDO, acute', 760, 0, 0, 'MCT', 'NLDO, acute', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LMCT_NLDa', 'MCT: NLDO, acute', 770, 0, 0, 'MCT', 'NLDO, acute', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BMCT_NLDa', 'MCT: NLDO, acute', 780, 0, 0, 'MCT', 'NLDO, acute', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RMCT_NLDc', 'MCT: NLDO, chronic', 790, 0, 0, 'MCT', 'NLDO, chronic', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LMCT_NLDc', 'MCT: NLDO, chronic', 800, 0, 0, 'MCT', 'NLDO, chronic', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BMCT_NLDc', 'MCT: NLDO, chronic', 810, 0, 0, 'MCT', 'NLDO, chronic', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RLL_clear', 'LL: clear field', 820, 0, 0, 'LL', '', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LLL_clear', 'LL: clear field', 830, 0, 0, 'LL', '', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BLL_clear', 'LL: clear field', 840, 0, 0, 'LL', '', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RLL_norm', 'LL: good tone', 850, 0, 0, 'LL', 'good tone', '', 0, 0, 1, 'R'),
('Eye_QP_EXT_defaults', 'LLL_norm', 'LL: good tone', 860, 0, 0, 'LL', 'good tone', '', 0, 0, 1, 'L'),
('Eye_QP_EXT_defaults', 'BLL_norm', 'LL: good tone', 870, 0, 0, 'LL', 'good tone', '', 0, 0, 1, 'B'),
('Eye_QP_EXT_defaults', 'RLL_ect', 'LL: ectropion', 880, 0, 0, 'LL', 'ectropion', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LLL_ect', 'LL: ectropion', 890, 0, 0, 'LL', 'ectropion', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BLL_ect', 'LL: ectropion', 900, 0, 0, 'LL', 'ectropion', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RLL_ent', 'LL: entropion', 910, 0, 0, 'LL', 'entropion', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LLL_ent', 'LL: entropion', 920, 0, 0, 'LL', 'entropion', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BLL_ent', 'LL: entropion', 930, 0, 0, 'LL', 'entropion', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RLL_trich', 'LL: trichiasis', 940, 0, 0, 'LL', 'trichiasis', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LLL_trich', 'LL: trichiasis', 950, 0, 0, 'LL', 'trichiasis', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BLL_trich', 'LL: trichiasis', 960, 0, 0, 'LL', 'trichiasis', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RLL_lesion', 'LL: lesion', 970, 0, 0, 'LL', 'lesion', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LLL_lesion', 'LL: lesion', 980, 0, 0, 'LL', 'lesion', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BLL_lesion', 'LL: lesion', 990, 0, 0, 'LL', 'lesion', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RLL_fat', 'LL: fat prolapse', 1010, 0, 0, 'LL', 'fat prolapse', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LLL_fat', 'LL: fat prolapse', 1020, 0, 0, 'LL', 'fat prolapse', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BLL_fat', 'LL: fat prolapse', 1030, 0, 0, 'LL', 'fat prolapse', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RLL_pi', 'LL: erythema', 1040, 0, 0, 'LL', 'erythema', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LLL_pi', 'LL: erythema', 1050, 0, 0, 'LL', 'erythema', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BLL_pi', 'LL: erythema', 1060, 0, 0, 'LL', 'erythema', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RLL_nev', 'LL: ecchymosis', 1070, 0, 0, 'LL', 'ecchymosis', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LLL_nev', 'LL: ecchymosis', 1080, 0, 0, 'LL', 'ecchymosis', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BLL_nev', 'LL: ecchymosis', 1090, 0, 0, 'LL', 'ecchymosis', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RLL_chalazion', 'LL: chalazion', 1100, 0, 0, 'LL', 'chalazion', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'BLL_chalazion', 'LL: chalazion', 1110, 0, 0, 'LL', 'chalazion', '', 0, 0, 0, 'B'),
('Eye_QP_EXT_defaults', 'RLL_stye', 'LL: stye', 1120, 0, 0, 'LL', 'stye', '', 0, 0, 0, 'R'),
('Eye_QP_EXT_defaults', 'LLL_stye', 'LL: stye', 1130, 0, 0, 'LL', 'stye', '', 0, 0, 0, 'L'),
('Eye_QP_EXT_defaults', 'BLL_stye', 'LL: stye', 1140, 0, 0, 'LL', 'stye', '', 0, 0, 0, 'B');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'Eye_QP_RETINA_defaults', 'Eye QP List RETINA for New Providers', 1, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`, `activity`, `subtype`) VALUES
('Eye_QP_RETINA_defaults', 'ODCUP_0', 'cup: clear field', 10, 0, 0, 'CUP', '', '', 0, 0, 1, 'OD'),
('Eye_QP_RETINA_defaults', 'OSCUP_0', 'cup: clear field', 20, 0, 0, 'CUP', '', '', 0, 0, 1, 'OS'),
('Eye_QP_RETINA_defaults', 'OUCUP_0', 'cup: clear field', 30, 0, 0, 'CUP', '', '', 0, 0, 1, 'OU'),
('Eye_QP_RETINA_defaults', 'ODCUP_1', 'cup: 0.1', 40, 0, 0, 'CUP', '0.1', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSCUP_1', 'cup: 0.1', 50, 0, 0, 'CUP', '0.1', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUCUP_1', 'cup: 0.1', 60, 0, 0, 'CUP', '0.1', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODCUP_3', 'cup: 0.3', 70, 0, 0, 'CUP', '0.3', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSCUP_3', 'cup: 0.3', 80, 0, 0, 'CUP', '0.3', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'ODCUP_5', 'cup: 0.5', 90, 0, 0, 'CUP', '0.5', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OUCUP_3', 'cup: 0.3', 100, 0, 0, 'CUP', '0.3', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'OSCUP_5', 'cup: 0.5', 110, 0, 0, 'CUP', '0.5', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUCUP_5', 'cup: 0.5', 120, 0, 0, 'CUP', '0.5', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODCUP_8', 'cup: 0.8', 130, 0, 0, 'CUP', '0.8', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSCUP_8', 'cup: 0.8', 140, 0, 0, 'CUP', '0.8', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUCUP_8', 'cup: 0.8', 150, 0, 0, 'CUP', '0.8', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODCUP_95', 'cup: 0.95', 160, 0, 0, 'CUP', '0.95', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSCUP_95', 'cup: 0.95', 170, 0, 0, 'CUP', '0.95', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUCUP_95', 'cup: 0.95', 180, 0, 0, 'CUP', '0.95', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODCUP_V', 'cup: V (vert)', 190, 0, 0, 'CUP', 'V', '', 0, 0, 2, 'OD'),
('Eye_QP_RETINA_defaults', 'OSCUP_V', 'cup: V (vert)', 200, 0, 0, 'CUP', 'V', '', 0, 0, 2, 'OS'),
('Eye_QP_RETINA_defaults', 'OUCUP_V', 'cup: V (vert)', 210, 0, 0, 'CUP', 'V', '', 0, 0, 2, 'OU'),
('Eye_QP_RETINA_defaults', 'ODCUP_x', 'cup: x (times)', 220, 0, 0, 'CUP', 'x', '', 0, 0, 2, 'OD'),
('Eye_QP_RETINA_defaults', 'OSCUP_x', 'cup: x (times)', 230, 0, 0, 'CUP', 'x', '', 0, 0, 2, 'OS'),
('Eye_QP_RETINA_defaults', 'OUCUP_x', 'cup: x (times)', 240, 0, 0, 'CUP', 'x', '', 0, 0, 2, 'OU'),
('Eye_QP_RETINA_defaults', 'ODCUP_H', 'cup: H (horiz)', 250, 0, 0, 'CUP', 'H', '', 0, 0, 2, 'OD'),
('Eye_QP_RETINA_defaults', 'OSCUP_H', 'cup: H (horiz)', 260, 0, 0, 'CUP', 'H', '', 0, 0, 2, 'OS'),
('Eye_QP_RETINA_defaults', 'OUCUP_H', 'cup: H (horiz)', 270, 0, 0, 'CUP', 'H', '', 0, 0, 2, 'OU'),
('Eye_QP_RETINA_defaults', 'ODCUP_notch', 'cup: notch', 280, 0, 0, 'CUP', 'notch', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSCUP_notch', 'cup: notch', 290, 0, 0, 'CUP', 'notch', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUCUP_notch', 'cup: notch', 300, 0, 0, 'CUP', 'notch', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODDISC_0', 'd: clear field', 310, 0, 0, 'DISC', '', '', 0, 0, 1, 'OD'),
('Eye_QP_RETINA_defaults', 'OSDISC_0', 'd: clear field', 320, 0, 0, 'DISC', '', '', 0, 0, 1, 'OS'),
('Eye_QP_RETINA_defaults', 'OUDISC_0', 'd: clear field', 330, 0, 0, 'DISC', '', '', 0, 0, 1, 'OU'),
('Eye_QP_RETINA_defaults', 'ODDISC_risk', 'd: at risk', 340, 0, 0, 'DISC', 'disc at risk', '', 0, 0, 1, 'OD'),
('Eye_QP_RETINA_defaults', 'OSDISC_risk', 'd: at risk', 350, 0, 0, 'DISC', 'disk at risk', '', 0, 0, 1, 'OS'),
('Eye_QP_RETINA_defaults', 'OUDISC_risk', 'd: at risk', 360, 0, 0, 'DISC', 'disk at risk', '', 0, 0, 1, 'OU'),
('Eye_QP_RETINA_defaults', 'ODDISC_pal', 'd: pallor', 370, 0, 0, 'DISC', 'pallor', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSCUP_pal', 'd: pallor', 380, 0, 0, 'DISC', 'pallor', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUDISC_pal', 'd: pallor', 390, 0, 0, 'DISC', 'pallor', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODDISC_NVD', 'd: NVD', 400, 0, 0, 'DISC', 'NVD', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSDISC_NVD', 'd: NVD', 410, 0, 0, 'DISC', 'NVD', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUDISC_NVD', 'd: NVD', 420, 0, 0, 'DISC', 'NVD', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODDISC_edema1', 'd: gr I edema', 430, 0, 0, 'DISC', 'Grade I papilledema', '', 0, 0, 1, 'OD'),
('Eye_QP_RETINA_defaults', 'OSDISC_edema1', 'd: gr I edema', 440, 0, 0, 'DISC', 'Grade I papilledema', '', 0, 0, 1, 'OS'),
('Eye_QP_RETINA_defaults', 'OUDISC_edema1', 'd: gr I edema', 450, 0, 0, 'DISC', 'Grade I papilledema', '', 0, 0, 1, 'OU'),
('Eye_QP_RETINA_defaults', 'ODDISC_edema2', 'd: gr III edema', 460, 0, 0, 'DISC', 'Grade III papilledema', '', 0, 0, 1, 'OD'),
('Eye_QP_RETINA_defaults', 'OSDISC_edema2', 'd: gr III edema', 470, 0, 0, 'DISC', 'Grade III papilledema', '', 0, 0, 1, 'OS'),
('Eye_QP_RETINA_defaults', 'OUDISC_edema2', 'd: gr III edema', 480, 0, 0, 'DISC', 'Grade III papilledema', '', 0, 0, 1, 'OU'),
('Eye_QP_RETINA_defaults', 'ODDISC_edemaf', 'd: gr V edema', 490, 0, 0, 'DISC', 'Grade V papilledema', '', 0, 0, 1, 'OD'),
('Eye_QP_RETINA_defaults', 'OSDISC_edemaf', 'd: gr V edema', 500, 0, 0, 'DISC', 'Grade V papilledema', '', 0, 0, 1, 'OS'),
('Eye_QP_RETINA_defaults', 'OUDISC_edemaf', 'd: gr V edema', 510, 0, 0, 'DISC', 'Grade V papilledema', '', 0, 0, 1, 'OU'),
('Eye_QP_RETINA_defaults', 'ODMAC_0', 'm: clear field', 520, 0, 0, 'MACULA', '', '', 0, 0, 1, 'OD'),
('Eye_QP_RETINA_defaults', 'OSMAC_0', 'm: clear field', 530, 0, 0, 'MACULA', '', '', 0, 0, 1, 'OS'),
('Eye_QP_RETINA_defaults', 'OUMAC_0', 'm: clear field', 540, 0, 0, 'MACULA', '', '', 0, 0, 1, 'OU'),
('Eye_QP_RETINA_defaults', 'ODMAC_hd', 'm: hard drusen', 550, 0, 0, 'MACULA', 'hard drusen', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSMAC_hd', 'm: hard drusen', 560, 0, 0, 'MACULA', 'hard drusen', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUMAC_hd', 'm: hard drusen', 570, 0, 0, 'MACULA', 'hard drusen', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODMAC_sd', 'm: soft drusen', 580, 0, 0, 'MACULA', 'soft drusen', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSMAC_sd', 'm: soft drusen', 590, 0, 0, 'MACULA', 'soft drusen', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUMAC_sd', 'm: soft drusen', 600, 0, 0, 'MACULA', 'soft drusen', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODMAC_PED', 'm: PED', 610, 0, 0, 'MACULA', 'PED', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSMAC_PED', 'm: PED', 620, 0, 0, 'MACULA', 'PED', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUMAC_PED', 'm: PED', 630, 0, 0, 'MACULA', 'PED', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODMAC_CSR', 'm: CSR', 640, 0, 0, 'MACULA', 'CSR', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSMAC_CSR', 'm: CSR', 650, 0, 0, 'MACULA', 'CSR', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUMAC_CSR', 'm: CSR', 660, 0, 0, 'MACULA', 'CSR', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODVESSELS_cl', 'v: clear field', 670, 0, 0, 'VESSELS', '', '', 0, 0, 1, 'OD'),
('Eye_QP_RETINA_defaults', 'OSVESSELS_cl', 'v: clear field', 680, 0, 0, 'VESSELS', '', '', 0, 0, 1, 'OS'),
('Eye_QP_RETINA_defaults', 'OUVESSELS_cl', 'v: clear field', 690, 0, 0, 'VESSELS', '', '', 0, 0, 1, 'OU'),
('Eye_QP_RETINA_defaults', 'ODVESSELS_12', 'v: 1:2', 700, 0, 0, 'VESSELS', '1:2', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSVESSELS_12', 'v: 1:2', 710, 0, 0, 'VESSELS', '1:2', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUVESSELS_12', 'v: 1:2', 720, 0, 0, 'VESSELS', '1:2', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODVESSELS_BDR', 'v: BDR', 730, 0, 0, 'VESSELS', 'BDR', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSVESSELS_BDR', 'v: BDR', 740, 0, 0, 'VESSELS', 'BDR', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUVESSELS_BDR', 'v: BDR', 750, 0, 0, 'VESSELS', 'BDR', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODVESSELS_PDR', 'v: PDR', 760, 0, 0, 'VESSELS', 'PDR', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSVESSELS_PDR', 'v: PDR', 770, 0, 0, 'VESSELS', 'PDR', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUVESSELS_PDR', 'v: PDR', 780, 0, 0, 'VESSELS', 'PDR', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODVESSELS_BRVO', 'v: BRVO', 790, 0, 0, 'VESSELS', 'BRVO', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSVESSELS_BRVO', 'v: BRVO', 800, 0, 0, 'VESSELS', 'BRVO', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUVESSELS_BRVO', 'v: BRVO', 810, 0, 0, 'VESSELS', 'BRVO', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODVESSELS_CRVO', 'v: CRVO', 820, 0, 0, 'VESSELS', 'CRVO', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSVESSELS_CRVO', 'v: CRVO', 830, 0, 0, 'VESSELS', 'CRVO', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUVESSELS_CRVO', 'v: CRVO', 840, 0, 0, 'VESSELS', 'CRVO', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODVESSELS_BRAO', 'v: BRAO', 850, 0, 0, 'VESSELS', 'BRAO', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSVESSELS_BRAO', 'v: BRAO', 860, 0, 0, 'VESSELS', 'BRAO', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUVESSELS_BRAO', 'v: BRAO', 870, 0, 0, 'VESSELS', 'BRAO', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODVESSELS_CRAO', 'v: CRAO', 880, 0, 0, 'VESSELS', 'CRAO', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSVESSELS_CRAO', 'v: CRAO', 890, 0, 0, 'VESSELS', 'CRAO', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUVESSELS_CRAO', 'v: CRAO', 900, 0, 0, 'VESSELS', 'CRAO', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODPERIPH_0', 'p: clear field', 910, 0, 0, 'PERIPH', '', '', 0, 0, 1, 'OD'),
('Eye_QP_RETINA_defaults', 'OSPERIPH_0', 'p: clear field', 920, 0, 0, 'PERIPH', '', '', 0, 0, 1, 'OS'),
('Eye_QP_RETINA_defaults', 'OUPERIPH_0', 'p: clear field', 930, 0, 0, 'PERIPH', '', '', 0, 0, 1, 'OU'),
('Eye_QP_RETINA_defaults', 'ODPERIPH_float', 'p: floater', 940, 0, 0, 'PERIPH', 'vitreous floater', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSPERIPH_float', 'p: floater', 950, 0, 0, 'PERIPH', 'vitreous floater', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUPERIPH_float', 'p: floater', 960, 0, 0, 'PERIPH', 'vitreous floater', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODPERIPH_pvd', 'p: PVD', 970, 0, 0, 'PERIPH', 'PVD', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSPERIPH_pvd', 'p: PVD', 980, 0, 0, 'PERIPH', 'PVD', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUPERIPH_pvd', 'p: PVD', 990, 0, 0, 'PERIPH', 'PVD', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODPERIPH_vh', 'p: vit hem', 1000, 0, 0, 'PERIPH', 'vitreous hemorrhage', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSPERIPH_vh', 'p: vit hem', 1010, 0, 0, 'PERIPH', 'vitreous hemorrhage', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUPERIPH_vh', 'p: vit hem', 1020, 0, 0, 'PERIPH', 'vitreous hemorrhage', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODPERIPH_tear', 'p: retinal tear', 1030, 0, 0, 'PERIPH', 'retinal tear', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSPERIPH_tear', 'p: retinal tear', 1040, 0, 0, 'PERIPH', 'retinal tear', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUPERIPH_tear', 'p: retinal tear', 1050, 0, 0, 'PERIPH', 'retinal tear', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODPERIPH_schisis', 'p: retinoschisis', 1060, 0, 0, 'PERIPH', 'retinoschisis', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSPERIPH_schisis', 'p: retinoschisis', 1070, 0, 0, 'PERIPH', 'retinoschisis', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUPERIPH_schisis', 'p: retinoschisis', 1080, 0, 0, 'PERIPH', 'retinoschisis', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODPERIPH_NVE', 'p: NVE', 1090, 0, 0, 'PERIPH', 'NVE', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSPERIPH_NVE', 'p: NVE', 1100, 0, 0, 'PERIPH', 'NVE', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUPERIPH_NVE', 'p: NVE', 1110, 0, 0, 'PERIPH', 'NVE', '', 0, 0, 0, 'OU'),
('Eye_QP_RETINA_defaults', 'ODPERIPH_RD', 'p: RD', 1120, 0, 0, 'PERIPH', 'RD', '', 0, 0, 0, 'OD'),
('Eye_QP_RETINA_defaults', 'OSPERIPH_RD', 'p: RD', 1130, 0, 0, 'PERIPH', 'RD', '', 0, 0, 0, 'OS'),
('Eye_QP_RETINA_defaults', 'OUPERIPH_RD', 'p: RD', 1140, 0, 0, 'PERIPH', 'RD', '', 0, 0, 0, 'OU');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'Eye_todo_done_defaults', 'Eye Orders Defaults', 1, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`, `activity`, `subtype`) VALUES
('Eye_todo_done_defaults', 'Ascan', 'A scan/IOL calc', 430, 0, 0, '', '', 'CPT4:76519', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'Bscan', 'B scan', 440, 0, 0, '', '', 'CPT4:76512', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'dilation', 'Dilated Exam', 90, 0, 0, '', 'Full dilated exam', '', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'ExtPhoto', 'Ext Photos', 200, 0, 0, '', '', 'CPT4:92285', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'FundusPhoto', 'Retina Photo', 210, 0, 0, '', '', 'CPT4:92250', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'Gonio', 'Gonio', 410, 0, 0, '', '', 'CPT4:92020', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'IOP', 'IOP', 80, 0, 0, '', 'Intraocular pressure check', '', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'irrigate', 'Irrigate', 450, 0, 0, '', '', 'CPT4:68840', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'ISHI', 'Color Plates', 320, 0, 0, '', '', '', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'lesion', 'Surg: office/15 min', 470, 0, 0, '', '', 'CPT4:67840', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'OCTDisc', 'OCT Disc', 300, 0, 0, '', '', 'CPT4:92133', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'OCTRetina', 'OCT Retina', 310, 0, 0, '', '', 'CPT4:92134', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'Pachy', 'Pachymetry', 420, 0, 0, '', '', 'CPT4:76514', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'refraction', 'Refraction', 460, 0, 0, '', '', 'CPT4:92015', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'RTC1month', 'RTC 1 month', 20, 0, 0, '', 'F/U 1 month', '', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'RTC1WK', 'RTC 1 week', 10, 0, 0, '', 'F/U 1 week', '', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'RTC1yr', 'RTC 1 year', 40, 0, 0, '', 'Recheck 1 year', '', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'RTC2yr', 'RTC  2 years', 50, 0, 0, '', 'Recheck 2 years', '', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'RTC3wks', 'RTC 3 weeks', 30, 0, 0, '', '', '', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'Topo', 'Corneal Topo', 400, 0, 0, '', '', 'CPT4:92025', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'VFBLEPH', 'VF BLEPH', 130, 0, 0, '', 'Taped and untaped VF, uppers only', 'CPT4:92083', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'VFC10', 'Visual Field C-10', 100, 0, 0, '', 'Central 10 red target VF', 'CPT4:92083', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'VFC24', 'VF C-24', 110, 0, 0, '', 'Central 24 VF', 'CPT4:92083', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'VFC30', 'VF C-30', 120, 0, 0, '', 'Central 30 VF', 'CPT4:92083', 0, 0, 1, ''),
('Eye_todo_done_defaults', 'Yag', 'Surg: YAG RT/LT', 480, 0, 0, '', '', 'CPT4:66821', 0, 0, 1, '');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'Eye_Defaults_for_GENERAL', 'Eye Exam Default Values for New Providers', 1, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`, `activity`, `subtype`) VALUES
('Eye_Defaults_for_GENERAL', 'LBROW', 'no brow ptosis', 60, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'LLF', '17', 140, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'LLL', 'good tone', 40, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'LMCT', 'no masses', 80, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'LMRD', '+3', 120, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'LUL', 'normal lids and lashes', 20, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODAC', 'deep and quiet', 190, 0, 0, '', 'ANTSEG', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODAPD', '0', 280, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODCONJ', 'quiet', 160, 0, 0, '', 'ANTSEG', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODCORNEA', 'clear', 170, 0, 0, '', 'ANTSEG', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODCUP', '0.3', 450, 0, 0, '', 'RETINA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODDISC', 'pink', 430, 0, 0, '', 'RETINA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODIOPTARGET', '21', 530, 0, 0, '', 'GLAUCOMA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODIRIS', 'round', 230, 0, 0, '', 'ANTSEG', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODLENS', 'clear', 210, 0, 0, '', 'ANTSEG', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODMACULA', 'flat', 470, 0, 0, '', 'RETINA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODPERIPH', 'flat', 510, 0, 0, '', 'RETINA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODPUPILREACTIVITY', '+2', 270, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODPUPILSIZE1', '3', 250, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODPUPILSIZE2', '2', 260, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODVESSELS', '2:3', 490, 0, 0, '', 'RETINA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODVFCONFRONTATION1', '0', 330, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODVFCONFRONTATION2', '0', 340, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODVFCONFRONTATION3', '0', 350, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODVFCONFRONTATION4', '0', 360, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'ODVFCONFRONTATION5', '0', 370, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSAC', 'deep and quiet', 200, 0, 0, '', 'ANTSEG', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSAPD', '0', 320, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSCONJ', 'quiet', 150, 0, 0, '', 'ANTSEG', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSCORNEA', 'clear', 180, 0, 0, '', 'ANTSEG', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSCUP', '0.3', 460, 0, 0, '', 'RETINA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSDISC', 'pink', 440, 0, 0, '', 'RETINA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSIOPTARGET', '21', 540, 0, 0, '', 'GLAUCOMA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSIRIS', 'round', 240, 0, 0, '', 'ANTSEG', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSLENS', 'clear', 220, 0, 0, '', 'ANTSEG', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSMACULA', 'flat', 480, 0, 0, '', 'RETINA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSPERIPH', 'flat', 520, 0, 0, '', 'RETINA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSPUPILREACTIVITY', '+2', 310, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSPUPILSIZE1', '3', 290, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSPUPILSIZE2', '2', 300, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSVESSELS', '2:3', 500, 0, 0, '', 'RETINA', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSVFCONFRONTATION1', '0', 380, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSVFCONFRONTATION2', '0', 390, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSVFCONFRONTATION3', '0', 400, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSVFCONFRONTATION4', '0', 410, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'OSVFCONFRONTATION5', '0', 420, 0, 0, '', 'NEURO', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'RADNEXA', 'normal lacrimal gland and orbit', 90, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'LADNEXA', 'normal lacrimal gland and orbit', 91, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'RBROW', 'no brow ptosis', 50, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'RLF', '17', 130, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'RLL', 'good tone', 30, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'RMCT', 'no masses', 70, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'RMRD', '+3', 110, 0, 0, '', 'EXT', '', 0, 0, 0, ''),
('Eye_Defaults_for_GENERAL', 'RUL', 'normal lids and lashes', 10, 0, 0, '', 'EXT', '', 0, 0, 0, '');

INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'Eye_Lens_Material', 'Eye Lens Material', 1, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`, `activity`, `subtype`) VALUES
('Eye_Lens_Material', 'LM_CG', 'Crown Glass', 10, 0, 0, '', 'Excellent optics. Low cost. Downsides: heavy, breakable. Abbe Value: 59', '', 0, 0, 1, ''),
('Eye_Lens_Material', 'LM_CR', 'CR-39', 20, 0, 0, '', 'Excellent optics. Low cost. Downside: thickness. Abbe Value: 58', '', 0, 0, 1, ''),
('Eye_Lens_Material', 'LM_HI_PLASTICS_1', 'High-index Plastics (1.6 to 1.67)', 60, 0, 0, '', 'Thin and lightweight. Block 100 percent UV. Less costly than 1.70-1.74 high-index lenses.  Abbe: 36(1.6) - 32 (1.67)', '', 0, 0, 1, ''),
('Eye_Lens_Material', 'LM_HI_PLASTICS_2', 'High-index Plastics (1.7 to 1.74)', 70, 0, 0, '', 'The thinnest lenses available. Block 100 percent UV. Lightweight.  Abbe: 36(1.7) - 33(1.74)', '', 0, 0, 1, ''),
('Eye_Lens_Material', 'LM_POLY', 'Polycarbonate', 40, 0, 0, '', 'Superior impact resistance. Blocks 100 percent UV. Lighter than high-index plastic lenses.  Abbe: 30', '', 0, 0, 1, ''),
('Eye_Lens_Material', 'LM_TRIBRID', 'Tribrid', 50, 0, 0, '', 'Thin and lightweight. Significantly more impact-resistant than CR-39 plastic and high-index plastic lenses (except polycarbonate and Trivex). Higher Abbe value than polycarbonate. Downside: Not yet available in a wide variety of lens designs.  Abbe: 41', '', 0, 0, 1, ''),
('Eye_Lens_Material', 'LM_TRIVEX', 'Trivex', 30, 0, 0, '', 'Superior impact resistance. Blocks 100 percent UV. Higher Abbe value than polycarbonate. Lightest lens material available. Abbe Value: 45', '', 0, 0, 1, '');


INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists' ,'Eye_Lens_Treatments', 'Eye Lens Treatments', 1, 0);
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `toggle_setting_1`, `toggle_setting_2`, `activity`, `subtype`) VALUES
('Eye_Lens_Treatments', 'LT_ARCOAT', 'Anti-reflective coating', 20, 0, 0, '', '', '', 0, 0, 1, ''),
('Eye_Lens_Treatments', 'LT_ASCRATCH', 'Anti-scratch coating', 10, 0, 0, '', '', '', 0, 0, 1, ''),
('Eye_Lens_Treatments', 'LT_UVBLOCK', 'UV-blocking treatment', 30, 0, 0, '', '', '', 0, 0, 1, ''),
('Eye_Lens_Treatments', 'LT_PHOTOGREY', 'Photochromic treatment', 40, 0, 0, '', '', '', 0, 0, 1, '');

INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `activity`, `toggle_setting_1`, `toggle_setting_2`, `subtype`) VALUES('lists','Plan_of_Care_Type','Plan of Care Type','305','1','0','','','','1','0','0','');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `activity`, `toggle_setting_1`, `toggle_setting_2`, `subtype`) VALUES('Plan_of_Care_Type','appointments','Appointments','4','0','0','','INT','','1','0','0','');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `activity`, `toggle_setting_1`, `toggle_setting_2`, `subtype`) VALUES('Plan_of_Care_Type','instructions','Instructions','5','0','0','','INT','','1','0','0','');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `activity`, `toggle_setting_1`, `toggle_setting_2`, `subtype`) VALUES('Plan_of_Care_Type','plan_of_care','Plan of Care','1','0','0','','INT','','1','0','0','');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `activity`, `toggle_setting_1`, `toggle_setting_2`, `subtype`) VALUES('Plan_of_Care_Type','procedure','Procedure','3','0','0','','RQO','','1','0','0','');
INSERT INTO `list_options` (`list_id`, `option_id`, `title`, `seq`, `is_default`, `option_value`, `mapping`, `notes`, `codes`, `activity`, `toggle_setting_1`, `toggle_setting_2`, `subtype`) VALUES('Plan_of_Care_Type','test_or_order','Test/Order','2','0','0','','RQO','','1','0','0','');

-- --------------------------------------------------------

-- --------------------------------------------------------
--
-- Table structure for table `extended_log`
--

DROP TABLE IF EXISTS `extended_log`;
CREATE TABLE `extended_log` (
  `id` bigint(20) NOT NULL auto_increment,
  `date` datetime default NULL,
  `event` varchar(255) default NULL,
  `user` varchar(255) default NULL,
  `recipient` varchar(255) default NULL,
  `description` longtext,
  `patient_id` bigint(20) default NULL,
  PRIMARY KEY  (`id`),
  KEY `patient_id` (`patient_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

CREATE TABLE version (
  v_major    int(11)     NOT NULL DEFAULT 0,
  v_minor    int(11)     NOT NULL DEFAULT 0,
  v_patch    int(11)     NOT NULL DEFAULT 0,
  v_realpatch int(11)    NOT NULL DEFAULT 0,
  v_tag      varchar(31) NOT NULL DEFAULT '',
  v_database int(11)     NOT NULL DEFAULT 0,
  v_acl      int(11)     NOT NULL DEFAULT 0
) ENGINE=InnoDB;
INSERT INTO version (v_major, v_minor, v_patch, v_realpatch, v_tag, v_database, v_acl) VALUES (0, 0, 0, 0, '', 0, 0);
-- --------------------------------------------------------

--
-- Table structure for table `customlists`
--

CREATE TABLE `customlists` (
  `cl_list_slno` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `cl_list_id` int(10) unsigned NOT NULL COMMENT 'ID OF THE lIST FOR NEW TAKE SELECT MAX(cl_list_id)+1',
  `cl_list_item_id` int(10) unsigned DEFAULT NULL COMMENT 'ID OF THE lIST FOR NEW TAKE SELECT MAX(cl_list_item_id)+1',
  `cl_list_type` int(10) unsigned NOT NULL COMMENT '0=>List Name 1=>list items 2=>Context 3=>Template 4=>Sentence 5=> SavedTemplate 6=>CustomButton',
  `cl_list_item_short` varchar(10) DEFAULT NULL,
  `cl_list_item_long` text,
  `cl_list_item_level` int(11) DEFAULT NULL COMMENT 'Flow level for List Designation',
  `cl_order` int(11) DEFAULT NULL,
  `cl_deleted` tinyint(1) DEFAULT '0',
  `cl_creator` int(11) DEFAULT NULL,
  PRIMARY KEY (`cl_list_slno`)
) ENGINE=InnoDB AUTO_INCREMENT=1;
INSERT INTO customlists(cl_list_id,cl_list_type,cl_list_item_long) VALUES (1,2,'Subjective');
INSERT INTO customlists(cl_list_id,cl_list_type,cl_list_item_long) VALUES (2,2,'Objective');
INSERT INTO customlists(cl_list_id,cl_list_type,cl_list_item_long) VALUES (3,2,'Assessment');
INSERT INTO customlists(cl_list_id,cl_list_type,cl_list_item_long) VALUES (4,2,'Plan');
-- --------------------------------------------------------

--
-- Table structure for table `template_users`
--

CREATE TABLE `template_users` (
  `tu_id` int(11) NOT NULL AUTO_INCREMENT,
  `tu_user_id` int(11) DEFAULT NULL,
  `tu_facility_id` int(11) DEFAULT NULL,
  `tu_template_id` int(11) DEFAULT NULL,
  `tu_template_order` int(11) DEFAULT NULL,
  PRIMARY KEY (`tu_id`),
  UNIQUE KEY `templateuser` (`tu_user_id`,`tu_template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1;

CREATE TABLE `product_warehouse` (
  `pw_drug_id`   int(11) NOT NULL,
  `pw_warehouse` varchar(31) NOT NULL,
  `pw_min_level` float       DEFAULT 0,
  `pw_max_level` float       DEFAULT 0,
  PRIMARY KEY  (`pw_drug_id`,`pw_warehouse`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

--
-- Table structure for table `misc_address_book`
--

CREATE TABLE `misc_address_book` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fname` varchar(255) DEFAULT NULL,
  `mname` varchar(255) DEFAULT NULL,
  `lname` varchar(255) DEFAULT NULL,
  `street` varchar(60) DEFAULT NULL,
  `city` varchar(30) DEFAULT NULL,
  `state` varchar(30) DEFAULT NULL,
  `zip` varchar(20) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

-- --------------------------------------------------------

-- --------------------------------------------------------
--
-- Table structure for table `esign_signatures`
--

CREATE TABLE `esign_signatures` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tid` int(11) NOT NULL COMMENT 'Table row ID for signature',
  `table` varchar(255) NOT NULL COMMENT 'table name for the signature',
  `uid` int(11) NOT NULL COMMENT 'user id for the signing user',
  `datetime` datetime NOT NULL COMMENT 'datetime of the signature action',
  `is_lock` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'sig, lock or amendment',
  `amendment` text COMMENT 'amendment text, if any',
  `hash` varchar(255) NOT NULL COMMENT 'hash of signed data',
  `signature_hash` varchar(255) NOT NULL COMMENT 'hash of signature itself',
  PRIMARY KEY (`id`),
  KEY `tid` (`tid`),
  KEY `table` (`table`)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;

--
-- Table structure for table `log_comment_encrypt`
--

DROP TABLE IF EXISTS `log_comment_encrypt`;
CREATE TABLE IF NOT EXISTS `log_comment_encrypt` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `log_id` int(11) NOT NULL,
  `encrypt` enum('Yes','No') NOT NULL DEFAULT 'No',
  `checksum` longtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

CREATE TABLE `shared_attributes` (
  `pid`          bigint(20)   NOT NULL,
  `encounter`    bigint(20)   NOT NULL COMMENT '0 if patient attribute, else encounter attribute',
  `field_id`     varchar(31)  NOT NULL COMMENT 'references layout_options.field_id',
  `last_update`  datetime     NOT NULL COMMENT 'time of last update',
  `user_id`      bigint(20)   NOT NULL COMMENT 'user who last updated',
  `field_value`  TEXT,
  PRIMARY KEY (`pid`, `encounter`, `field_id`)
);

-- --------------------------------------------------------

-- --------------------------------------------------------
--
-- Table structure for table `ccda_components`
--
CREATE TABLE ccda_components (
  ccda_components_id int(11) NOT NULL AUTO_INCREMENT,
  ccda_components_field varchar(100) DEFAULT NULL,
  ccda_components_name varchar(100) DEFAULT NULL,
  ccda_type int(11) NOT NULL COMMENT '0=>sections,1=>components',
  PRIMARY KEY (ccda_components_id)
) ENGINE=InnoDB AUTO_INCREMENT=23 ;
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('1','progress_note','Progress Notes',0);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('2','consultation_note','Consultation Note',0);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('3','continuity_care_document','Continuity Care Document',0);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('4','diagnostic_image_reporting','Diagnostic Image Reporting',0);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('5','discharge_summary','Discharge Summary',0);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('6','history_physical_note','History and Physical Note',0);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('7','operative_note','Operative Note',0);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('8','procedure_note','Procedure Note',0);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('9','unstructured_document','Unstructured Document',0);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('10','allergies','Allergies',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('11','medications','Medications',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('12','problems','Problems',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('13','immunizations','Immunizations',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('14','procedures','Procedures',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('15','results','Results',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('16','plan_of_care','Plan Of Care',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('17','vitals','Vitals',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('18','social_history','Social History',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('19','encounters','Encounters',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('20','functional_status','Functional Status',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('21','referral','Reason for Referral',1);
insert into ccda_components (ccda_components_id, ccda_components_field, ccda_components_name, ccda_type) values ('22','instructions','Instructions',1);
-- --------------------------------------------------------


-- --------------------------------------------------------
--
-- Table structure for table `ccda_sections`
--
CREATE TABLE ccda_sections (
  ccda_sections_id int(11) NOT NULL AUTO_INCREMENT,
  ccda_components_id int(11) DEFAULT NULL,
  ccda_sections_field varchar(100) DEFAULT NULL,
  ccda_sections_name varchar(100) DEFAULT NULL,
  ccda_sections_req_mapping tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (ccda_sections_id)
) ENGINE=InnoDB AUTO_INCREMENT=46 ;
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('1','1','assessment_plan','Assessment and Plan','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('2','2','assessment_plan','Assessment and Plan','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('3','2','history_of_present_illness','History of Present Illness','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('4','2','physical_exam','Physical Exam','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('5','2','reason_of_visit','Reason for Referral/Reason for Visit','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('6','3','allergies','Allergies','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('7','3','medications','Medications','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('8','3','problem_list','Problem List','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('9','3','procedures','Procedures','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('10','3','results','Results','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('11','4','report','Report','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('12','5','allergies','Allergies','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('13','5','hospital_course','Hospital Course','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('14','5','hospital_discharge_diagnosis','Hospital Discharge Diagnosis','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('15','5','hospital_discharge_medications','Hospital Discharge Medications','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('16','5','plan_of_care','Plan of Care','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('17','6','allergies','Allergies','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('19','6','chief_complaint','Chief Complaint / Reason for Visit','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('21','6','family_history','Family History','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('22','6','general_status','General Status','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('23','6','hpi_past_med','History of Past Illness (Past Medical History)','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('24','6','hpi','History of Present Illness','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('25','6','medications','Medications','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('26','6','physical_exam','Physical Exam','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('28','6','results','Results','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('29','6','review_of_systems','Review of Systems','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('30','6','social_history','Social History','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('31','6','vital_signs','Vital Signs','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('32','7','anesthesia','Anesthesia','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('33','7','complications','Complications','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('34','7','post_operative_diagnosis','Post Operative Diagnosis','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('35','7','pre_operative_diagnosis','Pre Operative Diagnosis','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('36','7','procedure_estimated_blood_loss','Procedure Estimated Blood Loss','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('37','7','procedure_findings','Procedure Findings','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('38','7','procedure_specimens_taken','Procedure Specimens Taken','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('39','7','procedure_description','Procedure Description','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('40','8','assessment_plan','Assessment and Plan','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('41','8','complications','Complications','1');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('42','8','postprocedure_diagnosis','Postprocedure Diagnosis','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('43','8','procedure_description','Procedure Description','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('44','8','procedure_indications','Procedure Indications','0');
insert into ccda_sections (ccda_sections_id, ccda_components_id, ccda_sections_field, ccda_sections_name, ccda_sections_req_mapping) values('45','9','unstructured_doc','Document','0');
-- --------------------------------------------------------


-- --------------------------------------------------------
--
-- Table structure for table `ccda_field_mapping`
--
CREATE TABLE ccda_field_mapping (
  id int(11) NOT NULL AUTO_INCREMENT,
  table_id int(11) DEFAULT NULL,
  ccda_field varchar(100) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;
-- --------------------------------------------------------


-- --------------------------------------------------------
--
-- Table structure for table `ccda`
--
CREATE TABLE ccda (
  id INT(11) NOT NULL AUTO_INCREMENT,
  pid BIGINT(20) DEFAULT NULL,
  encounter BIGINT(20) DEFAULT NULL,
  ccda_data MEDIUMTEXT,
  time VARCHAR(50) DEFAULT NULL,
  status SMALLINT(6) DEFAULT NULL,
  updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  user_id VARCHAR(50) null,
  couch_docid VARCHAR(100) NULL,
  couch_revid VARCHAR(100) NULL,
  `view` tinyint(4) NOT NULL DEFAULT '0',
  `transfer` tinyint(4) NOT NULL DEFAULT '0',
  `emr_transfer` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  UNIQUE KEY unique_key (pid,encounter,time)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;
-- --------------------------------------------------------


-- --------------------------------------------------------
--
-- Table structure for table `ccda_table_mapping`
--
CREATE TABLE ccda_table_mapping (
  id int(11) NOT NULL AUTO_INCREMENT,
  ccda_component varchar(100) DEFAULT NULL,
  ccda_component_section varchar(100) DEFAULT NULL,
  form_dir varchar(100) DEFAULT NULL,
  form_type smallint(6) DEFAULT NULL,
  form_table varchar(100) DEFAULT NULL,
  user_id int(11) DEFAULT NULL,
  deleted tinyint(4) NOT NULL DEFAULT '0',
  timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=1 ;
-- --------------------------------------------------------

-- --------------------------------------------------------
--
-- Table structure for table `external_procedures`
--
CREATE TABLE `external_procedures` (
  `ep_id` int(11) NOT NULL AUTO_INCREMENT,
  `ep_date` date DEFAULT NULL,
  `ep_code_type` varchar(20) DEFAULT NULL,
  `ep_code` varchar(9) DEFAULT NULL,
  `ep_pid` int(11) DEFAULT NULL,
  `ep_encounter` int(11) DEFAULT NULL,
  `ep_code_text` longtext,
  `ep_facility_id` varchar(255) DEFAULT NULL,
  `ep_external_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ep_id`)
) ENGINE=InnoDB;
-- --------------------------------------------------------

-- --------------------------------------------------------
--
-- Table structure for table `external_encounters`
--
CREATE TABLE `external_encounters` (
  `ee_id` int(11) NOT NULL AUTO_INCREMENT,
  `ee_date` date DEFAULT NULL,
  `ee_pid` int(11) DEFAULT NULL,
  `ee_provider_id` varchar(255) DEFAULT NULL,
  `ee_facility_id` varchar(255) DEFAULT NULL,
  `ee_encounter_diagnosis` varchar(255) DEFAULT NULL,
  `ee_external_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ee_id`)
) ENGINE=InnoDB;
-- --------------------------------------------------------

-- --------------------------------------------------------
--
-- Table structure for table `form_care_plan`
--
CREATE TABLE `form_care_plan` (
  `id` bigint(20) NOT NULL,
  `date` date DEFAULT NULL,
  `pid` bigint(20) DEFAULT NULL,
  `encounter` varchar(255) DEFAULT NULL,
  `user` varchar(255) DEFAULT NULL,
  `groupname` varchar(255) DEFAULT NULL,
  `authorized` tinyint(4) DEFAULT NULL,
  `activity` tinyint(4) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `codetext` text,
  `description` text,
  `external_id` varchar(30) DEFAULT NULL,
  `care_plan_type` varchar(30) DEFAULT NULL
) ENGINE=InnoDB;
-- --------------------------------------------------------

-- --------------------------------------------------------
--
-- Table structure for table `form_functional_cognitive_status`
--
CREATE TABLE `form_functional_cognitive_status` (
  `id` bigint(20) NOT NULL,
  `date` date DEFAULT NULL,
  `pid` bigint(20) DEFAULT NULL,
  `encounter` varchar(255) DEFAULT NULL,
  `user` varchar(255) DEFAULT NULL,
  `groupname` varchar(255) DEFAULT NULL,
  `authorized` tinyint(4) DEFAULT NULL,
  `activity` tinyint(4) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `codetext` text,
  `description` text,
  `external_id` varchar(30) DEFAULT NULL
) ENGINE=InnoDB;
-- --------------------------------------------------------

-- --------------------------------------------------------
--
-- Table structure for table `form_observation`
--
CREATE TABLE `form_observation` (
  `id` bigint(20) NOT NULL,
  `date` DATE DEFAULT NULL,
  `pid` bigint(20) DEFAULT NULL,
  `encounter` varchar(255) DEFAULT NULL,
  `user` varchar(255) DEFAULT NULL,
  `groupname` varchar(255) DEFAULT NULL,
  `authorized` tinyint(4) DEFAULT NULL,
  `activity` tinyint(4) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `ob_value` varchar(255),
  `ob_unit` varchar(255),
  `description` varchar(255),
  `code_type` varchar(255),
  `table_code` varchar(255)
) ENGINE=InnoDB;
-- --------------------------------------------------------

-- --------------------------------------------------------
--
-- Table structure for table `form_clinical_instructions`
--
CREATE TABLE `form_clinical_instructions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `pid` bigint(20) DEFAULT NULL,
  `encounter` varchar(255) DEFAULT NULL,
  `user` varchar(255) DEFAULT NULL,
  `instruction` text,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `activity` TINYINT DEFAULT 1 NULL,
  PRIMARY KEY (`id`)
)ENGINE=InnoDB;
-- --------------------------------------------------------

-- --------------------------------------------------------
--
-- Table structure for table 'valueset'
--
CREATE TABLE `valueset` (
  `nqf_code` varchar(255) NOT NULL DEFAULT '',
  `code` varchar(255) NOT NULL DEFAULT '',
  `code_system` varchar(255) NOT NULL DEFAULT '',
  `code_type` varchar(255) DEFAULT NULL,
  `valueset` varchar(255) NOT NULL DEFAULT '',
  `description` varchar(255) DEFAULT NULL,
  `valueset_name` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`nqf_code`,`code`,`valueset`)
) ENGINE=InnoDB;

-- ------------------------------------------------------
-- Table structure for table `immunization_observation`
--
CREATE TABLE `immunization_observation` (
  `imo_id` int(11) NOT NULL AUTO_INCREMENT,
  `imo_im_id` int(11) NOT NULL,
  `imo_pid` int(11) DEFAULT NULL,
  `imo_criteria` varchar(255) DEFAULT NULL,
  `imo_criteria_value` varchar(255) DEFAULT NULL,
  `imo_user` int(11) DEFAULT NULL,
  `imo_code` varchar(255) DEFAULT NULL,
  `imo_codetext` varchar(255) DEFAULT NULL,
  `imo_codetype` varchar(255) DEFAULT NULL,
  `imo_vis_date_published` date DEFAULT NULL,
  `imo_vis_date_presented` date DEFAULT NULL,
  `imo_date_observation` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`imo_id`)
) ENGINE=InnoDB;
-- --------------------------------------------------------
-- --------------------------------------------------------
--
-- Table structure for table 'calendar external'
--
CREATE TABLE calendar_external (
  `id` INT NOT NULL AUTO_INCREMENT,
  `date` DATE NOT NULL,
  `description` VARCHAR(45) NOT NULL,
  `source` VARCHAR(45) NULL,
  PRIMARY KEY (`id`)) ENGINE=InnoDB;

-- --------------------------------------------------------
--
-- Tables for Eye Module
--
DROP TABLE IF EXISTS `form_eye_mag_dispense`;
CREATE TABLE `form_eye_mag_dispense` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
`encounter` bigint(20) NULL,
`pid` bigint(20) DEFAULT NULL,
`user` varchar(255) DEFAULT NULL,
`groupname` varchar(255) DEFAULT NULL,
`authorized` tinyint(4) DEFAULT NULL,
`activity` tinyint(4) DEFAULT NULL,
`REFDATE` DATETIME NULL DEFAULT NULL,
`REFTYPE` varchar(10) DEFAULT NULL,
`RXTYPE` varchar(20)DEFAULT NULL,
`ODSPH` varchar(10) DEFAULT NULL,
`ODCYL` varchar(10) DEFAULT NULL,
`ODAXIS` varchar(10) DEFAULT NULL,
`OSSPH` varchar(10) DEFAULT NULL,
`OSCYL` varchar(10) DEFAULT NULL,
`OSAXIS` varchar(10) DEFAULT NULL,
`ODMIDADD` varchar(10) DEFAULT NULL,
`OSMIDADD` varchar(10) DEFAULT NULL,
`ODADD` varchar(10) DEFAULT NULL,
`OSADD` varchar(10) DEFAULT NULL,
`ODHPD` varchar(20) DEFAULT NULL,
`ODHBASE` varchar(20) DEFAULT NULL,
`ODVPD` varchar(20) DEFAULT NULL,
`ODVBASE` varchar(20) DEFAULT NULL,
`ODSLABOFF` varchar(20) DEFAULT NULL,
`ODVERTEXDIST` varchar(20) DEFAULT NULL,
`OSHPD` varchar(20) DEFAULT NULL,
`OSHBASE` varchar(20) DEFAULT NULL,
`OSVPD` varchar(20) DEFAULT NULL,
`OSVBASE` varchar(20) DEFAULT NULL,
`OSSLABOFF` varchar(20) DEFAULT NULL,
`OSVERTEXDIST` varchar(20) DEFAULT NULL,
`ODMPDD` varchar(20) DEFAULT NULL,
`ODMPDN` varchar(20) DEFAULT NULL,
`OSMPDD` varchar(20) DEFAULT NULL,
`OSMPDN` varchar(20) DEFAULT NULL,
`BPDD` varchar(20) DEFAULT NULL,
`BPDN` varchar(20) DEFAULT NULL,
`LENS_MATERIAL` varchar(20) DEFAULT NULL,
`LENS_TREATMENTS` varchar(100) DEFAULT NULL,
`CTLMANUFACTUREROD` varchar(25) DEFAULT NULL,
`CTLMANUFACTUREROS` varchar(25) DEFAULT NULL,
`CTLSUPPLIEROD` varchar(25) DEFAULT NULL,
`CTLSUPPLIEROS` varchar(25) DEFAULT NULL,
`CTLBRANDOD` varchar(50) DEFAULT NULL,
`CTLBRANDOS` varchar(50) DEFAULT NULL,
`ODDIAM` varchar(50) DEFAULT NULL,
`ODBC` varchar(50) DEFAULT NULL,
`OSDIAM` varchar(50) DEFAULT NULL,
`OSBC` varchar(50) DEFAULT NULL,
`RXCOMMENTS` text,
`COMMENTS` text,
PRIMARY KEY (`id`),
UNIQUE KEY `pid` (`pid`,`encounter`,`id`)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `form_eye_mag`;
CREATE TABLE `form_eye_mag` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`date` datetime DEFAULT NULL,
`pid` bigint(20) DEFAULT NULL,
`user` varchar(255) DEFAULT NULL,
`groupname` varchar(255) DEFAULT NULL,
`authorized` tinyint(4) DEFAULT NULL,
`activity` tinyint(4) DEFAULT NULL,
`Narrative` text,
`VISITTYPE` varchar(50) DEFAULT NULL,
`CC1` text,
`HPI1` text,
`QUALITY1` text,
`TIMING1` text,
`DURATION1` text,
`CONTEXT1` text,
`SEVERITY1` text,
`MODIFY1` text,
`ASSOCIATED1` text,
`LOCATION1` text,
`CHRONIC1`  text,
`CHRONIC2`text,
`CHRONIC3`text,
`CC2` text,
`HPI2` text,
`QUALITY2` text,
`TIMING2` text,
`DURATION2` text,
`CONTEXT2` text,
`SEVERITY2` text,
`MODIFY2` text,
`ASSOCIATED2` text,
`LOCATION2` text,
`CC3` text,
`HPI3` text,
`QUALITY3` text,
`TIMING3` text,
`DURATION3` text,
`CONTEXT3` text,
`SEVERITY3` text,
`MODIFY3` text,
`ASSOCIATED3` text,
`LOCATION3` text,
`ROSGENERAL` text,
`ROSHEENT` text,
`ROSCV` text,
`ROSPULM` text,
`ROSGI` text,
`ROSGU` text,
`ROSDERM` text,
`ROSNEURO` text,
`ROSPSYCH` text,
`ROSMUSCULO` text,
`ROSIMMUNO` text,
`ROSENDOCRINE` text,
`alert` char(3) DEFAULT 'yes',
`oriented` char(3) DEFAULT 'TPP',
`confused` char(3) DEFAULT 'nml',
`SCODVA` varchar(20) DEFAULT NULL,
`SCOSVA` varchar(20) DEFAULT NULL,
`PHODVA` varchar(20) DEFAULT NULL,
`PHOSVA` varchar(20) DEFAULT NULL,
`WODVA` varchar(20) DEFAULT NULL,
`WOSVA` varchar(20) DEFAULT NULL,
`CTLODVA` varchar(20) DEFAULT NULL,
`CTLOSVA` varchar(20) DEFAULT NULL,
`MRODVA` varchar(20) DEFAULT NULL,
`MROSVA` varchar(20) DEFAULT NULL,
`SCNEARODVA` varchar(20) DEFAULT NULL,
`SCNEAROSVA` varchar(20) DEFAULT NULL,
`WNEARODVA` varchar(10) DEFAULT NULL,
`WNEAROSVA` varchar(10) DEFAULT NULL,
`MRNEARODVA` varchar(20) DEFAULT NULL,
`MRNEAROSVA` varchar(20) DEFAULT NULL,
`GLAREODVA` varchar(20) DEFAULT NULL,
`GLAREOSVA` varchar(20) DEFAULT NULL,
`GLARECOMMENTS` varchar(100) DEFAULT NULL,
`ARODVA` varchar(20) DEFAULT NULL,
`AROSVA` varchar(20) DEFAULT NULL,
`CRODVA` varchar(20) DEFAULT NULL,
`CROSVA` varchar(20) DEFAULT NULL,
`CTLODVA1` varchar(20) DEFAULT NULL,
`CTLOSVA1` varchar(20) DEFAULT NULL,
`PAMODVA` varchar(20) DEFAULT NULL,
`PAMOSVA` varchar(20) DEFAULT NULL,
`LIODVA` varchar(20) DEFAULT NULL,
`LIOSVA` varchar(20) DEFAULT NULL,
`NVOCHECKED` varchar(20) DEFAULT NULL,
`ADDCHECKED` varchar(20) DEFAULT NULL,
`MRODSPH` varchar(20) DEFAULT NULL,
`MRODCYL` varchar(20) DEFAULT NULL,
`MRODAXIS` varchar(20) DEFAULT NULL,
`MRODPRISM` varchar(20) DEFAULT NULL,
`MRODBASE` varchar(20) DEFAULT NULL,
`MRODADD` varchar(20) DEFAULT NULL,
`MROSSPH` varchar(20) DEFAULT NULL,
`MROSCYL` varchar(20) DEFAULT NULL,
`MROSAXIS` varchar(20) DEFAULT NULL,
`MROSPRISM` varchar(20) DEFAULT NULL,
`MROSBASE` varchar(20) DEFAULT NULL,
`MROSADD` varchar(20) DEFAULT NULL,
`MRODNEARSPHERE` varchar(20) DEFAULT NULL,
`MRODNEARCYL` varchar(20) DEFAULT NULL,
`MRODNEARAXIS` varchar(20) DEFAULT NULL,
`MRODPRISMNEAR` varchar(20) DEFAULT NULL,
`MRODBASENEAR` varchar(20) DEFAULT NULL,
`MROSNEARSHPERE` varchar(20) DEFAULT NULL,
`MROSNEARCYL` varchar(20) DEFAULT NULL,
`MROSNEARAXIS` varchar(20) DEFAULT NULL,
`MROSPRISMNEAR` varchar(20) DEFAULT NULL,
`MROSBASENEAR` varchar(20) DEFAULT NULL,
`CRODSPH` varchar(20) DEFAULT NULL,
`CRODCYL` varchar(20) DEFAULT NULL,
`CRODAXIS` varchar(20) DEFAULT NULL,
`CROSSPH` varchar(20) DEFAULT NULL,
`CROSCYL` varchar(20) DEFAULT NULL,
`CROSAXIS` varchar(20) DEFAULT NULL,
`CRCOMMENTS` varchar(255) DEFAULT NULL,
`BALANCED` varchar(2) DEFAULT NULL,
`DIL_RISKS` varchar(2) DEFAULT 'on',
`WETTYPE` VARCHAR(10) DEFAULT NULL,
`ATROPINE` VARCHAR(25) DEFAULT NULL,
`CYCLOMYDRIL` VARCHAR(25) DEFAULT NULL,
`TROPICAMIDE` VARCHAR(25) DEFAULT NULL,
`CYCLOGYL` VARCHAR(25) DEFAULT NULL,
`NEO25` VARCHAR(25) DEFAULT NULL,
`ARODSPH` varchar(10) DEFAULT NULL,
`ARODCYL` varchar(10) DEFAULT NULL,
`ARODAXIS` varchar(10) DEFAULT NULL,
`AROSSPH` varchar(10) DEFAULT NULL,
`AROSCYL` varchar(10) DEFAULT NULL,
`AROSAXIS` varchar(10) DEFAULT NULL,
`ARODADD` varchar(10) DEFAULT NULL,
`AROSADD` varchar(10) DEFAULT NULL,
`ARNEARODVA` varchar(10) DEFAULT NULL,
`ARNEAROSVA` varchar(10) DEFAULT NULL,
`ARODPRISM` varchar(20) DEFAULT NULL,
`AROSPRISM` varchar(20) DEFAULT NULL,
`CTLODSPH` varchar(50) DEFAULT NULL,
`CTLODCYL` varchar(50) DEFAULT NULL,
`CTLODAXIS` varchar(50) DEFAULT NULL,
`CTLODBC` varchar(50) DEFAULT NULL,
`CTLODDIAM` varchar(50) DEFAULT NULL,
`CTLOSSPH` varchar(50) DEFAULT NULL,
`CTLOSCYL` varchar(50) DEFAULT NULL,
`CTLOSAXIS` varchar(50) DEFAULT NULL,
`CTLOSBC` varchar(50) DEFAULT NULL,
`CTLOSDIAM` varchar(50) DEFAULT NULL,
`CTL_COMMENTS` text,
`CTLMANUFACTUREROD` varchar(50) DEFAULT NULL,
`CTLSUPPLIEROD` varchar(50) DEFAULT NULL,
`CTLBRANDOD` varchar(50) DEFAULT NULL,
`CTLMANUFACTUREROS` varchar(50) DEFAULT NULL,
`CTLSUPPLIEROS` varchar(50) DEFAULT NULL,
`CTLBRANDOS` varchar(50) DEFAULT NULL,
`CTLODADD` varchar(50) DEFAULT NULL,
`CTLOSADD` varchar(50) DEFAULT NULL,
`ODIOPAP` varchar(50) DEFAULT NULL,
`OSIOPAP` varchar(50) DEFAULT NULL,
`ODIOPTPN` varchar(10) DEFAULT NULL,
`OSIOPTPN` varchar(10) DEFAULT NULL,
`ODIOPFTN` varchar(10) DEFAULT NULL,
`OSIOPFTN` varchar(10) DEFAULT NULL,
`ODIOPPOST`varchar(10) DEFAULT NULL,
`OSIOPPOST` varchar(10) DEFAULT NULL,
`ODIOPTARGET`varchar(10) DEFAULT NULL,
`OSIOPTARGET` varchar(10) DEFAULT NULL,
`IOPTIME` time DEFAULT NULL,
`IOPPOSTTIME` time DEFAULT NULL,
`AMSLEROD` smallint(1) DEFAULT NULL,
`AMSLEROS` smallint(1) DEFAULT NULL,
`ODK1` varchar(50) DEFAULT NULL,
`ODK2` varchar(50) DEFAULT NULL,
`ODK2AXIS` varchar(50) DEFAULT NULL,
`OSK1` varchar(50) DEFAULT NULL,
`OSK2` varchar(50) DEFAULT NULL,
`OSK2AXIS` varchar(50) DEFAULT NULL,
`ODAXIALLENGTH` varchar(50) DEFAULT NULL,
`OSAXIALLENGTH` varchar(50) DEFAULT NULL,
`ODACD` varchar(50) DEFAULT NULL,
`OSACD` varchar(50) DEFAULT NULL,
`ODW2W` varchar(10) DEFAULT NULL,
`OSW2W` varchar(10) DEFAULT NULL,
`ODLT` varchar(20) DEFAULT NULL,
`OSLT` varchar(20) DEFAULT NULL,
`ODPDMeasured` varchar(25) DEFAULT NULL,
`OSPDMeasured` varchar(25) DEFAULT NULL,
`ACT` char(3) DEFAULT 'on',
`ACT1CCDIST` varchar(50) DEFAULT NULL,
`ACT2CCDIST` varchar(50) DEFAULT NULL,
`ACT3CCDIST` varchar(50) DEFAULT NULL,
`ACT4CCDIST` varchar(50) DEFAULT NULL,
`ACT5CCDIST` varchar(50) DEFAULT NULL,
`ACT6CCDIST` varchar(50) DEFAULT NULL,
`ACT7CCDIST` varchar(50) DEFAULT NULL,
`ACT8CCDIST` varchar(50) DEFAULT NULL,
`ACT9CCDIST` varchar(50) DEFAULT NULL,
`ACT10CCDIST` varchar(50) DEFAULT NULL,
`ACT11CCDIST` varchar(50) DEFAULT NULL,
`ACT1SCDIST` varchar(50) DEFAULT NULL,
`ACT2SCDIST` varchar(50) DEFAULT NULL,
`ACT3SCDIST` varchar(50) DEFAULT NULL,
`ACT4SCDIST` varchar(50) DEFAULT NULL,
`ACT5SCDIST` varchar(50) DEFAULT NULL,
`ACT6SCDIST` varchar(50) DEFAULT NULL,
`ACT7SCDIST` varchar(50) DEFAULT NULL,
`ACT8SCDIST` varchar(50) DEFAULT NULL,
`ACT9SCDIST` varchar(50) DEFAULT NULL,
`ACT10SCDIST` varchar(50) DEFAULT NULL,
`ACT11SCDIST` varchar(50) DEFAULT NULL,
`ACT1SCNEAR` varchar(50) DEFAULT NULL,
`ACT2SCNEAR` varchar(50) DEFAULT NULL,
`ACT3SCNEAR` varchar(50) DEFAULT NULL,
`ACT4SCNEAR` varchar(50) DEFAULT NULL,
`ACT5SCNEAR` varchar(50) DEFAULT NULL,
`ACT6SCNEAR` varchar(50) DEFAULT NULL,
`ACT7SCNEAR` varchar(50) DEFAULT NULL,
`ACT8SCNEAR` varchar(50) DEFAULT NULL,
`ACT9SCNEAR` varchar(50) DEFAULT NULL,
`ACT10SCNEAR` varchar(50) DEFAULT NULL,
`ACT11SCNEAR` varchar(50) DEFAULT NULL,
`ACT1CCNEAR` varchar(50) DEFAULT NULL,
`ACT2CCNEAR` varchar(50) DEFAULT NULL,
`ACT3CCNEAR` varchar(50) DEFAULT NULL,
`ACT4CCNEAR` varchar(50) DEFAULT NULL,
`ACT5CCNEAR` varchar(50) DEFAULT NULL,
`ACT6CCNEAR` varchar(50) DEFAULT NULL,
`ACT7CCNEAR` varchar(50) DEFAULT NULL,
`ACT8CCNEAR` varchar(50) DEFAULT NULL,
`ACT9CCNEAR` varchar(50) DEFAULT NULL,
`ACT10CCNEAR` varchar(50) DEFAULT NULL,
`ACT11CCNEAR` varchar(50) DEFAULT NULL,
`ODVF1` tinyint(1) DEFAULT NULL,
`ODVF2` tinyint(1) DEFAULT NULL,
`ODVF3` tinyint(1) DEFAULT NULL,
`ODVF4` tinyint(1) DEFAULT NULL,
`OSVF1` tinyint(1) DEFAULT NULL,
`OSVF2` tinyint(1) DEFAULT NULL,
`OSVF3` tinyint(1) DEFAULT NULL,
`OSVF4` tinyint(1) DEFAULT NULL,
`MOTILITYNORMAL` char(3) DEFAULT 'on',
`MOTILITY_RS` int(1) DEFAULT NULL,
`MOTILITY_RI` int(1) DEFAULT NULL,
`MOTILITY_RR` int(1) DEFAULT NULL,
`MOTILITY_RL` int(1) DEFAULT NULL,
`MOTILITY_LS` int(1) DEFAULT NULL,
`MOTILITY_LI` int(1) DEFAULT NULL,
`MOTILITY_LR` int(1) DEFAULT NULL,
`MOTILITY_LL` int(1) DEFAULT NULL,
`MOTILITY_RRSO` int(1) DEFAULT NULL,
`MOTILITY_RLSO` int(1) DEFAULT NULL,
`MOTILITY_RRIO` int(1) DEFAULT NULL,
`MOTILITY_RLIO` int(1) DEFAULT NULL,
`MOTILITY_LRSO` int(1) DEFAULT NULL,
`MOTILITY_LLSO` int(1) DEFAULT NULL,
`MOTILITY_LRIO` int(1) DEFAULT NULL,
`MOTILITY_LLIO` int(1) DEFAULT NULL,
`STEREOPSIS` varchar(25) DEFAULT NULL,
`ODNPA` varchar(50) DEFAULT NULL,
`OSNPA` varchar(50) DEFAULT NULL,
`VERTFUSAMPS` varchar(50) DEFAULT NULL,
`DIVERGENCEAMPS` varchar(50) DEFAULT NULL,
`NPC` varchar(10) DEFAULT NULL,
`DACCDIST` varchar(10) DEFAULT NULL,
`DACCNEAR` varchar(10) DEFAULT NULL,
`CACCDIST` varchar(10) DEFAULT NULL,
`CACCNEAR` varchar(10) DEFAULT NULL,
`ODCOLOR` varchar(5) DEFAULT NULL,
`OSCOLOR` varchar(5) DEFAULT NULL,
`ODCOINS` varchar(5) DEFAULT NULL,
`OSCOINS` varchar(5) DEFAULT NULL,
`ODREDDESAT` varchar(10) DEFAULT NULL,
`OSREDDESAT` varchar(10) DEFAULT NULL,
`NEURO_COMMENTS` text,
`RUL` text,
`LUL` text,
`RLL` text,
`LLL` text,
`RBROW` text,
`LBROW` text,
`RMCT` text,
`LMCT` text,
`RADNEXA` varchar(255) DEFAULT NULL,
`LADNEXA` varchar(255) DEFAULT NULL,
`RMRD` varchar(25) DEFAULT NULL,
`LMRD` varchar(25) DEFAULT NULL,
`RLF` varchar(50) DEFAULT NULL,
`LLF` varchar(50) DEFAULT NULL,
`RVFISSURE` varchar(10) DEFAULT NULL,
`LVFISSURE` varchar(10) DEFAULT NULL,
`ODHERTEL` varchar(10) DEFAULT NULL,
`OSHERTEL` varchar(10) DEFAULT NULL,
`HERTELBASE` varchar(10) DEFAULT NULL,
`RCAROTID` varchar(50) DEFAULT NULL,
`LCAROTID` varchar(50) DEFAULT NULL,
`RTEMPART` varchar(50) DEFAULT NULL,
`LTEMPART` varchar(50) DEFAULT NULL,
`RCNV` varchar(50) DEFAULT NULL,
`LCNV` varchar(50) DEFAULT NULL,
`RCNVII` varchar(50) DEFAULT NULL,
`LCNVII` varchar(50) DEFAULT NULL,
`EXT_COMMENTS` text,
`ODSCHIRMER1` varchar(50) DEFAULT NULL,
`OSSCHRIMER1` varchar(50) DEFAULT NULL,
`ODSCHRIMER2` varchar(50) DEFAULT NULL,
`OSSCHRIMER2` varchar(50) DEFAULT NULL,
`OSCONJ` text,
`ODCONJ` text,
`ODCORNEA` text,
`OSCORNEA` text,
`ODAC` text,
`OSAC` text,
`ODLENS` text,
`OSLENS` text,
`ODIRIS` text,
`OSIRIS` text,
`ODKTHICKNESS` varchar(20) DEFAULT NULL,
`OSKTHICKNESS` varchar(20) DEFAULT NULL,
`ODGONIO` varchar(50) DEFAULT NULL,
`OSGONIO` varchar(50) DEFAULT NULL,
`ANTSEG_COMMENTS` text,
`PUPIL_NORMAL` varchar(2) DEFAULT '1',
`ODPUPILSIZE1` varchar(20) DEFAULT NULL,
`ODPUPILSIZE2` varchar(20) DEFAULT NULL,
`ODPUPILREACTIVITY` varchar(10) DEFAULT NULL,
`ODAPD` varchar(10) DEFAULT NULL,
`OSPUPILSIZE1` varchar(20) DEFAULT NULL,
`OSPUPILSIZE2` varchar(20) DEFAULT NULL,
`OSPUPILREACTIVITY` varchar(10) DEFAULT NULL,
`OSAPD` varchar(20) DEFAULT NULL,
`DIMODPUPILSIZE1` varchar(20) DEFAULT NULL,
`DIMODPUPILSIZE2` varchar(20) DEFAULT NULL,
`DIMODPUPILREACTIVITY` varchar(10) DEFAULT NULL,
`DIMOSPUPILSIZE1` varchar(20) DEFAULT NULL,
`DIMOSPUPILSIZE2` varchar(20) DEFAULT NULL,
`DIMOSPUPILREACTIVITY` varchar(10) DEFAULT NULL,
`PUPIL_COMMENTS` text,
`ODVFCONFRONTATION1` int(1) DEFAULT NULL,
`ODVFCONFRONTATION2` int(1) DEFAULT NULL,
`ODVFCONFRONTATION3` int(1) DEFAULT NULL,
`ODVFCONFRONTATION4` int(1) DEFAULT NULL,
`ODVFCONFRONTATION5` int(1) DEFAULT NULL,
`OSVFCONFRONTATION1` int(1) DEFAULT NULL,
`OSVFCONFRONTATION2` int(1) DEFAULT NULL,
`OSVFCONFRONTATION3` int(1) DEFAULT NULL,
`OSVFCONFRONTATION4` int(1) DEFAULT NULL,
`OSVFCONFRONTATION5` int(1) DEFAULT NULL,
`ODDISC` varchar(100) DEFAULT NULL,
`OSDISC` varchar(100) DEFAULT NULL,
`ODCUP` varchar(100) DEFAULT NULL,
`OSCUP` varchar(100) DEFAULT NULL,
`ODMACULA` varchar(100) DEFAULT NULL,
`OSMACULA` varchar(100) DEFAULT NULL,
`ODVESSELS` varchar(100) DEFAULT NULL,
`OSVESSELS` varchar(100) DEFAULT NULL,
`ODPERIPH` varchar(100) DEFAULT NULL,
`OSPERIPH` varchar(100) DEFAULT NULL,
`ODCMT` varchar(50) DEFAULT NULL,
`OSCMT` varchar(50) DEFAULT NULL,
`RETINA_COMMENTS` text,
`IMP` text,
`PLAN` text,
`Technician` varchar(50) DEFAULT NULL,
`Doctor` varchar(50) DEFAULT NULL,
`Resource` varchar(50) DEFAULT NULL,
`LOCKED` VARCHAR( 3 ) NULL DEFAULT NULL,
`LOCKEDDATE` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
`LOCKEDBY` varchar(50) DEFAULT NULL,
`FINISHED` varchar(25) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=MyISAM;

DROP TABLE IF EXISTS `form_eye_mag_prefs`;
CREATE TABLE `form_eye_mag_prefs` (
  `PEZONE` varchar(25) DEFAULT NULL,
  `LOCATION` varchar(25) DEFAULT NULL,
  `LOCATION_text` varchar(25) NOT NULL,
  `id` bigint(20) DEFAULT NULL,
  `selection` varchar(255) DEFAULT NULL,
  `ZONE_ORDER` int(11) DEFAULT NULL,
  `GOVALUE` varchar(10) DEFAULT '0',
  `ordering` tinyint(4) DEFAULT NULL,
  `FILL_ACTION` varchar(10) NOT NULL DEFAULT 'ADD',
  `GORIGHT` varchar(50) NOT NULL,
  `GOLEFT` varchar(50) NOT NULL,
  `UNSPEC` varchar(50) NOT NULL,
  UNIQUE KEY `id` (`id`,`PEZONE`,`LOCATION`,`selection`)
) ENGINE=InnoDB;

INSERT INTO `form_eye_mag_prefs` (`PEZONE`, `LOCATION`, `LOCATION_text`, `id`, `selection`, `ZONE_ORDER`, `GOVALUE`, `ordering`, `FILL_ACTION`, `GORIGHT`, `GOLEFT`, `UNSPEC`) VALUES
('PREFS', 'ACT_SHOW', 'ACT Show', 2048, 'ACT_SHOW', 65, 'CCDIST', 15, 'ADD', '', '', ''),
('PREFS', 'ACT_VIEW', 'ACT View', 2048, 'ACT_VIEW', 64, '', 14, 'ADD', '', '', ''),
('PREFS', 'ADDITIONAL', 'Additional Data Points', 2048, 'ADDITIONAL', 56, '0', 6, 'ADD', '', '', ''),
('PREFS', 'ANTSEG_DRAW', 'ANTSEG DRAW', 2048, 'ANTSEG_DRAW', 73, NULL, 16, 'ADD', '', '', ''),
('PREFS', 'ANTSEG_RIGHT', 'ANTSEG DRAW', 2048, 'ANTSEG_RIGHT', 73, 'QP', 19, 'ADD', '', '', ''),
('PREFS', 'ANTSEG_VIEW', 'Anterior Segment View', 2048, 'ANTSEG_VIEW', 61, '0', 11, 'ADD', '', '', ''),
('PREFS', 'CLINICAL', 'CLINICAL', 2048, 'CLINICAL', 57, '1', 7, 'ADD', '', '', ''),
('PREFS', 'CR', 'Cycloplegic Refraction', 2048, 'CR', 54, '0', 4, 'ADD', '', '', ''),
('PREFS', 'CTL', 'Contact Lens', 2048, 'CTL', 55, '0', 5, 'ADD', '', '', ''),
('PREFS', 'CYLINDER', 'CYL', 2048, 'CYL', 59, '', 9, 'ADD', '', '', ''),
('PREFS', 'EXAM', 'EXAM', 2048, 'EXAM', 58, 'QP', 8, 'ADD', '', '', ''),
('PREFS', 'EXT_DRAW', 'EXT DRAW', 2048, 'EXT_DRAW', 72, NULL, 16, 'ADD', '', '', ''),
('PREFS', 'EXT_RIGHT', 'EXT DRAW', 2048, 'EXT_RIGHT', 72, 'QP', 18, 'ADD', '', '', ''),
('PREFS', 'EXT_VIEW', 'External View', 2048, 'EXT_VIEW', 66, '0', 16, 'ADD', '', '', ''),
('PREFS', 'HPI_DRAW', 'HPI DRAW', 2048, 'HPI_DRAW', 70, NULL, 16, 'ADD', '', '', ''),
('PREFS', 'HPI_RIGHT', 'HPI DRAW', 2048, 'HPI_RIGHT', 70, '', 16, 'ADD', '', '', ''),
('PREFS', 'HPI_VIEW', 'HPI View', 2048, 'HPI_VIEW', 60, NULL, 10, 'ADD', '', '', ''),
('PREFS', 'IMPPLAN_DRAW', 'IMPPLAN DRAW', 2048, 'IMPPLAN_DRAW', 76, NULL, 16, 'ADD', '', '', ''),
('PREFS', 'IMPPLAN_RIGHT', 'IMPPLAN DRAW', 2048, 'IMPPLAN_RIGHT', 76, NULL, 22, 'ADD', '', '', ''),
('PREFS', 'IOP', 'Intraocular Pressure', 2048, 'IOP', 67, '', 17, 'ADD', '', '', ''),
('PREFS', 'KB_VIEW', 'KeyBoard View', 2048, 'KB_VIEW', 78, '0', 24, 'ADD', '', '', ''),
('PREFS', 'MR', 'Manifest Refraction', 2048, 'MR', 53, '0', 3, 'ADD', '', '', ''),
('PREFS', 'NEURO_DRAW', 'NEURO DRAW', 2048, 'NEURO_DRAW', 75, NULL, 16, 'ADD', '', '', ''),
('PREFS', 'NEURO_RIGHT', 'NEURO DRAW', 2048, 'NEURO_RIGHT', 75, '', 21, 'ADD', '', '', ''),
('PREFS', 'NEURO_VIEW', 'Neuro View', 2048, 'NEURO_VIEW', 63, '', 13, 'ADD', '', '', ''),
('PREFS', 'PANEL_RIGHT', 'PMSFH Panel', 2048, 'PANEL_RIGHT', 77, '1', 23, 'ADD', '', '', ''),
('PREFS', 'PMH_DRAW', 'PMH DRAW', 2048, 'PMH_DRAW', 71, NULL, 16, 'ADD', '', '', ''),
('PREFS', 'PMH_RIGHT', 'PMH DRAW', 2048, 'PMH_RIGHT', 71, '', 17, 'ADD', '', '', ''),
('PREFS', 'RETINA_DRAW', 'RETINA DRAW', 2048, 'RETINA_DRAW', 74, NULL, 16, 'ADD', '', '', ''),
('PREFS', 'RETINA_RIGHT', 'RETINA DRAW', 2048, 'RETINA_RIGHT', 74, '', 20, 'ADD', '', '', ''),
('PREFS', 'RETINA_VIEW', 'Retina View', 2048, 'RETINA_VIEW', 62, '1', 12, 'ADD', '', '', ''),
('PREFS', 'VA', 'Vision', 2048, 'RS', 51, '1', 2048, 'ADD', '', '', ''),
('PREFS', 'VAX', 'Visual Acuities', 2048, 'VAX', 65, '0', 15, 'ADD', '', '', ''),
('PREFS', 'TOOLTIPS', 'Toggle Tooltips', 2048, 'TOOLTIPS', 66, 'on', NULL, 'ADD', '', '', ''),
('PREFS', 'W', 'Current Rx', 2048, 'W', 52, '1', 2, 'ADD', '', '', ''),
('PREFS', 'W_width', 'Detailed Rx', 2048, 'W_width', 80, '100', '', '', '', '', ''),
('PREFS', 'MR_width','Detailed MR', 2048, 'MR_width', 81, '110', '', '', '', '', '');

DROP TABLE IF EXISTS `form_eye_mag_orders`;
CREATE TABLE `form_eye_mag_orders` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`ORDER_PID` bigint(20) NOT NULL,
`ORDER_DETAILS` varchar(255) NOT NULL,
`ORDER_STATUS` varchar(50) DEFAULT NULL,
`ORDER_PRIORITY` varchar(50) DEFAULT NULL,
`ORDER_DATE_PLACED` date NOT NULL,
`ORDER_PLACED_BYWHOM` varchar(50) DEFAULT NULL,
`ORDER_DATE_COMPLETED` date DEFAULT NULL,
`ORDER_COMPLETED_BYWHOM` varchar(50) DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `VISIT_ID` (`ORDER_PID`,`ORDER_DETAILS`,`ORDER_DATE_PLACED`,`ORDER_PLACED_BYWHOM`,`ORDER_DATE_COMPLETED`)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `form_eye_mag_impplan`;
CREATE TABLE `form_eye_mag_impplan` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`form_id` bigint(20) NOT NULL,
`pid` bigint(20) NOT NULL,
`title` varchar(255) NOT NULL,
`code` varchar(50) DEFAULT NULL,
`codetype` varchar(50) DEFAULT NULL,
`codedesc` varchar(255) DEFAULT NULL,
`codetext` varchar(255) DEFAULT NULL,
`plan` varchar(3000) DEFAULT NULL,
`PMSFH_link` varchar(50) DEFAULT NULL,
`IMPPLAN_order` tinyint(4) DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE KEY `second_index` (`form_id`,`pid`,`title`,`plan`(20))
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `form_eye_mag_wearing`;
CREATE TABLE `form_eye_mag_wearing` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ENCOUNTER` int(11) NOT NULL,
  `FORM_ID` smallint(6) NOT NULL,
  `PID` int(11) NOT NULL,
  `RX_NUMBER` int(11) NOT NULL,
  `ODSPH` varchar(10) DEFAULT NULL,
  `ODCYL` varchar(10) DEFAULT NULL,
  `ODAXIS` varchar(10) DEFAULT NULL,
  `OSSPH` varchar(10) DEFAULT NULL,
  `OSCYL` varchar(10) DEFAULT NULL,
  `OSAXIS` varchar(10) DEFAULT NULL,
  `ODMIDADD` varchar(10) DEFAULT NULL,
  `OSMIDADD` varchar(10) DEFAULT NULL,
  `ODADD` varchar(10) DEFAULT NULL,
  `OSADD` varchar(10) DEFAULT NULL,
  `ODVA` varchar(10) DEFAULT NULL,
  `OSVA` varchar(10) DEFAULT NULL,
  `ODNEARVA` varchar(10) DEFAULT NULL,
  `OSNEARVA` varchar(10) DEFAULT NULL,
  `ODHPD` varchar(20) DEFAULT NULL,
  `ODHBASE` varchar(20) DEFAULT NULL,
  `ODVPD` varchar(20) DEFAULT NULL,
  `ODVBASE` varchar(20) DEFAULT NULL,
  `ODSLABOFF` varchar(20) DEFAULT NULL,
  `ODVERTEXDIST` varchar(20) DEFAULT NULL,
  `OSHPD` varchar(20) DEFAULT NULL,
  `OSHBASE` varchar(20) DEFAULT NULL,
  `OSVPD` varchar(20) DEFAULT NULL,
  `OSVBASE` varchar(20) DEFAULT NULL,
  `OSSLABOFF` varchar(20) DEFAULT NULL,
  `OSVERTEXDIST` varchar(20) DEFAULT NULL,
  `ODMPDD` varchar(20) DEFAULT NULL,
  `ODMPDN` varchar(20) DEFAULT NULL,
  `OSMPDD` varchar(20) DEFAULT NULL,
  `OSMPDN` varchar(20) DEFAULT NULL,
  `BPDD` varchar(20) DEFAULT NULL,
  `BPDN` varchar(20) DEFAULT NULL,
  `LENS_MATERIAL` varchar(20) DEFAULT NULL,
  `LENS_TREATMENTS` varchar(100) DEFAULT NULL,
  `RX_TYPE` varchar(25) DEFAULT NULL,
  `COMMENTS` text,
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `FORM_ID` (`FORM_ID`,`ENCOUNTER`,`PID`,`RX_NUMBER`)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS `form_taskman`;
CREATE TABLE `form_taskman` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `REQ_DATE` datetime NOT NULL,
    `FROM_ID` bigint(20) NOT NULL,
    `TO_ID` bigint(20) NOT NULL,
    `PATIENT_ID` bigint(20) NOT NULL, `DOC_TYPE` varchar(20) DEFAULT NULL,
    `DOC_ID` bigint(20) DEFAULT NULL,
    `ENC_ID` bigint(20) DEFAULT NULL,
    `METHOD` varchar(20) NOT NULL, `COMPLETED` varchar(1) DEFAULT NULL COMMENT '1 = completed',
    `COMPLETED_DATE` datetime DEFAULT NULL,
    `COMMENT` varchar(50) DEFAULT NULL,
    `USERFIELD_1` varchar(50) DEFAULT NULL,
    PRIMARY KEY (`ID`)
) ENGINE=INNODB;

-- End of Eye Module tables
-- ----------------------------------------------------
--
-- Table structure for table 'product_registration'
--
CREATE TABLE `product_registration` (
  `registration_id` char(36) NOT NULL DEFAULT '',
  `email` varchar(255) NULL,
  `opt_out` TINYINT(1) NULL,
  PRIMARY KEY (`registration_id`)
) ENGINE=InnoDB;

-- Table to copy log contents for audit log tamper resistance check.
DROP TABLE IF EXISTS `log_validator`;
CREATE TABLE `log_validator` (
  `log_id` bigint(20) NOT NULL,
  `log_checksum` longtext,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB;

-- Table to save code history log
DROP TABLE IF EXISTS `codes_history`;
CREATE TABLE `codes_history` (
  `log_id` bigint(20) NOT NULL auto_increment,
  `date` datetime,
  `code` varchar(25),
  `modifier` varchar(12),
  `active` tinyint(1),
  `diagnosis_reporting` tinyint(1),
  `financial_reporting` tinyint(1),
  `category` varchar(255),
  `code_type_name` varchar(255),
  `code_text` varchar(255),
  `code_text_short` varchar(24),
  `prices` text,
  `action_type` varchar(25),
  `update_by` varchar(255),
   PRIMARY KEY (`log_id`)
) ENGINE=InnoDB;
