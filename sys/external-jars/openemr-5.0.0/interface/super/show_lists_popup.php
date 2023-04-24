<?php
 /*
  * This popup is called when choosing a list for a form layout
  */

include_once("../globals.php");

?>

<html>
<head>
<?php html_header_show();?>
<title><?php xl('List lists','e'); ?></title>
<link rel="stylesheet" href='<?php echo $css_header ?>' type='text/css'>

<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-2-2/index.js"></script>

<style>
h1 {
    font-size: 120%;
    padding: 3px;
    margin: 3px;
}
ul {
    list-style: none;
    padding: 3px;
    margin: 3px;
}
li {
    cursor: pointer;
    border-bottom: 1px solid #ccc;
    background-color: white;
}
.highlight {
    background-color: #336699;
    color: white;
}    
</style>

</head>

<body class="body_top text">
<div id="lists">
<h1><?php xl('Active lists','e'); ?></h1>
<ul>
<?php
$res = sqlStatement("SELECT * FROM list_options WHERE " .
    "list_id = 'lists' AND activity = 1 ORDER BY title");
while ($row = sqlFetchArray($res)) {
    echo "<li id='".$row['option_id']."' class='oneresult'>" . xl($row['title']) . "</li>";
}
?>
</ul>
</div>
</body>

<script language="javascript">

// jQuery stuff to make the page a little easier to use

$(document).ready(function(){
    $(".oneresult").mouseover(function() { $(this).toggleClass("highlight"); });
    $(".oneresult").mouseout(function() { $(this).toggleClass("highlight"); });
    $(".oneresult").click(function() { SelectList(this); });

    var SelectList = function(obj) {
        var listid = $(obj).attr("id");
        if (opener.closed || ! opener.SetList)
            alert('The destination form was closed; I cannot act on your selection.');
        else
            opener.SetList(listid);
        window.close();
        return false;
    };

});

</script>

</html>
