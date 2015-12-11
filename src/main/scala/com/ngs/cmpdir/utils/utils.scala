package com.ngs.cmpdir.utils

import com.ngs.cmpdir.actors.CmpJob
import com.ngs.cmpdir.actors.CmpJobsStreamEnd
import com.ngs.cmpdir.config._

import java.io.{ FileWriter, File }

import scala.collection.immutable.{ List, HashMap }
import scala.util.{ Try, Success, Failure }
import scala.util.control.NonFatal
import scala.io.Source

import akka.actor.ActorRef
import com.typesafe.config.{ ConfigFactory, Config }

object CmpDirUtils extends AllConfigs {

  object uuid {
    private var nextId = 0
    def getNextId(): Int = {
      nextId += 1
      nextId
    }
  } //----------------------

  def GenCmpJobs(supervisor: ActorRef): Unit = {

    def nextDir(dBasis: File, dCmp: File): Unit = {

      val basisSplit = dBasis.listFiles.partition(_.isFile)
      val cmpSplit = dCmp.listFiles.partition(_.isFile)
      val cmpFileMap = cmpSplit._1.foldLeft(Map.empty[String, File]) { (ac, f) => ac + (f.getName -> f) }
      val cmpDirMap = cmpSplit._2.foldLeft(Map.empty[String, File]) { (ac, f) => ac + (f.getName -> f) }

      basisSplit._1.foreach { f =>
        cmpFileMap.get(f.getName) match {
          case None => ()
          case Some(fcmp: File) => supervisor ! new CmpJob(f, fcmp, uuid.getNextId)
        }
      }

      basisSplit._2.foreach { d =>
        cmpDirMap.get(d.getName) match {
          case None => ()
          case Some(dcmp: File) => nextDir(d, dcmp)
        }
      }
    } //...........................................................

    val basFile = getCfgStringOrElse("files.basis-file", "")
    val cmpFiles = getCfgStrListOrElse("files.cmp-files", List[String]())

    if (basFile.length <= 0) println("GenCmpJobs: Basis file must be given.")
    else if (cmpFiles.length < 1) println("GenCmpJobs: Must have at least 1 compare file.")
    else {
      val fBasis = new File(basFile)
      val fCmps = cmpFiles.foldLeft(List[File]()) { (ac, s) => (new File(s)) :: ac }

      if (fBasis.isFile)
        fCmps.foreach { f => if (f.isFile) supervisor ! new CmpJob(fBasis, f, uuid.getNextId) }
      else
        fCmps.foreach { d => if (d.isDirectory) nextDir(fBasis, d) }
    }
  }

  object SrcGen {
    def apply(f: File): SrcGen = { new SrcGen(f) }
  }
  class SrcGen(f: File) {
    var optCurVal: Option[String] = None
    private val optItr =
      Try(Source.fromFile(f)) match {
        case Success(s) => Some(s.getLines)
        case Failure(f: Throwable) if (NonFatal(f)) => None
        case Failure(f: Throwable) => throw f
      }
    def hasNext: Boolean = optItr match {
      case Some(s) => s.hasNext
      case None => false
    }
    def next: Option[String] = optItr match {
      case Some(src) if src.hasNext => {
        optCurVal = Some(src.next)
        optCurVal
      }
      case _ => {
        optCurVal = None
        None
      }
    }
    def skipHeader(hdrChar: Char): Unit = {
      var done = false
      while (!done) optCurVal match {
        case Some(s) if (0 < s.length && s(0) == '#') => { next; done = false }
        case Some(s) => done = true
        case None => done = true
      }
    }
    def skipLines(numLines: Int): Unit = {
      var index = numLines
      while (0 < index) optCurVal match {
        case Some(s) => {
          index -= 1
          next
        }
        case None => index = 0
      }
    }
    def hasCurrent: Boolean = optCurVal match {
      case Some(s) => true
      case _ => false
    }
    def getCurrent: String = optCurVal match {
      case Some(s) => s
      case _ => ""
    }
  }

  def cmpFiles(bas: File, trg: File): Long = {
    //println("cmpFiles [" +bas.getAbsolutePath +"]" +" [" +trg.getAbsolutePath +"]")

    var error_count = 0
    try {
      if (bas.isFile && trg.isFile) {

        val suffix = bas.getAbsolutePath.split('.').last
        val ft = fileTypeMap.getOrElse(suffix, new FileParms(0, 0, 0, 0))

        if (ft.maxLines != 0) {
          val srcBas = SrcGen(bas)
          val srcTrg = SrcGen(trg)

          val wrt = new FileWriter(trg.getAbsolutePath + ".cmp")

          var line_count = 0
          val skip_hdr = (ft.skipHdr == 1)
          val start_line = ft.startLine
          val max_lines = if (ft.maxLines < 0) Int.MaxValue else ft.maxLines
          val max_errors = ft.maxErrors

          srcBas.next
          srcTrg.next

          //skip header
          if (skip_hdr) {
            srcBas.skipHeader('#')
            srcTrg.skipHeader('#')
          }

          //start line
          if (0 < start_line) {
            srcBas.skipLines(start_line)
            srcTrg.skipLines(start_line)
            line_count = start_line
          }

          while (line_count < max_lines &&
            error_count < max_errors &&
            srcBas.hasCurrent &&
            srcTrg.hasCurrent) {
            if (srcBas.getCurrent.compare(srcTrg.getCurrent.toString) != 0) {
              wrt.write(line_count.toString + "\n")
              wrt.write("[" + srcBas.getCurrent + "]\n")
              wrt.write("[" + srcTrg.getCurrent + "]\n")
              error_count += 1
            }
            line_count += 1
            srcBas.next
            srcTrg.next
            //println(s"srcBas.next: ${srcBas.getCurrent}")
          }
          wrt.close
        }
      }
    } catch {
      case NonFatal(e) => {
        println("exception: " + e.toString)
      }
    }
    error_count
  }

} //end CmpDirUtils