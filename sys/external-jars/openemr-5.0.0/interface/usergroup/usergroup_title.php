<?php
include_once("../globals.php");
?>

<html>
<head>

<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">

</head>
<body class="body_title">

<?php
$res = sqlQuery("select * from users where username='".$_SESSION{"authUser"}."'");
?>

<table border="0" cellspacing="0" cellpadding="0" width="100%" height="100%">
<tr>
<td valign="middle" nowrap>
<span class="title_bar_top"><?php xl('Logged in','e');?>: <?php echo $res{"fname"}." ".$res{"lname"};?></span>
</td>

<td align="right" valign="middle" nowrap>
<span class="title_bar_top"><?php echo dateformat();?></span>
</td>

</tr>
</table>

</body>
</html>
