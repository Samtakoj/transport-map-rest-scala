name := "transport-map-rest-scala"

version := "1.0"

scalaVersion := "2.12.1"

resolvers += Resolver.bintrayRepo("lhotari", "releases")
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

enablePlugins(JavaAppPackaging)

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.3"
libraryDependencies += "io.github.lhotari" %% "akka-http-health" % "1.0.2"
libraryDependencies += "io.spray" %% "spray-json" % "1.3.3"
libraryDependencies += "com.nrinaudo" %% "kantan.csv-generic" % "0.1.17"