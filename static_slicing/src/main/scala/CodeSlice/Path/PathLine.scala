package CodeSlice.Path

import io.shiftleft.codepropertygraph.generated.nodes.*

import scala.collection.mutable.{Map, Set}
import flatgraph.Edge
import CodeSlice.Group.CustomNode
import FileProcessor.IOFileProcessor

class PathLine {
  val flows: Set[CustomNode] = Set()
  val potentialPaths: Map[CustomNode, Set[CustomNode]] = Map()
  
  def addToSlice(node: CustomNode): Unit = {
    println(s"Adding to slice: File=${node.fileName}, Line=${node.lineNumber}")
    var continueWork = true

    for (existedNode <- flows) {
      if (existedNode.fileName == node.fileName &&
          existedNode.code.contains(node.code)) {
          continueWork = false
      }
      else if (existedNode.fileName == node.fileName &&
               node.code.contains(existedNode.code)) {
        flows.remove(existedNode)
        flows.add(node)
        continueWork = false
      }
    }

    if (continueWork) {
          val existingNodeOpt = flows.find(n =>
            n.fileName == node.fileName && 
            n.lineNumber == node.lineNumber
          )

          existingNodeOpt match {
            case Some(existingNode) =>
              if (node.code.length > existingNode.code.length) {
                flows.remove(existingNode)
                flows.add(node)
              }
            case None =>
              flows.add(node)
          }
    }
  }

  def addPotentialPaths(source: CustomNode, sinks: Set[CustomNode]): Unit = {
    if (potentialPaths.contains(source)) {
      potentialPaths(source) ++= sinks
    } else {
      potentialPaths(source) = sinks
    }
  }

  def exportCodeSlice(ioFileProcessor: IOFileProcessor, outputDir: String, packageName: String): Boolean = {
    val sortedFlows = this.flows.toSeq.sortBy(node => (node.fileName, node.lineNumber))

    for (node <- sortedFlows) {
      var randomName = java.util.UUID.randomUUID().toString
      ioFileProcessor.saveOutputPackage(
        outputDir + packageName + "-" + randomName + ".txt",
        node.code
      )
    }

    for ((source, sinks) <- this.potentialPaths) {
      val sb = new StringBuilder
      sb.append(s"Source: ${source.code}\n")
      sb.append("Sinks:\n")
      for (sink <- sinks) {
        sb.append(s"${sink.code}\n")
      }
      var randomName = java.util.UUID.randomUUID().toString
      ioFileProcessor.saveOutputPackage(
        outputDir + packageName + "-potentialPath-" + randomName + ".txt",
        sb.toString()
      )
    }
    
    true
  }
}
