import org.apache.spark.graphx._

object RN_RE_RW {
  def sampling(graph: Graph[Int, Int], method: String, ratio: Float): Graph[Int, Int] = {
    method match {
      case "random" =>
        val rand = scala.util.Random

        def vertexPred(id: VertexId, attr: Int): Boolean = {
          val randnum: Float = rand.nextFloat()
          val bool: Boolean = (randnum < ratio)
          bool
        }

        graph.subgraph(vpred = vertexPred)
      case "randomEdge" =>
        val rand = scala.util.Random

        def edgePred(edge: EdgeTriplet[Int, Int]): Boolean = {
          val randnum: Float = rand.nextFloat()
          val bool: Boolean = (randnum < ratio)
          bool
        }

        graph.subgraph(epred = edgePred)
      case "randomWalk" =>
        val rand = scala.util.Random

        def arraySampling(array: Array[VertexId]): Option[VertexId] = { //Option[VertexId]
          val leng = array.length
          if (leng != 0) Some(array(rand.nextInt(array.length)))
          else None
        }

        val neighbors = graph.collectNeighborIds(EdgeDirection.Out)
        /// First
        val initial = neighbors.sample(false, ratio / 10) //initial
      var sampledId = initial.map { case (id, array) => Option(id) }.collect()
        //var next = initial.map{case(id,array) => if (array.length!=0) arraySampling(array)}
        //// Second
        var next = initial.map { case (id, array) => arraySampling(array) }.collect()
        sampledId = sampledId ++ next
        var nextNB = neighbors.filter { case (id, array) => if (next.contains(Option(id))) true else false }
        //// Third - Tenth
        for (a <- 3 to 10) {
          next = nextNB.map { case (id, array) => arraySampling(array) }.collect()
          sampledId = sampledId ++ next
          nextNB = neighbors.filter { case (id, array) => if (next.contains(Option(id))) true else false }
        }

        def vertexPred(id: VertexId, attr: Int): Boolean = {
          sampledId.contains(Option(id))
        }

        graph.subgraph(vpred = vertexPred)
      case "snowball" =>
        // k is variable. We fix here it as 3.
        val kFix = 3
        val rand = scala.util.Random

        def arrayKSampling(array: Array[VertexId], k: Int): Array[VertexId] = { //Option[VertexId]
          val leng = array.length
          if (leng != 0) rand.shuffle(array.toList).take(k).toArray //Some(array(rand.nextInt(array.length)))
          else Array()
        }

        val neighbors = graph.collectNeighborIds(EdgeDirection.Out)
        /// First
        val initial = neighbors.sample(false, ratio / 10) //initial
      var sampledId = initial.map { case (id, array) => id }.collect()
        //var next = initial.map{case(id,array) => if (array.length!=0) arraySampling(array)}
        //// Second
        var next = initial.map { case (id, array) => arrayKSampling(array, kFix) }.collect().reduce(_ ++ _)
        sampledId = sampledId ++ next
        var nextNB = neighbors.filter { case (id, array) => if (next.contains(id)) true else false }
        //// Third - Tenth
        //for (a <- 3 to 10){
        next = nextNB.map { case (id, array) => arrayKSampling(array, kFix) }.collect().reduce(_ ++ _)
        sampledId = sampledId ++ next

        //	nextNB = neighbors.filter{case (id,array) => if (next.contains(id)) true else false}
        //}
        def vertexPred(id: VertexId, attr: Int): Boolean = {
          sampledId.contains(id)
        }

        graph.subgraph(vpred = vertexPred)

      case "randomWalkWithJump" =>
        // 10 times random walk
        // p is variable. We fix jump probability(threshold) p as 0.5.
        // It's because we don't consider any other parameters except ratio here.
        val n = graph.numVertices
        val pFix = (0.5).toFloat
        val rand = scala.util.Random

        def arraySampling(array: Array[VertexId]): Array[VertexId] = { //Option[VertexId]
          val leng = array.length
          if (leng != 0) Array(array(rand.nextInt(array.length)))
          else Array()
        }

        val neighbors = graph.collectNeighborIds(EdgeDirection.Out)
        /// First
        val initial = neighbors.sample(false, ratio / 10) //initial
      //var sampledId = initial//initial.map{case(id,array) => Option(id)}.collect()
      var sampledId = initial.map { case (id, array) => id }.collect()

        //// Second
        var prob = initial.map { v => (v, rand.nextFloat()) }.filter { case (v, p) => (p < pFix) }
        var next1 = prob.map { case ((id, array), p) => arraySampling(array) }.collect().reduce(_ ++ _)
        var next2 = neighbors.takeSample(false, sampledId.length - next1.length).map { case (id, array) => id }
        var nextSample = next1 ++ next2
        sampledId = sampledId ++ nextSample
        var nextNB = neighbors.filter { case (id, array) => if (nextSample.contains(id)) true else false }

        //// Third - Tenth
        for (a <- 3 to 10) {
          var prob = nextNB.map { v => (v, rand.nextFloat()) }.filter { case (v, p) => (p < pFix) }
          next1 = prob.map { case ((id, array), p) => arraySampling(array) }.collect().reduce(_ ++ _)
          next2 = neighbors.takeSample(false, nextSample.length - next1.length).map { case (id, array) => id }
          nextSample = next1 ++ next2
          sampledId = sampledId ++ nextSample
          nextNB = neighbors.filter { case (id, array) => if (nextSample.contains(id)) true else false }
        }

        def vertexPred(id: VertexId, attr: Int): Boolean = {
          sampledId.contains(id)
        }

        graph.subgraph(vpred = vertexPred)

      case _ =>
        throw new IllegalArgumentException("Invalid Sampling Method")
    }
  }
}
