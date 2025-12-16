package CodeSlice

import io.joern.jssrc2cpg.{Config, JsSrc2Cpg}
import io.shiftleft.codepropertygraph.generated.Cpg

import scala.util.{Failure, Success}
import Type.Source.SourceGroups
import Type.Sink.SinkGroups
import Type.{CallType, CodeType, RegexType}
import io.joern.joerncli.{JoernParse, JoernSlice}
import CodeSlice.Group.{SinkMethodGroup, SourceMethodGroup}
import CodeSlice.Path.PathLine
import io.shiftleft.semanticcpg.language.*
import io.joern.dataflowengineoss.language.*

import scala.collection.mutable.{Set, Queue, Map}
import java.nio.file.Paths
import CodeSlice.Group.CustomNode

import flatgraph.Edge
import io.joern.dataflowengineoss.DefaultSemantics

import scala.util.matching.Regex
import io.joern.dataflowengineoss.language.*
import io.joern.dataflowengineoss.layers.dataflows.{OssDataFlow, OssDataFlowOptions}
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.semanticcpg.layers.*
import io.joern.dataflowengineoss.queryengine.EngineContext
import io.joern.dataflowengineoss.semanticsloader.{FlowSemantic, NoCrossTaintSemantics, Semantics}
import io.joern.dataflowengineoss.slicing.{DataFlowConfig, DataFlowSlicing}
import io.shiftleft.codepropertygraph.generated.nodes.{Call, Method, StoredNode}
import io.shiftleft.semanticcpg.Overlays
import scala.collection.mutable.ListBuffer
import cask.endpoints.get
import scala.collection.mutable
import flatgraph.GNode

// 30064771073 - 30064771076 - 30064771078
@deprecated("Use V2 instead", "v2.0")
class CodeSliceImp(inputDir: String, outputDir: String) extends CodeSlice {

    private val cpg: Cpg = {
        println("ASTGEN_BIN = " + sys.env.get("ASTGEN_BIN"))
        val config = Config()
          .withInputPath(inputDir)
          .withOutputPath(s"$outputDir/cpg.bin")

        val jsSrc2Cpg = new JsSrc2Cpg()
        jsSrc2Cpg.createCpg(config) match {
            case Success(cpg) => cpg
            case Failure(exception) =>
                throw new RuntimeException(
                    s"Failed to create CPG from source code at $inputDir",
                    exception
                )
        }
    }

    private val edges = cpg.graph.allEdges.map(edge => edge.dst.id() -> edge).groupBy(_._1).map {
        case (dstId, edgeTuples) => dstId -> edgeTuples.map(_._2).toSet
    }
    private val allCalls = cpg.call.map(call => call.id() -> call).toMap
    private val allIdentifiers = cpg.identifier.map(id => id.id() -> id).toMap
    private val allBlocks = cpg.block.map(block => block.id() -> block).toMap

    private val files = cpg.file.toSet
    private val engineContext = new EngineContext()

    // TODO: thang
    override def getSinkMethodGroup: SinkMethodGroup = {
        val sinkMethodGroup = new SinkMethodGroup()

        for (sink <- SinkGroups.getAllSinks) {
            sink match {
                case CallType(value) =>
                    val calls = cpg.call
                      .where(_.name(value))
                      .toSet
                    for (call <- calls) {
                        sinkMethodGroup.appendNode(
                          call.id(),
                          call.lineNumber.getOrElse(-1),
                          call.columnNumber.getOrElse(-1),
                          call.label(),
                          call.file.name.headOption.getOrElse(""),
                          call.code,
                          call.methodFullName
                        )
                    }

                case CodeType(value) => None
                    
                case RegexType(value) =>
                    val pattern = value.r
                    
                    // Tìm trong call names
                    val callMatches = cpg.call
                      .filter(call => pattern.matches(call.name))
                      .toSet
                    for (call <- callMatches) {
                        sinkMethodGroup.appendNode(
                          call.id(),
                          call.lineNumber.getOrElse(-1),
                          call.columnNumber.getOrElse(-1),
                          call.label(),
                          call.file.name.headOption.getOrElse(""),
                          call.code,
                          call.methodFullName
                        )
                    }
                    
                    // Tìm trong call code (full expression)
                    val callCodeMatches = cpg.call
                      .filter(call => pattern.matches(call.code))
                      .toSet
                    for (call <- callCodeMatches) {
                        sinkMethodGroup.appendNode(
                          call.id(),
                          call.lineNumber.getOrElse(-1),
                          call.columnNumber.getOrElse(-1),
                          call.label(),
                          call.file.name.headOption.getOrElse(""),
                          call.code,
                        )
                    }
            }
        }
        sinkMethodGroup.dumpNodeInfo()
        sinkMethodGroup
    }

    // TODO: thang
    override def getSourceMethodGroup: SourceMethodGroup = {

        val sourceMethodGroup = new SourceMethodGroup()

        for (source <- SourceGroups.getAllSources) {
            source match {
                case CallType(value) =>
                    var calls = cpg.call
                      .where(_.name(value))
                      .toSet

                    var assigns = cpg.call.name("<operator>.assignment")
                        .where(_.argument(2).code(".*" + value + ".*"))
                        .toSet

                    if (!assigns.isEmpty) {
                        calls = assigns
                    }
                
                    for (call <- calls) {
                        sourceMethodGroup.appendNode(
                            call.id(),
                            call.lineNumber.getOrElse(-1),
                            call.columnNumber.getOrElse(-1),
                            call.label(),
                            call.file.name.headOption.getOrElse(""),
                            call.code,
                            call.methodFullName
                        )
                    }

                case CodeType(value) =>
                    val codes = cpg.identifier
                      .where(_.name(value))
                      .toSet

                    for (code <- codes) {
                        sourceMethodGroup.appendNode(
                          code.id(),
                          code.lineNumber.getOrElse(-1),
                          code.columnNumber.getOrElse(-1),
                          code.label(),
                          code.file.name.headOption.getOrElse(""),
                          code.code,
                        )
                    }

                case RegexType(value) =>
                    val pattern = value.r
                    
                    // Tìm trong call names
                    val callMatches = cpg.call
                      .filter(call => pattern.matches(call.name))
                      .toSet
                    for (call <- callMatches) {
                        sourceMethodGroup.appendNode(
                          call.id(),
                          call.lineNumber.getOrElse(-1),
                          call.columnNumber.getOrElse(-1),
                          call.label(),
                          call.file.name.headOption.getOrElse(""),
                          call.code,
                          call.methodFullName
                        )
                    }
                    
                    // Tìm trong call code (full expression)
                    val callCodeMatches = cpg.call
                      .filter(call => pattern.matches(call.code))
                      .toSet
                    for (call <- callCodeMatches) {
                        sourceMethodGroup.appendNode(
                          call.id(),
                          call.lineNumber.getOrElse(-1),
                          call.columnNumber.getOrElse(-1),
                          call.label(),
                          call.file.name.headOption.getOrElse(""),
                          call.code,
                        )
                    }
            }
        }

        sourceMethodGroup.dumpNodeInfo()
        sourceMethodGroup
    }

    override def close(): Unit = {
        cpg.close()
        println(s"Cleaned up resources for $inputDir")
    }

    // TODO: khoa
    override def getPathLine(
                              sourceMethodGroup: SourceMethodGroup,
                              sinkMethodGroup: SinkMethodGroup
                            ): PathLine = {
        val pathLine = new PathLine()
        
        for (sourceMethod <- sourceMethodGroup.getAllNodes) {
            for (sinkMethod <- sinkMethodGroup.getAllNodes) {
                if (sourceMethod != sinkMethod) {
                    val flows = reachableByFlow(sourceMethod, sinkMethod)
                    if (flows.size > 2) {
                        for (node <- flows) {
                            pathLine.addToSlice(node)
                        }
                    } else {
                        pathLine.addPotentialPaths(sourceMethod, Set(sinkMethod))
                    }
                }
            }
        }
        
        pathLine
    }

    // TODO: thang
    override def extractCode(pathLine: PathLine): String = ???

    /**
     * get a flow from source to sink if the flow is empty return node in between source and sink
     * use reverse breadth first search
     *
     * @param sourceMethod
     * @param sinkMethod
     * @return set of custom nodes output
     */
    private def reachableByFlow(sourceMethod: CustomNode, sinkMethod: CustomNode): Set[CustomNode] = {
        
        val currentNodesInSlice = Set[CustomNode]()
        val queue = Queue[CustomNode]()
        val sourceId = sourceMethod.nodeId
        var reachable : Boolean = false
        queue.enqueue(sinkMethod)

        def addToSlice(currentNode: CustomNode): Unit = {
            
            // Chỉ thêm node có ý nghĩa vào slice
            if (!isMeaningfulNode(currentNode)) {
                return
            }
            
            val existingNodeOpt = currentNodesInSlice.find(node =>
                node.lineNumber == currentNode.lineNumber &&
                  node.fileName == currentNode.fileName
            )

            existingNodeOpt match {
                case Some(existingNode) =>
                    if (currentNode.code.length > existingNode.code.length) {
                        currentNodesInSlice.remove(existingNode)
                        currentNodesInSlice.add(currentNode)
                        val indexInQueue = queue.indexWhere(node =>
                            node.lineNumber == currentNode.lineNumber &&
                              node.fileName == currentNode.fileName
                        )

                        if (indexInQueue != -1) {
                            queue.update(indexInQueue, currentNode)
                        } else {
                            queue.enqueue(currentNode)
                        }
                    }
                case None =>
                    queue.enqueue(currentNode)
                    currentNodesInSlice.add(currentNode)
            }
        }
        
        // Helper method để kiểm tra node có ý nghĩa không
        def isMeaningfulNode(node: CustomNode): Boolean = {
            val code = node.code.trim
            
            // Loại bỏ code rỗng
            if (code.isEmpty) return false
            
            // Loại bỏ các pattern không có ý nghĩa
            val nonMeaningfulPatterns = Set(
                "<empty>", ":program", "window", "undefined", 
                "null", "true", "false", "this"
            )
            if (nonMeaningfulPatterns.contains(code)) return false
            
            // Loại bỏ các node chỉ chứa ký tự đặc biệt
            if (!code.exists(_.isLetterOrDigit)) return false
            
            // Loại bỏ các label không mang ngữ nghĩa
            val nonMeaningfulLabels = Set("FILE", "NAMESPACE_BLOCK", "TYPE_DECL", "BINDING")
            if (nonMeaningfulLabels.contains(node.label)) return false
            
            // Loại bỏ identifier đơn lẻ (chỉ là tên biến đơn thuần)
            if (node.label == "IDENTIFIER" && isSingleIdentifier(code)) return false
            
            true
        }
        
        // Helper method để kiểm tra identifier đơn
        def isSingleIdentifier(code: String): Boolean = {
            val meaningfulChars = Set('(', ')', '[', ']', '{', '}', '.', '=', '+', '-', '*', '/', '<', '>', '!', '&', '|', '?', ':', ';', ',', '"', '\'', '`', ' ')
            
            !code.exists(meaningfulChars.contains) && code.length < 50
        }

        while (!queue.isEmpty) {
            
            val currentNode = queue.front
            queue.dequeue()
            addToSlice(currentNode)
            
            val reachableEdges = edges.getOrElse(currentNode.nodeId, Set())

            for (edge <- reachableEdges) {

                if (edge.src.id == sourceId) {
                    reachable = true
                } else {
                    val nodeLabel = edge.src.label()

                    val customNodeOpt: Option[CustomNode] = {
                        if (nodeLabel == "CALL") {
                            allCalls.get(edge.src.id).map( callNode => 
                                new CustomNode(
                                    callNode.id(),
                                    callNode.lineNumber.getOrElse(-1),
                                    callNode.columnNumber.getOrElse(-1),
                                    callNode.label(),
                                    callNode.file.name.headOption.getOrElse(""),
                                    callNode.code
                                )
                            )
                        } else if (nodeLabel == "IDENTIFIER") {
                            allIdentifiers.get(edge.src.id).map { identifierNode =>
                                new CustomNode(
                                    identifierNode.id(),
                                    identifierNode.lineNumber.getOrElse(-1),
                                    identifierNode.columnNumber.getOrElse(-1),
                                    identifierNode.label(),
                                    identifierNode.file.name.headOption.getOrElse(""),
                                    identifierNode.code
                                )
                            }
                        } else if (nodeLabel == "BLOCK") {
                            allBlocks.get(edge.src.id).map { blockNode =>
                                new CustomNode(
                                    blockNode.id(),
                                    blockNode.lineNumber.getOrElse(-1),
                                    blockNode.columnNumber.getOrElse(-1),
                                    blockNode.label(),
                                    blockNode.file.name.headOption.getOrElse(""),
                                    blockNode.code
                                )
                            }
                        } else {
                            None
                        }
                    }

                    customNodeOpt.foreach(addToSlice)
                }
            }
        }
        addToSlice(sourceMethod)
        currentNodesInSlice
    }

    /**
     * Print out the slice to sreen
     *
     * @param flows set of node ids
     */
    def displaySlice(flows : Set[Long]): Unit = {
        println("======== Start Flow ==========")

        val nodesWithLine = flows.toList.map { id =>

            val node = cpg.graph.node(id)
            val code = node.propertyOption[String]("CODE")
              .orElse(node.propertyOption[String]("code"))
              .getOrElse("")

            val lineNumber: Long = node.propertyOption[Any]("LINE_NUMBER")
              .orElse(node.propertyOption[Any]("lineNumber"))
              .map {
                  case l: Long => l
                  case i: Int => i.toLong
                  case s: Short => s.toLong
                  case _ => Long.MaxValue
              }
              .getOrElse(Long.MaxValue)

            (lineNumber, code, id)
        }.filter(_._2.nonEmpty) // keep only nodes with code

        // Sort by line number
        val sortedNodes = nodesWithLine.sortBy(_._1)

        // Build code slice
        val codeSlice = new StringBuilder
        for ((line, code, id) <- sortedNodes) {
            codeSlice.append(code + "\n")
        }

        if (codeSlice.nonEmpty) {
            println(codeSlice.toString())
        }

        println("======== End Flow ==========")
    }
}