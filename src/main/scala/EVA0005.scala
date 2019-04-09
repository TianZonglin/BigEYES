import java.io._
import java.text.SimpleDateFormat

import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


object EVA0005 {

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
    var sudu: Double = 1d                           // 作用于 updatePos
    var maxDisplace: Double =0d                     // 最小移动距离，作用于 updatePos
    var SPEED_DIVISOR: Double = 0d                  // 关于移动速度，作用于 updatePos
    val defaultNode                                    // 默认节点的绑定结构
      = ("o", 0.0, 0.0, (0.0, 0.0, 0.0, 0.0))

    def getWidth: Double = Math.sqrt(area)
    def getHeight: Double = Math.sqrt(area)

    def getConf: SparkConf = {
      /**
       * @method: getConf
       * @params: null
       * @return: org.apache.spark.SparkConf
       * @date: 2018/6/4
       * @change: 2:10
       * @description: 区分方式获取SparkConf
       */
       
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
      /**
       * @method: ran
       * @params:
       * @return: Double
       * @date: 2018/6/3
       * @change: 22:47
       * @description: 获得随机数，范围在 (-+getWidth * 0.1)*0.2)内
       */

      val random = new scala.util.Random

      val r = random.nextDouble * getWidth * 0.2 - getWidth * 0.1

      if(REMOTE_JOB){

        r

      }else{

        if( (r > (- getWidth * 0.1)*0.2) && (r < (getWidth * 0.1)*0.2)) ran

        else r
      }

    }

    def loadEdges(fn: String): Graph[Any, String] = {
      /**
       * @method: loadEdges
       * @params: [fn]
       * @return: Graph[Any, _root_.scala.Predef.String]
       * @date: 2018/6/3
       * @change: 22:49
       * @description: 根据 边文件 加载图数据，省略 # 开头注释字符
       */

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
      /**
       * @method: convert
       * @params: [g]
       * @return: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double]
       * @date: 2018/6/4
       * @change: 2:18
       * @description: 向图顶点绑定添加自定义信息 (顶点ID, 坐标X, 坐标Y, (中间值X, 中间值Y, 顶点度, 顶点级别))
       */

      val transformedShuffledNodes: RDD[(VertexId, (String, Double, Double, (Double, Double, Double, Double)))] =
        g.vertices.map {
          v =>
            (v._1, (v._1.toString, ran, ran, (0.0, 0.0, 0.0, 0.0)))
        }

      val transformedEdges: RDD[Edge[Double]] = g.edges.map(e => Edge(e.srcId, e.dstId, e.attr.toDouble))

      val graphN = Graph(transformedShuffledNodes, transformedEdges, defaultNode)

      graphN

    }

    class Vector(var x: Double = 0.0, var y: Double = 0.0) {
      /**
        * @class: Vector
        * @params: [x, y]
        * @date: 2018/6/4
        * @change: 2:28
        * @description: 重定义 Vector 操作符，加减乘除等
        */

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

      def lenght: Double = math.sqrt(x * x + y * y)

      def asString: String = {
        "x=" + x + " y=" + y + "   l=" + this.lenght
      }

    }

    def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
      /**
       * @method: printToFile
       * @params: [f, op]
       * @return: (_root_.java.io.PrintWriter => Unit) => Unit
       * @date: 2018/6/3
       * @change: 23:18
       * @description: 内置函数，直接令输出重定向到文件，输出json格式
       */

      val p = new java.io.PrintWriter(f)

      try {

        op(p)

      } finally {

        p.close()

      }

    }

    def cool(iteration: Int): Unit = {
      /**
       * @method: cool
       * @params: [iteration]
       * @return: Unit
       * @date: 2018/6/4
       * @change: 1:18
       * @description: 模拟退火过程
       */

      temperature = (1.0 - (iteration.toDouble / iterations)) * 0.1 * math.sqrt(area)

    }

    def dumpWithLayout(g: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double],
                       fn: String,
                       isFirst: Boolean)
    : Unit = {
      /**
       * @method: dumpWithLayout
       * @params: [g, fn, pref]
       * @return: Unit
       * @date: 2018/6/4
       * @change: 1:33
       * @description: 布局转存为文件
       */

        if(REMOTE_JOB){

          // 提交集群不进行每次的存文件

        }else{

          val furl = {
            if(isFirst) fn+s"_layout_first_$iterations.json"
            else fn+s"_layout_second_$iterations.json"
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
      }

/*
    def shuffle(g: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double])
    : Graph[(String, Double, Double, (Double, Double, Double, Double)), Double] = {
      /**
       * @method: shuffle
       * @params: [g]
       * @return: _root_.org.apache.spark.graphx.Graph[(_root_.scala.Predef.String, Double, Double, (Double, Double, Double, Double)), Double]
       * @date: 2018/6/3
       * @change: 2:12
       * @description: 初始shuffle过程，重新随机节点位置
       */
      val shuffledNodes: RDD[(VertexId, (String, Double, Double, (Double, Double, Double, Double)))]
        = g.vertices.map {
          v => {
            val random = new scala.util.Random
            (v._1, (v._2._1, random.nextDouble * getWidth, random.nextDouble * getHeight, v._2._4))
          }
        }

      val graphN = Graph(shuffledNodes, g.edges, defaultNode)

      graphN

    }

    def between(min: Double, value: Double, max: Double): Double = {
      /**
       * @method: between
       * @params: [min, value, max]
       * @return: Double
       * @date: 2018/6/3
       * @change: 2:13
       * @description: 节点判断函数
       */
      if (value < min) {
        return min
      }
      if (value > max) {
        return max
      }
      value
    }
*/

    def updatePosSecond(a: (String, Double, Double, (Double, Double, Double, Double)))
    : (String, Double, Double, (Double, Double, Double, Double)) = {
      /**
       * @method: updatePosSecond
       * @params: [a]
       * @return: (String, Double, Double, (Double, Double, Double, Double))
       * @date: 2018/6/3
       * @change: 22:48
       * @description: 更新节点位置XY，重置中间值XY，第二次布局时的更新
       *               新增：如果节点等级为0.0，才进行布局，保留第一次布局结果
       */

      val p = a._4._1
      val q = a._4._2
      val z = a._4._4

      var dist: Double = Math.sqrt( p * p + q * q)

      if(dist == 0 || dist.equals(Double.NaN))

        dist = (new scala.util.Random).nextDouble()*getHeight* 0.2 //随机一个距离，正值

      if (dist > 0 && (z == 0.0d) ){

        val limitedDist: Double = Math.min(maxDisplace * (sudu / SPEED_DIVISOR), dist)

        val x = p / dist * limitedDist
        val y = q / dist * limitedDist

        (a._1,a._2+x, a._3+y, (0.0, 0.0, a._4._3, a._4._4))

      }else{

        (a._1,a._2, a._3, (0.0, 0.0, a._4._3, a._4._4))

      }

    }

    def updatePosFirst(a: (String, Double, Double, (Double, Double, Double, Double)))
    : (String, Double, Double, (Double, Double, Double, Double)) = {
      /**
        * @method: updatePosFirst
        * @params: [a]
        * @return: (String, Double, Double, (Double, Double, Double, Double))
        * @date: 2018/6/4
        * @change: 1:55
        * @description: 更新节点位置XY，重置中间值XY，第一次布局时的更新
        */

      val p = a._4._1
      val q = a._4._2

      var dist: Double = Math.sqrt( p * p + q * q)

      if(dist == 0 || dist.equals(Double.NaN))

        dist = (new scala.util.Random).nextDouble()*getHeight* 0.2 //随机一个距离，正值

      if (dist > 0 ) {

        val limitedDist: Double = Math.min(maxDisplace * (sudu / SPEED_DIVISOR), dist)

        val x = p / dist * limitedDist
        val y = q / dist * limitedDist

        (a._1,a._2+x, a._3+y, (0.0, 0.0, a._4._3, a._4._4))

      }else{

        (a._1,a._2, a._3, (0.0, 0.0, a._4._3, a._4._4))

      }

    }

    def preUpdatePos(a: (String, Double, Double, (Double, Double, Double, Double)),
                     b: (Double, Double))
    : (String, Double, Double, (Double, Double, Double, Double)) = {
      /**
       * @method: preUpdatePos
       * @params: [a, b]
       * @return: (String, Double, Double, (Double, Double, Double, Double))
       * @date: 2018/6/4
       * @change: 2:41
       * @description: 更新中间值XY
       */

      (a._1, a._2, a._3, (a._4._1 + b._1, a._4._2 + b._2, a._4._3, a._4._4))

    }


    def repulsionForce(mp: ((Double, Double), (Double, Double)))
    : (Double, Double) = {
      /**
       * @method: repulsionForce
       * @params: [mp]
       * @return: (Double, Double)
       * @date: 2018/6/3
       * @change: 22:10
       * @description: 计算
       */

      val v1 = new Vector(mp._1._1, mp._1._2)
      val v2 = new Vector(mp._2._1, mp._2._2)

      val delta = v1 - v2

      val deltaLength = math.max(epsilon, delta.lenght)

      val force = k * k / deltaLength

      val disp = (delta / deltaLength) * force  * REP_SCALE

      (disp.x, disp.y)

    }



    def attractionForce(mp: ((Double, Double), (Double, Double)))
    : (Double, Double) = {
      /**
       * @method: attractionForce
       * @params: [mp]
       * @return: (Double, Double)
       * @date: 2018/6/3
       * @change: 23:13
       * @description:
       */

      val source = new Vector(mp._1._1, mp._1._2)
      val target = new Vector(mp._2._1, mp._2._2)

      val delta = source - target

      val deltaLength = math.max(epsilon, delta.lenght) // avoid x/0

      val force = deltaLength * deltaLength / k

      val disp = (delta / deltaLength) * force  *  ATT_SCALE

      (disp.x, disp.y)

    }



    def attractionForceInverted(mp: ((Double, Double), (Double, Double)))
    : (Double, Double) = {
      /**
       * @method: attractionForceInverted
       * @params: [mp]
       * @return: (Double, Double)
       * @date: 2018/6/3
       * @change: 22:36
       * @description:
       */

      val v1 = new Vector(mp._1._1, mp._1._2)
      val v2 = new Vector(mp._2._1, mp._2._2)

      val delta = v1 - v2

      val deltaLength = math.max(epsilon, delta.lenght) // avoid x/0

      val force = deltaLength * deltaLength / k

      val disp = (delta / deltaLength) * (-1.0 * force * ATT_SCALE)

      (disp.x, disp.y)

    }


    def calcRepulsion(
                       array: Array[(VertexId, (String, Double, Double, (Double, Double, Double, Double)))],
                       g: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double])
    : Graph[(String, Double, Double, (Double, Double, Double, Double)), Double] = {
      /**
       * @method: calcRepulsion
       * @params: [array, g]
       * @return: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double]
       * @date: 2018/6/3
       * @change: 23:28
       * @description: 计算图的全部节点的斥力，调用 repulsionForce()，逻辑有点问题
       */

      g.cache()

      var a = (0.0,0.0)

      val repV: VertexRDD[(Double, Double)] = g.aggregateMessages[(Double, Double)](

        sendMsg = {
          triplet => {
            triplet.sendToDst(triplet.srcAttr._2, triplet.srcAttr._3)
            triplet.sendToSrc(triplet.dstAttr._2, triplet.dstAttr._3)
          }
        },

        mergeMsg = {

          (m1, m2) => {

            def add(a:(Double,Double),b:(Double,Double)):(Double,Double) = (a._1+b._1,a._2+b._2)
            
            def mul(a:(Double,Double),b:(Double,Double)):(Double,Double) = (a._1*b._1,a._2*b._2)

            array.foreach(
                item=>{
                  a = add(a,repulsionForce( ((item._2._2,item._2._3),m2) ))
                }
            )

            add(a, mul((10d,10d),repulsionForce( (m1,m2) ) ))
          }
        }

      )

      val disp: VertexRDD[(Double, Double)] = repV.aggregateUsingIndex(

        repV,

        (p1, p2) => ( p1._1 + p2._1 , p1._2 + p2._2 )

      )

      val setC: VertexRDD[(String, Double, Double, (Double,Double,Double,Double))]
      = g.vertices.innerJoin(disp)(

        (_, a, b) => {

          if(b._1.equals(Double.NaN)||b._2.equals(Double.NaN)){

            preUpdatePos(a,(ran,ran))
          }else{

            preUpdatePos(a,b)
          }
        }
      )

      val graphN = Graph(setC, g.edges, defaultNode)

      g.unpersist(blocking = false)

      graphN

    }

    def calcAttraction(g: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double])
    : Graph[(String, Double, Double, (Double, Double, Double, Double)), Double] = {
      /**
       * @method: calcAttraction
       * @params: [g]
       * @return: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double]
       * @date: 2018/6/3
       * @change: 23:58
       * @description: 计算图的全部节点的引力，要计算双向引力，需要两次传递，逻辑有点问题
       */

      g.cache()
      g.checkpoint()

      val attr1: VertexRDD[(Double, Double)] = g.aggregateMessages[(Double, Double)](

        sendMsg = {
          triplet => {
            triplet.sendToDst(triplet.srcAttr._2, triplet.srcAttr._3)
            triplet.sendToSrc(triplet.dstAttr._2, triplet.dstAttr._3)
          }
        },
        mergeMsg = {
          (m1, m2) => attractionForce( (m1,m2) )
        }
      )

      val attr2: VertexRDD[(Double, Double)] = g.aggregateMessages[(Double, Double)](

        sendMsg = {
          triplet => {
            triplet.sendToSrc(triplet.srcAttr._2, triplet.srcAttr._3)
            triplet.sendToDst(triplet.srcAttr._2, triplet.srcAttr._3)
          }
        },
        mergeMsg = {
          (m1, m2) => attractionForceInverted( (m1,m2) )
        }

      ) // inverted attraction to other component is now the VertexRDD

      val disp1: VertexRDD[(Double, Double)] = attr1.aggregateUsingIndex(
        attr1,
        (p1, p2) => ( p1._1 + p2._1 , p1._2 + p2._2 )
      )

      val disp2: VertexRDD[(Double, Double)] = attr2.aggregateUsingIndex(
        attr2,
        (p1, p2) => ( p1._1 + p2._1 , p1._2 + p2._2 )
      )

      val setC: VertexRDD[(String, Double, Double, (Double,Double,Double,Double))]
      = g.vertices.innerJoin(disp1)(

        (_, a, b) => {

          if(b._1.equals(Double.NaN)||b._2.equals(Double.NaN)){

            preUpdatePos(a,(ran,ran))
          }else{

            preUpdatePos(a,b)
          }
        }
      )

      val g2 = Graph(setC, g.edges, defaultNode)

      val setD: VertexRDD[(String, Double, Double, (Double,Double,Double,Double))]
      = g2.vertices.innerJoin(disp2)( (_, a, b) => {

        if(b._1.equals(Double.NaN)||b._2.equals(Double.NaN)){

          preUpdatePos(a,(ran,ran))
        }else{

          preUpdatePos(a,b)
        }
      } )

      val graphN = Graph(setD, g.edges, defaultNode)

      g.unpersist(blocking = false)

      graphN

    }


    def gravity(g: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double])
    : Graph[(String, Double, Double, (Double, Double, Double, Double)), Double] = {
      /**
       * @method: gravity
       * @params: [g]
       * @return: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double]
       * @date: 2018/6/3
       * @change: 23:47
       * @description: 计算图的全部节点的向心力，使图围绕 (0, 0) 移动迭代，按计算公式；
       */

      g.cache()
      g.checkpoint()

      val repV: VertexRDD[(Double, Double)] = g.vertices.mapValues(

        v => {

          val d = Math.sqrt(v._4._1 * v._4._1 + v._4._2 * v._4._2)

          val gf = 0.01 * k * gravitys * d

          (v._4._1 - (gf * v._2 / d), v._4._2 - (gf * v._3 / d))
        }

      )


      val disp: VertexRDD[(Double, Double)] = repV.aggregateUsingIndex(
        repV,
        (p1, p2) => ( p1._1 + p2._1 , p1._2 + p2._2 )
      )

      val setC: VertexRDD[(String, Double, Double, (Double,Double,Double,Double))] = g.vertices.innerJoin(disp)(

        (_, a, b) => {

          if(b._1.equals(Double.NaN)||b._2.equals(Double.NaN)){

            preUpdatePos(a,(ran,ran))
          }else{

            preUpdatePos(a,b)
          }

        }
      )

      val graphN = Graph(setC, g.edges, defaultNode)

      g.unpersist(blocking = false)

      graphN

    }

    def layoutFDFR2( g: Graph[ (String, Double, Double, (Double,Double,Double,Double)), Double ],
                     ctime: Int,
                     diet:Boolean)
    : Graph[ (String, Double, Double, (Double,Double,Double,Double)), Double ] = {
      /**
       * @method: layoutFDFR2
       * @params: [g, ctime, diet]
       * @return: Graph[(String, Double, Double, (Double, Double, Double, Double)), Double]
       * @date: 2018/6/4
       * @change: 2:17
       * @description: 布局入口，按次迭代，每次经历 斥力、引力、向心力 三个计算步骤
       */

      g.cache()
      iterations = ctime

      println("> Layout Start ...")
      //g.vertices.take(2).foreach(println)

      println("> Start the Layout iteration: n=" + iterations + " (nr of iterations)." )
      var gs = g//shuffle( g )

      temperature = 0.1 * math.sqrt(area) // current temperature

      for(iteration <- 1 to iterations) {

        println("> Temperature: (T=" + temperature + ")")

        var sim = gs.vertices.takeSample(withReplacement = false,(sizeOfGraph*0.01).toInt)
        // 大数据级别时不要collect操作，采用抽样 //

        println("> xx - xxxxxxxxxxxxxx")

        if(!REMOTE_JOB){
          sim = gs.vertices.collect()
        }

        val gRep = calcRepulsion( sim, gs )

        println("> Repulsion computing finished ... ")
        //gRep.vertices.take(10).foreach(println)

        gRep.cache().checkpoint()

        val gBB = calcAttraction(gRep)

        println("> Attraction computing finished ... ")
        //gBB.vertices.take(10).foreach(println)

        gBB.cache().checkpoint()

        val gAttr = gravity(gBB)

        println("> Gravity computing finished ... ")
        //gAttr.vertices.take(10).foreach(println)

        gAttr.cache().checkpoint()

        val vNewPositions = gAttr.vertices.mapValues(

          (_, v) => {

            if(diet) updatePosFirst(v)
            else updatePosSecond(v)
        })

        gs = Graph(vNewPositions, g.edges, defaultNode)

        gs.cache().checkpoint()
/*
        gs.vertices.mapValues((_, v) => {

          if(v._4._1.equals(Double.NaN)||v._4._2.equals(Double.NaN)){

            updatePos((v._1, v._2, v._3, (ran, ran, v._4._3, v._4._4)))
          }else{

            updatePos(v)
          }

        })
*/
        println(s"> This iteration($iteration) computing finished ...")

        //
        // 可以每次迭代都保存布局结果
        // dumpWithLayout(gs, output+iteration, diet)

        cool(iteration)

      }

      gs
    }

    def wholeCompute(g:Graph[(String, Double, Double, (Double, Double, Double, Double)), Double])
    :Unit={
      /**
       * @method: wholeCompute
       * @params: [g]
       * @return: Unit
       * @date: 2018/6/4
       * @change: 2:05
       * @description: 采样算法流程，经历 度筛选，采样，一次布局（度筛选+采样），二次布局（剩余）
       */

      g.cache()
      g.checkpoint()

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
      /**
       * @method: initialAlgorithm
       * @params:
       * @return: Unit
       * @date: 2018/6/4
       * @change: 1:33
       * @description: 初始化参数，整个程序的执行入口，
       *               先导入图数据、绑定顶点信息，然后进入采样流程 wholeCompute()
       */

      def date:String
      = new SimpleDateFormat("MM-dd_HH-mm-ss_").format(System.currentTimeMillis())

      // 用户设定，定义输入输出，分隔符，及迭代次数，注意路径  //

      tab = "\t"

      if(REMOTE_JOB){

        // 集群HDFS绝对路径
        fname = args(0)
        input = "hdfs://219.216.65.14:9000/SNAP/DATASET/"+fname
        output = "hdfs://219.216.65.14:9000/SNAP/OUTPUT/"+date+"dump_"+fname

        iterations = args(1).toInt

      }else{

        // 本地项目相对路径
        fname = "simple5.txt"
        input = "resources\\"+fname
        output = "output\\dump_"+fname

        iterations = 10
      }



      // 4 > Use Time: 232625ms
      // 8 > Use Time: 433044ms
      // 静态量赋值，可微调，默认不需要，变量意义参加开头注释  //

      sudu = 1.0              //等于1时无效，默认无效
      SPEED_DIVISOR = 500d    //速度除数默认值
      REP_SCALE = 1           //等于1时无效，默认无效
      ATT_SCALE = 100         //等于1时无效，默认无效
      gravitys = 10d          //向心力因子默认值
      epsilon = 0.001         //默认值，防止点重合时距离为0而不计算
      area = 10000            //布局大小。最好是次方值，长宽均开根号得到
      dbi = 0.2               //默认 [ 度筛选 ] 比率
      sbi = 0.1               //默认 [ 采样比 ]

      // 计算得到，默认不调整 //

      temperature = 0.1 * math.sqrt(area)
      AREA_MULTIPLICATOR = area
      maxDisplace = Math.sqrt(AREA_MULTIPLICATOR * area) / 10.0


      // GraphX进行图数据载入 //

      val graphS = loadEdges( input )

      println(graphS.edges.count())

      // 图顶点绑定自定义信息 //

      val cGraphS = convert( graphS ).persist()

      // 部分变量载入图后计算 //

      sizeOfGraph = cGraphS.vertices.count()
      val sizeEdg = cGraphS.edges.count()

      k = 0.8 * math.sqrt(area * AREA_MULTIPLICATOR / (sizeOfGraph +1) )// 防止点为0

      // 进入图数据的采样流程 //

      wholeCompute(cGraphS)

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


