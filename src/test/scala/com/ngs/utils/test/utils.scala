package com.ngs.test.utils

import java.io.File

import com.ngs.cmpdir.utils.CmpDirUtils
import com.ngs.cmpdir.utils.CmpDirUtils.SrcGen
import com.ngs.cmpdir.config.FileConfig
import com.ngs.cmpdir.config.AllConfigs
import org.scalatest.FunSuite
import com.typesafe.scalalogging.StrictLogging

class utilsTest extends FunSuite with StrictLogging with AllConfigs {
  //logger.info(s"utilsTest: ...")

  test ("uuid init") {
    val id1 = CmpDirUtils.uuid.getNextId
    //logger.info(s"id1 - ${id1}")
    assert(id1 == 1)
    val id2 = CmpDirUtils.uuid.getNextId
    //logger.info(s"id2 - ${id2}")
    assert(id2 == 2)
    val id3 = CmpDirUtils.uuid.getNextId
    //logger.info(s"id3 - ${id3}")
    assert(id3 == 3)

//    var id = 3
//    (4 to 100000).foreach { _ =>
//      id = CmpDirUtils.uuid.getNextId
//      //if (id < 10) logger.info(s"uuid = ${id}")
//    }
//    //logger.info(s"uuid max = ${id}")
//    assert(id == 100000)
  }

  test ("src gen") {

    val f = new File(basisFile)
    val srcgen = SrcGen(f)
    srcgen.next
    logger.info(s"${srcgen.getCurrent}")

  }



}
