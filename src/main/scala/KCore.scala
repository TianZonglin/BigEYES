import scala.language.implicitConversions
import scala.reflect.ClassTag

import org.apache.spark.graphx._



/** KCore algorithm. */
object KCore {

  def run[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED],
                                      k: Int,
                                      maxIterations: Int = Int.MaxValue): Graph[Boolean, ED] = {
    require(maxIterations > 0, s"> 0," )

    val kGraph = Graph[Int, ED](graph.degrees, graph.edges)
    def vprog(vid: VertexId, vd: Int, i: Int) = {
      val ret = if ((vd - i) >= k) vd - i
      else if (vd > 0) 0
      else -1

      ret
    }

    def sendMessage(edge: EdgeTriplet[Int, ED]): Iterator[(VertexId, Int)] = {

      val lst = if (edge.srcAttr == 0) {

        List((edge.dstId, (1, edge.dstAttr)), (edge.srcId, (0, edge.srcAttr)))
      } else if (edge.dstAttr == 0) {
        List((edge.srcId, (1, edge.srcAttr)), (edge.dstId, (0, edge.dstAttr)))
      } else List.empty

      // no need to send msg to nodes that have already been "removed"
      val lstFiltered = lst.filter(_._2._2>=0).map(t => (t._1, t._2._1))

      lstFiltered.iterator
    }

    val initialMessage = 0
    val pregelGraph = Pregel(kGraph, initialMessage,
      maxIterations, EdgeDirection.Either)(
      vprog = vprog,
      sendMsg = sendMessage,
      mergeMsg = (a, b) => a + b)
    kGraph.unpersist()

    pregelGraph.mapVertices[Boolean]((id: VertexId, d: Int) => d>0)

  } // end of KCore

}