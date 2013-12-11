package org.oopsc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import java.io._
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.{NoSuchFileException, Files, Paths}
import java.util.Collection
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.oopsvm.Assembler
import org.oopsvm.VirtualMachine
import com.typesafe.scalalogging.slf4j.Logging

@RunWith(value = classOf[Parameterized])
class TestSuite(var path: String) extends Logging {
  private var p: Program = null

  def runVM(asm: String, input: String): String = {
    val asmStream = new ByteArrayInputStream(asm.getBytes("UTF-8"))
    val vm = new VirtualMachine(new Assembler(false, false).assemble(asmStream), new Array[Int](8), false, false, false, false, false, false, false)
    val output = new ByteArrayOutputStream

    vm.setStreams(new ByteArrayInputStream(input.getBytes), output)
    vm.run

    return output.toString("UTF-8")
  }

  @Test
  def testGrammar {
    val stream = new FileInputStream(this.path)
    val input = new ANTLRInputStream(stream)
    val lexer = new GrammarLexer(input)
    val tokens = new CommonTokenStream(lexer)
    val parser = new GrammarParser(tokens)

    parser.program

    val supposedToFail = this.path.contains("_se")

    if (supposedToFail) {
      assertNotNull(parser.getNumberOfSyntaxErrors)
    } else {
      assertEquals(0, parser.getNumberOfSyntaxErrors)
    }
  }

  /**
   * Performs syntax as well as contextual analysis. Also tests the code generation.
   */
  @Test
  def testEverything {
    val supposedToFail = this.path.contains("_se")
    val pathExpected = this.path.substring(0, this.path.length - 5) + ".out"
    val expected =
       try {
         TestSuite.readFile(pathExpected, StandardCharsets.UTF_8)
       } catch {
         case e: NoSuchFileException => ""
       }

    try {
      this.p = new SyntaxAnalysis(this.path, false).parse
      this.p.semanticAnalysis

      this.p.printTree
      this.p.optimise
      this.p.printTree

      /* Test code generation. */
      val stream = new ByteArrayOutputStream
      val code = CodeStream.apply(stream)
      this.p.generateCode(code, 1000, 1000)
      val asm = stream.toString("UTF-8")

      logger.debug(asm)

      /* Run the VM twice with different inputs. */
      val output = this.runVM(asm, "abc\n") + this.runVM(asm, "xyz\n")
      assertEquals(expected, output)
    } catch {
      case e: CompileException => {
        if (supposedToFail) {
          logger.error(e.getMessage)
          return
        }

        throw e
      }
    }

    if (supposedToFail) {
      fail
    }
  }
}

object TestSuite {
  def readFile(path: String, encoding: Charset): String = {
    val encoded = Files.readAllBytes(Paths.get(path))
    return encoding.decode(ByteBuffer.wrap(encoded)).toString
  }

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

  @Parameters(name = "{0}")
  def data: Collection[Array[AnyRef]] = {
    val files = recursiveListFiles(new File("tests/")).map(_.toString).filter(_.endsWith(".oops")).sorted.map(Array[AnyRef](_))
    return scala.collection.JavaConversions.mutableSeqAsJavaList(files)
  }
}