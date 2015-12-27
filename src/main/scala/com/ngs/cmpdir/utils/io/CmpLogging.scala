package com.ngs.cmpdir.utils.io

import akka.actor.ActorLogging
import com.typesafe.scalalogging.StrictLogging
import com.ngs.cmpdir.config.AllConfigs

trait CmpLogging extends AllConfigs with StrictLogging {
  val logType = appLogging
  def cmpprintln(s: String): Unit = logType match {
    case 0 => println(s)
    case _ => logger.info(s)
  }
  def cmpprintlnDirect(s: String): Unit = logger.info(s)
}

