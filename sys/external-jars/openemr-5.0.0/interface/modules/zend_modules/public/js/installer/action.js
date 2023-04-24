/* +-----------------------------------------------------------------------------+
*    OpenEMR - Open Source Electronic Medical Record
*    Copyright (C) 2013 Z&H Consultancy Services Private Limited <sam@zhservices.com>
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU Affero General Public License as
*    published by the Free Software Foundation, either version 3 of the
*    License, or (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU Affero General Public License for more details.
*
*    You should have received a copy of the GNU Affero General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*    @author  Jacob T.Paul <jacob@zhservices.com>
*    @author  Vipin Kumar <vipink@zhservices.com>
*    @author  Remesh Babu S <remesh@zhservices.com>
* +------------------------------------------------------------------------------+
*/

function register(status,title,name,method,type){
	$.post("./Installer/register", { st: status, mod_title: title, mod_name: name, mod_method:method,mtype:type},
	   function(data) {
			if(data == "Success") {
				window.location.reload();
      } else { 
        var resultTranslated = js_xl(data);
				$('#err').html(resultTranslated.msg).fadeIn().delay(2000).fadeOut();
      }
	  }
	);
}

function manage(id,action){
	if(document.getElementById('mod_enc_menu'))
		modencmenu = document.getElementById('mod_enc_menu').value;
	else
		modencmenu = '';
	if(document.getElementById('mod_nick_name_'+id))	
		modnickname = document.getElementById('mod_nick_name_'+id).value;
	else
		modnickname = '';
	$.post("./Installer/manage", { modId: id, modAction: action,mod_enc_menu:modencmenu,mod_nick_name:modnickname},
		function(data) {
			if(data=="Success"){
				if (parent.left_nav.location) {
					parent.left_nav.location.reload();
					parent.Title.location.reload();
					if(self.name=='RTop'){
						parent.RBot.location.reload();
					}
					else{
						parent.RTop.location.reload();
					}
					top.document.getElementById('fsright').rows = '*,*';
				}				
				window.location.reload();
			}
			else{
				alert(data);
			}
		}
	);
}

function configure(id,imgpath){
	if($("#ConfigRow_"+id).css("display")!="none"){
		$(".config").hide();		
		$("#ConfigRow_"+id).fadeOut();
	}
	else{
		$.post("./Installer/configure", {mod_id:id},
			function(data) {
				$(".config").hide();
				$("#ConfigRow_"+id).hide();
				$("#ConfigRow_"+id).html('<td colspan="10" align="center">'+data+'</td>').fadeIn();	
			}
		);
	}
}

function custom_toggle(obj){
	if($("#"+obj).css("display")!="none"){
		$("#"+obj).fadeOut();
	}
	else{
		$("#"+obj).fadeIn();
	}
}

function SaveMe(frmId,mod_id){
	var SelAccIndTab = $('#configaccord'+mod_id).accordion('getSelected');
	if(SelAccIndTab)
		var Acctitle 	= SelAccIndTab.panel('options').title;
	
	var SelTab 	= $('#tab'+mod_id).tabs('getSelected');
	if(SelTab)
		var Tabtitle = SelTab.panel('options').title;
		
	if(frmId == 'hooksform'){
		$.ajax({
			type: 'POST',
			url: "./Installer/SaveHooks",
			data: $('#'+frmId+mod_id).serialize(),   
			success: function(data){
				$.each(data, function(jsonIndex, jsonValue){
					if (jsonValue['return'] == 1) {
						$("#hook_response"+mod_id).html(jsonValue['msg']).fadeIn().fadeOut(1000);
						$(document).ready(function(){
						if(Tabtitle)
						$('#tab'+mod_id).tabs('select',Tabtitle);
						});
					}
				});
			}
		});	
	}
}



function DeleteACL(aclID,user,mod_id,msg){
	var SelAccIndTab = $('#configaccord'+mod_id).accordion('getSelected');
	if(SelAccIndTab)
		var Acctitle = SelAccIndTab.panel('options').title;
	if(confirm(msg)){
		$.ajax({
			type: 'POST',
			url: "./Installer/DeleteAcl",
			data:{
				aclID: aclID,
				user: user
			},
			success: function(data){
					$.each(data, function(jsonIndex, jsonValue){
						if (jsonValue['return'] == 1) {	
							$("#ConfigRow_"+mod_id).hide();
							configure(mod_id,'');
							alert(jsonValue['msg']);
							$(document).ready(function(){
								if(Acctitle)
									$('#configaccord'+mod_id).accordion('select',Acctitle);
							});
						}
					});
			}
		});
	}
}

function DeleteHooks(hooksID,mod_id,msg){
  var SelTab = $('#tab'+mod_id).tabs('getSelected');
  if(SelTab)
  var Tabtitle = SelTab.panel('options').title;
   if(confirm(msg)){
   $.ajax({
     type: 'POST',
     url: "./Installer/DeleteHooks",
     data:{
     hooksID: hooksID
     },
     success: function(data){
       $.each(data, function(jsonIndex, jsonValue){
         if (jsonValue['return'] == 1) {
           $("#ConfigRow_"+mod_id).hide();
           configure(mod_id,'');
           alert(jsonValue['msg']);
           $(document).ready(function(){
           if(Tabtitle)
           $('#tab'+mod_id).tabs('select',Tabtitle);
           });
         }
       });
     }
   });	
   }
}

/**
* Save Settings Tab Contants
* 
* @param {string} frmId
* @param {int} mod_id
* @returns {undefined}
*/
function saveConfig(frmId, mod_id) {
  $.ajax({
    type: 'POST',
    url: "./Installer/saveConfig",
    data: $('#' + frmId + mod_id).serialize(),   
    success: function(data){ 
      var resultTranslated = js_xl('Configuration saved successfully');
      $('#target' + data.modeId).html(resultTranslated.msg + ' ....').show().fadeOut(4000);
    }
  });
  
}

function validateNickName(modId) {
	Nickname = $("#mod_nick_name_"+modId).val();
	if($.trim(Nickname) != ""){
		$.ajax({
			type: 'POST',
			url: "./Installer/nickName",
			data:{
				nickname : Nickname,
			},
			success: function(data){
				if(data != 0){
					$("#mod_nick_name_"+modId).css("background","#FFB9B5");
					$("#mod_nick_name_message_"+modId).html("* Duplicate Nick Name");
				}else{
					$("#mod_nick_name_"+modId).css("background","");
					$("#mod_nick_name_message_"+modId).html("");
				}
			},
			error: function(){
				alert("Ajax Error");
			}
		});
	}else{
		$("#mod_nick_name_"+modId).css("background","");
		$("#mod_nick_name_message_"+modId).html("");
	}
}
  

