$(function(){ 

	const CVS_HEIGHT = 1500;
	const CVS_WIDTH = 1500;
	const ZOOM_OUT = Math.pow(0.875,3).toFixed(3);

	//Canvas全局状态量
    let environment = $("#environment");
	environment.data("GrpZoomOut",1);
	environment.data("GrpLocate","950,700");
	environment.data("LineWidth",0.5);
	environment.data("LineColor","#000000");
	environment.data("PointSize",0);
	environment.data("PointColor","#000000");
	environment.data("GridZoom",1.0);
	environment.data("GridWidth",0.5);
	environment.data("GridColor","#ccc");
	environment.data("BkgdColor","#ffffff");   

	//let root = 'ws://219.216.65.14';
	let wsroot = 'ws://localhost';
	//let httproot = 'http://219.216.65.14';
	let httproot = 'http://localhost';
	let canvaslink = wsroot+':7001/';
	let stdoutlink = wsroot+':8001/';
	let monitorlink = wsroot+':10001/';



	const canvas = document.getElementById("canvas");
    //const width = canvas.offsetWidth;
    //const height = canvas.offsetWidth;
	canvas.width = CVS_WIDTH;
	canvas.height = CVS_HEIGHT;
	const ctx = canvas.getContext("2d", { alpha: false });

	////////////////// 柱状图
    const canvas_right_top = document.getElementById("canvas-right-top");
    const ctx_right_top = echarts.init(canvas_right_top);
    //const app = {};
	let option = null;

	//console.log(environment);
	ctx.strokeStyle = 'black'; /////////////////// xx
	ctx.lineWidth=environment.data("LineWidth"); /////////////////// xx

    let len = 0;
    let boo = false;
    let dataset = null;

	//===========================================================================================================
	$.ajax({
		type: "GET",
		//url: "Wiki-Vote.txt_of_780_without_800.json",
		url: "Vote.txt_of_800_without_800.json",
		//url: "simple5.txt_of_800_without_800.json",
		async: false,
		dataType: "json",
		success: function(data){
			boo = true;
			len = data.links.length;
			dataset = data;
		}
	});

	//var newset;
    let getInnerVariable;
    let timeOutEvent = 0;//区分拖拽和点击的参数

	function drawGrid(stepxy, canvasxy) {
		ctx.save();
		ctx.fillStyle = environment.data("BkgdColor"); /////////////////// xx;
	  
		ctx.fillRect(0, 0, canvasxy, canvasxy);
		ctx.lineWidth = environment.data("GridWidth"); /////////////////// xx
		ctx.strokeStyle = environment.data("GridColor"); /////////////////// xx;


		if(parseFloat(environment.data("GridWidth")) !== 0){
            //alert(environment.data("GridWidth"));
		    for (let i = stepxy; i < canvasxy; i += stepxy) {
		        ctx.beginPath();
		        ctx.moveTo(i, 0);
		        ctx.lineTo(i, canvasxy);
			    ctx.moveTo(0, i);
		        ctx.lineTo(canvasxy, i);
		        ctx.closePath();
		        ctx.stroke();
		    }
		}
		ctx.restore();
	}

	function trans(sets,addx,addy,r){
		//console.log(addx,addy,r);
	    let param = sets.links;
        let banana = sets.nodes;
		for(let i = 0; i < param.length; i++){	//优化（位运算要包在括号内）
			param[i].x1 = ((0.5 + param[i].x1*r) << 0) + addx; //Math.round(param[i].x1*r) + addx;//
			param[i].x2 = ((0.5 + param[i].x2*r) << 0) + addx; //Math.round(param[i].x2*r) + addx;//
			param[i].y1 = ((0.5 + param[i].y1*r) << 0) + addy; //Math.round(param[i].y1*r) + addy;//
			param[i].y2 = ((0.5 + param[i].y2*r) << 0) + addy; //Math.round(param[i].y2*r) + addy;//
			/*
			性能对比：移位：大，初次140ms  中，初次40ms   未分层时，移动：大30ms 中8ms
			     Mathround：大，初次170ms  中，初次50ms   未分层时，移动：大30ms 中8ms
			*/
		}
		for(let i = 0; i < banana.length; i++){
			banana[i].cx = ((0.5 + banana[i].cx*r) << 0) + addx;  //Math.round(banana[i].cx*r) + addx;//
			banana[i].cy = ((0.5 + banana[i].cy*r) << 0) + addy;  //Math.round(banana[i].cy*r) + addy;//
		}
		//console.log(banana)
		return ({nodes:banana,links:param});
	}


	function createImg(addx,addy,wholesets,r,isdrawleft){

        wholesets = trans(wholesets,addx,addy,r);
        let lset = wholesets.links;
        let nset = wholesets.nodes;
        const beginTime = +new Date();

        if(boo){
            console.log("createImg");
			//console.log("NOW:"+environment.data("LineWidth"));
			ctx.strokeStyle = environment.data("LineColor");; /////////////////// xx
			ctx.lineWidth = environment.data("LineWidth"); /////////////////// xx
			ctx.clearRect(0,0,canvas.width,canvas.height);

			let GridZoom = environment.data("GridZoom"); /////////////////// xx
			//let CanvasZoom = environment.data("Grp"); /////////////////// xx
			//let widthandheight = 1500*CanvasZoom;
			//let xyValue = CVS_WIDTH;
			drawGrid(20*GridZoom, CVS_WIDTH);

			if(parseFloat(environment.data("LineWidth")) !== 0 ){
				ctx.beginPath(); //优化

				for(let i = 0; i < lset.length; i++){
                    let tmp = lset[i];
					ctx.moveTo(tmp.x1 , tmp.y1);
					ctx.lineTo(tmp.x2 , tmp.y2);
				}
				ctx.closePath(); //优化
				ctx.stroke(); //优化
			}
			// 一次画一笔，【缺点】线段会出现失真
			// var i = 0;
			// const sh = setInterval((function(){
			  // return function(){

				// var tmp = sset[i];

				// ctx.moveTo(Math.round(tmp.x1 ), Math.round(tmp.y1));
				// ctx.lineTo(Math.round(tmp.x2 ), Math.round(tmp.y2));
				// ctx.stroke();
				// console.log("draw:"+i);
				// if(++i == len){
					// clearInterval(sh); // 画完
					// console.log("over");
				// }
			  // }
			// })(i),10);

			//console.log(nset);
			//console.log(lset);
			//画点
			//ctx.strokeStyle = environment.data("PointColor"); /////////////////// xx
			let rsize = environment.data("PointSize"); /////////////////// xx
			let pcolor = environment.data("PointColor"); /////////////////// xx
			if(parseFloat(environment.data("PointSize")) !== 0 ){
				//console.log("R = "+rsize+", CL = "+pcolor);
				if(rsize){

					for(let i = 0; i < nset.length; i++){
                        let tmp = nset[i];
						ctx.beginPath(); // 开启绘制路径
						ctx.arc(tmp.cx, tmp.cy, rsize, 0, 2*Math.PI); // 绘制圆 参数依次为 圆的横坐标/纵坐标/半径/绘制圆的起始位置/绘制圆的弧度大小
						ctx.fillStyle = pcolor; /////////////////// xx
						ctx.closePath(); // 关闭绘制路径
						ctx.fill(); // 填充颜色
					}
				}
			}
			const midTime = +new Date();
			$("#timer").text("Canvas render time: "+(midTime-beginTime)+" ms");

			if(isdrawleft){
				drawLeft();
				const endTime = +new Date();
				$("#timerChart").text("Chart render time: "+(endTime-midTime)+" ms");
			}
		}
		return ({nodes:nset,links:lset});
	}


	function longPress(){
		timeOutEvent = 0;
	}


	canvas.onmousedown = function(ev){
		let e = ev||event;
        let x = e.clientX;
        let y = e.clientY;

		timeOutEvent = setTimeout("longPress()",500); //防误判
		e.preventDefault();

		drag(x,y,0,0);

	}


	// 缩放的函数是像素缩放，细节丢失！！！不能用
	// ctx.scale(20,20);
	// ctx.translate(100,00);


	// 失败的方法
	// document.body.addEventListener('touchmove', function(e){
		// e.preventDefault();
	// }, { passive: false });  //passive 参数不能省略，用来兼容ios和android

	// 旧版本的方法，chrome73内已失效，为了兼容electron 谷保存下面
	//let p_Cvs = $("#canvas");
	//p_Cvs.mouseover(function(){
	//    $(document).bind('mousewheel', function(event, delta, ) {return false;});
	//});
    //p_Cvs.mouseout(function(){
	//    $(document).unbind('mousewheel');
	//});

	//// 焦点进入时禁止全局滚动
    //p_Cvs.mouseover(function(){
	//	 $(document.body).css({
	//	   "overflow-x":"hidden",
	//	   "overflow-y":"hidden"
	//	 });
	//});
    //p_Cvs.mouseout(function(){
	//	 $(document.body).css({
	//	   "overflow-x":"auto",
	//	   "overflow-y":"auto"
	//	 });
	//});
    //缩放  老版本的缩放和 滚动屏蔽
    //let old = 1;
    //canvas.onmousewheel = function(ev){
    //    let e = ev||event;
    //    //console.log(e.wheelDelta/1200);
    //    let r = 1 + e.wheelDelta/1200;
    //    old  = old * r;
    //    //console.log(r);
    //    if(r>0){
    //        $("#cp5").val(r);
    //        //const d = ( environment.data("GrpZoomOut") *r ).toFixed(5);
    //        environment.data("GrpZoomOut",old);
    //        dataset = createImg(0,0,dataset,r,false);
    //        //old = r;
    //        // function inner() {
    //        // return newset;
    //        // }
    //        // getInnerVariable = inner;
    //    }
    //}
    let old = 1;
	// 兼容更好且 无滚动条宽度变化的方案（非隐藏滚动条，是直接屏蔽事件）
    let firefox = navigator.userAgent.indexOf('Firefox') !== -1;
    function MouseWheel(e) {console.log("******");
        ///对img按下鼠标滚路，阻止视窗滚动
        e = e || window.event;
        if (e.stopPropagation) {console.log('stopPropagation');
            let r = 1 + e.wheelDelta/1200;
            old  = old * r;
            if(r>0){
                $("#cp5").val(r);
                //const d = ( environment.data("GrpZoomOut") *r ).toFixed(5);
                environment.data("GrpZoomOut",old);
                dataset = createImg(0,0,dataset,r,false);

            }
        	e.stopPropagation();
        }
        else e.cancelBubble = true;
        if (e.preventDefault){console.log('preventDefault');
            //console.log(e.wheelDelta/1200);
            let r = 1 + e.wheelDelta/1200;
            old  = old * r;
            if(r>0){
                $("#cp5").val(r);
                //const d = ( environment.data("GrpZoomOut") *r ).toFixed(5);
                environment.data("GrpZoomOut",old);
                dataset = createImg(0,0,dataset,r,false);  ////////// 问题：缩小会失真，使徒行节点边少，放大不会； 柱状图目前未关联

            }
        	e.preventDefault();
        }
        else e.returnValue = false;
    }
    window.onload = function () {
        let ccvs = document.getElementById('canvas');
        firefox ? ccvs.addEventListener('DOMMouseScroll', MouseWheel, false) : (ccvs.onmousewheel = MouseWheel);
    }



	// var preventScroll = function(dom){
        // if(dom.jquery){
            // dom = dom.get(0);
        // }
        // if(navigator.userAgent.indexOf('Firefox') >= 0){   //firefox
            // dom.addEventListener('DOMMouseScroll',function(e){
                // dom.scrollTop += e.detail > 0 ? 60 : -60;
                // e.preventDefault();
            // },false);
        // }else{
            // dom.onmousewheel = function(e){
                // e = e || window.event;
                // dom.scrollTop += e.wheelDelta > 0 ? -60 : 60;
                // return false;
            // };
        // }
    // };


	//拖拽

	function drag(oldx,oldy,newx,newy){

        let addx,addy;
		//路径正确，鼠标移动事件
		canvas.onmousemove = function(ev){
            let e = ev||event;
			newx = e.clientX;
			newy = e.clientY;
			clearTimeout(timeOutEvent);
			timeOutEvent = 0;
			//鼠标移动每一帧都清楚画布内容，然后重新画
			addx = newx-oldx;
			addy = newy-oldy;

		};
		//鼠标移开事件
		canvas.onmouseup = function(){
			canvas.onmousemove = null;
			canvas.onmouseup = null;
			clearTimeout(timeOutEvent);
			if(timeOutEvent !== 0){
				alert("你这是点击，不是拖拽");
			}else{

				// newset = createImg(addx,addy,getInnerVariable(),1);
				let oldarr = environment.data("GrpLocate").split(",");
				let axx = (parseInt(oldarr[0])+addx);
				let ayy = (parseInt(oldarr[1])+addy);
				console.log(axx+","+ayy);
				environment.data("GrpLocate",axx+","+ayy);
				dataset = createImg(addx,addy,dataset,1,false);
				//drawLeft();

			}

		}

	}

	////////////////// 柱状图
	function drawLeft(){


        let data = generateData();

		drawLeftMid(data.valueData);

        let option = {
			title: {
				// text: echarts.format.addCommas(dataCount) + ' Data',
				// textStyle: {
					// color: '#333',
					// left:0,
					// fontSize: 8
				// }
				show: false
			},
			// toolbox: {
				// feature: {
					// dataZoom: {
						// yAxisIndex: false
					// },
					// saveAsImage: {
						// pixelRatio: 2
					// }
				// }
			// },
			tooltip: {
				trigger: 'axis',
				axisPointer: {
					type: 'shadow'
				}
			},
			grid: {
				left: 32,
				top: 45,   /////调容器内间距
				bottom: 20
			},
			dataZoom: [{
					type: 'inside'
				},{
					type: 'slider',
					left:29,
					top: 0,
					dataBackground: {
						areaStyle: {
							color: 'black'
						}
					},
					textStyle: {
						color: '#000',
						fontSize:'6'
					}

				}
			],
			xAxis: {
				data: data.categoryData,
				silent: false,
				splitLine: {
					show: false
				},
				axisLabel: {
					show: true,
					textStyle: {
						color: '#000',
						fontSize:'6'
					}
				},
				splitArea: {
					show: false
				}
			},
			yAxis: {
				axisLabel: {
					show: true,
					textStyle: {
						color: '#000',
						fontSize:'6'
					}
				},
				splitArea: {
					show: false
				}
			},
			series: [{
				type: 'bar',
				data: data.valueData,
				itemStyle:{
					normal:{
						color:'#777'
					}
				},
				tooltip: {
					textStyle: {
						color: '#fff',
						fontSize: 6
					}
				},
				// Set `large` for large data amount
				large: true
			}]
		};

		// 柱状图 得当前画布显示的数据 【计算全部节点的度】
		function generateData() {

            let categoryData = [];
            let valueData = [];

            let map = {};

			//console.log(newset);

			for(let i = 0; i < dataset.links.length; i++){

                let tmp = dataset.links[i];
                let p1 = tmp.x1 + "+" + tmp.y1;
                let p2 = tmp.x2 + "+" + tmp.y2;

				if(typeof(map[p1]) === 'undefined')
					map[p1] = 1;
				else
					map[p1] += 1;
				if(typeof(map[p2]) === 'undefined')
					map[p2] = 1;
				else
					map[p2] += 1;

			}
			for(let key in map){
			   categoryData.push(key);
			   valueData.push(map[key]);
			}
			return {
				categoryData: categoryData,
				valueData: valueData
			}
			//console.log(map);
		}
		if (option && typeof option === "object") {

			ctx_right_top.clear();
			ctx_right_top.setOption(option, true); //////// 注入配置项      解决重复绑定事件：clear(), true, off(click)
			ctx_right_top.off('click');
			ctx_right_top.on('click', function (params) {

				//console.log(option.xAxis.data[params.dataIndex],params.value);
                let arr = option.xAxis.data[params.dataIndex].split("+");
				ctx.beginPath(); // 开启绘制路径
				ctx.arc(arr[0], arr[1], 10, 0, 2*Math.PI); // 绘制圆 参数依次为 圆的横坐标/纵坐标/半径/绘制圆的起始位置/绘制圆的弧度大小
				ctx.fillStyle = "red"; // 设置填充颜色
				ctx.fill(); // 填充颜色
				ctx.closePath(); // 关闭绘制路径
			});
		}

	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////// 度分布

    let canvas_right_mid = document.getElementById("canvas-right-mid");
    let ctx_right_mid = echarts.init(canvas_right_mid);
	option = null;



	function drawLeftMid(all_degree){

        let degreeData = [];
        let amountData = [];
        const max_d = Math.max.apply(null, all_degree); //返回最大度
		//console.log(max_d);
        let arr_d = new Array(max_d);

		// 生成度分布
		for(let key in all_degree){
			let index = all_degree[key];
		    if(typeof(arr_d[index]) === "undefined"){
			   arr_d[index] = 1;
		    }else{
			   arr_d[index] += 1;
		    }
		}

		$.each(arr_d,function(de,amount){
		    if(typeof(arr_d[de]) !== "undefined"){
				degreeData.push(de);
				amountData.push(amount);

		    }
		});

		drawLeftBottom({x:degreeData, y:amountData});

		//console.log(arr_d);
        let option = {
			title: {
				show: false
			},

			tooltip: {
				trigger: 'axis',
				axisPointer: {
					type: 'shadow'
				}
			},
			grid: {
				left: 32,
				top:10,
				bottom: 20
			},
			dataZoom: [{
					type: 'inside'
				}
			],
			xAxis: {
				data: degreeData,
				silent: false,
				splitLine: {
					show: false
				},
				axisLabel: {
					show: true,
					textStyle: {
						color: '#000',
						fontSize:'6'
					}
				},
				splitArea: {
					show: false
				}
			},
			yAxis: {
				axisLabel: {
					show: true,
					textStyle: {
						color: '#000',
						fontSize:'6'
					}
				},
				splitArea: {
					show: false
				}
			},
			series: [{
				type: 'bar',
				data: amountData,
				itemStyle:{
					normal:{
						color:'#777'
					}
				},
				tooltip: {
					textStyle: {
						color: '#fff',
						fontSize: 6
					}
				},
				// Set `large` for large data amount
				large: false
			}]
		}
		if (option && typeof option === "object") {
			ctx_right_mid.clear();
			ctx_right_mid.setOption(option, true); //////// 注入配置项      解决重复绑定事件：clear(), true, off(click)
		}

	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////// 对数坐标

    let canvas_right_bottom = document.getElementById("canvas-right-bottom");
    let ctx_right_bottom = echarts.init(canvas_right_bottom);
	option = null;

	function drawLeftBottom(obj){

        let logx = [];
        let logy = [];
		$.each(obj.x, function(i, item){
			logx.push(Math.log(item));
		});
		$.each(obj.y, function(i, item){
			logy.push(Math.log(item));
		});

        let option = {
			title: {
				// text: echarts.format.addCommas(dataCount) + ' Data',
				// textStyle: {
					// color: '#333',
					// left:0,
					// fontSize: 8
				// }
				show: false
			},
 
			tooltip: {
				trigger: 'axis',
				axisPointer: {
					type: 'shadow'
				}
			},
			grid: {
				left: 32,
				top:30,
				bottom: 20
			},
			dataZoom: [{
					type: 'inside'
				}
			],
			xAxis: {
				data: logx,
				silent: false,
				splitLine: {
					show: false
				},
				axisLabel: {        
					show: true,
					textStyle: {
						color: '#000',
						fontSize:'6'
					}
				},
				splitArea: {
					show: false
				}
			},
			yAxis: {
				axisLabel: {        
					show: true,
					textStyle: {
						color: '#000',
						fontSize:'6'
					}
				},
				splitArea: {
					show: false
				}
			},
			series: [{
				type: 'scatter',
				data: logy,
				itemStyle:{
					normal:{
						color:'#777'
					}
				},
				symbolSize: 3,
				tooltip: {
					textStyle: {
						color: '#fff',
						fontSize: 6
					}
				},
				// Set `large` for large data amount
				large: false
			}]
		}
		if (option && typeof option === "object") {
			ctx_right_bottom.clear();
			ctx_right_bottom.setOption(option, true); //////// 注入配置项      解决重复绑定事件：clear(), true, off(click)
		}
		 
	} 

	///////////////////// init
	let oldarr = environment.data("GrpLocate").split(",");
	//newset = trans(dataset,400,400,1);
	//console.log(oldarr);
	dataset = createImg(parseInt(oldarr[0]),parseInt(oldarr[1]),dataset,ZOOM_OUT*environment.data("GrpZoomOut"),true); //初始
	//drawLeft();
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	$('select').selectpicker();
 
	/*
		var xhr = new XMLHttpRequest();
		xhr.open("GET", "http://219.216.65.14:4040/api/v1/applications",true);
		xhr.send();

	*/

	//try{/////// 语法错误无法被捕获，只能捕获 异常，异常 不等于 错误
	//int a = 1;

    let isSolved = false;
	//var oTimer = null;
	//var updateInfo = function() {
	//
	//	$.ajax({
	//		url: "http://219.216.65.14:4040/api/v1/applications",
	//		dataType: "json"
	//	}).done(function (result) {
	//		//$('#panel-heading').nextAll().remove();
	//		$('#panel-title').text("").text("任务描述（运行中）");
	//		var infos = result[0];
	//		var details = infos.attempts[0];
	//		//console.log(infos);
	//		var times = details.startTime.split(".");
	//		var timess = times[0].split("T");
	//		//let str = '<div class="panel-body" style="padding-top: 5px;padding-left: 5px;padding-right: 5px;padding-bottom: 5px;">';
	//		$("#tb1").html('AppId:&nbsp;<span class="label label-online">'+infos.id+'</span>');
	//		$("#tb2").html('AppName:&nbsp;<span class="label label-online">'+infos.name+'</span>');
	//		$("#tb3").html('StartTime:&nbsp;<span class="label label-online">'+timess[0]+'&nbsp;'+timess[1]+'</span>');
	//		$("#tb4").html('Completed:&nbsp;<span class="label label-online">Running</span>');
	//
	//			$.ajax({
	//				url: "http://219.216.65.14:4040/api/v1/applications/"+infos.id+"/allexecutors",
	//				dataType: "json"
	//			}).done(function (executor) {
	//				//console.log(executor);
	//				$('#thead').nextAll().remove();
	//				var counter = 0;
	//				var appendstr = "";
	//				$.each(executor, function(i, field){
	//					//if(i % 2 == 0){
	//						let str = '';
	//						counter++;
	//						if(field.isActive){
	//							str = "<span class=\"glyphicon glyphicon-ok-circle\" style=\"color:green\"></span>";
	//						}else{
	//							str = "<span class=\"glyphicon glyphicon-remove-circle\" style=\"color:red\"></span>";
	//						}
	//						//console.log(field.memoryUsed*100/1073741824);
	//						appendstr += '<tr style="height:0px;border:none !important;padding:0 0 0 0 !important;border:0px !important;line-height: 1.37 !important;"><td>'
	//								     +field.hostPort.split(":")[0]+'</td><td>'+str+'</td><td>'+field.rddBlocks
	//								     +'</td><td><div class="progress" style="margin-bottom:0px;border-radius: 0px !important;height: 17px !important;"><div class="progress-bar progress-success"style="width:'
	//								     +(field.memoryUsed*8000/field.maxMemory)+'%;"></div></div></td></tr>'
	//						$("#tb9").html('WorkerMaxmem:&nbsp;<span class="label label-online">'+(field.maxMemory/1024/1024).toFixed(1)+'MB</span>');
	//					//}else{
	//
	//						//$("#node_info").append('<tr class=\'success\'><td>'+field.hostPort.split(":")[0]+'</td><td>'+field.isActive+'</td><td>'+field.rddBlocks+'</td><td>'+field.memoryUsed+'</td></tr>');
	//					//}
	//				});
	//				counter
	//				if(counter==executor.length) $("#node_info").append(appendstr);
	//
	//			}).fail(function (jqXHR, textStatus, errorThrown) {
	//				// net::ERR_CONNECTION_REFUSED 发生时，也能进入
	//				console.info("轮询结束");
	//			});
	//
	//			$.ajax({
	//				url: "http://219.216.65.14:4040/api/v1/applications/"+infos.id+"/environment",
	//				dataType: "json"
	//			}).done(function (environ) {
	//				//console.log(environ.sparkProperties);
	//				var tmp = environ.sparkProperties;
	//				//let str = '<div class="panel-body" style="padding-top: 5px;padding-left: 5px;padding-right: 5px;padding-bottom: 5px;">';
	//				$("#tb5").html('DriverMemory:&nbsp;<span class="label label-online">'+tmp[5][1]+'</span>');
	//				$("#tb6").html('MasterNode:&nbsp;<span class="label label-online">'+tmp[9][1]+'</span>');
	//				$("#tb7").html('DeployMode:&nbsp;<span class="label label-online">'+tmp[13][1]+'</span>');
	//				$("#tb8").html('ExecuteJAr:&nbsp;<span class="label label-online">'+tmp[8][1].split("/bin/")[1]+'</span>');
	//
	//				$("#tb10").html('DriverHost:&nbsp;<span class="label label-online">'+tmp[4][1]+'</span>');
	//			}).fail(function (jqXHR, textStatus, errorThrown) {
	//				// net::ERR_CONNECTION_REFUSED 发生时，也能进入
	//				console.info("轮询结束");
	//			});
	//
	//	}).fail(function (jqXHR, textStatus, errorThrown) {
	//		// net::ERR_CONNECTION_REFUSED 发生时，也能进入
	//		console.info("轮询结束");
	//
	//		if(isSolved){
	//			$("#sendBtn").removeClass("disabled");
	//			$("#sendBtn").text("Go!");
	//		}
	//
	//		window.clearInterval(oTimer);
	//
	//
	//		$("#tb1").html('AppId:&nbsp;<span class=\"label label-default\">app-20190327220504-0015</span>');
	//		$("#tb2").html('AppName:&nbsp;<span class=\"label label-default\">RemoteGraphX</span>');
	//		$("#tb3").html('StartTime:&nbsp;<span class=\"label label-default\">2019-03-27 14:05:03</span>');
	//		$("#tb4").html('Completed:&nbsp;<span class=\"label label-default\">True</span>');
	//		$("#tb5").html('DriverMemory:&nbsp;<span class=\"label label-default\">1g</span>');
	//		$("#tb6").html('MasterNode:&nbsp;<span class=\"label label-default\">spark://hadoop02:7077</span>');
	//		$("#tb7").html('DeployMode:&nbsp;<span class=\"label label-default\">client</span>');
	//		$("#tb8").html('ExecuteJAr:&nbsp;<span class=\"label label-default\">visualization-build.jar</span>');
	//		$("#tb9").html('WorkerMaxmem:&nbsp;<span class=\"label label-default\">366.3MB</span>');
	//		$("#tb10").html('DriverHost:&nbsp;<span class=\"label label-default\">192.168.0.2</span>');
	//
	//		var off2 = '<tr style="height:0px;border:none !important;"><td>192.168.0.7 </td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.8 </td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.19</td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.20</td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.24</td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.29</td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.32</td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.33</td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.34</td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.35</td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.42</td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.43</td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.44</td><td>-</td><td>-</td><td>-</td></tr>'
	//				  +'<tr style="height:0px;border:none !important;"><td>192.168.0.46</td><td>-</td><td>-</td><td>-</td></tr>';
	//
	//		$('#panel-title').text("").text("任务描述（离线）");
	//		$('#thead').nextAll().remove();
	//		$("#node_info").append(off2);
	//	});
	//
	//}
	//
	//
	//updateInfo();
	//

	let sendBtn = $("#sendBtn");
	//建立连接
	let stdoutSocket = new WebSocket(stdoutlink);
	//开启连接
	stdoutSocket.onopen = function () {
		console.log('stdoutSocket open');
		isSolved = true;
        sendBtn.removeClass("disabled").addClass("btn-mysuccess");
	};
	//关闭连接
	stdoutSocket.onclose = function () {
		console.log('stdoutSocket close');
		//console.log(isSolved);
        sendBtn.removeClass("btn-mysuccess").addClass("disabled");
	};
	let constr = '';
	//拿到返回
	stdoutSocket.onmessage = function (e) {
		let pdiv=document.getElementById("proccesdiv");
		pdiv.style.visibility='visible';
		if(e.data === '@done'){
			constr += e.data;
			// $('#proccestitle').text("Finished").css("color","#3a87ad");
			//$('#proccesdiv').removeClass("active");
			pdiv.style.visibility='hidden';
			if(isSolved){
                sendBtn.removeClass("disabled");
                sendBtn.text("Go!");
			}else{
                sendBtn.text("Offline");
			}
			$('#panel-title').text("").text("任务描述（离线）");
			//window.clearInterval(oTimer);
			$("#tb1").html('AppId:&nbsp;<span class=\"label label-default\">app-20190327220504-0015</span>');
			$("#tb2").html('AppName:&nbsp;<span class=\"label label-default\">RemoteGraphX</span>');
			$("#tb3").html('StartTime:&nbsp;<span class=\"label label-default\">2019-03-27 14:05:03</span>');
			$("#tb4").html('Completed:&nbsp;<span class=\"label label-default\">True</span>');
			$("#tb5").html('DriverMemory:&nbsp;<span class=\"label label-default\">1g</span>');
			$("#tb6").html('MasterNode:&nbsp;<span class=\"label label-default\">spark://hadoop02:7077</span>');
			$("#tb7").html('DeployMode:&nbsp;<span class=\"label label-default\">client</span>');
			$("#tb8").html('ExecuteJAr:&nbsp;<span class=\"label label-default\">visualization-build.jar</span>');
			$("#tb9").html('WorkerMaxmem:&nbsp;<span class=\"label label-default\">366.3MB</span>');
			$("#tb10").html('DriverHost:&nbsp;<span class=\"label label-default\">192.168.0.2</span>');

			let off2 = '<tr style="height:0px;border:none !important;"><td>192.168.0.7 </td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.8 </td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.19</td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.20</td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.24</td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.29</td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.32</td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.33</td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.34</td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.35</td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.42</td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.43</td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.44</td><td>-</td><td>-</td><td>-</td></tr>'
					  +'<tr style="height:0px;border:none !important;"><td>192.168.0.46</td><td>-</td><td>-</td><td>-</td></tr>';
			$('#thead').nextAll().remove();
			$("#node_info").append(off2);

			console.log("本次执行结束");
		}else if((e.data).indexOf("\\n") >= 0||(e.data).indexOf("\\r") >= 0){
			constr += e.data;
		}else if(e.data.length <= 1){
		}else{
			//进度条
			if((e.data).indexOf("( ") >= 0){

				let regex1 = /\((.+?)\)/g;  // () 小括号
				let proces = (e.data).match(regex1)[0].replace("( ","").replace(" )","");
				let bfs = proces*100/$('#pzxD').val();
				console.log(bfs);
				$('#myproccess').css("width",bfs+"%");
			}
			constr += e.data+'<br>';
		}
		$('#stdout').html(constr);

	};
	/*
	//发送信息
	document.getElementById('sendBtn').onclick = function () {
		$("#sendBtn").addClass("disabled");
		$("#sendBtn").text("Running");
		//$('#proccesdiv').addClass("active progress-striped");
		//oTimer = setInterval(updateInfo,2000);
		//var pzxC = $('#pzxC').val();
		var select3 = $('#select3').val();
		var pzxD = $('#pzxD').val();
		//console.log(">>>>>>>"+select3);
		//var text = $('#sendTxt').val();

		webSocket.send(select3+','+pzxD);
	};

	function executeAnimeDrawing(index){
		if(index < 200){

			setTimeout(function(){
				$.ajax({
					type: "POST",
					//url: "Wiki-Vote.txt_of_780_without_800.json",
					data:{"filename":"simple5.txt_of_"+index+"_without_200.json"},
					url: "http://219.216.65.14:9999/fetch_layout_rst",
					//url: "simple5.txt_of_800_without_800.json",
					contentType: "application/json",//传入参数格式
					async: false,
					dataType: "json", //返回参数格式
					success: function(jsonstr){
						//var data = $.parseJSON( jsonstr );
						let oldarr = environment.data("GrpLocate").split(",");
						dataset = createImg(parseInt(oldarr[0]),parseInt(oldarr[1]),jsonstr,ZOOM_OUT*environment.data("GrpZoomOut"), false); //初始
					}
				});

			},500+50*(index-1));
			//if(index==799)index = 760;
			executeAnimeDrawing(++index);
			//setTimeout(function(){
			//  executeAnimeDrawing(index);
			//},500);
		}
	}
	*/
	//////////////////////////////////////////////////////////////////////////////////////////
	//建立连接
	let canvasSocket = new WebSocket(canvaslink);
	//开启连接
	canvasSocket.onopen = function () {
		console.log('canvasSocket open');
	};
	//关闭连接
	canvasSocket.onclose = function () {
		console.log('canvasSocket close');
		//console.log(isSolved);
	};
	canvasSocket.onmessage = function (e) {
		//console.log($.parseJSON(e.data));
		let currentLoc = environment.data("GrpLocate").split(",");
		dataset = createImg(
			parseInt(currentLoc[0]),
			parseInt(currentLoc[1]),
			$.parseJSON(e.data),
			ZOOM_OUT*environment.data("GrpZoomOut"),
			false
		);
	};
	////发送信息
	document.getElementById('testttt').onclick = function () {
		canvasSocket.send("facebook_combined.txt_of_@_without_300.json,1,150");
		//canvasSocket.send("Vote.txt_of_@_without_800.json,760,800");
	};
	////发送信息
	document.getElementById('testttt2').onclick = function () {
		canvasSocket.send("demo2\\Vote.txt_of_@_without_500.json,1,500");
		//canvasSocket.send("Vote.txt_of_@_without_800.json,760,800");
	};

	//////////////////////////////////////////////////////////////////////////////////////////
	//建立连接
	let monitorSocket = new WebSocket(monitorlink);
	//开启连接
	monitorSocket.onopen = function () {
		console.log('monitorSocket open');
	};
	monitorSocket.onmessage = function (e) {
		//console.log($.parseJSON(e.data));

		let obj = $.parseJSON(e.data);
		$("#tb1").html('AppId:&nbsp;<span class="label label-online">'+obj.AppId+'</span>');
		$("#tb2").html('AppName:&nbsp;<span class="label label-online">'+obj.AppName+'</span>');
		$("#tb3").html('StartTime:&nbsp;<span class="label label-online">'+obj.StartTime+'</span>');
		$("#tb4").html('Completed:&nbsp;<span class="label label-online">Running</span>');
		$("#tb5").html('DriverMemory:&nbsp;<span class="label label-online">'+obj.DriverMemory+'</span>');
		$("#tb6").html('MasterNode:&nbsp;<span class="label label-online">'+obj.MasterNode+'</span>');
		$("#tb7").html('DeployMode:&nbsp;<span class="label label-online">'+obj.DeployMode+'</span>');
		$("#tb8").html('ExecuteJAr:&nbsp;<span class="label label-online">'+obj.ExecuteJAr+'</span>');
		$("#tb9").html('WorkerMaxmem:&nbsp;<span class="label label-online">'+obj.WorkerMaxmem+'</span>');
		$("#tb10").html('DriverHost:&nbsp;<span class=\"label label-online\">'+obj.DriverHost+'</span>');

		let tbstr = '';
		//console.log(obj);
		$.each(obj.executors, function(i, field){

			let str = '';
			if(field.isActive){
				str = "<span class=\"glyphicon glyphicon-ok-circle\" style=\"color:green\"></span>";
			}else{
				str = "<span class=\"glyphicon glyphicon-remove-circle\" style=\"color:red\"></span>";
			}
			//console.log(field.memoryUsed*100/1073741824);
			tbstr+= '<tr style="height:0px;border:none !important;padding:0 0 0 0 !important;border:0px !important;line-height: 1.37 !important;"><td>'
				 +field.hostPort+'</td><td>'+str+'</td><td>'+field.rddBlocks
				 +'</td><td><div class="progress" style="margin-bottom:0px;border-radius: 0px !important;height: 17px !important;"><div class="progress-bar progress-success"style="width:'
				 +field.memoryUsed+';"></div></div></td></tr>'
		});
		$('#thead').nextAll().remove();
		$("#node_info").append(tbstr);
	};
	//关闭连接
	monitorSocket.onclose = function () {
		console.log('monitorSocket close');
		console.info("轮询结束");

	};

	document.getElementById('sendBtn').onclick = function () {
		$("#sendBtn").addClass("disabled");
		$("#sendBtn").text("Running");
		let select3 = $('#select3').val();
		let pzxD = $('#pzxD').val();

		stdoutSocket.send(select3+','+pzxD);
		monitorSocket.send("111");

		//canvasSocket.send("simple5.txt_of_@_without_200.json,1,196");
		//canvasSocket.send("Vote.txt_of_@_without_800.json,760,800");
	};



	/////////////////////////////////////////////////////////////////////////////////
	$('#select3').on('changed.bs.select', function (e) {
		//console.log(select3);
		//if(select3 === "Simple5")
		//	select3 = "simple5.txt_of_476_without_800.json";
		//	//select3 = "data200/simple5.txt_of_1_without_200.json";
		//if(select3 === "Vote")
		//	select3 = "Vote.txt_of_800_without_800.json";
		//	//select3 = "data200/simple5.txt_of_100_without_200.json";
		//if(select3 === "Wiki-Vote")
		//	select3 = "Wiki-Vote.txt_of_780_without_800.json";
			 
		$.ajax({
			type: "POST",
			//url: "Wiki-Vote.txt_of_780_without_800.json",
			url: httproot + ":3000/getjson",
			//url: "simple5.txt_of_800_without_800.json",
			data: $('#select3').val(),
			async: false,
			dataType: "json",
			success: function(data){
				boo = true;
				len = data.links.length;
				dataset = data;


			}
		});
        alert(len);
        console.log(dataset);
        dataset = createImg(950,700,dataset,ZOOM_OUT*environment.data("GrpZoomOut"), true); //初始
		//let oldarr = environment.data("GrpLocate").split(",");
		//console.log("select...");

		//newset = trans(dataset,400,400,1);

		//drawLeft();
	});

	///////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////

	//console.log(environment.data("LineWidth"));

	//$("#cp2").css("background-color","#3a87ad");
	let cp1 = $('#cp1'), cp2 = $('#cp2'), cp3 = $('#cp3'), cp4 = $('#cp4');

    cp1.colorpicker({format:'hex'});
	(function ($) {
		$.fn.watch = function (callback) {
			return this.each(function () {
				//缓存以前的值  
				$.data(this, 'originVal', $(this).val());
				//event  
				$(this).on('keyup paste', function () {
					let originVal = $.data(this, 'originVal');
                    let currentVal = $(this).val();

					if (originVal !== currentVal) {
						$.data(this, 'originVal', $(this).val());
						callback(currentVal);
						
					}
				});
			});
		}
	})(jQuery);
    cp1.watch(function(value) {
        cp1.css("color",value);
	 
	});

    cp2.colorpicker({format:'hex'});
	(function ($) {
		$.fn.watch = function (callback) {
			return this.each(function () {
				//缓存以前的值  
				$.data(this, 'originVal', $(this).val());
				//event  
				$(this).on('keyup paste', function () {
                    let originVal = $.data(this, 'originVal');
                    let currentVal = $(this).val();

					if (originVal !== currentVal) {
						$.data(this, 'originVal', $(this).val());
						callback(currentVal);
					}
				});
			});
		}
	})(jQuery);
    cp2.watch(function(value) {
		//console.log(value);
        cp2.css("color",value);
	});

    cp3.colorpicker({format:'hex'});
	(function ($) {
		$.fn.watch = function (callback) {
			return this.each(function () {
				//缓存以前的值  
				$.data(this, 'originVal', $(this).val());
				//event  
				$(this).on('keyup paste', function () {
					let originVal = $.data(this, 'originVal');
					let currentVal = $(this).val();

					if (originVal !== currentVal) {
						$.data(this, 'originVal', $(this).val());
						callback(currentVal);
					}
				});
			});
		}
	})(jQuery);
    cp3.watch(function(value) {
		//console.log(value);
        cp3.css("color",value);
	});

    cp4.colorpicker({format:'hex'});
    cp4.bind('input propertychange',function() {
		//console.log(cp4.val());
        cp4.css("color",cp4.val());
	}); 
	
	function update_CONST_CANVAS(id,latest){
		let isRun = false;
		let older;
		if(id === "#cp1"){
		 
			older = environment.data("LineColor");
			if(latest !== older){
				isRun = true;
				environment.data("LineColor",latest);   
			}
		}else if(id === "#cp3"){
			older = environment.data("PointColor");
			if(latest !== older){
				isRun = true;
				environment.data("PointColor",latest);   
			}
		}else if(id === "#cp2"){
			older = environment.data("GridColor");
			if(latest !== older){
				isRun = true;
				environment.data("GridColor",latest);   
			}
		}else if(id === "#cp4"){
			older = environment.data("BkgdColor");
			if(latest !== older){
				isRun = true;
				environment.data("BkgdColor",latest);   
			}
		}else{
			 
			latest = parseFloat(latest).toFixed(1);
			switch(id){
				//case "#cp5": 
				//	older = environment.data("GrpZoomOut");
				//	if(latest!=older){
				//		isRun = true;
				//		environment.data("GrpZoomOut",latest);   
				//	}
				//	break;
				case "#cp6": 
					older = environment.data("LineWidth");
					if(latest !== older){
						isRun = true;
						environment.data("LineWidth",latest);   
					}
					break;
				case "#cp7": 
					older = environment.data("PointSize");
					if(latest !== older){
						isRun = true;
						environment.data("PointSize",latest);   
					}
					break;
				case "#cp8": 
					older = environment.data("GridZoom");
					if(latest !== older){
						isRun = true;
						environment.data("GridZoom",latest);   
					}
					break;
				case "#cp9": 
					older = environment.data("GridWidth");
					if(latest !== older){
						isRun = true;
						environment.data("GridWidth",latest);   
					}
					break;
				case "#cp1": 
					older = environment.data("LineColor");
					if(latest !== older){
						isRun = true;
						environment.data("LineColor",latest);   
					}
					break;
				case "#cp3": 
					older = environment.data("PointColor");
					if(latest !== older){
						isRun = true;
						environment.data("PointColor",latest);   
					}
					break;
			}
			
			$(id).val(latest);
		}
		if(isRun){
			//updateCanvas here
			 
			dataset = createImg(0,0,dataset,1,false);  //初始
			//console.log(newset);
			console.log(older+", NOW:"+latest);
			console.log("need update");
			 
		}else{
			console.log(older+", NOW:"+latest);
		}
	}

//add
	
	$("#add5,#add6,#add7,#add8,#add9").click(function(){
		const inpt = "#cp"+(this.id).replace("add","");
		let ival = parseFloat($(inpt).val());
		let change = 0;
		switch(inpt){
			case "#cp5": change = 1.0; break;
			case "#cp6": change = 0.1; break;
			case "#cp7": change = 1.0; break;
			case "#cp8": change = 1.5; break;
			case "#cp9": change = 1.0; break;
		}
	
		let tmp = ival+change;
		update_CONST_CANVAS(inpt,tmp);
		//alert((++ival));
	});

//cut	
	$("#cut5,#cut6,#cut7,#cut8,#cut9").click(function(){
		const inpt = "#cp"+(this.id).replace("cut","");
		let ival = parseFloat($(inpt).val());
		let change = 0;
		let jx = 0;
		switch(inpt){
			case "#cp5": change = 1.0; jx = 0.1; break;
			case "#cp6": change = 0.1; break;
			case "#cp7": change = 1.0; break;
			case "#cp8": change = 0.5; jx = 0.1; break;
			case "#cp9": change = 0.5; break;
		}
		if(ival - change >= jx){
		
			let tmp = ival-change;//alert(ival+"-"+change+"="+tmp);
			update_CONST_CANVAS(inpt,tmp);
		}
	});

//reset
	
	$("#reset1,#reset2,#reset3,#reset4,#reset5,#reset6,#reset7,#reset8,#reset9").click(function(){
		const inpt = "#cp"+(this.id).replace("reset","");
		let change = 0;
		switch(inpt){
			case "#cp5": change = 1.0; break;
			case "#cp6": change = 0.5; break;
			case "#cp7": change = 0.0; break;
			case "#cp8": change = 1.0; break;
			case "#cp9": change = 0.5; break;
			case "#cp1": change = "#000000"; break;
			case "#cp3": change = "#000000"; break;
			case "#cp2": change = "#cccccc"; break;
			case "#cp4": change = "#ffffff"; break;
		}
		let tmp = change;
		update_CONST_CANVAS(inpt,tmp);
	});

	$("#cp1,#cp2,#cp3,#cp4,#cp5,#cp6,#cp7,#cp8,#cp9").blur(function(){
		const cpval = this.value;
		$("#"+this.id).css("color",cpval);	
		update_CONST_CANVAS("#"+this.id,cpval);
	});

	//let Ccpunt = 1;
    $("#resetZ").click(function(){
        //environment.data("GrpZoomOut",1/Ccpunt);
        //dataset = createImg(0,0,dataset,1/Ccpunt,false);
    });
    $("#cutZ").click(function(){
        //Ccpunt *= 0.9
        let ival = parseFloat(environment.data("GrpZoomOut"));
        environment.data("GrpZoomOut",ival*0.9);
        dataset = createImg(0,0,dataset,ival*0.9,false);
    });
    $("#addZ").click(function(){
        //Ccpunt *= 1.2
        let ival = parseFloat(environment.data("GrpZoomOut"));
        environment.data("GrpZoomOut",ival*1.2);
        dataset = createImg(0,0,dataset,ival*1.2,false);
    });



	///(function con() {  //con就是画每一帧动画的函数。
	///    console.log(111);
	///    requestAnimationFrame( con );  //动画更流畅  实现setInterval的效果
	///}());

 
	////////////////////////////////////////////////////////////////////////////////////////////////////////// 重叠坐标

    let canvas_central_left = document.getElementById("canvas-central-left");
    let ctx_central_left = echarts.init(canvas_central_left);

    option = null;
	const MAXX = 6000;

	$("#canvas-central-left").hide();

	$('#closeCVS').click(function(){
		$("#canvas-central-left").hide();
	});

	$('#checkComplex').click(function(){

		option = {
			xAxis: {
				scale: true
			},
			yAxis: {
				scale: true
			},
			dataZoom: [{
					type: 'inside'
				}
			],
			series: [{
				type: 'effectScatter',
				symbolSize: 10,
				data: getErrData()
			}, {
				type: 'scatter',
				symbolSize: 2,
				data: getZipData()
			}]
		};

		ctx_central_left.setOption(option, true);
		$("#canvas-central-left").show();//显示div
	});

	function getZipData(){

        let zips = [];
        let matrix = new Array();             //声明一维数组
		for(let x = 0; x < MAXX; x ++){
			  matrix[x]=new Array();        //声明二维数组
			  for(let y = 0; y < MAXX; y ++){
				   matrix[x][y] = 0;          //数组初始化为0
			  }
		}
		const nodelist = dataset.nodes;
		for(let i = 0; i < nodelist.length; i ++){
			const tmp = nodelist[i];
			//if(tmp.cx > 0 && tmp.cy > 0 && tmp.cx < 1500 && tmp.cy < 1500){
				//console.log((tmp.cx+1000)+","+(tmp.cy+1000));
				matrix[tmp.cx+2000][tmp.cy+2000] += 1;
			//	}
		}
		for(let x = 0; x < MAXX; x ++){
			  for(let y = 0; y < MAXX; y ++){
				   if( matrix[x][y] === 1){
						zips.push(new Array(x,y,matrix[x][y]));
				   }
			  }
		}
		//console.log(zips);
		return zips;

	}
	function getErrData(){

		let errs = [];
        let matrix = new Array();             //声明一维数组
		for(let x = 0; x < MAXX; x ++){
			  matrix[x]=new Array();        //声明二维数组
			  for(let y = 0; y < MAXX; y ++){
				   matrix[x][y] = 0;          //数组初始化为0
			  }
		}
		const nodelist = dataset.nodes;
		for(let i = 0; i < nodelist.length; i ++){
			const tmp = nodelist[i];
			//if(tmp.cx > 0 && tmp.cy > 0 && tmp.cx < 1500 && tmp.cy < 1500){
				//console.log((tmp.cx+1000)+","+(tmp.cy+1000));
				matrix[tmp.cx+2000][tmp.cy+2000] += 1;
			//	}
		}
		for(let x = 0; x < MAXX; x ++){
			  for(let y = 0; y < MAXX; y ++){
				   if( matrix[x][y] > 1){
						errs.push(new Array(x,y,matrix[x][y]));
				   }
			  }//
		}
		//errs.push([0,0]);
		//console.log(zips);
		return errs;
	}

	////////// show bones

    let canvas_central_mid = document.getElementById("canvas-central-mid");
    //let ctx_central_mid = echarts.init(canvas_central_mid);
    let cvs_ctmid = $("#canvas-central-mid");
    cvs_ctmid.hide();
	$('#closeCVSmid').click(function(){
        cvs_ctmid.hide();
	});

	$('#checkBones').click(function(){
		console.log(11111);
        cvs_ctmid.show();
	});

	////////////////////////////////////////////////////////////////////////////////////////// LEFT SELECT
	$("#select3").on('loaded.bs.select',function(){
		///alert(1);

		let str = "";
		$.ajax({
			type: "GET",
			//url: "Wiki-Vote.txt_of_780_without_800.json",
			url: httproot + ":3000/getlist",
			//url: "simple5.txt_of_800_without_800.json",
			async: false,
			dataType: "json",
			success: function(data){
				$.each(data, function(i, item){        
				   str += "<option>"+item.filename+"</option>"
				}); 
				$("#select3").append($(str));
				$("#select3").selectpicker("refresh");
			}
		});
	});
    //$("#select3").on
    //dataset = createImg(950,700,dataset,ZOOM_OUT*environment.data("GrpZoomOut"), true); //初始





});
 