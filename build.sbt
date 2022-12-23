lazy val scala211Version = "2.11.12"
lazy val scala212Version = "2.12.10"
lazy val sparkVersion = "3.2.1"

lazy val commonSettings = Seq(
  name := "spark-sftp",
  organization := "com.springml",
  version := "1.3.3",
  scalaVersion := scala212Version,
  crossScalaVersions := Seq(scala212Version)
)

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

initialize := {
  val _ = initialize.value
  val javaVersion = sys.props("java.specification.version")
  if (javaVersion != "1.8")
    sys.error("Java 1.8 is required for this project. Found " + javaVersion + " instead")
}


lazy val shaded = (project in file("."))
  .settings(commonSettings)

// Test dependencies
lazy val commonTestDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.apache.avro" % "avro-mapred" % "1.7.7" % "test" exclude("org.mortbay.jetty", "servlet-api"),
  "org.apache.spark" %% "spark-hive" % sparkVersion % "test"
)

// Dependent libraries
libraryDependencies ++= (commonTestDependencies ++ Seq(
  //spark libs
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  //spark -dependents
  "org.apache.spark" %% "spark-avro" % sparkVersion,
  "com.springml" % "sftp.client" % "1.0.3",
  "org.mockito" % "mockito-core" % "2.0.31-beta",
  "com.databricks" %% "spark-xml" % "0.5.0"
))


// Repositories
resolvers += "Spark Package Main Repo" at "https://dl.bintray.com/spark-packages/maven"






// licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

//credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

credentials += Credentials("Sonatype Nexus Repository Manager", "example.com", "deployment", "deployment_pwd")

publishTo := {
  val nexus = "http://example.com:8081/nexus/repository"
  if (version.value.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "/snapshots")
  else
    Some("releases" at nexus + "/releases")
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