package Type

sealed trait TypeDefinition

case class CallType(value: String) extends TypeDefinition

case class CodeType(value: String) extends TypeDefinition

case class RegexType(value: String) extends TypeDefinition


