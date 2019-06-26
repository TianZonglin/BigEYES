/*
import java.io._
import java.text.SimpleDateFormat

import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


object T2019626 {

  Logger.getLogger("org").setLevel(Level.ERROR)


  //type A = (String, (Float, Float), Map[String, (Float, Float)], Float, Int)

  def main(args: Array[String]): Unit = {
    mainF(Array("OK"))
  }

  def mainF(args: Array[String]): Unit = {



    val REMOTE_JOB = false
    val WIDTH = 0f

    // ---------------------

    def getConf: SparkConf = {
      if(REMOTE_JOB){
        new SparkConf()
          .setAppName("RemoteGraphX")
          .setMaster("spark://219.216.65.14:7077")
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

    // ---------------------
    def ran:Float = {
      val random = new scala.util.Random
      random.nextFloat() * 1000  - 500
    }
    val originGraph: Graph[Int, Int] = GraphLoader.edgeListFile(sc, "I:\\IDEA_PROJ\\Visualization\\resources\\facebook_combined.txt")
    // ---------------------
    val formatVertexRDD: RDD[(VertexId, (String, (Float, Float), Map[String, (Float, Float)], Float, Int))] =
      originGraph.vertices.map {
        v =>
          (v._1, (v._1.toString, (ran, ran), Map.empty, 0f, 0))
      }
    val formatEdgeRDD = originGraph.edges.map(e => Edge(e.srcId, e.dstId, 0f))
    val structuredGraph = Graph(formatVertexRDD, formatEdgeRDD, ("X", (0f, 0f), (0f, 0f), 0f, 0))
    structuredGraph.triplets.take(10).foreach(println)

    structuredGraph.persist()
    // ---------------------
    val sizeOfGraph = structuredGraph.vertices.count()
    val K = Math.sqrt(WIDTH * WIDTH / (sizeOfGraph +1)).toFloat

    val repl_1_N = Map()



    def dist(loc1:(Float, Float), loc2:(Float, Float)):Float = {
      val x = Math.pow((loc1._1 - loc2._1), 2)
      val y = Math.pow((loc1._2 - loc2._2), 2)
      Math.sqrt( x * x + y * y).toFloat
    }


    def vprog(vid: VertexId, vd: (String, (Float, Float), Map[String, (Float, Float)], Float, Int), msg: (Float, Float, Map[String, (Float, Float)])) = {
      if(msg._1 == 0 && msg._2 == 0 ){
        vd
      }else{
        val distance = dist(vd._2, msg)
        if( distance > 10){

          (vd._1, (msg._1, msg._2), vd._3, vd._4, vd._5)
        }else{
          vd
        }
      }
    }

    def sendMessage(edge: EdgeTriplet[(String, (Float, Float), Map[String, (Float, Float)], Float, Int), Float]): Iterator[(VertexId, (Float, Float, Map[String, (Float, Float)]))] = {

      if (edge.srcAttr._5 == 0 && edge.dstAttr._5 == 0) {
        val src = edge.srcAttr
        val dst = edge.dstAttr
        val xDist = src._2._1 - dst._2._1
        val yDist = src._2._2 - dst._2._2
        val d2 = (xDist * xDist + yDist * yDist)
        val d = Math.sqrt(d2)
        val attr = d2 / K
        val attrX = xDist / d * attr
        val attrY = yDist / d * attr

        var replX = 0f
        var replY = 0f

        edge.srcAttr._3.foreach( x =>{
          val v2 = x._2
          val xDist = edge.srcAttr._2._1 - x._2._1
          val yDist = edge.srcAttr._2._2 - x._2._2

          val dist = Math.sqrt(xDist * xDist + yDist * yDist).toFloat
          if (dist > 0) {
            val repl = K * K / dist
            // Force de répulsion
            replX += xDist / dist * repl // on l'applique...
            replY += yDist / dist * repl
            //println(s"$yDist / $dist * $repulsiveF")
          }

        })

        val forceX = attrX + replX
        val forceY = attrY + replY

        Iterator((edge.dstId, (forceX.toFloat,forceY.toFloat)))
      }else
        Iterator.empty

    }


    def mergeMsg(msg1:(Float, Float, Map[String, (Float, Float)]), msg2:(Float, Float, Map[String, (Float, Float)])): (Float, Float, Map[String, (Float, Float)]) = {
      if(msg1._1 == 0 && msg1._2 == 0 && msg2._1 == 0 && msg2._2 == 0){
        (0f,0f)
      }else{
        (msg1._1 + msg1._2, msg2._1 + msg2._2)
      }
    }

    val initialMessage = (0f, 0f)
    val pregelGraph = MyPregel(structuredGraph, initialMessage,
      100, EdgeDirection.Either)(
      vprog = vprog,
      sendMsg = sendMessage,
      mergeMsg = mergeMsg)

    println
    println
    pregelGraph.triplets.take(10).foreach(println)

    structuredGraph.unpersist()

  }



  def mainOlder(args: Array[String]) {

    val REMOTE_JOB: Boolean = false                 // 是否提交到集群运行
    var dbi: Double = 0d                               // 度筛选比率
    var sbi: Double = 0d                               // 图节点采样比率
    var fname: String = ""                          // 输入文件名
    var input: String = ""                          // 输入文件路径
    var output: String = ""                         // 输出文件路径
    var tab: String = ""                            // 输入文件的文本分隔符
    var wide: Double = 5000d                       // 输入文件的节点大小
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
          .setMaster("spark://219.216.65.14:7077")
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
      dumpWithLayout(graphN, output+"_random", isFirst = true)
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
          if(isFirst) fn+s"_FIRST_$iterations.json"
          else fn+s"_SECOND_$iterations.json"
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





  def layoutFDFR1( g: Graph[ (String, Double, Double, (Double,Double,Double,Double)), Double ],
                   ctime: Int,
                   diet:Boolean)
  : Graph[ (String, Double, Double, (Double,Double,Double,Double)), Double ] = {
    val maxIterations = ctime

    val k =  math.sqrt(area / g.vertices.count())

    def vprog(vid:VertexId,attr:(String, Double, Double, (Double,Double,Double,Double)),message:(Double,Double)) = {

      var dispX = message._1
      var dispY = message._2
      val disp = sqrt(dispX*dispX+dispY*dispY)
      val temp = attr._2
      if(disp > temp) {
        if (dispX.isInfinity && dispY.isInfinity) {
          dispX = 1.414D * temp
          dispY = 1.414D * temp
        }
        else {
          if (dispX.isInfinity)
            dispX = temp
          else
            dispX = dispX / disp * temp
          if (dispY.isInfinity)
            dispY = temp
          else
            dispY = dispY / disp * temp
        }
      }
      var newX = attr._2 + dispX
      var newY = attr._3 + dispY

      if(newX < 0) newX = 0
      if(newX > wide) newX = wide
      if(newY < 0) newY = 0
      if(newY > wide) newY = wide
      //调试代码
      if(newX.isNaN)
        println("attr="+attr._2+"  dispx="+dispX+"  dispXX="+ message._1+"   disp"+disp)

      //0.85是退火算法的冷却系数
      (attr._1,newX,newY,(attr._4._1,attr._4._2,attr._4._3,temp*0.85F))

    }

    def sendMessage(e: EdgeTriplet[(String, Double, Double, (Double,Double,Double,Double)), Double]): Iterator[(VertexId, (Double,Double))] = {
      val xDist = e.srcAttr._2 - e.dstAttr._2
      val yDist = e.srcAttr._3 - e.dstAttr._3
      val dist = math.sqrt(xDist * xDist + yDist * yDist)

      var attr = 0D
      var attr_X = 0D
      var attr_Y = 0D


      val lst = if (e.srcAttr._1.contains(e.dstId)) {
        attr = pow(dist,2)/k
        //防止顶点重合
        if(e.srcAttr._2 == e.dstAttr._2 && e.srcAttr._3 == e.dstAttr._3) {
          attr_X = 0D
          attr_Y = 0D
        }
        else{
          attr_X = (-1) * (e.srcAttr._2 - e.dstAttr._2) / (dist + 0.00001D) * attr
          attr_Y = (-1) * (e.srcAttr._3 - e.dstAttr._3) / (dist + 0.00001D) * attr
        }

        //计算关联紧密顶点间的斥力
        val repl = pow(k,2)/dist
        var repl_X = 0D
        var repl_Y = 0D
        //防止顶点重合
        if(e.srcAttr._2 == e.dstAttr._2 && e.srcAttr._3 == e.dstAttr._3) {
          if (e.srcId < e.dstId) {
            repl_X = (e.srcAttr._2 - e.dstAttr._2 + 0.00001D) / (dist + 0.00001D) * repl
            repl_Y = (e.srcAttr._3 - e.dstAttr._3 + 0.00001D) / (dist + 0.00001D) * repl
          }
          else{
            repl_X = (e.srcAttr._2 - e.dstAttr._2 - 0.00001D) / (dist + 0.00001D) * repl
            repl_Y = (e.srcAttr._3 - e.dstAttr._3 - 0.00001D) / (dist + 0.00001D) * repl
          }
        }
        else{
          repl_X = (e.srcAttr._2 - e.dstAttr._2) / dist * repl
          repl_Y = (e.srcAttr._3 - e.dstAttr._3) / dist * repl
        }

        //计算合力
        val forceX = attr_X + repl_X
        val forceY = attr_Y + repl_Y
        //调试代码
        /*        println("("+e.srcAttr._2._1+"-"+e.dstAttr._2._1+")/"+"("+distance+"+"+0.00001D+")*"+attract)
                println(repulsive)
                println(e.srcId+"$$"+e.dstId)
                println("attractX="+attractX+"   repulsiveX"+repulsiveX)
                println("attractY="+attractY+"   repulsiveY"+repulsiveY)*/
        if(forceX.isNaN){
          println( attr_X + "  :  "+repl_X)
        }
        if(attr_X.isNaN)
          println((e.srcAttr._2 - e.dstAttr._3)+"/" + dist + "*" + attr )
        if(repl_X.isNaN)
          println((e.srcAttr._2 - e.dstAttr._3)+"/" + dist + "*" + repl )

        List((e.srcId,(forceX,forceY)))


        //List((e.dstId, (1, e.dstAttr)), (e.srcId, (0, e.srcAttr)))
      //} else if (edge.dstAttr == 0) {
      //  List((edge.srcId, (1, edge.srcAttr)), (edge.dstId, (0, edge.dstAttr)))
      }
      else List.empty

      // no need to send msg to nodes that have already been "removed"
      // val lstFiltered = lst.filter(   _._2._2 >= 0).map(t => (t._1, t._2._1))

      lst.iterator

    }


    def mergeMessage(msg1:(Double,Double),msg2:(Double,Double)):(Double,Double)={
      var x = msg1._1 + msg2._1
      var y = msg1._2 + msg2._2
      if((msg1._1.isPosInfinity&&msg2._1.isNegInfinity)||(msg1._1.isNegInfinity&&msg2._1.isPosInfinity))
        x = math.random
      if((msg1._2.isPosInfinity&&msg2._2.isNegInfinity)||(msg1._2.isNegInfinity&&msg2._2.isPosInfinity))
        y = math.random
      (x,y)
    }


    val initialMessage = (0D,0D)
    val pregelGraph = Pregel(g, initialMessage, maxIterations, EdgeDirection.Either)(
      vprog = vprog,
      sendMsg = sendMessage,
      mergeMsg = mergeMessage
    )
    g.unpersist()

    //pregelGraph.mapVertices[Boolean]((id: VertexId, d: Int) => d > 0)
    pregelGraph
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
        //cool(iteration)
      }

      gs
    }

    def wholeCompute(g:Graph[(String, Double, Double, (Double, Double, Double, Double)), Double])
    :Unit={
      g.cache()
      g.checkpoint()
/*****/
      // 计算图的度分布，并排序

      val maxd = g.degrees.map( x => { (x._2,1) }
      ).reduceByKey(

        (a,b) => a+b

      ).sortBy(x => x._1)

      // 度划分比例

      val dbi:Double = 0.2d

      val ddgree = maxd.take((maxd.count() * dbi ).toInt)((maxd.count()*dbi).toInt-1)._2

      // degree打标记，0.0 为false，意思是没有被抽到，3.0 是度筛选节点
      val gbd = g.vertices.innerJoin(g.degrees)(

        (_,a,b) => {

          if(b >= ddgree) {

            (a._1, a._2, a._3, (a._4._1, a._4._2, b.toDouble, 3.0)) // 大节点不参与标记
          }else{

            (a._1, a._2, a._3, (a._4._1, a._4._2, b.toDouble, 0.0)) // 其余初始为没抽到
          }

        }
      )

      // 会过滤掉 degree == 0 ，返回类原图
      val ddGraphS = Graph(gbd, g.edges, defaultNode)

      // 一阶图（全部第一次布局 + 抽样部分第一次布局 ）在后面获得

      // 剩余二阶图（剩余的第二次布局）返回子图
      val subGraph_other = ddGraphS.subgraph(

        vpred = {

          (_,v) => v._4._4 < ddgree
        }
      )

      val simpleV = subGraph_other.vertices.innerJoin(
        subGraph_other.vertices.sample( withReplacement = false, sbi )
      )(

        (_,a,_) => {

          (a._1, a._2, a._3, (a._4._1, a._4._2, a._4._3, 1.0))
        }
      )

      //ddGraphS 已经打完标记，标记了在 subGraph_2中 上次sample时采到的点,返回类原图
      val g2_withall = ddGraphS.joinVertices(simpleV)(

        (_,_,b) => b
      )

      // 计算第一次的图，[ 度 >= degree ] and [ 标记为 1.0 ]
      val g_first = g2_withall.subgraph(

        vpred = {

          (_,v) => v._4._3 >= ddgree || v._4._4 == 1.0
        }
      )

      //两次布局
      val iter = (iterations * 0.5).toInt

      //第一次布局 过滤的度20% + 剩余的20%
      val layout1 = layoutFDFR2( g_first, iter , diet = true) //true 为第一次布局

      // 存文件
      if(REMOTE_JOB){
        // 集群提交只保存最后结果
        layout1.vertices.saveAsTextFile( output+"_1_Vertices_rst")
        layout1.edges.saveAsTextFile( output+"_1_Edges_rst")
      }else{
        dumpWithLayout(layout1, output+"_end", isFirst = true)
      }

      // 用第一次布局后的坐标覆盖 原图中节点 得到 g_second
      val g_second = g2_withall.joinVertices(layout1.vertices)(
        (_,_,b) => b
      )

      //第二次布局 剩余节点，第一次布局的结果保留
      val end = layoutFDFR2( g_second, iter ,  diet = false)//false 为第二次布局

      // 存文件
      if(REMOTE_JOB){
        end.vertices.saveAsTextFile( output+"_2_Vertices_rst")
        end.edges.saveAsTextFile( output+"_2_Edges_rst")
      }else{
        dumpWithLayout(end, output+"_end", isFirst = false)
      }

      g.unpersist(blocking = false)

      //无返回

    }


    def initialAlgorithm(): Unit = {

      def date:String
      = new SimpleDateFormat("MM-dd_HH-mm_").format(System.currentTimeMillis())

      // 用户设定，定义输入输出，分隔符，及迭代次数，注意路径  //

      tab = " " /****** 一 定 要 设 置 ********/

      if(REMOTE_JOB){

        // 集群HDFS绝对路径
        fname = args(0)
        input = "hdfs://219.216.65.14:9000/SNAP/DATASET/"+fname
        output = "hdfs://219.216.65.14:9000/SNAP/OUTPUT/"+date+"dump_"+fname+"_"+args(1).toInt

        iterations = args(1).toInt

      }else{

        // 本地项目相对路径
        fname = "simple5.txt"
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

      // 进入图数据的采样流程 //

      //wholeCompute(cGraphS)

      val end = layoutFDFR1(cGraphS, iterations ,diet = true )
      dumpWithLayout(end, output+"_random", isFirst = true)
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




*/