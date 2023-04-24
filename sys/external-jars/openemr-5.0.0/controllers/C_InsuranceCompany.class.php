<?php

require_once($GLOBALS['fileroot'] . "/library/classes/Controller.class.php");
require_once($GLOBALS['fileroot'] . "/library/classes/InsuranceCompany.class.php");
require_once($GLOBALS['fileroot'] . "/library/classes/X12Partner.class.php");

class C_InsuranceCompany extends Controller {

	var $template_mod;
	var $icompanies;

	function __construct($template_mod = "general") {
		parent::__construct();
		$this->icompanies = array();
		$this->template_mod = $template_mod;
		$this->assign("FORM_ACTION", $GLOBALS['webroot']."/controller.php?" . $_SERVER['QUERY_STRING']);
		$this->assign("CURRENT_ACTION", $GLOBALS['webroot']."/controller.php?" . "practice_settings&insurance_company&");
		$this->assign("STYLE", $GLOBALS['style']);
		$this->assign("WEB_ROOT", $GLOBALS['webroot'] );
		$this->InsuranceCompany = new InsuranceCompany();
	}

	function default_action() {
		return $this->list_action();
	}

	function edit_action($id = "",$patient_id="",$p_obj = null) {
		if ($p_obj != null && get_class($p_obj) == "insurancecompany") {
			$this->icompanies[0] = $p_obj;
		}
		elseif (get_class($this->icompanies[0]) != "insurancecompany" ) {
			$this->icompanies[0] = new InsuranceCompany($id);
		}

		$x = new X12Partner();
		$this->assign("x12_partners", $x->_utility_array($x->x12_partner_factory()));

		$this->assign("insurancecompany", $this->icompanies[0]);
		return $this->fetch($GLOBALS['template_dir'] . "insurance_companies/" . $this->template_mod . "_edit.html");
	}

	function list_action($sort = "") {

		if (!empty($sort)) {
			$this->assign("icompanies", $this->InsuranceCompany->insurance_companies_factory("",$sort));
		}
		else {
			$this->assign("icompanies", $this->InsuranceCompany->insurance_companies_factory());
		}

		return $this->fetch($GLOBALS['template_dir'] . "insurance_companies/" . $this->template_mod . "_list.html");
	}


	function edit_action_process() {
		if ($_POST['process'] != "true")
			return;
		//print_r($_POST);
		if (is_numeric($_POST['id'])) {
			$this->icompanies[0] = new InsuranceCompany($_POST['id']);
		}
		else {
			$this->icompanies[0] = new InsuranceCompany();
		}

  		parent::populate_object($this->icompanies[0]);

		$this->icompanies[0]->persist();
		$this->icompanies[0]->populate();

		//echo "action processeed";
		$_POST['process'] = "";
		header('Location:'.$GLOBALS['webroot']."/controller.php?" . "practice_settings&insurance_company&action=list");//Z&H
	}

}

?>
