<?php
// +-----------------------------------------------------------------------------+ 
// Copyright (C) 2011 Z&H Consultancy Services Private Limited <sam@zhservices.com>
//
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
//
// A copy of the GNU General Public License is included along with this program:
// openemr/interface/login/GnuGPL.html
// For more information write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// 
// Author:   Eldho Chacko <eldho@zhservices.com>
//           Jacob T Paul <jacob@zhservices.com>
//
// +------------------------------------------------------------------------------+

//SANITIZE ALL ESCAPES
$sanitize_all_escapes=true;
//

//STOP FAKE REGISTER GLOBALS
$fake_register_globals=false;
//

require_once("../../interface/globals.php");

$templateid = $_REQUEST['templateid'];
$Source = $_REQUEST['source'];
$list_id = $_REQUEST['list_id'];
$item = $_REQUEST['item'];
$multi = $_REQUEST['multi'];
$content = $_REQUEST['content'];

if($Source=="add_template"){
    $arr = explode("|",$multi);
    
    for($i=0;$i<sizeof($arr)-1;$i++){
    $sql = sqlStatement("SELECT * FROM customlists AS cl LEFT OUTER JOIN template_users AS tu ON cl.cl_list_slno=tu.tu_template_id
                        WHERE cl_list_item_long=? AND cl_list_type=3 AND cl_deleted=0 AND cl_list_id=? AND tu.tu_user_id=?",array($templateid,$arr[$i],$_SESSION['authId']));
    $cnt = sqlNumRows($sql);
    if($cnt==0){
    $newid=sqlInsert("INSERT INTO customlists (cl_list_id,cl_list_type,cl_list_item_long,cl_creator) VALUES (?,?,?,?)",array($arr[$i],3,$templateid,$_SESSION['authId']));
    sqlInsert("INSERT INTO template_users (tu_user_id,tu_template_id) VALUES (?,?)",array($_SESSION['authId'],$newid));
    }
    echo "<select name='template' id='template' onchange='TemplateSentence(this.value)' style='width:180px'>";
    echo "<option value=''>".htmlspecialchars(xl('Select category'),ENT_QUOTES)."</option>";
        $resTemplates = sqlStatement("SELECT * FROM template_users AS tu LEFT OUTER JOIN customlists AS c ON tu.tu_template_id=c.cl_list_slno WHERE
                                     tu.tu_user_id=? AND c.cl_list_type=3 AND cl_list_id=? AND cl_deleted=0 ORDER BY tu.tu_template_order,
                                     c.cl_list_item_long",array($_SESSION['authId'],$list_id));
        while($rowTemplates = sqlFetchArray($resTemplates)){
        echo "<option value='".htmlspecialchars($rowTemplates['cl_list_slno'],ENT_QUOTES)."'>".htmlspecialchars($rowTemplates['cl_list_item_long'],ENT_QUOTES)."</option>";
        }
    echo "</select>";
    }
}
else if($Source=="save_provider"){
    $arr = explode("|",$multi);
    for($i=0;$i<sizeof($arr)-1;$i++){
        $cnt = sqlNumRows(sqlStatement("SELECT * FROM template_users WHERE tu_user_id=? AND tu_template_id=?",array($arr[$i],$list_id)));
        if(!$cnt){
        sqlInsert("INSERT INTO template_users (tu_user_id,tu_template_id) VALUES (?,?)",array($arr[$i],$list_id));
        }
    }
}
else if($Source=="add_item"){
    $row = sqlQuery("SELECT max(cl_order)+1 as order1 FROM customlists WHERE cl_list_id=?",array($templateid));
    $order = $row['order1'];
    $newid = sqlInsert("INSERT INTO customlists (cl_list_id,cl_list_type,cl_list_item_long,cl_order,cl_creator) VALUES (?,?,?,?,?)",array($templateid,4,$item,$order,$_SESSION['authId']));
    sqlInsert("INSERT INTO template_users (tu_user_id,tu_template_id,tu_template_order) VALUES (?,?,?)",array($_SESSION['authId'],$newid,$order));
}
else if($Source=="delete_item"){
    sqlStatement("DELETE FROM template_users WHERE tu_template_id=? AND tu_user_id=?",array($item,$_SESSION['authId']));
}
else if($Source=="update_item"){
    $row = sqlQuery("SELECT max(cl_order)+1 as order1 FROM customlists WHERE cl_list_id=?",array($templateid));
    $order = $row['order1'];
    $newid = sqlInsert("INSERT INTO customlists (cl_list_id,cl_list_type,cl_list_item_long,cl_order,cl_creator) VALUES (?,?,?,?,?)",array($templateid,4,$content,$order,$_SESSION['authId']));
    sqlStatement("UPDATE template_users SET tu_template_id=? WHERE tu_template_id=? AND tu_user_id=?",array($newid,$item,$_SESSION['authId']));
}
else if($Source=='item_show'){
    $sql = "SELECT * FROM customlists WHERE cl_list_id=? AND cl_list_type=4 AND cl_deleted=0";
    $res = sqlStatement($sql,array($list_id));
    $selcat = sqlQuery("SELECT * FROM customlists WHERE cl_list_slno=? AND cl_list_type=3 AND cl_deleted=0",array($list_id));
    $selcont = sqlQuery("SELECT * FROM customlists WHERE cl_list_slno=? AND cl_list_type=2 AND cl_deleted=0",array($selcat['cl_list_id']));
    $cnt =sqlNumRows($res);
    if($cnt){
        echo "<table width='100%'>";
        echo "<tr class='text'><th colspan=2  style='background-color:#ffffff'>".htmlspecialchars(xl('Preview of')," ".$selcat['cl_list_item_long']."(".$selcont['cl_list_item_long'].")",ENT_QUOTES)."</th></tr>";
        $i=0;
        while($row=sqlFetchArray($res)){
            $i++;
            $class = ($class=='reportTableOddRow') ? 'reportTableEvenRow' : 'reportTableOddRow';
            echo "<tr class='text'><td style='background-color:#ffffff'>".$i."</td><td style='background-color:#ffffff'>".htmlspecialchars($row['cl_list_item_long'],ENT_QUOTES)."</td></tr>";
        }
        echo "</table>";
    }
    else{
        echo "<table width='100%'>";
        echo "<tr class='text'><th colspan=2  style='background-color:#ffffff'>".htmlspecialchars(xl('No items under selected category'),ENT_QUOTES)."</th></tr>";
        echo "</table>";
    }
    $Source="add_template";
}
else if($Source=='check_item'){
    $sql=sqlStatement("SELECT * FROM template_users WHERE tu_template_id=? AND tu_user_id=?",array($item,$list_id));
    $cnt=sqlNumRows($sql);
    if($cnt){
        echo htmlspecialchars(xl("OK"),ENT_QUOTES);
    }
    else{
        echo htmlspecialchars(xl("FAIL"),ENT_QUOTES);
    }
    $Source="add_template";
}
else if($Source=='display_item'){
    $multi = preg_replace('/\|$/','',$multi);
    $val = str_replace("|",",",$multi);
    echo "<select multiple name='topersonalizeditem[]' id='topersonalizeditem' size='6' style='width:220px' onchange='display_item()'>";
    $resTemplates = sqlStatement("SELECT * FROM customlists WHERE cl_list_type=4 AND cl_deleted=0 AND cl_list_id IN ($val) ORDER BY cl_list_item_long");
        while($rowTemplates = sqlFetchArray($resTemplates)){
        echo "<option value='".htmlspecialchars($rowTemplates['cl_list_slno'],ENT_QUOTES)."'>".htmlspecialchars($rowTemplates['cl_list_item_long'],ENT_QUOTES)."</option>";
        }
    echo "</select>";
    $Source="add_template";
}
else if($Source=='delete_category'){
    $res = sqlStatement("SELECT * FROM template_users AS tu LEFT OUTER JOIN users AS u ON tu.tu_user_id=u.id WHERE tu_template_id=? AND tu.tu_user_id!=?",array($templateid,$_SESSION['authId']));
    $users ='';
    $i=0;
    while($row=sqlFetchArray($res)){
        $i++;
        $users .= $i.")".$row['fname']." ".$row['lname']."\n";
    }
    echo htmlspecialchars($users,ENT_QUOTES);
    $Source="add_template";
}
else if($Source=='delete_full_category'){
    sqlStatement("UPDATE customlists SET cl_deleted=? WHERE cl_list_slno=?",array(1,$templateid));
    sqlStatement("DELETE template_users WHERE tu_template_id=?",array($templateid));
    $res = sqlStatement("SELECT * FROM customlists AS cl WHERE cl_list_id=?",array($templateid));
    while($row=sqlFetchArray($res)){
        sqlStatement("UPDATE customlists SET cl_deleted=1 WHERE cl_list_slno=?",array($row['cl_list_slno']));
        sqlStatement("DELETE template_users WHERE tu_template_id=?",array($row['cl_list_slno']));
    }

    $Source="add_template";
}
else if($Source=='checkcontext'){
    $res = sqlStatement("SELECT * FROM customlists WHERE cl_deleted=0 AND cl_list_type=3 AND cl_list_id=?",array($list_id));
    if(sqlNumRows($res)){
        echo "1";
    }
    else{
        echo "0";
    }
    $Source="add_template";
}
if($Source!="add_template"){
    $res= sqlStatement("SELECT * FROM customlists AS cl LEFT  OUTER JOIN template_users AS tu ON cl.cl_list_slno=tu.tu_template_id
                        WHERE cl_list_type=4 AND cl_list_id=? AND cl_deleted=0 AND tu.tu_user_id=? ORDER BY tu.tu_template_order",
                        array($templateid,$_SESSION['authId']));
    $i=0;
    while($row = sqlFetchArray($res)){
        $i++;
        echo "<li id='clorder_".htmlspecialchars($row['cl_list_slno'],ENT_QUOTES)."' style='cursor:pointer'><span>";
        if(acl_check('nationnotes', 'nn_configure')){
        echo "<img src='../../images/b_edit.png' onclick=update_item_div('".htmlspecialchars($row['cl_list_slno'],ENT_QUOTES)."')>";
        }
        echo "<div style='display:inline' id='".htmlspecialchars($row['cl_list_slno'],ENT_QUOTES)."' onclick=\"moveOptions_11('".htmlspecialchars($row['cl_list_slno'],ENT_QUOTES)."', 'textarea1');\">".htmlspecialchars($row['cl_list_item_long'],ENT_QUOTES)."</div>";
        if(acl_check('nationnotes', 'nn_configure')){
        echo "<img src='../../images/deleteBtn.png' onclick=\"delete_item('".htmlspecialchars($row['cl_list_slno'],ENT_QUOTES)."')\">";
        echo "<div id='update_item".htmlspecialchars($row['cl_list_slno'],ENT_QUOTES)."' style='display:none'><textarea name='update_item_txt".htmlspecialchars($row['cl_list_slno'],ENT_QUOTES)."' id='update_item_txt".htmlspecialchars($row['cl_list_slno'],ENT_QUOTES)."'>".htmlspecialchars($row['cl_list_item_long'],ENT_QUOTES)."</textarea></br>";
        echo "<input type='button' name='update' onclick=update_item('".$row['cl_list_slno']."') value='".htmlspecialchars(xl('Update'),ENT_QUOTES)."'><input type='button' name='cancel' value='". htmlspecialchars(xl('Cancel'),ENT_QUOTES)."' onclick=cancel_item('".htmlspecialchars($row['cl_list_slno'],ENT_QUOTES)."')></div>";
        }
        echo "</span></li>";
    }
    if(acl_check('nationnotes', 'nn_configure') && $templateid){
    echo "<li style='cursor:pointer'><span onclick='add_item()'>".htmlspecialchars(xl('Click to add new components'),ENT_QUOTES);
    echo "</span><div id='new_item' style='display:none'>";
    echo "<textarea name='item' id='item'></textarea></br>";
    echo "<input type='button' name='save' value='". htmlspecialchars(xl('Save'),ENT_QUOTES)."' onclick='save_item()'><input type='button' name='cancel' value='". htmlspecialchars(xl('Cancel'),ENT_QUOTES)."' onclick=cancel_item('".htmlspecialchars($row['cl_list_slno'],ENT_QUOTES)."')></div></li>";
    }
}
?>