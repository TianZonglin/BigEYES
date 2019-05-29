package Now

import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}


object TEST {

  Logger.getLogger("org").setLevel(Level.WARN)

  def main(args: Array[String]) {

    val REMOTE_JOB: Boolean = false                 // 是否提交到集群运行
    def getConf: SparkConf = {
      if(REMOTE_JOB){
        printf("本\t地\t运\t行")
        new SparkConf()
          .setAppName("LocalGraphX")
          .setMaster("local")
      }else{
        printf("本\t地\t运\t行")
        new SparkConf()
          .setAppName("LocalGraphX")
          .setMaster("local")
      }
    }

    val sc = new SparkContext(getConf)
    val a = sc.parallelize(List((0,4),(2222,4),(3333,5),(4444,5),(4444,5),(4444,5)))
    a.collect.foreach(x=>(print(" "+x)))
    println
    val b = a.reduce((x,a) => (x._1+1,a._2))
    println(b)

    //def degreeHistogram(net: Graph[Person, Link]): Array[(Int, Int)] =
      //net.degrees.map(t => (t._2,t._1)).
      //  groupByKey.map(t => (t._1,t._2.size)).
      //  sortBy(_._1).collect()


    sc.stop()

  }

}


