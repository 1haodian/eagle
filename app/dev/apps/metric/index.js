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
	/**
	 * `register` is global function that for application to set up 'controller', 'service', 'directive', 'route' in Eagle
	 */
	var metricApp = register(['ngRoute', 'ngAnimate', 'ui.router', 'eagle.service']);

    metricApp.route("HbaseOverview", {
    		url: "/metric/hbase/overview?startTime&endTime",
    		site: true,
    		templateUrl: "partials/hbase/overview.html",
    		controller: "listCtrl",
    		resolve: { time: true }
    	})
	metricApp.portal({name: "JMX Metric", icon: "taxi", list: [
		{name: "HBASE", path: "/metric/hbase/overview"}
	]}, true);

	metricApp.service("JPM", function ($q, $http, Time, Site, Application) {
		var METRIC = window._METRIC = {};

		return METRIC;
	});
	metricApp.requireCSS("style/index.css");
	metricApp.require("ctrl/overview.js");
})();
