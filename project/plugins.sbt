// You may use this file to add plugin dependencies for sbt.
resolvers += "bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven/"
resolvers += Resolver.mavenLocal
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")