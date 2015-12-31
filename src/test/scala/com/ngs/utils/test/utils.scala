package com.ngs.test.utils

import java.io.File
import com.ngs.cmpdir.actors.CmpJob
import com.ngs.cmpdir.config.FileConfig
import com.ngs.cmpdir.config.AllConfigs
import com.ngs.cmpdir.utils
import com.ngs.cmpdir.utils.datasink.{ DataSink, CmpJobListBufferSink, CmpJobActorRefSink }
import org.scalatest.FunSuite
import com.typesafe.scalalogging.StrictLogging
import com.ngs.cmpdir.utils.{ CmpJobGenerator, UidGenerator }

import scala.collection.mutable.ListBuffer

class utilsTest extends FunSuite with StrictLogging with AllConfigs {
  //logger.info(s"utilsTest: ...")

  test("config init confirm.") {
    object cfg extends AllConfigs
    assert(appName == """Compare Directories Test""")
    assert(appLogging == 0)
    assert(processors == 4)
    assert(maxActors == 7)
    assert(queueFactor == 3)
    assert(actorVerbose == false)
    assert(basisFileName == """C:\dev\CmpDirWorkspace\dev_local\CmpDir2\src\test\resources\basis""")
    assert(cmpFileNames.head == """C:\dev\CmpDirWorkspace\dev_local\CmpDir2\src\test\resources\cmp""")
    assert(fileTypes(0) == "tab")
    assert(fileTypes(1) == "txt")
    assert(cmpVerbose == false)
    assert(headerCharacter == '#')
  }

  test("CmpJobGenerator confirm.") {
    import com.ngs.cmpdir.utils.CmpJobGenerator
    import com.ngs.cmpdir.utils.datasink.CmpJobActorRefSink
    import akka.testkit.TestActorRef
    import com.ngs.cmpdir.utils.datasink.CmpJobListBufferSink
    val buf = new ListBuffer[CmpJob]()
    CmpJobGenerator.generate(CmpJobListBufferSink(buf))
    val cmplist = buf.map { cj => (cj.bas.getName, cj.cmp.getName) }

    //println(s"buflist: length = ${buf.length}")
    //buf.foreach { cj => println(s"bas: ${cj.bas.getName} cmp: ${cj.cmp.getName} ") }

    assert(cmplist.contains(("f1.tab", "f1.tab")) == true)
    assert(cmplist.contains(("f2.txt", "f2.txt")) == true)
    assert(cmplist.contains(("f3.tab", "f3.tab")) == true)
  }

  test("Source file missing trial.") {
    object myuid1 extends UidGenerator

    import com.ngs.cmpdir.utils.JobGenerator
    trait FileConfigTest extends FileConfig {
      override def basisFileName = "BogusBaseFile.tab"
      //override def cmpFileNames = List.empty[String]
    }
    object CmpJobGeneratorTest extends JobGenerator with FileConfigTest

    //    println(s"basisFileName: [${CmpJobGeneratorTest.basisFileName}]")
    val buf = new ListBuffer[CmpJob]()
    CmpJobGeneratorTest.generate(CmpJobListBufferSink(buf))

  }

  test("uuid init confirm.") {
    import com.ngs.cmpdir.utils.{ UidGenerator }

    object myuuid1 extends UidGenerator

    val id1 = myuuid1.getNextId
    assert(id1 == 1)
    val id2 = myuuid1.getNextId
    assert(id2 == 2)
    val id3 = myuuid1.getNextId
    assert(id3 == 3)

    object myuuid2 extends UidGenerator
    var id = 3
    (1 to 100000).foreach { _ => id = myuuid2.getNextId }
    assert(id == 100000)
  }
}
