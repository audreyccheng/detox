<?php

return array(
    'modules' =>  array(
        '0' => 'Application',
        '1' => 'Installer',
        '2' => 'Acl',
        '3' => 'Carecoordination',
        '4' => 'Ccr',
        '5' => 'Documents',
        '6' => 'Immunization',
        '7' => 'Syndromicsurveillance',
        '8'=> 'Patientvalidation',
        ),
    'module_listener_options' =>  array(
        'config_glob_paths' =>  array(
            '0' => 'config/autoload/{,*.}{global,local}.php',
        ),
        'module_paths' =>  array(
            '0' => './module',
            '1' => './vendor',
        ),
    ),
);
