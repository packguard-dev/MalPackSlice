package CodeSlice
import CodeSlice.Group.{SourceMethodGroup, SinkMethodGroup}
import CodeSlice.Path.PathLine

@deprecated("Use V2 instead", "v2.0")
trait CodeSlice {

  /** get a source method group from the source group
    *
    * @param sourceGroup
    *   group of all sources
    * @return
    *   source method group
    */
  def getSourceMethodGroup: SourceMethodGroup

  /** get a sink method group from the sink group
    *
    * @param sinkGroup
    * @return
    */
  def getSinkMethodGroup: SinkMethodGroup

  /** get a taint base analysis path line
    *
    * @param sourceMethodGroup
    *   source method group
    * @param sinkMethodGroup
    *   sink method group
    * @return
    *   a path line to extract code from
    */
  def getPathLine(
      sourceMethodGroup: SourceMethodGroup,
      sinkMethodGroup: SinkMethodGroup
  ): PathLine

  /** extract code from line numbers and save to output directory
    *
    * @param pathLine
    *   the taint analysis path line
    *
    * @return
    *   extracted code as a string
    */
  def extractCode(pathLine: PathLine): String

  /** Clean up resources such as open files or database connections
    */
  def close(): Unit
}
