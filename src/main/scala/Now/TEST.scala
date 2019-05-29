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
    val a = sc.parallelize(List((1111,4),(2222,4),(3333,5),(4444,5)))
    a.collect.foreach(x=>(print(" "+x)))
    val b = a.flatMap(x=>1 to x)



    sc.stop()

  }

}


