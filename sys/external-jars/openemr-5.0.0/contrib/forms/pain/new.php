<!-- Form generated from formsWiz -->
<?php
include_once("../../globals.php");
include_once("$srcdir/api.inc");
formHeader("Form: pain");
?>
<html><head>
<?php html_header_show();?>
<link rel="stylesheet" href="<?php echo $css_header;?>" type="text/css">
</head>
<body class="body_top">
<form method=post action="<?php echo $rootdir;?>/forms/pain/save.php?mode=new" name="my_form">
<span class="title">Pain Evaluation</span><br><br>


<input type=checkbox name='dull'  ><span class=text>Dull</span>
<input type=checkbox name='colicky'  ><span class=text>Colicky</span>
<input type=checkbox name='sharp'  ><span class=text>Sharp</span>
<span class=text>Duration of Pain: </span><input type=entry name="duration_of_pain" value="" ><br>


<span class=text>History of Pain: </span><br><textarea cols=40 rows=4 wrap=virtual name="history_of_pain" ></textarea><br>


<table><tr><td>
<table><tr>
<td><span class=text>Accompanying Symptoms Vomitting: </span></td><td><input type=entry name="accompanying_symptoms_vomitting" value="" ></td>
</tr><tr>
<td><span class=text>Accompanying Symptoms Nausea: </span></td><td><input type=entry name="accompanying_symptoms_nausea" value="" ></td>
</tr><tr>
<td><span class=text>Accompanying Symptoms Headache: </span></td><td><input type=entry name="accompanying_symptoms_headache" value="" ></td>
</tr></table>
</td><td>
<span class=text>Accompanying Symptoms Other: </span><br><textarea cols=40 rows=8 wrap=virtual name="accompanying_symptoms_other" ></textarea><br>
</td></tr></table>

<table>
<tr><td>
<span class=text>Pain Referred to Other Sites?: </span><br><textarea cols=40 rows=4 wrap=virtual name="pain_referred_to_other_sites" ></textarea>
</td><td>
<span class=text>What Relieves Pain?: </span><br><textarea cols=40 rows=4 wrap=virtual name="what_relieves_pain" ></textarea>
</td></tr><tr><td>
<span class=text>What Makes Pain Worse (Movement/Positions/Activities)?: </span><br><textarea cols=40 rows=4 wrap=virtual name="what_makes_pain_worse" ></textarea>
</td><td>
<span class=text>Additional Notes: </span><br><textarea cols=40 rows=4 wrap=virtual name="additional_notes" ></textarea>
</td></tr></table>

<br>
<a href="javascript:document.my_form.submit();" class="link_submit">[Save]</a>
<br>
<a href="<?php echo $GLOBALS['form_exit_url']; ?>" class="link">[Don't Save]</a>
</form>
<?php
formFooter();
?>
