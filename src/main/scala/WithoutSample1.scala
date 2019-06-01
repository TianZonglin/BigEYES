import java.io._
import java.lang.Math.{pow, random, sqrt}
import java.text.SimpleDateFormat

import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat
import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.reflect.ClassTag


object WithoutSample1 {

  Logger.getLogger("org").setLevel(Level.ERROR)

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

    val Width: Double = 500
    val Height: Double = 500

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

      val r = random.nextDouble * 1000  - 500

      if(REMOTE_JOB){

        r

      }else{

        //if( (r > (- getWidth * 0.1)*0.2) && (r < (getWidth * 0.1)*0.2)) ran
        r
        //else r
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
            (v._1, (v._1.toString, ran, ran, (0.0, 0.0, temperature, 0.0)))
        }

      val transformedEdges: RDD[Edge[Double]] = g.edges.map(e => Edge(e.srcId, e.dstId, e.attr.toDouble))

      val graphN = Graph(transformedShuffledNodes, transformedEdges, defaultNode)
      //graphN.vertices.foreach(println)
      dumpWithLayout(graphN, output+"_random", isFirst = true)
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

/*    def updatePosSecond(a: (String, Double, Double, (Double, Double, Double, Double)))
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
        //println(s"Math.min(${maxDisplace * (sudu / SPEED_DIVISOR)}, $dist)")
        val x = p / dist * limitedDist
        val y = q / dist * limitedDist

        (a._1,a._2+x, a._3+y, (0.0, 0.0, a._4._3, a._4._4))

      }else{

        (a._1,a._2, a._3, (0.0, 0.0, a._4._3, a._4._4))

      }

    }*/

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

      val dist: Double = Math.sqrt( p * p + q * q)

      if(dist == 0 || dist.equals(Double.NaN)){
        //dist = (new scala.util.Random).nextDouble()*getHeight* 0.2 - getHeight* 0.1 //随机一个距离，正值
      }

      if (dist > 0 ) {

        val limitedDist: Double = Math.min(maxDisplace * (speed / SPEED_DIVISOR), dist)
        //println(s"Math.min(${maxDisplace * (sudu / SPEED_DIVISOR)}, $dist)")
        val x = p / dist * limitedDist
        val y = q / dist * limitedDist

        (a._1,a._2+x, a._3+y, (0.0, 0.0, a._4._3, a._4._4))

      }else{

        (a._1,a._2, a._3, (0.0, 0.0, a._4._3, a._4._4))

      }

    }

    def preUpdatePosRep(a: (String, Double, Double, (Double, Double, Double, Double)),
                     b: (Double, Double))
    : (String, Double, Double, (Double, Double, Double, Double)) = {
      println(s"斥力 == (${a._1}, ${a._2}, ${a._3}, (${a._4._1} + ${b._1}, ${a._4._2} + ${b._2}, ${a._4._3}, ${a._4._4}))")
      (a._1, a._2, a._3, (a._4._1 + b._1, a._4._2 + b._2, a._4._3, a._4._4))

    }
    def preUpdatePosAtt(a: (String, Double, Double, (Double, Double, Double, Double)),
                        b: (Double, Double))
    : (String, Double, Double, (Double, Double, Double, Double)) = {
      //println(s"引力 ==  (${a._1}, ${a._2}, ${a._3}, (${a._4._1} + ${b._1}, ${a._4._2} + ${b._2}, ${a._4._3}, ${a._4._4}))")
      (a._1, a._2, a._3, (a._4._1 + b._1, a._4._2 + b._2, a._4._3, a._4._4))

    }
    def preUpdatePosGri(a: (String, Double, Double, (Double, Double, Double, Double)),
                        b: (Double, Double))
    : (String, Double, Double, (Double, Double, Double, Double)) = {
      println(s"向心力 ==  (${a._1}, ${a._2}, ${a._3}, (${a._4._1} + ${b._1}, ${a._4._2} + ${b._2}, ${a._4._3}, ${a._4._4}))")
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

      val dist = math.max(epsilon, delta.lenght)
      println(s"math.max($epsilon, ${delta.lenght})")
      val repulsiveF  = k * k / dist

      val disp = (delta / dist) * repulsiveF * REP_SCALE
      println("repulsive = "+(disp.x, disp.y))
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
      println(s"math.max($epsilon, ${delta.lenght})")
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
      println(s"math.max($epsilon, ${delta.lenght})")
      val force = deltaLength * deltaLength / k

      val disp = (delta / deltaLength) * (-1.0 * force * ATT_SCALE)

      (disp.x, disp.y)

    }

    def add(a:(Double,Double),b:(Double,Double)):(Double,Double) = (a._1+b._1,a._2+b._2)

    //def mul(a:(Double,Double),b:(Double,Double)):(Double,Double) = (a._1*b._1,a._2*b._2)

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
      //println("-----disp1 "+g.vertices.count)
      //正序只有36个
      //g.vertices.foreach(println)


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
/*      println("================  attr1")
      attr1.foreach(println)
      println
      println*/
      val after1 = g.joinVertices(attr1)(
        (_, a, b) => {
          //if(b._1.equals(Double.NaN)||b._2.equals(Double.NaN)){
          // println("===================================")
          //preUpdatePosAtt(a,(ran,ran))
          //}else{
          (a._1, a._2, a._3, (a._4._1 + b._1, a._4._2 + b._2, a._4._3, a._4._4))
          //}
        }
      )
/*      println("================  after1")
      after1.vertices.foreach(println)
      println
      println*/
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
/*      println("================  attr2")
      attr2.foreach(println)
      println
      println*/
      val after2 = after1.joinVertices(attr2)(
        (_, a, b) => {
          (a._1, a._2, a._3, (a._4._1 + b._1, a._4._2 + b._2, a._4._3, a._4._4))
        }
      )
/*      println("================  after2")
      after2.vertices.foreach(println)
      println
      println*/
/*      val disp1: VertexRDD[(Double, Double)] = attr1.aggregateUsingIndex(
        attr1,
        (p1, p2) => ( p1._1 + p2._1 , p1._2 + p2._2 )
      )
      println("-----------------------disp1 "+disp1.count)
      val disp2: VertexRDD[(Double, Double)] = attr2.aggregateUsingIndex(
        attr2,
        (p1, p2) => ( p1._1 + p2._1 , p1._2 + p2._2 )
      )
      println("------------------------------disp2 "+disp2.count)*/






/*      //println("-------------------------------------setC "+setC.count)
      val g2 = Graph(setC, g.edges, defaultNode)
      val setD: VertexRDD[(String, Double, Double, (Double,Double,Double,Double))]
      = g2.vertices.innerJoin(attr2)( (_, a, b) => {

          if(b._1.equals(Double.NaN)||b._2.equals(Double.NaN)){
            preUpdatePos(a,(ran,ran))
          }else{

            preUpdatePos(a,b)
          }
        }
      )*/
      //println("--------------------------------------------setD "+setD.count)
      //setC.foreach(println)


      //val graphN = Graph(all, g.edges, defaultNode)
/*      val graphN = g.joinVertices(setC)(
        (_,_,b) => b
      )*/
      //graphN.vertices.foreach(println)
      g.unpersist(blocking = false)

      //System.exit(0)
      after2

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

            preUpdatePosGri(a,(ran,ran))
          }else{

            preUpdatePosGri(a,b)
          }

        }
      )

      val graphN = Graph(setC, g.edges, defaultNode)

      g.unpersist(blocking = false)

      graphN

    }

    def layoutFDFR2( randomGraph: Graph[ (String, Double, Double, (Double,Double,Double,Double)), Double ],
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

      randomGraph.unpersist(blocking = true)
      //##调试代码
      /*    val randomGraph=graph.mapVertices((id,set)=>(set,{if(id==1){(3193.1953644410614,456.943409213304)}
            else if(id==4){(1537.7309068258644,2373.2497251986797)}
              else(random()*high,random()*wide)},temperature2 )  )*/

      randomGraph.persist()
      //randomGraph.triplets.saveAsTextFile("/pagerank/666")

      //randomGraph.vertices.saveAsTextFile("/pagerank/222")
      //计算距离
      def dist(location1:(Double,Double),location2:(Double,Double)):Double={
        sqrt(pow((location1._1-location2._1),2)+pow((location1._2-location2._2),2))
      }
      //计算引力
      def attractiveForce(distance:Double):Double={
        pow(distance,2)/k
      }
      //计算斥力
      def repulsiveForce(distance:Double):Double={
        pow(k,2)/distance
      }

      //#############a对b的引力即是，b对a的引力，斥力也一样，但是问题是，一个度很大的顶点不一定有有二街邻居，而度小的顶点的二阶邻居可能会包含这个度很大的顶点，度很大的店计算斥力只要计算其与一届邻居的斥力，而其他度小的要计算二阶邻居的斥力，这造成了互为邻居的点的力的计算不相等，但是对度小的顶点的力的计算已经完成，计算度大的顶点的力时是否可以考虑也计算二阶邻居的斥力？？
      def sendMessage(e:EdgeTriplet[(String, Double, Double, (Double, Double, Double, Double)), Double])={

        val Source = e.srcAttr
        val Destnt = e.dstAttr
        val distance = sqrt(pow((Source._2-Destnt._2),2)+pow((Source._3-Destnt._3),2))
        var attract=0D
        var attractX=0D
        var attractY=0D
        //只计算相邻节点间引力
        //if(Source._1.contains(e.dstId)) {
          attract = attractiveForce(distance)
          //防止顶点重合
          if(Source._2 == Destnt._2 && Source._3 ==Destnt._3) {
            attractX=0D
            attractY=0D
          }
          else{
            attractX=(-1)*(Source._2-Destnt._2)/(distance+0.00001D)*attract
            attractY=(-1)*(Source._3-Destnt._3)/(distance+0.00001D)*attract
          }
        //}
        //计算关联紧密顶点间的斥力
        val repulsive=repulsiveForce(distance)
        var repulsiveX=0D
        var repulsiveY=0D
/*        //防止顶点重合
        if(Source._2 == Destnt._2 && Source._3 == Destnt._3) {
          if (e.srcId < e.dstId) {
            repulsiveX=(Source._2-Destnt._2+0.00001D)/(distance+0.00001D)*repulsive
            repulsiveY=(Source._3-Destnt._3+0.00001D)/(distance+0.00001D)*repulsive
          }
          else{
            repulsiveX=(Source._2-Destnt._2-0.00001D)/(distance+0.00001D)*repulsive
            repulsiveY=(Source._3-Destnt._3-0.00001D)/(distance+0.00001D)*repulsive
          }
        }
        else{
          repulsiveX=(Source._2-Destnt._2)/distance*repulsive
          repulsiveY=(Source._3-Destnt._3)/distance*repulsive
        }*/


        //计算合力
        val forceX = attractX + repulsiveX
        val forceY = attractY + repulsiveY
        //调试代码
        /*        println("("+e.srcAttr._2._1+"-"+e.dstAttr._2._1+")/"+"("+distance+"+"+0.00001D+")*"+attract)
                println(repulsive)
                println(e.srcId+"$$"+e.dstId)
                println("attractX="+attractX+"   repulsiveX"+repulsiveX)
                println("attractY="+attractY+"   repulsiveY"+repulsiveY)
        if(forceX.isNaN){
          println( attractX + "  :  "+repulsiveX)
        }
        if(attractX.isNaN)
          println((e.srcAttr._2._1-e.dstAttr._2._1)+"/"+distance+"*"+attract)
        if(repulsiveX.isNaN)
          println((e.srcAttr._2._1-e.dstAttr._2._1)+"/"+distance+"*"+repulsive)
        (e.srcId,(forceX,forceY))
        //(e.dstId,e.srcAttr._2)*/
        println("发送     "+ e.srcId+"<-"+e.dstId,(forceX,forceY))
        //println("发送     "+ e.dstId+"<-"+e.srcId,(forceX,forceY))
        Iterator((e.srcId,(forceX,forceY)))
        //Iterator((e.dstId,(forceX,forceY)))
        //Iterator((e.dstId,(forceX,forceY)))
      }
      val initialMessage=(1D,1D)

      //更新顶点位置
      def vertexProgram(vid:VertexId,attr:(String, Double, Double, (Double, Double, Double, Double)),message:(Double,Double))={
        println(message)

        var dispX=message._1
        var dispY=message._2
        val disp=sqrt(dispX*dispX+dispY*dispY)
        val temp=attr._4._3
        if(disp>temp) {
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
        val newX=attr._2 + dispX
        val newY=attr._3 + dispY
        //if(newX<0){
        //  newX=0
        //}
        //if(newX> Width ){
        //  newX = Width
        //}
        //if(newY<0){
        //  newY=0
        //}
        //if(newY> Height){
        //  newY=Height
        //}

        //调试代码
        //if(newX.isNaN)
        //  println("attr="+attr._2._1+"  dispx="+dispX+"  dispXX="+dispXX+"   disp"+disp)
        if(message._1!=1.0 && message._2!=1.0){
          println("更新前       "+attr)
          println("更新后       "+(attr._1,newX,newY,(attr._2,attr._3,temp*0.85F,0D)))
          (attr._1,newX,newY,(attr._2,attr._3,temp*0.85F,0D))
        }else{
          //0.85是退火算法的冷却系数
          (attr._1,attr._2,attr._3,(newX,newY,temp*0.85F,0D))

        }


        //(attr._1,message,temp*0.85F)
      }
      def mergeMessage(msg1:(Double,Double),msg2:(Double,Double)):(Double,Double)={
        println("---Merge---")
        println("X= "+msg1._1+" + "+msg2._1)
        println("Y= "+msg1._2+" + "+msg2._2)
        var x=msg1._1+msg2._1
        var y=msg1._2+msg2._2
        if((msg1._1.isPosInfinity&&msg2._1.isNegInfinity)||(msg1._1.isNegInfinity&&msg2._1.isPosInfinity))
          x=random()
        if((msg1._2.isPosInfinity&&msg2._2.isNegInfinity)||(msg1._2.isNegInfinity&&msg2._2.isPosInfinity))
          y=random()
        (x,y)
      }

      Pregel(randomGraph,initialMessage,maxIterations=iterations,activeDirection=EdgeDirection.Either)(
        vprog=vertexProgram,
        sendMsg=sendMessage,
        mergeMsg=mergeMessage
      )
    }

Pregel

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
        fname = "simple55.txt"
        input = "resources\\"+fname
        output = "output\\"+fname

        iterations = 50

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

      //temperature = 0.1 * math.sqrt(area)
      temperature = 0.5 * math.sqrt(area)
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


      def Output[ED:ClassTag](graph: Graph[(Set[VertexId],(Double,Double),Float),ED]): Unit ={
        class VertexMultipleTextOutputFormat extends MultipleTextOutputFormat[Any, Any] {
          override def generateFileNameForKeyValue(key: Any, value: Any, name: String): String ={
            val v=value.asInstanceOf[(Double,Double)]
            val x=v._1
            val y=v._2
            val rowNum:Int=(x/50).asInstanceOf[Int]
            val culNum:Int=(y/50).asInstanceOf[Int]
            rowNum.toString+"_"+culNum.toString
          }
        }

        class EdgeMultipleTextOutputFormat extends MultipleTextOutputFormat[Any, Any] {
          override def generateFileNameForKeyValue(key: Any, value: Any, name: String): String ={
            val x=key.asInstanceOf[(Double,Double)]
            val rowNum:Int=(x._1/50).asInstanceOf[Int]
            val culNum:Int=(x._2/50).asInstanceOf[Int]
            rowNum.toString+"_"+culNum.toString
          }
        }
        val vertexs=graph.vertices.map(x=>(x._1,(x._2._2._1,x._2._2._2)))
        val edges=graph.triplets.map(x=>(x.srcAttr._2,x.dstAttr._2))
        vertexs.saveAsHadoopFile("C:\\Users\\msi\\Desktop\\outputVertexs",classOf[String],classOf[(Double,Double)],classOf[VertexMultipleTextOutputFormat])
        edges.saveAsHadoopFile("C:\\Users\\msi\\Desktop\\outputEdges",classOf[String],classOf[(Double,Double)],classOf[ EdgeMultipleTextOutputFormat])
      }
      cGraphS.vertices.take(300).foreach(println)

      dumpWithLayout(cGraphS, output+"_aaaa1", isFirst = true)

      //layoutFDFR2(cGraphS, iterations ,diet = true )
      val graph3=layoutFDFR2(cGraphS,iterations,diet = true).persist()

      dumpWithLayout(graph3, output+"_aaaa2", isFirst = true)
      graph3.vertices.take(300).foreach(println)
      println


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


