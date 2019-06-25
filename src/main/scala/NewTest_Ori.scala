import java.io.{File, FileWriter, PrintWriter}
import java.text.SimpleDateFormat

import TEST.Runner
import TEST.Value_CC.countCC
import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


object NewTest_Ori {

  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]) {

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
    tab = "\t"

    // 本地项目相对路径
    fname = "Gowalla_edges.txt"
    input = "resources\\" + fname
    output = "output\\" + fname


    val graphS = loadEdges(input)
    val cGraphS = convert(graphS).persist()
    //cGraphS.triplets.foreach(println)




    ////===================================================================
    ////度分布SMd
    //val distribution2 = cGraphS.degrees.map(t => (t._2, t._1 + "")).
    //  reduceByKey(_ + "," + _).
    //  sortBy(_._1, false).collect()
//
    //val dfb = distribution2.map(t => (t._1, t._2.split(",").length))
    //dfb.foreach(println)



    //===================================================================
    //av
    val sumDegree = cGraphS.degrees.map(t => t._2).reduce((a,b)=>(a+b))
    val sumGraph = cGraphS.vertices.count()
    val Vav = (sumDegree.toDouble/sumGraph).formatted("%.1f")

    //===================================================================
    //cc
    val paths: String = "I:\\IDEA_PROJ\\Visualization\\src\\main\\scala\\temp.csv"
    printToFile(new File(paths)) {
      p => {
        cGraphS.triplets.collect.foreach(
          x => {
            print("*")
            p.println(s"${x.srcId} ${x.dstId}")
          }
        )
      }
        p.flush()
        p.close()
    }
    val graphtemp = GraphLoader.edgeListFile(sc, paths, numEdgePartitions = 8)
    val Vcc = countCC(graphtemp)




    //===================================================================
    sizeOfGraph = cGraphS.vertices.count()
    val sizeEdg = cGraphS.edges.count()
    println(s"> Size of the graph  : $sizeOfGraph  nodes, $sizeEdg edges.")
    println("> DONE!")

    cGraphS.unpersist()

    val out = new FileWriter("I:\\IDEA_PROJ\\Visualization\\src\\main\\scala\\11111111111.csv", true)
    out.write(s"SMd,SMt,${(Vcc._2).formatted("%.4f")},${(Vcc._3).formatted("%.4f")},${Vav}\n")
    out.close()


    sc.stop()
  }

}


