import sbt.Opts.resolver
name := "search-as-you-type"
version := "0.1.0"
scalaVersion := "2.13.8"

// fork in run := false
// cancelable in Global := true
// Ctrl-C will cancel the running task, if the task is configured right (works with forked runs)
Global / cancelable := true
// Run everything in a fresh JVM, so we don't conflict with SBT / future jobs

lazy val server =
  Project(
    id = "server",
    base = file("server"),
  )
    .settings(
      libraryDependencies ++= Seq(
        "com.twitter" %% "finagle-http" % "24.2.0",
        "ch.qos.logback" % "logback-classic" % "1.5.17",
        "org.apache.kafka" % "kafka-clients" % "3.5.1",
        "org.slf4j" % "slf4j-api" % "2.0.17",
        "com.twitter" %% "finatra-http-server" % "24.2.0",
        "com.typesafe" % "config" % "1.4.3",
      ),
    )
    .settings(
      // runs the server in a new process
      // required so that ctrl+c closes the server
      //  (and so stops listening on port so can restart server w/o restarting sbt)
      run / fork := true,
      Test / fork := true,
    )
    .settings(
      (Compile / mainClass) := Some(
        "com.counterfly.logging.Server",
      ),
    )

lazy val logFilter =
  Project(
    id = "log-filter",
    base = file("log-filter"),
  )
    .settings(
      resolvers += Resolver.DefaultMavenRepository,
    )
    .settings(
      libraryDependencies ++= Seq(
        "org.apache.kafka" % "kafka-clients" % "3.5.1",
        "ch.qos.logback" % "logback-classic" % "1.5.17",
        "org.slf4j" % "slf4j-api" % "2.0.17",
        "com.typesafe" % "config" % "1.4.3",
        "org.json4s" %% "json4s-native" % "4.0.6",
        "org.json4s" %% "json4s-jackson" % "4.0.6",
        "org.specs2" %% "specs2-core" % "4.21.0" % Test,
      ),
    )
    .settings(
      run / fork := true,
      Test / fork := true,
    )
    .settings(
      (Compile / mainClass) := Some(
        "com.counterfly.logging.filter.LogFilter",
      ),
    )
