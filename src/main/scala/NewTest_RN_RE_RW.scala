/*
import java.io.{File, FileWriter, PrintWriter}
import java.text.SimpleDateFormat

import TEST.Value_CC.countCC
import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx._
import org.apache.spark.graphx.lib.SS
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


object NewTest_RN_RE_RW {

  Logger.getLogger("org").setLevel(Level.ERROR)
  def main(args: Array[String]): Unit = {

    val tupleA = Array(0.0107,0.4024,20.0)//,Email-Enron.txt
   val tupleB = Array(0.0192,0.1489,29.1)//,wiki-Vote.txt
   val tupleC = Array(0.0049,0.3579,19.3)//,Gowalla_edges.txt

    //mainF(tupleA, Array("0.6", "Gowalla_edges.txt","\t","random"))
    //mainF(tupleA, Array("0.4", "Gowalla_edges.txt","\t","random"))
    //mainF(tupleA, Array("0.2", "Gowalla_edges.txt","\t","random"))
//
    //mainF(tupleB, Array("0.6","wiki-Vote.txt","\t","random"))
    //mainF(tupleB, Array("0.4","wiki-Vote.txt","\t","random"))
    //mainF(tupleB, Array("0.2","wiki-Vote.txt","\t","random"))
//
    //mainF(tupleC, Array("0.6","Email-Enron.txt","\t","random"))
    //mainF(tupleC, Array("0.4","Email-Enron.txt","\t","random"))
    //mainF(tupleC, Array("0.2","Email-Enron.txt","\t","random"))
//
//
//
//
    //mainF(tupleA, Array("0.6", "Gowalla_edges.txt","\t","randomEdge"))
    //mainF(tupleA, Array("0.4", "Gowalla_edges.txt","\t","randomEdge"))
    //mainF(tupleA, Array("0.2", "Gowalla_edges.txt","\t","randomEdge"))
//
    //mainF(tupleB, Array("0.6","wiki-Vote.txt","\t","randomEdge"))
    //mainF(tupleB, Array("0.4","wiki-Vote.txt","\t","randomEdge"))
    //mainF(tupleB, Array("0.2","wiki-Vote.txt","\t","randomEdge"))
//
    //mainF(tupleC, Array("0.6","Email-Enron.txt","\t","randomEdge"))
    //mainF(tupleC, Array("0.4","Email-Enron.txt","\t","randomEdge"))
    //mainF(tupleC, Array("0.2","Email-Enron.txt","\t","randomEdge"))







  }

  def mainF(o: Array[Double], args: Array[String]) {
    val REMOTE_JOB: Boolean = false // 是否提交到集群运行
    var fname: String = "" // 输入文件名
    var input: String = "" // 输入文件路径
    var output: String = "" // 输出文件路径
    var tab: String = "" // 输入文件的文本分隔符
    var sizeOfGraph: Long = 0 // 输入文件的节点大小


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


    def date: String
    = new SimpleDateFormat("MM-dd_HH-mm_").format(System.currentTimeMillis())
    // 用户设定，定义输入输出，分隔符，及迭代次数，注意路径  //
    tab = args(2)

    val DDDD = (args(0)).toFloat
    //val ptab = "60%"
    // email: 10-11000-0.3  5-22000-0.6  10-11000-0.3
    fname = args(1)


    input = "resources\\" + fname
    output = "output\\" + fname


    val cGraphS = GraphLoader.edgeListFile(sc, input)



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
    //抽样
    val graphtemp = RN_RE_RW.sampling(cGraphS, args(3), DDDD)

    //val paths: String = "I:\\IDEA_PROJ\\Visualization\\src\\main\\scala\\temp.csv"
//
    //val writer = new PrintWriter(new File(paths))
    //val idset = subgraph.vertices.map{case (id,att) => id}.collect()
    //val newedge = cGraphS.edges.filter{
    //  e=> idset.contains(e.srcId) && idset.contains(e.dstId)
    //}
    //newedge.collect().foreach(
    //  e => writer.write(e.srcId + " " + e.dstId+"\n")
    //)
    ////writer.write()
    //writer.close()




    //val graphtemp = GraphLoader.edgeListFile(sc, paths, numEdgePartitions = 8)

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
    //vcc
    val Vcc = countCC(graphtemp)

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
    out.write(s"R3, ${date}, $fname, ${(graphtemp.vertices.count().toDouble/sizeOfGraph*100).toInt+"%"}, kc=$DDDD ,,,," +
      s"${SMd.formatted("%.3f")}, SMt, ${(Vcc._2/o(0)).formatted("%.3f")}, ${(Vcc._3/o(1)).formatted("%.3f")}, ${(Vav/o(2)).formatted("%.3f")}\n")
    out.close()
    sc.stop()
  }

}




*/
