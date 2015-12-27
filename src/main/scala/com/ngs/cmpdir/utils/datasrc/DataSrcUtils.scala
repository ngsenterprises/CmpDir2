package com.ngs.cmpdir.utils.datasrc

import java.io.File
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

trait SrcGenerator {
  var optCurVal: Option[String] = None
  var optItr: Option[Iterator[String]] = None

  def hasNext: Boolean = {
    optItr match {
      case Some(s) => s.hasNext
      case None => false
    }
  }
  def next: Option[String] = {
    optItr match {
      case Some(src) if src.hasNext => { optCurVal = Some(src.next); optCurVal }
      case _ => { optCurVal = None; None }
    }
  }
  final def skipHeader(hdrChar: Char): Int = {
    var done = false
    var count = 0
    while (!done) {
      optCurVal match {
        case None => done = true
        case Some(s: String) if s.length == 0 || s.trim().head != hdrChar => done = true
        case _ => {
          next
          count += 1
        }
      }
    }
    count
  }

  final def skipLines(lines: Int): Int = {
    var count = 0
    var done = false
    while (!done) {
      optCurVal match {
        case None => done = true
        case Some(s) =>
          next
          count += 1
      }
    }
    count
  }

  def hasCurrent: Boolean = optCurVal match {
    case Some(s) => true
    case _ => false
  }
  def getCurrent: String = optCurVal match {
    case Some(s) => s
    case _ => ""
  }
} //end trait SrcGenerator

class FileSrc(f: File) extends SrcGenerator {
  optItr = Try(Source.fromFile(f)) match {
    case Success(s) => Some(s.getLines())
    case Failure(f: Throwable) if NonFatal(f) => None
    case Failure(f: Throwable) => throw f
  }
} //end class DataSrc

object FileSrc {
  def apply(f: File): FileSrc = { new FileSrc(f) }
}

class ListBufferSrc(lb: ListBuffer[String]) extends SrcGenerator {

  optItr = Try(lb.toIterator) match {
    case Success(itr) => Some(itr)
    case Failure(f: Throwable) if NonFatal(f) => None
    case Failure(f: Throwable) => throw f
  }
}

object ListBufferSrc {
  def apply(lb: ListBuffer[String]): ListBufferSrc = { new ListBufferSrc(lb) }
}

