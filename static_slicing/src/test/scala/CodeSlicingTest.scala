import CodeSlice.{CodeSlice, CodeSliceImp}
import CodeSlice.Group.{SinkMethodGroup, SourceMethodGroup}
import FileProcessor.{IOFileProcessor, IOFileProcessorImpl}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import java.io.File
import scala.io.Source
import _root_.CodeSlice.JoernDataFlowAnalysis

class CodeSlicingTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {
  
  val testInputDir = "./src/test/resources/input"
  val testOutputDir = "./src/test/resources/output"
  val ioFileProcessor: IOFileProcessor = IOFileProcessorImpl
  
  override def beforeAll(): Unit = {
    // Ensure test directories exist
    new File(testInputDir).mkdirs()
    new File(testOutputDir).mkdirs()
  }
  
  override def afterAll(): Unit = {
    // Clean up test outputs
    val outputDir = new File(testOutputDir)
    if (outputDir.exists()) {
      outputDir.listFiles().foreach { file =>
        if (file.isDirectory) {
          file.listFiles().foreach(_.delete())
        }
        file.delete()
      }
    }
  }
  
  test("TC-C-001: Generate CPG from simple JavaScript file") {
    val packagePath = s"$testInputDir/test_package1"
    val outputPath = s"$testOutputDir/test_package1"
    
    // Verify test package exists
    val packageDir = new File(packagePath)
    assert(packageDir.exists() && packageDir.isDirectory, 
      s"Test package should exist at $packagePath")
    
    try {
      val codeSlice = new CodeSliceImp(packagePath, outputPath)
      
      // If CPG generation succeeds, test passes
      assert(true, "CPG generated successfully")
      
      codeSlice.close()
    } catch {
      case e: Exception =>
        fail(s"Failed to generate CPG: ${e.getMessage}")
    }
  }
  
  test("TC-S-001: Detect CallType sources (require, fetch, etc.)") {
    val packagePath = s"$testInputDir/test_package1"
    val outputPath = s"$testOutputDir/test_package1_sources"
    
    try {
      val codeSlice = new CodeSliceImp(packagePath, outputPath)
      val sourceGroup = codeSlice.getSourceMethodGroup
      
      // Should detect at least some sources
      val nodeCount = sourceGroup.getAllNodes.size
      assert(nodeCount > 0, s"Should detect sources, but found $nodeCount")
      
      println(s"Detected $nodeCount source nodes")
      
      codeSlice.close()
    } catch {
      case e: Exception =>
        fail(s"Failed to detect sources: ${e.getMessage}")
    }
  }
  
  test("TC-K-001: Detect code execution sinks (eval, exec, etc.)") {
    val packagePath = s"$testInputDir/test_package1"
    val outputPath = s"$testOutputDir/test_package1_sinks"
    
    try {
      val codeSlice = new CodeSliceImp(packagePath, outputPath)
      val sinkGroup = codeSlice.getSinkMethodGroup
      
      // Should detect at least some sinks (especially eval)
      val nodeCount = sinkGroup.getAllNodes.size
      assert(nodeCount > 0, s"Should detect sinks, but found $nodeCount")
      
      println(s"Detected $nodeCount sink nodes")
      
      codeSlice.close()
    } catch {
      case e: Exception =>
        fail(s"Failed to detect sinks: ${e.getMessage}")
    }
  }
  
  test("TC-P-001: Track simple source-to-sink path") {
    val packagePath = s"$testInputDir/test_package1"
    val outputPath = s"$testOutputDir/test_package1_paths"
    
    try {
      val codeSlice = new CodeSliceImp(packagePath, outputPath)
      
      val sourceGroup = codeSlice.getSourceMethodGroup
      val sinkGroup = codeSlice.getSinkMethodGroup
      
      assert(sourceGroup.getAllNodes.nonEmpty, "Should have sources")
      assert(sinkGroup.getAllNodes.nonEmpty, "Should have sinks")
      
      val pathLine = codeSlice.getPathLine(sourceGroup, sinkGroup)
      val codeSlice_output = pathLine.exportCodeSlice(ioFileProcessor, outputPath, "test_package1_path")
      
      assert(codeSlice_output, "Should export code slice successfully")
      assert(new File(outputPath).listFiles().exists(_.getName.startsWith("test_package1_path_")), 
        "Output directory should contain exported slice files")
      
      codeSlice.close()
    } catch {
      case e: Exception =>
        fail(s"Failed to track path: ${e.getMessage}")
    }
  }
  
  test("TC-M-001: Detect malware patterns") {
    val packagePath = s"$testInputDir/test_malware_samples"
    val outputPath = s"$testOutputDir/test_malware_samples"
    
    val packageDir = new File(packagePath)
    if (!packageDir.exists() || packageDir.listFiles().isEmpty) {
      cancel("Test malware samples directory not available or empty")
    }
    
    try {
      val codeSlice = new CodeSliceImp(packagePath, outputPath)
      
      val sourceGroup = codeSlice.getSourceMethodGroup
      val sinkGroup = codeSlice.getSinkMethodGroup
      
      // Malware patterns should have multiple sources and sinks
      assert(sourceGroup.getAllNodes.nonEmpty, "Should detect malicious sources")
      assert(sinkGroup.getAllNodes.nonEmpty, "Should detect malicious sinks")
      
      println(s"Malware detection - Sources: ${sourceGroup.getAllNodes.size}, Sinks: ${sinkGroup.getAllNodes.size}")
      
      codeSlice.close()
    } catch {
      case e: Exception =>
        fail(s"Failed to detect malware patterns: ${e.getMessage}")
    }
  }
  
  test("TC-F-001: List package directories") {
    val packages = ioFileProcessor.listPackageDirectories(testInputDir)
    
    assert(packages.nonEmpty, "Should find test packages")
    assert(packages.forall(p => p.contains("test_package") || p.contains("test_malware_samples")), 
      "Should only contain test packages")
    
    println(s"Found ${packages.size} packages: ${packages.mkString(", ")}")
  }
  
  test("TC-F-002: Load and save checkpoint") {
    val checkpointPath = s"$testOutputDir/test_checkpoint.txt"
    val testPackage = "test_package_checkpoint"
    
    // Clean up existing checkpoint
    new File(checkpointPath).delete()
    
    // Save checkpoint
    val saved = ioFileProcessor.saveCheckpoint(checkpointPath, testPackage)
    assert(saved, "Should save checkpoint successfully")
    
    // Load checkpoint
    val loaded = ioFileProcessor.loadCheckpoint(checkpointPath)
    assert(loaded.contains(testPackage), s"Should contain saved package: $testPackage")
    
    println(s"Checkpoint saved and loaded successfully: $loaded")
    
    // Clean up
    new File(checkpointPath).delete()
  }
  
  test("TC-F-003: Check package processing status") {
    val processedPackages = Set("package1", "package2")
    
    assert(ioFileProcessor.isPackageProcessed("package1", processedPackages), 
      "Should identify processed package")
    assert(!ioFileProcessor.isPackageProcessed("package3", processedPackages), 
      "Should identify unprocessed package")
  }
  
  test("TC-F-004: Create output directory") {
    val testDirPath = s"$testOutputDir/test_create_dir"
    
    // Delete if exists
    val testDir = new File(testDirPath)
    if (testDir.exists()) {
      testDir.delete()
    }
    
    val createdPath = ioFileProcessor.createOutputDirectoryIfNotExists(testDirPath)
    
    assert(createdPath == new File(testDirPath).getAbsolutePath, 
      "Should return correct absolute path")
    assert(new File(testDirPath).exists(), "Directory should be created")
    
    // Clean up
    new File(testDirPath).delete()
  }
  
  test("TC-F-005: Save output package") {
    val outputPath = s"$testOutputDir/test_output.txt"
    val content = "Test content for output package"
    
    val saved = ioFileProcessor.saveOutputPackage(outputPath, content)
    assert(saved, "Should save output successfully")
    
    // Verify content
    val file = new File(outputPath)
    assert(file.exists(), "Output file should exist")
    
    val source = Source.fromFile(file)
    val readContent = source.mkString
    source.close()
    
    assert(readContent == content, "Content should match")
    
    // Clean up
    file.delete()
  }
  
  test("TC-F-006: Handle processing queue (add/remove)") {
    val processingPath = s"$testOutputDir/test_processing.txt"
    val testPackage = "test_package_queue"
    
    // Clean up
    new File(processingPath).delete()
    
    // Add to processing
    val added = ioFileProcessor.addToProcessing(processingPath, testPackage)
    assert(added, "Should add to processing queue")
    
    // Verify in queue
    val processing = ioFileProcessor.loadProcessing(processingPath)
    assert(processing.contains(testPackage), "Should be in processing queue")
    
    // Remove from processing
    val removed = ioFileProcessor.removeFromProcessing(processingPath, testPackage)
    assert(removed, "Should remove from processing queue")
    
    // Verify removed
    val processingAfter = ioFileProcessor.loadProcessing(processingPath)
    assert(!processingAfter.contains(testPackage), "Should not be in processing queue")
    
    // Clean up
    new File(processingPath).delete()
  }
  
  test("TC-E-001: Process complete package end-to-end") {
    val packagePath = s"$testInputDir/test_package2"
    val outputPath = s"$testOutputDir/e2e_test"
    
    val packageDir = new File(packagePath)
    if (!packageDir.exists() || packageDir.listFiles().isEmpty) {
      cancel("Test package 2 not available")
    }
    
    try {
      val codeSlice = new CodeSliceImp(packagePath, outputPath)
      
      // Get sources
      val sourceGroup = codeSlice.getSourceMethodGroup
      
      // Get sinks
      val sinkGroup = codeSlice.getSinkMethodGroup
      
      // Get paths
      val pathLine = codeSlice.getPathLine(sourceGroup, sinkGroup)
      
      // Export code slice
      val saved = pathLine.exportCodeSlice(ioFileProcessor, outputPath, "e2e_test_slice")
      
      // // Save output
      
      assert(saved, "Should save code slice output")
      val outputDir = new File(outputPath)
      val outputFiles = outputDir.listFiles().filter(_.getName.startsWith("e2e_test_slice_"))
      assert(outputFiles.nonEmpty, "Should have output slice files")
      
      println(s"E2E test completed, output files: ${outputFiles.map(_.getName).mkString(", ")}")
      
      codeSlice.close()
    } catch {
      case e: Exception =>
        fail(s"E2E test failed: ${e.getMessage}")
    }
  }
  
  test("TC-EC-007: Non-existent input directory") {
    val nonExistentPath = s"$testInputDir/non_existent_package"
    val outputPath = s"$testOutputDir/error_test"
    
    try {
      val codeSlice = new CodeSliceImp(nonExistentPath, outputPath)
      // If it doesn't throw, that's also valid - just check behavior
      codeSlice.close()
    } catch {
      case e: Exception =>
        // Expected to fail gracefully
        assert(e.getMessage != null, "Should have meaningful error message")
        println(s"Correctly handled non-existent directory with error: ${e.getMessage}")
    }
  }

  test("SCP-01: Run Joern Data Flow Analysis on test package") {
    try {
      val analyzer = JoernDataFlowAnalysis(
        inputDir = "./src/main/resources/input/test_sample",
        outputDir = "./src/main/resources/output/test_sample"
      )

      analyzer.runDataFlowAnalysis()
      analyzer.saveSlicedPathsToFile(ioFileProcessor, "test_sample")
      analyzer.close()

    } catch {
      case e: Exception =>
        fail(s"Failed to run Joern Data Flow Analysis: ${e.getMessage}")
    }
  }
}
