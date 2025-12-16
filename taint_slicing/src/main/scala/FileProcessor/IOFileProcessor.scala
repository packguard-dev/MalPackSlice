package FileProcessor

trait IOFileProcessor {

  /** List all package directories in a specific directory
    *
    * @param directoryPath
    *   Path to the directory to scan
    * @return
    *   Sequence of package directory paths
    */
  def listPackageDirectories(directoryPath: String): Seq[String]

  /** Load checkpoint file to get already processed packages
    *
    * @param checkpointFilePath
    *   Path to the checkpoint file
    * @return
    *   Set of processed package names
    */
  def loadCheckpoint(checkpointFilePath: String): Set[String]

  /** Save a package name to the checkpoint file
    *
    * @param packageName
    *   Name of the package to save
    * @return
    *   Boolean indicating success/failure
    */
  def saveCheckpoint(checkpointFilePath: String, packageName: String): Boolean

  /** Check if a package has been processed (exists in checkpoint)
    *
    * @param packageName
    *   Name of the package to check
    * @param processedPackages
    *   Set of already processed packages
    * @return
    *   Boolean indicating if package was already processed
    */
  def isPackageProcessed(
      packageName: String,
      processedPackages: Set[String]
  ): Boolean

  /** Save output packages (CPG results)
    * @param outputFilePath
    *   directory to save output packages
    * @param content
    *   content to save in the output package
    */
  def saveOutputPackage(outputFilePath: String, content: String): Boolean

  /** Get package name from directory path
    *
    * @param packageDir
    *   Directory of the package
    * @return
    *   Package name
    */
  def getPackageName(packageDir: String): String

  /** Create output directory if it does not exist
    *
    * @param outputDir
    *   Path to the output directory
    * @return
    *   Output directory file path
    */
  def createOutputDirectoryIfNotExists(outputDir: String): String

  /** Load the "is processing" file to get currently processing packages
    *
    * @param isProcessingFilePath
    *   Path to the is_processing file
    * @return
    *   Set of currently processing package names
    */
  def loadProcessing(isProcessingFilePath: String): Set[String]

  /** Add package name to the "is processing" file
    *
    * @param isProcessingFilePath
    *   Path to the is_processing file
    * @param packageName
    *   Name of the package being processed
    * @return
    *   Boolean indicating success/failure
    */
  def addToProcessing(
      isProcessingFilePath: String,
      packageName: String
  ): Boolean

  /** Remove package name from the "is processing" file
    *
    * @param isProcessingFilePath
    *   Path to the is_processing file
    * @param packageName
    *   Name of the package to remove
    * @return
    *   Boolean indicating success/failure
    */
  def removeFromProcessing(
      isProcessingFilePath: String,
      packageName: String
  ): Boolean
}
