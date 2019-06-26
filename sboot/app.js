const express = require('express')
//const process = require('child_process');
const app = express()


const spawn = require('child_process').spawn;
const ws = require("nodejs-websocket")
const bodyParser = require('body-parser');
const fs = require('fs');

//app.use(bodyParser.urlencoded({extended:true}));
//app.use(bodyParser.urlencoded({extended:true}));
//app.use(bodyParser.json());

////////////////////// 允许跨域 /////////////////////////////
app.all('*', function(req, res, next) {
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Headers", "Content-Type,Content-Length, Authorization, Accept,X-Requested-With");
    res.header("Access-Control-Allow-Methods","PUT,POST,GET,DELETE,OPTIONS");
    res.header("X-Powered-By",' 3.2.1')
    if(req.method=="OPTIONS") res.send(200);/*让options请求快速返回*/
    else  next();
});

app.post('/fetch_layout_rst', function (req, res) {
	// console.log("req: "+req);
	req.on('data',function(data){
		let obj;
		let arr = data.toString().split("&");
		let filename = '';
		if(arr.length >= 2) {
			console.log("muti");
			//console.log(arr[0].split("=")[1]);
			filename = arr[0].split("=")[1];
		}else{
			console.log("single");
			//console.log(data.toString().split("=")[1]);
			filename = data.toString().split("=")[1];
		}
		const file = fs.readFileSync("/usr/local/node-v10.9.0-linux-x64/ExpressWeb/Results/"+filename, "utf8");
		//res.send({"a":filename})
		res.send(file)
    })
	// res.send(file)
})
 



// Scream server
//var server2 = app.listen(9999, function(){
//	var host = server2.address().address;
//	var port = server2.address().port;
//	console.log('address is http://%s:%s', host, port);
//});
 
// Scream server
var server = ws.createServer(function (conn) {
	console.log("Spark New connection");
	//获取连接信息
	conn.on("text", function (obj) {
		let param = obj.split(",");
		let fname = param[0];
		const rtime = param[1];
		//console.log(fname);
		//console.log(rtime);
		//const cmd = './spark-submit --class WithoutSample visualization-build.jar Vote.txt 6';
		//var exer = process.exec('ping -c 10 219.216.65.14');
		let exer = spawn(
			'/usr/local/spark/bin/spark-submit',
			['--class','WithoutSample','/usr/local/spark/bin/visualization-build.jar','Vote.txt',rtime]);
		exer.stdout.on('data', function (s) {
			conn.sendText(s.toString());
		});
		exer .stdout.on('end', function () {
			conn.sendText('@done');
		});
		//conn.sendText(obj.toUpperCase()+"!!!")
	});
	//断开连接的回调
	conn.on("close", function (code, reason) {
		console.log("Spark Connection closed")
	})
	//处理错误事件信息
	conn.on('error',function(err){
		console.log('Spark throw : err');
		console.log(err);
	})
}).listen(8001);
console.log('Spark webSocket server listening on port 8001');


var CanvasServer = ws.createServer(function (conn) {
	console.log("Canvas New connection");
	//获取连接信息
	conn.on("text", function (obj) {
		// xx@xx#.json start_i end_i
		let param = obj.split(",");
		let filename = param[0];
		let start_i = param[1];
		let endle_i = param[2];
		let jsoname;
		//console.log(fname);
		//console.log(rtime);
		//filename = filename.replace("#",endle_i);
		for(let index = start_i; index <= endle_i; index ++){
			//console.log(index);
			jsoname = filename.replace("@",index);
			//console.log(jsoname);
			//const filestr = fs.readFileSync("/usr/local/node-v10.9.0-linux-x64/ExpressWeb/Results/"+jsoname, "utf8");
			const filestr = fs.readFileSync("I:\\IDEA_PROJ\\Visualization\\output\\"+jsoname, "utf8");
			conn.sendText(filestr)
		}
	});
	//断开连接的回调
	conn.on("close", function (code, reason) {
		console.log("Canvas Connection closed")
	})
	//处理错误事件信息
	conn.on('error',function(err){
		console.log('Canvas throw : err');
		console.log(err);
	})
}).listen(7001);
console.log('Canvas webSocket server listening on port 7001');

var rp = require('request-promise');
 
var MonitorServer = ws.createServer(function (conn) {
	console.log("Monitor New connection");
	//获取连接信息
	conn.on("text", function (obj) {
		let cronJob = require("cron").CronJob;
		//setTimeout(function(){
		let hadexecuted = false;
		//每秒执行一次
		let Timer = new cronJob('* * * * * *', function () {
			let job1 = {
				uri: 'http://219.216.65.14:4040/api/v1/applications',
				headers: {'User-Agent': 'Request-Promise'},
				json: true // Automatically parses the JSON string in the response
			};
			let appid = '';
			let jsond = {};
			rp(job1)
				.then(function (repos) {
					//rp(job2)
					//	.then(function (repos) {
					//		console.log('User has %d repos', repos.length);
					//	})
					//	.catch(function (err) {
					//		job.stop();
					//	});	
					//	
					//rp(job3)
					//	.then(function (repos) {
					//		console.log('User has %d repos', repos.length);
					//	})
					//	.catch(function (err) {
					//		job.stop();
					//	});	
					appid = repos[0].id;
					let details = repos[0].attempts[0];
					let times = details.startTime.split(".");
					//console.log('times::::', times[0]);
					//conn.sendText(repos);
					jsond.AppId = repos[0].id;
					jsond.AppName = repos[0].name;
					jsond.StartTime = times[0].replace("T","");
					//console.log(repos);
					let job2 = {
						uri: "http://219.216.65.14:4040/api/v1/applications/"+appid+"/allexecutors",
						headers: {'User-Agent': 'Request-Promise'},
						json: true // Automatically parses the JSON string in the response
					};
					let job3 = {
						uri: "http://219.216.65.14:4040/api/v1/applications/"+appid+"/environment",
						headers: {'User-Agent': 'Request-Promise'},
						json: true // Automatically parses the JSON string in the response
					};
					rp(job3)
						.then(function (environ) {
							hadexecuted = true;
							let tmp = environ.sparkProperties;
							//console.log("=======");
							//console.log(tmp);
							jsond.DriverMemory = tmp[5][1];
							jsond.MasterNode = tmp[9][1];
							jsond.DeployMode = tmp[13][1];
							jsond.ExecuteJAr = tmp[8][1].split("/bin/")[1];
							jsond.DriverHost = tmp[4][1];

							rp(job2)
								.then(function (body) {
									let executors = [];
									body.forEach(function(v,i,a){
										executors.push({
											"hostPort": v.hostPort.split(":")[0],
											"rddBlocks": v.rddBlocks,
											"isActive": v.isActive,
											"memoryUsed": (v.memoryUsed*8000/v.maxMemory)+"%"
										});
										//console.log(a);
									});
									jsond.WorkerMaxmem = (body[0].maxMemory/1024/1024).toFixed(2)+"MB";
									jsond.executors = executors;
									//console.log(body);
									//console.log(jsond);
									conn.sendText(JSON.stringify(jsond));  //////////////// 发送
								})
								.catch(function (err) {
									console.log("ERROR: job2 appid/allexecutors");
									if(hadexecuted)
										Timer.stop();
								});
						})
						.catch(function (err) {
							console.log("ERROR: job3 appid/environment");
							if(hadexecuted)
								Timer.stop();
						});	
				})
				.catch(function (err) {
					console.log("ERROR: job1 api/v1/applications.")
					if(hadexecuted) 
						Timer.stop();
				});	
			
		}, null, true, 'Asia/Shanghai');
		//},5000);	

	});
	//断开连接的回调
	conn.on("close", function (code, reason) {
		console.log("Monitor Connection closed")
	})
	//处理错误事件信息
	conn.on('error',function(err){
		console.log('Monitor throw : err');
		console.log(err);
	})
}).listen(10001);
console.log('Monitor webSocket server listening on port 10001');


// GET请求 直接浏览器访问 是 GET 方式
app.post('/getjson', function (req, res) {
	  req.setEncoding('utf8');
	  res.setHeader('Access-Control-Allow-Origin','*')
	//console.log();
	req.on('data',function(data){
		//const file = fs.readFileSync("/usr/local/node-v10.9.0-linux-x64/ExpressWeb/Wiki-Vote.txt_of_780_without_800.json", "utf8");
		const file = fs.readFileSync("I:\\IDEA_PROJ\\Visualization\\output\\"+data, "utf8");
        console.log("****************");
		console.log("发送该数据至页面："+data);
		res.send(file);
		//console.log("****************");
		//console.log(data);
	})

})
 
 

app.get('/getlist', function (req, res) {
	  req.setEncoding('utf8');
	  res.setHeader('Access-Control-Allow-Origin','*')
 
	//let path = "/usr/local/node-v10.9.0-linux-x64/ExpressWeb/Results/demos/";
	let path = "I:\\IDEA_PROJ\\Visualization\\output\\";
	let filesList = [];
	var files = fs.readdirSync(path);
	files.forEach(function (itm, index) {
		var stat = fs.statSync(path + itm);
		if (stat.isDirectory()) {
			//递归读取文件
			//readFileList(path + itm + "/", filesList)
		} else {

			var obj = {};//定义一个对象存放文件的路径和名字
			obj.path = path;//路径
			obj.filename = itm//名字
			filesList.push(obj);
		}

	})
	
	
	//res.send({"a":filename})
	res.send(filesList)

 
	// res.send(file)
})





// app.get('/excuteJar', function (req, res) {
	// res.send('Got a EXECUTE request')
// })

// app.get('/user', function (req, res) {

// var spawn = require('child_process').spawn;
// var h = spawn('/usr/local/spark/bin/spark-submit',['--class','WithoutSample','/usr/local/spark/bin/visualization-build.jar','Vote.txt','6']);
// h.stdout.on('data', function (s) {
// console.log(s.toString());
// });
// h.stdout.on('end', function () {
// console.log('ls done');
// });

  // res.send('TTTTTEST')
// })

// app.delete('/user', function (req, res) {
  // res.send('Got a DELETE request at /user')
// })
app.listen(3000, () => console.log('Example app listening on port 3000!'))
