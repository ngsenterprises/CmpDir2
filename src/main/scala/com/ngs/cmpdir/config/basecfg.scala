package com.ngs.cmpdir.config

import java.io.File

//import scala.util.control.NonFatal
import scala.util.{ Try, Success, Failure }

import com.typesafe.config._
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.config.ConfigException._
import com.typesafe.config.ConfigException.{ Missing, WrongType }
//import com.typesafe.config
import collection.JavaConversions._
import com.typesafe.scalalogging.StrictLogging

object BaseConfig extends StrictLogging {
  //logger.info("Loading config file ...")
  val prop: String = System.getProperty("config.file")
  val configFile: String = if (prop == null) { "src/main/resources/application.conf" } else { prop }
  val config: Config = ConfigFactory.load(ConfigFactory.parseFileAnySyntax(new File(configFile)))
}

trait BaseConfig {
  def config = BaseConfig.config

  def getCfgStrListOrElse(skey: String, alt: List[String]): List[String] = {
    Try(config.getStringList(skey)) match {
      case Success(s) => s.toList
      case Failure(e) => alt
    }
  }

  def getCfgIntOrElse(skey: String, idef: Int): Int = {
    Try(config.getInt(skey)) match {
      case Failure(_) => idef
      case Success(ival: Int) => ival
    }
  }

  def getCfgBooleanOrElse(skey: String, bdef: Boolean): Boolean = {
    Try(config.getBoolean(skey)) match {
      case Failure(_) => bdef
      case Success(bval: Boolean) => bval
    }
  }

  def getCfgStringOrElse(key: String, alt: String): String = {
    Try(config.getString(key)) match {
      case Failure(_) => alt
      case Success(sval: String) => sval
    }
  }

}

trait AppConfig extends BaseConfig {
  val applicationName = getCfgStringOrElse("application.app-name", "")
}

trait AkkaConfig extends BaseConfig {
  val processors: Int = Runtime.getRuntime.availableProcessors
  val maxActors = getCfgIntOrElse("akka.max-actors", 7)
  val queueFactor = getCfgIntOrElse("akka.queue-factor", 3)
}

trait FileConfig extends BaseConfig {

  val basisFile = getCfgStringOrElse("basis-file", "")
  val testFile = getCfgStrListOrElse("cmp-files", List.empty[String])

  val fileTypes = getCfgStrListOrElse("files.cmp-types", List.empty[String])

  case class FileParms(skipHdr: Int, startLine: Int,
      maxLines: Int, maxErrors: Int) {
    def printParms = {
      println("FileParms {")
      println(s" skipHdr: ${skipHdr}")
      println(s" startLine: ${startLine}")
      println(s" maxErrors: ${maxErrors}")
      println("}")
    }
  }

  val fileTypeMap =
    fileTypes.foldLeft(Map.empty[String, FileParms]) { (ac, ext) =>
      val skiphdr = getCfgIntOrElse(s"files.${ext}.skip-hdr", 0)
      val startline = getCfgIntOrElse(s"files.${ext}.start-line", 0)
      val maxlines = getCfgIntOrElse(s"files.${ext}.max-lines", -1)
      val maxerrors = getCfgIntOrElse(s"files.${ext}.max-errors", 20)
      ac + (ext -> new FileParms(skiphdr, startline, maxlines, maxerrors))
    }

}

trait AllConfigs extends BaseConfig with AppConfig with AkkaConfig with FileConfig
