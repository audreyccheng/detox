<?php
/**
 * Global Configuration Override
 *
 * You can use this file for overriding configuration values from modules, etc.
 * You would place values in here that are agnostic to the environment and not
 * sensitive to security.
 *
 * @NOTE: In practice, this file will typically be INCLUDED in your source
 * control, so do not include passwords or other sensitive information in this
 * file.
 */

if ($GLOBALS['disable_utf8_flag']) {
  $db = array(
        'driver'         => 'Pdo',
        'dsn'            => 'mysql:dbname='.$GLOBALS['dbase'].';host='.$GLOBALS['host'],
        'username'       => $GLOBALS['login'],
        'password'       => $GLOBALS['pass'],
        'port'           => $GLOBALS['port'],
        'driver_options' => array(
            PDO::MYSQL_ATTR_INIT_COMMAND => 'SET sql_mode = \'\''
        )
    );
} else {
  $db = array(
        'driver'         => 'Pdo',
        'dsn'            => 'mysql:dbname='.$GLOBALS['dbase'].';host='.$GLOBALS['host'],
        'username'       => $GLOBALS['login'],
        'password'       => $GLOBALS['pass'],
        'port'           => $GLOBALS['port'],
        'driver_options' => array(
            PDO::MYSQL_ATTR_INIT_COMMAND => 'SET NAMES \'UTF8\', sql_mode = \'\''
        )
    );
}
return array(
    'db' => $db,
    'service_manager' => array(
        'factories' => array(
            'Zend\Db\Adapter\Adapter' => function ($serviceManager) {
				$adapterFactory = new Zend\Db\Adapter\AdapterServiceFactory();
				$adapter = $adapterFactory->createService($serviceManager);
               \Zend\Db\TableGateway\Feature\GlobalAdapterFeature::setStaticAdapter($adapter);
               return $adapter;
         	}
        ),
    ),
);
