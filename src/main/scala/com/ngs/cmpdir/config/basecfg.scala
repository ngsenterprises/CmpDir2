package com.ngs.cmpdir.config

import scala.util.{ Try, Success, Failure }
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.config.ConfigException.{ Missing, WrongType }
import collection.JavaConversions._
import com.typesafe.scalalogging.StrictLogging

object BaseConfig extends StrictLogging {
  logger.info("Loading config file ...")
  val config: Config = ConfigFactory.load()
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
  val appName = getCfgStringOrElse("application.app-name", "Compare App")
  val appLogging = getCfgIntOrElse("application.app-logging", 0)
}

trait AkkaConfig extends BaseConfig {
  val processors: Int = Runtime.getRuntime.availableProcessors
  val maxActors = math.max(1, getCfgIntOrElse("akka.max-actors", 7))
  val queueFactor = math.max(1, getCfgIntOrElse("akka.queue-factor", 3))
  val actorVerbose = getCfgBooleanOrElse("akka.actor-verbose", false)
}

trait FileConfig extends BaseConfig {
  def basisFileName = getCfgStringOrElse("files.basis-file-name", "")
  def cmpFileNames = getCfgStrListOrElse("files.cmp-file-names", List.empty[String])
  val fileTypes = getCfgStrListOrElse("files.cmp-types", List.empty[String])
  val cmpVerbose = getCfgBooleanOrElse("files.cmp-verbose", false)
  val headerCharacter = getCfgStringOrElse("files.header-character", "#")(0)

  case class FileParms(skipHdr: Boolean,
      startLine: Int,
      maxLines: Int,
      maxErrors: Int) {
    def printParms(): Unit = {
      println("FileParms {")
      println(s" skipHdr: $skipHdr")
      println(s" startLine: $startLine")
      println(s" maxErrors: $maxErrors")
      println("}")
    }
  }

  def nonNegIntMaxOverride(k: Int): Int = k match {
    case k: Int if k < 0 || Int.MaxValue <= k => Int.MaxValue
    case _ => k
  }

  val fileTypeMap =
    fileTypes.foldLeft(Map.empty[String, FileParms]) { (ac, ext) =>
      val skiphdr = getCfgBooleanOrElse(s"files.${ext}.skip-hdr", bdef = false)
      val startline = math.max(0, getCfgIntOrElse(s"files.${ext}.start-line", idef = 0))
      val maxlines = nonNegIntMaxOverride(getCfgIntOrElse(s"files.${ext}.max-lines", Int.MaxValue))
      val maxerrors = nonNegIntMaxOverride(getCfgIntOrElse(s"files.${ext}.max-errors", Int.MaxValue))
      ac + (ext -> new FileParms(skiphdr, startline, maxlines, maxerrors))
    }
}

trait AllConfigs extends BaseConfig with AppConfig with AkkaConfig with FileConfig {
  def printAll(): Unit = {
    println(s"appName: ${appName.toString}")
    println(s"appLogging: ${appLogging.toString}")
    println(s"processors: ${processors.toString}")
    println(s"maxActors: ${maxActors.toString}")
    println(s"queueFactor: ${queueFactor.toString}")
    println(s"actorVerbose: ${actorVerbose.toString}")
    println(s"basisFileName: ${basisFileName.toString}")
    cmpFileNames.zipWithIndex.foreach { item =>
      println(s"cmpFileName: ${item._2.toString} ${item._1.toString}")
    }
    fileTypes.zipWithIndex.foreach { item =>
      println(s"fileType: ${item._2.toString} ${item._1.toString}")
    }
    println(s"cmpVerbose: ${cmpVerbose.toString}")
    println(s"headerCharacter: $headerCharacter")
  }

}
