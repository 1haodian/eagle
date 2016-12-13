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
				normal: {
					label: {
						show: true,
						position: 'center',
						formatter: '{b}',
						textStyle: {
							baseline: 'bottom'
						}
					},
					labelLine: {
						show: false
					},
					color: "#1E90FF"
				}
			};
			var labelFromatter = {
				normal: {
					label: {
						formatter: function (params) {
							return 100 - params.value + '%'
						},
						textStyle: {
							baseline: 'top'
						}
					}
				}
			};
			var labelBottom = {
				normal: {
					color: '#ccc',
					label: {
						show: true,
						position: 'center'
					},
					labelLine: {
						show: false
					}
				},
				emphasis: {
					color: 'rgba(0,0,0,0)'
				}
			};
			var radius = [40, 55];
			var pieoption = {
				legend: {
					x: 'center',
					y: 'center'
				},
				series: [
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
			var option = {
				tooltip: {
					show: true
				},
				xAxis: [
					{
						type: 'category',
						data: ["region 1", "region 2", "region 3", "region 4", "region 5", "region 6"],
					}
				],
				yAxis: [
					{
						type: 'value'
					}
				],
				series: [
					{
						"type": "bar",
						"data": [5, 20, 40, 10, 10, 20],
						itemStyle: {
							normal: {color: 'pink'}
						}
					}
				]
			};


			avgloadoption = {
				tooltip: {
					trigger: 'axis'
				},
				calculable: true,
				xAxis: [
					{
						type: 'category',
						boundaryGap: false,
						data: ['week1', 'week2', 'week3', 'week4']
					}
				],
				yAxis: [
					{
						type: 'value'
					}
				],
				series: [
					{
						name: 'load',
						type: 'line',
						smooth: true,
						itemStyle: {normal: {areaStyle: {type: 'default'}, color: 'green'}},
						data: [10, 80, 21, 54],
					}
				]
			};


			assignnumoption = {
				legend: {
					data: ['Assign_max', 'Assign_min']
				},
				calculable: true,
				xAxis: [
					{
						type: 'category',
						boundaryGap: false,
						barCategoryGap: '1',
						data: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul']
					}
				],
				yAxis: [
					{
						type: 'value',
						axisLabel: {
							formatter: '{value}'
						}
					}
				],
				series: [
					{
						type: 'line',
						data: [11, 2, 15, 13, 3, 4, 5],
						markPoint: {
							data: [
								{type: 'max', name: ''},
								{type: 'min', name: ''}
							]
						},
						markLine: {
							data: [
								{type: 'average', name: '75th',itemStyle: {normal: {areaStyle: {type: 'default'}, color: 'red'}}},
								{type: 'max', name: 'avg',itemStyle: {normal: {areaStyle: {type: 'default'}, color: 'green'}}}
							]
						}
					}
				]
			};

			assignnumoption1 = {
				legend: {
					data: ['Assign_max', 'Assign_min']
				},
				calculable: true,
				xAxis: [
					{
						type: 'category',
						boundaryGap: false,
						barCategoryGap: '1',
						data: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul']
					}
				],
				yAxis: [
					{
						type: 'value',
						axisLabel: {
							formatter: '{value}'
						}
					}
				],
				series: [
					{
						type: 'line',
						data: [11, 20, 100, 9, 3, 4, 5],
						markPoint: {
							data: [
								{type: 'max', name: ''},
								{type: 'min', name: ''}
							]
						},
						markLine: {
							data: [
								{type: 'average', name: '75th',itemStyle: {normal: {areaStyle: {type: 'default'}, color: 'red'}}},
								{type: 'max', name: 'avg',itemStyle: {normal: {areaStyle: {type: 'default'}, color: 'green'}}},
								{type: 'min', name: '75th',itemStyle: {normal: {areaStyle: {type: 'default'}, color: 'red'}}},
							]
						}
					}
				]
			};

			hlogsplittimeoption = {
				legend: {
					data: ['Assign_max', 'Assign_min']
				},
				calculable: true,
				xAxis: [
					{
						type: 'category',
						boundaryGap: false,
						barCategoryGap: '1',
						data: ['Mon', 'Tue', 'Web', 'Thu']
					}
				],
				yAxis: [
					{
						type: 'value',
						axisLabel: {
							formatter: '{value}S'
						}
					}
				],
				series: [
					{
						type: 'line',
						data: [11, 20, 6, 9],
						markPoint: {
							data: [
								{type: 'max', name: ''},
								{type: 'min', name: ''}
							]
						},
						markLine: {
							data: [
								{type: 'average', name: '75th',itemStyle: {normal: {areaStyle: {type: 'default'}, color: 'red'}}},
								{type: 'max', name: 'avg',itemStyle: {normal: {areaStyle: {type: 'default'}, color: 'green'}}},
								{type: 'min', name: '75th',itemStyle: {normal: {areaStyle: {type: 'default'}, color: 'red'}}},
							]
						}
					}
				]
			};

			var ritcount = echarts.init(document.getElementById('ritcount'));
			ritcount.setOption(option);
			var memusage = echarts.init(document.getElementById('memusage'));
			memusage.setOption(pieoption);
			var avgload = echarts.init(document.getElementById('avgload'));
			avgload.setOption(avgloadoption);
			var ritovercount = echarts.init(document.getElementById('ritovercount'));
			ritovercount.setOption(option);
			var assignnum = echarts.init(document.getElementById('assignnum'));
			assignnum.setOption(assignnumoption);
			var bulkassignnum = echarts.init(document.getElementById('bulkassignnum'));
			bulkassignnum.setOption(assignnumoption1);
			var balanceassignnum = echarts.init(document.getElementById('balanceassignnum'));
			balanceassignnum.setOption(assignnumoption);
			var hlogsplittime = echarts.init(document.getElementById('hlogsplittime'));
			hlogsplittime.setOption(hlogsplittimeoption);

		});
	});
})();
