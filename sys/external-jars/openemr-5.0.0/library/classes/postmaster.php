<?php
// Copyright (C) 2010 Open Support LLC
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// Add these two lines to Authenticate in phpmailer.php, lines 633-634
// Customized for Web hosts that don't require SMTP authentication
// if ($SMTP_Auth=="No") { $connection = true; }
// Also, remove "25" in line 185 and change $Port to $this->Port on line 612 so that it can read Admin's setting

class MyMailer extends PHPMailer
{
    var $Mailer;
    var $SMTPAuth;
    var $Host;
    var $Username;
    var $Password;
    var $Port;
    var $CharSet;

    function __construct()
    {
        $this->emailMethod();
    }
    
    function emailMethod()
    {
        global $HTML_CHARSET;
        $this->CharSet = $HTML_CHARSET;
        switch($GLOBALS['EMAIL_METHOD'])
        {
            case "PHPMAIL" :
            {
                $this->Mailer = "mail";
            }
            break;
            case "SMTP" :
            {
		global $SMTP_Auth;
                $this->Mailer = "smtp";
                $this->SMTPAuth = $SMTP_Auth;
                $this->Host = $GLOBALS['SMTP_HOST'];
                $this->Username = $GLOBALS['SMTP_USER'];
                $this->Password = $GLOBALS['SMTP_PASS'];
                $this->Port = $GLOBALS['SMTP_PORT'];
                $this->SMTPSecure = $GLOBALS['SMTP_SECURE'];
            }
            break;
            case "SENDMAIL" :
            {
                $this->Mailer = "sendmail";
            }
            break;
        }
    }
}

?>
