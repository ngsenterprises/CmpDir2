package com.ngs.cmpdir.actors

import java.io.{ File, FileWriter }
import org.joda.time.{ Duration, DateTime }
import akka.actor._
import akka.actor.ActorLogging
import akka.actor.SupervisorStrategy._
import akka.actor.SupervisorStrategy.restart
import akka.routing.{ ActorRefRoutee, Router, RoundRobinRoutingLogic }

import scala.util.control.NonFatal
import scala.collection.mutable.{ ListBuffer, Queue }
import com.ngs.cmpdir.config.AllConfigs
import com.ngs.cmpdir.utils.FileCmp

sealed trait CmpMsg
case class CmpJob(bas: File, cmp: File, jobId: Int) extends CmpMsg
case class CmpJobComplete(jobId: Int) extends CmpMsg
case object CmpJobsStreamEnd extends CmpMsg
case object SystemComplete extends CmpMsg
case object StopSystem extends CmpMsg

object CmpFileActor {
  def props = Props(classOf[CmpFileActor])
}

class CmpFileActor extends Actor with AllConfigs with ActorLogging {

  def receive = {
    case CmpJob(bas, trg, jobId) => {
      val orgSender = sender
      FileCmp.cmp(bas, trg)
      orgSender ! CmpJobComplete(jobId)
    }
  }
}

object CmpSupervisor {
  def props = Props(classOf[CmpSupervisor])
}

class CmpSupervisor extends Actor with AllConfigs with ActorLogging {
  val numberOfActors = maxActors
  val allowableJobs = queueFactor * numberOfActors
  val jobQue = new Queue[CmpJob]()

  var activeJobs = 0
  var startTime = System.currentTimeMillis
  var numJobsReq = 0
  var numJobsComplete = 0

  override def supervisorStrategy =
    OneForOneStrategy() {
      case e: Exception if (NonFatal(e)) => {
        log.info("supervisorStrategy RESTART. " + e.toString)
        restart
      }
      case e: Throwable => {
        log.info("supervisorStrategy ESCALATE. " + e.toString)
        escalate
      }
    }

  val router = {
    val routees = Vector.fill(numberOfActors) {
      val r = context.actorOf(CmpFileActor.props)
      context.watch(r)
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case CmpJob(src, trg, jobId) => {
      if (jobId == 1)
        log.info("System compare under way ...")
      numJobsReq += 1
      if (activeJobs < allowableJobs) {
        activeJobs += 1
        router.route(new CmpJob(src, trg, jobId), self)
      } else jobQue += new CmpJob(src, trg, jobId)
    }

    case CmpJobComplete(jobId) => {
      numJobsComplete += 1
      activeJobs -= 1
      if (jobQue.isEmpty && activeJobs == 0)
        self ! SystemComplete
      else {
        while (!jobQue.isEmpty && activeJobs < allowableJobs) {
          activeJobs += 1
          router.route(jobQue.dequeue(), self)
        }
      }
    }

    case SystemComplete => {
      val orgSender = sender
      val completeTime = System.currentTimeMillis() - startTime
      val elapsed = new Duration(completeTime)

      if (appLogging == 0) {
        println(s"jobs, requested: ${numJobsReq}, completed: ${numJobsComplete}")
        println(s"elapsed time: ${elapsed.getStandardMinutes} min, ${elapsed.getStandardSeconds % 60} sec, ${completeTime % 1000} ms")
      } else {
        log.info(s"jobs, requested: ${numJobsReq}, completed: ${numJobsComplete}")
        log.info(s"elapsed time: ${elapsed.getStandardMinutes} min, ${elapsed.getStandardSeconds % 60} sec, ${completeTime % 1000} ms")
      }
      self ! StopSystem
    }

    case StopSystem => {
      context.stop(self)
      System.exit(0)
    }
  }
}
