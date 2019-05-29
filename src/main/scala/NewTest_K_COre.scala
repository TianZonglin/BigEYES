import java.text.SimpleDateFormat

import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


object NewTest_K_COre {

  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]) {

    val REMOTE_JOB: Boolean = false                 // 是否提交到集群运行
    var dbi: Double = 0d                               // 度筛选比率
    var sbi: Double = 0d                               // 图节点采样比率
    var fname: String = ""                          // 输入文件名
    var input: String = ""                          // 输入文件路径
    var output: String = ""                         // 输出文件路径
    var tab: String = ""                            // 输入文件的文本分隔符
    var sizeOfGraph: Long = 0                       // 输入文件的节点大小
    var temperature: Double = 0d                       // 模拟退火温度
    var area: Int = 0                                  // 布局大小，次方值
    var AREA_MULTIPLICATOR: Double = 0d                // 布局除数，计算用
    var iterations: Int = 0                            // 总迭代次数
    var k: Double = 0d                                 // 力导向弹性系数 K
    var epsilon: Double = 0d                           // 计算两点位置，最小量
    var gravitys: Double =0d                        // 正作用于重力的参数
    var REP_SCALE: Double = 0d                      // 正作用于斥力的参数
    var ATT_SCALE: Double = 0d                      // 正作用于引力的参数
    var speed: Double = 1d                           // 作用于 updatePos
    var maxDisplace: Double =0d                     // 最小移动距离，作用于 updatePos
    var SPEED_DIVISOR: Double = 0d                  // 关于移动速度，作用于 updatePos
    val defaultNode                                    // 默认节点的绑定结构
      = ("o", 0.0, 0.0, (0.0, 0.0, 0.0, 0.0))

    def getConf: SparkConf = {
      if(REMOTE_JOB){
        new SparkConf()
          .setAppName("RemoteGraphX")
          .setMaster("spark://hadoop02:7077")
          .set("spark.cores.max", "20")
          //.setJars(List("I:\\IDEA_PROJ\\VISNWK\\out\\artifacts\\visnwk_build_jar\\visnwk-build.jar"))
      }else{
        new SparkConf()
          .setAppName("LocalGraphX")
          .setMaster("local")
      }
    }
    val sc = new SparkContext(getConf)
    sc.setCheckpointDir("checkpoint")
    // 必要，否则报：Checkpoint directory has not been set in the SparkContext
    def ran:Double = {
      val random = new scala.util.Random
      random.nextDouble * 1000  - 500
    }

    def loadEdges(fn: String): Graph[Any, String] = {
      val s: String = "1.0"
      val edges: RDD[Edge[String]] =
        sc.textFile(fn)
          .filter(l => ! l.startsWith("#") )
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
    : Graph[(String, Double, Double, (Double, Double, Double, Double)), Double] = {
      val transformedShuffledNodes: RDD[(VertexId, (String, Double, Double, (Double, Double, Double, Double)))] =
        g.vertices.map {
          v =>
            (v._1, (v._1.toString, ran, ran, (0.0, 0.0, 0.0, 0.0)))
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





    def date:String
    = new SimpleDateFormat("MM-dd_HH-mm_").format(System.currentTimeMillis())
    // 用户设定，定义输入输出，分隔符，及迭代次数，注意路径  //
    tab = "\t"
    if(REMOTE_JOB){
      // 集群HDFS绝对路径
      fname = args(0)
      input = "hdfs://hadoop02:9000/SNAP/DATASET/"+fname
      output = "hdfs://hadoop02:9000/SNAP/OUTPUT/"+date+"dump_"+fname+"_"+args(1).toInt
      iterations = args(1).toInt
    }else{
      // 本地项目相对路径
      fname = "Vote.txt"
      input = "resources\\"+fname
      output = "output\\"+fname
      iterations = 200
    }

    val graphS = loadEdges( input )
    val cGraphS = convert( graphS ).persist()




    //cGraphS.degrees.collect.foreach(println(_))


  //////////////////////////////////////////////////////////////// 二跳邻居


   //type VMap=Map[VertexId,Int]

   ///**
   //  * 节点数据的更新 就是集合的union
   //  */
   //def vprog(vid:VertexId,vdata:VMap,message:VMap)
   //:Map[VertexId,Int]=addMaps(vdata,message)

   ///**
   //  * 发送消息
   //  */
   //def sendMsg(e:EdgeTriplet[VMap, _])={

   //  //取两个集合的差集  然后将生命值减1
   //  val srcMap=(e.dstAttr.keySet -- e.srcAttr.keySet).map { k => k->(e.dstAttr(k)-1) }.toMap
   //  val dstMap=(e.srcAttr.keySet -- e.dstAttr.keySet).map { k => k->(e.srcAttr(k)-1) }.toMap

   //  if(srcMap.size==0 && dstMap.size==0)
   //    Iterator.empty
   //  else
   //    Iterator((e.dstId,dstMap),(e.srcId,srcMap))
   //}

   ///**
   //  * 消息的合并
   //  */
   //def addMaps(spmap1: VMap, spmap2: VMap): VMap =
    //  (spmap1.keySet ++ spmap2.keySet).map {
   //    k => k -> math.min(spmap1.getOrElse(k, Int.MaxValue), spmap2.getOrElse(k, Int.MaxValue))
   //  }.toMap


   //val two=2  //这里是二跳邻居 所以只需要定义为2即可
   //val newG=cGraphS.mapVertices((vid,_)=>Map[VertexId,Int](vid->two))
   //  .pregel(Map[VertexId,Int](), two, EdgeDirection.Out)(vprog, sendMsg, addMaps)
   ////newG.vertices.collect().foreach(println(_))
   ////过滤得到二跳邻居 就是value=0 的顶点
   //val twoJumpFirends=newG.vertices
   //  .mapValues(_.filter(_._2==0).keys)

   //twoJumpFirends.collect().foreach(println(_))

    //twoJumpFirends.collect().foreach(println(_))

    val diameter = 7

    val distribution = cGraphS.degrees.map(t => (t._2,t._1+"")).
      reduceByKey(_+","+_).
      sortBy(_._1,false).collect()

    val counts = distribution.length

    val theta = (counts * 0.2).toInt

    val head_d = distribution.take(theta) //取前百分20

    val head_nodes = head_d.reduce((a,b)=>(1,a._2+","+b._2))

    //println("MAX_ID："+dufenbu.max()._1)

    //println("MAX_DGREE："+dufenbu.max()._2)






    val G = KCore.run(cGraphS, 2, 1)//达到最大次数（对于无法保证收敛的算法）或无消息传递时结束



    println("> DONE!")
    println("> DONE!")
    //head_nodes.foreach(println)
    println(head_nodes)
   // val G = KCore.run(cGraphS, 2, 10)

   // val G = KCore.run(cGraphS, 2, 10)

   // G.vertices.foreach(println)


















    sizeOfGraph = cGraphS.vertices.count()
    val sizeEdg = cGraphS.edges.count()
    println(s"> Size of the graph  : $sizeOfGraph  nodes, $sizeEdg edges.")
    println("> DONE!")

    sc.stop()

  }

}


