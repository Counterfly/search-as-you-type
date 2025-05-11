import sbt.Opts.resolver
name := "search-as-you-type"
version := "0.1.0"
scalaVersion := "2.13.8"

// fork in run := false
// cancelable in Global := true
// Ctrl-C will cancel the running task, if the task is configured right (works with forked runs)
Global / cancelable := true
// Run everything in a fresh JVM, so we don't conflict with SBT / future jobs

/* START EXTRA WARNINGS */
val baseOptions: List[String] = List(
  "-Xfatal-warnings",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Ywarn-unused:imports",
  "-Xlint:adapted-args",
  "-Xlint:missing-interpolator",
  "-Xlint:nullary-unit",
  "-Xlint:option-implicit",
  "-Xlint:package-object-classes",
)

// Accessing or exposing types that are supposed to be private, e.g. referring to a private/protected in a method
val warningsAccessViolations: List[String] = List(
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
)

// Code (and type) correctness, e.g. inferring the any type accidentally, ignoring a value (Future[Unit]?),
// accidental shadowing of private vals or types, extra implicit parameter lists
val warningsCorrectness: List[String] = List(
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
)

// Unused paramaters, local vals, implicits, pattern match variables and private class vals
val warningsUnusedValues: List[String] = List(
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
)

// Static analysis of code that can be established to always result in an error
val warningsConstantEvaluation: List[String] = List(
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
)

// Common mistakes like wildcard pattern matches that would fail, or non-anchored scaladoc
val warningsLinters: List[String] = List(
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
)

// Common extras
val extraWarnings: List[String] = (
  baseOptions ++
    warningsCorrectness ++
    warningsAccessViolations ++
    warningsUnusedValues ++
    warningsConstantEvaluation ++
    warningsLinters
).distinct
/* DONE EXTRA WARNINGS */

lazy val server =
  Project(
    id = "server",
    base = file("server"),
  )
    .settings(
      Seq(
        scalacOptions ++= extraWarnings,
      ),
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
      Seq(
        scalacOptions ++= extraWarnings,
      ),
    )
    .settings(
      resolvers += Resolver.DefaultMavenRepository,
    )
    .settings(
      libraryDependencies ++= Seq(
        "org.apache.kafka" %% "kafka-streams-scala" % "3.9.0",
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
