package CodeSlice.Group

import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.*
import scala.collection.mutable.{Set, Map}
import CodeSlice.Group.CustomNode

class SinkMethodGroup {
  private val nodes = Map[Long, CustomNode]()

  def appendNode(newNodeId: Long, lineNumber: Int, columnNumber: Int, label: String, fileName: String, code: String, methodFullName: String=""): Unit = {
    val nodeId: Long = newNodeId
    val newNode = new CustomNode(nodeId, lineNumber, columnNumber, label, fileName, code, methodFullName)
    nodes.getOrElseUpdate(nodeId, newNode)
  }

  def dumpNodeInfo(): Unit = {
    for ((_, node) <- nodes) {
      node.dumpNodeInfo()
    }
  }

  def getAllNodes: Iterable[CustomNode] = nodes.values
}
