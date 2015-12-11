package com.ngs.cmpdir

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
//import com.ngs.cmpdir.config.BaseConfig
//import com.ngs.cmpdir.config.AppConfig
import com.ngs.cmpdir.config.AllConfigs
import com.ngs.cmpdir.actors.CmpSupervisor

object Boot extends App with AllConfigs with StrictLogging {
  logger.info(s"$applicationName started ...")
  logger.info(s"Local machine processors: $processors")

  val system = ActorSystem("CmpSupervisor")
  val supervisor = system.actorOf(CmpSupervisor.props)

  utils.CmpDirUtils.GenCmpJobs(supervisor)
}
