<?php

// Default values for optional variables that are allowed to be set by callers.

// Unless specified explicitly, apply Auth functions
if (!isset($ignoreAuth)) $ignoreAuth = false;
// Unless specified explicitly, caller is not offsite_portal and Auth is required
if (!isset($ignoreAuth_offsite_portal)) $ignoreAuth_offsite_portal = false;
// Unless specified explicitly, do not reverse magic quotes
if (!isset($sanitize_all_escapes)) $sanitize_all_escapes = false;
// Unless specified explicitly, "fake" register_globals.
if (!isset($fake_register_globals)) $fake_register_globals = true;

// Is this windows or non-windows? Create a boolean definition.
if (!defined('IS_WINDOWS'))
 define('IS_WINDOWS', (stripos(PHP_OS,'WIN') === 0));

// Some important php.ini overrides. Defaults for these values are often
// too small.  You might choose to adjust them further.
//
ini_set('session.gc_maxlifetime', '14400');

// This is for sanitization of all escapes.
//  (ie. reversing magic quotes if it's set)
if ($sanitize_all_escapes) {
  if (get_magic_quotes_gpc()) {
    function undoMagicQuotes($array, $topLevel=true) {
      $newArray = array();
      foreach($array as $key => $value) {
        if (!$topLevel) {
          $key = stripslashes($key);
        }
        if (is_array($value)) {
          $newArray[$key] = undoMagicQuotes($value, false);
        }
        else {
          $newArray[$key] = stripslashes($value);
        }
      }
      return $newArray;
    }
    $_GET = undoMagicQuotes($_GET);
    $_POST = undoMagicQuotes($_POST);
    $_COOKIE = undoMagicQuotes($_COOKIE);
    $_REQUEST = undoMagicQuotes($_REQUEST);
  }
}

//
// The webserver_root and web_root are now automatically collected.
// If not working, can set manually below.
// Auto collect the full absolute directory path for openemr.
$webserver_root = dirname(dirname(__FILE__));
if (IS_WINDOWS) {
 //convert windows path separators
 $webserver_root = str_replace("\\","/",$webserver_root);
}
// Collect the apache server document root (and convert to windows slashes, if needed)
$server_document_root = realpath($_SERVER['DOCUMENT_ROOT']);
if (IS_WINDOWS) {
 //convert windows path separators
 $server_document_root = str_replace("\\","/",$server_document_root);
}
// Auto collect the relative html path, i.e. what you would type into the web
// browser after the server address to get to OpenEMR.
// This removes the leading portion of $webserver_root that it has in common with the web server's document
// root and assigns the result to $web_root. In addition to the common case where $webserver_root is
// /var/www/openemr and document root is /var/www, this also handles the case where document root is
// /var/www/html and there is an Apache "Alias" command that directs /openemr to /var/www/openemr.
$web_root = substr($webserver_root, strspn($webserver_root ^ $server_document_root, "\0"));
// Ensure web_root starts with a path separator
if (preg_match("/^[^\/]/",$web_root)) {
 $web_root = "/".$web_root;
}
// The webserver_root and web_root are now automatically collected in
//  real time per above code. If above is not working, can uncomment and
//  set manually here:
//   $webserver_root = "/var/www/openemr";
//   $web_root =  "/openemr";
//

// This is the directory that contains site-specific data.  Change this
// only if you have some reason to.
$GLOBALS['OE_SITES_BASE'] = "$webserver_root/sites";

// The session name names a cookie stored in the browser.
// Now that restore_session() is implemented in javaScript, session IDs are
// effectively saved in the top level browser window and there is no longer
// any need to change the session name for different OpenEMR instances.
session_name("OpenEMR");

session_start();

// Set the site ID if required.  This must be done before any database
// access is attempted.
if (empty($_SESSION['site_id']) || !empty($_GET['site'])) {
  if (!empty($_GET['site'])) {
    $tmp = $_GET['site'];
  }
  else {
    if (empty($ignoreAuth)) die("Site ID is missing from session data!");
    $tmp = $_SERVER['HTTP_HOST'];
    if (!is_dir($GLOBALS['OE_SITES_BASE'] . "/$tmp")) $tmp = "default";
  }
  if (empty($tmp) || preg_match('/[^A-Za-z0-9\\-.]/', $tmp))
    die("Site ID '". htmlspecialchars($tmp,ENT_NOQUOTES) . "' contains invalid characters.");
  if (isset($_SESSION['site_id']) && ($_SESSION['site_id'] != $tmp)) {
    // This is to prevent using session to penetrate other OpenEMR instances within same multisite module
    session_unset(); // clear session, clean logout
    if (isset($landingpage) && !empty($landingpage)) {
      // OpenEMR Patient Portal use
      header('Location: index.php?site='.$tmp);
    }
    else {
      // Main OpenEMR use
      header('Location: ../login/login.php?site='.$tmp); // Assuming in the interface/main directory
    }
    exit;
  }
  if (!isset($_SESSION['site_id']) || $_SESSION['site_id'] != $tmp) {
    $_SESSION['site_id'] = $tmp;
    //error_log("Session site ID has been set to '$tmp'"); // debugging
  }
}

// Set the site-specific directory path.
$GLOBALS['OE_SITE_DIR'] = $GLOBALS['OE_SITES_BASE'] . "/" . $_SESSION['site_id'];

require_once($GLOBALS['OE_SITE_DIR'] . "/config.php");

// Collecting the utf8 disable flag from the sqlconf.php file in order
// to set the correct html encoding. utf8 vs iso-8859-1. If flag is set
// then set to iso-8859-1.
require_once(dirname(__FILE__) . "/../library/sqlconf.php");
if (!$disable_utf8_flag) {
 ini_set('default_charset', 'utf-8');
 $HTML_CHARSET = "UTF-8";
 mb_internal_encoding('UTF-8');
}
else {
 ini_set('default_charset', 'iso-8859-1');
 $HTML_CHARSET = "ISO-8859-1";
 mb_internal_encoding('ISO-8859-1');
}

// Root directory, relative to the webserver root:
$GLOBALS['rootdir'] = "$web_root/interface";
$rootdir = $GLOBALS['rootdir'];
// Absolute path to the source code include and headers file directory (Full path):
$GLOBALS['srcdir'] = "$webserver_root/library";
// Absolute path to the location of documentroot directory for use with include statements:
$GLOBALS['fileroot'] = "$webserver_root";
// Absolute path to the location of interface directory for use with include statements:
$include_root = "$webserver_root/interface";
// Absolute path to the location of documentroot directory for use with include statements:
$GLOBALS['webroot'] = $web_root;

// Static assets directory, relative to the webserver root.
// (it is very likely that this path will be changed in the future))
$GLOBALS['assets_static_relative'] = "$web_root/public/assets";

// Relative images directory, relative to the webserver root.
$GLOBALS['images_static_relative'] = "$web_root/public/images";

// Static images directory, absolute to the webserver root.
$GLOBALS['images_static_absolute'] = "$webserver_root/public/images";

//Composer vendor directory, absolute to the webserver root.
$GLOBALS['vendor_dir'] = "$webserver_root/vendor";

$GLOBALS['template_dir'] = $GLOBALS['fileroot'] . "/templates/";
$GLOBALS['incdir'] = $include_root;
// Location of the login screen file
$GLOBALS['login_screen'] = $GLOBALS['rootdir'] . "/login_screen.php";

// Variable set for Eligibility Verification [EDI-271] path
$GLOBALS['edi_271_file_path'] = $GLOBALS['OE_SITE_DIR'] . "/edi/";

// Include the translation engine. This will also call sql.inc to
//  open the openemr mysql connection.
require_once (dirname(__FILE__) . "/../library/translation.inc.php");

// Include convenience functions with shorter names than "htmlspecialchars" (for security)
require_once (dirname(__FILE__) . "/../library/htmlspecialchars.inc.php");

// Include sanitization/checking functions (for security)
require_once (dirname(__FILE__) . "/../library/formdata.inc.php");

// Include sanitization/checking function (for security)
require_once (dirname(__FILE__) . "/../library/sanitize.inc.php");

// Includes functions for date internationalization
require_once (dirname(__FILE__) . "/../library/date_functions.php");

// Includes compoaser autoload
require_once $GLOBALS['vendor_dir'] ."/autoload.php";

// Includes functions for page validation
require_once (dirname(__FILE__) . "/../library/validation/validate_core.php");

// Include the version file
require_once (dirname(__FILE__) . "/../version.php");

// Defaults for specific applications.
$GLOBALS['weight_loss_clinic'] = false;
$GLOBALS['ippf_specific'] = false;
$GLOBALS['cene_specific'] = false;

// Defaults for drugs and products.
$GLOBALS['inhouse_pharmacy'] = false;
$GLOBALS['sell_non_drug_products'] = 0;

$glrow = sqlQuery("SHOW TABLES LIKE 'globals'");
if (!empty($glrow)) {
  // Collect user specific settings from user_settings table.
  //
  $gl_user = array();
  // Collect the user id first
  $temp_authuserid = '';
  if (!empty($_SESSION['authUserID'])) {
    //Set the user id from the session variable
    $temp_authuserid = $_SESSION['authUserID'];
  }
  else {
    if (!empty($_POST['authUser'])) {
      $temp_sql_ret = sqlQuery("SELECT `id` FROM `users` WHERE `username` = ?", array($_POST['authUser']) );
      if (!empty($temp_sql_ret['id'])) {
        //Set the user id from the login variable
        $temp_authuserid = $temp_sql_ret['id'];
      }
    }
  }
  if (!empty($temp_authuserid)) {
    $glres_user = sqlStatement("SELECT `setting_label`, `setting_value` " .
      "FROM `user_settings` " .
      "WHERE `setting_user` = ? " .
      "AND `setting_label` LIKE 'global:%'", array($temp_authuserid) );
    for($iter=0; $row=sqlFetchArray($glres_user); $iter++) {
      //remove global_ prefix from label
      $row['setting_label'] = substr($row['setting_label'],7);
      $gl_user[$iter]=$row;
    }
  }
  // Set global parameters from the database globals table.
  // Some parameters require custom handling.
  //
  $GLOBALS['language_menu_show'] = array();
  $glres = sqlStatement("SELECT gl_name, gl_index, gl_value FROM globals " .
    "ORDER BY gl_name, gl_index");
  while ($glrow = sqlFetchArray($glres)) {
    $gl_name  = $glrow['gl_name'];
    $gl_value = $glrow['gl_value'];
    // Adjust for user specific settings
    if (!empty($gl_user)) {
      foreach ($gl_user as $setting) {
        if ($gl_name == $setting['setting_label']) {
          $gl_value = $setting['setting_value'];
        }
      }
    }
    if ($gl_name == 'language_menu_other') {
      $GLOBALS['language_menu_show'][] = $gl_value;
    }
    else if ($gl_name == 'css_header') {
        $GLOBALS[$gl_name] = $rootdir.'/themes/'.$gl_value.'?v='.$v_js_includes;
        $temp_css_theme_name = $gl_value;
    }
    else if ($gl_name == 'weekend_days') {
        $GLOBALS[$gl_name] = explode(',', $gl_value);
    }
    else if ($gl_name == 'specific_application') {
      if ($gl_value == '2') $GLOBALS['ippf_specific'] = true;
      else if ($gl_value == '3') $GLOBALS['weight_loss_clinic'] = true;
    }
    else if ($gl_name == 'inhouse_pharmacy') {
      if ($gl_value) $GLOBALS['inhouse_pharmacy'] = true;
      if ($gl_value == '2') $GLOBALS['sell_non_drug_products'] = 1;
      else if ($gl_value == '3') $GLOBALS['sell_non_drug_products'] = 2;
    }
    else {
      $GLOBALS[$gl_name] = $gl_value;
    }
  }
  // Language cleanup stuff.
  $GLOBALS['language_menu_login'] = false;
  if ((count($GLOBALS['language_menu_show']) >= 1) || $GLOBALS['language_menu_showall']) {
    $GLOBALS['language_menu_login'] = true;
  }

  // Added this $GLOBALS['concurrent_layout'] set to 3 in order to support legacy forms
  // that may use this; note this global has been removed from the standard codebase.
  $GLOBALS['concurrent_layout'] = 3;

// Additional logic to override theme name.
// For RTL languages we substitute the theme name with the name of RTL-adapted CSS file.
    $rtl_override = false;
    if( isset( $_SESSION['language_direction'] )) {
        if( $_SESSION['language_direction'] == 'rtl' &&
        !strpos($GLOBALS['css_header'], 'rtl')  ) {

            // the $css_header_value is set above
            $rtl_override = true;
        }
    }
    else if (isset( $_SESSION['language_choice'] )) {
        //this will support the onsite patient portal which will have a language choice but not yet a set language direction
        $_SESSION['language_direction'] = getLanguageDir($_SESSION['language_choice']);
        if ( $_SESSION['language_direction'] == 'rtl' &&
        !strpos($GLOBALS['css_header'], 'rtl')) {
            // the $css_header_value is set above
            $rtl_override = true;
        }
    }
    else {
        //$_SESSION['language_direction'] is not set, so will use the default language
        $default_lang_id = sqlQuery('SELECT lang_id FROM lang_languages WHERE lang_description = ?',array($GLOBALS['language_default']));

        if ( getLanguageDir( $default_lang_id['lang_id'] ) === 'rtl' && !strpos($GLOBALS['css_header'], 'rtl')) { // @todo eliminate 1 SQL query
            $rtl_override = true;
        }
    }


    // change theme name, if the override file exists.
    if( $rtl_override ) {
        // the $css_header_value is set above
        $new_theme = 'rtl_' . $temp_css_theme_name;

        // Check file existance
        if( file_exists( $include_root.'/themes/'.$new_theme ) ) {
            $GLOBALS['css_header'] = $rootdir.'/themes/'.$new_theme.'?v='.$v_js_includes;
        } else {
            // throw a warning if rtl'ed file does not exist.
            error_log("Missing theme file ".text($include_root).'/themes/'.text($new_theme)   );
        }
    }
    unset( $temp_css_theme_name, $new_theme,$rtl_override);
    // end of RTL section

  //
  // End of globals table processing.
}
else {
  // Temporary stuff to handle the case where the globals table does not
  // exist yet.  This will happen in sql_upgrade.php on upgrading to the
  // first release containing this table.
  $GLOBALS['language_menu_login'] = true;
  $GLOBALS['language_menu_showall'] = true;
  $GLOBALS['language_menu_show'] = array('English (Standard)','Swedish');
  $GLOBALS['language_default'] = "English (Standard)";
  $GLOBALS['translate_layout'] = true;
  $GLOBALS['translate_lists'] = true;
  $GLOBALS['translate_gacl_groups'] = true;
  $GLOBALS['translate_form_titles'] = true;
  $GLOBALS['translate_document_categories'] = true;
  $GLOBALS['translate_appt_categories'] = true;
  $timeout = 7200;
  $openemr_name = 'OpenEMR';
  $css_header = "$rootdir/themes/style_default.css";
  $GLOBALS['css_header'] = $css_header;
  $GLOBALS['schedule_start'] = 8;
  $GLOBALS['schedule_end'] = 17;
  $GLOBALS['calendar_interval'] = 15;
  $GLOBALS['phone_country_code'] = '1';
  $GLOBALS['disable_non_default_groups'] = true;
  $GLOBALS['ippf_specific'] = false;
}

// If >0 this will enforce a separate PHP session for each top-level
// browser window.  You must log in separately for each.  This is not
// thoroughly tested yet and some browsers might have trouble with it,
// so make it 0 if you must.  Alternatively, you can set it to 2 to be
// notified when the session ID changes.
$GLOBALS['restore_sessions'] = 1; // 0=no, 1=yes, 2=yes+debug

// Theme definition.  All this stuff should be moved to CSS.
//
$top_bg_line = ' bgcolor="#dddddd" ';
$GLOBALS['style']['BGCOLOR2'] = "#dddddd";
$bottom_bg_line = $top_bg_line;
$title_bg_line = ' bgcolor="#bbbbbb" ';
$nav_bg_line = ' bgcolor="#94d6e7" ';
$login_filler_line = ' bgcolor="#f7f0d5" ';
$logocode = "<img class='img-responsive center-block' src='$web_root/sites/" . $_SESSION['site_id'] . "/images/login_logo.gif'>";
// optimal size for the tiny logo is height 43 width 86 px
// inside the open emr they will be auto reduced
$tinylogocode1 = "<img class='tinylogopng' src='$web_root/sites/" . $_SESSION['site_id'] . "/images/logo_1.png'>";
$tinylogocode2 = "<img class='tinylogopng' src='$web_root/sites/" . $_SESSION['site_id'] . "/images/logo_2.png'>";

$linepic = "$rootdir/pic/repeat_vline9.gif";
$table_bg = ' bgcolor="#cccccc" ';
$GLOBALS['style']['BGCOLOR1'] = "#cccccc";
$GLOBALS['style']['TEXTCOLOR11'] = "#222222";
$GLOBALS['style']['HIGHLIGHTCOLOR'] = "#dddddd";
$GLOBALS['style']['BOTTOM_BG_LINE'] = $bottom_bg_line;
// The height in pixels of the Logo bar at the top of the login page:
$GLOBALS['logoBarHeight'] = 110;
// The height in pixels of the Navigation bar:
$GLOBALS['navBarHeight'] = 22;
// The height in pixels of the Title bar:
$GLOBALS['titleBarHeight'] = 40;

// The assistant word, MORE printed next to titles that can be clicked:
//   Note this label gets translated here via the xl function
//    -if you don't want it translated, then strip the xl function away
$tmore = xl('(More)');
// The assistant word, BACK printed next to titles that return to previous screens:
//   Note this label gets translated here via the xl function
//    -if you don't want it translated, then strip the xl function away
$tback = xl('(Back)');

// This is the idle logout function:
// if a page has not been refreshed within this many seconds, the interface
// will return to the login page
if (!empty($special_timeout)) {
  $timeout = intval($special_timeout);
}

//Version tag
$patch_appending = "";
if ( ($v_realpatch != '0') && (!(empty($v_realpatch))) ) {
$patch_appending = " (".$v_realpatch.")";
}
$openemr_version = "$v_major.$v_minor.$v_patch".$v_tag.$patch_appending;

$srcdir = $GLOBALS['srcdir'];
$login_screen = $GLOBALS['login_screen'];
$GLOBALS['css_header'] = $css_header;
$GLOBALS['backpic'] = $backpic;

// 1 = send email message to given id for Emergency Login user activation,
// else 0.
$GLOBALS['Emergency_Login_email'] = empty($GLOBALS['Emergency_Login_email_id']) ? 0 : 1;

//set include_de_identification to enable De-identification (currently de-identification works fine only with linux machines)
//Run de_identification_upgrade.php script to upgrade OpenEMR database to include procedures,
//functions, tables for de-identification(Mysql root user and password is required for successful
//execution of the de-identification upgrade script)
$GLOBALS['include_de_identification']=0;
// Include the authentication module code here, but the rule is
// if the file has the word "login" in the source code file name,
// don't include the authentication module - we do this to avoid
// include loops.

if ( ($ignoreAuth_offsite_portal === true) && ($GLOBALS['portal_offsite_enable'] == 1) ) {
  $ignoreAuth = true;
}
if (!$ignoreAuth) {
  include_once("$srcdir/auth.inc");
}


// This is the background color to apply to form fields that are searchable.
// Currently it is applicable only to the "Search or Add Patient" form.
$GLOBALS['layout_search_color'] = '#ffff55';

//EMAIL SETTINGS
$SMTP_Auth = !empty($GLOBALS['SMTP_USER']);


//module configurations
$GLOBALS['baseModDir'] 	= "interface/modules/"; //default path of modules
$GLOBALS['customModDir']= "custom_modules";	//non zend modules
$GLOBALS['zendModDir']	= "zend_modules";	//zend modules

// Don't change anything below this line. ////////////////////////////

$encounter = empty($_SESSION['encounter']) ? 0 : $_SESSION['encounter'];

if (!empty($_GET['pid']) && empty($_SESSION['pid'])) {
  $_SESSION['pid'] = $_GET['pid'];
}
elseif (!empty($_POST['pid']) && empty($_SESSION['pid'])) {
  $_SESSION['pid'] = $_POST['pid'];
}
$pid = empty($_SESSION['pid']) ? 0 : $_SESSION['pid'];
$userauthorized = empty($_SESSION['userauthorized']) ? 0 : $_SESSION['userauthorized'];
$groupname = empty($_SESSION['authProvider']) ? 0 : $_SESSION['authProvider'];

// global interface function to format text length using ellipses
function strterm($string,$length) {
  if (strlen($string) >= ($length-3)) {
    return substr($string,0,$length-3) . "...";
  } else {
    return $string;
  }
}

// Override temporary_files_dir if PHP >= 5.2.1.
if (version_compare(phpversion(), "5.2.1", ">=")) {
 $GLOBALS['temporary_files_dir'] = rtrim(sys_get_temp_dir(),'/');
}

// turn off PHP compatibility warnings
ini_set("session.bug_compat_warn","off");

//////////////////////////////////////////////////////////////////

/* Pages with "myadmin" in the URL don't need register_globals. */
$fake_register_globals =
	$fake_register_globals && (strpos($_SERVER['REQUEST_URI'],"myadmin") === FALSE);


// Emulates register_globals = On.  Moved to the bottom of globals.php to prevent
// overrides of any variables used during global setup.
// EXTR_SKIP flag set to prevent overriding any variables defined earlier
if ($fake_register_globals) {
  extract($_GET,EXTR_SKIP);
  extract($_POST,EXTR_SKIP);
}
?>
