<!-- Form created by Andres paglayan -->
<?php
include_once("../../globals.php");
?>
<html><head>
<?php html_header_show();?>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
</head>
<body class="body_top">

<?php
include_once("$srcdir/api.inc");
$obj = formFetch("form_ped_GI", $_GET["id"]);
?>

<form method=post action="<?php echo $rootdir?>/forms/ped_GI/save.php?mode=update&id=<?php echo $_GET["id"];?>" name="my_form">
<span class="title">Pediatric Gastro Intestinal Evaluation</span><br><br>

<a href="javascript:top.restoreSession();document.my_form.submit();" class="link_submit">[Save]</a>
<br>
<a href="<?php echo $GLOBALS['form_exit_url']; ?>" class="link" onclick="top.restoreSession()">[Don't Save Changes]</a>
<br></br>
<!-- Form goes here -->

<?php
	include ('form.php');
?>

<!-- Form ends here -->
<a href="javascript:top.restoreSession();document.my_form.submit();" class="link_submit">[Save]</a>
<br>
<a href="<?php echo $GLOBALS['form_exit_url']; ?>" class="link" onclick="top.restoreSession()">[Don't Save Changes]</a>

</form>
<?php
formFooter();
?>
