name := "graph-migration"

version := "0.1"

scalaVersion := "2.11.8"

// Please make sure that following DSE version matches your DSE cluster version.
val dseVersion = "6.8.0"

resolvers += "DataStax Repo" at "https://repo.datastax.com/public-repos/"
resolvers += Resolver.mavenLocal // for testing

mainClass in (Compile, packageBin) := Some("com.datastax.graph.MigrateData")

// Warning Sbt 0.13.13 or greater is required due to a bug with dependency resolution
//libraryDependencies += "com.datastax.dse" % "dse-spark-dependencies" % dseVersion % "provided"

// WORKAROUND for non published DSE. 
// all folowing code should be removed after public release. dse-spark-dependencies artiact is enough
// please set DSE_HOME env variable

val DSE_HOME = file(sys.env.getOrElse("DSE_HOME", sys.env("HOME")+"/dse"))
// find all jars in the DSE
def allDseJars = {
  val finder: PathFinder = (DSE_HOME) ** "*.jar" 
  finder.get
}
// add all jars to dependancies
unmanagedJars in Compile ++= allDseJars
unmanagedJars in Test ++= allDseJars 
