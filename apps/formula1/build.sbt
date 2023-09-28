ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.16"

lazy val root = (project in file("."))
  .settings(
    name := "constructordominance"
  )

val sparkVersion = "3.2.2"


libraryDependencies := Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.hablapps" %% "doric_3-2" % "0.0.5"
  // "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
  // "org.apache.spark" %% "spark-sql" % sparkVersion % Provided,
)


