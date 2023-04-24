<?php 
require_once("../../../interface/globals.php");
require_once("./Utils.php");

/* Use this code to identify duplicate patients in OpenEMR
 *
 */
$parameters = GetParameters();

// establish some defaults
if (! isset($parameters['sortby'])) { $parameters['sortby'] = "name"; }
if (! isset($parameters['limit'])) { $parameters['limit'] = 100; }

if (! isset($parameters['match_name']) &&
    ! isset($parameters['match_dob']) &&
    ! isset($parameters['match_sex']) &&
    ! isset($parameters['match_ssn']))
{
    $parameters['match_name'] = 'on';
    $parameters['match_dob'] = 'on';
}
    
$oemrdb = $GLOBALS['dbh'];
?>

<html>
<head>
<script type="text/javascript" src="<?php echo $GLOBALS['assets_static_relative']; ?>/jquery-min-1-2-1/index.js"></script>
<style>
body {
    font-family: arial, helvetica, times new roman;
    font-size: 1em;
    background-color: #eee;
}
.match_block {
    border: 1px solid #eee;
    background-color: white;
    padding: 5px;
}

.match_block table {
    border-collapse: collapse;
}
.match_block table tr {
    cursor: pointer;
}
.match_block table td {
    padding: 5px;
}
    
.highlight {
    background-color: #99a;
    color: white;
}
.highlight_block {
    background-color: #ffa;
}
.bold {
    font-weight: bold;
}
    
</style>
</head>
<body>
<form name="search_form" id="search_form" method="post" action="index.php">
<input type="hidden" name="go" value="Go">
Matching criteria:
<input type="checkbox" name="match_name" id="match_name" <?php if ($parameters['match_name']) echo "CHECKED"; ?>> 
<label for="match_name">Name</label>
<input type="checkbox" name="match_dob" id="match_dob" <?php if ($parameters['match_dob']) echo "CHECKED"; ?>> 
<label for="match_dob">DOB</label>
<input type="checkbox" name="match_sex" id="match_sex" <?php if ($parameters['match_sex']) echo "CHECKED"; ?>> 
<label for="match_sex">Gender</label>
<input type="checkbox" name="match_ssn" id="match_ssn" <?php if ($parameters['match_ssn']) echo "CHECKED"; ?>> 
<label for="match_ssn">SSN</label>
<br>
Order results by:
<input type='radio' name='sortby' value='name' id="name" <?php if ($parameters['sortby']=='name') echo "CHECKED"; ?>>
<label for="name">Name</label>
<input type='radio' name='sortby' value='dob' id="dob" <?php if ($parameters['sortby']=='dob') echo "CHECKED"; ?>>
<label for="dob">DOB</label>
<input type='radio' name='sortby' value='sex' id="sex" <?php if ($parameters['sortby']=='sex') echo "CHECKED"; ?>>
<label for="sex">Gender</label>
<input type='radio' name='sortby' value='ssn' id="ssn" <?php if ($parameters['sortby']=='ssn') echo "CHECKED"; ?>>
<label for="ssn">SSN</label>
<br>
Limit search to first <input type='textbox' size='5' name='limit' id="limit" value='<?php echo $parameters['limit']; ?>'> records
<input type="button" name="do_search" id="do_search" value="Go">
</form>

<div id="thebiglist" style="height: 300px; overflow: auto; border: 1px solid blue;">
<form name="resolve" id="resolve" method="POST" action="dupcheck.php">

<?php
if ($parameters['go'] == "Go") {
    // go and do the search

    // counter that gathers duplicates into groups
    $dupecount = 0;

    // for EACH patient in OpenEMR find potential matches
    $sqlstmt = "select id, pid, fname, lname, dob, sex, ss from patient_data";
    switch ($parameters['sortby']) {
        case 'dob':
            $orderby = " ORDER BY dob";
            break;
        case 'sex':
            $orderby = " ORDER BY sex";
            break;
        case 'ssn':
            $orderby = " ORDER BY ss";
            break;
        case 'name':
        default:
            $orderby = " ORDER BY lname, fname";
            break;
    }
    $sqlstmt .= $orderby;
    if ($parameters['limit']) {
        $sqlstmt .= " LIMIT 0,".$parameters['limit'];
    }

    $qResults = sqlStatement($sqlstmt);
    while ($row = sqlFetchArray($qResults)) {

        if ($dupelist[$row['id']] == 1) continue;

        $sqlstmt = "select id, pid, fname, lname, dob, sex, ss ".
                    " from patient_data where ";
        $sqland = "";
        if ($parameters['match_name']) {
            $sqlstmt .= $sqland . " fname='".$row['fname']."'";
            $sqland = " AND ";
            $sqlstmt .= $sqland . " lname='".$row['lname']."'";
        }
        if ($parameters['match_sex']) {
            $sqlstmt .= $sqland . " sex='".$row['sex']."'";
            $sqland = " AND ";
        }
        if ($parameters['match_ssn']) {
            $sqlstmt .= $sqland . " ss='".$row['ss']."'";
            $sqland = " AND ";
        }
        if ($parameters['match_dob']) {
            $sqlstmt .= $sqland . " dob='".$row['dob']."'";
            $sqland = " AND ";
        }
        $mResults = sqlStatement($sqlstmt);

        if (! $mResults) continue;
        if (sqlNumRows($mResults) <= 1) continue;


        echo "<div class='match_block' style='padding: 5px 0px 5px 0px;' id='dupediv".$dupecount."'>";
        echo "<table>";

        echo "<tr class='onerow' id='".$row['id']."' oemrid='".$row['id']."' dupecount='".$dupecount."' title='Merge duplicates into this record'>";
        echo "<td>".$row['lname'].", ".$row['fname']."</td>";
        echo "<td>".$row['dob']."</td>";
        echo "<td>".$row['sex']."</td>";
        echo "<td>".$row['ss']."</td>";
        echo "<td><input type='button' value=' ? ' class='moreinfo' oemrid='".$row['pid']."' title='More info'></td>";
        echo "</tr>";

        while ($mrow = sqlFetchArray($mResults)) {
            if ($row['id'] == $mrow['id']) continue;
            echo "<tr class='onerow' id='".$mrow['id']."' oemrid='".$mrow['id']."' dupecount='".$dupecount."' title='Merge duplicates into this record'>";
            echo "<td>".$mrow['lname'].", ".$mrow['fname']."</td>";
            echo "<td>".$mrow['dob']."</td>";
            echo "<td>".$mrow['sex']."</td>";
            echo "<td>".$mrow['ss']."</td>";
            echo "<td><input type='button' value=' ? ' class='moreinfo' oemrid='".$mrow['pid']."' title='More info'></td>";
            echo "</tr>";
            // to keep the output clean let's not repeat IDs already tagged as dupes
            $dupelist[$row['id']] = 1;
            $dupelist[$mrow['id']] = 1;
        }
        $dupecount++;

        echo "</table>";
        echo "</div>\n";
    }
}

?>
</div> <!-- end the big list -->
<?php if ($dupecount > 0) : ?>
<div id="dupecounter" style='display:inline;'><?php echo $dupecount; ?></div>
&nbsp;duplicates found
<?php endif; ?>
</form>

</body>

<script language="javascript">

$(document).ready(function(){

    // capture RETURN keypress
    $("#limit").keypress(function(evt) { if (evt.keyCode == 13) $("#do_search").click(); });

    // perform the database search for duplicates
    $("#do_search").click(function() { 
        $("#thebiglist").html("<p style='margin:10px;'><img src='<?php echo $GLOBALS['webroot']; ?>/interface/pic/ajax-loader.gif'> Searching ...</p>");
        $("#search_form").submit();
        return true;
    });

    // pop up an OpenEMR window directly to the patient info
    var moreinfoWin = null; 
    $(".moreinfo").click(function(evt) { 
        if (moreinfoWin) { moreinfoWin.close(); }
        moreinfoWin = window.open("<?php echo $GLOBALS['webroot']; ?>/interface/patient_file/patient_file.php?set_pid="+$(this).attr("oemrid"), "moreinfo");
        evt.stopPropagation();
    });

    // highlight the block of matching records
    $(".match_block").mouseover(function() { $(this).toggleClass("highlight_block"); });
    $(".match_block").mouseout(function() { $(this).toggleClass("highlight_block"); });
    $(".onerow").mouseover(function() { $(this).toggleClass("highlight"); });
    $(".onerow").mouseout(function() { $(this).toggleClass("highlight"); });

    // begin the merge of a block into a single record
    $(".onerow").click(function() {
        var dupecount = $(this).attr("dupecount");
        var masterid = $(this).attr("oemrid");
        var newurl = "mergerecords.php?dupecount="+dupecount+"&masterid="+masterid;
        $("[dupecount="+dupecount+"]").each(function (i) {
            if (this.id != masterid) { newurl += "&otherid[]="+this.id; }
        });
        // open a new window and show the merge results
        moreinfoWin = window.open(newurl, "mergewin");
    });
});

function removedupe(dupeid) {
    // remove the merged records from the list of duplicates
    $("#dupediv"+dupeid).remove();
    // reduce the duplicate counter
    var dcounter = parseInt($("#dupecounter").html());
    $("#dupecounter").html(dcounter-1);
}
    
</script>

</html>
