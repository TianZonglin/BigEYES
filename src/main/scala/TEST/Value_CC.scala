package TEST

import TEST.Staff_CC._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx.{Graph, GraphLoader, _}
import org.apache.spark.{SparkConf, SparkContext}



/**
  * @version
  *     2.5
  * @author
  *     Sixun Ouyang
  * @define
  *     This program will compute the global, local, and average clustering coefficient of a graph
  * */

object Value_CC{

  type Distance = Double
  /**
    * main function, change the csv file path and the csv file
    *
    * */
  def main(args: Array[String]): Unit = {
    /**
      * turn off the log information and set master
      * */
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)
    @transient
    val conf = new SparkConf().setMaster("local").setAppName("ClusteringCoefficient")
    @transient
    val sc = new SparkContext(conf)

    /**
      * set path
      * TO RUN THIS PROGRAM, PLEASE DOWNLOAD THE CSV TO LET IT IN THE SAME FILE WITH THIS SCALA SCRIPT
      * */
    val path: String = "I:\\IDEA_PROJ\\Visualization\\src\\main\\scala\\TEST\\Staff_CC\\s10.csv"
    val graph = GraphLoader.edgeListFile(sc, path, numEdgePartitions = 8)

    /**
      * get the local, global and average clustering coefficient
      * */
    val cc = countCC(graph)

    println("The global clustering coefficient of this graph is " + cc._2)
    println("The average clustering coefficient of this graph is " + cc._3)




    // Specify the target size for the underlying clustering algorithm
    // DiameterApproximation.run(g, target=5000)

    // Control the number of active nodes/edges in each step
    // DiameterApproximation.run(g, delta=0.5)

    // Both parameters can be specified simultaneously
    // DiameterApproximation.run(g, target=5000, delta=0.5)

  }

  /**
    * calculate local, global and average clustering coefficient
    * param of pregel one (triplets):
    *      the first one (Int) represents the degree of each vertices
    *      the second one (Int) represents the connected triplets of each vertices
    *
    * */
  def countCC(graph: Graph[Int, Int]): (Graph[(Int, Int, Double), PartitionID], Double, Double) = {
    /**
      * calculate the connected triangles, i.e. triplets
      * */
    val triplets = new Triplets(graph).tripletsGraph

    /**
      * convert the graph, owing to the circuit of the graph will arouse pregel
      * */
    val util = new Util()
    val rawGraph = util.removeLoop(graph)

    /**
      * calculate the triangles
      * */
    val triangles = new Triangles(rawGraph).triangleGraph

    /**
      * calculate the local, global and average clustering coefficient
      * */
    val cc = util.calculateCC(triplets, triangles)
    (cc._1, cc._2, cc._3)
  }
}