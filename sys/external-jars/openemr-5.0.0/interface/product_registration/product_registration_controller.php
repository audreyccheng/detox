<?php
/**
 * ProductRegistrationController
 *
 * LICENSE: This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://opensource.org/licenses/gpl-license.php>;.
 *
 * @package OpenEMR
 * @author  Matthew Vita <matthewvita48@gmail.com>
 * @link    http://www.open-emr.org
 */

$fake_register_globals=false;
$sanitize_all_escapes=true;
$ignoreAuth=true;
require_once("../globals.php");
require_once($GLOBALS['fileroot'] . "/interface/main/exceptions/invalid_email_exception.php");
require_once($GLOBALS['fileroot'] . "/interface/main/utils/http_response_helper.php");
require_once($GLOBALS['fileroot'] . "/interface/product_registration/product_registration_service.php");
require_once($GLOBALS['fileroot'] . "/interface/product_registration/exceptions/generic_product_registration_exception.php");
require_once($GLOBALS['fileroot'] . "/interface/product_registration/exceptions/duplicate_registration_exception.php");

class ProductRegistrationController {
    private $productRegistrationService;

    public function __construct() {
        $this->productRegistrationService = new ProductRegistrationService();

        // (note this is here until we use Zend Framework)
        switch ($_SERVER['REQUEST_METHOD']) {
            case 'POST':
                $this->post();
                break;
            case 'GET':
                $this->get();
                break;
        }
    }

    public function get() {
        $statusPayload = $this->productRegistrationService->getProductStatus();

        HttpResponseHelper::send(200, $statusPayload, 'JSON');
    }

    public function post() {
        $registrationPayload = new stdClass();
        $status = 500;

        try {
            $registrationId = $this->productRegistrationService->registerProduct($_POST['email']);
            $registrationPayload->registration_id = $registrationId;
            $registrationPayload->email = $_POST['email'];
            $status = 201;
        } catch (InvalidEmailException $emailException) {
            $registrationPayload->error = $emailException->errorMessage();
        } catch (DuplicateRegistrationException $duplicateRegistrationException) {
            $registrationPayload->error = $duplicateRegistrationException->errorMessage();
        } catch (GenericProductRegistrationException $genericProductRegistrationException) {
            $registrationPayload->error = $genericProductRegistrationException->errorMessage();
        }

        HttpResponseHelper::send($status, $registrationPayload, 'JSON');
    }
}

// Initialize self (note this is here until we use Zend Framework)
$productRegistrationController = new ProductRegistrationController();
