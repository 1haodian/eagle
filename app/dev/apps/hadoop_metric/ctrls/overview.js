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
	 * `register` without params will load the module which using require
	 */
	register(function (hadoopMetricApp) {
		hadoopMetricApp.controller("overviewCtrl", function ($q, $wrapState, $scope, PageConfig, METRIC) {
			PageConfig.title = 'Overview';

			var metric_heapMB;
			var metricName = ['hadoop.memory.nonheapmemoryusage.used'];
			metric_heapMB = METRIC.metrics({site: $wrapState.param.siteId}, metricName, 20);
			$scope.commonOption = {};
			var series_metric_heapMB

			$q.all([metric_heapMB._promise]).then(function () {
				$scope.metricList = [
					mockMetric(metric_heapMB, 'name1', {}),
					mockMetric(metric_heapMB,'name1', {}),
					mockMetric(metric_heapMB,'name2', {smooth: true}),
					mockMetric(metric_heapMB,'name3', {areaStyle: {normal: {}}}),
					mockMetric(metric_heapMB,'name4', {type: 'bar'}),
					mockMetric(metric_heapMB,'name1', {}, 2),
					mockMetric(metric_heapMB,'name2', {smooth: true}, 2),
					mockMetric(metric_heapMB,'name3', {areaStyle: {normal: {}}, stack: 'one'}, 2),
					mockMetric(metric_heapMB,'name4', {type: 'bar', stack: 'one'}, 2),
					mockMetric(metric_heapMB,'name1', {}, 3),
					mockMetric(metric_heapMB,'name2', {smooth: true}, 3),
					mockMetric(metric_heapMB,'name3', {areaStyle: {normal: {}}, stack: 'one'}, 3),
					mockMetric(metric_heapMB,'name4', {type: 'bar', stack: 'one'}, 3)
				]
			})

			// Mock series data
			function mockMetric(metric_heapMB, name, option, count) {

				series_metric_heapMB = METRIC.metricsToSeries(name, metric_heapMB, option);
				count = count || 1;
				var now = +new Date();

				var series = [];
				for (var i = 0; i < count; i += 1) {
					series.push(series_metric_heapMB);
				}

				return {
					title: name,
					series: series
				};
			}


		});
	});
})();
//# sourceURL=overview.js
