val akkaVersion = "2.6.6"
val akkaHttpVersion = "10.2.0"

val `LinearRegression` = project
  .in(file("."))
  .settings(
    organization := "com.adobe",
    version := "1.0",
    scalaVersion := "2.12.11",
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.junit" % "junit-bom" % "5.4.0",
      "org.junit.jupiter" % "junit-jupiter-engine" % "5.4.0",
      "org.apache.spark" %% "spark-sql" % "3.0.1",
      "org.apache.spark" %% "spark-mllib" % "3.0.1",
      "org.scala-lang" % "scala-reflect" % "2.12.11" % "provided",
      "org.scalanlp" % "breeze_2.12" % "1.1",
      "org.scalanlp" % "breeze-viz_2.12" % "1.1",
      "org.xerial.snappy" % "snappy-java" % "1.1.8",
      "com.google.protobuf" % "protobuf-java" % "3.13.0"),
    fork in run := true,
    Global / cancelable := false, // ctrl-c
    // disable parallel tests
    parallelExecution in Test := false,
    // show full stack traces and test case durations
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := false,
    licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0"))))