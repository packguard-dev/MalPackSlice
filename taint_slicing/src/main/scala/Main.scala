import CodeSlice.{CodeSlice, CodeSliceImp}
import FileProcessor.*
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.TimeoutException
import _root_.CodeSlice.JoernDataFlowAnalysis

object Main {

  def main(args: Array[String]): Unit = {

    print("\u001b[H\u001b[2J")

    val ioFileProcessor: IOFileProcessor = IOFileProcessorImpl
    val checkpointFilePath = "./src/main/resources/checkpoint.txt"
    val isProcessingFilePath = "./src/main/resources/is_processing.txt"
    
    var processedPackages = ioFileProcessor.loadCheckpoint(checkpointFilePath)
    val processingPackages = ioFileProcessor.loadProcessing(isProcessingFilePath)

    for (
      packagePath: String <- ioFileProcessor.listPackageDirectories(
        "./src/main/resources/input/"
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
        
        try {
          // Chạy xử lý trong Future với timeout
          val processingFuture = Future {
            val analyzer = new JoernDataFlowAnalysis(
                inputDir = packagePath,
                outputDir = ioFileProcessor.createOutputDirectoryIfNotExists(
                  "./src/main/resources/output/" + packageName + "/"
              )
            )

            analyzer.runDataFlowAnalysis()
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
            println(s"TIMEOUT: Package $packageName exceeded ${TIMEOUT_SECONDS}s limit (${elapsedTime}s). Skipping...")
            System.gc()
          case e: Exception =>
            val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
            println(s"Error processing package $packageName after ${elapsedTime}s: ${e.getMessage}")
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
