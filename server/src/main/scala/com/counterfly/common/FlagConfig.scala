package com.counterfly.common

import com.twitter.app.App
import com.twitter.finatra.http.HttpServer
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigResolveOptions
import java.nio.file.Paths

trait Configurable {
  def config: Config
}

/**
 * This trait provides a mechanism to handle command line arguments and ensures
 * that the configuration is loaded correctly before starting the application.
 */
trait MyApp extends App {

  private[this] var _completeArgs: Option[Array[String]] = None
  def completeArgs: Array[String] =
    _completeArgs.getOrElse(
      sys.error("Command line arguments accessed before being parsed"),
    )
  def isArgsParsed: Boolean = _completeArgs.isDefined

  override def parseArgs(args: Array[String]): Unit = {
    super.parseArgs(args)
    _completeArgs = Some(args)
  }
}

trait MyServer extends HttpServer with MyApp with CloseOnShutdown

// trait MyServer extends TwitterServer with MyApp

/**
 * A trait that provides a way to load configuration files using flags. It
 * allows specifying a configuration file and overriding specific configuration
 * values.
 *
 * Example usage:
 * {{{
 * val config = FlagConfig.parseConfig("development.conf", useSystemEnvironment = true)
 * }}}
 *
 * Example usage from command line:
 * {{{
 * sbt com.counterfly.x.y.Server run -config production
 * }}}
 */
trait FlagConfig extends Configurable { self: MyApp =>
  // Can be useful to override in some cases, e.g. when an application has config but no environment
  def defaultConfigName: String = "development"

  private[this] val configName = flag(
    "config",
    defaultConfigName,
    "Config file to use for default values. Prepend the argument with 'file:' to load from the filesystem instead of the classpath.",
  )
  private[this] val overrides = flag(
    "configOverride",
    Seq.empty[String],
    s"Override configuration values, e.g.: app.base.cluster=dcc4,app.store.hbase=$${hopper.hbase.dcc3}",
  )

  @transient lazy val config = {
    if (!isArgsParsed) sys.error("config accessed before flags were parsed.")
    FlagConfig.parseConfig(
      configName(),
      useSystemEnvironment = true,
      extraConfig = overrides.get.map(_.mkString("\n")),
    )
  }
}

object FlagConfig {
  def parseConfig(
    configName: String,
    useSystemEnvironment: Boolean,
    extraConfig: Option[String] = None,
  ): Config = {
    val configFromFlag =
      if (configName.startsWith("file:")) {
        val path = configName.stripPrefix("file:")
        ConfigFactory.parseFileAnySyntax(
          Paths.get(path).toFile,
          ConfigParseOptions.defaults().setAllowMissing(false),
        )
      } else {
        ConfigFactory.parseResourcesAnySyntax(
          configName,
          ConfigParseOptions.defaults().setAllowMissing(false),
        )
      }

    ConfigFactory
      .parseString(extraConfig.getOrElse(""))
      .withFallback(configFromFlag)
      .withFallback(ConfigFactory.defaultReference())
      .resolve(
        ConfigResolveOptions.defaults
          .setAllowUnresolved(false)
          .setUseSystemEnvironment(useSystemEnvironment),
      )
  }
}
