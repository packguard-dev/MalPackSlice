package CodeSlice

import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.nodes._
import io.shiftleft.semanticcpg.language._

import io.joern.x2cpg.X2Cpg.applyDefaultOverlays

import io.joern.jssrc2cpg.{Config, JsSrc2Cpg}
import io.joern.x2cpg.X2Cpg

import io.joern.dataflowengineoss.language._
import io.joern.dataflowengineoss.queryengine.EngineContext
import io.joern.dataflowengineoss.semanticsloader.{Semantics, FlowSemantic, FullNameSemantics}
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.joern.dataflowengineoss.layers.dataflows.{OssDataFlow, OssDataFlowOptions}
import io.shiftleft.semanticcpg.layers.LayerCreatorContext
import io.joern.dataflowengineoss.DefaultSemantics
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success}
import _root_.Type.Source.SourceGroups
import _root_.Type.Sink.SinkGroups
import _root_.Type.{CallType, CodeType, RegexType, TypeDefinition}
import scala.annotation.switch
import java.util.regex.Pattern
import FileProcessor.IOFileProcessor
import org.checkerframework.checker.units.qual.s
import FileProcessor.IOFileProcessorImpl
import io.shiftleft.codepropertygraph.generated.nodes.AstNode
import scala.collection.mutable.Set


class JoernDataFlowAnalysis(inputDir: String, outputDir: String) {

    val outputDirectory: String = outputDir

    /**
     * Lazy create CPG when first accessed
     * Lazy evaluation to avoid unnecessary computation
     * @return Cpg object
     */
    lazy val cpg: Cpg = {
        println(s"Generating CPG from: $inputDir")
        
        val config = Config()
            .withInputPath(inputDir)
            .withOutputPath(s"$outputDir/cpg.bin")

        val jsSrc2Cpg = new JsSrc2Cpg()
        val generatedCpg = jsSrc2Cpg.createCpg(config) match {
            case Success(cpg) => cpg
            case Failure(exception) =>
                throw new RuntimeException(s"Failed to create CPG: $exception")
        }

        X2Cpg.applyDefaultOverlays(generatedCpg)
        val context = new LayerCreatorContext(generatedCpg)
        val options = new OssDataFlowOptions()
        new OssDataFlow(options).run(context)
        
        generatedCpg

    }

    /**
     * IMPORTANT: Must having semantics and engineContext in scope for data flow analysis
     */
    val defaultSemanticsList: List[FlowSemantic] = DefaultSemantics().elements

    implicit val semantic: Semantics = FullNameSemantics.fromList(defaultSemanticsList)

    implicit val engineContext: EngineContext = new EngineContext(semantic)

    var slicedPaths: ListBuffer[String] = ListBuffer()

    /**
     * Test Function use for Test and Debug
     * @param sourceRegex Regex pattern to identify source functions/variables
     * @param sinkRegex Regex pattern to identify sink functions/variables
     */
    def testSensitiveFlows(sourceRegex: String, sinkRegex: String): Unit = {
        println(s"Scanning flows from '$sourceRegex' to '$sinkRegex'...")

        val sources = cpg.call
                      .where(_.name(sourceRegex))
                      .toSet

        def sinks = cpg.call
                      .where(_.name(sinkRegex))
                      .l

        if (sources.isEmpty) {
            println(s"[WARNING] No sources found for regex: $sourceRegex")
            println("Hints: Check if regex matches 'code' or 'variable names'.")
            return
        }

        val flows = sinks.reachableByFlows(sources).l
        
        if (flows.isEmpty) {
            println("No flows found.")
        } else {
            println(s"Found ${flows.size} flows:")
            flows.foreach { flow => 
                println("--------------------------------------------------")
                flow.elements.map {
                     case c: Call => s"[Call] ${c.code}"
                     case i: Identifier => s"[Id] ${i.name}"
                     case p: MethodParameterIn => s"[Param] ${p.name}"
                     case other => s"[${other.label}] ${other.code}"
                }.foreach(println)
            }
        }
    }

    def runDataFlowAnalysis(): Unit = {

        for (sourceGroup <- SourceGroups.getAllSources) {
            
            val sources: List[Expression] = sourceGroup match {
                
                // 1. CallType: Chỉ tìm tên hàm chính xác (VD: "readFileSync")
                case CallType(name) => 
                    cpg.call
                       .nameExact(name)
                       .map(_.asInstanceOf[Expression])
                       .l

                // 2. RegexType: Tìm tên hàm HOẶC tên biến khớp Regex (VD: ".*exec.*")
                case RegexType(pattern) =>
                    val callSources = cpg.call
                                         .name(pattern)
                                         .map(_.asInstanceOf[Expression])
                                         .l

                    val idSources   = cpg.identifier
                                         .name(pattern)
                                         .map(_.asInstanceOf[Expression])
                                         .l
                    
                    callSources.union(idSources).l

                // 3. CodeType: Tìm chính xác đoạn code (VD: "process.argv")
                case CodeType(codeSnippet) =>
                    val pattern = ".*" + Pattern.quote(codeSnippet) + ".*"

                    val callSources = cpg.call
                                         .code(pattern)
                                         .map(_.asInstanceOf[Expression])
                                         .l

                    val idSources   = cpg.identifier
                                         .code(pattern)
                                         .map(_.asInstanceOf[Expression])
                                         .l
                    
                    callSources.union(idSources).l
            }

            // BƯỚC 2: Chỉ chạy nếu tìm thấy Source
            if (sources.nonEmpty) {
                println(s"=== FOUND SOURCES FOR $sourceGroup ===")

                for (sinkGroup <- SinkGroups.getAllSinks) {
                    val sinks = sinkGroup match {
                        case CallType(name) => cpg.call.name(s".*$name.*").argument.l
                        case RegexType(ptrn) => cpg.call.name(ptrn).argument.l
                        case CodeType(code) => cpg.call.code(s".*$code.*").argument.l
                    }

                    if (sinks.nonEmpty) {
                        val flows = sinks.reachableByFlows(sources).l
                        if (flows.nonEmpty) {
                            println(s"\n=== [ALERT] FOUND FLOWS FROM $sourceGroup TO $sinkGroup ===")
                            println(s"Total flows found: ${flows.size}")
                            
                            var currentFlow = ""

                            val relevantLines = flows.flatMap(_.elements)
                                         .map(_.lineNumber.getOrElse(-1))
                                         .filter(_ != -1)
                                         .toSet.toList.sorted

                            relevantLines.foreach { line =>

                                val callsOnLine = cpg.call
                                         .filter(_.lineNumber.contains(line))
                                         .l
                                
                                if (callsOnLine.nonEmpty) {
                                    val bestCode = callsOnLine.maxBy(_.code.length).code
                                    println(s"$line: $bestCode")
                                    currentFlow += s"$bestCode\n"
                                } else {
                                    val anyAstNode = cpg.all.collect { 
                                        case n: AstNode => n 
                                    }.filter(_.lineNumber.contains(line)).l
                                                    
                                    if (anyAstNode.nonEmpty) {
                                        val bestCode = anyAstNode.maxBy(_.code.length).code
                                        println(s"$line: $bestCode")
                                        currentFlow += s"$bestCode\n"
                                    }
                                }
                            }

                            this.slicedPaths += currentFlow
                        } else {
                            println(s"No direct flows found. Checking for Callback/Nested structures...")
                            val processedPairs = Set[(Long, Int)]()

                            sinks.foreach { sink =>
                                val enclosingSourceCall = findEnclosingSourceCall(sink, sources)
                                
                                if (enclosingSourceCall.isDefined) {
                                    val sourceCall = enclosingSourceCall.get
                                    val sourceId = sourceCall.id()
                                    val sinkLine = sink.lineNumber.getOrElse(-1)

                                    // Kiểm tra xem cặp (SourceID, SinkLine) này đã xử lý chưa?
                                    // Nếu chưa thì mới xử lý
                                    if (!processedPairs.contains((sourceId, sinkLine))) {
                                        processedPairs.add((sourceId, sinkLine))
                                        
                                        val sourceLine = sourceCall.lineNumber.getOrElse(-1)
                                        
                                        val srcCode = cpg.call.lineNumber(sourceLine).l.map(_.code).maxBy(_.length)
                                        
                                        val snkCalls = cpg.call.lineNumber(sinkLine).l
                                        val snkCode = if (snkCalls.nonEmpty) snkCalls.map(_.code).maxBy(_.length) 
                                                      else cpg.all.collect{case n: AstNode=>n}.filter(_.lineNumber.contains(sinkLine)).l.map(_.code).maxBy(_.length)

                                        println(s"Enclosing Source Call: $srcCode")
                                        println(s"Sink Code:           $snkCode")
                                        
                                        this.slicedPaths += s"$srcCode"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Helper function to find enclosing source call for a given AST node
    // Example when using callbacks or nested functions

    def findEnclosingSourceCall(node: AstNode, potentialSources: List[Expression]): Option[Call] = {
        val sourceIds = potentialSources.map(_.id()).toSet
        var currentNode = node

        while (currentNode != null) {
            
            currentNode match {
                case call: Call =>
                    if (sourceIds.contains(call.id())) {
                        println(s"Enclosing source call found: ${call.code}")
                        return Some(call)
                    }
                    
                case _ => 
            }

            val parents = currentNode.in(EdgeTypes.AST)
            
            if (parents.hasNext) {
                currentNode = parents.next().asInstanceOf[AstNode]
            } else {
                currentNode = null
            }
        }

        None
    }

    def saveSlicedPathsToFile(ioFileProcessor: IOFileProcessor, packageName: String): Boolean = {
        try {
            for (path <- this.slicedPaths) {
                val fileName = s"$outputDir/${packageName}_${System.currentTimeMillis()}.txt"
                val success = ioFileProcessor.saveOutputPackage(fileName, path)

                if (success) {
                    println(s"Sliced path saved to: $fileName")
                } else {
                    println(s"[ERROR] Failed to save sliced path to: $fileName")
                    throw new RuntimeException("Failed to save sliced path")
                }
            }

            true
        } catch {
            case e: Exception =>
                println(s"[ERROR] Exception while saving sliced paths: $e")
                false
        }
    }

    def close(): Unit = {
        cpg.close()
    }

}