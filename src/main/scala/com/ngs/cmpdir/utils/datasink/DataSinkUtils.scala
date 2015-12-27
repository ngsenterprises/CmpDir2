package com.ngs.cmpdir.utils.datasink

import akka.actor.ActorRef
import com.ngs.cmpdir.actors.CmpJob
import scala.collection.mutable.ListBuffer

trait DataSink {
  def inputCmpJob(job: CmpJob): Boolean
}

class CmpJobActorRefSink(ar: ActorRef) extends DataSink { this: CmpJobActorRefSink =>
  def inputCmpJob(cmpjob: CmpJob): Boolean = {
    ar ! cmpjob
    true
  }
}

object CmpJobActorRefSink {
  def apply(ar: ActorRef) = new CmpJobActorRefSink(ar)
}

class CmpJobListBufferSink(buf: ListBuffer[CmpJob]) extends DataSink { this: CmpJobListBufferSink =>
  def inputCmpJob(cmpjob: CmpJob): Boolean = {
    cmpjob +=: buf
    true
  }
}

object CmpJobListBufferSink {
  def apply(buf: ListBuffer[CmpJob]) = new CmpJobListBufferSink(buf)
}
