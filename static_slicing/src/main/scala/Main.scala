import FileProcessor.*
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.TimeoutException
import _root_.CodeSlice.JoernDataFlowAnalysis
import CodeSlice.CodeSliceImp

object Main {

  def main(args: Array[String]): Unit = {

    print("\u001b[H\u001b[2J")

    if (args.length < 2) {
      println("Usage: sbt \"run <input_folder> <output_folder>\"")
      println("Example: sbt \"run ../unpacked_data/ben ../../../workspace/MalLLM/data/benign\"")
      sys.exit(1)
    }

    val inputFolder = args(0)
    val outputFolder = args(1)

    println(s"Input folder: $inputFolder")
    println(s"Output folder: $outputFolder")

    val ioFileProcessor: IOFileProcessor = IOFileProcessorImpl
    val checkpointFilePath = "./src/main/resources/checkpoint.txt"
    val isProcessingFilePath = "./src/main/resources/is_processing.txt"
    val errorLogFilePath = "./src/main/resources/error_log.txt"
    val timeoutLogFilePath = "./src/main/resources/timeout_log.txt"
    
    var processedPackages = ioFileProcessor.loadCheckpoint(checkpointFilePath)
    val processingPackages = ioFileProcessor.loadProcessing(isProcessingFilePath)

    for (
      packagePath: String <- ioFileProcessor.listPackageDirectories(
        inputFolder
      )
    ) {
      val packageName = ioFileProcessor.getPackageName(packagePath)
      
      if (ioFileProcessor.isPackageProcessed(packageName, processedPackages)) {
        println(s"Skipping already processed package: $packageName")
      } else if (processingPackages.contains(packageName)) {
        println(s"Skipping package currently being processed: $packageName")
      } else {

        if (ioFileProcessor.saveCheckpoint(checkpointFilePath, packageName)) {
          println(s"Checkpoint saved for package $packageName before processing")
        }

        if (ioFileProcessor.addToProcessing(isProcessingFilePath, packageName)) {
          println(s"Added $packageName to is_processing file")
        }

        val startTime = System.currentTimeMillis()
        val TIMEOUT_SECONDS = 180
        
        var analyzer: CodeSliceImp = null
        
        try {
          val processingFuture = Future {
            analyzer = new CodeSliceImp(
                inputDir = packagePath,
                outputDir = s"$outputFolder/$packageName/"
            )

            analyzer.runStaticAnalysis()
            analyzer.saveSlicedPathsToFile(ioFileProcessor, packageName)
            analyzer.close()
            
            System.gc()
          }
          
          // Chờ tối đa TIMEOUT_SECONDS giây
          Await.result(processingFuture, TIMEOUT_SECONDS.seconds)
          
          val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
          println(s"Package $packageName processed successfully in ${elapsedTime}s")
          
        } catch {
          case e: TimeoutException =>
            val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
            val timeoutMessage = s"TIMEOUT: Package $packageName exceeded ${TIMEOUT_SECONDS}s limit (${elapsedTime}s)"
            println(timeoutMessage)
            
            ioFileProcessor.saveOutputPackage(timeoutLogFilePath, s"$packageName,${elapsedTime}s\n")
            
            if (analyzer != null && analyzer.slicedPaths.nonEmpty) {
              println(s"Saving ${analyzer.slicedPaths.size} slices found before timeout for $packageName")
              analyzer.saveSlicedPathsToFile(ioFileProcessor, packageName)
            }
            
            System.gc()
          case e: Exception =>
            val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
            val errorMessage = s"ERROR: Package $packageName after ${elapsedTime}s: ${e.getMessage}"
            println(errorMessage)
            
            ioFileProcessor.saveOutputPackage(errorLogFilePath, s"$packageName,${elapsedTime}s,${e.getMessage}\n")
            e.printStackTrace()
        } finally {
          if (ioFileProcessor.removeFromProcessing(isProcessingFilePath, packageName)) {
            println(s"Removed $packageName from is_processing file")
          }
          
          processedPackages = ioFileProcessor.loadCheckpoint(checkpointFilePath)
          println(s"Checkpoint reloaded: ${processedPackages.size} total processed packages")
        }
      }
    }
    
    println("Processing complete!")
  }

}
