#Smarty 2 template engine
##Distribution repository
Starting with Smarty 2.6.23 Composer has been configured to load the packages from github.
 
**NOTE: Because of this change you must clear your local composer cache with the "composer clearcache" command**

To get the latest stable version use

	"require": {
	   "smarty/smarty": "~2.6"
	}

in your composer.json file.
 
 To get the trunk version use

	"require": {
	   "smarty/smarty": "~2.6@dev"
	}

The "smarty/smarty" package will start at libs/....   subfolder.


