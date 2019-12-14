#### Complicated and massive data are as turbulent as floods recently, which is particularly obvious at present. When dealing with related data, complex association often brings catastrophic nightmares to the extraction of the overall characteristics of data. The purpose of BigEYES is to see the sun through clouds, unravel complex entanglement and present clear features of chaos.

### Initial Works

#### Web Container

- install http-server: `npm install http-server -g`
- run `cd boot`
- run `http-server -p 1234`
- visit `localhost:1234` in Chrome

Here, we can see the whole system, but no interaction we can do cause we still need to establish the connection between frontend and backend by using Websocket. Of course, there would have some red errors if you open the `console` part of debug functions by F12.


#### Backend with Websocket

- run `cd sboot`
- change the local link of your datas, the first one located around `/getjson`, the another one is '/getlist'
- run `node app.js` then you can see the output like `Canvas webSocket server listening on port 7001`
- now, refresh the webpage without any cache, then you can find the red errors gone, and the console would show text like `canvasSocket open` 

Here, you can use this system, but limited in using of drawing datas, if you want to link the function about executing tasks with Spark, then you need to do this below.
Of course, if you click the button of `GO`, it would be crash cause no Spark cluster connected.

#### Enhance the functions with Spark

- presume that you have establish a normal Spark cluster.
- put the computing zip package onto master node
- modify the ip-address of `app.js`, including all places that have the ip `219.216...`

Now, you can see it's okay if you click `GO`, and the part of `Monitor` could work as well.  


That's all.

### Previous Works


#### To Do List

- [ ] 4/03. Change SPARK/ GRAPHX codes to achieve A, B and C 
- [ ] 4/03. Upload input file into Hadoop/ Hdfs, combined with other parts 
- [x] 4/02. Use webskt. to replace ajax-polling in monitor module 
- [ ] 4/01. Fixable Coodinates CHECK NEW 
- [x] 4/01. Canvas dynamically render more than one image 
- [ ] 4/01. B. Classified display with color and weight 
- [ ] 3/29. Apply EDGE pruning into graph layout	* 
- [ ] 3/29. C. Muti-views of canvas image 
- [ ] 3/29. Complexed canvas binding events 
- [ ] 3/29. A. Connect SELECT with canvas/ ajax+websocket NOW
- [x] 3/29. Canvas interaction and controls [finished] 
- [x] 3/27. Cross domain	* 

#### Problems

- [ ] 4/03. How to allow echarts' scatter mode zoom among Y-axis * 
- [x] 4/01. How to create a code block running in the aynsc mode * 
- [ ] 4/01. Can't get the value of req.body by bodyParser in Node.js * 

#### Finished

- [x] 4/03. Solved cross domain* by Node/ agent running in backend 
- [x] 4/03. Rewrited monitor module with webskt. for loosely-coupled 
- [x] 4/03. Used websocket to transform json data constantly	
- [x] 4/02. Solved drag() and scale() 's veriables' unification	
- [x] 4/01. Used express, fs to fetch json file from back to front	
- [x] 4/01. Created TODO page 
- [x] 3/31. Optimized canvas render and finished Coordinates CHECK
- [x] 3/30. Finished canvas tools/ interaction and controls/ staynight 
- [x] 3/29. Reported progress 
- [x] 3/28. Run nodejs in Linux to execute jar 
- [x] 3/28. Used Springboot to build back-end 
- [x] 3/28. Finished task monitor module by ajax polling 
- [x] 3/27. Used Nginx to solve the cross domain 
- [x] 3/26. Optimized Hbase cluster 
- [x] 3/25. New age started 
- [x] 3/24. Rebuilt vis.jar and used spark rest api 
- [x] 3/23. Rebuilt prev. Spark cluster and project 
- [x] 3/23. How to determine a pow-law distribution 
- [x] 3/21. Used Echarts to display info 
- [x] 3/21. Finished Canvas drags and scale 
- [x] 3/20. Solved Canvas serration 
- [x] 3/20. Used Canvas instead of WebGL, D3js, HT for Web 
- [x] 3/20. Built css frame by iBootstrap 

#### 
  
### Input
```$xslt
# Directed graph (each unordered pair of nodes is saved once): Wiki-Vote.txt 
# Wikipedia voting on promotion to administratorship (till January 2008). Directed edge A->B means user A voted on B becoming Wikipedia administrator.
# Nodes: 7115 Edges: 103689
# FromNodeId	ToNodeId
30	1412
30	3352
30	7478
...
3	54
```

### Version 2.0

![BigEYES.png-1520.5kB][1]

### Version 1.0

![](http://static.zybuluo.com/EVA001/vg6swhpmqxiet5l8tk1q7sy8/image_1cf8beqjs1b1i1j5ntjftg31ric9.png)


  [1]: http://static.zybuluo.com/EVA001/ng5rcrpb5f76qpnd0e1z0flu/BigEYES.png