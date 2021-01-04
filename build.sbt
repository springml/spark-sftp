name := "spark-sftp"

organization := "com.springml"

scalaVersion := "2.12.10"

crossScalaVersions := Seq("2.11.12", "2.12.10")

sparkVersion := "2.4.3"

spName := "springml/spark-sftp"

version := "1.2.0"

// Dependent libraries
libraryDependencies ++= Seq(
  "com.springml" % "sftp.client" % "1.0.3",
  "org.mockito" % "mockito-core" % "2.0.31-beta",
  "com.databricks" %% "spark-xml" % "0.5.0"
)

// used spark components
sparkComponents ++= Seq("sql", "avro")

// Repositories
resolvers += "Spark Package Main Repo" at "https://dl.bintray.com/spark-packages/maven"

// Test dependencies
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % "test"
libraryDependencies += "org.apache.avro" % "avro-mapred" % "1.7.7" % "test" exclude("org.mortbay.jetty", "servlet-api")
libraryDependencies +=  "org.apache.spark" %% "spark-hive" % sparkVersion.value % "test"

spIgnoreProvided := true
// licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/springml/spark-sftp</url>
    <licenses>
      <license>
        <name>Apache License, Verision 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/springml/spark-sftp</connection>
      <developerConnection>scm:git:git@github.com:springml/spark-sftp</developerConnection>
      <url>github.com/springml/spark-sftp</url>
    </scm>
    <developers>
      <developer>
        <id>springml</id>
        <name>Springml</name>
        <url>http://www.springml.com</url>
      </developer>
    </developers>)
