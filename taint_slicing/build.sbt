ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"

// Tăng heap size để tránh OutOfMemoryError
javaOptions ++= Seq(
  "-Xmx16G",
  "-Xms2G",
  "-XX:+UseG1GC",
  "-XX:MaxGCPauseMillis=200"
)

libraryDependencies ++= Seq(
  "io.joern" %% "joern-cli" % "4.0.436",
  "io.joern" %% "jssrc2cpg" % "4.0.436",
  "io.joern" %% "x2cpg" % "4.0.436",
  "io.joern" %% "dataflowengineoss" % "4.0.436",
  "com.google.guava" % "guava" % "33.0.0-jre",
  "org.slf4j" % "slf4j-simple" % "2.0.16",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

resolvers += "Joern" at "https://repo.joern.io"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

