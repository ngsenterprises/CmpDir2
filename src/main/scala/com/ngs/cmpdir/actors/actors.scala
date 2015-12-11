package com.ngs.cmpdir.actors

import com.ngs.cmpdir.config.AkkaConfig
import java.io.{ File, FileWriter }
import akka.actor.{ Actor, ActorRef, Props, OneForOneStrategy, SupervisorStrategy }
import akka.actor.{ ActorLogging }
import akka.actor.SupervisorStrategy._
import akka.actor.SupervisorStrategy.restart
import akka.routing.{ ActorRefRoutee, Router, RoundRobinRoutingLogic }
import com.ngs.cmpdir.utils.CmpDirUtils

import scala.collection.immutable.{ HashMap, List }
import scala.io.Source
import scala.util.control.NonFatal
import scala.collection.mutable.{ ListBuffer, Queue }

import org.joda.time.{ Duration, DateTime }

sealed trait CmpMsg
case class CmpJob(bas: File, cmp: File, jobId: Int) extends CmpMsg
case class CmpJobComplete(jobId: Int) extends CmpMsg
case object CmpJobsStreamEnd extends CmpMsg
case object StopSystem extends CmpMsg

object CmpFileActor {
  def props = Props(classOf[CmpFileActor])
}

class CmpFileActor extends Actor with AkkaConfig with ActorLogging {

  def receive = {
    case CmpJob(bas, trg, jobId) => {
      val orgSender = sender
      //println("start job: " +jobId.toString)
      CmpDirUtils.cmpFiles(bas, trg)
      orgSender ! CmpJobComplete(jobId)
    }
  }
}

object CmpSupervisor {
  def props = Props(classOf[CmpSupervisor])
}

class CmpSupervisor extends Actor with AkkaConfig with ActorLogging {
  //val config = ConfigFactory.load()
  val numberOfActors = maxActors //7//config.getInt("app.akka.numberOfActors")
  val allowableJobs = queueFactor * numberOfActors
  val jobQue = new Queue[CmpJob]()

  var activeJobs = 0
  var startTime = System.currentTimeMillis
  var numJobsReq = 0
  var numJobsComplete = 0

  override def supervisorStrategy =
    OneForOneStrategy() {
      case e: Exception if (NonFatal(e)) => {
        println("supervisorStrategy RESTART. " + e.toString)
        restart
      }
      case e: Throwable => {
        println("supervisorStrategy ESCALATE. " + e.toString)
        escalate
      }
    }

  val router = {
    val routees = Vector.fill(5) {
      val r = context.actorOf(CmpFileActor.props)
      context.watch(r)
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case CmpJob(src, trg, jobId) => {
      //println(s"CmpJob ${src.getName} ${trg.getName}")
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
        self ! StopSystem
      else if (!jobQue.isEmpty && activeJobs < allowableJobs) {
        activeJobs += 1
        router.route(jobQue.dequeue(), self)
      }
    }

    case StopSystem => {
      val completeTime = System.currentTimeMillis() - startTime
      val elapsed = new Duration(completeTime)
      log.info(s"jobs - requested: ${numJobsReq}, completed: ${numJobsComplete}  ")
      log.info(s"StopSystemMsg elapsed time, min: ${elapsed.getStandardMinutes} sec: ${elapsed.getStandardSeconds % 60} ms: ${completeTime % 1000}")
      context.stop(self)
      System.exit(0)
    }
  }
}
