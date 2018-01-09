/*
 * Copyright 2005-2017 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
'use strict';

var ACTIVITI = ACTIVITI || {};

ACTIVITI.CONFIG = {
	'onPremise' : true,
	'contextRoot' : '/activiti-app',
	'webContextRoot' : '/activiti-app',
	'locales' : [ 'en', 'de', 'es', 'fr', 'it', 'nl', 'ja', 'nb', 'ru',
			'zh-CN', 'pt-BR' ],
	'signupUrl' : '#/signup'
};

ACTIVITI.CONFIG.resources = {
	'workflow' : [
			{
				'tag' : 'script',
				'type' : 'text/javascript',
				'src' : ACTIVITI.CONFIG.webContextRoot
						+ '/scripts/dynamic-stencils/templates.js?v=1.0'
			},
			{
				'tag' : 'script',
				'type' : 'text/javascript',
				'src' : ACTIVITI.CONFIG.webContextRoot
						+ '/scripts/dynamic-stencils/dynamicStencil.js?v=1.0'
			} ]
};

var customStencils = [];
angular.forEach(customStencils, function(stencil) {
	ACTIVITI.CONFIG.resources['workflow'].push({
		'tag' : 'script',
		'type' : 'text/javascript',
		'src' : ACTIVITI.CONFIG.webContextRoot + '/workflow/dynamic-stencils/'
				+ stencil + '-stencil/' + stencil + '-ctrl.js?v=1.0'
	});
});
