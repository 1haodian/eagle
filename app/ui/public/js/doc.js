/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var __sortTable_generateFilteredList;

(function () {
	'use strict';

	var isArray;
	if(typeof $ !== "undefined") {
		isArray = $.isArray;
	} else {
		isArray = Array.isArray;
	}

	function hasContentByPathList(object, content, pathList) {
		for(var i = 0 ; i < pathList.length ; i += 1) {
			var path = pathList[i];
			var value = common.getValueByPath(object, path);
			if((value + "").toUpperCase().indexOf(content) >= 0) {
				return true;
			}
		}
		return false;
	}

	function hasContent(object, content, depth) {
		var i, keys;

		depth = depth || 1;
		if(!content) return true;
		if(depth > 10) return false;

		if(object === undefined || object === null) {
			return false;
		} else if(isArray(object)) {
			for(i = 0 ; i < object.length ; i += 1) {
				if(hasContent(object[i], content, depth + 1)) return true;
			}
		} else if(typeof object === "object") {
			keys = Object.keys(object);
			for(i = 0 ; i < keys.length ; i += 1) {
				var value = object[keys[i]];
				if(hasContent(value, content, depth + 1)) return true;
			}
		} else {
			return (object + "").toUpperCase().indexOf(content) >= 0;
		}

		return false;
	}

	__sortTable_generateFilteredList = function(list, search, order, orderAsc, searchPathList) {
		var i, _list;
		var _search = (search + "").toUpperCase();

		if (search) {
			_list = [];
			if(searchPathList) {
				for(i = 0 ; i < list.length ; i += 1) {
					if(hasContentByPathList(list[i], _search, searchPathList)) _list.push(list[i]);
				}
			} else {
				for(i = 0 ; i < list.length ; i += 1) {
					if(hasContent(list[i], _search)) _list.push(list[i]);
				}
			}
		} else {
			_list = list;
		}

		if (order) {
			common.array.doSort(_list, order, orderAsc);
		}

		return _list;
	};
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function () {
	'use strict';

	var scope = {};
	if(typeof window != 'undefined') {
		scope = window;
	} else if(typeof self != 'undefined') {
		scope = self;
	}
	var common = scope.common = {};

	// ============================ Common ============================
	common.template = function (str, list) {
		$.each(list, function(key, value) {
			var _regex = new RegExp("\\$\\{" + key + "\\}", "g");
			str = str.replace(_regex, value);
		});
		return str;
	};

	common.getValueByPath = function (unit, path, defaultValue) {
		if(unit === null || unit === undefined) throw "Unit can't be empty!";
		if(path === "" || path === null || path === undefined) return unit;

		if(typeof path === "string") {
			path = path.replace(/\[(\d+)\]/g, ".$1").replace(/^\./, "").split(/\./);
		}
		for(var i = 0 ; i < path.length ; i += 1) {
			unit = unit[path[i]];
			if(unit === null || unit === undefined) {
				unit = null;
				break;
			}
		}
		if(unit === null && defaultValue !== undefined) {
			unit = defaultValue;
		}
		return unit;
	};

	common.setValueByPath = function(unit, path, value) {
		if(!unit || typeof path !== "string" || path === "") throw "Unit or path can't be empty!";

		var _inArray = false;
		var _end = 0;
		var _start = 0;
		var _unit = unit;

		function _nextPath(array) {
			var _key = path.slice(_start, _end);
			if(_inArray) {
				_key = _key.slice(0, -1);
			}
			if(!_unit[_key]) {
				if(array) {
					_unit[_key] = [];
				} else {
					_unit[_key] = {};
				}
			}
			_unit = _unit[_key];
		}

		for(; _end < path.length ; _end += 1) {
			if(path[_end] === ".") {
				_nextPath(false);
				_start = _end + 1;
				_inArray = false;
			} else if(path[_end] === "[") {
				_nextPath(true);
				_start = _end + 1;
				_inArray = true;
			}
		}

		_unit[path.slice(_start, _inArray ? -1 : _end)] = value;

		return unit;
	};

	common.parseJSON = function (str, defaultVal) {
		if(str && Number(str).toString() !== str) {
			try {
				str = (str + "").trim();
				return JSON.parse(str);
			} catch(err) {}
		}

		if(arguments.length === 1) {
			console.warn("Can't parse JSON: " + str);
		}

		return defaultVal === undefined ? null : defaultVal;
	};

	common.stringify = function(json) {
		return JSON.stringify(json, function(key, value) {
			if(/^(_|\$)/.test(key)) return undefined;
			return value;
		});
	};

	common.isEmpty = function(val) {
		if($.isArray(val)) {
			return val.length === 0;
		} else {
			return val === null || val === undefined;
		}
	};

	common.extend = function(target, origin) {
		$.each(origin, function(key, value) {
			if(/^(_|\$)/.test(key)) return;

			target[key] = value;
		});
		return target;
	};

	function merge(obj1, obj2) {
		$.each(obj2, function (key, value) {
			var oriValue = obj1[key];

			if(typeof oriValue === "object" && typeof value === "object" && !common.isEmpty(value)) {
				merge(oriValue, value);
			} else {
				obj1[key] = value;
			}
		});
	}

	common.merge = function (mergedObj) {
		for(var i = 1 ; i < arguments.length ; i += 1) {
			var obj = arguments[i];
			merge(mergedObj, obj);
		}

		return mergedObj;
	};

	common.getKeys = function (obj) {
		return $.map(obj, function (val, key) {
			return key;
		});
	};

	// ============================ String ============================
	common.string = {};
	common.string.safeText = function (str) {
		return str
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;');
	};

	common.string.capitalize = function (str) {
		return (str + "").replace(/\b\w/g, function(match) {
			return match.toUpperCase();
		});
	};

	common.string.preFill = function (str, key, len) {
		str = str + "";
		len = len || 2;
		while(str.length < len) {
			str = key + str;
		}
		return str;
	};

	// ============================ Array =============================
	common.array = {};

	common.array.findIndex = function(val, list, path, findAll, caseSensitive) {
		var _list = [];
		val = caseSensitive === false ? (val + "").toUpperCase() : val;

		for(var i = 0 ; i < list.length ; i += 1) {
			var unit = list[i];
			var _val = common.getValueByPath(unit, path);
			_val = caseSensitive === false ? (_val + "").toUpperCase() : _val;

			if(_val === val) {
				if(!findAll) return i;
				_list.push(i);
			}
		}

		return findAll ? _list: -1;
	};

	common.array.find = function(val, list, path, findAll, caseSensitive) {
		var index = common.array.findIndex(val, list, path, findAll, caseSensitive);

		if(findAll) {
			return $.map(index, function (index) {
				return list[index];
			});
		} else {
			return index === -1 ? null : list[index];
		}
	};

	common.array.minus = function (list1, list2, path1, path2) {
		if(arguments.length === 3) path2 = path1;
		var list = [];
		$.each(list1, function (i, item) {
			var val1 = common.getValueByPath(item, path1);
			if(!common.array.find(val1, list2, path2)) {
				list.push(item);
			}
		});
		return list;
	};

	common.array.remove = function (val, list, path) {
		return $.grep(list, function (obj) {
			return common.getValueByPath(obj, path) !== val;
		});
	};

	common.array.doSort = function (list, path, asc, sortList) {
		var sortFunc;
		sortList = sortList || [];

		if(asc !== false) {
			sortFunc = function (obj1, obj2) {
				var val1 = common.getValueByPath(obj1, path);
				var val2 = common.getValueByPath(obj2, path);

				var index1 = common.array.findIndex(val1, sortList);
				var index2 = common.array.findIndex(val2, sortList);

				if(index1 !== -1 && index2 === -1) {
					return -1;
				} else if(index1 == -1 && index2 !== -1) {
					return 1;
				} else if(index1 !== -1 && index2 !== -1) {
					return index1 - index2;
				}

				if (val1 === val2) {
					return 0;
				} else if (val1 === null || val1 === undefined || val1 < val2) {
					return -1;
				}
				return 1;
			};
		} else {
			sortFunc = function (obj1, obj2) {
				var val1 = common.getValueByPath(obj1, path);
				var val2 = common.getValueByPath(obj2, path);

				var index1 = common.array.findIndex(val1, sortList);
				var index2 = common.array.findIndex(val2, sortList);

				if(index1 !== -1 && index2 === -1) {
					return -1;
				} else if(index1 == -1 && index2 !== -1) {
					return 1;
				} else if(index1 !== -1 && index2 !== -1) {
					return index1 - index2;
				}

				if (val1 === val2) {
					return 0;
				} else if (val1 === null || val1 === undefined || val1 < val2) {
					return 1;
				}
				return -1;
			};
		}

		return list.sort(sortFunc);
	};

	// =========================== Deferred ===========================
	common.deferred = {};

	common.deferred.all = function (deferredList) {
		var deferred = $.Deferred();
		var successList = [];
		var failureList = [];
		var hasFailure = false;
		var rest = deferredList.length;
		function doCheck() {
			rest -= 1;
			if(rest === 0) {
				if(hasFailure) {
					deferred.reject(failureList);
				} else {
					deferred.resolve(successList);
				}
			}
		}

		$.each(deferredList, function (i, deferred) {
			if(deferred && deferred.then) {
				var promise = deferred.then(function (data) {
					successList[i] = data;
				}, function (data) {
					failureList[i] = data;
					hasFailure = true;
				});
				if(promise.always) {
					promise.always(doCheck);
				} else if(promise.finally) {
					promise.finally(doCheck);
				}
			} else {
				successList[i] = deferred;
				doCheck();
			}
		});

		return deferred;
	};

	// ============================ Number ============================
	common.number = {};

	common.number.isNumber = function (num) {
		return typeof num === "number" && !isNaN(num);
	};

	common.number.parse = function (num) {
		num = Number(num);
		if(isNaN(num)) num = 0;
		return num;
	};

	common.number.toFixed = function (num, fixed) {
		if(!common.number.isNumber(num)) return "-";
		num = Number(num);
		return num.toFixed(fixed || 0);
	};

	common.number.format = function (num, fixed) {
		if(!common.number.isNumber(num)) return "-";
		return common.number.toFixed(num, fixed).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
	};

	common.number.abbr = function (number, isByte, digits) {
		digits = digits || 2;
		var decPlaces = Math.pow(10, digits);
		var abbrev = isByte ? ['KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'] : ['K', 'M', 'B', 'T', 'Q'];
		var base = isByte ? 1024 : 1000;
		var sign = number < 0 ? -1 : 1;
		var unit = '';
		number = Math.abs(number);
		if(isNaN(number)) return "-";

		for(var i = abbrev.length - 1; i >= 0; i--) {
			var size = Math.pow(base, i + 1);
			if(size <= number) {
				number = Math.round(number * decPlaces / size) / decPlaces;
				if((number === base) && (i < abbrev.length - 1)) {
					number = 1;
					i++;
				}
				unit = abbrev[i];
				break;
			}
		}
		unit = unit ? unit : "";
		return (number * sign).toFixed(digits) + unit;
	};

	common.number.compare = function (num1, num2) {
		if(!common.number.isNumber(num1) || !common.number.isNumber(num2)) return "-";
		if(num1 === 0) return 'N/A';
		return (num2 - num1) / num1;
	};

	common.number.inRange = function (rangList, num) {
		for(var i = 0 ; i < rangList.length - 1 ; i += 1) {
			var start = rangList[i];
			var end = rangList[i + 1];
			if(start <= num && num < end) return i;
		}
		return rangList.length - 1;
	};

	common.number.sum = function (list, path) {
		var total = 0;
		$.each(list, function (i, obj) {
			var value = common.getValueByPath(obj, path);
			if(typeof value === "number" && !isNaN(value)) {
				total += value;
			}
		});
		return total;
	};
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function () {
	'use strict';

	// ======================================================================================
	// =                                   Browser Hooker                                   =
	// ======================================================================================
	var is_chrome = navigator.userAgent.indexOf('Chrome') > -1;
	var is_explorer = navigator.userAgent.indexOf('MSIE') > -1;
	var is_firefox = navigator.userAgent.indexOf('Firefox') > -1;
	var is_safari = navigator.userAgent.indexOf("Safari") > -1;
	var is_opera = navigator.userAgent.toLowerCase().indexOf("op") > -1;
	if(is_chrome && is_safari) is_safari = false;
	if(is_chrome && is_opera) is_chrome = false;

	if(is_safari) {
		$("body").addClass("safari");
	}

	// ======================================================================================
	// =                                        Host                                        =
	// ======================================================================================
	var _host = "";
	var _app = {};
	if(localStorage) {
		_host = localStorage.getItem("host") || "";
		_app = common.parseJSON(localStorage.getItem("app"), {});
	}

	window._host = function (host) {
		if(host) {
			_host = host.replace(/[\\\/]+$/, "");
			if(localStorage) {
				localStorage.setItem("host", _host);
			}
		}
		return _host;
	};

	window._app = function (appName, viewPath) {
		if(arguments.length) {
			_app[appName] = {
				viewPath: viewPath
			};
			if(localStorage) {
				localStorage.setItem("app", JSON.stringify(_app));
			}
		}
		return _app;
	};

	window._app.clear = function () {
		if(localStorage) {
			_app = {};
			localStorage.removeItem("app");
		}
	};

	// ======================================================================================
	// =                                      Register                                      =
	// ======================================================================================
	var _moduleStateId = 0;
	var _registerAppList = [];
	var _lastRegisterApp = null;
	var _hookRequireFunc = null;

	function Module(dependencies) {
		this.dependencies = dependencies;
		this.queueList = [];
		this.routeList = [];
		this.portalList = [];
		this.widgetList = [];

		this.requireRest = 0;
		this.requireDeferred = null;

		return this;
	}

	// GRUNT REPLACEMENT: Module.buildTimestamp = TIMESTAMP
	window._TRS = function() {
		return Module.buildTimestamp || Math.random();
	};

	Module.prototype.service = function () {
		this.queueList.push({type: "service", args: arguments});
		return this;
	};
	Module.prototype.directive = function () {
		this.queueList.push({type: "directive", args: arguments});
		return this;
	};
	Module.prototype.controller = function () {
		this.queueList.push({type: "controller", args: arguments});
		return this;
	};

	/**
	 * Add portal into side navigation bar.
	 * @param {{}} portal				Config portal content
	 * @param {string} portal.name		Display name
	 * @param {string} portal.icon		Display icon. Use 'FontAwesome'
	 * @param {string=} portal.path		Route path
	 * @param {[]=} portal.list			Sub portal
	 * @param {boolean} isSite			true will show in site page or will shown in main page
	 */
	Module.prototype.portal = function (portal, isSite) {
		this.portalList.push({portal: portal, isSite: isSite});
		return this;
	};

	/**
	 * Set application route
	 * @param {{}|string=} state				Config state. More info please check angular ui router
	 * @param {{}} config						Route config
	 * @param {string} config.url				Root url. start with '/'
	 * @param {string} config.templateUrl		Template url. Relative path of application `viewPath`
	 * @param {string} config.controller		Set page controller
	 */
	Module.prototype.route = function (state, config) {
		if(arguments.length === 1) {
			config = state;
			state = "_APPLICATION_STATE_" + _moduleStateId++;
		}

		if(!config.url) throw "Url not defined!";

		this.routeList.push({
			state: state,
			config: config
		});
		return this;
	};

	/**
	 * Register home page widget
	 * @param {string} name				Widget name
	 * @param {Function} renderFunc		Register function
	 * @param {boolean} isSite			true will show in site page or will shown in main page
	 */
	Module.prototype.widget = function (name, renderFunc, isSite) {
		this.widgetList.push({
			widget: {
				name: name,
				renderFunc: renderFunc
			},
			isSite: isSite
		});
		return this;
	};

	Module.prototype.require = function (scriptURL) {
		var _this = this;

		_this.requireRest += 1;
		if(!_this.requireDeferred) {
			_this.requireDeferred = $.Deferred();
		}

		setTimeout(function () {
			$.getScript(_this.baseURL + "/" + scriptURL).then(function () {
				if(_hookRequireFunc) {
					_hookRequireFunc(_this);
				} else {
					console.error("Hook function not set!", _this);
				}
			}).always(function () {
				_hookRequireFunc = null;
				_this.requireRest -= 1;
				_this.requireCheck();
			});
		}, 0);
	};

	Module.prototype.requireCSS = function (styleURL) {
		var _this = this;
		setTimeout(function () {
			$("<link/>", {
				rel: "stylesheet",
				type: "text/css",
				href: _this.baseURL + "/" + styleURL + "?_=" + _TRS()
			}).appendTo("head");
		}, 0);
	};

	Module.prototype.requireCheck = function () {
		if(this.requireRest === 0) {
			this.requireDeferred.resolve();
		}
	};

	/**
	 * Get module instance. Will init angular module.
	 * @param {string} moduleName	angular module name
	 */
	Module.prototype.getInstance = function (moduleName) {
		var _this = this;
		var deferred = $.Deferred();
		var module = angular.module(moduleName, this.dependencies);

		// Required list
		$.when(this.requireDeferred).always(function () {
			// Fill module props
			$.each(_this.queueList, function (i, item) {
				var type = item.type;
				var args = Array.prototype.slice.apply(item.args);
				if (type === "controller") {
					args[0] = moduleName + "_" + args[0];
				}
				module[type].apply(module, args);
			});

			// Render routes
			var routeList = $.map(_this.routeList, function (route) {
				var config = route.config = $.extend({}, route.config);

				// Parse url
				if(config.site) {
					config.url = "/site/:siteId/" + config.url.replace(/^[\\\/]/, "");
				}

				// Parse template url
				var parser = document.createElement('a');
				parser.href = _this.baseURL + "/" + config.templateUrl;
				parser.search = parser.search ? parser.search + "&_=" + window._TRS() : "?_=" + window._TRS();
				config.templateUrl = parser.href;

				if (typeof config.controller === "string") {
					config.controller = moduleName + "_" + config.controller;
				}

				return route;
			});

			// Portal update
			$.each(_this.portalList, function (i, config) {
				config.portal.application = moduleName;
			});

			// Widget update
			$.each(_this.widgetList, function (i, config) {
				config.widget.application = moduleName;
			});

			deferred.resolve({
				application: moduleName,
				portalList: _this.portalList,
				routeList: routeList,
				widgetList: _this.widgetList
			});
		});

		return deferred;
	};

	window.register = function (dependencies) {
		if($.isArray(dependencies)) {
			_lastRegisterApp = new Module(dependencies);
		} else if(typeof dependencies === "function") {
			_hookRequireFunc = function (module) {
				dependencies(module);
			};
		}
		return _lastRegisterApp;
	};

	// ======================================================================================
	// =                                        Main                                        =
	// ======================================================================================
	$(function () {
		console.info("[Eagle] Application initialization...");

		// Load providers
		$.get(_host + "/rest/apps/providers").then(function (res) {
			/**
			 * @param {{}} oriApp					application provider
			 * @param {string} oriApp.viewPath		path of application interface
			 */
			var promiseList = $.map(res.data || [], function (oriApp) {
				var deferred = $.Deferred();
				var viewPath = common.getValueByPath(_app, [oriApp.type, "viewPath"], oriApp.viewPath);

				if(viewPath) {
					var url = viewPath;
					url = url.replace(/^[\\\/]/, "").replace(/[\\\/]$/, "");

					$.getScript(url + "/index.js").then(function () {
						if(_lastRegisterApp) {
							_registerAppList.push(oriApp.type);
							_lastRegisterApp.baseURL = url;
							_lastRegisterApp.getInstance(oriApp.type).then(function (module) {
								deferred.resolve(module);
							});
						} else {
							console.error("Application not register:", oriApp.type);
							deferred.resolve();
						}
					}, function () {
						console.error("Load application failed:", oriApp.type, viewPath);
						deferred.resolve();
					}).always(function () {
						_lastRegisterApp = null;
					});
				} else {
					deferred.resolve();
				}

				return deferred;
			});

			common.deferred.all(promiseList).then(function (moduleList) {
				// Filter module list
				moduleList = $.map(moduleList, function (module) {
					return module;
				});

				var routeList = $.map(moduleList, function (module) {
					return module.routeList;
				});
				var portalList = $.map(moduleList, function (module) {
					return module.portalList;
				});
				var widgetList = $.map(moduleList, function (module) {
					return module.widgetList;
				});

				$(document).trigger("APPLICATION_READY", {
					appList: _registerAppList,
					moduleList: moduleList,
					routeList: routeList,
					portalList: portalList,
					widgetList: widgetList
				});
			});
		});
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var app = {};

(function() {
	'use strict';

	$(document).on("APPLICATION_READY", function (event, register) {
		console.info("[Eagle] Angular bootstrap...");

		var STATE_NAME_MATCH = /^[^.]*/;
		var state_next;
		var state_current;
		var param_next;
		var param_current;

		var modules = {};
		$.each(register.moduleList, function (i, module) {
			modules[module.application] = module;
		});

		// ======================================================================================
		// =                                   Initialization                                   =
		// ======================================================================================
		var eagleApp = angular.module('eagleApp', ['ngRoute', 'ngAnimate', 'ui.router', 'eagleControllers', 'eagle.service'].concat(register.appList));

		// ======================================================================================
		// =                                   Router config                                    =
		// ======================================================================================
		function routeResolve(config) {
			var resolve = {};
			if(config === false) return resolve;

			config = $.extend({
				auth: true,
				site: true,
				application: true
			}, config);

			if(config.auth) {
				// TODO: need auth module
			}

			resolve.Site = function (Site) {
				return Site.getPromise(config);
			};

			resolve.Application = function (Application) {
				return Application.getPromise();
			};

			resolve._router = function (Site, $wrapState, $q) {
				var name = state_next.name;
				var siteId = param_next.siteId;
				if (siteId && name !== "site") {
					return Site.getPromise(config).then(function () {
						var match =  false;
						$.each(common.getValueByPath(Site.find(siteId), ["applicationList"]), function (i, app) {
							var appType = app.descriptor.type;
							var module = modules[appType];
							if(!module) return;

							if(common.array.find(name, module.routeList, ["state"])) {
								match = true;
								return false;
							}
						});

						if(!match) {
							console.log("[Application] No route match:", name);
							$wrapState.go("site", {siteId: siteId});
							return $q.reject(false);
						}
					});
				} else {
					return true;
				}
			};

			resolve.Time = function (Time) {
				return Time.getPromise(config, state_next, param_next);
			};

			return resolve;
		}

		eagleApp.config(function ($stateProvider, $urlRouterProvider, $httpProvider, $animateProvider) {
			$urlRouterProvider.otherwise("/");
			$stateProvider
			// ================================== Home ==================================
				.state('home', {
					url: "/",
					templateUrl: "partials/home.html?_=" + window._TRS(),
					controller: "homeCtrl",
					resolve: routeResolve()
				})
				.state('setup', {
					url: "/setup",
					templateUrl: "partials/setup.html?_=" + window._TRS(),
					controller: "setupCtrl",
					resolve: routeResolve({ site: false, application: false })
				})
				// ================================= Alerts =================================
				.state('alertList', {
					url: "/alerts",
					templateUrl: "partials/alert/list.html?_=" + window._TRS(),
					controller: "alertListCtrl",
					resolve: routeResolve()
				})
				.state('policyList', {
					url: "/policies",
					templateUrl: "partials/alert/policyList.html?_=" + window._TRS(),
					controller: "policyListCtrl",
					resolve: routeResolve()
				})
				.state('streamList', {
					url: "/streams",
					templateUrl: "partials/alert/streamList.html?_=" + window._TRS(),
					controller: "alertStreamListCtrl",
					resolve: routeResolve()
				})
				.state('policyCreate', {
					url: "/policy/create",
					templateUrl: "partials/alert/policyEdit/main.html?_=" + window._TRS(),
					controller: "policyCreateCtrl",
					resolve: routeResolve()
				})
				.state('policyEdit', {
					url: "/policy/edit/{name}",
					templateUrl: "partials/alert/policyEdit/main.html?_=" + window._TRS(),
					controller: "policyEditCtrl",
					resolve: routeResolve()
				})

				.state('alertDetail', {
					url: "/alert/detail/{alertId}",
					templateUrl: "partials/alert/detail.html?_=" + window._TRS(),
					controller: "alertDetailCtrl",
					resolve: routeResolve()
				})
				.state('policyDetail', {
					url: "/policy/detail/{name}",
					templateUrl: "partials/alert/policyDetail.html?_=" + window._TRS(),
					controller: "policyDetailCtrl",
					resolve: routeResolve()
				})

				// =============================== Integration ==============================
				.state('integration', {
					abstract: true,
					url: "/integration/",
					templateUrl: "partials/integration/main.html?_=" + window._TRS(),
					controller: "integrationCtrl",
					resolve: routeResolve(false)
				})
				.state('integration.siteList', {
					url: "siteList",
					templateUrl: "partials/integration/siteList.html?_=" + window._TRS(),
					controller: "integrationSiteListCtrl",
					resolve: routeResolve({ application: false })
				})
				.state('integration.site', {
					url: "site/:id",
					templateUrl: "partials/integration/site.html?_=" + window._TRS(),
					controller: "integrationSiteCtrl",
					resolve: routeResolve({ application: false })
				})
				.state('integration.applicationList', {
					url: "applicationList",
					templateUrl: "partials/integration/applicationList.html?_=" + window._TRS(),
					controller: "integrationApplicationListCtrl",
					resolve: routeResolve({ application: false })
				})

				// ================================== Site ==================================
				.state('site', {
					url: "/site/:siteId",
					templateUrl: "partials/site/home.html?_=" + window._TRS(),
					controller: "siteCtrl",
					resolve: routeResolve()
				})
			;

			// =========================== Application States ===========================
			$.each(register.routeList, function (i, route) {
				var config = $.extend({}, route.config);

				var resolve = {};
				var resolveConfig = {};
				if(route.config.resolve) {
					$.each(route.config.resolve, function (key, value) {
						if (typeof value === "function") {
							resolve[key] = value;
						} else {
							resolveConfig[key] = value;
						}
					});
				}
				config.resolve = $.extend(routeResolve(resolveConfig), resolve);

				$stateProvider.state(route.state, config);
			});

			/* $httpProvider.interceptors.push(function($q) {
				function eagleRequestHandle(res) {
					var data = res.data || {
						exception: "",
						message: ""
					};
					if(res.status === -1) {
						$.dialog({
							title: "AJAX Failed",
							content: $("<pre>")
								.text("url:\n" + common.getValueByPath(res, ["config", "url"]))
						});
					} else if(data.success === false || res.status === 404) {
						$.dialog({
							title: "AJAX Error",
							content: $("<pre>")
								.text(
									"url:\n" + common.getValueByPath(res, ["config", "url"]) + "\n\n" +
									"status:\n" + res.status + "\n\n" +
									"exception:\n" + data.exception + "\n\n" +
									"message:\n" + data.message
								)
						});
					}
					return res;
				}

				return {
					response: eagleRequestHandle,
					responseError: function(res) {
						return $q.reject(eagleRequestHandle(res));
					}
				};
			}); */
		});

		// ======================================================================================
		// =                                   Main Controller                                  =
		// ======================================================================================
		eagleApp.controller('MainCtrl', function ($scope, $wrapState, $urlRouter, PageConfig, Portal, Widget, Entity, Site, Application, UI, Time, Policy) {
			window._WrapState = $scope.$wrapState = $wrapState;
			window._PageConfig = $scope.PageConfig = PageConfig;
			window._Portal = $scope.Portal = Portal;
			window._Widget = $scope.Widget = Widget;
			window._Entity = $scope.Entity = Entity;
			window._Site = $scope.Site = Site;
			window._Application = $scope.Application = Application;
			window._UI = $scope.UI = UI;
			window._Time = $scope.Time = Time;
			window._Policy = $scope.Policy = Policy;
			$scope.common = common;

			$scope._TRS = window._TRS();

			Object.defineProperty(window, "scope", {
				get: function () {
					var ele = $("#content .nav-tabs-custom.ng-scope .ng-scope[ui-view], #content .ng-scope[ui-view]").last();
					return angular.element(ele).scope();
				}
			});

			// ============================== Route Update ==============================
			$scope.$on('$stateChangeStart', function (event, next, nextParam, current, currentParam) {
				console.log("[Switch] current ->", current, currentParam);
				console.log("[Switch] next ->", next, nextParam);

				state_next = next || {};
				state_current = current || {};
				param_next = nextParam;
				param_current = currentParam;

				var currentName = (current || {}).name || "";
				var nextName = (next || {}).name || "";

				// Page initialization
				if(currentName.match(STATE_NAME_MATCH)[0] !== nextName.match(STATE_NAME_MATCH)[0]) {
					PageConfig.reset();
				}
			});

			$scope.$on('$stateChangeSuccess', function (event) {
				var _innerSearch = Time._innerSearch;
				Time._innerSearch = null;
				if(_innerSearch) {
					setTimeout(function () {
						$wrapState.go(".", $.extend({}, $wrapState.param, _innerSearch), {location: "replace", notify: false});
					}, 0);
				}
				console.log("[Switch] done ->", event);
			});

			// ================================ Function ================================
			// Get side bar navigation item class
			$scope.getNavClass = function (portal) {
				var path = (portal.path || "").replace(/^#/, '');

				if ($wrapState.path() === path) {
					return "active";
				} else {
					return "";
				}
			};

			// Customize time range
			$scope.customizeTimeRange = function () {
				$("#eagleStartTime").val(Time.format("startTime"));
				$("#eagleEndTime").val(Time.format("endTime"));
				$("#eagleTimeRangeMDL").modal();
			};

			$scope.setLastDuration = function (hours) {
				var endTime = new Time();
				var startTime = endTime.clone().subtract(hours, "hours");
				Time.timeRange(startTime, endTime);
			};

			$scope.updateTimeRange = function () {
				var startTime = Time.verifyTime($("#eagleStartTime").val());
				var endTime = Time.verifyTime($("#eagleEndTime").val());
				if(startTime && endTime) {
					Time.timeRange(startTime, endTime);
					$("#eagleTimeRangeMDL").modal("hide");
				} else {
					alert("Time range not validate");
				}
			};

			// ================================== Init ==================================
			$.each(register.portalList, function (i, config) {
				Portal.register(config.portal, config.isSite);
			});

			$.each(register.widgetList, function (i, config) {
				Widget.register(config.widget, config.isSite);
			});
		});

		// ======================================================================================
		// =                                      Bootstrap                                     =
		// ======================================================================================
		//noinspection JSCheckFunctionSignatures
		angular.element(document).ready(function() {
			console.info("[Eagle] UI start...");

			//noinspection JSCheckFunctionSignatures
			angular.bootstrap(document, ['eagleApp']);

			$("body").removeClass("ng-init-lock");
			$("#appLoadTip").remove();
		});
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleSrv = angular.module('eagle.service', []);
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var UNITS = [
		["days", "day", "day"],
		["hours", "hr", "hr"],
		["minutes", "min", "min"],
		["seconds", "s", "s"]
	];

	var keepTime = false;
	var serviceModule = angular.module('eagle.service');

	serviceModule.service('Time', function($q, $wrapState) {
		var startTime, endTime;
		var reloadListenerList = [];

		var $Time = function (time) {
			var _mom;

			if(arguments.length === 1 && time === undefined) {
				return null;
			}

			switch (time) {
				case "startTime":
					return startTime;
				case "endTime":
					return endTime;
				case "month":
					_mom = new moment();
					_mom.utcOffset($Time.UTC_OFFSET);
					_mom.date(1).hours(0).minutes(0).seconds(0).millisecond(0);
					break;
				case "monthEnd":
					_mom = $Time("month").add(1, "month").subtract(1, "s");
					break;
				case "week":
					_mom = new moment();
					_mom.utcOffset($Time.UTC_OFFSET);
					_mom.weekday(0).hours(0).minutes(0).seconds(0).millisecond(0);
					break;
				case "weekEnd":
					_mom = $Time("week").add(7, "d").subtract(1, "s");
					break;
				case "day":
					_mom = new moment();
					_mom.utcOffset($Time.UTC_OFFSET);
					_mom.hours(0).minutes(0).seconds(0).millisecond(0);
					break;
				case "dayEnd":
					_mom = $Time("day").add(1, "d").subtract(1, "s");
					break;
				case "hour":
					_mom = new moment();
					_mom.utcOffset($Time.UTC_OFFSET);
					_mom.minutes(0).seconds(0).millisecond(0);
					break;
				case "hourEnd":
					_mom = $Time("hour").add(1, "h").subtract(1, "s");
					break;
				default:
					// Parse string number
					if(typeof time === "string") {
						if(!isNaN(+time)) {
							time = +time;
						} else {
							time = new moment(time);
							time.add(time.utcOffset(), "minutes");
						}
					}

					_mom = new moment(time);
					_mom.utcOffset($Time.UTC_OFFSET);
			}
			return _mom;
		};

		$Time.TIME_RANGE_PICKER = "timeRange";
		$Time.pickerType = null;
		$Time._reloadListenerList = reloadListenerList;

		// TODO: time zone
		$Time.UTC_OFFSET = 0;

		$Time.FORMAT = "YYYY-MM-DD HH:mm:ss";
		$Time.SHORT_FORMAT = "MM-DD HH:mm";

		$Time.format = function (time, format) {
			time = $Time(time);
			return time ? time.format(format || $Time.FORMAT) : "-";
		};

		$Time.startTime = function () {
			return startTime;
		};

		$Time.endTime = function () {
			return endTime;
		};

		$Time.timeRange = function (startTimeValue, endTimeValue) {
			startTime = $Time(startTimeValue);
			endTime = $Time(endTimeValue);

			keepTime = true;
			$wrapState.go(".", $.extend({}, $wrapState.param, {
				startTime: $Time.format(startTime),
				endTime: $Time.format(endTime)
			}), {notify: false});

			$.each(reloadListenerList, function (i, listener) {
				listener($Time);
			});
		};

		$Time.onReload = function (func, $scope) {
			reloadListenerList.push(func);

			// Clean up
			if($scope) {
				$scope.$on('$destroy', function() {
					$Time.offReload(func);
				});
			}
		};

		$Time.offReload = function (func) {
			reloadListenerList = $.grep(reloadListenerList, function(_func) {
				return _func !== func;
			});
		};

		$Time.verifyTime = function(str, format) {
			format = format || $Time.FORMAT;
			var date = $Time(str);
			if(str === $Time.format(date, format)) {
				return date;
			}
			return null;
		};

		$Time.diff = function (from, to) {
			from = $Time(from);
			to = $Time(to);
			if (!from || !to) return null;
			return to.diff(from);
		};

		$Time.diffStr = function (from, to) {
			var diff = from;
			if(arguments.length === 2) {
				diff = $Time.diff(from, to);
			}
			if(diff === null) return "-";
			if(diff === 0) return "0s";

			var match = false;
			var rows = [];
			var duration = moment.duration(diff);
			var rest = 3;

			$.each(UNITS, function (i, unit) {
				var interval = Math.floor(duration[unit[0]]());
				if(interval > 0) match = true;

				if(match) {
					if(interval !== 0) {
						rows.push(interval + (interval > 1 ? unit[1] : unit[2]));
					}

					rest -=1;
					if(rest === 0) return false;
				}
			});

			return rows.join(", ");
		};

		$Time.diffInterval = function (from, to) {
			var timeDiff = $Time.diff(from, to);
			if(timeDiff <= 1000 * 60 * 60 * 6) {
				return 1000 * 60 * 5;
			} else if(timeDiff <= 1000 * 60 * 60 * 24) {
				return 1000 * 60 * 15;
			} else if(timeDiff <= 1000 * 60 * 60 * 24 * 7) {
				return 1000 * 60 * 30;
			} else if(timeDiff <= 1000 * 60 * 60 * 24 * 14) {
				return 1000 * 60 * 60;
			} else {
				return 1000 * 60 * 60 * 24;
			}
		};

		$Time.align = function (time, interval, ceil) {
			time = $Time(time);
			if(!time) return null;

			var func = ceil ? Math.ceil : Math.floor;

			var timestamp = time.valueOf();
			return $Time(func(timestamp / interval) * interval);
		};

		$Time.millionFormat = function (num) {
			if(!num) return "-";
			num = Math.floor(num / 1000);
			var s = num % 60;
			num = Math.floor(num / 60);
			var m = num % 60;
			num = Math.floor(num / 60);
			var h = num % 60;
			return common.string.preFill(h, "0") + ":" +
				common.string.preFill(m, "0") + ":" +
				common.string.preFill(s, "0");
		};

		$Time.getPromise = function (config, state, param) {
			var deferred = $q.defer();

			if(keepTime) {
				keepTime = false;
				deferred.resolve($Time);
			} else {
				if (config.time === true) {
					$Time.pickerType = $Time.TIME_RANGE_PICKER;

					startTime = $Time.verifyTime(param.startTime);
					endTime = $Time.verifyTime(param.endTime);

					if (!startTime || !endTime) {
						endTime = $Time();
						startTime = endTime.clone().subtract(2, "hour");

						keepTime = true;
						$Time._innerSearch = {
							startTime: $Time.format(startTime),
							endTime: $Time.format(endTime)
						};
					}
				} else {
					$Time._innerSearch = null;
					$Time.pickerType = null;
				}
				deferred.resolve($Time);
			}

			return deferred.promise;
		};

		return $Time;
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var serviceModule = angular.module('eagle.service');

	// ============================================================
	// =                           Page                           =
	// ============================================================
	serviceModule.service('PageConfig', function() {
		function PageConfig() {
		}

		PageConfig.reset = function () {
			PageConfig.title = "";
			PageConfig.subTitle = "";
			PageConfig.navPath = [];
			PageConfig.hideTitle = false;
		};

		return PageConfig;
	});

	// ============================================================
	// =                          Portal                          =
	// ============================================================
	serviceModule.service('Portal', function($wrapState, Site, Application) {
		function checkSite() {
			return Site.list.length !== 0;
		}

		function checkApplication() {
			return checkSite() && Application.list.length !== 0;
		}

		var defaultPortalList = [
			{name: "Home", icon: "home", path: "#/"},
			{name: "Alert", icon: "bell", showFunc: checkApplication, list: [
				{name: "Alerts", path: "#/alerts"},
				{name: "Policies", path: "#/policies"},
				{name: "Streams", path: "#/streams"},
				{name: "Define Policy", path: "#/policy/create"}
			]}
		];
		var adminPortalList = [
			{name: "Integration", icon: "puzzle-piece", showFunc: checkSite, list: [
				{name: "Sites", path: "#/integration/siteList"},
				{name: "Applications", path: "#/integration/applicationList"}
			]}
		];

		var Portal = {};

		var mainPortalList = [];
		var sitePortalList = [];
		var connectedMainPortalList = [];
		var sitePortals = {};

		var backHome = {name: "Back", icon: "arrow-left", path: "#/"};

		Portal.register = function (portal, isSite) {
			(isSite ? sitePortalList : mainPortalList).push(portal);
		};

		function convertSitePortal(site, portal) {
			portal = $.extend({}, portal, {
				path: portal.path ? "#/site/" + site.siteId + "/" + portal.path.replace(/^[\\\/]/, "") : null
			});

			if(portal.list) {
				portal.list = $.map(portal.list, function (portal) {
					return convertSitePortal(site, portal);
				});
			}

			return portal;
		}

		Portal.refresh = function () {
			// TODO: check admin

			// Main level
			connectedMainPortalList = defaultPortalList.concat(adminPortalList);
			var siteList = $.map(Site.list, function (site) {
				return {
					name: site.siteName || site.siteId,
					path: "#/site/" + site.siteId
				};
			});
			connectedMainPortalList.push({name: "Sites", icon: "server", showFunc: checkApplication, list: siteList});

			// Site level
			sitePortals = {};
			$.each(Site.list, function (i, site) {
				var siteHome = {name: site.siteName || site.siteId + " Home", icon: "home", path: "#/site/" + site.siteId};
				sitePortals[site.siteId] = [backHome, siteHome].concat($.map(sitePortalList, function (portal) {
					var hasApp = !!common.array.find(portal.application, site.applicationList, "descriptor.type");
					if(hasApp) {
						return convertSitePortal(site, portal);
					}
				}));
			});
		};

		Object.defineProperty(Portal, 'list', {
			get: function () {
				var match = $wrapState.path().match(/^\/site\/([^\/]*)/);
				if(match && match[1]) {
					return sitePortals[match[1]];
				} else {
					return connectedMainPortalList;
				}
			}
		});


		// Initialization
		Site.onReload(Portal.refresh);

		Portal.refresh();

		return Portal;
	});
}());

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var serviceModule = angular.module('eagle.service');

	serviceModule.service('Widget', function($wrapState, Site, Application) {
		var Widget = {};

		var mainWidgetList = [];
		var siteWidgetList = [];

		var displayWidgetList = [];
		var siteWidgets = {};

		Widget.register = function (widget, isSite) {
			(isSite ? siteWidgetList : mainWidgetList).push(widget);
		};

		Widget.refresh = function () {
			// Common widget
			displayWidgetList = $.map(mainWidgetList, function (widget) {
				var hasApp = !!common.array.find(widget.application, Application.list, "descriptor.type");
				if(hasApp) {
					return widget;
				}
			});

			// Site widget
			siteWidgets = {};
			$.each(Site.list, function (i, site) {
				siteWidgets[site.siteId] = $.map(siteWidgetList, function (widget) {
					var hasApp = !!common.array.find(widget.application, site.applicationList, "descriptor.type");
					if(hasApp) {
						return widget;
					}
				});
			});
		};

		Object.defineProperty(Widget, 'list', {
			get: function () {
				var site = Site.current();
				if(!site) {
					return displayWidgetList;
				} else if(site.siteId) {
					return siteWidgets[site.siteId];
				} else {
					console.warn("Can't find current site id.");
					return [];
				}
			}
		});

		// Initialization
		Site.onReload(Widget.refresh);

		Widget.refresh();

		return Widget;
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var serviceModule = angular.module('eagle.service');
	serviceModule.service('$wrapState', function($state, $location, $stateParams) {
		var $wrapState = {};
		var _targetState = null;
		var _targetPriority = 0;

		// Go
		$wrapState.go = function(state, param, option, priority) {
			setTimeout(function() {
				_targetState = null;
				_targetPriority = 0;
			});

			if(typeof param !== "object") {
				priority = param;
				param = {};
				option = {};
			} else if(typeof option !== "object") {
				priority = option;
				option = {};
			}

			priority = priority === true ? 1 : (priority || 0);
			if(_targetPriority > priority) {
				console.log("[Wrap State] Go - low priority:", state, "(Skip)");
				return false;
			}

			if(_targetState !== state || priority) {
				if($state.current && $state.current.name === state && angular.equals($state.params, param)) {
					console.log("[Wrap State] Go reload.", $state);
					$state.reload();
				} else {
					console.log("[Wrap State] Go:", state, param, priority);
					$state.go(state, param, option);
				}
				_targetState = state;
				_targetPriority = priority;
				return true;
			} else {
				console.log("[Wrap State] Go:", state, "(Ignored)");
			}
			return false;
		};

		// Reload
		$wrapState.reload = function() {
			console.log("[Wrap State] Do reload.");
			$state.reload();
		};

		// Path
		$wrapState.path = function(path) {
			if(path !== undefined) {
				console.log("[Wrap State][Deprecated] Switch path:", path);
			}
			return $location.path(path);
		};

		// URL
		$wrapState.url = function(url) {
			if(url !== undefined) console.log("[Wrap State] Switch url:", url);
			return $location.url(url);
		};

		Object.defineProperties($wrapState, {
			// Origin $state
			origin: {
				get: function() {
					return $state;
				}
			},

			// Current
			current: {
				get: function() {
					return $state.current;
				}
			},

			// Parameter
			param: {
				get: function() {
					return $.extend({}, $location.search(), $stateParams);
				}
			},

			// State
			state: {
				get: function() {
					return $state;
				}
			}
		});

		$wrapState.search = $location.search.bind($location);
		$wrapState.$location = $location;

		return $wrapState;
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var serviceModule = angular.module('eagle.service');

	var _host = "";
	if(localStorage) {
		_host = localStorage.getItem("host") || "";
	}

	serviceModule.service('Entity', function($http, $q) {
		function Entity() {}

		function wrapList(list, promise) {
			list._done = false;
			list._promise = promise.then(function (res) {
				var data = res.data;
				list.splice(0);
				Array.prototype.push.apply(list, data.data);
				list._done = true;

				return res;
			});
			return withThen(list);
		}

		function withThen(list) {
			list._then = list._promise.then.bind(list._promise);
			return list;
		}

		// Dev usage. Set rest api source
		Entity._host = function (host) {
			console.warn("This function only used for development usage.");
			if(host) {
				_host = host.replace(/[\\\/]+$/, "");
				if(localStorage) {
					localStorage.setItem("host", _host);
				}
			}
			return _host;
		};

		Entity.query = function (url, param) {
			var list = [];
			list._refresh = function () {
				var config = {};
				if(param) config.params = param;
				return wrapList(list, $http.get(_host + "/rest/" + url, config));
			};

			return list._refresh();
		};

		Entity.create = Entity.post = function (url, entity) {
			var list = [];
			return wrapList(list, $http({
				method: 'POST',
				url: _host + "/rest/" + url,
				headers: {
					"Content-Type": "application/json"
				},
				data: entity
			}));
		};

		Entity.delete = function (url, uuid) {
			var list = [];
			return wrapList(list, $http({
				method: 'DELETE',
				url: _host + "/rest/" + url,
				headers: {
					"Content-Type": "application/json"
				},
				data: {uuid: uuid}
			}));
		};

		/**
		 * Merge 2 array into one. Will return origin list before target list is ready. Then fill with target list.
		 * @param oriList
		 * @param tgtList
		 * @return {[]}
		 */
		Entity.merge = function (oriList, tgtList) {
			oriList = oriList || [];

			var list = [].concat(oriList);
			list._done = tgtList._done;
			list._refresh = tgtList._refresh;
			list._promise = tgtList._promise;

			list._promise.then(function () {
				list.splice(0);
				Array.prototype.push.apply(list, tgtList);
				list._done = true;
			});

			list = withThen(list);

			return list;
		};

		// TODO: metadata will be removed
		Entity.queryMetadata = function (url, param) {
			var metaList = Entity.query('metadata/' +  url, param);
			var _refresh = metaList._refresh;

			function process() {
				metaList._then(function (res) {
					var data = res.data;
					if(!$.isArray(data)) {
						data = [data];
					}

					metaList.splice(0);
					Array.prototype.push.apply(metaList, data);
				});
			}

			metaList._refresh = function () {
				_refresh();
				process();
			};

			process();

			return metaList;
		};

		Entity.deleteMetadata = function (url) {
			return {
				_promise: $http.delete(_host + "/rest/metadata/" + url).then(function (res) {
					console.log(res);
				})
			};
		};

		return Entity;
	});
}());

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var serviceModule = angular.module('eagle.service');

	serviceModule.service('Site', function($q, $wrapState, Entity, Application) {
		var Site = {};
		var reloadListenerList = [];

		Site.list = [];

		// Link with application
		function linkApplications(siteList, ApplicationList) {
			$.each(siteList, function (i, site) {
				var applications = common.array.find(site.siteId, ApplicationList, 'site.siteId', true);

				$.each(applications, function (i, app) {
					app.descriptor = app.descriptor || {};
					var oriApp = Application.providers[app.descriptor.type];
					Object.defineProperty(app, 'origin', {
						configurable: true,
						get: function () {
							return oriApp;
						}
					});
				});

				Object.defineProperties(site, {
					applicationList: {
						configurable: true,
						get: function () {
							return applications;
						}
					}
				});
			});
		}

		// Load sites
		Site.reload = function () {
			var list = Site.list = Entity.query('sites');
			list._promise.then(function () {
				linkApplications(list, Application.list);
				$.each(reloadListenerList, function (i, listener) {
					listener(Site);
				});
			});
			return Site;
		};

		Site.onReload = function (func) {
			reloadListenerList.push(func);
		};

		// Find Site
		Site.find = function (siteId) {
			return common.array.find(siteId, Site.list, 'siteId');
		};

		Site.current = function () {
			return Site.find($wrapState.param.siteId);
		};

		Site.switchSite = function (site) {
			var param = $wrapState.param;
			if(!site) {
				$wrapState.go("home");
			} else if(param.siteId) {
				$wrapState.go($wrapState.current.name, $.extend({}, param, {siteId: site.siteId}));
			} else {
				$wrapState.go("site", {siteId: site.siteId});
			}
		};

		Site.getPromise = function (config) {
			var siteList = Site.list;

			return $q.all([siteList._promise, Application.getPromise()]).then(function() {
				// Site check
				if(config && config.site !== false && siteList.length === 0) {
					$wrapState.go('setup', 1);
					return $q.reject(Site);
				}

				// Application check
				if(config && config.application !== false && Application.list.length === 0) {
					$wrapState.go('integration.site', {id: siteList[0].siteId}, 1);
					return $q.reject(Site);
				}

				return Site;
			});
		};

		// Initialization
		Application.onReload(function () {
			Site.reload();
		});

		Site.reload();

		return Site;
	});
}());

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var serviceModule = angular.module('eagle.service');

	serviceModule.service('Application', function($q, $wrapState, Entity) {
		var Application = {};
		var reloadListenerList = [];

		Application.list = [];

		Application.find = function (type, site) {
			return $.grep(Application.list, function (app) {
				return app.descriptor.type === type && (site ? app.site.siteId === site : true);
			});
		};

		// Load applications
		Application.reload = function () {
			Application.list = Entity.query('apps');
			Application.list._then(function () {
				$.each(reloadListenerList, function (i, listener) {
					listener(Application);
				});
			});
			return Application;
		};

		Application.onReload = function (func) {
			reloadListenerList.push(func);
		};

		// Load providers
		Application.providers = {};
		Application.providerList = Entity.query('apps/providers');
		Application.providerList._promise.then(function () {
			$.each(Application.providerList, function (i, oriApp) {
				Application.providers[oriApp.type] = oriApp;
			});
		});

		Application.findProvider = function (type) {
			var provider = common.array.find(type, Application.providerList, ["type"]);
			if(provider) return provider;

			var app = common.array.find(type, Application.list, ["appId"]);
			if(!app || !app.descriptor) return null;

			return common.array.find(app.descriptor.type, Application.providerList, ["type"]);
		};

		Application.getPromise = function () {
			return Application.list._promise.then(function() {
				return Application;
			});
		};

		// Initialization
		Application.reload();

		return Application;
	});
}());

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var serviceModule = angular.module('eagle.service');

	function wrapPromise(promise) {
		var retFunc = function (notifyFunc, resolveFunc, rejectFunc) {
			promise.then(resolveFunc, rejectFunc, function (holder) {
				notifyFunc(holder.entity, holder.closeFunc, holder.unlock);
			});
		};

		retFunc.then = promise.then;

		return retFunc;
	}

	/**
	 * Check function to check fields pass or not
	 * @callback checkFieldFunction
	 * @param {{}} entity
	 * @return {string}
	 */

	serviceModule.service('UI', function($rootScope, $q, $compile) {
		function UI() {}

		function _bindShortcut($dialog) {
			$dialog.on("keydown", function (event) {
				if(event.which === 13) {
					if(!$(":focus").is("textarea")) {
						$dialog.find(".confirmBtn:enabled").click();
					}
				}
			});
		}

		function _fieldDialog(create, name, entity, fieldList, checkFunc) {
			var _deferred, $mdl, $scope;

			var _entity = entity || {};

			_deferred = $q.defer();
			$scope = $rootScope.$new(true);
			$scope.name = name;
			$scope.entity = _entity;
			$scope.fieldList = fieldList.concat();
			$scope.checkFunc = checkFunc;
			$scope.lock = false;
			$scope.create = create;

			$scope.config = typeof name === "object" ? name : {};

			// Init
			if(!entity) {
				$.each(fieldList, function (i, field) {
					if(field.defaultValue) {
						_entity[field.field] = field.defaultValue;
					}
				});
			}

			// Modal
			$mdl = $(TMPL_FIELDS).appendTo('body');
			$compile($mdl)($scope);
			$mdl.modal();
			setTimeout(function () {
				$mdl.find("input, select").filter(':visible:first:enabled').focus();
			}, 500);

			$mdl.on("hide.bs.modal", function() {
				_deferred.reject();
			});
			$mdl.on("hidden.bs.modal", function() {
				_deferred.resolve({
					entity: _entity
				});
				$mdl.remove();

				if ($(".modal-backdrop").length) {
					$("body").addClass("modal-open");
				}
			});

			// Function
			$scope.getFieldDescription = function (field) {
				if(typeof field.description === "function") {
					return field.description($scope.entity);
				}
				return field.description || ((field.name || field.field) + '...');
			};

			$scope.emptyFieldList = function() {
				return $.map(fieldList, function(field) {
					if(!field.optional && !_entity[field.field]) {
						return field.field;
					}
				});
			};

			$scope.newField = function () {
				UI.fieldConfirm({
					title: "New Field"
				}, null, [{
					field: "field",
					name: "Field Name"
				}])(function (entity, closeFunc, unlock) {
					if(common.array.find(entity.field, $scope.fieldList, "field")) {
						$.dialog({
							title: "OPS",
							content: "Field already exist!"
						});

						unlock();
					} else {
						$scope.fieldList.push({
							field: entity.field,
							_customize: true
						});

						closeFunc();
					}
				});
			};

			$scope.removeField = function (field) {
				$scope.fieldList = common.array.remove(field, $scope.fieldList);
			};

			$scope.confirm = function() {
				$scope.lock = true;
				_deferred.notify({
					entity: _entity,
					closeFunc: function() {
						$mdl.modal('hide');
					},
					unlock: function() {
						$scope.lock = false;
					}
				});
			};

			_bindShortcut($mdl);

			return _deferred.promise;
		}

		/***
		 * Create a customize field confirm modal.
		 * @param {string} name							- Create entity name
		 * @param {object} entity						- Bind entity
		 * @param {Object[]} fieldList					- Display fields
		 * @param {string} fieldList[].field				- Mapping entity field
		 * @param {string=} fieldList[].name				- Field display name
		 * @param {*=} fieldList[].defaultValue				- Field default value. Only will be set if entity object is undefined
		 * @param {string=} fieldList[].type				- Field types: 'select', 'blob'
		 * @param {number=} fieldList[].rows				- Display as textarea if rows is set
		 * @param {string=} fieldList[].description			- Display as placeholder
		 * @param {boolean=} fieldList[].optional			- Optional field will not block the confirm
		 * @param {boolean=} fieldList[].readonly			- Read Only can not be updated
		 * @param {string[]=} fieldList[].valueList			- For select type usage
		 * @param {checkFieldFunction=} checkFunc	- Check logic function. Return string will prevent access
		 */
		UI.createConfirm = function(name, entity, fieldList, checkFunc) {
			return wrapPromise(_fieldDialog(true, name, entity, fieldList, checkFunc));
		};

		/***
		 * Create a customize field confirm modal.
		 * @param {object} config						- Configuration object
		 * @param {string} config.title						- Title of dialog box
		 * @param {string=} config.size						- "large". Set dialog size
		 * @param {boolean=} config.addable					- Set add customize field
		 * @param {boolean=} config.confirm					- Display or not confirm button
		 * @param {string=} config.confirmDesc				- Confirm button display description
		 * @param {object} entity						- bind entity
		 * @param {Object[]} fieldList					- Display fields
		 * @param {string} fieldList[].field				- Mapping entity field
		 * @param {string=} fieldList[].name				- Field display name
		 * @param {*=} fieldList[].defaultValue				- Field default value. Only will be set if entity object is undefined
		 * @param {string=} fieldList[].type				- Field types: 'select', 'blob'
		 * @param {number=} fieldList[].rows				- Display as textarea if rows is set
		 * @param {string=} fieldList[].description			- Display as placeholder
		 * @param {boolean=} fieldList[].optional			- Optional field will not block the confirm
		 * @param {boolean=} fieldList[].readonly			- Read Only can not be updated
		 * @param {string[]=} fieldList[].valueList			- For select type usage
		 * @param {checkFieldFunction=} checkFunc	- Check logic function. Return string will prevent access
		 */
		UI.fieldConfirm = function(config, entity, fieldList, checkFunc) {
			return wrapPromise(_fieldDialog("field", config, entity, fieldList, checkFunc));
		};

		UI.deleteConfirm = function (name) {
			var _deferred, $mdl, $scope;

			_deferred = $q.defer();
			$scope = $rootScope.$new(true);
			$scope.name = name;
			$scope.lock = false;

			// Modal
			$mdl = $(TMPL_DELETE).appendTo('body');
			$compile($mdl)($scope);
			$mdl.modal();

			$mdl.on("hide.bs.modal", function() {
				_deferred.reject();
			});
			$mdl.on("hidden.bs.modal", function() {
				_deferred.resolve({
					name: name
				});
				$mdl.remove();

				if ($(".modal-backdrop").length) {
					$("body").addClass("modal-open");
				}
			});

			// Function
			$scope.delete = function() {
				$scope.lock = true;
				_deferred.notify({
					name: name,
					closeFunc: function() {
						$mdl.modal('hide');
					},
					unlock: function() {
						$scope.lock = false;
					}
				});
			};

			return wrapPromise(_deferred.promise);
		};

		return UI;
	});

	// ===========================================================
	// =                         Template                        =
	// ===========================================================
	var TMPL_FIELDS =
		'<div class="modal fade" tabindex="-1" role="dialog">' +
		'<div class="modal-dialog" ng-class="{\'modal-lg\': config.size === \'large\'}" role="document">' +
		'<div class="modal-content">' +
		'<div class="modal-header">' +
			'<button type="button" class="close" data-dismiss="modal" aria-label="Close">' +
				'<span aria-hidden="true">&times;</span>' +
			'</button>' +
			'<h4 class="modal-title">{{config.title || (create ? "New" : "Update") + " " + name}}</h4>' +
		'</div>' +
		'<div class="modal-body">' +
			'<div class="form-group" ng-repeat="field in fieldList" ng-switch="field.type">' +
				'<label for="featureName">' +
					'<span ng-if="!field.optional && !field._customize">*</span> ' +
					'<a ng-if="field._customize" class="fa fa-times" ng-click="removeField(field)"></a> ' +
					'{{field.name || field.field}}' +
				'</label>' +
				'<textarea class="form-control" placeholder="{{getFieldDescription(field)}}" ng-model="entity[field.field]" rows="{{ field.rows || 10 }}" ng-readonly="field.readonly" ng-disabled="lock" ng-switch-when="blob"></textarea>' +
				'<select class="form-control" ng-model="entity[field.field]" ng-init="entity[field.field] = entity[field.field] || field.valueList[0]" ng-switch-when="select">' +
				'<option ng-repeat="value in field.valueList">{{value}}</option>' +
				'</select>' +
				'<input type="text" class="form-control" placeholder="{{getFieldDescription(field)}}" ng-model="entity[field.field]" ng-readonly="field.readonly" ng-disabled="lock" ng-switch-default>' +
			'</div>' +
			'<a ng-if="config.addable" ng-click="newField()">+ New field</a>' +
		'</div>' +
		'<div class="modal-footer">' +
			'<p class="pull-left text-danger">{{checkFunc(entity)}}</p>' +
			'<button type="button" class="btn btn-default" data-dismiss="modal" ng-disabled="lock">Close</button>' +
			'<button type="button" class="btn btn-primary confirmBtn" ng-click="confirm()" ng-disabled="checkFunc(entity) || emptyFieldList().length || lock" ng-if="config.confirm !== false">' +
				'{{config.confirmDesc || (create ? "Create" : "Update")}}' +
			'</button>' +
		'</div>' +
		'</div>' +
		'</div>' +
		'</div>';

	var TMPL_DELETE =
		'<div class="modal fade" tabindex="-1" role="dialog" aria-hidden="true">' +
		'<div class="modal-dialog">' +
		'<div class="modal-content">' +
		'<div class="modal-header">' +
		'<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>' +
		'<h4 class="modal-title">Delete Confirm</h4></div>' +
		'<div class="modal-body">' +
		'<span class="text-red fa fa-exclamation-triangle pull-left" style="font-size: 50px;"></span>' +
		'<p>You are <strong class="text-red">DELETING</strong> \'{{name}}\'!</p>' +
		'<p>Proceed to delete?</p>' +
		'</div>' +
		'<div class="modal-footer">' +
		'<button type="button" class="btn btn-danger" ng-click="delete()" ng-disabled="lock">Delete</button>' +
		'<button type="button" class="btn btn-default" data-dismiss="modal" ng-disabled="lock">Cancel</button>' +
		'</div>' +
		'</div>' +
		'</div>' +
		'</div>';
}());

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var serviceModule = angular.module('eagle.service');

	serviceModule.service('Policy', function($q, UI, Entity) {
		return {
			publisherTypes: {
				'org.apache.eagle.alert.engine.publisher.impl.AlertEmailPublisher': {
					name: "Email",
					displayFields: ["recipients"],
					fields: ["subject", "template", "sender", "recipients"]
				},
				'org.apache.eagle.alert.engine.publisher.impl.AlertKafkaPublisher': {
					name: "Kafka",
					displayFields: ["topic"],
					fields: ["topic", "kafka_broker"]
				},
				'org.apache.eagle.alert.engine.publisher.impl.AlertSlackPublisher': {
					name: "Slack",
					displayFields: ["channels"],
					fields: ["token", "channels", "severitys", "urltemplate"]
				},
				'org.apache.eagle.alert.engine.publisher.impl.AlertEagleStorePlugin': {
					name: "Storage",
					displayFields: [],
					fields: []
				}
			},

			delete: function (policy) {
				var deferred = $q.defer();

				UI.deleteConfirm(policy.name)(function (entity, closeFunc) {
					Entity.deleteMetadata("policies/" + policy.name)._promise.finally(function () {
						closeFunc();
						deferred.resolve();
					});
				}, function () {
					deferred.reject();
				});

				return deferred.promise;
			},

			start: function (policy) {
				return Entity.post("metadata/policies/" + encodeURIComponent(policy.name) + "/status/ENABLED", {})._promise;
			},

			stop: function (policy) {
				return Entity.post("metadata/policies/" + encodeURIComponent(policy.name) + "/status/DISABLED", {})._promise;
			}
		};
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


(function() {
	'use strict';

	angular.module('eagle.components', []);
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleComponents = angular.module('eagle.components');

	eagleComponents.directive('sortTable', function($compile) {
		return {
			restrict: 'AE',
			scope: true,
			//terminal: true,
			priority: 1001,

			/**
			 * @param $scope
			 * @param $element
			 * @param {{}} $attrs
			 * @param {string} $attrs.sortTable			Data source
			 * @param {string?} $attrs.isSorting		Will bind parent variable of sort state
			 * @param {string?} $attrs.scope			Will bind parent variable of current scope
			 * @param {string?} $attrs.sortpath			Default sort path
			 * @param {[]?} $attrs.searchPathList		Filter search path list
			 */
			controller: function($scope, $element, $attrs) {
				var sortmatch;
				var worker;
				var worker_id = 0;
				if(typeof(Worker) !== "undefined") {
					worker = new Worker("public/js/worker/sortTableWorker.js?_=" + window._TRS());
				}

				// Initialization
				$scope.pageNumber = 1;
				$scope.pageSize = 10;
				$scope.maxSize = 10;
				$scope.search = "";
				$scope.orderKey = "";
				$scope.orderAsc = true;

				if($attrs.sortpath) {
					sortmatch = $attrs.sortpath.match(/^(-)?(.*)$/);
					if(sortmatch[1]) {
						$scope.orderAsc = false;
					}
					$scope.orderKey = sortmatch[2];
				}

				// UI - Column sort
				$scope.doSort = function(path) {
					if($scope.orderKey === path) {
						$scope.orderAsc = !$scope.orderAsc;
					} else {
						$scope.orderKey = path;
						$scope.orderAsc = true;
					}
				};
				$scope.checkSortClass = function(key) {
					if($scope.orderKey === key) {
						return "fa sort-mark " + ($scope.orderAsc ? "fa-sort-asc" : "fa-sort-desc");
					}
					return "fa fa-sort sort-mark";
				};

				// List filter & sort
				function setState(bool) {
					if(!$attrs.isSorting) return;

					$scope.$parent[$attrs.isSorting] = bool;
				}


				var cacheSearch = "";
				var cacheOrder = "";
				var cacheOrderAsc = null;
				var cacheFilteredList = null;
				$scope.getFilteredList = function () {
					if(
						cacheSearch !== $scope.search ||
						cacheOrder !== $scope.orderKey ||
						cacheOrderAsc !== $scope.orderAsc ||
						!cacheFilteredList
					) {
						cacheSearch = $scope.search;
						cacheOrder = $scope.orderKey;
						cacheOrderAsc = $scope.orderAsc;

						var fullList = $scope.$parent[$attrs.sortTable] || [];
						if(!cacheFilteredList) cacheFilteredList = fullList;

						if(!worker) {
							cacheFilteredList = __sortTable_generateFilteredList(fullList, cacheSearch, cacheOrder, cacheOrderAsc, $scope.$parent[$attrs.searchPathList]);
							setState(false);
						} else {
							worker_id += 1;
							setState(true);
							var list = JSON.stringify(fullList);
							worker.postMessage({
								search: cacheSearch,
								order: cacheOrder,
								orderAsc: cacheOrderAsc,
								searchPathList: $scope.$parent[$attrs.searchPathList],
								list: list,
								id: worker_id
							});
						}
					}

					return cacheFilteredList;
				};

				// Week watch. Will not track each element
				$scope.$watch($attrs.sortTable + ".length", function () {
					cacheFilteredList = null;
				});
				$scope.$watch($attrs.sortTable + ".___SORT_TABLE___", function () {
					var fullList = $scope.$parent[$attrs.sortTable];
					if(fullList && !fullList.___SORT_TABLE___) {
						fullList.___SORT_TABLE___ = +new Date();
						cacheFilteredList = null;
					}
				});

				function workMessage(event) {
					var data = event.data;
					if(worker_id !== data.id) return;

					setState(false);
					cacheFilteredList = data.list;
					$scope.$apply();
				}
				worker.addEventListener("message", workMessage);

				$scope.$on('$destroy', function() {
					worker.removeEventListener("message", workMessage);
				});

				// Scope bind
				if($attrs.scope) {
					$scope.$parent[$attrs.scope] = $scope;
				}
			},
			compile: function ($element) {
				var contents = $element.contents().remove();

				return {
					post: function preLink($scope, $element) {
						$scope.defaultPageSizeList = [10, 25, 50, 100];

						$element.append(contents);

						// Tool Container
						var $toolContainer = $(
							'<div class="tool-container clearfix">' +
							'</div>'
						).insertBefore($element.find("table"));

						// Search Box
						var $search = $(
							'<div class="search-box">' +
							'<input type="search" class="form-control input-sm" placeholder="Search" ng-model="search" />' +
							'<span class="fa fa-search" />' +
							'</div>'
						).appendTo($toolContainer);
						$compile($search)($scope);

						// Page Size
						var $pageSize = $(
							'<div class="page-size">' +
							'Show' +
							'<select class="form-control" ng-model="pageSize" convert-to-number>' +
							'<option ng-repeat="size in pageSizeList || defaultPageSizeList track by $index">{{size}}</option>' +
							'</select>' +
							'Entities' +
							'</div>'
						).appendTo($toolContainer);
						$compile($pageSize)($scope);

						// Sort Column
						$element.find("table [sortpath]").each(function () {
							var $this = $(this);
							var _sortpath = $this.attr("sortpath");
							$this.attr("ng-click", "doSort('" + _sortpath + "')");
							$this.prepend('<span ng-class="checkSortClass(\'' + _sortpath + '\')"></span>');
							$compile($this)($scope);
						});

						// Repeat Items
						var $tr = $element.find("table [ts-repeat], table > tbody > tr").filter(":first");
						$tr.attr("ng-repeat", 'item in getFilteredList().slice((pageNumber - 1) * pageSize, pageNumber * pageSize) track by $index');
						$compile($tr)($scope);

						// Page Navigation
						var $navigation = $(
							'<div class="navigation-bar clearfix">' +
							'<span>' +
							'show {{(pageNumber - 1) * pageSize + 1}} to {{pageNumber * pageSize}} of {{getFilteredList().length}} items' +
							'</span>' +
							'<uib-pagination total-items="getFilteredList().length" ng-model="pageNumber" boundary-links="true" items-per-page="pageSize" max-size="maxSize"></uib-pagination>' +
							'</div>'
						).appendTo($element);
						$compile($navigation)($scope);
					}
				};
			}
		};
	});

	eagleComponents.directive('convertToNumber', function() {
		return {
			require: 'ngModel',
			link: function(scope, element, attrs, ngModel) {
				ngModel.$parsers.push(function(val) {
					return parseInt(val, 10);
				});
				ngModel.$formatters.push(function(val) {
					return '' + val;
				});
			}
		};
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleComponents = angular.module('eagle.components');

	eagleComponents.service('Chart', function () {
		return {
			color: [ "#0073b7", "#dd4b39", "#00a65a", "#f39c12", "#605ca8", "#001F3F", "#39CCCC", "#D81B60", "#3c8dbc", "#f56954", "#00c0ef", "#3D9970", "#FF851B"  , "#01FF70", "#F012BE"],
			//color: ['#4285f4', '#c23531','#2f4554', '#61a0a8', '#d48265', '#91c7ae','#749f83',  '#ca8622', '#bda29a','#6e7074', '#546570', '#c4ccd3'],
			charts: {}
		};
	});

	eagleComponents.directive('chart', function(Chart) {
		var charts = Chart.charts;

		function chartResize() {
			setTimeout(function () {
				$.each(charts, function (id, chart) {
					chart.resize();
				});
			}, 310);
		}

		$(window).resize(chartResize);
		$("body").on("expanded.pushMenu collapsed.pushMenu", chartResize);

		return {
			restrict: 'AE',
			scope: {
				title: "@?title",
				series: "=",
				category: "=?category",
				categoryFunc: "=?categoryFunc",
				xTitle: "@?xTitle",
				yTitle: "@?yTitle",

				option: "=?option",

				click: "=?ngClick",

				chart: "@?chart"
			},
			controller: function ($scope, $element, $attrs, Time) {
				var i;
				var lastTooltipEvent;
				var chart = echarts.init($element[0]);
				charts[chart.id] = chart;

				function refreshChart() {
					var maxYAxis = 0;
					var legendList = [];
					var categoryList = $scope.category ? $scope.category : [];

					var seriesList = $.map($scope.series || [], function (series, id) {
						if(id === 0 && !$scope.category) {
							//var preDate = -1;
							categoryList = $.map(series.data, function (point) {
								/*ivar time = new Time(point.x);
								f(preDate !== time.date()) {
									preDate = time.date();
									return Time.format(point.x, "MMM.D HH:mm");
								}*/
								if($scope.categoryFunc) {
									return $scope.categoryFunc(point.x);
								}
								return Time.format(point.x, "HH:mm");
							});
						}

						legendList.push(series.name);
						if(series.yAxisIndex) maxYAxis = Math.max(series.yAxisIndex, maxYAxis);

						return $.extend({}, series, {
							data: $scope.category ? series.data : $.map(series.data, function (point) {
								return point.y;
							})
						});
					});

					var yAxis = [];
					for(i = 0 ; i <= maxYAxis ; i += 1) {
						yAxis.push({
							name: $scope.yTitle,
							type: "value"
						});
					}

					var option = {
						color: Chart.color.concat(),
						title: [{text: $scope.title}],
						tooltip: {trigger: 'axis'},
						legend: [{
							data: legendList
						}],
						grid: {
							top: '30',
							left: '0',
							right: '0',
							bottom: '0',
							containLabel: true
						},
						xAxis: {
							name: $scope.xTitle,
							type: 'category',
							data: categoryList,
							axisTick: { show: false }
						},
						yAxis: yAxis,
						series: seriesList
					};

					if($scope.option) {
						option = common.merge(option, $scope.option);
					}

					chart.setOption(option);
				}

				// Event handle
				var chartClick = false;
				chart.on("click", function (e) {
					if($scope.click) {
						if($scope.click(e)) {
							refreshChart();
						}
					}
					chartClick = true;
				});

				chart.getZr().on('click', function () {
					if(!chartClick && $scope.click) {
						if($scope.click($.extend({
							componentType: "tooltip"
						}, lastTooltipEvent))) {
							refreshChart();
						}
					}
					chartClick = false;
				});

				chart.on('showtip', function (e) {
					lastTooltipEvent = e;
				});

				// Insert chart object to parent scope
				if($attrs.chart) {
					$scope.$parent.$parent[$attrs.chart] = chart;
				}

				chart.refresh = function () {
					refreshChart();
				};

				// Render
				refreshChart();
				$scope.$watch("series", refreshChart);

				$scope.$on('$destroy', function() {
					delete charts[chart.id];
					chart.dispose();

					delete $scope.$parent.$parent[$attrs.chart];
				});
			},
			template: '<div>Loading...</div>',
			replace: true
		};
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleComponents = angular.module('eagle.components');

	eagleComponents.directive('widget', function($compile, Site) {
		return {
			restrict: 'AE',
			priority: 1001,

			controller: function($scope, $element, $attrs) {
			},
			compile: function ($element) {
				$element.contents().remove();

				return {
					post: function preLink($scope, $element) {
						var widget = $scope.widget;
						$scope.site = Site.current();

						if(widget.renderFunc) {
							// Prevent auto compile if return false
							if(widget.renderFunc($element, $scope, $compile) !== false) {
								$compile($element.contents())($scope);
							}
						} else {
							$element.append("Widget don't provide render function:" + widget.application + " - " + widget.name);
						}
					}
				};
			}
		};
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleComponents = angular.module('eagle.components');

	eagleComponents.directive('staticInclude', function($http, $templateCache, $compile) {
		return function(scope, element, attrs) {
			var templatePath = attrs.staticInclude;
			$http.get(templatePath, { cache: $templateCache }).success(function(response) {
				var contents = element.html(response).contents();
				$compile(contents)(scope);
			});
		};
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleComponents = angular.module('eagle.components');

	eagleComponents.directive('editor', function() {
		return {
			restrict: 'AE',
			require: 'ngModel',

			link: function($scope, $element, $attrs, $ctrl) {
				$element.innerHeight(21 * Number($attrs.rows || 10));

				var updateId;
				function updateScope(value) {
					clearTimeout(updateId);

					updateId = setTimeout(function () {
						$ctrl.$setViewValue(value);
					}, 0);
				}

				var editLock = false;

				var editor = ace.edit($element[0]);
				var session = editor.getSession();
				editor.container.style.lineHeight = 1.5;
				editor.setOptions({
					fontSize: "14px"
				});
				editor.setTheme("ace/theme/tomorrow");
				editor.$blockScrolling = Infinity;
				editor.getSession().on('change', function(event) {
					editLock = true;

					var value = session.getValue();
					updateScope(value);

				});
				session.setUseWorker(false);
				session.setUseWrapMode(true);
				session.setMode("ace/mode/sql");

				$scope.$watch($attrs.ngModel, function (newValue) {
					if(editLock) {
						editLock = false;
						return;
					}

					session.setValue(newValue || "");
				});

				$scope.$on('$destroy', function() {
					editor.destroy();
				});
			},
			template: '<div class="form-control"></div>',
			replace: true
		};
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleComponents = angular.module('eagle.components');

	eagleComponents.directive('naBlock', function() {
		return {
			scope: {
				naBlock: "="
			},
			restrict: 'AE',
			transclude: true,

			template:
			'<div>' +
				'<span ng-if="naBlock && naBlock !== true">{{naBlock}}</span>' +
				'<code ng-if="!naBlock">N/A</code>' +
				'<div ng-if="naBlock === true" ng-transclude></div>' +
			'</div>',
			replace: true
		};
	});
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleControllers = angular.module('eagleControllers', ['ui.bootstrap', 'eagle.components', 'eagle.service']);

	// ===========================================================
	// =                        Controller                       =
	// ===========================================================
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleControllers = angular.module('eagleControllers');

	// ======================================================================================
	// =                                        Home                                        =
	// ======================================================================================
	eagleControllers.controller('homeCtrl', function ($scope, $wrapState, PageConfig) {
		PageConfig.title = "Home";

		$scope.colorList = [
			"bg-aqua",
			"bg-green",
			"bg-yellow",
			"bg-red"
		];
	});

	// ======================================================================================
	// =                                       Set Up                                       =
	// ======================================================================================
	eagleControllers.controller('setupCtrl', function ($wrapState, $scope, PageConfig, Entity, Site) {
		PageConfig.hideTitle = true;

		$scope.lock = false;
		$scope.siteId = "sandbox";
		$scope.siteName = "Sandbox";
		$scope.description = "";

		$scope.createSite = function () {
			$scope.lock = true;

			Entity.create("sites", {
				siteId: $scope.siteId,
				siteName: $scope.siteName,
				description: $scope.description
			})._then(function () {
				Site.reload();
				$wrapState.go('home');
			}, function (res) {
				$.dialog({
					title: "OPS!",
					content: res.message
				});
			}).finally(function () {
				$scope.lock = false;
			});
		};
	});
}());

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleControllers = angular.module('eagleControllers');

	// ======================================================================================
	// =                                        Alert                                       =
	// ======================================================================================
	eagleControllers.controller('alertListCtrl', function ($scope, $wrapState, $interval, PageConfig, Entity) {
		PageConfig.title = "Alerts";

		$scope.displayType = "raw";
		$scope.alertList = Entity.queryMetadata("alerts", {size: 10000});

		// ================================================================
		// =                             Sync                             =
		// ================================================================
		var refreshInterval = $interval($scope.alertList._refresh, 1000 * 10);
		$scope.$on('$destroy', function() {
			$interval.cancel(refreshInterval);
		});
	});

	eagleControllers.controller('alertDetailCtrl', function ($scope, $wrapState, PageConfig, Entity) {
		PageConfig.title = "Alert Detail";

		$scope.alertList = Entity.queryMetadata("alerts/" + encodeURIComponent($wrapState.param.alertId));
		$scope.alertList._then(function () {
			$scope.alert = $scope.alertList[0];
			if(!$scope.alert) {
				$.dialog({
					title: "OPS",
					content: "Alert '" + $wrapState.param.alertId + "' not found!"
				});
			}
		});
	});

	// ======================================================================================
	// =                                       Stream                                       =
	// ======================================================================================
	eagleControllers.controller('alertStreamListCtrl', function ($scope, $wrapState, PageConfig, Application, Entity) {
		PageConfig.title = "Streams";

		$scope.streamList = [];
		Entity.queryMetadata("streams")._then(function (res) {
			$scope.streamList = $.map(res.data, function (stream) {
				var application = Application.findProvider(stream.dataSource);
				return $.extend({application: application}, stream);
			});
		});

		$scope.dataSources = {};
		Entity.queryMetadata("datasources")._then(function(res) {
			$.each(res.data, function (i, dataSource) {
				$scope.dataSources[dataSource.name] = dataSource;
			});
		});

		$scope.showDataSource = function (stream) {
			var dataSource = $scope.dataSources[stream.dataSource];
			$.dialog({
				title: dataSource.name,
				content: $("<pre class='text-break'>").html(JSON.stringify(dataSource, null, "\t")),
				size: "large"
			});
		};
	});

	// ======================================================================================
	// =                                       Policy                                       =
	// ======================================================================================
	eagleControllers.controller('policyListCtrl', function ($scope, $wrapState, PageConfig, Entity, Policy) {
		PageConfig.title = "Policies";

		$scope.policyList = [];

		function updateList() {
			var list = Entity.queryMetadata("policies");
			list._then(function () {
				$scope.policyList = list;
			});
		}
		updateList();

		$scope.deletePolicy = function(policy) {
			Policy.delete(policy).then(updateList);
		};

		$scope.startPolicy = function(policy) {
			Policy.start(policy).then(updateList);
		};

		$scope.stopPolicy = function(policy) {
			Policy.stop(policy).then(updateList);
		};
	});

	eagleControllers.controller('policyDetailCtrl', function ($scope, $wrapState, $interval, PageConfig, Entity, Policy) {
		PageConfig.title = "Policy";
		PageConfig.subTitle = "Detail";
		PageConfig.navPath = [
			{title: "Policy List", path: "/policies"},
			{title: "Detail"}
		];

		$scope.tab = "setting";
		$scope.displayType = "raw";

		$scope.setTab = function (tab) {
			$scope.tab = tab;
		};

		function updatePolicy() {
			var policyName = $wrapState.param.name;
			var encodePolicyName = encodeURIComponent(policyName);
			var policyList = Entity.queryMetadata("policies/" + encodePolicyName);
			policyList._promise.then(function () {
				$scope.policy = policyList[0];
				console.log("[Policy]", $scope.policy);

				if(!$scope.policy) {
					$.dialog({
						title: "OPS",
						content: "Policy '" + $wrapState.param.name + "' not found!"
					}, function () {
						$wrapState.go("policyList");
					});
					return;
				}

				Entity.post("metadata/policies/parse", $scope.policy.definition.value)._then(function (res) {
					$scope.executionPlan = res.data;
				});
			});

			$scope.policyPublisherList = Entity.queryMetadata("policies/" + encodePolicyName + "/publishments/");

			Entity.queryMetadata("schedulestates")._then(function (res) {
				var schedule = res.data || {};
				$scope.assignment = common.array.find(policyName, schedule.assignments, ["policyName"]) || {};

				var queueList = $.map(schedule.monitoredStreams, function (stream) {
					return stream.queues;
				});
				$scope.queue = common.array.find($scope.assignment.queueId, queueList, ["queueId"]);
			});
		}
		updatePolicy();

		$scope.alertList = Entity.queryMetadata("policies/" + encodeURIComponent($wrapState.param.name) + "/alerts", {size: 1000});

		$scope.deletePolicy = function() {
			Policy.delete($scope.policy).then(function () {
				$wrapState.go("policyList");
			});
		};

		$scope.startPolicy = function() {
			Policy.start($scope.policy).then(updatePolicy);
		};

		$scope.stopPolicy = function() {
			Policy.stop($scope.policy).then(updatePolicy);
		};

		var refreshInterval = $interval($scope.alertList._refresh, 1000 * 60);
		$scope.$on('$destroy', function() {
			$interval.cancel(refreshInterval);
		});
	});
}());

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleControllers = angular.module('eagleControllers');

	// ======================================================================================
	// =                                    Policy Create                                   =
	// ======================================================================================
	function connectPolicyEditController(entity, args) {
		var newArgs = [entity];
		Array.prototype.push.apply(newArgs, args);
		/* jshint validthis: true */
		policyEditController.apply(this, newArgs);
	}

	eagleControllers.controller('policyCreateCtrl', function ($scope, $q, $wrapState, $timeout, PageConfig, Entity, Policy) {
		PageConfig.title = "Define Policy";
		connectPolicyEditController({}, arguments);
	});
	eagleControllers.controller('policyEditCtrl', function ($scope, $q, $wrapState, $timeout, PageConfig, Entity, Policy) {
		PageConfig.title = "Edit Policy";
		var args = arguments;

		$scope.policyList = Entity.queryMetadata("policies/" + encodeURIComponent($wrapState.param.name));
		$scope.policyList._promise.then(function () {
			var policy = $scope.policyList[0];

			if(policy) {
				connectPolicyEditController(policy, args);
			} else {
				$.dialog({
					title: "OPS",
					content: "Policy '" + $wrapState.param.name + "' not found!"
				}, function () {
					$wrapState.go("policyList");
				});
			}
		});
	});

	function policyEditController(policy, $scope, $q, $wrapState, $timeout, PageConfig, Entity, Policy) {
		$scope.publisherTypes = Policy.publisherTypes;

		$scope.policy = policy;
		$scope.policy = common.merge({
			name: "",
			description: "",
			inputStreams: [],
			outputStreams: [],
			definition: {
				type: "siddhi",
				value: ""
			},
			alertDefinition: {
				subject: "",
				body: "",
				severity: "WARNING",
				category: "DEFAULT"
			},
			partitionSpec: [],
			parallelismHint: 5
		}, $scope.policy);
		console.log("[Policy]", $scope.policy);

		var cacheSearchType;
		var cacheSearchSourceKey;
		var searchApplications;

		$scope.searchType = "app";
		$scope.searchSourceKey = "";
		$scope.applications = {};
		$scope.newPolicy = !$scope.policy.name;
		$scope.autoPolicyDescription = $scope.newPolicy && !$scope.policy.description;

		PageConfig.navPath = [
			{title: "Policy List", path: "/policies"},
			{title: ($scope.newPolicy ? "Define" : "Update") + " Policy"}
		];

		// ==============================================================
		// =                             UI                             =
		// ==============================================================
		$scope.sourceTab = "all";
		$scope.setSourceTab = function (tab) {
			$scope.sourceTab = tab;
		};

		// ==============================================================
		// =                        Input Stream                        =
		// ==============================================================
		$scope.getSearchApplication = function() {
			if(cacheSearchSourceKey !== $scope.searchSourceKey.toUpperCase() || cacheSearchType !== $scope.searchType) {
				var match = false;
				cacheSearchSourceKey = $scope.searchSourceKey.toUpperCase();
				cacheSearchType = $scope.searchType;

				searchApplications = {};
				$.each($scope.applications, function (appName, streams) {
					$.each(streams, function (i, stream) {
						var groupName = cacheSearchType === "app" ? stream.dataSource : stream.siteId;
						if(
							groupName.toUpperCase().indexOf(cacheSearchSourceKey) >= 0 ||
							stream.streamId.toUpperCase().indexOf(cacheSearchSourceKey) >= 0
						) {
							match = true;
							var group = searchApplications[groupName] = searchApplications[groupName] || [];
							group.push(stream);
						}
					});
				});

				if(!match) {
					searchApplications = null;
				}
			}
			return searchApplications;
		};

		$scope.streams = {};
		$scope.streamList = Entity.queryMetadata("streams");
		$scope.streamList._then(function () {
			$scope.applications = {};
			cacheSearchSourceKey = null;

			$.each($scope.streamList, function (i, stream) {
				var list = $scope.applications[stream.dataSource] = $scope.applications[stream.dataSource] || [];
				list.push(stream);
				$scope.streams[stream.streamId] = stream;
			});
		});

		$scope.isInputStreamSelected = function (streamId) {
			return $.inArray(streamId, $scope.policy.inputStreams) >= 0;
		};

		/*$scope.checkInputStream = function (streamId) {
			if($scope.isInputStreamSelected(streamId)) {
				$scope.policy.inputStreams = common.array.remove(streamId, $scope.policy.inputStreams);
			} else {
				$scope.policy.inputStreams.push(streamId);
			}
		};*/

		// ==============================================================
		// =                         Definition                         =
		// ==============================================================
		function autoDescription() {
			if(!$scope.autoPolicyDescription) return;

			$scope.policy.description = "Policy for " + $scope.policy.outputStreams.join(", ");
		}

		var checkPromise;
		$scope.definitionMessage = "";
		$scope.checkDefinition = function () {
			$timeout.cancel(checkPromise);
			checkPromise = $timeout(function () {
				Entity.post("metadata/policies/parse", $scope.policy.definition.value)._then(function (res) {
					var data = res.data;
					console.log(data);
					if(data.success) {
						$scope.definitionMessage = "";
						if(data.policyExecutionPlan) {
							// Input streams
							$scope.policy.inputStreams = $.map(data.policyExecutionPlan.inputStreams, function (value, stream) {
								return stream;
							});

							// Output streams
							var outputStreams= $.map(data.policyExecutionPlan.outputStreams, function (value, stream) {
								return stream;
							});
							$scope.policy.outputStreams = outputStreams.concat();
							$scope.outputStreams = outputStreams;
							autoDescription();

							// Partition
							$scope.policy.partitionSpec = data.policyExecutionPlan.streamPartitions;
						}
					} else {
						$scope.definitionMessage = data.message;
					}
				});
			}, 350);
		};

		// ==============================================================
		// =                        Output Stream                       =
		// ==============================================================
		$scope.outputStreams = ($scope.policy.outputStreams || []).concat();

		$scope.isOutputStreamSelected = function (streamId) {
			return $.inArray(streamId, $scope.policy.outputStreams) >= 0;
		};

		$scope.checkOutputStream = function (streamId) {
			if($scope.isOutputStreamSelected(streamId)) {
				$scope.policy.outputStreams = common.array.remove(streamId, $scope.policy.outputStreams);
			} else {
				$scope.policy.outputStreams.push(streamId);
			}
			autoDescription();
		};

		// ==============================================================
		// =                         Partition                          =
		// ==============================================================
		$scope.partition = {};

		$scope.addPartition = function () {
			$scope.partition = {
				streamId: $scope.policy.inputStreams[0],
				type: "GROUPBY",
				columns: []
			};
			$(".modal[data-id='partitionMDL']").modal();
		};

		$scope.newPartitionCheckColumn = function (column) {
			return $.inArray(column, $scope.partition.columns) >= 0;
		};

		$scope.newPartitionClickColumn = function (column) {
			if($scope.newPartitionCheckColumn(column)) {
				$scope.partition.columns = common.array.remove(column, $scope.partition.columns);
			} else {
				$scope.partition.columns.push(column);
			}
		};

		$scope.addPartitionConfirm = function () {
			$scope.policy.partitionSpec.push($scope.partition);
		};

		$scope.removePartition = function (partition) {
			$scope.policy.partitionSpec = common.array.remove(partition, $scope.policy.partitionSpec);
		};

		// ==============================================================
		// =                         Publisher                          =
		// ==============================================================
		$scope.publisherList = Entity.queryMetadata("publishments");
		$scope.addPublisherType = "";
		$scope.policyPublisherList = [];
		$scope.publisher = {};

		if(!$scope.newPolicy) {
			Entity.queryMetadata("policies/" + $scope.policy.name + "/publishments/")._then(function (res) {
				$scope.policyPublisherList = $.map(res.data, function (publisher) {
					return $.extend({
						_exist: true
					}, publisher);
				});
			});
		}

		$scope.addPublisher = function () {
			$scope.publisher = {
				existPublisher: $scope.publisherList[0],
				type: "org.apache.eagle.alert.engine.publisher.impl.AlertEmailPublisher",
				dedupIntervalMin: "PT1M",
				serializer : "org.apache.eagle.alert.engine.publisher.impl.StringEventSerializer",
				properties: {}
			};
			if($scope.publisherList.length) {
				$scope.addPublisherType = "exist";
			} else {
				$scope.addPublisherType = "new";
			}

			$(".modal[data-id='publisherMDL']").modal();
		};

		$scope.removePublisher = function (publisher) {
			$scope.policyPublisherList = common.array.remove(publisher, $scope.policyPublisherList);
		};

		$scope.checkPublisherName = function () {
			if(common.array.find($scope.publisher.name, $scope.publisherList.concat($scope.policyPublisherList), ["name"])) {
				return "'" + $scope.publisher.name + "' already exist";
			}
			return false;
		};

		$scope.addPublisherConfirm = function () {
			if($scope.addPublisherType === "exist") {
				$scope.publisher = $.extend({
					_exist: true
				}, $scope.publisher.existPublisher);
			}
			var properties = {};
			$.each(Policy.publisherTypes[$scope.publisher.type].fields, function (i, field) {
				properties[field] = $scope.publisher.properties[field] || "";
			});
			$scope.policyPublisherList.push($.extend({}, $scope.publisher, {properties: properties}));
		};

		// ==============================================================
		// =                            Save                            =
		// ==============================================================
		$scope.saveLock = false;
		$scope.saveCheck = function () {
			return (
				!$scope.saveLock &&
				$scope.policy.name &&
				common.number.parse($scope.policy.parallelismHint) > 0 &&
				$scope.policy.definition.value &&
				$scope.policy.outputStreams.length &&
				$scope.policyPublisherList.length
			);
		};

		$scope.saveConfirm = function () {
			$scope.saveLock = true;

			// Check policy
			Entity.post("metadata/policies/validate", $scope.policy)._then(function (res) {
				var validate = res.data;
				console.log(validate);
				if(!validate.success) {
					$.dialog({
						title: "OPS",
						content: validate.message
					});
					$scope.saveLock = false;
					return;
				}

				// Create publisher
				var publisherPromiseList = $.map($scope.policyPublisherList, function (publisher) {
					if(publisher._exist) return;

					return Entity.create("metadata/publishments", publisher)._promise;
				});
				if(publisherPromiseList.length) console.log("Creating publishers...", $scope.policyPublisherList);

				$q.all(publisherPromiseList).then(function () {
					console.log("Create publishers success...");

					// Create policy
					Entity.create("metadata/policies", $scope.policy)._then(function () {
						console.log("Create policy success...");
						// Link with publisher
						Entity.post("metadata/policies/" + $scope.policy.name + "/publishments/", $.map($scope.policyPublisherList, function (publisher) {
							return publisher.name;
						}))._then(function () {
							// Link Success
							$.dialog({
								title: "Done",
								content: "Close dialog to go to the policy detail page."
							}, function () {
								$wrapState.go("policyDetail", {name: $scope.policy.name});
							});
						}, function (res) {
							// Link Failed
							$.dialog({
								title: "OPS",
								content: "Link publishers failed:" + res.data.message
							});
						}).finally(function () {
							$scope.policyLock = false;
						});
					}, function (res) {
						$.dialog({
							title: "OPS",
							content: "Create policy failed: " + res.data.message
						});
						$scope.policyLock = false;
					});
				}, function (args) {
					$.dialog({
						title: "OPS",
						content: "Create publisher failed. More detail please check output in 'console'."
					});
					console.log("Create publishers failed:", args);
					$scope.policyLock = false;
				});
			}, function () {
				$scope.saveLock = false;
			});
		};
	}
})();

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleControllers = angular.module('eagleControllers');

	// ======================================================================================
	// =                                        Main                                        =
	// ======================================================================================
	eagleControllers.controller('integrationCtrl', function ($scope, $wrapState, PageConfig) {
		PageConfig.title = "Integration";
		$scope.getState = function() {
			return $wrapState.current.name;
		};
	});

	// ======================================================================================
	// =                                        Site                                        =
	// ======================================================================================
	eagleControllers.controller('integrationSiteListCtrl', function ($scope, $wrapState, PageConfig, UI, Entity, Site) {
		PageConfig.title = "Integration";
		PageConfig.subTitle = "Site";

		$scope.deleteSite = function (site) {
			UI.deleteConfirm(site.siteId)
			(function (entity, closeFunc, unlock) {
				Entity.delete("sites", site.uuid)._then(function (res) {
					Site.reload();
					closeFunc();
				}, function (res) {
					$.dialog({
						title: "OPS",
						content: common.getValueByPath(res, ["data", "message"])
					});
					unlock();
				});
			});
		};

		$scope.newSite = function () {
			UI.createConfirm("Site", {}, [
				{field: "siteId", name: "Site Id"},
				{field: "siteName", name: "Display Name", optional: true},
				{field: "description", name: "Description", optional: true, type: "blob", rows: 5}
			])(function (entity, closeFunc, unlock) {
				Entity.create("sites", entity)._then(function () {
					Site.reload();
					closeFunc();
				}, unlock);
			});
		};
	});

	eagleControllers.controller('integrationSiteCtrl', function ($sce, $scope, $wrapState, $interval, PageConfig, Entity, UI, Site, Application) {
		PageConfig.title = "Site";
		PageConfig.subTitle = $wrapState.param.id;

		// Check site
		$scope.site = Site.find($wrapState.param.id);
		if(!$scope.site) {
			$.dialog({
				title: "OPS",
				content: "Site not found!"
			}, function () {
				$wrapState.go("integration.siteList");
			});
			return;
		}
		console.log("[Site]", $scope.site);

		$scope.siteName = $scope.site.siteId + ($scope.site.siteName ? "(" + $scope.site.siteName + ")" : "");

		// Map applications
		function mapApplications() {
			Site.getPromise().then(function () {
				$scope.site = Site.find($wrapState.param.id);
				var uninstalledApplicationList = common.array.minus(Application.providerList, $scope.site.applicationList, "type", "descriptor.type");
				$scope.applicationList = common.array.doSort(
					$.map($scope.site.applicationList, function (app) {
						app.installed = true;
						return app;
					}), ["origin", "name"]
				).concat(
					common.array.doSort(
						$.map(uninstalledApplicationList, function (oriApp) {
							return {origin: oriApp};
						}), ["origin", "name"]
					)
				);
			});
		}
		mapApplications();

		// Application refresh
		var refreshApplications = $scope.refreshApplications = function() {
			Application.reload().getPromise().then(mapApplications);
		};

		// Application status class
		$scope.getAppStatusClass = function (application) {
			switch((application.status || "").toUpperCase()) {
				case "INITIALIZED":
					return "primary";
				case "STARTING":
					return "warning";
				case "RUNNING":
					return "success";
				case "STOPPING":
					return "warning";
				case "STOPPED":
					return "danger";
			}
			return "default";
		};

		// Get started application count
		$scope.getStartedAppCount = function () {
			return $.grep($scope.site.applicationList, function (app) {
				return $.inArray((app.status || "").toUpperCase(), ["STARTING", "RUNNING"]) >= 0;
			}).length;
		};

		// Application detail
		$scope.showAppDetail = function (application) {
			$("a[data-id='introTab']").click();

			$scope.tmpApp = application;
			application = application.origin;
			var docs = application.docs || {install: "", uninstall: ""};
			$scope.application = application;
			$scope.installHTML = $sce.trustAsHtml(docs.install);
			$scope.uninstallHTML = $sce.trustAsHtml(docs.uninstall);
			$("#appMDL").modal();
		};

		// ================================================================
		// =                         Installation                         =
		// ================================================================
		$scope.tmpApp = {};
		$scope.generalFields = [];
		$scope.advancedFields = [];
		$scope.customizeFields = [];
		$scope.installLock = false;

		// =================== Fields ===================
		$scope.newField = function () {
			UI.fieldConfirm({
				title: "New Property"
			}, null, [{
				field: "name",
				name: "Property Name"
			}, {
				field: "value",
				name: "Property Value",
				optional: true
			}])(function (entity, closeFunc, unlock) {
				var fullList = $scope.generalFields.concat($scope.advancedFields).concat($scope.customizeFields);
				if(common.array.find(entity.name, fullList, "field")) {
					$.dialog({
						title: "OPS",
						content: "Property already exist!"
					});

					unlock();
				} else {
					$scope.customizeFields.push({
						name: entity.name,
						_customize: true,
						required: true
					});
					$scope.tmpApp.configuration[entity.name] = entity.value;

					closeFunc();
				}
			});
		};

		$scope.removeField = function (field) {
			$scope.customizeFields = common.array.remove(field, $scope.customizeFields);
			delete $scope.tmpApp.configuration[field.name];
		};

		// =================== Check ===================
		$scope.checkJarPath = function () {
			var jarPath = ($scope.tmpApp || {}).jarPath;
			if(/\.jar$/.test(jarPath)) {
				$scope.tmpApp.mode = "CLUSTER";
			} else if(/\.class/.test(jarPath)) {
				$scope.tmpApp.mode = "LOCAL";
			}
		};

		$scope.collapseCheck = function () {
			setTimeout(function() {
				$scope.$apply();
			}, 400);
		};

		$scope.isCollapsed = function (dataId) {
			return !$("[data-id='" + dataId + "']").hasClass("in");
		};

		$scope.checkFields = function () {
			var pass = true;
			var config = common.getValueByPath($scope, ["tmpApp", "configuration"], {});
			$.each($scope.generalFields, function (i, field) {
				if (field.required && !config[field.name]) {
					pass = false;
					return false;
				}
			});
			return pass;
		};

		// =================== Config ===================
		$scope.getCustomFields = function (configuration) {
			var fields = common.getValueByPath($scope.application, "configuration.properties", []).concat();
			return $.map(configuration || {}, function (value, key) {
				if(!common.array.find(key, fields, ["name"])) {
					return {
						name: key,
						_customize: true,
						required: true
					};
				}
			});
		};

		$scope.configByJSON = function () {
			UI.fieldConfirm({
				title: "Configuration"
			}, {
				json: JSON.stringify($scope.tmpApp.configuration, null, "\t")
			}, [{
				field: "json",
				name: "JSON Configuration",
				type: "blob"
			}], function (entity) {
				var json = common.parseJSON(entity.json, false);
				if(!json) return 'Require JSON format';
			})(function (entity, closeFunc) {
				$scope.tmpApp.configuration = common.parseJSON(entity.json, {});
				$scope.customizeFields = $scope.getCustomFields($scope.tmpApp.configuration);
				closeFunc();
			});
		};

		// =================== Install ===================
		$scope.installAppConfirm = function () {
			$scope.installLock = true;

			var uuid = $scope.tmpApp.uuid;
			var app = $.extend({}, $scope.tmpApp);
			delete app.uuid;

			if(uuid) {
				Entity.create("apps/" + uuid, app)._then(function () {
					refreshApplications();
					$("#installMDL").modal("hide");
				}, function (res) {
					$.dialog({
						title: "OPS",
						content: res.data.message
					});
					$scope.installLock = false;
				});
			} else {
				Entity.create("apps/install", app)._then(function () {
					refreshApplications();
					$("#installMDL").modal("hide");
				}, function (res) {
					$.dialog({
						title: "OPS",
						content: res.data.message
					});
					$scope.installLock = false;
				});
			}
		};

		// Install application
		$scope.installApp = function (application, entity) {
			entity = entity || {};
			application = application.origin;
			$scope.installLock = false;
			$scope.application = application;

			var docs = application.docs || {install: "", uninstall: ""};
			$scope.installHTML = $sce.trustAsHtml(docs.install);

			$scope.tmpApp = {
				mode: "CLUSTER",
				jarPath: application.jarPath,
				configuration: {}
			};

			if(!entity.uuid) {
				common.merge($scope.tmpApp, {
					siteId: $scope.site.siteId,
					appType: application.type
				});
				$scope.checkJarPath();
			}

			var fields = common.getValueByPath(application, "configuration.properties", []).concat();
			$scope.generalFields = [];
			$scope.advancedFields = [];
			$scope.customizeFields = [];

			$("[data-id='appEnvironment']").attr("class","collapse in").removeAttr("style");
			$("[data-id='appGeneral']").attr("class","collapse in").removeAttr("style");
			$("[data-id='appAdvanced']").attr("class","collapse").removeAttr("style");
			$("[data-id='appCustomize']").attr("class","collapse in").removeAttr("style");

			$.each(fields, function (i, field) {
				// Fill default value
				$scope.tmpApp.configuration[field.name] = common.template(field.value || "", {
					siteId: $scope.site.siteId,
					appType: application.type
				});

				// Reassign the fields
				if(field.required) {
					$scope.generalFields.push(field);
				} else {
					$scope.advancedFields.push(field);
				}
			});

			// Fill miss field of entity
			common.merge($scope.tmpApp, entity);
			$scope.customizeFields = $scope.getCustomFields(entity.configuration);

			// Dependencies check
			var missDep = false;
			$.each(application.dependencies, function (i, dep) {
				if(!Application.find(dep.type, $scope.site.siteId)[0]) {
					missDep = true;
					return false;
				}
			});

			$("#installMDL").modal();
			if(missDep) {
				$("a[data-id='guideTab']").click();
			} else {
				$("a[data-id='configTab']").click();
			}
		};

		// Uninstall application
		$scope.uninstallApp = function (application) {
			UI.deleteConfirm(application.descriptor.name + " - " + application.site.siteId)
			(function (entity, closeFunc, unlock) {
				Entity.delete("apps/uninstall", application.uuid)._then(function () {
					refreshApplications();
					closeFunc();
				}, unlock);
			});
		};

		// ================================================================
		// =                          Management                          =
		// ================================================================
		// Start application
		$scope.startApp = function (application) {
			$.dialog({
				title: "Confirm",
				content: "Do you want to start '" + application.descriptor.name + "'?",
				confirm: true
			}, function (ret) {
				if(!ret) return;
				Entity.post("apps/start", { uuid: application.uuid })._then(function () {
					refreshApplications();
				});
			});
		};

		// Stop application
		$scope.stopApp = function (application) {
			$.dialog({
				title: "Confirm",
				content: "Do you want to stop '" + application.descriptor.name + "'?",
				confirm: true
			}, function (ret) {
				if(!ret) return;
				Entity.post("apps/stop", { uuid: application.uuid })._then(function () {
					refreshApplications();
				});
			});
		};

		$scope.editApp = function (application) {
			var type = application.descriptor.type;
			var provider = Application.findProvider(type);

			$scope.installApp({origin: provider}, {
				mode: application.mode,
				jarPath: application.jarPath,
				uuid: application.uuid,
				configuration: $.extend({}, application.configuration)
			});
		};

		// ================================================================
		// =                             Sync                             =
		// ================================================================
		var refreshInterval = $interval(refreshApplications, 1000 * 60);
		$scope.$on('$destroy', function() {
			$interval.cancel(refreshInterval);
		});
	});

	// ======================================================================================
	// =                                     Application                                    =
	// ======================================================================================
	eagleControllers.controller('integrationApplicationListCtrl', function ($sce, $scope, $wrapState, PageConfig, Application) {
		PageConfig.title = "Integration";
		PageConfig.subTitle = "Applications";

		$scope.showAppDetail = function(application) {
			var docs = application.docs || {install: "", uninstall: ""};
			$scope.application = application;
			$scope.installHTML = $sce.trustAsHtml(docs.install);
			$scope.uninstallHTML = $sce.trustAsHtml(docs.uninstall);
			$("#appMDL").modal();
		};
	});
}());

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
	'use strict';

	var eagleControllers = angular.module('eagleControllers');


	// ======================================================================================
	// =                                        Main                                        =
	// ======================================================================================
	eagleControllers.controller('siteCtrl', function ($scope, PageConfig, Site) {
		var site = Site.current();
		PageConfig.title = site.siteName || site.siteId;
		PageConfig.subTitle = "home";
	});
}());

//# sourceMappingURL=doc.js.map