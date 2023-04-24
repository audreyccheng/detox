<?php
/**
 * Copyright (C) 2016 Kevin Yeh <kevin.y@integralemr.com>
 * Copyright (C) 2016 Brady Miller <brady.g.miller@gmail.com>
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
 * @author  Kevin Yeh <kevin.y@integralemr.com>
 * @author  Brady Miller <brady.g.miller@gmail.com>
 * @link    http://www.open-emr.org
 */

include_once("$srcdir/registry.inc");

$menu_update_map=array();
$menu_update_map["Visit Forms"]="update_visit_forms";
$menu_update_map["Modules"]="update_modules_menu";

function update_modules_menu(&$menu_list)
{
    $module_query = sqlStatement("select mod_directory,mod_name,mod_nick_name,mod_relative_link,type from modules where mod_active = 1 AND sql_run= 1 order by mod_ui_order asc");
    if (sqlNumRows($module_query)) {
      while ($modulerow = sqlFetchArray($module_query)) {
                    $acl_section = strtolower($modulerow['mod_directory']);
                    if (!zh_acl_check($_SESSION['authUserID'],$acl_section)) continue;
                    $modulePath = "";
                    $added 		= "";
                    if($modulerow['type'] == 0) {
                            $modulePath = $GLOBALS['customModDir'];
                            $added		= "";
                    }
                    else{
                            $added		= "index";
                            $modulePath = $GLOBALS['zendModDir'];
                    }

                    $relative_link ="/interface/modules/".$modulePath."/".$modulerow['mod_relative_link'].$added;
                    $mod_nick_name = $modulerow['mod_nick_name'] ? $modulerow['mod_nick_name'] : $modulerow['mod_name'];
          $newEntry=new stdClass();
          $newEntry->label=xlt($mod_nick_name);
          $newEntry->url=$relative_link;
          $newEntry->requirement=0;
          $newEntry->target='mod';
          array_push($menu_list->children,$newEntry);
       }
    }
}
function update_visit_forms(&$menu_list)
{
    $baseURL="/interface/patient_file/encounter/load_form.php?formname=";
    $menu_list->children=array();
$lres = sqlStatement("SELECT * FROM list_options " .
  "WHERE list_id = 'lbfnames' AND activity = 1 ORDER BY seq, title");
if (sqlNumRows($lres)) {
  while ($lrow = sqlFetchArray($lres)) {
    $option_id = $lrow['option_id']; // should start with LBF
    $title = $lrow['title'];
    $formURL=$baseURL . urlencode($option_id);
    $formEntry=new stdClass();
    $formEntry->label=xl_form_title($title);
    $formEntry->url=$formURL;
    $formEntry->requirement=2;
    $formEntry->target='enc';
    array_push($menu_list->children,$formEntry);
  }
}

    $reg = getRegistered();
    if (!empty($reg)) {
      foreach ($reg as $entry) {
        $option_id = $entry['directory'];
              $title = trim($entry['nickname']);
        if ($option_id == 'fee_sheet' ) continue;
        if ($option_id == 'newpatient') continue;
        if (empty($title)) $title = $entry['name'];

        $formURL=$baseURL . urlencode($option_id);
        $formEntry=new stdClass();
        $formEntry->label=xl_form_title($title);
        $formEntry->url=$formURL;
        $formEntry->requirement=2;
        $formEntry->target='enc';
        array_push($menu_list->children,$formEntry);
      }
    }
}
function menu_update_entries(&$menu_list)
{
    global $menu_update_map;
    for($idx=0;$idx<count($menu_list);$idx++)
    {

        $entry = $menu_list[$idx];
        if(!isset($entry->url))
        {
            if(isset($menu_update_map[$entry->label]))
            {
                $menu_update_map[$entry->label]($entry);
            }
        }
        // Translate the labels
        $entry->label=xlt($entry->label);
        // Recursive update of children
        if(isset($entry->children))
        {
            menu_update_entries($entry->children);
        }
    }
}

function menu_apply_restrictions(&$menu_list_src,&$menu_list_updated)
{
    for ($idx=0; $idx<count($menu_list_src); $idx++)
    {
        $srcEntry = $menu_list_src[$idx];
        $includeEntry = true;

        // If the entry has an ACL Requirements(currently only support loose), then test
        if (isset($srcEntry->acl_req))
        {

            if (is_array($srcEntry->acl_req[0]))
            {
                $noneSet = true;

                for ($aclIdx=0; $aclIdx<count($srcEntry->acl_req); $aclIdx++)
                {
                    $curSettingOne = $srcEntry->acl_req[$aclIdx][0];
                    $curSettingTwo = $srcEntry->acl_req[$aclIdx][1];
                    // ! at the start of the $curSettingOne means test the negation
                    if (substr($curSettingOne,0,1) === '!')
                    {
                        $curSettingOne = substr($curSettingOne,1);
                        // If the acl_check fails, then show it
                        if (!acl_check($curSettingOne,$curSettingTwo))
                        {
                            $noneSet = false;
                        }
                    }
                    else
                    {
                        // If the acl_check passes, then show it
                        if (acl_check($curSettingOne,$curSettingTwo))
                        {
                            $noneSet = false;
                        }
                    }

                }
                if ($noneSet)
                {
                    $includeEntry = false;
                }
            }
            else
            {
                if (!acl_check($srcEntry->acl_req[0],$srcEntry->acl_req[1]))
                {
                    $includeEntry = false;
                }
            }
        }

        // If the entry has loose global setting requirements, check
        // Note that global_req is a loose check (if more than 1 global, only 1 needs to pass to show the menu item)
        if (isset($srcEntry->global_req))
        {
            if (is_array($srcEntry->global_req))
            {
                $noneSet = true;
                for ($globalIdx=0; $globalIdx<count($srcEntry->global_req); $globalIdx++)
                {
                    $curSetting = $srcEntry->global_req[$globalIdx];
                    // ! at the start of the string means test the negation
                    if (substr($curSetting,0,1) === '!')
                    {
                        $curSetting = substr($curSetting,1);
                        // If the global isn't set at all, or if it is false, then show it
                        if (!isset($GLOBALS[$curSetting]) || !$GLOBALS[$curSetting])
                        {
                            $noneSet = false;
                        }
                    }
                    else
                    {
                        // If the setting is both set and true, then show it
                        if (isset($GLOBALS[$curSetting]) && $GLOBALS[$curSetting])
                        {
                            $noneSet = false;
                        }
                    }

                }
                if ($noneSet)
                {
                    $includeEntry = false;
                }
            }
            else
            {
                // ! at the start of the string means test the negation
                if (substr($srcEntry->global_req,0,1) === '!')
                {
                    $globalSetting=substr($srcEntry->global_req,1);
                    // If the setting is both set and true, then skip this entry
                    if (isset($GLOBALS[$globalSetting]) && $GLOBALS[$globalSetting])
                    {
                        $includeEntry = false;
                    }
                }
                else
                {
                    // If the global isn't set at all, or if it is false then skip the entry
                    if (!isset($GLOBALS[$srcEntry->global_req]) || !$GLOBALS[$srcEntry->global_req])
                    {
                        $includeEntry = false;
                    }
                }
            }
        }

        // If the entry has strict global setting requirements, check
        // Note that global_req_strict is a strict check (if more than 1 global, they all need to pass to show the menu item)
        if (isset($srcEntry->global_req_strict))
        {
            if (is_array($srcEntry->global_req_strict))
            {
                $allSet = true;
                for ($globalIdx=0; $globalIdx<count($srcEntry->global_req_strict); $globalIdx++)
                {
                    $curSetting = $srcEntry->global_req_strict[$globalIdx];
                    // ! at the start of the string means test the negation
                    if (substr($curSetting,0,1) === '!')
                    {
                        $curSetting = substr($curSetting,1);
                        // If the setting is both set and true, then do not show it
                        if (isset($GLOBALS[$curSetting]) && $GLOBALS[$curSetting])
                        {
                            $allSet = false;
                        }
                    }
                    else
                    {
                        // If the global isn't set at all, or if it is false, then do not show it
                        if (!isset($GLOBALS[$curSetting]) || !$GLOBALS[$curSetting])
                        {
                            $allSet = false;
                        }
                    }

                }
                if (!$allSet)
                {
                    $includeEntry = false;
                }
            }
            else
            {
                // ! at the start of the string means test the negation
                if (substr($srcEntry->global_req_strict,0,1) === '!')
                {
                    $globalSetting=substr($srcEntry->global_req_strict,1);
                    // If the setting is both set and true, then skip this entry
                    if (isset($GLOBALS[$globalSetting]) && $GLOBALS[$globalSetting])
                    {
                        $includeEntry = false;
                    }
                }
                else
                {
                    // If the global isn't set at all, or if it is false then skip the entry
                    if (!isset($GLOBALS[$srcEntry->global_req_strict]) || !$GLOBALS[$srcEntry->global_req_strict])
                    {
                        $includeEntry = false;
                    }
                }
            }
        }

        if(isset($srcEntry->children))
        {
            // Iterate through and check the child elements
            $checked_children=array();
            menu_apply_restrictions($srcEntry->children,$checked_children);
            $srcEntry->children=$checked_children;
        }

        if(!isset($srcEntry->url))
        {
            // If this is a header only entry, and there are no child elements, then don't include it in the list.
            if(count($srcEntry->children)===0)
            {
                $includeEntry=false;
            }
        }
        if($includeEntry)
        {

            array_push($menu_list_updated,$srcEntry);
        }
    }
}
