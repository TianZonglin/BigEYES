
import java.io.{File, FileWriter}
import java.text.SimpleDateFormat

import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


object NewTest_K_COre {

  Logger.getLogger("org").setLevel(Level.ERROR)
  def main(args: Array[String]): Unit = {

    val tupleA = Array(0.0107,0.4024,20.0)//,Email-Enron.txt
    val tupleB = Array(0.0192,0.1489,29.1)//,wiki-Vote.txt
    val tupleC = Array(0.0049,0.3579,19.3)//,Gowalla_edges.txt

    val tupleD = Array(0.0281,0.0887,43.7)//,ego-facebook.txt



   //NewTest_RN_RE_RW.mainF(tupleC, Array("0.6","Email-Enron.txt","\t","snowball"))
   //NewTest_RN_RE_RW.mainF(tupleC, Array("0.4","Email-Enron.txt","\t","snowball"))
   //NewTest_RN_RE_RW.mainF(tupleC, Array("0.2","Email-Enron.txt","\t","snowball"))



/*
    NewTest_SS.mainF(tupleA, Array("10","Email-Enron.txt","\t"))
    NewTest_SS.mainF(tupleA, Array("9","Email-Enron.txt","\t"))
    NewTest_SS.mainF(tupleA, Array("8","Email-Enron.txt","\t"))
    NewTest_SS.mainF(tupleA, Array("6","Email-Enron.txt","\t"))
    NewTest_SS.mainF(tupleA, Array("5","Email-Enron.txt","\t"))
    NewTest_SS.mainF(tupleA, Array("4","Email-Enron.txt","\t"))
    NewTest_SS.mainF(tupleA, Array("3","Email-Enron.txt","\t"))
    NewTest_SS.mainF(tupleA, Array("2","Email-Enron.txt","\t"))





    NewTest_SS.mainF(tupleC, Array("18","facebook_combined.txt"," "))
    NewTest_SS.mainF(tupleC, Array("17","facebook_combined.txt"," "))
    NewTest_SS.mainF(tupleC, Array("16","facebook_combined.txt"," "))
    NewTest_SS.mainF(tupleC, Array("33","facebook_combined.txt"," "))
    NewTest_SS.mainF(tupleC, Array("32","facebook_combined.txt"," "))
    NewTest_SS.mainF(tupleC, Array("31","facebook_combined.txt"," "))
    NewTest_SS.mainF(tupleC, Array("65","facebook_combined.txt"," "))
    NewTest_SS.mainF(tupleC, Array("64","facebook_combined.txt"," "))
    NewTest_SS.mainF(tupleC, Array("63","facebook_combined.txt"," "))
*/




    //mainF(tupleD, Array("51","facebook_combined.txt"," "))// 50
    //mainF(tupleD, Array("50","facebook_combined.txt"," "))
    //mainF(tupleD, Array("49","facebook_combined.txt"," "))
//
    //mainF(tupleD, Array("25","facebook_combined.txt"," "))// 50
    //mainF(tupleD, Array("26","facebook_combined.txt"," "))
    //mainF(tupleD, Array("27","facebook_combined.txt"," "))
//
    //mainF(tupleD, Array("17","facebook_combined.txt"," "))
    //mainF(tupleD, Array("16","facebook_combined.txt"," "))
    //mainF(tupleD, Array("15","facebook_combined.txt"," "))
//
    //NewTest_RN_RE_RW.mainF(tupleA, Array("0.6", "Gowalla_edges.txt","\t","snowball"))
    //NewTest_RN_RE_RW.mainF(tupleA, Array("0.4", "Gowalla_edges.txt","\t","snowball"))
    //NewTest_RN_RE_RW.mainF(tupleA, Array("0.2", "Gowalla_edges.txt","\t","snowball"))




    //NewTest_RN_RE_RW.mainF(tupleD, Array("0.6","facebook_combined.txt"," ","random"))
    //NewTest_RN_RE_RW.mainF(tupleD, Array("0.4","facebook_combined.txt"," ","random"))
    //NewTest_RN_RE_RW.mainF(tupleD, Array("0.2","facebook_combined.txt"," ","random"))
    //NewTest_RN_RE_RW.mainF(tupleD, Array("0.6","facebook_combined.txt"," ","randomEdge"))
    //NewTest_RN_RE_RW.mainF(tupleD, Array("0.4","facebook_combined.txt"," ","randomEdge"))
    //NewTest_RN_RE_RW.mainF(tupleD, Array("0.2","facebook_combined.txt"," ","randomEdge"))
    //NewTest_RN_RE_RW.mainF(tupleD, Array("0.6","facebook_combined.txt"," ","snowball"))
    //NewTest_RN_RE_RW.mainF(tupleD, Array("0.4","facebook_combined.txt"," ","snowball"))
    //NewTest_RN_RE_RW.mainF(tupleD, Array("0.2","facebook_combined.txt"," ","snowball"))


    //mainF(tupleB, Array("36","wiki-Vote.txt","\t"))
    //mainF(tupleB, Array("35","wiki-Vote.txt","\t"))
    //mainF(tupleB, Array("34","wiki-Vote.txt","\t"))
    //mainF(tupleB, Array("11","wiki-Vote.txt","\t"))
    //mainF(tupleB, Array("10","wiki-Vote.txt","\t"))
    //mainF(tupleB, Array("9","wiki-Vote.txt","\t"))
    //mainF(tupleB, Array("4", "wiki-Vote.txt","\t"))
    //mainF(tupleB, Array("3", "wiki-Vote.txt","\t"))
    //mainF(tupleB, Array("2", "wiki-Vote.txt","\t"))
//
    //mainF(tupleC, Array("16","Gowalla_edges.txt","\t"))
    //mainF(tupleC, Array("15","Gowalla_edges.txt","\t"))
    //mainF(tupleC, Array("14","Gowalla_edges.txt","\t"))
    //mainF(tupleC, Array("9", "Gowalla_edges.txt","\t"))
    //mainF(tupleC, Array("8", "Gowalla_edges.txt","\t"))
    //mainF(tupleC, Array("7", "Gowalla_edges.txt","\t"))
    //mainF(tupleC, Array("6", "Gowalla_edges.txt","\t"))
    //mainF(tupleC, Array("5", "Gowalla_edges.txt","\t"))
    //mainF(tupleC, Array("4", "Gowalla_edges.txt","\t"))


    //mainF(Array("6","soc-Epinions1.txt","\t"))
    //mainF(Array("5","soc-Epinions1.txt","\t"))
    //mainF(Array("4","soc-Epinions1.txt","\t"))
    //mainF(Array("3","soc-Epinions1.txt","\t"))
    //mainF(Array("2","soc-Epinions1.txt","\t"))
    //mainF(Array("10","wiki-Vote.txt","\t"))
    //mainF(Array("5","wiki-Vote.txt","\t"))
    //mainF(Array("4","wiki-Vote.txt","\t"))
    //mainF(Array("3","wiki-Vote.txt","\t"))
    //mainF(Array("2","wiki-Vote.txt","\t"))
    //mainF(Array("10","Email-Enron.txt","\t"))
    //mainF(Array("5","Email-Enron.txt","\t"))
    //mainF(Array("4","Email-Enron.txt","\t"))
    //mainF(Array("3","Email-Enron.txt","\t"))
    //mainF(Array("2","Email-Enron.txt","\t"))

  }

  def mainF(o: Array[Double], args: Array[String]) {

    val REMOTE_JOB: Boolean = false // 是否提交到集群运行
    var dbi: Double = 0d // 度筛选比率
    var sbi: Double = 0d // 图节点采样比率
    var fname: String = "" // 输入文件名
    var input: String = "" // 输入文件路径
    var output: String = "" // 输出文件路径
    var tab: String = "" // 输入文件的文本分隔符
    var sizeOfGraph: Long = 0 // 输入文件的节点大小
    var temperature: Double = 0d // 模拟退火温度
    var area: Int = 0 // 布局大小，次方值
    var AREA_MULTIPLICATOR: Double = 0d // 布局除数，计算用
    var iterations: Int = 0 // 总迭代次数
    var k: Double = 0d // 力导向弹性系数 K
    var epsilon: Double = 0d // 计算两点位置，最小量
    var gravitys: Double = 0d // 正作用于重力的参数
    var REP_SCALE: Double = 0d // 正作用于斥力的参数
    var ATT_SCALE: Double = 0d // 正作用于引力的参数
    var speed: Double = 1d // 作用于 updatePos
    var maxDisplace: Double = 0d // 最小移动距离，作用于 updatePos
    var SPEED_DIVISOR: Double = 0d // 关于移动速度，作用于 updatePos
    val defaultNode // 默认节点的绑定结构
    = ("o", 0.0, 0.0, (0.0, 0.0, 0.0, 0))

    def getConf: SparkConf = {
      if (REMOTE_JOB) {
        new SparkConf()
          .setAppName("RemoteGraphX")
          .setMaster("spark://hadoop02:7077")
          .set("spark.cores.max", "20")
        //.setJars(List("I:\\IDEA_PROJ\\VISNWK\\out\\artifacts\\visnwk_build_jar\\visnwk-build.jar"))
      } else {
        new SparkConf()
          .setAppName("LocalGraphX")
          .setMaster("local")
      }
    }

    val sc = new SparkContext(getConf)
    sc.setCheckpointDir("checkpoint")

    // 必要，否则报：Checkpoint directory has not been set in the SparkContext
    def ran: Double = {
      val random = new scala.util.Random
      random.nextDouble * 1000 - 500
    }

    def loadEdges(fn: String): Graph[Any, String] = {
      val s: String = "1.0"
      val edges: RDD[Edge[String]] =
        sc.textFile(fn)
          .filter(l => !l.startsWith("#"))
          //.sample(withReplacement = false, 1, salt)
          .map { //无放回
          line =>
            val fields = line.split(tab)
            Edge(fields(0).toLong, fields(1).toLong, s)

        }
      val graph: Graph[Any, String] = Graph.fromEdges(edges, "defaultProperty")
      graph
    }

    def convert(g: Graph[Any, String])
    : Graph[(String, Double, Double, (Double, Double, Double, Int)), Double] = {
      val transformedShuffledNodes: RDD[(VertexId, (String, Double, Double, (Double, Double, Double, Int)))] =
        g.vertices.map {
          v =>
            (v._1, (v._1.toString, ran, ran, (0.0, 0.0, 0.0, 0)))
        }
      val transformedEdges: RDD[Edge[Double]] = g.edges.map(e => Edge(e.srcId, e.dstId, e.attr.toDouble))
      val graphN = Graph(transformedShuffledNodes, transformedEdges, defaultNode)
      //graphN.vertices.foreach(println)
      //dumpWithLayout(graphN, output+"_random", isFirst = true)
      graphN

    }


    def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
      val p = new java.io.PrintWriter(f)
      try {
        op(p)
      } finally {
        p.close()
      }
    }


    def date: String
    = new SimpleDateFormat("MM-dd_HH-mm_").format(System.currentTimeMillis())
    // 用户设定，定义输入输出，分隔符，及迭代次数，注意路径  //
    tab = args(2)

    val DDDD = Integer.parseInt(args(0))
    //val ptab = "60%"
    // email: 10-11000-0.3  5-22000-0.6  10-11000-0.3
    fname = args(1)


    input = "resources\\" + fname
    output = "output\\" + fname
    iterations = 200


    val graphS = loadEdges(input)
    val origin_graphS = graphS
    val cGraphS = convert(graphS).persist()




    //cGraphS.triplets.foreach(println(_))

    //===================================================================
    //获取超级节点
    val distribution = cGraphS.degrees.map(t => (t._2, t._1 + "")).
      reduceByKey(_ + "," + _).
      sortBy(_._1, false).collect()

    val counts = distribution.length
    val theta = (counts * 0.5).toInt
    val head_d = distribution.take(theta) //取前百分20
    println("补充的分类个数："+head_d.length)

    val d_max = head_d.take(1)(0)._1 //最大出入度
    val head_nodes = head_d.reduce((a, b) => (1, a._2 + "," + b._2))._2.split(",")
    //println("> Get V_sup " + head_nodes.getClass.getTypeName)
    println("这些分类的总体个数："+head_nodes.length)
    //打印抽取的节点
    println(s">度分布添加- DONE!")

    //===================================================================
    //度分布SMd
    val distribution2 = cGraphS.degrees.map(t => (t._2, t._1 + "")).
      reduceByKey(_ + "," + _).
      sortBy(_._1, false).collect()

    val dfb_px = distribution2.map(t => (t._1, t._2.split(",").length))
    //dfb.foreach(println)

    //===================================================================
    //Core分层
    val diameter = 7
    val level = (diameter / 2).toInt
    val span = (d_max / level).toInt
    println(s"counts:${head_nodes.length},theta:$theta,d_max:$d_max,diameter:$diameter,level:$level,span:$span")
    var index = d_max
    while (index > 0) {
      index = index - span
      //println(index) //各层的指定Core的K值
      //TO DO
      //如何计算SHELL
    }
    ////var i = 0
    ////while (i < 100){
    ////  val testNUM = i
    ////  val G = KCore.run(cGraphS, testNUM, 1)
    ////  //达到最大次数（对于无法保证收敛的算法）或无消息传递时结束
    ////  val CoreYES = G.vertices.map(t => (t._2, t._1)).groupByKey.map(t => (t._1, t._2.size)).sortBy(_._1, false).take(1)(0)._2
    ////  println(s"$testNUM\t$CoreYES")
    ////  i += 5
    ////}


    def getGbyNode(g: Graph[(String, Double, Double, (Double, Double, Double, Int)), Double], layer: Array[String])
    : Graph[(String, Double, Double, (Double, Double, Double, Int)), Double] = {


      val gbd = g.vertices.mapValues((_,x)=>{
        if(layer.contains(x._1)){

          (x._1,x._2,x._3,(x._4._1,x._4._2,x._4._3,1))
        }else{
          (x._1,x._2,x._3,(x._4._1,x._4._2,x._4._3,0))
        }
      })

      Graph(gbd, g.edges, defaultNode)

    }



    val G = KCore.run(cGraphS, DDDD, 1)
    // 10 11924   30 3300  100  680
    //达到最大次数（对于无法保证收敛的算法）或无消息传递时结束
    //val CoreYES = G.vertices.map(t => (t._2, t._1))
    val KC_RDD_cGraphS = G.vertices.filter(x => {x._2==true}).map(x => x._1)//.foreach(println)



    //KC_RDD.foreach(println)
    val KC_arrs  = KC_RDD_cGraphS.map(x=>x.toString).collect()
    val KC_Dgree_cGraphS = getGbyNode(cGraphS, (KC_arrs++head_nodes).distinct)

    //KC_Dgree_cGraphS.triplets.foreach(println(_))
    println("D筛包含的点数："+head_nodes.length)
    println("K核包含的点数："+KC_arrs.length)
    println("K核+D筛包含的点数："+(KC_arrs++head_nodes).distinct.length)



/**
  * 0 10 20 30 40 50
    30	158
    35	114
    40	83
    45	56
    50	42
    55	20
    60	10
    65	7
    70	3
    75	2
    80	1
*/




    val paths: String = "I:\\IDEA_PROJ\\Visualization\\src\\main\\scala\\temp.csv"
    printToFile(new File(paths)) {
      p => {
        KC_Dgree_cGraphS.triplets.collect.foreach(
          x => {
            print("*")
            if(x.srcAttr._4._4 == 1 && x.dstAttr._4._4 == 1){
              p.println(s"${x.srcId} ${x.dstId}")
            }//else if(x.srcAttr._4._4 == 1){
             // p.println(s"${x.srcId} ${x.srcId}")
            //}else if(x.dstAttr._4._4 == 1){
            //  p.println(s"${x.dstId} ${x.dstId}")
            //}
          }
        )
      }
        p.flush()
        p.close()
    }

    val graphtemp = GraphLoader.edgeListFile(sc, paths, numEdgePartitions = 8)

    //===================================================================
    //度分布SMd
    val distribution_qx = graphtemp.degrees.map(t => (t._2, t._1 + "")).
      reduceByKey(_ + "," + _).
      sortBy(_._1, false).collect()
    val dfb_qx = distribution_qx.map(t => (t._1, t._2.split(",").length))

    val cpx = dfb_px.length //da
    val cqx = dfb_qx.length //xiao

    val reflect = cpx.toDouble/cqx
    val tem_qx = dfb_qx
    //var SMd = 0
    //var indexs = 0
    //for(indexs <- 0 to dfb_qx.length-1){
    //  val ind = Math.floor(indexs * reflect).toInt
    //  val vp = dfb_px(ind)._2
    //  val vq = dfb_qx(indexs)._2
    //  println(s"${ind}----${indexs}------${dfb_qx.length}")
    //  SMd = SMd + vp * Math.log( vp / vq).toInt
    //}

    var SMd = 0.0
    var indexs = 0
    for(indexs <- 0 to dfb_qx.length-1){
      val ind = Math.floor(indexs * reflect).toInt
      val vp = dfb_px(ind)._2
      val vq = dfb_qx(indexs)._2
      //println(s"${ind}----${indexs}------${dfb_qx.length}")
      SMd = SMd + (vp - vq)
    }
    SMd = (SMd/dfb_qx.length)

    //===================================================================
    //SMt
    var a = 0
    var b = 0
    var c = 0
    var d = 0
/*
    var

    val rddmapS = graphtemp.edges.map(x=>{
      (x.srcId+""+x.dstId, 1)
    }).collect()
    val rddmapG = cGraphS.edges.map(x=>{
      (x.srcId+""+x.dstId, 1)
    }).collect()

    graphtemp.


    val mapS = rddmapS.toMap
    var it = 0
    for(it <- 0 to rddmapG.length-1){
      val o1 = rddmapG(it)._2
      var o2:Int = 0
      try {
        o2 = mapS(rddmapG(it)._1)
        if(o1 == 1 && o2 == 1){ a = a + 1 }
        else if(o1 == 1 && o2 == 0){ b = b + 1}
        else if(o1 == 0 && o2 == 1){ c = c + 1}
        else if(o1 == 0 && o2 == 0){ d = d + 1}
      } catch {
        case ex: Exception => {
          System.err.println("ZZZZZZZZZZZZZZZz")  // 打印到标

        }
      }
    }
    val phi = (a*d-b*c)/(Math.sqrt((a + b)*(c + d)*(a + c)*(b + d)))
    printf("==========================================phi: "+phi)        */

    //===================================================================
    //vcc
    val Vcc = TEST.Value_CC.countCC(graphtemp)

    //===================================================================
    //av
    val sumDegree = graphtemp.degrees.map(t => t._2).reduce((a,b)=>(a+b))
    val sumGraph = graphtemp.vertices.count()
    val Vav = (sumDegree.toDouble/sumGraph)

    //===================================================================
    //endprint
    sizeOfGraph = cGraphS.vertices.count()
    val sizeEdg = cGraphS.edges.count()
    println(s"> Size of the graph  : $sizeOfGraph  nodes, $sizeEdg edges.")
    println("> DONE!")

    val out = new FileWriter("I:\\IDEA_PROJ\\Visualization\\src\\main\\scala\\11111111111.csv", true)
    out.write(s"KSS, ${System.currentTimeMillis()}, $fname, ${((KC_arrs++head_nodes).distinct.length.toDouble/sizeOfGraph*100).toInt+"%"}, kc=$DDDD ,,,," +
      s"${SMd.formatted("%.3f")}, SMt, ${(Vcc._2/o(0)).formatted("%.3f")}, ${(Vcc._3/o(1)).formatted("%.3f")}, ${(Vav/o(2)).formatted("%.3f")}\n")
    out.close()
    sc.stop()
  }

}



