package CodeSlice

import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.nodes._
import io.shiftleft.semanticcpg.language._ 
import io.joern.x2cpg.X2Cpg.applyDefaultOverlays
import io.joern.jssrc2cpg.{Config, JsSrc2Cpg}
import io.joern.x2cpg.X2Cpg
import io.shiftleft.semanticcpg.layers.LayerCreatorContext
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import scala.collection.mutable.{ListBuffer, Queue, Set => MutableSet}
import scala.collection.immutable.{Set => ImmutableSet}
import scala.util.{Failure, Success}
import _root_.Type.Source.SourceGroups
import _root_.Type.Sink.SinkGroups
import _root_.Type.{CallType, CodeType, RegexType}
import java.util.regex.Pattern
import FileProcessor.IOFileProcessor

class CodeSliceImp(inputDir: String, outputDir: String) {

  val outputDirectory: String = outputDir
  var slicedPaths: ListBuffer[String] = ListBuffer()
  
  private val uniquePathTracker: MutableSet[String] = MutableSet()

  lazy val cpg: Cpg = {
    println(s"Generating Raw CPG from: $inputDir")
    val config = Config().withInputPath(inputDir).withOutputPath(s"$outputDir/cpg.bin")
    val jsSrc2Cpg = new JsSrc2Cpg()
    val generatedCpg = jsSrc2Cpg.createCpg(config) match {
      case Success(cpg) => cpg
      case Failure(exception) => throw new RuntimeException(s"Failed to create CPG: $exception")
    }
    
    X2Cpg.applyDefaultOverlays(generatedCpg)
    generatedCpg
  }

  def close(): Unit = cpg.close()

  private def backwardSliceBFS(sinkNode: AstNode, allSourceIds: ImmutableSet[Long]): List[AstNode] = {
    val visitedIds = MutableSet[Long]()     
    val rawSliceNodes = ListBuffer[AstNode]() 
    val queue = Queue[AstNode]()

    queue.enqueue(sinkNode)
    visitedIds.add(sinkNode.id())

    var foundSource = false

    while (queue.nonEmpty) {
      val currentNode = queue.dequeue()
      
      rawSliceNodes += currentNode

      if (allSourceIds.contains(currentNode.id())) {
        foundSource = true
      }

      val prevInstructions = currentNode.in(EdgeTypes.CFG)

      for (prev <- prevInstructions) {
        if (prev.isInstanceOf[AstNode] && !visitedIds.contains(prev.id())) {
          val prevAst = prev.asInstanceOf[AstNode]
          visitedIds.add(prevAst.id())
          queue.enqueue(prevAst)
        }
      }
    }

    if (foundSource) {
      filterAndCleanSlice(rawSliceNodes.toList)
    } else {
      List.empty
    }
  }

  private def filterAndCleanSlice(nodes: List[AstNode]): List[AstNode] = {
    val meaningfulNodes = nodes.filter(isMeaningfulNode)
    val distinctLinesMap = scala.collection.mutable.Map[(String, Int), AstNode]()

    for (node <- meaningfulNodes) {
      val fileName = node.start.file.name.headOption.getOrElse("<unknown>")
      val lineNum = node.lineNumber.map(_.intValue()).getOrElse(-1)

      if (lineNum != -1) {
        val key = (fileName, lineNum)
        if (!distinctLinesMap.contains(key)) {
            val bestNodeOnLine = getBestNodeForLine(fileName, lineNum, node)
            distinctLinesMap(key) = bestNodeOnLine
        }
      }
    }
    distinctLinesMap.values.toList.sortBy(_.lineNumber.map(_.intValue()).getOrElse(0))
  }

  private def getBestNodeForLine(fileName: String, lineNum: Int, originalNode: AstNode): AstNode = {
      try {
          val candidates = cpg.call
              .where(_.file.nameExact(fileName))
              .lineNumber(lineNum)
              .l

          if (candidates.nonEmpty) {
              candidates.maxBy(_.code.length)
          } else {
              originalNode
          }
      } catch {
          case e: Exception => originalNode
      }
  }

  private def isMeaningfulNode(node: AstNode): Boolean = {
    val code = node.code.trim
    if (code.isEmpty) return false
    if (node.lineNumber.isEmpty) return false 
    val nonMeaningfulPatterns = Set("<empty>", ":program", "window", "undefined", "null", "true", "false", "this")
    if (nonMeaningfulPatterns.contains(code)) return false
    if (!code.exists(_.isLetterOrDigit)) return false
    val nonMeaningfulLabels = Set("FILE", "NAMESPACE_BLOCK", "TYPE_DECL", "BINDING", "METHOD_RETURN", "BLOCK")
    if (nonMeaningfulLabels.contains(node.label)) return false
    if (node.label == "IDENTIFIER" && isSingleIdentifier(code)) return false
    true
  }

  private def isSingleIdentifier(code: String): Boolean = {
    val meaningfulChars = Set('(', ')', '[', ']', '{', '}', '.', '=', '+', '-', '*', '/', '<', '>', '!', '&', '|', '?', ':', ';', ',', '"', '\'', '`', ' ')
    !code.exists(meaningfulChars.contains) && code.length < 50
  }

  def runStaticAnalysis(): Unit = {
    println("--- 1. Collecting Source IDs (Raw CPG) ---")
    var allSourceIds = ImmutableSet[Long]()
    
    for (sourceGroup <- SourceGroups.getAllSources) {
      val sources: List[AstNode] = sourceGroup match {
        case CallType(name) => cpg.call.nameExact(name).map(_.asInstanceOf[AstNode]).l
        case RegexType(ptrn) => 
            cpg.call.name(ptrn).map(_.asInstanceOf[AstNode]).l ++
            cpg.identifier.name(ptrn).map(_.asInstanceOf[AstNode]).l
        case CodeType(code) => 
            val p = ".*" + Pattern.quote(code) + ".*"
            cpg.call.code(p).map(_.asInstanceOf[AstNode]).l ++
            cpg.identifier.code(p).map(_.asInstanceOf[AstNode]).l
      }
      allSourceIds = allSourceIds ++ sources.map(_.id())
    }

    if (allSourceIds.isEmpty) { println("No sources found."); return }

    println("--- 2. Analyzing Sinks with Reverse CFG BFS ---")
    
    uniquePathTracker.clear()

    for (sinkGroup <- SinkGroups.getAllSinks) {
      val sinks = sinkGroup match {
        case CallType(name) => cpg.call.name(s".*$name.*").argument.l
        case RegexType(ptrn) => cpg.call.name(ptrn).argument.l
        case CodeType(code) => cpg.call.code(s".*$code.*").argument.l
      }

      for (sink <- sinks) {
         val sliceNodes = backwardSliceBFS(sink, allSourceIds)
         
         if (sliceNodes.nonEmpty) {
            val sb = new StringBuilder()
            
            sliceNodes.foreach { node => 
               val code = node.code
               sb.append(s"$code\n")
            }
            
            val pathContent = sb.toString()

            if (!uniquePathTracker.contains(pathContent)) {
                uniquePathTracker.add(pathContent)
                this.slicedPaths += pathContent
                
                println(s"\n[ALERT] Reachable Path found for Sink: ${sink.code}")
                sliceNodes.foreach { node => 
                   val line = node.lineNumber.map(_.intValue()).getOrElse(-1)
                   println(s"$line: ${node.code}")
                }
            }
         }
      }
    }
  }
  
  def saveSlicedPathsToFile(ioFileProcessor: IOFileProcessor, packageName: String): Boolean = {
    try {
        var count = 0
        for (path <- this.slicedPaths) {
            count += 1
            val fileName = s"$outputDir/${packageName}_slice_${count}.txt"
            ioFileProcessor.saveOutputPackage(fileName, path)
        }
        true
    } catch { case _: Exception => false }
  }
}