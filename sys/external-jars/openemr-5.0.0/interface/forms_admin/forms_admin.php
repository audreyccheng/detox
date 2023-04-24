<?php 
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

//INCLUDES, DO ANY ACTIONS, THEN GET OUR DATA
include_once("../globals.php");
include_once("$srcdir/registry.inc");
include_once("$srcdir/sql.inc");
if ($_GET['method'] == "enable"){
	updateRegistered ( $_GET['id'], "state=1" );
}
elseif ($_GET['method'] == "disable"){
	updateRegistered ( $_GET['id'], "state=0" );
}
elseif ($_GET['method'] == "install_db"){
	$dir = getRegistryEntry ( $_GET['id'], "directory" );
	if (installSQL ("$srcdir/../interface/forms/{$dir['directory']}"))
		updateRegistered ( $_GET['id'], "sql_run=1" );
	else
		$err = xl('ERROR: could not open table.sql, broken form?');
}
elseif ($_GET['method'] == "register"){
	registerForm ( $_GET['name'] ) or $err=xl('error while registering form!');
}
$bigdata = getRegistered("%") or $bigdata = false;


//START OUT OUR PAGE....
?>
<html>
<head>
<?php html_header_show();?>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
</head>
<body class="body_top">
<span class="title"><?php xl('Forms Administration','e');?></span>
<br><br>
<?php
	foreach($_POST as $key=>$val) {
	       if (preg_match('/nickname_(\d+)/', $key, $matches)) {
               		$nickname_id = $matches[1];
			sqlQuery("update registry set nickname='".$val."' where id=".$nickname_id);
		}
	       if (preg_match('/category_(\d+)/', $key, $matches)) {
               		$category_id = $matches[1];
			sqlQuery("update registry set category='".$val."' where id=".$category_id);
		}
	       if (preg_match('/priority_(\d+)/', $key, $matches)) {
               		$priority_id = $matches[1];
			sqlQuery("update registry set priority='".$val."' where id=".$priority_id);
		}
        }
?>


<?php //ERROR REPORTING
if ($err)
	echo "<span class=bold>$err</span><br><br>\n";
?>


<?php //REGISTERED SECTION ?>
<span class=bold><?php xl('Registered','e');?></span><br>
<form method=POST action ='./forms_admin.php'>
<i><?php xl('click here to update priority, category and nickname settings','e'); ?></i>
<input type=submit name=update value='<?php xl('update','e'); ?>'><br> 
<table border=0 cellpadding=1 cellspacing=2 width="500">
	<tr>
		<td> </td>
		<td> </td>
		<td> </td>
		<td> </td>
		<td> </td>
		<td><?php xl('Priority ','e'); ?></td>
		<td><?php xl('Category ','e'); ?></td>
		<td><?php xl('Nickname','e'); ?></td>
	</tr>
<?php
$color="#CCCCCC";
if ($bigdata != false)
foreach($bigdata as $registry)
{
	$priority_category = sqlQuery("select priority, category, nickname from registry where id=".$registry['id']);
	?>
	<tr>
		<td bgcolor="<?php echo $color?>" width="2%">
			<span class=text><?php echo $registry['id'];?></span> 
		</td>
		<td bgcolor="<?php echo $color?>" width="30%">
			<span class=bold><?php echo xl_form_title($registry['name']); ?></span> 
		</td>
		<?php
			if ($registry['sql_run'] == 0)
				echo "<td bgcolor='$color' width='10%'><span class='text'>".xl('registered')."</span>";
			elseif ($registry['state'] == "0")
				echo "<td bgcolor='#FFCCCC' width='10%'><a class=link_submit href='./forms_admin.php?id={$registry['id']}&method=enable'>".xl('disabled')."</a>";
			else
				echo "<td bgcolor='#CCFFCC' width='10%'><a class=link_submit href='./forms_admin.php?id={$registry['id']}&method=disable'>".xl('enabled')."</a>";
		?></td>
		<td bgcolor="<?php $color?>" width="10%">
			<span class=text><?php

			if ($registry['unpackaged'])
				echo xl('PHP extracted','e');
			else
				echo xl('PHP compressed','e');
			
			?></span> 
		</td>
		<td bgcolor="<?php echo $color?>" width="10%">
			<?php
			if ($registry['sql_run'])
				echo "<span class=text>".xl('DB installed')."</span>";
			else
				echo "<a class=link_submit href='./forms_admin.php?id={$registry['id']}&method=install_db'>".xl('install DB')."</a>";
			?> 
		</td>
		<?php
			echo "<td><input type=text size=4 name=priority_".$registry['id']." value='".$priority_category['priority']."'></td>";
			echo "<td><input type=text size=8 name=category_".$registry['id']." value='".$priority_category['category']."'></td>";
			echo "<td><input type=text size=8 name=nickname_".$registry['id']." value='".$priority_category['nickname']."'></td>";
		?>
	</tr>
	<?php
	if ($color=="#CCCCCC")
		$color="#999999";
	else
		$color="#CCCCCC";
} //end of foreach
	?>
</table>
<hr>

<?php  //UNREGISTERED SECTION ?>
<span class=bold><?php xl('Unregistered','e');?></span><br>
<table border=0 cellpadding=1 cellspacing=2 width="500">
<?php
$dpath = "$srcdir/../interface/forms/";
$dp = opendir($dpath);
$color="#CCCCCC";
for ($i=0; false != ($fname = readdir($dp)); $i++)
	if ($fname != "." && $fname != ".." && $fname != "CVS" && $fname != "LBF" &&
    (is_dir($dpath.$fname) || stristr($fname, ".tar.gz") ||
    stristr($fname, ".tar") || stristr($fname, ".zip") ||
    stristr($fname, ".gz")))
		$inDir[$i] = $fname;

// ballards 11/05/2005 fixed bug in removing registered form from the list
if ($bigdata != false)
{
	foreach ( $bigdata as $registry )
	{
		$key = array_search($registry['directory'], $inDir) ;  /* returns integer or FALSE */
		unset($inDir[$key]);
	}
}

foreach ( $inDir as $fname )
{
        // added 8-2009 by BM - do not show the metric vitals form as option since deprecated
	//  also added a toggle in globals.php in case user wants the option to add this deprecated form
        if (($fname == "vitalsM") && ($GLOBALS['disable_deprecated_metrics_form'])) continue;
    
	if (stristr($fname, ".tar.gz") || stristr($fname, ".tar") || stristr($fname, ".zip") || stristr($fname, ".gz"))
		$phpState = "PHP compressed";
	else
		$phpState =  "PHP extracted";
	?>
	<tr>
		<td bgcolor="<?php echo $color?>" width="1%">
			<span class=text> </span> 
		</td>
		<td bgcolor="<?php echo $color?>" width="20%">
	        <?php
                $form_title_file = @file($GLOBALS['srcdir']."/../interface/forms/$fname/info.txt");
                        if ($form_title_file)
                                $form_title = $form_title_file[0];
                        else
                                $form_title = $fname;
                ?>
			<span class=bold><?php echo xl_form_title($form_title); ?></span> 
		</td>
		<td bgcolor="<?php echo $color?>" width="10%"><?php
			if ($phpState == "PHP extracted")
				echo '<a class=link_submit href="./forms_admin.php?name=' . urlencode($fname) . '&method=register">' . xl('register') . '</a>';
			else
				echo '<span class=text>' . xl('n/a') . '</span>';
		?></td>
		<td bgcolor="<?php echo $color?>" width="20%">
			<span class=text><?php echo xl($phpState); ?></span> 
		</td>
		<td bgcolor="<?php echo $color?>" width="10%">
			<span class=text><?php xl('n/a','e'); ?></span> 
		</td>
	</tr>
	<?php
	if ($color=="#CCCCCC")
	        $color="#999999";
	else
	        $color="#CCCCCC";
	flush();
}//end of foreach
?>
</table>

</body>
</html>
