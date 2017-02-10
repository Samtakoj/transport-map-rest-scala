name := "transport-map-rest-scala"

version := "1.0"

scalaVersion := "2.12.1"

resolvers += Resolver.bintrayRepo("lhotari", "releases")

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.3"
libraryDependencies += "io.github.lhotari" %% "akka-http-health" % "1.0.2"