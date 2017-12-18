angular
		.module('activitiApp')
		.directive(
				'dynamicStencil',
				function($rootScope, $compile) {

					return {
						template : '<div id="{{stencilName}}DynamicStencil" ng-include="getContentUrl()"></div>',
						link : function(scope, element, attrs) {

							scope.stencilName = attrs.stencil;

							scope.getContentUrl = function() {
								return scope.stencilName;
							}

						}

					};
				});
