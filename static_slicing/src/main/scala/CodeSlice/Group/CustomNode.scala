package CodeSlice.Group

import io.shiftleft.codepropertygraph.generated.nodes.*
import io.shiftleft.codepropertygraph.generated.language.*

class CustomNode(
    val newNodeId: Long,
    val newNodeLineNumber: Int,
    val newNodeColumnNumber: Int,
    val newNodeLabel: String,
    val newNodeFileName: String,
    val newNodeCode: String,
    val methodFullNameParam: String = ""
) {
  val nodeId: Long = newNodeId
  val code: String = newNodeCode
  val lineNumber: Int = newNodeLineNumber
  val columnNumber: Int = newNodeColumnNumber
  val label: String = newNodeLabel
  val fileName: String = newNodeFileName
  val methodFullName: String = methodFullNameParam


  def dumpNodeInfo(): Unit = {
    println(
      s"CustomNode - ID: $nodeId, Label: $label, Code: $code, Line: $lineNumber, Column: $columnNumber, File: $fileName, MethodFullName: $methodFullName"
    )
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: CustomNode => this.nodeId == other.nodeId
    case _                 => false
  }

  override def hashCode(): Int = nodeId.hashCode()
}
