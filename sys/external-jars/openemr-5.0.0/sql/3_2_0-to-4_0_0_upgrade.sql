--
--  Comment Meta Language for sql upgrades:
--
--  Each section within an upgrade sql file is enveloped with an #If*/#EndIf block.  At first glance, these appear to be standard mysql 
--  comments meant to be cryptic hints to -other developers about the sql goodness contained therein.  However, were you to rely on such basic premises,
--  you would find yourself grossly decieved.  Indeed, without the knowledge that these comments are, in fact a sneakily embedded meta langauge derived 
--  for a purpose none-other than to aid in the protection of the database during upgrades,  you would no doubt be subject to much ridicule and public 
--  beratement at the hands of the very developers who envisioned such a crafty use of comments. -jwallace
--
--  While these lines are as enigmatic as they are functional, there is a method to the madness.  Let's take a moment to briefly go over proper comment meta language use.
--  
--  The #If* sections have the behavior of functions and come complete with arguments supplied command-line style 
--
--  Your Comment meta language lines cannot contain any other comment styles such as the nefarious double dashes "--" lest your lines be skipped and 
--  the blocks automatcially executed with out regard to the existing database state.   
--
--  Comment Meta Language Constructs:
-- 
--  #IfNotTable
--    argument: table_name
--    behavior: if the table_name does not exist,  the block will be executed

--  #IfTable
--    argument: table_name
--    behavior: if the table_name does exist, the block will be executed

--  #IfMissingColumn
--    arguments: table_name colname
--    behavior:  if the colname in the table_name table does not exist,  the block will be executed

--  #IfNotColumnType
--    arguments: table_name colname value
--    behavior:  If the table table_name does not have a column colname with a data type equal to value, then the block will be executed

--  #IfNotRow
--    arguments: table_name colname value
--    behavior:  If the table table_name does not have a row where colname = value, the block will be executed.

--  #IfNotRow2D
--    arguments: table_name colname value colname2 value2
--    behavior:  If the table table_name does not have a row where colname = value AND colname2 = value2, the block will be executed.

--  #IfNotRow2Dx2
--    desc:      This is a very specialized function to allow adding items to the list_options table to avoid both redundant option_id and title in each element.
--    arguments: table_name colname value colname2 value2 colname3 value3
--    behavior:  The block will be executed if both statements below are true:
--               1) The table table_name does not have a row where colname = value AND colname2 = value2.
--               2) The table table_name does not have a row where colname = value AND colname3 = value3.

--  #EndIf
--    all blocks are terminated with and #EndIf statement. 

#IfMissingColumn log patient_id
ALTER TABLE log ADD patient_id bigint(20) DEFAULT NULL;
#EndIf

#IfMissingColumn log success
ALTER TABLE log ADD success tinyint(1) DEFAULT 1;
#EndIf

#IfMissingColumn log checksum
ALTER TABLE log ADD checksum longtext DEFAULT NULL;
#EndIf

#IfMissingColumn log crt_user
ALTER TABLE log ADD crt_user varchar(255) DEFAULT NULL;
#EndIf

#IfNotRow2Dx2 list_options list_id language option_id armenian title Armenian
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'armenian', 'Armenian', 10, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id chinese title Chinese
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'chinese', 'Chinese', 20, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id danish title Danish
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'danish', 'Danish', 30, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id deaf title Deaf
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'deaf', 'Deaf', 40, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id farsi title Farsi
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'farsi', 'Farsi', 60, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id french title French
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'french', 'French', 70, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id german title German
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'german', 'German', 80, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id greek title Greek
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'greek', 'Greek', 90, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id hmong title Hmong
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'hmong', 'Hmong', 100, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id italian title Italian
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'italian', 'Italian', 110, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id japanese title Japanese
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'japanese', 'Japanese', 120, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id korean title Korean
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'korean', 'Korean', 130, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id laotian title Laotian
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'laotian', 'Laotian', 140, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id mien title Mien
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'mien', 'Mien', 150, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id norwegian title Norwegian
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'norwegian', 'Norwegian', 160, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id othrs title Others
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'othrs', 'Others', 170, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id portuguese title Portuguese
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'portuguese', 'Portuguese', 180, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id punjabi title Punjabi
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'punjabi', 'Punjabi', 190, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id russian title Russian
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'russian', 'Russian', 200, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id tagalog title Tagalog
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'tagalog', 'Tagalog', 220, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id turkish title Turkish
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'turkish', 'Turkish', 230, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id vietnamese title Vietnamese
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'vietnamese', 'Vietnamese', 240, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id yiddish title Yiddish
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'yiddish', 'Yiddish', 250, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id language option_id zulu title Zulu
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('language', 'zulu', 'Zulu', 260, 0);
#EndIf

update list_options set seq = 50 where list_id = 'language' and option_id = 'English';
update list_options set seq = 210 where list_id = 'language' and option_id = 'Spanish';

#IfNotRow2Dx2 list_options list_id ethrace option_id aleut title ALEUT
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'aleut', 'ALEUT', 10,  0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id amer_indian title American Indian
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'amer_indian', 'American Indian', 20, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id cambodian title Cambodian
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'cambodian', 'Cambodian', 50, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id cs_american title Central/South American
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'cs_american', 'Central/South American', 70, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id chinese title Chinese
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'chinese', 'Chinese', 80, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id cuban title Cuban
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'cuban', 'Cuban', 90, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id eskimo title Eskimo
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'eskimo', 'Eskimo', 100, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id filipino title Filipino
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'filipino', 'Filipino', 110, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id guamanian title Guamanian
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'guamanian', 'Guamanian', 120, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id hawaiian title Hawaiian
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'hawaiian', 'Hawaiian', 130, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id othr_us title Hispanic - Other (Born in US)
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'othr_us', 'Hispanic - Other (Born in US)', 150, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id othr_non_us title Hispanic - Other (Born outside US)
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'othr_non_us', 'Hispanic - Other (Born outside US)', 160, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id hmong title Hmong
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'hmong', 'Hmong', 170, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id indian title Indian
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'indian', 'Indian', 180, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id japanese title Japanese
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'japanese', 'Japanese', 190, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id korean title Korean
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'korean', 'Korean', 200, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id laotian title Laotian
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'laotian', 'Laotian', 210, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id mexican title Mexican/MexAmer/Chicano
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'mexican', 'Mexican/MexAmer/Chicano', 220, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id mlt-race title Multiracial
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'mlt-race', 'Multiracial', 230, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id othr title Other
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'othr', 'Other', 240, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id othr_spec title Other - Specified
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'othr_spec', 'Other - Specified', 250, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id pac_island title Pacific Islander
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'pac_island', 'Pacific Islander', 260, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id puerto_rican title Puerto Rican
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'puerto_rican', 'Puerto Rican', 270, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id refused title Refused To State
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'refused', 'Refused To State', 280, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id samoan title Samoan
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'samoan', 'Samoan', 290, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id spec title Specified
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'spec', 'Specified', 300, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id thai title Thai
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'thai', 'Thai', 310, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id unknown title Unknown
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'unknown', 'Unknown', 320, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id unspec title Unspecified
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'unspec', 'Unspecified', 330, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id vietnamese title Vietnamese
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'vietnamese', 'Vietnamese', 340, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id white title White
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'white', 'White', 350, 0);
#EndIf
#IfNotRow2Dx2 list_options list_id ethrace option_id withheld title Withheld
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethrace', 'withheld', 'Withheld', 360, 0);
#EndIf

update list_options set seq = 60 where list_id = 'ethrace' and option_id = 'Caucasian';
update list_options set seq = 30 where list_id = 'ethrace' and option_id = 'Asian';
update list_options set seq = 40 where list_id = 'ethrace' and option_id = 'Black';
update list_options set seq = 140 where list_id = 'ethrace' and option_id = 'Hispanic';

#IfNotRow2D list_options list_id lists option_id eligibility
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists', 'eligibility', 'Eligibility', 47, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('eligibility', 'eligible', 'Eligible', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('eligibility', 'ineligible', 'Ineligible', 20, 0);
#EndIf

#IfNotRow2D layout_options form_id DEM field_id vfc
INSERT INTO `layout_options` VALUES ('DEM', 'vfc', '5Stats', 'VFC', 12, 1, 1, 20, 0, 'eligibility', 1, 1, '', '', 'Eligibility status for Vaccine for Children supplied vaccine');
#EndIf

#IfNotRow2D layout_options form_id DEM field_id mothersname
INSERT INTO `layout_options` VALUES ('DEM', 'mothersname', '2Contact', 'Mother''s Name', 6, 2, 1, 20, 63, '', 1, 1, '', '', '');
#EndIf

#IfNotRow2D layout_options form_id DEM field_id guardiansname
INSERT INTO `layout_options` VALUES ('DEM', 'guardiansname', '2Contact', 'Guardian''s Name', 7, 2, 1, 20, 63, '', 1, 1, '', '', '');
#EndIf

#IfNotRow2D layout_options form_id DEM field_id allow_imm_reg_use
INSERT INTO `layout_options` VALUES ('DEM', 'allow_imm_reg_use', '3Choices', 'Allow Immunization Registry Use', 9, 1, 1, 0, 0, 'yesno', 1, 1, '', '', '');
#EndIf

#IfNotRow2D layout_options form_id DEM field_id allow_imm_info_share
INSERT INTO `layout_options` VALUES ('DEM', 'allow_imm_info_share', '3Choices', 'Allow Immunization Info Sharing', 10, 1, 1, 0, 0, 'yesno', 1, 1, '', '', '');
#EndIf

#IfNotRow2D layout_options form_id DEM field_id allow_health_info_ex
INSERT INTO `layout_options` VALUES ('DEM', 'allow_health_info_ex', '3Choices', 'Allow Health Information Exchange', 11, 1, 1, 0, 0, 'yesno', 1, 1, '', '', '');
#EndIf

#IfMissingColumn patient_data vfc
ALTER TABlE patient_data
  ADD vfc varchar(255) NOT NULL DEFAULT '',
  ADD mothersname varchar(255) NOT NULL DEFAULT '',
  ADD guardiansname varchar(255) NOT NULL DEFAULT '',
  ADD allow_imm_reg_use varchar(255) NOT NULL DEFAULT '',
  ADD allow_imm_info_share varchar(255) NOT NULL DEFAULT '',
  ADD allow_health_info_ex varchar(255) NOT NULL DEFAULT '';
#EndIf

#IfNotRow categories name Advance Directive
  INSERT INTO categories select (select MAX(id) from categories) + 1, 'Advance Directive', '', 1, rght, rght + 7 from categories where name = 'Categories';
  INSERT INTO categories select (select MAX(id) from categories) + 1, 'Do Not Resuscitate Order', '', (select id from categories where name = 'Advance Directive'), rght + 1, rght + 2 from categories where name = 'Categories';
  INSERT INTO categories select (select MAX(id) from categories) + 1, 'Durable Power of Attorney', '', (select id from categories where name = 'Advance Directive'), rght + 3, rght + 4 from categories where name = 'Categories';
  INSERT INTO categories select (select MAX(id) from categories) + 1, 'Living Will', '', (select id from categories where name = 'Advance Directive'), rght + 5, rght + 6 from categories where name = 'Categories';
  UPDATE categories SET rght = rght + 8 WHERE name = 'Categories';
  UPDATE categories_seq SET id = (select MAX(id) from categories);
#EndIf

#IfNotRow categories name Patient Photograph
 INSERT INTO categories select (select MAX(id) from categories) + 1, 'Patient Photograph', '', 1, rght, rght + 1 from categories where name = 'Categories';
 UPDATE categories SET rght = rght + 2 WHERE name = 'Categories';
 UPDATE categories_seq SET id = (select MAX(id) from categories);
#Endif

#IfMissingColumn patient_data completed_ad
ALTER TABLE patient_data
  ADD completed_ad VARCHAR(3) NOT NULL DEFAULT 'NO',
  ADD ad_reviewed date DEFAULT NULL;
#EndIf

#IfNotRow2D list_options list_id lists option_id apptstat
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'apptstat','Appointment Statuses', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','-'       ,'- None'              , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','*'       ,'* Reminder done'     ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','+'       ,'+ Chart pulled'      ,15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','x'       ,'x Canceled'          ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','?'       ,'? No show'           ,25,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','@'       ,'@ Arrived'           ,30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','~'       ,'~ Arrived late'      ,35,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','!'       ,'! Left w/o visit'    ,40,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','#'       ,'# Ins/fin issue'     ,45,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','<'       ,'< In exam room'      ,50,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','>'       ,'> Checked out'       ,55,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','$'       ,'$ Coding done'       ,60,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('apptstat','%'       ,'% Canceled < 24h'    ,65,0);
ALTER TABLE openemr_postcalendar_events CHANGE pc_apptstatus pc_apptstatus varchar(15) NOT NULL DEFAULT '-';
#EndIf

#IfNotRow2D list_options list_id lists option_id transactions
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists', 'transactions', 'Transactions', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('transactions', 'Referral', 'Referral', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('transactions', 'Patient Request', 'Patient Request', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('transactions', 'Physician Request', 'Physician Request', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('transactions', 'Legal', 'Legal', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('transactions', 'Billing', 'Billing', 50, 0);
#EndIf

#IfNotRow2D layout_options form_id DEM field_id referral_source
INSERT INTO `layout_options` VALUES ('DEM', 'referral_source', '5Stats', 'Referral Source',10, 26, 1, 0, 0, 'refsource', 1, 1, '', '', 'How did they hear about us');
#EndIf

#IfMissingColumn list_options notes
ALTER TABLE list_options
  CHANGE mapping mapping varchar(31) NOT NULL DEFAULT '',
  ADD notes varchar(255) NOT NULL DEFAULT '';
#EndIf

#IfNotRow2D list_options list_id lists option_id warehouse
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','warehouse','Warehouses',21,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('warehouse','onsite','On Site', 5,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id abook_type
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','abook_type'  ,'Address Book Types'  , 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('abook_type','ord_img','Imaging Service'     , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('abook_type','ord_imm','Immunization Service',10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('abook_type','ord_lab','Lab Service'         ,15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('abook_type','spe'    ,'Specialist'          ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('abook_type','vendor' ,'Vendor'              ,25,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('abook_type','oth'    ,'Other'               ,95,0);
#EndIf

#IfMissingColumn users abook_type
ALTER TABLE users
  ADD abook_type varchar(31) NOT NULL DEFAULT '';
#EndIf

#IfNotRow2D list_options list_id lists option_id proc_type
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_type','Procedure Types', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_type','grp','Group'          ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_type','ord','Procedure Order',20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_type','res','Discrete Result',30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_type','rec','Recommendation' ,40,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id proc_body_site
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_body_site','Procedure Body Sites', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_body_site','arm'    ,'Arm'    ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_body_site','buttock','Buttock',20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_body_site','oth'    ,'Other'  ,90,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id proc_specimen
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_specimen','Procedure Specimen Types', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_specimen','blood' ,'Blood' ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_specimen','saliva','Saliva',20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_specimen','urine' ,'Urine' ,30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_specimen','oth'   ,'Other' ,90,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id proc_route
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_route','Procedure Routes', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_route','inj' ,'Injection',10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_route','oral','Oral'     ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_route','oth' ,'Other'    ,90,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id proc_lat
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_lat','Procedure Lateralities', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_lat','left' ,'Left'     ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_lat','right','Right'    ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_lat','bilat','Bilateral',30,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id proc_unit
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
#EndIf

#IfNotRow2D list_options list_id lists option_id ord_priority
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','ord_priority','Order Priorities', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_priority','high'  ,'High'  ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_priority','normal','Normal',20,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id ord_status
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','ord_status','Order Statuses', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_status','pending' ,'Pending' ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_status','routed'  ,'Routed'  ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_status','complete','Complete',30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ord_status','canceled','Canceled',40,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id proc_rep_status
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_rep_status','Procedure Report Statuses', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','final'  ,'Final'      ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','review' ,'Reviewed'   ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','prelim' ,'Preliminary',30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','cancel' ,'Canceled'   ,40,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','error'  ,'Error'      ,50,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_rep_status','correct','Corrected'  ,60,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id proc_res_abnormal
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_res_abnormal','Procedure Result Abnormal', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_abnormal','no'  ,'No'  ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_abnormal','yes' ,'Yes' ,20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_abnormal','high','High',30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_abnormal','low' ,'Low' ,40,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id proc_res_status
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_res_status','Procedure Result Statuses', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','final'     ,'Final'      ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','prelim'    ,'Preliminary',20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','cancel'    ,'Canceled'   ,30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','error'     ,'Error'      ,40,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','correct'   ,'Corrected'  ,50,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_status','incomplete','Incomplete' ,60,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id proc_res_bool
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists','proc_res_bool','Procedure Boolean Results', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_bool','neg' ,'Negative',10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('proc_res_bool','pos' ,'Positive',20,0);
#EndIf

#IfNotTable procedure_type
CREATE TABLE `procedure_type` (
  `procedure_type_id`   bigint(20)   NOT NULL AUTO_INCREMENT,
  `parent`              bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references procedure_type.procedure_type_id',
  `name`                varchar(63)  NOT NULL DEFAULT '' COMMENT 'name for this category, procedure or result type',
  `lab_id`              bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references users.id, 0 means default to parent',
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
  PRIMARY KEY (`procedure_type_id`),
  KEY parent (parent)
) ENGINE=MyISAM;
#EndIf

#IfNotTable procedure_order
CREATE TABLE `procedure_order` (
  `procedure_order_id`     bigint(20)   NOT NULL AUTO_INCREMENT,
  `procedure_type_id`      bigint(20)   NOT NULL            COMMENT 'references procedure_type.procedure_type_id',
  `provider_id`            bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references users.id',
  `patient_id`             bigint(20)   NOT NULL            COMMENT 'references patient_data.pid',
  `encounter_id`           bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references form_encounter.encounter',
  `date_collected`         datetime     DEFAULT NULL        COMMENT 'time specimen collected',
  `date_ordered`           date         DEFAULT NULL,
  `order_priority`         varchar(31)  NOT NULL DEFAULT '',
  `order_status`           varchar(31)  NOT NULL DEFAULT '' COMMENT 'pending,routed,complete,canceled',
  `patient_instructions`   text         NOT NULL DEFAULT '',
  `activity`               tinyint(1)   NOT NULL DEFAULT 1  COMMENT '0 if deleted',
  `control_id`             bigint(20)   NOT NULL            COMMENT 'This is the CONTROL ID that is sent back from lab',
  PRIMARY KEY (`procedure_order_id`),
  KEY datepid (date_ordered, patient_id)
) ENGINE=MyISAM;
#EndIf

#IfNotTable procedure_report
CREATE TABLE `procedure_report` (
  `procedure_report_id` bigint(20)     NOT NULL AUTO_INCREMENT,
  `procedure_order_id`  bigint(20)     DEFAULT NULL   COMMENT 'references procedure_order.procedure_order_id',
  `date_collected`      datetime       DEFAULT NULL,
  `date_report`         date           DEFAULT NULL,
  `source`              bigint(20)     NOT NULL DEFAULT 0  COMMENT 'references users.id, who entered this data',
  `specimen_num`        varchar(63)    NOT NULL DEFAULT '',
  `report_status`       varchar(31)    NOT NULL DEFAULT '' COMMENT 'received,complete,error',
  `review_status`       varchar(31)    NOT NULL DEFAULT 'received' COMMENT 'panding reivew status: received,reviewed',
  PRIMARY KEY (`procedure_report_id`),
  KEY procedure_order_id (procedure_order_id)
) ENGINE=MyISAM; 
#EndIf

#IfNotTable procedure_result
CREATE TABLE `procedure_result` (
  `procedure_result_id` bigint(20)   NOT NULL AUTO_INCREMENT,
  `procedure_report_id` bigint(20)   NOT NULL            COMMENT 'references procedure_report.procedure_report_id',
  `procedure_type_id`   bigint(20)   NOT NULL            COMMENT 'references procedure_type.procedure_type_id',
  `date`                datetime     DEFAULT NULL        COMMENT 'lab-provided date specific to this result',
  `facility`            varchar(255) NOT NULL DEFAULT '' COMMENT 'lab-provided testing facility ID',
  `units`               varchar(31)  NOT NULL DEFAULT '',
  `result`              varchar(255) NOT NULL DEFAULT '',
  `range`               varchar(255) NOT NULL DEFAULT '',
  `abnormal`            varchar(31)  NOT NULL DEFAULT '' COMMENT 'no,yes,high,low',
  `comments`            text         NOT NULL DEFAULT '' COMMENT 'comments from the lab',
  `document_id`         bigint(20)   NOT NULL DEFAULT 0  COMMENT 'references documents.id if this result is a document',
  `result_status`       varchar(31)  NOT NULL DEFAULT '' COMMENT 'preliminary, cannot be done, final, corrected, incompete...etc.',
  PRIMARY KEY (`procedure_result_id`),
  KEY procedure_report_id (procedure_report_id)
) ENGINE=MyISAM; 
#EndIf

#IfMissingColumn history_data recreational_drugs
ALTER TABLE history_data ADD recreational_drugs longtext;
#EndIf

update layout_options set seq = 1, data_type = 28, titlecols = 1, datacols = 3 where form_id = 'HIS' and field_id = 'coffee';
update layout_options set seq = 6, data_type = 28, titlecols = 1, datacols = 3 where form_id = 'HIS' and field_id = 'exercise_patterns';
update layout_options set seq = 2, data_type = 28, titlecols = 1, datacols = 3 where form_id = 'HIS' and field_id = 'tobacco';
update layout_options set seq = 3, data_type = 28, titlecols = 1, datacols = 3 where form_id = 'HIS' and field_id = 'alcohol';
update layout_options set seq = 5, data_type = 28, titlecols = 1, datacols = 3 where form_id = 'HIS' and field_id = 'counseling';
update layout_options set seq = 7, data_type = 28, titlecols = 1, datacols = 3 where form_id = 'HIS' and field_id = 'hazardous_activities';
update layout_options set seq = 8, titlecols = 1, datacols = 3 where form_id = 'HIS' and field_id = 'sleep_patterns';
update layout_options set seq = 9, titlecols = 1, datacols = 3 where form_id = 'HIS' and field_id = 'seatbelt_use';

#IfNotRow2D layout_options form_id HIS field_id recreational_drugs
INSERT INTO layout_options (form_id, field_id, group_name, title, seq, data_type, uor, fld_length, max_length, list_id, titlecols, datacols, default_value, edit_options, description) VALUES ('HIS','recreational_drugs','4Lifestyle','Recreational Drugs',4,28,1,20,255,'',1,3,'','' ,'Recreational drugs use');
#EndIf

#IfMissingColumn users pwd_expiration_date
ALTER TABLE users ADD pwd_expiration_date date DEFAULT NULL;
#EndIf

#IfMissingColumn users pwd_history1
ALTER TABLE users ADD pwd_history1 longtext DEFAULT NULL;
#EndIf

#IfMissingColumn users pwd_history2
ALTER TABLE users ADD pwd_history2 longtext DEFAULT NULL;
#EndIf

#IfMissingColumn drug_inventory warehouse_id
ALTER TABLE drug_inventory
  ADD warehouse_id varchar(31) NOT NULL DEFAULT '';
#EndIf

#IfMissingColumn drug_inventory vendor_id
ALTER TABLE drug_inventory
  ADD vendor_id bigint(20) NOT NULL DEFAULT 0;
#EndIf

#IfMissingColumn drug_sales xfer_inventory_id
ALTER TABLE drug_sales
  ADD xfer_inventory_id int(11) NOT NULL DEFAULT 0;
#EndIf

#IfMissingColumn drugs allow_combining
ALTER TABLE drugs
  ADD allow_combining tinyint(1) NOT NULL DEFAULT 0 COMMENT '1 = allow filling an order from multiple lots',
  ADD allow_multiple  tinyint(1) NOT NULL DEFAULT 1 COMMENT '1 = allow multiple lots at one warehouse';
#EndIf

#IfNotRow registry directory procedure_order
INSERT INTO `registry` VALUES ('Procedure Order', 1, 'procedure_order', NULL, 1, 1, '2010-02-25 00:00:00', 0, 'Administrative', '');
#EndIf

UPDATE registry SET category = 'Administrative' WHERE category = 'category' AND directory = 'fee_sheet';
UPDATE registry SET category = 'Administrative' WHERE category = 'category' AND directory = 'procedure_order';
UPDATE registry SET category = 'Administrative' WHERE category = 'category' AND directory = 'newpatient';
UPDATE registry SET category = 'Administrative' WHERE category = 'category' AND directory = 'misc_billing_options';
UPDATE registry SET category = 'Clinical' WHERE category = 'category';

#IfMissingColumn users default_warehouse
ALTER TABLE users ADD default_warehouse varchar(31) NOT NULL DEFAULT '';
#EndIf

UPDATE layout_options SET edit_options = 'N'  WHERE form_id = 'DEM' AND field_id = 'title'  AND edit_options = '';
UPDATE layout_options SET edit_options = 'CD' WHERE form_id = 'DEM' AND field_id = 'fname'  AND edit_options = 'C';
UPDATE layout_options SET edit_options = 'CD' WHERE form_id = 'DEM' AND field_id = 'lname'  AND edit_options = 'C';
UPDATE layout_options SET edit_options = 'ND' WHERE form_id = 'DEM' AND field_id = 'pubpid' AND edit_options = '';
UPDATE layout_options SET edit_options = 'N'  WHERE form_id = 'DEM' AND field_id = 'sex'    AND edit_options = '';

#IfNotRow2D list_options list_id lists option_id message_status
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'         ,'message_status','Message Status',45,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('message_status','Done'           ,'Done'         , 5,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('message_status','Forwarded'      ,'Forwarded'    ,10,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('message_status','New'            ,'New'          ,15,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('message_status','Read'           ,'Read'         ,20,0);
#EndIf

#IfNotRow2D list_options list_id note_type option_id Lab Results
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Lab Results' ,'Lab Results', 15,0);
#EndIf
#IfNotRow2D list_options list_id note_type option_id New Orders
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','New Orders' ,'New Orders', 20,0);
#EndIf
#IfNotRow2D list_options list_id note_type option_id Patient Reminders
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('note_type','Patient Reminders' ,'Patient Reminders', 25,0);
#EndIf

#IfMissingColumn pnotes message_status
ALTER TABLE pnotes
  ADD message_status VARCHAR(20) NOT NULL DEFAULT 'New';
#EndIf

#IfNotTable globals
CREATE TABLE `globals` (
  `gl_name`             varchar(63)    NOT NULL,
  `gl_index`            int(11)        NOT NULL DEFAULT 0,
  `gl_value`            varchar(255)   NOT NULL DEFAULT '',
  PRIMARY KEY (`gl_name`, `gl_index`)
) ENGINE=MyISAM; 
#EndIf

#IfNotTable lang_custom
CREATE TABLE lang_custom (
  `lang_description`   varchar(100)   NOT NULL default '',
  `lang_code`          char(2)        NOT NULL default '',
  `constant_name`      varchar(255)   NOT NULL default '',
  `definition`         mediumtext     NOT NULL default ''
) ENGINE=MyISAM;
#EndIf

#IfNotRow2D list_options list_id lists option_id irnpool
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'irnpool','Invoice Reference Number Pools', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default, notes ) VALUES ('irnpool','main','Main',1,1,'000001');
#EndIf

#IfMissingColumn users irnpool
ALTER TABLE users ADD irnpool varchar(31) NOT NULL DEFAULT '';
#EndIf

#IfMissingColumn form_encounter invoice_refno
ALTER TABLE form_encounter ADD invoice_refno varchar(31) NOT NULL DEFAULT '';
#EndIf

#IfMissingColumn drug_sales notes
ALTER TABLE drug_sales
  ADD notes varchar(255) NOT NULL DEFAULT '';
#EndIf

#IfNotTable code_types
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
  PRIMARY KEY (ct_key)
) ENGINE=MyISAM;
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag ) VALUES ('ICD9' , 2, 1, 2, ''    , 0, 0, 0, 1);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag ) VALUES ('CPT4' , 1, 2, 2, 'ICD9', 1, 0, 0, 0);
INSERT INTO code_types (ct_key, ct_id, ct_seq, ct_mod, ct_just, ct_fee, ct_rel, ct_nofs, ct_diag ) VALUES ('HCPCS', 3, 3, 2, 'ICD9', 1, 0, 0, 0);
#EndIf

#IfNotRow2D list_options list_id lists option_id code_types
INSERT INTO list_options ( list_id, option_id, title, seq ) VALUES ('lists', 'code_types', 'Code Types', 1);
#EndIf

#IfMissingColumn codes reportable
ALTER TABLE `codes` 
  ADD `reportable` TINYINT(1) DEFAULT 0 COMMENT '0 = non-reportable, 1 = reportable';
#EndIf

#IfNotTable syndromic_surveillance
CREATE TABLE `syndromic_surveillance` (
  `id` bigint(20) NOT NULL auto_increment,
  `lists_id` bigint(20) NOT NULL,
  `submission_date` datetime NOT NULL,
  `filename` varchar(255) NOT NULL default '',
  PRIMARY KEY  (`id`),
  KEY (`lists_id`)
) ENGINE=MyISAM AUTO_INCREMENT=1 ;
#EndIf

#IfMissingColumn lists reinjury_id
ALTER TABLE lists
  ADD reinjury_id bigint(20)  NOT NULL DEFAULT 0,
  ADD injury_part varchar(31) NOT NULL DEFAULT '',
  ADD injury_type varchar(31) NOT NULL DEFAULT '';
#EndIf

ALTER TABLE layout_options CHANGE description description text;

#IfMissingColumn transactions refer_reply_date
ALTER TABLE transactions ADD refer_reply_date date DEFAULT NULL;
#EndIf

#IfMissingColumn transactions reply_related_code
ALTER TABLE transactions ADD reply_related_code varchar(255) NOT NULL DEFAULT '';
#EndIf

#IfNotTable extended_log
CREATE TABLE `extended_log` (
  `id`          bigint(20)   NOT NULL AUTO_INCREMENT,
  `date`        datetime     DEFAULT NULL,
  `event`       varchar(255) DEFAULT NULL,
  `user`        varchar(255) DEFAULT NULL,
  `recipient`   varchar(255) DEFAULT NULL,
  `description` longtext,
  `patient_id`  bigint(20)   DEFAULT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=1;
#EndIf

#IfNotRow2D list_options list_id lists option_id disclosure_type
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'disclosure_type','Disclosure Type', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('disclosure_type', 'disclosure-treatment', 'Treatment', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('disclosure_type', 'disclosure-payment', 'Payment', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('disclosure_type', 'disclosure-healthcareoperations', 'Health Care Operations', 30, 0);
#EndIf

#IfNotTable user_settings
CREATE TABLE `user_settings` (
  `setting_user`  bigint(20)   NOT NULL DEFAULT 0,
  `setting_label` varchar(63)  NOT NULL,
  `setting_value` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`setting_user`, `setting_label`)
) ENGINE=MyISAM;
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'allergy_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'appointments_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'demographics_ps_expand', '0');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'dental_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'directives_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'disclosures_ps_expand', '0');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'immunizations_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'insurance_ps_expand', '0');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'medical_problem_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'medication_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'pnotes_ps_expand', '0');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'prescriptions_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'surgery_ps_expand', '1');
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'vitals_ps_expand', '1');
#EndIf

#IfNotRow user_settings setting_label gacl_protect
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'gacl_protect', '0');
#EndIf

#IfNotTable version
CREATE TABLE version (
  v_major    int(11)     NOT NULL DEFAULT 0,
  v_minor    int(11)     NOT NULL DEFAULT 0,
  v_patch    int(11)     NOT NULL DEFAULT 0,
  v_tag      varchar(31) NOT NULL DEFAULT '',
  v_database int(11)     NOT NULL DEFAULT 0
) ENGINE=MyISAM;
INSERT INTO version (v_major, v_minor, v_patch, v_tag, v_database) VALUES (0, 0, 0, '', 0);
#EndIf

#IfMissingColumn lists injury_grade
ALTER TABLE lists
  ADD injury_grade varchar(31) NOT NULL DEFAULT '';
#EndIf

#IfMissingColumn form_encounter referral_source
ALTER TABLE form_encounter ADD referral_source varchar(31) NOT NULL DEFAULT '';
#EndIf

#IfMissingColumn facility tax_id_type
ALTER TABLE facility ADD COLUMN tax_id_type VARCHAR(31) NOT NULL DEFAULT '' AFTER facility_npi;
#EndIf

#IfMissingColumn ar_session created_time
ALTER TABLE  `ar_session` ADD  `created_time` timestamp NOT NULL default CURRENT_TIMESTAMP,
ADD  `modified_time` datetime NOT NULL,
ADD  `global_amount` decimal( 12, 2 ) NOT NULL ,
ADD  `payment_type` varchar( 50 ) NOT NULL ,
ADD  `description` text NOT NULL ,
ADD  `adjustment_code` varchar( 50 ) NOT NULL ,
ADD  `post_to_date` date NOT NULL ,
ADD  `patient_id` int( 11 ) NOT NULL ,
ADD `payment_method` varchar( 25 ) NOT NULL ;
#EndIf

#IfMissingColumn ar_activity modified_time
ALTER TABLE `ar_activity` ADD `modified_time` datetime NOT NULL,
ADD `follow_up` char(1) NOT NULL,
ADD `follow_up_note` text NOT NULL,
ADD `account_code` varchar(15) NOT NULL;
#EndIf

#IfNotRow2D list_options list_id lists option_id payment_adjustment_code
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_adjustment_code','Payment Adjustment Code', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_adjustment_code', 'family_payment', 'Family Payment', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_adjustment_code', 'group_payment', 'Group Payment', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_adjustment_code', 'insurance_payment', 'Insurance Payment', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_adjustment_code', 'patient_payment', 'Patient Payment', 50, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_adjustment_code', 'pre_payment', 'Pre Payment', 60, 0);
#EndIf

#IfNotRow2D list_options list_id lists option_id payment_ins
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_ins','Payment Ins', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_ins', '0', 'Pat', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_ins', '1', 'Ins1', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_ins', '2', 'Ins2', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_ins', '3', 'Ins3', 30, 0);
#EndIf

#IfNotRow2D list_options list_id lists option_id payment_method
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_method','Payment Method', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_method', 'bank_draft', 'Bank Draft', 50, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_method', 'cash', 'Cash', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_method', 'check_payment', 'Check Payment', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_method', 'credit_card', 'Credit Card', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_method', 'electronic', 'Electronic', 40, 0);
#EndIf

#IfNotRow2D list_options list_id lists option_id payment_sort_by
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_sort_by','Payment Sort By', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'check_date', 'Check Date', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'payer_id', 'Ins Code', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'payment_method', 'Payment Method', 50, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'payment_type', 'Paying Entity', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'pay_total', 'Amount', 70, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'reference', 'Check Number', 60, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_sort_by', 'session_id', 'Id', 10, 0);
#EndIf

#IfNotRow2D list_options list_id lists option_id payment_status
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_status','Payment Status', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_status', 'fully_paid', 'Fully Paid', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_status', 'unapplied', 'Unapplied', 20, 0);
#EndIf

#IfNotRow2D list_options list_id lists option_id payment_type
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_type','Payment Type', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_type', 'insurance', 'Insurance', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_type', 'patient', 'Patient', 20, 0);
#EndIf

#IfNotTable eligibility_response
CREATE TABLE `eligibility_response` (
  `response_id` bigint(20) NOT NULL auto_increment,
  `response_description` varchar(255) default NULL,
  `response_status` enum('A','D') NOT NULL default 'A',
  `response_vendor_id` bigint(20) default NULL,
  `response_create_date` date default NULL,
  `response_modify_date` date default NULL,
  PRIMARY KEY  (`response_id`)
) ENGINE=MyISAM AUTO_INCREMENT=1;
#EndIf

#IfNotTable eligibility_verification
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
) ENGINE=MyISAM AUTO_INCREMENT=1;
#EndIf

#IfNotRow2D list_options list_id lists option_id smoking_status
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'smoking_status','Smoking Status', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('smoking_status', '1', 'Current every day smoker', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('smoking_status', '2', 'Current some day smoker', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('smoking_status', '3', 'Former smoker', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('smoking_status', '4', 'Never smoker', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('smoking_status', '5', 'Smoker, current status unknown', 50, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('smoking_status', '9', 'Unknown if ever smoked', 60, 0);
#EndIf

UPDATE layout_options SET seq=2 WHERE form_id = 'HIS' AND field_id='coffee';
UPDATE layout_options SET data_type=32,seq=1,fld_length=0,list_id='smoking_status' WHERE form_id = 'HIS' AND field_id='tobacco';

#IfMissingColumn drug_sales distributor_id
ALTER TABLE drug_sales ADD distributor_id bigint(20) NOT NULL DEFAULT 0;
#EndIf

#IfNotRow2D list_options list_id abook_type option_id dist
INSERT INTO list_options (list_id,option_id,title,seq,is_default) VALUES ('abook_type','dist','Distributor',30,0);
#EndIf

#IfNotRow2D list_options list_id adjreason option_value 1
UPDATE list_options SET option_value = 2 WHERE list_id = 'adjreason' AND option_value = 0 AND option_id LIKE 'To co%';
UPDATE list_options SET option_value = 3 WHERE list_id = 'adjreason' AND option_value = 0 AND option_id LIKE 'To ded%';
UPDATE list_options SET option_value = 4 WHERE list_id = 'adjreason' AND option_value = 0 AND option_id LIKE 'Ins termed';
UPDATE list_options SET option_value = 4 WHERE list_id = 'adjreason' AND option_value = 0 AND option_id LIKE 'No coverage';
UPDATE list_options SET option_value = 4 WHERE list_id = 'adjreason' AND option_value = 0 AND option_id LIKE 'Pre-existing';
UPDATE list_options SET option_value = 5 WHERE list_id = 'adjreason' AND option_value = 0 AND option_id LIKE 'Ins info%';
UPDATE list_options SET option_value = 5 WHERE list_id = 'adjreason' AND option_value = 0 AND option_id LIKE 'Ins recoup';
UPDATE list_options SET option_value = 5 WHERE list_id = 'adjreason' AND option_value = 0 AND option_id LIKE '%refund';
UPDATE list_options SET option_value = 5 WHERE list_id = 'adjreason' AND option_value = 0 AND option_id LIKE '%overpaid';
UPDATE list_options SET option_value = 1 WHERE list_id = 'adjreason' AND option_value = 0;
#EndIf

#IfNotRow2D user_settings setting_label billing_ps_expand setting_user 0
INSERT INTO user_settings ( setting_user, setting_label, setting_value ) VALUES (0, 'billing_ps_expand', '0');
#EndIf

#IfMissingColumn lists reaction
ALTER TABLE lists ADD reaction varchar(255) NOT NULL DEFAULT '';
#EndIf

#IfNotRow2D list_options list_id lists option_id race
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'race','Race', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('race', 'amer_ind_or_alaska_native', 'American Indian or Alaska Native', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('race', 'Asian', 'Asian',20,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('race', 'black_or_afri_amer', 'Black or African American',30,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('race', 'native_hawai_or_pac_island', 'Native Hawaiian or Other Pacific Islander',40,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('race', 'white', 'White',50,0);
#EndIf

#IfNotRow2D list_options list_id lists option_id ethnicity
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'ethnicity','Ethnicity', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethnicity', 'hisp_or_latin', 'Hispanic or Latino', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('ethnicity', 'not_hisp_or_latin', 'Not Hispanic or Latino', 10, 0);
#EndIf

#IfMissingColumn patient_data race
ALTER TABLE `patient_data` ADD `race` varchar(255) NOT NULL default '';
#EndIf

#IfMissingColumn patient_data ethnicity
ALTER TABLE `patient_data` ADD `ethnicity` varchar(255) NOT NULL default '';
#EndIf

#IfNotRow2D layout_options field_id race form_id DEM
INSERT INTO `layout_options` VALUES ('DEM', 'race', '5Stats', 'Race', 3, 33, 1, 0, 0, 'race', 1, 1, '', '', 'Race');
#EndIf

UPDATE layout_options SET data_type=33,list_id='ethnicity',field_id='ethnicity',title='Ethnicity',description='Ethnicity' WHERE form_id = 'DEM' AND field_id = 'ethnoracial';
UPDATE layout_options SET seq=4 WHERE form_id = 'DEM' and field_id='financial_review';

UPDATE patient_data SET ethnicity= case WHEN ethnoracial IN ('Hispanic','othr_us','othr_non_us') THEN ethnoracial ELSE '' END, race = case WHEN ethnoracial NOT IN ('Hispanic','othr_us','othr_non_us') THEN ethnoracial ELSE '' END;

UPDATE list_options SET option_value = 3 WHERE list_id = 'abook_type' AND option_value = 0 AND option_id = 'ord_img';
UPDATE list_options SET option_value = 3 WHERE list_id = 'abook_type' AND option_value = 0 AND option_id = 'ord_imm';
UPDATE list_options SET option_value = 3 WHERE list_id = 'abook_type' AND option_value = 0 AND option_id = 'ord_lab';
UPDATE list_options SET option_value = 2 WHERE list_id = 'abook_type' AND option_value = 0 AND option_id = 'spe';
UPDATE list_options SET option_value = 3 WHERE list_id = 'abook_type' AND option_value = 0 AND option_id = 'vendor';
UPDATE list_options SET option_value = 3 WHERE list_id = 'abook_type' AND option_value = 0 AND option_id = 'dist';
UPDATE list_options SET option_value = 1 WHERE list_id = 'abook_type' AND option_value = 0 AND option_id = 'oth';

#IfNotRow2D list_options list_id lists option_id payment_date
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists'   ,'payment_date','Payment Date', 1,0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_date', 'date_val', 'Date', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_date', 'post_to_date', 'Post To Date', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('payment_date', 'deposit_date', 'Deposit Date', 30, 0);
#EndIf

#IfNotRow2D list_options list_id lists option_id date_master_criteria
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('lists', 'date_master_criteria', 'Date Master Criteria', 33, 1);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'all', 'All', 10, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'today', 'Today', 20, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'this_month_to_date', 'This Month to Date', 30, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'last_month', 'Last Month', 40, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'this_week_to_date', 'This Week to Date', 50, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'this_calendar_year', 'This Calendar Year', 60, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'last_calendar_year', 'Last Calendar Year', 70, 0);
INSERT INTO list_options ( list_id, option_id, title, seq, is_default ) VALUES ('date_master_criteria', 'custom', 'Custom', 80, 0);
#EndIf

#IfTable globals
UPDATE globals SET gl_value = 'style_oemr.css' WHERE gl_name = 'css_header' AND gl_value = 'style_default.css';
#EndIf

#IfTable user_settings
UPDATE user_settings SET setting_value = 'style_oemr.css' WHERE setting_label = 'global:css_header' AND setting_value = 'style_default.css';
#EndIf

#IfMissingColumn documents hash
ALTER TABLE documents ADD hash varchar(40) DEFAULT NULL COMMENT '40-character SHA-1 hash of document';
#EndIf

#IfTable users
UPDATE users SET pwd_history1='',pwd_history2='';
#EndIf
