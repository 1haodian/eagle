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
	register(function (metricApp) {
		metricApp.controller("overviewCtrl", function ($q, $wrapState, $element, $scope, $timeout, PageConfig, Time, Entity, METRIC) {


			$scope.site = $wrapState.param.siteId;

			PageConfig.title = "Overview";

			$scope.type = "hbase";
			$scope.dataseries = [];
			$scope.commonOption = {};


			var labelTop = {
				normal : {
					label : {
						show : true,
						position : 'center',
						formatter : '{b}',
						textStyle: {
							baseline : 'bottom'
						}
					},
					labelLine : {
						show : false
					}
				}
			};
			var labelFromatter = {
				normal : {
					label : {
						formatter : function (params){
							return 100 - params.value + '%'
						},
						textStyle: {
							baseline : 'top'
						}
					}
				},
			}
			var labelBottom = {
				normal : {
					color: '#ccc',
					label : {
						show : true,
						position : 'center'
					},
					labelLine : {
						show : false
					}
				},
				emphasis: {
					color: 'rgba(0,0,0,0)'
				}
			};
			var radius = [40, 55];
			var pieoption = {
				legend: {
					x : 'center',
					y : 'center',
				},
				series : [
					{
						type: 'pie',
						radius: radius,
						x: '0%', // for funnel
						itemStyle: labelFromatter,
						data: [
							{name: 'other', value: 46, itemStyle: labelBottom},
							{name: '', value: 54, itemStyle: labelTop}
						]
					}
				]
			};


			//	var chart = echarts.init($element[0]);
			var myChart = echarts.init(document.getElementById('main'));
			var myChart1 = echarts.init(document.getElementById('main1'));
			var myChart2 = echarts.init(document.getElementById('main2'));

			var option = {
				tooltip: {
					show: true
				},
				legend: {
					data: ['Region Assignment']
				},
				xAxis: [
					{
						type: 'category',
						data: ["1", "2", "3", "4", "5", "6"]
					}
				],
				yAxis: [
					{
						type: 'value'
					}
				],
				series: [
					{
						"name": "xiaoliang",
						"type": "bar",
						"data": [5, 20, 40, 10, 10, 20]
					}
				]
			};

			myChart.setOption(option);
			myChart1.setOption(pieoption);
			myChart2.setOption(option);

		});
	});
})();
