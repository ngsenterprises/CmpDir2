package com.ngs.cmpdir.utils

import java.io.{ FileWriter, File }
import scala.collection.immutable.{ List, HashMap }
import scala.util.{ Try, Success, Failure }
import com.typesafe.scalalogging.StrictLogging
import com.ngs.cmpdir.actors.CmpJob
import com.ngs.cmpdir.config._
import com.ngs.cmpdir.utils.datasink.DataSink
import com.ngs.cmpdir.utils.datasrc.FileSrc
import com.ngs.cmpdir.utils.io.CmpLogging

object Uid extends UidGenerator

trait UidGenerator {
  private var nextId = 0
  def getNextId(): Int = {
    nextId += 1
    nextId
  }
}

trait JobGenerator extends FileConfig with CmpLogging {

  def nextDir(dBasis: File, dCmp: File)(implicit cmpSink: DataSink): Try[_] = {

    //println(s"nextDir: ${dBasis.getName} ${dCmp.getName}")
    val basisSplit = Try(dBasis.listFiles.partition(_.isFile)) match {
      case Failure(e) => throw e
      case Success(s) => s
    }
    //println(s"basisSplit._1 ${basisSplit._1.toList}")
    //println(s"basisSplit._1: ${basisSplit._1.toList}")
    //println(s"basisSplit._2: ${basisSplit._2.toList}")

    //println(s"nextDir2: ${dBasis.getName} ${dCmp.getName}")
    val cmpSplit = Try(dCmp.listFiles.partition(_.isFile)) match {
      case Failure(e) => throw e
      case Success(s) => s
    }
    //println(s"cmpSplit._1: ${cmpSplit._1.toList}")
    //println(s"cmpSplit._2: ${cmpSplit._2.toList}")

    //println(s"nextDir3: ${dBasis.getName} ${dCmp.getName}")
    //println(s"basisSplit._1.toList: ${basisSplit._1.toList}")

    //    val basisFiles = basisSplit._1.toList.foldLeft(List.empty[File]) { (ac, f) =>
    //      f.getName.split(".") match {
    //        case a: Array[String] if (0 < a.length && fileTypes.contains(a.last)) => f :: ac
    //        case _ => ac
    //      }
    //    }
    //println(s"basisFiles: ${basisFiles}")
    //println(s"basisFiles.length: ${basisSplit._1.length}")
    if (0 < basisSplit._1.length) {
      val cmpFileMap = cmpSplit._1.foldLeft(Map.empty[String, File]) { (ac, f) => ac + (f.getName -> f) }
      //println(s"cmpFileMap: ${cmpFileMap.toList}")
      basisSplit._1.foreach {
        case fbasis: File =>
          //println(s"fbasis: ${fbasis.getName}")
          val basisName = fbasis.getName
          val a = basisName.split('.')
          //println(s"a: ${a.toList}")
          val ext = if (0 < a.length) a.last else ""
          //println(s"ext: ${ext}")
          if (0 < ext.length && fileTypes.contains(ext) && cmpFileMap.keys.toList.contains(basisName)) {
            val fcmp = cmpFileMap.getOrElse(basisName, new File(""))
            if (0 < fcmp.getName.length)
              cmpSink.inputCmpJob(new CmpJob(fcmp, fcmp, Uid.getNextId))
          }
      }
    }
    val cmpDirMap = cmpSplit._2.foldLeft(Map.empty[String, File]) { (ac, f) => ac + (f.getName -> f) }

    //println(s"nextDir6: ${dBasis.getName} ${dCmp.getName}")
    basisSplit._2.foreach { d =>
      cmpDirMap.get(d.getName) match {
        case None => Success("")
        case Some(dcmp: File) => nextDir(d, dcmp)
      }
    }
    //println(s"nextDir7: ${dBasis.getName} ${dCmp.getName}")

    Success(true)
  } //...........................................................

  def initGen(implicit cmpSink: DataSink): Try[_] = {
    //logger.info(s"initGen ...")
    if (basisFileName.length <= 0) throw new Exception("GenCmpJobs: Basis file must be given.")
    else if (cmpFileNames.length < 1) throw new Exception("GenCmpJobs: Must have at least 1 compare file.")
    else {
      //println(s"basisFileName: ${basisFileName}")
      val fBasis = new File(basisFileName)
      val fCmps = cmpFileNames.foldLeft(List[File]()) { (ac, fn) => new File(fn) :: ac }

      if (fBasis.isFile) {
        //println(s"FILE basisFileName: ${basisFileName}")
        val basisName = fBasis.getName
        val a = basisName.split('.')
        val ext = if (0 < a.length) a.last else ""
        if (0 < ext.length && fileTypes.contains(ext)) {
          val fcmplist = Try(fCmps.partition(_.isFile)) match {
            case Failure(e) => throw e
            case Success(s) => s._1
          }
          fcmplist.foreach {
            case f: File =>
              if (f.getName.endsWith(ext))
                cmpSink.inputCmpJob(new CmpJob(fBasis, f, Uid.getNextId))
          }
        }

        Success(true)
      } else if (fBasis.isDirectory) {
        //println(s"DIR basisFileName: ${basisFileName}")
        fCmps.foreach { f => if (f.isDirectory) nextDir(fBasis, f) }
        Success(true)
      } else
        Failure(new Exception("GenCmpJobs: Basis file not found."))
    }
  }

  def generate(cmpSink: DataSink): Boolean = {
    //logger.info(s"Generating file compare jobs ...")
    val res = initGen(cmpSink) match {
      case Failure(f) => { logger.info(f.getMessage); false }
      case Success(s) => true
    }
    //logger.info(s"Generating file compare jobs completed ...")
    res
  }
}
object CmpJobGenerator extends JobGenerator

trait FileComparator extends AllConfigs with CmpLogging {
  def cmp(bas: File, trg: File): Int = {
    var error_count = 0
    if (cmpVerbose) cmpprintln(s"[${trg.getAbsolutePath}]")
    if (bas.isFile && trg.isFile) {
      val suffix = bas.getAbsolutePath.split('.').last
      val ft = fileTypeMap.getOrElse(suffix, new FileParms(false, 0, Int.MaxValue, Int.MaxValue))

      if (ft.maxLines != 0) {
        val srcBas = FileSrc(bas)
        val srcTrg = FileSrc(trg)
        val wrt = new FileWriter(trg.getAbsolutePath + ".cmp")

        var lines_compared = 0
        var bas_line_cnt = 0
        var trg_line_cnt = 0

        srcBas.next
        srcTrg.next

        //skip header
        if (ft.skipHdr) {
          bas_line_cnt += srcBas.skipHeader(headerCharacter)
          trg_line_cnt += srcTrg.skipHeader(headerCharacter)
        }

        //start line
        if (0 < ft.startLine) {
          bas_line_cnt += srcBas.skipLines(ft.startLine)
          trg_line_cnt += srcTrg.skipLines(ft.startLine)
        }

        while (lines_compared < ft.maxLines &&
          error_count < ft.maxErrors &&
          srcBas.hasCurrent &&
          srcTrg.hasCurrent) {
          if (srcBas.getCurrent.compare(srcTrg.getCurrent.toString) != 0) {
            wrt.write(s"${bas_line_cnt}: [${srcBas.getCurrent}]\n")
            wrt.write(s"${trg_line_cnt}: [${srcTrg.getCurrent}]\n")
            wrt.write("\n")
            error_count += 1
          }
          lines_compared += 1
          srcBas.next
          bas_line_cnt += 1
          srcTrg.next
          trg_line_cnt += 1
        }
        wrt.close
      }
    }
    error_count
  }
} //end trait FileComparator

object FileCmp extends FileComparator

