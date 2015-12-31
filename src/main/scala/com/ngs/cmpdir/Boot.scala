package com.ngs.cmpdir

import akka.actor.ActorSystem
import com.ngs.cmpdir.utils.io.CmpLogging
import com.typesafe.scalalogging.StrictLogging
import com.ngs.cmpdir.config.AllConfigs
import com.ngs.cmpdir.actors.CmpSupervisor
import com.ngs.cmpdir.utils.datasink.CmpJobActorRefSink

object Boot extends App with AllConfigs with CmpLogging {
  cmpprintln(s"$appName started [mod...")
  cmpprintln(s"Local machine processors: $processors")

  val system = ActorSystem("CmpSupervisor")
  val supervisor = system.actorOf(CmpSupervisor.props)

  utils.CmpJobGenerator.generate(CmpJobActorRefSink(supervisor))

}
