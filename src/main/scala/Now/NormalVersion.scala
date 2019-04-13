package Now

import java.io._
import java.text.SimpleDateFormat

import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


object NormalVersion {

  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]) {

    val REMOTE_JOB: Boolean = false                 // 是否提交到集群运行
    var dbi: Double = 0d                               // 度筛选比率
    var sbi: Double = 0d                               // 图节点采样比率
    var fname: String = ""                          // 输入文件名
    var input: String = ""                          // 输入文件路径
    var output: String = ""                         // 输出文件路径
    var tab: String = "\t"                            // 输入文件的文本分隔符
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
        printf("集\t群\t环\t境\t运\t行")
        new SparkConf()
          .setAppName("RemoteGraphX")
          .setMaster("spark://hadoop02:7077")
          .set("spark.cores.max", "20")
          //.setJars(List("I:\\IDEA_PROJ\\VISNWK\\out\\artifacts\\visnwk_build_jar\\visnwk-build.jar"))
      }else{
        printf("本\t地\t运\t行")
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
      val s: String = "1.0"                                 // 【附加的顶点信息】
      val edges: RDD[Edge[String]] =
        sc.textFile(fn, 8)                                     // 【注意第二个参数：minPartitions = 8】 [stage: ... (7 + 1) / 8]
          .filter(l => ! l.startsWith("#") )
          //.sample(withReplacement = false, 1, salt)
          .map {
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

    class Vector(var x: Double = 0.0, var y: Double = 0.0) {

      def +(operand: Vector): Vector = {
        new Vector(x + operand.x, y + operand.y)
      }

      def -(operand: Vector): Vector = {
        new Vector(x - operand.x, y - operand.y)
      }

      def *(operand: Vector): Vector = {
        new Vector(x * operand.x, y * operand.y)
      }

      def *(operand: Double): Vector = {
        new Vector(x * operand, y * operand)
      }

      def /(operand: Double): Vector = {
        new Vector(x / operand, y / operand)
      }

      def isNaN: Boolean = x.isNaN || y.isNaN

      def set(x: Double, y: Double): Vector = {
        this.x = x
        this.y = y
        this
      }

      def clear(): Unit = {
        x = 0.0
        y = 0.0
      }

    }

    def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {

      val p = new java.io.PrintWriter(f)

      try {

        op(p)

      } finally {

        p.close()

      }

    }

    def cool(iteration: Int): Unit = {

      temperature = (1.0 - (iteration.toDouble / iterations)) * 0.1 * math.sqrt(area)

    }

    def dumpWithLayout(g: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double],
                       fn: String,
                       isFirst: Boolean)
    : Unit = {

        val furl = {
          if(isFirst) fn+s"_without_$iterations.json"
          else fn+s"__without__$iterations.json"
        }

        printToFile(new File(furl)) {
          p => {
            p.println("""{"nodes": [""")
            g.vertices.collect.foreach(
              x => p.println(s"""{"weight": "0","name": "o","value": "${x._2._4._3/10f}","cx":"${x._2._2}","cy": "${x._2._3}"}, """)
            )
            p.println("""{"weight": "0","name": "o","value": "0","cx":"0","cy": "0"}],    "links": [""")
            g.triplets.collect.foreach(
              x => p.println(s"""{"value": "0","x1": "${x.srcAttr._2}","y1": "${x.srcAttr._3}","x2": "${x.dstAttr._2}","y2": "${x.dstAttr._3}"},""")
            )
            p.println("""{"value": "0","x1": "0","y1": "0","x2": "0","y2": "0"}]}""")
          }
        }
      }



    /***
      * 计算斥力
      */
    def calcRepulsion(array: Array[(VertexId, (String, Double, Double, (Double, Double, Double, Double)))],
                       g: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double])
    : Graph[(String, Double, Double, (Double, Double, Double, Double)), Double] = {
      g.cache()
      val setC: VertexRDD[(String, Double, Double, (Double,Double,Double,Double))]
      = g.vertices.mapValues(
         v => {
           val v1 = (v._2,v._3)
           var bx = 0D
           var by = 0D
           array.foreach(x=>{
             val v2 = (x._2._2,x._2._3)
             val xDist = v1._1 - v2._1
             val yDist = v1._2 - v2._2

             val dist = math.sqrt(xDist * xDist + yDist * yDist)
             if (dist > 0) {
               val repulsiveF = k * k / dist
               // Force de répulsion
               bx += xDist / dist * repulsiveF // on l'applique...
               by += yDist / dist * repulsiveF
               //println(s"$yDist / $dist * $repulsiveF")
             }

           })
           //println(s"k = $k bx = $bx, by = $by")
           (v._1, v._2, v._3, (v._4._1 + bx, v._4._2 + by, v._4._3, v._4._4))
        }
      )
      val graphN = Graph(setC, g.edges, defaultNode)
      g.unpersist(blocking = false)
      graphN
    }

    def calcAttraction(g: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double])
    : Graph[(String, Double, Double, (Double, Double, Double, Double)), Double] = {

      g.cache()
      g.checkpoint()

      val attr1: VertexRDD[(Double, Double)] = g.aggregateMessages[(Double, Double)](

        sendMsg = {
          triplet => {
            val Source = triplet.srcAttr
            val Target = triplet.dstAttr
            val xDist = Source._2 - Target._2
            val yDist = Source._3 - Target._3

            val dist = math.sqrt(xDist * xDist + yDist * yDist)
            val attractiveF = dist * dist / k

            if (dist > 0) {
              val dx = xDist / dist * attractiveF
              val dy = yDist / dist * attractiveF
              //println(s"dx = $dx, dy = $dy")
              triplet.sendToDst(dx, dy)
            }

          }
        },
        mergeMsg = {
          (m1, m2) => (m1._1+m2._1, m1._2+m2._2)

        }
      )

      val after1 = g.joinVertices(attr1)(
        (_, a, b) => {

          (a._1, a._2, a._3, (a._4._1 + b._1, a._4._2 + b._2, a._4._3, a._4._4))

        }
      )

      val attr2: VertexRDD[(Double, Double)] = after1.aggregateMessages[(Double, Double)](
        sendMsg = {
          triplet => {
            val Source = triplet.srcAttr
            val Target = triplet.dstAttr
            val xDist = Source._2 - Target._2
            val yDist = Source._3 - Target._3

            val dist = math.sqrt(xDist * xDist + yDist * yDist)
            val attractiveF = dist * dist / k

            if (dist > 0) {
              val dx = xDist / dist * attractiveF
              val dy = yDist / dist * attractiveF
              //println(s"dx = $dx, dy = $dy")
              triplet.sendToSrc(0D-dx, 0D-dy)
            }
          }
        },
        mergeMsg = {
          (m1, m2) => (m1._1+m2._1, m1._2+m2._2)
        }
      )

      val after2 = after1.joinVertices(attr2)(
        (_, a, b) => {
          (a._1, a._2, a._3, (a._4._1 + b._1, a._4._2 + b._2, a._4._3, a._4._4))
        }
      )

      g.unpersist(blocking = false)

      after2

    }

    def layoutFDFR2( g: Graph[ (String, Double, Double, (Double,Double,Double,Double)), Double ],
                     ctime: Int,
                     diet:Boolean)
    : Graph[ (String, Double, Double, (Double,Double,Double,Double)), Double ] = {

      Pregel

      g.cache()
      iterations = ctime

      println("> count:"+g.vertices.count)
      //g.vertices.take(2).foreach(println)

      println("> Start the Layout iteration: n=" + iterations + " (nr of iterations)." )
      var gs = g//shuffle( g )

      temperature = 0.1 * math.sqrt(area) // current temperature

      for(iteration <- 1 to iterations) {

        //println("> Temperature: (T=" + temperature + ")")
        // 大数据级别时不要collect操作，采用抽样 //
        // 大数据级别时不要collect操作，采用度高节点（重要节点） //

        val sim = gs.vertices.collect()

        val gRep = calcRepulsion( sim, gs )

        gRep.cache().checkpoint()

        val gBB = calcAttraction(gRep)

        gBB.cache().checkpoint()

        //var cnt = 0
        val vNewPositions = gBB.vertices.mapValues(

          (_, a) => {

            val nx = a._2
            val ny = a._3

            val d = Math.sqrt(nx * nx + ny * ny)
            val gf = 0.01f * k * gravitys * d

            //添加上重力
            val p = (a._4._1 - gf * nx / d) * speed / SPEED_DIVISOR
            val q = (a._4._2 - gf * ny / d) * speed / SPEED_DIVISOR

            val dist: Double = Math.sqrt( p * p + q * q)
            //println(s"p = $p , q = $q")
            if (dist > 0 ) {
              //cnt = cnt + 1
              //println(cnt)
              val limitedDist: Double = Math.min(maxDisplace * (speed / SPEED_DIVISOR), dist)
              //println(s"Math.min(${maxDisplace * (speed / SPEED_DIVISOR)}, $dist)   cnt = $iteration")
              val x = p / dist * limitedDist
              val y = q / dist * limitedDist
              //println(s"dx = ${a._2+x} dy = ${a._3+y}  cnt = $iteration")
              (a._1,a._2+x, a._3+y, (0.0, 0.0, a._4._3, a._4._4))
            }else{
              (a._1,a._2, a._3, (0.0, 0.0, a._4._3, a._4._4))
            }
        })

        gs = Graph(vNewPositions, g.edges, defaultNode)
        gs.cache().checkpoint()

        println(s"> This iteration( $iteration ) of computing layout zb has finished ...")

        //
        // 可以每次迭代都保存布局结果
        if(!REMOTE_JOB){
          dumpWithLayout(gs, output+"_of_"+iteration, diet)
        }
        cool(iteration)
      }

      gs
    }

    def initialAlgorithm(): Unit = {

      def date:String
      = new SimpleDateFormat("MM-dd_HH-mm_").format(System.currentTimeMillis())

      // 用户设定，定义输入输出，分隔符，及迭代次数，注意路径  //

      if(REMOTE_JOB){

        // 集群HDFS绝对路径
        fname = args(0)
        input = "hdfs://hadoop02:9000/SNAP/DATASET/"+fname
        output = "hdfs://hadoop02:9000/SNAP/OUTPUT/"+date+"dump_"+fname+"_"+args(1).toInt

        iterations = args(1).toInt

      }else{

        // 本地项目相对路径
        fname = "edges.txt"
        input = "resources\\"+fname
        output = "output\\"+fname

        iterations = 200

      }

      //iterations = 4  > Use Time: 278662ms
      //iterations = 8  > Use Time: 500043ms
      //iterations = 16 > Use Time: 500043ms


      // 静态量赋值，可微调，默认不需要，变量意义参加开头注释  //

      speed = 20.0              //等于1时无效，默认无效   = FR
      SPEED_DIVISOR = 800d    //速度除数默认值   = FR
      REP_SCALE = 1           //等于1时无效，默认无效
      ATT_SCALE = 1         //等于1时无效，默认无效
      gravitys = 3d          //向心力因子默认值    = FR
      epsilon = 0.001         //默认值，防止点重合时距离为0而不计算
      area = 10000            //布局大小。最好是次方值，长宽均开根号得到 = FR
      dbi = 0.2               //默认 [ 度筛选 ] 比率
      sbi = 0.1               //默认 [ 采样比 ]

      // 计算得到，默认不调整 //

      temperature = 0.1 * math.sqrt(area)
      AREA_MULTIPLICATOR = area //   = FR
      maxDisplace = Math.sqrt(AREA_MULTIPLICATOR * area) / 10.0 //最大移动距离   = FR


      // GraphX进行图数据载入 //

      val graphS = loadEdges( input )

      println(graphS.vertices.count())

      // 图顶点绑定自定义信息 //

      val cGraphS = convert( graphS ).persist()

      // 部分变量载入图后计算 //

      sizeOfGraph = cGraphS.vertices.count()
      val sizeEdg = cGraphS.edges.count()

      k = math.sqrt(area * AREA_MULTIPLICATOR / (sizeOfGraph +1) )// 防止点为0

      // 跳过图数据的采样流程 //

      // 跳过采样直接调用布局 //
      val end = layoutFDFR2(cGraphS, iterations ,diet = true )
      // 存文件
      if(REMOTE_JOB){
        end.vertices.saveAsTextFile( output+"_WithoutSample_Vertices_rst")
        end.edges.saveAsTextFile( output+"_WithoutSample_Edges_rst")
      }else{
        dumpWithLayout(end, output+"_WithoutSample_end", isFirst = false)
      }



      println(s"> Area               : $area")
      println(s"> Spring constant    : $k")
      println("> Graph data and area were prepared sucessfully.")
      println("> Graph layout finished.")
      println(s"> Size of the graph  : $sizeOfGraph  nodes, $sizeEdg edges.")
      println("> DONE!")

    }


  //def main(args: Array[String]) {

    // 承接闭合主函数，调用initialAlgorithm()对参数初始化

    val s1 = System.currentTimeMillis()

    initialAlgorithm()

    println(s"> Use Time: ${System.currentTimeMillis()-s1}ms")

    sc.stop()

  }

}


